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

class CameraUseStateTest {
    @Test
    fun anOpenCameraShowsWhenUnlocked() {
        val s = CameraUseState()
        s.onUnavailable("1", locked = false)
        assertTrue(s.shown(locked = false))
        s.onAvailable("1")
        assertFalse(s.shown(locked = false))
    }

    @Test
    fun nothingShowsWhileTheKeyguardIsUp() {
        val s = CameraUseState()
        s.onUnavailable("1", locked = true)
        assertFalse(s.shown(locked = true))
    }

    @Test
    fun faceUnlockCameraStaysHiddenJustAfterTheUnlockUntilItIsReleased() {
        val s = CameraUseState()
        s.onUnavailable("1", locked = true)   // face recognition opens the front camera under the keyguard
        assertFalse(s.shown(locked = false))  // unlocked, camera not released yet
        s.onAvailable("1")
        assertFalse(s.shown(locked = false))
        s.onUnavailable("1", locked = false)  // a real camera app opens it afterwards
        assertTrue(s.shown(locked = false))
    }

    @Test
    fun aCameraOpenedBeforeTheLockShowsAgainAfterTheUnlock() {
        val s = CameraUseState()
        s.onUnavailable("0", locked = false)
        assertFalse(s.shown(locked = true))
        assertTrue(s.shown(locked = false))
    }

    @Test
    fun aLitTorchIsNeverACamera() {
        val s = CameraUseState()
        s.onUnavailable("0", locked = false)
        s.onTorch("0", true)
        assertFalse(s.shown(locked = false))
        s.onTorch("0", false)
        assertTrue(s.shown(locked = false))
    }
}
