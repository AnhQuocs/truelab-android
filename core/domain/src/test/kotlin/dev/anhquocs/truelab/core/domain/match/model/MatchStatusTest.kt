package dev.anhquocs.truelab.core.domain.match.model
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchStatusTest {

    @Test
    fun fromCode_parsesNumericCodesCorrectly() {
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("8"))
        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("1"))
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("0"))
        assertEquals(MatchStatus.CANCELLED, MatchStatus.fromCode("-1"))
    }

    @Test
    fun fromCode_parsesStringStatusesCorrectly() {
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("ended"))
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("ENDED"))
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("determined"))
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("finished"))
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("ft"))
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("aet"))
        assertEquals(MatchStatus.ENDED, MatchStatus.fromCode("pen"))

        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("live"))
        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("in_progress"))
        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("playing"))
        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("1h"))
        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("2h"))
        assertEquals(MatchStatus.IN_PROGRESS, MatchStatus.fromCode("ht"))

        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("scheduled"))
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("fixture"))
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("pending"))
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("not_started"))
        assertEquals(MatchStatus.SCHEDULED, MatchStatus.fromCode("ns"))

        assertEquals(MatchStatus.CANCELLED, MatchStatus.fromCode("cancelled"))
        assertEquals(MatchStatus.CANCELLED, MatchStatus.fromCode("postponed"))
        assertEquals(MatchStatus.CANCELLED, MatchStatus.fromCode("abandoned"))
    }

    @Test
    fun fromCode_unknownFallback() {
        assertEquals(MatchStatus.UNKNOWN, MatchStatus.fromCode("random_unknown_status"))
        assertEquals(MatchStatus.UNKNOWN, MatchStatus.fromCode(""))
    }

    @Test
    fun match_isEnded_isDerivedFromStatus() {
        val homeTeam = TeamSummary(1, "Arsenal")
        val awayTeam = TeamSummary(2, "Chelsea")

        val endedMatch = Match(
            id = 1L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-09-26 15:00:00",
            status = MatchStatus.ENDED
        )
        val scheduledMatch = Match(
            id = 2L,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = null,
            awayScore = null,
            startTimeDate = "2026-09-26 18:00:00",
            status = MatchStatus.SCHEDULED
        )

        assertTrue(endedMatch.isEnded)
        assertTrue(endedMatch.isHomeWin)
        assertFalse(endedMatch.isAwayWin)
        assertFalse(endedMatch.isDraw)
        assertEquals(3, endedMatch.totalGoals)

        assertFalse(scheduledMatch.isEnded)
        assertFalse(scheduledMatch.isHomeWin)
    }
}
