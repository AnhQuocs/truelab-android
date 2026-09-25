package dev.anhquocs.truelab.core.domain.metadata.repository

import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import kotlinx.coroutines.flow.Flow

interface DatasetMetadataRepository {
    fun getMetadata(key: String = DatasetMetadata.DEFAULT_KEY): Flow<DatasetMetadata?>
    suspend fun refreshSnapshot(timestamp: Long = System.currentTimeMillis()): Result<DatasetMetadata>
}
