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

    @Test
    fun cornerInsetKeepsTheCornerInsideTheArc() {
        // r = 34 px: inset = ceil(34 * 0.2929) = 10; the corner (10, 10) is 24 * sqrt(2) = 33.9 <= 34 from the arc centre.
        val inset = BlurFollow.cornerInsetPx(800, 400, 34f)
        assertTrue(inset == 10)
        val d = (34 - inset) * kotlin.math.sqrt(2.0)
        assertTrue(d <= 34.0)
    }

    @Test
    fun cornerInsetNeverUsesMoreThanHalfTheShorterSide() {
        // A 40 px tall island cannot have a 34 px radius: the radius is 20 px, so the inset is ceil(20 * 0.2929) = 6.
        assertTrue(BlurFollow.cornerInsetPx(300, 40, 34f) == 6)
    }

    @Test
    fun layoutKeyInsetsTheRectAndKeepsTheBlurRadiusLast() {
        val key = BlurFollow.layoutKey(100, 50, 900, 450, 34f, 60)!!
        assertTrue(key.contentEquals(intArrayOf(110, 60, 780, 380, 60)))
    }

    @Test
    fun layoutKeyIsNullWhenTheIslandIsStillAPill() {
        assertTrue(BlurFollow.layoutKey(0, 0, 30, 20, 34f, 60) == null)
    }

    @Test
    fun layoutKeyIsNullWithoutABlurRadiusOrArea() {
        assertTrue(BlurFollow.layoutKey(100, 50, 900, 450, 34f, 0) == null)
        assertTrue(BlurFollow.layoutKey(100, 50, 100, 450, 34f, 60) == null)
    }

    @Test
    fun tintKeepsAShareOfTheOpacityWithinBounds() {
        assertTrue(kotlin.math.abs(BlurFollow.tintAlpha(1f) - 0.6f) < 1e-6f)
        assertTrue(kotlin.math.abs(BlurFollow.tintAlpha(0.5f) - 0.3f) < 1e-6f)
    }

    @Test
    fun tintIsNeverMoreSolidThanTheShellItself() {
        // A shell that is already see-through is left alone, even below the tint floor.
        assertTrue(kotlin.math.abs(BlurFollow.tintAlpha(0.2f) - 0.2f) < 1e-6f)
        assertTrue(BlurFollow.tintAlpha(0f) == 0f)
    }

    @Test
    fun shellAlphaEasesBetweenSolidAndTint() {
        assertTrue(BlurFollow.shellAlpha(1f, 0f) == 1f)
        assertTrue(kotlin.math.abs(BlurFollow.shellAlpha(1f, 1f) - 0.6f) < 1e-6f)
        assertTrue(kotlin.math.abs(BlurFollow.shellAlpha(1f, 0.5f) - 0.8f) < 1e-6f)
        // A fraction outside 0..1 is clamped.
        assertTrue(BlurFollow.shellAlpha(1f, 2f) == BlurFollow.shellAlpha(1f, 1f))
    }
}
