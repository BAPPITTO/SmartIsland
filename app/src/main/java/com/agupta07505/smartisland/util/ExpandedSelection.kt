/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification

/**
 * What an OPEN island does when the card it is showing disappears on its own (a system event ends, an app
 * cancels its notification, an event switch turns it off).
 *
 * Before this rule the open island kept its position in the list, so it silently landed on the next card:
 * when the "Plugged in" card ended, an old notification that had been sitting in the stack showed up expanded,
 * as if it had just arrived. Only a new notification may open the island by itself, so now the island closes.
 */
object ExpandedSelection {

    sealed interface Outcome {
        /** Nothing to do here: the card is still there, or an older rule handles it. */
        data object Keep : Outcome

        /** Close the island: the card is gone and nothing replaced it. */
        data object Collapse : Outcome

        /** The app posted a replacement for the card that went (new key, same app): show that one. */
        data class Select(val index: Int) : Outcome
    }

    /**
     * The one card that is routinely replaced by a card with a different key: a call that is still ringing or dialing
     * becomes the ongoing call (answered, connected). Collapsing between the removal and the new post would close the
     * island in the middle of answering a call, so this card keeps the old behaviour. Every other live event that ends
     * (music, transfer, timer, recording, battery, an ended call ...) closes the island.
     */
    private fun isReplacedUnderNewKey(card: IslandNotification): Boolean =
        card.mode == IslandMode.IncomingCall && (card.isCallRinging || card.isCallConnecting)

    /**
     * [previous] and [current] are the lists the island shows (same order as the island), [selectedIndex] is
     * the card being shown in [previous].
     */
    fun afterSelectedGone(
        previous: List<IslandNotification>,
        selectedIndex: Int,
        current: List<IslandNotification>
    ): Outcome {
        val gone = previous.getOrNull(selectedIndex) ?: return Outcome.Keep
        if (current.any { it.key == gone.key }) return Outcome.Keep
        if (isReplacedUnderNewKey(gone)) return Outcome.Keep
        // Only an ordinary notification may be replaced by the same app's new card. A live event that ended closes the island,
        // whatever the same app (or any other) posts next.
        if (gone.mode != IslandMode.Notification) return Outcome.Collapse
        val previousKeys = previous.mapTo(HashSet()) { it.key }
        val replacement = current.indexOfFirst { it.key !in previousKeys && it.packageName == gone.packageName }
        return if (replacement >= 0) Outcome.Select(replacement) else Outcome.Collapse
    }

    /**
     * An open island that shows the screen recorder closes the moment the recording ends, whatever else is waiting in
     * the list (an old notification, one that already auto-expanded, a card that is switched off must never take the recorder's
     * place). The recording has ended when the shown card is no longer a recording card: its key is gone, or it was re-posted
     * as something else (the "saved" notification under the same key). The one exception is a call: a ringing or connecting call
     * in [current] keeps the island as the call logic wants it.
     */
    fun recorderEnded(previous: List<IslandNotification>, selectedIndex: Int, current: List<IslandNotification>): Boolean {
        val shown = previous.getOrNull(selectedIndex) ?: return false
        if (shown.mode != IslandMode.ScreenRecording) return false
        if (current.any { it.key == shown.key && it.mode == IslandMode.ScreenRecording }) return false
        return current.none { it.mode == IslandMode.IncomingCall && (it.isCallRinging || it.isCallConnecting) }
    }
}
