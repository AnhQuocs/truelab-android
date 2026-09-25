package dev.anhquocs.truelab.core.data.metadata.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.metadata.local.entity.DatasetMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DatasetMetadataDao {

    @Query("SELECT * FROM dataset_metadata WHERE `key` = :key LIMIT 1")
    fun getMetadata(key: String): Flow<DatasetMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(metadata: DatasetMetadataEntity): Long

    @Query("SELECT COUNT(*) FROM matches")
    fun getTotalMatches(): Int

    @Query("SELECT COUNT(*) FROM teams")
    fun getTotalTeams(): Int

    @Query("SELECT COUNT(*) FROM odds")
    fun getTotalOddsRecords(): Int

    @Query("SELECT COUNT(*) FROM leagues")
    fun getTotalLeagues(): Int

    @Query("SELECT COUNT(*) FROM seasons")
    fun getTotalSeasons(): Int

    @Query("SELECT MIN(startTimeDate) FROM matches WHERE startTimeDate IS NOT NULL AND startTimeDate != ''")
    fun getEarliestMatchDate(): String?

    @Query("SELECT MAX(startTimeDate) FROM matches WHERE startTimeDate IS NOT NULL AND startTimeDate != ''")
    fun getLatestMatchDate(): String?
}
