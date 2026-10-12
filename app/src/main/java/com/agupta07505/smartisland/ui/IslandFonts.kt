/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.FontMix
import com.agupta07505.smartisland.model.FontSlot
import com.agupta07505.smartisland.util.CustomFonts
import java.util.concurrent.ConcurrentHashMap

/** One built-in choice in the font picker. [family] is what the island text uses. */
internal data class IslandFontOption(val id: String, val label: String, val note: String, val family: FontFamily)

private val LoraFamily = FontFamily(
    Font(R.font.lora_400, FontWeight.Normal),
    Font(R.font.lora_500, FontWeight.Medium),
    Font(R.font.lora_700, FontWeight.Bold)
)

private val InterDisplayFamily = FontFamily(
    Font(R.font.inter_display_400, FontWeight.Normal),
    Font(R.font.inter_display_500, FontWeight.Medium),
    Font(R.font.inter_display_700, FontWeight.Bold)
)

private val PoppinsFamily = FontFamily(
    Font(R.font.poppins_400, FontWeight.Normal),
    Font(R.font.poppins_500, FontWeight.Medium),
    Font(R.font.poppins_700, FontWeight.Bold)
)

/**
 * The fonts that ship with the app. Lora, Inter Display and Poppins are open fonts (SIL Open Font License 1.1, see
 * THIRD_PARTY_FONTS.md). Any other font is added by the user from a .ttf or .otf file (see [CustomFonts]).
 */
internal val IslandFontOptions: List<IslandFontOption> = listOf(
    IslandFontOption(SmartIslandSettings.FONT_SYSTEM, "System", "Your phone's own font", FontFamily.Default),
    IslandFontOption(SmartIslandSettings.FONT_SERIF, "Serif", "The phone's serif font", FontFamily.Serif),
    IslandFontOption(SmartIslandSettings.FONT_LORA, "Lora", "A warm, readable serif", LoraFamily),
    IslandFontOption(SmartIslandSettings.FONT_INTER_DISPLAY, "Inter Display", "A clean, modern sans", InterDisplayFamily),
    IslandFontOption(SmartIslandSettings.FONT_POPPINS, "Poppins", "Round, geometric sans", PoppinsFamily),
    IslandFontOption(SmartIslandSettings.FONT_MONOSPACE, "Monospace", "Fixed width, like code", FontFamily.Monospace)
)

/** The family of a built-in font id; the default font for anything else (including custom ids). */
internal fun islandFontFamily(id: String): FontFamily =
    (IslandFontOptions.firstOrNull { it.id == id } ?: IslandFontOptions.first { it.id == SmartIslandSettings.Default.islandFont }).family

private class CachedCustomFamily(val lastModified: Long, val length: Long, val family: FontFamily)

private val customFamilies = ConcurrentHashMap<String, CachedCustomFamily>()

/**
 * The family for any font id. A custom id loads the font file the user added (cached until the file changes);
 * a custom font whose file is missing or unreadable falls back to the phone's own font, so the island never
 * ends up without text.
 */
internal fun islandFontFamily(context: Context, id: String): FontFamily {
    if (!CustomFonts.isCustomId(id)) return islandFontFamily(id)
    val file = CustomFonts.fileFor(context, id) ?: return FontFamily.Default
    val cached = customFamilies[id]
    if (cached != null && cached.lastModified == file.lastModified() && cached.length == file.length()) return cached.family
    val typeface = CustomFonts.typeface(context, id) ?: return FontFamily.Default
    val family = FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
    customFamilies[id] = CachedCustomFamily(file.lastModified(), file.length(), family)
    return family
}

/** Forgets a deleted custom font. */
internal fun forgetCustomFontFamily(id: String) {
    customFamilies.remove(id)
}

/** The resource of a built-in font's file for [weight] (they ship 400, 500 and 700); null for the phone's own families. */
private fun builtinFontRes(id: String, weight: Int): Int? {
    fun pick(r400: Int, r500: Int, r700: Int) = when {
        weight < 450 -> r400
        weight < 600 -> r500
        else -> r700
    }
    return when (id) {
        SmartIslandSettings.FONT_LORA -> pick(R.font.lora_400, R.font.lora_500, R.font.lora_700)
        SmartIslandSettings.FONT_INTER_DISPLAY -> pick(R.font.inter_display_400, R.font.inter_display_500, R.font.inter_display_700)
        SmartIslandSettings.FONT_POPPINS -> pick(R.font.poppins_400, R.font.poppins_500, R.font.poppins_700)
        else -> null
    }
}

/**
 * One font entry of the mixed family: the font behind [id], registered for text asked for at [weight].
 * A custom font file or a built-in font draws every weight of its slot with its one file (the slot's representative weight picks
 * the built-in file); the phone's own families (system, serif, monospace) are loaded at the exact weight, so they keep their cuts.
 * A custom font whose file is missing or unreadable falls back to the phone's own font, like [islandFontFamily] does.
 */
private fun slotFont(context: Context, id: String, slot: FontSlot, weight: Int): Font {
    val label = FontWeight(weight)
    if (CustomFonts.isCustomId(id)) {
        val file = CustomFonts.fileFor(context, id)
        if (file != null && CustomFonts.typeface(context, id) != null) return Font(file, label)
    }
    builtinFontRes(id, slot.representativeWeight)?.let { return Font(it, label) }
    val name = if (id == SmartIslandSettings.FONT_SERIF) "serif" else if (id == SmartIslandSettings.FONT_MONOSPACE) "monospace" else "sans-serif"
    return Font(DeviceFontFamilyName(name), label)
}

/**
 * The island's font family when fonts are mixed (see model/FontMix.kt): normal and lighter text uses the Light slot's font, medium
 * and semi-bold the Regular slot's, bold and heavier the Medium slot's. A slot left empty uses [islandFont]. All nine weights
 * 100..900 are registered, so each request lands on exactly one slot, never on a neighbour or a synthesised bold.
 */
internal fun mixedFontFamily(context: Context, islandFont: String, light: String, regular: String, medium: String): FontFamily {
    val fonts = FontSlot.entries.flatMap { slot ->
        val id = FontMix.effectiveId(FontMix.idOf(slot, light, regular, medium), islandFont)
        slot.weights.map { weight -> slotFont(context, id, slot, weight) }
    }
    return FontFamily(fonts)
}

/** The family of the island's text for these settings: the mixed family when the mix is on, else the one island font. */
internal fun islandTextFamily(context: Context, settings: SmartIslandSettings): FontFamily =
    if (FontMix.isActive(settings.fontMixEnabled, settings.fontMixLight, settings.fontMixRegular, settings.fontMixMedium)) {
        mixedFontFamily(context, settings.islandFont, settings.fontMixLight, settings.fontMixRegular, settings.fontMixMedium)
    } else {
        islandFontFamily(context, settings.islandFont)
    }
