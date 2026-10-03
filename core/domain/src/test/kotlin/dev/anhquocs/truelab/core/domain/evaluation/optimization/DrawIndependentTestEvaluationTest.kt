package dev.anhquocs.truelab.core.domain.evaluation.optimization

import dev.anhquocs.truelab.core.domain.evaluation.model.CandidateAOptimizationConfig
import dev.anhquocs.truelab.core.domain.evaluation.model.DrawMarginParameterGrid
import dev.anhquocs.truelab.core.domain.evaluation.model.TemporalDatasetSplitter
import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.EvaluateDrawModelsOnIndependentTestUseCase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.OptimizeCandidateADrawUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.prediction.model.DrawMarginConfig
import dev.anhquocs.truelab.core.domain.prediction.model.DynamicDrawPriorConfig
import dev.anhquocs.truelab.core.domain.prediction.model.MatchPredictionContext
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Bộ kiểm thử toàn diện cho Phase B5: Calibration Candidate A, Đóng băng tham số, Brier Score và Independent Test Evaluation.
 */
class DrawIndependentTestEvaluationTest {

    private lateinit var predictUseCase: PredictMatchOutcomeUseCase
    private lateinit var metricsUseCase: CalculateEvaluationMetricsUseCase
    private lateinit var optimizeCandidateAUseCase: OptimizeCandidateADrawUseCase
    private lateinit var evaluateIndependentTestUseCase: EvaluateDrawModelsOnIndependentTestUseCase

    private val teamA = TeamSummary(1, "Arsenal")
    private val teamB = TeamSummary(2, "Chelsea")
    private val teamC = TeamSummary(3, "Liverpool")

    @Before
    fun setUp() {
        predictUseCase = PredictMatchOutcomeUseCase()
        metricsUseCase = CalculateEvaluationMetricsUseCase()
        optimizeCandidateAUseCase = OptimizeCandidateADrawUseCase(
            predictMatchOutcomeUseCase = predictUseCase,
            calculateEvaluationMetricsUseCase = metricsUseCase
        )
        evaluateIndependentTestUseCase = EvaluateDrawModelsOnIndependentTestUseCase(
            predictMatchOutcomeUseCase = predictUseCase,
            calculateEvaluationMetricsUseCase = metricsUseCase
        )
    }

    // ==========================================
    // 1. Candidate A Grid Tests
    // ==========================================

    @Test
    fun `Test 1 - Candidate A grid generates exactly 42 distinct configurations`() {
        val grid = DrawMarginParameterGrid.generateCandidateAGrid()
        assertEquals(42, grid.size)
        assertEquals(42, grid.distinct().size)
    }

    @Test
    fun `Test 2 - Candidate A grid boundaries match specifications`() {
        val grid = DrawMarginParameterGrid.generateCandidateAGrid()
        val deltas = grid.map { it.deltaMargin }.toSet()
        val thetas = grid.map { it.thetaMinProb }.toSet()

        assertEquals(setOf(0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08), deltas)
        assertEquals(setOf(0.250, 0.255, 0.260, 0.265, 0.270, 0.275), thetas)
    }

    // ==========================================
    // 2. Brier Score Calculation Tests
    // ==========================================

