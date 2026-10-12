/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps an older notification snapshot from overwriting a newer one for the same key.
 *
 * Every post is handled on its own coroutine, so when an app updates one notification several times
 * within a second (WhatsApp posts ringing, then ongoing, under the same key) the handlers can finish
 * out of order and the OLD ringing snapshot lands last: the island kept saying "Ringing" during the
 * call. The VoIP reconcile has the same problem: it reads the active notifications, scans them for a
 * few seconds, then promotes its snapshot, even when the call has since ended, which is the call
 * timer that appeared after hanging up.
 *
 * The listener callbacks arrive in order, so they record what is newest / gone; a handler asks
 * [isStale] before it posts.
 */
class StalePostGuard {
    private val newestPostTime = ConcurrentHashMap<String, Long>()
    private val removedPostTime = ConcurrentHashMap<String, Long>()

    /** Record a post (in callback order). */
    fun notePosted(key: String, postTime: Long) {
        newestPostTime.merge(key, postTime) { old, new -> maxOf(old, new) }
        trim()
    }

    /** Record that the notification with [postTime] was genuinely removed (not our own shade cancel). */
    fun noteRemoved(key: String, postTime: Long) {
        removedPostTime.merge(key, postTime) { old, new -> maxOf(old, new) }
        trim()
    }

    /** True when a snapshot posted at [postTime] is older than a newer post, or was already removed. */
    fun isStale(key: String, postTime: Long): Boolean {
        val newest = newestPostTime[key]
        if (newest != null && postTime < newest) return true
        val removed = removedPostTime[key]
        return removed != null && postTime <= removed
    }

    fun clear() {
        newestPostTime.clear()
        removedPostTime.clear()
    }

    private fun trim() {
        if (newestPostTime.size > MAX_KEYS || removedPostTime.size > MAX_KEYS) {
            clear()
        }
    }

    private companion object {
        const val MAX_KEYS = 400
    }
}
