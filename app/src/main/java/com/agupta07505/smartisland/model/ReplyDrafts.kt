/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Half-typed quick replies, kept per notification key so a draft survives Cancel, a collapse of the island, the
 * notification shade opening, or another card taking over. A draft ends when the reply is sent or the notification is
 * gone ([retainOnly]). Not observable on purpose: the reply box reads it once when it is created and writes on every edit.
 */
class ReplyDrafts {
    private val drafts = HashMap<String, String>()

    /**
     * Key of the card whose reply box must open again (typing resumes after a call took the island). The card
     * opens its box when it shows this key; the view model clears it once typing is active again.
     */
    val resume = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    @Synchronized
    fun get(key: String): String = drafts[key].orEmpty()

    /** Blank text is not a draft. */
    @Synchronized
    fun set(key: String, text: String) {
        if (text.isEmpty()) drafts.remove(key) else drafts[key] = text
    }

    @Synchronized
    fun clear(key: String) {
        drafts.remove(key)
    }

    /** Forgets the drafts of notifications that no longer exist. */
    @Synchronized
    fun retainOnly(liveKeys: Set<String>) {
        drafts.keys.retainAll(liveKeys)
    }
}
