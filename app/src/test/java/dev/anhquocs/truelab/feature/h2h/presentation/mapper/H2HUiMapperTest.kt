package dev.anhquocs.truelab.feature.h2h.presentation.mapper

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatistics
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.team.model.HeadToHeadComparisonSummary
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.model.TeamHomeAwaySplits
import dev.anhquocs.truelab.core.domain.team.model.TeamPerformanceSplit
import dev.anhquocs.truelab.core.domain.team.model.TeamPerformanceStatistics
import org.junit.Assert.assertEquals
import org.junit.Test

class H2HUiMapperTest {

    private val teamA = TeamDetail(
        id = 1,
        name = "Arsenal",
        leagueName = "Premier League",
        logo = "https://example.com/arsenal.png",
        eloRating = 1950.0
    )

    private val teamB = TeamDetail(
        id = 2,
        name = "Chelsea",
        leagueName = "Premier League",
        logo = "https://example.com/chelsea.png",
        eloRating = 1820.0
    )

    private val matchHistory = listOf(
        Match(
            id = 101L,
            homeTeam = TeamSummary(1, "Arsenal"),
            awayTeam = TeamSummary(2, "Chelsea"),
            homeScore = 3,
            awayScore = 1,
            startTimeDate = "2026-03-01T15:00:00",
            status = MatchStatus.ENDED
        ),
        Match(
            id = 102L,
            homeTeam = TeamSummary(2, "Chelsea"),
            awayTeam = TeamSummary(1, "Arsenal"),
            homeScore = 2,
            awayScore = 2,
            startTimeDate = "2025-11-15T17:30:00",
            status = MatchStatus.ENDED
        )
    )

    private val descStatsMock = DescriptiveStatistics(
        count = 28,
        mean = 2.32,
        median = 2.0,
        min = 0.0,
        max = 5.0,
        range = 5.0,
        populationVariance = 1.2,
        sampleVariance = 1.25,
        populationStandardDeviation = 1.09,
        sampleStandardDeviation = 1.11,
        skewness = 0.15
    )

    private val descStatsMockB = DescriptiveStatistics(
        count = 28,
        mean = 1.60,
        median = 1.5,
        min = 0.0,
        max = 4.0,
        range = 4.0,
        populationVariance = 1.0,
        sampleVariance = 1.05,
        populationStandardDeviation = 1.0,
        sampleStandardDeviation = 1.02,
        skewness = -0.1
    )

    private val domainSummary = HeadToHeadComparisonSummary(
        teamA = teamA,
        teamB = teamB,
        totalMatches = 2,
        teamAWins = 1,
        draws = 1,
        teamBWins = 0,
        teamAGoals = 5,
        teamBGoals = 3,
        teamAWinPercent = 0.50,
        drawPercent = 0.50,
        teamBWinPercent = 0.0,
        teamAForm = FormScore(
            score = 80.0,
            rawScore = 75.0,
            matchesCount = 5,
            wins = 4,
            draws = 0,
            losses = 1,
            totalPoints = 12.0,
            maxPoints = 15.0
        ),
        teamBForm = FormScore(
            score = 55.0,
            rawScore = 50.0,
            matchesCount = 5,
            wins = 2,
            draws = 1,
            losses = 2,
            totalPoints = 7.0,
            maxPoints = 15.0
        ),
        teamAHomeAwaySplits = TeamHomeAwaySplits(
            teamId = 1,
            homeSplit = TeamPerformanceSplit(
                played = 14, won = 11, draw = 2, loss = 1, goalsFor = 35, goalsAgainst = 10, goalDiff = 25, points = 35, winRate = 0.785
            ),
            awaySplit = TeamPerformanceSplit.EMPTY,
            totalSplit = TeamPerformanceSplit(
                played = 28, won = 20, draw = 5, loss = 3, goalsFor = 65, goalsAgainst = 22, goalDiff = 43, points = 65, winRate = 0.714
            )
        ),
        teamBHomeAwaySplits = TeamHomeAwaySplits(
            teamId = 2,
            homeSplit = TeamPerformanceSplit.EMPTY,
            awaySplit = TeamPerformanceSplit(
                played = 14, won = 6, draw = 3, loss = 5, goalsFor = 20, goalsAgainst = 20, goalDiff = 0, points = 21, winRate = 0.428
            ),
            totalSplit = TeamPerformanceSplit(
                played = 28, won = 14, draw = 6, loss = 8, goalsFor = 45, goalsAgainst = 35, goalDiff = 10, points = 48, winRate = 0.500
            )
        ),
        teamAStats = TeamPerformanceStatistics(
            teamId = 1,
            matchesCount = 28,
            goalsScoredStats = descStatsMock,
            goalsConcededStats = descStatsMock,
            totalGoalsStats = descStatsMock,
            goalDiffStats = descStatsMock
        ),
        teamBStats = TeamPerformanceStatistics(
            teamId = 2,
            matchesCount = 28,
            goalsScoredStats = descStatsMockB,
            goalsConcededStats = descStatsMockB,
            totalGoalsStats = descStatsMockB,
            goalDiffStats = descStatsMockB
        ),
        h2hMatches = matchHistory
    )

    @Test
    fun toUiRecord_mapsAllAttributesCorrectly() {
        val uiRecord = H2HUiMapper.toUiRecord(domainSummary)

        assertEquals(2, uiRecord.totalMatches)
        assertEquals(1, uiRecord.teamAWins)
        assertEquals(1, uiRecord.draws)
        assertEquals(0, uiRecord.teamBWins)
        assertEquals(5, uiRecord.teamAGoals)
        assertEquals(3, uiRecord.teamBGoals)
        assertEquals("1950.0", uiRecord.teamAElo)
        assertEquals("1820.0", uiRecord.teamBElo)
        assertEquals("+130.0", uiRecord.eloDiffFormatted)
        assertEquals("80.0", uiRecord.teamAFormScore)
        assertEquals("55.0", uiRecord.teamBFormScore)
        assertEquals("4W 0D 1L", uiRecord.teamAStreak)
        assertEquals("2W 1D 2L", uiRecord.teamBStreak)
        assertEquals("2.32", uiRecord.teamAMeanGoals)
        assertEquals("1.60", uiRecord.teamBMeanGoals)
        assertEquals("+43", uiRecord.teamAGoalDiff)
        assertEquals("+10", uiRecord.teamBGoalDiff)
        assertEquals("11-2-1", uiRecord.teamAHomeRecord)
        assertEquals("6-3-5", uiRecord.teamBAwayRecord)
    }

    @Test
    fun toMatchDataRecords_mapsHistoryProperly() {
        val records = H2HUiMapper.toMatchDataRecords(domainSummary)

        assertEquals(2, records.size)
        val firstMatch = records[0]
        assertEquals("101", firstMatch.id)
        assertEquals("Arsenal", firstMatch.homeTeam)
        assertEquals("Chelsea", firstMatch.awayTeam)
        assertEquals(3, firstMatch.homeScore)
        assertEquals(1, firstMatch.awayScore)
    }
}
