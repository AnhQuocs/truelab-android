package dev.anhquocs.truelab.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.domain.benchmark.usecase.RunAlgorithmBenchmarkUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SearchMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SortMatchesUseCase
import dev.anhquocs.truelab.core.domain.odds.usecase.AnalyzeOddsTrendUseCase
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateEloRatingUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamStatisticsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SearchTeamsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SortSeasonRankingUseCase
import javax.inject.Singleton

/**
 * Hilt DI Module providing the Pure Computation Domain UseCases.
 *
 * Keeps :core:domain 100% pure Kotlin/JVM without Dagger annotations,
 * while allowing ViewModels in :app to inject them via constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainUseCaseModule {

    @Provides
    @Singleton
    fun provideSearchMatchesUseCase(): SearchMatchesUseCase = SearchMatchesUseCase()

    @Provides
    @Singleton
    fun provideSortMatchesUseCase(): SortMatchesUseCase = SortMatchesUseCase()

    @Provides
    @Singleton
    fun provideSearchTeamsUseCase(): SearchTeamsUseCase = SearchTeamsUseCase()

    @Provides
    @Singleton
    fun provideSortSeasonRankingUseCase(): SortSeasonRankingUseCase = SortSeasonRankingUseCase()

    @Provides
    @Singleton
    fun provideGetTeamStatisticsUseCase(): GetTeamStatisticsUseCase = GetTeamStatisticsUseCase()

    @Provides
    @Singleton
    fun provideAnalyzeOddsTrendUseCase(): AnalyzeOddsTrendUseCase = AnalyzeOddsTrendUseCase()

    @Provides
    @Singleton
    fun provideCalculateTeamFormUseCase(): CalculateTeamFormUseCase = CalculateTeamFormUseCase()

    @Provides
    @Singleton
    fun provideCalculateEloRatingUseCase(): CalculateEloRatingUseCase = CalculateEloRatingUseCase()

    @Provides
    @Singleton
    fun providePredictMatchOutcomeUseCase(): PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase()

    @Provides
    @Singleton
    fun provideRunAlgorithmBenchmarkUseCase(): RunAlgorithmBenchmarkUseCase = RunAlgorithmBenchmarkUseCase()
}
