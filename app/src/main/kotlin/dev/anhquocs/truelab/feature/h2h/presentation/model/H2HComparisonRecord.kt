package dev.anhquocs.truelab.feature.h2h.presentation.model

/**
 * UI presentation model chứa toàn bộ các chỉ số đối sánh giữa 2 đội bóng đã được format.
 */
data class H2HComparisonRecord(
    val totalMatches: Int,
    val teamAWins: Int,
    val draws: Int,
    val teamBWins: Int,
    val teamAWinPercent: Float,
    val drawPercent: Float,
    val teamBWinPercent: Float,
    val teamAWinPercentFormatted: String,
    val drawPercentFormatted: String,
    val teamBWinPercentFormatted: String,
    val teamAGoals: Int,
    val teamBGoals: Int,
    val teamAElo: String,
    val teamBElo: String,
    val eloDiffFormatted: String,
    val teamAFormScore: String,
    val teamBFormScore: String,
    val teamAStreak: String,
    val teamBStreak: String,
    val teamARecentMatches: List<TeamRecentMatchItem> = emptyList(),
    val teamBRecentMatches: List<TeamRecentMatchItem> = emptyList(),
    val teamAHomeRecord: String,
    val teamAHomeWinRate: String,
    val teamAAwayRecord: String,
    val teamAAwayWinRate: String,
    val teamBHomeRecord: String,
    val teamBHomeWinRate: String,
    val teamBAwayRecord: String,
    val teamBAwayWinRate: String,
    val teamAMeanGoals: String,
    val teamBMeanGoals: String,
    val teamAGoalDiff: String,
    val teamBGoalDiff: String
)

/**
 * UI presentation model cho từng trận đấu gần nhất của đội bóng.
 */
data class TeamRecentMatchItem(
    val matchId: Long,
    val result: String, // "W", "D", "L"
    val opponentName: String,
    val opponentLogo: String?,
    val score: String, // e.g. "2 - 0"
    val isHome: Boolean,
    val date: String // e.g. "2026-09-29"
)

