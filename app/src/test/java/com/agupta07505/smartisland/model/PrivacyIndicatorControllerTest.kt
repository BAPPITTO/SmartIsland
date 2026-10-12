/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrivacyIndicatorControllerTest {
    private val cam = PrivacyKind.Camera
    private val mic = PrivacyKind.Microphone
    private val loc = PrivacyKind.Location
    private val share = PrivacyKind.ScreenShare

    @Test
    fun showsOnStartAndLetsGoAfterTwoSecondsEvenIfStillInUse() {
        val c = PrivacyIndicatorController()
        assertEquals(cam, c.update(setOf(cam), 0).shown)
        assertEquals(cam, c.update(setOf(cam), 1_999).shown)
        val after = c.update(setOf(cam), 2_000)
        assertNull(after.shown)
        assertEquals(setOf(cam), after.active)   // still in use: the pill keeps its dot
    }

    @Test
    fun staysOnWithoutShowingAgainUntilItStopsAndStartsAgain() {
        val c = PrivacyIndicatorController()
        c.update(setOf(cam), 0)
        assertNull(c.update(setOf(cam), 5_000).shown)
        assertNull(c.update(setOf(cam), 9_000).shown)
        c.update(emptySet(), 10_000)
        assertEquals(cam, c.update(setOf(cam), 11_000).shown)
    }

    @Test
    fun severalStartingTogetherTakeTurnsInPriorityOrder() {
        val c = PrivacyIndicatorController()
        val all = setOf(share, loc, mic, cam)
        assertEquals(cam, c.update(all, 0).shown)
        assertEquals(cam, c.update(all, 1_000).shown)
        assertEquals(mic, c.update(all, 2_000).shown)
        assertEquals(loc, c.update(all, 4_000).shown)
        assertEquals(share, c.update(all, 6_000).shown)
        assertNull(c.update(all, 8_000).shown)
    }

    @Test
    fun aHigherPriorityNewcomerDoesNotCutTheCurrentOneShort() {
        val c = PrivacyIndicatorController()
        assertEquals(loc, c.update(setOf(loc), 0).shown)
        assertEquals(loc, c.update(setOf(loc, cam), 500).shown)
        assertEquals(cam, c.update(setOf(loc, cam), 2_000).shown)
    }

    @Test
    fun aQueuedOneThatStopsBeforeItsTurnIsDropped() {
        val c = PrivacyIndicatorController()
        c.update(setOf(cam, mic), 0)
        c.update(setOf(cam), 1_000)                 // mic stopped while waiting
        assertNull(c.update(setOf(cam), 2_000).shown)
    }

    @Test
    fun theShowingOneFinishesItsTwoSecondsEvenIfItStopsEarly() {
        val c = PrivacyIndicatorController()
        c.update(setOf(cam), 0)
        assertEquals(cam, c.update(emptySet(), 500).shown)
        assertNull(c.update(emptySet(), 2_000).shown)
    }

    @Test
    fun screenSharingFollowsTheSameRules() {
        val c = PrivacyIndicatorController()
        assertEquals(share, c.update(setOf(share), 0).shown)
        assertNull(c.update(setOf(share), 2_000).shown)
    }

    @Test
    fun afterTheHoldTheCutoutMergesAwayBeforeItIsReleased() {
        val c = PrivacyIndicatorController()
        c.update(setOf(cam), 0)
        val hold = c.update(setOf(cam), 1_999)
        assertEquals(cam, hold.shown)
        assertEquals(false, hold.merging)
        val merging = c.update(setOf(cam), 2_000)
        assertNull(merging.shown)
        assertEquals(cam, merging.leaving)
        assertEquals(true, merging.merging)
        assertEquals(true, merging.bubbleShown)
        assertEquals(cam, merging.bubbleKind)
        val still = c.update(setOf(cam), 2_000 + PrivacyIndicatorController.MERGE_MS - 1)
        assertEquals(cam, still.leaving)
        val gone = c.update(setOf(cam), 2_000 + PrivacyIndicatorController.MERGE_MS)
        assertNull(gone.leaving)
        assertEquals(false, gone.bubbleShown)
        assertEquals(setOf(cam), gone.active)
    }

    @Test
    fun nextQueuedIndicatorTakesOverWithoutAMergeTail() {
        val c = PrivacyIndicatorController()
        val both = setOf(cam, mic)
        c.update(both, 0)
        val next = c.update(both, 2_000)
        assertEquals(mic, next.shown)
        assertNull(next.leaving)
    }

    @Test
    fun resetClearsTheMergeTail() {
        val c = PrivacyIndicatorController()
        c.update(setOf(cam), 0)
        c.update(setOf(cam), 2_000)
        c.reset()
        assertNull(c.update(emptySet(), 2_100).leaving)
    }
}
