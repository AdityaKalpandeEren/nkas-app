package com.kalpande.algopulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.ui.theme.AppTheme
import com.kalpande.algopulse.ui.theme.tnum
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/**
 * Month grid coloured by daily P&L (intensity ~ sqrt of size so small days still show).
 * Tapping a day calls [onSelect].
 */
@Composable
fun MonthHeatmap(
    month: YearMonth,
    values: Map<LocalDate, Double>,
    ccy: String,
    today: LocalDate,
    modifier: Modifier = Modifier,
    selected: LocalDate? = null,
    onSelect: ((LocalDate) -> Unit)? = null,
    cellHeight: Dp = 54.dp,
    showAmounts: Boolean = true,
) {
    val trade = AppTheme.trade
    val cs = MaterialTheme.colorScheme
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val rows = (lead + days + 6) / 7
    val inMonth = values.filterKeys { YearMonth.from(it) == month }
    val maxAbs = max(inMonth.values.maxOfOrNull { abs(it) } ?: 0.0, 1e-9)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach {
                Text(
                    it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - lead + 1
                    Box(Modifier.weight(1f).height(cellHeight)) {
                        if (dayNum in 1..days) {
                            val date = month.atDay(dayNum)
                            val v = inMonth[date]
                            val intensity = if (v == null) 0f else (0.22f + 0.78f * sqrt(abs(v) / maxAbs).toFloat())
                            val bg = when {
                                v == null -> cs.surfaceContainerHigh.copy(alpha = if (date.dayOfWeek.value >= 6) 0.35f else 0.8f)
                                v > 0 -> trade.profit.copy(alpha = 0.16f + 0.62f * (intensity - 0.22f) / 0.78f)
                                v < 0 -> trade.loss.copy(alpha = 0.16f + 0.62f * (intensity - 0.22f) / 0.78f)
                                else -> cs.surfaceContainerHighest
                            }
                            val border = when {
                                date == selected -> cs.primary
                                date == today -> cs.onSurfaceVariant.copy(alpha = 0.7f)
                                else -> Color.Transparent
                            }
                            var m = Modifier
                                .fillMaxSize()
                                .padding(vertical = 2.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(bg)
                                .border(if (border == Color.Transparent) 0.dp else 1.5.dp, border, MaterialTheme.shapes.small)
                            if (onSelect != null) m = m.clickable { onSelect(date) }
                            Column(m.padding(horizontal = 4.dp, vertical = 3.dp)) {
                                Text(
                                    "$dayNum", style = MaterialTheme.typography.labelSmall,
                                    color = if (v == null) cs.onSurfaceVariant else cs.onSurface,
                                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Medium,
                                )
                                if (v != null && showAmounts && kotlin.math.abs(v) >= 0.5) {
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        Fmt.compact(v, ccy, signed = true).replace(Fmt.symbol(ccy), ""),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp).tnum(),
                                        color = cs.onSurface, maxLines = 1,
                                        modifier = Modifier.align(Alignment.End),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
