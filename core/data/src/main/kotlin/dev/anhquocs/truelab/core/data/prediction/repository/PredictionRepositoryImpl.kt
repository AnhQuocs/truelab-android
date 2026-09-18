package dev.anhquocs.truelab.core.data.prediction.repository

import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.domain.prediction.repository.PredictionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PredictionRepositoryImpl @Inject constructor() : PredictionRepository {

    override fun predictMatch(matchId: Long, algorithm: String): Flow<PredictionResult> = flow {
        val result = PredictionResult.computeWeightedScoring(
            matchId = matchId,
            homeFormScore = 75.0,
            awayFormScore = 60.0,
            homeElo = 1580.0,
            awayElo = 1520.0
        )
        emit(result)
    }
}
