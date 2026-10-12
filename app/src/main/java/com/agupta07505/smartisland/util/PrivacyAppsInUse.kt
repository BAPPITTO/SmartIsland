/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.PrivacyKind

/**
 * Which apps use a sensor right now, for the dialog a tapped privacy indicator opens.
 *
 * The system's own "apps using this" screens refuse to open from a normal app (the log shows a SecurityException for
 * `REVIEW_PERMISSION_USAGE` / `MANAGE_PERMISSION_APPS`), and no public API names another app's sensor use. With Shizuku the
 * app reads the system's own dumps (read-only): `dumpsys appops` for camera / microphone / location (an op that is
 * "running" is in use now) and `dumpsys media_projection` for screen sharing. Without Shizuku the dialog says so.
 *
 * The dump text differs between Android versions and skins; the parsers are written to ignore what they do not understand
 * (an empty list, never a wrong one). Check on the device with `adb shell dumpsys appops` while the camera is open.
 */
object PrivacyAppsInUse {
    /** AppOps names that make up each sensor. */
    internal fun opsOf(kind: PrivacyKind): Set<String> = when (kind) {
        PrivacyKind.Camera -> setOf("CAMERA")
        PrivacyKind.Microphone -> setOf("RECORD_AUDIO", "PHONE_CALL_MICROPHONE")
        PrivacyKind.Location -> setOf("FINE_LOCATION", "COARSE_LOCATION")
        PrivacyKind.ScreenShare -> emptySet()
    }

    private val packageHeader = Regex("^\\s*Package\\s+([\\w.]+)\\s*:")
    private val opHeader = Regex("^\\s*([A-Z][A-Z0-9_]+)\\s*\\(")
    private val uidHeader = Regex("^\\s*Uid\\s+\\S+:")

    /**
     * Packages with one of [ops] running, from the text of `dumpsys appops`. An op block starts at a line like
     * `CAMERA (allow):` under a `Package <name>:` line and lasts until the next op, package or uid line; it counts when a
     * line in it says "running" (`Running start at: ...`). Sorted, no duplicates.
     */
    internal fun parseRunningPackages(dump: String, ops: Set<String>): List<String> {
        if (ops.isEmpty()) return emptyList()
        val found = sortedSetOf<String>()
        var pkg: String? = null
        var inWantedOp = false
        for (line in dump.lineSequence()) {
            val p = packageHeader.find(line)
            if (p != null) { pkg = p.groupValues[1]; inWantedOp = false; continue }
            if (uidHeader.containsMatchIn(line)) { pkg = null; inWantedOp = false; continue }
            val o = opHeader.find(line)
            if (o != null) { inWantedOp = o.groupValues[1] in ops; continue }
            if (inWantedOp && pkg != null && line.contains("running", ignoreCase = true) &&
                !line.contains("not running", ignoreCase = true)
            ) {
                found.add(pkg)
            }
        }
        return found.toList()
    }

    /** Packages using [kind] now, or null when they cannot be read (no Shizuku, or the command failed). */
    suspend fun read(kind: PrivacyKind): List<String>? {
        if (!ShizukuManager.hasPermission()) return null
        if (kind == PrivacyKind.ScreenShare) {
            val dump = ShizukuManager.runShellForOutput("dumpsys media_projection").getOrNull() ?: return null
            return listOfNotNull(PrivacyIndicatorMonitor.parseMediaProjectionPackage(dump))
        }
        val dump = ShizukuManager.runShellForOutput("dumpsys appops").getOrNull() ?: return null
        return parseRunningPackages(dump, opsOf(kind))
    }
}
