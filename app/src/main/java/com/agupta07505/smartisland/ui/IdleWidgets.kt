/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.agupta07505.smartisland.model.WidgetIslandContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.DndModes
import com.agupta07505.smartisland.model.ModeKind
import com.agupta07505.smartisland.model.ModeStatus
import com.agupta07505.smartisland.model.NotificationCounts
import com.agupta07505.smartisland.model.WeatherLook
import com.agupta07505.smartisland.model.nextStepWithContent
import com.agupta07505.smartisland.model.weatherLook
import com.agupta07505.smartisland.ui.IslandText as Text
import com.agupta07505.smartisland.util.IdleWidgetData
import com.agupta07505.smartisland.util.rememberIdleWidgetData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Idle widgets on the collapsed cutout (portrait pill and landscape circle) while no event is on the island.
 * What is shown is chosen in Layout > Idle widgets; the values come from [rememberIdleWidgetData].
 *
 * Wide pill (portrait): an icon in the left slot and the value in the right slot, like every other event, so the camera hole in the
 * middle stays clear. Small circle (landscape): only the value, centred, in small type.
 */
class IdleWidgetFrame(
    val kind: IdleWidgetKind,
    val data: IdleWidgetData,
    val fahrenheit: Boolean,
    /** The cutout is the small round idle circle: one centred value instead of two slots. */
    val compact: Boolean,
    /** 0..1: the widget fades in once the cutout has settled at its idle size. Read where it is drawn, so the fade does not recompose. */
    val appear: State<Float>,
    /** Font of the pill's glyph text; null = the island font. */
    val glyphFont: androidx.compose.ui.text.font.FontFamily? = null,
    /** Size of the pill's glyphs as a factor (1 = normal). */
    val glyphScale: Float = 1f,
    /** The widget on the pill was just changed by the automatic timer (not by a swipe): the glyphs travel left to right in three ticks. */
    val autoSwitch: Boolean = false,
    /** Milliseconds between the sideways ticks of the glyphs (Settings > Idle widgets). */
    val tickPeriodMs: Long = IdleSlots.SIDE_TICK_PERIOD_MS
)

/** The same data and unit as this frame, for another [kind], always as the wide (non-compact) layout. Used by the semi-expanded widget island. */
internal fun IdleWidgetFrame.asWide(kind: IdleWidgetKind): IdleWidgetFrame =
    IdleWidgetFrame(
        kind = kind, data = data, fahrenheit = fahrenheit, compact = false, appear = appear,
        glyphFont = glyphFont, glyphScale = glyphScale, tickPeriodMs = tickPeriodMs
    )

/**
 * Content of the semi-expanded widget island, per widget (the clock has none: its job is the hourly tick):
 *  - date: "Thursday 24th of February 2026";
 *  - weather: the icon, the temperature in the chosen unit and the name of the weather;
 *  - battery (only while charging): percent, time until full, voltage or wattage ([batteryMetric]) and the battery temperature;
 *  - next alarm (only within 12 h): the alarm time and the time left;
 *  - net speed: download and upload speed beside their arrows;
 *  - notifications: the apps with unread messages or mail and how many each has (up to three, the rest as "+N more");
 *  - mode: the active mode's icon, its name and a line on what it lets through.
 * Every island has the pause / play button on its right: it holds the collapsed pill on its current widget ([paused]).
 */
@Composable
internal fun IdleWidgetIslandContent(
    frame: IdleWidgetFrame,
    batteryMetric: String,
    paused: Boolean,
    onTogglePause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A swipe on the pill changes the widget: the body cross-fades, the pause button stays.
        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = frame.kind,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            contentAlignment = Alignment.Center,
            label = "widgetIslandBody"
        ) { kind ->
            val shown = frame.asWide(kind)
            when (kind) {
                IdleWidgetKind.Date -> DateIslandBody(shown)
                IdleWidgetKind.Weather -> WeatherIslandBody(shown)
                IdleWidgetKind.Battery -> BatteryIslandBody(shown, batteryMetric)
                IdleWidgetKind.NextAlarm -> AlarmIslandBody(shown)
                IdleWidgetKind.NetSpeed -> SpeedIslandBody(shown)
                IdleWidgetKind.Notifications -> NotificationsIslandBody(shown)
                IdleWidgetKind.Modes -> ModesIslandBody(shown)
                IdleWidgetKind.Clock, IdleWidgetKind.DayMoments -> Unit
            }
        }
        PauseToggle(paused = paused, onClick = onTogglePause)
    }
}

