/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.util.AnimationSpeed
import kotlin.math.exp
import kotlin.math.max

/** Base length (ms, before the speed setting) of the secondary cutout coming back from its merge. */
const val SECONDARY_RETURN_BASE_MS = 460

/**
 * The idle-circle tap, as a two-step close that is exactly as long as the opening it mirrors.
 * Orientation-neutral: portrait and landscape use the same numbers, each with its own animation speed.
 *
 * Opening (a tap on the idle circle brings the stored cards back): the idle circle vanishes, the pill springs in
 * once the vanish is [PILL_ENTER_AT_IDLE_VANISH] done, and the secondary cutout returns from its merge at the same
 * time. [openingMs] adds those up from the very constants the opening animation runs on.
 *
 * Closing ([plan]):
 *  - step 2 ([Plan.step2Ms]) is as long as the pill's arrival ([pillArrivalMs]): the collapsed pill leaves (the presence
 *    spring, at [Plan.exitStiffness]) at the same pace it slides in, and the idle circle blinks in as it always did.
 *  - step 1 ([Plan.step1Ms]) comes first and is [STEP1_OVER_STEP2] times as long as step 2: the expanded island closes and
 *    the secondary cutout merges, together, slower than the pill's own movement. The pill stays.
 *
 * Every length here is in the user's speed (Relaxed / Normal / Snappy already applied) and leaves the system animator
 * scale out; whoever plays it adds that scale ([AnimationSpeed.systemStiffness] for springs,
 * [AnimationSpeed.systemDurationMs] for tweens, `SystemAnimScale.timeScale()` for plain waits).
 */
object IdleCloseSequence {
    /**
     * How many times as long step 1 (expanded close + secondary merge) is as step 2 (the pill leaving, which is as long as
     * the pill arriving). Above 1 the island closes more slowly than the pill slides out. Was a 60 / 40 split of the opening.
     */
    const val STEP1_OVER_STEP2 = 1.5f

    /** No step is ever shorter than this, so a spring has something to run on. */
    const val MIN_STEP_MS = 60

    /** The expanded card's content is gone after this share of step 1 (the shell is still closing under it). */
    const val CONTENT_OUT_SHARE_OF_STEP1 = 0.7f

    /** A spring counts as arrived at this share of its travel (the same bar [PILL_SPRING_SETTLED] uses for the pill). */
    const val SETTLED = PILL_SPRING_SETTLED

    /** What the close plays: lengths in ms and the stiffness of the two critically damped springs (user speed applied). */
    data class Plan(
        val totalMs: Int,
        val step1Ms: Int,
        val step2Ms: Int,
        /** Stiffness of the expanded island's close (width, height, radius, progress). Critically damped. */
        val closeStiffness: Float,
        /** Stiffness of the pill's exit spring. Critically damped. */
        val exitStiffness: Float,
        /** Length of the expanded card's content fade-out. */
        val contentOutMs: Int
    )

    /** Real length (ms, user speed) of the collapsed pill's slide-in spring: the length its slide-out also gets. */
    fun pillArrivalMs(speed: AnimationSpeed): Int =
        pillSpringSettleMs(speed.userStiffness(PILL_SPRING_STIFFNESS)).toInt()

    /**
     * Real length (ms, user speed) of the idle-circle tap's opening animation: the idle circle's vanish up to the
     * hand-over, then the pill's arrival spring; the secondary cutout's return runs next to it, so the longer one wins.
     */
    fun openingMs(speed: AnimationSpeed): Int {
        val vanishMs = speed.userDurationMs(IDLE_VANISH_MS)
        // The pill starts when the idle circle is down to (1 - PILL_ENTER_AT_IDLE_VANISH) of its opacity, which the
        // vanish's FastOutSlowIn easing reaches after this share of its length.
        val handOverMs = (vanishMs * fastOutSlowInTimeFor(PILL_ENTER_AT_IDLE_VANISH)).toInt()
        val pillMs = pillArrivalMs(speed)
        val secondaryMs = speed.userDurationMs(SECONDARY_RETURN_BASE_MS)
        return max(handOverMs + pillMs, secondaryMs)
    }

    /** The close for [speed]: step 2 as long as the pill's arrival, step 1 [STEP1_OVER_STEP2] times that. */
    fun plan(speed: AnimationSpeed): Plan {
        val step2 = pillArrivalMs(speed).coerceAtLeast(MIN_STEP_MS)
        val step1 = (step2 * STEP1_OVER_STEP2).toInt().coerceAtLeast(MIN_STEP_MS)
        return Plan(
            totalMs = step1 + step2,
            step1Ms = step1,
            step2Ms = step2,
            closeStiffness = criticalStiffnessForSettleMs(step1),
            exitStiffness = criticalStiffnessForSettleMs(step2),
            contentOutMs = (step1 * CONTENT_OUT_SHARE_OF_STEP1).toInt().coerceAtLeast(1)
        )
    }

    /**
     * Stiffness of a critically damped spring (mass 1) that is [SETTLED] of the way home after [ms] milliseconds.
     * Closed form: x(t) = 1 - (1 + wt) e^(-wt), so the time is a fixed multiple of 1 / sqrt(stiffness).
     */
    fun criticalStiffnessForSettleMs(ms: Int): Float {
        val x = criticalSettleX(SETTLED)
        val w = x / (ms.coerceAtLeast(1) / 1000.0)
        return (w * w).toFloat()
    }

    /** The value of w * t at which a critically damped spring has covered [fraction] of its travel. */
    private fun criticalSettleX(fraction: Float): Double {
        val target = 1.0 - fraction.toDouble().coerceIn(0.0, 0.999)
        var lo = 0.0
        var hi = 30.0
        repeat(60) {
            val mid = (lo + hi) / 2.0
            // (1 + x) e^-x falls from 1 to 0, and is the share of the travel still to go.
            if ((1.0 + mid) * exp(-mid) > target) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    /**
     * Share of a FastOutSlowIn tween's length (0..1) after which it has covered [progress] of its travel.
     * FastOutSlowIn is the cubic Bezier (0.4, 0, 0.2, 1); solved by bisection on the curve parameter.
     */
    internal fun fastOutSlowInTimeFor(progress: Float): Float {
        val p = progress.toDouble().coerceIn(0.0, 1.0)
        var lo = 0.0
        var hi = 1.0
        repeat(40) {
            val s = (lo + hi) / 2.0
            if (bezier(s, 0.0, 1.0) < p) lo = s else hi = s
        }
        val s = (lo + hi) / 2.0
        return bezier(s, 0.4, 0.2).toFloat()
    }

    /** One coordinate of the cubic Bezier from 0 to 1 with inner control values [c1] and [c2], at parameter [s]. */
    private fun bezier(s: Double, c1: Double, c2: Double): Double {
        val u = 1.0 - s
        return 3.0 * u * u * s * c1 + 3.0 * u * s * s * c2 + s * s * s
    }
}
