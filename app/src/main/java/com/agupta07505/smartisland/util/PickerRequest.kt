/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which picker the island's plus button wants open once the app's settings screen is up ([APPS]: the pinned apps picker,
 * [UTILITY]: the "add a utility" dialog). The settings sections read it, open the picker and clear it.
 */
object PickerRequest {
    const val EXTRA_OPEN_PICKER = "open_picker"
    const val APPS = "apps"
    const val UTILITY = "utility"

    var pending: String? by mutableStateOf<String?>(null)
}
