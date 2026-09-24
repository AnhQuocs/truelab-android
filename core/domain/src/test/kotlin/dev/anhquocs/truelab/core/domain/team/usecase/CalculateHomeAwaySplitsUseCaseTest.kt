package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateHomeAwaySplitsUseCaseTest {

    private lateinit var useCase: CalculateHomeAwaySplitsUseCase

    private val targetTeam = TeamSummary(id = 1, name = "Arsenal")
    private val opponentA = TeamSummary(id = 2, name = "Chelsea")
    private val opponentB = TeamSummary(id = 3, name = "Liverpool")
    private val opponentC = TeamSummary(id = 4, name = "Tottenham")
    private val neutralTeam1 = TeamSummary(id = 10, name = "Man City")
    private val neutralTeam2 = TeamSummary(id = 11, name = "Aston Villa")

    @Before
    fun setUp() {
        useCase = CalculateHomeAwaySplitsUseCase()
    }

    private fun createMatch(
        id: Long,
        homeTeam: TeamSummary,
        awayTeam: TeamSummary,
        homeScore: Int?,
        awayScore: Int?,
        status: MatchStatus = MatchStatus.ENDED
    ): Match {
        return Match(
            id = id,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = "2026-01-01T15:00:00",
            status = status
        )
    }

    @Test
    fun `1 Empty matches list returns all empty zero splits`() {
        val splits = useCase(teamId = targetTeam.id, matches = emptyList())

        assertEquals(targetTeam.id, splits.teamId)

        // Home split
        assertEquals(0, splits.homeSplit.played)
        assertEquals(0, splits.homeSplit.won)
        assertEquals(0, splits.homeSplit.draw)
        assertEquals(0, splits.homeSplit.loss)
        assertEquals(0, splits.homeSplit.goalsFor)
        assertEquals(0, splits.homeSplit.goalsAgainst)
        assertEquals(0, splits.homeSplit.goalDiff)
        assertEquals(0, splits.homeSplit.points)
        assertEquals(0.0, splits.homeSplit.winRate, 1e-6)

        // Away split
        assertEquals(0, splits.awaySplit.played)
        assertEquals(0, splits.awaySplit.won)
        assertEquals(0, splits.awaySplit.draw)
        assertEquals(0, splits.awaySplit.loss)
        assertEquals(0, splits.awaySplit.goalsFor)
        assertEquals(0, splits.awaySplit.goalsAgainst)
        assertEquals(0, splits.awaySplit.goalDiff)
        assertEquals(0, splits.awaySplit.points)
        assertEquals(0.0, splits.awaySplit.winRate, 1e-6)

        // Total split
        assertEquals(0, splits.totalSplit.played)
        assertEquals(0, splits.totalSplit.won)
        assertEquals(0, splits.totalSplit.draw)
        assertEquals(0, splits.totalSplit.loss)
        assertEquals(0, splits.totalSplit.goalsFor)
        assertEquals(0, splits.totalSplit.goalsAgainst)
        assertEquals(0, splits.totalSplit.goalDiff)
        assertEquals(0, splits.totalSplit.points)
        assertEquals(0.0, splits.totalSplit.winRate, 1e-6)
    }

    @Test
    fun `2 Home-only matches produce valid home split while away split remains empty`() {
        val m1 = createMatch(1L, targetTeam, opponentA, 2, 0) // Win
        val m2 = createMatch(2L, targetTeam, opponentB, 1, 1) // Draw
        val m3 = createMatch(3L, targetTeam, opponentC, 1, 3) // Loss

        val splits = useCase(teamId = targetTeam.id, matches = listOf(m1, m2, m3))

        // Home split: 3 played, 1 W, 1 D, 1 L, GF=4, GA=4, GD=0, Pts=4, winRate=1/3 (~0.3333)
        assertEquals(3, splits.homeSplit.played)
        assertEquals(1, splits.homeSplit.won)
        assertEquals(1, splits.homeSplit.draw)
        assertEquals(1, splits.homeSplit.loss)
        assertEquals(4, splits.homeSplit.goalsFor)
        assertEquals(4, splits.homeSplit.goalsAgainst)
        assertEquals(0, splits.homeSplit.goalDiff)
        assertEquals(4, splits.homeSplit.points)
        assertEquals(1.0 / 3.0, splits.homeSplit.winRate, 1e-6)

        // Away split should be empty
        assertEquals(0, splits.awaySplit.played)
        assertEquals(0.0, splits.awaySplit.winRate, 1e-6)

        // Total split should match home split exactly
        assertEquals(splits.homeSplit, splits.totalSplit)
    }

    @Test
    fun `3 Away-only matches produce valid away split while home split remains empty`() {
        val m1 = createMatch(1L, opponentA, targetTeam, 0, 3) // Win
        val m2 = createMatch(2L, opponentB, targetTeam, 2, 2) // Draw

        val splits = useCase(teamId = targetTeam.id, matches = listOf(m1, m2))

        // Home split should be empty
        assertEquals(0, splits.homeSplit.played)
        assertEquals(0.0, splits.homeSplit.winRate, 1e-6)

        // Away split: 2 played, 1 W, 1 D, 0 L, GF=5, GA=2, GD=3, Pts=4, winRate=0.5
        assertEquals(2, splits.awaySplit.played)
        assertEquals(1, splits.awaySplit.won)
        assertEquals(1, splits.awaySplit.draw)
        assertEquals(0, splits.awaySplit.loss)
        assertEquals(5, splits.awaySplit.goalsFor)
        assertEquals(2, splits.awaySplit.goalsAgainst)
        assertEquals(3, splits.awaySplit.goalDiff)
        assertEquals(4, splits.awaySplit.points)
        assertEquals(0.5, splits.awaySplit.winRate, 1e-6)

        // Total matches away
        assertEquals(splits.awaySplit, splits.totalSplit)
    }

    @Test
    fun `4 Mixed home and away matches calculate separate and combined splits accurately`() {
        // Home matches: 2 played, 2 won (3-0, 2-1) -> GF=5, GA=1, GD=4, Pts=6, winRate=1.0
        val h1 = createMatch(1L, targetTeam, opponentA, 3, 0)
        val h2 = createMatch(2L, targetTeam, opponentB, 2, 1)

        // Away matches: 3 played, 1 won, 1 draw, 1 loss (0-2 win, 1-1 draw, 3-0 loss)
        // -> GF=3, GA=4, GD=-1, Pts=4, winRate=1/3
        val a1 = createMatch(3L, opponentA, targetTeam, 0, 2) // Win
        val a2 = createMatch(4L, opponentB, targetTeam, 1, 1) // Draw
        val a3 = createMatch(5L, opponentC, targetTeam, 3, 0) // Loss

        val splits = useCase(teamId = targetTeam.id, matches = listOf(h1, h2, a1, a2, a3))

        // Home
        assertEquals(2, splits.homeSplit.played)
        assertEquals(2, splits.homeSplit.won)
        assertEquals(0, splits.homeSplit.draw)
        assertEquals(0, splits.homeSplit.loss)
        assertEquals(5, splits.homeSplit.goalsFor)
        assertEquals(1, splits.homeSplit.goalsAgainst)
        assertEquals(4, splits.homeSplit.goalDiff)
        assertEquals(6, splits.homeSplit.points)
        assertEquals(1.0, splits.homeSplit.winRate, 1e-6)

        // Away
        assertEquals(3, splits.awaySplit.played)
        assertEquals(1, splits.awaySplit.won)
        assertEquals(1, splits.awaySplit.draw)
        assertEquals(1, splits.awaySplit.loss)
        assertEquals(3, splits.awaySplit.goalsFor)
        assertEquals(4, splits.awaySplit.goalsAgainst)
        assertEquals(-1, splits.awaySplit.goalDiff)
        assertEquals(4, splits.awaySplit.points)
        assertEquals(1.0 / 3.0, splits.awaySplit.winRate, 1e-6)

        // Total
        assertEquals(5, splits.totalSplit.played)
        assertEquals(3, splits.totalSplit.won)
        assertEquals(1, splits.totalSplit.draw)
        assertEquals(1, splits.totalSplit.loss)
        assertEquals(8, splits.totalSplit.goalsFor)
        assertEquals(5, splits.totalSplit.goalsAgainst)
        assertEquals(3, splits.totalSplit.goalDiff)
        assertEquals(10, splits.totalSplit.points)
        assertEquals(3.0 / 5.0, splits.totalSplit.winRate, 1e-6)
    }

    @Test
    fun `5 Points calculation obeys won times 3 plus draw rule`() {
        val m1 = createMatch(1L, targetTeam, opponentA, 1, 0) // Win -> 3 pts
        val m2 = createMatch(2L, targetTeam, opponentB, 2, 2) // Draw -> 1 pt
        val m3 = createMatch(3L, targetTeam, opponentC, 0, 1) // Loss -> 0 pt

        val splits = useCase(teamId = targetTeam.id, matches = listOf(m1, m2, m3))

        assertEquals(4, splits.homeSplit.points)
        assertEquals(4, splits.totalSplit.points)
    }

    @Test
    fun `6 Negative goal difference handles proper sign in goalDiff`() {
        val m1 = createMatch(1L, opponentA, targetTeam, 4, 1) // Loss: GF=1, GA=4, GD=-3
        val m2 = createMatch(2L, opponentB, targetTeam, 5, 0) // Loss: GF=0, GA=5, GD=-5

        val splits = useCase(teamId = targetTeam.id, matches = listOf(m1, m2))

        assertEquals(2, splits.awaySplit.played)
        assertEquals(0, splits.awaySplit.won)
        assertEquals(2, splits.awaySplit.loss)
        assertEquals(1, splits.awaySplit.goalsFor)
        assertEquals(9, splits.awaySplit.goalsAgainst)
        assertEquals(-8, splits.awaySplit.goalDiff)
        assertEquals(0, splits.awaySplit.points)
        assertEquals(0.0, splits.awaySplit.winRate, 1e-6)
    }

    @Test
    fun `7 Unfinished and scheduled matches are completely ignored`() {
        val validMatch = createMatch(1L, targetTeam, opponentA, 2, 1, MatchStatus.ENDED)
        val scheduledMatch = createMatch(2L, targetTeam, opponentB, null, null, MatchStatus.SCHEDULED)
        val inProgressMatch = createMatch(3L, targetTeam, opponentC, 1, 0, MatchStatus.IN_PROGRESS)
        val cancelledMatch = createMatch(4L, opponentA, targetTeam, null, null, MatchStatus.CANCELLED)

        val splits = useCase(
            teamId = targetTeam.id,
            matches = listOf(validMatch, scheduledMatch, inProgressMatch, cancelledMatch)
        )

        assertEquals(1, splits.totalSplit.played)
        assertEquals(1, splits.homeSplit.played)
        assertEquals(0, splits.awaySplit.played)
    }

    @Test
    fun `8 Matches with null or missing scores are completely ignored`() {
        val valid = createMatch(1L, targetTeam, opponentA, 2, 0, MatchStatus.ENDED)
        val nullHome = createMatch(2L, targetTeam, opponentB, null, 1, MatchStatus.ENDED)
        val nullAway = createMatch(3L, opponentC, targetTeam, 2, null, MatchStatus.ENDED)

        val splits = useCase(
            teamId = targetTeam.id,
            matches = listOf(valid, nullHome, nullAway)
        )

        assertEquals(1, splits.totalSplit.played)
        assertEquals(1, splits.totalSplit.won)
    }

    @Test
    fun `9 Matches not involving the target team are completely ignored`() {
        val validForTarget = createMatch(1L, targetTeam, opponentA, 1, 0)
        val neutralMatch = createMatch(2L, neutralTeam1, neutralTeam2, 3, 2)

        val splits = useCase(
            teamId = targetTeam.id,
            matches = listOf(validForTarget, neutralMatch)
        )

        assertEquals(1, splits.totalSplit.played)
        assertEquals(1, splits.totalSplit.won)
        assertEquals(1, splits.totalSplit.goalsFor)
        assertEquals(0, splits.totalSplit.goalsAgainst)
    }

    @Test
    fun `10 Total split satisfies mathematical partition consistency across home and away`() {
        val h1 = createMatch(1L, targetTeam, opponentA, 4, 1)
        val h2 = createMatch(2L, targetTeam, opponentB, 0, 0)
        val a1 = createMatch(3L, opponentA, targetTeam, 1, 2)
        val a2 = createMatch(4L, opponentB, targetTeam, 3, 0)

        val splits = useCase(teamId = targetTeam.id, matches = listOf(h1, h2, a1, a2))

        val home = splits.homeSplit
        val away = splits.awaySplit
        val total = splits.totalSplit

        assertEquals(home.played + away.played, total.played)
        assertEquals(home.won + away.won, total.won)
        assertEquals(home.draw + away.draw, total.draw)
        assertEquals(home.loss + away.loss, total.loss)
        assertEquals(home.goalsFor + away.goalsFor, total.goalsFor)
        assertEquals(home.goalsAgainst + away.goalsAgainst, total.goalsAgainst)
        assertEquals(home.goalDiff + away.goalDiff, total.goalDiff)
        assertEquals(home.points + away.points, total.points)
    }

    @Test
    fun `11 Scrambled match input order produces deterministic identical splits`() {
        val m1 = createMatch(1L, targetTeam, opponentA, 2, 0)
        val m2 = createMatch(2L, opponentB, targetTeam, 1, 1)
        val m3 = createMatch(3L, targetTeam, opponentC, 0, 3)

        val splitsOrdered = useCase(targetTeam.id, listOf(m1, m2, m3))
        val splitsScrambled = useCase(targetTeam.id, listOf(m3, m1, m2))

        assertEquals(splitsOrdered, splitsScrambled)
    }

    @Test
    fun `12 All wins all draws and all losses subsets calculate boundary winRates and points`() {
        // All wins: 3 wins -> winRate 1.0, pts 9
        val w1 = createMatch(1L, targetTeam, opponentA, 1, 0)
        val w2 = createMatch(2L, opponentB, targetTeam, 0, 2)
        val resWins = useCase(targetTeam.id, listOf(w1, w2))
        assertEquals(1.0, resWins.totalSplit.winRate, 1e-6)
        assertEquals(6, resWins.totalSplit.points)

        // All draws: 2 draws -> winRate 0.0, pts 2
        val d1 = createMatch(3L, targetTeam, opponentA, 0, 0)
        val d2 = createMatch(4L, opponentB, targetTeam, 2, 2)
        val resDraws = useCase(targetTeam.id, listOf(d1, d2))
        assertEquals(0.0, resDraws.totalSplit.winRate, 1e-6)
        assertEquals(2, resDraws.totalSplit.points)

        // All losses: 2 losses -> winRate 0.0, pts 0
        val l1 = createMatch(5L, targetTeam, opponentA, 0, 1)
        val l2 = createMatch(6L, opponentB, targetTeam, 3, 1)
        val resLosses = useCase(targetTeam.id, listOf(l1, l2))
        assertEquals(0.0, resLosses.totalSplit.winRate, 1e-6)
        assertEquals(0, resLosses.totalSplit.points)
    }
}

