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

class PrivacyIndicatorTest {
    private fun card(key: String, mode: IslandMode) = IslandNotification(
        key = key,
        packageName = "app.$key",
        appName = key,
        title = key,
        text = "",
        timeMillis = 0L,
        mode = mode
    )

    @Test
    fun screenSharingShowsWithoutARecording() {
        assertTrue(screenShareIndicatorActive(true, listOf(card("n", IslandMode.Notification))))
    }

    @Test
    fun screenRecordingIsNotShownAsScreenSharing() {
        assertFalse(screenShareIndicatorActive(true, listOf(card("r", IslandMode.ScreenRecording))))
    }

    @Test
    fun noProjectionNeverShows() {
        assertFalse(screenShareIndicatorActive(false, emptyList()))
    }
}

class PrivacyPillDotsTest {
    @org.junit.Test
    fun dotsFollowKindOrderAndAreCapped() {
        val dots = pillDotKinds(setOf(PrivacyKind.Location, PrivacyKind.Camera))
        org.junit.Assert.assertEquals(listOf(PrivacyKind.Camera, PrivacyKind.Location), dots)
        org.junit.Assert.assertTrue(pillDotKinds(PrivacyKind.entries.toSet()).size <= PILL_DOTS_MAX)
        org.junit.Assert.assertTrue(pillDotKinds(emptySet()).isEmpty())
    }

    @org.junit.Test
    fun bubbleShownForAnyKind() {
        org.junit.Assert.assertTrue(PrivacyIndicatorState(shown = PrivacyKind.Microphone).bubbleShown)
        org.junit.Assert.assertFalse(PrivacyIndicatorState(active = setOf(PrivacyKind.Camera)).bubbleShown)
    }

    @org.junit.Test
    fun mergingStateKeepsTheBubbleButIsNotShown() {
        val s = PrivacyIndicatorState(active = setOf(PrivacyKind.Camera), leaving = PrivacyKind.Camera)
        org.junit.Assert.assertTrue(s.bubbleShown)
        org.junit.Assert.assertTrue(s.merging)
        org.junit.Assert.assertTrue(s.hasAnything)
        org.junit.Assert.assertFalse(PrivacyIndicatorState(shown = PrivacyKind.Camera).merging)
    }
}
