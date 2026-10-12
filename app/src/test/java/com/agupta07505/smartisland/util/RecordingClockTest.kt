/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingClockTest {

    @Test
    fun parsesMinutesSecondsAndHours() {
        assertEquals(26_000L, RecordingClock.parseElapsedMs("Currently recording 00:26"))
        assertEquals(3_723_000L, RecordingClock.parseElapsedMs("1:02:03"))
        assertNull(RecordingClock.parseElapsedMs("Recorder", null, "no time here"))
    }

    @Test
    fun pausedWhenResumeIsOfferedOrTextSaysPaused() {
        assertTrue(RecordingClock.isPaused(listOf("Resume recording", "Save recording"), "Recording paused 00:30"))
        assertTrue(RecordingClock.isPaused(emptyList(), "Recording paused"))
        assertFalse(RecordingClock.isPaused(listOf("Pause recording", "Save recording"), "Currently recording 00:26"))
    }

    @Test
    fun runningClockFollowsTheRecordersOwnTimeWithoutJitter() {
        val first = RecordingClock.next(null, 100_000L, paused = false, finished = false, parsedMs = 26_000L, chronometerBaseMs = null)
        val second = RecordingClock.next(first, 101_050L, paused = false, finished = false, parsedMs = 27_000L, chronometerBaseMs = null)
        assertEquals(first.startMillis, second.startMillis)
        assertNull(second.frozenMs)
    }

    @Test
    fun pauseFreezesAndResumeContinuesFromTheFrozenValue() {
        val running = RecordingClock.next(null, 0L, paused = false, finished = false, parsedMs = 30_000L, chronometerBaseMs = null)
        val paused = RecordingClock.next(running, 10_000L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(40_000L, paused.frozenMs)
        // Still frozen much later.
        val stillPaused = RecordingClock.next(paused, 90_000L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(40_000L, stillPaused.elapsedMs(500_000L))
        val resumed = RecordingClock.next(stillPaused, 100_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertNull(resumed.frozenMs)
        assertEquals(45_000L, resumed.elapsedMs(105_000L))
    }

    @Test
    fun finishedRecordingNeverTicks() {
        val running = RecordingClock.next(null, 0L, paused = false, finished = false, parsedMs = 12_000L, chronometerBaseMs = null)
        val done = RecordingClock.next(running, 3_000L, paused = false, finished = true, parsedMs = null, chronometerBaseMs = null)
        assertEquals(15_000L, done.elapsedMs(1_000_000L))
    }

    @Test
    fun stopwatchPausedWithoutAnyReadableTimeKeepsTheTimeItHadWhenPaused() {
        // Running, started 4 s before the pause post; the app shows a chronometer (no time text).
        val running = RecordingClock.next(null, 104_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 100_000L)
        // Paused post carries no time at all.
        val paused = RecordingClock.next(running, 104_050L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertEquals(4_050L, paused.frozenMs)
        // However much later it is read (collapse / expand), it does not move.
        assertEquals(4_050L, paused.elapsedMs(104_050L))
        assertEquals(4_050L, paused.elapsedMs(160_000L))
        // Resume without readable time continues from the frozen value.
        val resumed = RecordingClock.next(paused, 200_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = null)
        assertNull(resumed.frozenMs)
        assertEquals(4_050L, resumed.elapsedMs(200_000L))
        assertEquals(9_050L, resumed.elapsedMs(205_000L))
    }

    @Test
    fun pausedPostWithBogusZeroKeepsTheRunningTime() {
        val running = RecordingClock.next(null, 104_000L, paused = false, finished = false, parsedMs = null, chronometerBaseMs = 100_000L)
        val paused = RecordingClock.next(running, 104_050L, paused = true, finished = false, parsedMs = 0L, chronometerBaseMs = null)
        assertEquals(4_050L, paused.frozenMs)
    }

    @Test
    fun pausedPostWithNoHistoryUsesTheViewsOwnChronometer() {
        val paused = RecordingClock.next(null, 50_000L, paused = true, finished = false, parsedMs = null, chronometerBaseMs = null, fallbackFrozenMs = 7_000L)
        assertEquals(7_000L, paused.frozenMs)
    }
}
