/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerFreezeTest {
    private val min = 60_000L

    @Test
    fun firstPauseUsesWhatWasLeftOnTheRunningDeadline() {
        assertEquals(12 * min, TimerFreeze.remainingMs(1_000L, null, 1_000L + 12 * min, null, 30 * min))
    }

    @Test
    fun repostShowingTheFullLengthKeepsTheFrozenTime() {
        // Paused at 12:00 left, clock app open for 5 minutes, repost prints 30:00.
        assertEquals(12 * min, TimerFreeze.remainingMs(400_000L, 12 * min, 1_000L + 12 * min, null, 30 * min))
    }

    @Test
    fun staleRunningDeadlineDoesNotShrinkAFrozenTime() {
        assertEquals(12 * min, TimerFreeze.remainingMs(100_000L, 12 * min, 100_000L + 3 * min, null, null))
    }

    @Test
    fun cardRemovedAndNothingRememberedFallsBackToKeptThenText() {
        assertEquals(7 * min, TimerFreeze.remainingMs(0L, null, null, 7 * min, 30 * min))
        assertEquals(30 * min, TimerFreeze.remainingMs(0L, null, null, null, 30 * min))
        assertEquals(0L, TimerFreeze.remainingMs(0L, null, null, null, null))
    }

    @Test
    fun exactChronometerReadingWinsOverEverything() {
        assertEquals(
            12 * min + 250L,
            TimerFreeze.remainingMs(
                nowMs = 1_000L, rememberedFrozenMs = 11 * min, runningTargetMs = 1_000L + 10 * min,
                keptFrozenMs = 9 * min, textRemainingMs = 30 * min, shadeShownMs = 12 * min,
                shadeExactMs = 12 * min + 250L
            )
        )
    }

    @Test
    fun plusOneMinuteWhilePausedIsTakenFromTheShade() {
        // Remembered 12:00, the user pressed +1:00 in the shade, the stopped chronometer now reads 13:00.
        assertEquals(
            13 * min,
            TimerFreeze.remainingMs(0L, 12 * min, null, null, null, shadeExactMs = 13 * min)
        )
    }
}
