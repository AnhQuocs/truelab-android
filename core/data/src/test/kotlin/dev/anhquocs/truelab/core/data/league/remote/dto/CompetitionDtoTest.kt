package dev.anhquocs.truelab.core.data.league.remote.dto

import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CompetitionDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun competitionListResponse_parsesCorrectly() {
        val rawJson = """
            {
              "data": {
                "data": [
                  {
                    "id": 1515,
                    "name": "FIFA World Cup",
                    "short_name": "World Cup",
                    "slug": "fifa-world-cup",
                    "logo": "https://example.com/logo.png",
                    "category_id": 1,
                    "country_id": 0,
                    "cur_season_id": 28277
                  }
                ],
                "meta": {
                  "current_page": 1,
                  "last_page": 10,
                  "per_page": 50,
                  "total": 500
                }
              },
              "message": "success"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<BaseResponse<CompetitionListResponseBase>>(rawJson)

        assertEquals("success", parsed.message)
        assertEquals(1, parsed.data.data.size)
        val comp = parsed.data.data[0]
        assertEquals(1515, comp.id)
        assertEquals("FIFA World Cup", comp.name)
        assertEquals("World Cup", comp.shortName)
        assertEquals(28277L, comp.curSeasonId)

        assertNotNull(parsed.data.meta)
        assertEquals(1, parsed.data.meta?.currentPage)
        assertEquals(10, parsed.data.meta?.lastPage)
        assertEquals(50, parsed.data.meta?.perPage)
        assertEquals(500, parsed.data.meta?.total)
    }

    @Test
    fun seasonResponse_parsesCorrectly() {
        val rawJson = """
            {
              "data": [
                {
                  "id": 28277,
                  "competition_id": 1515,
                  "year": "2026",
                  "has_table": 1,
                  "is_current": 1,
                  "start_time": 1781204400,
                  "end_time": 1784487600
                }
              ],
              "message": "success"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<BaseResponse<List<SeasonDto>>>(rawJson)

        assertEquals("success", parsed.message)
        assertEquals(1, parsed.data.size)
        val season = parsed.data[0]
        assertEquals(28277L, season.id)
        assertEquals(1515L, season.competitionId)
        assertEquals("2026", season.year)
        assertEquals(1, season.isCurrent)
        assertEquals(1781204400L, season.startTime)
        assertEquals(1784487600L, season.endTime)
    }
}
