package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.tanh

class RestAdvantageSignalTransformerTest {

    private val defaultConfig = PredictionWeightConfig.DEFAULT

    private fun createMatch(
        id: Long,
        startTimeDate: String,
        isEnded: Boolean = true
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(1, "Home"),
            awayTeam = TeamSummary(2, "Away"),
            homeScore = if (isEnded) 1 else null,
            awayScore = if (isEnded) 0 else null,
            startTimeDate = startTimeDate,
            status = if (isEnded) MatchStatus.ENDED else MatchStatus.SCHEDULED
        )
    }

    @Test
    fun `1 - zero delta yields neutral probabilities`() {
        val signal = RestAdvantageSignalTransformer.transform(deltaRest = 0.0, config = defaultConfig)
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
        assertEquals(0.10, signal.weight, 1e-6)
    }

    @Test
    fun `2 - positive delta favors home team`() {
        val signal = RestAdvantageSignalTransformer.transform(deltaRest = 2.0, config = defaultConfig)
        val expectedShift = 0.09 * tanh(2.0 / 3.0)
        assertEquals(0.37 + expectedShift, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37 - expectedShift, signal.awayProb, 1e-6)
        assertTrue(signal.homeProb > 0.37)
        assertTrue(signal.awayProb < 0.37)
    }

    @Test
    fun `3 - negative delta favors away team`() {
        val signal = RestAdvantageSignalTransformer.transform(deltaRest = -2.0, config = defaultConfig)
        val expectedShift = 0.09 * tanh(-2.0 / 3.0)
        assertEquals(0.37 + expectedShift, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37 - expectedShift, signal.awayProb, 1e-6)
        assertTrue(signal.homeProb < 0.37)
        assertTrue(signal.awayProb > 0.37)
    }

    @Test
    fun `4 - plus minus 1 day evaluation matches tanh contract`() {
        val signalPlus1 = RestAdvantageSignalTransformer.transform(deltaRest = 1.0, config = defaultConfig)
        val expectedShift1 = 0.09 * tanh(1.0 / 3.0)
        assertEquals(0.37 + expectedShift1, signalPlus1.homeProb, 1e-6)
        assertEquals(0.26, signalPlus1.drawProb, 1e-6)
        assertEquals(0.37 - expectedShift1, signalPlus1.awayProb, 1e-6)

        val signalMinus1 = RestAdvantageSignalTransformer.transform(deltaRest = -1.0, config = defaultConfig)
        assertEquals(0.37 - expectedShift1, signalMinus1.homeProb, 1e-6)
        assertEquals(0.26, signalMinus1.drawProb, 1e-6)
        assertEquals(0.37 + expectedShift1, signalMinus1.awayProb, 1e-6)
    }

    @Test
    fun `5 - plus minus 3 days (1 sensitivity unit) evaluation matches tanh contract`() {
        val signalPlus3 = RestAdvantageSignalTransformer.transform(deltaRest = 3.0, config = defaultConfig)
        val expectedShift3 = 0.09 * tanh(1.0)
        assertEquals(0.37 + expectedShift3, signalPlus3.homeProb, 1e-6)
        assertEquals(0.26, signalPlus3.drawProb, 1e-6)
        assertEquals(0.37 - expectedShift3, signalPlus3.awayProb, 1e-6)

        val signalMinus3 = RestAdvantageSignalTransformer.transform(deltaRest = -3.0, config = defaultConfig)
        assertEquals(0.37 - expectedShift3, signalMinus3.homeProb, 1e-6)
        assertEquals(0.26, signalMinus3.drawProb, 1e-6)
        assertEquals(0.37 + expectedShift3, signalMinus3.awayProb, 1e-6)
    }

    @Test
    fun `6 - extreme positive delta saturates cleanly without exceeding 0_46`() {
        val signal100 = RestAdvantageSignalTransformer.transform(deltaRest = 100.0, config = defaultConfig)
        assertEquals(0.46, signal100.homeProb, 1e-4)
        assertEquals(0.26, signal100.drawProb, 1e-6)
        assertEquals(0.28, signal100.awayProb, 1e-4)
        assertTrue(signal100.homeProb <= 0.46 + 1e-6)
    }

    @Test
    fun `7 - extreme negative delta saturates cleanly without dropping below 0_28`() {
        val signalMinus100 = RestAdvantageSignalTransformer.transform(deltaRest = -100.0, config = defaultConfig)
        assertEquals(0.28, signalMinus100.homeProb, 1e-4)
        assertEquals(0.26, signalMinus100.drawProb, 1e-6)
        assertEquals(0.46, signalMinus100.awayProb, 1e-4)
        assertTrue(signalMinus100.homeProb >= 0.28 - 1e-6)
    }

    @Test
    fun `8 - missing home previous match returns neutral baseline`() {
        val awayPrev = createMatch(1, "2026-10-01T15:00:00")
        val signal = RestAdvantageSignalTransformer.transform(
            homePreviousMatch = null,
            awayPreviousMatch = awayPrev,
            targetKickoffTime = "2026-10-05T15:00:00",
            config = defaultConfig
        )
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
    }

    @Test
    fun `9 - missing away previous match returns neutral baseline`() {
        val homePrev = createMatch(1, "2026-10-01T15:00:00")
        val signal = RestAdvantageSignalTransformer.transform(
            homePreviousMatch = homePrev,
            awayPreviousMatch = null,
            targetKickoffTime = "2026-10-05T15:00:00",
            config = defaultConfig
        )
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
    }

    @Test
    fun `10 - missing both previous matches returns neutral baseline`() {
        val signal = RestAdvantageSignalTransformer.transform(
            homePreviousMatch = null,
            awayPreviousMatch = null,
            targetKickoffTime = "2026-10-05T15:00:00",
            config = defaultConfig
        )
        assertEquals(0.37, signal.homeProb, 1e-6)
        assertEquals(0.26, signal.drawProb, 1e-6)
        assertEquals(0.37, signal.awayProb, 1e-6)
    }

    @Test
    fun `11 - symmetry holds for all x where Home(plus x) equals Away(minus x)`() {
        val testValues = listOf(0.1, 0.5, 1.0, 2.5, 3.0, 5.0, 7.0, 10.0, 30.0)
        for (x in testValues) {
            val signalPos = RestAdvantageSignalTransformer.transform(deltaRest = x, config = defaultConfig)
            val signalNeg = RestAdvantageSignalTransformer.transform(deltaRest = -x, config = defaultConfig)

            assertEquals("Home(+x) should equal Away(-x) for x=$x", signalPos.homeProb, signalNeg.awayProb, 1e-6)
            assertEquals("Away(+x) should equal Home(-x) for x=$x", signalPos.awayProb, signalNeg.homeProb, 1e-6)
            assertEquals("Draw prob should remain constant for x=$x", signalPos.drawProb, signalNeg.drawProb, 1e-6)
        }
    }

    @Test
    fun `12 - monotonicity holds strictly across delta range`() {
        val deltas = (-20..20).map { it * 0.5 }
        var prevHomeProb = 0.0
        var prevAwayProb = 1.0

        for (d in deltas) {
            val signal = RestAdvantageSignalTransformer.transform(deltaRest = d, config = defaultConfig)
            if (prevHomeProb > 0.0) {
                assertTrue("HomeProb should be monotonically non-decreasing for d=$d", signal.homeProb >= prevHomeProb)
                assertTrue("AwayProb should be monotonically non-increasing for d=$d", signal.awayProb <= prevAwayProb)
            }
            prevHomeProb = signal.homeProb
            prevAwayProb = signal.awayProb
        }
    }

    @Test
    fun `13 - probability sum is always exactly 1_0`() {
        val deltas = listOf(-100.0, -10.0, -3.0, -1.0, 0.0, 1.0, 3.0, 10.0, 100.0)
        for (d in deltas) {
            val signal = RestAdvantageSignalTransformer.transform(deltaRest = d, config = defaultConfig)
            val sum = signal.homeProb + signal.drawProb + signal.awayProb
            assertEquals("Sum must be 1.0 for deltaRest=$d", 1.0, sum, 1e-6)
        }
    }

    @Test
    fun `14 - probability bounds are strictly respected in range 0_28 to 0_46`() {
        val deltas = listOf(-1000.0, -50.0, -3.0, 0.0, 3.0, 50.0, 1000.0)
        for (d in deltas) {
            val signal = RestAdvantageSignalTransformer.transform(deltaRest = d, config = defaultConfig)
            assertTrue("HomeProb >= 0.28 for d=$d", signal.homeProb >= 0.28 - 1e-6)
            assertTrue("HomeProb <= 0.46 for d=$d", signal.homeProb <= 0.46 + 1e-6)
            assertTrue("AwayProb >= 0.28 for d=$d", signal.awayProb >= 0.28 - 1e-6)
            assertTrue("AwayProb <= 0.46 for d=$d", signal.awayProb <= 0.46 + 1e-6)
            assertEquals("DrawProb == 0.26 for d=$d", 0.26, signal.drawProb, 1e-6)
        }
    }

    @Test
    fun `15 - temporal safety prevents future match leakage and respects kickoff order`() {
        val targetKickoff = "2026-10-05T15:00:00"

        // Previous match occurring AFTER target kickoff (future leakage)
        val futureHomeMatch = createMatch(1, "2026-10-06T15:00:00")
        val validAwayMatch = createMatch(2, "2026-10-01T15:00:00")

        val signalFuture = RestAdvantageSignalTransformer.transform(
            homePreviousMatch = futureHomeMatch,
            awayPreviousMatch = validAwayMatch,
            targetKickoffTime = targetKickoff,
            config = defaultConfig
        )

        // Must fallback to neutral baseline
        assertEquals(0.37, signalFuture.homeProb, 1e-6)
        assertEquals(0.26, signalFuture.drawProb, 1e-6)
        assertEquals(0.37, signalFuture.awayProb, 1e-6)

        // Valid past matches compute correct delta: Home 4 days rest (Oct 1 -> Oct 5), Away 2 days rest (Oct 3 -> Oct 5) -> delta = +2 days
        val validHomeMatch = createMatch(3, "2026-10-01T15:00:00")
        val validAwayMatch2 = createMatch(4, "2026-10-03T15:00:00")

        val signalValid = RestAdvantageSignalTransformer.transform(
            homePreviousMatch = validHomeMatch,
            awayPreviousMatch = validAwayMatch2,
            targetKickoffTime = targetKickoff,
            config = defaultConfig
        )

        val expectedShift = 0.09 * tanh(2.0 / 3.0)
        assertEquals(0.37 + expectedShift, signalValid.homeProb, 1e-6)
        assertEquals(0.26, signalValid.drawProb, 1e-6)
        assertEquals(0.37 - expectedShift, signalValid.awayProb, 1e-6)
    }
}
