/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.ModeStatus
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.NotificationSummary
import com.agupta07505.smartisland.model.WeatherSnapshot
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The live values the idle widgets show. Each field is Compose state, so a widget that reads one redraws when it changes.
 * Filled by [rememberIdleWidgetData], which only listens for what the switched-on widgets need.
 */
class IdleWidgetData internal constructor() {
    /** Wall clock, refreshed every minute (and when the time or zone changes). */
    var nowMs by mutableLongStateOf(System.currentTimeMillis())
        internal set

    /** Battery level 0..100, or -1 until it is known. */
    var batteryPercent by mutableIntStateOf(-1)
        internal set
    var charging by mutableStateOf(false)
        internal set

    /** Battery voltage in mV, 0 = unknown. For the widget island. */
    var batteryVoltageMv by mutableIntStateOf(0)
        internal set

    /** Battery temperature in tenths of a degree Celsius, [NO_TEMPERATURE] = unknown. */
    var batteryTempTenthsC by mutableIntStateOf(NO_TEMPERATURE)
        internal set

    /** Time until the battery is full (ms), -1 = the system cannot tell. */
    var chargeTimeRemainingMs by mutableLongStateOf(-1L)
        internal set

    /** The battery's current reading as the device reports it (see WidgetIslandContent.batteryWatts for the unit), Long.MIN_VALUE = unsupported. */
    var batteryCurrentRaw by mutableLongStateOf(Long.MIN_VALUE)
        internal set

    /** When the next alarm rings (epoch ms), 0 = none. */
    var nextAlarmMs by mutableLongStateOf(0L)
        internal set

    /** Device-wide traffic in the last couple of seconds, bytes per second. */
    var speedBytesPerSecond by mutableLongStateOf(0L)
        internal set
    var speedIsUpload by mutableStateOf(false)
        internal set

    /** The two directions on their own (bytes per second), for the widget island. */
    var downBytesPerSecond by mutableLongStateOf(0L)
        internal set
    var upBytesPerSecond by mutableLongStateOf(0L)
        internal set

    var weather by mutableStateOf<WeatherSnapshot?>(null)
        internal set

    /** Unread messages and mail, in total and per app (see [NotificationCountTracker]). */
    var notificationSummary by mutableStateOf(NotificationSummary.Empty)
        internal set

    /** The Do Not Disturb mode that is on, or [ModeStatus.Off]. */
    var mode by mutableStateOf(ModeStatus.Off)
        internal set

    companion object {
        const val NO_TEMPERATURE = Int.MIN_VALUE
    }
}

/**
 * Starts what the [kinds] need and stops it again when [active] turns false (a card is on the island, the screen is off, the
 * widgets are switched off): a minute tick for the clock, the battery broadcast, the next-alarm broadcast, a 2 s traffic counter,
 * the weather refresh (only with [allowNetwork]), the notification count (from [NotificationCountTracker]) and the Do Not Disturb mode.
 */
