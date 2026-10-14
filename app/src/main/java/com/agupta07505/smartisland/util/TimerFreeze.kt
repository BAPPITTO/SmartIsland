/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * What a paused clock-app timer has left. No Android types, so it is unit-testable.
 *
 * Clock apps cancel the timer notification when their screen opens and post a paused one when it is left, and that post
 * often prints the timer's full length ("30:00"). So once the island has frozen a value for a pause, that value wins over
 * everything else until the timer runs again.
 */
object TimerFreeze {
    /**
     * @param rememberedFrozenMs the value frozen earlier for this same pause (kept per app, survives the card's removal)
     * @param runningTargetMs the deadline the island saw while the timer was running, if any
     * @param keptFrozenMs the frozen value still held on the current card
     * @param textRemainingMs the time read from the notification text, which may be the full length
     */
    fun remainingMs(
        nowMs: Long,
        rememberedFrozenMs: Long?,
        runningTargetMs: Long?,
        keptFrozenMs: Long?,
        textRemainingMs: Long?
    ): Long = when {
        rememberedFrozenMs != null -> rememberedFrozenMs
        runningTargetMs != null && runningTargetMs > nowMs -> runningTargetMs - nowMs
        keptFrozenMs != null -> keptFrozenMs
        textRemainingMs != null && textRemainingMs > 0L -> textRemainingMs
        else -> 0L
    }
}
