package com.kalpande.algopulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Freshness
import com.kalpande.algopulse.ui.theme.AppTheme
import com.kalpande.algopulse.ui.theme.tnum

/** True in screenshot tests (and could back a "reduce motion" setting): no infinite / entry animations. */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    var m = modifier.clip(shape)
    m = if (brush != null) m.background(brush) else m.background(container)
    m = m.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun pnlColor(value: Double?): Color {
    val t = AppTheme.trade
    return when {
        value == null -> MaterialTheme.colorScheme.onSurfaceVariant
        value > 0.0049 -> t.profit
        value < -0.0049 -> t.loss
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun PnlText(
    value: Double,
    ccy: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    compact: Boolean = false,
    signed: Boolean = true,
    fontWeight: FontWeight? = null,
    animate: Boolean = false,
) {
    val reduce = LocalReduceMotion.current
    val shown = if (animate && !reduce) {
        val v by animateFloatAsState(value.toFloat(), tween(650), label = "pnl")
        v.toDouble()
    } else value
    Text(
        text = if (compact) Fmt.compact(shown, ccy, signed) else Fmt.money(shown, ccy, signed),
        style = style.tnum(),
        color = pnlColor(value),
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun RText(value: Double?, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall) {
    Text(Fmt.r(value), style = style.tnum(), color = pnlColor(value), modifier = modifier, maxLines = 1)
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    container: Color = color.copy(alpha = 0.14f),
    icon: ImageVector? = null,
    style: TextStyle = MaterialTheme.typography.labelSmall,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = style, color = color, maxLines = 1)
    }
}

@Composable
fun StatusDot(color: Color, pulsing: Boolean, size: Dp = 8.dp) {
    val reduce = LocalReduceMotion.current
    Box(Modifier.size(size * 2.2f), contentAlignment = Alignment.Center) {
        if (pulsing && !reduce) {
            val inf = rememberInfiniteTransition(label = "pulse")
            val scale by inf.animateFloat(1f, 2.2f, infiniteRepeatable(tween(1500), RepeatMode.Restart), label = "s")
            val alpha by inf.animateFloat(0.55f, 0f, infiniteRepeatable(tween(1500), RepeatMode.Restart), label = "a")
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .background(color, CircleShape),
            )
        }
        Box(Modifier.size(size).background(color, CircleShape))
    }
}

@Composable
fun freshnessColor(f: Freshness): Color {
    val t = AppTheme.trade
    return when (f) {
        Freshness.LIVE -> t.live
        Freshness.DELAYED -> t.warn
        Freshness.DOWN -> t.down
        Freshness.UNKNOWN -> t.neutral
    }
}

@Composable
fun FreshnessPill(f: Freshness, ageText: String, modifier: Modifier = Modifier) {
    val c = freshnessColor(f)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.13f))
            .padding(start = 2.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(c, pulsing = f == Freshness.LIVE, size = 7.dp)
        Text("${f.label} · $ageText", style = MaterialTheme.typography.labelSmall, color = c, maxLines = 1)
    }
}

/** Pill-style segmented control (market, period, range pickers). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(cs.surfaceContainerHigh)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) cs.primary.copy(alpha = 0.16f) else Color.Transparent, label = "segbg")
            val fg by animateColorAsState(if (sel) cs.primary else cs.onSurfaceVariant, label = "segfg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = if (dense) 6.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = if (dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    color = fg,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun <T> ChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(items, key = { label(it) }) { item ->
            val sel = item == selected
            FilterChip(
                selected = sel,
                onClick = { onSelect(item) },
                label = { Text(label(item), maxLines = 1) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = sel,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    sub: String? = null,
    subColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.tnum(), color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) {
            Spacer(Modifier.height(1.dp))
            Text(sub, style = MaterialTheme.typography.labelSmall.tnum(), color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.tnum(), color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Shimmer(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val alpha = if (reduce) 0.5f else {
        val inf = rememberInfiniteTransition(label = "shimmer")
        val a by inf.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
        a
    }
    Box(modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)))
}
