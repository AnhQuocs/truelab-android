package dev.anhquocs.truelab.core.data.league.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSeasons(seasons: List<SeasonEntity>): LongArray

    @Query("SELECT * FROM seasons WHERE leagueId = :leagueId ORDER BY year DESC")
    fun getSeasonsByLeague(leagueId: Int): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons WHERE leagueId = :leagueId AND isCurrent = 1 LIMIT 1")
    fun getCurrentSeason(leagueId: Int): Flow<SeasonEntity?>
}
