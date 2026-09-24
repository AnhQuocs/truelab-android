package dev.anhquocs.truelab.feature.team.presentation.model

/**
 * Model biểu diễn hồ sơ thống kê đội bóng phục vụ hiển thị UI.
 */
data class TeamAnalyticsRecord(
    val id: String,
    val name: String,
    val logo: String? = null,
    val league: String = "Football League",
    val eloRating: Int = 1500,
    val rank: Int = 0,
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val form: List<Char> = emptyList(),
    val formScore: Int = 0,
    val homeWinRate: Double? = null,
    val homeRecord: String = "—",
    val awayWinRate: Double? = null,
    val awayRecord: String = "—",
    val h2hHighlight: String = "—"
)
