package dev.anhquocs.truelab.core.data.team.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TeamRepositoryImpl @Inject constructor(
    private val teamDao: TeamDao,
    private val rankingDao: RankingDao,
    private val json: Json
) : TeamRepository {

    override fun getTeamDetail(teamId: Int): Flow<TeamDetail?> {
        return teamDao.getTeamById(teamId).map { it?.toDomain() }
    }

    override fun getTeams(): Flow<List<TeamDetail>> {
        return teamDao.searchTeams("").map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSeasonRanking(matchId: Long): Flow<List<SeasonRanking>> {
        // Gap solved: RankingDao now queries specifically by matchId!
        return rankingDao.getRankingsForMatch(matchId).map { list ->
            list.map { it.toDomain(json) }
        }
    }

    override fun getSeasonRankings(): Flow<List<SeasonRanking>> {
        return rankingDao.getLatestSeasonRankings().map { list ->
            list.map { it.toDomain(json) }
        }
    }
}
