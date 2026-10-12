/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.agupta07505.smartisland.model.PrivacyKind

/**
 * Opens the system's own screen that lists which apps use (or last used) a sensor.
 *
 * The small popup SystemUI shows when its green dot is tapped is internal to SystemUI: no other app can open it. The
 * closest thing the system lets a normal app open is the Privacy Dashboard (Android 12+), filtered to the sensor's
 * permission group, which lists the apps and when they used it. Older versions or ROMs without it fall back to the
 * "apps that can use this permission" list and then to the Privacy settings page.
 */
object PrivacyUsage {
    private const val GROUP_CAMERA = "android.permission-group.CAMERA"
    private const val GROUP_MICROPHONE = "android.permission-group.MICROPHONE"
    private const val GROUP_LOCATION = "android.permission-group.LOCATION"

    private const val ACTION_REVIEW_PERMISSION_USAGE = "android.intent.action.REVIEW_PERMISSION_USAGE"
    private const val ACTION_MANAGE_PERMISSION_APPS = "android.intent.action.MANAGE_PERMISSION_APPS"
    private const val EXTRA_PERMISSION_GROUP_NAME = "android.intent.extra.PERMISSION_GROUP_NAME"
    private const val EXTRA_PERMISSION_NAME = "android.intent.extra.PERMISSION_NAME"

    private fun groupOf(kind: PrivacyKind): String? = when (kind) {
        PrivacyKind.Camera -> GROUP_CAMERA
        PrivacyKind.Microphone -> GROUP_MICROPHONE
        PrivacyKind.Location -> GROUP_LOCATION
        PrivacyKind.ScreenShare -> null
    }

    private fun permissionOf(kind: PrivacyKind): String? = when (kind) {
        PrivacyKind.Camera -> "android.permission.CAMERA"
        PrivacyKind.Microphone -> "android.permission.RECORD_AUDIO"
        PrivacyKind.Location -> "android.permission.ACCESS_FINE_LOCATION"
        PrivacyKind.ScreenShare -> null
    }

    /** The screens to try for [kind], best first. */
    fun intentsFor(kind: PrivacyKind): List<Intent> = buildList {
        val group = groupOf(kind)
        val permission = permissionOf(kind)
        if (group != null) {
            add(Intent(ACTION_REVIEW_PERMISSION_USAGE).putExtra(EXTRA_PERMISSION_GROUP_NAME, group))
        } else {
            add(Intent(ACTION_REVIEW_PERMISSION_USAGE))
        }
        if (permission != null) {
            add(Intent(ACTION_MANAGE_PERMISSION_APPS).putExtra(EXTRA_PERMISSION_NAME, permission))
        }
        add(Intent(Settings.ACTION_PRIVACY_SETTINGS))
    }
}

/** Opens the system's "which apps use this" screen for [kind]. False when nothing on the device could open. */
fun Context.openPrivacyUsage(kind: PrivacyKind): Boolean {
    val intents = PrivacyUsage.intentsFor(kind)
    intents.forEachIndexed { index, intent ->
        val last = index == intents.lastIndex
        if (safeStartActivity(intent, errorMessage = if (last) "Privacy settings are not available on your device." else null)) {
            return true
        }
    }
    return false
}
