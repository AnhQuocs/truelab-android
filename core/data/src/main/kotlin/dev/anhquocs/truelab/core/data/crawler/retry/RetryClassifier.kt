package dev.anhquocs.truelab.core.data.crawler.retry

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface defining error classification for retry eligibility.
 */
interface RetryClassifier {
    /**
     * Determines whether the given throwable represents a transient error eligible for retry.
     */
    fun isRetryable(throwable: Throwable): Boolean
}

/**
 * Default implementation of [RetryClassifier] tailored for Retrofit, OkHttp, and Kotlinx Serialization.
 *
 * Retryable errors:
 * - Connection timeouts ([SocketTimeoutException])
 * - Connection refusals ([ConnectException])
 * - DNS resolution failures ([UnknownHostException])
 * - Socket exceptions ([SocketException])
 * - Transient network IO errors ([IOException])
 * - Server-side HTTP 5xx errors (500, 502, 503, 504)
 *
 * Non-retryable errors:
 * - Client-side HTTP 4xx errors (400, 401, 403, 404, etc.)
 * - Serialization / JSON decoding errors ([SerializationException])
 * - Coroutine cancellation ([CancellationException])
 * - Schema, database, and business logic errors
 */
@Singleton
class DefaultRetryClassifier @Inject constructor() : RetryClassifier {

    override fun isRetryable(throwable: Throwable): Boolean {
        // Never retry coroutine cancellation
        if (throwable is CancellationException) {
            return false
        }

        // Never retry serialization/decoding failures
        if (throwable is SerializationException) {
            return false
        }

        // Handle Retrofit HTTP exceptions
        if (throwable is HttpException) {
            val code = throwable.code()
            return code in 500..599
        }

        // Handle transient network exceptions
        return when (throwable) {
            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is SocketException -> true
            is IOException -> true
            else -> false
        }
    }
}
