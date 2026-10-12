/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.SwipeAction

/**
 * A short lock on swiping, shared by the touches of one island. It starts when a swipe that was armed is pulled back
 * into the dead zone and lasts [SwipeArming.LOCK_MS]; while it runs no swipe can arm (no tick), lifting fires nothing,
 * and a lift followed by a new touch stays locked until it ends. Taps are not affected.
 *
 * Times are plain milliseconds from any monotonic clock (the caller passes the clock, so this stays testable).
 */
class SwipeLock(private val lengthMs: Long = SwipeArming.LOCK_MS) {
    private var lockedUntilMs: Long = NEVER

    /** Starts (or restarts) the lock at [nowMs]. */
    fun start(nowMs: Long) {
        lockedUntilMs = nowMs + lengthMs
    }

    fun isLocked(nowMs: Long): Boolean = nowMs < lockedUntilMs

    /** Milliseconds left at [nowMs]; 0 when not locked. */
    fun remainingMs(nowMs: Long): Long = if (isLocked(nowMs)) lockedUntilMs - nowMs else 0L

    fun clear() {
        lockedUntilMs = NEVER
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE
    }
}

/**
 * How long the lock cue stays fully shown: the lock's [remainingMs] minus the time it takes to fade in ([inMs]) and out
 * ([outMs]), so the cue is gone the moment the lock ends. Never negative.
 */
fun lockCueHoldMs(remainingMs: Long, inMs: Long, outMs: Long): Long = (remainingMs - inMs - outMs).coerceAtLeast(0L)

/**
 * Tracks whether a swipe that is still in progress would fire if the finger lifted now.
 *
 * A swipe is *armed* once the drag passes [threshold] in one clear direction ([GestureSensitivity.pillSwipe]). It stays
 * armed until the drag comes back inside [disarmFraction] of the threshold along that direction, so a finger jittering
 * at the edge does not flip the state every frame. Dragging back inside that zone cancels the swipe: lifting then does
 * nothing. [direction] is what lifting would fire right now (null = nothing).
 *
 * Pulling an armed swipe back also starts the [lock] (see [SwipeLock]) when [lockWhen] accepts the direction that was
 * armed; while it runs, [update] arms nothing. [enteredLock] is true only for the [update] call that started it.
 *
 * The collapsed pill fires on lift using [direction]. The expanded island decides on lift with the plain threshold, so
 * it uses a [disarmFraction] of 1 and this class only drives the tick that says "lifting now will fire".
 */
class SwipeArming(
    private val sensitivity: GestureSensitivity,
    private val threshold: Float,
    private val disarmFraction: Float = DEFAULT_DISARM_FRACTION,
    private val lock: SwipeLock = SwipeLock(),
    /** Own threshold for left / right (the expanded island asks for a longer sideways swipe than its vertical one). */
    private val horizontalThreshold: Float = threshold
) {
    var direction: PillSwipeDirection? = null
        private set

    /** True only right after an [update] that pulled an armed swipe back and started the lock. */
    var enteredLock: Boolean = false
        private set

    /**
     * Which armed directions start the lock when pulled back. A direction that did nothing visible (a side set to "None")
     * was never "armed" for the user, so pulling it back must not lock. Default: all of them.
     */
    var lockWhen: (PillSwipeDirection) -> Boolean = { true }

    /** Feeds the drag so far at [nowMs]. Returns true when armed / disarmed or the direction flipped. */
    fun update(dx: Float, dy: Float, nowMs: Long): Boolean {
        val before = direction
        enteredLock = false
        if (lock.isLocked(nowMs)) {
            direction = null
            return before != null
        }
        val now = sensitivity.pillSwipe(dx, dy, threshold, horizontalThreshold)
        direction = when {
            now != null -> now
            before != null && along(before, dx, dy) >=
                (if (before == PillSwipeDirection.Left || before == PillSwipeDirection.Right) horizontalThreshold else threshold) * disarmFraction -> before
            else -> null
        }
        // Armed, then back in the dead zone: swiping is locked for a moment.
        if (before != null && direction == null && lockWhen(before)) {
            lock.start(nowMs)
            enteredLock = true
        }
        return direction != before
    }

    /** Forgets the drag (new touch). The lock is not touched: it outlives the touch. */
    fun reset() {
        direction = null
        enteredLock = false
    }

    private fun along(d: PillSwipeDirection, dx: Float, dy: Float): Float = when (d) {
        PillSwipeDirection.Left -> -dx
        PillSwipeDirection.Right -> dx
        PillSwipeDirection.Up -> -dy
        PillSwipeDirection.Down -> dy
    }

    companion object {
        /** Armed swipes cancel once the drag is back under this share of the threshold. */
        const val DEFAULT_DISARM_FRACTION = 0.75f

        /**
         * How long swiping stays locked after an armed swipe is pulled back into the dead zone (ms). Long enough
         * that a jittery thumb cannot re-arm at once, short enough that a deliberate second try is not stuck.
         * 300 to 500 is the range worth trying on a device.
         */
        const val LOCK_MS = 400L
    }
}

/** True when running [action] on the collapsed pill puts a different card in the pill (so the new one slides in). */
fun swipeChangesCard(action: SwipeAction): Boolean = when (action) {
    SwipeAction.NextPrevious, SwipeAction.NextNotification, SwipeAction.PreviousNotification,
    SwipeAction.NextEvent, SwipeAction.PreviousEvent,
    SwipeAction.NextSibling, SwipeAction.PreviousSibling -> true
    else -> false
}

/**
 * How far the pill's content follows the finger (px): [drag] x [FOLLOW], never more than [maxSlide]. Past that point the
 * content stays put while the finger keeps going.
 */
fun pillContentFollow(drag: Float, maxSlide: Float): Float = (drag * PILL_CONTENT_FOLLOW).coerceIn(-maxSlide, maxSlide)

const val PILL_CONTENT_FOLLOW = 0.6f

/**
 * A follow that eases into its limit instead of hitting it: starts with the given [slope] (x of the drag, like the plain follow) and
 * flattens smoothly towards [max] (tanh), so the content or card never stops dead under the finger. Odd, and 0 at 0.
 */
fun softFollow(drag: Float, slope: Float, max: Float): Float =
    if (max <= 0f) 0f else max * kotlin.math.tanh(drag * slope / max)

/**
 * How far (px) the expanded card follows the finger sideways: [drag] x [CARD_FOLLOW], never more than
 * [maxSlide]. Past that point the card stays put while the finger keeps going.
 */
fun expandedCardFollow(drag: Float, maxSlide: Float): Float = (drag * CARD_FOLLOW).coerceIn(-maxSlide, maxSlide)

/** How far (dp) the expanded card can slide sideways: 30 % of the island's width, between 24 and 120 dp. */
fun expandedCardMaxSlideDp(islandWidthDp: Float): Float = (islandWidthDp * 0.3f).coerceIn(24f, 120f)

const val CARD_FOLLOW = 0.85f
