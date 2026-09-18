package dev.anhquocs.truelab.core.data.theme.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.anhquocs.truelab.core.data.language.preference.languageDataStore
import dev.anhquocs.truelab.core.domain.theme.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class ThemePreferenceManager(context: Context) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_pref")
    }

    private val dataStore = context.languageDataStore

    val themeModeFlow: Flow<AppThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            val key = preferences[THEME_KEY] ?: AppThemeMode.SYSTEM.key
            AppThemeMode.fromKey(key)
        }

    suspend fun saveThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeMode.key
        }
    }
}
