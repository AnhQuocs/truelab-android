package dev.anhquocs.truelab.core.data.metadata.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao
import dev.anhquocs.truelab.core.data.metadata.local.entity.DatasetMetadataEntity
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DatasetMetadataRepositoryImpl @Inject constructor(
    private val metadataDao: DatasetMetadataDao
) : DatasetMetadataRepository {

    override fun getMetadata(key: String): Flow<DatasetMetadata?> {
        return metadataDao.getMetadata(key).map { it?.toDomain() }
    }

    override suspend fun refreshSnapshot(timestamp: Long): Result<DatasetMetadata> = withContext(Dispatchers.IO) {
        try {
            val totalMatches = metadataDao.getTotalMatches()
            val totalTeams = metadataDao.getTotalTeams()
            val totalOdds = metadataDao.getTotalOddsRecords()
            val totalLeagues = metadataDao.getTotalLeagues()
            val totalSeasons = metadataDao.getTotalSeasons()
            val earliestDate = metadataDao.getEarliestMatchDate()
            val latestDate = metadataDao.getLatestMatchDate()

            val entity = DatasetMetadataEntity(
                key = DatasetMetadata.DEFAULT_KEY,
                lastSyncTimestamp = timestamp,
                totalMatches = totalMatches,
                totalTeams = totalTeams,
                totalOddsRecords = totalOdds,
                totalLeagues = totalLeagues,
                totalSeasons = totalSeasons,
                earliestMatchDate = earliestDate,
                latestMatchDate = latestDate,
                schemaVersion = DatasetMetadata.CURRENT_SCHEMA_VERSION
            )

            metadataDao.insertOrUpdate(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
