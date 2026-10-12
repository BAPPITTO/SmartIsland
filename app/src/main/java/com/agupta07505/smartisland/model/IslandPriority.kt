/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Which card owns the primary (main) cutout and which one goes to the secondary round cutout.
 *
 * Order, highest first: Navigation > Live updates > Music (playing before paused) > Downloads (downloads, uploads,
 * backups, copies and the other transfer kinds) > ordinary notifications. The second card in this
 * order is the one shown in the secondary round cutout. Among ordinary notifications the newest comes first, so
 * the latest one is in the pill and the one before it in the secondary cutout (other kinds keep arrival order).
 *
 * Modes outside that list are placed by judgment, change the numbers here to move them:
 * a ringing/active call tops everything, other ongoing activities (timer, stopwatch, recorders,
 * hotspot, flashlight) share the live-updates level, and system cards (Bluetooth, Wi-Fi, VPN) rank with
 * ordinary notifications.
 *
 * Short events (notifications, battery, Bluetooth) never take the primary cutout from a live event (see [popsUp]). Battery events:
 * they take the secondary cutout right behind the primary card, live event or not. With a live event on the pill they never
 * pop into the primary cutout over it; with none they pop into it for a moment when they arrive, then ordinary
 * notifications keep it and the battery event stays in the secondary cutout.
 */
object IslandPriority {

    fun rank(mode: IslandMode): Int = when (mode) {
        IslandMode.IncomingCall -> 0
        IslandMode.Navigation -> 10
        IslandMode.LiveActivity,
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording,
        IslandMode.Hotspot,
        IslandMode.Flashlight -> 20
        IslandMode.Music -> 30
        IslandMode.DownloadUpload -> 40
        IslandMode.Notification,
        IslandMode.Battery,
        IslandMode.Bluetooth,
        IslandMode.Empty -> 50
    }

    /** A paused player ranks just below a playing one, so playing media gets the main cutout. */
    fun rank(n: IslandNotification): Int =
        rank(n.mode) + if (n.mode == IslandMode.Music && !n.mediaIsPlaying) 1 else 0

    /**
     * Cards that fall back to the idle cutout after [com.agupta07505.smartisland.util.IslandTimings.STATIC_IDLE_TIMEOUT_MS]
     * once nothing on the island is actively running: ordinary notifications, paused music, and
     * paused stopwatch / timer / recorders, and paused downloads and uploads. Anything running (playing media, a ticking timer,
     * navigation, downloads, calls ...) is NOT idled out by this rule, so it keeps ordinary cards on the island; the live
     * idle timeout in IslandViewModel puts such a card away on its own timer unless the user opened it or it is a call.
     * Resuming brings the card back.
     */
    fun canIdleOut(n: IslandNotification): Boolean = when (n.mode) {
        IslandMode.Notification -> true
        IslandMode.Music -> !n.mediaIsPlaying
        IslandMode.Stopwatch,
        IslandMode.Timer,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording -> n.recordingPaused
        IslandMode.DownloadUpload -> n.transferState == TransferState.PAUSED
        else -> false
    }

    /**
     * Short events that pop into the primary cutout for a moment when they arrive: ordinary notifications, battery and
     * connection cards (Bluetooth, Wi-Fi, VPN). They pop up only when no live event is on the island ([popsUp] with the
     * cards): over a live event they wait in the secondary cutout instead.
     */
    fun popsUp(mode: IslandMode): Boolean = when (mode) {
        IslandMode.Notification, IslandMode.Battery, IslandMode.Bluetooth -> true
        else -> false
    }

    /**
     * [popsUp] for [card] given the cards on the island: no short event (notification, battery, Bluetooth) ever takes the
     * primary cutout from a live event. While one is live the newcomer waits behind it (the secondary cutout shows it).
     */
    fun popsUp(card: IslandNotification, cards: List<IslandNotification>): Boolean =
        popsUp(card.mode) && !hasLive(cards)

    /**
     * A live event: a call, navigation, a live activity, a timer, stopwatch or recording, a hotspot, a flashlight, music
     * or a transfer. Ordinary notifications and the system cards (battery, Bluetooth) are not.
     */
    fun isLive(mode: IslandMode): Boolean = mode != IslandMode.Empty && rank(mode) < rank(IslandMode.Notification)

    fun hasLive(cards: List<IslandNotification>): Boolean = cards.any { isLive(it.mode) }

    /**
     * Where the battery events of [sorted] (already in priority order) sit: right behind the card in the primary cutout,
     * live event or notification, so the first of them is in the secondary cutout.
     */
    private fun placeBattery(sorted: List<IslandNotification>): List<IslandNotification> {
        val battery = sorted.filter { it.mode == IslandMode.Battery }
        if (battery.isEmpty()) return sorted
        val rest = sorted.filter { it.mode != IslandMode.Battery }
        if (rest.isEmpty()) return sorted
        return listOf(rest.first()) + battery + rest.drop(1)
    }

    /**
     * Cards in display order. Equal ranks keep their arrival order, except ordinary notifications, which are
     * newest first (by [IslandNotification.timeMillis]; equal times keep arrival order). The card with
     * [popupKey], if any, goes first until its popup is over.
     */
    fun order(list: List<IslandNotification>, popupKey: String? = null): List<IslandNotification> {
        if (list.size < 2) return list
        val sorted = placeBattery(newestNotificationsFirst(list.sortedBy { rank(it) })) // both sorts are stable
        if (popupKey == null) return sorted
        val popup = sorted.firstOrNull { it.key == popupKey } ?: return sorted
        if (!popsUp(popup, list)) return sorted // a short event over a live event never takes the primary cutout
        return listOf(popup) + sorted.filter { it !== popup }
    }

    /**
     * Puts the ordinary notifications of [ordered] newest first, in the slots they already occupy, so the cards
     * around them (battery, Bluetooth, which share their rank) keep their places.
     */
    private fun newestNotificationsFirst(ordered: List<IslandNotification>): List<IslandNotification> {
        val newest = ordered.filter { it.mode == IslandMode.Notification }.sortedByDescending { it.timeMillis }
        if (newest.size < 2) return ordered
        val it = newest.iterator()
        return ordered.map { card -> if (card.mode == IslandMode.Notification) it.next() else card }
    }
}
