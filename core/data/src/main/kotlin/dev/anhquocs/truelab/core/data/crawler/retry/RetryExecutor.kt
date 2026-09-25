package dev.anhquocs.truelab.core.data.crawler.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Coroutine-friendly retry executor providing exponential backoff with jitter.
 *
 * @param classifier Strategy used to determine whether a given throwable is retryable.
 * @param defaultPolicy Default retry configuration to use when none is explicitly provided.
 * @param delayProvider Suspending delay mechanism (defaults to [delay], customizable for testing).
 * @param randomProvider Random generator for jitter calculations (defaults to [Random.nextDouble]).
 */
class RetryExecutor(
    private val classifier: RetryClassifier = DefaultRetryClassifier(),
    private val defaultPolicy: RetryPolicy = RetryPolicy(),
    private val delayProvider: suspend (Long) -> Unit = { delay(it) },
    private val randomProvider: () -> Double = { Random.nextDouble() }
) {

    /**
     * Executes the suspending [block] with retry logic based on the specified [policy].
     *
     * @param policy The retry policy controlling attempt count, delays, and backoff factor.
     * @param block The suspending work to execute. Receives the 1-based attempt index.
     * @return The result of the successful execution of [block].
     * @throws Throwable The root cause exception if retries are exhausted or a non-retryable error occurs.
     */
    suspend fun <T> executeWithRetry(
        policy: RetryPolicy = defaultPolicy,
        block: suspend (attempt: Int) -> T
    ): T {
        var currentAttempt = 1

        while (true) {
            try {
                return block(currentAttempt)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }

                val isRetryable = classifier.isRetryable(e)
                val hasAttemptsLeft = currentAttempt < policy.maxAttempts

                if (!isRetryable || !hasAttemptsLeft) {
                    throw e
                }

                val backoffDelay = calculateDelay(currentAttempt, policy)
                if (backoffDelay > 0) {
                    delayProvider(backoffDelay)
                }

                currentAttempt++
            }
        }
    }

    /**
     * Convenience function for executing a suspending [block] that does not need the attempt index.
     */
    suspend fun <T> execute(
        policy: RetryPolicy = defaultPolicy,
        block: suspend () -> T
    ): T = executeWithRetry(policy) { block() }

    /**
     * Computes the backoff delay for the given attempt index based on the [policy].
     *
     * Formula:
     * baseDelay = min(maxDelayMs, (initialDelayMs * factor^(attempt - 1)))
     * jitteredDelay = baseDelay * (0.5 + 0.5 * random) if jitter is enabled
     */
    internal fun calculateDelay(attempt: Int, policy: RetryPolicy): Long {
        if (policy.initialDelayMs <= 0L) return 0L

        val multiplier = policy.factor.pow((attempt - 1).toDouble())
        val calculated = (policy.initialDelayMs.toDouble() * multiplier).toLong()
        val baseDelay = min(policy.maxDelayMs, calculated)

        return if (policy.jitter && baseDelay > 0) {
            val randomFactor = 0.5 + 0.5 * randomProvider()
            (baseDelay.toDouble() * randomFactor).toLong().coerceIn(0L, policy.maxDelayMs)
        } else {
            baseDelay
        }
    }
}
