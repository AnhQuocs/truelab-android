package dev.anhquocs.truelab.core.data.prediction.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.prediction.repository.PredictionRepositoryImpl
import dev.anhquocs.truelab.core.domain.prediction.repository.PredictionRepository
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchUseCase
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictionUseCases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PredictionDataModule {

    @Binds
    @Singleton
    abstract fun bindPredictionRepository(
        predictionRepositoryImpl: PredictionRepositoryImpl
    ): PredictionRepository

    companion object {
        @Provides
        @Singleton
        fun providePredictionUseCases(
            repository: PredictionRepository
        ): PredictionUseCases {
            return PredictionUseCases(
                predictMatchUseCase = PredictMatchUseCase(repository)
            )
        }
    }
}
