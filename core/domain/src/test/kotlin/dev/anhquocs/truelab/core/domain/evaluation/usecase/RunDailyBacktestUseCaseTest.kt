package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.evaluation.model.DailyBacktestProgressEvent
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.odds.model.MatchOdds
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateDynamicEloUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunDailyBacktestUseCaseTest {

    private lateinit var fakeOddsRepository: FakeTestOddsRepository
    private lateinit var runDailyBacktestUseCase: RunDailyBacktestUseCase

    private val arsenal = TeamSummary(1, "Arsenal")
    private val chelsea = TeamSummary(2, "Chelsea")
    private val liverpool = TeamSummary(3, "Liverpool")
    private val manCity = TeamSummary(4, "Man City")

    @Before
    fun setUp() {
        fakeOddsRepository = FakeTestOddsRepository()
        runDailyBacktestUseCase = RunDailyBacktestUseCase(
            oddsRepository = fakeOddsRepository,
            predictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
            calculateDynamicEloUseCase = CalculateDynamicEloUseCase(),
            calculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase(),
            maxConcurrency = 3
        )
    }

    @Test
    fun invoke_withZeroTargetMatches_emitsCompletedWithZeroMetrics() = runBlocking {
        val events = runDailyBacktestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = emptyList(),
            allMatches = emptyList()
        ).toList()

        assertEquals(1, events.size)
        val completed = events.first() as DailyBacktestProgressEvent.Completed
        assertEquals(0, completed.result.totalMatches)
        assertEquals(0, completed.result.correctMatches)
        assertEquals(0.0, completed.result.accuracy, 0.001)
        assertEquals(0.0, completed.result.oddsCoverage.coveragePercentage, 0.001)
    }

    @Test
    fun invoke_withTargetMatches_hydratesOddsAndEvaluatesCorrectly() = runBlocking {
        val targetMatch = Match(
            id = 101L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 2,
            awayScore = 0,
            startTimeDate = "2026-10-02T19:00:00",
            status = MatchStatus.ENDED
        )

        // Provide pre-match odds before kickoff (1790964000 = 2026-10-02T18:00:00Z)
        fakeOddsRepository.setOdds(
            101L,
            listOf(
                OddsRecordItem(
                    companyId = 1,
                    companyName = "Bet365",
                    oddsType = "eu",
                    homeWin = 1.5,
                    draw = 4.0,
                    awayWin = 6.0,
                    changeTime = 1790964000L,
                    marketPhase = "instant"
                )
            )
        )

        val events = runDailyBacktestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(targetMatch),
            allMatches = listOf(targetMatch),
            initialEloMap = mapOf(1 to 1900.0, 2 to 1700.0)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        assertEquals(1, completed.result.totalMatches)
        assertEquals(1, completed.result.correctMatches)
        assertEquals(1.0, completed.result.accuracy, 0.001)
        assertEquals(100.0, completed.result.oddsCoverage.coveragePercentage, 0.001)
        assertEquals(1, completed.result.oddsCoverage.matchesWithUsableOdds)
        assertEquals(0, completed.result.oddsCoverage.matchesWithoutOdds)

        val record = completed.result.records.first()
        assertEquals(101L, record.matchId)
        assertEquals("HOME_WIN", record.predictedOutcome)
        assertEquals("HOME_WIN", record.actualOutcome)
        assertTrue(record.isCorrect)
        assertTrue(record.hasUsableOdds)
        assertEquals(2, record.homeScore)
        assertEquals(0, record.awayScore)
    }

    @Test
    fun invoke_withMissingOdds_handlesMissingSignalGracefullyAndRecordsCoverage() = runBlocking {
        val targetMatch1 = Match(
            id = 201L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 1,
            awayScore = 0,
            startTimeDate = "2026-10-02T15:00:00",
            status = MatchStatus.ENDED
        )
        val targetMatch2 = Match(
            id = 202L,
            homeTeam = liverpool,
            awayTeam = manCity,
            homeScore = 0,
            awayScore = 2,
            startTimeDate = "2026-10-02T17:30:00",
            status = MatchStatus.ENDED
        )

        // Only match 201 has odds; match 202 has NO odds
        fakeOddsRepository.setOdds(
            201L,
            listOf(
                OddsRecordItem(
                    companyId = 1,
                    companyName = "Crown",
                    oddsType = "eu",
                    homeWin = 1.8,
                    draw = 3.5,
                    awayWin = 4.5,
                    changeTime = 1790949600L,
                    marketPhase = "initial"
                )
            )
        )

        val events = runDailyBacktestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(targetMatch1, targetMatch2),
            allMatches = listOf(targetMatch1, targetMatch2)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        assertEquals(2, completed.result.totalMatches)
        assertEquals(1, completed.result.oddsCoverage.matchesWithUsableOdds)
        assertEquals(1, completed.result.oddsCoverage.matchesWithoutOdds)
        assertEquals(50.0, completed.result.oddsCoverage.coveragePercentage, 0.001)

        val record1 = completed.result.records.find { it.matchId == 201L }!!
        val record2 = completed.result.records.find { it.matchId == 202L }!!

        assertTrue(record1.hasUsableOdds)
        assertFalse(record2.hasUsableOdds)
    }

    @Test
    fun invoke_strictlyEnforcesTemporalIntegrity_noDataLeakage() = runBlocking {
        val historicalPrior = Match(
            id = 1L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 3,
            awayScore = 0,
            startTimeDate = "2026-09-01T15:00:00",
            status = MatchStatus.ENDED
        )
        val targetMatch = Match(
            id = 2L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 1,
            awayScore = 1,
            startTimeDate = "2026-10-02T15:00:00",
            status = MatchStatus.ENDED
        )
        val futureMatch = Match(
            id = 3L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 0,
            awayScore = 5,
            startTimeDate = "2026-11-01T15:00:00",
            status = MatchStatus.ENDED
        )

        // Post-kickoff odds (Live/rolling ball) - must NOT be used (1790958600 = 2026-10-02T16:30:00Z after 15:00:00Z)
        fakeOddsRepository.setOdds(
            2L,
            listOf(
                OddsRecordItem(
                    companyId = 1,
                    companyName = "Bet365",
                    oddsType = "european",
                    homeWin = 1.05,
                    draw = 12.0,
                    awayWin = 25.0,
                    changeTime = 1790958600L,
                    marketPhase = "rolling_ball"
                )
            )
        )

        val events = runDailyBacktestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(targetMatch),
            allMatches = listOf(historicalPrior, targetMatch, futureMatch)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        val record = completed.result.records.first()

        // Post-kickoff odds rejected -> hasUsableOdds = false
        assertFalse(record.hasUsableOdds)
        assertEquals(0.0, completed.result.oddsCoverage.coveragePercentage, 0.001)
    }

    @Test
    fun invoke_withPenaltyMatch_preservesPenaltyScoresAndKeepsDrawOutcome() = runBlocking {
        val targetMatch = Match(
            id = 301L,
            homeTeam = arsenal,
            awayTeam = chelsea,
            homeScore = 2,
            awayScore = 2,
            isPenalty = true,
            homePenaltyScore = 4,
            awayPenaltyScore = 2,
            startTimeDate = "2026-10-02T19:00:00",
            status = MatchStatus.ENDED
        )

        val events = runDailyBacktestUseCase(
            evaluationDate = "2026-10-02",
            targetMatches = listOf(targetMatch),
            allMatches = listOf(targetMatch)
        ).toList()

        val completed = events.last() as DailyBacktestProgressEvent.Completed
        val record = completed.result.records.first()

        assertEquals("DRAW", record.actualOutcome)
        assertTrue(record.isPenalty)
        assertEquals(4, record.homePenaltyScore)
        assertEquals(2, record.awayPenaltyScore)
    }

    private class FakeTestOddsRepository : OddsRepository {
        private val oddsMap = HashMap<Long, List<OddsRecordItem>>()

        fun setOdds(matchId: Long, odds: List<OddsRecordItem>) {
            oddsMap[matchId] = odds
        }

        override fun getMatchOdds(matchId: Long): Flow<MatchOdds> {
            val list = oddsMap[matchId] ?: emptyList()
            return flowOf(MatchOdds(matchId = matchId, oddsList = list))
        }

        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsRecordItem>> {
            return flowOf(oddsMap[matchId] ?: emptyList())
        }

        override suspend fun getLatestEuropeanOddsMap(): Map<Long, OddsRecordItem> {
            return emptyMap()
        }

        override suspend fun fetchAndCacheOddsForMatch(matchId: Long): Result<Unit> {
            return Result.success(Unit)
        }
    }
}
