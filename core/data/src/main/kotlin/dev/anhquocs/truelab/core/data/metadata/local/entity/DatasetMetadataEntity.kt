package dev.anhquocs.truelab.core.data.metadata.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata

@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey
    val key: String = DatasetMetadata.DEFAULT_KEY,
    val lastSyncTimestamp: Long,
    val totalMatches: Int,
    val totalTeams: Int,
    val totalOddsRecords: Int,
    val totalLeagues: Int,
    val totalSeasons: Int,
    val earliestMatchDate: String? = null,
    val latestMatchDate: String? = null,
    val schemaVersion: Int = DatasetMetadata.CURRENT_SCHEMA_VERSION
)
