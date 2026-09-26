package dev.anhquocs.truelab

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.anhquocs.truelab.core.data.crawler.worker.SyncWorkScheduler
import javax.inject.Inject

@HiltAndroidApp
class TrueLabApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncWorkScheduler: SyncWorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncWorkScheduler.scheduleInitialSync()
        syncWorkScheduler.schedulePeriodicSync()
    }
}
