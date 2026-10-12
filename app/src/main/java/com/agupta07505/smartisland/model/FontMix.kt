/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * "Mix fonts": the island text can use up to three different fonts at once, the way a system font family ships separate
 * Light, Regular and Medium cuts (SF Pro Display and friends). Every piece of island text already asks for a weight
 * (secondary lines are normal, titles semi-bold, numbers and names bold), so the weight decides which font draws it:
 *
 *  - [FontSlot.Light]   normal weight and lighter: secondary and small detail text
 *  - [FontSlot.Regular] medium and semi-bold: the main line of a card
 *  - [FontSlot.Medium]  bold and heavier: titles, numbers, names
 *
 * A slot left empty uses the island font, so mixing is opt-in per slot. The Compose side that builds the real font family
 * from this is `mixedFontFamily` in ui/IslandFonts.kt; this file is the pure rule so it can be unit tested.
 */
enum class FontSlot(
    /** The weight whose file is used when the slot holds one of the built-in fonts (they ship 400, 500 and 700). */
    val representativeWeight: Int,
    /** Every CSS-style weight (100..900) the slot draws. Together the three slots cover all nine, with no gaps and no overlaps. */
    val weights: List<Int>
) {
    Light(400, listOf(100, 200, 300, 400)),
    Regular(500, listOf(500, 600)),
    Medium(700, listOf(700, 800, 900))
}

object FontMix {
    /** The slot that draws text asked for at [weight] (any value, it is clamped to 1..1000 like Compose does). */
    fun slotFor(weight: Int): FontSlot = when {
        weight <= 400 -> FontSlot.Light
        weight <= 600 -> FontSlot.Regular
        else -> FontSlot.Medium
    }

    /** The font id a slot really uses: its own choice, or the island font when it has none ([slotId] empty). */
    fun effectiveId(slotId: String, islandFont: String): String = slotId.ifEmpty { islandFont }

    fun idOf(slot: FontSlot, light: String, regular: String, medium: String): String = when (slot) {
        FontSlot.Light -> light
        FontSlot.Regular -> regular
        FontSlot.Medium -> medium
    }

    /** The mix only does something when it is switched on and at least one slot has a font of its own. */
    fun isActive(enabled: Boolean, light: String, regular: String, medium: String): Boolean =
        enabled && (light.isNotEmpty() || regular.isNotEmpty() || medium.isNotEmpty())
}
