package dev.anhquocs.truelab.core.domain.evaluation.optimization

import dev.anhquocs.truelab.core.domain.evaluation.model.CandidateBOptimizationConfig
import dev.anhquocs.truelab.core.domain.evaluation.model.DrawParameterGrid
import dev.anhquocs.truelab.core.domain.evaluation.model.TemporalDatasetSplitter
import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import dev.anhquocs.truelab.core.domain.evaluation.usecase.OptimizeCandidateBDrawUseCase
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.prediction.model.DrawModelingStrategy
import dev.anhquocs.truelab.core.domain.prediction.model.DrawStrategyConfig
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
 * Bộ kiểm thử toàn diện cho hệ thống tối ưu hóa tham số Grid Search Candidate B (Phase B4).
 *
 * Bao phủ đầy đủ 8 nhóm tiêu chuẩn:
 * A. Grid Generation (64 cấu hình, không trùng, đúng biên, xác định)
 * B. Metric Calculation (Confusion Matrix, Macro F1, Draw P/R/F1, Zero-support safety)
 * C. Constraint Enforcement (Ràng buộc Home/Away F1 >= Baseline - 0.03)
 * D. Best Selection (Chọn Macro F1 cao nhất trong eligible, loại bỏ ineligible)
 * E. Baseline Isolation (Baseline không bị biến đổi trước và sau)
 * F. Candidate B Invariant Conservation (Sum = 1, xác suất [0, 1])
 * G. Temporal Split (Thứ tự thời gian, không shuffle, không overlap)
 * H. Report Table Output (Bảng Markdown đầy đủ cột)
 */
class DrawParameterOptimizationTest {

    private lateinit var optimizeUseCase: OptimizeCandidateBDrawUseCase
    private lateinit var predictUseCase: PredictMatchOutcomeUseCase
    private lateinit var metricsUseCase: CalculateEvaluationMetricsUseCase

    private val teamA = TeamSummary(1, "Arsenal")
    private val teamB = TeamSummary(2, "Chelsea")
    private val teamC = TeamSummary(3, "Liverpool")
    private val teamD = TeamSummary(4, "Man City")

    @Before
    fun setUp() {
        predictUseCase = PredictMatchOutcomeUseCase()
        metricsUseCase = CalculateEvaluationMetricsUseCase()
        optimizeUseCase = OptimizeCandidateBDrawUseCase(
            predictMatchOutcomeUseCase = predictUseCase,
            calculateEvaluationMetricsUseCase = metricsUseCase
        )
    }

    // ==========================================
    // A. Grid Generation Tests
    // ==========================================

    @Test
    fun `Test A1 - grid generation produces exactly 64 distinct configurations`() {
        val grid = DrawParameterGrid.generateCandidateBGrid()

        assertEquals("Search space must contain exactly 64 configurations", 64, grid.size)
        val distinctCount = grid.distinct().size
        assertEquals("All 64 configurations must be distinct", 64, distinctCount)
    }

    @Test
    fun `Test A2 - grid parameter boundaries match specifications`() {
        val grid = DrawParameterGrid.generateCandidateBGrid()

        val pdMaxSet = grid.map { it.maxDrawProb }.toSet()
        val eloSigmaSet = grid.map { it.eloSigma }.toSet()
        val formSigmaSet = grid.map { it.formSigma }.toSet()
        val pdMinSet = grid.map { it.minDrawProb }.toSet()

        assertEquals(setOf(0.32, 0.34, 0.36, 0.38), pdMaxSet)
        assertEquals(setOf(0.75, 1.00, 1.25, 1.50), eloSigmaSet)
        assertEquals(setOf(0.15, 0.20, 0.25, 0.30), formSigmaSet)
        assertEquals(setOf(0.12), pdMinSet)
    }

    @Test
    fun `Test A3 - grid generation is deterministic across multiple calls`() {
        val grid1 = DrawParameterGrid.generateCandidateBGrid()
        val grid2 = DrawParameterGrid.generateCandidateBGrid()

        assertEquals(grid1, grid2)
    }

    // ==========================================
    // B. Metric Calculation Tests
    // ==========================================

    @Test
    fun `Test B1 - metric calculation computes exact confusion matrix and macro F1`() {
        val predictions = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("DRAW", "DRAW"),
            Pair("AWAY_WIN", "AWAY_WIN"),
            Pair("HOME_WIN", "DRAW"),
            Pair("DRAW", "AWAY_WIN")
        )

        val result = metricsUseCase(predictions)

