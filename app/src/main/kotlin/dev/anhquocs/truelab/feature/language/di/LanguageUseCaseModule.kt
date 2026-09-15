package dev.anhquocs.truelab.feature.language.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.domain.language.repository.LanguageRepository
import dev.anhquocs.truelab.core.domain.language.usecase.GetLanguageUseCase
import dev.anhquocs.truelab.core.domain.language.usecase.LanguageUseCases
import dev.anhquocs.truelab.core.domain.language.usecase.UpdateLanguageUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LanguageUseCaseModule {

    @Provides
    @Singleton
    fun provideLanguageUseCases(
        repository: LanguageRepository
    ): LanguageUseCases {
        return LanguageUseCases(
            getLanguageUseCase = GetLanguageUseCase(repository),
            updateLanguageUseCase = UpdateLanguageUseCase(repository)
        )
    }
}
