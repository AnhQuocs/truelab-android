package dev.anhquocs.truelab.core.domain.league.model

data class League(
    val id: Int,
    val name: String,
    val shortName: String? = null,
    val logo: String? = null,
    val country: String? = null,
    val category: String? = null
)

data class Season(
    val id: String,
    val leagueId: Int,
    val name: String,
    val year: Int,
    val isCurrent: Boolean = false,
    val startDate: String? = null,
    val endDate: String? = null
)
