package dev.anhquocs.truelab.core.data.league.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeagueDao {

    @Upsert
    fun insertLeagues(leagues: List<LeagueEntity>): LongArray

    @Query("SELECT * FROM leagues ORDER BY name ASC")
    fun getLeagues(): Flow<List<LeagueEntity>>

    @Query("SELECT * FROM leagues WHERE id = :leagueId")
    fun getLeagueById(leagueId: Int): Flow<LeagueEntity?>
}
