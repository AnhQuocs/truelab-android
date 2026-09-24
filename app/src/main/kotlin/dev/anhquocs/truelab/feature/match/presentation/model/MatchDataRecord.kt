package dev.anhquocs.truelab.feature.match.presentation.model

/**
 * UI presentation model representing a match item in MatchesScreen.
 */
data class MatchDataRecord(
    val id: String,
    val league: String,
    val date: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val actualResult: String, // "HOME_WIN", "DRAW", "AWAY_WIN", "SCHEDULED"
    val avgHomeOdds: Double,
    val avgDrawOdds: Double,
    val avgAwayOdds: Double,
    val providerCount: Int,
    val eloDiff: Int,
    val totalGoals: Int,
    val isNormalized: Boolean,
    val predictedProb: String
)
