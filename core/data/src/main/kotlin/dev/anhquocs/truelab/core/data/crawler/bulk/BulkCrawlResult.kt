package dev.anhquocs.truelab.core.data.crawler.bulk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BulkCrawlResult(
    @SerialName("target_unique_matches") val targetUniqueMatches: Int,
    @SerialName("accepted_unique_matches") val acceptedUniqueMatches: Int,
    @SerialName("total_processed_days") val totalProcessedDays: Int,
    @SerialName("start_date") val startDate: String,
    @SerialName("earliest_date_reached") val earliestDateReached: String,
    @SerialName("quality_tiers_count") val qualityTiersCount: Map<String, Int>,
    @SerialName("quarantined_competitions_count") val quarantinedCompetitionsCount: Int,
    @SerialName("quarantined_matches_count") val quarantinedMatchesCount: Int,
    @SerialName("failed_dates") val failedDates: List<String>,
    @SerialName("is_target_reached") val isTargetReached: Boolean,
    @SerialName("duration_ms") val durationMs: Long
)
