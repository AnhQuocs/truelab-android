package dev.anhquocs.truelab.core.data.crawler.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.crawler.retry.DefaultRetryClassifier
import dev.anhquocs.truelab.core.data.crawler.retry.RetryClassifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CrawlerDataModule {

    @Binds
    @Singleton
    abstract fun bindRetryClassifier(
        defaultRetryClassifier: DefaultRetryClassifier
    ): RetryClassifier
}
