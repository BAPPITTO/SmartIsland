/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * When the contents change while the island expands, tied to the shell's own growth.
 *
 * Before, the pill's content left over half the base fade (140 ms landscape, 110 ms portrait) and the card content only
 * started a quarter in and took a full fade (done at 350 / 275 ms), while the shell was already 90 % grown at about
 * 193 / 150 ms: an empty shell for a few frames, then the content arriving after the growth. Now all three numbers are
 * read off the expand spring that is really running (the given stiffness and damping are the speed-scaled ones), so the
 * contents follow the shell at every speed setting:
 *  - the pill's content is gone when the shell has covered [PILL_OUT_END_PROGRESS] of its way,
 *  - the card content starts at [CARD_IN_START_PROGRESS] and is complete at [CARD_IN_END_PROGRESS]. The shell clips it,
 *    so it is revealed as the shell grows.
 * The expand spring itself is unchanged.
 */
object ExpandMotion {
    const val PILL_OUT_END_PROGRESS = 0.25f
    const val CARD_IN_START_PROGRESS = 0.1f
    const val CARD_IN_END_PROGRESS = 0.9f

    /** A fade never gets shorter than this (a couple of frames), however stiff the spring is. */
    const val MIN_FADE_MS = 50

    private fun at(progress: Float, stiffness: Float, damping: Float): Int =
        springTimeToProgressMs(stiffness, damping, progress).toInt()

    fun pillOutMs(stiffness: Float, damping: Float): Int =
        at(PILL_OUT_END_PROGRESS, stiffness, damping).coerceAtLeast(MIN_FADE_MS)

    fun cardInDelayMs(stiffness: Float, damping: Float): Int =
        at(CARD_IN_START_PROGRESS, stiffness, damping)

    fun cardInMs(stiffness: Float, damping: Float): Int =
        (at(CARD_IN_END_PROGRESS, stiffness, damping) - cardInDelayMs(stiffness, damping)).coerceAtLeast(MIN_FADE_MS)
}
