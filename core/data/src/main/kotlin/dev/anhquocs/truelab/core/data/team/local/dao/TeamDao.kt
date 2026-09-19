package dev.anhquocs.truelab.core.data.team.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    // IMPORTANT: Using IGNORE instead of REPLACE to prevent overwriting 
    // calculated Elo Ratings and Form Scores when syncing matches.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTeams(teams: List<TeamEntity>): LongArray

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun getTeamById(teamId: Int): Flow<TeamEntity?>

    @Query("SELECT * FROM teams WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTeams(query: String): Flow<List<TeamEntity>>
}
