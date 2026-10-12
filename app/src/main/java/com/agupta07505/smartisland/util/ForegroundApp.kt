/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode

/** What [ForegroundApp] needs to know about one accessibility window (no Android types, so it can be unit tested). */
data class AppWindow(
    val packageName: String?,
    /** `AccessibilityWindowInfo.TYPE_APPLICATION`. */
    val isApplicationType: Boolean,
    val isActive: Boolean,
    val isFocused: Boolean
)

/**
 * Which app the user is looking at, and which of the island's cards that app hides.
 *
 * Before: the foreground app was the package of the last "window opened" event, so a dialog, a floating window or a share
 * sheet from another package replaced it while the real app was still on screen, and the app's own live card came back
 * over it. Now it is read from the window list: the focused (else active) application window.
 */
object ForegroundApp {

    /**
     * The package of the app window the user is in, or null when there is none (the shade, a transition).
     * [ignored] is true for packages that must never count: this app, SystemUI, keyboards.
     */
    fun pick(windows: List<AppWindow>, ignored: (String) -> Boolean): String? {
        val candidates = windows.filter { w ->
            w.isApplicationType && !w.packageName.isNullOrEmpty() && !ignored(w.packageName)
        }
        return (candidates.firstOrNull { it.isFocused } ?: candidates.firstOrNull { it.isActive })?.packageName
    }

    /**
     * True when [mode] is a live card (something the app keeps running: music, navigation, a timer, a recording, a
     * download, a live activity). The app that runs it shows it itself, so the island hides it while that app is open.
     * Calls are exempt (the user answers them from the island or the dialer), and so are plain notifications and every
     * system event card.
     */
    fun isLiveMode(mode: IslandMode): Boolean = when (mode) {
        IslandMode.Music,
        IslandMode.Navigation,
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording,
        IslandMode.Hotspot,
        IslandMode.LiveActivity,
        IslandMode.DownloadUpload -> true
        else -> false
    }

    fun hidesCard(mode: IslandMode, cardPackage: String, foregroundPackage: String?): Boolean =
        !foregroundPackage.isNullOrEmpty() && cardPackage == foregroundPackage && isLiveMode(mode)
}
