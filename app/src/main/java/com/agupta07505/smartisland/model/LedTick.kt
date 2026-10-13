/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * The LED footsteps tick of the idle widget slots (Settings > Idle widgets > Tick style).
 *
 * A slot's content never slides smoothly: it is drawn as a grid of LEDs and moves in whole LED rows. A change is a burst of
 * 1 to 9 short hops with a rest between them, like footsteps. The burst length comes from a repeating pattern the user types
 * ("1, 2, 3, 2, 5"): the first change takes one hop, the next two, then three, two, five, and round again. On every hop the old
 * content steps up (or down) and out while the new content, or the same content when only the beat is due, steps in behind it.
 *
 * Pure rules, so they can be unit tested; the drawing is ui/LedTickSlot.kt.
 */
object LedTick {
    /** Smooth is the original heavy tick; LED is the footsteps tick drawn on an LED grid. */
    const val STYLE_SMOOTH = "smooth"
    const val STYLE_LED = "led"
    val VALID_STYLES = setOf(STYLE_SMOOTH, STYLE_LED)

    fun normalizeStyle(id: String?): String = id?.takeIf { it in VALID_STYLES } ?: STYLE_SMOOTH

    /** Ticks per change, repeated in turn. */
    const val DEFAULT_PATTERN = "1, 2, 3, 2, 5"
    const val MIN_HOPS = 1
    const val MAX_HOPS = 9
    const val MAX_PATTERN_LENGTH = 12
    private val DEFAULT_PATTERN_LIST = listOf(1, 2, 3, 2, 5)

    /** The hop counts typed in [text]: every number from 1 to 9, in order, at most [MAX_PATTERN_LENGTH]; the default when none is left. */
    fun parsePattern(text: String?): List<Int> {
        val found = (text ?: "").split(Regex("[^0-9]+"))
            .mapNotNull { it.toIntOrNull() }
            .filter { it in MIN_HOPS..MAX_HOPS }
            .take(MAX_PATTERN_LENGTH)
        return found.ifEmpty { DEFAULT_PATTERN_LIST }
    }

    /** [text] written back the way the settings store it ("1, 2, 3"). */
    fun normalizePattern(text: String?): String = parsePattern(text).joinToString(", ")

    /** Milliseconds from the start of one hop to the start of the next ("speed"), kept within range and to the 20 ms the slider moves in. */
    const val MIN_PACE_MS = 160
    const val MAX_PACE_MS = 800
    const val DEFAULT_PACE_MS = 420
    const val PACE_STEP_MS = 20

    fun normalizePace(ms: Int): Int {
        val clamped = ms.coerceIn(MIN_PACE_MS, MAX_PACE_MS)
        return MIN_PACE_MS + Math.round((clamped - MIN_PACE_MS).toFloat() / PACE_STEP_MS) * PACE_STEP_MS
    }

    /** The LED rows of a slot's board: 7 rows of letters with one spare row above and below. A full travel is this many rows. */
    const val ROWS = 9

    /** How long one hop itself takes (the rest of the pace is the pause). */
    const val HOP_MS = 80

    /** The hop count of the [beat]-th change: the pattern repeats. */
    fun hopsAt(pattern: List<Int>, beat: Long): Int {
        if (pattern.isEmpty()) return DEFAULT_PATTERN_LIST[Math.floorMod(beat, DEFAULT_PATTERN_LIST.size.toLong()).toInt()]
        return pattern[Math.floorMod(beat, pattern.size.toLong()).toInt()]
    }

    /** Length of a burst of [hops] hops at [paceMs]. */
    fun burstMs(hops: Int, paceMs: Int): Int = (hops.coerceAtLeast(1) - 1) * paceMs + HOP_MS

    /**
     * How far the burst has travelled, 0..1, [tMs] after it started: hop i starts at i * pace and covers 1/hops of the travel with an
     * ease-out, then rests until the next hop.
     */
    fun progress(tMs: Float, hops: Int, paceMs: Int): Float {
        val n = hops.coerceAtLeast(1)
        for (i in 0 until n) {
            val from = i.toFloat() / n
            val to = (i + 1).toFloat() / n
            val start = i * paceMs.toFloat()
            if (tMs >= start + HOP_MS) continue
            if (tMs < start) return from
            val u = (tMs - start) / HOP_MS
            return from + (to - from) * (1f - (1f - u) * (1f - u))
        }
        return 1f
    }

    /** The whole LED rows the old content has moved at [progress]. */
    fun rowOffset(progress: Float, rows: Int = ROWS): Int = Math.round(progress.coerceIn(0f, 1f) * rows)

    /**
     * Which beat [nowMs] belongs to, so every slot that starts a burst around the same time takes the same pattern entry and the
     * footsteps of the left and right slot agree.
     */
    fun beatIndex(nowMs: Long, periodMs: Long): Long {
        val p = periodMs.coerceAtLeast(1L)
        return Math.floorDiv(nowMs + p / 2, p)
    }

    /** LED colours: red like a station board, amber, green, or white like the rest of the pill. */
    const val COLOR_RED = "red"
    const val COLOR_AMBER = "amber"
    const val COLOR_GREEN = "green"
    const val COLOR_WHITE = "white"
    val VALID_COLORS = listOf(COLOR_RED, COLOR_AMBER, COLOR_GREEN, COLOR_WHITE)

    fun normalizeColor(id: String?): String = id?.takeIf { it in VALID_COLORS } ?: COLOR_RED

    /**
     * Footsteps: with the stagger on, the right slot (usually the value) starts its burst half a pace after the left one (usually the
     * glyph), so a change reads left, right, left, right. Off = both start together.
     */
    const val STAGGER_SHARE = 0.5f

    /** How long the slot waits before its burst: half a pace for the right slot when [stagger] is on, else none. */
    fun staggerMs(paceMs: Int, right: Boolean, stagger: Boolean): Long =
        if (stagger && right) Math.round(paceMs * STAGGER_SHARE).toLong() else 0L

    /** The font of the LED text: empty = the built-in 5x7 dot font, else any island font id, which is drawn as dots. */
    const val FONT_BUILTIN = ""
}

/** What the drawing needs, read from the settings once. */
data class LedTickConfig(
    val pattern: List<Int> = LedTick.parsePattern(LedTick.DEFAULT_PATTERN),
    val paceMs: Int = LedTick.DEFAULT_PACE_MS,
    /** Empty = the built-in dot font, else an island font id (system, serif, a built-in or a custom font) drawn as dots. */
    val fontId: String = LedTick.FONT_BUILTIN,
    val colorId: String = LedTick.COLOR_RED,
    /** The right slot steps half a pace after the left one (see [LedTick.staggerMs]). */
    val stagger: Boolean = true
)
