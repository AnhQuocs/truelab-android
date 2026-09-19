package dev.anhquocs.truelab.core.data.prediction.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.prediction.local.entity.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePrediction(prediction: PredictionEntity): Long

    @Query("SELECT * FROM predictions WHERE matchId = :matchId LIMIT 1")
    fun getPredictionForMatch(matchId: Long): Flow<PredictionEntity?>
}
