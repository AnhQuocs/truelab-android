package dev.anhquocs.truelab.core.data.match.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.match.repository.MatchRepositoryImpl
import dev.anhquocs.truelab.core.domain.match.repository.MatchRepository
import dev.anhquocs.truelab.core.domain.match.usecase.GetMatchDetailUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.GetMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.MatchUseCases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MatchDataModule {

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        matchRepositoryImpl: MatchRepositoryImpl
    ): MatchRepository

    companion object {
        @Provides
        @Singleton
        fun provideMatchUseCases(
            repository: MatchRepository
        ): MatchUseCases {
            return MatchUseCases(
                getMatchesUseCase = GetMatchesUseCase(repository),
                getMatchDetailUseCase = GetMatchDetailUseCase(repository)
            )
        }
    }
}
