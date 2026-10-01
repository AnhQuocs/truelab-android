package dev.anhquocs.truelab.feature.h2h.presentation.mapper

import dev.anhquocs.truelab.core.domain.team.model.HeadToHeadComparisonSummary
import dev.anhquocs.truelab.feature.h2h.presentation.model.H2HComparisonRecord
import dev.anhquocs.truelab.feature.match.presentation.mapper.toUiRecord
import dev.anhquocs.truelab.feature.match.presentation.model.MatchDataRecord
import java.util.Locale
import kotlin.math.roundToInt

object H2HUiMapper {

    fun toUiRecord(summary: HeadToHeadComparisonSummary): H2HComparisonRecord {
        val total = summary.totalMatches

        val aWinPct = summary.teamAWinPercent.toFloat()
        val dPct = summary.drawPercent.toFloat()
        val bWinPct = summary.teamBWinPercent.toFloat()

        val aWinPctStr = if (total > 0) "${(summary.teamAWinPercent * 100).roundToInt()}%" else "0%"
        val dPctStr = if (total > 0) "${(summary.drawPercent * 100).roundToInt()}%" else "0%"
        val bWinPctStr = if (total > 0) "${(summary.teamBWinPercent * 100).roundToInt()}%" else "0%"

        val eloA = summary.teamA.eloRating
        val eloB = summary.teamB.eloRating
        val eloDiff = eloA - eloB
        val eloDiffStr = when {
            eloDiff > 0 -> "+${String.format(Locale.US, "%.1f", eloDiff)}"
            eloDiff < 0 -> String.format(Locale.US, "%.1f", eloDiff)
            else -> "0.0"
        }

        val formAStr = summary.teamAForm?.let {
            String.format(Locale.US, "%.1f", it.score)
        } ?: if (summary.teamA.formScore > 0) String.format(Locale.US, "%.1f", summary.teamA.formScore) else "—"

        val formBStr = summary.teamBForm?.let {
            String.format(Locale.US, "%.1f", it.score)
        } ?: if (summary.teamB.formScore > 0) String.format(Locale.US, "%.1f", summary.teamB.formScore) else "—"

        val streakAStr = summary.teamAForm?.let { "${it.wins}W ${it.draws}D ${it.losses}L" } ?: "—"
        val streakBStr = summary.teamBForm?.let { "${it.wins}W ${it.draws}D ${it.losses}L" } ?: "—"

        val homeSplitA = summary.teamAHomeAwaySplits.homeSplit
        val awaySplitA = summary.teamAHomeAwaySplits.awaySplit
        val homeSplitB = summary.teamBHomeAwaySplits.homeSplit
        val awaySplitB = summary.teamBHomeAwaySplits.awaySplit

        val homeRecordAStr = "${homeSplitA.won}W - ${homeSplitA.draw}D - ${homeSplitA.loss}L"
        val homeWinRateAStr = "${(homeSplitA.winRate * 100).roundToInt()}%"
        val awayRecordAStr = "${awaySplitA.won}W - ${awaySplitA.draw}D - ${awaySplitA.loss}L"
        val awayWinRateAStr = "${(awaySplitA.winRate * 100).roundToInt()}%"

        val homeRecordBStr = "${homeSplitB.won}W - ${homeSplitB.draw}D - ${homeSplitB.loss}L"
        val homeWinRateBStr = "${(homeSplitB.winRate * 100).roundToInt()}%"
        val awayRecordBStr = "${awaySplitB.won}W - ${awaySplitB.draw}D - ${awaySplitB.loss}L"
        val awayWinRateBStr = "${(awaySplitB.winRate * 100).roundToInt()}%"

        val meanGoalsAStr = summary.teamAStats?.let {
            String.format(Locale.US, "%.2f", it.goalsScoredStats.mean)
        } ?: "—"

        val meanGoalsBStr = summary.teamBStats?.let {
            String.format(Locale.US, "%.2f", it.goalsScoredStats.mean)
        } ?: "—"

        val goalDiffA = summary.teamAHomeAwaySplits.totalSplit.goalDiff
        val goalDiffAStr = if (goalDiffA > 0) "+$goalDiffA" else "$goalDiffA"

        val goalDiffB = summary.teamBHomeAwaySplits.totalSplit.goalDiff
        val goalDiffBStr = if (goalDiffB > 0) "+$goalDiffB" else "$goalDiffB"

        return H2HComparisonRecord(
            totalMatches = total,
            teamAWins = summary.teamAWins,
            draws = summary.draws,
            teamBWins = summary.teamBWins,
            teamAWinPercent = aWinPct,
            drawPercent = dPct,
            teamBWinPercent = bWinPct,
            teamAWinPercentFormatted = aWinPctStr,
            drawPercentFormatted = dPctStr,
            teamBWinPercentFormatted = bWinPctStr,
            teamAGoals = summary.teamAGoals,
            teamBGoals = summary.teamBGoals,
            teamAElo = String.format(Locale.US, "%.1f", eloA),
            teamBElo = String.format(Locale.US, "%.1f", eloB),
            eloDiffFormatted = eloDiffStr,
            teamAFormScore = formAStr,
            teamBFormScore = formBStr,
            teamAStreak = streakAStr,
            teamBStreak = streakBStr,
            teamARecentMatches = mapRecentMatches(summary.teamARecentMatches, summary.teamA.id),
            teamBRecentMatches = mapRecentMatches(summary.teamBRecentMatches, summary.teamB.id),
            teamAHomeRecord = homeRecordAStr,
            teamAHomeWinRate = homeWinRateAStr,
            teamAAwayRecord = awayRecordAStr,
            teamAAwayWinRate = awayWinRateAStr,
            teamBHomeRecord = homeRecordBStr,
            teamBHomeWinRate = homeWinRateBStr,
            teamBAwayRecord = awayRecordBStr,
            teamBAwayWinRate = awayWinRateBStr,
            teamAMeanGoals = meanGoalsAStr,
            teamBMeanGoals = meanGoalsBStr,
            teamAGoalDiff = goalDiffAStr,
            teamBGoalDiff = goalDiffBStr
        )
    }

    internal fun mapRecentMatches(
        matches: List<dev.anhquocs.truelab.core.domain.match.model.Match>,
        teamId: Int
    ): List<dev.anhquocs.truelab.feature.h2h.presentation.model.TeamRecentMatchItem> {
        return matches.mapNotNull { match ->
            val homeScore = match.homeScore ?: return@mapNotNull null
            val awayScore = match.awayScore ?: return@mapNotNull null
            val isHome = match.homeTeam.id == teamId
            val isAway = match.awayTeam.id == teamId
            if (!isHome && !isAway) return@mapNotNull null

            val opponent = if (isHome) match.awayTeam else match.homeTeam
            val result = when {
                homeScore == awayScore -> "D"
                isHome -> if (homeScore > awayScore) "W" else "L"
                else -> if (awayScore > homeScore) "W" else "L"
            }
            val scoreText = "$homeScore - $awayScore"
            val dateText = match.startTimeDate.substringBefore('T').substringBefore(' ')

            dev.anhquocs.truelab.feature.h2h.presentation.model.TeamRecentMatchItem(
                matchId = match.id,
                result = result,
                opponentName = opponent.name,
                opponentLogo = opponent.logo,
                score = scoreText,
                isHome = isHome,
                date = dateText
            )
        }
    }

    fun toMatchDataRecords(summary: HeadToHeadComparisonSummary): List<MatchDataRecord> {
        return summary.h2hMatches.map { it.toUiRecord() }
    }
}
