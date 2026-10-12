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

/** The notification-count and mode widgets in the shared widget rules. */
class IdleWidgetNewKindsTest {
    @Test
    fun idsRoundTrip() {
        assertEquals(IdleWidgetKind.Notifications, IdleWidgetKind.fromId("notifications"))
        assertEquals(IdleWidgetKind.Modes, IdleWidgetKind.fromId("modes"))
        val kinds = listOf(IdleWidgetKind.Notifications, IdleWidgetKind.Modes, IdleWidgetKind.Date)
        assertEquals(kinds, IdleWidgets.parse(IdleWidgets.serialize(kinds)))
    }

    @Test
    fun selectAllIncludesThem() {
        val all = IdleWidgets.parse(IdleWidgetOff.allIds())
        assertTrue(IdleWidgetKind.Notifications in all)
        assertTrue(IdleWidgetKind.Modes in all)
    }

    @Test
    fun onlyTheLeftSlotIsUsedSoTheRightOneTakesNoSideTick() {
        val lead = IdleSlots.SpeedLead.None
        for (kind in listOf(IdleWidgetKind.Notifications, IdleWidgetKind.Modes)) {
            assertTrue(IdleSlots.takesSideTick(kind, right = false, ownTickRunning = false, lead = lead))
            assertFalse(IdleSlots.takesSideTick(kind, right = true, ownTickRunning = false, lead = lead))
            assertFalse(IdleSlots.takesSideTick(kind, right = false, ownTickRunning = true, lead = lead))
        }
    }

    @Test
    fun islandHasContentOnlyWhileThereIsSomethingToSay() {
        fun has(kind: IdleWidgetKind, count: Int = 0, mode: Boolean = false) =
            WidgetIslandContent.hasContent(kind, false, 0L, 0L, false, notificationCount = count, modeActive = mode)
        assertFalse(has(IdleWidgetKind.Notifications))
        assertTrue(has(IdleWidgetKind.Notifications, count = 2))
        assertFalse(has(IdleWidgetKind.Modes))
        assertTrue(has(IdleWidgetKind.Modes, mode = true))
        // The old widgets are unchanged by the new arguments.
        assertTrue(has(IdleWidgetKind.Date))
        assertFalse(has(IdleWidgetKind.Battery))
    }

    @Test
    fun islandGrowsForAListButNeverShrinksBelowTheNormalSize() {
        val plain = widgetIslandSize(120f, 32f, 400f)
        assertEquals(plain, widgetIslandSize(120f, 32f, 400f, contentHeightDp = 10f))
        val tall = widgetIslandSize(120f, 32f, 400f, contentHeightDp = 94f)
        assertEquals(94f, tall.heightDp, 0.001f)
        assertEquals(plain.widthDp, tall.widthDp, 0.001f)
    }
}
