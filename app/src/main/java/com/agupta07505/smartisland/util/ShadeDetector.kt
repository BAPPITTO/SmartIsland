/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** What [ShadeDetector] needs to know about one accessibility window (kept free of Android types so it can be unit tested). */
data class ShadeWindow(
    val packageName: String?,
    /** The window's accessibility title (SystemUI sets "Notification shade.", "Quick settings.", "Lock screen."). */
    val title: String?,
    /** `AccessibilityWindowInfo.TYPE_SYSTEM`. */
    val isSystemType: Boolean,
    val isActive: Boolean,
    val isFocused: Boolean,
    val top: Int,
    val bottom: Int
) {
    val height: Int get() = bottom - top
}

/** The titles SystemUI uses, in the phone's current language (the service reads them from SystemUI's own resources). */
data class ShadeTitles(
    val shade: List<String>,
    val lockScreen: List<String>
) {
    companion object {
        val ENGLISH = ShadeTitles(
            shade = listOf("Notification shade", "Quick settings"),
            lockScreen = listOf("Lock screen")
        )
    }
}

/**
 * Tells whether the notification shade (or quick settings) is pulled down, from the accessibility windows.
 *
 * SystemUI keeps ONE window for the shade, the lock screen and heads-up notifications, and makes it full height for all
 * three, so size alone is not enough:
 * - Unlocked: the shade window is full height AND is the active / focused window. A heads-up notification makes the
 *   window tall too but it never takes the focus, so it does not count.
 * - Locked: the lock screen itself is also a tall, active SystemUI window, so the window title decides: it must be the
 *   shade's or quick settings' title, and not the lock screen's.
 */
object ShadeDetector {
    const val SYSTEM_UI = "com.android.systemui"

    /** A shade window covers at least this much of the screen height; the status bar and navigation bar windows do not. */
    const val MIN_HEIGHT_FRACTION = 0.4f

    fun isShadeOpen(
        windows: List<ShadeWindow>,
        displayHeight: Int,
        keyguardLocked: Boolean,
        titles: ShadeTitles = ShadeTitles.ENGLISH
    ): Boolean = windows.any { w ->
        if (w.packageName != SYSTEM_UI || !w.isSystemType) return@any false
        val title = w.title.orEmpty()
        val tall = displayHeight > 0 && w.height >= displayHeight * MIN_HEIGHT_FRACTION
        when {
            !tall -> false
            titles.lockScreen.matches(title) -> false
            keyguardLocked -> titles.shade.matches(title)
            else -> w.isActive || w.isFocused
        }
    }

    private fun List<String>.matches(title: String): Boolean =
        any { it.isNotBlank() && title.contains(it, ignoreCase = true) }
}
