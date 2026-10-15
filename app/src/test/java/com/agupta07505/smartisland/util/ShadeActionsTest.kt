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

    private val google = "com.google.android.deskclock"
    private val timer = TimerStopwatchParser.ClockKind.Timer
    private val stopwatch = TimerStopwatchParser.ClockKind.Stopwatch

    private fun titles(a: List<IslandNotificationAction>) = a.map { it.title }

    @Test
    fun runningGoogleTimerNeverShowsStop() {
        // The layout carries every button; only Pause and +1:00 belong to a running timer.
        val a = ShadeActions.mirror(
            emptyList(), listOf(view("Pause"), view("+1:00"), view("Stop")), timer, google
        )
        assertEquals(listOf("Pause", "+1:00"), titles(a))
    }

    @Test
    fun pausedGoogleTimerShowsResumeAndStopOnly() {
        val a = ShadeActions.mirror(
            emptyList(), listOf(view("Resume"), view("Lap"), view("Stop")), timer, google
        )
        assertEquals(listOf("Resume", "Stop"), titles(a))
    }

    @Test
    fun runningGoogleStopwatchHasLapButNoStop() {
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("Lap"), real("Stop")), emptyList(), stopwatch, google
        )
        assertEquals(listOf("Pause", "Lap"), titles(a))
    }

    @Test
    fun pausedGoogleStopwatchHasNoLap() {
        val a = ShadeActions.mirror(
            listOf(real("Resume"), real("Lap"), real("Stop")), emptyList(), stopwatch, google
        )
        assertEquals(listOf("Resume", "Stop"), titles(a))
    }

    @Test
    fun stateIsEnforcedBeforeTheShadeLimit() {
        // Four standard buttons: Stop must not take the place of Lap just because it comes before it.
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("Stop"), real("Reset"), real("Lap")), emptyList(), stopwatch, google
        )
        assertEquals(listOf("Pause", "Lap"), titles(a))
    }

    @Test
    fun otherClockAppsAreNotForcedIntoAState() {
        // A third-party clock may legitimately show Stop while running: only the visible buttons count.
        val a = ShadeActions.mirror(
            listOf(real("Pause"), real("Stop")), emptyList(), timer, "com.some.thirdparty.timer"
        )
        assertEquals(listOf("Pause", "Stop"), titles(a))
    }

    @Test
    fun aListWithNoStateIsLeftAlone() {
        val a = ShadeActions.mirror(listOf(real("Stop"), real("Lap")), emptyList(), stopwatch, google)
        assertEquals(listOf("Stop", "Lap"), titles(a))
    }

    @Test
    fun everyGoogleClockStateOffersExactlyItsButtons() {
        fun shown(kind: TimerStopwatchParser.ClockKind, vararg visible: String) =
            titles(ShadeActions.mirror(visible.map { real(it) }, emptyList(), kind, google))
        // The layout always carries every button; the state decides.
        assertEquals(listOf("Pause", "+1:00"), shown(timer, "Pause", "Stop", "+1:00"))
        assertEquals(listOf("Resume", "Stop"), shown(timer, "Resume", "Stop"))
        assertEquals(listOf("Pause", "Lap"), shown(stopwatch, "Pause", "Lap", "Stop"))
        assertEquals(listOf("Resume", "Reset"), shown(stopwatch, "Resume", "Reset", "Lap"))
    }

    @Test
    fun oemClocksKeepTheirOwnButtonsUntouched() {
        // Not enforced for a phone maker's clock: Stop next to Pause stays.
        val actions = ShadeActions.mirror(
            listOf(real("Pause"), real("Stop")), emptyList(), timer, "com.miui.deskclock"
        )
        assertEquals(listOf("Pause", "Stop"), titles(actions))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.miui.deskclock"))
        assertFalse(TimerStopwatchParser.usesShadeFixes("com.sec.android.app.clockpackage"))
        assertTrue(TimerStopwatchParser.usesShadeFixes(google))
    }

    @Test
    fun proxyButtonsAreFlaggedAndTheAppsOwnIntentIsNot() {
        val proxy = IslandNotificationAction("Pause", mockk<PendingIntent>(), viaView = true)
        val own = real("Pause")
        assertTrue(proxy.viaView)
        assertFalse(own.viaView)
        // When the app also offers the same button as a standard action, that one is used (no proxy involved).
        val mirrored = ShadeActions.mirror(listOf(own), listOf(proxy), timer, google)
        assertFalse(mirrored.single().viaView)
    }
}
