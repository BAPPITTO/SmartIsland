/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Decides when the blur window must be moved or resized to follow the island.
 *
 * Every move of the blur window is a window relayout (a binder call and a new blur layer). The island's rect jitters by a pixel or
 * two while it swells, leans or is dragged, and none of that is visible through the blur, so a change below [TOLERANCE_PX] on every
 * edge is skipped. A change of the blur radius or a window that was hidden always goes through.
 */
object BlurFollow {
    /** Largest drift (px) of any edge that is not worth a relayout. The blur window is already inset from the island's rounded corners. */
    const val TOLERANCE_PX = 3

    /** True when the blur window has to be laid out again for [next], given what it was last laid out as ([last], null when hidden). */
    fun needsRelayout(last: IntArray?, next: IntArray): Boolean {
        if (last == null || last.size != next.size) return true
        // Index 0..3 are left, top, width, height; any further entry (the radius) must match exactly.
        for (i in next.indices) {
            val limit = if (i < 4) TOLERANCE_PX else 0
            if (kotlin.math.abs(last[i] - next[i]) > limit) return true
        }
        return false
    }

    /** True when any edge of [next] is more than 2 px from [last]: smaller drifts are not worth reporting. */
    fun rectMoved(last: android.graphics.Rect, next: android.graphics.Rect): Boolean =
        kotlin.math.abs(last.left - next.left) > 2 || kotlin.math.abs(last.top - next.top) > 2 ||
            kotlin.math.abs(last.right - next.right) > 2 || kotlin.math.abs(last.bottom - next.bottom) > 2
}
