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
import androidx.compose.runtime.remember
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
import dev.anhquocs.truelab.feature.home.presentation.HomeScreen
import dev.anhquocs.truelab.feature.match.presentation.MatchesScreen
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
                modifier = Modifier
                    .fillMaxSize()
            ) {
                composable(MainNavDestination.Home.route) {
                    HomeScreen()
                }

                composable(MainNavDestination.Matches.route) {
                    MatchesScreen()
                }

                composable(MainNavDestination.Teams.route) {
                    TeamsScreen()
                }

                composable(MainNavDestination.Analytics.route) {
                    AnalyticsScreen()
                }
            }

            val bottomNavOffsetY = animateDpAsState(
                targetValue = if (visibilityState.visible) 0.dp else 120.dp,
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
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}