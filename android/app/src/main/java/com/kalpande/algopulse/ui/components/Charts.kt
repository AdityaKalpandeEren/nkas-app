package com.kalpande.algopulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalpande.algopulse.ui.theme.AppTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ChartPoint(val label: String, val value: Double)

private fun nearestIndex(x: Float, width: Float, n: Int): Int =
    if (n <= 1 || width <= 0f) 0 else (x / width * (n - 1)).roundToInt().coerceIn(0, n - 1)

/**
 * Smooth line / area chart for cumulative P&L with touch scrubbing. [onScrub] gets the
 * touched index (null on release) so a header can show the value under the finger.
 * [secondary] (same length) is drawn dashed - e.g. a benchmark.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    lineColor: Color? = null,
    secondary: List<Double>? = null,
    secondaryColor: Color = AppTheme.trade.accent2,
    baseline: Double? = 0.0,
    onScrub: ((Int?) -> Unit)? = null,
) {
    val trade = AppTheme.trade
    val gridColor = trade.grid
    val n = points.size
    val color = lineColor ?: if ((points.lastOrNull()?.value ?: 0.0) >= 0) trade.profit else trade.loss
    var sel by remember(points) { mutableStateOf<Int?>(null) }
    val scrub by rememberUpdatedState(onScrub)
    val reduce = LocalReduceMotion.current
    val reveal = remember(points) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(points) {
        if (!reduce) reveal.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    if (n < 2) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(
                if (n == 0) "No data yet" else "One day so far",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val values = points.map { it.value }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> sel = nearestIndex(o.x, size.width.toFloat(), n); scrub?.invoke(sel) },
                    onDragEnd = { sel = null; scrub?.invoke(null) },
                    onDragCancel = { sel = null; scrub?.invoke(null) },
                    onHorizontalDrag = { change, _ ->
                        sel = nearestIndex(change.position.x, size.width.toFloat(), n)
                        scrub?.invoke(sel)
                        change.consume()
                    },
                )
            }
            .pointerInput(points) {
                detectTapGestures(onPress = { o ->
                    sel = nearestIndex(o.x, size.width.toFloat(), n)
                    scrub?.invoke(sel)
                    tryAwaitRelease()
                    sel = null
                    scrub?.invoke(null)
                })
            },
    ) {
        val all = values + (secondary ?: emptyList()) + listOfNotNull(baseline)
        var lo = all.min()
        var hi = all.max()
        if (hi - lo < 1e-9) { hi += 1.0; lo -= 1.0 }
        val pad = (hi - lo) * 0.08
        lo -= pad; hi += pad
        val top = 6f
        val h = size.height - top - 6f
        fun x(i: Int) = i.toFloat() / (n - 1) * size.width
        fun y(v: Double) = top + ((hi - v) / (hi - lo) * h).toFloat()

        // light horizontal grid
        for (k in 1..3) {
            val gy = top + h * k / 4f
            drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        if (baseline != null && baseline in lo..hi) {
            drawLine(
                gridColor, Offset(0f, y(baseline)), Offset(size.width, y(baseline)), strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
            )
        }

        val line = smoothPath(values.indices.map { Offset(x(it), y(values[it])) })
        clipRect(right = size.width * reveal.value) {
            val area = Path().apply {
                addPath(line)
                lineTo(x(n - 1), size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.0f))))
            if (secondary != null && secondary.size == n) {
                drawPath(
                    smoothPath(secondary.indices.map { Offset(x(it), y(secondary[it])) }), secondaryColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))),
                )
            }
            drawPath(line, color, style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        val s = sel
        if (s != null) {
            val px = x(s)
            drawLine(color.copy(alpha = 0.6f), Offset(px, 0f), Offset(px, size.height), strokeWidth = 2f)
            drawCircle(color.copy(alpha = 0.25f), radius = 16f, center = Offset(px, y(values[s])))
            drawCircle(color, radius = 8f, center = Offset(px, y(values[s])))
            if (secondary != null && secondary.size == n) {
                drawCircle(secondaryColor, radius = 6f, center = Offset(px, y(secondary[s])))
            }
        } else {
            drawCircle(color, radius = 7f, center = Offset(x(n - 1), y(values.last())))
        }
    }
}

private fun smoothPath(pts: List<Offset>): Path = Path().apply {
    if (pts.isEmpty()) return@apply
    moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val p0 = pts[i - 1]
        val p1 = pts[i]
        val mx = (p0.x + p1.x) / 2f
        cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
    }
}

/** Diverging P&L bars (green up / red down) with tap selection and sparse axis labels. */
@Composable
fun BarChart(
    bars: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    selected: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    valueLabel: ((Double) -> String)? = null,
) {
    val trade = AppTheme.trade
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val n = bars.size
    if (n == 0) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodySmall, color = labelColor)
        }
        return
    }
    val reduce = LocalReduceMotion.current
    val grow = remember(bars) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(bars) { if (!reduce) grow.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(bars) {
                detectTapGestures { o ->
                    val slot = size.width.toFloat() / n
                    select?.invoke((o.x / slot).toInt().coerceIn(0, n - 1))
                }
            },
    ) {
        val labelH = 18.dp.toPx()
        val chartH = size.height - labelH
        val maxAbs = max(bars.maxOf { abs(it.value) }, 1e-9)
        val hasNeg = bars.any { it.value < 0 }
        val hasPos = bars.any { it.value > 0 }
        // Value labels sit beyond the bar ends, so the tallest bars must leave room for them
        val valueFmt = valueLabel?.takeIf { n <= 12 }
        val valueH = if (valueFmt != null) measurer.measure("0", TextStyle(fontSize = 10.sp)).size.height + 4f else 0f
        val plotTop = if (hasPos) valueH else 0f
        val plotH = chartH - plotTop - (if (hasNeg) valueH else 0f)
        val zeroY = when {
            hasPos && hasNeg -> {
                val maxPos = bars.maxOf { max(it.value, 0.0) }
                val maxNeg = bars.maxOf { max(-it.value, 0.0) }
                plotTop + (plotH * (maxPos / (maxPos + maxNeg))).toFloat()
            }
            hasNeg -> plotTop + 4f
            else -> plotTop + plotH - 1f
        }
        val scale = if (hasPos && hasNeg) {
            val maxPos = bars.maxOf { max(it.value, 0.0) }
            val maxNeg = bars.maxOf { max(-it.value, 0.0) }
            (plotH - 4f) / (maxPos + maxNeg)
        } else (plotH - 6f) / maxAbs
        val slot = size.width / n
        val bw = min(slot * 0.64f, 26.dp.toPx())
        drawLine(trade.grid, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 2f)
        bars.forEachIndexed { i, b ->
            val hPx = (abs(b.value) * scale * grow.value).toFloat().coerceAtLeast(if (b.value != 0.0) 3f else 0f)
            val cx = slot * i + slot / 2f
            val c = if (b.value >= 0) trade.profit else trade.loss
            val alpha = if (selected == null || selected == i) 1f else 0.38f
            val topLeft = if (b.value >= 0) Offset(cx - bw / 2, zeroY - hPx) else Offset(cx - bw / 2, zeroY)
            drawRoundRect(c.copy(alpha = alpha), topLeft, Size(bw, hPx), CornerRadius(min(bw / 2.5f, 8f)))
            if (valueFmt != null && b.value != 0.0) {
                val layout = measurer.measure(valueFmt(b.value), TextStyle(fontSize = 10.sp, color = c))
                val ty = if (b.value >= 0) (zeroY - hPx - layout.size.height - 2f).coerceAtLeast(0f)
                else (zeroY + hPx + 2f).coerceAtMost(chartH - layout.size.height)
                val tx = (cx - layout.size.width / 2f).coerceIn(0f, max(0f, size.width - layout.size.width))
                drawText(layout, topLeft = Offset(tx, ty))
            }
        }
        val step = max(1, (n / 6.0).roundToInt())
        val style = TextStyle(fontSize = 10.sp, color = labelColor)
        for (i in 0 until n step step) drawLabel(measurer, bars[i].label, slot * i + slot / 2f, size.height - labelH + 3f, style)
        if ((n - 1) % step != 0 && n > 1) drawLabel(measurer, bars[n - 1].label, slot * (n - 1) + slot / 2f, size.height - labelH + 3f, style)
    }
}

