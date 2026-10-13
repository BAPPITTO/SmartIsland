/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.LedFace
import com.agupta07505.smartisland.model.LedFont
import com.agupta07505.smartisland.model.LedFrames
import com.agupta07505.smartisland.model.LedGlow
import com.agupta07505.smartisland.model.LedGrid
import com.agupta07505.smartisland.model.LedIcon
import com.agupta07505.smartisland.model.LedTick
import com.agupta07505.smartisland.model.LedTickConfig
import com.agupta07505.smartisland.model.WeatherLook
import com.agupta07505.smartisland.model.WidgetIslandContent
import com.agupta07505.smartisland.model.weatherLook
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * The LED footsteps tick (Settings > Idle widgets > Tick style): the slots of the collapsed pill drawn as a grid of LEDs whose
 * content moves in short hops, in bursts whose length follows the user's pattern. Rules and timing are in model/LedTick.kt and
 * model/LedFont.kt (pure, unit tested); this file draws them.
 */

/** Size of one LED in dp at the normal glyph size; the glyph scale setting scales it. */
private const val LED_PITCH_DP = 1.3f

/** Which widgets draw their slots on the LED board. The notification count and the mode glyph keep the smooth tick. */
internal fun ledSlotSupported(kind: IdleWidgetKind, right: Boolean): Boolean = when (kind) {
    IdleWidgetKind.Date, IdleWidgetKind.Weather, IdleWidgetKind.Battery,
    IdleWidgetKind.NextAlarm, IdleWidgetKind.NetSpeed, IdleWidgetKind.Clock, IdleWidgetKind.Wind -> true
    else -> false
}

internal fun ledColor(id: String): Color = when (LedTick.normalizeColor(id)) {
    LedTick.COLOR_AMBER -> Color(0xFFFFB020)
    LedTick.COLOR_GREEN -> Color(0xFF3DDC84)
    LedTick.COLOR_WHITE -> Color(0xFFF2F2F2)
    else -> Color(0xFFFF4034)
}

/** The dots of [face]: an icon is one of the 7 by 7 dot icons; a text uses the built-in dot font, or the chosen font turned into dots. */
internal fun ledGridOf(context: Context, face: LedFace, fontId: String): LedGrid = when (face) {
    is LedFace.Icon -> LedFont.icon(face.icon)
    is LedFace.Text ->
        if (fontId.isEmpty()) LedFont.text(face.text)
        else LedRaster.text(context, face.text, fontId) ?: LedFont.text(face.text)
}

private fun ledWeatherIcon(look: WeatherLook, isDay: Boolean): LedIcon = when (look) {
    WeatherLook.Clear -> if (isDay) LedIcon.Sun else LedIcon.Moon
    WeatherLook.PartlyCloudy, WeatherLook.Cloudy, WeatherLook.Fog -> LedIcon.Cloud
    WeatherLook.Rain -> LedIcon.Rain
    WeatherLook.Snow -> LedIcon.Snow
    WeatherLook.Storm -> LedIcon.Bolt
}

private fun ledBatteryIcon(percent: Int, charging: Boolean): LedIcon = when {
    charging -> LedIcon.BatteryFull
    percent in 0..15 -> LedIcon.BatteryLow
    percent >= 90 -> LedIcon.BatteryFull
    else -> LedIcon.Battery
}

private fun ledAlarmField(atMs: Long, field: Int): Int =
    java.util.Calendar.getInstance().apply { timeInMillis = atMs }.get(field)

/**
 * One slot of the pill on the LED board. The face is worked out from the same data as the smooth slots; when it changes (a new
 * temperature, the next minute) the board plays a burst from the old face to the new one, and on every beat of the glyph tick
 * period it plays a burst from the face to the same face. A slot that alternates between two faces (the faster net side's arrow
 * and its speed, the alarm glyph and its hour) changes face on its own beat, so it needs no extra beat.
 */
