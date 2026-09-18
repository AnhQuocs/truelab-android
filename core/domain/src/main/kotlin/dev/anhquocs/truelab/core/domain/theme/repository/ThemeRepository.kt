package dev.anhquocs.truelab.core.domain.theme.repository

import dev.anhquocs.truelab.core.domain.theme.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeMode(): Flow<AppThemeMode>
    suspend fun saveThemeMode(themeMode: AppThemeMode)
}
