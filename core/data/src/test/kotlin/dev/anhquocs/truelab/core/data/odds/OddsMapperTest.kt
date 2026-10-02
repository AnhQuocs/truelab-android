package dev.anhquocs.truelab.core.data.odds

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toEntity
import dev.anhquocs.truelab.core.data.odds.remote.dto.CompanyInfo
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OddsMapperTest {

    @Test
    fun `map multi-market DTO to Entity and Domain correctly`() {
        val euRecord = OddsRecord(
            companyId = 10,
            oddsType = "eu",
            homeWin = 2.25,
            draw = 3.20,
            awayWin = 3.10,
            changeTime = 1759340000,
            marketPhase = "immediate",
            company = CompanyInfo(id = 10, name = "Crown", shortName = "CRW")
        )

        val entity = euRecord.toEntity(matchId = 129243L)
        assertEquals(129243L, entity.matchId)
        assertEquals(10, entity.companyId)
        assertEquals("Crown", entity.companyName)
        assertEquals("eu", entity.oddsType)
        assertEquals(2.25, entity.homeWin)
        assertEquals(3.20, entity.draw)
        assertEquals(3.10, entity.awayWin)
        assertEquals("immediate", entity.marketPhase)

        val domain = entity.toDomain()
        assertEquals(10, domain.companyId)
        assertEquals("Crown", domain.companyName)
        assertEquals("eu", domain.oddsType)
        assertEquals(2.25, domain.homeWin)
        assertEquals(3.20, domain.draw)
        assertEquals(3.10, domain.awayWin)

        val impliedProb = domain.calculateImpliedProbability()
        assertNotNull(impliedProb)
        val sumProb = impliedProb!!.homeProb + impliedProb.drawProb + impliedProb.awayProb
        assertEquals(1.0, sumProb, 0.0001)
    }

    @Test
    fun `map asian handicap and over under DTOs correctly`() {
        val ahRecord = OddsRecord(
            companyId = 1,
            oddsType = "asia",
            handicap = -0.25,
            homeWin = 1.90,
            awayWin = 1.98,
            changeTime = 1759340000,
            marketPhase = "initial",
            company = CompanyInfo(id = 1, name = "Bet365", shortName = "B365")
        )
        val ahEntity = ahRecord.toEntity(matchId = 129243L)
        assertEquals(-0.25, ahEntity.handicap)
        assertEquals("asia", ahEntity.oddsType)

        val ouRecord = OddsRecord(
            companyId = 1,
            oddsType = "bs",
            over = 1.92,
            under = 1.88,
            changeTime = 1759340000,
            marketPhase = "initial",
            company = CompanyInfo(id = 1, name = "Bet365", shortName = "B365")
        )
        val ouEntity = ouRecord.toEntity(matchId = 129243L)
        assertEquals(1.92, ouEntity.over)
        assertEquals(1.88, ouEntity.under)
        assertEquals("bs", ouEntity.oddsType)
    }
}
