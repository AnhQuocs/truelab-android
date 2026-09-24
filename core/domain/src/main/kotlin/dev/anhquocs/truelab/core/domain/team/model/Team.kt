package dev.anhquocs.truelab.core.domain.team.model

data class TeamDetail(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val leagueName: String? = null,
    val eloRating: Double = 1500.0,
    val formScore: Double = 0.0
)

data class SeasonRanking(
    val teamId: Int,
    val position: Int,
    val won: Int,
    val draw: Int,
    val loss: Int,
    val goalDiff: Int,
    val recently: List<String> = emptyList()
) {
    val totalMatches: Int get() = won + draw + loss
    val totalPoints: Int get() = won * 3 + draw
}

