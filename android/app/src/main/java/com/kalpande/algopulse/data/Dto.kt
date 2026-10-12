package com.kalpande.algopulse.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The dashboard JSON (schema 1) written by `dashboard/export.py` in each bot repo and
 * published as the `dashboard-data` release asset. Field names are kept short on the
 * wire; every field has a default so older / newer exporters never break parsing.
 */
@Serializable
data class DashboardDto(
    val schema: Int = 1,
    val market: String = "",
    val ccy: String = "USD",
    val tz: String = "UTC",
    val session: SessionDto = SessionDto(),
    val bench: String? = null,
    @SerialName("generated_at") val generatedAt: String = "",
    val repo: String? = null,
    @SerialName("run_id") val runId: Long? = null,
    val health: HealthDto = HealthDto(),
    val notes: List<String> = emptyList(),
    val sources: Map<String, String> = emptyMap(),
    val systems: List<SystemDto> = emptyList(),
)

@Serializable
data class SessionDto(val open: String = "09:30", val close: String = "16:00")

@Serializable
data class HealthDto(
    @SerialName("window_h") val windowH: Int? = null,
    val runs: Map<String, Int> = emptyMap(),
    val total: Int? = null,
    @SerialName("last_run_at") val lastRunAt: String? = null,
    @SerialName("max_gap_min") val maxGapMin: Double? = null,
    @SerialName("median_gap_min") val medianGapMin: Double? = null,
    @SerialName("last_jobs") val lastJobs: List<JobDto> = emptyList(),
    @SerialName("last_jobs_run") val lastJobsRun: Long? = null,
    val failures: List<FailureDto> = emptyList(),
)

@Serializable
data class JobDto(
    val name: String = "",
    val result: String? = null,
    val started: String? = null,
    val done: String? = null,
)

@Serializable
data class FailureDto(
    val id: Long = 0,
    val at: String = "",
    val result: String? = null,
    val url: String? = null,
)

@Serializable
data class SystemDto(
    val id: String,
    val name: String = "",
    val code: String = "",
    val group: String = "Models",
    val style: String = "intraday",
    val kind: String = "trades",
    val desc: String = "",
    val src: String? = null,
    val since: String? = null,
    @SerialName("risk_unit") val riskUnit: Double? = null,
    val status: StatusDto = StatusDto(),
    val days: List<DayDto> = emptyList(),
    val trades: List<TradeDto> = emptyList(),
    @SerialName("trades_from") val tradesFrom: String? = null,
    val open: List<OpenDto> = emptyList(),
    // swing portfolios (kind == "portfolio")
    val fills: List<FillDto> = emptyList(),
    val holdings: List<HoldingDto> = emptyList(),
    val equity: List<EquityDto> = emptyList(),
    val bench: String? = null,
    @SerialName("start_equity") val startEquity: Double? = null,
    @SerialName("bench_start") val benchStart: Double? = null,
    val cash: Double? = null,
) {
    val isPortfolio: Boolean get() = kind == "portfolio"
}

@Serializable
data class StatusDto(
    val level: String = "ok",
    val text: String? = null,
    val note: String? = null,
    val updated: String? = null,
    val asof: String? = null,
)

/** One trading day of a system: everything needed to roll up weeks, months and years. */
@Serializable
data class DayDto(
    val d: String,
    val n: Int = 0,
    val w: Int = 0,
    val l: Int = 0,
    val pnl: Double = 0.0,
    val gw: Double = 0.0,
    val gl: Double = 0.0,
    val r: Double? = null,
    val rw: Double? = null,
    val rl: Double? = null,
    val r2: Double? = null,
    val rn: Int? = null,
    val best: Double? = null,
    val worst: Double? = null,
    val fees: Double? = null,
)

@Serializable
data class TradeDto(
    val d: String,
    val sym: String = "",
    val side: String = "L",
    val et: String? = null,
    val xt: String? = null,
    val ep: Double? = null,
    val xp: Double? = null,
    val sl: Double? = null,
    val tp: Double? = null,
    val q: Double? = null,
    val out: String? = null,
    val r: Double? = null,
    val pnl: Double? = null,
    val ret: Double? = null,
    val hold: Int? = null,
    val fees: Double? = null,
    val gross: Double? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class OpenDto(
    val sym: String = "",
    val side: String = "L",
    val et: String? = null,
    val ep: Double? = null,
    val sl: Double? = null,
    val tp: Double? = null,
    @SerialName("stop_now") val stopNow: Double? = null,
    val q: Double? = null,
    val last: Double? = null,
    val r: Double? = null,
    val upnl: Double? = null,
    val note: String? = null,
)

@Serializable
data class FillDto(
    val d: String,
    val sym: String = "",
    val act: String? = null,
    val q: Double? = null,
    val px: Double? = null,
    val cf: Double? = null,
    val pnl: Double? = null,
)

@Serializable
data class HoldingDto(
    val sym: String,
    val q: Double? = null,
    val cost: Double? = null,
    val last: Double? = null,
    @SerialName("val") val value: Double? = null,
    val upnl: Double? = null,
    val w: Double? = null,
    val since: String? = null,
    val sector: String? = null,
)

@Serializable
data class EquityDto(val d: String, val eq: Double? = null, val bm: Double? = null)
