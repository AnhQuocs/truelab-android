package dev.anhquocs.truelab.core.data.ranking.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RankingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRankings(rankings: List<SeasonRankingEntity>): LongArray

    @Query("SELECT * FROM season_rankings WHERE matchId = :matchId ORDER BY position ASC")
    fun getRankingsForMatch(matchId: Long): Flow<List<SeasonRankingEntity>>

    @Query("SELECT * FROM season_rankings WHERE teamId = :teamId ORDER BY matchId DESC LIMIT 1")
    fun getLatestRankingForTeam(teamId: Int): Flow<SeasonRankingEntity?>

    @Query("SELECT * FROM season_rankings WHERE matchId = (SELECT MAX(matchId) FROM season_rankings) ORDER BY position ASC")
    fun getLatestSeasonRankings(): Flow<List<SeasonRankingEntity>>
}
