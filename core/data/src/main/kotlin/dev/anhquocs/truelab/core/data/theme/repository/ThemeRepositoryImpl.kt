package dev.anhquocs.truelab.core.data.theme.repository

import dev.anhquocs.truelab.core.data.theme.preference.ThemePreferenceManager
import dev.anhquocs.truelab.core.domain.theme.model.AppThemeMode
import dev.anhquocs.truelab.core.domain.theme.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(
    private val manager: ThemePreferenceManager
) : ThemeRepository {

    override fun getThemeMode(): Flow<AppThemeMode> = manager.themeModeFlow

    override suspend fun saveThemeMode(themeMode: AppThemeMode) {
        manager.saveThemeMode(themeMode)
    }
}
