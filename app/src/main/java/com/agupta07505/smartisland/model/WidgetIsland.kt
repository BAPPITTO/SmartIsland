/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The semi-expanded idle widget island: a tap on the widget glyph grows the collapsed cutout into a small island (about twice the
 * cutout), a tap outside closes it back to the cutout. Pure rules only; the drawing is ui/WidgetIsland.kt.
 */

/** Where on the collapsed pill a touch landed: the glyph slot (left), the glyph-free middle band, or the value slot (right). */
enum class PillTapZone { Glyph, Middle, Value }

/**
 * Splits the pill into its three zones. The middle band is the same one [pillCenterTapHit] uses ([PILL_CENTER_ZONE_SHARE] of the pill's width,
 * around its centre), so the camera hole keeps its meaning. [xPx] is measured in the shell, which is the pill while collapsed.
 */
fun pillTapZone(xPx: Float, shellWidthPx: Float, pillWidthPx: Float): PillTapZone {
    val half = pillWidthPx * PILL_CENTER_ZONE_SHARE / 2f
    val centre = shellWidthPx / 2f
    return when {
        xPx < centre - half -> PillTapZone.Glyph
        xPx > centre + half -> PillTapZone.Value
        else -> PillTapZone.Middle
    }
}

/** What a tap on the collapsed pill does while an idle widget is on it and no card is. */
enum class WidgetTapAction {
    /** A tap on the glyph slot: for now it grows (or, when it is open, closes) the semi-expanded widget island. */
    GlyphTap,

    /** A tap on the value slot: for now the same as [GlyphTap], kept apart so it can get its own action later. */
    ValueTap,

    /** The normal idle tap: show the stored cards the idle timeout put away. */
    RevealStoredCards,

    /** Nothing from the widgets: the pill's other taps decide. */
    None
}

/**
 * Tap rules of the collapsed pill while an idle widget is on it (portrait, and landscape's collapsed pill: same zones in both):
 *  - glyph slot ([WidgetTapAction.GlyphTap]) and value slot ([WidgetTapAction.ValueTap]): both open the widget island for now, but they are two
 *    separate actions with their own hooks, so either can be given another job without touching the other;
 *  - middle band: the stored cards, like before (nothing when there are none).
 * The small round idle circle ([compact]) is not this pill and keeps its own behaviour untouched: [WidgetTapAction.None].
 * No widget on the pill: [WidgetTapAction.None].
 */
fun idleWidgetTapAction(
    widgetOnPill: Boolean,
    compact: Boolean,
    zone: PillTapZone,
    hasStoredCards: Boolean
): WidgetTapAction {
    if (!widgetOnPill || compact) return WidgetTapAction.None
    return when (zone) {
        PillTapZone.Middle -> if (hasStoredCards) WidgetTapAction.RevealStoredCards else WidgetTapAction.None
        PillTapZone.Glyph -> WidgetTapAction.GlyphTap
        PillTapZone.Value -> WidgetTapAction.ValueTap
    }
}

/** How much bigger than the collapsed cutout the widget island is, on both axes. */
const val WIDGET_ISLAND_SCALE = 2f

/** The island never gets smaller than this (dp): the small landscape circle doubled would be too small for an icon and a value. */
const val WIDGET_ISLAND_MIN_WIDTH_DP = 124f
const val WIDGET_ISLAND_MIN_HEIGHT_DP = 64f

/** Corner radius (dp) of the open island; the collapsed cutout's own radius grows into it. */
const val WIDGET_ISLAND_CORNER_RADIUS_DP = 28f

/** Gap (dp) kept between the island and the screen edge. */
const val WIDGET_ISLAND_EDGE_MARGIN_DP = 8f

data class WidgetIslandSize(val widthDp: Float, val heightDp: Float)

/**
 * Size (dp) of the open widget island: [WIDGET_ISLAND_SCALE] times the cutout, not below the minimum, never wider than the screen less the margins.
 * A widget with a list (the notification count lists its apps) passes the height its rows need as [contentHeightDp]; the island grows to it.
 */
