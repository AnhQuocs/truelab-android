package dev.anhquocs.truelab.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.anhquocs.truelab.feature.analytics.presentation.AnalyticsScreen
import dev.anhquocs.truelab.feature.benchmark.presentation.BenchmarkScreen
import dev.anhquocs.truelab.feature.home.presentation.HomeScreen
import dev.anhquocs.truelab.feature.language.presentation.ui.ChangeLanguageBottomSheet
import dev.anhquocs.truelab.feature.match.presentation.MatchesScreen
import dev.anhquocs.truelab.feature.prediction.presentation.PredictionScreen
import dev.anhquocs.truelab.feature.setting.presentation.SettingScreen
import dev.anhquocs.truelab.feature.setting.presentation.components.ChangeThemeBottomSheet
import dev.anhquocs.truelab.feature.team.presentation.TeamsScreen
import dev.anhquocs.truelab.navigation.BottomNavVisibilityState
import dev.anhquocs.truelab.navigation.LocalBottomNavVisibility
import dev.anhquocs.truelab.navigation.MainNavBar
import dev.anhquocs.truelab.navigation.MainNavDestination

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val visibilityState = remember { BottomNavVisibilityState() }

    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var showThemeBottomSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainNavDestination.Home.route

    CompositionLocalProvider(LocalBottomNavVisibility provides visibilityState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = MainNavDestination.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainNavDestination.Home.route) {
                    HomeScreen(
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSetting = {
                            navController.navigate("setting") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(MainNavDestination.Matches.route) {
                    MatchesScreen(
                        onNavigateToPrediction = {
                            navController.navigate(MainNavDestination.Prediction.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(MainNavDestination.Teams.route) {
                    TeamsScreen()
                }

                composable(MainNavDestination.Analytics.route) {
                    AnalyticsScreen()
                }

                composable(MainNavDestination.Prediction.route) {
                    PredictionScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(MainNavDestination.Benchmark.route) {
                    BenchmarkScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("setting") {
                    SettingScreen(
                        onChangeLanguage = { showLanguageBottomSheet = true },
                        onSelectedTheme = { showThemeBottomSheet = true },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // Kiểm tra xem màn hình hiện tại có thuộc Bottom Nav không
            val isBottomNavDestination = MainNavDestination.bottomNavItems.any { it.route == currentRoute }
            val shouldShowBottomNav = visibilityState.visible && isBottomNavDestination

            val bottomNavOffsetY = animateDpAsState(
                targetValue = if (shouldShowBottomNav) 0.dp else 120.dp,
                animationSpec = tween(durationMillis = 300),
                label = "bottomNavOffset",
            )

            MainNavBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, bottomNavOffsetY.value.roundToPx()) },
                currentRoute = currentRoute,
                onNavigateToDestination = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )

            // Bottom Sheets
            if (showLanguageBottomSheet) {
                ChangeLanguageBottomSheet(
                    onDismissRequest = { showLanguageBottomSheet = false }
                )
            }

            if (showThemeBottomSheet) {
                ChangeThemeBottomSheet(
                    onDismissRequest = { showThemeBottomSheet = false }
                )
            }
        }
    }
}