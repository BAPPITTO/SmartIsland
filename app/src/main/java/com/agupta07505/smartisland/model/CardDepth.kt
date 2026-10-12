/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** How far into a notification card the expanded island is. */
enum class CardDepth { First, Preview, Reader, Selector, PictureMid, Picture }

/**
 * The steps of an expanded notification card, as plain values (the view model holds them, the card, its arrow and the
 * swipe gestures all read and write them, so they cannot disagree).
 *
 * First (text only) -> Preview (last text left, last picture right; only with 2 or more pictures) -> Reader (all text,
 * thumbnails) -> PictureMid (one picture at half size) -> Picture (the same picture at full size). A card with exactly one
 * picture has no preview: First -> Reader -> PictureMid -> Picture. A long text without pictures is First -> Reader.
 * A card with 2 or more pictures has the image [selector] instead of PictureMid: Reader -> Selector (all pictures at half
 * size, only a tap on one opens it) -> Picture; swipe up goes Picture -> Selector -> Preview. In portrait the selector is
 * inside the card, in landscape it is an island beside it. The caller passes a media count of 0 for a card that has a
 * sticker: stickers have no picture steps, no preview and no arrows.
 * [preview] and [reader] stay set while a picture is open ([pictureIndex] != null): the card keeps its reader state.
 * [pictureHalf] says the open picture is at the half-size step (see [PictureSize]); [selector] says the selector is open or is
 * the step a full picture came from.
 */
data class CardSteps(
    val preview: Boolean = false,
    val reader: Boolean = false,
    val pictureIndex: Int? = null,
    val pictureHalf: Boolean = false,
    val selector: Boolean = false
) {
    val depth: CardDepth
        get() = when {
            pictureIndex != null -> if (pictureHalf) CardDepth.PictureMid else CardDepth.Picture
            selector -> CardDepth.Selector
            reader -> CardDepth.Reader
            preview -> CardDepth.Preview
            else -> CardDepth.First
        }

    companion object {
        val First = CardSteps()

        /** The card's arrow: one step in, and from the reader straight back to the first state. */
        fun arrow(steps: CardSteps, mediaCount: Int): CardSteps = when {
            steps.reader -> First
            mediaCount >= 2 && steps.preview -> CardSteps(reader = true)
            mediaCount >= 2 -> CardSteps(preview = true)
            else -> CardSteps(reader = true)
        }

        /**
         * Swipe down: one step deeper, or null when there is none (the last step, a selector that only a tap leaves, or a card
         * with nothing to open). [textExpandable] says whether a card without pictures has a cut-off text to open.
         * With 2 or more pictures the reader opens the selector, not a picture.
         */
        fun deeper(steps: CardSteps, mediaCount: Int, textExpandable: Boolean): CardSteps? = when (steps.depth) {
            CardDepth.First -> when {
                mediaCount >= 2 -> CardSteps(preview = true)
                mediaCount == 1 || textExpandable -> CardSteps(reader = true)
                else -> null
            }
            CardDepth.Preview -> CardSteps(reader = true)
            CardDepth.Reader -> when {
                mediaCount >= 2 -> CardSteps(reader = true, preview = steps.preview, selector = true)
                mediaCount >= 1 -> CardSteps(reader = true, preview = steps.preview, pictureIndex = mediaCount - 1, pictureHalf = true)
                else -> null
            }
            CardDepth.Selector -> null
            CardDepth.PictureMid -> steps.copy(pictureHalf = false)
            CardDepth.Picture -> null
        }

        /** Swipe up: one step back (picture -> half picture or selector -> reader or preview -> first), or null at the first state. */
        fun back(steps: CardSteps, mediaCount: Int): CardSteps? = when (steps.depth) {
            CardDepth.First -> null
            CardDepth.Preview -> First
            CardDepth.Reader -> if (mediaCount >= 2) CardSteps(preview = true) else First
            CardDepth.Selector -> CardSteps(preview = true)
            CardDepth.PictureMid -> CardSteps(reader = true)
            CardDepth.Picture -> if (steps.selector) steps.copy(pictureIndex = null, pictureHalf = false) else steps.copy(pictureHalf = true)
        }

        /** A tap on one picture in the selector opens it at full size. */
        fun pickFromSelector(steps: CardSteps, index: Int): CardSteps = steps.copy(pictureIndex = index, pictureHalf = false, selector = true)
    }
}

/**
 * What the view model remembers about the open step of ONE card ([key]). A different card (or none) is always at the first
 * state, so a stale value never shows on the wrong card. The open picture lives in the view model's `expandedMedia`.
 */
data class CardStepState(
    val key: String? = null,
    val preview: Boolean = false,
    val reader: Boolean = false,
    /** The open picture (see [steps]) is at the half-size step. Only counts while a picture of this card is open. */
    val pictureHalf: Boolean = false,
    /** The image selector is open (or is the step the open picture came from). */
    val selector: Boolean = false,
    /** Reported by the card: a text without pictures is cut off, so it has a reader to open. */
    val textExpandable: Boolean = false
) {
    fun steps(forKey: String?, pictureKey: String?, pictureIndex: Int): CardSteps {
        if (forKey == null) return CardSteps.First
        val picture = if (pictureKey == forKey) pictureIndex else null
        if (key != forKey) return CardSteps(pictureIndex = picture)
        return CardSteps(preview = preview, reader = reader, pictureIndex = picture, pictureHalf = picture != null && pictureHalf, selector = selector)
    }
}

/**
 * Heights (dp) of the picture steps of a notification card. The half-size picture sits halfway between the card's smallest
 * height and the full-size picture, which is the tallest an expanded island ever gets.
 */
object PictureSize {
    /** The smallest height an expanded notification card has. */
    const val CARD_MIN_DP = 95f

    /** The full-size picture's height, the tallest an expanded island grows. */
    const val FULL_DP = 260f

    /** The half-size picture's height: halfway between [CARD_MIN_DP] and [FULL_DP]. */
    const val MID_DP = (CARD_MIN_DP + FULL_DP) / 2f

    fun heightDp(half: Boolean): Float = if (half) MID_DP else FULL_DP
}
