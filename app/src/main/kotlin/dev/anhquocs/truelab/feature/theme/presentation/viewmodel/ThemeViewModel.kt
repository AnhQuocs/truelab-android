package dev.anhquocs.truelab.feature.theme.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.core.domain.theme.model.AppThemeMode
import dev.anhquocs.truelab.core.domain.theme.usecase.ThemeUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeUseCases: ThemeUseCases
) : ViewModel() {

    val currentThemeMode: StateFlow<AppThemeMode> = themeUseCases.getThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppThemeMode.SYSTEM
        )

    fun changeThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            themeUseCases.saveThemeModeUseCase(themeMode)
        }
    }
}
