/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.util

import android.app.PendingIntent
import com.agupta07505.smartisland.model.IslandNotificationAction
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeActionsTest {
    private fun real(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())
    private fun view(title: String) = IslandNotificationAction(title, mockk<PendingIntent>())

    @Test
    fun runningTimerShowsPauseAndAddMinuteOnly() {
        val actions = ShadeActions.mirror(listOf(real("Pause"), real("+1:00")), emptyList())
        assertEquals(listOf("Pause", "+1:00"), actions.map { it.title })
    }

    @Test
    fun pausedTimerShowsResumeAndStop() {
        val actions = ShadeActions.mirror(listOf(real("Resume"), real("Stop")), emptyList())
        assertEquals(listOf("Resume", "Stop"), actions.map { it.title })
        assertTrue(ShadeActions.offersResume(actions))
    }

    @Test
    fun runningStopwatchShowsPauseAndLapOnly() {
        val actions = ShadeActions.mirror(listOf(real("Pause"), real("Lap")), emptyList())
        assertEquals(listOf("Pause", "Lap"), actions.map { it.title })
        assertFalse(ShadeActions.offersResume(actions))
    }

    @Test
    fun buttonsWithoutAnIntentAreNotShown() {
        val actions = ShadeActions.mirror(
            listOf(real("Pause"), IslandNotificationAction("Stop", null)), emptyList()
        )
        assertEquals(listOf("Pause"), actions.map { it.title })
    }

    @Test
    fun noMoreThanTheShadeLimit() {
        val actions = ShadeActions.mirror(
            listOf(real("Pause"), real("Lap"), real("Stop"), real("Reset")), emptyList()
        )
        assertEquals(ShadeActions.SHADE_ACTION_LIMIT, actions.size)
    }

    @Test
    fun customViewButtonsWinAndKeepTheirOrder() {
        val actions = ShadeActions.mirror(
            standard = emptyList(),
            view = listOf(view("pause button"), view("Add 1 minute"))
        )
        assertEquals(listOf("pause button", "Add 1 minute"), actions.map { it.title })
    }

    @Test
    fun theAppsOwnIntentReplacesTheViewProxy() {
        val own = real("Pause")
        val actions = ShadeActions.mirror(listOf(own), listOf(view("Pause"), view("+1:00")))
        assertEquals(2, actions.size)
        assertSame(own.pendingIntent, actions[0].pendingIntent)
    }

    @Test
    fun clickableViewsThatAreNotButtonsAreDropped() {
        val actions = ShadeActions.mirror(emptyList(), listOf(view("notification content"), view("Pause")))
        assertEquals(listOf("Pause"), actions.map { it.title })
    }
}
