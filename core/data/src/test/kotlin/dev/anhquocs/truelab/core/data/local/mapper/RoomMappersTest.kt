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

    @Test
    fun matchWithTeams_toDomain_mapsLeagueNameAndLogo() {
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
        val leagueEntity = LeagueEntity(id = 39, name = "Premier League", logo = "https://example.com/epl.png")
        val matchWithTeams = MatchWithTeams(match = matchEntity, homeTeam = homeTeam, awayTeam = awayTeam, league = leagueEntity)

        val domain = matchWithTeams.toDomain()

        assertEquals("Premier League", domain.leagueName)
        assertEquals("https://example.com/epl.png", domain.leagueLogo)
    }

    @Test
    fun matchWithTeams_toDomain_mapsStringStatusesCorrectly() {
        val homeTeam = TeamEntity(id = 1, name = "Arsenal", logo = "arsenal.png", leagueName = "EPL")
        val awayTeam = TeamEntity(id = 2, name = "Chelsea", logo = "chelsea.png", leagueName = "EPL")

        val endedMatch = MatchEntity(
            id = 1002L, homeTeamId = 1, awayTeamId = 2, homeScore = 1, awayScore = 0,
            startTimeDate = "2026-09-26 13:00:00", status = "ended"
        )
        val liveMatch = MatchEntity(
            id = 1003L, homeTeamId = 1, awayTeamId = 2, homeScore = 0, awayScore = 0,
            startTimeDate = "2026-09-26 14:00:00", status = "live"
        )
        val scheduledMatch = MatchEntity(
            id = 1004L, homeTeamId = 1, awayTeamId = 2, homeScore = null, awayScore = null,
            startTimeDate = "2026-09-26 18:00:00", status = "scheduled"
        )
        val determinedMatch = MatchEntity(
            id = 1005L, homeTeamId = 1, awayTeamId = 2, homeScore = 3, awayScore = 2,
            startTimeDate = "2026-09-26 10:00:00", status = "determined"
        )

        assertEquals(MatchStatus.ENDED, MatchWithTeams(endedMatch, homeTeam, awayTeam).toDomain().status)
        assertEquals(MatchStatus.IN_PROGRESS, MatchWithTeams(liveMatch, homeTeam, awayTeam).toDomain().status)
        assertEquals(MatchStatus.SCHEDULED, MatchWithTeams(scheduledMatch, homeTeam, awayTeam).toDomain().status)
        assertEquals(MatchStatus.ENDED, MatchWithTeams(determinedMatch, homeTeam, awayTeam).toDomain().status)
    }

    @Test
    fun competitionItemDto_toLeagueEntity_mapsCorrectly() {
        val dto = dev.anhquocs.truelab.core.data.league.remote.dto.CompetitionItemDto(
            id = 1515,
            name = "FIFA World Cup",
            shortName = "FIFA World Cup",
            slug = "fifa-world-cup",
            logo = "https://example.com/logo.png",
            categoryId = 1,
            countryId = 0,
            curSeasonId = 28277L
        )

        val entity = dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.run { dto.toLeagueEntity() }

        assertEquals(1515, entity.id)
        assertEquals("FIFA World Cup", entity.name)
        assertEquals("FIFA World Cup", entity.shortName)
        assertEquals("https://example.com/logo.png", entity.logo)
        assertEquals("0", entity.country)
        assertEquals("1", entity.category)
    }

    @Test
    fun seasonDto_toSeasonEntity_mapsCorrectly() {
        val dto = dev.anhquocs.truelab.core.data.league.remote.dto.SeasonDto(
            id = 28277L,
            competitionId = 1515L,
            year = "2026",
            isCurrent = 1,
            startTime = 1781204400L,
            endTime = 1784487600L
        )

        val entity = dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.run { dto.toSeasonEntity() }

        assertEquals("28277", entity.id)
        assertEquals(1515, entity.leagueId)
        assertEquals("2026", entity.name)
        assertEquals(2026, entity.year)
        assertEquals(true, entity.isCurrent)
        assertEquals("1781204400", entity.startDate)
        assertEquals("1784487600", entity.endDate)
    }

    @Test
    fun matchRecord_toMatchEntity_mapsCompetitionIdToLeagueId() {
        val matchRecord = dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord(
            id = 571388L,
            competitionId = 1515,
            homeTeam = dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo(1, "USA", null),
            awayTeam = dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo(2, "Belgium", null),
            homeScore = 3,
            awayScore = 0,
            startTimeDate = "1930-07-13T13:00:00Z",
            status = "ended"
        )

        val entity = dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.run { matchRecord.toMatchEntity() }

        assertEquals(571388L, entity.id)
        assertEquals(1, entity.homeTeamId)
        assertEquals(2, entity.awayTeamId)
        assertEquals(3, entity.homeScore)
        assertEquals(0, entity.awayScore)
        assertEquals("ended", entity.status)
        assertEquals(1515, entity.leagueId)
    }

    @Test
    fun matchRecord_toLeagueEntity_extractsLeagueInformation() {
        val matchRecord = dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord(
            id = 571388L,
            competitionId = 1515,
            competition = dev.anhquocs.truelab.core.data.match.remote.dto.CompetitionSummaryInfo(
                id = 1515,
                name = "FIFA World Cup",
                shortName = "World Cup",
                logo = "https://example.com/wc.png"
            ),
            homeTeam = dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo(1, "USA", null),
            awayTeam = dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo(2, "Belgium", null),
            homeScore = 3,
            awayScore = 0,
            startTimeDate = "1930-07-13T13:00:00Z",
            status = "ended"
        )

        val leagueEntity = dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.run { matchRecord.toLeagueEntity() }

        org.junit.Assert.assertNotNull(leagueEntity)
        assertEquals(1515, leagueEntity?.id)
        assertEquals("FIFA World Cup", leagueEntity?.name)
        assertEquals("World Cup", leagueEntity?.shortName)
        assertEquals("https://example.com/wc.png", leagueEntity?.logo)
    }

    @Test
    fun matchRecord_toMatchEntity_mapsPenaltyFieldsCorrectly() {
        val matchRecord = dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord(
            id = 571389L,
            competitionId = 1515,
            homeTeam = dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo(1, "Indonesia", null),
            awayTeam = dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo(2, "Thailand", null),
            homeScore = 2,
            awayScore = 2,
            isPenalty = true,
            penaltyResult = dev.anhquocs.truelab.core.data.match.remote.dto.PenaltyResultDto(homeScore = 4, awayScore = 2),
            startTimeDate = "2023-12-10T13:00:00Z",
            status = "ended"
        )

        val entity = dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.run { matchRecord.toMatchEntity() }

        assertEquals(571389L, entity.id)
        assertEquals(2, entity.homeScore)
        assertEquals(2, entity.awayScore)
        assertEquals(true, entity.isPenalty)
        assertEquals(4, entity.homePenaltyScore)
        assertEquals(2, entity.awayPenaltyScore)
    }

    @Test
    fun matchWithTeams_toDomain_mapsPenaltyFieldsCorrectly() {
        val matchEntity = MatchEntity(
            id = 1006L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeScore = 2,
            awayScore = 2,
            isPenalty = true,
            homePenaltyScore = 4,
            awayPenaltyScore = 2,
            startTimeDate = "2023-12-10 20:00:00",
            status = "8"
        )
        val homeTeam = TeamEntity(id = 1, name = "Indonesia", logo = "indonesia.png", leagueName = null)
        val awayTeam = TeamEntity(id = 2, name = "Thailand", logo = "thailand.png", leagueName = null)
        val matchWithTeams = MatchWithTeams(match = matchEntity, homeTeam = homeTeam, awayTeam = awayTeam)

        val domain = matchWithTeams.toDomain()

        assertEquals(1006L, domain.id)
        assertEquals(2, domain.homeScore)
        assertEquals(2, domain.awayScore)
        assertEquals(true, domain.isPenalty)
        assertEquals(4, domain.homePenaltyScore)
        assertEquals(2, domain.awayPenaltyScore)
    }
}
