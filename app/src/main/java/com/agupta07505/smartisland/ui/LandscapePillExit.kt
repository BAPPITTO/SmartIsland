/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.model.IDLE_BLINK_MS
import com.agupta07505.smartisland.model.IdleCloseSequence
import com.agupta07505.smartisland.model.IDLE_OPEN_AT_EXIT
import com.agupta07505.smartisland.model.IDLE_PULSE_DOWN_MS
import com.agupta07505.smartisland.model.IDLE_PULSE_SCALE
import com.agupta07505.smartisland.model.IDLE_PULSE_UP_MS
import com.agupta07505.smartisland.model.IDLE_VANISH_MS
import com.agupta07505.smartisland.model.PILL_ENTER_AT_IDLE_VANISH
import com.agupta07505.smartisland.model.PILL_SPRING_DAMPING
import com.agupta07505.smartisland.model.PILL_SPRING_STIFFNESS
import com.agupta07505.smartisland.model.PillExitDirection
import com.agupta07505.smartisland.model.PillPhase
import com.agupta07505.smartisland.model.PillTransition
import com.agupta07505.smartisland.model.pillExitAlpha
import com.agupta07505.smartisland.model.pillExitDirection
import com.agupta07505.smartisland.model.pillExitOffsetDp
import com.agupta07505.smartisland.model.pillExitTravelDp
import com.agupta07505.smartisland.model.pillSlideProgress
import com.agupta07505.smartisland.model.pillTransition
import com.agupta07505.smartisland.util.AnimationSpeed
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * One state machine for the landscape cutout leaving AND coming back (rules: model/PillPresence.kt, maths of the
 * slide: model/LandscapePillExit.kt). Kept out of the overlay's very large function on purpose, like the idle dot.
 *
 * Two numbers drive everything:
 *  - [presence]: 1 = collapsed pill in place, 0 = pill gone. It is driven by a critically damped spring (no bounce)
 *    on an [Animatable], so a card arriving mid-exit (or the card going mid-entry) reverses in place and keeps the
 *    velocity it had: never a snap, never a restart from rest. The animation speed scales the spring's stiffness.
 *  - [idle]: how much of the idle circle is drawn (0..1). The idle circle is the island's separate round dot, so it can
 *    overlap the pill's slide: it opens when the exit is 70 % done and, when a card comes, vanishes first and the pill
 *    slides in once the vanish is 60 % done. Both hand-offs wait on progress, not on a clock. Same order for calls and
 *    popups (no exceptions).
 *
 * While the pill leaves, the island keeps the settings it had with the card (size and position do not glide to
 * the idle size); when it is gone the island takes the idle settings with a jump ([PillPhase.Settling]) and the dot
 * hands over at the same spot.
 */
internal class PillExitState {
    // Plain fields: written after composition, read when composing.
    var hadCollapsedContent = false
    var hadNoContent = false
    var lastSettings: SmartIslandSettings? = null
    var lastCenterXDp = 0f
    var lastTopDp = 0f
    var lastWidthDp = 0f
    var lastHeightDp = 0f

    /** The orientation the record above was made in: a pill seen in the other orientation is no help for this one. */
    var recordedLandscape: Boolean? = null
    var job: Job? = null

    var phase by mutableStateOf(PillPhase.None)
    private var frozen by mutableStateOf<SmartIslandSettings?>(null)
    private var direction = PillExitDirection.Up
    private var travelDp = 0f

    /** 1 = pill in place, 0 = gone. Mirror of [presenceAnim] that the draw code reads. */
    var presence by mutableFloatStateOf(1f)

    /** The spring that owns [presence]; it keeps its velocity when a run is cancelled and a reversal takes over. */
    private val presenceAnim = Animatable(1f)

    /**
     * Stiffness (user speed applied, system scale not) the next exit runs on, or null for the normal pill spring. Set for
     * an exit that step 2 of the idle-circle close starts ([IdleCloseSequence]); it is also what a reversal then uses.
     */
    var exitStiffnessUser: Float? = null

    /** The next run starts from [presence] at rest (a new exit or entry), instead of continuing the running motion. */
    var restartPresence = true

    /** 0 = idle circle not drawn, 1 = fully open. */
    var idle by mutableFloatStateOf(0f)

