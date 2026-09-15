package dev.anhquocs.truelab.feature.language.presentation

import androidx.annotation.StringRes
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage

@StringRes
fun AppLanguage.getDisplayNameRes(): Int = when (this) {
    AppLanguage.VIETNAMESE -> R.string.language_vietnamese
    AppLanguage.ENGLISH -> R.string.language_english
}
