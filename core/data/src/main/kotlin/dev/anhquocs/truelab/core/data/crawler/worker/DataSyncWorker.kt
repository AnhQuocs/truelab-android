package dev.anhquocs.truelab.core.data.crawler.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.anhquocs.truelab.core.data.crawler.DataSyncEngine
import dev.anhquocs.truelab.core.data.crawler.cache.DatasetCategory
import dev.anhquocs.truelab.core.data.crawler.model.SyncResult
import dev.anhquocs.truelab.core.data.crawler.retry.DefaultRetryClassifier
import dev.anhquocs.truelab.core.data.crawler.retry.RetryClassifier
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager CoroutineWorker responsible for background data synchronization.
 *
 * Delegates execution to [DataSyncEngine], reusing D2.1 RetryEngine and D2.2 CacheFreshnessPolicy.
 */
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSyncEngine: DataSyncEngine,
    private val retryClassifier: RetryClassifier = DefaultRetryClassifier()
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetDate = inputData.getString(KEY_TARGET_DATE) ?: getTodayDateString()
        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)
        val categoryName = inputData.getString(KEY_DATASET_CATEGORY)
        val category = categoryName?.let {
            try {
                DatasetCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                DatasetCategory.SCHEDULED_MATCHES
            }
        } ?: DatasetCategory.SCHEDULED_MATCHES

        return try {
            when (val syncResult = dataSyncEngine.syncFullPipelineForDate(
                date = targetDate,
                syncOddsAndRankings = false,
                forceRefresh = forceRefresh,
                category = category
            )) {
                is SyncResult.Success -> Result.success()
                is SyncResult.Failure -> {
                    if (retryClassifier.isRetryable(syncResult.error)) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (retryClassifier.isRetryable(e)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
    }

    companion object {
        const val KEY_TARGET_DATE = "target_date"
        const val KEY_FORCE_REFRESH = "force_refresh"
        const val KEY_DATASET_CATEGORY = "dataset_category"
        const val DATE_FORMAT = "yyyy-MM-dd"
    }
}
