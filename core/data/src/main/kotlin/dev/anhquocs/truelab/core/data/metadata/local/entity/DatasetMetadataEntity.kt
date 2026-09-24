package dev.anhquocs.truelab.core.data.metadata.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey
    val key: String = "PRIMARY_DATASET",
    val lastSyncTimestamp: Long,
    val totalMatches: Int,
    val totalTeams: Int,
    val totalOddsRecords: Int,
    val totalLeagues: Int,
    val totalSeasons: Int,
    val earliestMatchDate: String? = null,
    val latestMatchDate: String? = null,
    val schemaVersion: Int = 2
)
