package dev.anhquocs.truelab.core.data.crawler.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.crawler.DataSyncEngine
import dev.anhquocs.truelab.core.data.crawler.cache.CacheFreshnessChecker
import dev.anhquocs.truelab.core.data.crawler.cache.DataFreshnessPolicy
import dev.anhquocs.truelab.core.data.crawler.cache.DefaultCacheFreshnessChecker
import dev.anhquocs.truelab.core.data.crawler.retry.DefaultRetryClassifier
import dev.anhquocs.truelab.core.data.crawler.retry.RetryClassifier
import dev.anhquocs.truelab.core.data.crawler.retry.RetryExecutor
import dev.anhquocs.truelab.core.data.crawler.worker.DefaultSyncWorkScheduler
import dev.anhquocs.truelab.core.data.crawler.worker.SyncWorkScheduler
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.ranking.remote.api.RankingApi
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CrawlerDataModule {

    @Binds
    @Singleton
    abstract fun bindRetryClassifier(
        defaultRetryClassifier: DefaultRetryClassifier
    ): RetryClassifier

    @Binds
    @Singleton
    abstract fun bindCacheFreshnessChecker(
        defaultCacheFreshnessChecker: DefaultCacheFreshnessChecker
    ): CacheFreshnessChecker

    @Binds
    @Singleton
    abstract fun bindSyncWorkScheduler(
        defaultSyncWorkScheduler: DefaultSyncWorkScheduler
    ): SyncWorkScheduler

    companion object {
        @Provides
        @Singleton
        fun provideDataFreshnessPolicy(): DataFreshnessPolicy = DataFreshnessPolicy()

        @Provides
        @Singleton
        fun provideRetryExecutor(
            classifier: RetryClassifier
        ): RetryExecutor = RetryExecutor(classifier = classifier)

        @Provides
        @Singleton
        fun provideDataSyncEngine(
            matchApi: MatchApi,
            oddsApi: OddsApi,
            rankingApi: RankingApi,
            database: TrueLabDatabase,
            json: Json,
            metadataRepository: DatasetMetadataRepository,
            retryExecutor: RetryExecutor,
            cacheFreshnessChecker: CacheFreshnessChecker,
            freshnessPolicy: DataFreshnessPolicy
        ): DataSyncEngine = DataSyncEngine(
            matchApi = matchApi,
            oddsApi = oddsApi,
            rankingApi = rankingApi,
            database = database,
            json = json,
            metadataRepository = metadataRepository,
            retryExecutor = retryExecutor,
            cacheFreshnessChecker = cacheFreshnessChecker,
            freshnessPolicy = freshnessPolicy
        )

        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context
        ): WorkManager = WorkManager.getInstance(context)
    }
}
