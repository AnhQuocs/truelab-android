package dev.anhquocs.truelab.core.data.team.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import dev.anhquocs.truelab.core.data.team.local.entity.TeamWithLeagueItem
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

    @Query("SELECT * FROM teams WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    fun searchTeams(query: String, limit: Int): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams ORDER BY name ASC LIMIT :limit")
    fun getTeams(limit: Int): Flow<List<TeamEntity>>

    @Query("""
        SELECT 
            t.id, 
            t.name, 
            t.logo, 
            COALESCE(t.leagueName, l.name) AS leagueName, 
            l.logo AS leagueLogo,
            t.eloRating, 
            t.formScore
        FROM teams t
        LEFT JOIN matches m ON (t.id = m.homeTeamId OR t.id = m.awayTeamId)
        LEFT JOIN leagues l ON m.leagueId = l.id
        WHERE (:query IS NULL OR :query = '' OR t.name LIKE '%' || :query || '%')
        GROUP BY t.id
        ORDER BY 
            CASE WHEN :sortBy = 'NAME' THEN t.name END ASC,
            CASE WHEN :sortBy = 'FORM' THEN t.formScore END DESC,
            COUNT(m.id) DESC, 
            t.name ASC
        LIMIT :limit OFFSET :offset
    """)
    fun getTeamsPaged(
        query: String? = null,
        sortBy: String = "POPULAR",
        limit: Int = 20,
        offset: Int = 0
    ): Flow<List<TeamWithLeagueItem>>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun getTeamByIdDirect(teamId: Int): TeamEntity?

    @Query("UPDATE teams SET eloRating = :eloRating WHERE id = :teamId")
    fun updateTeamElo(teamId: Int, eloRating: Double)
}
