package dev.anhquocs.truelab.core.data.league.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeagueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLeagues(leagues: List<LeagueEntity>): LongArray

    @Query("SELECT * FROM leagues ORDER BY name ASC")
    fun getLeagues(): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues WHERE id = :leagueId")
    fun getLeagueById(leagueId: Int): Flow<LeagueEntity?>
}
