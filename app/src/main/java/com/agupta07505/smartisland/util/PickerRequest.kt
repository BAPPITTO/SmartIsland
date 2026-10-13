/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Which picker the island's plus button wants open: [APPS] is the pinned apps picker, [UTILITY] the "add a utility" flow. Both open in
 * their own small window ([com.agupta07505.smartisland.ui.pickers.PickerActivity]); the app's settings screens host the same shared dialogs
 * themselves and are never asked to open one on the island's behalf.
 */
object PickerRequest {
    const val APPS = "apps"
    const val UTILITY = "utility"
}
