package com.kalpande.algopulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.data.OpenDto
import com.kalpande.algopulse.data.SystemDto
import com.kalpande.algopulse.data.TradeDto
import com.kalpande.algopulse.data.parseZoned
import com.kalpande.algopulse.domain.Agg
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.ui.theme.AppTheme
import com.kalpande.algopulse.ui.theme.MonoStyle
import com.kalpande.algopulse.ui.theme.tnum
import java.time.ZoneId

@Composable
fun SideBadge(side: String) {
    val t = AppTheme.trade
    val long = side != "S"
    Box(
        Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background((if (long) t.profit else t.loss).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (long) "L" else "S", style = MaterialTheme.typography.labelMedium, color = if (long) t.profit else t.loss, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun outcomeColor(out: String?): Color {
    val t = AppTheme.trade
    val o = out.orEmpty().uppercase()
    return when {
        "TARGET" in o || "TRAIL" in o -> t.profit
        "STOP" in o -> t.loss
        else -> t.neutral
    }
}

/** One closed trade: symbol, side, times, outcome, R, P&L. */
@Composable
fun TradeRow(t: TradeDto, ccy: String, zone: ZoneId, showR: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val et = parseZoned(t.et, zone)
    val xt = parseZoned(t.xt, zone)
    var m = modifier.fillMaxWidth()
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(m.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        SideBadge(t.side)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.sym, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Spacer(Modifier.width(8.dp))
                if (!t.out.isNullOrBlank()) Pill(t.out.replace('_', ' '), outcomeColor(t.out))
            }
            Spacer(Modifier.height(2.dp))
            val span = if (et != null && xt != null && et.toLocalDate() != xt.toLocalDate()) {
                "${Fmt.dayTime(et)} → ${Fmt.dayTime(xt)}"
            } else "${Fmt.time(et)} → ${Fmt.time(xt)}"
            Text(
                listOfNotNull(span, t.tags.firstOrNull()).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall.tnum(),
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            PnlText(t.pnl ?: 0.0, ccy, style = MaterialTheme.typography.titleSmall)
            if (showR && t.r != null) RText(t.r) else if (t.ret != null) {
                Text(Fmt.pct(t.ret, 2, signed = true), style = MaterialTheme.typography.bodySmall.tnum(), color = pnlColor(t.ret))
            }
        }
    }
}

/** An open position. [stale] = left over from an earlier session (not booked). */
@Composable
fun OpenRow(o: OpenDto, systemName: String, ccy: String, zone: ZoneId, stale: Boolean, modifier: Modifier = Modifier) {
    val t = AppTheme.trade
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        SideBadge(o.side)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.sym, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                Text(systemName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            val since = parseZoned(o.et, zone)
            Text(
                buildString {
                    append("in ${Fmt.price(o.ep, ccy)}")
                    o.sl?.let { append(" · stop ${Fmt.price(o.stopNow ?: it, ccy)}") }
                    o.q?.let { append(" · ${Fmt.qty(it)} sh") }
                    if (since != null) append(" · ${Fmt.dayTime(since)}")
                },
                style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (stale) {
                Spacer(Modifier.height(3.dp))
                Pill("Not closed - from an earlier session", t.warn, icon = Icons.Rounded.WarningAmber)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            when {
                o.upnl != null -> PnlText(o.upnl, ccy, style = MaterialTheme.typography.titleSmall)
                else -> Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            if (o.r != null) RText(o.r) else if (o.last != null) {
                Text(Fmt.price(o.last, ccy), style = MaterialTheme.typography.bodySmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StyleBadge(style: String) {
    val (label, color) = when (style) {
        "swing" -> "Swing" to AppTheme.trade.accent2
        "hybrid" -> "Intraday + swing" to MaterialTheme.colorScheme.tertiary
        else -> "Intraday" to MaterialTheme.colorScheme.primary
    }
    Pill(label, color)
}

/**
 * Strategy list card. [period] is the selected period's totals (Today / Week / ... / All time);
 * the second figure is all-time, or the session when the period already is all-time.
 */
@Composable
fun SystemCard(
    s: SystemDto,
    flag: String,
    ccy: String,
    period: Agg,
    periodLabel: String,
    second: Agg,
    secondLabel: String,
    spark: List<Double>,
    showR: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flag, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.code, style = MonoStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StyleBadge(s.style)
                    if (s.status.level == "warn") Pill("attention", AppTheme.trade.warn)
                }
            }
            Sparkline(spark)
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat(periodLabel) { PnlText(period.pnl, ccy, style = MaterialTheme.typography.titleSmall, compact = true) }
            MiniStat(secondLabel) { PnlText(second.pnl, ccy, style = MaterialTheme.typography.titleSmall, compact = true) }
            if (s.isPortfolio) {
                MiniStat("Up days") {
                    Text(Fmt.rate(if (period.greenDays + period.redDays > 0) period.greenDays.toDouble() / (period.greenDays + period.redDays) else null),
                        style = MaterialTheme.typography.titleSmall.tnum())
                }
                MiniStat("Days") { Text("${period.days}", style = MaterialTheme.typography.titleSmall.tnum()) }
            } else {
                MiniStat("Win rate") { Text(Fmt.rate(period.winRate), style = MaterialTheme.typography.titleSmall.tnum()) }
                MiniStat(if (showR && period.rN > 0) "Avg R" else "Trades") {
                    if (showR && period.rN > 0) RText(period.expectancyR, style = MaterialTheme.typography.titleSmall)
                    else Text(Fmt.count(period.trades), style = MaterialTheme.typography.titleSmall.tnum())
                }
            }
        }
        // winners still held (LCR keeps them up to 5 sessions) are not in the realized figures above
        if (!s.isPortfolio && s.openPnl != null && s.open.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OpenPnlLine(s, ccy)
        }
    }
}

/** "Open (3): +Rs 11.5k · All-time incl. open: -Rs 30.9k" */
@Composable
fun OpenPnlLine(s: SystemDto, ccy: String) {
    val style = MaterialTheme.typography.labelMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Open (${s.open.size}) ", style = style, color = muted)
        PnlText(s.openPnl ?: 0.0, ccy, style = style, compact = true)
        Text("  ·  All-time incl. open ", style = style, color = muted)
        PnlText(s.pnlInclOpen ?: 0.0, ccy, style = style, compact = true, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniStat(label: String, value: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        value()
    }
}
