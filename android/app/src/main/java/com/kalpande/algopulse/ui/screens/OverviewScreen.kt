package com.kalpande.algopulse.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.domain.Agg
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Freshness
import com.kalpande.algopulse.domain.Range
import com.kalpande.algopulse.domain.between
import com.kalpande.algopulse.domain.cumulative
import com.kalpande.algopulse.ui.MarketFilter
import com.kalpande.algopulse.ui.MarketView
import com.kalpande.algopulse.ui.UiState
import com.kalpande.algopulse.ui.components.AppCard
import com.kalpande.algopulse.ui.components.ChartPoint
import com.kalpande.algopulse.ui.components.ChipRow
import com.kalpande.algopulse.ui.components.DivergingBar
import com.kalpande.algopulse.ui.components.EmptyState
import com.kalpande.algopulse.ui.components.FreshnessPill
import com.kalpande.algopulse.ui.components.LineChart
import com.kalpande.algopulse.ui.components.LocalReduceMotion
import com.kalpande.algopulse.ui.components.OpenRow
import com.kalpande.algopulse.ui.components.PnlText
import com.kalpande.algopulse.ui.components.RText
import com.kalpande.algopulse.ui.components.ScreenHeader
import com.kalpande.algopulse.ui.components.ScreenScaffold
import com.kalpande.algopulse.ui.components.SectionHeader
import com.kalpande.algopulse.ui.components.SegmentedControl
import com.kalpande.algopulse.ui.components.Shimmer
import com.kalpande.algopulse.ui.components.Sparkline
import com.kalpande.algopulse.ui.components.StatTile
import com.kalpande.algopulse.ui.components.pnlColor
import com.kalpande.algopulse.ui.theme.AppTheme
import com.kalpande.algopulse.ui.theme.tnum
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: UiState,
    now: Instant,
    filter: MarketFilter,
    onFilter: (MarketFilter) -> Unit,
    scope: Scope,
    onScope: (Scope) -> Unit,
    onRefresh: () -> Unit,
    onOpenSystem: (Market, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
) {
    val minute = now.epochSecond / 60
    val views = remember(state.markets, scope, minute) {
        Market.entries.associateWith { MarketView(state[it], scope, now) }
    }
    val pull = rememberPullToRefreshState()
    ScreenScaffold {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            state = pull,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pull, isRefreshing = state.refreshing,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                )
            },
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
                item {
                    val fetched = state.markets.values.mapNotNull { it.fetchedAt }.maxOrNull()
                    ScreenHeader(
                        overline = "Nayan Kalpande",
                        title = "Appreciation Society",
                        titleMaxLines = 2,
                        subtitle = "${greeting(now)} · updated ${Fmt.relative(fetched, now)}",
                    ) {
                        RefreshButton(state.refreshing, onRefresh)
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                    }
                }
                item {
                    SegmentedControl(
                        options = MarketFilter.entries.map { it.label },
                        selected = filter.ordinal,
                        onSelect = { onFilter(MarketFilter.entries[it]) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
                    )
                    ChipRow(Scope.entries, scope, { it.label }, onScope, Modifier.padding(vertical = 4.dp))
                }
                for (m in Market.entries) {
                    val v = views.getValue(m)
                    if (v.data != null && v.freshness == Freshness.DOWN && (filter.market == null || filter.market == m)) {
                        item(key = "outage-${m.key}") { OutageBanner(v, now, onOpenHealth) }
                    }
                }
                if (!state.anyData) {
                    item { FirstRun(state) }
                } else {
                    val m = filter.market
                    if (m == null) allMarkets(views, onFilter, onOpenSystem) else oneMarket(views.getValue(m), state, onOpenSystem)
                }
                item {
                    Text(
                        "Paper trading only · data published by the bots' GitHub workflows",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    )
                }
            }
        }
    }
}

