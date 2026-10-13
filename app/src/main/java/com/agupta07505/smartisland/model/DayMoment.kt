/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone

/**
 * The four day moments the Day moments idle widget marks with a glyph in the pill's left slot, at the phone's local time (the time zone of where
 * you are): sunrise at 06:00 (the sun rises from the bottom), afternoon at 12:00 (the sun grows out of the sunrise glyph), sunset at 17:30 (the sun sinks into the bottom)
 * and midnight at 23:00 (the moon grows out of the sunset glyph).
 * Pure rules; the drawing is ui/DayMoment.kt.
 */
enum class DayMoment(val id: String, val hour: Int, val minute: Int, val towardsDown: Boolean) {
    Sunrise("sunrise", 6, 0, false),
    Afternoon("afternoon", 12, 0, false),
    Sunset("sunset", 17, 30, true),
    Midnight("midnight", 23, 0, true)
}

/** How a moment's glyph pair moves during the tick. */
enum class MomentMotion {
    /** The sun comes up from behind the horizon line; the old glyph sinks away down. */
    RiseFromBottom,
    /** The new glyph grows to full size out of the old one, in place. */
    GrowFromPrevious,
    /** The old sun sinks down past the bottom edge; the horizon glyph settles in. */
    SinkIntoBottom
}

/** What is drawn for a moment: the half sun on its horizon line, the full sun, or the moon. */
enum class MomentGlyph { Horizon, Sun, Moon }

/**
 * Pose of one glyph at a tick progress: [offsetShare] of its own height (positive = down), [scale] and [alpha]. For the horizon glyph [lift] is how
 * far the sun has come up from behind the fixed horizon line (0 = hidden, 1 = at rest).
 */
data class GlyphPose(val offsetShare: Float, val scale: Float, val alpha: Float, val lift: Float = 1f)

object DayMoments {
    /** The glyph that sinks leaves this far (share of its height) below the slot's bottom edge, fully hidden by the clip. */
    const val HIDDEN_BELOW_SHARE = 1.1f
    /** A glyph that grows out of its predecessor starts at this share of its full size. */
    const val GROW_FROM_SCALE = 0.45f
    /** The horizon line (alpha of the horizon glyph) is fully in by this share of the rise. */
    const val HORIZON_IN_SHARE = 0.35f

    fun glyph(moment: DayMoment): MomentGlyph = when (moment) {
        DayMoment.Sunrise, DayMoment.Sunset -> MomentGlyph.Horizon
        DayMoment.Afternoon -> MomentGlyph.Sun
        DayMoment.Midnight -> MomentGlyph.Moon
    }

    fun motion(moment: DayMoment): MomentMotion = when (moment) {
        DayMoment.Sunrise -> MomentMotion.RiseFromBottom
        DayMoment.Afternoon, DayMoment.Midnight -> MomentMotion.GrowFromPrevious
        DayMoment.Sunset -> MomentMotion.SinkIntoBottom
    }

    /** The glyph of [moment] coming in, at tick [progress] (0 = start, 1 = at rest; the heavy tick's wind-up below 0 counts as 0). */
    fun incomingPose(moment: DayMoment, progress: Float): GlyphPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion(moment)) {
            // The horizon line stays where it is; only the sun comes up from behind it.
            MomentMotion.RiseFromBottom -> GlyphPose(0f, 1f, (p / HORIZON_IN_SHARE).coerceIn(0f, 1f), lift = p)
            MomentMotion.GrowFromPrevious -> GlyphPose(0f, GROW_FROM_SCALE + (1f - GROW_FROM_SCALE) * p, p)
            MomentMotion.SinkIntoBottom -> GlyphPose(0f, 1f, (p / HORIZON_IN_SHARE).coerceIn(0f, 1f), lift = 1f)
        }
    }

    /** The previous moment's glyph going away, at tick [progress]. */
    fun outgoingPose(moment: DayMoment, progress: Float): GlyphPose {
        val p = progress.coerceIn(0f, 1f)
        return when (motion(moment)) {
            MomentMotion.RiseFromBottom -> GlyphPose(HIDDEN_BELOW_SHARE * p, 1f, 1f - p)
            // The old glyph swells into the new one: it keeps growing while it fades, so the two read as one shape.
            MomentMotion.GrowFromPrevious -> GlyphPose(0f, 1f + (1f - GROW_FROM_SCALE) * 0.25f * p, 1f - p)
            MomentMotion.SinkIntoBottom -> GlyphPose(HIDDEN_BELOW_SHARE * p, 1f, 1f - p)
        }
    }

    /** The moment before [moment] in the day's cycle (sunrise follows midnight): its glyph is what ticks away. */
    fun previous(moment: DayMoment): DayMoment {
        val all = DayMoment.values()
        return all[Math.floorMod(moment.ordinal - 1, all.size)]
    }

    /** A moment on a full hour that is also an hour tick waits this long (ms) for the hour tick to finish first. */
    const val AFTER_HOUR_TICK_MS = 7000L

    /** A day moment plays with the same timing as the hour chime: hold, heavy tick, hold, with a short fade in and out (ms). */
    const val HOLD_BEFORE_MS = HourChime.HOLD_BEFORE_MS
    const val TICK_MS = HourChime.TICK_MS
    const val HOLD_AFTER_MS = HourChime.HOLD_AFTER_MS
    const val FADE_IN_MS = HourChime.FADE_IN_MS
    const val FADE_OUT_MS = HourChime.FADE_OUT_MS

    /** When [moment] happens on the day [dayOffset] days after the day of [nowMs] in [zone]. */
    fun atMs(moment: DayMoment, nowMs: Long, dayOffset: Int = 0, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = nowMs
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, moment.hour)
        cal.set(Calendar.MINUTE, moment.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** The first moment strictly after [nowMs] and when it happens (today's, or tomorrow's once today's four have passed). */
    fun next(nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Pair<DayMoment, Long> =
        DayMoment.values()
            .flatMap { m -> listOf(0, 1).map { d -> m to atMs(m, nowMs, d, zone) } }
            .filter { it.second > nowMs }
            .minByOrNull { it.second }!!
}
