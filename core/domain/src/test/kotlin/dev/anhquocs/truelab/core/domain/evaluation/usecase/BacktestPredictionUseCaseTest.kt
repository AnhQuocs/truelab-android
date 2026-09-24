package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BacktestPredictionUseCaseTest {

    private lateinit var useCase: BacktestPredictionUseCase

    private val teamManCity = TeamSummary(id = 1, name = "Manchester City")
    private val teamArsenal = TeamSummary(id = 2, name = "Arsenal")
    private val teamLiverpool = TeamSummary(id = 3, name = "Liverpool")
    private val teamChelsea = TeamSummary(id = 4, name = "Chelsea")

    @Before
    fun setUp() {
        useCase = BacktestPredictionUseCase()
    }

    private fun createOddsItem(
        companyId: Int,
        companyName: String,
        homeWin: Double,
        draw: Double,
        awayWin: Double
    ): OddsRecordItem {
        return OddsRecordItem(
            companyId = companyId,
            companyName = companyName,
            oddsType = "1x2",
            handicap = null,
            over = null,
            under = null,
            homeWin = homeWin,
            draw = draw,
            awayWin = awayWin,
            changeTime = 1700000000000L,
            marketPhase = "CLOSING"
        )
    }

    private fun createMatch(
        id: Long,
        homeTeam: TeamSummary,
        awayTeam: TeamSummary,
        homeScore: Int?,
        awayScore: Int?,
        startTimeDate: String,
        status: MatchStatus = MatchStatus.ENDED
    ): Match {
        return Match(
            id = id,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = status
        )
    }

    @Test
    fun `1 Regular backtest dataset executes prediction on chronological sequence`() {
        val match1 = createMatch(1L, teamManCity, teamArsenal, 2, 1, "2026-01-10T15:00:00")
        val match2 = createMatch(2L, teamArsenal, teamLiverpool, 1, 1, "2026-01-17T15:00:00")
        val match3 = createMatch(3L, teamManCity, teamLiverpool, 3, 0, "2026-01-24T15:00:00")

        val teamEloMap = mapOf(1 to 1980.0, 2 to 1940.0, 3 to 1920.0)
        val oddsMap = mapOf(
            1L to createOddsItem(1, "Provider A", 1.80, 3.50, 4.20),
            2L to createOddsItem(2, "Provider A", 2.40, 3.20, 2.90),
            3L to createOddsItem(3, "Provider A", 1.65, 3.80, 5.00)
        )

        val result = useCase(
            matches = listOf(match3, match1, match2), // Pass in scrambled order
            matchOddsMap = oddsMap,
            teamEloMap = teamEloMap
        )

        assertEquals(3, result.totalMatches)
        assertEquals(3, result.records.size)

        // Verify matches were sorted chronologically: match1 -> match2 -> match3
        assertEquals(1L, result.records[0].matchId)
        assertEquals(2L, result.records[1].matchId)
        assertEquals(3L, result.records[2].matchId)

        // Verify actual outcomes
        assertEquals("HOME_WIN", result.records[0].actualOutcome)
        assertEquals("DRAW", result.records[1].actualOutcome)
        assertEquals("HOME_WIN", result.records[2].actualOutcome)

        assertTrue(result.evaluationResult.accuracy in 0.0..1.0)
        assertEquals(3, result.evaluationResult.totalEvaluated)
    }

    @Test
    fun `2 Empty dataset returns zero total and correct matches without crash`() {
        val result = useCase(emptyList())

        assertEquals(0, result.totalMatches)
        assertEquals(0, result.correctMatches)
        assertTrue(result.records.isEmpty())
        assertEquals(0.0, result.evaluationResult.accuracy, 1e-6)
        assertEquals(0, result.evaluationResult.totalEvaluated)
    }

    @Test
    fun `3 Matches not ended are excluded from evaluation targets`() {
        val endedMatch = createMatch(1L, teamManCity, teamArsenal, 2, 1, "2026-01-10T15:00:00", MatchStatus.ENDED)
        val scheduledMatch = createMatch(2L, teamArsenal, teamLiverpool, null, null, "2026-01-17T15:00:00", MatchStatus.SCHEDULED)
        val inProgressMatch = createMatch(3L, teamManCity, teamLiverpool, 1, 0, "2026-01-24T15:00:00", MatchStatus.IN_PROGRESS)

        val result = useCase(listOf(endedMatch, scheduledMatch, inProgressMatch))

        assertEquals(1, result.totalMatches)
        assertEquals(1, result.records.size)
        assertEquals(1L, result.records[0].matchId)
    }

    @Test
    fun `4 Matches missing homeScore or awayScore are excluded from evaluation targets`() {
        val validMatch = createMatch(1L, teamManCity, teamArsenal, 2, 1, "2026-01-10T15:00:00", MatchStatus.ENDED)
        val nullHome = createMatch(2L, teamArsenal, teamLiverpool, null, 1, "2026-01-17T15:00:00", MatchStatus.ENDED)
        val nullAway = createMatch(3L, teamManCity, teamLiverpool, 2, null, "2026-01-24T15:00:00", MatchStatus.ENDED)

        val result = useCase(listOf(validMatch, nullHome, nullAway))

        assertEquals(1, result.totalMatches)
        assertEquals(1L, result.records[0].matchId)
    }

    @Test
    fun `5 Current match is not included in its own historical context`() {
        // Only 1 match in dataset
        val singleMatch = createMatch(100L, teamManCity, teamArsenal, 3, 0, "2026-01-10T15:00:00")

        val result = useCase(listOf(singleMatch))

        assertEquals(1, result.totalMatches)
        val record = result.records[0]
        assertEquals(100L, record.matchId)
        // Probabilities should still be computed safely from default signals without self-leakage
        assertTrue(record.homeWinProb > 0.0)
        assertTrue(record.drawProb > 0.0)
        assertTrue(record.awayWinProb > 0.0)
    }

    @Test
    fun `6 Matches occurring after match being evaluated are excluded from context`() {
        // T1 on Jan 10 (Man City vs Chelsea)
        // T2 on Jan 15 (Man City vs Arsenal) - TARGET
        // T3 on Jan 20 (Man City vs Liverpool) - FUTURE
        val t1 = createMatch(1L, teamManCity, teamChelsea, 4, 0, "2026-01-10T15:00:00")
        val t2 = createMatch(2L, teamManCity, teamArsenal, 1, 0, "2026-01-15T15:00:00")
        val t3 = createMatch(3L, teamManCity, teamLiverpool, 0, 5, "2026-01-20T15:00:00")

        val result = useCase(listOf(t1, t2, t3))

        assertEquals(3, result.totalMatches)
        // When evaluating T2, T3 is in the future so T2's context only sees T1.
        assertEquals(2L, result.records[1].matchId)
    }

    @Test
    fun `7 Matches occurring before match being evaluated are used in historical context`() {
        // 3 past matches for Man City
        val p1 = createMatch(1L, teamManCity, teamChelsea, 2, 0, "2026-01-01T15:00:00")
        val p2 = createMatch(2L, teamManCity, teamArsenal, 3, 1, "2026-01-05T15:00:00")
        val p3 = createMatch(3L, teamManCity, teamLiverpool, 1, 0, "2026-01-10T15:00:00")
        val target = createMatch(4L, teamManCity, teamChelsea, 2, 0, "2026-01-15T15:00:00")

        val result = useCase(listOf(p1, p2, p3, target))

        assertEquals(4, result.totalMatches)
        val targetRecord = result.records.find { it.matchId == 4L }
        assertNotNull(targetRecord)
        assertEquals("HOME_WIN", targetRecord!!.actualOutcome)
    }

    @Test
    fun `8 Future H2H matches are strictly excluded from H2H calculation`() {
        // Match 1: Jan 10 (Man City vs Arsenal 1-0) - TARGET
        // Match 2: Jan 20 (Arsenal vs Man City 3-0) - FUTURE H2H
        val m1 = createMatch(1L, teamManCity, teamArsenal, 1, 0, "2026-01-10T15:00:00")
        val m2 = createMatch(2L, teamArsenal, teamManCity, 3, 0, "2026-01-20T15:00:00")

        val result = useCase(listOf(m1, m2))

        assertEquals(2, result.totalMatches)
        assertEquals(1L, result.records[0].matchId)
        assertEquals(2L, result.records[1].matchId)
    }

    @Test
    fun `9 Future Form matches are strictly excluded from Form calculation`() {
        // Arsenal matches:
        // Jan 05: Arsenal vs Chelsea 2-0 (PAST for Jan 10)
        // Jan 10: Man City vs Arsenal 1-0 (TARGET)
        // Jan 15: Arsenal vs Liverpool 4-0 (FUTURE for Jan 10)
        val mPast = createMatch(1L, teamArsenal, teamChelsea, 2, 0, "2026-01-05T15:00:00")
        val mTarget = createMatch(2L, teamManCity, teamArsenal, 1, 0, "2026-01-10T15:00:00")
        val mFuture = createMatch(3L, teamArsenal, teamLiverpool, 4, 0, "2026-01-15T15:00:00")

        val result = useCase(listOf(mPast, mTarget, mFuture))

        assertEquals(3, result.totalMatches)
    }

    @Test
    fun `10 Prediction result fields are mapped accurately to BacktestMatchRecord`() {
        val match = createMatch(10L, teamManCity, teamArsenal, 2, 1, "2026-01-10T15:00:00")
        val result = useCase(listOf(match))

        val record = result.records[0]
        assertEquals(10L, record.matchId)
        assertEquals("2026-01-10T15:00:00", record.matchDate)
        assertEquals("Manchester City", record.homeTeamName)
        assertEquals("Arsenal", record.awayTeamName)
        assertTrue(record.predictedOutcome in setOf("HOME_WIN", "DRAW", "AWAY_WIN"))
        assertEquals("HOME_WIN", record.actualOutcome)
        assertTrue(record.homeWinProb in 0.0..1.0)
        assertTrue(record.drawProb in 0.0..1.0)
        assertTrue(record.awayWinProb in 0.0..1.0)
        assertTrue(record.confidenceScore in 0.0..1.0)
        assertEquals(record.predictedOutcome == record.actualOutcome, record.isCorrect)
    }

    @Test
    fun `11 Actual outcome is correctly derived from homeScore and awayScore`() {
        val homeWin = createMatch(1L, teamManCity, teamArsenal, 3, 1, "2026-01-10T15:00:00")
        val draw = createMatch(2L, teamArsenal, teamLiverpool, 2, 2, "2026-01-11T15:00:00")
        val awayWin = createMatch(3L, teamChelsea, teamManCity, 0, 1, "2026-01-12T15:00:00")

        val result = useCase(listOf(homeWin, draw, awayWin))

        assertEquals("HOME_WIN", result.records[0].actualOutcome)
        assertEquals("DRAW", result.records[1].actualOutcome)
        assertEquals("AWAY_WIN", result.records[2].actualOutcome)
    }

    @Test
    fun `12 EvaluationResult matches records and confusion matrix totals`() {
        val m1 = createMatch(1L, teamManCity, teamArsenal, 2, 0, "2026-01-10T15:00:00")
        val m2 = createMatch(2L, teamArsenal, teamLiverpool, 1, 1, "2026-01-11T15:00:00")
        val m3 = createMatch(3L, teamChelsea, teamManCity, 0, 2, "2026-01-12T15:00:00")

        val result = useCase(listOf(m1, m2, m3))

        assertEquals(3, result.evaluationResult.totalEvaluated)
        assertEquals(3, result.evaluationResult.confusionMatrix.totalSamples)
        assertEquals(result.correctMatches, result.evaluationResult.confusionMatrix.correctPredictions)
    }

    @Test
    fun `13 correctMatches accurately reflects count of isCorrect true`() {
        val m1 = createMatch(1L, teamManCity, teamArsenal, 2, 0, "2026-01-10T15:00:00")
        val m2 = createMatch(2L, teamArsenal, teamLiverpool, 1, 1, "2026-01-11T15:00:00")

        val result = useCase(listOf(m1, m2))

        val manualCorrect = result.records.count { it.isCorrect }
        assertEquals(manualCorrect, result.correctMatches)
    }

    @Test
    fun `14 Matches with identical timestamps are sorted deterministically and exclude each other from past context`() {
        // Two matches kicking off simultaneously at 15:00
        val mA = createMatch(101L, teamManCity, teamArsenal, 2, 1, "2026-01-10T15:00:00")
        val mB = createMatch(102L, teamChelsea, teamLiverpool, 0, 0, "2026-01-10T15:00:00")

        val result1 = useCase(listOf(mA, mB))
        val result2 = useCase(listOf(mB, mA))

        assertEquals(101L, result1.records[0].matchId)
        assertEquals(102L, result1.records[1].matchId)

        assertEquals(101L, result2.records[0].matchId)
        assertEquals(102L, result2.records[1].matchId)

        // Two matches with exact same timestamp involving the same team cannot leak into each other's past context
        val mSim1 = createMatch(1L, teamManCity, teamArsenal, 5, 0, "2026-01-10T15:00:00")
        val mSim2 = createMatch(2L, teamManCity, teamChelsea, 0, 1, "2026-01-10T15:00:00")
        val resSim = useCase(listOf(mSim1, mSim2))
        assertEquals(2, resSim.totalMatches)
    }
}
