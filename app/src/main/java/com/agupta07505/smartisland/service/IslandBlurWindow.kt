/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.agupta07505.smartisland.util.BlurFollow
import com.agupta07505.smartisland.util.runCatchingLogged

/**
 * The blur window of Live blur: a small, empty overlay window sized to the expanded island. The compositor blurs whatever is behind
 * it, inside its own rectangle only (`FLAG_BLUR_BEHIND` + `blurBehindRadius`, API 31+). It is never touchable or focusable and
 * draws nothing itself; Phase 5 draws the island's tinted shell over it.
 *
 * Z-order: windows of one type stack in the order they were added, so [attach] must run before the island window is added,
 * or the blur window would sit on top of the island and blur its content.
 *
 * Only the service touches this class, on the main thread.
 */
internal class IslandBlurWindow(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var view: View? = null
    /** What the window was last laid out as (null while hidden). See [BlurFollow.layoutKey]. */
    private var lastKey: IntArray? = null

    /** True where the system can blur behind a window now (API 31+, and not turned off by battery saver, the OEM or a developer option). */
    fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && runCatching { windowManager.isCrossWindowBlurEnabled }.getOrDefault(false)

    /** Adds the (hidden) window. Call before the island window is added. Safe to call twice. */
    fun attach() {
        if (view != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val v = View(context).apply { visibility = View.GONE }
        val added = runCatchingLogged(TAG, "Blur window addView failed") {
            windowManager.addView(v, baseParams(1, 1, 0, 0, 0))
            true
        }
        if (added == true) view = v
    }

    /** Removes the window. Safe to call twice. */
    fun detach() {
        val v = view ?: return
        view = null
        lastKey = null
        runCatchingLogged(TAG, "Blur window removeView failed") {
            if (v.isAttachedToWindow) windowManager.removeViewImmediate(v)
        }
    }

    /**
     * Shows the blur over the island at [key] (null hides it). [key] is [BlurFollow.layoutKey] in screen pixels; a relayout only
     * happens when the island moved more than [BlurFollow.TOLERANCE_PX] or the radius changed.
     */
    fun show(key: IntArray?) {
        val v = view ?: return
        if (key == null || !isSupported()) {
            hideNow(v)
            return
        }
        if (!BlurFollow.needsRelayout(lastKey, key) && v.visibility == View.VISIBLE) return
        lastKey = key
        val params = baseParams(key[2], key[3], key[0], key[1], key[4])
        runCatchingLogged(TAG, "Blur window layout failed") {
            if (v.visibility != View.VISIBLE) v.visibility = View.VISIBLE
            windowManager.updateViewLayout(v, params)
        }
    }

    private fun hideNow(v: View) {
        if (lastKey == null && v.visibility == View.GONE) return
        lastKey = null
        runCatchingLogged(TAG, "Blur window hide failed") {
            v.visibility = View.GONE
            // Radius 0 turns the compositor blur off; a 1x1 window costs nothing.
            windowManager.updateViewLayout(v, baseParams(1, 1, 0, 0, 0))
        }
    }

    private fun baseParams(w: Int, h: Int, x: Int, y: Int, blurRadiusPx: Int): WindowManager.LayoutParams {
        // Same flags as the island window so that y means the same screen row in both, but never touchable or focusable.
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (blurRadiusPx > 0) {
            @Suppress("DEPRECATION")
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        }
        return WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurBehindRadius = blurRadiusPx
            }
        }
    }

    private companion object {
        const val TAG = "IslandBlurWindow"
    }
}
