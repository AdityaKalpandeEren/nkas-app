package com.kalpande.algopulse

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.Scope
import com.kalpande.algopulse.data.Settings
import com.kalpande.algopulse.ui.MarketFilter
import com.kalpande.algopulse.ui.components.LocalReduceMotion
import com.kalpande.algopulse.ui.screens.CalendarScreen
import com.kalpande.algopulse.ui.screens.HealthScreen
import com.kalpande.algopulse.ui.screens.OverviewScreen
import com.kalpande.algopulse.ui.screens.SettingsScreen
import com.kalpande.algopulse.ui.screens.StrategiesScreen
import com.kalpande.algopulse.ui.screens.SystemDetailScreen
import com.kalpande.algopulse.ui.theme.AlgoPulseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * Renders every screen with the real sample dashboards (JVM, no emulator):
 *   ./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'
 * PNGs land in app/build/outputs/roborazzi/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h1900dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val state = SampleData.state()
    private val now = SampleData.NOW

    private fun snap(name: String, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                AlgoPulseTheme(dark) {
                    Surface(color = MaterialTheme.colorScheme.background) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Composable
    private fun overview(filter: MarketFilter, at: Instant = now) = OverviewScreen(
        state = state, now = at, filter = filter, onFilter = {}, scope = Scope.ALL, onScope = {}, onRefresh = {},
        onOpenSystem = { _, _ -> }, onOpenSettings = {}, onOpenHealth = {},
    )

    /** Real NSE dashboard of 2026-10-15 10:48 IST with open positions marked to market. */
    private fun openState(): com.kalpande.algopulse.ui.UiState {
        val bytes = javaClass.classLoader!!.getResourceAsStream("nse_open_sample.json.gz")!!.readBytes()
        val nse = com.kalpande.algopulse.data.MarketData(Market.NSE, com.kalpande.algopulse.data.DashboardRepository.parse(bytes))
        return state.copy(markets = state.markets + (Market.NSE to state.markets.getValue(Market.NSE).copy(data = nse)))
    }

    @Test fun strategiesOpenPnl() = snap("13_strategies_open_pnl") {
        StrategiesScreen(openState(), Instant.parse("2026-10-15T05:20:00Z"), MarketFilter.NSE, {}, Scope.ALL, {}) { _, _ -> }
    }

    @Test fun detailLcrOpenPnl() = snap("14_detail_nse_lcr_open_pnl") {
        SystemDetailScreen(openState(), Instant.parse("2026-10-15T05:20:00Z"), Market.NSE, "nse_lcr") {}
    }

    /** NSE dashboard with the V8 trend book from its 2026-10-16 dry run (6 buys, trailing stops). */
    private fun v8State(): com.kalpande.algopulse.ui.UiState {
        val bytes = javaClass.classLoader!!.getResourceAsStream("nse_v8_sample.json.gz")!!.readBytes()
        val nse = com.kalpande.algopulse.data.MarketData(Market.NSE, com.kalpande.algopulse.data.DashboardRepository.parse(bytes))
        return state.copy(markets = state.markets + (Market.NSE to state.markets.getValue(Market.NSE).copy(data = nse)))
    }

    @Test fun strategiesV8() = snap("15_strategies_nse_v8") {
        StrategiesScreen(v8State(), Instant.parse("2026-10-16T12:00:00Z"), MarketFilter.NSE, {}, Scope.ALL, {}) { _, _ -> }
    }

    @Test fun detailV8() = snap("16_detail_nse_v8") {
        SystemDetailScreen(v8State(), Instant.parse("2026-10-16T12:00:00Z"), Market.NSE, "nse_v8") {}
    }

    @Test fun overviewAllDark() = snap("01_overview_all_dark") { overview(MarketFilter.ALL) }

    @Test fun overviewUsDark() = snap("02_overview_us_dark") { overview(MarketFilter.US) }

    @Test fun overviewNseLight() = snap("03_overview_nse_light", dark = false) { overview(MarketFilter.NSE) }

    @Test fun strategiesDark() = snap("04_strategies_dark") {
        StrategiesScreen(state, now, MarketFilter.ALL, {}, Scope.ALL, {}) { _, _ -> }
    }

    @Test fun detailV3Dark() = snap("05_detail_us_v3_dark") {
        SystemDetailScreen(state, now, Market.US, "us_v3") {}
    }

    @Test fun detailScoreEngineLight() = snap("06_detail_score_engine_light", dark = false) {
        SystemDetailScreen(state, now, Market.US, "research_score_engine") {}
    }

    @Test fun detailNseV5Dark() = snap("07_detail_nse_v5_dark") {
        SystemDetailScreen(state, now, Market.NSE, "nse_v5") {}
    }

    @Test fun calendarNseDark() = snap("08_calendar_nse_dark") {
        CalendarScreen(state, now, Market.NSE, {}, Scope.ALL, {}) { _, _ -> }
    }

    @Test fun healthDark() = snap("09_health_dark") {
        HealthScreen(state, now, {}, {}, {})
    }

    @Test fun healthOutageDark() = snap("10_health_outage_dark") {
        HealthScreen(state, now.plusSeconds(41 * 3600), {}, {}, {})
    }

    @Test fun overviewOutageDark() = snap("11_overview_outage_dark") { overview(MarketFilter.ALL, now.plusSeconds(41 * 3600)) }

    @Test fun settingsLight() = snap("12_settings_light", dark = false) {
        SettingsScreen(Settings(), {}) {}
    }
}
