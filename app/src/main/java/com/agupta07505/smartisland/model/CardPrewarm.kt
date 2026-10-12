/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Which cards are composed and measured while the island is still collapsed (so the tap that opens
 * the island does not have to compose them), see `PrewarmedCard`.
 *
 * A pre-composed card is never drawn and takes no touches, but its effects and animations still run while it waits. So
 * only the modes that run nothing continuously are listed (the media card is the exception, see the second overload). The ones left out keep composing on the tap, as before:
 *  - Music (a 33 fps progress loop while playing, but see the second overload), Battery (endless animations), Bluetooth (a 3 s
 *    loop when the battery side shows). Timer, Stopwatch and the recording cards are listed: their pulses only run while shown;
 *  - Empty (the app shortcuts grid decodes icons).
 */
object CardPrewarm {
    private val modes = setOf(
        IslandMode.Notification,
        IslandMode.IncomingCall,
        IslandMode.LiveActivity,
        IslandMode.Navigation,
        IslandMode.DownloadUpload,
        IslandMode.Hotspot,
        IslandMode.Flashlight,
        // The live cards with a pulse or a clock. Their endless animations stop while the card waits (LocalCardShown), so
        // they cost a composition, not a frame loop, and the swipe finds their real height.
        IslandMode.Timer,
        IslandMode.Stopwatch,
        IslandMode.ScreenRecording,
        IslandMode.AudioRecording
    )

    /** Cards that cost (almost) nothing while they wait, so they are pre-composed whenever there is a card. */
    fun shouldPrewarm(mode: IslandMode?): Boolean = mode != null && mode in modes

    /**
     * Same, plus the cards that are only worth waiting for while someone can tap them: the media card. It is pre-composed
     * only while the screen is on, and its loops (position, wave) pause while it is not shown (`LocalCardShown`).
     */
    fun shouldPrewarm(mode: IslandMode?, screenOn: Boolean): Boolean =
        shouldPrewarm(mode) || (screenOn && mode == IslandMode.Music)
}
