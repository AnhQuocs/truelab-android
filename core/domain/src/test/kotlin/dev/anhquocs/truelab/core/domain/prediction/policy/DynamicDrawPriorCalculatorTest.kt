package dev.anhquocs.truelab.core.domain.prediction.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit test cho [DynamicDrawPriorCalculator] (Phase B3 - Candidate B: Dynamic Draw Prior).
 *
 * Kiểm tra các tính chất toán học bất biến:
 * 1. Delta = 0 -> P_D = P_D,max
 * 2. Delta = +x -> P_D < P_D,max
 * 3. Delta = -x -> P_D(+x) == P_D(-x) (Tính đối xứng hoàn hảo)
 * 4. |Delta| tăng -> P_D giảm đơn điệu
 * 5. Boundedness -> Luôn nằm trong [P_D,min, P_D,max]
 * 6. Numerical stability -> An toàn với NaN, Infinity, sigma <= 0
 */
class DynamicDrawPriorCalculatorTest {

    private val defaultMaxDraw = 0.36
    private val defaultMinDraw = 0.12
    private val defaultEloSigma = 1.0
    private val defaultFormSigma = 0.25

    @Test
    fun `Test 1 - delta equal 0 yields exactly maxDrawProb`() {
        val priorElo = DynamicDrawPriorCalculator.computePrior(
            delta = 0.0,
            sigma = defaultEloSigma,
            maxDrawProb = defaultMaxDraw,
            minDrawProb = defaultMinDraw
        )
        val priorForm = DynamicDrawPriorCalculator.computePrior(
            delta = 0.0,
            sigma = defaultFormSigma,
            maxDrawProb = defaultMaxDraw,
            minDrawProb = defaultMinDraw
        )

        assertEquals(0.36, priorElo, 1e-6)
        assertEquals(0.36, priorForm, 1e-6)
    }

    @Test
    fun `Test 2 - positive delta yields draw probability less than maxDrawProb`() {
        val prior = DynamicDrawPriorCalculator.computePrior(
            delta = 0.5,
            sigma = defaultEloSigma,
            maxDrawProb = defaultMaxDraw,
            minDrawProb = defaultMinDraw
        )

        assertTrue("P_D ($prior) must be strictly less than maxDrawProb (0.36)", prior < defaultMaxDraw)
        assertTrue("P_D ($prior) must be strictly greater than minDrawProb (0.12)", prior > defaultMinDraw)
    }

    @Test
    fun `Test 3 - symmetry confirms P_D(+delta) equals P_D(-delta)`() {
        val deltaValues = listOf(0.1, 0.25, 0.5, 1.0, 1.5, 2.0, 3.0)

        for (d in deltaValues) {
            val positivePrior = DynamicDrawPriorCalculator.computePrior(
                delta = d,
                sigma = defaultEloSigma,
                maxDrawProb = defaultMaxDraw,
                minDrawProb = defaultMinDraw
            )
            val negativePrior = DynamicDrawPriorCalculator.computePrior(
                delta = -d,
                sigma = defaultEloSigma,
                maxDrawProb = defaultMaxDraw,
                minDrawProb = defaultMinDraw
            )
            assertEquals("Symmetry failed for delta = $d", positivePrior, negativePrior, 1e-9)
        }
    }

    @Test
    fun `Test 4 - larger absolute delta monotonically decreases draw probability`() {
        val deltaSteps = listOf(0.0, 0.2, 0.5, 1.0, 1.5, 2.0, 3.0, 5.0)
        var previousPrior = 1.0

        for (d in deltaSteps) {
            val currentPrior = DynamicDrawPriorCalculator.computePrior(
                delta = d,
                sigma = defaultEloSigma,
                maxDrawProb = defaultMaxDraw,
                minDrawProb = defaultMinDraw
            )
            assertTrue("Expected prior for delta $d ($currentPrior) <= previous ($previousPrior)", currentPrior <= previousPrior)
            previousPrior = currentPrior
        }
    }

    @Test
    fun `Test 5 - prior is strictly bounded in minDrawProb and maxDrawProb`() {
        val testDeltas = listOf(-100.0, -10.0, -1.0, 0.0, 1.0, 10.0, 100.0)

        for (d in testDeltas) {
            val prior = DynamicDrawPriorCalculator.computePrior(
                delta = d,
                sigma = defaultEloSigma,
                maxDrawProb = defaultMaxDraw,
                minDrawProb = defaultMinDraw
            )
            assertTrue("P_D ($prior) must be >= minDrawProb (0.12)", prior >= defaultMinDraw)
            assertTrue("P_D ($prior) must be <= maxDrawProb (0.36)", prior <= defaultMaxDraw)
        }
    }

    @Test
    fun `Test 6 - numerical stability handles NaN Infinity and non-positive sigma safely`() {
        val nanPrior = DynamicDrawPriorCalculator.computePrior(Double.NaN, defaultEloSigma, defaultMaxDraw, defaultMinDraw)
        val infPrior = DynamicDrawPriorCalculator.computePrior(Double.POSITIVE_INFINITY, defaultEloSigma, defaultMaxDraw, defaultMinDraw)
        val zeroSigmaPrior = DynamicDrawPriorCalculator.computePrior(0.5, 0.0, defaultMaxDraw, defaultMinDraw)
        val negSigmaPrior = DynamicDrawPriorCalculator.computePrior(0.5, -1.0, defaultMaxDraw, defaultMinDraw)

        assertTrue(nanPrior.isFinite() && nanPrior in defaultMinDraw..defaultMaxDraw)
        assertTrue(infPrior.isFinite() && infPrior in defaultMinDraw..defaultMaxDraw)
        assertTrue(zeroSigmaPrior.isFinite() && zeroSigmaPrior in defaultMinDraw..defaultMaxDraw)
        assertTrue(negSigmaPrior.isFinite() && negSigmaPrior in defaultMinDraw..defaultMaxDraw)
    }
}
