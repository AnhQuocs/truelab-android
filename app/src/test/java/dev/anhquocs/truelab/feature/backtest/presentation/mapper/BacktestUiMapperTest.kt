package dev.anhquocs.truelab.feature.backtest.presentation.mapper

import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.evaluation.model.BacktestMatchRecord
import dev.anhquocs.truelab.core.domain.evaluation.model.ClassEvaluationMetrics
import dev.anhquocs.truelab.core.domain.evaluation.model.ConfusionMatrix3Way
import dev.anhquocs.truelab.core.domain.evaluation.model.ModelEvaluationResult
import dev.anhquocs.truelab.core.domain.evaluation.model.PredictionBacktestResult
import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestUiMapperTest {

    @Test
    fun toOverviewUiRecord_formatsAccuraciesAndPercentagesCorrectly() {
        val evalResult = ModelEvaluationResult(
            accuracy = 0.666666,
            macroPrecision = 0.70123,
            macroRecall = 0.65432,
            macroF1 = 0.67891,
            homeMetrics = ClassEvaluationMetrics(0.8, 0.7, 0.75, 10),
            drawMetrics = ClassEvaluationMetrics(0.5, 0.4, 0.44, 5),
            awayMetrics = ClassEvaluationMetrics(0.8, 0.8, 0.8, 15),
            confusionMatrix = ConfusionMatrix3Way(),
            totalEvaluated = 30
        )

        val backtestResult = PredictionBacktestResult(
            evaluationResult = evalResult,
            records = emptyList(),
            totalMatches = 30,
            correctMatches = 20
        )

        val overview = BacktestUiMapper.toOverviewUiRecord(backtestResult)

        assertEquals(30, overview.totalMatches)
        assertEquals(20, overview.correctMatches)
        assertEquals("66.7%", overview.formattedAccuracy)
        assertEquals("70.1%", overview.formattedMacroPrecision)
        assertEquals("65.4%", overview.formattedMacroRecall)
        assertEquals("67.9%", overview.formattedMacroF1)
    }

    @Test
    fun toConfusionMatrixUiRecord_mapsCellsAndHighlightsDiagonal() {
        val matrix = ConfusionMatrix3Way(
            homeAsHome = 8,
            homeAsDraw = 2,
            homeAsAway = 0,
            drawAsHome = 1,
            drawAsDraw = 3,
            drawAsAway = 1,
            awayAsHome = 1,
            awayAsDraw = 2,
            awayAsAway = 7
        )

        val uiRecord = BacktestUiMapper.toConfusionMatrixUiRecord(matrix)

        assertEquals(25, uiRecord.totalSamples)
        // Check diagonal
        assertTrue(uiRecord.homeAsHome.isDiagonal)
        assertTrue(uiRecord.drawAsDraw.isDiagonal)
        assertTrue(uiRecord.awayAsAway.isDiagonal)
        assertEquals(8, uiRecord.homeAsHome.count)
        assertEquals("32.0%", uiRecord.homeAsHome.formattedPercentage)

        // Check non-diagonal
        assertFalse(uiRecord.homeAsDraw.isDiagonal)
        assertEquals(2, uiRecord.homeAsDraw.count)
        assertEquals("8.0%", uiRecord.homeAsDraw.formattedPercentage)
    }

    @Test
    fun toClassMetricUiRecords_mapsAllThreeClasses() {
        val evalResult = ModelEvaluationResult(
            accuracy = 0.5,
            macroPrecision = 0.5,
            macroRecall = 0.5,
            macroF1 = 0.5,
            homeMetrics = ClassEvaluationMetrics(0.8, 0.6, 0.68, 10),
            drawMetrics = ClassEvaluationMetrics(0.3, 0.2, 0.24, 5),
            awayMetrics = ClassEvaluationMetrics(0.7, 0.7, 0.7, 8),
            confusionMatrix = ConfusionMatrix3Way(),
            totalEvaluated = 23
        )

        val classMetrics = BacktestUiMapper.toClassMetricUiRecords(evalResult)

        assertEquals(3, classMetrics.size)
        // Home
        assertEquals(R.string.prediction_outcome_home, classMetrics[0].classLabelRes)
        assertEquals("80.0%", classMetrics[0].formattedPrecision)
        assertEquals("60.0%", classMetrics[0].formattedRecall)
        assertEquals(10, classMetrics[0].support)

        // Draw
        assertEquals(R.string.prediction_outcome_draw, classMetrics[1].classLabelRes)
        assertEquals("30.0%", classMetrics[1].formattedPrecision)
        assertEquals(5, classMetrics[1].support)

        // Away
        assertEquals(R.string.prediction_outcome_away, classMetrics[2].classLabelRes)
        assertEquals("70.0%", classMetrics[2].formattedPrecision)
        assertEquals(8, classMetrics[2].support)
    }

    @Test
    fun toMatchUiRecords_convertsProbabilitiesAndBadgesCorrectly() {
        val records = listOf(
            BacktestMatchRecord(
                matchId = 1L,
                matchDate = "2026-03-01",
                homeTeamName = "Arsenal",
                awayTeamName = "Chelsea",
                predictedOutcome = CalculateEvaluationMetricsUseCase.LABEL_HOME_WIN,
                actualOutcome = CalculateEvaluationMetricsUseCase.LABEL_HOME_WIN,
                homeWinProb = 0.55,
                drawProb = 0.25,
                awayWinProb = 0.20,
                confidenceScore = 0.78,
                isCorrect = true
            ),
            BacktestMatchRecord(
                matchId = 2L,
                matchDate = "2026-03-02",
                homeTeamName = "Liverpool",
                awayTeamName = "Man City",
                predictedOutcome = CalculateEvaluationMetricsUseCase.LABEL_DRAW,
                actualOutcome = CalculateEvaluationMetricsUseCase.LABEL_AWAY_WIN,
                homeWinProb = 0.30,
                drawProb = 0.40,
                awayWinProb = 0.30,
                confidenceScore = 0.45,
                isCorrect = false
            )
        )

        val uiMatches = BacktestUiMapper.toMatchUiRecords(records)

        assertEquals(2, uiMatches.size)
        assertEquals("Arsenal", uiMatches[0].homeTeamName)
        assertEquals(R.string.prediction_outcome_home, uiMatches[0].predictedOutcomeRes)
        assertEquals(R.string.prediction_outcome_home, uiMatches[0].actualOutcomeRes)
        assertEquals(55, uiMatches[0].homeWinPct)
        assertEquals(25, uiMatches[0].drawPct)
        assertEquals(20, uiMatches[0].awayWinPct)
        assertEquals(78, uiMatches[0].confidencePct)
        assertTrue(uiMatches[0].isCorrect)

        assertEquals("Liverpool", uiMatches[1].homeTeamName)
        assertEquals(R.string.prediction_outcome_draw, uiMatches[1].predictedOutcomeRes)
        assertEquals(R.string.prediction_outcome_away, uiMatches[1].actualOutcomeRes)
        assertFalse(uiMatches[1].isCorrect)
    }
}
