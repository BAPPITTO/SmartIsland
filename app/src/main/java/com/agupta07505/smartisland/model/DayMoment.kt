/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone

/**
 * The four day moments the Day moments idle widget marks with a glyph in the pill's left slot, at the phone's local time (the time zone of where
 * you are): sunrise at 06:00 and afternoon at 12:00 tick upwards, sunset at 17:30 and midnight at 23:00 tick downwards.
 * Pure rules; the drawing is ui/DayMoment.kt.
 */
enum class DayMoment(val id: String, val hour: Int, val minute: Int, val towardsDown: Boolean) {
    Sunrise("sunrise", 6, 0, false),
    Afternoon("afternoon", 12, 0, false),
    Sunset("sunset", 17, 30, true),
    Midnight("midnight", 23, 0, true)
}

object DayMoments {
    /** Fade of the pill's content out and in around the glyph (ms). */
    const val FADE_IN_MS = 160
    /** The previous moment's glyph rests this long (ms) before the tick, so it is seen (like "59" in the hour tick). */
    const val HOLD_BEFORE_MS = 600
    /** The tick itself (ms), with its slow suspended pull-back ([IdleSlots.heavyTickProgress]). */
    const val TICK_MS = 900
    /** The new glyph rests this long (ms) before the pill's own content comes back. */
    const val HOLD_AFTER_MS = 1700
    const val FADE_OUT_MS = 260

    /** The moment before [moment] in the day's cycle (sunrise follows midnight): its glyph is what ticks away. */
    fun previous(moment: DayMoment): DayMoment {
        val all = DayMoment.values()
        return all[Math.floorMod(moment.ordinal - 1, all.size)]
    }

    /** A moment on a full hour that is also an hour tick waits this long (ms) for the hour tick to finish first. */
    const val AFTER_HOUR_TICK_MS = 5000L

    /** When [moment] happens on the day [dayOffset] days after the day of [nowMs] in [zone]. */
    fun atMs(moment: DayMoment, nowMs: Long, dayOffset: Int = 0, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, moment.hour)
        cal.set(Calendar.MINUTE, moment.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** The first moment strictly after [nowMs] and when it happens (today's, or tomorrow's once today's four have passed). */
    fun next(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Pair<DayMoment, Long> =
        DayMoment.values()
            .flatMap { m -> listOf(0, 1).map { d -> m to atMs(m, nowMs, d, zone) } }
            .filter { it.second > nowMs }
            .minByOrNull { it.second }!!
}