@Composable
internal fun LedSlot(kind: IdleWidgetKind, frame: IdleWidgetFrame, right: Boolean, config: LedTickConfig) {
    val context = LocalContext.current
    val data = frame.data
    val locale = Locale.getDefault()
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24, locale) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale) }
    val dayFormat = remember(locale) { SimpleDateFormat(IdleSlots.DAY_PATTERN, locale) }
    val monthFormat = remember(locale) { SimpleDateFormat(IdleSlots.MONTH_PATTERN, locale) }

    val lead = IdleSlots.speedLead(data.downBytesPerSecond, data.upBytesPerSecond)
    val owns = kind == IdleWidgetKind.NetSpeed && IdleSlots.speedLeadOwnsSlot(lead, right)
    val alternates = owns || (kind == IdleWidgetKind.NextAlarm && !right && data.nextAlarmMs > 0L)
    val period = frame.tickPeriodMs

    // True while the alternate face (the arrow, the alarm hour) is the one to show.
    var alternate by remember { mutableStateOf(false) }
    LaunchedEffect(alternates, period) {
        alternate = false
        if (!alternates) return@LaunchedEffect
        while (true) {
            delay(period)
            alternate = true
            delay(period.coerceIn(800L, 1500L))
            alternate = false
        }
    }

    val face: LedFace = when (kind) {
        IdleWidgetKind.Date -> LedFace.Text((if (right) monthFormat else dayFormat).format(Date(data.nowMs)))
        IdleWidgetKind.Weather -> {
            val snapshot = data.weather
            if (snapshot == null) {
                if (right) LedFace.Text("--°") else LedFace.Icon(LedIcon.Cloud)
            } else if (right) {
                LedFace.Text(IdleWidgets.displayTemp(snapshot.temperatureC, frame.fahrenheit))
            } else {
                LedFace.Icon(ledWeatherIcon(weatherLook(snapshot.weatherCode), snapshot.isDay))
            }
        }
        IdleWidgetKind.Battery -> {
            val watts = if (data.charging) WidgetIslandContent.batteryWatts(data.batteryCurrentRaw, data.batteryVoltageMv) else null
            if (IdleSlots.batteryShowsPower(data.charging, watts) && watts != null) {
                if (right) {
                    val temp = data.batteryTempTenthsC.takeIf { it != com.agupta07505.smartisland.util.IdleWidgetData.NO_TEMPERATURE }
                    LedFace.Text(temp?.let { IdleSlots.shortBatteryTemp(it, frame.fahrenheit) } ?: "--")
                } else {
                    LedFace.Text(IdleSlots.shortWatts(watts))
                }
            } else if (right) {
                LedFace.Text(if (data.batteryPercent < 0) "--" else "${data.batteryPercent}%")
            } else {
                LedFace.Icon(ledBatteryIcon(data.batteryPercent, data.charging))
            }
        }
        IdleWidgetKind.NextAlarm -> when {
            data.nextAlarmMs <= 0L -> if (right) LedFace.Text("--") else LedFace.Icon(LedIcon.Alarm)
            right -> LedFace.Text(IdleSlots.alarmMinuteText(ledAlarmField(data.nextAlarmMs, java.util.Calendar.MINUTE)))
            alternate -> LedFace.Text(IdleSlots.alarmHourText(ledAlarmField(data.nextAlarmMs, java.util.Calendar.HOUR_OF_DAY), is24))
            else -> LedFace.Icon(LedIcon.Alarm)
        }
        IdleWidgetKind.NetSpeed ->
            if (owns && alternate) LedFace.Icon(if (lead == IdleSlots.SpeedLead.Upload) LedIcon.ArrowUp else LedIcon.ArrowDown)
            else LedFace.Text(IdleWidgets.shortSpeed(if (right) data.downBytesPerSecond else data.upBytesPerSecond))
        IdleWidgetKind.Wind ->
            if (right) LedFace.Text(IdleWidgets.displayWind(data.weather?.windKmh ?: -1f, frame.windUnit)) else LedFace.Icon(LedIcon.Wind)
        IdleWidgetKind.Clock ->
            if (right) LedFace.Text(timeFormat.format(Date(data.nowMs))) else LedFace.Icon(LedIcon.Clock)
        else -> LedFace.Text("")
    }
    // Which way the content leaves: the same directions the smooth ticks use.
    val towardsDown = when (kind) {
        IdleWidgetKind.Weather -> !right
        IdleWidgetKind.NetSpeed -> lead != IdleSlots.SpeedLead.Upload
        IdleWidgetKind.NextAlarm -> true
        else -> false
    }
    LedTickFace(
        face = face,
        config = config,
        towardsDown = towardsDown,
        tickPeriodMs = period,
        repeatBeat = !alternates,
        glyphScale = frame.glyphScale,
        // Footsteps: the right slot steps half a pace after the left one. Only the slot whose own face changed moves on a data change, so
        // a new minute or temperature steps the value alone and the glyph stays still; a new widget changes both faces and both step.
        startDelayMs = LedTick.staggerMs(config.paceMs, right, config.stagger),
        ready = { frame.appear.value >= 1f }
    )
}

