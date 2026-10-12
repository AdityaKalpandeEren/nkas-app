package com.kalpande.algopulse.ui

import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.MarketData
import com.kalpande.algopulse.data.OpenDto
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.data.SystemDto
import com.kalpande.algopulse.data.parseDate
import com.kalpande.algopulse.domain.Agg
import com.kalpande.algopulse.domain.DayPoint
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Freshness
import com.kalpande.algopulse.domain.PeriodSummary
import com.kalpande.algopulse.domain.cumulative
import com.kalpande.algopulse.domain.mergeDays
import com.kalpande.algopulse.domain.on
import com.kalpande.algopulse.domain.summary
import com.kalpande.algopulse.domain.systemDays
import com.kalpande.algopulse.domain.total
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One system with its totals relative to the market's current session. */
class SystemView(val market: Market, val s: SystemDto, session: LocalDate) {
    val days: List<DayPoint> = systemDays(s)
    val total: Agg = days.total()
    val today: Agg = days.on(session)
    val spark: List<Double> = days.cumulative().takeLast(30).map { it.second }
}

/** Everything a screen needs about one market, for the selected scope. */
class MarketView(val state: MarketState, val scope: Scope, val now: Instant) {
    val market: Market = state.market
    val data: MarketData? = state.data
    val ccy: String = data?.ccy ?: market.currency
    val zone: ZoneId = data?.zone ?: ZoneId.of(market.defaultZone)
    val systems: List<SystemDto> = data?.systems?.filter { scope.includes(it) } ?: emptyList()
    val today: LocalDate = data?.today(now) ?: LocalDate.now(zone)
    val session: LocalDate = data?.sessionDay(now) ?: today
    val isTodaySession: Boolean = session == today
    val freshness: Freshness = Freshness.of(data?.generatedAt, now)
    val generatedAt: Instant? = data?.generatedAt
    val ageText: String = Fmt.relative(generatedAt, now)

    val days: List<DayPoint> by lazy { mergeDays(systems) }
    val summary: PeriodSummary by lazy { days.summary(session) }
    val views: List<SystemView> by lazy { systems.map { SystemView(market, it, session) } }

    /** Intraday systems should hold nothing across sessions: anything open from an earlier date is a leftover. */
    fun isLeftover(s: SystemDto): Boolean {
        if (s.style != "intraday") return false
        val asof = parseDate(s.status.asof) ?: return false
        return asof.isBefore(today)
    }

    val openPositions: List<Pair<SystemDto, OpenDto>> by lazy { systems.flatMap { s -> s.open.map { s to it } } }

    val sessionLabel: String
        get() = if (isTodaySession) "Today · ${Fmt.day(session)}" else "Last session · ${Fmt.day(session)}"
}
