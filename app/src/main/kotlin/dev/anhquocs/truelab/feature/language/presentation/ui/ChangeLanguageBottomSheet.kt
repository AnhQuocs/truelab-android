package dev.anhquocs.truelab.feature.language.presentation.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.MainActivity
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import dev.anhquocs.truelab.core.ui.localization.LangUtils
import dev.anhquocs.truelab.core.ui.theme.ButtonHeightMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.SpacingL
import dev.anhquocs.truelab.core.ui.theme.SpacingM
import dev.anhquocs.truelab.core.ui.theme.SpacingS
import dev.anhquocs.truelab.feature.language.presentation.viewmodel.LanguageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLanguageBottomSheet(
    onDismissRequest: () -> Unit,
    languageViewModel: LanguageViewModel = hiltViewModel()
) {
    val currentLang by languageViewModel.currentLanguage.collectAsStateWithLifecycle()
    var selectedLang by remember(currentLang) { mutableStateOf(currentLang) }

    val context = LocalContext.current
    val activity = context as? Activity

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLarge, topEnd = RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingM)
                .padding(bottom = SpacingL, top = SpacingM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(id = R.string.select_language),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(vertical = SpacingS)
            )

            Spacer(modifier = Modifier.height(SpacingM))

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusLarge))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .fillMaxWidth()
                    .padding(SpacingS)
            ) {
                LanguageOption(
                    title = stringResource(id = R.string.language_english) + " 🇬🇧",
                    isSelected = selectedLang == AppLanguage.ENGLISH,
                    onClick = { selectedLang = AppLanguage.ENGLISH }
                )

                LanguageOption(
                    title = stringResource(id = R.string.language_vietnamese) + " 🇻🇳",
                    isSelected = selectedLang == AppLanguage.VIETNAMESE,
                    onClick = { selectedLang = AppLanguage.VIETNAMESE }
                )
            }

            Spacer(modifier = Modifier.height(SpacingL))

            Button(
                onClick = {
                    val code = selectedLang.code
                    LangUtils.updateLocale(context, code)
                    languageViewModel.changeLanguage(selectedLang)

                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    activity?.overridePendingTransition(0, 0)
                },
                shape = RoundedCornerShape(RadiusLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeightMedium)
            ) {
                Text(
                    text = stringResource(id = R.string.apply),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun LanguageOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(horizontal = SpacingL, vertical = SpacingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
        )

        RadioButton(
            selected = isSelected,
            onClick = { onClick() },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

