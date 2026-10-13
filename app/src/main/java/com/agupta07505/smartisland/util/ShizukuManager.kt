/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {

    /** Package names tried when the permission lookup finds nothing: the official Shizuku app. */
    private val KNOWN_PACKAGES = listOf("moe.shizuku.privileged.api")

    /**
     * The package that declares Shizuku's API permission, or null. Any app that declares it counts (the official app, a fork or a
     * renamed build), so this does not depend on one package name.
     */
    fun findPackageName(context: Context): String? = runCatching {
        context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0).packageName
    }.getOrNull()

    /**
     * Checks if Shizuku is installed. Tried in order, the first hit wins:
     * 1. the app that declares Shizuku's API permission (any fork or renamed build);
     * 2. a known package name (the official app);
     * 3. a live Shizuku service, which can only exist if something provides it (this also covers Sui and phones where package
     *    visibility hides the app from this one).
     * Crash proof against Throwable (including unit test stubs).
     */
    fun isInstalled(context: Context): Boolean {
        if (findPackageName(context) != null) return true
        val byName = KNOWN_PACKAGES.any { name ->
            try {
                context.packageManager.getPackageInfo(name, 0)
                true
            } catch (t: Throwable) {
                false
            }
        }
        return byName || isBinderAvailable()
    }

    /**
     * Safely checks if the Shizuku service binder is currently alive and responsive.
     * Catches any Throwable (DeadObjectException, ExceptionInInitializerError, RemoteException).
     */
    fun isBinderAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Safely checks if Shizuku API permission has been granted to Smart Island.
     */
    fun hasPermission(): Boolean {
        if (!isBinderAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Safely requests Shizuku permission. Does not crash if Shizuku service is dead.
     */
    fun requestPermission(requestCode: Int = 1001) {
        if (!isBinderAvailable()) return
        try {
            Shizuku.requestPermission(requestCode)
        } catch (t: Throwable) {
            android.util.Log.e("ShizukuManager", "Failed to request Shizuku permission", t)
        }
    }

    private fun runShizukuCommands(commands: List<String>): Result<String> {
        if (!hasPermission()) {
            return Result.failure(IllegalStateException("Shizuku permission not granted or service binder offline."))
        }
        return runCatching {
            val fullScript = commands.joinToString("; ")
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", fullScript), null, null) as Process

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0 || output.isNotBlank() || error.isBlank()) {
                "Permissions auto-granted successfully via Shizuku."
            } else {
                throw RuntimeException("Shizuku command error (exit $exitCode): $error $output")
            }
        }
    }

    /**
     * Runs one shell command through Shizuku and returns its stdout. Used for read-only dumps
     * (e.g. `dumpsys media_projection`). Fails when Shizuku is not running or not permitted.
     */
    suspend fun runShellForOutput(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext Result.failure(IllegalStateException("Shizuku permission not granted or service binder offline."))
        }
        runCatching {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()
            output
        }
    }

    /**
     * Runs one shell command through Shizuku and returns its raw stdout bytes (at most [maxBytes], else
     * null). Blocking: call it from a background thread. Used to read a picture file the app itself
     * may not open (see [NotificationMediaReader]).
     */
    fun runShellForBytes(command: String, maxBytes: Int): ByteArray? {
        if (!hasPermission()) return null
        return runCatching {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val out = java.io.ByteArrayOutputStream()
            var tooBig = false
            process.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > maxBytes) { tooBig = true; break }
                }
            }
            if (tooBig) process.destroy() else process.waitFor()
            if (tooBig) null else out.toByteArray()
        }.getOrNull()
    }

    internal fun mergeColonSeparated(currentList: String, newEntry: String): String {
        val list = currentList.split(':').filter { it.isNotBlank() }.toMutableList()
        if (!list.contains(newEntry)) {
            list.add(newEntry)
        }
        return list.joinToString(":")
    }

    internal fun getMergedAccessibilityServices(context: Context, serviceComponent: String): String {
        val current = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        return mergeColonSeparated(current, serviceComponent)
    }

    internal fun getMergedNotificationListeners(context: Context, listenerComponent: String): String {
        val current = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        return mergeColonSeparated(current, listenerComponent)
    }

    /** How much the 1-tap setup grants: only what the island needs to work, or that plus every optional permission. */
    enum class SetupScope { Necessary, All }

    private const val SETUP_PREFS = "shizuku_setup"
    private const val SETUP_SCOPE_KEY = "scope"

    /** The scope of the last setup that succeeded on this phone, or null when none has run. */
    fun savedScope(context: Context): SetupScope? = runCatching {
        val name = context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE).getString(SETUP_SCOPE_KEY, null)
        SetupScope.entries.firstOrNull { it.name == name }
    }.getOrNull()

    private fun saveScope(context: Context, scope: SetupScope) {
        runCatching {
            context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE).edit().putString(SETUP_SCOPE_KEY, scope.name).apply()
        }
    }

    /**
     * The shell commands of one setup. Necessary: everything the island needs to work, and the steps that take many screens by hand:
     * - Allow restricted settings (Android 13+, needed before accessibility and notification access can be switched on)
     * - Accessibility Service and System Alert Window (preserving existing accessibility services)
     * - Notification Listener Access (preserving existing notification listeners)
     * - Hidden notification text (RECEIVE_SENSITIVE_NOTIFICATIONS)
     * - Modify system settings (WRITE_SETTINGS, the brightness slider)
     * - Notifications and Nearby devices (Bluetooth) runtime permissions
     * - Battery optimisation whitelist, autostart, background and standby bucket
     * All adds the optional ones (see [optionalCommands]).
     */
    internal fun setupCommands(
        scope: SetupScope,
        pkg: String,
        accessibilityClass: String,
        notificationClass: String,
        mergedAccessibilityServices: String,
        mergedNotificationListeners: String
    ): List<String> {
        val necessary = listOf(
            // First: accessibility and notification access cannot be switched on while this is still restricted.
            "appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
            "appops set $pkg SYSTEM_ALERT_WINDOW allow",
            // Modify system settings (the brightness slider on the Utilities page).
            "appops set $pkg WRITE_SETTINGS allow",
            "appops set $pkg BIND_ACCESSIBILITY_SERVICE allow",
            "appops set $pkg POST_NOTIFICATION allow",
            "appops set $pkg AUTO_START allow",
            "appops set $pkg RUN_IN_BACKGROUND allow",
            "appops set $pkg RUN_ANY_IN_BACKGROUND allow",
            "settings put secure enabled_accessibility_services $mergedAccessibilityServices",
            "settings put secure accessibility_enabled 1",
            "cmd notification allow_listener $notificationClass",
            "settings put secure enabled_notification_listeners $mergedNotificationListeners",
            "am set-standby-bucket $pkg active",
            "dumpsys deviceidle whitelist +$pkg"
        ) + runtimePermissionCommands(pkg) + sensitiveNotificationCommands(pkg)
        return if (scope == SetupScope.All) necessary + optionalCommands(pkg) else necessary
    }

    /**
     * Permissions only some features use, granted by the "all permissions" setup and never by the necessary one:
     * - Usage access (recent apps in the App Shortcuts)
     * - Do Not Disturb access (the silent ringer, the mode idle widget)
     * - Location, fine and coarse (the weather widget, the Wi-Fi network name, GPS use in the privacy dots)
     * - Watch app ops (camera, microphone and location use in the privacy dots, without the fallback)
     * A ROM that lacks one of them just prints an error; the commands are joined with ";" so the rest still run.
     * The VPN consent is a system dialog that cannot be granted from here.
     */
    internal fun optionalCommands(pkg: String): List<String> = listOf(
        "appops set $pkg GET_USAGE_STATS allow",
        "cmd notification allow_dnd $pkg",
        "pm grant $pkg android.permission.ACCESS_FINE_LOCATION",
        "pm grant $pkg android.permission.ACCESS_COARSE_LOCATION",
        "pm grant $pkg android.permission.WATCH_APPOPS"
    )

    /**
     * Repairs a stale/crashed AccessibilityService binding through Shizuku.
     *
     * Reproduces the effective Accessibility OFF -> ON refresh that was
     * verified on HyperOS, while preserving every other enabled service.
     */
    suspend fun repairAccessibility(context: Context): Result<String> =
        withContext(Dispatchers.IO) {
            val pkg = context.packageName
            val accessibilityClass =
                "$pkg/${SmartIslandOverlayService::class.java.name}"

            val shortAccessibilityClass =
                ComponentName(
                    context,
                    SmartIslandOverlayService::class.java
                ).flattenToShortString()

            val commands = listOf(
                "current=\$(settings get secure enabled_accessibility_services)",

                // Temporarily disable Accessibility globally.
                "settings put secure accessibility_enabled 0",

                // Remove Smart Island in either representation, while preserving
                // every other enabled Accessibility service.
                "new=''; IFS=':'; for s in \$current; do if [ -n \"\$s\" ] && [ \"\$s\" != \"$accessibilityClass\" ] && [ \"\$s\" != \"$shortAccessibilityClass\" ]; then if [ -n \"\$new\" ]; then new=\"\$new:\$s\"; else new=\"\$s\"; fi; fi; done; if [ -n \"\$new\" ] && [ \"\$new\" != \"null\" ]; then settings put secure enabled_accessibility_services \"\$new\"; else settings delete secure enabled_accessibility_services; fi",

                // Give Android/HyperOS time to process the disable.
                "sleep 2",

                // Add Smart Island back using the short component form.
                "current=\$(settings get secure enabled_accessibility_services); if [ -n \"\$current\" ] && [ \"\$current\" != \"null\" ]; then current=\"\$current:$shortAccessibilityClass\"; else current=\"$shortAccessibilityClass\"; fi; settings put secure enabled_accessibility_services \"\$current\"",

                // Re-enable Accessibility.
                "settings put secure accessibility_enabled 1"
            )

            runShizukuCommands(commands)
        }

    /** Grants only what the island needs to work (see [setupCommands]). */
    suspend fun autoGrantNecessaryPermissions(context: Context): Result<String> = grant(context, SetupScope.Necessary)

    /** Grants everything: the necessary permissions and the optional ones (see [setupCommands]). */
    suspend fun autoGrantAllPermissions(context: Context): Result<String> = grant(context, SetupScope.All)

    /**
     * Applies again the scope the user chose last (the necessary one when none was chosen yet). Used after a boot or an update,
     * so a phone that only got the necessary permissions is never given the optional ones behind the user's back.
     */
    suspend fun reapplySavedSetup(context: Context): Result<String> = grant(context, savedScope(context) ?: SetupScope.Necessary)

    private suspend fun grant(context: Context, scope: SetupScope): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val accessibilityClass = "$pkg/${SmartIslandOverlayService::class.java.name}"
        val notificationClass = "$pkg/${SmartIslandNotificationListenerService::class.java.name}"
        val commands = setupCommands(
            scope = scope,
            pkg = pkg,
            accessibilityClass = accessibilityClass,
            notificationClass = notificationClass,
            mergedAccessibilityServices = getMergedAccessibilityServices(context, accessibilityClass),
            mergedNotificationListeners = getMergedNotificationListeners(context, notificationClass)
        )
        runShizukuCommands(commands).onSuccess { saveScope(context, scope) }
    }

    /**
     * Runtime permissions of the necessary setup: nearby devices (Bluetooth connect, for the Bluetooth cards) and notifications. Location
     * (fine and coarse) is left out on purpose: it is optional (see [optionalCommands]), used only by the weather widget and the Wi-Fi /
     * privacy extras.
     * A ROM that lacks one of the permissions just prints an error; the commands are joined with ";" so the rest still run.
     */
    internal fun runtimePermissionCommands(pkg: String): List<String> = listOf(
        "pm grant $pkg android.permission.BLUETOOTH_CONNECT",
        "pm grant $pkg android.permission.POST_NOTIFICATIONS"
    )

    /** Permissions the necessary setup never grants; only the "all permissions" setup does (see [optionalCommands]). */
    internal val OPTIONAL_PERMISSIONS = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION"
    )

    /**
     * Shell commands that let this app read notifications Android marked as sensitive. Without them the
     * system hands the listener "Sensitive notification content hidden" instead of the title and text.
     * Both forms are tried (the permission and the app op of the same name): a ROM that lacks one of them
     * just prints an error, and the commands are joined with ";" so the rest still run.
     */
    internal fun sensitiveNotificationCommands(pkg: String): List<String> = listOf(
        "pm grant $pkg android.permission.RECEIVE_SENSITIVE_NOTIFICATIONS",
        "appops set $pkg RECEIVE_SENSITIVE_NOTIFICATIONS allow"
    )

    /**
     * True when this app may read notifications Android marked as sensitive: either the permission or the app op of the same name
     * is granted (the two commands of [sensitiveNotificationCommands] set one each; a ROM may honour only one of them).
     * False on a phone that has neither, including an Android version without the feature.
     */
    fun isSensitiveNotificationAccessAllowed(context: Context): Boolean {
        val byPermission = context.checkSelfPermission("android.permission.RECEIVE_SENSITIVE_NOTIFICATIONS") ==
            PackageManager.PERMISSION_GRANTED
        if (byPermission) return true
        // The string form of the check needs Android 10; an unknown operation name throws, which is simply "not allowed" here.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return false
        return runCatching {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            ops.unsafeCheckOpNoThrow(
                "android:receive_sensitive_notifications",
                android.os.Process.myUid(),
                context.packageName
            ) == android.app.AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    /** Grants only the sensitive-notification access (see [sensitiveNotificationCommands]). */
    suspend fun grantSensitiveNotificationAccess(context: Context): Result<String> = withContext(Dispatchers.IO) {
        runShizukuCommands(sensitiveNotificationCommands(context.packageName))
    }

    /**
     * Shell script that force-stops this app and starts it again. It runs inside Shizuku's shell, not in this
     * process, so it keeps going after the force-stop kills the app. Force-stopping also switches the
     * accessibility service off (Android removes it from the enabled list), so whatever was enabled before is
     * switched back on before the app is started again.
     */
    internal fun restartCommands(
        pkg: String,
        launcherActivity: String,
        accessibilityComponent: String,
        notificationComponent: String,
        accessibilityWasEnabled: Boolean,
        notificationWasEnabled: Boolean,
        mergedAccessibilityServices: String
    ): List<String> = buildList {
        add("am force-stop $pkg")
        if (accessibilityWasEnabled) {
            add("settings put secure enabled_accessibility_services $mergedAccessibilityServices")
            add("settings put secure accessibility_enabled 1")
        }
        if (notificationWasEnabled) add("cmd notification allow_listener $notificationComponent")
        add("sleep 1")
        add("am start -n $pkg/$launcherActivity")
    }

    /**
     * Grants the sensitive-notification access, then force-stops and relaunches the app so the notification
     * listener picks the new access up without the user restarting anything by hand. Returns after the grant;
     * once [onGranted] has run, the restart kills this process, so nothing after it is guaranteed to execute.
     */
    suspend fun grantSensitiveNotificationAccessAndRestart(
        context: Context,
        onGranted: suspend () -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val granted = runShizukuCommands(sensitiveNotificationCommands(pkg))
        if (granted.isFailure) return@withContext granted

        val accessibilityComponent = "$pkg/${SmartIslandOverlayService::class.java.name}"
        val notificationComponent = "$pkg/${SmartIslandNotificationListenerService::class.java.name}"
        val enabledAccessibility = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull().orEmpty()
        val enabledListeners = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        val script = restartCommands(
            pkg = pkg,
            launcherActivity = com.agupta07505.smartisland.MainActivity::class.java.name,
            accessibilityComponent = accessibilityComponent,
            notificationComponent = notificationComponent,
            accessibilityWasEnabled = enabledAccessibility.split(':').contains(accessibilityComponent),
            notificationWasEnabled = enabledListeners.split(':').contains(notificationComponent),
            mergedAccessibilityServices = mergeColonSeparated(enabledAccessibility, accessibilityComponent)
        )
        onGranted()
        runShizukuCommands(script)
    }

    /**
     * Grants OEM Autostart and disables background kill / app standby restrictions via Shizuku.
     */
    suspend fun grantOemAutostartAndKillProtection(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        val commands = listOf(
            "appops set $pkg AUTO_START allow",
            "appops set $pkg RUN_IN_BACKGROUND allow",
            "appops set $pkg RUN_ANY_IN_BACKGROUND allow",
            "am set-standby-bucket $pkg active",
            "dumpsys deviceidle whitelist +$pkg"
        )
        runShizukuCommands(commands)
    }
}
