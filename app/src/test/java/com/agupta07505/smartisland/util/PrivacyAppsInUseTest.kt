/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.PrivacyKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacyAppsInUseTest {
    private val dump = """
        Uid 10123:
          Package com.example.cam:
            CAMERA (allow):
              Access: [fg-s] 2026-10-06 14:55:00.000 (-5s0ms) duration=+3s
              Running start at: -3s0ms
            RECORD_AUDIO (allow):
              Access: [fg-s] 2026-10-06 14:50:00.000 (-9m)
          Package com.example.helper:
            CAMERA (allow):
              Access: [bg-s] 2026-10-06 14:00:00.000 (-1h)
        Uid 10456:
          Package com.example.rec:
            RECORD_AUDIO (allow):
              Running start at: -1m
    """.trimIndent()

    @Test fun cameraIsTheAppWithARunningCameraOp() =
        assertEquals(listOf("com.example.cam"), PrivacyAppsInUse.parseRunningPackages(dump, PrivacyAppsInUse.opsOf(PrivacyKind.Camera)))

    @Test fun microphoneIgnoresAnotherOpsRunningLine() =
        assertEquals(listOf("com.example.rec"), PrivacyAppsInUse.parseRunningPackages(dump, PrivacyAppsInUse.opsOf(PrivacyKind.Microphone)))

    @Test fun nothingRunningOrUnknownTextGivesAnEmptyList() {
        assertEquals(emptyList<String>(), PrivacyAppsInUse.parseRunningPackages(dump, PrivacyAppsInUse.opsOf(PrivacyKind.Location)))
        assertEquals(emptyList<String>(), PrivacyAppsInUse.parseRunningPackages("garbage", PrivacyAppsInUse.opsOf(PrivacyKind.Camera)))
        assertEquals(emptyList<String>(), PrivacyAppsInUse.parseRunningPackages(dump, PrivacyAppsInUse.opsOf(PrivacyKind.ScreenShare)))
    }
}
