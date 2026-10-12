/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlin.math.roundToInt

/**
 * A card step (preview, reader, picture) that is following the finger (model/StepDrag.kt). The gesture
 * handler writes it, the expanded content reads it to run the island's height and the card's brightness from the old step to
 * the new one. Provided once at the top of the overlay, like [LocalReaderScroll].
 */
class StepDragState {
    /** A step is open under the finger or settling after the lift. */
    var active by mutableStateOf(false)

    /**
     * The finger is up and the step has finished or gone back: the real card is the only copy drawn (and tappable) again, while
     * the shell may still be handing its height back.
     */
    var settled by mutableStateOf(false)

    /**
     * The expanded content is drawing the two landscape islands (the picker or a picture is open, or still shrinking
     * away). The overlay keeps its two-island window and surface until this is false, instead of guessing the animation's length.
     */
    var splitShown by mutableStateOf(false)

    /**
     * The overlay's side of the close hand-off: the shell has taken over as the single island (surface, clip and window are back).
     * The content keeps drawing the two-island layout until this is true (model/SplitMotion.holdTwoIslandLayout), so the single
     * layout is never drawn without its surface. The content clears it when a split opens; the overlay sets it once the shell is ready.
     */
    var shellSingleReady by mutableStateOf(true)

    /**
     * A left / right swipe that may bring another card is being dragged. 0 = none; 1 = the next page is the likely
     * target, -1 = the previous page, 2 = a card change with no side preference (see [com.agupta07505.smartisland.util.swipeNeighbourHint]).
     * The expanded content measures the neighbouring cards at once instead of after the idle delay, so the shell already knows the
     * height it is heading for when the finger lifts.
     */
    var swipeNeighbour by mutableIntStateOf(0)

    /**
     * Landscape split: the ONE split progress (0 = card only, 1 = card and the island beside it), set by the expanded content while it
     * draws the two islands and null otherwise. The shell reads it while measuring to take its width from the progress directly
     * (model/SplitShell.kt). Plain value, written by the content, read at layout time.
     */
    var splitProgressNow: (() -> Float)? = null

    /** True from the first frame the shell followed [splitProgressNow] until its own width is back at the single island's. Plain value. */
    private var splitShellLatched = false
    private var splitShellLatchedAtMs = 0L

    /**
     * The width (dp) the shell must have for the landscape split, or a negative number when it follows its own width. While the
     * split is drawn it is the progress's width; after the split has gone it stays the single island's width until the shell's own
     * width ([own]) is there too, so the hand-over between the two never shows a wider or narrower shell.
     */
    fun splitShellWidthDp(splitNow: Boolean, expanded: Boolean, single: Float, total: Float, own: Float): Float {
        val reader = splitProgressNow
        if (splitNow && expanded && reader != null) {
            splitShellLatched = true
            splitShellLatchedAtMs = android.os.SystemClock.uptimeMillis()
            return com.agupta07505.smartisland.model.SplitShell.width(single, total, reader())
        }
        if (splitShellLatched) {
            val expired = android.os.SystemClock.uptimeMillis() - splitShellLatchedAtMs > com.agupta07505.smartisland.model.SplitShell.HANDOVER_MAX_MS
            if (!expanded || expired || com.agupta07505.smartisland.model.SplitShell.handoverDone(own, single)) {
                splitShellLatched = false
            } else {
                return single
            }
        }
        return -1f
    }

    /** Landscape split: the single island's height (dp) and the split row's height (dp), kept up to date by the expanded content. Plain values. */
    var splitSingleHeightDp: Float = 0f
    var splitTotalHeightDp: Float = 0f
    private var splitHeightLatched = false
    private var splitHeightLatchedAtMs = 0L

    /**
     * The height (dp) the shell must have for the landscape split, or a negative number when it follows its own height. Like
     * [splitShellWidthDp]: while the split is drawn it is the progress's height; after the split has gone it stays the single
     * island's height until the shell's own height ([own]) is there too, so the height never runs as a second stage.
     */
    fun splitShellHeightDp(splitNow: Boolean, expanded: Boolean, own: Float): Float {
        val reader = splitProgressNow
        val single = splitSingleHeightDp
        val total = splitTotalHeightDp
        if (splitNow && expanded && reader != null && single > 0f && total > 0f) {
            splitHeightLatched = true
            splitHeightLatchedAtMs = android.os.SystemClock.uptimeMillis()
            return com.agupta07505.smartisland.model.SplitShell.height(single, total, reader())
        }
        if (splitHeightLatched) {
            val expired = android.os.SystemClock.uptimeMillis() - splitHeightLatchedAtMs > com.agupta07505.smartisland.model.SplitShell.HANDOVER_MAX_MS
            if (!expanded || expired || single <= 0f ||
                kotlin.math.abs(own - single) <= com.agupta07505.smartisland.model.SplitShell.HANDOVER_TOLERANCE_DP
            ) {
                splitHeightLatched = false
            } else {
                return single
            }
        }
        return -1f
    }

    /** 0..1: how far the island has gone from the old step's height to the new step's. */
    var progress by mutableFloatStateOf(0f)

    /**
     * The shell's height (dp) while a step follows the finger, set by the gesture handler every move; negative = not in use.
     * The shell reads it while measuring (no recomposition) and skips its own height spring, so it keeps up with the finger.
     */
    var heightDp by mutableFloatStateOf(-1f)

    /** The shell's height (dp) when the drag began. Plain value: written once per drag. */
    var fromDp: Float = 0f

    /** The natural height (dp) of the step now shown, kept up to date by the expanded content. Plain value. */
    var targetDp: Float = 0f

    /** The shell's own (spring) height (dp) as of its last measure. Plain value, written by the shell. */
    var shellHeightDp: Float = 0f

    /** Key of the card the step belongs to. */
    var cardKey: String? = null

    /** The step the card was at before the drag; the copy that fades out keeps showing it. Plain value, kept by the card. */
    var fromSteps: com.agupta07505.smartisland.model.CardSteps? = null

    /** The height the shell takes for [progress] of the way from [fromDp] to [targetDp]. */
    fun currentHeightDp(): Float = com.agupta07505.smartisland.model.StepDrag.heightDp(fromDp, targetDp, progress)

    /** Everything goes back to rest (the shell's own height takes over). */
    fun reset() {
        active = false
        settled = false
        progress = 0f
        heightDp = -1f
        cardKey = null
    }
}

val LocalStepDrag = compositionLocalOf { StepDragState() }

/**
 * Where a card sits when the slot it is placed in is taller than the card: centred, as before, but pinned to the TOP while a
 * card step follows the finger (or is being handed over). The slot's height is the finger-driven shell height there, which differs
 * from the card's own height for the whole drag, so a centred card slid down and snapped back up: the avatar and text were drawn
 * lower than their place for the first frames of a step down, and lower again while a step up shrank the shell. Read at layout
 * time (the state is read inside [align]), so toggling it does not recompose anything.
 */
internal class StepAwareVerticalAlignment(private val stepDrag: StepDragState) : Alignment.Vertical {
    override fun align(size: Int, space: Int): Int =
        if (stepDrag.active) 0 else ((space - size) / 2f).roundToInt()
}

@Composable
internal fun rememberStepAwareAlignment(): Alignment.Vertical {
    val stepDrag = LocalStepDrag.current
    return remember(stepDrag) { StepAwareVerticalAlignment(stepDrag) }
}
