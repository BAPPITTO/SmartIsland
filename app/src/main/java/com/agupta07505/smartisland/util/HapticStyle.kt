/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/** What the island is reporting through the vibration motor. */
enum class HapticKind { Expand, Collapse, Swipe, Hold, Merge, Lock }

/** One buzz: how long and how hard (1..255), plus the system effect id to prefer on Android 10+. */
data class HapticSpec(val durationMs: Long, val amplitude: Int, val effect: Effect)

/** Mirrors the system's predefined effects so this file stays free of Android classes (unit-testable). */
enum class Effect { Tick, Click, HeavyClick }

/**
 * Haptic strength (Notifications and privacy). [OFF] vibrates never. The other two scale the amplitude
 * of the fallback buzz; on Android 10+ the system's own tick/click effects are used instead, which feel
 * crisp on any motor.
 *
 * Each kind has its own feel: expand is a soft click, collapse a lighter tick, a swipe action a short
 * tick, and the 300 ms hold (which confirms the hold mode) the heavy click it always was. The merge touch (two
 * cutouts meeting) is the lightest tick of all. The swipe lock is a dull, longer buzz.
 */
enum class HapticStrength(val id: String, val scale: Float) {
    OFF("off", 0f),
    LIGHT("light", 0.55f),
    NORMAL("normal", 1f),
    STRONG("strong", 1.5f);

    fun spec(kind: HapticKind): HapticSpec? {
        if (this == OFF) return null
        val (duration, amplitude, effect) = when (kind) {
            HapticKind.Expand -> Triple(22L, 150, Effect.Click)
            HapticKind.Collapse -> Triple(14L, 110, Effect.Tick)
            HapticKind.Swipe -> Triple(12L, 130, Effect.Tick)
            HapticKind.Hold -> Triple(60L, 200, Effect.HeavyClick)
            HapticKind.Merge -> Triple(10L, 90, Effect.Tick)
            // Swiping locks for a moment. A dull, slightly longer buzz, unlike the crisp arm tick.
            HapticKind.Lock -> Triple(28L, 100, Effect.Click)
        }
        return HapticSpec(duration, (amplitude * scale).toInt().coerceIn(1, 255), effect)
    }

    companion object {
        const val ID_OFF = "off"
        const val ID_LIGHT = "light"
        const val ID_NORMAL = "normal"
        const val ID_STRONG = "strong"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        /** Unknown or missing ids (old backups) fall back to [NORMAL]. */
        fun fromId(id: String?): HapticStrength = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
