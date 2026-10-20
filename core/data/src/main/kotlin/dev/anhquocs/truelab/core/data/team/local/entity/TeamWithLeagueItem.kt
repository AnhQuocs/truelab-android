package dev.anhquocs.truelab.core.data.team.local.entity

import dev.anhquocs.truelab.core.domain.team.model.TeamDetail

/**
 * POJO ánh xạ kết quả truy vấn kết hợp thông tin đội bóng và giải đấu thực tế.
 */
data class TeamWithLeagueItem(
    val id: Int,
    val name: String,
    val logo: String?,
    val leagueName: String?,
    val leagueLogo: String?,
    val eloRating: Double = 1500.0,
    val formScore: Double = 0.0
) {
    fun toDomain() = TeamDetail(
        id = id,
        name = name,
        logo = logo,
        leagueName = leagueName,
        leagueLogo = leagueLogo,
        eloRating = eloRating,
        formScore = formScore
    )
}
