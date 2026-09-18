package dev.anhquocs.truelab.core.domain.theme.usecase

import dev.anhquocs.truelab.core.domain.theme.model.AppThemeMode
import dev.anhquocs.truelab.core.domain.theme.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

data class ThemeUseCases(
    val getThemeModeUseCase: GetThemeModeUseCase,
    val saveThemeModeUseCase: SaveThemeModeUseCase
)

class GetThemeModeUseCase(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<AppThemeMode> = repository.getThemeMode()
}

class SaveThemeModeUseCase(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(themeMode: AppThemeMode) = repository.saveThemeMode(themeMode)
}
