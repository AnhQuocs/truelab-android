package dev.anhquocs.truelab.feature.match.presentation.model

/**
 * UI presentation model representing a match item in MatchesScreen.
 */
data class MatchDataRecord(
    val id: String,
    val league: String,
    val leagueLogo: String? = null,
    val date: String,
    val matchTime: String = "",
    val homeTeam: String,
    val homeLogo: String? = null,
    val awayTeam: String,
    val awayLogo: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val isPenalty: Boolean = false,
    val homePenaltyScore: Int? = null,
    val awayPenaltyScore: Int? = null,
    val actualResult: String, // "LIVE", "HOME_WIN", "DRAW", "AWAY_WIN", "SCHEDULED", "CANCELLED"
    val isEnded: Boolean = false,
    val isScheduled: Boolean = false,
    val avgHomeOdds: Double? = null,
    val avgDrawOdds: Double? = null,
    val avgAwayOdds: Double? = null,
    val providerCount: Int = 0,
    val eloDiff: Int? = null,
    val totalGoals: Int = 0,
    val isNormalized: Boolean = true,
    val predictedProb: String = "—"
)
