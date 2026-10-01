package dev.anhquocs.truelab.core.data.crawler.checkpoint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CrawlerCheckpoint(
    @SerialName("target_unique_matches") val targetUniqueMatches: Int = 30000,
    @SerialName("current_accepted_unique_matches") val currentAcceptedUniqueMatches: Int = 0,
    @SerialName("last_processed_date") val lastProcessedDate: String? = null,
    @SerialName("last_processed_page") val lastProcessedPage: Int = 1,
    @SerialName("total_processed_days") val totalProcessedDays: Int = 0,
    @SerialName("failed_dates") val failedDates: List<String> = emptyList(),
    @SerialName("session_start_time") val sessionStartTime: Long = System.currentTimeMillis(),
    @SerialName("last_updated_time") val lastUpdatedTime: Long = System.currentTimeMillis(),
    @SerialName("quality_tiers_count") val qualityTiersCount: Map<String, Int> = emptyMap()
)
