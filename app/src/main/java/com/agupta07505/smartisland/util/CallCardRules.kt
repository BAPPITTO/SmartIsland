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
 * Which call cards a newly posted call notification replaces.
 *
 * A VoIP app (WhatsApp, Telegram ...) often posts the ringing notification and the ongoing-call
 * notification under DIFFERENT keys. The island cancels the ringing one from the shade itself, so the
 * system never reports that notification as removed later, and its card stayed on the island with
 * Answer / Decline buttons and "Ringing..." while the shade already showed "Ongoing voice call".
 */
object CallCardRules {

    /**
     * Keys of the app's older call cards to drop when a call notification with [newKey] from
     * [packageName] is posted.
     *
     * A call that is ringing replaces nothing (a second incoming call may arrive while one is running).
     * A call that is dialing or ongoing replaces every other call card of the same app: the ringing card
     * it grew out of, and a stale dialing card.
     */
    fun supersededKeys(
        existing: List<IslandNotification>,
        newKey: String,
        packageName: String,
        newIsRinging: Boolean
    ): List<String> =
        if (newIsRinging) {
            emptyList()
        } else {
            existing
                .filter { it.mode == IslandMode.IncomingCall && it.packageName == packageName && it.key != newKey }
                .map { it.key }
        }
}
