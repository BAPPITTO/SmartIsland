/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Rules for the expanded island's shell state, as pure functions.
 *
 * The shell draws a rounded clip and a surface for the SINGLE island, and leaves both to the two landscape islands while they are drawn.
 * Some swipes left a single card drawn as a plain rectangle (no clip, no surface) until the next swipe. [twoIslandsDrawn] makes the clip
 * and surface follow what the content really draws, and [stuckReason] is the watchdog's test for whatever state is still inconsistent when
 * the island sits at rest.
 */
object ShellStateGuard {
    /** The island must have been at rest this long (ms) with an inconsistent shell before the watchdog acts. */
    const val REST_MS = 600L

    /**
     * The shell is clipped, shadowed and given its surface as a single island unless this is true: the overlay's two-island flag (which
     * sizes the window early) AND the content's own report that it draws the two islands.
     */
    fun twoIslandsDrawn(overlayFlag: Boolean, contentSplitShown: Boolean): Boolean = overlayFlag && contentSplitShown

    /** How long the watchdog waits (ms): the two-island hold has its own close to finish first, so it adds its length. */
    fun waitMs(holdActive: Boolean, holdMs: Int): Long = REST_MS + if (holdActive) holdMs.toLong() else 0L

    /** Why the shell state is wrong, or null when it is fine (or the island is not expanded and at rest). */
    fun stuckReason(
        expanded: Boolean,
        atRest: Boolean,
        overlayFlag: Boolean,
        splitWanted: Boolean,
        twoIslandsDrawn: Boolean,
        singleSurfaceAlpha: Float,
        radiusDp: Float
    ): String? {
        if (!expanded || !atRest) return null
        // The two-island flag is only kept on by the close's hold; a picker or picture that is not open does not hold it for ever.
        if (overlayFlag && !splitWanted) return REASON_HOLD
        // A single island is drawn but its surface is not (fully) there.
        if (!twoIslandsDrawn && singleSurfaceAlpha < SURFACE_BACK) return REASON_NO_SURFACE
        // A single island with square corners.
        if (!twoIslandsDrawn && radiusDp <= MIN_RADIUS_DP) return REASON_NO_RADIUS
        return null
    }

    /** Reasons the overlay can repair by dropping the two-island flag. The radius comes from the layout and is only logged. */
    fun canForceSingle(reason: String?): Boolean = reason == REASON_HOLD || reason == REASON_NO_SURFACE

    const val REASON_HOLD = "hold-without-island"
    const val REASON_NO_SURFACE = "single-without-surface"
    const val REASON_NO_RADIUS = "single-without-radius"

    private const val SURFACE_BACK = 0.98f
    private const val MIN_RADIUS_DP = 0.5f
}
