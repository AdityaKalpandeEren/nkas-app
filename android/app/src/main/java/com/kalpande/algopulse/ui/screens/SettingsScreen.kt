package com.kalpande.algopulse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kalpande.algopulse.BuildConfig
import com.kalpande.algopulse.R
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.Settings
import com.kalpande.algopulse.data.ThemeMode
import com.kalpande.algopulse.ui.components.AppCard
import com.kalpande.algopulse.ui.components.ScreenHeader
import com.kalpande.algopulse.ui.components.ScreenScaffold
import com.kalpande.algopulse.ui.components.SegmentedControl

private val spad = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)

@Composable
fun SettingsScreen(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Market?>(null) }
    val ctx = LocalContext.current
    ScreenScaffold {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
            item {
                ScreenHeader("Settings", navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } })
            }
            item {
                Group("Refresh") {
                    Text("Auto-refresh while the app is open", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    val opts = listOf(0, 60, 120, 300)
                    SegmentedControl(
                        listOf("Off", "1 min", "2 min", "5 min"), opts.indexOf(settings.autoRefreshSec).coerceAtLeast(0),
                        { i -> onUpdate { it.copy(autoRefreshSec = opts[i]) } }, Modifier.fillMaxWidth(), dense = true,
                    )
                }
            }
            item {
                Group("Outage alerts") {
                    Toggle("Background monitoring", "Notify when a bot stops publishing", settings.monitorEnabled) { v ->
                        onUpdate { it.copy(monitorEnabled = v) }
                    }
                    Text("Check every", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(6.dp))
                    val iv = listOf(15, 30, 60)
                    SegmentedControl(
                        listOf("15 min", "30 min", "60 min"), iv.indexOf(settings.monitorIntervalMin).coerceAtLeast(0),
                        { i -> onUpdate { it.copy(monitorIntervalMin = iv[i]) } }, Modifier.fillMaxWidth(), dense = true,
                    )
                    Text("Alert when no data for", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                    Spacer(Modifier.height(6.dp))
                    val st = listOf(15, 25, 45, 90)
                    SegmentedControl(
                        st.map { "$it min" }, st.indexOf(settings.staleAlertMin).coerceAtLeast(0),
                        { i -> onUpdate { it.copy(staleAlertMin = st[i]) } }, Modifier.fillMaxWidth(), dense = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Toggle("Failed workflow runs", "Notify about new failed runs", settings.failureAlerts) { v ->
                        onUpdate { it.copy(failureAlerts = v) }
                    }
                    Toggle("Daily summary", "Each market's paper P&L after its close", settings.dailySummary) { v ->
                        onUpdate { it.copy(dailySummary = v) }
                    }
                }
            }
            item {
                Group("Display") {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    SegmentedControl(
                        listOf("System", "Dark", "Light"), ThemeMode.entries.indexOf(settings.theme),
                        { i -> onUpdate { it.copy(theme = ThemeMode.entries[i]) } }, Modifier.fillMaxWidth(), dense = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Toggle("Show R-multiples", "Risk units next to money P&L", settings.showR) { v -> onUpdate { it.copy(showR = v) } }
                }
            }
            item {
                Group("Data sources") {
                    Market.entries.forEach { m ->
                        val url = settings.url(m)
                        Row(
                            Modifier.fillMaxWidth().clickable { editing = m }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${m.flag}  ${m.label} dashboard", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (url == m.defaultUrl) "Default · ${m.repo}" else url,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { editing = m }) { Text("Edit") }
                        }
                    }
                }
            }
            item {
                Group("About") {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Monitors the US and NSE paper-trading bots: P&L by day, week, month and year for every model and strategy, " +
                            "plus bot health. All figures are paper trades.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Market.entries.forEach { m ->
                        TextButton(onClick = {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${m.repo}/actions"))) }
                        }) { Text("${m.label} workflow runs on GitHub") }
                    }
                }
            }
        }
    }
    editing?.let { m ->
        var text by remember(m) { mutableStateOf(settings.url(m)) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("${m.label} dashboard URL") },
            text = {
                Column {
                    OutlinedTextField(text, { text = it }, singleLine = false, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { text = m.defaultUrl }) { Text("Reset to default") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate { if (m == Market.US) it.copy(usUrl = text) else it.copy(nseUrl = text) }
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(spad) {
        Text(
            title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp),
        )
        AppCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
