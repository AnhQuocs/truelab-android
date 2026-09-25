package dev.anhquocs.truelab.core.data.crawler.retry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun `default values match requirements and semantics`() {
        val policy = RetryPolicy()
        assertEquals(3, policy.maxAttempts)
        assertEquals(1000L, policy.initialDelayMs)
        assertEquals(10000L, policy.maxDelayMs)
        assertEquals(2.0, policy.factor, 0.001)
        assertTrue(policy.jitter)
    }

    @Test
    fun `custom valid configuration is accepted`() {
        val policy = RetryPolicy(
            maxAttempts = 5,
            initialDelayMs = 500L,
            maxDelayMs = 5000L,
            factor = 1.5,
            jitter = false
        )
        assertEquals(5, policy.maxAttempts)
        assertEquals(500L, policy.initialDelayMs)
        assertEquals(5000L, policy.maxDelayMs)
        assertEquals(1.5, policy.factor, 0.001)
        assertFalse(policy.jitter)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid maxAttempts less than 1 throws IllegalArgumentException`() {
        RetryPolicy(maxAttempts = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative initialDelayMs throws IllegalArgumentException`() {
        RetryPolicy(initialDelayMs = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxDelayMs less than initialDelayMs throws IllegalArgumentException`() {
        RetryPolicy(initialDelayMs = 2000L, maxDelayMs = 1000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `factor less than 1 throws IllegalArgumentException`() {
        RetryPolicy(factor = 0.99)
    }
}
