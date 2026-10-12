package com.kalpande.algopulse.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Period
import com.kalpande.algopulse.domain.bucketize
import com.kalpande.algopulse.domain.on
import com.kalpande.algopulse.domain.total
import com.kalpande.algopulse.ui.MarketView
import com.kalpande.algopulse.ui.UiState
import com.kalpande.algopulse.ui.components.AppCard
import com.kalpande.algopulse.ui.components.BarChart
import com.kalpande.algopulse.ui.components.ChartPoint
import com.kalpande.algopulse.ui.components.ChipRow
import com.kalpande.algopulse.ui.components.EmptyState
import com.kalpande.algopulse.ui.components.MonthHeatmap
import com.kalpande.algopulse.ui.components.PnlText
import com.kalpande.algopulse.ui.components.ScreenHeader
import com.kalpande.algopulse.ui.components.ScreenScaffold
import com.kalpande.algopulse.ui.components.SectionHeader
import com.kalpande.algopulse.ui.components.SegmentedControl
import com.kalpande.algopulse.ui.components.StatTile
import com.kalpande.algopulse.ui.components.pnlColor
import com.kalpande.algopulse.ui.theme.tnum
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

private val cpad = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@Composable
fun CalendarScreen(
    state: UiState,
    now: Instant,
    market: Market,
    onMarket: (Market) -> Unit,
    scope: Scope,
    onScope: (Scope) -> Unit,
    onOpenSystem: (Market, String) -> Unit,
) {
    val minute = now.epochSecond / 60
    val v = remember(state.markets, market, scope, minute) { MarketView(state[market], scope, now) }
    var month by remember(market, v.data != null) { mutableStateOf(YearMonth.from(v.session)) }
    var selected by remember(market) { mutableStateOf<LocalDate?>(null) }
    val values = remember(v.days) { v.days.associate { it.date to it.agg.pnl } }
    val first = v.days.firstOrNull()?.date?.let { YearMonth.from(it) } ?: month

    ScreenScaffold {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item { ScreenHeader("Calendar", subtitle = "Daily paper P&L heatmap") }
            item {
                SegmentedControl(
                    Market.entries.map { "${it.flag}  ${it.label}" }, market.ordinal, { onMarket(Market.entries[it]) },
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
                )
                ChipRow(Scope.entries, scope, { it.label }, onScope, Modifier.padding(vertical = 4.dp))
            }
            if (v.data == null) {
                item { EmptyState(Icons.Rounded.EventBusy, "No ${market.label} data yet", "Pull to refresh on the Overview tab.") }
                return@LazyColumn
            }
            item {
                val monthDays = v.days.filter { YearMonth.from(it.date) == month }
                val agg = monthDays.total()
                val best = monthDays.maxByOrNull { it.agg.pnl }
                val worst = monthDays.minByOrNull { it.agg.pnl }
                AppCard(cpad.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }, enabled = month > first) {
                            Icon(Icons.Rounded.ChevronLeft, "Previous month")
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Fmt.month(month.atDay(1)), style = MaterialTheme.typography.titleMedium)
                            PnlText(agg.pnl, v.ccy, style = MaterialTheme.typography.titleLarge)
                        }
                        IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(v.today)) {
                            Icon(Icons.Rounded.ChevronRight, "Next month")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    MonthHeatmap(month, values, v.ccy, v.today, selected = selected, onSelect = { selected = if (selected == it) null else it })
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Green days", "${agg.greenDays}", Modifier.weight(1f), valueColor = pnlColor(1.0))
                        StatTile("Red days", "${agg.redDays}", Modifier.weight(1f), valueColor = pnlColor(-1.0))
                        StatTile("Trades", Fmt.count(agg.trades), Modifier.weight(1f), sub = "win ${Fmt.rate(agg.winRate)}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Best day", best?.let { Fmt.compact(it.agg.pnl, v.ccy) } ?: "—", Modifier.weight(1f),
                            valueColor = pnlColor(best?.agg?.pnl), sub = best?.let { Fmt.day(it.date) })
                        StatTile("Worst day", worst?.let { Fmt.compact(it.agg.pnl, v.ccy) } ?: "—", Modifier.weight(1f),
                            valueColor = pnlColor(worst?.agg?.pnl), sub = worst?.let { Fmt.day(it.date) })
                    }
                }
            }
            selected?.let { day -> item(key = "day") { DayBreakdown(v, day, onOpenSystem) } }
            item {
                val months = remember(v.days) { v.days.bucketize(Period.MONTH).takeLast(12) }
                AppCard(cpad.fillMaxWidth()) {
                    SectionHeader("By month", subtitle = "Last ${months.size} month${if (months.size == 1) "" else "s"}")
                    Spacer(Modifier.height(10.dp))
                    BarChart(
                        months.map { ChartPoint(it.shortLabel, it.agg.pnl) }, height = 160.dp,
                        valueLabel = { Fmt.compact(it, v.ccy) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBreakdown(v: MarketView, day: LocalDate, onOpenSystem: (Market, String) -> Unit) {
    val rows = v.views.map { it to it.days.on(day) }.filter { !it.second.isEmpty }.sortedByDescending { it.second.pnl }
    val agg = v.days.on(day)
    AppCard(cpad.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Fmt.dayLong(day), style = MaterialTheme.typography.titleMedium)
                Text(activityLine(agg, showR = false), style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PnlText(agg.pnl, v.ccy, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (rows.isEmpty()) {
            Text("No activity", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rows.forEach { (sv, a) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenSystem(v.market, sv.s.id) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sv.s.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(activityLine(a), style = MaterialTheme.typography.labelSmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PnlText(a.pnl, v.ccy, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
