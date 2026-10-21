package dev.anhquocs.truelab.feature.match.presentation.mapper

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDataRecord
import kotlin.math.roundToInt

/**
 * Pure Domain-to-UI transformation mapper for Match items.
 */
fun Match.toUiRecord(): MatchDataRecord {
    val resultStr = when {
        status == MatchStatus.IN_PROGRESS -> "LIVE"
        status == MatchStatus.CANCELLED -> "CANCELLED"
        !isEnded -> "SCHEDULED"
        isHomeWin -> "HOME_WIN"
        isAwayWin -> "AWAY_WIN"
        isDraw -> "DRAW"
        else -> "UNKNOWN"
    }

    val calculatedEloDiff = if (homeTeam.eloRating > 0 && awayTeam.eloRating > 0) {
        (homeTeam.eloRating - awayTeam.eloRating).roundToInt()
    } else {
        null
    }

    return MatchDataRecord(
        id = id.toString(),
        league = leagueName ?: "—",
        leagueLogo = leagueLogo,
        date = DateTimeFormatterUtils.formatToVietnamDate(startTimeDate),
        matchTime = DateTimeFormatterUtils.formatToVietnamTime(startTimeDate),
        homeTeam = homeTeam.name,
        homeLogo = homeTeam.logo,
        awayTeam = awayTeam.name,
        awayLogo = awayTeam.logo,
        homeScore = homeScore,
        awayScore = awayScore,
        isPenalty = isPenalty,
        homePenaltyScore = homePenaltyScore,
        awayPenaltyScore = awayPenaltyScore,
        actualResult = resultStr,
        isEnded = isEnded,
        isScheduled = status == MatchStatus.SCHEDULED || !isEnded,
        avgHomeOdds = null,
        avgDrawOdds = null,
        avgAwayOdds = null,
        providerCount = 0,
        eloDiff = calculatedEloDiff,
        totalGoals = totalGoals,
        isNormalized = true,
        predictedProb = "—"
    )
}
