package dev.anhquocs.truelab.core.data.odds.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OddsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOdds(odds: List<OddsEntity>): LongArray

    @Query("""
        SELECT * FROM odds 
        WHERE matchId = :matchId 
          AND (:companyId IS NULL OR companyId = :companyId)
          AND (:oddsType IS NULL OR oddsType = :oddsType)
        ORDER BY changeTime DESC
    """)
    fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsEntity>>

    @Query("""
        SELECT * FROM odds 
        WHERE matchId = :matchId 
        GROUP BY companyId, oddsType 
        HAVING changeTime = MAX(changeTime)
    """)
    fun getLatestOddsForMatch(matchId: Long): Flow<List<OddsEntity>>
}
