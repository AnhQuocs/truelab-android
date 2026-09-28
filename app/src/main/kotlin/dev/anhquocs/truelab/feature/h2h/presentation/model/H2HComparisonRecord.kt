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
    val teamAHomeRecord: String,
    val teamAHomeWinRate: String,
    val teamBAwayRecord: String,
    val teamBAwayWinRate: String,
    val teamAMeanGoals: String,
    val teamBMeanGoals: String,
    val teamAGoalDiff: String,
    val teamBGoalDiff: String
)
