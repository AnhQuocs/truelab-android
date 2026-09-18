package dev.anhquocs.truelab.core.data.ranking.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonRankResponse(
    @SerialName("team_id") val teamId: Int,
    @SerialName("position") val position: Int,
    @SerialName("won") val won: Int,
    @SerialName("draw") val draw: Int,
    @SerialName("loss") val loss: Int,
    @SerialName("goal_diff") val goalDiff: Int,
    @SerialName("recently") val recently: List<String>? = null
)