    /** 0..1 shape of the idle circle's pulse (0 = normal size, 1 = fully enlarged by [IDLE_PULSE_SCALE]). */
    var pulse by mutableFloatStateOf(0f)

    /** Set while composing: the island's size and position jump instead of gliding. */
    var snapGeometry = false

    /** Set while composing: the settings the island keeps rendering with during the exit, else null. */
    var frozenNow: SmartIslandSettings? = null

    /** Set while composing on the very frame a card arrives on the idle circle: draw the pill as already away. */
    var enterStarting = false

    /** Set while composing: the pill is leaving (or about to), so the secondary cutout keeps its merge instead of snapping apart. */
    var leavingNow = false

    /**
     * The separate idle dot exists (landscape, not notch mode), so it can stand in for the island's own idle cutout
     * while the island takes the idle settings (see [PillPhase.Settling]). Portrait has no dot and keeps the old hand-over.
     */
    var dotCoversHandover = false

    /**
     * True once the island shell has really reached the idle settings (size and opacity), read from the overlay.
     * The shell's size and opacity are animated values; right after the exit they still hold the pill's numbers, which showed
     * as a dark pill-sized box at the idle spot that shrank and faded into the circle (the warp).
     */
    var shellSettled: () -> Boolean = { true }

    val hasRecord: Boolean get() = lastSettings != null && lastWidthDp > 0f && lastHeightDp > 0f

    fun prepare(screenWidthDp: Float) {
        direction = pillExitDirection(lastCenterXDp, screenWidthDp)
        travelDp = pillExitTravelDp(direction, lastCenterXDp, lastWidthDp, lastHeightDp, lastTopDp, screenWidthDp)
    }

    fun frozenSettings(): SmartIslandSettings? = frozen

    fun freeze(settings: SmartIslandSettings?) {
        frozen = settings
    }

    /** Hard stop: the island shows as usual. */
    fun reset() {
        job?.cancel()
        frozen = null
        phase = PillPhase.None
        presence = 1f
        idle = 0f
        pulse = 0f
        restartPresence = true
        exitStiffnessUser = null
    }

    private fun effPresence(): Float = if (enterStarting) 0f else if (phase == PillPhase.None || phase == PillPhase.Settling) 1f else presence

    private fun effIdle(): Float = when {
        enterStarting -> 1f
        // The dot keeps standing in for the idle cutout until the shell has settled on the idle settings.
        phase == PillPhase.Settling && dotCoversHandover -> 1f
        phase == PillPhase.Leaving || phase == PillPhase.Entering -> idle.coerceIn(0f, 1f)
        else -> 0f
    }

    fun slideXDp(): Float = pillExitOffsetDp(direction, travelDp, pillSlideProgress(effPresence())).dx

    fun slideYDp(): Float = pillExitOffsetDp(direction, travelDp, pillSlideProgress(effPresence())).dy

    fun pillAlpha(): Float = pillExitAlpha(pillSlideProgress(effPresence()))

    /**
     * Opacity of the island shell itself. Like [pillAlpha], except that while the island takes the idle settings (and the dot
     * covers the spot) the shell is not drawn: its size and opacity are still the pill's for a few frames. The secondary
     * cutouts keep using [pillAlpha].
     */
    fun shellAlpha(): Float = if (phase == PillPhase.Settling && dotCoversHandover) 0f else pillAlpha()

    /**
     * Scale of the idle circle: 1, except for the one pulse every exit ends with (it blinks in, then enlarges and
     * shrinks back to normal size, tweens only). There is no plain blink any more.
     */
    fun dotScale(): Float = 1f + IDLE_PULSE_SCALE * pulse.coerceIn(0f, 1f)

    /** Opacity of the idle circle: driven by the machine while it runs, else by [base] (its normal fade). */
    fun dotAlpha(base: Float): Float = if (dotDriven(base)) effIdle() else base

    // A dot that stays on screen next to the pill anyway (base fully shown) is left alone while a pill comes in.
    private fun dotDriven(base: Float): Boolean = when {
        phase == PillPhase.Leaving -> true
        phase == PillPhase.Settling -> dotCoversHandover
        phase == PillPhase.Entering || enterStarting -> base < 0.99f
        else -> false
    }

    fun start(block: suspend () -> Unit, scope: kotlinx.coroutines.CoroutineScope) {
        job?.cancel()
        job = scope.launch { block() }
    }

