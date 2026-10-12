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
 * The clock idle widget does not show the time (the status bar already does). Once an hour, on the full hour, the pill shows the hour in its
 * left slot and the minutes in its right slot ticking over: the old hour ticks away downwards and the new hour ticks in from above, the minutes
 * "59" tick away upwards and "00" ticks in from below. Pure rules here; the drawing is ui/HourChime.kt.
 */
object HourChime {
    const val MINUTES_BEFORE = "59"
    const val MINUTES_AFTER = "00"

    /** The pill shows the old time for this long (ms) before it ticks over, so "59" is seen. */
    const val HOLD_BEFORE_MS = 280
    /** The tick itself (ms), including its slow suspended pull-back ([IdleSlots.heavyTickProgress]). */
    const val TICK_MS = 900
    /** The new time stays this long (ms) after the tick, then the pill goes back to what it showed. */
    const val HOLD_AFTER_MS = 1700
    /** Fade of the chime in and out (ms). */
    const val FADE_IN_MS = 160
    const val FADE_OUT_MS = 260

    /** The next full hour strictly after [nowMs], in [zone] (right for half-hour zones and daylight saving). */
    fun nextFullHourMs(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, 1)
        return cal.timeInMillis
    }

    /** The hour of day (0..23) at [ms] in [zone]. */
    fun hourOf(ms: Long, zone: TimeZone = TimeZone.getDefault()): Int =
        Calendar.getInstance(zone).also { it.timeInMillis = ms }.get(Calendar.HOUR_OF_DAY)

    /** The hour before [hour] (0..23): the one that ticks away. */
    fun previousHour(hour: Int): Int = ((hour % 24) + 23) % 24

    /** How the hour reads: two digits in 24 hour mode ("01", "13"), as on a 12 hour clock otherwise ("1", "12"). */
    fun hourLabel(hour: Int, is24: Boolean): String {
        val h = ((hour % 24) + 24) % 24
        return if (is24) "%02d".format(h) else (if (h % 12 == 0) 12 else h % 12).toString()
    }
}

/** What the chime shows: the hour that ticks away and the one that ticks in. */
class HourChimeLabels(val oldHour: String, val newHour: String)

fun hourChimeLabels(newHour: Int, is24: Boolean): HourChimeLabels =
    HourChimeLabels(HourChime.hourLabel(HourChime.previousHour(newHour), is24), HourChime.hourLabel(newHour, is24))
