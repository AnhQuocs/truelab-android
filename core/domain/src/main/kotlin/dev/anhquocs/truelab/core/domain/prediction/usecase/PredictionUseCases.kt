package dev.anhquocs.truelab.core.domain.prediction.usecase

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.domain.prediction.repository.PredictionRepository
import kotlinx.coroutines.flow.Flow

data class PredictionUseCases(
    val predictMatchUseCase: PredictMatchUseCase
)

class PredictMatchUseCase(
    private val repository: PredictionRepository
) {
    operator fun invoke(matchId: Long, algorithm: String = "WeightedScoring"): Flow<PredictionResult> =
        repository.predictMatch(matchId, algorithm)
}
