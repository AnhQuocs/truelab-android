package dev.anhquocs.truelab.core.domain.match.model

data class Match(
    val id: Long,
    val homeTeam: TeamSummary,
    val awayTeam: TeamSummary,
    val homeScore: Int?,
    val awayScore: Int?,
    val startTimeDate: String,
    val status: MatchStatus,
    val leagueId: Int? = null,
    val season: String? = null
) {
    val isEnded: Boolean get() = status == MatchStatus.ENDED
    val totalGoals: Int get() = (homeScore ?: 0) + (awayScore ?: 0)
    val goalDifference: Int get() = (homeScore ?: 0) - (awayScore ?: 0)
    val isHomeWin: Boolean get() = isEnded && (homeScore ?: 0) > (awayScore ?: 0)
    val isAwayWin: Boolean get() = isEnded && (awayScore ?: 0) > (homeScore ?: 0)
    val isDraw: Boolean get() = isEnded && homeScore == awayScore
}

data class TeamSummary(
    val id: Int,
    val name: String,
    val logo: String? = null
)

enum class MatchStatus {
    SCHEDULED,
    IN_PROGRESS,
    ENDED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun fromCode(code: String): MatchStatus {
            return when (code) {
                "8" -> ENDED
                "1" -> IN_PROGRESS
                "0" -> SCHEDULED
                "-1" -> CANCELLED
                else -> UNKNOWN
            }
        }
    }
}