/** The state of one LED board: what it settled on, its afterglow and a counter that redraws it. */
private class LedBoardState(initial: LedGrid) {
    var target: LedGrid = initial
    var glow: LedGlow = LedGlow(initial.cols).also { it.settle(LedFrames.place(initial, initial.cols, LedTick.ROWS, 0)) }
        private set
    var cols by mutableIntStateOf(initial.cols)
        private set
    var version by mutableIntStateOf(0)

    fun resize(newCols: Int) {
        if (newCols != glow.cols) {
            glow = LedGlow(newCols)
            cols = newCols
        }
    }

    /** Plays one burst from [from] to [to] and leaves the board at rest on [to]. */
    suspend fun burst(from: LedGrid, to: LedGrid, towardsDown: Boolean, config: LedTickConfig, beat: Long) {
        val hops = LedTick.hopsAt(config.pattern, beat)
        val boardCols = maxOf(from.cols, to.cols)
        resize(boardCols)
        val total = LedTick.burstMs(hops, config.paceMs).toFloat()
        val start = withFrameNanos { it }
        var last = start
        while (true) {
            val now = withFrameNanos { it }
            val t = (now - start) / 1_000_000f
            val dt = (now - last) / 1_000_000f
            last = now
            val rows = LedTick.rowOffset(LedTick.progress(t, hops, config.paceMs))
            glow.step(LedFrames.frame(from, to, towardsDown, rows, boardCols), dt)
            version++
            if (t >= total) break
        }
        // Let the trail die away, then settle the board at its own width.
        val rest = LedFrames.place(to, boardCols, LedTick.ROWS, 0)
        var guard = 0
        while (glow.fading(rest) && guard++ < 60) {
            val now = withFrameNanos { it }
            glow.step(rest, (now - last) / 1_000_000f)
            last = now
            version++
        }
        target = to
        resize(to.cols)
        glow.settle(LedFrames.place(to, to.cols, LedTick.ROWS, 0))
        version++
    }
}

/**
 * The LED board of one slot. [face] is what the slot should show; [tickPeriodMs] is the glyph tick period (the beat and the
 * clock the pattern follows); [repeatBeat] says whether the board re-ticks the same face on every beat; [ready] holds the beat
 * back while the widget is still fading in.
 */
