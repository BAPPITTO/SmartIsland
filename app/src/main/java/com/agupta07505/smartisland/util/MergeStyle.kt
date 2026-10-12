/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

/**
 * How the secondary round cutout joins the primary cutout when it merges (Layout tab).
 *
 * [SLIDE] is the original look: the bubble slides sideways, stretching toward the pill, and sinks into it.
 * [BUBBLE] is push, then join: the bubble travels to the pill, the two soft bodies press into each other (flat
 * faces, no gap, the bubble's icon fades), then they join and the pill takes the bubble in with a small swell. On
 * Android 12+ the join is drawn gooey (blur plus an alpha threshold, see ui/GooeyMerge.kt); older versions keep a
 * plain bridge. The maths is in ui/MergeBubble.kt.
 *
 * Lengths were 620 / 780 ms; [SLIDE] is one quick motion, [BUBBLE] is longer because it has three parts.
 */
enum class MergeStyle(val id: String, val baseDurationMs: Int) {
    SLIDE("slide", 420),
    BUBBLE("bubble", 900);

    companion object {
        const val ID_SLIDE = "slide"
        const val ID_BUBBLE = "bubble"

        val VALID_IDS: Set<String> = entries.map { it.id }.toSet()

        /** Unknown or missing ids (old backups, hand-edited files) fall back to [SLIDE], today's look. */
        fun fromId(id: String?): MergeStyle = entries.firstOrNull { it.id == id } ?: SLIDE
    }
}
