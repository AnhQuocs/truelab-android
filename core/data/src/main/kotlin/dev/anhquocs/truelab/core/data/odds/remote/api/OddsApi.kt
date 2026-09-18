package dev.anhquocs.truelab.core.data.odds.remote.api

import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsHistoryResponse
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OddsApi {
    @GET("/sport/v1.0/matches/{matchId}/odds")
    suspend fun getOdds(
        @Path("matchId") matchId: Long
    ): BaseResponse<List<OddsRecord>>

    @GET("/sport/v1.0/matches/{matchId}/odds-history")
    suspend fun getOddsHistory(
        @Path("matchId") matchId: Long,
        @Query("company_id") companyId: Int? = null,
        @Query("odds_type") oddsType: String? = null
    ): BaseResponse<OddsHistoryResponse>
}
