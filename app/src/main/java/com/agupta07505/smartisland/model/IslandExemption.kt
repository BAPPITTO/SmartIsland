/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Apps that switch the island totally off while they are in the foreground (Settings > Exempt apps), with one list per orientation:
 * an app can be exempt in portrait only, in landscape only, or in both. Pure rule, so it can be unit tested.
 */
object IslandExemption {
    fun isOff(
        foregroundPackage: String?,
        portraitApps: Set<String>,
        landscapeApps: Set<String>,
        isLandscape: Boolean
    ): Boolean {
        if (foregroundPackage.isNullOrEmpty()) return false
        return foregroundPackage in (if (isLandscape) landscapeApps else portraitApps)
    }
}
