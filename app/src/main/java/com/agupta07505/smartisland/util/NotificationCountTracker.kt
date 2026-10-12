/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import com.agupta07505.smartisland.model.CountEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The notifications the count idle widget counts, kept by the notification listener service (the only part of the app that sees the system shade)
 * and read by the widget (ui/IdleWidgets.kt, via [rememberIdleWidgetData]). Each entry is one notification in the shade or one card the island
 * holds, already merged per key ([com.agupta07505.smartisland.model.NotificationCounts.merge]); what counts is decided later by
 * [com.agupta07505.smartisland.model.NotificationCounts.summarize].
 */
object NotificationCountTracker {
    private val state = MutableStateFlow<List<CountEntry>>(emptyList())
    val entries: StateFlow<List<CountEntry>> = state

    fun update(entries: List<CountEntry>) {
        if (state.value != entries) state.value = entries
    }

    /** The listener lost its connection: nothing is known any more. */
    fun clear() {
        state.value = emptyList()
    }
}
