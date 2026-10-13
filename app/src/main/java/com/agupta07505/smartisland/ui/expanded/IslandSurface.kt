/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.expanded

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings

/** Corner radius of an expanded island (the overlay animates its single island to the same value). */
internal val SPLIT_ISLAND_CORNER = 34.dp

/** Landscape with the sibling picker open: gap between the active card island and the picker island. */
internal val SPLIT_ISLAND_GAP = 14.dp

/** Narrowest the card island gets in the split layout, so tiny cards (battery, Bluetooth) stay tappable. */
internal val SPLIT_CARD_MIN_WIDTH = 240.dp

/**
 * How much of the dark scrim over card artwork / backdrops to keep. The scrim runs up to 85% black
 * and hid the gradient completely; with the gradient on it is much lighter.
 */
internal fun cardScrimScale(settings: SmartIslandSettings): Float =
    if (settings.expandedGradientEnabled) 0.3f else 1f

/**
 * The colour an expanded island is drawn in: the pill colour from the settings in the dark look, the palette's pale surface in the
 * light look. (The collapsed pill always keeps the pill colour; it has to match the camera cutout.)
 */
internal fun expandedSurfaceColor(settings: SmartIslandSettings, palette: IslandPalette): Color =
    if (palette.isLight) palette.surface else Color(settings.pillColor)

/** How far the gradient colour reaches into the light surface; a full-strength blue slab would take the pale look away. */
private const val LIGHT_GRADIENT_REACH = 0.65f

/** The share of the gradient strength that already shows at the top left corner. */
private const val GRADIENT_NEAR_SHARE = 0.35f

/**
 * Brush for an expanded island: the pill colour blends toward the gradient colour from the top
 * left to the bottom right. Strength 1 ends on the gradient colour itself. Null when the
 * gradient option is off, so callers fall back to the flat pill colour.
 */
internal fun expandedGradientBrush(settings: SmartIslandSettings, palette: IslandPalette = IslandPalette.Dark): Brush? {
    if (!settings.expandedGradientEnabled) return null
    val start = expandedSurfaceColor(settings, palette)
    val gradientColor = Color(settings.expandedGradientColor)
    val strength = settings.expandedGradientStrength.coerceIn(0f, 1f) * (if (palette.isLight) LIGHT_GRADIENT_REACH else 1f)
    // The far corner takes the full strength; the near corner already carries a third of it, so the colour is spread over the
    // whole card and every step of the slider shows, instead of only a tint at one corner.
    val from = androidx.compose.ui.graphics.lerp(start, gradientColor, strength * GRADIENT_NEAR_SHARE)
    val to = androidx.compose.ui.graphics.lerp(start, gradientColor, strength)
    return Brush.linearGradient(listOf(from, to))
}

/**
 * Draws one island surface (shadow, rounded clip, pill colour at [alpha]) behind this element.
 * Used when the expanded view is two separate islands, because the overlay's own single island
 * surface is switched off then.
 */
internal fun Modifier.islandSurface(
    settings: SmartIslandSettings,
    alpha: Float,
    shape: Shape = RoundedCornerShape(SPLIT_ISLAND_CORNER),
    /** When set (landscape split), the shadow is drawn at this 0..1 share of its strength, read at draw time; null = a plain shadow. */
    shadowStrength: (() -> Float)? = null,
    /** The look of the island; the dark one is the original. */
    palette: IslandPalette = IslandPalette.Dark
): Modifier {
    val shadowed = if (settings.enableShadow && settings.shadowElevation > 0f && shadowStrength != null) {
        val elevation = (settings.shadowElevation * 1.5f * palette.shadowScale).dp
        val islandShape = shape
        this.graphicsLayer {
            shadowElevation = elevation.toPx() * shadowStrength().coerceIn(0f, 1f)
            this.shape = islandShape
            clip = false
            ambientShadowColor = Color.Black
            spotShadowColor = Color.Black
        }
    } else if (settings.enableShadow && settings.shadowElevation > 0f) {
        this.shadow(
            elevation = (settings.shadowElevation * 1.5f * palette.shadowScale).dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black,
            spotColor = Color.Black
        )
    } else {
        this
    }
    val brush = expandedGradientBrush(settings, palette)
    val surfaceColor = expandedSurfaceColor(settings, palette)
    return shadowed
        .clip(shape)
        .then(
            if (brush != null) Modifier.background(brush, shape, alpha)
            else Modifier.background(surfaceColor.copy(alpha = alpha))
        )
}