fun widgetIslandSize(pillWidthDp: Float, pillHeightDp: Float, screenWidthDp: Float, contentHeightDp: Float = 0f): WidgetIslandSize {
    val maxWidth = (screenWidthDp - 2f * WIDGET_ISLAND_EDGE_MARGIN_DP).coerceAtLeast(0f)
    val width = maxOf(pillWidthDp * WIDGET_ISLAND_SCALE, WIDGET_ISLAND_MIN_WIDTH_DP).coerceAtMost(maxWidth)
    val height = maxOf(pillHeightDp * WIDGET_ISLAND_SCALE, WIDGET_ISLAND_MIN_HEIGHT_DP, contentHeightDp)
    return WidgetIslandSize(width, height)
}

/**
 * Where the open island's centre sits, in dp from the screen centre: on the pill's own centre ([pillCentreDp], from the screen centre), moved
 * inwards only as far as needed to keep [WIDGET_ISLAND_EDGE_MARGIN_DP] to the screen edge (a landscape cutout sits in a corner).
 */
fun widgetIslandCentre(pillCentreDp: Float, islandWidthDp: Float, screenWidthDp: Float): Float {
    val limit = (screenWidthDp / 2f - WIDGET_ISLAND_EDGE_MARGIN_DP - islandWidthDp / 2f).coerceAtLeast(0f)
    return pillCentreDp.coerceIn(-limit, limit)
}

/** Cards ranked at or above this ([IslandPriority.rank], lower = more important) close an open widget island: calls, navigation, live and ongoing events. */
const val WIDGET_ISLAND_IMPORTANT_RANK = 20

/**
 * Whether a card of [mode] closes an open widget island when it arrives. Calls and the live / ongoing events (navigation, live activities, timer,
 * stopwatch, recorders, hotspot, flashlight) do; music, downloads, ordinary notifications and the system event cards wait behind the island.
 */
fun cardClosesWidgetIsland(mode: IslandMode): Boolean = IslandPriority.rank(mode) <= WIDGET_ISLAND_IMPORTANT_RANK

/**
 * Whether an open widget island has to close by itself:
 *  - the island is expanded ([expanded]) or an important card ([hasImportantCard], see [cardClosesWidgetIsland]) is on it;
 *  - no widget is available any more ([widgetAvailable] false: widgets switched off, screen off). A card that waits behind the island (the pill shows
 *    it, so the idle widget is off the pill) does not count as lost: the caller keeps the widget available then;
 *  - the widget it shows has nothing to say any more ([hasContent] false: the charger was unplugged, the alarm rang);
 *  - the pill became the small round idle circle ([compact], landscape): that circle has no island and stays untouched.
 */
fun widgetIslandMustClose(
    expanded: Boolean,
    hasImportantCard: Boolean,
    widgetAvailable: Boolean,
    hasContent: Boolean = true,
    compact: Boolean = false
): Boolean = expanded || hasImportantCard || !widgetAvailable || !hasContent || compact

/**
 * The rotation step a swipe on the pill moves an open island to: from [step] in [direction] (+1 next, -1 previous) over [count] widgets, skipping
 * widgets for which [hasContent] is false (index = position in the rotation). Null when no other widget has content: the swipe springs back.
 */
fun nextStepWithContent(step: Int, direction: Int, count: Int, hasContent: (Int) -> Boolean): Int? {
    if (count < 2 || direction == 0) return null
    val dir = if (direction > 0) 1 else -1
    var target = step
    repeat(count - 1) {
        target += dir
        if (hasContent(Math.floorMod(target, count))) return target
    }
    return null
}

/**
 * The widget island's auto-close timer. The island closes by itself after [DEFAULT_SECONDS] without a touch; the setting runs from [MIN_SECONDS] up
 * to [MAX_SECONDS], and [NEVER] (0) keeps it open until a tap outside, a card or the pill losing its widget closes it.
 */
object WidgetIslandTimer {
    const val NEVER = 0
    const val MIN_SECONDS = 2
    const val MAX_SECONDS = 60
    const val DEFAULT_SECONDS = 3

    /** Stored value to a usable one: 0 or less is [NEVER], everything else stays within [MIN_SECONDS]..[MAX_SECONDS]. */
    fun normalize(seconds: Int): Int = if (seconds <= NEVER) NEVER else seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)

    /** Delay (ms) until the open island closes by itself, or null for never. */
    fun closeDelayMs(seconds: Int): Long? = normalize(seconds).let { if (it == NEVER) null else it * 1000L }
}
