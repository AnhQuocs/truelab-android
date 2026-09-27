package dev.anhquocs.truelab.feature.match.presentation.mapper

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchUiMapperTest {

    private val homeTeam = TeamSummary(id = 1, name = "Arsenal")
    private val awayTeam = TeamSummary(id = 2, name = "Chelsea")

    @Test
    fun `toUiRecord maps LIVE match correctly`() {
        val match = Match(
            id = 101L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = 2,
            awayScore = 0,
            startTimeDate = "2026-09-26T06:00:00Z",
            status = MatchStatus.IN_PROGRESS
        )

        val record = match.toUiRecord()

        assertEquals("LIVE", record.actualResult)
        assertEquals(2, record.homeScore)
        assertEquals(0, record.awayScore)
        assertEquals("Arsenal", record.homeTeam)
        assertEquals("Chelsea", record.awayTeam)
    }

    @Test
    fun `toUiRecord maps ENDED home win correctly`() {
        val match = Match(
            id = 102L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = 3,
            awayScore = 1,
            startTimeDate = "2026-09-25T15:00:00Z",
            status = MatchStatus.ENDED
        )

        val record = match.toUiRecord()

        assertEquals("HOME_WIN", record.actualResult)
    }

    @Test
    fun `toUiRecord maps ENDED away win correctly`() {
        val match = Match(
            id = 103L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = 0,
            awayScore = 2,
            startTimeDate = "2026-09-25T15:00:00Z",
            status = MatchStatus.ENDED
        )

        val record = match.toUiRecord()

        assertEquals("AWAY_WIN", record.actualResult)
    }

    @Test
    fun `toUiRecord maps ENDED draw correctly`() {
        val match = Match(
            id = 104L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = 1,
            awayScore = 1,
            startTimeDate = "2026-09-25T15:00:00Z",
            status = MatchStatus.ENDED
        )

        val record = match.toUiRecord()

        assertEquals("DRAW", record.actualResult)
    }

    @Test
    fun `toUiRecord maps SCHEDULED match correctly`() {
        val match = Match(
            id = 105L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = null,
            awayScore = null,
            startTimeDate = "2026-09-27T15:00:00Z",
            status = MatchStatus.SCHEDULED
        )

        val record = match.toUiRecord()

        assertEquals("SCHEDULED", record.actualResult)
    }

    @Test
    fun `toUiRecord maps CANCELLED match correctly`() {
        val match = Match(
            id = 106L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = null,
            awayScore = null,
            startTimeDate = "2026-09-27T15:00:00Z",
            status = MatchStatus.CANCELLED
        )

        val record = match.toUiRecord()

        assertEquals("CANCELLED", record.actualResult)
    }

    @Test
    fun `toUiRecord sets null placeholders for uncalculated odds and eloDiff`() {
        val match = Match(
            id = 107L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = 1,
            awayScore = 0,
            startTimeDate = "2026-09-25T15:00:00Z",
            status = MatchStatus.ENDED
        )

        val record = match.toUiRecord()

        org.junit.Assert.assertNull(record.avgHomeOdds)
        org.junit.Assert.assertNull(record.avgDrawOdds)
        org.junit.Assert.assertNull(record.avgAwayOdds)
        org.junit.Assert.assertNull(record.eloDiff)
        assertEquals("—", record.predictedProb)
    }
}
