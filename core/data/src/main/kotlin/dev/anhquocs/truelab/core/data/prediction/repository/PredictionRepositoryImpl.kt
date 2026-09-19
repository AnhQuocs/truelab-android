package dev.anhquocs.truelab.core.data.prediction.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult
import dev.anhquocs.truelab.core.domain.prediction.repository.PredictionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class PredictionRepositoryImpl @Inject constructor(
    private val predictionDao: PredictionDao
) : PredictionRepository {

    override fun predictMatch(matchId: Long, algorithm: String): Flow<PredictionResult> {
        // Fetch existing prediction from the database, filtering out nulls.
        return predictionDao.getPredictionForMatch(matchId).mapNotNull { it?.toDomain() }
    }
}
