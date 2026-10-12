package com.kalpande.algopulse.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.MarketData
import com.kalpande.algopulse.data.SystemDto
import com.kalpande.algopulse.data.TradeDto
import com.kalpande.algopulse.data.parseDate
import com.kalpande.algopulse.data.parseInstant
import com.kalpande.algopulse.data.parseZoned
import com.kalpande.algopulse.domain.Agg
import com.kalpande.algopulse.domain.Bucket
import com.kalpande.algopulse.domain.DayPoint
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Period
import com.kalpande.algopulse.domain.Range
import com.kalpande.algopulse.domain.bestDay
import com.kalpande.algopulse.domain.between
import com.kalpande.algopulse.domain.bucketize
import com.kalpande.algopulse.domain.cumulative
import com.kalpande.algopulse.domain.maxDrawdown
import com.kalpande.algopulse.domain.on
import com.kalpande.algopulse.domain.sharpe
import com.kalpande.algopulse.domain.systemDays
import com.kalpande.algopulse.domain.total
import com.kalpande.algopulse.domain.worstDay
import com.kalpande.algopulse.ui.UiState
import com.kalpande.algopulse.ui.components.AppCard
import com.kalpande.algopulse.ui.components.BarChart
import com.kalpande.algopulse.ui.components.ChartPoint
import com.kalpande.algopulse.ui.components.ChipRow
import com.kalpande.algopulse.ui.components.EmptyState
import com.kalpande.algopulse.ui.components.LabelValue
import com.kalpande.algopulse.ui.components.LineChart
import com.kalpande.algopulse.ui.components.MonthHeatmap
import com.kalpande.algopulse.ui.components.OpenRow
import com.kalpande.algopulse.ui.components.Pill
import com.kalpande.algopulse.ui.components.PnlText
import com.kalpande.algopulse.ui.components.RText
import com.kalpande.algopulse.ui.components.Ring
import com.kalpande.algopulse.ui.components.ScreenHeader
import com.kalpande.algopulse.ui.components.ScreenScaffold
import com.kalpande.algopulse.ui.components.SectionHeader
import com.kalpande.algopulse.ui.components.SegmentedControl
import com.kalpande.algopulse.ui.components.SideBadge
import com.kalpande.algopulse.ui.components.StatTile
import com.kalpande.algopulse.ui.components.StyleBadge
import com.kalpande.algopulse.ui.components.TradeRow
import com.kalpande.algopulse.ui.components.outcomeColor
import com.kalpande.algopulse.ui.components.pnlColor
import com.kalpande.algopulse.ui.theme.AppTheme
import com.kalpande.algopulse.ui.theme.MonoStyle
import com.kalpande.algopulse.ui.theme.tnum
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class TradeFilter(val label: String) { ALL("All"), WINS("Winners"), LOSSES("Losers"), LONG("Long"), SHORT("Short") }

