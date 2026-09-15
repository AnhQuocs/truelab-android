package dev.anhquocs.truelab

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.anhquocs.truelab.base.BaseComponentActivity
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.feature.language.presentation.ui.ChangeLanguageBottomSheet
import dev.anhquocs.truelab.feature.language.presentation.viewmodel.LanguageViewModel
import dev.anhquocs.truelab.feature.language.presentation.getDisplayNameRes
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

            TrueLabTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Greeting(name = stringResource(currentLanguage.getDisplayNameRes()))
                        Spacer(modifier = Modifier.height(SpacingL))
                        Button(onClick = { showLanguageBottomSheet = true }) {
                            Text(text = stringResource(R.string.change_language_title))
                        }
                    }

                    if (showLanguageBottomSheet) {
                        ChangeLanguageBottomSheet(
                            onDismissRequest = {
                                showLanguageBottomSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrueLabTheme {
        Greeting("Android")
    }
}