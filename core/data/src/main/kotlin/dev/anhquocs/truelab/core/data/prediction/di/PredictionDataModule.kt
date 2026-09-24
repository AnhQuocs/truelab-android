package dev.anhquocs.truelab.core.data.prediction.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.prediction.repository.PredictionRepositoryImpl
import dev.anhquocs.truelab.core.domain.prediction.repository.PredictionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PredictionDataModule {

    @Binds
    @Singleton
    abstract fun bindPredictionRepository(
        predictionRepositoryImpl: PredictionRepositoryImpl
    ): PredictionRepository
}
