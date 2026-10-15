/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import com.agupta07505.smartisland.model.IslandNotificationAction

/**
 * Reads what an app put into its notification through custom [RemoteViews] instead of the standard
 * fields. Some clocks and voice recorders do that: the running time lives in a Chronometer view
 * (the notification's `when` is only the moment it was posted) and Pause / Stop are buttons inside
 * the view, so neither shows up in `Notification.actions`.
 *
 * The views are inflated off screen with the public [RemoteViews.apply]. Nothing is pressed while
 * inspecting. Each button view is kept in [liveButtons] and the island gets a self-addressed
 * broadcast PendingIntent for it; when that fires, [click] presses the kept view, which makes
 * RemoteViews send the app's own PendingIntent exactly as in the shade.
 * Everything is best effort; any failure returns null and the island falls back to the standard
 * fields.
 */
object NotificationViewInspector {

    data class Result(
        /** Time the view's chronometer has been running, if the view has one. */
        val chronometerElapsedMs: Long?,
        /** Clickable buttons found in the view, in layout order. */
        val buttons: List<IslandNotificationAction>,
        /**
         * Time left on a count-DOWN chronometer in the view (a running clock timer), measured from its base.
         * Null when the view has no count-down chronometer or it already ran out.
         */
        val countdownRemainingMs: Long? = null,
        /**
         * The clock time the shade itself prints in the view ("04:59", "1:02:03"), in seconds: the chronometer's
         * own text first, then the first other text that is only a clock time. A paused timer's chronometer does
         * not tick, so this is the value the shade shows while paused.
         */
        val shownTimeSeconds: Long? = null,
        /** True when [shownTimeSeconds] is the chronometer's own text, not some other text in the view. */
        val shownFromChronometer: Boolean = false
    )

    @Suppress("DEPRECATION")
    fun inspect(context: Context, notification: Notification, key: String): Result? {
        // Largest layout first: it has the full set of buttons.
        val candidates = listOfNotNull(
            notification.bigContentView,
            notification.contentView,
            notification.headsUpContentView
        )
        if (candidates.isEmpty()) return null

        var elapsed: Long? = null
        var countdown: Long? = null
        var shown: Long? = null
        var shownFromChrono = false
        var buttons: List<IslandNotificationAction> = emptyList()
        var buttonViews: List<View> = emptyList()
        for (views in candidates) {
            val (result, kept) = runCatching { inflate(context, views, key) }
                .onFailure { android.util.Log.d(TAG, "Could not inflate notification views: ${it.message}") }
                .getOrNull() ?: continue
            if (elapsed == null) elapsed = result.chronometerElapsedMs
            if (countdown == null) countdown = result.countdownRemainingMs
            if (shown == null && result.shownTimeSeconds != null) {
                shown = result.shownTimeSeconds
                shownFromChrono = result.shownFromChronometer
            }
            if (buttons.isEmpty()) {
                buttons = result.buttons
                buttonViews = kept
            }
            if ((elapsed != null || countdown != null || shown != null) && buttons.isNotEmpty()) break
        }
        // Only the layout whose buttons the island shows is kept for pressing.
        if (buttonViews.isEmpty()) liveButtons.remove(key) else liveButtons[key] = buttonViews
        if (elapsed == null && countdown == null && shown == null && buttons.isEmpty()) return null
        android.util.Log.d(
            TAG,
            "Inspected custom views: elapsedMs=$elapsed countdownMs=$countdown shownSec=$shown buttons=${buttons.map { it.title }}"
        )
        return Result(elapsed, buttons, countdown, shown, shownFromChrono)
    }

    private fun inflate(context: Context, views: RemoteViews, key: String): Pair<Result, List<View>> {
        val root = views.apply(context, FrameLayout(context))

        var chronometer: Chronometer? = null
        var otherClockText: Long? = null
        val clickable = ArrayList<View>()
        walk(root) { v ->
            if (v is Chronometer && chronometer == null) chronometer = v
            if (v.hasOnClickListeners()) clickable.add(v)
            // A plain text that is only a clock time ("04:59"): what a paused timer prints.
            if (v is TextView && v !is Chronometer && otherClockText == null) {
                otherClockText = clockOnly(v.text?.toString())
            }
        }

        // Several clickable views can share a label (an icon and its text): keep one of each.
        val kept = ArrayList<View>()
        val buttons = ArrayList<IslandNotificationAction>()
        for (v in clickable) {
            val label = (v as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }
                ?: v.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                ?: idName(v)
            if (buttons.any { it.title == label }) continue
            buttons.add(IslandNotificationAction(title = label, pendingIntent = clickIntent(context, key, kept.size)))
            kept.add(v)
        }

        val elapsed = chronometer?.let { c ->
            if (c.isCountDown) null else (SystemClock.elapsedRealtime() - c.base).takeIf { it >= 0 }
        }
        val countdown = chronometer?.let { c ->
            if (c.isCountDown) (c.base - SystemClock.elapsedRealtime()).takeIf { it > 0 } else null
        }
        // The chronometer's own text is what the shade shows; a stopped one keeps the value it had when set.
        val chronoText = chronometer?.let { clockOnly(it.text?.toString()) }
        val shown = chronoText ?: otherClockText
        return Result(elapsed, buttons, countdown, shown, chronoText != null) to kept
    }

    /** Seconds of a text that is nothing but a clock time ("04:59", "1:02:03"), else null. */
    private fun clockOnly(text: String?): Long? {
        val t = text?.trim().orEmpty()
        if (t.isEmpty() || t.length > 9 || !CLOCK_ONLY.matches(t)) return null
        return TimerStopwatchParser.parseClockTimeToSeconds(t)
    }

    /** Self-addressed broadcast; the data URI keeps one distinct PendingIntent per key and button. */
    private fun clickIntent(context: Context, key: String, index: Int): PendingIntent {
        val uri = Uri.Builder()
            .scheme(SCHEME)
            .authority("click")
            .appendQueryParameter("key", key)
            .appendQueryParameter("i", index.toString())
            .build()
        val intent = Intent(ACTION_CLICK, uri).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Presses the kept view for a broadcast made by [clickIntent]. Call on the main thread. */
    fun click(uri: Uri?) {
        val key = uri?.getQueryParameter("key") ?: return
        val index = uri.getQueryParameter("i")?.toIntOrNull() ?: return
        val view = liveButtons[key]?.getOrNull(index) ?: return
        runCatching { view.performClick() }
            .onFailure { android.util.Log.d(TAG, "Could not press notification button: ${it.message}") }
    }

    /** Drops the kept views of a notification that is gone. */
    fun forget(key: String) {
        liveButtons.remove(key)
    }

    fun forgetAll() {
        liveButtons.clear()
    }

    private fun walk(view: View, visit: (View) -> Unit) {
        visit(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), visit)
        }
    }

    /** "pause_button" for a button with no label: the id name still says what it does. */
    private fun idName(view: View): String = runCatching {
        if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else ""
    }.getOrDefault("").replace('_', ' ')

    const val ACTION_CLICK = "com.agupta07505.smartisland.action.PRESS_NOTIFICATION_VIEW"
    const val SCHEME = "smartisland-view"
    private const val TAG = "NotificationViewInspector"
    private val CLOCK_ONLY = Regex("""-?(?:\d{1,2}:)?\d{1,2}:\d{2}""")

    /** Off-screen button views per notification key, indexed like the island's buttons. */
    private val liveButtons = java.util.concurrent.ConcurrentHashMap<String, List<View>>()
}
