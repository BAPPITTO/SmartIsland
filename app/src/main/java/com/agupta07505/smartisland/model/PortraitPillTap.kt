/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Portrait: there is no separate idle pill. The collapsed cutout is a camera-hole pill with its glyphs on the
 * left and right (and the privacy dots at the far right); the middle of it is empty. That empty middle band is the
 * idle control, the portrait twin of the landscape idle dot:
 *  - collapsed with cards: a tap in the band puts every card away (the two-step close with step 1 skipped);
 *  - expanded: a tap in the band, which is the top of the expanded island where the pill sits, closes the island with
 *    the secondary cutout's merge, then puts the cards away;
 *  - no cards, stored ones waiting: a tap anywhere on the idle cutout reveals them (unchanged).
 * A tap on a glyph (left / right of the band) keeps its normal job: it expands that event (collapsed) or is the
 * expanded card's own tap.
 *
 * Collapsed, the island's own tap gesture asks [pillCenterTapHit] (px, relative to the shell, which is the pill). Expanded,
 * the card's own click would take the tap, so a transparent target of the same size ([PILL_CENTER_ZONE_SHARE] of the pill's
 * width, the pill's height, top centre of the shell) sits above the card (`PortraitPillCenterTarget`); it draws nothing.
 */
const val PILL_CENTER_ZONE_SHARE = 0.34f

/** True when a touch at ([xPx], [yPx]) in a shell [shellWidthPx] wide lands in the glyph-free centre band of the pill. */
fun pillCenterTapHit(
    xPx: Float,
    yPx: Float,
    shellWidthPx: Float,
    pillWidthPx: Float,
    pillHeightPx: Float
): Boolean {
    if (shellWidthPx <= 0f || pillWidthPx <= 0f || pillHeightPx <= 0f) return false
    val half = pillWidthPx * PILL_CENTER_ZONE_SHARE / 2f
    val centre = shellWidthPx / 2f
    return yPx in 0f..pillHeightPx && xPx in (centre - half)..(centre + half)
}

/** The camera hole in dp: centre X from the screen centre, top edge from the screen top. */
data class CutoutArea(val centreXDp: Float, val topDp: Float, val widthDp: Float, val heightDp: Float)

/** Smallest touch target (dp) over the camera cutout: a real hole is often smaller than a finger. */
const val CUTOUT_TARGET_MIN_WIDTH_DP = 40f
const val CUTOUT_TARGET_MIN_HEIGHT_DP = 28f

/** The camera cutout grown to at least the minimum touch target, around its own centre (the top never goes above the screen). */
fun cutoutTouchTarget(area: CutoutArea): CutoutArea {
    val w = maxOf(area.widthDp, CUTOUT_TARGET_MIN_WIDTH_DP)
    val h = maxOf(area.heightDp, CUTOUT_TARGET_MIN_HEIGHT_DP)
    val centreY = area.topDp + area.heightDp / 2f
    return CutoutArea(area.centreXDp, (centreY - h / 2f).coerceAtLeast(0f), w, h)
}

/** How far (dp) the pill's centre band must reach into the camera cutout, on both axes, to count as overlapping it. */
const val CUTOUT_OVERLAP_MIN_DP = 1f

/**
 * True when the collapsed pill's glyph-free centre band overlaps the camera cutout by at least [minOverlapDp] on both axes. All values
 * are dp in screen space: X as the centre measured from the screen centre, Y as the top edge measured from the screen top.
 * Null cutout (nothing detected, nothing set by hand) never overlaps.
 */
fun centreBandOverlapsCutout(
    bandCentreXDp: Float,
    bandWidthDp: Float,
    bandTopDp: Float,
    bandHeightDp: Float,
    cutoutCentreXDp: Float?,
    cutoutWidthDp: Float,
    cutoutTopDp: Float,
    cutoutHeightDp: Float,
    minOverlapDp: Float = CUTOUT_OVERLAP_MIN_DP
): Boolean {
    if (cutoutCentreXDp == null || bandWidthDp <= 0f || bandHeightDp <= 0f || cutoutWidthDp <= 0f || cutoutHeightDp <= 0f) return false
    val horizontal = minOf(bandCentreXDp + bandWidthDp / 2f, cutoutCentreXDp + cutoutWidthDp / 2f) -
        maxOf(bandCentreXDp - bandWidthDp / 2f, cutoutCentreXDp - cutoutWidthDp / 2f)
    val vertical = minOf(bandTopDp + bandHeightDp, cutoutTopDp + cutoutHeightDp) - maxOf(bandTopDp, cutoutTopDp)
    return horizontal >= minOverlapDp && vertical >= minOverlapDp
}

