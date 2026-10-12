/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayMomentTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(h: Int, m: Int, day: Int = 15): Long =
        Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.MARCH, day, h, m, 0) }.timeInMillis

    @Test
    fun directionsAndTimes() {
        assertFalse(DayMoment.Sunrise.towardsDown)
        assertFalse(DayMoment.Afternoon.towardsDown)
        assertTrue(DayMoment.Sunset.towardsDown)
        assertTrue(DayMoment.Midnight.towardsDown)
        assertEquals(6 to 0, DayMoment.Sunrise.hour to DayMoment.Sunrise.minute)
        assertEquals(12 to 0, DayMoment.Afternoon.hour to DayMoment.Afternoon.minute)
        assertEquals(17 to 30, DayMoment.Sunset.hour to DayMoment.Sunset.minute)
        assertEquals(23 to 0, DayMoment.Midnight.hour to DayMoment.Midnight.minute)
    }

    @Test
    fun nextIsTheFirstMomentAfterNow() {
        assertEquals(DayMoment.Sunrise to at(6, 0), DayMoments.next(at(3, 10), zone))
        assertEquals(DayMoment.Afternoon to at(12, 0), DayMoments.next(at(6, 0), zone))
        assertEquals(DayMoment.Afternoon to at(12, 0), DayMoments.next(at(9, 45), zone))
        assertEquals(DayMoment.Sunset to at(17, 30), DayMoments.next(at(12, 0), zone))
        assertEquals(DayMoment.Midnight to at(23, 0), DayMoments.next(at(17, 31), zone))
    }

    @Test
    fun afterTheLastMomentItIsTomorrowsSunrise() {
        assertEquals(DayMoment.Sunrise to at(6, 0, 16), DayMoments.next(at(23, 0), zone))
        assertEquals(DayMoment.Sunrise to at(6, 0, 16), DayMoments.next(at(23, 59), zone))
    }

    @Test
    fun theGlyphThatTicksAwayIsTheMomentBefore() {
        assertEquals(DayMoment.Midnight, DayMoments.previous(DayMoment.Sunrise))
        assertEquals(DayMoment.Sunrise, DayMoments.previous(DayMoment.Afternoon))
        assertEquals(DayMoment.Afternoon, DayMoments.previous(DayMoment.Sunset))
        assertEquals(DayMoment.Sunset, DayMoments.previous(DayMoment.Midnight))
    }

    @Test
    fun everyMomentSwapsToADifferentGlyph() {
        // Sunrise and sunset share the twilight glyph, but never follow each other, so a tick always changes what is drawn.
        val glyph = mapOf(
            DayMoment.Sunrise to "twilight", DayMoment.Sunset to "twilight",
            DayMoment.Afternoon to "sun", DayMoment.Midnight to "moon"
        )
        DayMoment.values().forEach { assertFalse(glyph[it] == glyph[DayMoments.previous(it)]) }
    }

    @Test
    fun theHoldBeforeTheTickIsBriefButSeen() {
        assertTrue(DayMoments.HOLD_BEFORE_MS in 300..1000)
    }
}