private val pad = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemDetailScreen(state: UiState, now: Instant, market: Market, id: String, onBack: () -> Unit) {
    val data = state[market].data
    val s = data?.system(id)
    val back: @Composable () -> Unit = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
    if (data == null || s == null) {
        ScreenScaffold {
            Column {
                ScreenHeader("Strategy", navigation = back)
                EmptyState(Icons.Rounded.SearchOff, "Not available", "This system isn't in the latest ${market.label} data.")
            }
        }
        return
    }
    val ccy = data.ccy
    val zone = data.zone
    val session = data.sessionDay(now)
    val days = remember(s) { systemDays(s) }
    val total = remember(days) { days.total() }
    val today = days.on(session)
    val showR = state.settings.showR
    var period by rememberSaveable { mutableStateOf(Period.DAY) }
    var filter by rememberSaveable { mutableStateOf(TradeFilter.ALL) }
    var sheet by remember { mutableStateOf<TradeDto?>(null) }
    val trades = remember(s, filter) {
        s.trades.filter {
            when (filter) {
                TradeFilter.ALL -> true
                TradeFilter.WINS -> (it.pnl ?: 0.0) > 0
                TradeFilter.LOSSES -> (it.pnl ?: 0.0) < 0
                TradeFilter.LONG -> it.side != "S"
                TradeFilter.SHORT -> it.side == "S"
            }
        }.sortedWith(compareByDescending<TradeDto> { it.d }.thenByDescending { it.xt ?: it.et ?: "" })
            .groupBy { it.d }
    }
    val dayAgg = remember(days) { days.associate { it.date.toString() to it.agg } }

    ScreenScaffold {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { ScreenHeader(s.name, subtitle = "${market.flag} ${market.label} · ${s.code}", navigation = back) }
            item { StatusSection(s, data, now) }
            item { if (s.isPortfolio) PortfolioHero(s, days, session, ccy) else HeroStats(s, total, today, session, ccy, showR) }
            item { PeriodSection(days, period, { period = it }, ccy, showR && !s.isPortfolio, portfolio = s.isPortfolio) }
            item { KpiGrid(s, days, total, ccy, showR) }
            item { EquitySection(s, days, session, ccy) }
            item { CalendarSection(days, session, ccy, data.today(now)) }
            if (s.isPortfolio) {
                item { HoldingsSection(s, ccy) }
                if (s.fills.isNotEmpty()) item { FillsSection(s, ccy) }
            }
            if (s.open.isNotEmpty()) {
                item {
                    val leftover = s.style == "intraday" && (parseDate(s.status.asof)?.isBefore(data.today(now)) == true)
                    AppCard(pad.fillMaxWidth()) {
                        SectionHeader("Open positions", subtitle = "${s.open.size} open")
                        s.open.forEach { OpenRow(it, s.code, ccy, zone, stale = leftover) }
                    }
                }
            }
            if (!s.isPortfolio) {
                item {
                    Column(Modifier.padding(top = 14.dp)) {
                        SectionHeader(
                            "Trades", Modifier.padding(horizontal = 20.dp),
                            subtitle = buildString {
                                append("${Fmt.count(s.trades.size)} shown")
                                if (s.tradesFrom != null) append(" · from ${Fmt.day(LocalDate.parse(s.tradesFrom))} (daily totals cover all history)")
                            },
                        )
                        ChipRow(TradeFilter.entries, filter, { it.label }, { filter = it }, Modifier.padding(vertical = 6.dp))
                    }
                }
                if (trades.isEmpty()) {
                    item { Text("No trades match.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                trades.forEach { (d, list) ->
                    stickyHeader(key = "day-$d") { DayHeader(d, dayAgg[d], ccy, showR) }
                    items(list.size, key = { "t-$d-$it" }) { i ->
                        TradeRow(list[i], ccy, zone, showR, onClick = { sheet = list[i] })
                    }
                }
            }
        }
    }
    sheet?.let { TradeSheet(it, s, ccy, zone, showR) { sheet = null } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusSection(s: SystemDto, data: MarketData, now: Instant) {
    val t = AppTheme.trade
    Column(pad) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StyleBadge(s.style)
            Pill(s.group, MaterialTheme.colorScheme.secondary)
            if (s.isPortfolio) Pill("Portfolio", t.accent2)
            val updated = parseInstant(s.status.updated)
            if (updated != null) Pill("state updated ${Fmt.relative(updated, now)}", t.neutral)
        }
        if (s.desc.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(s.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val text = listOfNotNull(s.status.text, s.status.note).joinToString("\n")
        if (text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            val warn = s.status.level == "warn"
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (warn) t.warnContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (warn) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = t.warn, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HeroStats(s: SystemDto, total: Agg, today: Agg, session: LocalDate, ccy: String, showR: Boolean) {
    AppCard(pad.fillMaxWidth(), brush = androidx.compose.ui.graphics.Brush.linearGradient(AppTheme.trade.heroGradient)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("All-time P&L", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PnlText(total.pnl, ccy, style = MaterialTheme.typography.displaySmall, animate = true)
                Text(
                    "since ${s.since?.let { Fmt.dayNum(LocalDate.parse(it)) } ?: "—"} · ${Fmt.count(total.trades)} trades" +
                        if (showR && total.rN > 0) " · ${Fmt.r(total.rSum)}" else "",
                    style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // W/L counts go under the ring: inside it, four-digit counts ("1.1kW · 1.7kL") run into the stroke
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                    Ring((total.winRate ?: 0.0).toFloat(), Modifier.size(84.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Fmt.rate(total.winRate), style = MaterialTheme.typography.titleMedium.tnum())
                        Text("win", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Fmt.countCompact(total.wins)} W · ${Fmt.countCompact(total.losses)} L",
                    style = MaterialTheme.typography.labelSmall.tnum(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.day(session), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(activityLine(today), style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                PnlText(today.pnl, ccy, style = MaterialTheme.typography.titleLarge)
                if (showR && today.rN > 0) RText(today.rSum)
            }
        }
    }
}

@Composable
private fun PortfolioHero(s: SystemDto, days: List<DayPoint>, session: LocalDate, ccy: String) {
    val eqNow = s.equity.lastOrNull()?.eq ?: s.startEquity ?: 0.0
    val start = s.startEquity ?: s.equity.firstOrNull()?.eq ?: eqNow
    val ret = if (start > 0) (eqNow / start - 1) * 100 else null
    val b0 = s.benchStart ?: s.equity.firstOrNull()?.bm
    val b1 = s.equity.lastOrNull()?.bm
    val bret = if (b0 != null && b1 != null && b0 > 0) (b1 / b0 - 1) * 100 else null
    val today = days.on(session)
    AppCard(pad.fillMaxWidth(), brush = androidx.compose.ui.graphics.Brush.linearGradient(AppTheme.trade.heroGradient)) {
        Text("Portfolio value", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Fmt.money(eqNow, ccy, decimals = 0), style = MaterialTheme.typography.displaySmall.tnum())
        Row(verticalAlignment = Alignment.CenterVertically) {
            PnlText(eqNow - start, ccy, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(Fmt.pct(ret, 2, signed = true), style = MaterialTheme.typography.titleMedium.tnum(), color = pnlColor(ret))
            Text(
                "  since ${s.since?.let { Fmt.dayNum(LocalDate.parse(it)) } ?: "—"}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(s.bench ?: "Benchmark", Fmt.pct(bret, 2, signed = true), Modifier.weight(1f), valueColor = pnlColor(bret))
            val alpha = if (ret != null && bret != null) ret - bret else null
            StatTile("vs benchmark", Fmt.pct(alpha, 2, signed = true), Modifier.weight(1f), valueColor = pnlColor(alpha))
            StatTile(Fmt.day(session), Fmt.money(today.pnl, ccy, signed = true), Modifier.weight(1f), valueColor = pnlColor(today.pnl))
        }
    }
}

@Composable
private fun PeriodSection(days: List<DayPoint>, period: Period, onPeriod: (Period) -> Unit, ccy: String, showR: Boolean, portfolio: Boolean = false) {
    val buckets = remember(days, period) { days.bucketize(period) }
    val shown = buckets.takeLast(30)
    var sel by remember(buckets) { mutableStateOf<Int?>(null) }
    var expanded by remember(period) { mutableStateOf(false) }
    AppCard(pad.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("P&L by", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 12.dp))
            SegmentedControl(Period.entries.map { it.label }, period.ordinal, { onPeriod(Period.entries[it]) }, Modifier.weight(1f), dense = true)
        }
        Spacer(Modifier.height(12.dp))
        BarChart(shown.map { ChartPoint(it.shortLabel, it.agg.pnl) }, selected = sel, onSelect = { sel = if (sel == it) null else it })
        val b = sel?.let { shown.getOrNull(it) } ?: shown.lastOrNull()
        if (b != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(b.label, style = MaterialTheme.typography.titleSmall)
                    Text(activityLine(b.agg), style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PnlText(b.agg.pnl, ccy, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            HeaderCell("Period", 1.5f, TextAlign.Start)
            if (!portfolio) {
                HeaderCell("Trades", 0.75f)
                HeaderCell("Win", 0.6f)
                if (showR) HeaderCell("R", 0.95f)
            } else {
                HeaderCell("Green days", 1.0f)
            }
            HeaderCell("P&L", 1.1f)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val rows = buckets.asReversed().let { if (expanded) it else it.take(8) }
        rows.forEach { BucketRow(it, ccy, showR, portfolio) }
        if (buckets.size > 8) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (expanded) "Show less" else "Show all ${buckets.size}")
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float, align: TextAlign = TextAlign.End) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align, modifier = Modifier.weight(weight),
    )
}

@Composable
private fun BucketRow(b: Bucket, ccy: String, showR: Boolean, portfolio: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(b.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!portfolio) {
            Text(Fmt.count(b.agg.trades), style = MaterialTheme.typography.bodySmall.tnum(), textAlign = TextAlign.End, modifier = Modifier.weight(0.75f))
            Text(Fmt.rate(b.agg.winRate), style = MaterialTheme.typography.bodySmall.tnum(), textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
            if (showR) {
                Text(
                    if (b.agg.rN > 0) Fmt.rShort(b.agg.rSum) else "—", style = MaterialTheme.typography.bodySmall.tnum(),
                    color = pnlColor(if (b.agg.rN > 0) b.agg.rSum else null), textAlign = TextAlign.End, modifier = Modifier.weight(0.95f),
                    maxLines = 1,
                )
            }
        } else {
            Text("${b.agg.greenDays}/${b.agg.days}", style = MaterialTheme.typography.bodySmall.tnum(), textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
        }
        Text(
            Fmt.compact(b.agg.pnl, ccy), style = MaterialTheme.typography.bodySmall.tnum(), fontWeight = FontWeight.SemiBold,
            color = pnlColor(b.agg.pnl), textAlign = TextAlign.End, modifier = Modifier.weight(1.1f), maxLines = 1,
        )
    }
}

@Composable
private fun KpiGrid(s: SystemDto, days: List<DayPoint>, total: Agg, ccy: String, showR: Boolean) {
    val dd = remember(days) { days.maxDrawdown() }
    val sharpe = remember(days) { days.sharpe() }
    val best = remember(days) { days.bestDay() }
    val worst = remember(days) { days.worstDay() }
    val tiles = buildList<Kpi> {
        if (!s.isPortfolio) {
            add(Kpi("Profit factor", Fmt.ratio(total.profitFactor), null))
            add(Kpi("Expectancy / trade", total.expectancy?.let { Fmt.money(it, ccy, signed = true) } ?: "—", total.expectancy))
            add(Kpi("Avg win", total.avgWin?.let { Fmt.money(it, ccy) } ?: "—", total.avgWin))
            add(Kpi("Avg loss", total.avgLoss?.let { Fmt.money(it, ccy) } ?: "—", total.avgLoss))
            add(Kpi("Payoff ratio", Fmt.ratio(total.payoff), null))
            add(Kpi("Best trade", total.best?.let { Fmt.money(it, ccy, signed = true) } ?: "—", total.best))
            add(Kpi("Worst trade", total.worst?.let { Fmt.money(it, ccy, signed = true) } ?: "—", total.worst))
            if (showR && total.rN > 0) {
                add(Kpi("Avg R / trade", Fmt.r(total.expectancyR), total.expectancyR))
                add(Kpi("t-stat (R)", Fmt.decimal(total.tStatR), null))
            }
            if (total.fees > 0) add(Kpi("Charges", Fmt.money(-total.fees, ccy), -total.fees))
        }
        add(Kpi("Max drawdown", Fmt.money(dd, ccy), if (dd < 0) dd else null))
        add(Kpi("Sharpe (daily, ann.)", Fmt.decimal(sharpe), null))
        add(Kpi("Best day", best?.let { Fmt.compact(it.agg.pnl, ccy) } ?: "—", best?.agg?.pnl, best?.let { Fmt.day(it.date) }))
        add(Kpi("Worst day", worst?.let { Fmt.compact(it.agg.pnl, ccy) } ?: "—", worst?.agg?.pnl, worst?.let { Fmt.day(it.date) }))
        add(Kpi("Green / red days", "${total.greenDays} / ${total.redDays}", null))
        add(Kpi("Active days", "${total.days}", null))
    }
    Column(pad) {
        SectionHeader("Statistics", subtitle = if (total.tStatR != null && showR && !s.isPortfolio) tNote(total.tStatR!!) else null)
        Spacer(Modifier.height(8.dp))
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                pair.forEach { k ->
                    StatTile(
                        k.label, k.value, Modifier.weight(1f), sub = k.sub,
                        valueColor = if (k.signal == null) MaterialTheme.colorScheme.onSurface else pnlColor(k.signal),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Kpi(val label: String, val value: String, val signal: Double?, val sub: String? = null)

private fun tNote(t: Double): String = when {
    t >= 2 -> "t = ${Fmt.decimal(t)}: edge is statistically meaningful"
    t <= -2 -> "t = ${Fmt.decimal(t)}: reliably losing"
    else -> "t = ${Fmt.decimal(t)}: not distinguishable from noise yet"
}

@Composable
private fun EquitySection(s: SystemDto, days: List<DayPoint>, session: LocalDate, ccy: String) {
    var range by rememberSaveable { mutableStateOf(Range.ALL) }
    var scrub by remember { mutableStateOf<Int?>(null) }
    AppCard(pad.fillMaxWidth()) {
        if (s.isPortfolio && s.equity.size >= 1) {
            val start = s.startEquity ?: s.equity.first().eq ?: 1.0
            val b0 = s.benchStart ?: s.equity.firstOrNull()?.bm
            val pts = s.equity.filter { it.eq != null }
            val mine = pts.map { ChartPoint(Fmt.day(LocalDate.parse(it.d)), (it.eq!! / start - 1) * 100) }
            val bench = if (b0 != null && pts.all { it.bm != null }) pts.map { (it.bm!! / b0 - 1) * 100 } else null
            val i = scrub
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (i != null && i < mine.size) mine[i].label else "Return vs ${s.bench ?: "benchmark"}",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                val v = if (i != null && i < mine.size) mine[i].value else mine.lastOrNull()?.value
                Text(Fmt.pct(v, 2, signed = true), style = MaterialTheme.typography.titleMedium.tnum(), color = pnlColor(v))
            }
            Spacer(Modifier.height(10.dp))
            LineChart(mine, secondary = bench, onScrub = { scrub = it })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend("Portfolio", if ((mine.lastOrNull()?.value ?: 0.0) >= 0) AppTheme.trade.profit else AppTheme.trade.loss)
                if (bench != null) Legend(s.bench ?: "Benchmark", AppTheme.trade.accent2)
            }
        } else {
            val pts = remember(days, range, session) { days.between(range.start(session), session).cumulative() }
            val i = scrub
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (i != null && i < pts.size) Fmt.day(pts[i].first) else "Equity curve",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                PnlText(if (i != null && i < pts.size) pts[i].second else pts.lastOrNull()?.second ?: 0.0, ccy, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(10.dp))
            LineChart(pts.map { ChartPoint(Fmt.day(it.first), it.second) }, onScrub = { scrub = it })
            Spacer(Modifier.height(10.dp))
            SegmentedControl(Range.entries.map { it.label }, range.ordinal, { range = Range.entries[it] }, Modifier.fillMaxWidth(), dense = true)
        }
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(MaterialTheme.shapes.extraSmall).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarSection(days: List<DayPoint>, session: LocalDate, ccy: String, today: LocalDate) {
    var month by remember(session) { mutableStateOf(YearMonth.from(session)) }
    val values = remember(days) { days.associate { it.date to it.agg.pnl } }
    val first = days.firstOrNull()?.date?.let { YearMonth.from(it) } ?: month
    val monthTotal = values.filterKeys { YearMonth.from(it) == month }.values.sum()
    AppCard(pad.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }, enabled = month > first) { Icon(Icons.Rounded.ChevronLeft, "Previous month") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.month(month.atDay(1)), style = MaterialTheme.typography.titleMedium)
                PnlText(monthTotal, ccy, style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(today)) { Icon(Icons.Rounded.ChevronRight, "Next month") }
        }
        Spacer(Modifier.height(8.dp))
        MonthHeatmap(month, values, ccy, today, cellHeight = 46.dp)
    }
}

@Composable
private fun HoldingsSection(s: SystemDto, ccy: String) {
    val t = AppTheme.trade
    AppCard(pad.fillMaxWidth()) {
        SectionHeader("Holdings", subtitle = "${s.holdings.size} positions" + (s.cash?.let { " · cash ${Fmt.money(it, ccy)}" } ?: ""))
        Spacer(Modifier.height(6.dp))
        s.holdings.forEach { h ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.sym, style = MaterialTheme.typography.titleSmall)
                        if (h.sector != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(h.sector, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val w = ((h.w ?: 0.0) / 10.0).coerceIn(0.0, 1.0).toFloat()
                    Box(Modifier.fillMaxWidth(0.9f).height(5.dp).clip(MaterialTheme.shapes.extraSmall).background(t.grid)) {
                        Box(Modifier.fillMaxWidth(w).height(5.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.primary))
                    }
                    Text(
                        "${Fmt.qty(h.q)} @ ${Fmt.price(h.last, ccy)} · ${Fmt.pct(h.w, 1)} of equity",
                        style = MaterialTheme.typography.labelSmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(h.value?.let { Fmt.money(it, ccy) } ?: "—", style = MaterialTheme.typography.bodyMedium.tnum())
                    if (h.upnl != null) PnlText(h.upnl, ccy, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun FillsSection(s: SystemDto, ccy: String) {
    var all by remember { mutableStateOf(false) }
    val fills = s.fills.sortedByDescending { it.d }
    AppCard(pad.fillMaxWidth()) {
        SectionHeader("Rebalance fills", subtitle = "${s.fills.size} fills")
        (if (all) fills else fills.take(12)).forEach { f ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(f.act ?: "?", if (f.act == "SELL") AppTheme.trade.loss else AppTheme.trade.profit)
                Spacer(Modifier.width(8.dp))
                Text("${f.sym}  ${Fmt.qty(f.q)} @ ${Fmt.price(f.px, ccy)}", style = MaterialTheme.typography.bodySmall.tnum(), modifier = Modifier.weight(1f))
                Text(Fmt.day(LocalDate.parse(f.d)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if ((f.pnl ?: 0.0) != 0.0) {
                    Spacer(Modifier.width(8.dp))
                    PnlText(f.pnl ?: 0.0, ccy, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (fills.size > 12) {
            TextButton(onClick = { all = !all }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (all) "Show less" else "Show all ${fills.size}")
            }
        }
    }
}

@Composable
private fun DayHeader(d: String, agg: Agg?, ccy: String, showR: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(Fmt.dayLong(LocalDate.parse(d)), style = MaterialTheme.typography.labelLarge)
            if (agg != null) Text(activityLine(agg), style = MaterialTheme.typography.labelSmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (agg != null) {
            Column(horizontalAlignment = Alignment.End) {
                PnlText(agg.pnl, ccy, style = MaterialTheme.typography.labelLarge)
                if (showR && agg.rN > 0) RText(agg.rSum, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TradeSheet(t: TradeDto, s: SystemDto, ccy: String, zone: ZoneId, showR: Boolean, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SideBadge(t.side)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.sym, style = MaterialTheme.typography.headlineSmall)
                    Text("${s.name} · ${if (t.side == "S") "Short" else "Long"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!t.out.isNullOrBlank()) Pill(t.out.replace('_', ' '), outcomeColor(t.out), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                PnlText(t.pnl ?: 0.0, ccy, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(12.dp))
                Column {
                    if (showR && t.r != null) RText(t.r, style = MaterialTheme.typography.titleMedium)
                    if (t.ret != null) Text(Fmt.pct(t.ret, 2, signed = true), style = MaterialTheme.typography.bodyMedium.tnum(), color = pnlColor(t.ret))
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))
            val et = parseZoned(t.et, zone)
            val xt = parseZoned(t.xt, zone)
            LabelValue("Entry", "${Fmt.price(t.ep, ccy)} · ${Fmt.dayTime(et)}")
            LabelValue("Exit", "${Fmt.price(t.xp, ccy)} · ${Fmt.dayTime(xt)}")
            if (t.sl != null) LabelValue("Initial stop", Fmt.price(t.sl, ccy))
            if (t.tp != null) LabelValue("Target", Fmt.price(t.tp, ccy))
            if (t.q != null) LabelValue("Quantity", Fmt.qty(t.q))
            val held = t.hold?.toLong() ?: if (et != null && xt != null) java.time.Duration.between(et, xt).toMinutes() else null
            if (held != null) LabelValue("Held", Fmt.duration(held))
            if (t.gross != null) LabelValue("Gross", Fmt.money(t.gross, ccy, signed = true), valueColor = pnlColor(t.gross))
            if (t.fees != null) LabelValue("Charges", Fmt.money(-t.fees, ccy), valueColor = AppTheme.trade.loss)
            LabelValue("Session", Fmt.dayLong(LocalDate.parse(t.d)))
            if (t.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    t.tags.forEach { Pill(it, MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium) }
                }
            }
            if (!t.note.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("Why it fired", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(t.note, style = MonoStyle, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
