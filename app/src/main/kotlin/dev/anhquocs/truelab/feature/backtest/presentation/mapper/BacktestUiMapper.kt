package dev.anhquocs.truelab.feature.backtest.presentation.mapper

import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.evaluation.model.BacktestMatchRecord
import dev.anhquocs.truelab.core.domain.evaluation.model.ConfusionMatrix3Way
import dev.anhquocs.truelab.core.domain.evaluation.model.ModelEvaluationResult
import dev.anhquocs.truelab.core.domain.evaluation.model.PredictionBacktestResult
import dev.anhquocs.truelab.core.domain.evaluation.usecase.CalculateEvaluationMetricsUseCase
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestMatchUiRecord
import dev.anhquocs.truelab.feature.backtest.presentation.model.BacktestOverviewUiRecord
import dev.anhquocs.truelab.feature.backtest.presentation.model.ClassMetricUiRecord
import dev.anhquocs.truelab.feature.backtest.presentation.model.ConfusionMatrixCellUi
import dev.anhquocs.truelab.feature.backtest.presentation.model.ConfusionMatrixUiRecord
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Mapper formatting Domain evaluation and backtest models into clean Presentation UI records.
 */
object BacktestUiMapper {

    fun toOverviewUiRecord(result: PredictionBacktestResult): BacktestOverviewUiRecord {
        val eval = result.evaluationResult
        val accPct = eval.accuracy * 100.0
        val macroP = eval.macroPrecision * 100.0
        val macroR = eval.macroRecall * 100.0
        val macroF1 = eval.macroF1 * 100.0

        return BacktestOverviewUiRecord(
            totalMatches = result.totalMatches,
            correctMatches = result.correctMatches,
            accuracyPercent = accPct,
            formattedAccuracy = String.format(Locale.US, "%.1f%%", accPct),
            macroPrecisionPercent = macroP,
            formattedMacroPrecision = String.format(Locale.US, "%.1f%%", macroP),
            macroRecallPercent = macroR,
            formattedMacroRecall = String.format(Locale.US, "%.1f%%", macroR),
            macroF1Percent = macroF1,
            formattedMacroF1 = String.format(Locale.US, "%.1f%%", macroF1)
        )
    }

    fun toConfusionMatrixUiRecord(matrix: ConfusionMatrix3Way): ConfusionMatrixUiRecord {
        val total = matrix.totalSamples
        val maxCell = max(
            1,
            maxOf(
                matrix.homeAsHome, matrix.homeAsDraw, matrix.homeAsAway,
                matrix.drawAsHome, matrix.drawAsDraw, matrix.drawAsAway,
                matrix.awayAsHome, matrix.awayAsDraw, matrix.awayAsAway
            )
        )

        fun createCell(count: Int, isDiagonal: Boolean): ConfusionMatrixCellUi {
            val pct = if (total > 0) (count.toDouble() / total) * 100.0 else 0.0
            val intensity = if (maxCell > 0) (count.toFloat() / maxCell) else 0f
            return ConfusionMatrixCellUi(
                count = count,
                percentageOfTotal = pct,
                formattedPercentage = String.format(Locale.US, "%.1f%%", pct),
                isDiagonal = isDiagonal,
                intensityLevel = intensity
            )
        }

        return ConfusionMatrixUiRecord(
            homeAsHome = createCell(matrix.homeAsHome, isDiagonal = true),
            homeAsDraw = createCell(matrix.homeAsDraw, isDiagonal = false),
            homeAsAway = createCell(matrix.homeAsAway, isDiagonal = false),
            drawAsHome = createCell(matrix.drawAsHome, isDiagonal = false),
            drawAsDraw = createCell(matrix.drawAsDraw, isDiagonal = true),
            drawAsAway = createCell(matrix.drawAsAway, isDiagonal = false),
            awayAsHome = createCell(matrix.awayAsHome, isDiagonal = false),
            awayAsDraw = createCell(matrix.awayAsDraw, isDiagonal = false),
            awayAsAway = createCell(matrix.awayAsAway, isDiagonal = true),
            totalSamples = total
        )
    }

    fun toClassMetricUiRecords(result: ModelEvaluationResult): List<ClassMetricUiRecord> {
        val home = result.homeMetrics
        val draw = result.drawMetrics
        val away = result.awayMetrics

        return listOf(
            ClassMetricUiRecord(
                classLabelRes = R.string.prediction_outcome_home,
                precisionPercent = home.precision * 100.0,
                formattedPrecision = String.format(Locale.US, "%.1f%%", home.precision * 100.0),
                recallPercent = home.recall * 100.0,
                formattedRecall = String.format(Locale.US, "%.1f%%", home.recall * 100.0),
                f1Percent = home.f1Score * 100.0,
                formattedF1 = String.format(Locale.US, "%.1f%%", home.f1Score * 100.0),
                support = home.support
            ),
            ClassMetricUiRecord(
                classLabelRes = R.string.prediction_outcome_draw,
                precisionPercent = draw.precision * 100.0,
                formattedPrecision = String.format(Locale.US, "%.1f%%", draw.precision * 100.0),
                recallPercent = draw.recall * 100.0,
                formattedRecall = String.format(Locale.US, "%.1f%%", draw.recall * 100.0),
                f1Percent = draw.f1Score * 100.0,
                formattedF1 = String.format(Locale.US, "%.1f%%", draw.f1Score * 100.0),
                support = draw.support
            ),
            ClassMetricUiRecord(
                classLabelRes = R.string.prediction_outcome_away,
                precisionPercent = away.precision * 100.0,
                formattedPrecision = String.format(Locale.US, "%.1f%%", away.precision * 100.0),
                recallPercent = away.recall * 100.0,
                formattedRecall = String.format(Locale.US, "%.1f%%", away.recall * 100.0),
                f1Percent = away.f1Score * 100.0,
                formattedF1 = String.format(Locale.US, "%.1f%%", away.f1Score * 100.0),
                support = away.support
            )
        )
    }

    fun toMatchUiRecords(records: List<BacktestMatchRecord>): List<BacktestMatchUiRecord> {
        return records.map { record ->
            BacktestMatchUiRecord(
                matchId = record.matchId,
                formattedDate = dev.anhquocs.truelab.core.ui.utils.DateTimeFormatterUtils.formatToVietnamDateTime(record.matchDate),
                homeTeamName = record.homeTeamName,
                awayTeamName = record.awayTeamName,
                predictedOutcomeRes = mapOutcomeToRes(record.predictedOutcome),
                actualOutcomeRes = mapOutcomeToRes(record.actualOutcome),
                homeWinPct = (record.homeWinProb * 100).roundToInt(),
                drawPct = (record.drawProb * 100).roundToInt(),
                awayWinPct = (record.awayWinProb * 100).roundToInt(),
                confidencePct = (record.confidenceScore * 100).roundToInt(),
                isCorrect = record.isCorrect
            )
        }
    }

    private fun mapOutcomeToRes(outcome: String): Int {
        return when (outcome) {
            CalculateEvaluationMetricsUseCase.LABEL_HOME_WIN -> R.string.prediction_outcome_home
            CalculateEvaluationMetricsUseCase.LABEL_DRAW -> R.string.prediction_outcome_draw
            CalculateEvaluationMetricsUseCase.LABEL_AWAY_WIN -> R.string.prediction_outcome_away
            else -> R.string.prediction_outcome_draw
        }
    }
}
