/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The expanded card grows out of the collapsed pill's centre.
 *
 * One axis at a time, in the island frame (the island's own centre line when expanded is 0 for x;
 * y is the top edge on screen). `progress` is the expand spring (0 = pill, 1 = card, may overshoot).
 *
 * The pill's centre is the anchor. It stays put on screen while the island grows around it, and the
 * anchor's place inside the island slides from the middle (the pill) to where it sits in the card
 * (`anchorInCard`, 0..1). When the pill's centre lies outside the card (a pill in a corner, a card
 * pushed inward) the anchor is clamped to the nearest card edge, and the anchor's own screen position
 * eases to that clamped point so the last frame is exactly the card.
 */
object ExpandOrigin {

    /** Where the anchor (pill centre) sits inside the card, 0 = start edge, 1 = end edge. */
    fun anchorInCard(pillCentre: Float, cardStart: Float, cardSize: Float): Float {
        if (cardSize <= 0f) return 0.5f
        return ((pillCentre - cardStart) / cardSize).coerceIn(0f, 1f)
    }

    /** The anchor's screen position at the end: the pill centre, pulled inside the card when it lies outside. */
    fun anchorAtEnd(pillCentre: Float, cardStart: Float, cardSize: Float): Float =
        pillCentre.coerceIn(cardStart, cardStart + maxOf(cardSize, 0f))

    /**
     * Start edge of the island on this axis.
     * @param size the island's current size on this axis (its own spring, so the drawn shape and this agree).
     */
    fun start(
        progress: Float,
        size: Float,
        pillCentre: Float,
        cardStart: Float,
        cardSize: Float
    ): Float {
        val endAnchor = anchorAtEnd(pillCentre, cardStart, cardSize)
        val anchor = pillCentre + (endAnchor - pillCentre) * progress
        val inIsland = 0.5f + (anchorInCard(pillCentre, cardStart, cardSize) - 0.5f) * progress
        return anchor - inIsland * size
    }
}
