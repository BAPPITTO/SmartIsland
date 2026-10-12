/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Things the island reports as "in use". Declaration order is the priority order: when several start
 * at once, the earlier one gets the secondary cutout first.
 */
enum class PrivacyKind(val eventId: String) {
    Camera(IslandEvent.PRIVACY_CAMERA.id),
    Microphone(IslandEvent.PRIVACY_MICROPHONE.id),
    Location(IslandEvent.PRIVACY_LOCATION.id),
    ScreenShare(IslandEvent.PRIVACY_MEDIA_PROJECTION.id)
}

/**
 * What the overlay draws for privacy.
 *  - [active]: everything in use right now. Drives the small dot in the collapsed pill.
 *  - [shown]: the one indicator that holds the secondary round cutout, for [PrivacyIndicatorController.HOLD_MS].
 *  - [leaving]: the indicator that just let go; for [PrivacyIndicatorController.MERGE_MS] its cutout stays so it can
 *    merge into the main pill (same animation as a notification's secondary cutout) instead of vanishing.
 */
data class PrivacyIndicatorState(
    val active: Set<PrivacyKind> = emptySet(),
    val shown: PrivacyKind? = null,
    val leaving: PrivacyKind? = null
) {
    val hasAnything: Boolean get() = active.isNotEmpty() || shown != null || leaving != null

    /** True while an indicator holds the secondary round cutout (any kind), including while it merges away. */
    val bubbleShown: Boolean get() = shown != null || leaving != null

    /** Which kind the secondary cutout draws: the one showing, else the one merging away. */
    val bubbleKind: PrivacyKind? get() = shown ?: leaving

    /** True while the cutout is merging into the pill (its hold is over). */
    val merging: Boolean get() = shown == null && leaving != null

    /** Kept for older call sites: the cast icon is the one holding the cutout. */
    val mediaProjectionActive: Boolean get() = shown == PrivacyKind.ScreenShare
}

/** Dots shown in the pill, in a fixed order, at most [PILL_DOTS_MAX]. */
fun pillDotKinds(active: Set<PrivacyKind>): List<PrivacyKind> =
    PrivacyKind.entries.filter { it in active }.take(PILL_DOTS_MAX)

const val PILL_DOTS_MAX = 4

/**
 * Turns "what is in use" into "what the secondary cutout shows".
 *
 * Every indicator, screen sharing included, follows the same rules:
 *  - it shows when it STARTS (a kind going from not in use to in use), not while it merely stays on;
 *  - it holds the cutout for [holdMs] (2 s) and then lets go, even if the sensor is still in use
 *    (the pill then keeps a small dot, see [PrivacyIndicatorState.active]);
 *  - one indicator at a time; if several start together they take turns in [PrivacyKind] order;
 *  - one that stops before its turn comes is dropped; one already showing finishes its 2 s.
 */
class PrivacyIndicatorController(private val holdMs: Long = HOLD_MS) {
    private var previous: Set<PrivacyKind> = emptySet()
    private val pending = mutableListOf<PrivacyKind>()
    private var current: PrivacyKind? = null
    private var shownUntil = 0L
    private var leaving: PrivacyKind? = null
    private var leavingUntil = 0L

    fun update(active: Set<PrivacyKind>, nowMs: Long): PrivacyIndicatorState {
        for (kind in PrivacyKind.entries) {
            val started = kind in active && kind !in previous
            if (started && kind != current && kind !in pending) pending.add(kind)
        }
        pending.retainAll(active)
        previous = active

        if (current != null && nowMs >= shownUntil) {
            leaving = current
            leavingUntil = nowMs + MERGE_MS
            current = null
        }
        if (current == null && pending.isNotEmpty()) {
            val next = pending.minByOrNull { it.ordinal }!!
            pending.remove(next)
            current = next
            shownUntil = nowMs + holdMs
        }
        // A new indicator takes the cutout straight away; otherwise the old one merges away first.
        if (current != null || nowMs >= leavingUntil) leaving = null
        return PrivacyIndicatorState(active = active, shown = current, leaving = leaving)
    }

    fun reset() {
        previous = emptySet()
        pending.clear()
        current = null
        leaving = null
    }

    companion object {
        const val HOLD_MS = 2_000L

        /** How long the cutout stays after the hold so the merge can play (covers Relaxed speed, bubble style). */
        const val MERGE_MS = 1_200L
    }
}

/**
 * True when a second round cutout sits beside the main one: either a second card exists, or the
 * screen is being shared (the cast icon takes that slot). Never in notch mode.
 */
fun hasSecondaryCutout(notificationCount: Int, screenShareActive: Boolean, notchMode: Boolean): Boolean =
    !notchMode && (notificationCount >= 2 || screenShareActive)

/**
 * Screen sharing is shown only when something other than the recorder holds the projection: a
 * screen recorder is also a media projection, so while a screen-recording card exists the cast
 * icon stays hidden and the recording is shown as a recording only.
 */
fun screenShareIndicatorActive(mediaProjectionActive: Boolean, cards: List<IslandNotification>): Boolean =
    mediaProjectionActive && cards.none { it.mode == IslandMode.ScreenRecording }

/**
 * Decides when the cast icon may show. A screen recorder holds a media projection too, and the
 * projection can show up before the recorder's card does (or linger after it is gone), so a bare
 * "projection active and no recorder card" check made the cast icon pop up during recordings.
 *
 * The icon shows only when the projection has been active for [START_DELAY_MS] (2 s) (time for the
 * recorder's card to arrive), no recorder card or known recorder app holds it, and no recorder card
 * was seen in the last [RECORDER_GRACE_MS] (the card can disappear before the projection ends).
 */
class ScreenShareGate(
    private val startDelayMs: Long = START_DELAY_MS,
    private val recorderGraceMs: Long = RECORDER_GRACE_MS
) {
    private var activeSince: Long? = null
    private var recorderSeenAt: Long = Long.MIN_VALUE / 2

    fun shouldShow(projectionActive: Boolean, recorderPresent: Boolean, nowMs: Long): Boolean {
        if (!projectionActive) {
            activeSince = null
            if (recorderPresent) recorderSeenAt = nowMs
            return false
        }
        val since = activeSince ?: nowMs.also { activeSince = it }
        if (recorderPresent) {
            recorderSeenAt = nowMs
            return false
        }
        if (nowMs - recorderSeenAt < recorderGraceMs) return false
        return nowMs - since >= startDelayMs
    }

    companion object {
        const val START_DELAY_MS = 2_000L
        const val RECORDER_GRACE_MS = 8_000L
    }
}
