package com.kalpande.algopulse.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.domain.Agg
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Period
import com.kalpande.algopulse.domain.between
import com.kalpande.algopulse.domain.periodStart
import com.kalpande.algopulse.domain.total
import com.kalpande.algopulse.ui.MarketFilter
import com.kalpande.algopulse.ui.MarketView
import com.kalpande.algopulse.ui.SystemView
import com.kalpande.algopulse.ui.UiState
import com.kalpande.algopulse.ui.components.ChipRow
import com.kalpande.algopulse.ui.components.EmptyState
import com.kalpande.algopulse.ui.components.ScreenHeader
import com.kalpande.algopulse.ui.components.ScreenScaffold
import com.kalpande.algopulse.ui.components.SegmentedControl
import com.kalpande.algopulse.ui.components.SystemCard
import java.time.Instant
import java.time.LocalDate

enum class SortBy(val label: String) {
    PNL("P&L"),
    WIN("Win rate"),
    EXPECTANCY("Avg R / trade"),
    TRADES("Most trades"),
    NAME("Name"),
}

/** The period the strategy cards compare. */
enum class StratPeriod(val label: String) {
    SESSION("Today"), WEEK("Week"), MONTH("Month"), YEAR("Year"), ALL("All time");

    fun agg(sv: SystemView, session: LocalDate): Agg = when (this) {
        SESSION -> sv.today
        WEEK -> sv.days.between(periodStart(session, Period.WEEK), session).total()
        MONTH -> sv.days.between(periodStart(session, Period.MONTH), session).total()
        YEAR -> sv.days.between(periodStart(session, Period.YEAR), session).total()
        ALL -> sv.total
    }
}

enum class StyleFilter(val label: String) {
    ALL("All styles"), INTRADAY("Intraday"), SWING("Swing");

    fun includes(style: String) = when (this) {
        ALL -> true
        INTRADAY -> style == "intraday" || style == "hybrid"
        SWING -> style == "swing" || style == "hybrid"
    }
}

@Composable
fun StrategiesScreen(
    state: UiState,
    now: Instant,
    filter: MarketFilter,
    onFilter: (MarketFilter) -> Unit,
    scope: Scope,
    onScope: (Scope) -> Unit,
    onOpenSystem: (Market, String) -> Unit,
) {
    var sort by rememberSaveable { mutableStateOf(SortBy.PNL) }
    var period by rememberSaveable { mutableStateOf(StratPeriod.ALL) }
    var style by rememberSaveable { mutableStateOf(StyleFilter.ALL) }
    var menu by remember { mutableStateOf(false) }
    val minute = now.epochSecond / 60
    val views = remember(state.markets, scope, minute) { Market.entries.associateWith { MarketView(state[it], scope, now) } }
    val markets = filter.market?.let { listOf(it) } ?: Market.entries
    val sections: List<Pair<String, List<Pair<SystemView, Agg>>>> = remember(views, markets, sort, style, period) {
        markets.flatMap { m ->
            val v = views.getValue(m)
            v.views.filter { style.includes(it.s.style) }
                .map { it to period.agg(it, v.session) }
                .groupBy { it.first.s.group }
                .toList()
                .sortedBy { (g, _) -> listOf("Models", "ML filters", "Research").indexOf(g).let { if (it < 0) 9 else it } }
                .map { (g, list) -> "${m.flag}  ${m.label} · $g" to sorted(list, sort) }
        }
    }
    val total = sections.sumOf { it.second.size }
    val profitable = sections.sumOf { s -> s.second.count { it.second.pnl > 0 } }
    ScreenScaffold {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScreenHeader("Strategies", subtitle = "$total systems · $profitable profitable · ${period.label.lowercase()}") {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            SortBy.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.label) },
                                    onClick = { sort = s; menu = false },
                                    trailingIcon = { if (s == sort) Icon(Icons.Rounded.Check, null) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                SegmentedControl(
                    MarketFilter.entries.map { it.label }, filter.ordinal, { onFilter(MarketFilter.entries[it]) },
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
                )
                ChipRow(Scope.entries, scope, { it.label }, onScope, Modifier.padding(top = 4.dp))
                ChipRow(StyleFilter.entries, style, { it.label }, { style = it }, Modifier.padding(top = 4.dp))
                SegmentedControl(
                    StratPeriod.entries.map { it.label }, period.ordinal, { period = StratPeriod.entries[it] },
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), dense = true,
                )
            }
            if (total == 0) {
                item { EmptyState(Icons.Rounded.Inventory2, "No systems", "Nothing matches these filters yet.") }
            }
            sections.forEach { (title, list) ->
                item(key = "h-$title") {
                    Text(
                        title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                    )
                }
                items(list, key = { "${it.first.market.key}-${it.first.s.id}" }) { (sv, agg) ->
                    val v = views.getValue(sv.market)
                    val sessionLabel = if (v.isTodaySession) "Today" else Fmt.dayTiny(v.session)
                    val all = period == StratPeriod.ALL
                    SystemCard(
                        s = sv.s, flag = sv.market.flag, ccy = v.ccy,
                        period = agg, periodLabel = if (period == StratPeriod.SESSION) sessionLabel else period.label,
                        second = if (all) sv.today else sv.total, secondLabel = if (all) sessionLabel else "All time",
                        spark = sv.spark, showR = state.settings.showR, onClick = { onOpenSystem(sv.market, sv.s.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

private fun sorted(list: List<Pair<SystemView, Agg>>, by: SortBy): List<Pair<SystemView, Agg>> = when (by) {
    SortBy.PNL -> list.sortedByDescending { it.second.pnl }
    SortBy.WIN -> list.sortedByDescending { it.second.winRate ?: -1.0 }
    SortBy.EXPECTANCY -> list.sortedByDescending { it.second.expectancyR ?: Double.NEGATIVE_INFINITY }
    SortBy.TRADES -> list.sortedByDescending { it.second.trades }
    SortBy.NAME -> list.sortedBy { it.first.s.name }
}
