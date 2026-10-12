/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenShareGateTest {
    @Test
    fun realShareShowsAfterTheStartDelay() {
        val g = ScreenShareGate()
        assertFalse(g.shouldShow(true, false, 0))
        assertFalse(g.shouldShow(true, false, 1_999))
        assertTrue(g.shouldShow(true, false, 2_000))
    }

    @Test
    fun recorderCardArrivingLateNeverFlashesTheIcon() {
        val g = ScreenShareGate()
        assertFalse(g.shouldShow(true, false, 0))
        assertFalse(g.shouldShow(true, false, 1_000))
        assertFalse(g.shouldShow(true, true, 1_500))
        assertFalse(g.shouldShow(true, true, 60_000))
    }

    @Test
    fun recorderCardGoneWhileProjectionLingersStaysHidden() {
        val g = ScreenShareGate()
        g.shouldShow(true, true, 0)
        assertFalse(g.shouldShow(true, false, 5_000))
        assertFalse(g.shouldShow(true, false, 7_999))
    }

    @Test
    fun projectionEndingResetsTheDelay() {
        val g = ScreenShareGate()
        g.shouldShow(true, false, 0)
        assertTrue(g.shouldShow(true, false, 5_000))
        assertFalse(g.shouldShow(false, false, 6_000))
        assertFalse(g.shouldShow(true, false, 7_000))
    }
}
