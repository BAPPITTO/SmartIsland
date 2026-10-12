/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

/**
 * Idle widgets: small live readouts the collapsed cutout shows while no event is on the island (the time, the weather, ...).
 * One widget is shown at a time; with several switched on they take turns every few seconds.
 *
 * Pure logic only (ids, the stored list, the rotation), so it can be unit tested. Drawing is in ui/IdleWidgets.kt, the data in
 * util/IdleWidgetData.kt.
 */
enum class IdleWidgetKind(val id: String) {
    Clock("clock"),
    Date("date"),
    Weather("weather"),
    Battery("battery"),
    NextAlarm("alarm"),
    NetSpeed("speed"),
    /** Sunrise, afternoon, sunset and midnight glyphs that tick in on the pill at fixed local times (ui/DayMoment.kt). Never takes a turn on the pill. */
    DayMoments("daymoments"),
    /** Count of unread messages and mail on the left of the cutout; the island lists the apps (ui/IdleWidgets.kt). Sits out while there are none. */
    Notifications("notifications"),
    /** The active Do Not Disturb / mode glyph (bedtime, driving, ...) on the left of the cutout; the island names it. Sits out while no mode is on. */
    Modes("modes");

    companion object {
        fun fromId(id: String): IdleWidgetKind? = values().firstOrNull { it.id == id }
    }
}

object IdleWidgets {
    /** Temperature units: follow the device region, or force Celsius / Fahrenheit. */
    const val UNIT_AUTO = "AUTO"
    const val UNIT_CELSIUS = "C"
    const val UNIT_FAHRENHEIT = "F"
    val VALID_UNITS = setOf(UNIT_AUTO, UNIT_CELSIUS, UNIT_FAHRENHEIT)

    const val DEFAULT_IDS = "clock"
    const val MIN_CYCLE_SECONDS = 3
    const val MAX_CYCLE_SECONDS = 60
    const val DEFAULT_CYCLE_SECONDS = 6

    /** The stored comma list as widget kinds: unknown ids and repeats are dropped, the order is kept. */
    fun parse(stored: String): List<IdleWidgetKind> =
        stored.split(',').map { it.trim() }.mapNotNull { IdleWidgetKind.fromId(it) }.distinct()

    fun serialize(kinds: List<IdleWidgetKind>): String = kinds.distinct().joinToString(",") { it.id }

    /** Switches [kind] on (added at the end) or off. The last widget cannot be switched off here: the master switch does that. */
    fun toggle(stored: String, kind: IdleWidgetKind): String {
        val current = parse(stored)
        val next = if (kind in current) {
            if (current.size == 1) current else current - kind
        } else {
            current + kind
        }
        return serialize(next)
    }

    /** A stored list that has to hold at least one widget: an empty or unreadable one becomes [DEFAULT_IDS]. */
    fun normalize(stored: String): String = parse(stored).let { if (it.isEmpty()) DEFAULT_IDS else serialize(it) }

    /** The widget shown at rotation step [step] (any integer; it wraps around). Null when the list is empty. */
    fun pick(kinds: List<IdleWidgetKind>, step: Int): IdleWidgetKind? =
        if (kinds.isEmpty()) null else kinds[Math.floorMod(step, kinds.size)]

    /** Fahrenheit when forced, or when the unit is automatic and the region uses it (US and a few others). */
    fun useFahrenheit(unit: String, regionCountry: String): Boolean = when (unit) {
        UNIT_FAHRENHEIT -> true
        UNIT_CELSIUS -> false
        else -> regionCountry.uppercase() in FAHRENHEIT_REGIONS
    }

    private val FAHRENHEIT_REGIONS = setOf("US", "BS", "BZ", "KY", "PW", "LR", "FM", "MH")

    fun displayTemp(celsius: Float, fahrenheit: Boolean): String {
        val value = if (fahrenheit) celsius * 9f / 5f + 32f else celsius
        return "${Math.round(value)}\u00B0"
    }

    /** Short speed text for the pill ("320K", "4.2M"): bytes per second in, a few characters out. */
    fun shortSpeed(bytesPerSecond: Long): String {
        val b = bytesPerSecond.coerceAtLeast(0L).toDouble()
        return when {
            b < 1_000.0 -> "0K"
            b < 1_000_000.0 -> "${Math.round(b / 1_000.0)}K"
            b < 10_000_000.0 -> String.format(java.util.Locale.US, "%.1fM", b / 1_000_000.0)
            else -> "${Math.round(b / 1_000_000.0)}M"
        }
    }
}

/** The result of the weather lookup: temperature in Celsius and the WMO weather code Open-Meteo reports. */
data class WeatherSnapshot(
    val temperatureC: Float,
    val weatherCode: Int,
    val isDay: Boolean,
    val fetchedAtMs: Long
)

/** What a weather code looks like, grouped to the few icons the pill can show. */
enum class WeatherLook { Clear, PartlyCloudy, Cloudy, Fog, Rain, Snow, Storm }

fun weatherLook(code: Int): WeatherLook = when (code) {
    0 -> WeatherLook.Clear
    1, 2 -> WeatherLook.PartlyCloudy
    3 -> WeatherLook.Cloudy
    45, 48 -> WeatherLook.Fog
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherLook.Rain
    71, 73, 75, 77, 85, 86 -> WeatherLook.Snow
    95, 96, 99 -> WeatherLook.Storm
    else -> WeatherLook.Cloudy
}