    @Test
    fun `Test 3 - Brier score on perfect predictions is zero`() {
        val samples = listOf(
            CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                homeProb = 1.0, drawProb = 0.0, awayProb = 0.0, actualOutcome = "HOME_WIN"
            ),
            CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                homeProb = 0.0, drawProb = 1.0, awayProb = 0.0, actualOutcome = "DRAW"
            ),
            CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                homeProb = 0.0, drawProb = 0.0, awayProb = 1.0, actualOutcome = "AWAY_WIN"
            )
        )
        val score = metricsUseCase.calculateBrierScore(samples)
        assertEquals(0.0, score, 1e-6)
    }

    @Test
    fun `Test 4 - Brier score on worst predictions is 2_0`() {
        val samples = listOf(
            CalculateEvaluationMetricsUseCase.ProbabilisticEvaluationSample(
                homeProb = 0.0, drawProb = 0.0, awayProb = 1.0, actualOutcome = "HOME_WIN"
            )
        )
        // (0-1)^2 + (0-0)^2 + (1-0)^2 = 1 + 1 = 2
        val score = metricsUseCase.calculateBrierScore(samples)
        assertEquals(2.0, score, 1e-6)
    }

    @Test
    fun `Test 5 - Brier score on empty list returns zero safely`() {
        val score = metricsUseCase.calculateBrierScore(emptyList())
        assertEquals(0.0, score, 1e-6)
    }

    // ==========================================
    // 3. Optimization and Independent Test Evaluation Tests
    // ==========================================

    @Test
    fun `Test 6 - Candidate A optimization on synthetic matches selects valid best configuration`() {
        val matches = listOf(
            createMatch(1, teamA, teamB, 1, 1, "2023-01-01T15:00:00Z"),
            createMatch(2, teamB, teamC, 2, 1, "2023-01-02T15:00:00Z"),
            createMatch(3, teamA, teamC, 0, 0, "2023-01-03T15:00:00Z"),
            createMatch(4, teamC, teamA, 0, 2, "2023-01-04T15:00:00Z"),
            createMatch(5, teamB, teamA, 1, 1, "2023-01-05T15:00:00Z")
        )

        val result = optimizeCandidateAUseCase(
            matches = matches,
            optimizationConfig = CandidateAOptimizationConfig(validationRatio = 1.0)
        )

        assertNotNull(result)
        assertEquals(42, result.totalConfigurations)
        assertEquals(42, result.allRecords.size)
    }

    @Test
    fun `Test 7 - Independent test evaluation executes all 3 models with frozen parameters`() {
        val testContexts = listOf(
            Pair(
                MatchPredictionContext(
                    matchId = 101L,
                    homeTeamId = teamA.id,
                    awayTeamId = teamB.id,
                    matchStartTimeDate = "2023-05-01T15:00:00Z",
                    homeElo = 1500.0,
                    awayElo = 1500.0
                ),
                "DRAW"
            ),
            Pair(
                MatchPredictionContext(
                    matchId = 102L,
                    homeTeamId = teamB.id,
                    awayTeamId = teamC.id,
                    matchStartTimeDate = "2023-05-02T15:00:00Z",
                    homeElo = 1600.0,
                    awayElo = 1400.0
                ),
                "HOME_WIN"
            )
        )

        val frozenA = DrawMarginConfig(deltaMargin = 0.05, thetaMinProb = 0.26)
        val frozenB = DynamicDrawPriorConfig(maxDrawProb = 0.38, minDrawProb = 0.12, eloSigma = 1.0, formSigma = 0.30)

        val result = evaluateIndependentTestUseCase(
            testContexts = testContexts,
            frozenCandidateAConfig = frozenA,
            frozenCandidateBConfig = frozenB
        )

        assertEquals(2, result.totalSamples)
        assertNotNull(result.baselineResult)
        assertNotNull(result.candidateAResult)
        assertNotNull(result.candidateBResult)
        assertTrue(result.baselineBrierScore >= 0.0)
        assertTrue(result.candidateABrierScore >= 0.0)
        assertTrue(result.candidateBBrierScore >= 0.0)

        val deltasA = result.candidateADeltas()
        val deltasB = result.candidateBDeltas()
        assertNotNull(deltasA)
        assertNotNull(deltasB)
    }

    @Test
    fun `Test 8 - Temporal split guarantees chronological order and no overlap`() {
        val items = (1..100).toList()
        val split = TemporalDatasetSplitter.splitChronological(items, validationRatio = 0.70)

        assertEquals(70, split.calibrationValidation.size)
        assertEquals(30, split.independentTest.size)
        assertEquals((1..70).toList(), split.calibrationValidation)
        assertEquals((71..100).toList(), split.independentTest)
    }

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
}
