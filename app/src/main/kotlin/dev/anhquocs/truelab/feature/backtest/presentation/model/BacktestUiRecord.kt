package dev.anhquocs.truelab.feature.backtest.presentation.model

import androidx.annotation.StringRes

/**
 * UI representation of the overall backtest summary metrics.
 */
data class BacktestOverviewUiRecord(
    val totalMatches: Int,
    val correctMatches: Int,
    val accuracyPercent: Double,
    val formattedAccuracy: String,
    val macroPrecisionPercent: Double,
    val formattedMacroPrecision: String,
    val macroRecallPercent: Double,
    val formattedMacroRecall: String,
    val macroF1Percent: Double,
    val formattedMacroF1: String
)

/**
 * UI representation of a single cell in the 3x3 confusion matrix.
 */
data class ConfusionMatrixCellUi(
    val count: Int,
    val percentageOfTotal: Double,
    val formattedPercentage: String,
    val isDiagonal: Boolean,
    val intensityLevel: Float // Normalized from 0.0f to 1.0f for heatmap background
)

/**
 * UI representation of the 3x3 confusion matrix.
 *
 * Rows: Actual (Home Win, Draw, Away Win)
 * Columns: Predicted (Home Win, Draw, Away Win)
 */
data class ConfusionMatrixUiRecord(
    val homeAsHome: ConfusionMatrixCellUi,
    val homeAsDraw: ConfusionMatrixCellUi,
    val homeAsAway: ConfusionMatrixCellUi,
    val drawAsHome: ConfusionMatrixCellUi,
    val drawAsDraw: ConfusionMatrixCellUi,
    val drawAsAway: ConfusionMatrixCellUi,
    val awayAsHome: ConfusionMatrixCellUi,
    val awayAsDraw: ConfusionMatrixCellUi,
    val awayAsAway: ConfusionMatrixCellUi,
    val totalSamples: Int
)

/**
 * UI representation of per-class evaluation metrics (Precision, Recall, F1, Support).
 */
data class ClassMetricUiRecord(
    @param:StringRes val classLabelRes: Int,
    val precisionPercent: Double,
    val formattedPrecision: String,
    val recallPercent: Double,
    val formattedRecall: String,
    val f1Percent: Double,
    val formattedF1: String,
    val support: Int
)

/**
 * UI representation of odds coverage metrics.
 */
data class OddsCoverageUiRecord(
    val totalMatches: Int,
    val matchesWithOdds: Int,
    val matchesWithoutOdds: Int,
    val coveragePercent: Double,
    val formattedCoverage: String
)

/**
 * UI representation of an individual backtested match.
 */
data class BacktestMatchUiRecord(
    val matchId: Long,
    val formattedDate: String,
    val homeTeamName: String,
    val awayTeamName: String,
    @param:StringRes val predictedOutcomeRes: Int,
    @param:StringRes val actualOutcomeRes: Int,
    val homeWinPct: Int,
    val drawPct: Int,
    val awayWinPct: Int,
    val confidencePct: Int,
    val isCorrect: Boolean,
    val scoreDisplay: String = "",
    val hasUsableOdds: Boolean = false
)
