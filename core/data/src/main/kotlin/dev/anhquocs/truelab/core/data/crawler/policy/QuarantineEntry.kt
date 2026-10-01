package dev.anhquocs.truelab.core.data.crawler.policy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuarantineEntry(
    @SerialName("competition_id") val competitionId: Int,
    @SerialName("name") val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("sample_match_count") val sampleMatchCount: Int = 1,
    @SerialName("first_observed_date") val firstObservedDate: String,
    @SerialName("last_observed_date") val lastObservedDate: String,
    @SerialName("sample_home_team") val sampleHomeTeam: String? = null,
    @SerialName("sample_away_team") val sampleAwayTeam: String? = null,
    @SerialName("reason") val reason: String = "UNKNOWN_COMPETITION"
)
