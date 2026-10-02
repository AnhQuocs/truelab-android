package dev.anhquocs.truelab.core.data.odds.remote

import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OddsRemoteDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `parse multi-market odds response JSON correctly`() {
        val jsonString = """
        {
            "code": 200,
            "message": "success",
            "data": [
                {
                    "company_id": 1,
                    "odds_type": "eu",
                    "home_win": 2.10,
                    "draw": 3.30,
                    "away_win": 3.50,
                    "change_time": 1759340000,
                    "market_phase": "immediate",
                    "company": {
                        "id": 1,
                        "name": "Bet365",
                        "short_name": "B365"
                    }
                },
                {
                    "company_id": 1,
                    "odds_type": "asia",
                    "handicap": -0.5,
                    "home_win": 1.95,
                    "away_win": 1.90,
                    "change_time": 1759340000,
                    "market_phase": "immediate",
                    "company": {
                        "id": 1,
                        "name": "Bet365",
                        "short_name": "B365"
                    }
                },
                {
                    "company_id": 1,
                    "odds_type": "bs",
                    "over": 1.85,
                    "under": 1.95,
                    "change_time": 1759340000,
                    "market_phase": "immediate",
                    "company": {
                        "id": 1,
                        "name": "Bet365",
                        "short_name": "B365"
                    }
                }
            ]
        }
        """.trimIndent()

        val response = json.decodeFromString<BaseResponse<List<OddsRecord>>>(jsonString)

        val data = response.data
        assertNotNull(data)
        assertEquals(3, data.size)

        val eu = data.find { it.oddsType == "eu" }!!
        assertEquals(1, eu.companyId)
        assertEquals(2.10, eu.homeWin)
        assertEquals(3.30, eu.draw)
        assertEquals(3.50, eu.awayWin)
        assertEquals("Bet365", eu.company?.name)

        val asia = data.find { it.oddsType == "asia" }!!
        assertEquals(-0.5, asia.handicap)
        assertEquals(1.95, asia.homeWin)
        assertEquals(1.90, asia.awayWin)

        val bs = data.find { it.oddsType == "bs" }!!
        assertEquals(1.85, bs.over)
        assertEquals(1.95, bs.under)
    }
}
