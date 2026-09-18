package dev.anhquocs.truelab.core.data.match.remote.api

import dev.anhquocs.truelab.core.data.match.remote.dto.MatchInfoDetailResponseBase
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MatchApi {
    @GET("/sport/v1.0/matches")
    suspend fun getMatches(
        @Query("date") date: String,
        @Query("status") status: Int = 8,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("sort") sort: String = "time_asc"
    ): BaseResponse<MatchInfoDetailResponseBase>
}
