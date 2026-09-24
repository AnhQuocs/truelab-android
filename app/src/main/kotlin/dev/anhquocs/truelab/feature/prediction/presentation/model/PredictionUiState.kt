package dev.anhquocs.truelab.feature.prediction.presentation.model

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.ui.utils.UiText

sealed interface PredictionUiState {
    data object Loading : PredictionUiState

    data class Empty(val message: UiText) : PredictionUiState

    data class Error(val message: UiText) : PredictionUiState

    data class Success(
        val selectedMatch: Match,
        val availableMatches: List<Match>,
        val predictionResult: PredictionResult,
        val homeElo: Double?,
        val awayElo: Double?,
        val homeWinPercent: Int,
        val drawPercent: Int,
        val awayWinPercent: Int,
        val confidencePercent: Int
    ) : PredictionUiState
}
