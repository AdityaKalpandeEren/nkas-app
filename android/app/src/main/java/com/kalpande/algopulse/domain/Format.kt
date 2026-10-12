package com.kalpande.algopulse.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Number / money / date formatting. INR uses Indian grouping (1,23,45,678) and L / Cr. */
object Fmt {
    const val MINUS = "−"

    fun symbol(ccy: String): String = when (ccy.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        else -> "$ccy "
    }

    private fun indian(ccy: String) = ccy.equals("INR", ignoreCase = true)

    /** Groups the integer part: western 1,234,567 or Indian 12,34,567. */
    fun groupDigits(n: Long, indian: Boolean): String {
        val s = abs(n).toString()
        if (s.length <= 3) return s
        if (!indian) return s.reversed().chunked(3).joinToString(",").reversed()
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val restGrouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$restGrouped,$last3"
    }

    /** |v| with grouping and a fixed number of decimals (no sign). */
    fun number(v: Double, decimals: Int, indian: Boolean = false): String {
        val a = abs(v)
        val scale = Math.pow(10.0, decimals.toDouble())
        val scaled = (a * scale).roundToLong()
        val intPart = scaled / scale.toLong()
        val frac = scaled % scale.toLong()
        val g = groupDigits(intPart, indian)
        return if (decimals > 0) "$g.${frac.toString().padStart(decimals, '0')}" else g
    }

    private fun sign(v: Double, signed: Boolean, decimals: Int): String {
        val rounded = abs(v) * Math.pow(10.0, decimals.toDouble())
        val isZero = rounded < 0.5
        return when {
            isZero -> ""
            v < 0 -> MINUS
            signed -> "+"
            else -> ""
        }
    }

    fun defaultDecimals(v: Double): Int = if (abs(v) >= 100) 0 else 2

    fun money(v: Double, ccy: String, signed: Boolean = false, decimals: Int = defaultDecimals(v)): String =
        sign(v, signed, decimals) + symbol(ccy) + number(v, decimals, indian(ccy))

    /** $1.2k / $3.4M ; ₹12.3k / ₹1.25L / ₹3.10Cr */
    fun compact(v: Double, ccy: String, signed: Boolean = true): String {
        val a = abs(v)
        val body = if (indian(ccy)) when {
            a >= 1e7 -> String.format(Locale.US, "%.2fCr", a / 1e7)
            a >= 1e5 -> String.format(Locale.US, "%.2fL", a / 1e5)
            a >= 1e3 -> String.format(Locale.US, "%.1fk", a / 1e3)
            else -> number(a, 0, true)
        } else when {
            a >= 1e9 -> String.format(Locale.US, "%.2fB", a / 1e9)
            a >= 1e6 -> String.format(Locale.US, "%.2fM", a / 1e6)
            a >= 1e3 -> String.format(Locale.US, "%.1fk", a / 1e3)
            else -> number(a, 0, false)
        }
        return sign(v, signed, 0) + symbol(ccy) + body
    }

    fun price(v: Double?, ccy: String): String {
        if (v == null) return "—"
        val d = if (abs(v) < 1) 4 else 2
        return symbol(ccy) + number(v, d, indian(ccy))
    }

    fun r(v: Double?, signed: Boolean = true): String {
        if (v == null) return "—"
        return sign(v, signed, 2) + String.format(Locale.US, "%.2f", abs(v)) + "R"
    }

    fun pct(v: Double?, decimals: Int = 1, signed: Boolean = false): String {
        if (v == null) return "—"
        return sign(v, signed, decimals) + String.format(Locale.US, "%.${decimals}f", abs(v)) + "%"
    }

    /** A 0..1 fraction as a percentage. */
    fun rate(v: Double?): String = if (v == null) "—" else String.format(Locale.US, "%.0f%%", v * 100)

    fun ratio(v: Double?): String = when {
        v == null -> "—"
        v.isInfinite() -> "∞"
        else -> String.format(Locale.US, "%.2f", v)
    }

    fun decimal(v: Double?, decimals: Int = 2): String =
        if (v == null) "—" else sign(v, false, decimals) + String.format(Locale.US, "%.${decimals}f", abs(v))

    fun count(n: Int): String = groupDigits(n.toLong(), false)

    /** 1,234 -> 1.2k (for tight spaces). */
    fun countCompact(n: Int): String = if (n >= 1000) String.format(Locale.US, "%.1fk", n / 1000.0) else n.toString()

    /** R with fewer decimals as it grows: 1.25R, 12.4R, 147R. */
    fun rShort(v: Double?): String {
        if (v == null) return "—"
        val a = abs(v)
        val d = when { a >= 100 -> 0; a >= 10 -> 1; else -> 2 }
        return sign(v, true, d) + String.format(Locale.US, "%.${d}f", a) + "R"
    }

    fun qty(v: Double?): String = when {
        v == null -> "—"
        v == Math.floor(v) -> groupDigits(v.toLong(), false)
        else -> String.format(Locale.US, "%.2f", v)
    }

    // ─── time ────────────────────────────────────────────────────────────

    fun relative(at: Instant?, now: Instant): String {
        if (at == null) return "never"
        val s = Duration.between(at, now).seconds
        return when {
            s < 45 -> "just now"
            s < 90 -> "1 min ago"
            s < 3600 -> "${s / 60} min ago"
            s < 2 * 3600 -> "1 h ago"
            s < 86400 -> "${s / 3600} h ago"
            s < 2 * 86400 -> "yesterday"
            else -> "${s / 86400} d ago"
        }
    }

    fun ageMinutes(at: Instant?, now: Instant): Long? = at?.let { Duration.between(it, now).toMinutes() }

    fun duration(minutes: Long): String = when {
        minutes < 60 -> "$minutes min"
        minutes < 48 * 60 -> "${minutes / 60} h ${minutes % 60} min"
        else -> "${minutes / 1440} d ${(minutes % 1440) / 60} h"
    }

    private val dayMed = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val dayTiny = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)
    private val dayShortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayLong = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val dayNum = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val hm = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val dayHm = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)

    fun day(d: LocalDate): String = d.format(dayMed)
    fun dayTiny(d: LocalDate): String = d.format(dayTiny)
    fun dayShort(d: LocalDate): String = d.format(dayShortFmt)
    fun dayLong(d: LocalDate): String = d.format(dayLong)
    fun dayNum(d: LocalDate): String = d.format(dayNum)
    fun month(d: LocalDate): String = d.format(monthYear)
    fun time(z: ZonedDateTime?): String = z?.format(hm) ?: "—"
    fun dayTime(z: ZonedDateTime?): String = z?.format(dayHm) ?: "—"
    fun clock(at: Instant?, zone: ZoneId = ZoneId.systemDefault()): String = at?.atZone(zone)?.format(dayHm) ?: "—"
}
