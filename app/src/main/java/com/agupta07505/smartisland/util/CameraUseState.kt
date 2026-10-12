/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Which camera ids count as "a camera is in use" for the privacy indicator (pure, no Android types).
 *
 * Fix: face recognition on the lock screen opens the front camera, and that showed the camera indicator. While the
 * keyguard is up nothing counts, and an id that turns unavailable while the keyguard is up is remembered in
 * [ignoredWhileLocked] until it is released, so it also stays hidden for the moment after the phone unlocks (face unlock
 * lets go of the camera a little after the unlock). An id that was already in use before the phone locked is not
 * ignored, so a real camera app left running shows again after the unlock.
 */
class CameraUseState {
    private val unavailable = mutableSetOf<String>()
    private val torchOn = mutableSetOf<String>()
    private val ignoredWhileLocked = mutableSetOf<String>()

    fun onUnavailable(cameraId: String, locked: Boolean) {
        unavailable.add(cameraId)
        if (locked) ignoredWhileLocked.add(cameraId)
    }

    fun onAvailable(cameraId: String) {
        unavailable.remove(cameraId)
        ignoredWhileLocked.remove(cameraId)
    }

    fun onTorch(cameraId: String, enabled: Boolean) {
        if (enabled) torchOn.add(cameraId) else torchOn.remove(cameraId)
    }

    /** True when the indicator should show: a camera is open, it is not a lit torch, and it was not opened under the keyguard. */
    fun shown(locked: Boolean): Boolean =
        !locked && (unavailable - torchOn - ignoredWhileLocked).isNotEmpty()
}
