package dev.anhquocs.truelab.core.data.odds.local

import androidx.room.Room
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OddsDaoRoomTest {

    private lateinit var db: TrueLabDatabase
    private lateinit var oddsDao: OddsDao

    @Before
    fun createDb() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrueLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        oddsDao = db.oddsDao()

        // Insert required parent records for foreign key constraints
        val team1 = TeamEntity(id = 1, name = "Denmark", logo = null, leagueName = "UNL", eloRating = 1650.0, formScore = 0.7)
        val team2 = TeamEntity(id = 2, name = "Portugal", logo = null, leagueName = "UNL", eloRating = 1800.0, formScore = 0.85)
        db.teamDao().insertTeams(listOf(team1, team2))

        val match = MatchEntity(
            id = 129246L,
            homeTeamId = 1,
            awayTeamId = 2,
            homeScore = null,
            awayScore = null,
            startTimeDate = "2026-10-01 18:45:00",
            status = "1",
            leagueId = null,
            season = null,
            minutes = null
        )
        db.matchDao().insertMatches(listOf(match))
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insert_eu_odds_with_null_handicap_and_over_under_succeeds() {
        val euOdds = OddsEntity(
            matchId = 129246L,
            companyId = 3,
            companyName = "Crown",
            oddsType = "eu",
            handicap = null,
            over = null,
            under = null,
            homeWin = 4.00,
            draw = 3.75,
            awayWin = 1.85,
            changeTime = 1790879871L,
            marketPhase = "instant"
        )

        val rowIds = oddsDao.insertOdds(listOf(euOdds))
        assertEquals(1, rowIds.size)

        val stored = oddsDao.getOddsListForMatch(129246L)
        assertEquals(1, stored.size)
        val record = stored.first()
        assertEquals("eu", record.oddsType)
        assertNull(record.handicap)
        assertNull(record.over)
        assertNull(record.under)
        assertEquals(4.00, record.homeWin!!, 0.001)
        assertEquals(3.75, record.draw!!, 0.001)
        assertEquals(1.85, record.awayWin!!, 0.001)
    }

    @Test
    fun insert_asia_odds_with_null_1x2_succeeds() {
        val asiaOdds = OddsEntity(
            matchId = 129246L,
            companyId = 2,
            companyName = "Bet365",
            oddsType = "asia",
            handicap = 0.75,
            over = null,
            under = null,
            homeWin = null,
            draw = null,
            awayWin = null,
            changeTime = 1790879500L,
            marketPhase = "instant"
        )

        val rowIds = oddsDao.insertOdds(listOf(asiaOdds))
        assertEquals(1, rowIds.size)

        val stored = oddsDao.getOddsListForMatch(129246L)
        assertEquals(1, stored.size)
        val record = stored.first()
        assertEquals("asia", record.oddsType)
        assertEquals(0.75, record.handicap!!, 0.001)
        assertNull(record.homeWin)
        assertNull(record.draw)
        assertNull(record.awayWin)
        assertNull(record.over)
        assertNull(record.under)
    }

    @Test
    fun insert_bs_odds_with_null_1x2_and_handicap_succeeds() {
        val bsOdds = OddsEntity(
            matchId = 129246L,
            companyId = 2,
            companyName = "Bet365",
            oddsType = "bs",
            handicap = null,
            over = 0.95,
            under = 0.85,
            homeWin = null,
            draw = null,
            awayWin = null,
            changeTime = 1790879600L,
            marketPhase = "instant"
        )

        val rowIds = oddsDao.insertOdds(listOf(bsOdds))
        assertEquals(1, rowIds.size)

        val stored = oddsDao.getOddsListForMatch(129246L)
        assertEquals(1, stored.size)
        val record = stored.first()
        assertEquals("bs", record.oddsType)
        assertEquals(0.95, record.over!!, 0.001)
        assertEquals(0.85, record.under!!, 0.001)
        assertNull(record.handicap)
        assertNull(record.homeWin)
        assertNull(record.draw)
        assertNull(record.awayWin)
    }

    @Test
    fun insert_multi_market_odds_batch_all_persist_without_sqlite_constraint_exception() {
        val multiMarketBatch = listOf(
            // EU (17 records representation)
            OddsEntity(
                matchId = 129246L, companyId = 1, companyName = "Macauslot", oddsType = "eu",
                handicap = null, over = null, under = null,
                homeWin = 3.90, draw = 3.60, awayWin = 1.90,
                changeTime = 1790879000L, marketPhase = "instant"
            ),
            OddsEntity(
                matchId = 129246L, companyId = 3, companyName = "Crown", oddsType = "eu",
                handicap = null, over = null, under = null,
                homeWin = 4.00, draw = 3.75, awayWin = 1.85,
                changeTime = 1790879871L, marketPhase = "instant"
            ),
            // ASIA (15 records representation)
            OddsEntity(
                matchId = 129246L, companyId = 1, companyName = "Macauslot", oddsType = "asia",
                handicap = 0.5, over = null, under = null,
                homeWin = null, draw = null, awayWin = null,
                changeTime = 1790879000L, marketPhase = "instant"
            ),
            // BS (16 records representation)
            OddsEntity(
                matchId = 129246L, companyId = 2, companyName = "Bet365", oddsType = "bs",
                handicap = null, over = 1.02, under = 0.82,
                homeWin = null, draw = null, awayWin = null,
                changeTime = 1790879000L, marketPhase = "instant"
            ),
            // CR (6 records representation)
            OddsEntity(
                matchId = 129246L, companyId = 4, companyName = "Ladbrokes", oddsType = "cr",
                handicap = null, over = 9.5, under = null,
                homeWin = null, draw = null, awayWin = null,
                changeTime = 1790879000L, marketPhase = "instant"
            )
        )

        val insertedRowIds = oddsDao.insertOdds(multiMarketBatch)
        assertEquals(5, insertedRowIds.size)

        val storedList = oddsDao.getOddsListForMatch(129246L)
        assertEquals(5, storedList.size)

        val euCount = storedList.count { it.oddsType == "eu" }
        val asiaCount = storedList.count { it.oddsType == "asia" }
        val bsCount = storedList.count { it.oddsType == "bs" }
        val crCount = storedList.count { it.oddsType == "cr" }

        assertEquals(2, euCount)
        assertEquals(1, asiaCount)
        assertEquals(1, bsCount)
        assertEquals(1, crCount)
    }

    @Test
    fun query_latest_pre_match_european_odds_returns_valid_snapshot() = runBlocking {
        val euOdds1 = OddsEntity(
            matchId = 129246L,
            companyId = 3,
            companyName = "Crown",
            oddsType = "eu",
            handicap = null,
            over = null,
            under = null,
            homeWin = 4.00,
            draw = 3.75,
            awayWin = 1.85,
            changeTime = 1790879871L,
            marketPhase = "instant"
        )
        val asiaOdds = OddsEntity(
            matchId = 129246L,
            companyId = 3,
            companyName = "Crown",
            oddsType = "asia",
            handicap = 0.5,
            over = null,
            under = null,
            homeWin = null,
            draw = null,
            awayWin = null,
            changeTime = 1790879871L,
            marketPhase = "instant"
        )

        oddsDao.insertOdds(listOf(euOdds1, asiaOdds))

        val preMatchEuropeanOdds = oddsDao.getLatestPreMatchEuropeanOddsForAllMatches().first()
        assertEquals(1, preMatchEuropeanOdds.size)
        val selected = preMatchEuropeanOdds.first()
        assertEquals(129246L, selected.matchId)
        assertEquals("Crown", selected.companyName)
        assertEquals(4.00, selected.homeWin!!, 0.001)
        assertEquals(3.75, selected.draw!!, 0.001)
        assertEquals(1.85, selected.awayWin!!, 0.001)
    }
}
