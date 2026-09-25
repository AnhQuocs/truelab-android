package dev.anhquocs.truelab.core.data.crawler.retry

/**
 * Configuration for network retry policies with exponential backoff and optional jitter.
 *
 * @property maxAttempts Total number of execution attempts, including the initial attempt.
 *                       For example, maxAttempts = 3 means 1 initial attempt + up to 2 retries.
 * @property initialDelayMs Initial delay before the first retry in milliseconds.
 * @property maxDelayMs Maximum delay cap for any retry backoff in milliseconds.
 * @property factor Exponential backoff multiplier factor.
 * @property jitter Whether to apply jitter (randomized variation) to the backoff delay.
 */
data class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    val factor: Double = DEFAULT_FACTOR,
    val jitter: Boolean = DEFAULT_JITTER
) {
    init {
        require(maxAttempts >= 1) {
            "maxAttempts must be >= 1, but was $maxAttempts"
        }
        require(initialDelayMs >= 0) {
            "initialDelayMs must be >= 0, but was $initialDelayMs"
        }
        require(maxDelayMs >= initialDelayMs) {
            "maxDelayMs ($maxDelayMs) must be >= initialDelayMs ($initialDelayMs)"
        }
        require(factor >= 1.0) {
            "factor must be >= 1.0, but was $factor"
        }
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        const val DEFAULT_INITIAL_DELAY_MS = 1000L
        const val DEFAULT_MAX_DELAY_MS = 10000L
        const val DEFAULT_FACTOR = 2.0
        const val DEFAULT_JITTER = true
    }
}
