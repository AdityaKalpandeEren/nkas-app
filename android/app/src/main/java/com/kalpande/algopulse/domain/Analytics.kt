package com.kalpande.algopulse.domain

import com.kalpande.algopulse.data.DayDto
import com.kalpande.algopulse.data.SystemDto
import com.kalpande.algopulse.data.parseDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Additive performance totals - a day, a week, a system's history or a whole market. */
data class Agg(
    val trades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val pnl: Double = 0.0,
    val grossWin: Double = 0.0,
    val grossLoss: Double = 0.0,
    val rSum: Double = 0.0,
    val rWin: Double = 0.0,
    val rLoss: Double = 0.0,
    val r2: Double = 0.0,
    val rN: Int = 0,
    val best: Double? = null,
    val worst: Double? = null,
    val fees: Double = 0.0,
    val days: Int = 0,
    val greenDays: Int = 0,
    val redDays: Int = 0,
) {
    operator fun plus(o: Agg) = Agg(
        trades = trades + o.trades, wins = wins + o.wins, losses = losses + o.losses, pnl = pnl + o.pnl,
        grossWin = grossWin + o.grossWin, grossLoss = grossLoss + o.grossLoss, rSum = rSum + o.rSum,
        rWin = rWin + o.rWin, rLoss = rLoss + o.rLoss, r2 = r2 + o.r2, rN = rN + o.rN,
        best = maxOrNull(best, o.best), worst = minOrNull(worst, o.worst), fees = fees + o.fees,
        days = days + o.days, greenDays = greenDays + o.greenDays, redDays = redDays + o.redDays,
    )

    val decided: Int get() = wins + losses
    val winRate: Double? get() = if (decided > 0) wins.toDouble() / decided else null
    val profitFactor: Double?
        get() = when {
            grossLoss < 0 -> grossWin / -grossLoss
            grossWin > 0 -> Double.POSITIVE_INFINITY
            else -> null
        }
    val avgWin: Double? get() = if (wins > 0) grossWin / wins else null
    val avgLoss: Double? get() = if (losses > 0) grossLoss / losses else null
    val payoff: Double? get() = avgWin?.let { w -> avgLoss?.takeIf { it < 0 }?.let { w / -it } }
    val expectancy: Double? get() = if (trades > 0) pnl / trades else null
    val expectancyR: Double? get() = if (rN > 0) rSum / rN else null

    /** t-statistic of the mean R per trade (|t| < 2 = not distinguishable from noise). */
    val tStatR: Double?
        get() {
            if (rN < 3) return null
            val mean = rSum / rN
            val variance = (r2 - rN * mean * mean) / (rN - 1)
            if (variance <= 1e-12) return null
            return mean / sqrt(variance / rN)
        }

    val isEmpty: Boolean get() = trades == 0 && pnl == 0.0

    companion object {
        private fun maxOrNull(a: Double?, b: Double?) = if (a == null) b else if (b == null) a else max(a, b)
        private fun minOrNull(a: Double?, b: Double?) = if (a == null) b else if (b == null) a else min(a, b)
    }
}

/**
 * One day of a system as an [Agg]. Swing portfolios mark equity daily: they contribute their
 * P&L but not "trades" (their day count is rebalance fills, not round trips).
 */
fun DayDto.toAgg(portfolio: Boolean): Agg {
    val dayFlag = if (n > 0 || pnl != 0.0) 1 else 0
    if (portfolio) {
        return Agg(
            pnl = pnl, grossWin = max(pnl, 0.0), grossLoss = min(pnl, 0.0), days = dayFlag,
            greenDays = if (pnl > 0) 1 else 0, redDays = if (pnl < 0) 1 else 0,
        )
    }
    return Agg(
        trades = n, wins = w, losses = l, pnl = pnl, grossWin = gw, grossLoss = gl,
        rSum = r ?: 0.0, rWin = rw ?: 0.0, rLoss = rl ?: 0.0, r2 = r2 ?: 0.0, rN = rn ?: 0,
        best = best, worst = worst, fees = fees ?: 0.0, days = dayFlag,
        greenDays = if (pnl > 0) 1 else 0, redDays = if (pnl < 0) 1 else 0,
    )
}

data class DayPoint(val date: LocalDate, val agg: Agg)

fun systemDays(s: SystemDto): List<DayPoint> =
    s.days.mapNotNull { d -> parseDate(d.d)?.let { DayPoint(it, d.toAgg(s.isPortfolio)) } }.sortedBy { it.date }