/**
 * Collapsed pill: does a long press open the Open Apps shortcut (the quick-launch grid)? It replaces the old tap on the empty island.
 *  - the pill's centre band overlaps the camera cutout: only a long press on a glyph (outside the band) opens it; the band keeps the normal
 *    long-press action;
 *  - no overlap: a long press anywhere on the collapsed cutout opens it.
 * Notch mode behaves as "overlapping": the pill is the notch, so its glyphs open the apps and its middle keeps the normal long press.
 * Landscape: the collapsed pill (a card or the widget pill on the island, [pillHasContent]) always opens the Open Apps shortcut (no overlap
 * rule: the pill is not on the camera there). The idle cutout (no content) is not this pill: a long press on it only runs the normal
 * long-press action (hide / show the pill), like the idle circle.
 * Off when the shortcut launcher is switched off (the normal long-press action runs then).
 */
fun longPressOpensApps(
    isLandscape: Boolean,
    notchMode: Boolean,
    shortcutsEnabled: Boolean,
    centreOverlapsCutout: Boolean,
    touchInCentreBand: Boolean,
    pillHasContent: Boolean = true
): Boolean {
    if (!shortcutsEnabled) return false
    if (isLandscape) return pillHasContent
    return if (centreOverlapsCutout || notchMode) !touchInCentreBand else true
}

/** Whether a tap in the pill's centre band is the idle control at all: portrait, with cards on the island. */
fun portraitCenterTapIsIdleControl(isLandscape: Boolean, notchMode: Boolean, hasCards: Boolean): Boolean =
    !isLandscape && hasCards

/**
 * Where the transparent close target of the expanded island sits, in dp, measured from the top centre of the overlay window.
 * [offsetXDp] is null when the target should follow the pill's own sideways offset (the old behaviour).
 */
data class ExpandedCloseZone(
    val widthDp: Float,
    val heightDp: Float,
    val offsetXDp: Float?,
    val topDp: Float
)

/**
 * The close target for an expanded island in portrait. On a phone with a camera cutout (`CameraCutoutDetector`) the target is
 * the cutout itself: its detected width and height, centred on the hole, starting at the hole's top edge. The width never
 * exceeds the pill's centre band ([PILL_CENTER_ZONE_SHARE]), so the target never covers the glyphs. [windowTopDp] is the overlay window's own top (the
 * window is moved down by the pill's Y offset), so the cutout's screen position is converted to window space. Without a
 * hardware cutout the target is the centre band of the pill, as before.
 */
fun expandedCloseZone(
    hasHardwareCutout: Boolean,
    cutoutWidthDp: Float,
    cutoutHeightDp: Float,
    cutoutXOffsetDp: Float,
    cutoutTopDp: Float,
    windowTopDp: Float,
    pillWidthDp: Float,
    pillHeightDp: Float
): ExpandedCloseZone {
    if (!hasHardwareCutout || cutoutWidthDp <= 0f || cutoutHeightDp <= 0f) {
        return ExpandedCloseZone(pillWidthDp * PILL_CENTER_ZONE_SHARE, pillHeightDp, null, 0f)
    }
    return ExpandedCloseZone(
        // Never wider than the pill's glyph-free centre band: the detected width is the hole plus padding and is raised to the
        // pill's minimum width (76 dp), so on its own it reached over the glyphs left and right of the band.
        widthDp = minOf(cutoutWidthDp, pillWidthDp * PILL_CENTER_ZONE_SHARE),
        heightDp = cutoutHeightDp,
        offsetXDp = cutoutXOffsetDp,
        topDp = (cutoutTopDp - windowTopDp).coerceAtLeast(0f)
    )
}

