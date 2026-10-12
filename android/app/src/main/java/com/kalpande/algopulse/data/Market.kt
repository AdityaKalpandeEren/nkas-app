package com.kalpande.algopulse.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Market(
    val key: String,
    val label: String,
    val flag: String,
    val repo: String,
    val defaultZone: String,
    val currency: String,
) {
    US("us", "US", "🇺🇸", "AdityaKalpandeEren/kalpandeRepoEmaUs", "America/New_York", "USD"),
    NSE("nse", "NSE", "🇮🇳", "AdityaKalpandeEren/kalpandeRepoEmaNse", "Asia/Kolkata", "INR");

    val defaultUrl: String
        get() = "https://github.com/$repo/releases/download/dashboard-data/dashboard.json.gz"

    val title: String get() = if (this == US) "US market" else "NSE India"
}

/** System grouping used by the scope filter. */
enum class Scope(val label: String, val group: String?) {
    ALL("All", null),
    MODELS("Models", "Models"),
    ML("ML filters", "ML filters"),
    RESEARCH("Research", "Research");

    fun includes(s: SystemDto): Boolean = group == null || s.group == group
}

/** A parsed dashboard plus the derived values every screen needs. */
class MarketData(val market: Market, val dto: DashboardDto) {
    val zone: ZoneId = runCatching { ZoneId.of(dto.tz) }.getOrElse { ZoneId.of(market.defaultZone) }
    val ccy: String = dto.ccy.ifBlank { market.currency }
    val generatedAt: Instant? = parseInstant(dto.generatedAt)
    val systems: List<SystemDto> = dto.systems

    private val openTime: LocalTime = runCatching { LocalTime.parse(dto.session.open) }.getOrElse { LocalTime.of(9, 30) }
    private val closeTime: LocalTime = runCatching { LocalTime.parse(dto.session.close) }.getOrElse { LocalTime.of(16, 0) }

    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(zone).toLocalDate()

    /** The latest date any system traded or marked its equity. */
    val lastDataDay: LocalDate? = systems.flatMap { s -> s.days.map { it.d } }.maxOrNull()?.let(::parseDate)

    /**
     * The session the app calls "today": today in the market's time zone when trading has
     * started (or data exists for it), otherwise the last session with data.
     */
    fun sessionDay(now: Instant = Instant.now()): LocalDate {
        val t = today(now)
        val hasToday = lastDataDay == t
        val zoned = now.atZone(zone)
        val weekday = zoned.dayOfWeek.value <= 5
        return if (hasToday || (weekday && zoned.toLocalTime() >= openTime)) t else lastDataDay ?: t
    }

    fun isMarketOpen(now: Instant = Instant.now()): Boolean {
        val z: ZonedDateTime = now.atZone(zone)
        return z.dayOfWeek.value <= 5 && z.toLocalTime() >= openTime && z.toLocalTime() < closeTime
    }

    fun system(id: String): SystemDto? = systems.firstOrNull { it.id == id }
}

fun parseDate(s: String?): LocalDate? = s?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

fun parseInstant(s: String?): Instant? {
    if (s.isNullOrBlank()) return null
    return runCatching { Instant.parse(s) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(s).toInstant() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(s.replace(' ', 'T')).toInstant() }.getOrNull()
}

/** "2026-10-09T09:35:00-04:00" -> local time in [zone]. */
fun parseZoned(s: String?, zone: ZoneId): ZonedDateTime? = parseInstant(s)?.atZone(zone)