    private fun presenceSpec(speed: AnimationSpeed) =
        spring<Float>(
            dampingRatio = PILL_SPRING_DAMPING,
            stiffness = exitStiffnessUser?.let { speed.systemStiffness(it) } ?: speed.stiffness(PILL_SPRING_STIFFNESS)
        )

    /** A new run starts at rest from [presence]; a reversal keeps the value and the velocity the spring has. */
    private suspend fun beginPresenceRun() {
        if (restartPresence) {
            presenceAnim.snapTo(presence)
            restartPresence = false
        }
    }

    /** Pill slides out and fades; the idle circle opens when the exit is 70 % done. Continues from the current motion. */
    suspend fun runExit(speed: AnimationSpeed) {
        beginPresenceRun()
        coroutineScope {
            launch {
                // Progress-based: if a reversed entry already is past 70 % this passes at once.
                snapshotFlow { pillSlideProgress(presence) }.first { it >= IDLE_OPEN_AT_EXIT }
                // A blink: a short fade at full size (see [dotScale]), no spring and no overshoot. With animations off it is at once.
                val blinkMs = if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) 1 else speed.durationMs(IDLE_BLINK_MS).coerceAtLeast(1)
                animateTo(idle, 1f, blinkMs, LinearOutSlowInEasing) { idle = it }
                // Every exit pulses once: the circle enlarges and shrinks back to normal size.
                if (!com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
                    animateTo(0f, 1f, speed.durationMs(IDLE_PULSE_UP_MS).coerceAtLeast(1), FastOutSlowInEasing) { pulse = it }
                    animateTo(1f, 0f, speed.durationMs(IDLE_PULSE_DOWN_MS).coerceAtLeast(1), FastOutSlowInEasing) { pulse = it }
                }
                pulse = 0f
            }
            presenceAnim.animateTo(0f, presenceSpec(speed)) { presence = value }
        }
        // Gone: the island takes the idle settings with a jump and the dot hands over at the same spot.
        presenceAnim.snapTo(1f)
        frozen = null
        presence = 1f
        idle = 0f
        pulse = 0f
        phase = PillPhase.Settling
        withFrameNanos { }
        withFrameNanos { }
        // Hold the hand-over (shell hidden, dot drawn) until the shell is really at the idle size and opacity, else
        // the pill-sized shell shows at the idle spot and shrinks and fades into the circle. Capped, so it can never hang.
        if (dotCoversHandover && !com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) {
            var startNanos = -1L
            while (true) {
                val now = withFrameNanos { it }
                if (startNanos < 0L) startNanos = now
                if (shellSettled() || (now - startNanos) / 1_000_000L >= SETTLE_CAP_MS) break
            }
        }
        phase = PillPhase.None
        exitStiffnessUser = null
    }

    /** Idle circle vanishes first; the pill springs back in once the vanish is 60 % done. Continues from the current motion. */
    suspend fun runEnter(speed: AnimationSpeed) {
        beginPresenceRun()
        // A card came back while the circle was pulsing: the pulse is over, the circle vanishes from its normal size.
        pulse = 0f
        val vanishFull = speed.durationMs(IDLE_VANISH_MS)
        val idleStart = idle.coerceIn(0f, 1f)
        // Animations off (system scale 0): no minimum length, the idle circle is gone at once.
        val vanishMs = if (com.agupta07505.smartisland.util.SystemAnimScale.animationsOff()) 1 else (vanishFull * idleStart).toInt().coerceAtLeast(40)
        coroutineScope {
            launch { animateTo(idle, 0f, vanishMs, FastOutSlowInEasing) { idle = it } }
            // Nothing of the idle circle is drawn yet (reversing an exit that has not opened it): come back right away.
            if (idleStart > 0.01f) {
                snapshotFlow { idle }.first { it <= idleStart * (1f - PILL_ENTER_AT_IDLE_VANISH) }
            }
            presenceAnim.animateTo(1f, presenceSpec(speed)) { presence = value }
        }
        presenceAnim.snapTo(1f)
        presence = 1f
        idle = 0f
        phase = PillPhase.None
        exitStiffnessUser = null
    }

    private suspend fun animateTo(from: Float, to: Float, ms: Int, easing: Easing, set: (Float) -> Unit) {
        animate(initialValue = from, targetValue = to, animationSpec = tween(ms, easing = easing)) { v, _ -> set(v) }
    }
}

