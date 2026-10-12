/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.vector.ImageVector
import com.agupta07505.smartisland.model.PrivacyKind
import com.agupta07505.smartisland.model.pillDotKinds
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue

private val SCREEN_SHARE_COLOR = Color(0xFF4DA3FF)
private val CAMERA_COLOR = Color(0xFF34C759)
private val MICROPHONE_COLOR = Color(0xFFFF9F0A)
private val LOCATION_COLOR = Color(0xFF5E9BFF)

internal fun privacyColor(kind: PrivacyKind): Color = when (kind) {
    PrivacyKind.Camera -> CAMERA_COLOR
    PrivacyKind.Microphone -> MICROPHONE_COLOR
    PrivacyKind.Location -> LOCATION_COLOR
    PrivacyKind.ScreenShare -> SCREEN_SHARE_COLOR
}

private fun privacyIcon(kind: PrivacyKind): ImageVector = when (kind) {
    PrivacyKind.Camera -> Icons.Rounded.CameraAlt
    PrivacyKind.Microphone -> Icons.Rounded.Mic
    PrivacyKind.Location -> Icons.Rounded.LocationOn
    PrivacyKind.ScreenShare -> Icons.Rounded.Cast
}

private fun privacyLabel(kind: PrivacyKind): String = when (kind) {
    PrivacyKind.Camera -> "Camera in use"
    PrivacyKind.Microphone -> "Microphone in use"
    PrivacyKind.Location -> "Location in use"
    PrivacyKind.ScreenShare -> "Screen sharing"
}

/**
 * Small round dots inside the main cutout, one per kind in use. They take no touches themselves (the pill's own gesture
 * handler would otherwise lose the end of the touch); instead they report where they are on screen through
 * [onBounds], and a tap that lands there opens the system's "which apps use this" screen (see IslandMainGestures).
 */
@Composable
internal fun PrivacyDots(
    active: Set<PrivacyKind>,
    modifier: Modifier = Modifier,
    onBounds: ((PrivacyDotsHit?) -> Unit)? = null
) {
    val kinds = pillDotKinds(active)
    // The callback is a new lambda on every recomposition; keying the effect on it would clear the bounds each time.
    val currentOnBounds by androidx.compose.runtime.rememberUpdatedState(onBounds)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { currentOnBounds?.invoke(null) }
    }
    if (kinds.isEmpty()) {
        androidx.compose.runtime.SideEffect { currentOnBounds?.invoke(null) }
        return
    }
    Row(
        modifier = modifier.then(
            if (onBounds == null) Modifier else Modifier.onGloballyPositioned {
                currentOnBounds?.invoke(PrivacyDotsHit(it.boundsInWindow(), kinds))
            }
        ),
        horizontalArrangement = Arrangement.spacedBy(PRIVACY_DOT_GAP_DP.dp)
    ) {
        kinds.forEach { kind ->
            Box(
                Modifier
                    .size(PRIVACY_DOT_SIZE_DP.dp)
                    .clip(CircleShape)
                    .background(privacyColor(kind))
            )
        }
    }
}

/** Where the pill's privacy dots are on screen (window pixels) and which kinds they show, left to right. */
data class PrivacyDotsHit(val bounds: androidx.compose.ui.geometry.Rect, val kinds: List<PrivacyKind>) {
    /**
     * The kind under a touch at ([x], [y]) in window pixels, or null when it is not on the dots. The dots are tiny, so
     * the target is widened: [slopYPx] up and down (the pill is tall, nothing else is there), [slopRightPx] towards
     * the pill's edge, and only [slopLeftPx] towards the other glyphs, so a tap meant for a glyph next to the dots is
     * not taken for a tap on them. With several dots the touch picks the nearest one by x.
     */
    fun kindAt(x: Float, y: Float, slopLeftPx: Float, slopRightPx: Float, slopYPx: Float): PrivacyKind? {
        if (kinds.isEmpty()) return null
        val hit = androidx.compose.ui.geometry.Rect(
            bounds.left - slopLeftPx, bounds.top - slopYPx, bounds.right + slopRightPx, bounds.bottom + slopYPx
        )
        if (!hit.contains(androidx.compose.ui.geometry.Offset(x, y))) return null
        if (kinds.size == 1 || bounds.width <= 0f) return kinds.first()
        val fraction = ((x - bounds.left) / bounds.width).coerceIn(0f, 0.9999f)
        return kinds[(fraction * kinds.size).toInt().coerceIn(0, kinds.lastIndex)]
    }
}

/** Width the dots take, used to keep other right-side content clear of them. */
internal fun privacyDotsWidthDp(active: Set<PrivacyKind>): Int {
    val n = pillDotKinds(active).size
    return if (n == 0) 0 else n * PRIVACY_DOT_SIZE_DP + (n - 1) * PRIVACY_DOT_GAP_DP + PRIVACY_DOT_CLEARANCE_DP
}

/** The tinted disc fills this share of the secondary cutout (was all of it), so the cutout reads smaller. */
internal const val PRIVACY_DISC_FRACTION = 0.78f
/** The icon fills this share of the disc (was 0.72 of the whole cutout). */
internal const val PRIVACY_ICON_FRACTION = 0.62f

private const val PRIVACY_DOT_SIZE_DP = 5
private const val PRIVACY_DOT_GAP_DP = 3
private const val PRIVACY_DOT_CLEARANCE_DP = 4

/** Secondary round cutout content for any privacy kind: its icon on a tinted, softly breathing disc. */
@Composable
internal fun PrivacyBubbleGlyph(kind: PrivacyKind, modifier: Modifier = Modifier) {
    val color = privacyColor(kind)
    val transition = rememberInfiniteTransition(label = "screenShareGlow")
    val glow by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "screenShareGlowAlpha"
    )
    Box(
        modifier = modifier
            .fillMaxSize(PRIVACY_DISC_FRACTION)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            privacyIcon(kind),
            contentDescription = privacyLabel(kind),
            tint = color,
            modifier = Modifier.fillMaxSize(PRIVACY_ICON_FRACTION).graphicsLayer { alpha = glow }
        )
    }
}
