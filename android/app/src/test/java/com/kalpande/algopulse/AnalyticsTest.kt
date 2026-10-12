package com.kalpande.algopulse

import com.kalpande.algopulse.data.DayDto
import com.kalpande.algopulse.data.Market
import com.kalpande.algopulse.data.SystemDto
import com.kalpande.algopulse.domain.Period
import com.kalpande.algopulse.domain.bucketize
import com.kalpande.algopulse.domain.maxDrawdown
import com.kalpande.algopulse.domain.mergeDays
import com.kalpande.algopulse.domain.on
import com.kalpande.algopulse.domain.summary
import com.kalpande.algopulse.domain.systemDays
import com.kalpande.algopulse.domain.total
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsTest {
    private val a = SystemDto(
        id = "a", days = listOf(
            DayDto("2026-10-05", n = 2, w = 1, l = 1, pnl = 100.0, gw = 300.0, gl = -200.0, r = 0.5, rw = 1.5, rl = -1.0, r2 = 3.25, rn = 2, best = 300.0, worst = -200.0),
            DayDto("2026-10-06", n = 1, w = 0, l = 1, pnl = -400.0, gw = 0.0, gl = -400.0, r = -2.0, rw = 0.0, rl = -2.0, r2 = 4.0, rn = 1, best = -400.0, worst = -400.0),
            DayDto("2026-10-13", n = 1, w = 1, l = 0, pnl = 250.0, gw = 250.0, gl = 0.0, r = 1.0, rw = 1.0, rl = 0.0, r2 = 1.0, rn = 1, best = 250.0, worst = 250.0),
        ),
    )
    private val portfolio = SystemDto(
        id = "p", kind = "portfolio",
        days = listOf(DayDto("2026-10-05", n = 20, pnl = -50.0), DayDto("2026-10-06", n = 0, pnl = 80.0)),
    )

    @Test
    fun totalsAndRatios() {
        val t = systemDays(a).total()
        assertEquals(4, t.trades)
        assertEquals(-50.0, t.pnl, 1e-9)
        assertEquals(0.5, t.winRate!!, 1e-9)
        assertEquals(550.0 / 600.0, t.profitFactor!!, 1e-9)
        assertEquals(300.0, t.best!!, 1e-9)
        assertEquals(-400.0, t.worst!!, 1e-9)
        assertEquals(-0.125, t.expectancyR!!, 1e-9)
        assertNotNull(t.tStatR)
        assertEquals(2, t.greenDays)
        assertEquals(1, t.redDays)
    }

    @Test
    fun portfolioDaysAddPnlButNotTrades() {
        val merged = mergeDays(listOf(a, portfolio))
        val d5 = merged.on(LocalDate.parse("2026-10-05"))
        assertEquals(2, d5.trades)               // portfolio fills are not round trips
        assertEquals(50.0, d5.pnl, 1e-9)
        assertEquals(1, d5.days)
        assertEquals(1, d5.greenDays)
    }

    @Test
    fun buckets() {
        val days = systemDays(a)
        val weeks = days.bucketize(Period.WEEK)
        assertEquals(2, weeks.size)
        assertEquals(LocalDate.parse("2026-10-05"), weeks[0].start)
        assertEquals(-300.0, weeks[0].agg.pnl, 1e-9)
        assertEquals(1, days.bucketize(Period.MONTH).size)
        val s = days.summary(LocalDate.parse("2026-10-13"))
        assertEquals(250.0, s.today.pnl, 1e-9)
        assertEquals(250.0, s.week.pnl, 1e-9)
        assertEquals(-50.0, s.month.pnl, 1e-9)
    }

    @Test
    fun drawdown() {
        assertEquals(-400.0, systemDays(a).maxDrawdown(), 1e-9)
    }

    @Test
    fun parsesRealSamples() {
        for (m in Market.entries) {
            val d = SampleData.market(m)
            assertTrue("${m.label} systems", d.systems.size >= 8)
            assertNotNull(d.generatedAt)
            val total = mergeDays(d.systems).total()
            assertTrue("${m.label} has trades", total.trades > 100)
            assertTrue(d.systems.any { it.isPortfolio })
        }
        val us = SampleData.market(Market.US)
        val v3 = us.system("us_v3")!!
        assertEquals(19, v3.trades.size)
        assertEquals(-10993.5, systemDays(v3).total().pnl, 0.01)
    }
}
