package dev.anhquocs.truelab.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.anhquocs.truelab.R

sealed class MainNavDestination(
    val route: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    data object Home : MainNavDestination(
        route = "home",
        titleRes = R.string.nav_home,
        iconRes = R.drawable.ic_home
    )

    data object Matches : MainNavDestination(
        route = "matches",
        titleRes = R.string.nav_matches,
        iconRes = R.drawable.ic_matches
    )

    data object Teams : MainNavDestination(
        route = "teams",
        titleRes = R.string.nav_teams,
        iconRes = R.drawable.ic_teams
    )

    data object Analytics : MainNavDestination(
        route = "analytics",
        titleRes = R.string.nav_analytics,
        iconRes = R.drawable.ic_analytics
    )

    data object Prediction : MainNavDestination(
        route = "prediction",
        titleRes = R.string.nav_prediction,
        iconRes = R.drawable.ic_prediction
    )

    data object Benchmark : MainNavDestination(
        route = "benchmark",
        titleRes = R.string.nav_benchmark,
        iconRes = R.drawable.ic_benchmark
    )

    companion object {
        val bottomNavItems: List<MainNavDestination>
            get() = listOf(Home, Matches, Teams, Analytics)

        val all: List<MainNavDestination>
            get() = listOf(Home, Matches, Teams, Analytics, Prediction, Benchmark)
    }
}