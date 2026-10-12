/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import com.agupta07505.smartisland.util.AnimationSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleCloseSequenceTest {

    private val speeds = AnimationSpeed.entries

    @Test
    fun theOpeningIsTheIdleVanishUpToTheHandOverPlusThePillSpringOrTheSecondaryReturn() {
        // Normal: the vanish hands over after about 120 ms, then the pill spring settles in about 300 ms.
        val opening = IdleCloseSequence.openingMs(AnimationSpeed.NORMAL)
        val pill = pillSpringSettleMs(PILL_SPRING_STIFFNESS).toInt()
        assertTrue("opening $opening ms", opening in (pill + 40)..(pill + 220))
        // The secondary cutout comes back over 460 ms next to it: the longer one is the opening.
        assertTrue(opening >= SECONDARY_RETURN_BASE_MS)
    }

    @Test
    fun theSecondaryReturnLengthIsTheOneTheOverlayUses() {
        assertEquals(460, SECONDARY_RETURN_BASE_MS)
    }

    @Test
    fun theWholeCloseIsTheTwoStepsAdded() {
        for (speed in speeds) {
            val plan = IdleCloseSequence.plan(speed)
            assertEquals(plan.totalMs, plan.step1Ms + plan.step2Ms)
        }
    }

    @Test
    fun thePillLeavesAsLongAsItArrives() {
        for (speed in speeds) {
            val plan = IdleCloseSequence.plan(speed)
            assertEquals("$speed", IdleCloseSequence.pillArrivalMs(speed), plan.step2Ms)
        }
    }

    @Test
    fun theIslandAndSecondaryCloseSlowerThanThePillLeaves() {
        for (speed in speeds) {
            val plan = IdleCloseSequence.plan(speed)
            assertTrue("$speed", plan.step1Ms > plan.step2Ms)
            assertEquals(IdleCloseSequence.STEP1_OVER_STEP2, plan.step1Ms / plan.step2Ms.toFloat(), 0.02f)
            assertTrue(plan.step1Ms >= IdleCloseSequence.MIN_STEP_MS && plan.step2Ms >= IdleCloseSequence.MIN_STEP_MS)
        }
    }

    @Test
    fun theTwoSpringsSettleExactlyWhenTheirStepEnds() {
        for (speed in speeds) {
            val plan = IdleCloseSequence.plan(speed)
            val close = springTimeToProgressMs(plan.closeStiffness, PILL_SPRING_DAMPING, IdleCloseSequence.SETTLED)
            val exit = springTimeToProgressMs(plan.exitStiffness, PILL_SPRING_DAMPING, IdleCloseSequence.SETTLED)
            assertEquals("$speed close", plan.step1Ms.toDouble(), close.toDouble(), 2.0)
            assertEquals("$speed exit", plan.step2Ms.toDouble(), exit.toDouble(), 2.0)
        }
    }

    @Test
    fun noBounceInEitherStep() {
        val plan = IdleCloseSequence.plan(AnimationSpeed.NORMAL)
        for (stiffness in listOf(plan.closeStiffness, plan.exitStiffness)) {
            for (ms in 0..1000 step 10) {
                val p = springProgress(ms / 1000f, stiffness, PILL_SPRING_DAMPING)
                assertTrue("overshoot at $ms ms: $p", p <= 1f + 1e-4f)
            }
        }
    }

    @Test
    fun snappyIsShorterAndRelaxedLongerThanNormal() {
        val relaxed = IdleCloseSequence.plan(AnimationSpeed.RELAXED).totalMs
        val normal = IdleCloseSequence.plan(AnimationSpeed.NORMAL).totalMs
        val snappy = IdleCloseSequence.plan(AnimationSpeed.SNAPPY).totalMs
        assertTrue("snappy $snappy normal $normal relaxed $relaxed", snappy < normal && normal < relaxed)
    }

    @Test
    fun theCardContentIsGoneBeforeStepOneEnds() {
        for (speed in speeds) {
            val plan = IdleCloseSequence.plan(speed)
            assertTrue(plan.contentOutMs in 1 until plan.step1Ms)
        }
    }

    @Test
    fun stiffnessForALengthRoundTripsThroughTheSpring() {
        for (ms in listOf(80, 150, 275, 400, 700)) {
            val k = IdleCloseSequence.criticalStiffnessForSettleMs(ms)
            assertEquals(ms.toDouble(), springTimeToProgressMs(k, 1f, IdleCloseSequence.SETTLED).toDouble(), 2.0)
        }
    }

    @Test
    fun fastOutSlowInTimeIsMonotoneAndHitsTheEnds() {
        assertEquals(0f, IdleCloseSequence.fastOutSlowInTimeFor(0f), 0.001f)
        assertEquals(1f, IdleCloseSequence.fastOutSlowInTimeFor(1f), 0.001f)
        var last = 0f
        for (i in 1..10) {
            val t = IdleCloseSequence.fastOutSlowInTimeFor(i / 10f)
            assertTrue(t >= last)
            last = t
        }
        // The curve starts quickly: 60 % of the travel takes well under 60 % of the time.
        assertTrue(IdleCloseSequence.fastOutSlowInTimeFor(0.6f) < 0.6f)
    }
}