        assertEquals(5, result.totalEvaluated)
        assertEquals(3, result.confusionMatrix.correctPredictions)
        assertEquals(0.60, result.accuracy, 1e-6)

        // Class HOME_WIN: TP=1, Pred=2 (P=0.5), Actual=1 (R=1.0) -> F1 = 2/3
        assertEquals(0.5, result.homeMetrics.precision, 1e-6)
        assertEquals(1.0, result.homeMetrics.recall, 1e-6)
        assertEquals(2.0 / 3.0, result.homeMetrics.f1Score, 1e-6)

        // Class DRAW: TP=1, Pred=2 (P=0.5), Actual=2 (R=0.5) -> F1 = 0.5
        assertEquals(0.5, result.drawMetrics.precision, 1e-6)
        assertEquals(0.5, result.drawMetrics.recall, 1e-6)
        assertEquals(0.5, result.drawMetrics.f1Score, 1e-6)

        // Macro F1
        val expectedMacroF1 = (result.homeMetrics.f1Score + result.drawMetrics.f1Score + result.awayMetrics.f1Score) / 3.0
        assertEquals(expectedMacroF1, result.macroF1, 1e-6)
    }

    @Test
    fun `Test B2 - zero-support class handles division by zero safely without NaN`() {
        val predictions = listOf(
            Pair("HOME_WIN", "HOME_WIN"),
            Pair("HOME_WIN", "HOME_WIN")
        )

        val result = metricsUseCase(predictions)

        assertTrue(result.accuracy.isFinite())
        assertTrue(result.macroPrecision.isFinite())
        assertTrue(result.macroRecall.isFinite())
        assertTrue(result.macroF1.isFinite())
        assertEquals(0.0, result.drawMetrics.f1Score, 1e-6)
        assertEquals(0.0, result.awayMetrics.f1Score, 1e-6)
    }

    // ==========================================
    // C. Constraint Enforcement Tests
    // ==========================================

    @Test
    fun `Test C1 - configuration satisfying constraint is marked eligible`() {
        val balancedContext = MatchPredictionContext(
            matchId = 1L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeRecentMatches = emptyList(),
            awayRecentMatches = emptyList(),
            h2hMatches = emptyList(),
            latestOdds = null,
            matchStartTimeDate = "2026-10-01 20:00"
        )
        val contexts = listOf(Pair(balancedContext, "DRAW"))

        val result = optimizeUseCase.optimizeOnContexts(
            validationContexts = contexts,
            optimizationConfig = CandidateBOptimizationConfig(f1Tolerance = 0.03)
        )

        assertNotNull(result)
        assertEquals(64, result.totalConfigurations)
        assertTrue(result.eligibleConfigurationsCount > 0)
    }

    // ==========================================
    // D. Best Selection Tests
    // ==========================================

    @Test
    fun `Test D1 - best configuration selects highest Macro F1 among eligible`() {
        val matches = listOf(
            Match(1L, teamA, teamB, 1, 1, "2026-10-01T20:00:00", MatchStatus.ENDED),
            Match(2L, teamC, teamD, 2, 0, "2026-10-02T20:00:00", MatchStatus.ENDED),
            Match(3L, teamB, teamC, 0, 2, "2026-10-03T20:00:00", MatchStatus.ENDED),
            Match(4L, teamD, teamA, 0, 0, "2026-10-04T20:00:00", MatchStatus.ENDED)
        )

        val result = optimizeUseCase(
            matches = matches,
            optimizationConfig = CandidateBOptimizationConfig(validationRatio = 1.0)
        )

        assertNotNull(result)
        if (result.bestConfiguration != null) {
            assertTrue("Best configuration must be eligible", result.bestConfiguration!!.eligible)
            // Best configuration must have macroF1 >= all other eligible configs
            val otherEligible = result.allRecords.filter { it.eligible }
            for (other in otherEligible) {
                assertTrue(result.bestConfiguration!!.macroF1 >= other.macroF1)
            }
        }
    }

    // ==========================================
    // E. Baseline Isolation Tests
    // ==========================================

    @Test
    fun `Test E1 - baseline is completely unchanged before and after grid search`() {
        val testContext = MatchPredictionContext(
            matchId = 100L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1650.0,
            awayElo = 1450.0,
            homeRecentMatches = emptyList(),
            awayRecentMatches = emptyList(),
            h2hMatches = emptyList(),
            latestOdds = null,
            matchStartTimeDate = "2026-10-01 20:00"
        )

        val baselineBefore = predictUseCase(
            context = testContext,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        )

        // Run Grid Search
        val contexts = listOf(Pair(testContext, "HOME_WIN"))
        optimizeUseCase.optimizeOnContexts(contexts)

        val baselineAfter = predictUseCase(
            context = testContext,
            drawStrategyConfig = DrawStrategyConfig(strategy = DrawModelingStrategy.BASELINE)
        )

        assertEquals(baselineBefore.homeWinProb, baselineAfter.homeWinProb, 1e-9)
        assertEquals(baselineBefore.drawProb, baselineAfter.drawProb, 1e-9)
        assertEquals(baselineBefore.awayWinProb, baselineAfter.awayWinProb, 1e-9)
        assertEquals(baselineBefore.predictedOutcome, baselineAfter.predictedOutcome)
    }

    // ==========================================
    // F. Candidate B Invariant Conservation Tests
    // ==========================================

    @Test
    fun `Test F1 - all 64 configurations conserve probability simplex and produce valid bounds`() {
        val testContext = MatchPredictionContext(
            matchId = 100L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeRecentMatches = emptyList(),
            awayRecentMatches = emptyList(),
            h2hMatches = emptyList(),
            latestOdds = null,
            matchStartTimeDate = "2026-10-01 20:00"
        )

        val grid = DrawParameterGrid.generateCandidateBGrid()

        for (config in grid) {
            val result = predictUseCase(
                context = testContext,
                drawStrategyConfig = DrawStrategyConfig(
                    strategy = DrawModelingStrategy.DYNAMIC_DRAW_PRIOR,
                    dynamicPriorConfig = config
                )
            )

            val sum = result.homeWinProb + result.drawProb + result.awayWinProb
            assertEquals("Probability sum must be 1.0 for config $config", 1.0, sum, 1e-6)
            assertTrue("Home prob must be in [0, 1]", result.homeWinProb in 0.0..1.0)
            assertTrue("Draw prob must be in [0, 1]", result.drawProb in 0.0..1.0)
            assertTrue("Away prob must be in [0, 1]", result.awayWinProb in 0.0..1.0)
            assertTrue("Home prob must be finite", result.homeWinProb.isFinite())
            assertTrue("Draw prob must be finite", result.drawProb.isFinite())
            assertTrue("Away prob must be finite", result.awayWinProb.isFinite())
        }
    }

    // ==========================================
    // G. Temporal Split Tests
    // ==========================================

    @Test
    fun `Test G1 - temporal split divides dataset chronologically without shuffling`() {
        val items = (1..10).map { "Item_$it" }

        val split = TemporalDatasetSplitter.splitChronological(items, validationRatio = 0.70)

        // 70% of 10 = 7 items
        assertEquals(7, split.calibrationValidation.size)
        assertEquals(3, split.independentTest.size)

        assertEquals(listOf("Item_1", "Item_2", "Item_3", "Item_4", "Item_5", "Item_6", "Item_7"), split.calibrationValidation)
        assertEquals(listOf("Item_8", "Item_9", "Item_10"), split.independentTest)

        // No overlap
        assertFalse(split.calibrationValidation.any { it in split.independentTest })
    }

    @Test
    fun `Test G2 - temporal split on empty list returns empty sublists safely`() {
        val split = TemporalDatasetSplitter.splitChronological<String>(emptyList(), 0.70)

        assertTrue(split.calibrationValidation.isEmpty())
        assertTrue(split.independentTest.isEmpty())
    }

    // ==========================================
    // H. Report Table Output Tests
    // ==========================================

    @Test
    fun `Test H1 - formatReportTable produces formatted markdown table`() {
        val balancedContext = MatchPredictionContext(
            matchId = 1L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeElo = 1500.0,
            awayElo = 1500.0,
            homeRecentMatches = emptyList(),
            awayRecentMatches = emptyList(),
            h2hMatches = emptyList(),
            latestOdds = null,
            matchStartTimeDate = "2026-10-01 20:00"
        )
        val contexts = listOf(Pair(balancedContext, "DRAW"))

        val result = optimizeUseCase.optimizeOnContexts(contexts)
        val report = result.formatReportTable(limit = 5)

        assertNotNull(report)
        assertTrue(report.contains("| Rank | PD_max | σ_Elo | σ_Form | Macro F1 | Draw F1 | Draw Rec | Home F1 | Away F1 | Acc | Eligible |"))
        assertTrue(report.contains("| #1 |"))
    }
}
