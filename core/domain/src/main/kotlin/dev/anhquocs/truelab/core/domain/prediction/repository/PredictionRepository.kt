package dev.anhquocs.truelab.core.domain.prediction.repository

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import kotlinx.coroutines.flow.Flow

interface PredictionRepository {
    fun predictMatch(matchId: Long, algorithm: String = "WeightedScoring"): Flow<PredictionResult>
}
