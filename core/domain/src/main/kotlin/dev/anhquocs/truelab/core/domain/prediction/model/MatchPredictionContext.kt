package dev.anhquocs.truelab.core.domain.prediction.model

import dev.anhquocs.truelab.core.algorithm.evaluation.FormScore
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem

/**
 * Ngữ cảnh dữ liệu đầu vào cho quy trình dự đoán kết quả trận đấu (Prediction Orchestration).
 *
 * @property matchId ID của trận đấu cần dự đoán.
 * @property homeTeamId ID của đội nhà.
 * @property awayTeamId ID của đội khách.
 * @property homeElo Điểm Elo của đội nhà (tùy chọn, mặc định null -> fallback 1500.0).
 * @property awayElo Điểm Elo của đội khách (tùy chọn, mặc định null -> fallback 1500.0).
 * @property homeRecentMatches Danh sách trận đấu gần đây của đội nhà (tự động loại trừ trận hiện tại).
 * @property awayRecentMatches Danh sách trận đấu gần đây của đội khách (tự động loại trừ trận hiện tại).
 * @property h2hMatches Danh sách trận đấu đối đầu lịch sử (tự động loại trừ trận hiện tại).
 * @property latestOdds Tỷ lệ cược mới nhất (tùy chọn, null -> weight = 0.0).
 * @property isNeutralVenue Cờ xác định thi đấu tại sân trung lập (mặc định false).
 * @property homeFormScore Điểm phong độ đã tính sẵn của đội nhà (nếu có, ưu tiên hơn homeRecentMatches).
 * @property awayFormScore Điểm phong độ đã tính sẵn của đội khách (nếu có, ưu tiên hơn awayRecentMatches).
 * @property homeMeanScored Số bàn thắng trung bình của đội nhà (nếu có, ưu tiên hơn tính từ recent matches).
 * @property homeMeanConceded Số bàn thua trung bình của đội nhà (nếu có, ưu tiên hơn tính từ recent matches).
 * @property awayMeanScored Số bàn thắng trung bình của đội khách (nếu có, ưu tiên hơn tính từ recent matches).
 * @property awayMeanConceded Số bàn thua trung bình của đội khách (nếu có, ưu tiên hơn tính từ recent matches).
 * @property homeWins Số trận thắng đối đầu của đội nhà (nếu có, ưu tiên hơn tính từ h2hMatches).
 * @property draws Số trận hòa đối đầu (nếu có, ưu tiên hơn tính từ h2hMatches).
 * @property awayWins Số trận thắng đối đầu của đội khách (nếu có, ưu tiên hơn tính từ h2hMatches).
 */
data class MatchPredictionContext(
    val matchId: Long,
    val homeTeamId: Int,
    val awayTeamId: Int,
    val homeElo: Double? = null,
    val awayElo: Double? = null,
    val homeRecentMatches: List<Match> = emptyList(),
    val awayRecentMatches: List<Match> = emptyList(),
    val h2hMatches: List<Match> = emptyList(),
    val latestOdds: OddsRecordItem? = null,
    val isNeutralVenue: Boolean = false,
    val homeFormScore: FormScore? = null,
    val awayFormScore: FormScore? = null,
    val homeMeanScored: Double? = null,
    val homeMeanConceded: Double? = null,
    val awayMeanScored: Double? = null,
    val awayMeanConceded: Double? = null,
    val homeWins: Int? = null,
    val draws: Int? = null,
    val awayWins: Int? = null
)
