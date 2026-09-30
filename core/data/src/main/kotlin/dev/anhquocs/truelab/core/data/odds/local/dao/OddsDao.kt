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
          AND id IN (
              SELECT id FROM (
                  SELECT id, MAX(changeTime)
                  FROM odds
                  WHERE matchId = :matchId
                  GROUP BY companyId, oddsType
              )
          )
    """)
    fun getLatestOddsForMatch(matchId: Long): Flow<List<OddsEntity>>

    @Query("""
        SELECT o.* FROM odds o
        JOIN matches m ON o.matchId = m.id
        WHERE o.oddsType = 'eu'
          AND o.marketPhase IN ('instant', 'initial')
          AND o.changeTime < CAST(strftime('%s', m.startTimeDate) AS INTEGER)
          AND o.homeWin IS NOT NULL AND o.draw IS NOT NULL AND o.awayWin IS NOT NULL
          AND o.homeWin > 0 AND o.draw > 0 AND o.awayWin > 0
        ORDER BY o.matchId ASC, 
                 (CASE WHEN o.marketPhase = 'instant' THEN 1 ELSE 0 END) DESC, 
                 o.changeTime DESC, 
                 o.id DESC
    """)
    fun getLatestPreMatchEuropeanOddsForAllMatches(): Flow<List<OddsEntity>>
}
