package dev.anhquocs.truelab.presentation

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.anhquocs.truelab.base.BaseComponentActivity
import dev.anhquocs.truelab.feature.language.presentation.viewmodel.LanguageViewModel
import dev.anhquocs.truelab.ui.theme.TrueLabTheme

@AndroidEntryPoint
class MainActivity : BaseComponentActivity() {

    private val languageViewModel: LanguageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentLanguage by languageViewModel.currentLanguage.collectAsStateWithLifecycle()
            var showLanguageBottomSheet by remember { mutableStateOf(false) }

            val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()

            TrueLabTheme {
                val systemBarColorArgb = MaterialTheme.colorScheme.background.toArgb()

                SideEffect {
                    enableEdgeToEdge(
                        statusBarStyle = if (isDarkMode) {
                            SystemBarStyle.dark(systemBarColorArgb)
                        } else {
                            SystemBarStyle.light(systemBarColorArgb, systemBarColorArgb)
                        },
                        navigationBarStyle = if (isDarkMode) {
                            SystemBarStyle.dark(systemBarColorArgb)
                        } else {
                            SystemBarStyle.light(systemBarColorArgb, systemBarColorArgb)
                        },
                    )
                }
                MainScreen()
            }
        }
    }
}