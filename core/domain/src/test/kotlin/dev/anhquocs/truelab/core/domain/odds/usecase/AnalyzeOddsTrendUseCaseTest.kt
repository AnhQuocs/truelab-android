package dev.anhquocs.truelab.core.domain.odds.usecase

import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatisticsCalculator
import dev.anhquocs.truelab.core.algorithm.statistics.DescriptiveStatistics
import dev.anhquocs.truelab.core.algorithm.statistics.StatisticsCalculator
import dev.anhquocs.truelab.core.algorithm.trend.MovingAverageCalculator
import dev.anhquocs.truelab.core.algorithm.trend.SimpleMovingAverageCalculator
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import dev.anhquocs.truelab.core.domain.odds.model.TargetOddsField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnalyzeOddsTrendUseCaseTest {

    private lateinit var useCase: AnalyzeOddsTrendUseCase

    @Before
    fun setUp() {
        useCase = AnalyzeOddsTrendUseCase(
            movingAverageCalculator = SimpleMovingAverageCalculator(),
            statisticsCalculator = DescriptiveStatisticsCalculator()
        )
    }

    private fun createOddsItem(
        changeTime: Long,
        homeWin: Double? = null,
        draw: Double? = null,
        awayWin: Double? = null,
        over: Double? = null,
        under: Double? = null,
        handicap: Double? = null,
        companyId: Int = 1,
        companyName: String = "Bet365",
        oddsType: String = "standard",
        marketPhase: String? = null
    ): OddsRecordItem {
        return OddsRecordItem(
            companyId = companyId,
            companyName = companyName,
            oddsType = oddsType,
            handicap = handicap,
            over = over,
            under = under,
            homeWin = homeWin,
            draw = draw,
            awayWin = awayWin,
            changeTime = changeTime,
            marketPhase = marketPhase
        )
    }

    @Test
    fun `empty history returns empty analysis with default fallbacks`() {
        val result = useCase(matchId = 100L, oddsHistory = emptyList(), windowSize = 3)

        assertEquals(100L, result.matchId)
        assertEquals(TargetOddsField.HOME_WIN, result.targetField)
        assertEquals(3, result.windowSize)
        assertTrue(result.timestamps.isEmpty())
        assertTrue(result.rawOddsSeries.isEmpty())
        assertTrue(result.smaSeries.isEmpty())
        assertNull(result.openingOdds)
        assertNull(result.currentOdds)
        assertEquals(0.0, result.volatility, 0.0)
        assertFalse(result.hasSufficientData)
    }

    @Test
    fun `single valid odds record yields identical opening and current odds and zero volatility`() {
        val history = listOf(createOddsItem(changeTime = 1000L, homeWin = 1.95))

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 3)

        assertEquals(listOf(1000L), result.timestamps)
        assertEquals(listOf(1.95), result.rawOddsSeries)
        assertTrue(result.smaSeries.isEmpty()) // 1 < 3
        assertEquals(1.95, result.openingOdds!!, 1e-6)
        assertEquals(1.95, result.currentOdds!!, 1e-6)
        assertEquals(0.0, result.volatility, 1e-6) // N < 2 -> 0.0
        assertFalse(result.hasSufficientData)
    }

    @Test
    fun `sorts records ascending by changeTime regardless of input order`() {
        val history = listOf(
            createOddsItem(changeTime = 3000L, homeWin = 2.10),
            createOddsItem(changeTime = 1000L, homeWin = 1.90),
            createOddsItem(changeTime = 2000L, homeWin = 2.00)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 3)

        assertEquals(listOf(1000L, 2000L, 3000L), result.timestamps)
        assertEquals(listOf(1.90, 2.00, 2.10), result.rawOddsSeries)
        assertEquals(1.90, result.openingOdds!!, 1e-6)
        assertEquals(2.10, result.currentOdds!!, 1e-6)
    }

    @Test
    fun `filters out null target odds records`() {
        val history = listOf(
            createOddsItem(changeTime = 1000L, homeWin = 1.85),
            createOddsItem(changeTime = 2000L, homeWin = null),
            createOddsItem(changeTime = 3000L, homeWin = 1.95)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 2)

        assertEquals(listOf(1000L, 3000L), result.timestamps)
        assertEquals(listOf(1.85, 1.95), result.rawOddsSeries)
        assertEquals(2, result.rawOddsSeries.size)
        assertTrue(result.hasSufficientData)
    }

    @Test
    fun `filters out zero and negative odds values`() {
        val history = listOf(
            createOddsItem(changeTime = 1000L, homeWin = 1.80),
            createOddsItem(changeTime = 2000L, homeWin = 0.0),
            createOddsItem(changeTime = 3000L, homeWin = -1.50),
            createOddsItem(changeTime = 4000L, homeWin = 1.90)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 2)

        assertEquals(listOf(1000L, 4000L), result.timestamps)
        assertEquals(listOf(1.80, 1.90), result.rawOddsSeries)
    }

    @Test
    fun `filters out non-finite odds values such as NaN and Infinities`() {
        val history = listOf(
            createOddsItem(changeTime = 1000L, homeWin = 1.75),
            createOddsItem(changeTime = 2000L, homeWin = Double.NaN),
            createOddsItem(changeTime = 3000L, homeWin = Double.POSITIVE_INFINITY),
            createOddsItem(changeTime = 4000L, homeWin = Double.NEGATIVE_INFINITY),
            createOddsItem(changeTime = 5000L, homeWin = 1.85)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 2)

        assertEquals(listOf(1000L, 5000L), result.timestamps)
        assertEquals(listOf(1.75, 1.85), result.rawOddsSeries)
        assertEquals(1, result.smaSeries.size)
        assertEquals(1.80, result.smaSeries[0], 1e-6)
    }

    @Test
    fun `when data count N is less than windowSize SMA is empty and hasSufficientData is false`() {
        val history = listOf(
            createOddsItem(changeTime = 1000L, homeWin = 1.90),
            createOddsItem(changeTime = 2000L, homeWin = 2.00)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 3)

        assertEquals(2, result.rawOddsSeries.size)
        assertTrue(result.smaSeries.isEmpty())
        assertFalse(result.hasSufficientData)
        // Volatility is still calculated for N=2
        assertTrue(result.volatility > 0.0)
    }

    @Test
    fun `when data count N equals windowSize SMA produces exactly one average point`() {
        val history = listOf(
            createOddsItem(changeTime = 1000L, homeWin = 1.80),
            createOddsItem(changeTime = 2000L, homeWin = 1.90),
            createOddsItem(changeTime = 3000L, homeWin = 2.00)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 3)

        assertEquals(1, result.smaSeries.size)
        assertEquals((1.80 + 1.90 + 2.00) / 3.0, result.smaSeries[0], 1e-6)
        assertTrue(result.hasSufficientData)
    }

    @Test
    fun `when data count N is greater than windowSize SMA rolling averages are computed correctly`() {
        // N = 5, windowSize = 3 -> result size = 5 - 3 + 1 = 3
        val history = listOf(
            createOddsItem(changeTime = 1000L, homeWin = 1.80),
            createOddsItem(changeTime = 2000L, homeWin = 1.90),
            createOddsItem(changeTime = 3000L, homeWin = 2.00),
            createOddsItem(changeTime = 4000L, homeWin = 2.10),
            createOddsItem(changeTime = 5000L, homeWin = 2.20)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 3)

        assertEquals(3, result.smaSeries.size)
        assertEquals((1.80 + 1.90 + 2.00) / 3.0, result.smaSeries[0], 1e-6) // 1.90
        assertEquals((1.90 + 2.00 + 2.10) / 3.0, result.smaSeries[1], 1e-6) // 2.00
        assertEquals((2.00 + 2.10 + 2.20) / 3.0, result.smaSeries[2], 1e-6) // 2.10
        assertTrue(result.hasSufficientData)
    }

    @Test
    fun `delegates SMA calculation to injected MovingAverageCalculator`() {
        var smaDelegated = false
        val customSMA = object : MovingAverageCalculator {
            override fun calculate(dataset: List<Double>, windowSize: Int): List<Double> {
                smaDelegated = true
                assertEquals(3, dataset.size)
                assertEquals(3, windowSize)
                return listOf(99.9)
            }
        }

        val customUseCase = AnalyzeOddsTrendUseCase(
            movingAverageCalculator = customSMA,
            statisticsCalculator = DescriptiveStatisticsCalculator()
        )

        val history = listOf(
            createOddsItem(changeTime = 1L, homeWin = 1.5),
            createOddsItem(changeTime = 2L, homeWin = 1.6),
            createOddsItem(changeTime = 3L, homeWin = 1.7)
        )

        val result = customUseCase(matchId = 1L, oddsHistory = history, windowSize = 3)

        assertTrue(smaDelegated)
        assertEquals(listOf(99.9), result.smaSeries)
    }

    @Test
    fun `delegates volatility calculation to injected StatisticsCalculator`() {
        var statsDelegated = false
        val customStats = object : StatisticsCalculator {
            override fun mean(dataset: List<Double>): Double = 0.0
            override fun median(dataset: List<Double>): Double = 0.0
            override fun min(dataset: List<Double>): Double = 0.0
            override fun max(dataset: List<Double>): Double = 0.0
            override fun range(dataset: List<Double>): Double = 0.0
            override fun populationVariance(dataset: List<Double>): Double = 0.0
            override fun sampleVariance(dataset: List<Double>): Double = 0.0
            override fun populationStandardDeviation(dataset: List<Double>): Double = 0.0
            override fun sampleStandardDeviation(dataset: List<Double>): Double {
                statsDelegated = true
                assertEquals(2, dataset.size)
                return 0.1234
            }
            override fun skewness(dataset: List<Double>): Double = 0.0
            override fun summarize(dataset: List<Double>): DescriptiveStatistics = throw UnsupportedOperationException()
        }

        val customUseCase = AnalyzeOddsTrendUseCase(
            movingAverageCalculator = SimpleMovingAverageCalculator(),
            statisticsCalculator = customStats
        )

        val history = listOf(
            createOddsItem(changeTime = 1L, homeWin = 1.8),
            createOddsItem(changeTime = 2L, homeWin = 1.9)
        )

        val result = customUseCase(matchId = 1L, oddsHistory = history, windowSize = 2)

        assertTrue(statsDelegated)
        assertEquals(0.1234, result.volatility, 1e-6)
    }

    @Test
    fun `opening odds corresponds to earliest valid record and current odds to latest`() {
        val history = listOf(
            createOddsItem(changeTime = 500L, homeWin = 2.50), // opening
            createOddsItem(changeTime = 600L, homeWin = 2.40),
            createOddsItem(changeTime = 700L, homeWin = 2.30)  // current
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 2)

        assertEquals(2.50, result.openingOdds!!, 1e-6)
        assertEquals(2.30, result.currentOdds!!, 1e-6)
    }

    @Test
    fun `timestamps preserve strict 1-to-1 order with valid raw series`() {
        val history = listOf(
            createOddsItem(changeTime = 300L, homeWin = 2.1),
            createOddsItem(changeTime = 100L, homeWin = 1.9),
            createOddsItem(changeTime = 200L, homeWin = null), // filtered
            createOddsItem(changeTime = 400L, homeWin = 2.2)
        )

        val result = useCase(matchId = 100L, oddsHistory = history, windowSize = 2)

        assertEquals(listOf(100L, 300L, 400L), result.timestamps)
        assertEquals(listOf(1.9, 2.1, 2.2), result.rawOddsSeries)
        assertEquals(result.timestamps.size, result.rawOddsSeries.size)
    }

    @Test
    fun `does not mutate input oddsHistory list`() {
        val original = listOf(
            createOddsItem(changeTime = 300L, homeWin = 2.1),
            createOddsItem(changeTime = 100L, homeWin = 1.9)
        )
        val copy = original.toList()

        useCase(matchId = 100L, oddsHistory = original, windowSize = 2)

        assertEquals(copy, original)
        assertEquals(300L, original[0].changeTime)
        assertEquals(100L, original[1].changeTime)
    }

    @Test
    fun `correctly extracts all different target odds fields`() {
        val item1 = createOddsItem(
            changeTime = 100L,
            homeWin = 2.10,
            draw = 3.20,
            awayWin = 3.50,
            over = 1.85,
            under = 1.95,
            handicap = 0.25
        )
        val item2 = createOddsItem(
            changeTime = 200L,
            homeWin = 2.15,
            draw = 3.25,
            awayWin = 3.40,
            over = 1.90,
            under = 1.90,
            handicap = 0.50
        )
        val history = listOf(item1, item2)

        val hw = useCase(1L, history, TargetOddsField.HOME_WIN, 2)
        assertEquals(listOf(2.10, 2.15), hw.rawOddsSeries)

        val d = useCase(1L, history, TargetOddsField.DRAW, 2)
        assertEquals(listOf(3.20, 3.25), d.rawOddsSeries)

        val aw = useCase(1L, history, TargetOddsField.AWAY_WIN, 2)
        assertEquals(listOf(3.50, 3.40), aw.rawOddsSeries)

        val ov = useCase(1L, history, TargetOddsField.OVER, 2)
        assertEquals(listOf(1.85, 1.90), ov.rawOddsSeries)

        val un = useCase(1L, history, TargetOddsField.UNDER, 2)
        assertEquals(listOf(1.95, 1.90), un.rawOddsSeries)

        val hc = useCase(1L, history, TargetOddsField.HANDICAP, 2)
        assertEquals(listOf(0.25, 0.50), hc.rawOddsSeries)
    }

    @Test
    fun `throws IllegalArgumentException when windowSize is non-positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase(matchId = 1L, oddsHistory = emptyList(), windowSize = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            useCase(matchId = 1L, oddsHistory = emptyList(), windowSize = -2)
        }
    }
}
