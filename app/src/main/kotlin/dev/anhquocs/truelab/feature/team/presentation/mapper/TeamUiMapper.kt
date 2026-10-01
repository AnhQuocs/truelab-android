package dev.anhquocs.truelab.feature.team.presentation.mapper

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.TeamDetail
import dev.anhquocs.truelab.core.domain.team.model.TeamHomeAwaySplits
import dev.anhquocs.truelab.feature.team.presentation.model.TeamAnalyticsRecord
import kotlin.math.roundToInt

object TeamUiMapper {

    /**
     * Ánh xạ Domain models sang [TeamAnalyticsRecord] phục vụ UI.
     *
     * @param team Thông tin chi tiết đội bóng (chứa tên, logo, leagueName, eloRating).
     * @param ranking Bảng xếp hạng mùa giải từ Repository (nếu có).
     * @param formScore Kết quả tính toán phong độ time-decay từ [CalculateTeamFormUseCase] (nếu có).
     * @param splits Kết quả tính toán phân tách sân nhà / sân khách từ [CalculateHomeAwaySplitsUseCase] (nếu có).
     * @param recentMatches Danh sách trận đấu gần nhất của đội bóng (nếu có, dùng để fallback form badges khi ranking không có).
     */
    fun toRecord(
        team: TeamDetail,
        ranking: SeasonRanking?,
        formScore: FormScore? = null,
        splits: TeamHomeAwaySplits? = null,
        recentMatches: List<Match> = emptyList()
    ): TeamAnalyticsRecord {
        val formBadges = if (!ranking?.recently.isNullOrEmpty()) {
            ranking!!.recently.mapNotNull { item ->
                item.trim().firstOrNull()?.uppercaseChar()
            }
        } else {
            recentMatches.sortedBy { it.startTimeDate }.mapNotNull { match ->
                val hs = match.homeScore ?: return@mapNotNull null
                val as_ = match.awayScore ?: return@mapNotNull null
                when {
                    hs == as_ -> 'D'
                    match.homeTeam.id == team.id -> if (hs > as_) 'W' else 'L'
                    match.awayTeam.id == team.id -> if (as_ > hs) 'W' else 'L'
                    else -> null
                }
            }
        }

        val formPoints = formScore?.totalPoints?.roundToInt() ?: 0
        val maxFormPoints = if (formScore != null && formScore.maxPoints > 0) {
            formScore.maxPoints.roundToInt()
        } else {
            15
        }
        val calculatedScore = formScore?.score?.roundToInt() ?: 0

        val homeWinRate = splits?.let {
            if (it.homeSplit.played > 0) (it.homeSplit.winRate * 100).roundToInt().toDouble() else null
        }
        val homeRecord = splits?.let {
            if (it.homeSplit.played > 0) "${it.homeSplit.won}W-${it.homeSplit.draw}D-${it.homeSplit.loss}L" else "—"
        } ?: "—"

        val awayWinRate = splits?.let {
            if (it.awaySplit.played > 0) (it.awaySplit.winRate * 100).roundToInt().toDouble() else null
        }
        val awayRecord = splits?.let {
            if (it.awaySplit.played > 0) "${it.awaySplit.won}W-${it.awaySplit.draw}D-${it.awaySplit.loss}L" else "—"
        } ?: "—"

        val played = ranking?.totalMatches ?: splits?.totalSplit?.played ?: 0
        val wins = ranking?.won ?: splits?.totalSplit?.won ?: 0
        val draws = ranking?.draw ?: splits?.totalSplit?.draw ?: 0
        val losses = ranking?.loss ?: splits?.totalSplit?.loss ?: 0

        return TeamAnalyticsRecord(
            id = team.id.toString(),
            name = team.name,
            logo = team.logo,
            league = team.leagueName ?: "Premier League • England",
            eloRating = team.eloRating.roundToInt(),
            rank = ranking?.position ?: 0,
            played = played,
            wins = wins,
            draws = draws,
            losses = losses,
            form = formBadges,
            formPoints = formPoints,
            maxFormPoints = maxFormPoints,
            formScore = calculatedScore,
            homeWinRate = homeWinRate,
            homeRecord = homeRecord,
            awayWinRate = awayWinRate,
            awayRecord = awayRecord,
            h2hHighlight = "—"
        )
    }
}