/** Days of several systems summed per date (market or scope totals). */
fun mergeDays(systems: List<SystemDto>): List<DayPoint> {
    val acc = sortedMapOf<LocalDate, Agg>()
    for (s in systems) for (p in systemDays(s)) {
        val cur = acc[p.date]
        // per-day "green/red/days" must be recounted after summing, not summed per system
        acc[p.date] = if (cur == null) p.agg else cur + p.agg
    }
    return acc.map { (d, a) ->
        DayPoint(d, a.copy(days = if (a.trades > 0 || a.pnl != 0.0) 1 else 0,
            greenDays = if (a.pnl > 0) 1 else 0, redDays = if (a.pnl < 0) 1 else 0))
    }
}

fun List<DayPoint>.total(): Agg = fold(Agg()) { acc, p -> acc + p.agg }

fun List<DayPoint>.between(from: LocalDate?, to: LocalDate?): List<DayPoint> =
    filter { (from == null || !it.date.isBefore(from)) && (to == null || !it.date.isAfter(to)) }

fun List<DayPoint>.on(date: LocalDate): Agg = firstOrNull { it.date == date }?.agg ?: Agg()

/** Running total of P&L, one point per day. */
fun List<DayPoint>.cumulative(): List<Pair<LocalDate, Double>> {
    var c = 0.0
    return map { c += it.agg.pnl; it.date to c }
}

/** Largest peak-to-trough fall of the cumulative P&L (a negative number, 0 if none). */
fun List<DayPoint>.maxDrawdown(): Double {
    var peak = 0.0
    var cum = 0.0
    var dd = 0.0
    for (p in this) {
        cum += p.agg.pnl
        peak = max(peak, cum)
        dd = min(dd, cum - peak)
    }
    return dd
}

/** Annualised Sharpe of daily P&L (scale-free, so it works on money P&L). */
fun List<DayPoint>.sharpe(): Double? {
    val xs = map { it.agg.pnl }
    if (xs.size < 10) return null
    val mean = xs.average()
    val sd = sqrt(xs.sumOf { (it - mean) * (it - mean) } / (xs.size - 1))
    return if (sd > 1e-9) mean / sd * sqrt(252.0) else null
}

fun List<DayPoint>.bestDay(): DayPoint? = filter { it.agg.pnl != 0.0 }.maxByOrNull { it.agg.pnl }
fun List<DayPoint>.worstDay(): DayPoint? = filter { it.agg.pnl != 0.0 }.minByOrNull { it.agg.pnl }

// ─── periods ──────────────────────────────────────────────────────────────

enum class Period(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

data class Bucket(val start: LocalDate, val end: LocalDate, val label: String, val shortLabel: String, val agg: Agg)

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val dayShort = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

fun periodStart(date: LocalDate, period: Period): LocalDate = when (period) {
    Period.DAY -> date
    Period.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Period.MONTH -> date.withDayOfMonth(1)
    Period.YEAR -> date.withDayOfYear(1)
}

fun periodEnd(start: LocalDate, period: Period): LocalDate = when (period) {
    Period.DAY -> start
    Period.WEEK -> start.plusDays(6)
    Period.MONTH -> start.plusMonths(1).minusDays(1)
    Period.YEAR -> start.plusYears(1).minusDays(1)
}

/** Day / week / month / year totals, oldest first. */
fun List<DayPoint>.bucketize(period: Period): List<Bucket> =
    groupBy { periodStart(it.date, period) }.toSortedMap().map { (start, pts) ->
        val end = periodEnd(start, period)
        val (label, short) = when (period) {
            Period.DAY -> start.format(dayFmt) to start.dayOfMonth.toString()
            Period.WEEK -> "Week of ${start.format(dayShort)}" to start.format(dayShort)
            Period.MONTH -> start.format(monthFmt) to start.format(monthShort)
            Period.YEAR -> start.year.toString() to start.year.toString()
        }
        Bucket(start, end, label, short, pts.total())
    }

/** Chart ranges for cumulative P&L. */
enum class Range(val label: String) {
    W1("1W"), M1("1M"), M3("3M"), YTD("YTD"), ALL("All");

    fun start(today: LocalDate): LocalDate? = when (this) {
        W1 -> today.minusDays(7)
        M1 -> today.minusMonths(1)
        M3 -> today.minusMonths(3)
        YTD -> today.withDayOfYear(1)
        ALL -> null
    }
}

/** This week / month / year to date + all time, relative to [day]. */
data class PeriodSummary(val today: Agg, val week: Agg, val month: Agg, val year: Agg, val all: Agg)

fun List<DayPoint>.summary(day: LocalDate) = PeriodSummary(
    today = on(day),
    week = between(periodStart(day, Period.WEEK), day).total(),
    month = between(periodStart(day, Period.MONTH), day).total(),
    year = between(periodStart(day, Period.YEAR), day).total(),
    all = total(),
)
