/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * What a long press on an app in the quick-launch grid can do. A plain tap always opens the app full screen
 * (that is not an action here, it is the grid's normal click).
 */
enum class AppShortcutAction { AppInfo, FloatingWindow, SplitScreen }

/**
 * The long-press menu, in display order. Floating window is listed only while it is turned on in settings
 * (the same switch the notification swipe uses), so the menu never offers something that would only show a toast.
 */
fun appShortcutMenu(floatingRoute: FloatingWindowRoute): List<AppShortcutAction> = buildList {
    add(AppShortcutAction.AppInfo)
    if (floatingRoute != FloatingWindowRoute.Off) add(AppShortcutAction.FloatingWindow)
    add(AppShortcutAction.SplitScreen)
}

/** How a split-screen request is carried out. */
enum class SplitPlan {
    /** An app is open under the island: it takes one half and the chosen app opens in the other. */
    PairWithForeground,

    /**
     * Nothing to pair with (the home screen, or the chosen app is the one already open): open the chosen app, then ask Android
     * for split screen so its own app selection picks the second app.
     */
    LaunchThenSystemPicker
}

/**
 * Decides the split-screen plan from what is on screen. [homePackages] are the home (launcher) apps: the home screen cannot
 * be one half of a split, so from there Android's normal split-screen app selection takes over.
 */
fun splitScreenPlan(foregroundPackage: String?, homePackages: Set<String>, targetPackage: String): SplitPlan = when {
    foregroundPackage.isNullOrEmpty() -> SplitPlan.LaunchThenSystemPicker
    foregroundPackage in homePackages -> SplitPlan.LaunchThenSystemPicker
    foregroundPackage == targetPackage -> SplitPlan.LaunchThenSystemPicker
    else -> SplitPlan.PairWithForeground
}
