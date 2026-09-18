package dev.anhquocs.truelab.core.data.team.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.team.repository.TeamRepositoryImpl
import dev.anhquocs.truelab.core.domain.team.repository.TeamRepository
import dev.anhquocs.truelab.core.domain.team.usecase.GetSeasonRankingUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamDetailUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.TeamUseCases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamDataModule {

    @Binds
    @Singleton
    abstract fun bindTeamRepository(
        teamRepositoryImpl: TeamRepositoryImpl
    ): TeamRepository

    companion object {
        @Provides
        @Singleton
        fun provideTeamUseCases(
            repository: TeamRepository
        ): TeamUseCases {
            return TeamUseCases(
                getTeamDetailUseCase = GetTeamDetailUseCase(repository),
                getSeasonRankingUseCase = GetSeasonRankingUseCase(repository)
            )
        }
    }
}
