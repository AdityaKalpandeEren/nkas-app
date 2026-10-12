package com.kalpande.algopulse.domain

import java.time.Duration
import java.time.Instant

/**
 * How current a market's published data is. The bots publish every workflow run
 * (~every 2 minutes, around the clock), so a dashboard older than ~25 minutes means
 * the trigger or the workflow has stopped - the outage the app exists to catch.
 */
enum class Freshness(val label: String) {
    LIVE("Live"),
    DELAYED("Delayed"),
    DOWN("Down"),
    UNKNOWN("No data");

    companion object {
        const val LIVE_MIN = 8L
        const val DOWN_MIN = 25L

        fun of(generatedAt: Instant?, now: Instant, downAfterMin: Long = DOWN_MIN): Freshness {
            if (generatedAt == null) return UNKNOWN
            val age = Duration.between(generatedAt, now).toMinutes()
            return when {
                age <= LIVE_MIN -> LIVE
                age <= downAfterMin -> DELAYED
                else -> DOWN
            }
        }
    }
}
