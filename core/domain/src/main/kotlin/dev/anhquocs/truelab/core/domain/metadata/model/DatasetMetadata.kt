package dev.anhquocs.truelab.core.domain.metadata.model

data class DatasetMetadata(
    val key: String = DEFAULT_KEY,
    val lastSyncTimestamp: Long,
    val totalMatches: Int,
    val totalTeams: Int,
    val totalOddsRecords: Int,
    val totalLeagues: Int,
    val totalSeasons: Int,
    val earliestMatchDate: String? = null,
    val latestMatchDate: String? = null,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    companion object {
        const val DEFAULT_KEY = "PRIMARY_DATASET"
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
