/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Which engine opens the floating window.
 *
 * - [System]: Android's own freeform windowing (needs freeform support on the ROM / developer options).
 * - [Lmo]: LMO Freeform (`com.libremobileos.freeform`), asked through its exported START_FREEFORM broadcast.
 */
enum class FloatingWindowMode(val id: String) {
    System("system"),
    Lmo("lmo");

    companion object {
        const val ID_SYSTEM = "system"
        const val ID_LMO = "lmo"
        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        fun fromId(id: String?): FloatingWindowMode = entries.firstOrNull { it.id == id } ?: System
    }
}

/** What a floating-window request does, decided from the settings alone (no Android types, so it is unit-testable). */
enum class FloatingWindowRoute { Off, System, Lmo }

fun floatingWindowRoute(enabled: Boolean, modeId: String?): FloatingWindowRoute = when {
    !enabled -> FloatingWindowRoute.Off
    FloatingWindowMode.fromId(modeId) == FloatingWindowMode.Lmo -> FloatingWindowRoute.Lmo
    else -> FloatingWindowRoute.System
}
