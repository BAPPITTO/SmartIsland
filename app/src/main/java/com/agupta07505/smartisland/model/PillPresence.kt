/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Landscape (and portrait, for an idle-circle tap only, see [IdleCloseSequence]): one state machine for the collapsed cutout leaving (last card gone, idle circle takes over)
 * and coming back (a card arrives while the idle circle shows). Exit and reverse are the SAME motion, driven
 * by one number, "presence": 1 = pill in place, 0 = pill gone. Leaving animates presence 1 -> 0, coming back
 * animates it 0 -> 1 starting from wherever it is. Both run on ONE critically damped spring (no bounce), so an
 * interruption reverses in place and keeps its velocity: it never snaps and never restarts from rest.
 *
 * The idle circle (the camera cutout circle) is not part of this motion. It does not fade, blink or pulse, and the pill
 * neither waits for it nor starts it: the circle is on or off (it stays on while the pill leaves, then the island's own
 * idle cutout takes the same spot).
 * The pill keeps its size in this app, so leaving and coming back share one geometry (no size mismatch).
 */
enum class PillPhase {
    None,
    Leaving,
    Entering,

    /** The pill has just left: the island takes the idle settings with a jump (no glide) for a couple of frames. */
    Settling
}

/** What the state machine does on a composition. */
enum class PillTransition {
    /** Nothing to start or change. */
    None,

    /** Idle shown -> start the pill leaving (from fully present). */
    StartExit,

    /** Idle shown, a card arrives -> start the pill coming back (from fully gone). */
    StartEnter,

    /** Pill is leaving and a card arrives (or the user expands): reverse from the current progress. */
    ReverseToPill,

    /** Pill is coming back and its card goes: reverse from the current progress. */
    ReverseToIdle,

    /** Not eligible any more (portrait, notch mode): drop the effect and show the island as usual. */
    Cancel
}

/** The pill's presence spring: critically damped (no bounce). Stiffness is scaled by the animation speed. */
const val PILL_SPRING_DAMPING = 1f
const val PILL_SPRING_STIFFNESS = 340f

/** The spring counts as settled at this share of its travel (used to cap the merge that rides along with the exit). */
const val PILL_SPRING_SETTLED = 0.98f

/**
 * Progress (0..1) of a Compose spring (mass 1, so omega = sqrt(stiffness)) that starts at rest and is aimed at 1,
 * [tSec] seconds after it started. Closed form for the under-damped and the critically damped case.
 */
fun springProgress(tSec: Float, stiffness: Float, dampingRatio: Float): Float {
    if (tSec <= 0f || stiffness <= 0f) return 0f
    val w = sqrt(stiffness.toDouble())
    val z = dampingRatio.toDouble().coerceIn(0.05, 1.0)
    val t = tSec.toDouble()
    val x = if (z >= 0.999) {
        1.0 - (1.0 + w * t) * exp(-w * t)
    } else {
        val wd = w * sqrt(1.0 - z * z)
        1.0 - exp(-z * w * t) * (cos(wd * t) + (z * w / wd) * sin(wd * t))
    }
    return x.toFloat()
}

/** First millisecond at which that spring has covered [fraction] of its travel (searched up to 3 s). */
fun springTimeToProgressMs(stiffness: Float, dampingRatio: Float, fraction: Float): Long {
    val target = fraction.coerceIn(0f, 0.995f)
    if (target <= 0f) return 0L
    var ms = 0L
    while (ms < 3000L) {
        if (springProgress(ms / 1000f, stiffness, dampingRatio) >= target) return ms
        ms++
    }
    return ms
}

/** How long (ms) the pill's presence spring takes to settle at the given (already speed-scaled) [stiffness]. */
fun pillSpringSettleMs(stiffness: Float): Long =
    springTimeToProgressMs(stiffness, PILL_SPRING_DAMPING, PILL_SPRING_SETTLED)

/**
 * @param eligible landscape (or a portrait idle-circle tap in progress) and not notch mode
 * @param expanded the island is expanded: a new exit or entry never starts, but an exit already running reverses
 * @param hasContent a card is on the island now
 * @param hadPill the previous composition showed a collapsed pill (card, collapsed, eligible)
 * @param hadNoContent the previous composition had no card (the idle circle was showing)
 * @param hasRecord the pill's last settings and geometry are known (it was shown at least once)
 */
fun pillTransition(
    phase: PillPhase,
    eligible: Boolean,
    expanded: Boolean,
    hasContent: Boolean,
    hadPill: Boolean,
    hadNoContent: Boolean,
    hasRecord: Boolean
): PillTransition = when {
    phase != PillPhase.None && !eligible -> PillTransition.Cancel
    // A card arrives (or the island expands) while the pill is leaving: it stops and slides back from where it is.
    phase == PillPhase.Leaving && hasContent -> PillTransition.ReverseToPill
    // The card goes again while the pill is coming back: it turns around and leaves from where it is.
    phase == PillPhase.Entering && !hasContent -> PillTransition.ReverseToIdle
    phase == PillPhase.Leaving || phase == PillPhase.Entering -> PillTransition.None
    !eligible || expanded || !hasRecord -> PillTransition.None
    !hasContent && hadPill -> PillTransition.StartExit
    // A card arrives on the idle circle: the pill slides in from its assigned position (same motion as the exit, backwards).
    hasContent && hadNoContent -> PillTransition.StartEnter
    else -> PillTransition.None
}

/** Slide progress of the pill (0 = in place, 1 = gone) for a [presence] value. Exit and reverse use the same curve. */
fun pillSlideProgress(presence: Float): Float = 1f - presence.coerceIn(0f, 1f)
