/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.abs
import kotlin.math.exp

/**
 * The numbers behind a card step (preview, reader, picture) that follows the finger. A vertical drag
 * moves the step's progress 0..1; the island's height runs from the height it had to the height of the new step, and the
 * new step's content brightens with it. Lifting past [COMMIT_PROGRESS] finishes the step, lifting short of it goes back.
 */
object StepDrag {
    /** How far (dp) the finger travels, past the tap slop, for the step to be fully open. */
    const val RANGE_DP = 110f

    /** Lifting at or beyond this progress completes the step; below it the step goes back. */
    const val COMMIT_PROGRESS = 0.4f

    /** After the lift the shell keeps the dragged height until its own spring is this close (dp), or [HANDOVER_MAX_MS] has passed. */
    const val HANDOVER_TOLERANCE_DP = 1.5f
    const val HANDOVER_MAX_MS = 300L

    /** Base length of the finish / go-back animation after the lift (speed setting applied by the caller). */
    const val SETTLE_MS = 240

    /** The go-back (short pull) settle is shorter than a finish: nothing new is being revealed, the old step just returns. */
    const val GO_BACK_SETTLE_MS = 160

    /** The drag has reached its far end (the step is fully open). */
    const val MAX_PROGRESS = 1f

    fun progress(signedDragPx: Float, slopPx: Float, rangePx: Float): Float =
        if (rangePx <= 0f) 0f else ((signedDragPx - slopPx) / rangePx).coerceIn(0f, 1f)

    fun commits(progress: Float): Boolean = progress >= COMMIT_PROGRESS

    /**
     * Light smoothing of the finger-driven progress. Raw touch samples arrive unevenly (and a quick flick delivers a few big
     * jumps), which showed as a stepped, janky height. The shown progress now eases toward the finger's progress with this time
     * constant (about 75 ms: enough to hide the steps, short enough that the island still feels attached to the finger). The
     * commit decision at the lift still uses the finger's own progress, not the eased one.
     */
    const val SMOOTH_TAU_MS = 75f

    /** Closer than this to the finger's progress, the shown progress just lands on it (no endless tiny writes). */
    const val SMOOTH_SNAP = 0.002f

    /**
     * One frame of the ease: [current] moves toward [target] by `1 - e^(-dt / tau)` of the gap. Frame-rate independent, never
     * overshoots, and an idle finger lets it arrive exactly.
     */
    fun smoothed(current: Float, target: Float, dtMs: Float): Float {
        if (dtMs <= 0f) return current
        val gap = target - current
        if (abs(gap) <= SMOOTH_SNAP) return target
        return current + gap * (1f - exp(-dtMs / SMOOTH_TAU_MS))
    }

    /**
     * The old and the new step trade places within the first [CROSSFADE_END] of the move, not over all of it, so
     * the two copies of the card (drawn at two heights while the shell resizes) overlap for a short part of the drag only. The two
     * alphas add up to 1 at every progress, so the card never goes see-through in between.
     */
    const val CROSSFADE_END = 0.4f

    /** The step being left: 1 at the start of the move, 0 from [CROSSFADE_END] on. */
    fun oldStepAlpha(progress: Float): Float = 1f - newStepAlpha(progress)

    /** The step being opened: 0 at the start of the move, 1 from [CROSSFADE_END] on. */
    fun newStepAlpha(progress: Float): Float = (progress / CROSSFADE_END).coerceIn(0f, 1f)

    fun heightDp(fromDp: Float, targetDp: Float, progress: Float): Float =
        fromDp + (targetDp - fromDp) * progress.coerceIn(0f, 1f)
}

/**
 * One buzz per step drag, never two. It plays the moment the finger reaches the far end of the drag ([StepDrag.MAX_PROGRESS]);
 * a drag lifted past [StepDrag.COMMIT_PROGRESS] without ever reaching the end buzzes once at the lift instead. Taps (arrow,
 * thumbnail) do not use this: they buzz once at the tap. Make a new one for every touch.
 */
class StepDragHaptic {
    private var played = false

    /** Feed the drag's progress; true exactly once, when it first reaches the end. */
    fun onProgress(progress: Float): Boolean {
        if (played || progress < StepDrag.MAX_PROGRESS) return false
        played = true
        return true
    }

    /** At the lift: true when the step finishes and nothing has buzzed for this drag yet. */
    fun onLift(committed: Boolean): Boolean {
        val fire = committed && !played
        played = true
        return fire
    }
}
