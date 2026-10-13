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

    /** Corner radius (dp) of the expanded island's shell; the blur rectangle is inset so it stays inside this rounded corner. */
    const val EXPANDED_CORNER_DP = 34f

    /** Lowest and highest opacity of the island's tint over the blur: below it the card text loses contrast, above it the blur barely shows. */
    const val TINT_MIN = 0.30f
    const val TINT_MAX = 0.75f

    /** Share of the island's own opacity that is kept as the tint while Live blur shows through. */
    const val TINT_SHARE = 0.6f

    /**
     * The opacity the island's shell is drawn with while the blur window sits under it: the user's opacity ([opacity], 0..1) scaled by
     * [TINT_SHARE] and kept within [TINT_MIN]..[TINT_MAX], so the blur reads as frosted glass and the card stays legible.
     * A shell that is already see-through (opacity below [TINT_MIN]) is left as it is, never made more solid.
     */
    fun tintAlpha(opacity: Float): Float {
        val o = opacity.coerceIn(0f, 1f)
        return minOf(o, (o * TINT_SHARE).coerceIn(TINT_MIN, TINT_MAX))
    }

    /** The shell's opacity at [fraction] (0 = solid look, 1 = tint over the blur): eases between [opacity] and [tintAlpha]. */
    fun shellAlpha(opacity: Float, fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        if (f <= 0f) return opacity
        return opacity + (tintAlpha(opacity) - opacity) * f
    }

    /** Smallest blur rectangle (px, either side) worth showing: below this the island is still a pill and nothing is blurred. */
    const val MIN_SIDE_PX = 24

    /**
     * How far (px) a rectangle must be pulled in from each edge of a rounded rectangle with corner radius [radiusPx] so that its
     * corners lie inside the rounded corner: r * (1 - 1/sqrt(2)). The radius never exceeds half of the shorter side.
     */
    fun cornerInsetPx(widthPx: Int, heightPx: Int, radiusPx: Float): Int {
        val r = radiusPx.coerceAtMost(minOf(widthPx, heightPx) / 2f).coerceAtLeast(0f)
        return kotlin.math.ceil(r * (1f - 1f / kotlin.math.sqrt(2f))).toInt()
    }

    /**
     * The layout key of the blur window for an island at [left], [top], [right], [bottom] (px, on screen): left, top, width, height
     * of the rectangle inset from the rounded corners, then [blurRadiusPx]. Null when the island is too small to blur.
     * The order matches [needsRelayout].
     */
    fun layoutKey(left: Int, top: Int, right: Int, bottom: Int, radiusPx: Float, blurRadiusPx: Int): IntArray? {
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0 || blurRadiusPx <= 0) return null
        val inset = cornerInsetPx(w, h, radiusPx)
        val iw = w - 2 * inset
        val ih = h - 2 * inset
        if (iw < MIN_SIDE_PX || ih < MIN_SIDE_PX) return null
        return intArrayOf(left + inset, top + inset, iw, ih, blurRadiusPx)
    }
}
