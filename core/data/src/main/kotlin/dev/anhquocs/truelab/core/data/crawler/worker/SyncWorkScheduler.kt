package dev.anhquocs.truelab.core.data.crawler.worker

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.anhquocs.truelab.core.data.crawler.cache.DatasetCategory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface responsible for scheduling background data synchronization via WorkManager.
 */
interface SyncWorkScheduler {

    /**
     * Enqueues unique one-time initial background sync with network constraint.
     *
     * @param forceRefresh Whether to force remote refresh bypassing cache TTL (default: false).
     * @param existingWorkPolicy Policy to handle existing work with the same unique name (default: KEEP).
     */
    fun scheduleInitialSync(
        forceRefresh: Boolean = false,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP
    )

    /**
     * Cancels any active initial synchronization work.
     */
    fun cancelInitialSync()

    /**
     * Enqueues unique periodic background sync with network and battery constraints.
     *
     * @param intervalMinutes Periodic interval in minutes (minimum 15 minutes as enforced by WorkManager).
     * @param existingWorkPolicy Policy to handle existing work with the same unique name (default: KEEP).
     */
    fun schedulePeriodicSync(
        intervalMinutes: Long = DEFAULT_PERIODIC_INTERVAL_MINUTES,
        existingWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
    )

    /**
     * Cancels any scheduled background synchronization work.
     */
    fun cancelPeriodicSync()

    companion object {
        const val INITIAL_SYNC_WORK_NAME = "TrueLabInitialDataSyncWork"
        const val PERIODIC_SYNC_WORK_NAME = "TrueLabPeriodicDataSyncWork"
        const val DEFAULT_PERIODIC_INTERVAL_MINUTES = 60L
        const val MINIMUM_PERIODIC_INTERVAL_MINUTES = 15L
    }
}

/**
 * Default implementation of [SyncWorkScheduler] using Android WorkManager.
 */
@Singleton
class DefaultSyncWorkScheduler @Inject constructor(
    private val workManager: WorkManager
) : SyncWorkScheduler {

    override fun scheduleInitialSync(
        forceRefresh: Boolean,
        existingWorkPolicy: ExistingWorkPolicy
    ) {
        val initialWorkRequest = createInitialWorkRequest(forceRefresh)

        workManager.enqueueUniqueWork(
            SyncWorkScheduler.INITIAL_SYNC_WORK_NAME,
            existingWorkPolicy,
            initialWorkRequest
        )
    }

    override fun cancelInitialSync() {
        workManager.cancelUniqueWork(SyncWorkScheduler.INITIAL_SYNC_WORK_NAME)
    }

    override fun schedulePeriodicSync(
        intervalMinutes: Long,
        existingWorkPolicy: ExistingPeriodicWorkPolicy
    ) {
        val periodicWorkRequest = createPeriodicWorkRequest(intervalMinutes)

        workManager.enqueueUniquePeriodicWork(
            SyncWorkScheduler.PERIODIC_SYNC_WORK_NAME,
            existingWorkPolicy,
            periodicWorkRequest
        )
    }

    override fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(SyncWorkScheduler.PERIODIC_SYNC_WORK_NAME)
    }

    companion object {
        /**
         * Builds a configured [OneTimeWorkRequest] for initial sync with network constraint.
         */
        fun createInitialWorkRequest(forceRefresh: Boolean = false): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putBoolean(DataSyncWorker.KEY_FORCE_REFRESH, forceRefresh)
                .putString(DataSyncWorker.KEY_DATASET_CATEGORY, DatasetCategory.SCHEDULED_MATCHES.name)
                .build()

            return OneTimeWorkRequestBuilder<DataSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
        }

        /**
         * Builds a configured [PeriodicWorkRequest] with appropriate constraints and intervals.
         */
        fun createPeriodicWorkRequest(intervalMinutes: Long): PeriodicWorkRequest {
            val effectiveInterval = intervalMinutes.coerceAtLeast(SyncWorkScheduler.MINIMUM_PERIODIC_INTERVAL_MINUTES)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return PeriodicWorkRequestBuilder<DataSyncWorker>(
                effectiveInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
        }
    }
}
