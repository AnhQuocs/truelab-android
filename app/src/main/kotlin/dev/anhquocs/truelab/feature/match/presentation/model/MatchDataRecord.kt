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
    val isPenalty: Boolean = false,
    val homePenaltyScore: Int? = null,
    val awayPenaltyScore: Int? = null,
    val actualResult: String, // "LIVE", "HOME_WIN", "DRAW", "AWAY_WIN", "SCHEDULED", "CANCELLED"
    val avgHomeOdds: Double? = null,
    val avgDrawOdds: Double? = null,
    val avgAwayOdds: Double? = null,
    val providerCount: Int = 0,
    val eloDiff: Int? = null,
    val totalGoals: Int,
    val isNormalized: Boolean = true,
    val predictedProb: String = "—"
)
