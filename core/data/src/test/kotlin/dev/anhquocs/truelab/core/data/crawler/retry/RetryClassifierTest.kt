package dev.anhquocs.truelab.core.data.crawler.retry

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RetryClassifierTest {

    private val classifier = DefaultRetryClassifier()

    private fun createHttpException(statusCode: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(statusCode, body)
        return HttpException(response)
    }

    @Test
    fun `socket timeout exception is retryable`() {
        assertTrue(classifier.isRetryable(SocketTimeoutException("Read timed out")))
    }

    @Test
    fun `connect exception is retryable`() {
        assertTrue(classifier.isRetryable(ConnectException("Failed to connect to host")))
    }

    @Test
    fun `unknown host exception is retryable`() {
        assertTrue(classifier.isRetryable(UnknownHostException("Unable to resolve host")))
    }

    @Test
    fun `socket exception is retryable`() {
        assertTrue(classifier.isRetryable(SocketException("Connection reset")))
    }

    @Test
    fun `general io exception is retryable`() {
        assertTrue(classifier.isRetryable(IOException("Broken pipe")))
    }

    @Test
    fun `http 5xx server errors are retryable`() {
        assertTrue(classifier.isRetryable(createHttpException(500)))
        assertTrue(classifier.isRetryable(createHttpException(502)))
        assertTrue(classifier.isRetryable(createHttpException(503)))
        assertTrue(classifier.isRetryable(createHttpException(504)))
    }

    @Test
    fun `http 4xx client errors are non-retryable`() {
        assertFalse(classifier.isRetryable(createHttpException(400)))
        assertFalse(classifier.isRetryable(createHttpException(401)))
        assertFalse(classifier.isRetryable(createHttpException(403)))
        assertFalse(classifier.isRetryable(createHttpException(404)))
        assertFalse(classifier.isRetryable(createHttpException(422)))
    }

    @Test
    fun `serialization exceptions are non-retryable`() {
        assertFalse(classifier.isRetryable(SerializationException("JSON decode error at path $")))
    }

    @Test
    fun `cancellation exception is non-retryable`() {
        assertFalse(classifier.isRetryable(CancellationException("Coroutine was cancelled")))
    }

    @Test
    fun `generic runtime and logic exceptions are non-retryable`() {
        assertFalse(classifier.isRetryable(IllegalArgumentException("Invalid argument")))
        assertFalse(classifier.isRetryable(IllegalStateException("Bad state")))
        assertFalse(classifier.isRetryable(NullPointerException("Missing reference")))
        assertFalse(classifier.isRetryable(Exception("Generic error")))
    }
}
