/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * The system's "Animator duration scale" (Developer options, and "Remove animations" in Accessibility, which sets it to 0).
 *
 * Two jobs:
 *  - [timeScale]: the real scale, for plain wall-clock waits that Compose does not know about (the idle-circle close lead
 *    in the ViewModel). 0 means "do not animate": those waits become 0.
 *  - [extraScale]: what [AnimationSpeed] still has to multiply into Compose springs and tweens. Compose's window recomposer
 *    already hands the system scale to every animation on its own (through the coroutine context), so by default this is 1
 *    and nothing is scaled twice. If the overlay's probe finds Compose is NOT applying it ([composeAppliesScale] false),
 *    this becomes the real scale and AnimationSpeed does it itself.
 *
 * At scale 0 a spring gets a huge stiffness and a tween a 1 ms length, so the island snaps to its end state.
 */
object SystemAnimScale {
    /** Largest scale honoured (Developer options offers up to 10x). */
    const val MAX_SCALE = 10f

    /** Stiffness given to a spring when animations are off: it settles in a frame or two. */
    const val SNAP_STIFFNESS = 1_000_000f

    @Volatile
    private var scale: Float = 1f

    /** True until the overlay's probe finds that Compose does not carry the system scale into its animations. */
    @Volatile
    var composeAppliesScale: Boolean = true

    private var observer: ContentObserver? = null

    /** The system scale now (1 when it could not be read). */
    fun timeScale(): Float = scale

    /** True when the system asks for no animation at all. */
    fun animationsOff(): Boolean = scale <= 0f

    /** The part of the scale [AnimationSpeed] must apply itself. */
    fun extraScale(): Float = if (composeAppliesScale) 1f else scale

    /** Reads the setting and keeps it current until [stop]. Safe to call again. */
    fun start(context: Context) {
        val app = context.applicationContext
        scale = read(app)
        if (observer != null) return
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale = read(app)
            }
        }
        runCatching {
            app.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, obs
            )
            observer = obs
        }
    }

    fun stop(context: Context) {
        val obs = observer ?: return
        runCatching { context.applicationContext.contentResolver.unregisterContentObserver(obs) }
        observer = null
    }

    private fun read(context: Context): Float =
        sanitize(
            runCatching {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            }.getOrDefault(1f)
        )

    /** Not a number, negative or absurd values fall back to 1; the rest is clamped to 0..[MAX_SCALE]. */
    fun sanitize(raw: Float): Float = when {
        raw.isNaN() || raw.isInfinite() || raw < 0f -> 1f
        else -> raw.coerceAtMost(MAX_SCALE)
    }

    /** A spring's time scales with 1 / sqrt(stiffness), so to take [scale] times as long its stiffness is divided by scale squared. */
    fun scaledStiffness(base: Float, scale: Float): Float =
        if (scale <= 0f) SNAP_STIFFNESS else base / (scale * scale)

    /** A tween just takes [scale] times as long; never below 1 ms. */
    fun scaledDurationMs(baseMs: Int, scale: Float): Int =
        if (scale <= 0f) 1 else (baseMs * scale).toInt().coerceAtLeast(1)
}
