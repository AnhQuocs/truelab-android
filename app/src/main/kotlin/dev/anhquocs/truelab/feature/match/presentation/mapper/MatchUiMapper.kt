package dev.anhquocs.truelab.feature.match.presentation.mapper

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDataRecord

/**
 * Pure Domain-to-UI transformation mapper for Match items.
 *
 * Does NOT contain business calculations (search, sort, or score calculation).
 * Uses explicit placeholder values for fields that are not present in the Domain Match model.
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

    return MatchDataRecord(
        id = id.toString(),
        league = "—",
        date = dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.formatToVietnamDateTime(startTimeDate),
        homeTeam = homeTeam.name,
        awayTeam = awayTeam.name,
        homeScore = homeScore ?: 0,
        awayScore = awayScore ?: 0,
        actualResult = resultStr,
        avgHomeOdds = null,
        avgDrawOdds = null,
        avgAwayOdds = null,
        providerCount = 0,
        eloDiff = null,
        totalGoals = totalGoals,
        isNormalized = true,
        predictedProb = "—"
    )
}
