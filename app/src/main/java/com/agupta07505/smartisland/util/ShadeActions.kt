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

/** Builds the island's buttons for a clock timer / stopwatch so they match the shade: same buttons, same order. */
object ShadeActions {

    /** The shade draws at most this many of a notification's own buttons. */
    const val SHADE_ACTION_LIMIT = 3

    /** Roles a clock card can use; any other clickable view is not a button. */
    private val CLOCK_ROLES = setOf(
        ActionRole.Pause, ActionRole.Resume, ActionRole.Stop, ActionRole.Reset, ActionRole.Lap, ActionRole.AddMinute
    )

    /** Clocks whose button set per state is fixed, so it can be enforced. */
    private val KNOWN_STATE_CLOCKS = setOf("com.google.android.deskclock", "com.android.deskclock")

    fun hasKnownStates(packageName: String): Boolean = packageName.lowercase() in KNOWN_STATE_CLOCKS

    /**
     * Drops buttons that cannot belong to the state the list announces (Pause = running, Resume = paused):
     * running timer Pause + "+1:00", paused timer Resume + Stop, running stopwatch Pause + Lap, paused stopwatch
     * Resume + Stop / Reset. A list with neither or both is returned as is.
     */
    fun enforceState(actions: List<IslandNotificationAction>, kind: TimerStopwatchParser.ClockKind): List<IslandNotificationAction> {
        val roles = actions.map { classifyAction(it.title) }
        val offersPause = ActionRole.Pause in roles
        val offersResume = ActionRole.Resume in roles
        if (offersPause == offersResume) return actions // neither, or both: no reliable state
        val banned: Set<ActionRole> = when {
            offersPause && kind == TimerStopwatchParser.ClockKind.Timer ->
                setOf(ActionRole.Stop, ActionRole.Reset, ActionRole.Resume, ActionRole.Lap)
            offersPause ->
                setOf(ActionRole.Stop, ActionRole.Reset, ActionRole.Resume, ActionRole.AddMinute)
            kind == TimerStopwatchParser.ClockKind.Timer -> setOf(ActionRole.Pause, ActionRole.Lap)
            else -> setOf(ActionRole.Pause, ActionRole.Lap, ActionRole.AddMinute)
        }
        return actions.filter { classifyAction(it.title) !in banned }
    }

    /** [standard] are the notification's own actions, [view] the buttons read from its custom view in layout order. */
    fun mirror(
        standard: List<IslandNotificationAction>,
        view: List<IslandNotificationAction>,
        kind: TimerStopwatchParser.ClockKind? = null,
        packageName: String = ""
    ): List<IslandNotificationAction> {
        if (kind == null || !hasKnownStates(packageName)) return mirrorUnchecked(standard, view)
        // Before the shade limit, so a hidden fourth button cannot push out a right one.
        return mirrorUnchecked(
            enforceState(standard.filter { it.pendingIntent != null }, kind),
            enforceState(view.filter { classifyAction(it.title) in CLOCK_ROLES }, kind)
        )
    }

    private fun mirrorUnchecked(
        standard: List<IslandNotificationAction>,
        view: List<IslandNotificationAction>
    ): List<IslandNotificationAction> {
        val real = standard.filter { it.pendingIntent != null }
        val fromView = view.filter { classifyAction(it.title) in CLOCK_ROLES }

        if (fromView.isNotEmpty()) {
            // The shade draws the view's buttons in its order; the app's own intent beats the view proxy.
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

    /** True while running (the shade offers Pause). */
    fun offersPause(actions: List<IslandNotificationAction>): Boolean =
        actions.any { classifyAction(it.title) == ActionRole.Pause }

    /** True while paused (the shade offers Resume). */
    fun offersResume(actions: List<IslandNotificationAction>): Boolean =
        actions.any { classifyAction(it.title) == ActionRole.Resume }
}
