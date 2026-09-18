package dev.anhquocs.truelab.core.data.match.remote.dto

import dev.anhquocs.truelab.core.data.remote.dto.MetaResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchInfoDetailResponseBase(
    @SerialName("data") val data: List<MatchRecord>,
    @SerialName("meta") val meta: MetaResponse? = null
)

@Serializable
data class MatchRecord(
    @SerialName("id") val id: Long,
    @SerialName("home_team") val homeTeam: TeamInfo,
    @SerialName("away_team") val awayTeam: TeamInfo,
    @SerialName("home_score") val homeScore: Int,
    @SerialName("away_score") val awayScore: Int,
    @SerialName("start_time_date") val startTimeDate: String,
    @SerialName("status") val status: String
)

@Serializable
data class TeamInfo(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("logo") val logo: String? = null
)
