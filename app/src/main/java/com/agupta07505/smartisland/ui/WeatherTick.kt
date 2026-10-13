/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.HourChime
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.PILL_CENTER_ZONE_SHARE
import com.agupta07505.smartisland.model.WeatherLook
import com.agupta07505.smartisland.model.WeatherSnapshot
import com.agupta07505.smartisland.model.weatherLook
import com.agupta07505.smartisland.ui.IslandText as Text
import com.agupta07505.smartisland.util.AnimationSpeed
import com.agupta07505.smartisland.util.IdleWeather
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** What the weather tick compares and draws: the condition (with day / night) and the temperature text in the chosen unit. */
private data class TickFace(val look: WeatherLook, val isDay: Boolean, val temp: String)

private const val WEATHER_TICK_TRAVEL = 0.75f

/** The stored weather reading is looked at this often (ms) while the tick is on; a stale one is refreshed here when the pill's own widget is not. */
private const val WEATHER_POLL_MS = 60_000L

/** The pill's own weather widget fetches first; this layer only steps in when the reading is this much older than a refresh (ms). */
private const val WEATHER_GRACE_MS = 60_000L

/**
 * The weather change tick as a takeover, like the hour tick: whenever the weather's condition or temperature changes, the pill's content
 * gives way for a moment and the left slot ticks from the old icon to the new one (downwards), the right slot from the old temperature to
 * the new one (upwards), at the slow pace of the hour tick. It plays whatever the pill shows (a live event, a notification, another widget,
 * and also when the idle widgets are held off after a tap to close or a long press). Only while the weather widget and the idle widgets are
 * switched on, the screen is on and the pill is the wide one. While the pill itself shows the weather widget, that widget's own tick plays
 * instead, so the change is never drawn twice.
 */
