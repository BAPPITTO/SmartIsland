/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.agupta07505.smartisland.util.BlurFollow
import com.agupta07505.smartisland.util.runCatchingLogged

/**
 * The blur window of Live blur: a small, empty overlay window sized to the expanded island. The compositor blurs whatever is behind
 * it, inside the window's own rectangle only (`Window.setBackgroundBlurRadius`, API 31+). It is never touchable or focusable and
 * draws nothing itself; the island's tinted shell is drawn over it.
 *
 * It must be the window's BACKGROUND blur. `blurBehindRadius` / `FLAG_BLUR_BEHIND` blur the whole screen behind a window, not the
 * window's rectangle, which is not what the island needs. A background blur only works through a `Window` object, so the overlay
 * is a [Dialog] whose window is an accessibility overlay (the service's context allows that type).
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
    private var dialog: Dialog? = null
    /** What the window was last laid out as (null while hidden). See [BlurFollow.layoutKey]. */
    private var lastKey: IntArray? = null

    /** True where the system can blur behind a window now (API 31+, and not turned off by battery saver, the OEM or a developer option). */
    fun isSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && runCatching { windowManager.isCrossWindowBlurEnabled }.getOrDefault(false)

    /** Adds the (hidden: 1x1, no blur) window. Call before the island window is added. Safe to call twice. */
    fun attach() {
        if (dialog != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val created = runCatchingLogged(TAG, "Blur window create failed") {
            val d = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
            d.setCancelable(false)
            d.setContentView(View(context))
            val w = d.window ?: return@runCatchingLogged null
            w.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setFormat(PixelFormat.TRANSLUCENT)
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Same flags as the island window so that y means the same screen row in both, but never touchable or focusable.
            w.addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            val lp = w.attributes
            lp.gravity = Gravity.TOP or Gravity.START
            lp.width = 1
            lp.height = 1
            lp.x = 0
            lp.y = 0
            lp.dimAmount = 0f
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            w.attributes = lp
            w.setBackgroundBlurRadius(0)
            d.show()
            d
        }
        if (created != null) dialog = created
    }

    /** Removes the window. Safe to call twice. */
    fun detach() {
        val d = dialog ?: return
        dialog = null
        lastKey = null
        runCatchingLogged(TAG, "Blur window removeView failed") {
            d.window?.setBackgroundBlurRadius(0)
            d.dismiss()
        }
    }

    /**
     * Shows the blur over the island at [key] (null hides it). [key] is [BlurFollow.layoutKey] in screen pixels; a relayout only
     * happens when the island moved more than [BlurFollow.TOLERANCE_PX] or the radius changed.
     */
    fun show(key: IntArray?) {
        val d = dialog ?: return
        if (key == null || !isSupported()) {
            hideNow(d)
            return
        }
        if (!BlurFollow.needsRelayout(lastKey, key) && lastKey != null) return
        lastKey = key
        apply(d, key[2], key[3], key[0], key[1], key[4])
    }

    private fun hideNow(d: Dialog) {
        if (lastKey == null) return
        lastKey = null
        // Radius 0 turns the compositor blur off; a 1x1 window costs nothing.
        apply(d, 1, 1, 0, 0, 0)
    }

    private fun apply(d: Dialog, w: Int, h: Int, x: Int, y: Int, blurRadiusPx: Int) {
        val win = d.window ?: return
        runCatchingLogged(TAG, "Blur window layout failed") {
            val lp = win.attributes
            lp.width = w
            lp.height = h
            lp.x = x
            lp.y = y
            win.attributes = lp
            // Background blur: only the window's own rectangle is blurred, whatever is behind that rectangle.
            win.setBackgroundBlurRadius(blurRadiusPx)
        }
    }

    private companion object {
        const val TAG = "IslandBlurWindow"
    }
}
