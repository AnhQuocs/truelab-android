package dev.anhquocs.truelab.core.data.theme.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.theme.preference.ThemePreferenceManager
import dev.anhquocs.truelab.core.data.theme.repository.ThemeRepositoryImpl
import dev.anhquocs.truelab.core.domain.theme.repository.ThemeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeDataModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository

    companion object {
        @Provides
        @Singleton
        fun provideThemePreferenceManager(
            @ApplicationContext context: Context
        ): ThemePreferenceManager = ThemePreferenceManager(context)
    }
}
