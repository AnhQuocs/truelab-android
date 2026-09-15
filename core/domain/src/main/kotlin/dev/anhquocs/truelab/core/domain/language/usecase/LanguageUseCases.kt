package dev.anhquocs.truelab.core.domain.language.usecase

import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import dev.anhquocs.truelab.core.domain.language.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow

data class LanguageUseCases(
    val getLanguageUseCase: GetLanguageUseCase,
    val updateLanguageUseCase: UpdateLanguageUseCase
)

class GetLanguageUseCase(
    private val repository: LanguageRepository
) {
    operator fun invoke(): Flow<AppLanguage> = repository.getLanguage()
}

class UpdateLanguageUseCase(
    private val repository: LanguageRepository
) {
    suspend operator fun invoke(language: AppLanguage) = repository.saveLanguage(language)
}
