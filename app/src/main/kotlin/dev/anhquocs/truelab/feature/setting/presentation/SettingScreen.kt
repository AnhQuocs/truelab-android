package dev.anhquocs.truelab.feature.setting.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.language.model.AppLanguage
import dev.anhquocs.truelab.core.domain.theme.model.AppThemeMode
import dev.anhquocs.truelab.core.ui.theme.Dimen
import dev.anhquocs.truelab.core.ui.theme.RadiusLarge
import dev.anhquocs.truelab.core.ui.theme.RadiusMedium
import dev.anhquocs.truelab.core.ui.theme.RadiusPill
import dev.anhquocs.truelab.core.ui.utils.bold
import dev.anhquocs.truelab.core.ui.utils.medium
import dev.anhquocs.truelab.core.ui.utils.s11
import dev.anhquocs.truelab.core.ui.utils.s12
import dev.anhquocs.truelab.core.ui.utils.s13
import dev.anhquocs.truelab.core.ui.utils.s14
import dev.anhquocs.truelab.core.ui.utils.s18
import dev.anhquocs.truelab.core.ui.utils.semiBold
import dev.anhquocs.truelab.feature.language.presentation.viewmodel.LanguageViewModel
import dev.anhquocs.truelab.feature.theme.presentation.viewmodel.ThemeViewModel

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    onChangeLanguage: () -> Unit,
    onSelectedTheme: () -> Unit,
    onNavigateBack: () -> Unit = {},
    languageViewModel: LanguageViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val currentLang by languageViewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentTheme by themeViewModel.currentThemeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SettingTopBar(
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Dimen.PaddingM,
                end = Dimen.PaddingM,
                top = Dimen.PaddingM,
                bottom = Dimen.PaddingXXL
            ),
            verticalArrangement = Arrangement.spacedBy(Dimen.PaddingL)
        ) {
            // Section 1: Preferences (Language & Theme)
            item {
                Text(
                    text = stringResource(R.string.setting_section_preferences),
                    style = MaterialTheme.typography.s12.semiBold(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Dimen.PaddingXS, bottom = Dimen.PaddingXS)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Language Item
                        val langDisplayText = when (currentLang) {
                            AppLanguage.ENGLISH -> "English 🇬🇧"
                            AppLanguage.VIETNAMESE -> "Tiếng Việt 🇻🇳"
                        }

                        SettingActionItem(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.setting_language_title),
                            value = langDisplayText,
                            onClick = onChangeLanguage
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Dimen.PaddingM),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        // Theme Item
                        val (themeIcon, themeDisplayText) = when (currentTheme) {
                            AppThemeMode.LIGHT -> Icons.Default.LightMode to stringResource(R.string.setting_theme_light)
                            AppThemeMode.DARK -> Icons.Default.DarkMode to stringResource(R.string.setting_theme_dark)
                            AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness to stringResource(
                                R.string.setting_theme_system
                            )
                        }

                        SettingActionItem(
                            icon = themeIcon,
                            title = stringResource(R.string.setting_theme_title),
                            value = themeDisplayText,
                            onClick = onSelectedTheme
                        )
                    }
                }
            }

            // Section 2: About TrueLab & System Architecture
            item {
                Text(
                    text = stringResource(R.string.setting_section_about),
                    style = MaterialTheme.typography.s12.semiBold(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Dimen.PaddingXS, bottom = Dimen.PaddingXS)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimen.PaddingS)
                    ) {
                        SettingInfoRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.setting_app_version),
                            value = stringResource(R.string.setting_app_version_val)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Dimen.PaddingM),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        SettingInfoRow(
                            icon = Icons.Default.Code,
                            title = stringResource(R.string.setting_architecture),
                            value = stringResource(R.string.setting_architecture_val)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Dimen.PaddingM),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        SettingInfoRow(
                            icon = Icons.Default.Storage,
                            title = stringResource(R.string.setting_engine),
                            value = stringResource(R.string.setting_engine_val)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimen.TopbarHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Dimen.PaddingS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(Dimen.SizeML),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(Dimen.PaddingS))
            Text(
                text = stringResource(R.string.prediction_title),
                style = MaterialTheme.typography.s18,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SettingActionItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusMedium))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(Dimen.PaddingS),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimen.SizeS)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.s14.semiBold(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingXXS)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusPill))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                    .padding(horizontal = Dimen.PaddingS, vertical = Dimen.PaddingXXS)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.s11.bold(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(Dimen.SizeS)
            )
        }
    }
}

@Composable
private fun SettingInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.PaddingM, vertical = Dimen.PaddingS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimen.PaddingM)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimen.SizeS)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.s13.medium(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.s12.bold(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}