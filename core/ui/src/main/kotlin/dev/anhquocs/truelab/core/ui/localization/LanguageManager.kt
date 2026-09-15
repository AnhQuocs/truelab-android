package dev.anhquocs.truelab.core.ui.localization

import android.content.Context
import android.os.LocaleList
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import java.util.Locale

object LanguageManager {
    fun setAppLocale(context: Context, language: AppLanguage): Context {
        val locale = Locale.forLanguageTag(language.code)
        Locale.setDefault(locale)

        val config = context.resources.configuration

        config.setLocales(LocaleList(locale))

        return context.createConfigurationContext(config)
    }
}
