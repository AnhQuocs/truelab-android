package dev.anhquocs.truelab.feature.language.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import dev.anhquocs.truelab.core.domain.language.usecase.LanguageUseCases
import dev.anhquocs.truelab.core.ui.localization.LangUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageUseCases: LanguageUseCases
) : ViewModel() {

    private val _currentLanguage = MutableStateFlow(AppLanguage.fromCode(LangUtils.currentLang))
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            languageUseCases.getLanguageUseCase().collect {
                LangUtils.currentLang = it.code
                _currentLanguage.value = it
            }
        }
    }

    fun changeLanguage(language: AppLanguage) {

        viewModelScope.launch {
            languageUseCases.updateLanguageUseCase(language)
        }
    }
}