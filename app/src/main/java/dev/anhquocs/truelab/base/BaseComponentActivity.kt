package dev.anhquocs.truelab.base

import android.content.Context
import androidx.activity.ComponentActivity
import dev.anhquocs.truelab.core.data.language.preference.LanguagePreferenceManager
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import dev.anhquocs.truelab.core.ui.localization.LangUtils
import dev.anhquocs.truelab.core.ui.localization.LanguageManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

open class BaseComponentActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val updatedContext = runBlocking {
            val manager = LanguagePreferenceManager(newBase)
            val lang = (manager.languageFlow.firstOrNull() ?: AppLanguage.ENGLISH)
            val contextWithLocale = LanguageManager.setAppLocale(newBase, lang)

            LangUtils.currentLang = lang.code

            contextWithLocale
        }
        super.attachBaseContext(updatedContext)
    }
}
