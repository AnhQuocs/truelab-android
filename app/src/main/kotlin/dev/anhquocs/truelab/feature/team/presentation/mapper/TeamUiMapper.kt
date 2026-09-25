package dev.anhquocs.truelab.feature.team.presentation.mapper

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
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
     */
    fun toRecord(
        team: TeamDetail,
        ranking: SeasonRanking?,
        formScore: FormScore? = null,
        splits: TeamHomeAwaySplits? = null
    ): TeamAnalyticsRecord {
        val formBadges = ranking?.recently?.mapNotNull { item ->
            item.trim().firstOrNull()?.uppercaseChar()
        } ?: emptyList()

        val calculatedScore = formScore?.score?.roundToInt() ?: 0

        val homeWinRate = splits?.let { (it.homeSplit.winRate * 100).roundToInt().toDouble() }
        val homeRecord = splits?.let { "${it.homeSplit.won}W-${it.homeSplit.draw}D-${it.homeSplit.loss}L" } ?: "—"
        val awayWinRate = splits?.let { (it.awaySplit.winRate * 100).roundToInt().toDouble() }
        val awayRecord = splits?.let { "${it.awaySplit.won}W-${it.awaySplit.draw}D-${it.awaySplit.loss}L" } ?: "—"

        return TeamAnalyticsRecord(
            id = team.id.toString(),
            name = team.name,
            logo = team.logo,
            league = team.leagueName ?: "Premier League • England",
            eloRating = team.eloRating.roundToInt(),
            rank = ranking?.position ?: 0,
            played = ranking?.totalMatches ?: 0,
            wins = ranking?.won ?: 0,
            draws = ranking?.draw ?: 0,
            losses = ranking?.loss ?: 0,
            form = formBadges,
            formScore = calculatedScore,
            homeWinRate = homeWinRate,
            homeRecord = homeRecord,
            awayWinRate = awayWinRate,
            awayRecord = awayRecord,
            h2hHighlight = "—"
        )
    }
}
