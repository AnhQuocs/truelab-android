package dev.anhquocs.truelab.core.ui.localization

import android.content.Context
import java.util.Locale

object LangUtils {
    var currentLang: String = "en"

    fun getLocalizedText(map: Map<String, String>?): String {
        return map?.get(currentLang) ?: map?.get("en") ?: ""
    }

    fun getLocalizedList(map: Map<String, List<String>>?): List<String> {
        return map?.get(currentLang) ?: emptyList()
    }

    fun updateLocale(context: Context, langCode: String) {
        val locale = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration

        config.setLocale(locale)
        context.createConfigurationContext(config)

        currentLang = langCode
    }
}
