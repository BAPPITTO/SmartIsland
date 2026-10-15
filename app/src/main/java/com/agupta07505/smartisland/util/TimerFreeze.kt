/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * What a paused clock-app timer has left. No Android types.
 * Clock apps re-post a paused timer showing its full length, so a value frozen by the island wins until the timer runs again.
 */
object TimerFreeze {
    /** Whole-second shade text jitters; a reading this close to the remembered value is the same pause. */
    private const val SHADE_TOLERANCE_MS = 2_000L

    /** Priority order is the order of the branches below; [shadeExactMs] is [TimerClock.frozenFromChronometer]. */
    fun remainingMs(
        nowMs: Long,
        rememberedFrozenMs: Long?,
        runningTargetMs: Long?,
        keptFrozenMs: Long?,
        textRemainingMs: Long?,
        shadeShownMs: Long? = null,
        shadeExactMs: Long? = null
    ): Long = when {
        // Exact to the ms: no tolerance, no memory needed.
        shadeExactMs != null && shadeExactMs > 0L -> shadeExactMs
        // Also catches a resume + pause inside the clock app that the island never saw.
        shadeShownMs != null && shadeShownMs > 0L &&
            (rememberedFrozenMs == null || kotlin.math.abs(shadeShownMs - rememberedFrozenMs) > SHADE_TOLERANCE_MS) ->
            shadeShownMs
        rememberedFrozenMs != null -> rememberedFrozenMs
        runningTargetMs != null && runningTargetMs > nowMs -> runningTargetMs - nowMs
        keptFrozenMs != null -> keptFrozenMs
        textRemainingMs != null && textRemainingMs > 0L -> textRemainingMs
        else -> 0L
    }
}
