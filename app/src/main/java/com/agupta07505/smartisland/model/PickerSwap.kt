/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The active card and the sibling picker trade places by size, driven by ONE progress value
 * (0 = card only, 1 = picker only). Opening runs the progress 0 -> 1, closing 1 -> 0 from wherever it is, so a close that
 * starts mid-open reverses in place and the two directions are exact mirror images (same curves, same easing).
 *
 * Portrait uses it now (the card shrinks and fades while the picker grows and fades in, and back). Landscape's two side
 * by side islands take the same numbers once its close is built.
 *
 * Lengths are base values, scaled by the animation speed of the orientation (and "Remove animations" snaps).
 */
object PickerSwap {
    /** Base length of the swap while the picker opens. */
    const val OPEN_MS = 280

    /** Base length while it closes: a little quicker, the picker is the thing being put away. */
    const val CLOSE_MS = 240

    /**
     * landscape's two islands run their ONE progress with a spring (no bounce). Base stiffness values (the animation
     * speed of the orientation scales them); about as long as [OPEN_MS] / [CLOSE_MS], the close a little stiffer.
     */
    const val SPLIT_OPEN_STIFFNESS = 400f
    const val SPLIT_CLOSE_STIFFNESS = 520f

    /** The card is fully gone (alpha 0) once the progress has reached this share. */
    const val CARD_GONE_AT = 0.6f

    /** The picker starts to show once the progress has passed this share (a short overlap, never two solid layers). */
    const val PICKER_IN_FROM = 0.3f

    /** The card shrinks to this scale at progress 1; the picker starts from the same scale at progress 0. */
    const val END_SCALE = 0.92f

    fun cardAlpha(progress: Float): Float = 1f - (progress / CARD_GONE_AT).coerceIn(0f, 1f)

    fun pickerAlpha(progress: Float): Float = ((progress - PICKER_IN_FROM) / (1f - PICKER_IN_FROM)).coerceIn(0f, 1f)

    fun cardScale(progress: Float): Float = 1f - (1f - END_SCALE) * progress.coerceIn(0f, 1f)

    fun pickerScale(progress: Float): Float = END_SCALE + (1f - END_SCALE) * progress.coerceIn(0f, 1f)

    /** The card layer is composed (it is still visible, or it is the resting state). */
    fun cardLayerShown(progress: Float, pickerOpen: Boolean): Boolean = !pickerOpen || progress < 0.999f

    /** The picker layer is composed (it is visible, or it is the resting state). */
    fun pickerLayerShown(progress: Float, pickerOpen: Boolean): Boolean = pickerOpen || progress > 0.001f

    fun lengthMs(opening: Boolean): Int = if (opening) OPEN_MS else CLOSE_MS
}
