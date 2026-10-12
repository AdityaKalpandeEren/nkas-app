package com.kalpande.algopulse.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class Settings(
    val usUrl: String = Market.US.defaultUrl,
    val nseUrl: String = Market.NSE.defaultUrl,
    val autoRefreshSec: Int = 120,
    val monitorEnabled: Boolean = true,
    val monitorIntervalMin: Int = 15,
    val staleAlertMin: Int = 25,
    val failureAlerts: Boolean = true,
    val dailySummary: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val showR: Boolean = true,
) {
    fun url(m: Market): String = when (m) {
        Market.US -> usUrl
        Market.NSE -> nseUrl
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings + small bits of monitor state (what was already notified). */
class SettingsRepository(private val context: Context) {

    private object K {
        val usUrl = stringPreferencesKey("us_url")
        val nseUrl = stringPreferencesKey("nse_url")
        val autoRefresh = intPreferencesKey("auto_refresh_sec")
        val monitor = booleanPreferencesKey("monitor_enabled")
        val monitorInterval = intPreferencesKey("monitor_interval_min")
        val staleMin = intPreferencesKey("stale_alert_min")
        val failureAlerts = booleanPreferencesKey("failure_alerts")
        val dailySummary = booleanPreferencesKey("daily_summary")
        val theme = stringPreferencesKey("theme")
        val showR = booleanPreferencesKey("show_r")
    }

    private fun read(p: Preferences) = Settings(
        usUrl = p[K.usUrl] ?: Market.US.defaultUrl,
        nseUrl = p[K.nseUrl] ?: Market.NSE.defaultUrl,
        autoRefreshSec = p[K.autoRefresh] ?: 120,
        monitorEnabled = p[K.monitor] ?: true,
        monitorIntervalMin = p[K.monitorInterval] ?: 15,
        staleAlertMin = p[K.staleMin] ?: 25,
        failureAlerts = p[K.failureAlerts] ?: true,
        dailySummary = p[K.dailySummary] ?: true,
        theme = runCatching { ThemeMode.valueOf(p[K.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
        showR = p[K.showR] ?: true,
    )

    val flow: Flow<Settings> = context.dataStore.data.map(::read)

    suspend fun current(): Settings = flow.first()

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { p ->
            val cur = read(p)
            val n = transform(cur)
            p[K.usUrl] = n.usUrl.trim().ifBlank { Market.US.defaultUrl }
            p[K.nseUrl] = n.nseUrl.trim().ifBlank { Market.NSE.defaultUrl }
            p[K.autoRefresh] = n.autoRefreshSec
            p[K.monitor] = n.monitorEnabled
            p[K.monitorInterval] = n.monitorIntervalMin
            p[K.staleMin] = n.staleAlertMin
            p[K.failureAlerts] = n.failureAlerts
            p[K.dailySummary] = n.dailySummary
            p[K.theme] = n.theme.name
            p[K.showR] = n.showR
        }
    }

    // ── monitor bookkeeping (dedupe notifications) ──────────────────────────
    suspend fun getMark(key: String): String? = context.dataStore.data.first()[stringPreferencesKey("mark_$key")]

    suspend fun setMark(key: String, value: String) {
        context.dataStore.edit { it[stringPreferencesKey("mark_$key")] = value }
    }
}