@Composable
fun rememberIdleWidgetData(kinds: List<IdleWidgetKind>, active: Boolean, allowNetwork: Boolean): IdleWidgetData {
    val context = LocalContext.current.applicationContext
    val data = remember { IdleWidgetData() }
    val wantsBattery = active && IdleWidgetKind.Battery in kinds
    val wantsAlarm = active && IdleWidgetKind.NextAlarm in kinds
    val wantsTime = active && (IdleWidgetKind.Clock in kinds || IdleWidgetKind.Date in kinds || wantsAlarm)
    val wantsSpeed = active && IdleWidgetKind.NetSpeed in kinds
    val wantsWeather = active && allowNetwork && IdleWidgetKind.Weather in kinds
    val wantsNotifications = active && IdleWidgetKind.Notifications in kinds
    val wantsMode = active && IdleWidgetKind.Modes in kinds

    LaunchedEffect(wantsNotifications) {
        if (!wantsNotifications) {
            data.notificationSummary = NotificationSummary.Empty
            return@LaunchedEffect
        }
        NotificationCountTracker.entries.collect { data.notificationSummary = NotificationCounts.summarize(it) }
    }

    // Do Not Disturb: the system announces filter and policy changes; a mode switching to another one keeps the filter, so a slow check backs the broadcasts up.
    DisposableEffect(context, wantsMode) {
        if (!wantsMode) {
            data.mode = ModeStatus.Off
            return@DisposableEffect onDispose { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                data.mode = DndModeReader.read(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(NotificationManager.ACTION_NOTIFICATION_POLICY_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        data.mode = DndModeReader.read(context)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    LaunchedEffect(wantsMode) {
        if (!wantsMode) return@LaunchedEffect
        while (true) {
            delay(MODE_POLL_MS)
            val now = withContext(Dispatchers.Default) { DndModeReader.read(context) }
            if (now != data.mode) data.mode = now
        }
    }

    DisposableEffect(context, wantsTime, wantsBattery, wantsAlarm) {
        if (!wantsTime && !wantsBattery && !wantsAlarm) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED ->
                        data.nowMs = System.currentTimeMillis()
                    Intent.ACTION_BATTERY_CHANGED -> readBattery(context, data, intent)
                    AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> data.nextAlarmMs = nextAlarmMs(context)
                }
            }
        }
        val filter = IntentFilter().apply {
            if (wantsTime) {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            if (wantsBattery) addAction(Intent.ACTION_BATTERY_CHANGED)
            if (wantsAlarm) addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        // Whatever the clock showed while the widgets were off is stale.
        data.nowMs = System.currentTimeMillis()
        if (wantsAlarm) data.nextAlarmMs = nextAlarmMs(context)
        // A sticky broadcast (the battery one) answers the registration itself, so the level is right at once.
        val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        if (wantsBattery && sticky != null && sticky.action == Intent.ACTION_BATTERY_CHANGED) readBattery(context, data, sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(wantsSpeed) {
        if (!wantsSpeed) {
            data.speedBytesPerSecond = 0L
            data.downBytesPerSecond = 0L
            data.upBytesPerSecond = 0L
            return@LaunchedEffect
        }
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastAt = SystemClock.elapsedRealtime()
        while (true) {
            delay(SPEED_SAMPLE_MS)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val at = SystemClock.elapsedRealtime()
            val seconds = (at - lastAt).coerceAtLeast(1L) / 1000.0
            if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
                data.speedBytesPerSecond = 0L
                data.downBytesPerSecond = 0L
                data.upBytesPerSecond = 0L
            } else {
                val down = ((rx - lastRx).coerceAtLeast(0L) / seconds).toLong()
                val up = ((tx - lastTx).coerceAtLeast(0L) / seconds).toLong()
                data.speedIsUpload = up > down
                data.downBytesPerSecond = down
                data.upBytesPerSecond = up
                data.speedBytesPerSecond = maxOf(down, up)
            }
            lastRx = rx
            lastTx = tx
            lastAt = at
        }
    }

    LaunchedEffect(wantsWeather) {
        if (!wantsWeather) return@LaunchedEffect
        IdleWeather.cached(context)?.let { data.weather = it }
        while (true) {
            val age = System.currentTimeMillis() - (data.weather?.fetchedAtMs ?: 0L)
            if (data.weather == null || age >= IdleWeather.REFRESH_MS) {
                val fresh = IdleWeather.fetch(context)
                if (fresh != null) data.weather = fresh
            }
            val shownAge = System.currentTimeMillis() - (data.weather?.fetchedAtMs ?: 0L)
            // A failed lookup is retried in a few minutes; a good one waits for the next refresh.
            val wait = if (data.weather == null || shownAge >= IdleWeather.REFRESH_MS) IdleWeather.RETRY_MS
            else (IdleWeather.REFRESH_MS - shownAge).coerceAtLeast(IdleWeather.RETRY_MS)
            delay(wait)
        }
    }
    return data
}

private const val SPEED_SAMPLE_MS = 2000L
private const val MODE_POLL_MS = 5000L

private fun readBattery(context: Context, data: IdleWidgetData, intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level >= 0 && scale > 0) data.batteryPercent = (level * 100 / scale).coerceIn(0, 100)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    data.charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    data.batteryVoltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).coerceAtLeast(0)
    data.batteryTempTenthsC = if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
        intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, IdleWidgetData.NO_TEMPERATURE)
    } else {
        IdleWidgetData.NO_TEMPERATURE
    }
    // Time to full and the current only matter on the charging island, so they are only asked for while charging.
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    if (data.charging && manager != null) {
        data.chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { manager.computeChargeTimeRemaining() }.getOrDefault(-1L)
        } else {
            -1L
        }
        data.batteryCurrentRaw = runCatching { manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }
            .getOrDefault(Long.MIN_VALUE)
    } else {
        data.chargeTimeRemainingMs = -1L
        data.batteryCurrentRaw = Long.MIN_VALUE
    }
}

