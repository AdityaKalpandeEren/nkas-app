package com.kalpande.algopulse.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kalpande.algopulse.data.HealthDto
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.data.parseInstant
import com.kalpande.algopulse.domain.Fmt
import com.kalpande.algopulse.domain.Freshness
import com.kalpande.algopulse.ui.MarketView
import com.kalpande.algopulse.ui.UiState
import com.kalpande.algopulse.ui.components.AppCard
import com.kalpande.algopulse.ui.components.LabelValue
import com.kalpande.algopulse.ui.components.Pill
import com.kalpande.algopulse.ui.components.ScreenHeader
import com.kalpande.algopulse.ui.components.ScreenScaffold
import com.kalpande.algopulse.ui.components.SectionHeader
import com.kalpande.algopulse.ui.components.StatusDot
import com.kalpande.algopulse.ui.components.freshnessColor
import com.kalpande.algopulse.ui.theme.AppTheme
import com.kalpande.algopulse.ui.theme.tnum
import java.time.Duration
import java.time.Instant

private val hpad = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@Composable
fun HealthScreen(
    state: UiState,
    now: Instant,
    onRefresh: () -> Unit,
    onRunCheck: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val minute = now.epochSecond / 60
    val views = remember(state.markets, minute) { Market.entries.map { MarketView(state[it], Scope.ALL, now) } }
    ScreenScaffold {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                ScreenHeader("Health", subtitle = overallLine(views)) {
                    RefreshButton(state.refreshing, onRefresh)
                }
            }
            views.forEach { v -> item(key = "h-${v.market.key}") { MarketHealthCard(v, now, state) } }
            item { AlertsCard(state, onRunCheck, onOpenSettings) }
            views.forEach { v ->
                if (v.data != null) item(key = "sys-${v.market.key}") { SystemsStatusCard(v, now) }
            }
        }
    }
}

