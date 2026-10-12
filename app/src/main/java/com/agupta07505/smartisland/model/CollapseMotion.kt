/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * One collapse motion for the expanded island, shared by portrait and landscape,
 * for a collapse that lands on a visible pill.
 *
 * The width settles with a small bounce (the pill pinches a little narrower than it should be, then relaxes
 * back); height, corner radius and the vertical progress are critically damped, because the pill is only a
 * few dozen dp tall and any dip would look squashed. Expanding keeps its own spring (`islandSpring*`).
 *
 * Both orientations: a close started by the idle circle (a tap on it, or the idle timer) is different. The
 * collapsed pill it lands on is about to leave, so a bounce at the end would play against a vanishing target. That close
 * is critically damped, and its length comes from [IdleCloseSequence] (step 1 of the two-step close, as long as the idle
 * circle's opening animation lets it be); the visible-pill collapse above stays as it is.
 */
object CollapseMotion {
    /** Same in both orientations. Lower than the portrait expand (400), a touch above the landscape expand (280). */
    const val STIFFNESS = 300f

    /** Width spring: about 4 % overshoot. */
    const val WIDTH_DAMPING = 0.72f

    /** Height, corner radius and the vertical progress: no overshoot. */
    const val SETTLE_DAMPING = 1f

    /** The idle-circle close: no bounce anywhere. Its stiffness is [IdleCloseSequence.Plan.closeStiffness], not a constant. */
    const val IDLE_PRESS_DAMPING = 1f

    /** Damping of the width / progress spring of that collapse; height and radius always use [SETTLE_DAMPING]. */
    fun closeWidthDamping(idlePress: Boolean): Float = if (idlePress) IDLE_PRESS_DAMPING else WIDTH_DAMPING

    /** How far a spring goes past its target, as a fraction of the travelled distance (0 when not underdamped). */
    fun overshootFraction(damping: Float): Float {
        if (damping >= 1f) return 0f
        val z = damping.coerceAtLeast(0.01f)
        return exp(-PI.toFloat() * z / sqrt(1f - z * z))
    }

    /** The most the width dips below the pill's width, for a collapse that travels [travelDp]. */
    fun widthDipDp(travelDp: Float): Float = travelDp.coerceAtLeast(0f) * overshootFraction(WIDTH_DAMPING)

    // ---- The content fades of a collapse -------------------------------------------------------

    /** The pill's own content starts to come back once the width has covered this share of its way (the dip is still to come). */
    const val PILL_CONTENT_HOLD_PROGRESS = 0.95f

    /** Length of the pill content's fade-in, as a share of the orientation's base fade length. */
    const val PILL_CONTENT_IN_SHARE = 0.6f

    /** Length of the card content's fade-out, as a share of the base fade length (it was 0.5 before). */
    const val CARD_CONTENT_OUT_SHARE = 0.6f

    /**
     * How long the pill's content waits before it fades in: the time the width spring (the given, already speed-scaled
     * stiffness and damping) takes to cover [PILL_CONTENT_HOLD_PROGRESS] of its travel. It follows the user's speed.
     */
    fun pillContentDelayMs(stiffness: Float, damping: Float): Int =
        springTimeToProgressMs(stiffness, damping, PILL_CONTENT_HOLD_PROGRESS).toInt()

    /**
     * The same wait, for a collapse that starts when the island is only [expandProgress] (0 = pill,
     * 1 = card) of the way open, e.g. a tap on a card that was opened a moment ago.
     *
     * [pillContentDelayMs] is the time the width needs to come within `1 - PILL_CONTENT_HOLD_PROGRESS` (5 %) of the *full*
     * travel of its way home. From full size that is what it measures. From a smaller start the width is already closer,
     * so the wait is the time to fall from [expandProgress] to that same 5 % of the full travel: shorter, and 0 when the
     * island was never more than 5 % open. Before, the full-size wait was always used, so after a very short expansion the
     * pill's text came back late. The spring's starting speed is ignored (a collapse that interrupts an expansion that is
     * still opening can take a few ms longer than this).
     */
    fun pillContentDelayFromMs(expandProgress: Float, stiffness: Float, damping: Float): Int {
        val remaining = 1f - PILL_CONTENT_HOLD_PROGRESS
        val start = expandProgress.coerceIn(0f, 1f)
        if (start <= remaining) return 0
        return springTimeToProgressMs(stiffness, damping, 1f - remaining / start).toInt()
    }

    fun pillContentInMs(baseMs: Int): Int = (baseMs * PILL_CONTENT_IN_SHARE).toInt().coerceAtLeast(1)

    /** The idle-circle close takes its content fade from the plan ([IdleCloseSequence.Plan.contentOutMs]), not from here. */
    fun cardContentOutMs(baseMs: Int): Int = (baseMs * CARD_CONTENT_OUT_SHARE).toInt().coerceAtLeast(1)
}
