package com.kalpande.algopulse.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kalpande.algopulse.MainActivity
import com.kalpande.algopulse.data.DashboardRepository
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.MarketData
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Freshness
import com.kalpande.algopulse.domain.mergeDays
import com.kalpande.algopulse.domain.on
import java.time.Instant

/** Home-screen widget: each market's session P&L + a live/down dot. Reads the app's cached data. */
class PnlWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = DashboardRepository(context)
        val rows = Market.entries.map { it to repo.loadCached(it)?.data }
        val now = Instant.now()
        provideContent { GlanceTheme { Content(rows, now) } }
    }

    @Composable
    private fun Content(rows: List<Pair<Market, MarketData?>>, now: Instant) {
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(22.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("NKAS", style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp))
                Spacer(GlanceModifier.defaultWeight())
                Text("paper P&L", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
            }
            Spacer(GlanceModifier.height(6.dp))
            rows.forEach { (m, d) -> MarketRow(m, d, now) }
        }
    }

    @Composable
    private fun MarketRow(m: Market, d: MarketData?, now: Instant) {
        val pnl = d?.let { mergeDays(it.systems).on(it.sessionDay(now)).pnl }
        val fresh = Freshness.of(d?.generatedAt, now)
        val dot = when (fresh) {
            Freshness.LIVE -> Color(0xFF10B981)
            Freshness.DELAYED -> Color(0xFFF59E0B)
            Freshness.DOWN -> Color(0xFFF43F5E)
            Freshness.UNKNOWN -> Color(0xFF94A3B8)
        }
        val pnlColor = when {
            pnl == null -> GlanceTheme.colors.onSurfaceVariant
            pnl > 0 -> ColorProvider(Color(0xFF10B981))
            pnl < 0 -> ColorProvider(Color(0xFFF43F5E))
            else -> GlanceTheme.colors.onSurfaceVariant
        }
        Row(GlanceModifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("●", style = TextStyle(color = ColorProvider(dot), fontSize = 12.sp))
            Spacer(GlanceModifier.width(6.dp))
            Text("${m.flag} ${m.label}", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text(
                pnl?.let { Fmt.compact(it, d?.ccy ?: m.currency) } ?: "—",
                style = TextStyle(color = pnlColor, fontWeight = FontWeight.Bold, fontSize = 17.sp),
            )
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) = PnlWidget().updateAll(context)
    }
}

class PnlWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PnlWidget()
}
