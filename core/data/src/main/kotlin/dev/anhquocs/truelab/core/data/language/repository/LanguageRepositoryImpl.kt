package dev.anhquocs.truelab.core.data.language.repository

import dev.anhquocs.truelab.core.data.language.preference.LanguagePreferenceManager
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import dev.anhquocs.truelab.core.domain.language.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LanguageRepositoryImpl @Inject constructor(
    private val manager: LanguagePreferenceManager
) : LanguageRepository {

    override fun getLanguage(): Flow<AppLanguage> = manager.languageFlow

    override suspend fun saveLanguage(language: AppLanguage) {
        manager.saveLanguage(language)
    }
}