private fun greeting(now: Instant): String {
    val h = now.atZone(ZoneId.systemDefault()).toLocalTime()
    return when {
        h < LocalTime.of(12, 0) -> "Good morning"
        h < LocalTime.of(17, 0) -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
fun RefreshButton(refreshing: Boolean, onRefresh: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val angle = if (refreshing && !reduce) {
        val inf = rememberInfiniteTransition(label = "spin")
        val a by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "a")
        a
    } else 0f
    IconButton(onClick = onRefresh, enabled = !refreshing) {
        Icon(Icons.Rounded.Refresh, "Refresh", modifier = Modifier.rotate(angle))
    }
}

private fun LazyListScope.allMarkets(
    views: Map<Market, MarketView>,
    onFilter: (MarketFilter) -> Unit,
    onOpenSystem: (Market, String) -> Unit,
) {
    for (m in Market.entries) {
        val v = views.getValue(m)
        item(key = "summary-${m.key}") {
            MarketSummaryCard(v, onClick = { onFilter(if (m == Market.US) MarketFilter.US else MarketFilter.NSE) })
        }
    }
    val open = Market.entries.map { views.getValue(it) }.filter { it.openPositions.isNotEmpty() }
    if (open.isNotEmpty()) item(key = "open-all") { OpenPositionsCard(open) }
    for (m in Market.entries) {
        val v = views.getValue(m)
        if (v.data != null) item(key = "bysys-${m.key}") { BySystemCard(v, showFlag = true, onOpenSystem = onOpenSystem) }
    }
}

private fun LazyListScope.oneMarket(v: MarketView, state: UiState, onOpenSystem: (Market, String) -> Unit) {
    if (v.data == null) {
        item {
            val ms = state[v.market]
            EmptyState(
                Icons.Rounded.CloudSync,
                if (ms.notPublished) "${v.market.label}: waiting for the first publish" else "${v.market.label}: no data",
                ms.error ?: "The dashboard job publishes data every workflow run (~2 min). Pull to refresh.",
            )
        }
        return
    }
    item(key = "hero") { HeroCard(v) }
    item(key = "periods") { PeriodGrid(v) }
    item(key = "bysys") { BySystemCard(v, showFlag = false, onOpenSystem = onOpenSystem) }
    if (v.openPositions.isNotEmpty()) item(key = "open") { OpenPositionsCard(listOf(v)) }
    item(key = "rank") { RankingCard(v, onOpenSystem) }
}

private val cardPad = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@Composable
private fun OutageBanner(v: MarketView, now: Instant, onClick: () -> Unit) {
    val t = AppTheme.trade
    val mins = Fmt.ageMinutes(v.generatedAt, now) ?: 0
    Row(
        cardPad
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(t.lossContainer)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CloudOff, null, tint = t.loss, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${v.market.flag} ${v.market.label} bot is not publishing", style = MaterialTheme.typography.titleSmall, color = t.loss)
            Text(
                "No new data for ${Fmt.duration(mins)}. Check the cron-job.org trigger and GitHub Actions.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FirstRun(state: UiState) {
    if (!state.loaded || state.refreshing) {
        Column(cardPad) {
            Shimmer(Modifier.fillMaxWidth().height(210.dp))
            Spacer(Modifier.height(12.dp))
            Shimmer(Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(12.dp))
            Shimmer(Modifier.fillMaxWidth().height(160.dp))
        }
        return
    }
    val waiting = state.markets.values.all { it.notPublished }
    EmptyState(
        icon = if (waiting) Icons.Rounded.CloudSync else Icons.Rounded.CloudOff,
        title = if (waiting) "Waiting for the first publish" else "Couldn't load the dashboards",
        body = if (waiting) {
            "Each bot publishes its dashboard from GitHub Actions every ~2 minutes once the dashboard job is merged. Pull down to refresh."
        } else {
            state.markets.values.mapNotNull { it.error }.distinct().joinToString("\n").ifBlank { "Check your connection and pull to refresh." }
        },
    )
}

@Composable
private fun MarketSummaryCard(v: MarketView, onClick: () -> Unit) {
    AppCard(cardPad.fillMaxWidth(), onClick = onClick, brush = Brush.linearGradient(AppTheme.trade.heroGradient)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${v.market.flag}  ${v.market.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FreshnessPill(v.freshness, v.ageText)
        }
        if (v.data == null) {
            Spacer(Modifier.height(10.dp))
            Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@AppCard
        }
        Spacer(Modifier.height(10.dp))
        Text(v.sessionLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                PnlText(v.summary.today.pnl, v.ccy, style = MaterialTheme.typography.headlineMedium, animate = true)
                Text(activityLine(v.summary.today, showR = false), style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Sparkline(v.days.cumulative().takeLast(60).map { it.second }, Modifier.size(118.dp, 46.dp))
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Mini("This week", v.summary.week.pnl, v.ccy)
            Mini("This month", v.summary.month.pnl, v.ccy)
            Mini("All time", v.summary.all.pnl, v.ccy)
        }
    }
}

@Composable
private fun Mini(label: String, value: Double, ccy: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PnlText(value, ccy, style = MaterialTheme.typography.titleSmall, compact = true)
    }
}

fun activityLine(a: Agg, showR: Boolean = true): String = when {
    a.trades == 0 && a.pnl == 0.0 -> "No trades"
    a.trades == 0 -> "Mark-to-market"
    else -> buildString {
        append("${Fmt.count(a.trades)} trade${if (a.trades == 1) "" else "s"}")
        a.winRate?.let { append(" · ${Fmt.rate(it)} win") }
        if (showR && a.rN > 0) append(" · ${Fmt.r(a.rSum)}")
    }
}

@Composable
private fun HeroCard(v: MarketView) {
    var range by rememberSaveable { mutableStateOf(Range.ALL) }
    val chartDays = remember(v.days, range, v.session) { v.days.between(range.start(v.session), v.session) }
    val cum = remember(chartDays) { chartDays.cumulative() }
    var scrub by remember(cum) { mutableStateOf<Int?>(null) }
    AppCard(cardPad.fillMaxWidth(), brush = Brush.linearGradient(AppTheme.trade.heroGradient)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${v.market.flag}  ${v.market.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FreshnessPill(v.freshness, v.ageText)
        }
        Spacer(Modifier.height(14.dp))
        Text(v.sessionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PnlText(v.summary.today.pnl, v.ccy, style = MaterialTheme.typography.displaySmall, animate = true)
        Text(activityLine(v.summary.today, showR = false), style = MaterialTheme.typography.bodyMedium.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Mini("Week", v.summary.week.pnl, v.ccy)
            Mini("Month", v.summary.month.pnl, v.ccy)
            Mini("Year", v.summary.year.pnl, v.ccy)
            Mini("All time", v.summary.all.pnl, v.ccy)
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        val s = scrub
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (s != null && s < cum.size) Fmt.day(cum[s].first) else "Cumulative P&L · ${range.label}",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            val value = if (s != null && s < cum.size) cum[s].second else cum.lastOrNull()?.second ?: 0.0
            PnlText(value, v.ccy, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        LineChart(
            points = cum.map { ChartPoint(Fmt.day(it.first), it.second) },
            height = 170.dp,
            onScrub = { scrub = it },
        )
        Spacer(Modifier.height(10.dp))
        SegmentedControl(Range.entries.map { it.label }, range.ordinal, { range = Range.entries[it] }, Modifier.fillMaxWidth(), dense = true)
    }
}

@Composable
private fun PeriodGrid(v: MarketView) {
    val s = v.summary
    Column(cardPad) {
        SectionHeader("Performance", subtitle = "Realised paper P&L by period")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PeriodTile("This week", s.week, v.ccy, Modifier.weight(1f))
            PeriodTile("This month", s.month, v.ccy, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PeriodTile("This year", s.year, v.ccy, Modifier.weight(1f))
            PeriodTile("All time", s.all, v.ccy, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PeriodTile(label: String, a: Agg, ccy: String, modifier: Modifier) {
    StatTile(
        label = label,
        value = Fmt.money(a.pnl, ccy, signed = true),
        valueColor = pnlColor(a.pnl),
        sub = if (a.trades > 0) "${Fmt.count(a.trades)} trades · ${Fmt.rate(a.winRate)} win" else "${a.days} active day${if (a.days == 1) "" else "s"}",
        modifier = modifier,
    )
}

@Composable
private fun BySystemCard(v: MarketView, showFlag: Boolean, onOpenSystem: (Market, String) -> Unit) {
    val rows = remember(v.views) {
        v.views.sortedWith(compareByDescending<com.kalpande.algopulse.ui.SystemView> { !it.today.isEmpty }.thenByDescending { it.today.pnl })
    }
    val maxAbs = rows.maxOfOrNull { abs(it.today.pnl) } ?: 0.0
    AppCard(cardPad.fillMaxWidth()) {
        SectionHeader(
            if (showFlag) "${v.market.flag}  ${v.market.label} by system" else "By system",
            subtitle = v.sessionLabel,
        )
        Spacer(Modifier.height(6.dp))
        rows.forEach { sv ->
            val active = !sv.today.isEmpty
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenSystem(v.market, sv.s.id) }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(0.46f)) {
                    Text(
                        sv.s.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (active) activityLine(sv.today, showR = false) else "No trades",
                        style = MaterialTheme.typography.labelSmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                DivergingBar(sv.today.pnl, maxAbs, Modifier.weight(0.26f).padding(horizontal = 8.dp))
                Column(Modifier.weight(0.28f), horizontalAlignment = Alignment.End) {
                    if (active) PnlText(sv.today.pnl, v.ccy, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    else Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (active && sv.today.rN > 0) RText(sv.today.rSum, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun OpenPositionsCard(views: List<MarketView>) {
    var all by rememberSaveable { mutableStateOf(false) }
    // leftovers first: they are the ones that need attention
    val rows = views.flatMap { v -> v.openPositions.map { (s, o) -> Triple(v, s, o) } }
        .sortedByDescending { (v, s, _) -> v.isLeftover(s) }
    val leftovers = rows.count { (v, s, _) -> v.isLeftover(s) }
    AppCard(cardPad.fillMaxWidth()) {
        SectionHeader(
            "Open positions",
            subtitle = "${rows.size} open" + (if (leftovers > 0) " · $leftovers not closed from an earlier session" else "") +
                " · unrealised where a live price is known",
        )
        Spacer(Modifier.height(4.dp))
        (if (all) rows else rows.take(6)).forEach { (v, s, o) ->
            OpenRow(o, "${if (views.size > 1) v.market.flag + " " else ""}${s.name}", v.ccy, v.zone, stale = v.isLeftover(s))
        }
        if (rows.size > 6) {
            androidx.compose.material3.TextButton(onClick = { all = !all }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (all) "Show less" else "Show all ${rows.size}")
            }
        }
    }
}

@Composable
private fun RankingCard(v: MarketView, onOpenSystem: (Market, String) -> Unit) {
    val ranked = remember(v.views) { v.views.sortedByDescending { it.total.pnl } }
    AppCard(cardPad.fillMaxWidth()) {
        SectionHeader("All-time ranking", subtitle = "Every ${v.market.label} system since it started")
        Spacer(Modifier.height(6.dp))
        ranked.forEachIndexed { i, sv ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenSystem(v.market, sv.s.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${i + 1}", style = MaterialTheme.typography.labelLarge.tnum(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(26.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(sv.s.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (sv.s.isPortfolio) "${sv.total.days} days · since ${sv.s.since ?: "—"}"
                        else "${Fmt.count(sv.total.trades)} trades · ${Fmt.rate(sv.total.winRate)} win",
                        style = MaterialTheme.typography.labelSmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Sparkline(sv.spark, Modifier.size(64.dp, 24.dp))
                Spacer(Modifier.width(10.dp))
                PnlText(sv.total.pnl, v.ccy, compact = true, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
