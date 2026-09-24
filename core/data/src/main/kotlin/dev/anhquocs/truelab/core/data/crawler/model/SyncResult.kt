package dev.anhquocs.truelab.core.data.crawler.model

sealed interface SyncResult<out T> {
    data class Success<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult<T>

    data class Failure(
        val error: Throwable,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult<Nothing>
}

data class SyncSummary(
    val matchesSynced: Int = 0,
    val teamsSynced: Int = 0,
    val leaguesSynced: Int = 0,
    val seasonsSynced: Int = 0,
    val oddsRecordsSynced: Int = 0,
    val rankingsSynced: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