@Composable
internal fun LedTickFace(
    face: LedFace,
    config: LedTickConfig,
    towardsDown: Boolean,
    tickPeriodMs: Long,
    repeatBeat: Boolean,
    glyphScale: Float,
    startDelayMs: Long = 0L,
    ready: () -> Boolean = { true }
) {
    val context = LocalContext.current
    val faceNow by rememberUpdatedState(face)
    val configNow by rememberUpdatedState(config)
    val downNow by rememberUpdatedState(towardsDown)
    val periodNow by rememberUpdatedState(tickPeriodMs)
    val repeatNow by rememberUpdatedState(repeatBeat)
    val readyNow by rememberUpdatedState(ready)
    val delayNow by rememberUpdatedState(startDelayMs)

    val board = remember(config.fontId) { LedBoardState(ledGridOf(context, face, config.fontId)) }

    LaunchedEffect(board) {
        var current = faceNow
        var nextBeatAt = SystemClock.elapsedRealtime() + periodNow
        while (true) {
            val wait = if (repeatNow) (nextBeatAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L) else Long.MAX_VALUE
            val changed = withTimeoutOrNull(wait) { snapshotFlow { faceNow }.first { it != current } }
            val cfg = configNow
            val beat = LedTick.beatIndex(SystemClock.elapsedRealtime(), periodNow)
            if (changed != null) {
                if (delayNow > 0L) delay(delayNow)
                board.burst(ledGridOf(context, current, cfg.fontId), ledGridOf(context, changed, cfg.fontId), downNow, cfg, beat)
                current = changed
            } else if (repeatNow && readyNow()) {
                if (delayNow > 0L) delay(delayNow)
                val same = ledGridOf(context, current, cfg.fontId)
                board.burst(same, same, downNow, cfg, beat)
            }
            // The start delay is already part of what has passed, so it is taken off: the right slot keeps its half pace behind the
            // left one instead of falling further behind on every beat.
            nextBeatAt = SystemClock.elapsedRealtime() + periodNow - delayNow
        }
    }

    val color = ledColor(config.colorId)
    val pitch = LED_PITCH_DP * glyphScale
    Canvas(modifier = Modifier.size((board.cols * pitch).dp, (LedTick.ROWS * pitch).dp)) {
        @Suppress("UNUSED_VARIABLE")
        val redraw = board.version
        val glow = board.glow
        val cols = glow.cols
        val step = pitch.dp.toPx()
        val radius = step * 0.36f
        val level = glow.level
        for (i in level.indices) {
            val b = level[i]
            val center = Offset((i % cols + 0.5f) * step, (i / cols + 0.5f) * step)
            if (b > LedGlow.VISIBLE) {
                if (b > 0.6f) drawCircle(color = color.copy(alpha = 0.22f * b), radius = radius * 1.9f, center = center)
                drawCircle(color = color.copy(alpha = 0.35f + 0.65f * b), radius = radius, center = center)
            } else {
                drawCircle(color = color.copy(alpha = 0.10f), radius = radius, center = center)
            }
        }
    }
}

/**
 * A live sample of the LED style for the settings: a small black pill with a glyph board and a value board, stepping through a few
 * typical changes with the chosen pattern, pace, font, colour and stagger. A new minute or temperature steps the value alone; a new
 * widget steps both, the value half a pace after the glyph when the stagger is on.
 */
@Composable
internal fun LedTickPreview(config: LedTickConfig, modifier: Modifier = Modifier) {
    val pairs = remember {
        listOf(
            LedFace.Icon(LedIcon.Clock) to LedFace.Text("12:45"),
            LedFace.Icon(LedIcon.Clock) to LedFace.Text("12:46"),
            LedFace.Icon(LedIcon.Sun) to LedFace.Text("28\u00B0"),
            LedFace.Icon(LedIcon.Sun) to LedFace.Text("29\u00B0"),
            LedFace.Icon(LedIcon.Battery) to LedFace.Text("78%"),
            LedFace.Icon(LedIcon.Battery) to LedFace.Text("77%")
        )
    }
    var index by remember { mutableIntStateOf(0) }
    val longest = config.pattern.maxOrNull() ?: LedTick.MAX_HOPS
    val gap = LedTick.burstMs(longest, config.paceMs) + LedTick.staggerMs(config.paceMs, true, config.stagger) + 1400L
    LaunchedEffect(gap) {
        while (true) {
            delay(gap)
            index++
        }
    }
    val pair = pairs[index % pairs.size]
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black)
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.runtime.key(config.fontId) {
            LedTickFace(
                face = pair.first,
                config = config,
                towardsDown = false,
                tickPeriodMs = gap,
                repeatBeat = false,
                glyphScale = 2.4f,
                startDelayMs = LedTick.staggerMs(config.paceMs, false, config.stagger)
            )
            LedTickFace(
                face = pair.second,
                config = config,
                towardsDown = false,
                tickPeriodMs = gap,
                repeatBeat = false,
                glyphScale = 2.4f,
                startDelayMs = LedTick.staggerMs(config.paceMs, true, config.stagger)
            )
        }
    }
}
