package dev.anhquocs.truelab.feature.theme.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.domain.theme.repository.ThemeRepository
import dev.anhquocs.truelab.core.domain.theme.usecase.GetThemeModeUseCase
import dev.anhquocs.truelab.core.domain.theme.usecase.SaveThemeModeUseCase
import dev.anhquocs.truelab.core.domain.theme.usecase.ThemeUseCases
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThemeUseCaseModule {

    @Provides
    @Singleton
    fun provideThemeUseCases(
        repository: ThemeRepository
    ): ThemeUseCases {
        return ThemeUseCases(
            getThemeModeUseCase = GetThemeModeUseCase(repository),
            saveThemeModeUseCase = SaveThemeModeUseCase(repository)
        )
    }
}
