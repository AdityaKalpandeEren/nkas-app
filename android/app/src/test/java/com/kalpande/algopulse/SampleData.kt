package com.kalpande.algopulse

import com.kalpande.algopulse.data.DashboardRepository
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.MarketData
import com.kalpande.algopulse.ui.MarketState
import com.kalpande.algopulse.ui.UiState
import java.time.Instant

/** Real (trimmed) dashboards exported on 2026-10-12, used by unit + screenshot tests. */
object SampleData {
    /** 2 min after the samples were published: US pre-market (last session Fri 9 Oct), NSE mid-session. */
    val NOW: Instant = Instant.parse("2026-10-12T08:30:00Z")

    fun market(m: Market): MarketData {
        val bytes = javaClass.classLoader!!.getResourceAsStream("${m.key}_sample.json.gz")!!.readBytes()
        return MarketData(m, DashboardRepository.parse(bytes))
    }

    fun state(): UiState = UiState(
        markets = Market.entries.associateWith { m ->
            MarketState(m, data = market(m), fetchedAt = NOW.minusSeconds(30), bytes = 60_000)
        },
        loaded = true,
    )
}
