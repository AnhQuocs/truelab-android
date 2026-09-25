package dev.anhquocs.truelab.core.data.crawler.cache

/**
 * Categories of datasets with differing time-to-live (TTL) requirements.
 */
enum class DatasetCategory {
    /**
     * Today's matches or live matches requiring high sync frequency.
     */
    LIVE_MATCHES,

    /**
     * Upcoming/scheduled matches that change moderately.
     */
    SCHEDULED_MATCHES,

    /**
     * Finished or historical matches whose results rarely change.
     */
    HISTORICAL_MATCHES,

    /**
     * Default general synchronization category.
     */
    DEFAULT
}

/**
 * Configuration of cache freshness and time-to-live (TTL) policies for different dataset categories.
 *
 * @property liveMatchesTtlMs TTL for live and today's matches in milliseconds (default: 5 minutes).
 * @property scheduledMatchesTtlMs TTL for upcoming scheduled matches in milliseconds (default: 30 minutes).
 * @property historicalMatchesTtlMs TTL for historical finished matches in milliseconds (default: 24 hours).
 * @property defaultTtlMs Fallback default TTL in milliseconds (default: 15 minutes).
 */
data class DataFreshnessPolicy(
    val liveMatchesTtlMs: Long = DEFAULT_LIVE_MATCHES_TTL_MS,
    val scheduledMatchesTtlMs: Long = DEFAULT_SCHEDULED_MATCHES_TTL_MS,
    val historicalMatchesTtlMs: Long = DEFAULT_HISTORICAL_MATCHES_TTL_MS,
    val defaultTtlMs: Long = DEFAULT_TTL_MS
) {
    init {
        require(liveMatchesTtlMs >= 0L) {
            "liveMatchesTtlMs must be >= 0, but was $liveMatchesTtlMs"
        }
        require(scheduledMatchesTtlMs >= 0L) {
            "scheduledMatchesTtlMs must be >= 0, but was $scheduledMatchesTtlMs"
        }
        require(historicalMatchesTtlMs >= 0L) {
            "historicalMatchesTtlMs must be >= 0, but was $historicalMatchesTtlMs"
        }
        require(defaultTtlMs >= 0L) {
            "defaultTtlMs must be >= 0, but was $defaultTtlMs"
        }
    }

    /**
     * Returns the TTL in milliseconds for the given [category].
     */
    fun getTtlMs(category: DatasetCategory): Long = when (category) {
        DatasetCategory.LIVE_MATCHES -> liveMatchesTtlMs
        DatasetCategory.SCHEDULED_MATCHES -> scheduledMatchesTtlMs
        DatasetCategory.HISTORICAL_MATCHES -> historicalMatchesTtlMs
        DatasetCategory.DEFAULT -> defaultTtlMs
    }

    companion object {
        const val DEFAULT_LIVE_MATCHES_TTL_MS = 5 * 60 * 1000L // 5 minutes
        const val DEFAULT_SCHEDULED_MATCHES_TTL_MS = 30 * 60 * 1000L // 30 minutes
        const val DEFAULT_HISTORICAL_MATCHES_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val DEFAULT_TTL_MS = 15 * 60 * 1000L // 15 minutes
    }
}