/** The pause / play button: pause while the pill is switching by itself, play while it is held. */
@Composable
private fun PauseToggle(paused: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (paused) 0.22f else 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            contentDescription = if (paused) "Resume widget switching" else "Pause widget switching",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DateIslandBody(frame: IdleWidgetFrame) {
    val locale = Locale.getDefault()
    val text = remember(frame.data.nowMs, locale) {
        val date = Date(frame.data.nowMs)
        if (WidgetIslandContent.usesEnglishOrdinalDate(locale.language)) {
            WidgetIslandContent.englishLongDate(
                weekdayName = SimpleDateFormat("EEEE", locale).format(date),
                day = SimpleDateFormat("d", locale).format(date).toIntOrNull() ?: 1,
                monthName = SimpleDateFormat("MMMM", locale).format(date),
                year = SimpleDateFormat("yyyy", locale).format(date).toIntOrNull() ?: 0
            )
        } else {
            SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMy"), locale).format(date)
        }
    }
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 3
    )
}

@Composable
private fun WeatherIslandBody(frame: IdleWidgetFrame) {
    val snapshot = frame.data.weather ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IdleWidgetIcon(IdleWidgetKind.Weather, frame, size = 30.dp)
        Column {
            Text(
                text = IdleWidgets.displayTemp(snapshot.temperatureC, frame.fahrenheit) + if (frame.fahrenheit) "F" else "C",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = WidgetIslandContent.weatherName(snapshot.weatherCode, snapshot.isDay),
                color = SOFT_WHITE,
                fontSize = 11.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun BatteryIslandBody(frame: IdleWidgetFrame, metric: String) {
    val data = frame.data
    val toFull = if (data.chargeTimeRemainingMs > 0L) WidgetIslandContent.durationText(data.chargeTimeRemainingMs) + " to full" else null
    val power = if (metric == WidgetIslandContent.BATTERY_VOLT) {
        data.batteryVoltageMv.takeIf { it > 0 }?.let { WidgetIslandContent.voltsText(it) }
    } else {
        WidgetIslandContent.batteryWatts(data.batteryCurrentRaw, data.batteryVoltageMv)?.let { WidgetIslandContent.wattsText(it) }
    }
    val temp = data.batteryTempTenthsC.takeIf { it != IdleWidgetData.NO_TEMPERATURE }
        ?.let { WidgetIslandContent.batteryTempText(it, frame.fahrenheit) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IdleWidgetIcon(IdleWidgetKind.Battery, frame, size = 18.dp)
            Text(
                text = if (data.batteryPercent < 0) "--" else "${data.batteryPercent}%",
                color = GREEN,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        toFull?.let { Text(text = it, color = Color.White, fontSize = 11.sp, maxLines = 1, softWrap = false) }
        val detail = listOfNotNull(power, temp).joinToString("  \u00B7  ")
        if (detail.isNotEmpty()) Text(text = detail, color = SOFT_WHITE, fontSize = 10.sp, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun AlarmIslandBody(frame: IdleWidgetFrame) {
    val context = LocalContext.current
    val data = frame.data
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", locale) }
    val at = data.nextAlarmMs
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IdleWidgetIcon(IdleWidgetKind.NextAlarm, frame, size = 26.dp)
        Column {
            Text(
                text = timeFormat.format(Date(at)),
                color = ORANGE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "in " + WidgetIslandContent.durationText(at - data.nowMs),
                color = SOFT_WHITE,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SpeedIslandBody(frame: IdleWidgetFrame) {
    val data = frame.data
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SpeedRow(Icons.Rounded.ArrowDownward, SKY, WidgetIslandContent.speedText(data.downBytesPerSecond))
        SpeedRow(Icons.Rounded.ArrowUpward, ORANGE, WidgetIslandContent.speedText(data.upBytesPerSecond))
    }
}

@Composable
private fun SpeedRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

/** The notification island: one row per app (icon, name, count), the biggest first, and a "+N more" line for what did not fit. */
@Composable
private fun NotificationsIslandBody(frame: IdleWidgetFrame) {
    val rows = NotificationCounts.rows(frame.data.notificationSummary)
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 10.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        rows.shown.forEach { app ->
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppIcon(app.packageName)
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = NotificationCounts.badgeText(app.count), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }
        if (rows.moreApps > 0) {
            Text(
                text = "+${rows.moreNotifications} more",
                color = SOFT_WHITE,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().height(14.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** An app's own icon, small; a bell when it cannot be loaded. */
@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon: ImageBitmap? = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(48, 48).asImageBitmap() }.getOrNull()
    }
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(16.dp).clip(CircleShape))
    } else {
        Icon(imageVector = Icons.Rounded.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** The mode island: the mode's icon beside its name and what it lets through. */
@Composable
private fun ModesIslandBody(frame: IdleWidgetFrame) {
    val mode = frame.data.mode
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeIcon(mode, size = 28.dp)
        Column {
            Text(text = mode.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = DndModes.detail(mode), color = SOFT_WHITE, fontSize = 10.sp, maxLines = 2)
        }
    }
}

/**
 * The glyph of a mode, white: the mode's own icon when the system exposes it (Android's mode icons, or the phone maker's for its own modes),
 * else a glyph for its kind.
 */
@Composable
internal fun ModeIcon(mode: ModeStatus, size: Dp) {
    val context = LocalContext.current
    val own: ImageBitmap? = remember(mode.iconPackage, mode.iconRes) {
        if (mode.iconRes == 0 || mode.iconPackage.isEmpty()) null else runCatching {
            val resources = context.packageManager.getResourcesForApplication(mode.iconPackage)
            androidx.core.content.res.ResourcesCompat.getDrawable(resources, mode.iconRes, null)?.toBitmap(96, 96)?.asImageBitmap()
        }.getOrNull()
    }
    if (own != null) {
        Image(bitmap = own, contentDescription = mode.name, colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(size))
    } else {
        Icon(imageVector = modeFallbackIcon(mode.kind), contentDescription = mode.name, tint = Color.White, modifier = Modifier.size(size))
    }
}

private fun modeFallbackIcon(kind: ModeKind): ImageVector = when (kind) {
    ModeKind.Dnd -> Icons.Rounded.DoNotDisturbOn
    ModeKind.TotalSilence -> Icons.Rounded.VolumeOff
    ModeKind.AlarmsOnly -> Icons.Rounded.Alarm
    ModeKind.Bedtime -> Icons.Rounded.Bedtime
    ModeKind.Driving -> Icons.Rounded.DirectionsCar
    ModeKind.Theater -> Icons.Rounded.TheaterComedy
    ModeKind.Gaming -> Icons.Rounded.SportsEsports
    ModeKind.Work -> Icons.Rounded.Work
    ModeKind.Schedule -> Icons.Rounded.Schedule
    ModeKind.Custom -> Icons.Rounded.NotificationsOff
}

/** The pill's left-slot glyph of the count: a soft badge with the number. It ticks (heavy clock tick, downwards) when the number changes. */
@Composable
private fun NotificationCountGlyph(frame: IdleWidgetFrame) {
    val text = NotificationCounts.badgeText(frame.data.notificationSummary.total)
    VerticalTick(target = text, towardsDown = true) { shown ->
        Box(
            modifier = Modifier
                .height((16f * frame.glyphScale).dp)
                .defaultMinSize(minWidth = (16f * frame.glyphScale).dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = 0.22f))
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            SlotText(shown, frame)
        }
    }
}

/** The pill's left-slot glyph of the mode: its icon; it ticks when another mode takes over. */
@Composable
private fun ModeGlyph(frame: IdleWidgetFrame) {
    val mode = frame.data.mode
    VerticalTick(target = mode, towardsDown = true) { shown ->
        ModeIcon(shown, size = (15f * frame.glyphScale).dp)
    }
}

/** The cutout must have been idle this long (ms) before a widget fades in, so it never shows on a pill that is still leaving or changing size. */
private const val IDLE_WIDGET_SETTLE_MS = 350L

/** The idle circle is drawn at least this big (dp) before a widget fits in it. */
private const val IDLE_WIDGET_MIN_DP = 20f

/** Cutouts up to this wide (dp) count as the small circle. The narrowest portrait pill is wider (76 dp). */
internal const val IDLE_WIDGET_COMPACT_MAX_DP = 60f

private val AMBER = Color(0xFFFBBF24)
private val ORANGE = Color(0xFFFF9F0A)
private val GREEN = Color(0xFF34C759)
private val RED = Color(0xFFFF453A)
private val SKY = Color(0xFF38BDF8)
private val SOFT_WHITE = Color(0xFFE5E7EB)

/**
 * What the collapsed cutout draws for the idle widgets, or null when none is due (switched off, a card is on the island, the screen
 * is off, the circle is too small). Cycles through the switched-on widgets every [SmartIslandSettings.idleWidgetCycleSeconds].
 */
@Composable
internal fun rememberIdleWidgetFrame(
    settings: SmartIslandSettings,
    noCard: Boolean,
    /** Gets the swipe hook ([IslandInteractionState.idleWidgetCycle]) while a widget is on the pill; null (tests, previews) leaves swiping out. */
    ui: IslandInteractionState? = null,
    /** Held off the pill on purpose (off after tap to close / long press to hide): no widget, like a switched-off setting. */
    suppressed: Boolean = false
): IdleWidgetFrame? {
    val screenOn = rememberScreenInteractive()
    val kinds = remember(settings.idleWidgetIds) { IdleWidgets.parse(settings.idleWidgetIds) }
    val active = settings.idleWidgetsEnabled && !suppressed && noCard && screenOn && settings.width >= IDLE_WIDGET_MIN_DP
    // A low-priority card on the pill takes the idle widget off it, but an open widget island under it still shows live values.
    val keepData = settings.idleWidgetsEnabled && screenOn && ui?.widgetIslandOpen == true
    val data = rememberIdleWidgetData(kinds, active || keepData, settings.allowNetworkChecks)
    // A widget with nothing to show yet (no alarm set, weather not loaded, battery unread) sits out of the rotation.
    // The clock never takes a turn on the pill: the status bar has the time. Its hourly tick is HourChimeLayer's.
    // The battery widget (pill and island) only exists while charging: the status bar has the level the rest of the time.
    val visible = kinds.filter { isAvailable(it, data) }
        .ifEmpty { kinds.filter { it !in NEEDS_CONTENT_KINDS } }
    var step by remember { mutableIntStateOf(0) }
    val cycleMs = settings.idleWidgetCycleSeconds * 1000L
    // The widget island's pause button holds the pill on its widget: no automatic switching (a swipe still steps it).
    // It also holds while the island is open, so the pill and the island always show the same widget (it goes on after the island closes).
    val paused = ui?.idleCyclePaused == true || ui?.widgetIslandOpen == true
    val rotates = active && visible.size > 1 && !paused
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        settled = false
        if (active) {
            delay(IDLE_WIDGET_SETTLE_MS)
            settled = true
        }
    }
    val appear = animateFloatAsState(
        targetValue = if (active && settled) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "idleWidgetAppear"
    )
    // A swipe moves the rotation by hand; the automatic timer then starts its count again, so a widget never flips right after one.
    var manualTicks by remember { mutableIntStateOf(0) }
    // Set by the automatic timer just before it steps, cleared by a swipe: the pill's glyph slots then play the three-tick left-to-right switch.
    var autoSwitched by remember { mutableStateOf(false) }
    if (ui != null) {
        SideEffect {
            val cycle: ((Boolean) -> Boolean)? = if (active) {
                ({ forward: Boolean ->
                    if (visible.size > 1) {
                        val direction = if (forward) 1 else -1
                        // With the island open it follows the swipe, so the next widget has to have something to show; otherwise the swipe springs back.
                        val target = if (ui.widgetIslandOpen) {
                            nextStepWithContent(step, direction, visible.size) { index ->
                                WidgetIslandContent.hasContent(
                                    kind = visible[index],
                                    charging = data.charging,
                                    nextAlarmMs = data.nextAlarmMs,
                                    nowMs = System.currentTimeMillis(),
                                    hasWeather = data.weather != null,
                                    notificationCount = data.notificationSummary.total,
                                    modeActive = data.mode.active
                                )
                            }
                        } else {
                            step + direction
                        }
                        if (target != null) {
                            step = target
                            autoSwitched = false
                            manualTicks++
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                })
            } else {
                null
            }
            ui.idleWidgetCycle = cycle
        }
        DisposableEffect(ui) { onDispose { ui.idleWidgetCycle = null } }
    }
    LaunchedEffect(rotates, cycleMs, manualTicks) {
        if (!rotates) return@LaunchedEffect
        while (true) {
            delay(cycleMs)
            autoSwitched = true
            step++
        }
    }
    if (!active) return null
    val kind = IdleWidgets.pick(visible, step) ?: return null
    val country = remember { Locale.getDefault().country }
    val context = LocalContext.current
    val glyphFont = remember(settings.idleGlyphFont) {
        if (settings.idleGlyphFont.isEmpty()) null else islandFontFamily(context, settings.idleGlyphFont)
    }
    return IdleWidgetFrame(
        kind = kind,
        data = data,
        fahrenheit = IdleWidgets.useFahrenheit(settings.idleWidgetTempUnit, country),
        compact = settings.width <= IDLE_WIDGET_COMPACT_MAX_DP,
        appear = appear,
        glyphFont = glyphFont,
        glyphScale = settings.idleGlyphScale / 100f,
        autoSwitch = autoSwitched,
        tickPeriodMs = IdleSlots.tickPeriodMs(settings.idleGlyphTickSeconds)
    )
}

/** Widgets that are never shown just to have something on the pill: they only appear while they have something to say. */
private val NEEDS_CONTENT_KINDS = setOf(
    IdleWidgetKind.Clock, IdleWidgetKind.DayMoments, IdleWidgetKind.Battery, IdleWidgetKind.Notifications, IdleWidgetKind.Modes
)

private fun isAvailable(kind: IdleWidgetKind, data: IdleWidgetData): Boolean = when (kind) {
    IdleWidgetKind.Clock, IdleWidgetKind.DayMoments -> false
    IdleWidgetKind.Notifications -> data.notificationSummary.total > 0
    IdleWidgetKind.Modes -> data.mode.active
    IdleWidgetKind.NextAlarm -> data.nextAlarmMs > System.currentTimeMillis()
    IdleWidgetKind.Weather -> data.weather != null
    IdleWidgetKind.Battery -> data.batteryPercent >= 0 && data.charging
    else -> true
}

/**
 * One side of the wide pill: what the glyph slot (left) or the value slot (right) shows. The widgets cross-fade when the next one takes over.
 * Per widget (see model/IdleSlots.kt):
 *  - date: the day on the left, the month on the right;
 *  - weather: the condition as a white glyph on the left, the temperature on the right; when either changes both tick like the hour tick (the glyph
 *    ticks away downwards and the new one comes in from above, the temperature goes upwards and the new one comes in from below);
 *  - battery while charging: wattage on the left, battery temperature on the right (without a readable current: the icon and percent as before);
 *  - next alarm: the alarm glyph and the alarm hour alternate on the left every second (ticking downwards), the minutes on the right;
 *  - net speed: upload on the left, download on the right.
 * The small idle circle ([IdleWidgetCompact]) is unchanged.
 */
@Composable
internal fun IdleWidgetSlot(frame: IdleWidgetFrame?, right: Boolean) {
    if (frame == null || frame.compact) return
    AnimatedContent(
        modifier = Modifier.graphicsLayer { alpha = frame.appear.value }.clipToBounds(),
        targetState = frame.kind,
        transitionSpec = {
            if (frame.autoSwitch) {
                // The automatic switch: the active glyph steps out to the right in three ticks while the next one steps in from the left.
                val ease = Easing { IdleSlots.tickProgress(it) }
                val travel = tween<IntOffset>(SWITCH_MS, easing = ease)
                (slideInHorizontally(travel) { -it * 2 } + fadeIn(tween(SWITCH_MS, easing = ease))) togetherWith
                    (slideOutHorizontally(travel) { it * 2 } + fadeOut(tween(SWITCH_MS, easing = ease)))
            } else {
                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
            }
        },
        label = "idleWidgetSlot"
    ) { kind ->
        IdleWidgetSlotFace(kind, frame, right)
    }
}

@Composable
private fun IdleWidgetSlotFace(kind: IdleWidgetKind, frame: IdleWidgetFrame, right: Boolean) {
    // True while the slot's own change tick (weather) is playing: the 2.5 s sideways tick leaves it alone.
    val ownTick = remember { mutableStateOf(false) }
    SideTickBox(kind, right, frame, ownTickRunning = { ownTick.value }) {
        IdleWidgetSlotBody(kind, frame, right, ownTick)
    }
}

/**
 * The 2.5 s beat: the slot's glyph ticks out to the right and the same glyph comes back in from the left. Skipped (see [IdleSlots.takesSideTick])
 * for the alarm glyph / hour, a slot whose own tick is running and the net slot carrying an arrow.
 */
@Composable
private fun SideTickBox(
    kind: IdleWidgetKind,
    right: Boolean,
    frame: IdleWidgetFrame,
    ownTickRunning: () -> Boolean,
    content: @Composable () -> Unit
) {
    val x = remember { Animatable(0f) }
    val pull = remember { Animatable(0f) }
    val frameNow by rememberUpdatedState(frame)
    val ownNow by rememberUpdatedState(ownTickRunning)
    LaunchedEffect(kind, right) {
        while (true) {
            delay(frameNow.tickPeriodMs)
            val f = frameNow
            val lead = IdleSlots.speedLead(f.data.downBytesPerSecond, f.data.upBytesPerSecond)
            if (f.appear.value < 1f || !IdleSlots.takesSideTick(kind, right, ownNow(), lead)) continue
            // The heavy tick: hang back a little, slowly, then snap out.
            pull.animateTo(1f, tween(SIDE_WINDUP_MS, easing = FastOutSlowInEasing))
            x.animateTo(1f, tween(SIDE_TICK_MS, easing = FastOutLinearInEasing))
            pull.snapTo(0f)
            x.snapTo(-1f)
            x.animateTo(0f, tween(SIDE_TICK_MS, easing = LinearOutSlowInEasing))
        }
    }
    // The outer box stays put and clips, so the glyph never travels outside its own glyph space.
    Box(modifier = Modifier.clipToBounds()) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationX = (x.value - SIDE_PULL_DEPTH * pull.value) * size.width
                alpha = 1f - kotlin.math.abs(x.value)
            }
        ) { content() }
    }
}

@Composable
private fun IdleWidgetSlotBody(kind: IdleWidgetKind, frame: IdleWidgetFrame, right: Boolean, ownTick: androidx.compose.runtime.MutableState<Boolean>) {
    val data = frame.data
    when (kind) {
        IdleWidgetKind.Date -> {
            val locale = Locale.getDefault()
            val text = remember(data.nowMs, locale, right) {
                SimpleDateFormat(if (right) IdleSlots.MONTH_PATTERN else IdleSlots.DAY_PATTERN, locale).format(Date(data.nowMs))
            }
            SlotText(text, frame)
        }
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            if (snapshot == null) {
                if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind, frame)
            } else {
                // The target carries what is drawn, so the old face keeps its own values while it ticks away.
                val face = WeatherFace(
                    look = weatherLook(snapshot.weatherCode),
                    isDay = snapshot.isDay,
                    temp = IdleWidgets.displayTemp(snapshot.temperatureC, frame.fahrenheit)
                )
                LaunchedEffect(face) {
                    ownTick.value = true
                    delay(TICK_MS + 80L)
                    ownTick.value = false
                }
                VerticalTick(target = face, towardsDown = !right) { shown ->
                    if (right) {
                        SlotText(shown.temp, frame)
                    } else {
                        Icon(
                            imageVector = weatherIcon(shown.look, shown.isDay).first,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size((15f * frame.glyphScale).dp)
                        )
                    }
                }
            }
        }
        IdleWidgetKind.Battery -> {
            val watts = if (data.charging) WidgetIslandContent.batteryWatts(data.batteryCurrentRaw, data.batteryVoltageMv) else null
            if (IdleSlots.batteryShowsPower(data.charging, watts) && watts != null) {
                if (right) {
                    val temp = data.batteryTempTenthsC.takeIf { it != IdleWidgetData.NO_TEMPERATURE }
                    SlotText(temp?.let { IdleSlots.shortBatteryTemp(it, frame.fahrenheit) } ?: "--", frame)
                } else {
                    SlotText(IdleSlots.shortWatts(watts), frame)
                }
            } else {
                if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind, frame)
            }
        }
        IdleWidgetKind.NextAlarm -> {
            if (data.nextAlarmMs <= 0L) {
                if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind, frame)
            } else if (right) {
                val minute = remember(data.nextAlarmMs) { alarmField(data.nextAlarmMs, java.util.Calendar.MINUTE) }
                SlotText(IdleSlots.alarmMinuteText(minute), frame)
            } else {
                AlarmLeftSlot(frame)
            }
        }
        IdleWidgetKind.NetSpeed -> NetSpeedSlot(frame, right)
        // One glyph on the left; the right slot stays empty.
        IdleWidgetKind.Notifications -> if (!right) NotificationCountGlyph(frame)
        IdleWidgetKind.Modes -> if (!right) ModeGlyph(frame)
        else -> if (right) IdleWidgetValue(kind, frame, compact = false, monochrome = true) else IdleWidgetIcon(kind, frame)
    }
}

/**
 * The net slots show their speed. The side that is faster (see [IdleSlots.speedLead]) ticks to its arrow on every 2.5 s beat: upload's arrow
 * ticks upwards, download's downwards; after a short hold the speed ticks back in the same way. The slower side takes the sideways tick.
 */
@Composable
private fun NetSpeedSlot(frame: IdleWidgetFrame, right: Boolean) {
    val data = frame.data
    val lead = IdleSlots.speedLead(data.downBytesPerSecond, data.upBytesPerSecond)
    val owns = IdleSlots.speedLeadOwnsSlot(lead, right)
    var arrow by remember { mutableStateOf(false) }
    val frameNow by rememberUpdatedState(frame)
    LaunchedEffect(owns) {
        arrow = false
        if (!owns) return@LaunchedEffect
        while (true) {
            delay(frameNow.tickPeriodMs)
            arrow = true
            delay(ARROW_HOLD_MS)
            arrow = false
        }
    }
    val upload = lead == IdleSlots.SpeedLead.Upload
    VerticalTick(target = owns && arrow, towardsDown = !upload) { showArrow ->
        if (showArrow) {
            Icon(
                imageVector = if (upload) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size((15f * frame.glyphScale).dp)
            )
        } else {
            SlotText(IdleWidgets.shortSpeed(if (right) data.downBytesPerSecond else data.upBytesPerSecond), frame)
        }
    }
}

/** The alarm's left slot: the monochrome alarm glyph and the alarm hour take turns every second, each tick moving downwards like the hour tick. */
@Composable
private fun AlarmLeftSlot(frame: IdleWidgetFrame) {
    val context = LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick++
        }
    }
    val hour = remember(frame.data.nextAlarmMs, is24) {
        IdleSlots.alarmHourText(alarmField(frame.data.nextAlarmMs, java.util.Calendar.HOUR_OF_DAY), is24)
    }
    VerticalTick(target = IdleSlots.alarmShowsHour(tick), towardsDown = true) { showHour ->
        if (showHour) SlotText(hour, frame) else IdleWidgetIcon(IdleWidgetKind.NextAlarm, frame)
    }
}

/** What the weather slots draw: the condition (with day / night) and the temperature text. Either one changing ticks both slots. */
private data class WeatherFace(val look: WeatherLook, val isDay: Boolean, val temp: String)

private fun alarmField(atMs: Long, field: Int): Int =
    java.util.Calendar.getInstance().apply { timeInMillis = atMs }.get(field)

@Composable
private fun SlotText(text: String, frame: IdleWidgetFrame) {
    Text(
        text = text,
        color = Color.White,
        fontSize = (11f * frame.glyphScale).sp,
        fontFamily = frame.glyphFont,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

/**
 * The hour tick's vertical move for a slot whose content changes with [target]: with [towardsDown] the old content leaves downwards and the new one
 * comes in from above, otherwise it leaves upwards and the new one comes in from below.
 */
@Composable
private fun <T> VerticalTick(target: T, towardsDown: Boolean, content: @Composable (T) -> Unit) {
    val dir = if (towardsDown) 1 else -1
    AnimatedContent(
        targetState = target,
        modifier = Modifier.clipToBounds(),
        transitionSpec = {
            // A clock's heavy tick: the glyph hangs back a little, slowly, then snaps through ([IdleSlots.heavyTickProgress]); the fades wait for the snap.
            val heavy = Easing { IdleSlots.heavyTickProgress(it) }
            val windUp = (TICK_MS * IdleSlots.HEAVY_WINDUP_SHARE).toInt()
            val snap = TICK_MS - windUp
            (slideInVertically(tween(TICK_MS, easing = heavy)) { -dir * it * 3 / 4 } + fadeIn(tween(snap, delayMillis = windUp))) togetherWith
                (slideOutVertically(tween(TICK_MS, easing = heavy)) { dir * it * 3 / 4 } + fadeOut(tween(snap * 2 / 3, delayMillis = windUp)))
        },
        label = "idleSlotTick"
    ) { content(it) }
}

private const val TICK_MS = 560

/** One half of the sideways beat tick (out to the right, then back in from the left). */
private const val SIDE_TICK_MS = 220

/** The slow, suspended pull-back before the sideways tick, and how far it goes (share of the glyph's width). */
private const val SIDE_WINDUP_MS = 260
private const val SIDE_PULL_DEPTH = 0.12f

/** How long the net arrow stays before the speed ticks back. */
private const val ARROW_HOLD_MS = 700L

/** The automatic switch's travel: three ticks. */
private const val SWITCH_MS = 900

/** The small round circle: just the value, centred. */
@Composable
internal fun IdleWidgetCompact(frame: IdleWidgetFrame?) {
    if (frame == null || !frame.compact) return
    AnimatedContent(
        modifier = Modifier.graphicsLayer { alpha = frame.appear.value },
        targetState = frame.kind,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "idleWidgetCompact"
    ) { kind ->
        Box(contentAlignment = Alignment.Center) {
            if (kind == IdleWidgetKind.Modes) ModeIcon(frame.data.mode, size = 12.dp) else IdleWidgetValue(kind, frame, compact = true)
        }
    }
}

@Composable
internal fun IdleWidgetIcon(kind: IdleWidgetKind, frame: IdleWidgetFrame, size: Dp = (15f * frame.glyphScale).dp) {
    val data = frame.data
    val (icon, tint) = when (kind) {
        IdleWidgetKind.Clock -> Icons.Rounded.Schedule to SOFT_WHITE
        IdleWidgetKind.DayMoments -> Icons.Rounded.WbTwilight to SOFT_WHITE
        IdleWidgetKind.Date -> Icons.Rounded.CalendarToday to SOFT_WHITE
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            if (snapshot == null) Icons.Rounded.Cloud to SOFT_WHITE
            else weatherIcon(weatherLook(snapshot.weatherCode), snapshot.isDay)
        }
        IdleWidgetKind.Battery -> batteryIcon(data.batteryPercent, data.charging)
        IdleWidgetKind.NextAlarm -> Icons.Rounded.Alarm to ORANGE
        IdleWidgetKind.NetSpeed ->
            if (data.speedIsUpload) Icons.Rounded.ArrowUpward to ORANGE else Icons.Rounded.ArrowDownward to SKY
        IdleWidgetKind.Notifications -> Icons.Rounded.Notifications to SOFT_WHITE
        IdleWidgetKind.Modes -> modeFallbackIcon(data.mode.kind) to SOFT_WHITE
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(size)
    )
}

@Composable
internal fun IdleWidgetValue(kind: IdleWidgetKind, frame: IdleWidgetFrame, compact: Boolean, monochrome: Boolean = false) {
    val context = LocalContext.current
    val data = frame.data
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale) }
    val dateFormat = remember(locale) {
        SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEd"), locale)
    }
    val dayFormat = remember(locale) { SimpleDateFormat("d", locale) }
    val weekdayFormat = remember(locale) { SimpleDateFormat("EEE", locale) }

    var color = Color.White
    val text: String
    when (kind) {
        IdleWidgetKind.Clock -> text = timeFormat.format(Date(data.nowMs))
        IdleWidgetKind.DayMoments -> text = ""
        IdleWidgetKind.Date -> text = (if (compact) dayFormat else dateFormat).format(Date(data.nowMs))
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            text = if (snapshot == null) "--\u00B0" else IdleWidgets.displayTemp(snapshot.temperatureC, frame.fahrenheit)
        }
        IdleWidgetKind.Battery -> {
            val pct = data.batteryPercent
            text = if (pct < 0) "--" else if (compact) "$pct" else "$pct%"
            color = when {
                data.charging -> GREEN
                pct in 0..15 -> RED
                else -> Color.White
            }
        }
        IdleWidgetKind.NextAlarm -> {
            val at = data.nextAlarmMs
            text = when {
                at <= 0L -> "--"
                // Within a day: the time. Further away: the weekday, so "Mon" is not mistaken for this morning.
                at - data.nowMs > DAY_MS -> weekdayFormat.format(Date(at))
                else -> timeFormat.format(Date(at))
            }
            color = ORANGE
        }
        IdleWidgetKind.NetSpeed -> {
            text = IdleWidgets.shortSpeed(data.speedBytesPerSecond)
            color = if (data.speedIsUpload) ORANGE else SKY
        }
        IdleWidgetKind.Notifications -> text = NotificationCounts.badgeText(data.notificationSummary.total)
        // The small circle has no room for a name: the mode's own glyph is drawn there instead ([IdleWidgetCompact]).
        IdleWidgetKind.Modes -> text = ""
    }
    if (monochrome) color = Color.White
    val size = if (!compact) (11f * frame.glyphScale).sp else when (kind) {
        IdleWidgetKind.Date, IdleWidgetKind.Weather, IdleWidgetKind.Battery -> 10.sp
        else -> 8.sp
    }
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontFamily = if (compact) null else frame.glyphFont,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

