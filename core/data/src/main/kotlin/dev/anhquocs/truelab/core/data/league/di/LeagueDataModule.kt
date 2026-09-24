package dev.anhquocs.truelab.core.data.league.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.league.repository.LeagueRepositoryImpl
import dev.anhquocs.truelab.core.domain.league.repository.LeagueRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LeagueDataModule {

    @Binds
    @Singleton
    abstract fun bindLeagueRepository(
        leagueRepositoryImpl: LeagueRepositoryImpl
    ): LeagueRepository
}
