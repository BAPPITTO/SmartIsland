/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/** Label in front of a message the user sent from the island's reply box. */
const val SENT_REPLY_LABEL = "You: "

/**
 * The card after a reply was sent from the island: the typed message is added to the card itself (short text and
 * the long text the reader shows) instead of the card being closed and the app's echo coming back as a new one.
 * When the app re-posts the same key, its own text replaces this.
 */
fun IslandNotification.withSentReply(message: String): IslandNotification {
    val sent = message.trim()
    if (sent.isEmpty()) return this
    val line = SENT_REPLY_LABEL + sent
    val history = bigText.ifBlank { text }
    return copy(
        text = line,
        bigText = if (history.isBlank()) line else history + "\n" + line
    )
}
