package dev.anhquocs.truelab.core.domain.language.repository

import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    fun getLanguage(): Flow<AppLanguage>
    suspend fun saveLanguage(language: AppLanguage)
}
