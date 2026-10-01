package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionDistributionOfflineTest {

    private val predictUseCase = PredictMatchOutcomeUseCase()

    private fun createMatch(
        id: Long,
        homeTeam: TeamSummary,
        awayTeam: TeamSummary,
        homeScore: Int,
        awayScore: Int,
        startTimeDate: String
    ): Match {
        return Match(
            id = id,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = MatchStatus.ENDED
        )
    }

    @Test
    fun `all prediction transformers produce valid 3-way probabilities summing to 1`() {
        val homeTeam = TeamSummary(id = 1, name = "Arsenal")
        val awayTeam = TeamSummary(id = 2, name = "Chelsea")

        val recentMatchesHome = listOf(
            createMatch(101L, homeTeam, TeamSummary(3, "Wolves"), 2, 0, "2026-01-01T15:00:00"),
            createMatch(102L, TeamSummary(4, "Fulham"), homeTeam, 1, 1, "2026-01-08T15:00:00")
        )
        val recentMatchesAway = listOf(
            createMatch(103L, awayTeam, TeamSummary(5, "Brighton"), 0, 1, "2026-01-02T15:00:00"),
            createMatch(104L, TeamSummary(6, "Everton"), awayTeam, 2, 2, "2026-01-09T15:00:00")
        )

        val context = MatchPredictionContext(
            matchId = 200L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1900.0,
            awayElo = 1820.0,
            homeRecentMatches = recentMatchesHome,
            awayRecentMatches = recentMatchesAway,
            h2hMatches = emptyList(),
            latestOdds = null,
            isNeutralVenue = false
        )

        val result = predictUseCase(context)

        assertTrue("homeWinProb must be in (0, 1)", result.homeWinProb in 0.01..0.99)
        assertTrue("drawProb must be in (0, 1)", result.drawProb in 0.01..0.99)
        assertTrue("awayWinProb must be in (0, 1)", result.awayWinProb in 0.01..0.99)
        assertEquals(1.0, result.homeWinProb + result.drawProb + result.awayWinProb, 1e-4)

        // Verify Draw Evidence is non-zero
        val evidence = result.evidence
        assertTrue("Elo drawProb > 0", (evidence?.elo?.drawProb ?: 0.0) > 0.0)
        assertTrue("Form drawProb > 0", (evidence?.form?.drawProb ?: 0.0) > 0.0)
        assertTrue("Goals drawProb > 0", (evidence?.goals?.drawProb ?: 0.0) > 0.0)
        assertTrue("H2H drawProb > 0", (evidence?.h2h?.drawProb ?: 0.0) > 0.0)
        assertTrue("HomeAdv drawProb > 0", (evidence?.homeAdvantage?.drawProb ?: 0.0) > 0.0)
    }

    @Test
    fun `synthetic cohort distribution audit verifies average draw probability and argmax characteristics`() {
        var homeCount = 0
        var drawCount = 0
        var awayCount = 0

        var sumHomeProb = 0.0
        var sumDrawProb = 0.0
        var sumAwayProb = 0.0

        var minDraw = 1.0
        var maxDraw = 0.0

        val n = 300
        for (i in 1..n) {
            val hElo = 1300.0 + (i % 10) * 80.0
            val aElo = 1300.0 + ((i * 3) % 10) * 80.0

            val odds: OddsRecordItem? = if (i % 3 == 0) {
                OddsRecordItem(
                    companyId = 1,
                    companyName = "Bet",
                    oddsType = "eu",
                    handicap = null,
                    over = null,
                    under = null,
                    homeWin = 2.1 + (i % 5) * 0.3,
                    draw = 3.1 + (i % 3) * 0.2,
                    awayWin = 2.8 + (i % 4) * 0.4,
                    changeTime = 1700000000L,
                    marketPhase = "instant"
                )
            } else {
                null
            }

            val context = MatchPredictionContext(
                matchId = i.toLong(),
                homeTeamId = i,
                awayTeamId = i + 1000,
                homeElo = hElo,
                awayElo = aElo,
                homeRecentMatches = emptyList(),
                awayRecentMatches = emptyList(),
                h2hMatches = emptyList(),
                latestOdds = odds,
                isNeutralVenue = (i % 5 == 0)
            )

            val res = predictUseCase(context)

            when (res.predictedOutcome) {
                "HOME_WIN" -> homeCount++
                "DRAW" -> drawCount++
                "AWAY_WIN" -> awayCount++
            }

            sumHomeProb += res.homeWinProb
            sumDrawProb += res.drawProb
            sumAwayProb += res.awayWinProb

            if (res.drawProb < minDraw) minDraw = res.drawProb
            if (res.drawProb > maxDraw) maxDraw = res.drawProb
        }

        val avgHome = sumHomeProb / n
        val avgDraw = sumDrawProb / n
        val avgAway = sumAwayProb / n

        println("=== PREDICTION DISTRIBUTION AUDIT ===")
        println("Cohort Size: $n")
        println("Predictions -> HOME_WIN: $homeCount, DRAW: $drawCount, AWAY_WIN: $awayCount")
        println("Averages -> Home: ${String.format("%.3f", avgHome)}, Draw: ${String.format("%.3f", avgDraw)}, Away: ${String.format("%.3f", avgAway)}")
        println("Draw Range -> Min: ${String.format("%.3f", minDraw)}, Max: ${String.format("%.3f", maxDraw)}")

        // Assert that Draw probability is always active and healthy (> 20%)
        assertTrue("Min Draw Probability must be > 0.15", minDraw >= 0.15)
        assertTrue("Avg Draw Probability should be around baseline (0.24-0.30)", avgDraw in 0.24..0.30)
        assertTrue("Total probabilities must sum to 1.0", (avgHome + avgDraw + avgAway) in 0.999..1.001)
    }
}
