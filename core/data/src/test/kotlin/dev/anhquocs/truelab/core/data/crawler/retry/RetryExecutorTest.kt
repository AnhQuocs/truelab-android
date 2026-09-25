package dev.anhquocs.truelab.core.data.crawler.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class RetryExecutorTest {

    private val classifier = DefaultRetryClassifier()

    @Test
    fun `successful execution on first attempt executes exactly once without delays`() = runTest {
        val recordedDelays = mutableListOf<Long>()
        var attemptCount = 0

        val executor = RetryExecutor(
            classifier = classifier,
            delayProvider = { recordedDelays.add(it) }
        )

        val result = executor.executeWithRetry { attempt ->
            attemptCount++
            assertEquals(1, attempt)
            "SUCCESS"
        }

        assertEquals("SUCCESS", result)
        assertEquals(1, attemptCount)
        assertTrue(recordedDelays.isEmpty())
    }

    @Test
    fun `transient failure on attempt 1 succeeds on attempt 2 with one delay`() = runTest {
        val recordedDelays = mutableListOf<Long>()
        var attemptCount = 0

        val executor = RetryExecutor(
            classifier = classifier,
            defaultPolicy = RetryPolicy(jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val result = executor.executeWithRetry { attempt ->
            attemptCount++
            if (attempt == 1) {
                throw SocketTimeoutException("Connection timed out")
            }
            "SUCCESS_ON_ATTEMPT_2"
        }

        assertEquals("SUCCESS_ON_ATTEMPT_2", result)
        assertEquals(2, attemptCount)
        assertEquals(listOf(1000L), recordedDelays)
    }

    @Test
    fun `transient failure on attempt 1 and 2 succeeds on attempt 3 with exponential delays`() = runTest {
        val recordedDelays = mutableListOf<Long>()
        var attemptCount = 0

        val executor = RetryExecutor(
            classifier = classifier,
            defaultPolicy = RetryPolicy(jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        val result = executor.executeWithRetry { attempt ->
            attemptCount++
            if (attempt < 3) {
                throw SocketTimeoutException("Timeout attempt $attempt")
            }
            "SUCCESS_ON_ATTEMPT_3"
        }

        assertEquals("SUCCESS_ON_ATTEMPT_3", result)
        assertEquals(3, attemptCount)
        assertEquals(listOf(1000L, 2000L), recordedDelays)
    }

    @Test
    fun `exhausted retry after maxAttempts throws root cause exception and stops`() = runTest {
        val recordedDelays = mutableListOf<Long>()
        var attemptCount = 0

        val executor = RetryExecutor(
            classifier = classifier,
            defaultPolicy = RetryPolicy(maxAttempts = 3, jitter = false),
            delayProvider = { recordedDelays.add(it) }
        )

        try {
            executor.executeWithRetry { attempt ->
                attemptCount++
                throw IOException("Network down at attempt $attempt")
            }
            fail("Should have thrown IOException")
        } catch (e: IOException) {
            assertEquals("Network down at attempt 3", e.message)
        }

        assertEquals(3, attemptCount)
        assertEquals(listOf(1000L, 2000L), recordedDelays)
    }

    @Test
    fun `non-retryable error fails immediately on attempt 1 with zero delay`() = runTest {
        val recordedDelays = mutableListOf<Long>()
        var attemptCount = 0

        val executor = RetryExecutor(
            classifier = classifier,
            delayProvider = { recordedDelays.add(it) }
        )

        try {
            executor.executeWithRetry { attempt ->
                attemptCount++
                throw IllegalArgumentException("Invalid ID parameter")
            }
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Invalid ID parameter", e.message)
        }

        assertEquals(1, attemptCount)
        assertTrue(recordedDelays.isEmpty())
    }

    @Test
    fun `delay is capped at maxDelayMs`() {
        val policy = RetryPolicy(
            maxAttempts = 5,
            initialDelayMs = 2000L,
            maxDelayMs = 5000L,
            factor = 2.0,
            jitter = false
        )

        val executor = RetryExecutor(classifier = classifier)

        // Attempt 1: 2000ms
        assertEquals(2000L, executor.calculateDelay(1, policy))
        // Attempt 2: 4000ms
        assertEquals(4000L, executor.calculateDelay(2, policy))
        // Attempt 3: 8000ms -> capped at 5000ms
        assertEquals(5000L, executor.calculateDelay(3, policy))
        // Attempt 4: 16000ms -> capped at 5000ms
        assertEquals(5000L, executor.calculateDelay(4, policy))
    }

    @Test
    fun `jitter varies delay within 50 to 100 percent of calculated base delay`() {
        val policy = RetryPolicy(
            initialDelayMs = 1000L,
            factor = 2.0,
            jitter = true
        )

        // Test with random = 0.0 -> factor 0.5
        val minExecutor = RetryExecutor(
            classifier = classifier,
            randomProvider = { 0.0 }
        )
        assertEquals(500L, minExecutor.calculateDelay(1, policy))
        assertEquals(1000L, minExecutor.calculateDelay(2, policy))

        // Test with random = 1.0 -> factor 1.0
        val maxExecutor = RetryExecutor(
            classifier = classifier,
            randomProvider = { 1.0 }
        )
        assertEquals(1000L, maxExecutor.calculateDelay(1, policy))
        assertEquals(2000L, maxExecutor.calculateDelay(2, policy))
    }

    @Test
    fun `cancellation exception is immediately rethrown without retry`() = runTest {
        var attemptCount = 0
        val executor = RetryExecutor(classifier = classifier)

        try {
            executor.executeWithRetry {
                attemptCount++
                throw CancellationException("Job was cancelled")
            }
            fail("Should have rethrown CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Job was cancelled", e.message)
        }

        assertEquals(1, attemptCount)
    }
}