private fun overallLine(views: List<MarketView>): String {
    val down = views.filter { it.freshness == Freshness.DOWN }
    val delayed = views.filter { it.freshness == Freshness.DELAYED }
    return when {
        down.isNotEmpty() -> "${down.joinToString { it.market.label }} not publishing"
        delayed.isNotEmpty() -> "${delayed.joinToString { it.market.label }} delayed"
        views.all { it.freshness == Freshness.LIVE } -> "All bots live"
        else -> "Waiting for data"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarketHealthCard(v: MarketView, now: Instant, state: UiState) {
    val t = AppTheme.trade
    val ctx = LocalContext.current
    val c = freshnessColor(v.freshness)
    val h: HealthDto? = v.data?.dto?.health
    val ms = state[v.market]
    AppCard(hpad.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(c, pulsing = v.freshness == Freshness.LIVE, size = 10.dp)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text("${v.market.flag}  ${v.market.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    when (v.freshness) {
                        Freshness.LIVE -> "Live - publishing every run"
                        Freshness.DELAYED -> "Delayed - last publish ${v.ageText}"
                        Freshness.DOWN -> "DOWN - nothing for ${Fmt.duration(Fmt.ageMinutes(v.generatedAt, now) ?: 0)}"
                        Freshness.UNKNOWN -> if (ms.notPublished) "Waiting for the first publish" else "No data"
                    },
                    style = MaterialTheme.typography.bodySmall, color = c, fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))
        LabelValue("Data published", v.generatedAt?.let { "${Fmt.clock(it)} · ${v.ageText}" } ?: "—")
        if (h != null) {
            val lastRun = parseInstant(h.lastRunAt)
            LabelValue("Last workflow run", lastRun?.let { "${Fmt.clock(it)} · ${Fmt.relative(it, now)}" } ?: "—")
            val fails = (h.runs["failure"] ?: 0) + (h.runs["timed_out"] ?: 0) + (h.runs["startup_failure"] ?: 0)
            LabelValue(
                "Runs (last ${h.windowH ?: 3} h)",
                "${h.total ?: 0} · ${h.runs["success"] ?: 0} ok · $fails failed · ${h.runs["cancelled"] ?: 0} cancelled",
                valueColor = if (fails > 0) t.loss else MaterialTheme.colorScheme.onSurface,
            )
            if (h.medianGapMin != null) {
                val gapWarn = (h.maxGapMin ?: 0.0) > 10
                LabelValue(
                    "Trigger cadence",
                    "every ${Fmt.decimal(h.medianGapMin, 1)} min · max gap ${Fmt.decimal(h.maxGapMin, 1)} min",
                    valueColor = if (gapWarn) t.warn else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (h.lastJobs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Jobs in the last finished run", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    h.lastJobs.sortedBy { it.name }.forEach { j ->
                        val (icon, col) = when (j.result) {
                            "success" -> Icons.Rounded.CheckCircle to t.profit
                            "skipped" -> Icons.Rounded.RemoveCircle to t.neutral
                            "cancelled" -> Icons.Rounded.RemoveCircle to t.warn
                            null, "in_progress", "queued" -> Icons.Rounded.RemoveCircle to t.neutral
                            else -> Icons.Rounded.Error to t.loss
                        }
                        val secs = runCatching { Duration.between(Instant.parse(j.started), Instant.parse(j.done)).seconds }.getOrNull()
                        Pill("${j.name}${secs?.let { " · ${it}s" } ?: ""}", col, icon = icon, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (h.failures.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Recent failed runs", style = MaterialTheme.typography.labelMedium, color = t.loss)
                h.failures.reversed().forEach { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(enabled = f.url != null) {
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(f.url))) }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Error, null, tint = t.loss, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${Fmt.clock(parseInstant(f.at))} · ${f.result ?: "failed"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        if (f.url != null) Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open run", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        v.data?.dto?.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
            Spacer(Modifier.height(8.dp))
            notes.forEach { Pill(it, t.warn, icon = Icons.Rounded.WarningAmber) }
        }
        Spacer(Modifier.height(6.dp))
        val fetched = ms.fetchedAt?.let { "downloaded ${Fmt.relative(it, now)} · ${ms.bytes / 1024} KB" } ?: "not downloaded yet"
        Text(
            if (ms.error != null) "Last download failed: ${ms.error}" else fetched,
            style = MaterialTheme.typography.labelSmall,
            color = if (ms.error != null) t.loss else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlertsCard(state: UiState, onRunCheck: () -> Unit, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val preview = LocalInspectionMode.current
    var granted by remember {
        mutableStateOf(
            preview || Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val s = state.settings
    AppCard(hpad.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Outage alerts", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (!s.monitorEnabled) "Off - turn on in Settings"
                    else "Checks every ${s.monitorIntervalMin} min · alerts after ${s.staleAlertMin} min without data" +
                        if (s.failureAlerts) " or a failed run" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!granted) {
                Button(onClick = { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Allow notifications")
                }
            } else {
                Button(onClick = onRunCheck, enabled = s.monitorEnabled) { Text("Run a check now") }
            }
            OutlinedButton(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun SystemsStatusCard(v: MarketView, now: Instant) {
    val t = AppTheme.trade
    AppCard(hpad.fillMaxWidth()) {
        SectionHeader("${v.market.flag}  ${v.market.label} systems", subtitle = "State each job saved, newest first")
        Spacer(Modifier.height(4.dp))
        v.systems.sortedByDescending { it.status.updated ?: "" }.forEach { s ->
            val leftover = v.isLeftover(s) && s.open.isNotEmpty()
            val dot: Color = when {
                leftover || s.status.level == "warn" -> t.warn
                s.status.level == "error" -> t.loss
                else -> t.live
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.padding(top = 6.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.size(8.dp).clip(MaterialTheme.shapes.extraSmall).background(dot))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            parseInstant(s.status.updated)?.let { Fmt.relative(it, now) } ?: "—",
                            style = MaterialTheme.typography.labelSmall.tnum(), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    listOfNotNull(s.status.text, s.status.note).forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (leftover) {
                        Text(
                            "${s.open.size} position(s) still open from ${s.status.asof} - not closed by the job",
                            style = MaterialTheme.typography.bodySmall, color = t.warn,
                        )
                    }
                }
            }
        }
    }
}
