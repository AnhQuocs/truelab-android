package dev.anhquocs.truelab.core.data.odds.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OddsRecord(
    @SerialName("company_id") val companyId: Int,
    @SerialName("odds_type") val oddsType: String,
    @SerialName("handicap") val handicap: Double? = null,
    @SerialName("over") val over: Double? = null,
    @SerialName("under") val under: Double? = null,
    @SerialName("home_win") val homeWin: Double? = null,
    @SerialName("draw") val draw: Double? = null,
    @SerialName("away_win") val awayWin: Double? = null,
    @SerialName("change_time") val changeTime: Long,
    @SerialName("market_phase") val marketPhase: String? = null,
    @SerialName("company") val company: CompanyInfo? = null
)

@Serializable
data class CompanyInfo(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("short_name") val shortName: String
)

@Serializable
data class OddsHistoryResponse(
    @SerialName("data") val data: List<OddsRecord>
)
