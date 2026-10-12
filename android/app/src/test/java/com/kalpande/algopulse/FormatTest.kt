package com.kalpande.algopulse

import com.kalpande.algopulse.domain.Fmt
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun indianGrouping() {
        assertEquals("1,23,45,678", Fmt.groupDigits(12345678, indian = true))
        assertEquals("12,345,678", Fmt.groupDigits(12345678, indian = false))
        assertEquals("999", Fmt.groupDigits(999, indian = true))
        assertEquals("1,000", Fmt.groupDigits(1000, indian = true))
    }

    @Test
    fun money() {
        assertEquals("₹3,03,264", Fmt.money(303263.79, "INR"))
        assertEquals("−$1,206", Fmt.money(-1206.0, "USD", signed = true))
        assertEquals("+$12.50", Fmt.money(12.5, "USD", signed = true))
        assertEquals("$0.00", Fmt.money(-0.001, "USD", signed = true))
    }

    @Test
    fun compact() {
        assertEquals("−₹3.03L", Fmt.compact(-303263.79, "INR"))
        assertEquals("+₹1.25Cr", Fmt.compact(12_500_000.0, "INR"))
        assertEquals("+$13.9k", Fmt.compact(13869.55, "USD"))
        assertEquals("−$197.5k", Fmt.compact(-197460.94, "USD"))
        assertEquals("$0", Fmt.compact(0.2, "USD"))
    }

    @Test
    fun rAndPct() {
        assertEquals("+1.25R", Fmt.r(1.249))
        assertEquals("−0.50R", Fmt.r(-0.5))
        assertEquals("57%", Fmt.rate(0.567))
        assertEquals("+2.31%", Fmt.pct(2.313, 2, signed = true))
    }
}
