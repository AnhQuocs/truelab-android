package dev.anhquocs.truelab.core.data.crawler.cache

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface defining evaluation rules for cache data freshness and expiration.
 */
interface CacheFreshnessChecker {

    /**
     * Evaluates whether data with [lastSyncTimestamp] is fresh given [currentTime] and [ttlMs].
     */
    fun isFresh(lastSyncTimestamp: Long, currentTime: Long, ttlMs: Long): Boolean

    /**
     * Evaluates whether data with [lastSyncTimestamp] is stale given [currentTime] and [ttlMs].
     */
    fun isStale(lastSyncTimestamp: Long, currentTime: Long, ttlMs: Long): Boolean =
        !isFresh(lastSyncTimestamp, currentTime, ttlMs)
}

/**
 * Default implementation of [CacheFreshnessChecker].
 *
 * Rules:
 * - If [lastSyncTimestamp] <= 0 -> Stale (data has never been synchronized).
 * - If [ttlMs] <= 0 -> Stale (no caching or zero TTL).
 * - If [currentTime] < [lastSyncTimestamp] -> Stale (clock anomaly / future timestamp).
 * - If [currentTime] - [lastSyncTimestamp] < [ttlMs] -> Fresh.
 * - If [currentTime] - [lastSyncTimestamp] >= [ttlMs] -> Stale (expired).
 */
@Singleton
class DefaultCacheFreshnessChecker @Inject constructor() : CacheFreshnessChecker {

    override fun isFresh(lastSyncTimestamp: Long, currentTime: Long, ttlMs: Long): Boolean {
        if (lastSyncTimestamp <= 0L) return false
        if (ttlMs <= 0L) return false
        if (currentTime < lastSyncTimestamp) return false

        val ageMs = currentTime - lastSyncTimestamp
        return ageMs < ttlMs
    }
}
