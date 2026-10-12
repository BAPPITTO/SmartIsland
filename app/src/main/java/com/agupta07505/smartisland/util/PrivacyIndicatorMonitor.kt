/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.GnssStatus
import android.location.LocationManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.agupta07505.smartisland.model.PrivacyKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Feeds a [PrivacyIndicatorTracker] with whether the screen is being shared.
 *
 * Screen sharing (media projection) has no public API. With Shizuku authorized the monitor reads
 * `dumpsys media_projection` every few seconds while the screen is on. Without Shizuku it stays off.
 *
 * All work runs in [scope] (main), where the tracker lives.
 */
class PrivacyIndicatorMonitor(
    private val context: Context,
    private val tracker: PrivacyIndicatorTracker,
    private val scope: CoroutineScope
) {
    private var wantProjection = false
    private var projectionJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraWatch: AutoCloseable? = null
    private var micWatch: AutoCloseable? = null
    private var locationWatch: AutoCloseable? = null

    /** Re-evaluates the camera indicator; set while the camera watch runs. */
    private var cameraRepublish: (() -> Unit)? = null
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private fun isKeyguardLocked(): Boolean = keyguardManager?.isKeyguardLocked == true

    /** Call when the phone locks or unlocks: the camera indicator is hidden while the keyguard is up (face recognition). */
    fun onLockStateChanged() {
        cameraRepublish?.invoke()
    }

    /** Starts or stops each source so it only runs while the user has its switch on. */
    fun configure(
        projection: Boolean,
        camera: Boolean = false,
        microphone: Boolean = false,
        location: Boolean = false
    ) {
        if (projection != wantProjection) {
            wantProjection = projection
            if (projection) startProjection() else stopProjection()
        }
        if (camera && cameraWatch == null) cameraWatch = watchCamera()
        if (!camera && cameraWatch != null) {
            cameraWatch?.close(); cameraWatch = null; tracker.clear(PrivacyKind.Camera)
        }
        if (microphone && micWatch == null) micWatch = watchMicrophone()
        if (!microphone && micWatch != null) {
            micWatch?.close(); micWatch = null; tracker.clear(PrivacyKind.Microphone)
        }
        // Location needs the permission; retry on every configure() so granting it takes effect.
        if (location && locationWatch == null) locationWatch = watchLocation()
        if (!location && locationWatch != null) {
            locationWatch?.close(); locationWatch = null; tracker.clear(PrivacyKind.Location)
        }
    }

    fun stop() {
        configure(projection = false)
    }

    // ---------------------------------------------------------------- camera

    /**
     * A camera id becomes "unavailable" while any app has it open, which is public API and needs no
     * permission. Ids whose torch is on are ignored: a lit flashlight must not look like a camera in use.
     * Nothing shows while the keyguard is up (the lock screen's face recognition uses the camera), see [CameraUseState].
     */
    private fun watchCamera(): AutoCloseable? = runCatching {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val state = CameraUseState()
        fun publish() = tracker.setActive(PrivacyKind.Camera, state.shown(isKeyguardLocked()))
        cameraRepublish = ::publish

        val availability = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) { state.onAvailable(cameraId); publish() }
            override fun onCameraUnavailable(cameraId: String) { state.onUnavailable(cameraId, isKeyguardLocked()); publish() }
        }
        val torch = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                state.onTorch(cameraId, enabled)
                publish()
            }
            override fun onTorchModeUnavailable(cameraId: String) { state.onTorch(cameraId, false); publish() }
        }
        manager.registerAvailabilityCallback(availability, mainHandler)
        manager.registerTorchCallback(torch, mainHandler)
        AutoCloseable {
            cameraRepublish = null
            runCatching { manager.unregisterAvailabilityCallback(availability) }
            runCatching { manager.unregisterTorchCallback(torch) }
        }
    }.onFailure { android.util.Log.w(TAG, "Camera watch failed: ${it.message}") }.getOrNull()

    // ---------------------------------------------------------------- microphone

    /**
     * Any active audio recording on the device. Other apps' recordings reach this callback with their
     * identity removed, which is all that is needed here. A recording the system has silenced (an app in
     * the background that is not really getting audio) does not count.
     */
    private fun watchMicrophone(): AutoCloseable? = runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fun publish(configs: List<AudioRecordingConfiguration>) {
            val live = configs.any { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !it.isClientSilenced }
            tracker.setActive(PrivacyKind.Microphone, live)
        }
        val callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) = publish(configs)
        }
        audio.registerAudioRecordingCallback(callback, mainHandler)
        publish(audio.activeRecordingConfigurations)
        AutoCloseable { runCatching { audio.unregisterAudioRecordingCallback(callback) } }
    }.onFailure { android.util.Log.w(TAG, "Microphone watch failed: ${it.message}") }.getOrNull()

    // ---------------------------------------------------------------- location

    /**
     * The GNSS engine starts when an app asks for GPS and stops when none does. This sees GPS use only
     * (not Wi-Fi / cell location) and needs the location permission, which the app already asks for.
     */
    private fun watchLocation(): AutoCloseable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null
        return runCatching {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val callback = object : GnssStatus.Callback() {
                override fun onStarted() = tracker.setActive(PrivacyKind.Location, true)
                override fun onStopped() = tracker.setActive(PrivacyKind.Location, false)
            }
            manager.registerGnssStatusCallback(context.mainExecutor, callback)
            AutoCloseable { runCatching { manager.unregisterGnssStatusCallback(callback) } }
        }.onFailure { android.util.Log.w(TAG, "Location watch failed: ${it.message}") }.getOrNull()
    }

    // ---------------------------------------------------------------- screen sharing

    private fun startProjection() {
        projectionJob?.cancel()
        projectionJob = scope.launch {
            val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            while (isActive) {
                if (power?.isInteractive != false && ShizukuManager.hasPermission()) {
                    val dump = withContext(Dispatchers.IO) {
                        ShizukuManager.runShellForOutput("dumpsys media_projection").getOrNull()
                    }
                    val active = dump?.let { parseMediaProjectionActive(it) }
                    if (active != null) {
                        tracker.setMediaProjectionActive(active)
                        tracker.setHolderPackage(if (active) dump?.let { parseMediaProjectionPackage(it) } else null)
                    }
                    delay(PROJECTION_POLL_MS)
                } else {
                    delay(PROJECTION_IDLE_POLL_MS)
                }
            }
        }
    }

    private fun stopProjection() {
        projectionJob?.cancel()
        projectionJob = null
        tracker.clear(PrivacyKind.ScreenShare)
    }

    companion object {
        private const val TAG = "PrivacyIndicatorMonitor"
        private const val PROJECTION_POLL_MS = 3_000L
        private const val PROJECTION_IDLE_POLL_MS = 10_000L

        /**
         * Reads `dumpsys media_projection`. Returns true/false when the output is understood and
         * null when it is not (header missing), so an unexpected format never flips the state.
         * The exact wording differs between Android versions; check it on the device with
         * `adb shell dumpsys media_projection` while sharing.
         */
        /** Package of the app holding the projection (`mPackageName=...`), or null when the dump has none. */
        internal fun parseMediaProjectionPackage(dump: String): String? =
            Regex("(?im)^\\s*mPackageName\\s*=\\s*([\\w.]+)").find(dump)?.groupValues?.get(1)

        internal fun parseMediaProjectionActive(dump: String): Boolean? {
            if (!dump.contains("media projection", ignoreCase = true) &&
                !dump.contains("MediaProjection", ignoreCase = true)
            ) return null
            val noProjection = Regex("(?im)^\\s*media projection\\s*:\\s*null\\s*$")
            if (noProjection.containsMatchIn(dump)) return false
            val hasGrant = Regex("(?im)^\\s*(mProjectionGrant|media projection)\\s*[:=]\\s*(?!null\\b)\\S+")
            if (hasGrant.containsMatchIn(dump)) return true
            if (Regex("(?im)^\\s*mPackageName\\s*=\\s*\\S+").containsMatchIn(dump)) return true
            return false
        }
    }
}
