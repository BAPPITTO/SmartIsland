/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Locale

/**
 * The texts of the collapsed pill's two glyph slots (left and right) for the idle widgets that put text in them. Pure rules, so they can be unit tested;
 * the drawing is IdleWidgetSlot in ui/IdleWidgets.kt.
 *
 *  - date: day on the left, month on the right;
 *  - weather: the condition's glyph (white) on the left, the temperature on the right, both tick when either changes;
 *  - battery, while charging and the phone reports its current: wattage on the left, battery temperature on the right;
 *  - next alarm: the alarm glyph and the alarm hour alternate on the left every second, the minutes on the right;
 *  - net speed: upload on the left, download on the right;
 *  - notification count and mode: one glyph on the left (the count badge, the active mode's icon), the right slot stays empty.
 */
object IdleSlots {
    /** Glyph size choices (percent of the normal size) in Layout > Idle widgets; the stored value is kept within [MIN_GLYPH_SCALE]..[MAX_GLYPH_SCALE]. */
    val GLYPH_SCALES = listOf(85, 100, 115, 130)
    const val DEFAULT_GLYPH_SCALE = 100
    const val MIN_GLYPH_SCALE = 70
    const val MAX_GLYPH_SCALE = 150

    fun normalizeGlyphScale(percent: Int): Int = percent.coerceIn(MIN_GLYPH_SCALE, MAX_GLYPH_SCALE)

    /** The date widget's day is a number with two digits ("05") on the left; the month is its standard abbreviation ("Feb") on the right. */
    const val DAY_PATTERN = "dd"
    const val MONTH_PATTERN = "MMM"

    /** The alarm glyph shows on even ticks, the alarm hour on odd ones (one tick per second). */
    fun alarmShowsHour(tick: Int): Boolean = Math.floorMod(tick, 2) == 1

    /** The alarm's hour as the system clock writes it: "07" in 24 h, "7" in 12 h ("12" at noon and midnight). */
    fun alarmHourText(hour24: Int, is24: Boolean): String {
        val h = Math.floorMod(hour24, 24)
        if (is24) return String.format(Locale.US, "%02d", h)
        val h12 = h % 12
        return (if (h12 == 0) 12 else h12).toString()
    }

    fun alarmMinuteText(minute: Int): String = String.format(Locale.US, "%02d", Math.floorMod(minute, 60))

    /** Charging power for the pill ("8.4W", "18W"): short, a slot is only a third of the pill. */
    fun shortWatts(watts: Double): String {
        val tenth = Math.round(watts * 10.0) / 10.0
        return if (tenth >= 10.0) "${Math.round(watts)}W" else String.format(Locale.US, "%.1fW", tenth)
    }

    /** Battery temperature (tenths of a degree Celsius) for the pill ("36°"), in the chosen unit. */
    fun shortBatteryTemp(tenthsC: Int, fahrenheit: Boolean): String = IdleWidgets.displayTemp(tenthsC / 10f, fahrenheit)

    /** The battery shows wattage and temperature on the pill only while charging with a readable current; otherwise it keeps its icon and percent. */
    fun batteryShowsPower(charging: Boolean, watts: Double?): Boolean = charging && watts != null

    /** Range (seconds) of the self-ticking glyph timer slider; the stored value is kept within it. */
    const val MIN_TICK_SECONDS = 1.5f
    const val MAX_TICK_SECONDS = 10f
    const val DEFAULT_TICK_SECONDS = 2.5f

    fun normalizeTickSeconds(seconds: Float): Float =
        (if (seconds.isNaN()) DEFAULT_TICK_SECONDS else seconds).coerceIn(MIN_TICK_SECONDS, MAX_TICK_SECONDS)

    /** The tick period in ms for a stored value in seconds (rounded to the half second the slider moves in). */
    fun tickPeriodMs(seconds: Float): Long = (Math.round(normalizeTickSeconds(seconds) * 2f) * 500L)

    /** The idle glyphs tick left to right (out on the right, the same glyph back in from the left) once every this many ms. */
    const val SIDE_TICK_PERIOD_MS = 2500L

    /** The automatic widget switch travels in this many ticks: the old glyphs step out to the right, the next ones step in from the left. */
    const val SWITCH_TICK_STEPS = 3

