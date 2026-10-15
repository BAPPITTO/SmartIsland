/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** Timer arithmetic matching the shade's chronometer, which truncates (4:59.9 shows "04:59"). No Android types. */
object TimerClock {
    private const val MAX_PLAUSIBLE_MS = 100L * 3600L * 1000L

    /** What a countdown chronometer prints for [remainingMs]: whole seconds, truncated. */
    fun secondsLeft(remainingMs: Long): Long = remainingMs.coerceAtLeast(0L) / 1000L

    /** Truncates for [exact] cards; other cards add [legacyBiasMs] (500 = round, 999 = round up). */
    fun displaySeconds(remainingMs: Long, exact: Boolean, legacyBiasMs: Long): Long =
        if (exact) secondsLeft(remainingMs) else ((remainingMs + legacyBiasMs) / 1000L).coerceAtLeast(0L)

    /** Time until the shown second changes, plus 5 ms so the tick lands after the boundary. */
    fun msToNextTick(remainingMs: Long): Long = (remainingMs.coerceAtLeast(0L) % 1000L) + 5L

    /** Deadline for a countdown read as whole seconds: the top of [sec, sec + 1) so it prints [sec] now. */
    fun targetFromShownSeconds(nowMs: Long, shownSec: Long): Long = nowMs + shownSec * 1000L + 999L

    /**
     * Value a paused countdown chronometer had when posted. Its base is `postElapsed + remaining`; the printed text
     * shrinks as the post ages, the base does not. Null if the printed text disagrees (the app uses the base differently).
     */
    fun frozenFromChronometer(baseElapsedMs: Long, postElapsedMs: Long, shownSec: Long?, applyDelayMs: Long): Long? {
        val corrected = baseElapsedMs - postElapsedMs
        if (corrected <= 0L || corrected > MAX_PLAUSIBLE_MS) return null
        if (shownSec != null) {
            val delay = applyDelayMs.coerceAtLeast(0L)
            val lo = shownSec * 1000L + delay - 1000L
            val hi = shownSec * 1000L + delay + 1999L
            if (corrected !in lo..hi) return null
        }
        return corrected
    }

    /**
     * Value a paused stopwatch had when posted. Its stopped count-up chronometer prints `elapsed + age of post`, so the
     * printed text grows; the base does not. Null if the printed text disagrees.
     */
    fun elapsedFromChronometer(baseElapsedMs: Long, postElapsedMs: Long, shownSec: Long?, applyDelayMs: Long): Long? {
        val corrected = postElapsedMs - baseElapsedMs
        if (corrected < 0L || corrected > MAX_PLAUSIBLE_MS) return null
        if (shownSec != null) {
            val delay = applyDelayMs.coerceAtLeast(0L)
            val lo = shownSec * 1000L - delay - 1000L
            val hi = shownSec * 1000L - delay + 1999L
            if (corrected !in lo..hi) return null
        }
        return corrected
    }

    /** Time until a count-up clock's shown second changes, plus 5 ms. */
    fun msToNextTickUp(elapsedMs: Long): Long = 1000L - (elapsedMs.coerceAtLeast(0L) % 1000L) + 5L
}
