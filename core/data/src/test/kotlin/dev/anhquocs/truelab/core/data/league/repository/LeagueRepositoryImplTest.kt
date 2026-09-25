package dev.anhquocs.truelab.core.data.league.repository

import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.dao.SeasonDao
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeagueRepositoryImplTest {

    private class FakeLeagueDao : LeagueDao {
        val leagues = mutableListOf<LeagueEntity>()

        override fun insertLeagues(leagues: List<LeagueEntity>): LongArray {
            this.leagues.addAll(leagues)
            return LongArray(leagues.size) { (it + 1).toLong() }
        }

        override fun getLeagues(): Flow<List<LeagueEntity>> = flowOf(leagues)

        override fun getLeagueById(leagueId: Int): Flow<LeagueEntity?> =
            flowOf(leagues.find { it.id == leagueId })
    }

    private class FakeSeasonDao : SeasonDao {
        val seasons = mutableListOf<SeasonEntity>()

        override fun insertSeasons(seasons: List<SeasonEntity>): LongArray {
            this.seasons.addAll(seasons)
            return LongArray(seasons.size) { (it + 1).toLong() }
        }

        override fun getSeasonsByLeague(leagueId: Int): Flow<List<SeasonEntity>> =
            flowOf(seasons.filter { it.leagueId == leagueId })

        override fun getCurrentSeason(leagueId: Int): Flow<SeasonEntity?> =
            flowOf(seasons.find { it.leagueId == leagueId && it.isCurrent })
    }

    @Test
    fun getLeagues_returnsMappedDomainLeagues() = runTest {
        val leagueDao = FakeLeagueDao()
        val seasonDao = FakeSeasonDao()
        val repo = LeagueRepositoryImpl(leagueDao, seasonDao)

        leagueDao.insertLeagues(
            listOf(
                LeagueEntity(id = 39, name = "Premier League", country = "England"),
                LeagueEntity(id = 140, name = "La Liga", country = "Spain")
            )
        )

        val result = repo.getLeagues().first()

        assertEquals(2, result.size)
        assertEquals("Premier League", result[0].name)
        assertEquals("La Liga", result[1].name)
    }

    @Test
    fun getLeagueDetail_returnsSingleMappedDomainLeague() = runTest {
        val leagueDao = FakeLeagueDao()
        val seasonDao = FakeSeasonDao()
        val repo = LeagueRepositoryImpl(leagueDao, seasonDao)

        leagueDao.insertLeagues(
            listOf(LeagueEntity(id = 39, name = "Premier League", country = "England"))
        )

        val found = repo.getLeagueDetail(39).first()
        val notFound = repo.getLeagueDetail(999).first()

        assertNotNull(found)
        assertEquals("Premier League", found?.name)
        assertNull(notFound)
    }

    @Test
    fun getSeasons_returnsMappedDomainSeasonsForLeague() = runTest {
        val leagueDao = FakeLeagueDao()
        val seasonDao = FakeSeasonDao()
        val repo = LeagueRepositoryImpl(leagueDao, seasonDao)

        seasonDao.insertSeasons(
            listOf(
                SeasonEntity(id = "39_2024", leagueId = 39, name = "2023-2024", year = 2024, isCurrent = true),
                SeasonEntity(id = "39_2023", leagueId = 39, name = "2022-2023", year = 2023, isCurrent = false),
                SeasonEntity(id = "140_2024", leagueId = 140, name = "2023-2024", year = 2024, isCurrent = true)
            )
        )

        val eplSeasons = repo.getSeasons(39).first()

        assertEquals(2, eplSeasons.size)
        assertEquals("2023-2024", eplSeasons[0].name)
        assertEquals("2022-2023", eplSeasons[1].name)
    }

    @Test
    fun getSeasons_emptyLeague_returnsEmptyList() = runTest {
        val leagueDao = FakeLeagueDao()
        val seasonDao = FakeSeasonDao()
        val repo = LeagueRepositoryImpl(leagueDao, seasonDao)

        val seasons = repo.getSeasons(999).first()
        assertTrue(seasons.isEmpty())
    }
}

