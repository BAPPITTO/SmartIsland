/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The pages of the quick-launch window. Apps and Recents sit side by side on the "apps level" (swipe left / right); the
 * Utilities page is the other level (swipe up / down), and the two levels loop into each other.
 */
enum class LauncherPage { Apps, Recents, Utilities }

/** A swipe in the launcher, named by the way the finger moves. */
enum class LauncherSwipe { Up, Down, Left, Right }

/**
 * The page a [swipe] leads to from [current].
 *
 * - Up or down switches between the apps level and Utilities, and loops: from Utilities either direction returns to the apps
 *   level, to [lastAppsLevelPage] (the page that was open when the finger left it).
 * - Left or right switches between Apps and Recents, when [recentsEnabled]; Utilities has nothing sideways.
 * - Recents is only a real page while it is enabled: with it off, any apps-level page is Apps.
 */
fun launcherPageAfter(
    current: LauncherPage,
    swipe: LauncherSwipe,
    recentsEnabled: Boolean,
    lastAppsLevelPage: LauncherPage = LauncherPage.Apps
): LauncherPage {
    val appsLevel = if (recentsEnabled && lastAppsLevelPage == LauncherPage.Recents) LauncherPage.Recents else LauncherPage.Apps
    val here = if (current == LauncherPage.Recents && !recentsEnabled) LauncherPage.Apps else current
    return when (swipe) {
        LauncherSwipe.Up, LauncherSwipe.Down ->
            if (here == LauncherPage.Utilities) appsLevel else LauncherPage.Utilities
        LauncherSwipe.Left, LauncherSwipe.Right -> when {
            here == LauncherPage.Utilities -> here
            !recentsEnabled -> LauncherPage.Apps
            here == LauncherPage.Apps -> LauncherPage.Recents
            else -> LauncherPage.Apps
        }
    }
}

/**
 * True while the launcher window is open and a drag that moves belongs to it (its page swipes), so the island's own swipe actions
 * (collapse, dismiss, switch card) must not also fire for it. A tap, which does not move, is not a drag.
 */
fun launcherOwnsDrag(launcherOpen: Boolean, dragX: Float, dragY: Float): Boolean =
    launcherOpen && (kotlin.math.abs(dragX) > 0.5f || kotlin.math.abs(dragY) > 0.5f)

/** The swipe a finger drag of ([dx], [dy]) makes once it passes [thresholdPx] along its longer axis; null before that. */
fun launcherSwipeFor(dx: Float, dy: Float, thresholdPx: Float): LauncherSwipe? {
    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy)
    return when {
        ay >= ax && ay >= thresholdPx -> if (dy < 0f) LauncherSwipe.Up else LauncherSwipe.Down
        ax > ay && ax >= thresholdPx -> if (dx < 0f) LauncherSwipe.Left else LauncherSwipe.Right
        else -> null
    }
}
