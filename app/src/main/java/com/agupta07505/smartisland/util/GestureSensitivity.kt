/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import kotlin.math.abs

/** Which way a pill swipe went. */
enum class PillSwipeDirection { Left, Right, Up, Down }

/**
 * How easily swipes fire (Gestures screen).
 *
 * - [distanceScale] multiplies every swipe distance (the pill swipe, the expanded swipes, the "was it a tap" slop):
 *   HIGH fires sooner, LOW needs a longer swipe.
 * - [dominance] is how much longer one axis must be than the other before the swipe counts as that direction.
 *   1 is the old rule (the longer axis wins, ties go vertical). LOW asks for 1.6, so a diagonal drag aimed at neither
 *   direction does nothing instead of firing the wrong action.
 *
 * NORMAL reproduces the behaviour from before this setting existed exactly.
 */
enum class GestureSensitivity(val id: String, val distanceScale: Float, val dominance: Float) {
    LOW("low", 1.45f, 1.6f),
    NORMAL("normal", 1f, 1f),
    HIGH("high", 0.7f, 1f);

    /** A distance in dp (tap slop, swipe threshold) after this sensitivity. */
    fun scaled(dp: Float): Float = dp * distanceScale

    /** True when the drag is clearly horizontal: [dx] beats [dy] by [dominance]. */
    fun isHorizontal(dx: Float, dy: Float): Boolean = abs(dx) > abs(dy) * dominance

    /** True when the drag is clearly vertical. With dominance 1 a tie counts as vertical, as it always did. */
    fun isVertical(dx: Float, dy: Float): Boolean = abs(dy) >= abs(dx) * dominance

    /**
     * The pill swipe a drag of ([dx], [dy]) pixels amounts to, or null while it is too short or too diagonal.
     * [threshold] is in the same unit as the drag.
     */
    fun pillSwipe(dx: Float, dy: Float, threshold: Float, horizontalThreshold: Float = threshold): PillSwipeDirection? = when {
        abs(dx) >= horizontalThreshold && isHorizontal(dx, dy) ->
            if (dx < 0f) PillSwipeDirection.Left else PillSwipeDirection.Right
        abs(dy) >= threshold && isVertical(dx, dy) ->
            if (dy < 0f) PillSwipeDirection.Up else PillSwipeDirection.Down
        else -> null
    }

    companion object {
        const val ID_LOW = "low"
        const val ID_NORMAL = "normal"
        const val ID_HIGH = "high"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        /** Unknown or missing ids (old backups) fall back to [NORMAL]. */
        fun fromId(id: String?): GestureSensitivity = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
