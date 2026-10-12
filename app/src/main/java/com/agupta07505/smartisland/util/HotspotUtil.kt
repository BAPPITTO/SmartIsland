/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object HotspotUtil {
    /**
     * Shell commands that start and stop Wi-Fi tethering on Android 11+ (the tethering service). They are what people use from adb,
     * but they are not part of a public interface and some ROMs change them, which is why the app only uses them after the
     * Shizuku check (see [ShizukuMethodCheck]) saw them work on this phone.
     */
    internal const val START_COMMAND = "service call tethering 4 null s16 random"
    internal const val STOP_COMMAND = "service call tethering 5 s16 random"

    /**
     * Whether the hotspot is on. Reads the platform's own flag when the ROM allows it; otherwise it goes by the island's Hotspot
     * card, which the notification listener shows while the hotspot's notification is up.
     */
    fun isOn(context: Context): Boolean {
        val byFlag = runCatching {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            android.net.wifi.WifiManager::class.java.getMethod("isWifiApEnabled").invoke(wifi) as Boolean
        }.getOrNull()
        if (byFlag != null) return byFlag
        return runCatching {
            com.agupta07505.smartisland.di.SmartIslandRepositories.notificationRepository(context).notifications.value
                .any { it.mode == com.agupta07505.smartisland.model.IslandMode.Hotspot }
        }.getOrDefault(false)
    }

    /** Switches the hotspot with the Shizuku shell command that matches its current state. Blocking: call it off the main thread. */
    suspend fun toggleWithShizuku(context: Context) {
        val command = if (isOn(context)) STOP_COMMAND else START_COMMAND
        ShizukuManager.runShellForOutput(command)
    }

    fun parseDeviceCount(title: String?, text: String?): Int {
        val fullText = "${title.orEmpty()} ${text.orEmpty()}"
        val lower = fullText.lowercase()

        if (lower.contains("no device") || lower.contains("0 device") || lower.contains("no connected") || lower.contains("0 connected")) {
            return 0
        }

        // Pattern 1: "1 device", "2 devices", "1 connected", "2 clients"
        val pattern1 = Regex("""\b(\d+)\s*(?:device|connected|client)s?\b""", RegexOption.IGNORE_CASE)
        pattern1.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // Pattern 2: "devices: 1", "connected: 2", "clients: 0"
        val pattern2 = Regex("""\b(?:devices?|connected|clients?)\s*[:=]?\s*(\d+)\b""", RegexOption.IGNORE_CASE)
        pattern2.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        // Pattern 3: "1 connected device"
        val pattern3 = Regex("""\b(\d+)\s+connected\s+devices?\b""", RegexOption.IGNORE_CASE)
        pattern3.find(fullText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

        return 0
    }

    /**
     * Opens the device's Hotspot & Tethering configuration page.
     * Uses contentIntent if available, otherwise attempts platform and OEM specific tethering intents.
     */
    fun openHotspotSettings(context: Context, contentIntent: PendingIntent? = null, floating: Boolean = false) {
        if (contentIntent != null) {
            val sent = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle()
                    contentIntent.send(context, 0, null, null, null, null, options)
                } else {
                    try {
                        contentIntent.send(context, 0, null)
                    } catch (e: Exception) {
                        contentIntent.send()
                    }
                }
                true
            }.getOrDefault(false)
            if (sent) return
        }

        val intents = listOf(
            // 1. Android Standard WiFi Tethering Settings (API 30+)
            Intent("android.settings.WIFI_TETHER_SETTINGS"),
            // 2. Android Standard Tethering Settings (API 26+)
            Intent("android.settings.TETHER_SETTINGS"),
            // 3. Xiaomi / HyperOS / MIUI Component Tethering
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.wifi.tether.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")),
            Intent("miui.intent.action.TETHER_SETTINGS"),
            // 4. Samsung Hotspot Settings
            Intent("com.samsung.android.settings.WIFI_AP_SETTINGS"),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity")),
            // 5. General Wireless / Network Settings Fallbacks
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            // The utility asks for a floating window first; any page the phone cannot float opens full screen.
            val launched = (floating && UtilityBridge.openIntentFloating?.invoke(Intent(intent)) == true) || runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (launched) return
        }

        Toast.makeText(context, "Opening Hotspot settings...", Toast.LENGTH_SHORT).show()
    }
}
