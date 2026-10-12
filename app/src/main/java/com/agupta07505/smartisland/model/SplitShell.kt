/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Landscape split (card island plus picker or picture island): the shell's size as a direct function of the ONE split progress.
 *
 * Before, the shell's width came from its own spring / snap and the content ran on `splitProgress`, so at the end of a close the
 * card island (the single island's width, centred in a wider shell) and the shell were different rectangles. Here the shell's width
 * is `single + (total - single) * progress`, so at progress 0 it is exactly the single island and the card island fills it.
 */
object SplitShell {
    /**
     * Width the shell has while the split is drawn. [progress] is the RAW split progress; the width follows [SplitMotion.geometry] of it,
     * the same remap the islands' own widths use, so the shell is exactly the single island when the tail starts.
     */
    fun width(single: Float, total: Float, progress: Float): Float = single + (total - single) * SplitMotion.geometry(progress)

    /**
     * Height the shell has while the split is drawn: the single island's height at progress 0, the taller of the two islands at 1,
     * following the same [SplitMotion.geometry] as the width. Before, the height ran on the shell's own spring, so the shell was
     * taller than the card from the first frame of an opening and shrank in a second stage after the width of a close.
     */
    fun height(single: Float, total: Float, progress: Float): Float = single + (total - single) * SplitMotion.geometry(progress)

    /** The shell's own (non-split) width has reached the single island's width: the split override can be let go. */
    fun handoverDone(own: Float, single: Float): Boolean = kotlin.math.abs(own - single) <= HANDOVER_TOLERANCE_DP

    /** The override is let go after this long even if the shell's own width never got there (a safety, not a timing). */
    const val HANDOVER_MAX_MS = 600L

    const val HANDOVER_TOLERANCE_DP = 0.5f
}
