package com.kalpande.algopulse.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.ThemeMode
import com.kalpande.algopulse.domain.Freshness
import com.kalpande.algopulse.ui.screens.CalendarScreen
import com.kalpande.algopulse.ui.screens.HealthScreen
import com.kalpande.algopulse.ui.screens.OverviewScreen
import com.kalpande.algopulse.ui.screens.SettingsScreen
import com.kalpande.algopulse.ui.screens.StrategiesScreen
import com.kalpande.algopulse.ui.screens.SystemDetailScreen
import com.kalpande.algopulse.ui.theme.AlgoPulseTheme
import kotlinx.coroutines.delay
import java.time.Instant

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    OVERVIEW("overview", "Overview", Icons.Rounded.SpaceDashboard),
    STRATEGIES("strategies", "Strategies", Icons.Rounded.Insights),
    CALENDAR("calendar", "Calendar", Icons.Rounded.CalendarMonth),
    HEALTH("health", "Health", Icons.Rounded.MonitorHeart),
}

@Composable
fun AppRoot(vm: MainViewModel, onDarkTheme: (Boolean) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val filter by vm.marketFilter.collectAsStateWithLifecycle()
    val scope by vm.scope.collectAsStateWithLifecycle()
    val tabRequest by vm.tabRequest.collectAsStateWithLifecycle()
    val dark = when (state.settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    LaunchedEffect(dark) { onDarkTheme(dark) }
    val now by produceState(Instant.now()) {
        while (true) {
            delay(15_000)
            value = Instant.now()
        }
    }
    // Refresh on open and every N seconds while the app is visible (never in the background).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val autoSec = state.settings.autoRefreshSec
    LaunchedEffect(lifecycle, autoSec) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.refresh(silent = true)
            while (autoSec > 0) {
                delay(autoSec * 1000L)
                vm.refresh(silent = true)
            }
        }
    }

    AlgoPulseTheme(dark) {
        val nav = rememberNavController()
        LaunchedEffect(tabRequest) {
            val t = tabRequest ?: return@LaunchedEffect
            Tab.entries.firstOrNull { it.route == t }?.let { nav.goTab(it) }
            vm.tabRequest.value = null
        }
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val showBar = Tab.entries.any { it.route == route }
        val anyDown = state.markets.values.any { ms ->
            ms.data != null && Freshness.of(ms.data.generatedAt, now) == Freshness.DOWN
        }
        var calendarMarket by rememberSaveable { mutableStateOf(filter.market ?: Market.US) }
        val openSystem: (Market, String) -> Unit = { m, id -> nav.navigate("system/${m.name}/$id") }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                if (showBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = route == t.route,
                                onClick = { nav.goTab(t) },
                                icon = {
                                    if (t == Tab.HEALTH && anyDown) {
                                        BadgedBox(badge = { Badge() }) { Icon(t.icon, t.label) }
                                    } else Icon(t.icon, t.label)
                                },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Tab.OVERVIEW.route,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(120)) },
            ) {
                composable(Tab.OVERVIEW.route) {
                    OverviewScreen(
                        state = state, now = now, filter = filter, onFilter = { vm.marketFilter.value = it },
                        scope = scope, onScope = { vm.scope.value = it }, onRefresh = { vm.refresh() },
                        onOpenSystem = openSystem, onOpenSettings = { nav.navigate("settings") },
                        onOpenHealth = { nav.goTab(Tab.HEALTH) },
                    )
                }
                composable(Tab.STRATEGIES.route) {
                    StrategiesScreen(
                        state = state, now = now, filter = filter, onFilter = { vm.marketFilter.value = it },
                        scope = scope, onScope = { vm.scope.value = it }, onOpenSystem = openSystem,
                    )
                }
                composable(Tab.CALENDAR.route) {
                    CalendarScreen(
                        state = state, now = now, market = calendarMarket, onMarket = { calendarMarket = it },
                        scope = scope, onScope = { vm.scope.value = it }, onOpenSystem = openSystem,
                    )
                }
                composable(Tab.HEALTH.route) {
                    HealthScreen(
                        state = state, now = now, onRefresh = { vm.refresh() }, onRunCheck = vm::runMonitorNow,
                        onOpenSettings = { nav.navigate("settings") },
                    )
                }
                composable("system/{market}/{id}") { e ->
                    val m = runCatching { Market.valueOf(e.arguments?.getString("market").orEmpty()) }.getOrDefault(Market.US)
                    Box(Modifier.navigationBarsPadding()) {
                        SystemDetailScreen(state, now, m, e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
                    }
                }
                composable("settings") {
                    Box(Modifier.navigationBarsPadding()) {
                        SettingsScreen(state.settings, vm::updateSettings, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}


private fun NavHostController.goTab(t: Tab) {
    navigate(t.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
