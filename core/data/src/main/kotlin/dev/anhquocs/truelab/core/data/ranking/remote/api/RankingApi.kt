package dev.anhquocs.truelab.core.data.ranking.remote.api

import dev.anhquocs.truelab.core.data.ranking.remote.dto.SeasonRankResponse
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface RankingApi {
    @GET("/sport/v1.0/season/{matchId}/ranking")
    suspend fun getSeasonRanking(
        @Path("matchId") matchId: Long
    ): BaseResponse<List<SeasonRankResponse>>
}
