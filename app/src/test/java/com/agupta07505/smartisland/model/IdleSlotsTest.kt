/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleSlotsTest {
    @Test
    fun glyphScaleIsKeptInRange() {
        assertEquals(70, IdleSlots.normalizeGlyphScale(10))
        assertEquals(100, IdleSlots.normalizeGlyphScale(100))
        assertEquals(150, IdleSlots.normalizeGlyphScale(900))
        assertTrue(IdleSlots.GLYPH_SCALES.all { it in IdleSlots.MIN_GLYPH_SCALE..IdleSlots.MAX_GLYPH_SCALE })
        assertTrue(IdleSlots.DEFAULT_GLYPH_SCALE in IdleSlots.GLYPH_SCALES)
    }

    @Test
    fun dateIsDayAndAbbreviatedMonth() {
        assertEquals("dd", IdleSlots.DAY_PATTERN)
        assertEquals("MMM", IdleSlots.MONTH_PATTERN)
        val d = java.text.SimpleDateFormat(IdleSlots.DAY_PATTERN, java.util.Locale.US)
        val m = java.text.SimpleDateFormat(IdleSlots.MONTH_PATTERN, java.util.Locale.US)
        val feb5 = java.util.GregorianCalendar(2026, java.util.Calendar.FEBRUARY, 5).time
        assertEquals("05", d.format(feb5))
        assertEquals("Feb", m.format(feb5))
    }

    @Test
    fun alarmGlyphAndHourAlternate() {
        assertFalse(IdleSlots.alarmShowsHour(0))
        assertTrue(IdleSlots.alarmShowsHour(1))
        assertFalse(IdleSlots.alarmShowsHour(2))
        assertTrue(IdleSlots.alarmShowsHour(3))
    }

    @Test
    fun alarmHourFollowsTheClockFormat() {
        assertEquals("07", IdleSlots.alarmHourText(7, true))
        assertEquals("19", IdleSlots.alarmHourText(19, true))
        assertEquals("7", IdleSlots.alarmHourText(7, false))
        assertEquals("7", IdleSlots.alarmHourText(19, false))
        assertEquals("12", IdleSlots.alarmHourText(0, false))
        assertEquals("12", IdleSlots.alarmHourText(12, false))
        assertEquals("00", IdleSlots.alarmHourText(0, true))
        assertEquals("05", IdleSlots.alarmMinuteText(5))
        assertEquals("30", IdleSlots.alarmMinuteText(30))
    }

    @Test
    fun batteryPowerTexts() {
        assertEquals("8.4W", IdleSlots.shortWatts(8.4))
        assertEquals("18W", IdleSlots.shortWatts(18.2))
        assertEquals("10W", IdleSlots.shortWatts(9.96))
        assertEquals("36°", IdleSlots.shortBatteryTemp(362, false))
        assertEquals("97°", IdleSlots.shortBatteryTemp(362, true))
        assertTrue(IdleSlots.batteryShowsPower(true, 5.0))
        assertFalse(IdleSlots.batteryShowsPower(true, null))
        assertFalse(IdleSlots.batteryShowsPower(false, 5.0))
    }

    @Test
    fun speedLeadFollowsTheFasterSide() {
        assertEquals(IdleSlots.SpeedLead.Upload, IdleSlots.speedLead(5_000, 80_000))
        assertEquals(IdleSlots.SpeedLead.Download, IdleSlots.speedLead(900_000, 80_000))
        assertEquals(IdleSlots.SpeedLead.None, IdleSlots.speedLead(200, 300))
        assertEquals(IdleSlots.SpeedLead.None, IdleSlots.speedLead(5_000, 5_000))
        assertTrue(IdleSlots.speedLeadOwnsSlot(IdleSlots.SpeedLead.Upload, right = false))
        assertTrue(IdleSlots.speedLeadOwnsSlot(IdleSlots.SpeedLead.Download, right = true))
        assertFalse(IdleSlots.speedLeadOwnsSlot(IdleSlots.SpeedLead.Download, right = false))
    }

    @Test
    fun sideTickSkipsAlarmGlyphHourAndSelfTickingSlots() {
        val none = IdleSlots.SpeedLead.None
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.NextAlarm, right = false, ownTickRunning = false, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.NextAlarm, right = true, ownTickRunning = false, lead = none))
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.Weather, right = false, ownTickRunning = true, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.Weather, right = false, ownTickRunning = false, lead = none))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.Date, right = true, ownTickRunning = false, lead = none))
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.NetSpeed, right = false, ownTickRunning = false, lead = IdleSlots.SpeedLead.Upload))
        assertTrue(IdleSlots.takesSideTick(IdleWidgetKind.NetSpeed, right = true, ownTickRunning = false, lead = IdleSlots.SpeedLead.Upload))
    }

    @Test
    fun tickProgressHopsThriceAndRests() {
        assertEquals(0f, IdleSlots.tickProgress(0f), 1e-6f)
        assertEquals(1f, IdleSlots.tickProgress(1f), 1e-6f)
        // End of each step's hold sits exactly on the step boundary: 1/3 and 2/3.
        assertEquals(1f / 3f, IdleSlots.tickProgress(0.30f), 1e-4f)
        assertEquals(1f / 3f, IdleSlots.tickProgress(0.33f), 1e-2f)
        assertEquals(2f / 3f, IdleSlots.tickProgress(0.63f), 1e-2f)
        var last = 0f
        for (i in 0..100) {
            val v = IdleSlots.tickProgress(i / 100f)
            assertTrue(v >= last - 1e-6f)
            last = v
        }
    }

    @Test
    fun heavyTickHangsBackSlowlyThenSnapsThrough() {
        assertEquals(0f, IdleSlots.heavyTickProgress(0f), 1e-6f)
        assertEquals(1f, IdleSlots.heavyTickProgress(1f), 1e-6f)
        // The pull-back ends exactly at -depth, at the end of the wind-up share.
        assertEquals(-IdleSlots.HEAVY_WINDUP_DEPTH, IdleSlots.heavyTickProgress(IdleSlots.HEAVY_WINDUP_SHARE), 1e-5f)
        // Slow: at half of the wind-up it has not yet gone half way back (smooth start), and it never goes below -depth.
        var min = 0f
        var last = Float.NEGATIVE_INFINITY
        for (i in 0..200) {
            val f = i / 200f
            val v = IdleSlots.heavyTickProgress(f)
            if (v < min) min = v
            assertTrue(v <= 1f + 1e-6f)
            if (f >= IdleSlots.HEAVY_WINDUP_SHARE) { assertTrue(v >= last - 1e-6f); last = v }
        }
        assertEquals(-IdleSlots.HEAVY_WINDUP_DEPTH, min, 1e-5f)
        // The snap after the wind-up is much faster than the wind-up: more distance in a tenth of the time.
        val windStep = IdleSlots.heavyTickProgress(0.04f) - IdleSlots.heavyTickProgress(0f)
        val snapStep = IdleSlots.heavyTickProgress(0.46f) - IdleSlots.heavyTickProgress(0.40f)
        assertTrue(snapStep > -windStep * 3f)
    }

    @Test
    fun dayMomentsNeverTakeTheSideTick() {
        assertFalse(IdleSlots.takesSideTick(IdleWidgetKind.DayMoments, right = false, ownTickRunning = false, lead = IdleSlots.SpeedLead.None))
    }
}
