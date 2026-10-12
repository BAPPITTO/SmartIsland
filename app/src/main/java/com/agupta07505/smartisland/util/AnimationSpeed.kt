/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * Animation speed of the island (Layout tab, chosen separately for portrait and landscape).
 *
 * The overlay's springs and tweens were tuned for [NORMAL]; the other two scale them. A spring's
 * stiffness is multiplied by [stiffnessScale] and a tween's length by [durationScale], so Snappy
 * is both stiffer and shorter, and Relaxed the opposite. [NORMAL] returns the exact old numbers.
 *
 * The system animator duration scale is applied on top, but only the part Compose does not already apply itself
 * (see [SystemAnimScale.extraScale]; normally nothing, so nothing is scaled twice). At scale 0 the island snaps.
 * [userStiffness] and [userDurationMs] leave the system scale out, for code that applies the real scale on its own.
 */
enum class AnimationSpeed(val id: String, val stiffnessScale: Float, val durationScale: Float) {
    RELAXED("relaxed", 0.6f, 1.45f),
    NORMAL("normal", 1f, 1f),
    SNAPPY("snappy", 1.7f, 0.65f);

    /** Stiffness with the user's choice only. */
    fun userStiffness(base: Float): Float = base * stiffnessScale

    /** Length with the user's choice only. */
    fun userDurationMs(baseMs: Int): Int = (baseMs * durationScale).toInt().coerceAtLeast(1)

    /** Stiffness for a Compose spring: the user's choice, then whatever part of the system scale Compose leaves to us. */
    fun stiffness(base: Float): Float = SystemAnimScale.scaledStiffness(userStiffness(base), SystemAnimScale.extraScale())

    /** Length for a Compose tween: the user's choice, then whatever part of the system scale Compose leaves to us. */
    fun durationMs(baseMs: Int): Int = SystemAnimScale.scaledDurationMs(userDurationMs(baseMs), SystemAnimScale.extraScale())

    /** Adds the system's part of the scale to a stiffness that already has the user's choice in it (see [SystemAnimScale.extraScale]). */
    fun systemStiffness(userStiffness: Float): Float = SystemAnimScale.scaledStiffness(userStiffness, SystemAnimScale.extraScale())

    /** Same for a length that already has the user's choice in it. */
    fun systemDurationMs(userMs: Int): Int = SystemAnimScale.scaledDurationMs(userMs, SystemAnimScale.extraScale())

    companion object {
        const val ID_RELAXED = "relaxed"
        const val ID_NORMAL = "normal"
        const val ID_SNAPPY = "snappy"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        /** Unknown or missing ids (old backups, hand-edited files) fall back to [NORMAL]. */
        fun fromId(id: String?): AnimationSpeed = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}
