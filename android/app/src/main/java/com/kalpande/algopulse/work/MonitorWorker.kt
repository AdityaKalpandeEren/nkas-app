package com.kalpande.algopulse.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kalpande.algopulse.data.DashboardRepository
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.MarketData
import com.kalpande.algopulse.data.Settings
import com.kalpande.algopulse.data.SettingsRepository
import com.kalpande.algopulse.data.parseInstant
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.mergeDays
import com.kalpande.algopulse.domain.on
import com.kalpande.algopulse.domain.systemDays
import com.kalpande.algopulse.widget.PnlWidget
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

/**
 * Background health check:
 *  - outage: the market's dashboard hasn't been republished for [Settings.staleAlertMin]
 *    (probed cheaply via the GitHub release API; one alert per outage + a "back" alert)
 *  - new failed workflow runs (from the dashboard's health section)
 *  - an end-of-day P&L summary per market
 * Never alerts on the phone's own connectivity: no answer = no verdict.
 */
class MonitorWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val marks = SettingsRepository(ctx)
        val s = marks.current()
        if (!s.monitorEnabled && tags.none { it.contains("now") }) return Result.success()
        val repo = DashboardRepository(ctx)
        val now = Instant.now()
        for (m in Market.entries) runCatching { check(ctx, m, s, repo, marks, now) }
        runCatching { PnlWidget.refreshAll(ctx) }
        return Result.success()
    }

    private suspend fun check(ctx: Context, m: Market, s: Settings, repo: DashboardRepository, marks: SettingsRepository, now: Instant) {
        val url = s.url(m)
        val lastFull = marks.getMark("full_${m.key}")?.toLongOrNull() ?: 0L
        val wantFull = (s.failureAlerts || s.dailySummary) && now.epochSecond - lastFull >= 50 * 60
        var published: Instant? = repo.assetUpdatedAt(url)
        var data: MarketData? = null
        if (published == null || wantFull) {
            data = runCatching { repo.fetch(m, url).data }.getOrNull()
            if (data != null) {
                marks.setMark("full_${m.key}", now.epochSecond.toString())
                if (published == null) published = data.generatedAt
            }
        }
        if (published == null) return

        // 1) outage / recovery
        val age = Duration.between(published, now).toMinutes()
        val staleKey = "stale_${m.key}"
        val prev = marks.getMark(staleKey)
        if (age >= s.staleAlertMin) {
            if (prev != published.toString()) {
                Notifier.notify(
                    ctx, 100 + m.ordinal, Notifier.CH_HEALTH,
                    "${m.flag} ${m.label} bot stopped publishing",
                    "No new data for ${Fmt.duration(age)} (last ${Fmt.clock(published)}). " +
                        "Check the cron-job.org trigger and GitHub Actions.",
                    "health",
                )
                marks.setMark(staleKey, published.toString())
            }
        } else if (prev != null && prev != "ok") {
            Notifier.notify(
                ctx, 100 + m.ordinal, Notifier.CH_HEALTH, "${m.flag} ${m.label} bot is back",
                "Publishing again - last update ${Fmt.relative(published, now)}.", "health",
            )
            marks.setMark(staleKey, "ok")
        }

        val d = data ?: return
        // 2) new failed runs (first sighting only records, so old failures never alert)
        if (s.failureAlerts) {
            val seen = marks.getMark("fail_${m.key}")?.toLongOrNull()
            val fails = d.dto.health.failures.filter { it.id > (seen ?: 0L) }
            if (fails.isNotEmpty()) {
                if (seen != null) {
                    Notifier.notify(
                        ctx, 110 + m.ordinal, Notifier.CH_HEALTH, "${m.flag} ${m.label}: ${fails.size} failed run(s)",
                        fails.joinToString("\n") { "${Fmt.clock(parseInstant(it.at))} · ${it.result ?: "failed"}" }, "health",
                    )
                }
                marks.setMark("fail_${m.key}", fails.maxOf { it.id }.toString())
            } else if (seen == null) {
                marks.setMark("fail_${m.key}", "0")
            }
        }

        // 3) end-of-day summary (after the research square-off: 20:15 ET / 16:00 IST)
        if (s.dailySummary) {
            val z = now.atZone(d.zone)
            val after = if (m == Market.US) LocalTime.of(20, 15) else LocalTime.of(16, 0)
            val today = z.toLocalDate()
            if (z.dayOfWeek.value <= 5 && z.toLocalTime() >= after && marks.getMark("sum_${m.key}") != today.toString()) {
                val day = mergeDays(d.systems).on(today)
                if (!day.isEmpty) {
                    val per = d.systems.map { it to systemDays(it).on(today) }.filter { !it.second.isEmpty }
                        .sortedByDescending { it.second.pnl }
                    val lines = buildList {
                        add("${Fmt.count(day.trades)} trades · ${Fmt.rate(day.winRate)} win")
                        per.firstOrNull()?.let { (sys, a) -> add("Best: ${sys.name} ${Fmt.compact(a.pnl, d.ccy)}") }
                        per.lastOrNull()?.takeIf { per.size > 1 }?.let { (sys, a) -> add("Worst: ${sys.name} ${Fmt.compact(a.pnl, d.ccy)}") }
                    }
                    Notifier.notify(
                        ctx, 120 + m.ordinal, Notifier.CH_SUMMARY,
                        "${m.flag} ${m.label} ${Fmt.day(today)}: ${Fmt.money(day.pnl, d.ccy, signed = true)}",
                        lines.joinToString("\n"), "overview",
                    )
                }
                marks.setMark("sum_${m.key}", today.toString())
            }
        }
    }
}
