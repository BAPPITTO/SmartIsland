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

class ChipAvoidanceTest {
    @Test
    fun noChipWhenNothingIsInUse() {
        assertFalse(ChipAvoidance.chipShowing(PrivacyIndicatorState(), emptyList()))
    }

    @Test
    fun aSensorInUseCountsAsAChip() {
        val state = PrivacyIndicatorState(active = setOf(PrivacyKind.Camera))
        assertTrue(ChipAvoidance.chipShowing(state, emptyList()))
    }

    @Test
    fun portraitFlipsTheSideOnlyWhileAChipShows() {
        assertFalse(ChipAvoidance.circleOnLeft(settingIsLeft = false, chipShowing = false, isLandscape = false))
        assertTrue(ChipAvoidance.circleOnLeft(settingIsLeft = false, chipShowing = true, isLandscape = false))
        assertTrue(ChipAvoidance.circleOnLeft(settingIsLeft = true, chipShowing = false, isLandscape = false))
        assertFalse(ChipAvoidance.circleOnLeft(settingIsLeft = true, chipShowing = true, isLandscape = false))
    }

    @Test
    fun landscapeNeverFlips() {
        assertFalse(ChipAvoidance.circleOnLeft(settingIsLeft = false, chipShowing = true, isLandscape = true))
        assertTrue(ChipAvoidance.circleOnLeft(settingIsLeft = true, chipShowing = true, isLandscape = true))
    }
}
