package dev.anhquocs.truelab.core.data.language.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.language.preference.LanguagePreferenceManager
import dev.anhquocs.truelab.core.data.language.repository.LanguageRepositoryImpl
import dev.anhquocs.truelab.core.domain.language.repository.LanguageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageDataModule {

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        languageRepositoryImpl: LanguageRepositoryImpl
    ): LanguageRepository

    companion object {
        @Provides
        @Singleton
        fun provideLanguagePreferenceManager(
            @ApplicationContext context: Context
        ): LanguagePreferenceManager = LanguagePreferenceManager(context)
    }
}
