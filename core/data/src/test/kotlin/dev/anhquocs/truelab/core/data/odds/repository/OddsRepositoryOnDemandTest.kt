package dev.anhquocs.truelab.core.data.odds.repository

import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import dev.anhquocs.truelab.core.data.odds.remote.api.OddsApi
import dev.anhquocs.truelab.core.data.odds.remote.dto.CompanyInfo
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsHistoryResponse
import dev.anhquocs.truelab.core.data.odds.remote.dto.OddsRecord
import dev.anhquocs.truelab.core.data.remote.dto.BaseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OddsRepositoryOnDemandTest {

    private lateinit var fakeDao: FakeOddsDao
    private lateinit var fakeApi: FakeOddsApi
    private lateinit var repository: OddsRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeOddsDao()
        fakeApi = FakeOddsApi()
        repository = OddsRepositoryImpl(fakeDao, fakeApi)
    }

    @Test
    fun `fetchAndCacheOddsForMatch - when cache is missing, fetches API and saves to Room`() = runTest {
        val matchId = 100L
        fakeApi.stubbedOdds = listOf(
            OddsRecord(
                companyId = 1,
                oddsType = "eu",
                homeWin = 2.10,
                draw = 3.30,
                awayWin = 3.50,
                changeTime = 1759340000,
                marketPhase = "immediate",
                company = CompanyInfo(1, "Bet365", "B365")
            ),
            OddsRecord(
                companyId = 1,
                oddsType = "asia",
                handicap = -0.5,
                homeWin = 1.95,
                awayWin = 1.90,
                changeTime = 1759340000,
                marketPhase = "immediate"
            )
        )

        val result = repository.fetchAndCacheOddsForMatch(matchId)
        assertTrue(result.isSuccess)
        assertEquals(1, fakeApi.callCount)
        assertEquals(2, fakeDao.storedOdds.size)

        val matchOdds = repository.getMatchOdds(matchId).first()
        assertEquals(2, matchOdds.oddsList.size)
        assertEquals("eu", matchOdds.oddsList[0].oddsType)
    }

    @Test
    fun `fetchAndCacheOddsForMatch - when valid pre-match cache exists, does not call API`() = runTest {
        val matchId = 200L
        fakeDao.storedOdds.add(
            OddsEntity(
                id = 1,
                matchId = matchId,
                companyId = 1,
                companyName = "Bet365",
                oddsType = "eu",
                handicap = null,
                over = null,
                under = null,
                homeWin = 2.10,
                draw = 3.30,
                awayWin = 3.50,
                changeTime = 1759340000,
                marketPhase = "immediate"
            )
        )

        val result = repository.fetchAndCacheOddsForMatch(matchId)
        assertTrue(result.isSuccess)
        assertEquals(0, fakeApi.callCount) // Reused local cache
    }

    @Test
    fun `fetchAndCacheOddsForMatch - API failure returns Result failure safely without throwing`() = runTest {
        val matchId = 300L
        fakeApi.shouldThrow = true

        val result = repository.fetchAndCacheOddsForMatch(matchId)
        assertTrue(result.isFailure)
        assertEquals(1, fakeApi.callCount)
        assertEquals(0, fakeDao.storedOdds.size)
    }

    private class FakeDaoOddsFlow(val dao: FakeOddsDao, val matchId: Long)

    private class FakeOddsDao : OddsDao {
        val storedOdds = mutableListOf<OddsEntity>()
        val flow = MutableStateFlow<List<OddsEntity>>(emptyList())

        override fun insertOdds(odds: List<OddsEntity>): LongArray {
            storedOdds.addAll(odds)
            flow.value = storedOdds.toList()
            return odds.map { it.id }.toLongArray()
        }

        override fun getOddsHistory(matchId: Long, companyId: Int?, oddsType: String?): Flow<List<OddsEntity>> {
            return flow
        }

        override fun getOddsForMatch(matchId: Long): Flow<List<OddsEntity>> {
            return flow
        }

        override fun getOddsListForMatch(matchId: Long): List<OddsEntity> {
            return storedOdds.filter { it.matchId == matchId }
        }

        override fun getLatestOddsForMatch(matchId: Long): Flow<List<OddsEntity>> {
            return flow
        }

        override fun getLatestPreMatchEuropeanOddsForAllMatches(): Flow<List<OddsEntity>> {
            return flow
        }
    }

    private class FakeOddsApi : OddsApi {
        var callCount = 0
        var stubbedOdds: List<OddsRecord> = emptyList()
        var shouldThrow = false

        override suspend fun getOdds(matchId: Long): BaseResponse<List<OddsRecord>> {
            callCount++
            if (shouldThrow) {
                throw RuntimeException("Network timeout")
            }
            return BaseResponse(
                statusCode = 200,
                message = "success",
                data = stubbedOdds
            )
        }

        override suspend fun getOddsHistory(
            matchId: Long,
            companyId: Int?,
            oddsType: String?
        ): BaseResponse<OddsHistoryResponse> {
            return BaseResponse(
                statusCode = 200,
                message = "success",
                data = OddsHistoryResponse(stubbedOdds)
            )
        }
    }
}