/** Longest the hand-over after an exit waits for the shell to reach the idle settings (ms). */
private const val SETTLE_CAP_MS = 700L

/**
 * Creates the state machine and runs it. Call before the island's layout so the snap flag and the frozen settings are
 * known when its geometry targets change. Landscape, not notch mode only; an expanding card during an exit reverses it.
 */
@Composable
internal fun rememberLandscapePillExit(
    hasContent: Boolean,
    expanded: Boolean,
    isLandscape: Boolean,
    /** Step 2 of the idle-circle close: the pill leaves on the sequence's own exit spring. */
    idleLeaving: Boolean,
    notchMode: Boolean,
    screenWidthDp: Float,
    animSpeed: AnimationSpeed,
    /**
     * The collapsed pill is really on screen (not auto-hidden). A pill that was hidden must not "exit": the slide
     * draws it at full opacity at its old spot and carries it off the edge, which showed as a stray dark pill smearing away.
     */
    pillOnScreen: Boolean = true
): PillExitState {
    val state = remember { PillExitState() }
    val scope = rememberCoroutineScope()
    // Landscape only (portrait closes with its normal fade, there is no pill slide), and until a run that began there is over.
    val eligible = !notchMode && (isLandscape || state.phase != PillPhase.None)
    val exitStiffness = if (idleLeaving) IdleCloseSequence.plan(animSpeed).exitStiffness else null
    val transition = pillTransition(
        phase = state.phase,
        eligible = eligible,
        expanded = expanded,
        hasContent = hasContent,
        hadPill = state.hadCollapsedContent,
        hadNoContent = state.hadNoContent,
        hasRecord = state.hasRecord && state.recordedLandscape == isLandscape
    )
    val startingExit = transition == PillTransition.StartExit
    val startingEnter = transition == PillTransition.StartEnter
    if (startingExit || startingEnter) state.prepare(screenWidthDp)
    state.enterStarting = startingEnter
    state.leavingNow = startingExit || state.phase == PillPhase.Leaving
    state.frozenNow = if (startingExit) state.lastSettings else state.frozenSettings()
    state.snapGeometry = startingExit || startingEnter || state.phase != PillPhase.None
    SideEffect {
        when (transition) {
            PillTransition.StartExit -> {
                state.pulse = 0f
                state.exitStiffnessUser = exitStiffness
                state.freeze(state.lastSettings)
                state.presence = 1f
                state.restartPresence = true
                state.idle = 0f
                state.phase = PillPhase.Leaving
                state.start({ state.runExit(animSpeed) }, scope)
            }
            PillTransition.StartEnter -> {
                state.freeze(null)
                state.presence = 0f
                state.restartPresence = true
                state.idle = 1f
                state.phase = PillPhase.Entering
                state.start({ state.runEnter(animSpeed) }, scope)
            }
            PillTransition.ReverseToPill -> {
                // A card (or expanding) arrived mid-exit: turn around from the current spot.
                state.freeze(null)
                state.phase = PillPhase.Entering
                state.start({ state.runEnter(animSpeed) }, scope)
            }
            PillTransition.ReverseToIdle -> {
                // The card went mid-entry: leave again from the current spot.
                state.freeze(state.lastSettings)
                state.phase = PillPhase.Leaving
                state.start({ state.runExit(animSpeed) }, scope)
            }
            PillTransition.Cancel -> state.reset()
            PillTransition.None -> Unit
        }
        // Not tied to [eligible]: the composition before a portrait tap is not eligible yet, but the pill was there.
        state.hadCollapsedContent = !notchMode && !expanded && hasContent && pillOnScreen
        state.hadNoContent = !notchMode && !expanded && !hasContent
    }
    return state
}

/** Remembers the collapsed cutout's settings and spot while it shows a card, so the machine knows where it starts. */
internal fun PillExitState.record(
    shown: Boolean,
    isLandscape: Boolean,
    settings: SmartIslandSettings,
    centerXDp: Float,
    topDp: Float,
    widthDp: Float,
    heightDp: Float
) {
    if (shown && widthDp > 0f && heightDp > 0f) {
        lastSettings = settings
        recordedLandscape = isLandscape
        lastCenterXDp = centerXDp
        lastTopDp = topDp
        lastWidthDp = widthDp
        lastHeightDp = heightDp
    }
}
