/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.DayMoment
import com.agupta07505.smartisland.model.DayMoments
import com.agupta07505.smartisland.model.GlyphPose
import com.agupta07505.smartisland.model.MomentGlyph
import com.agupta07505.smartisland.model.IdleSlots
import com.agupta07505.smartisland.model.IdleWidgetKind
import com.agupta07505.smartisland.model.IdleWidgets
import com.agupta07505.smartisland.model.PILL_CENTER_ZONE_SHARE
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** One glyph of the tick: the horizon sun (own drawing, see HorizonSun.kt), the full sun or the moon, posed by [poseOf] at the current tick. */
@Composable
private fun MomentGlyphView(moment: DayMoment, glyphScale: Float, poseOf: () -> GlyphPose) {
    val size = Modifier.size((16f * glyphScale).dp)
    val layer: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        val pose = poseOf()
        translationY = pose.offsetShare * this.size.height
        scaleX = pose.scale
        scaleY = pose.scale
        alpha = pose.alpha
    }
    when (DayMoments.glyph(moment)) {
        MomentGlyph.Horizon -> HorizonSunGlyph(lift = poseOf().lift, modifier = size.graphicsLayer(layer))
        MomentGlyph.Sun -> Icon(Icons.Rounded.WbSunny, null, tint = Color.White, modifier = size.graphicsLayer(layer))
        MomentGlyph.Moon -> Icon(Icons.Rounded.NightsStay, null, tint = Color.White, modifier = size.graphicsLayer(layer))
    }
}

/**
 * The Day moments widget: at the phone's local 6:00, 12:00, 17:30 and 23:00 the pill's content gives way for a moment to one glyph in the left
 * slot. It first shows the previous moment's glyph (the moon before sunrise, the sunrise before the afternoon sun, ...) for a brief moment; then the
 * new glyph ticks in and the old one ticks away, with the heavy clock tick (a slow, suspended pull-back, then the snap). Sunrise: the sun comes up from
 * the bottom edge. Afternoon: the full sun grows out of the sunrise glyph. Sunset: the sun sinks into the bottom edge. Midnight: the moon grows out of the
 * sunset glyph (see [DayMoments.incomingPose] / [DayMoments.outgoingPose]). Then the pill's own content comes back. Same conditions as the hour tick: the
 * widget and the idle widgets are on and not held off, the screen is on, the pill is the wide one. When a moment falls on a full hour that the
 * hour tick also plays on, it waits for the hour tick to finish.
 */
@Composable
internal fun DayMomentLayer(
    settings: SmartIslandSettings,
    ui: IslandInteractionState,
    animSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
    suppressed: Boolean = false,
    /** Landscape: the pill may be brought in for the tick although a long press hid it ([withTickPill]). */
    canHold: Boolean = false
) {
    val kinds = remember(settings.idleWidgetIds) { IdleWidgets.parse(settings.idleWidgetIds) }
    val momentsOn = IdleWidgetKind.DayMoments in kinds
    val clockOn = IdleWidgetKind.Clock in kinds
    val enabled = settings.idleWidgetsEnabled && !suppressed && momentsOn && (canHold || settings.width > IDLE_WIDGET_COMPACT_MAX_DP)
    val settingsNow = androidx.compose.runtime.rememberUpdatedState(settings)
    val screenOn = rememberScreenInteractive()
    val tick = remember { Animatable(0f) }
    var shown by remember { mutableStateOf<DayMoment?>(null) }

    LaunchedEffect(enabled, screenOn) {
        if (!enabled || !screenOn) return@LaunchedEffect
        try {
            while (true) {
                val (next, at) = DayMoments.next(System.currentTimeMillis())
                delay((at - System.currentTimeMillis()).coerceAtLeast(0L))
                // The pill comes in at the moment itself, so a wait for the hour tick below keeps it on the island (see withTickPill).
                ui.withTickPill(canHold, { settingsNow.value.width > IDLE_WIDGET_COMPACT_MAX_DP }) {
                    // 6:00, 12:00 and 23:00 are full hours: the hour tick has the pill first.
                    if (clockOn && next.minute == 0) delay(DayMoments.AFTER_HOUR_TICK_MS)
                    while (ui.chimeShow.value > 0.001f) delay(100)
                    shown = next
                    tick.snapTo(0f)
                    ui.chimeShow.animateTo(1f, tween(animSpeed.durationMs(DayMoments.FADE_IN_MS)))
                    // The previous glyph rests for a moment, then the heavy tick swaps it for the new one.
                    delay(animSpeed.durationMs(DayMoments.HOLD_BEFORE_MS).toLong())
                    tick.animateTo(1f, tween(animSpeed.durationMs(DayMoments.TICK_MS), easing = Easing { IdleSlots.heavyTickProgress(it) }))
                    delay(animSpeed.durationMs(DayMoments.HOLD_AFTER_MS).toLong())
                    ui.chimeShow.animateTo(0f, tween(animSpeed.durationMs(DayMoments.FADE_OUT_MS)))
                    shown = null
                }
            }
        } finally {
            // Cancelled half way (screen off, the setting switched off): the pill's own content must come back.
            withContext(NonCancellable) {
                ui.chimeShow.snapTo(0f)
                shown = null
            }
        }
    }

    val moment = shown ?: return
    val glyphScale = settings.idleGlyphScale / 100f
    val slot = (1f - PILL_CENTER_ZONE_SHARE) / 2f
    Row(
        modifier = modifier
            .width(settings.width.dp)
            .height(settings.height.dp)
            .graphicsLayer { alpha = ui.chimeShow.value },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The slot clips, so a sun rising from or sinking into the bottom edge is cut off by it like a horizon.
        Box(modifier = Modifier.weight(slot).fillMaxHeight().clipToBounds(), contentAlignment = Alignment.Center) {
            // The previous moment's glyph goes away: sinking down (sunrise, sunset) or swelling into the new one (afternoon, midnight) ...
            MomentGlyphView(DayMoments.previous(moment), glyphScale) { DayMoments.outgoingPose(moment, tick.value) }
            // ... and the new one comes up from behind the horizon, grows out of the old one, or settles in over the sinking sun.
            MomentGlyphView(moment, glyphScale) { DayMoments.incomingPose(moment, tick.value) }
        }
        Spacer(Modifier.weight(PILL_CENTER_ZONE_SHARE))
        Spacer(Modifier.weight(slot))
    }
}
