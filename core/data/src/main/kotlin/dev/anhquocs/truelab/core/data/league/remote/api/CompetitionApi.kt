package dev.anhquocs.truelab.core.data.league.remote.api

import dev.anhquocs.truelab.core.data.league.remote.dto.CompetitionListResponseBase
import dev.anhquocs.truelab.core.data.league.remote.dto.SeasonDto
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CompetitionApi {

    @GET("/sport/v1.0/competitions/list/")
    suspend fun getCompetitionsList(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): BaseResponse<CompetitionListResponseBase>

    @GET("/sport/v1.0/competitions/{competition_id}/seasons")
    suspend fun getCompetitionSeasons(
        @Path("competition_id") competitionId: Long
    ): BaseResponse<List<SeasonDto>>
}
