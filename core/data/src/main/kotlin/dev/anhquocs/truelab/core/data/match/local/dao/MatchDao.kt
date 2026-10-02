package dev.anhquocs.truelab.core.data.match.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Upsert
    fun upsertMatches(matches: List<MatchEntity>): LongArray

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
        LEFT JOIN leagues l ON m.leagueId = l.id
        WHERE ht.name LIKE '%' || :query || '%' 
           OR at.name LIKE '%' || :query || '%'
           OR l.name LIKE '%' || :query || '%'
        ORDER BY m.startTimeDate DESC 
        LIMIT :limit
    """)
    fun searchMatches(query: String, limit: Int = 30): Flow<List<MatchWithTeams>>

    @Transaction
    @Query("""
        SELECT m.* FROM matches m
        WHERE (
            (:startDateUtc IS NULL OR :startDateUtc = '' OR REPLACE(m.startTimeDate, ' ', 'T') >= :startDateUtc)
            AND (:endDateUtc IS NULL OR :endDateUtc = '' OR REPLACE(m.startTimeDate, ' ', 'T') < :endDateUtc)
        )
          AND (:leagueId IS NULL OR m.leagueId = :leagueId)
          AND (
              (:statusFilter = 'FINISHED' AND m.status IN ('8', 'ended', 'determined', 'finished', 'ft', 'aet', 'pen'))
              OR (:statusFilter = 'LIVE_AND_UPCOMING' AND ((:isPastDate = 1) OR (:isFutureDate = 1) OR m.status IN ('live', 'pending', '1', '0')))
              OR (:statusFilter = 'LIVE' AND m.status IN ('live', '1'))
              OR (:statusFilter = 'UPCOMING' AND m.status IN ('pending', '0'))
              OR (:statusFilter = 'ALL')
          )
          AND (:searchQuery IS NULL OR :searchQuery = '' OR (
              EXISTS (
                  SELECT 1 FROM teams t 
                  WHERE (t.id = m.homeTeamId OR t.id = m.awayTeamId) 
                    AND t.name LIKE '%' || :searchQuery || '%'
              )
              OR EXISTS (
                  SELECT 1 FROM leagues l
                  WHERE l.id = m.leagueId
                    AND l.name LIKE '%' || :searchQuery || '%'
              )
          ))
        ORDER BY 
          CASE 
              WHEN m.status IN ('live', '1') THEN 0
              WHEN m.status IN ('pending', '0') THEN 1
              ELSE 2
          END,
          m.startTimeDate ASC
        LIMIT :limit
    """)
    fun getPredictableMatchesFiltered(
        startDateUtc: String? = null,
        endDateUtc: String? = null,
        isPastDate: Boolean = false,
        isFutureDate: Boolean = false,
        leagueId: Int? = null,
        statusFilter: String = "ALL",
        searchQuery: String? = null,
        limit: Int = 50
    ): Flow<List<MatchWithTeams>>
}
