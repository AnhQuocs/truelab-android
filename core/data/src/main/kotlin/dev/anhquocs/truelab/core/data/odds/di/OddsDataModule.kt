package dev.anhquocs.truelab.core.data.odds.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.odds.repository.OddsRepositoryImpl
import dev.anhquocs.truelab.core.domain.odds.repository.OddsRepository
import dev.anhquocs.truelab.core.domain.odds.usecase.GetMatchOddsUseCase
import dev.anhquocs.truelab.core.domain.odds.usecase.GetOddsHistoryUseCase
import dev.anhquocs.truelab.core.domain.odds.usecase.OddsUseCases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OddsDataModule {

    @Binds
    @Singleton
    abstract fun bindOddsRepository(
        oddsRepositoryImpl: OddsRepositoryImpl
    ): OddsRepository

    companion object {
        @Provides
        @Singleton
        fun provideOddsUseCases(
            repository: OddsRepository
        ): OddsUseCases {
            return OddsUseCases(
                getMatchOddsUseCase = GetMatchOddsUseCase(repository),
                getOddsHistoryUseCase = GetOddsHistoryUseCase(repository)
            )
        }
    }
}
