/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurFollowTest {
    private val base = intArrayOf(100, 50, 800, 400, 60)

    @Test
    fun hiddenWindowAlwaysRelayouts() {
        assertTrue(BlurFollow.needsRelayout(null, base))
    }

    @Test
    fun sameRectIsSkipped() {
        assertFalse(BlurFollow.needsRelayout(base, base.copyOf()))
    }

    @Test
    fun driftWithinToleranceIsSkipped() {
        val next = intArrayOf(102, 52, 797, 403, 60)
        assertFalse(BlurFollow.needsRelayout(base, next))
    }

    @Test
    fun driftBeyondToleranceRelayouts() {
        val next = intArrayOf(100, 50, 800, 404, 60)
        assertTrue(BlurFollow.needsRelayout(base, next))
    }

    @Test
    fun radiusChangeAlwaysRelayouts() {
        val next = intArrayOf(100, 50, 800, 400, 61)
        assertTrue(BlurFollow.needsRelayout(base, next))
    }
}
