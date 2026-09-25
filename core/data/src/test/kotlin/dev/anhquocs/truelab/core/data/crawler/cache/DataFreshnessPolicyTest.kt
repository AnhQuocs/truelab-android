package dev.anhquocs.truelab.core.data.crawler.cache

import org.junit.Assert.assertEquals
import org.junit.Test

class DataFreshnessPolicyTest {

    @Test
    fun `default TTL values match specification`() {
        val policy = DataFreshnessPolicy()

        assertEquals(5 * 60 * 1000L, policy.liveMatchesTtlMs)
        assertEquals(30 * 60 * 1000L, policy.scheduledMatchesTtlMs)
        assertEquals(24 * 60 * 60 * 1000L, policy.historicalMatchesTtlMs)
        assertEquals(15 * 60 * 1000L, policy.defaultTtlMs)
    }

    @Test
    fun `getTtlMs returns correct TTL for each dataset category`() {
        val policy = DataFreshnessPolicy()

        assertEquals(5 * 60 * 1000L, policy.getTtlMs(DatasetCategory.LIVE_MATCHES))
        assertEquals(30 * 60 * 1000L, policy.getTtlMs(DatasetCategory.SCHEDULED_MATCHES))
        assertEquals(24 * 60 * 60 * 1000L, policy.getTtlMs(DatasetCategory.HISTORICAL_MATCHES))
        assertEquals(15 * 60 * 1000L, policy.getTtlMs(DatasetCategory.DEFAULT))
    }

    @Test
    fun `custom valid TTL configuration is accepted`() {
        val policy = DataFreshnessPolicy(
            liveMatchesTtlMs = 60_000L,
            scheduledMatchesTtlMs = 120_000L,
            historicalMatchesTtlMs = 300_000L,
            defaultTtlMs = 180_000L
        )

        assertEquals(60_000L, policy.getTtlMs(DatasetCategory.LIVE_MATCHES))
        assertEquals(120_000L, policy.getTtlMs(DatasetCategory.SCHEDULED_MATCHES))
        assertEquals(300_000L, policy.getTtlMs(DatasetCategory.HISTORICAL_MATCHES))
        assertEquals(180_000L, policy.getTtlMs(DatasetCategory.DEFAULT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative liveMatchesTtlMs throws IllegalArgumentException`() {
        DataFreshnessPolicy(liveMatchesTtlMs = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative scheduledMatchesTtlMs throws IllegalArgumentException`() {
        DataFreshnessPolicy(scheduledMatchesTtlMs = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative historicalMatchesTtlMs throws IllegalArgumentException`() {
        DataFreshnessPolicy(historicalMatchesTtlMs = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative defaultTtlMs throws IllegalArgumentException`() {
        DataFreshnessPolicy(defaultTtlMs = -1L)
    }
}