private const val DAY_MS = 24 * 60 * 60 * 1000L

private fun weatherIcon(look: WeatherLook, isDay: Boolean): Pair<ImageVector, Color> = when (look) {
    WeatherLook.Clear -> if (isDay) Icons.Rounded.WbSunny to AMBER else Icons.Rounded.NightsStay to SOFT_WHITE
    WeatherLook.PartlyCloudy -> Icons.Rounded.WbCloudy to (if (isDay) AMBER else SOFT_WHITE)
    WeatherLook.Cloudy, WeatherLook.Fog -> Icons.Rounded.Cloud to SOFT_WHITE
    WeatherLook.Rain -> Icons.Rounded.WaterDrop to SKY
    WeatherLook.Snow -> Icons.Rounded.AcUnit to SKY
    WeatherLook.Storm -> Icons.Rounded.Bolt to AMBER
}

private fun batteryIcon(percent: Int, charging: Boolean): Pair<ImageVector, Color> = when {
    charging -> Icons.Rounded.BatteryChargingFull to GREEN
    percent in 0..15 -> Icons.Rounded.BatteryAlert to RED
    percent >= 90 -> Icons.Rounded.BatteryFull to SOFT_WHITE
    else -> Icons.Rounded.Battery5Bar to SOFT_WHITE
}
