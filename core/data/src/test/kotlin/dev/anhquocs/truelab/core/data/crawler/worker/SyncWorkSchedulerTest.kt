package dev.anhquocs.truelab.core.data.crawler.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import dev.anhquocs.truelab.core.data.crawler.cache.DatasetCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncWorkSchedulerTest {

    private class FakeSyncWorkScheduler : SyncWorkScheduler {
        var lastInitialForceRefresh: Boolean? = null
        var lastInitialExistingWorkPolicy: ExistingWorkPolicy? = null
        var isInitialCancelled: Boolean = false

        var lastIntervalMinutes: Long? = null
        var lastExistingWorkPolicy: ExistingPeriodicWorkPolicy? = null
        var isCancelled: Boolean = false

        override fun scheduleInitialSync(
            forceRefresh: Boolean,
            existingWorkPolicy: ExistingWorkPolicy
        ) {
            lastInitialForceRefresh = forceRefresh
            lastInitialExistingWorkPolicy = existingWorkPolicy
            isInitialCancelled = false
        }

        override fun cancelInitialSync() {
            isInitialCancelled = true
        }

        override fun schedulePeriodicSync(
            intervalMinutes: Long,
            existingWorkPolicy: ExistingPeriodicWorkPolicy
        ) {
            lastIntervalMinutes = intervalMinutes
            lastExistingWorkPolicy = existingWorkPolicy
            isCancelled = false
        }

        override fun cancelPeriodicSync() {
            isCancelled = true
        }
    }

    // --- Initial Sync Tests ---

    @Test
    fun `createInitialWorkRequest configures NetworkType CONNECTED and default input data`() {
        val request = DefaultSyncWorkScheduler.createInitialWorkRequest(forceRefresh = false)
        assertNotNull(request)

        val constraints = request.workSpec.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)

        val inputData = request.workSpec.input
        assertEquals(false, inputData.getBoolean(DataSyncWorker.KEY_FORCE_REFRESH, true))
        assertEquals(DatasetCategory.SCHEDULED_MATCHES.name, inputData.getString(DataSyncWorker.KEY_DATASET_CATEGORY))
    }

    @Test
    fun `createInitialWorkRequest configures forceRefresh true correctly`() {
        val request = DefaultSyncWorkScheduler.createInitialWorkRequest(forceRefresh = true)
        assertNotNull(request)

        val inputData = request.workSpec.input
        assertEquals(true, inputData.getBoolean(DataSyncWorker.KEY_FORCE_REFRESH, false))
    }

    @Test
    fun `fakeScheduler records initial schedule and cancel parameters correctly`() {
        val fakeScheduler = FakeSyncWorkScheduler()

        fakeScheduler.scheduleInitialSync(
            forceRefresh = true,
            existingWorkPolicy = ExistingWorkPolicy.KEEP
        )

        assertEquals(true, fakeScheduler.lastInitialForceRefresh)
        assertEquals(ExistingWorkPolicy.KEEP, fakeScheduler.lastInitialExistingWorkPolicy)
        assertEquals(false, fakeScheduler.isInitialCancelled)

        fakeScheduler.cancelInitialSync()
        assertEquals(true, fakeScheduler.isInitialCancelled)
    }

    @Test
    fun `initial sync work name constant matches specification`() {
        assertEquals("TrueLabInitialDataSyncWork", SyncWorkScheduler.INITIAL_SYNC_WORK_NAME)
    }

    // --- Periodic Sync Tests ---

    @Test
    fun `createPeriodicWorkRequest configures NetworkType CONNECTED and requiresBatteryNotLow constraints`() {
        val request = DefaultSyncWorkScheduler.createPeriodicWorkRequest(intervalMinutes = 30L)
        assertNotNull(request)

        val constraints = request.workSpec.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())
    }

    @Test
    fun `createPeriodicWorkRequest enforces minimum interval of 15 minutes`() {
        val request = DefaultSyncWorkScheduler.createPeriodicWorkRequest(intervalMinutes = 5L)
        assertNotNull(request)

        // 15 minutes in milliseconds = 15 * 60 * 1000L = 900_000L
        assertEquals(15 * 60 * 1000L, request.workSpec.intervalDuration)
    }

    @Test
    fun `createPeriodicWorkRequest respects intervals greater than minimum`() {
        val request = DefaultSyncWorkScheduler.createPeriodicWorkRequest(intervalMinutes = 60L)
        assertNotNull(request)

        // 60 minutes in milliseconds = 60 * 60 * 1000L = 3_600_000L
        assertEquals(60 * 60 * 1000L, request.workSpec.intervalDuration)
    }

    @Test
    fun `fakeScheduler records schedule and cancel parameters correctly`() {
        val fakeScheduler = FakeSyncWorkScheduler()

        fakeScheduler.schedulePeriodicSync(
            intervalMinutes = 45L,
            existingWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
        )

        assertEquals(45L, fakeScheduler.lastIntervalMinutes)
        assertEquals(ExistingPeriodicWorkPolicy.KEEP, fakeScheduler.lastExistingWorkPolicy)
        assertEquals(false, fakeScheduler.isCancelled)

        fakeScheduler.cancelPeriodicSync()
        assertEquals(true, fakeScheduler.isCancelled)
    }

    @Test
    fun `periodic sync work name constant matches specification`() {
        assertEquals("TrueLabPeriodicDataSyncWork", SyncWorkScheduler.PERIODIC_SYNC_WORK_NAME)
        assertEquals(60L, SyncWorkScheduler.DEFAULT_PERIODIC_INTERVAL_MINUTES)
        assertEquals(15L, SyncWorkScheduler.MINIMUM_PERIODIC_INTERVAL_MINUTES)
    }
}