@Composable
internal fun WeatherTickLayer(
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
    /** Landscape: the pill may be brought in for the tick although a long press hid it ([withTickPill]). */
    canHold: Boolean = false
) {
    val context = LocalContext.current
    val settingsNow = androidx.compose.runtime.rememberUpdatedState(settings)
    val weatherOn = remember(settings.idleWidgetIds) { IdleWidgetKind.Weather in IdleWidgets.parse(settings.idleWidgetIds) }
    val enabled = settings.idleWidgetsEnabled && weatherOn && (canHold || settings.width > IDLE_WIDGET_COMPACT_MAX_DP)
    val screenOn = rememberScreenInteractive()
    val country = remember { Locale.getDefault().country }
    val fahrenheit = IdleWidgets.useFahrenheit(settings.idleWidgetTempUnit, country)
    val networkOk = settings.allowNetworkChecks

    var snapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var change by remember { mutableStateOf<Pair<TickFace, TickFace>?>(null) }
    val tick = remember { Animatable(0f) }
    val fahrenheitNow = androidx.compose.runtime.rememberUpdatedState(fahrenheit)

    // Keeps the latest stored reading; refreshes it only when it is stale and the pill's own widget has not (it asks first).
    LaunchedEffect(enabled, screenOn, networkOk) {
        if (!enabled || !screenOn) return@LaunchedEffect
        while (true) {
            var latest = IdleWeather.cached(context)
            val age = System.currentTimeMillis() - (latest?.fetchedAtMs ?: 0L)
            if (networkOk && (latest == null || age >= IdleWeather.REFRESH_MS + WEATHER_GRACE_MS)) {
                IdleWeather.fetch(context)?.let { latest = it }
            }
            if (latest != null) snapshot = latest
            delay(WEATHER_POLL_MS)
        }
    }

    fun faceOf(s: WeatherSnapshot, f: Boolean) =
        TickFace(weatherLook(s.weatherCode), s.isDay, IdleWidgets.displayTemp(s.temperatureC, f))

    var lastFace by remember { mutableStateOf<TickFace?>(null) }
    LaunchedEffect(snapshot, fahrenheit, enabled) {
        val s = snapshot
        if (!enabled || s == null) {
            lastFace = null
            return@LaunchedEffect
        }
        val face = faceOf(s, fahrenheitNow.value)
        val old = lastFace
        lastFace = face
        if (old == null || old == face) return@LaunchedEffect
        // The pill's own weather widget ticks by itself while it is on the pill; nothing to add then.
        if (ui.idleFrameNow?.kind == IdleWidgetKind.Weather) return@LaunchedEffect
        try {
            ui.withTickPill(canHold, { settingsNow.value.width > IDLE_WIDGET_COMPACT_MAX_DP }) {
                while (ui.chimeShow.value > 0.001f) delay(100)
                change = old to face
                tick.snapTo(0f)
                ui.chimeShow.animateTo(1f, tween(animSpeed.durationMs(HourChime.FADE_IN_MS)))
                delay(animSpeed.durationMs(HourChime.HOLD_BEFORE_MS).toLong())
                tick.animateTo(1f, tween(animSpeed.durationMs(HourChime.TICK_MS), easing = Easing { IdleSlots.heavyTickProgress(it) }))
                delay(animSpeed.durationMs(HourChime.HOLD_AFTER_MS).toLong())
                ui.chimeShow.animateTo(0f, tween(animSpeed.durationMs(HourChime.FADE_OUT_MS)))
                change = null
            }
        } finally {
            withContext(NonCancellable) {
                if (change != null) ui.chimeShow.snapTo(0f)
                change = null
            }
        }
    }

    val (old, new) = change ?: return
    val glyphFont = remember(settings.idleGlyphFont) {
        if (settings.idleGlyphFont.isEmpty()) null else islandFontFamily(context, settings.idleGlyphFont)
    }
    val glyphScale = settings.idleGlyphScale / 100f
    val slot = (1f - PILL_CENTER_ZONE_SHARE) / 2f
    Row(
        modifier = modifier
            .width(settings.width.dp)
            .height(settings.height.dp)
            .graphicsLayer { alpha = ui.chimeShow.value },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon: the old one leaves downwards, the new one comes in from above.
        TickSlot(Modifier.weight(slot), towardsDown = true, tick = tick,
            old = { Icon(weatherIcon(old.look, old.isDay).first, null, tint = Color.White, modifier = Modifier.size((15f * glyphScale).dp)) },
            new = { Icon(weatherIcon(new.look, new.isDay).first, null, tint = Color.White, modifier = Modifier.size((15f * glyphScale).dp)) })
        Spacer(Modifier.weight(PILL_CENTER_ZONE_SHARE))
        // Temperature: the old one leaves upwards, the new one comes in from below.
        TickSlot(Modifier.weight(slot), towardsDown = false, tick = tick,
            old = { TickText(old.temp, glyphFont, glyphScale) },
            new = { TickText(new.temp, glyphFont, glyphScale) })
    }
}

@Composable
private fun TickText(text: String, family: androidx.compose.ui.text.font.FontFamily?, scale: Float) {
    Text(text = text, color = Color.White, fontSize = (11f * scale).sp, fontFamily = family, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}

@Composable
private fun TickSlot(
    modifier: Modifier,
    towardsDown: Boolean,
    tick: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    old: @Composable () -> Unit,
    new: @Composable () -> Unit
) {
    val dir = if (towardsDown) 1f else -1f
    Box(modifier = modifier.fillMaxHeight().clipToBounds(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.graphicsLayer {
            val p = tick.value
            translationY = dir * p * size.height * WEATHER_TICK_TRAVEL
            alpha = 1f - p.coerceIn(0f, 1f)
        }) { old() }
        Box(modifier = Modifier.graphicsLayer {
            val p = tick.value
            translationY = -dir * (1f - p) * size.height * WEATHER_TICK_TRAVEL
            alpha = p.coerceIn(0f, 1f)
        }) { new() }
    }
}