    /** Net speed below this (bytes per second) is too quiet to count as one side exceeding the other. */
    const val SPEED_DOMINANCE_MIN_BPS = 1_000L

    /** Which side of the net speed widget is faster right now; that side's slot ticks to its arrow instead of the sideways tick. */
    enum class SpeedLead { None, Upload, Download }

    fun speedLead(downBytesPerSecond: Long, upBytesPerSecond: Long): SpeedLead = when {
        maxOf(downBytesPerSecond, upBytesPerSecond) < SPEED_DOMINANCE_MIN_BPS -> SpeedLead.None
        upBytesPerSecond > downBytesPerSecond -> SpeedLead.Upload
        downBytesPerSecond > upBytesPerSecond -> SpeedLead.Download
        else -> SpeedLead.None
    }

    /** The slot that carries the arrow when [lead] leads: upload sits on the left, download on the right. */
    fun speedLeadOwnsSlot(lead: SpeedLead, right: Boolean): Boolean =
        (lead == SpeedLead.Upload && !right) || (lead == SpeedLead.Download && right)

    /**
     * Whether a slot takes the sideways tick on the 2.5 s beat. The alarm's glyph and hour slot already tick by themselves and never take it
     * (nor does the clock, which has no slot); the weather slots only skip the beat while their own change tick is running ([ownTickRunning]);
     * the net slot that is carrying an arrow skips it too.
     */
    fun takesSideTick(kind: IdleWidgetKind, right: Boolean, ownTickRunning: Boolean, lead: SpeedLead): Boolean = when {
        ownTickRunning -> false
        kind == IdleWidgetKind.Clock || kind == IdleWidgetKind.DayMoments -> false
        // Notifications and modes use the left slot for their glyph; neither should run a sideways tick in the right slot.
        // The notification count is shown on the right, but stays static while the app glyph takes its tick.
        (kind == IdleWidgetKind.Notifications || kind == IdleWidgetKind.Modes) && right -> false
        kind == IdleWidgetKind.NextAlarm && !right -> false
        kind == IdleWidgetKind.NetSpeed && speedLeadOwnsSlot(lead, right) -> false
        else -> true
    }

    /** Share of a heavy tick spent on the slow, suspended pull-back before the tick itself, and how far back it goes (share of the travel). */
    const val HEAVY_WINDUP_SHARE = 0.4f
    const val HEAVY_WINDUP_DEPTH = 0.12f

    /**
     * Easing of every tick except the automatic switch's three ticks: a clock's heavy tick. The first [HEAVY_WINDUP_SHARE] is a slow, suspended
     * move a little backwards (to -[HEAVY_WINDUP_DEPTH]), then the tick snaps through to 1. Goes below 0 on purpose; clamp it where it drives an alpha.
     */
    fun heavyTickProgress(fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        if (f >= 1f) return 1f
        if (f < HEAVY_WINDUP_SHARE) {
            val u = f / HEAVY_WINDUP_SHARE
            return -HEAVY_WINDUP_DEPTH * (u * u * (3f - 2f * u))
        }
        val u = (f - HEAVY_WINDUP_SHARE) / (1f - HEAVY_WINDUP_SHARE)
        val fast = 1f - (1f - u) * (1f - u) * (1f - u)
        return -HEAVY_WINDUP_DEPTH + (1f + HEAVY_WINDUP_DEPTH) * fast
    }

    /**
     * Easing for the ticking travel: the move from 0 to 1 happens in [steps] short hops with a rest between them.
     * Each step spends its first [HOP_SHARE] on the hop (ease-out) and holds the rest.
     */
    private const val HOP_SHARE = 0.45f

    fun tickProgress(fraction: Float, steps: Int = SWITCH_TICK_STEPS): Float {
        val f = fraction.coerceIn(0f, 1f)
        if (f >= 1f) return 1f
        val n = steps.coerceAtLeast(1)
        val scaled = f * n
        val step = scaled.toInt().coerceAtMost(n - 1)
        val local = scaled - step
        val hop = (local / HOP_SHARE).coerceIn(0f, 1f)
        val eased = 1f - (1f - hop) * (1f - hop)
        return (step + eased) / n
    }
}