private fun nextAlarmMs(context: Context): Long =
    runCatching { (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.nextAlarmClock?.triggerTime }
        .getOrNull() ?: 0L

/**
 * Weather for the weather widget: Open-Meteo (no key, no account), asked with the place rounded to one decimal (about 10 km).
 * The place comes from the last known coarse location when the app may read it, else from the last one stored, else from an
 * IP lookup (the same service the VPN card uses). Only runs while "network checks" is on.
 */
object IdleWeather {
    const val REFRESH_MS = 30 * 60 * 1000L
    const val RETRY_MS = 5 * 60 * 1000L
    private const val TAG = "IdleWeather"
    private const val PREFS = "idle_widget_weather"
    private const val GPS_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L
    private const val IP_MAX_AGE_MS = 6 * 60 * 60 * 1000L

    fun hasLocationPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun cached(context: Context): WeatherSnapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("at")) return null
        return WeatherSnapshot(
            temperatureC = p.getFloat("t", 0f),
            weatherCode = p.getInt("c", 3),
            isDay = p.getBoolean("d", true),
            fetchedAtMs = p.getLong("at", 0L)
        )
    }

    /** Reads the last known coarse location and keeps it (rounded). True when a place was stored. Call it while the app is in front. */
    @SuppressLint("MissingPermission")
    fun captureLocation(context: Context): Boolean {
        if (!hasLocationPermission(context)) return false
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val best = runCatching {
            lm.getProviders(true).mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull { it.time }
        }.getOrNull() ?: return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("gps_lat", round1(best.latitude))
            .putFloat("gps_lon", round1(best.longitude))
            .putLong("gps_at", System.currentTimeMillis())
            .apply()
        return true
    }

    suspend fun fetch(context: Context): WeatherSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            captureLocation(context)
            val place = storedGps(context) ?: ipPlace(context)
            if (place == null) {
                AppLogRecorder.w(TAG, "No place for the weather: no location permission and the IP lookup failed")
                return@runCatching null
            }
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.1f&longitude=%.1f&current=temperature_2m,weather_code,is_day&timezone=auto",
                place.first, place.second
            )
            val json = httpGet(url) ?: return@runCatching null
            val current = JSONObject(json).optJSONObject("current") ?: return@runCatching null
            if (!current.has("temperature_2m")) return@runCatching null
            val snapshot = WeatherSnapshot(
                temperatureC = current.optDouble("temperature_2m", 0.0).toFloat(),
                weatherCode = current.optInt("weather_code", 3),
                isDay = current.optInt("is_day", 1) != 0,
                fetchedAtMs = System.currentTimeMillis()
            )
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("t", snapshot.temperatureC)
                .putInt("c", snapshot.weatherCode)
                .putBoolean("d", snapshot.isDay)
                .putLong("at", snapshot.fetchedAtMs)
                .apply()
            snapshot
        }.getOrElse {
            AppLogRecorder.w(TAG, "Weather lookup failed", it)
            null
        }
    }

    private fun storedGps(context: Context): Pair<Float, Float>? {
        if (!hasLocationPermission(context)) return null
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("gps_at")) return null
        if (System.currentTimeMillis() - p.getLong("gps_at", 0L) > GPS_MAX_AGE_MS) return null
        return p.getFloat("gps_lat", 0f) to p.getFloat("gps_lon", 0f)
    }

    private fun ipPlace(context: Context): Pair<Float, Float>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.contains("ip_at") && System.currentTimeMillis() - p.getLong("ip_at", 0L) < IP_MAX_AGE_MS) {
            return p.getFloat("ip_lat", 0f) to p.getFloat("ip_lon", 0f)
        }
        val body = httpGet("https://ipwho.is/") ?: return null
        val json = JSONObject(body)
        if (!json.optBoolean("success", true) || !json.has("latitude") || !json.has("longitude")) return null
        val lat = round1(json.optDouble("latitude"))
        val lon = round1(json.optDouble("longitude"))
        p.edit().putFloat("ip_lat", lat).putFloat("ip_lon", lon).putLong("ip_at", System.currentTimeMillis()).apply()
        return lat to lon
    }

    private fun httpGet(address: String): String? {
        val conn = URL(address).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "SmartIsland")
        return try {
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun round1(value: Double): Float = (Math.round(value * 10.0) / 10.0).toFloat()
}
