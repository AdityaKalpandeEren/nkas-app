package com.kalpande.algopulse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kalpande.algopulse.data.DashboardRepository
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.MarketData
import com.kalpande.algopulse.data.NotPublishedException
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.data.Settings
import com.kalpande.algopulse.data.SettingsRepository
import com.kalpande.algopulse.widget.PnlWidget
import com.kalpande.algopulse.work.Monitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class MarketState(
    val market: Market,
    val data: MarketData? = null,
    val fetchedAt: Instant? = null,
    val bytes: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val notPublished: Boolean = false,
)

data class UiState(
    val markets: Map<Market, MarketState> = Market.entries.associateWith { MarketState(it) },
    val refreshing: Boolean = false,
    val settings: Settings = Settings(),
    val loaded: Boolean = false,
) {
    operator fun get(m: Market): MarketState = markets[m] ?: MarketState(m)
    val anyData: Boolean get() = markets.values.any { it.data != null }
}

enum class MarketFilter(val label: String, val market: Market?) {
    ALL("All", null),
    US("${Market.US.flag}  US", Market.US),
    NSE("${Market.NSE.flag}  NSE", Market.NSE),
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DashboardRepository(app)
    private val settingsRepo = SettingsRepository(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Selections shared by the tabs. */
    val marketFilter = MutableStateFlow(MarketFilter.ALL)
    val scope = MutableStateFlow(Scope.ALL)

    /** Tab requested by a notification tap (consumed by the UI). */
    val tabRequest = MutableStateFlow<String?>(null)

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepo.flow.collect { s -> _state.update { it.copy(settings = s) } }
        }
        viewModelScope.launch {
            for (m in Market.entries) {
                val cached = repo.loadCached(m) ?: continue
                setMarket(m) { if (it.data == null) it.copy(data = cached.data, fetchedAt = cached.fetchedAt, bytes = cached.bytes) else it }
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    private fun setMarket(m: Market, f: (MarketState) -> MarketState) {
        _state.update { s -> s.copy(markets = s.markets + (m to f(s[m]))) }
    }

    fun refresh(silent: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            if (!silent) _state.update { it.copy(refreshing = true) }
            val s = settingsRepo.current()
            Market.entries.map { m -> async { fetchOne(m, s.url(m)) } }.awaitAll()
            _state.update { it.copy(refreshing = false) }
            runCatching { PnlWidget.refreshAll(getApplication()) }
        }
    }

    private suspend fun fetchOne(m: Market, url: String) {
        setMarket(m) { it.copy(loading = true) }
        try {
            val l = repo.fetch(m, url)
            setMarket(m) {
                it.copy(data = l.data, fetchedAt = l.fetchedAt, bytes = l.bytes, loading = false, error = null, notPublished = false)
            }
        } catch (e: NotPublishedException) {
            setMarket(m) { it.copy(loading = false, notPublished = it.data == null, error = if (it.data != null) e.message else null) }
        } catch (e: Exception) {
            setMarket(m) { it.copy(loading = false, error = e.message ?: e.javaClass.simpleName) }
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            val before = settingsRepo.current()
            settingsRepo.update(transform)
            val after = settingsRepo.current()
            runCatching { Monitor.schedule(getApplication(), after) }
            if (before.usUrl != after.usUrl || before.nseUrl != after.nseUrl) refresh()
        }
    }

    fun runMonitorNow() {
        runCatching { Monitor.runNow(getApplication()) }
    }
}
