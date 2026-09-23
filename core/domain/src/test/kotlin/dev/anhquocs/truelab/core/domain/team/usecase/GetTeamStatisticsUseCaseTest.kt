package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatistics
import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatisticsCalculator
import dev.anhquocs.truelab.core.algorithm.statistics.StatisticsCalculator
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTeamStatisticsUseCaseTest {

    private lateinit var useCase: GetTeamStatisticsUseCase

    @Before
    fun setUp() {
        useCase = GetTeamStatisticsUseCase(
            statisticsCalculator = DescriptiveStatisticsCalculator()
        )
    }

    private fun createMatch(
        id: Long,
        homeTeamId: Int,
        awayTeamId: Int,
        homeScore: Int?,
        awayScore: Int?,
        status: MatchStatus = MatchStatus.ENDED,
        startTimeDate: String = "2026-09-01 20:00"
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(id = homeTeamId, name = "Team $homeTeamId"),
            awayTeam = TeamSummary(id = awayTeamId, name = "Team $awayTeamId"),
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = status
        )
    }

    @Test
    fun `happy path with multiple matches computes correct descriptive statistics across all 4 metrics`() {
        // Team 1:
        // Match 1 (Home): 3 - 1 -> Scored 3, Conceded 1, Total 4, Diff +2
        // Match 2 (Away): 0 - 2 -> Scored 2, Conceded 0, Total 2, Diff +2
        // Match 3 (Home): 1 - 2 -> Scored 1, Conceded 2, Total 3, Diff -1
        // Match 4 (Away): 2 - 1 -> Scored 1, Conceded 2, Total 3, Diff -1
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 3, awayScore = 1),
            createMatch(2L, homeTeamId = 3, awayTeamId = 1, homeScore = 0, awayScore = 2),
            createMatch(3L, homeTeamId = 1, awayTeamId = 4, homeScore = 1, awayScore = 2),
            createMatch(4L, homeTeamId = 5, awayTeamId = 1, homeScore = 2, awayScore = 1)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(1, result.teamId)
        assertEquals(4, result.matchesCount)

        // Goals Scored: [3.0, 2.0, 1.0, 1.0] -> Mean = 7/4 = 1.75, Min = 1.0, Max = 3.0
        assertEquals(4, result.goalsScoredStats.count)
        assertEquals(1.75, result.goalsScoredStats.mean, 1e-6)
        assertEquals(1.0, result.goalsScoredStats.min, 1e-6)
        assertEquals(3.0, result.goalsScoredStats.max, 1e-6)
        assertEquals(2.0, result.goalsScoredStats.range, 1e-6)

        // Goals Conceded: [1.0, 0.0, 2.0, 2.0] -> Mean = 5/4 = 1.25, Min = 0.0, Max = 2.0
        assertEquals(4, result.goalsConcededStats.count)
        assertEquals(1.25, result.goalsConcededStats.mean, 1e-6)
        assertEquals(0.0, result.goalsConcededStats.min, 1e-6)
        assertEquals(2.0, result.goalsConcededStats.max, 1e-6)

        // Total Goals: [4.0, 2.0, 3.0, 3.0] -> Mean = 12/4 = 3.0, Min = 2.0, Max = 4.0
        assertEquals(4, result.totalGoalsStats.count)
        assertEquals(3.0, result.totalGoalsStats.mean, 1e-6)
        assertEquals(2.0, result.totalGoalsStats.min, 1e-6)
        assertEquals(4.0, result.totalGoalsStats.max, 1e-6)

        // Goal Diff: [2.0, 2.0, -1.0, -1.0] -> Mean = 2/4 = 0.5, Min = -1.0, Max = 2.0
        assertEquals(4, result.goalDiffStats.count)
        assertEquals(0.5, result.goalDiffStats.mean, 1e-6)
        assertEquals(-1.0, result.goalDiffStats.min, 1e-6)
        assertEquals(2.0, result.goalDiffStats.max, 1e-6)
    }

    @Test
    fun `maps correctly when team plays as home team`() {
        // Team 1 Home vs Team 2: 2 - 0 -> Scored 2, Conceded 0, Total 2, Diff +2
        // Team 1 Home vs Team 3: 1 - 1 -> Scored 1, Conceded 1, Total 2, Diff 0
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 1, awayScore = 1)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(2, result.matchesCount)
        assertEquals(1.5, result.goalsScoredStats.mean, 1e-6) // (2 + 1)/2
        assertEquals(0.5, result.goalsConcededStats.mean, 1e-6) // (0 + 1)/2
        assertEquals(2.0, result.totalGoalsStats.mean, 1e-6)
        assertEquals(1.0, result.goalDiffStats.mean, 1e-6) // (2 + 0)/2
    }

    @Test
    fun `maps correctly when team plays as away team`() {
        // Team 2 vs Team 1 Away: 0 - 3 -> Scored 3, Conceded 0, Total 3, Diff +3
        // Team 3 vs Team 1 Away: 2 - 1 -> Scored 1, Conceded 2, Total 3, Diff -1
        val matches = listOf(
            createMatch(1L, homeTeamId = 2, awayTeamId = 1, homeScore = 0, awayScore = 3),
            createMatch(2L, homeTeamId = 3, awayTeamId = 1, homeScore = 2, awayScore = 1)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(2, result.matchesCount)
        assertEquals(2.0, result.goalsScoredStats.mean, 1e-6) // (3 + 1)/2
        assertEquals(1.0, result.goalsConcededStats.mean, 1e-6) // (0 + 2)/2
        assertEquals(3.0, result.totalGoalsStats.mean, 1e-6)
        assertEquals(1.0, result.goalDiffStats.mean, 1e-6) // (3 - 1)/2
    }

    @Test
    fun `maps correctly across mixed home and away matches`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 4, awayScore = 1), // Home: +3 (4-1)
            createMatch(2L, homeTeamId = 3, awayTeamId = 1, homeScore = 3, awayScore = 2)  // Away: -1 (2-3)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(2, result.matchesCount)
        assertEquals(3.0, result.goalsScoredStats.mean, 1e-6) // (4 + 2)/2
        assertEquals(2.0, result.goalsConcededStats.mean, 1e-6) // (1 + 3)/2
        assertEquals(1.0, result.goalDiffStats.mean, 1e-6) // (3 - 1)/2
    }

    @Test
    fun `verifies goals scored distribution metrics`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 0, awayScore = 0),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 2, awayScore = 0),
            createMatch(3L, homeTeamId = 1, awayTeamId = 4, homeScore = 4, awayScore = 0)
        )

        val result = useCase(teamId = 1, matches = matches)

        val scored = result.goalsScoredStats
        assertEquals(3, scored.count)
        assertEquals(2.0, scored.mean, 1e-6)
        assertEquals(2.0, scored.median, 1e-6)
        assertEquals(0.0, scored.min, 1e-6)
        assertEquals(4.0, scored.max, 1e-6)
        assertEquals(4.0, scored.range, 1e-6)
        assertEquals(4.0, scored.sampleVariance, 1e-6) // ((0-2)^2 + (2-2)^2 + (4-2)^2)/(3-1) = 8/2 = 4
        assertEquals(2.0, scored.sampleStandardDeviation, 1e-6)
    }

    @Test
    fun `verifies goals conceded distribution metrics`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 0, awayScore = 1),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 0, awayScore = 2),
            createMatch(3L, homeTeamId = 1, awayTeamId = 4, homeScore = 0, awayScore = 3)
        )

        val result = useCase(teamId = 1, matches = matches)

        val conceded = result.goalsConcededStats
        assertEquals(3, conceded.count)
        assertEquals(2.0, conceded.mean, 1e-6)
        assertEquals(2.0, conceded.median, 1e-6)
        assertEquals(1.0, conceded.min, 1e-6)
        assertEquals(3.0, conceded.max, 1e-6)
        assertEquals(2.0, conceded.range, 1e-6)
    }

    @Test
    fun `verifies total goals distribution metrics`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 1), // Total = 3
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 3, awayScore = 2)  // Total = 5
        )

        val result = useCase(teamId = 1, matches = matches)

        val total = result.totalGoalsStats
        assertEquals(2, total.count)
        assertEquals(4.0, total.mean, 1e-6)
        assertEquals(3.0, total.min, 1e-6)
        assertEquals(5.0, total.max, 1e-6)
    }

    @Test
    fun `verifies goal difference distribution metrics`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 3, awayScore = 0), // Diff = +3
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 0, awayScore = 3)  // Diff = -3
        )

        val result = useCase(teamId = 1, matches = matches)

        val diff = result.goalDiffStats
        assertEquals(2, diff.count)
        assertEquals(0.0, diff.mean, 1e-6)
        assertEquals(-3.0, diff.min, 1e-6)
        assertEquals(3.0, diff.max, 1e-6)
        assertEquals(6.0, diff.range, 1e-6)
    }

    @Test
    fun `ignores unfinished matches with status other than ENDED`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0, status = MatchStatus.ENDED),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 5, awayScore = 0, status = MatchStatus.IN_PROGRESS),
            createMatch(3L, homeTeamId = 1, awayTeamId = 4, homeScore = null, awayScore = null, status = MatchStatus.SCHEDULED)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(1, result.matchesCount)
        assertEquals(2.0, result.goalsScoredStats.mean, 1e-6)
    }

    @Test
    fun `ignores matches with null homeScore`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = null, awayScore = 0)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(1, result.matchesCount)
        assertEquals(1.0, result.goalsScoredStats.mean, 1e-6)
    }

    @Test
    fun `ignores matches with null awayScore`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 2, awayScore = null)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(1, result.matchesCount)
        assertEquals(1.0, result.goalsScoredStats.mean, 1e-6)
    }

    @Test
    fun `excludes currentMatchId to prevent data leakage`() {
        val matches = listOf(
            createMatch(101L, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0),
            createMatch(102L, homeTeamId = 1, awayTeamId = 3, homeScore = 10, awayScore = 0) // Current match
        )

        // Case 1: currentMatchId matches match 102
        val withCurrentMatch = useCase(teamId = 1, matches = matches, currentMatchId = 102L)
        assertEquals(1, withCurrentMatch.matchesCount)
        assertEquals(2.0, withCurrentMatch.goalsScoredStats.mean, 1e-6)

        // Case 2: currentMatchId is null
        val withoutCurrentMatch = useCase(teamId = 1, matches = matches, currentMatchId = null)
        assertEquals(2, withoutCurrentMatch.matchesCount)
        assertEquals(6.0, withoutCurrentMatch.goalsScoredStats.mean, 1e-6)

        // Case 3: currentMatchId does not exist in matches list
        val nonExistentCurrentMatch = useCase(teamId = 1, matches = matches, currentMatchId = 999L)
        assertEquals(2, nonExistentCurrentMatch.matchesCount)
    }

    @Test
    fun `ignores matches where teamId is neither home nor away team`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0),
            createMatch(2L, homeTeamId = 88, awayTeamId = 99, homeScore = 5, awayScore = 4) // Irrelevant
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(1, result.matchesCount)
        assertEquals(2.0, result.goalsScoredStats.mean, 1e-6)
    }

    @Test
    fun `empty matches list returns zero count and NaN statistics`() {
        val result = useCase(teamId = 1, matches = emptyList())

        assertEquals(1, result.teamId)
        assertEquals(0, result.matchesCount)
        assertEquals(0, result.goalsScoredStats.count)
        assertTrue(result.goalsScoredStats.mean.isNaN())
        assertTrue(result.goalsConcededStats.mean.isNaN())
        assertTrue(result.totalGoalsStats.mean.isNaN())
        assertTrue(result.goalDiffStats.mean.isNaN())
    }

    @Test
    fun `returns zero count when no matches are valid`() {
        val matches = listOf(
            createMatch(1L, homeTeamId = 88, awayTeamId = 99, homeScore = 2, awayScore = 0),
            createMatch(2L, homeTeamId = 1, awayTeamId = 2, homeScore = null, awayScore = null, status = MatchStatus.SCHEDULED)
        )

        val result = useCase(teamId = 1, matches = matches)

        assertEquals(0, result.matchesCount)
        assertTrue(result.goalsScoredStats.mean.isNaN())
    }

    @Test
    fun `does not mutate input matches list`() {
        val original = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 2, awayScore = 0),
            createMatch(2L, homeTeamId = 1, awayTeamId = 3, homeScore = 1, awayScore = 1)
        )
        val copy = original.toList()

        useCase(teamId = 1, matches = original)

        assertEquals(copy, original)
        assertEquals(1L, original[0].id)
        assertEquals(2L, original[1].id)
    }

    @Test
    fun `delegates all series to injected StatisticsCalculator summarize method`() {
        var summarizeCallCount = 0
        val receivedSeries = mutableListOf<List<Double>>()

        val customCalculator = object : StatisticsCalculator {
            override fun mean(dataset: List<Double>): Double = 0.0
            override fun median(dataset: List<Double>): Double = 0.0
            override fun min(dataset: List<Double>): Double = 0.0
            override fun max(dataset: List<Double>): Double = 0.0
            override fun range(dataset: List<Double>): Double = 0.0
            override fun populationVariance(dataset: List<Double>): Double = 0.0
            override fun sampleVariance(dataset: List<Double>): Double = 0.0
            override fun populationStandardDeviation(dataset: List<Double>): Double = 0.0
            override fun sampleStandardDeviation(dataset: List<Double>): Double = 0.0
            override fun skewness(dataset: List<Double>): Double = 0.0
            override fun summarize(dataset: List<Double>): DescriptiveStatistics {
                summarizeCallCount++
                receivedSeries.add(dataset)
                return DescriptiveStatistics(
                    count = dataset.size,
                    mean = 42.0,
                    median = 42.0,
                    min = 42.0,
                    max = 42.0,
                    range = 0.0,
                    populationVariance = 0.0,
                    sampleVariance = 0.0,
                    populationStandardDeviation = 0.0,
                    sampleStandardDeviation = 0.0,
                    skewness = 0.0
                )
            }
        }

        val customUseCase = GetTeamStatisticsUseCase(customCalculator)
        val matches = listOf(
            createMatch(1L, homeTeamId = 1, awayTeamId = 2, homeScore = 3, awayScore = 1)
        )

        val result = customUseCase(teamId = 1, matches = matches)

        assertEquals(4, summarizeCallCount) // 4 series: scored, conceded, total, diff
        assertEquals(42.0, result.goalsScoredStats.mean, 1e-6)
        assertEquals(listOf(3.0), receivedSeries[0]) // Scored
        assertEquals(listOf(1.0), receivedSeries[1]) // Conceded
        assertEquals(listOf(4.0), receivedSeries[2]) // Total
        assertEquals(listOf(2.0), receivedSeries[3]) // Diff
    }
}
