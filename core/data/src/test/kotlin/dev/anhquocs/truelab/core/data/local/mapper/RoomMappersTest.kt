package dev.anhquocs.truelab.core.data.local.mapper

import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.league.local.entity.SeasonEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.match.local.entity.MatchWithTeams
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import dev.anhquocs.truelab.core.domain.league.model.League
import dev.anhquocs.truelab.core.domain.league.model.Season
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomMappersTest {

    @Test
    fun leagueEntity_toDomain_mapsCorrectly() {
        val entity = LeagueEntity(
            id = 39,
            name = "Premier League",
            shortName = "EPL",
            logo = "https://example.com/epl.png",
            country = "England",
            category = "Domestic League"
        )

        val domain = entity.toDomain()

        assertEquals(39, domain.id)
        assertEquals("Premier League", domain.name)
        assertEquals("EPL", domain.shortName)
        assertEquals("https://example.com/epl.png", domain.logo)
        assertEquals("England", domain.country)
        assertEquals("Domestic League", domain.category)
    }

    @Test
    fun league_toEntity_mapsCorrectly() {
        val domain = League(
            id = 140,
            name = "La Liga",
            shortName = "LL",
            logo = null,
            country = "Spain",
            category = null
        )

        val entity = domain.toEntity()

        assertEquals(140, entity.id)
        assertEquals("La Liga", entity.name)
        assertEquals("LL", entity.shortName)
        assertNull(entity.logo)
        assertEquals("Spain", entity.country)
        assertNull(entity.category)
    }

    @Test
    fun seasonEntity_toDomain_mapsCorrectly() {
        val entity = SeasonEntity(
            id = "39_2024",
            leagueId = 39,
            name = "2023-2024",
            year = 2024,
            isCurrent = true,
            startDate = "2023-08-11",
            endDate = "2024-05-19"
        )

        val domain = entity.toDomain()

        assertEquals("39_2024", domain.id)
        assertEquals(39, domain.leagueId)
        assertEquals("2023-2024", domain.name)
        assertEquals(2024, domain.year)
        assertEquals(true, domain.isCurrent)
        assertEquals("2023-08-11", domain.startDate)
        assertEquals("2024-05-19", domain.endDate)
    }

    @Test
    fun season_toEntity_mapsCorrectly() {
        val domain = Season(
            id = "140_2023",
            leagueId = 140,
            name = "2022-2023",
            year = 2023,
            isCurrent = false
        )

        val entity = domain.toEntity()

        assertEquals("140_2023", entity.id)
        assertEquals(140, entity.leagueId)
        assertEquals("2022-2023", entity.name)
        assertEquals(2023, entity.year)
        assertEquals(false, entity.isCurrent)
        assertNull(entity.startDate)
        assertNull(entity.endDate)
    }

    @Test
    fun matchWithTeams_toDomain_mapsLeagueIdAndSeason() {
        val matchEntity = MatchEntity(
            id = 1001L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2024-05-10 20:00:00",
            status = "8",
            leagueId = 39,
            season = "2023-2024"
        )
        val homeTeam = TeamEntity(id = 1, name = "Arsenal", logo = "arsenal.png", leagueName = "EPL")
        val awayTeam = TeamEntity(id = 2, name = "Chelsea", logo = "chelsea.png", leagueName = "EPL")
        val matchWithTeams = MatchWithTeams(match = matchEntity, homeTeam = homeTeam, awayTeam = awayTeam)

        val domain = matchWithTeams.toDomain()

        assertEquals(1001L, domain.id)
        assertEquals("Arsenal", domain.homeTeam.name)
        assertEquals("Chelsea", domain.awayTeam.name)
        assertEquals(2, domain.homeScore)
        assertEquals(1, domain.awayScore)
        assertEquals(MatchStatus.ENDED, domain.status)
        assertEquals(39, domain.leagueId)
        assertEquals("2023-2024", domain.season)
    }
}
