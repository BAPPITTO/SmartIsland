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
 * Stuck cards after the listener was unbound. While the notification listener is disconnected the
 * system sends no removal callbacks, so a timer, a stopwatch, a recorder, a navigation or a music card whose
 * notification ended meanwhile stays on the island for good. These live in the system shade (the island never cancels
 * them), so after a reconnect the shade is the truth: a card of one of these modes whose key is not there any more is gone.
 *
 * Every other card is left alone on purpose: ordinary notifications, downloads and ringing calls are cancelled from the
 * shade by the island itself, so they are not in it and cannot be checked this way; system events (battery, hotspot,
 * Bluetooth, flashlight) have no notification at all.
 */
object StaleCardSweep {
    /** The modes whose notification stays in the system shade while the card is on the island. */
    val SHADE_RESIDENT_MODES: Set<IslandMode> = setOf(
        IslandMode.Music, IslandMode.Navigation, IslandMode.Timer, IslandMode.Stopwatch,
        IslandMode.ScreenRecording, IslandMode.AudioRecording
    )

    /**
     * Cards the island made itself (the Stopwatch and Timer utilities): they have no notification in the shade, so the shade says
     * nothing about them and they are never dropped here.
     */
    private val ISLAND_OWN_KEYS: Set<String> = setOf(InAppStopwatch.KEY, InAppTimer.KEY)

    /** Keys of the cards to drop: shade-resident modes whose key is missing from [activeKeys]. */
    fun keysToDrop(cards: List<IslandNotification>, activeKeys: Set<String>): List<String> =
        cards.filter {
            it.mode in SHADE_RESIDENT_MODES && it.key.isNotEmpty() && it.key !in activeKeys && it.key !in ISLAND_OWN_KEYS
        }.map { it.key }
}
