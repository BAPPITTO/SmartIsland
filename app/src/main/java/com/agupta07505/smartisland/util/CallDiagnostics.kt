/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.agupta07505.smartisland.model.IslandMode

/**
 * Describes call-looking notifications for the in-app diagnostic log, so a missed VoIP call can be
 * traced to the exact fields the app posted and the decision Smart Island took. Every read is
 * guarded: a malformed bundle must never break notification handling.
 */
object CallDiagnostics {
    private const val COMPAT_TEMPLATE_KEY = "androidx.core.app.extras.COMPAT_TEMPLATE"

    fun isCallLike(n: Notification): Boolean {
        val extras = n.extras
        val text = runCatching {
            "${extras?.getCharSequence(Notification.EXTRA_TITLE)} ${extras?.getCharSequence(Notification.EXTRA_TEXT)}"
        }.getOrDefault("")
        return n.category == Notification.CATEGORY_CALL ||
            CallStyleExtras.isCallTemplate(n) ||
            CallStyleExtras.channelLooksLikeCall(n) ||
            n.fullScreenIntent != null ||
            VoipCallDetector.textLooksLikeCall(text) ||
            CallStyleExtras.looksRinging(n)
    }

    fun describe(sbn: StatusBarNotification, mode: IslandMode, suppressed: Boolean): String {
        val n = sbn.notification
        val extras = n.extras
        fun str(key: String) = runCatching { extras?.getCharSequence(key)?.toString() }.getOrNull()
        val actions = runCatching {
            n.actions.orEmpty().joinToString { "'${it.title}'${if (it.actionIntent == null) "(no intent)" else ""}" }
        }.getOrDefault("?")
        val keys = runCatching { extras?.keySet()?.sorted()?.joinToString() }.getOrNull()
        return buildString {
            append("call candidate pkg=${sbn.packageName} key=${sbn.key}")
            append(" mode=$mode suppressedFromIsland=$suppressed")
            append(" category=${n.category} channel=${n.channelId} flags=0x${Integer.toHexString(n.flags)}")
            append(" template=${runCatching { extras?.getString(Notification.EXTRA_TEMPLATE) }.getOrNull()}")
            append(" compatTemplate=${runCatching { extras?.getString(COMPAT_TEMPLATE_KEY) }.getOrNull()}")
            append(" callType=${CallStyleExtras.callType(n)} fullScreenIntent=${n.fullScreenIntent != null}")
            append(" strictRinging=${CallStyleExtras.isRinging(n)} looksRinging=${CallStyleExtras.looksRinging(n)}")
            append(" chronometer=${runCatching { extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) }.getOrNull()}")
            append(" title='${str(Notification.EXTRA_TITLE)}' text='${str(Notification.EXTRA_TEXT)}'")
            append(" sub='${str(Notification.EXTRA_SUB_TEXT)}' actions=[$actions] extras=[$keys]")
        }
    }
}
