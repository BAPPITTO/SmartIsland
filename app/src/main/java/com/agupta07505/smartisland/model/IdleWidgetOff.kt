/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * When the idle widgets are taken off the collapsed cutout on purpose (Settings > Idle widgets), per orientation:
 *  - after a tap on the cutout closed the open island: the widgets stay off until the next time the island is opened or the screen turns off;
 *  - after a long press hid the pill: the widgets stay off for as long as the pill is hidden;
 *  - while the island holds any stored notification (even one the idle timeout put away): the widgets stay off until none is left.
 * Pure rules, so they can be unit tested; the state lives in IslandViewModel.
 */
object IdleWidgetOff {
    fun suppressed(
        closedByTap: Boolean,
        pillHidden: Boolean,
        offAfterTapClose: Boolean,
        offAfterLongPress: Boolean,
        hasStoredNotifications: Boolean = false,
        offWhenStored: Boolean = false
    ): Boolean = (closedByTap && offAfterTapClose) || (pillHidden && offAfterLongPress) || (hasStoredNotifications && offWhenStored)

    /** The slider choices for the widget switch timer: whole seconds from [IdleWidgets.MIN_CYCLE_SECONDS] to [IdleWidgets.MAX_CYCLE_SECONDS]. */
    fun normalizeCycleSeconds(seconds: Int): Int =
        seconds.coerceIn(IdleWidgets.MIN_CYCLE_SECONDS, IdleWidgets.MAX_CYCLE_SECONDS)

    /** Every widget ([IdleWidgetKind.values]) as the stored list, for the selector's "Select all". */
    fun allIds(): String = IdleWidgets.serialize(IdleWidgetKind.values().toList())
}
