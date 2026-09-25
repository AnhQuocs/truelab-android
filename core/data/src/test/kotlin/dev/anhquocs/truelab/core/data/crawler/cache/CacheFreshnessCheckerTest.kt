package dev.anhquocs.truelab.core.data.crawler.cache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheFreshnessCheckerTest {

    private val checker = DefaultCacheFreshnessChecker()

    @Test
    fun `data within TTL is evaluated as fresh and not stale`() {
        val lastSync = 1_000_000L
        val currentTime = 1_200_000L // 200s elapsed
        val ttl = 300_000L           // 300s TTL

        assertTrue(checker.isFresh(lastSync, currentTime, ttl))
        assertFalse(checker.isStale(lastSync, currentTime, ttl))
    }

    @Test
    fun `data exceeding TTL is evaluated as stale and not fresh`() {
        val lastSync = 1_000_000L
        val currentTime = 1_400_000L // 400s elapsed
        val ttl = 300_000L           // 300s TTL

        assertFalse(checker.isFresh(lastSync, currentTime, ttl))
        assertTrue(checker.isStale(lastSync, currentTime, ttl))
    }

    @Test
    fun `data exactly at TTL boundary is evaluated as stale`() {
        val lastSync = 1_000_000L
        val currentTime = 1_300_000L // exactly 300s elapsed
        val ttl = 300_000L           // 300s TTL

        assertFalse(checker.isFresh(lastSync, currentTime, ttl))
        assertTrue(checker.isStale(lastSync, currentTime, ttl))
    }

    @Test
    fun `unset or zero lastSyncTimestamp is evaluated as stale`() {
        val ttl = 300_000L
        val currentTime = 1_000_000L

        assertFalse(checker.isFresh(0L, currentTime, ttl))
        assertTrue(checker.isStale(0L, currentTime, ttl))

        assertFalse(checker.isFresh(-100L, currentTime, ttl))
        assertTrue(checker.isStale(-100L, currentTime, ttl))
    }

    @Test
    fun `zero or negative TTL is evaluated as stale`() {
        val lastSync = 1_000_000L
        val currentTime = 1_000_100L

        assertFalse(checker.isFresh(lastSync, currentTime, 0L))
        assertTrue(checker.isStale(lastSync, currentTime, 0L))

        assertFalse(checker.isFresh(lastSync, currentTime, -100L))
        assertTrue(checker.isStale(lastSync, currentTime, -100L))
    }

    @Test
    fun `clock anomaly or future timestamp is evaluated as stale`() {
        val lastSync = 2_000_000L
        val currentTime = 1_000_000L // current time is earlier than sync timestamp
        val ttl = 300_000L

        assertFalse(checker.isFresh(lastSync, currentTime, ttl))
        assertTrue(checker.isStale(lastSync, currentTime, ttl))
    }
}
