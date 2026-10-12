/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Keeps the secondary pill clear of Android's own status bar chips (the screen recording chip and the
 * camera / microphone / location indicator).
 *
 * Portrait only. While such a chip is on screen the secondary pill sits on the OTHER side of the collapsed cutout than the
 * Position setting says, and goes back when the chip is gone. The saved setting never changes. It jumps, it does not slide.
 */
object ChipAvoidance {
    /** True while a system chip is likely on screen: something is in use (camera, microphone, location, projection) or a recorder runs. */
    fun chipShowing(privacy: PrivacyIndicatorState, cards: List<IslandNotification>): Boolean =
        privacy.active.isNotEmpty() || cards.any { it.mode == IslandMode.ScreenRecording }

    /** Which side the secondary pill is on: the setting's side, flipped while a chip shows (portrait only). */
    fun circleOnLeft(settingIsLeft: Boolean, chipShowing: Boolean, isLandscape: Boolean): Boolean =
        settingIsLeft != (chipShowing && !isLandscape)
}