private fun DrawScope.drawLabel(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    cx: Float,
    top: Float,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style)
    val x = (cx - layout.size.width / 2f).coerceIn(0f, max(0f, size.width - layout.size.width))
    drawText(layout, topLeft = Offset(x, top))
}

/** Tiny trend line for list rows. */
@Composable
fun Sparkline(values: List<Double>, modifier: Modifier = Modifier.size(84.dp, 30.dp), color: Color? = null) {
    val trade = AppTheme.trade
    if (values.size < 2) {
        Box(modifier)
        return
    }
    val c = color ?: if (values.last() >= values.first()) trade.profit else trade.loss
    Canvas(modifier) {
        val lo = values.min()
        val hi = values.max().let { if (it - lo < 1e-9) lo + 1 else it }
        fun x(i: Int) = i.toFloat() / (values.size - 1) * size.width
        fun y(v: Double) = 2f + ((hi - v) / (hi - lo) * (size.height - 4f)).toFloat()
        val p = smoothPath(values.indices.map { Offset(x(it), y(values[it])) })
        val area = Path().apply {
            addPath(p); lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(area, Brush.verticalGradient(listOf(c.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(p, c, style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Horizontal bar centred on zero: green to the right, red to the left. */
@Composable
fun DivergingBar(value: Double, maxAbs: Double, modifier: Modifier = Modifier) {
    val trade = AppTheme.trade
    Canvas(modifier.height(8.dp)) {
        val mid = size.width / 2f
        drawRoundRect(trade.grid.copy(alpha = 0.6f), size = size, cornerRadius = CornerRadius(size.height / 2))
        if (maxAbs <= 0) return@Canvas
        val w = (abs(value) / maxAbs * mid).toFloat().coerceAtLeast(if (value != 0.0) 3f else 0f)
        val c = if (value >= 0) trade.profit else trade.loss
        val left = if (value >= 0) mid else mid - w
        drawRoundRect(c, Offset(left, 0f), Size(w, size.height), CornerRadius(size.height / 2))
        drawLine(trade.neutral.copy(alpha = 0.6f), Offset(mid, -2f), Offset(mid, size.height + 2f), strokeWidth = 2f)
    }
}

/** Win-rate style ring. */
@Composable
fun Ring(fraction: Float, modifier: Modifier = Modifier, color: Color = AppTheme.trade.profit, track: Color = AppTheme.trade.grid) {
    val reduce = LocalReduceMotion.current
    val anim = remember(fraction) { Animatable(if (reduce) fraction else 0f) }
    LaunchedEffect(fraction) { if (!reduce) anim.animateTo(fraction, tween(800, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        val stroke = size.minDimension * 0.12f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * anim.value.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round))
    }
}
