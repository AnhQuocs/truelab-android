package dev.anhquocs.truelab.core.data.match.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMatches(matches: List<MatchEntity>): LongArray

    @Transaction
    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun getMatchById(matchId: Long): Flow<MatchWithTeams?>

    @Transaction
    @Query("SELECT * FROM matches ORDER BY startTimeDate DESC LIMIT :limit OFFSET :offset")
    fun getMatchesPaged(limit: Int, offset: Int): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("SELECT * FROM matches WHERE startTimeDate LIKE :date || '%' ORDER BY startTimeDate DESC")
    fun getMatchesByDate(date: String): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("SELECT * FROM matches WHERE status = :status ORDER BY startTimeDate DESC")
    fun getMatchesByStatus(status: String): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("""
        SELECT * FROM matches
        WHERE (homeTeamId = :teamAId AND awayTeamId = :teamBId)
           OR (homeTeamId = :teamBId AND awayTeamId = :teamAId)
        ORDER BY startTimeDate DESC
    """)
    fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("""
        SELECT * FROM matches
        WHERE (homeTeamId = :teamId OR awayTeamId = :teamId)
          AND status IN ('8', 'ended', 'determined', 'finished', 'ft', 'aet', 'pen')
        ORDER BY startTimeDate DESC LIMIT :limit
    """)
    fun getRecentMatchesForTeam(teamId: Int, limit: Int = 5): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("SELECT * FROM matches WHERE leagueId = :leagueId ORDER BY startTimeDate DESC")
    fun getMatchesByLeague(leagueId: Int): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("SELECT * FROM matches WHERE leagueId = :leagueId AND season = :season ORDER BY startTimeDate DESC")
    fun getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("SELECT * FROM matches ORDER BY startTimeDate ASC")
    fun getAllMatches(): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("""
        SELECT * FROM matches 
        ORDER BY CASE 
            WHEN status NOT IN ('8', 'ended', 'determined', 'finished', 'ft', 'aet', 'pen') THEN 0 
            ELSE 1 
        END, startTimeDate DESC 
        LIMIT :limit
    """)
    fun getPredictableMatches(limit: Int = 50): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("""
        SELECT m.* FROM matches m
        JOIN teams ht ON m.homeTeamId = ht.id
        JOIN teams at ON m.awayTeamId = at.id
        WHERE ht.name LIKE '%' || :query || '%' OR at.name LIKE '%' || :query || '%'
        ORDER BY m.startTimeDate DESC 
        LIMIT :limit
    """)
    fun searchMatches(query: String, limit: Int = 30): Flow<List<MatchWithTeams>>
}
