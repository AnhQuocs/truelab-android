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

    fun calculateFormScore(): Double {
        if (recently.isEmpty()) return 0.0
        val points = recently.take(5).sumOf { matchResult ->
            when (matchResult.uppercase()) {
                "W" -> 3.toInt()
                "D" -> 1.toInt()
                else -> 0.toInt()
            }
        }
        val maxPoints = (recently.take(5).size * 3).coerceAtLeast(1)
        return (points.toDouble() / maxPoints) * 100.0
    }
}
