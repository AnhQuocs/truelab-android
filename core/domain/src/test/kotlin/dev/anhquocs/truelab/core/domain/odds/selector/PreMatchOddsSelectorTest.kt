package dev.anhquocs.truelab.core.domain.odds.selector

import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PreMatchOddsSelectorTest {

    private val kickoffIso = "2026-10-01T18:45:00Z"
    private val kickoffEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(kickoffIso)!!

    @Test
    fun `selectPreMatchEuropeanOdds - accepts valid initial snapshot before kickoff`() {
        val odds = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "eu",
                homeWin = 2.10,
                draw = 3.30,
                awayWin = 3.50,
                changeTime = kickoffEpoch - 3600,
                marketPhase = "initial"
            )
        )

        val selected = PreMatchOddsSelector.selectPreMatchEuropeanOdds(odds, kickoffIso)
        assertNotNull(selected)
        assertEquals(2.10, selected!!.homeWin)
        assertEquals("initial", selected.marketPhase)
    }

    @Test
    fun `selectPreMatchEuropeanOdds - prefers immediate snapshot over initial snapshot`() {
        val initialOdds = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "eu",
            homeWin = 2.10,
            draw = 3.30,
            awayWin = 3.50,
            changeTime = kickoffEpoch - 7200,
            marketPhase = "initial"
        )
        val immediateOdds = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "eu",
            homeWin = 1.95,
            draw = 3.40,
            awayWin = 3.90,
            changeTime = kickoffEpoch - 1800,
            marketPhase = "immediate"
        )

        val selected = PreMatchOddsSelector.selectPreMatchEuropeanOdds(
            listOf(initialOdds, immediateOdds),
            kickoffIso
        )
        assertNotNull(selected)
        assertEquals(1.95, selected!!.homeWin)
        assertEquals("immediate", selected.marketPhase)
    }

    @Test
    fun `selectPreMatchEuropeanOdds - rejects rolling_ball and in_play live odds`() {
        val liveOdds = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "eu",
                homeWin = 1.50,
                draw = 4.00,
                awayWin = 6.00,
                changeTime = kickoffEpoch + 600,
                marketPhase = "rolling_ball"
            ),
            OddsRecordItem(
                companyId = 2,
                companyName = "Crown",
                oddsType = "eu",
                homeWin = 1.55,
                draw = 3.90,
                awayWin = 5.80,
                changeTime = kickoffEpoch + 1200,
                marketPhase = "in_play"
            )
        )

        val selected = PreMatchOddsSelector.selectPreMatchEuropeanOdds(liveOdds, kickoffIso)
        assertNull(selected)
    }

    @Test
    fun `selectPreMatchEuropeanOdds - rejects changeTime greater than or equal to kickoff`() {
        val postKickoffOdds = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "eu",
                homeWin = 2.10,
                draw = 3.30,
                awayWin = 3.50,
                changeTime = kickoffEpoch, // Exactly kickoff -> rejected
                marketPhase = "immediate"
            ),
            OddsRecordItem(
                companyId = 2,
                companyName = "Crown",
                oddsType = "eu",
                homeWin = 2.05,
                draw = 3.35,
                awayWin = 3.60,
                changeTime = kickoffEpoch + 100, // After kickoff -> rejected
                marketPhase = "immediate"
            )
        )

        val selected = PreMatchOddsSelector.selectPreMatchEuropeanOdds(postKickoffOdds, kickoffIso)
        assertNull(selected)
    }

    @Test
    fun `selectPreMatchEuropeanOdds - selects latest changeTime when phases are identical`() {
        val olderOdds = OddsRecordItem(
            companyId = 1,
            companyName = "Bet365",
            oddsType = "eu",
            homeWin = 2.10,
            draw = 3.30,
            awayWin = 3.50,
            changeTime = kickoffEpoch - 3600,
            marketPhase = "immediate"
        )
        val newerOdds = OddsRecordItem(
            companyId = 2,
            companyName = "Crown",
            oddsType = "eu",
            homeWin = 2.00,
            draw = 3.40,
            awayWin = 3.80,
            changeTime = kickoffEpoch - 600,
            marketPhase = "immediate"
        )

        val selected = PreMatchOddsSelector.selectPreMatchEuropeanOdds(
            listOf(olderOdds, newerOdds),
            kickoffIso
        )
        assertNotNull(selected)
        assertEquals(2.00, selected!!.homeWin)
        assertEquals("Crown", selected.companyName)
    }

    @Test
    fun `selectPreMatchEuropeanOdds - ignores non-EU odds types`() {
        val ahAndOu = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "asia",
                handicap = -0.5,
                homeWin = 1.90,
                awayWin = 1.95,
                changeTime = kickoffEpoch - 1000,
                marketPhase = "immediate"
            ),
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "bs",
                over = 1.85,
                under = 1.95,
                changeTime = kickoffEpoch - 1000,
                marketPhase = "immediate"
            )
        )

        val selected = PreMatchOddsSelector.selectPreMatchEuropeanOdds(ahAndOu, kickoffIso)
        assertNull(selected)
    }

    @Test
    fun `selectPreMatchAsianHandicapOdds - selects valid pre-match AH snapshot`() {
        val ahList = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "asia",
                handicap = -0.75,
                homeWin = 1.90,
                awayWin = 1.95,
                changeTime = kickoffEpoch - 1800,
                marketPhase = "immediate"
            )
        )

        val selected = PreMatchOddsSelector.selectPreMatchAsianHandicapOdds(ahList, kickoffIso)
        assertNotNull(selected)
        assertEquals(-0.75, selected!!.handicap)
    }

    @Test
    fun `selectPreMatchOverUnderOdds - selects valid pre-match OU snapshot`() {
        val ouList = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Bet365",
                oddsType = "bs",
                over = 1.88,
                under = 1.92,
                changeTime = kickoffEpoch - 1800,
                marketPhase = "immediate"
            )
        )

        val selected = PreMatchOddsSelector.selectPreMatchOverUnderOdds(ouList, kickoffIso)
        assertNotNull(selected)
        assertEquals(1.88, selected!!.over)
    }

    @Test
    fun `selectPreMatchAsianHandicapMainLine - selects line with minimum price difference and positive prices`() {
        val ahList = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Pinnacle",
                oddsType = "asia",
                handicap = 0.25,
                homeWin = 0.70,
                awayWin = 1.20, // diff = 0.50
                changeTime = kickoffEpoch - 3600,
                marketPhase = "immediate"
            ),
            OddsRecordItem(
                companyId = 1,
                companyName = "Pinnacle",
                oddsType = "asia",
                handicap = 0.50,
                homeWin = 0.92,
                awayWin = 0.96, // diff = 0.04 (Main line)
                changeTime = kickoffEpoch - 1800,
                marketPhase = "immediate"
            ),
            OddsRecordItem(
                companyId = 1,
                companyName = "Pinnacle",
                oddsType = "asia",
                handicap = 0.75,
                homeWin = 1.15,
                awayWin = 0.75, // diff = 0.40
                changeTime = kickoffEpoch - 3600,
                marketPhase = "immediate"
            )
        )

        val mainLine = PreMatchOddsSelector.selectPreMatchAsianHandicapMainLine(ahList, kickoffIso)
        assertNotNull(mainLine)
        assertEquals(0.50, mainLine!!.handicap)
        assertEquals(0.92, mainLine.homeWin)
    }

    @Test
    fun `selectPreMatchOverUnderMainLine - selects line with minimum price difference and valid line`() {
        val ouList = listOf(
            OddsRecordItem(
                companyId = 1,
                companyName = "Pinnacle",
                oddsType = "bs",
                handicap = 2.25,
                over = 0.75,
                under = 1.15, // diff = 0.40
                changeTime = kickoffEpoch - 3600,
                marketPhase = "immediate"
            ),
            OddsRecordItem(
                companyId = 1,
                companyName = "Pinnacle",
                oddsType = "bs",
                handicap = 2.50,
                over = 0.95,
                under = 0.93, // diff = 0.02 (Main line)
                changeTime = kickoffEpoch - 1800,
                marketPhase = "immediate"
            )
        )

        val mainLine = PreMatchOddsSelector.selectPreMatchOverUnderMainLine(ouList, kickoffIso)
        assertNotNull(mainLine)
        assertEquals(2.50, mainLine!!.handicap)
        assertEquals(0.95, mainLine.over)
    }
}
