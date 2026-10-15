/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandNotificationAction
import com.agupta07505.smartisland.ui.expanded.ActionRole
import com.agupta07505.smartisland.ui.expanded.classifyAction

/**
 * Builds the island's button list for a stock clock timer / stopwatch so it matches the notification shade:
 * the same buttons, in the same order, and nothing the shade does not show (Google Clock shows Pause + "+1:00"
 * while a timer runs, Pause + Lap while a stopwatch runs, and Resume + Stop only once either is paused).
 */
object ShadeActions {

    /** The shade draws at most this many of a notification's own buttons. */
    const val SHADE_ACTION_LIMIT = 3

    /** Roles a clock card can use. Anything else found in a view is not a button (the whole-card tap, an icon). */
    private val CLOCK_ROLES = setOf(
        ActionRole.Pause, ActionRole.Resume, ActionRole.Stop, ActionRole.Reset, ActionRole.Lap, ActionRole.AddMinute
    )

    /**
     * @param standard the notification's own `actions`
     * @param view clickable buttons read from the notification's custom view, in layout order, or empty
     */
    fun mirror(
        standard: List<IslandNotificationAction>,
        view: List<IslandNotificationAction>
    ): List<IslandNotificationAction> {
        val real = standard.filter { it.pendingIntent != null }
        val fromView = view.filter { classifyAction(it.title) in CLOCK_ROLES }

        if (fromView.isNotEmpty()) {
            // The shade draws the custom view: its buttons, in its order. The app's own intent for the same
            // button is better than the island's proxy for pressing the view, so it replaces it.
            val used = HashSet<Int>()
            return fromView.map { shown ->
                val match = real.withIndex().firstOrNull { (i, a) -> i !in used && sameButton(a, shown) }
                if (match != null) {
                    used.add(match.index)
                    match.value
                } else {
                    shown
                }
            }.distinctBy { classifyAction(it.title) to it.title.trim().lowercase() }
        }

        return real
            .take(SHADE_ACTION_LIMIT)
            .distinctBy { it.title.trim().lowercase() }
    }

    private fun sameButton(a: IslandNotificationAction, b: IslandNotificationAction): Boolean {
        if (a.title.trim().equals(b.title.trim(), ignoreCase = true)) return true
        val role = classifyAction(a.title)
        return role in CLOCK_ROLES && role == classifyAction(b.title)
    }

    /** True when the shade offers a Pause button, which is how a running timer / stopwatch looks. */
    fun offersPause(actions: List<IslandNotificationAction>): Boolean =
        actions.any { classifyAction(it.title) == ActionRole.Pause }

    /** True when the shade offers a Resume button, which is how a paused timer / stopwatch looks. */
    fun offersResume(actions: List<IslandNotificationAction>): Boolean =
        actions.any { classifyAction(it.title) == ActionRole.Resume }
}
