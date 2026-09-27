package dev.anhquocs.truelab.core.data.league.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompetitionListResponseBase(
    @SerialName("data") val data: List<CompetitionItemDto>,
    @SerialName("meta") val meta: CompetitionMetaResponse? = null
)

@Serializable
data class CompetitionMetaResponse(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
    @SerialName("total") val total: Int? = null
)

@Serializable
data class CompetitionItemDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("short_name") val shortName: String? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("country_id") val countryId: Int? = null,
    @SerialName("cur_season_id") val curSeasonId: Long? = null
)

@Serializable
data class SeasonDto(
    @SerialName("id") val id: Long,
    @SerialName("competition_id") val competitionId: Long,
    @SerialName("year") val year: String,
    @SerialName("is_current") val isCurrent: Int? = null,
    @SerialName("start_time") val startTime: Long? = null,
    @SerialName("end_time") val endTime: Long? = null
)
