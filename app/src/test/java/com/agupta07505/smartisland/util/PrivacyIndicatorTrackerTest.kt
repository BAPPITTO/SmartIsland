/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.PrivacyKind
import com.agupta07505.smartisland.model.hasSecondaryCutout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyIndicatorTrackerTest {

    @Test
    fun screenSharingTurnsOnAndOff() {
        val t = PrivacyIndicatorTracker()
        assertFalse(t.state.value.hasAnything)
        t.setMediaProjectionActive(true)
        assertTrue(PrivacyKind.ScreenShare in t.state.value.active)
        assertTrue(t.state.value.hasAnything)
        t.setMediaProjectionActive(false)
        assertFalse(t.state.value.hasAnything)
    }

    @Test
    fun clearDropsScreenSharing() {
        val t = PrivacyIndicatorTracker()
        t.setMediaProjectionActive(true)
        t.clear()
        assertTrue(t.state.value.active.isEmpty())
    }

    @Test
    fun eachKindIsTrackedOnItsOwn() {
        val t = PrivacyIndicatorTracker()
        t.setActive(PrivacyKind.Camera, true)
        t.setActive(PrivacyKind.Microphone, true)
        assertEquals(setOf(PrivacyKind.Camera, PrivacyKind.Microphone), t.state.value.active)
        t.clear(PrivacyKind.Camera)
        assertEquals(setOf(PrivacyKind.Microphone), t.state.value.active)
    }

    @Test
    fun castBubbleTakesTheSecondaryCutoutWithOrWithoutASecondCard() {
        assertTrue(hasSecondaryCutout(notificationCount = 0, screenShareActive = true, notchMode = false))
        assertTrue(hasSecondaryCutout(notificationCount = 1, screenShareActive = true, notchMode = false))
        assertTrue(hasSecondaryCutout(notificationCount = 2, screenShareActive = false, notchMode = false))
        assertFalse(hasSecondaryCutout(notificationCount = 1, screenShareActive = false, notchMode = false))
        assertFalse(hasSecondaryCutout(notificationCount = 2, screenShareActive = true, notchMode = true))
    }
}
