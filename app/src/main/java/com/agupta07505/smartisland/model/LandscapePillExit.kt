/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Landscape only: how the collapsed cutout leaves when its last card goes and the island returns to the
 * idle circle. The cutout itself slides away and fades (it keeps its own size and look while it does), then the
 * idle circle opens at its spot.
 *
 *  - Pill in the middle (anywhere far from the left and right edges): it slides up.
 *  - Pill near the left edge: it slides out to the left; near the right edge: out to the right.
 *
 * All values are dp. X positions are measured from the screen centre (like the island's x offset).
 */
enum class PillExitDirection { Up, Left, Right }

/** "Near an edge" means the pill's centre is within this share of the screen width from that edge. */
const val PILL_EXIT_EDGE_FRACTION = 0.25f

/** The pill never slides further than this (it fades out on the way), so a pill low on the screen does not fly across it. */
const val PILL_EXIT_MAX_TRAVEL_DP = 120f

/** Spare room past the screen edge so the pill is fully clear of it. */
const val PILL_EXIT_MARGIN_DP = 8f

/** Base length of the exit (scaled by the animation speed). */
const val PILL_EXIT_MS = 360

fun pillExitDirection(centerXDp: Float, screenWidthDp: Float): PillExitDirection {
    if (screenWidthDp <= 0f) return PillExitDirection.Up
    val fromLeft = screenWidthDp / 2f + centerXDp
    val fromRight = screenWidthDp - fromLeft
    val zone = screenWidthDp * PILL_EXIT_EDGE_FRACTION
    return when {
        fromLeft <= zone && fromLeft <= fromRight -> PillExitDirection.Left
        fromRight <= zone -> PillExitDirection.Right
        else -> PillExitDirection.Up
    }
}

/** Distance (dp) the pill slides: to just past the edge it heads for, capped at [PILL_EXIT_MAX_TRAVEL_DP]. */
fun pillExitTravelDp(
    direction: PillExitDirection,
    centerXDp: Float,
    widthDp: Float,
    heightDp: Float,
    topDp: Float,
    screenWidthDp: Float
): Float {
    val toEdge = when (direction) {
        PillExitDirection.Up -> topDp + heightDp
        PillExitDirection.Left -> screenWidthDp / 2f + centerXDp + widthDp / 2f
        PillExitDirection.Right -> screenWidthDp / 2f - centerXDp + widthDp / 2f
    } + PILL_EXIT_MARGIN_DP
    return toEdge.coerceIn(0f, PILL_EXIT_MAX_TRAVEL_DP)
}

data class PillExitOffset(val dx: Float, val dy: Float)

/** Offset (dp) of the sliding pill at [progress] (0 = in place, 1 = gone). */
fun pillExitOffsetDp(direction: PillExitDirection, travelDp: Float, progress: Float): PillExitOffset {
    val d = travelDp * progress.coerceIn(0f, 1f)
    return when (direction) {
        PillExitDirection.Up -> PillExitOffset(0f, -d)
        PillExitDirection.Left -> PillExitOffset(-d, 0f)
        PillExitDirection.Right -> PillExitOffset(d, 0f)
    }
}

private fun smooth(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/** The leaving pill stays solid for the first third, then fades out. */
fun pillExitAlpha(progress: Float): Float = 1f - smooth((progress - 0.3f) / 0.7f)
