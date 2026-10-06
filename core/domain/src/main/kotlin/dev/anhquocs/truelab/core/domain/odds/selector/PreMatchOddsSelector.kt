package dev.anhquocs.truelab.core.domain.odds.selector

import dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Utility và nghiệp vụ chọn lọc tỷ lệ kèo trước giờ thi đấu (Pre-Match Odds Selector).
 *
 * Đảm bảo nguyên tắc bảo toàn dữ liệu (Zero Temporal Data Leakage):
 * - Chỉ chấp nhận tỷ lệ kèo phát sinh trước thời điểm bắt đầu trận đấu (changeTime < kickoff).
 * - Loại bỏ tuyệt đối các tỷ lệ kèo trực tiếp trong trận (Live Odds / In-Play / Rolling Ball).
 * - Ưu tiên snapshot cập nhật sát giờ thi đấu nhất (immediate/instant) so với kèo mở ban đầu (initial).
 */
object PreMatchOddsSelector {

    private val LIVE_MARKET_PHASES = setOf(
        "rolling_ball",
        "in_play",
        "live",
        "running",
        "inplay"
    )

    private val ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Chọn lọc bản ghi Kèo Châu Âu (1X2 / EU) tối ưu và hợp lệ nhất trước giờ bóng lăn.
     *
     * @param oddsList Danh sách các bản ghi tỷ lệ kèo của trận đấu.
     * @param kickoffTime Chuỗi thời gian bắt đầu trận đấu (ISO 8601 hoặc yyyy-MM-dd HH:mm:ss).
     * @return [OddsRecordItem] hợp lệ trước trận, hoặc null nếu không có dữ liệu phù hợp.
     */
    fun selectPreMatchEuropeanOdds(
        oddsList: List<OddsRecordItem>,
        kickoffTime: String?
    ): OddsRecordItem? {
        val kickoffEpochSeconds = parseKickoffEpochSeconds(kickoffTime)
        return selectPreMatchEuropeanOdds(oddsList, kickoffEpochSeconds)
    }

    /**
     * Chọn lọc bản ghi Kèo Châu Âu (1X2 / EU) tối ưu theo epoch seconds của thời điểm bắt đầu trận.
     */
    fun selectPreMatchEuropeanOdds(
        oddsList: List<OddsRecordItem>,
        kickoffEpochSeconds: Long?
    ): OddsRecordItem? {
        val validCandidates = oddsList.filter { odds ->
            isPreMatchEuropeanOddsValid(odds, kickoffEpochSeconds)
        }

        return rankAndSelectBestSnapshot(validCandidates)
    }

    /**
     * Chọn lọc bản ghi Kèo Châu Á (Asian Handicap / asia) hợp lệ trước trận (Data Foundation).
     */
    fun selectPreMatchAsianHandicapOdds(
        oddsList: List<OddsRecordItem>,
        kickoffTime: String?
    ): OddsRecordItem? {
        val kickoffEpochSeconds = parseKickoffEpochSeconds(kickoffTime)
        val validCandidates = oddsList.filter { odds ->
            val type = odds.oddsType.lowercase().trim()
            (type == "asia" || type == "as") &&
                isPreMatchPhaseAndTimingValid(odds, kickoffEpochSeconds) &&
                (odds.handicap != null || (odds.homeWin != null && odds.awayWin != null))
        }
        return rankAndSelectBestSnapshot(validCandidates)
    }

    /**
     * Chọn lọc bản ghi Kèo Tổng số bàn thắng (Over/Under / bs) hợp lệ trước trận (Data Foundation).
     */
    fun selectPreMatchOverUnderOdds(
        oddsList: List<OddsRecordItem>,
        kickoffTime: String?
    ): OddsRecordItem? {
        val kickoffEpochSeconds = parseKickoffEpochSeconds(kickoffTime)
        val validCandidates = oddsList.filter { odds ->
            val type = odds.oddsType.lowercase().trim()
            type == "bs" &&
                isPreMatchPhaseAndTimingValid(odds, kickoffEpochSeconds) &&
                (odds.over != null && odds.under != null)
        }
        return rankAndSelectBestSnapshot(validCandidates)
    }

    /**
     * Kiểm tra tính hợp lệ toàn diện của bản ghi 1X2 European Odds.
     */
    private fun isPreMatchEuropeanOddsValid(
        odds: OddsRecordItem,
        kickoffEpochSeconds: Long?
    ): Boolean {
        if (!odds.oddsType.equals("eu", ignoreCase = true)) return false

        val hw = odds.homeWin
        val d = odds.draw
        val aw = odds.awayWin
        if (hw == null || d == null || aw == null) return false
        if (hw <= 1.0 || d <= 1.0 || aw <= 1.0) return false

        return isPreMatchPhaseAndTimingValid(odds, kickoffEpochSeconds)
    }

    /**
     * Kiểm tra giai đoạn thị trường và mốc thời gian trước trận (Strict Invariant: changeTime < kickoff).
     */
    private fun isPreMatchPhaseAndTimingValid(
        odds: OddsRecordItem,
        kickoffEpochSeconds: Long?
    ): Boolean {
        val phase = (odds.marketPhase ?: "").lowercase().trim()
        if (phase in LIVE_MARKET_PHASES) return false

        if (kickoffEpochSeconds != null && kickoffEpochSeconds > 0) {
            if (odds.changeTime >= kickoffEpochSeconds) {
                return false
            }
        }

        return true
    }

    /**
     * Xếp hạng và chọn ra snapshot chất lượng nhất:
     * 1. Giai đoạn: immediate/instant/rỗng (rank 2) > initial (rank 1) > khác (rank 0).
     * 2. Thời gian: changeTime lớn nhất (sát giờ kickoff nhất nhưng vẫn < kickoff).
     */
    private fun rankAndSelectBestSnapshot(candidates: List<OddsRecordItem>): OddsRecordItem? {
        if (candidates.isEmpty()) return null

        return candidates.maxWithOrNull(
            compareBy<OddsRecordItem> { odds ->
                getMarketPhasePriority(odds.marketPhase)
            }.thenBy { odds ->
                odds.changeTime
            }
        )
    }

    private fun getMarketPhasePriority(marketPhase: String?): Int {
        val phase = (marketPhase ?: "").lowercase().trim()
        return when {
            phase == "immediate" || phase == "instant" || phase.isEmpty() -> 2
            phase == "initial" -> 1
            else -> 0
        }
    }

    /**
     * Chuyển đổi chuỗi ngày giờ sang epoch seconds (UTC).
     */
    fun parseKickoffEpochSeconds(rawDateTime: String?): Long? {
        if (rawDateTime.isNullOrBlank()) return null
        var trimmed = rawDateTime.trim()
        if (trimmed.length == 16 && (trimmed[10] == ' ' || trimmed[10] == 'T')) {
            trimmed += ":00"
        }

        return try {
            Instant.parse(trimmed).epochSecond
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(trimmed, ISO_OFFSET_FORMATTER).toInstant().epochSecond
            } catch (_: Exception) {
                try {
                    val normalized = if (trimmed.contains(" ") && !trimmed.contains("T")) {
                        trimmed.replace(" ", "T") + "Z"
                    } else if (!trimmed.endsWith("Z") && !trimmed.contains("+")) {
                        trimmed + "Z"
                    } else {
                        trimmed
                    }
                    Instant.parse(normalized).epochSecond
                } catch (_: Exception) {
                    try {
                        val localDateTime = LocalDateTime.parse(trimmed, STANDARD_FORMATTER)
                        localDateTime.toInstant(ZoneOffset.UTC).epochSecond
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }
}
