/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaProjectionDumpParseTest {
    @Test
    fun nullProjectionIsInactive() {
        val dump = "MEDIA PROJECTION MANAGER (dumpsys media_projection)\nMedia Projection: null\n"
        assertEquals(false, PrivacyIndicatorMonitor.parseMediaProjectionActive(dump))
    }

    @Test
    fun grantedProjectionIsActive() {
        val dump = "MEDIA PROJECTION MANAGER (dumpsys media_projection)\nMedia Projection: MediaProjection{uid=10234}\n"
        assertEquals(true, PrivacyIndicatorMonitor.parseMediaProjectionActive(dump))
    }

    @Test
    fun unknownOutputLeavesStateAlone() {
        assertNull(PrivacyIndicatorMonitor.parseMediaProjectionActive("Can't find service: media_projection"))
    }

    @Test
    fun holderPackageIsRead() {
        val dump = "Media Projection: MediaProjection{}\n  mPackageName=com.example.recorder\n"
        assertEquals("com.example.recorder", PrivacyIndicatorMonitor.parseMediaProjectionPackage(dump))
        assertNull(PrivacyIndicatorMonitor.parseMediaProjectionPackage("Media Projection: null"))
    }
}
