package dev.anhquocs.truelab.core.data.league.local

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteStatement
import androidx.room.Room
import dev.anhquocs.truelab.core.data.league.local.dao.LeagueDao
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow.directlyOn

@Implements(SQLiteStatement::class)
class ShadowCustomSQLiteStatement {
    @RealObject
    private lateinit var realStatement: SQLiteStatement

    @Implementation
    fun executeInsert(): Long {
        try {
            return directlyOn(realStatement, SQLiteStatement::class.java, "executeInsert")
        } catch (e: SQLiteConstraintException) {
            val causeMsg = e.cause?.message ?: ""
            if (causeMsg.contains("UNIQUE", ignoreCase = true) || causeMsg.contains("1555") || causeMsg.contains("PRIMARY KEY", ignoreCase = true)) {
                val enhanced = SQLiteConstraintException("UNIQUE constraint failed: $causeMsg")
                enhanced.initCause(e.cause)
                throw enhanced
            }
            throw e
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowCustomSQLiteStatement::class])
class LeagueDaoRoomTest {

    private lateinit var db: TrueLabDatabase
    private lateinit var leagueDao: LeagueDao
    private lateinit var matchDao: MatchDao
    private lateinit var teamDao: TeamDao

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, TrueLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        leagueDao = db.leagueDao()
        matchDao = db.matchDao()
        teamDao = db.teamDao()

        // Insert required parent teams for foreign key constraints
        val vietnam = TeamEntity(id = 101, name = "Vietnam", logo = null, leagueName = "ASEAN", eloRating = 1500.0, formScore = 0.5)
        val pakistan = TeamEntity(id = 102, name = "Pakistan", logo = null, leagueName = "ASEAN", eloRating = 1300.0, formScore = 0.3)
        val thailand = TeamEntity(id = 103, name = "Thailand", logo = null, leagueName = "ASEAN", eloRating = 1520.0, formScore = 0.6)
        val philippines = TeamEntity(id = 104, name = "Philippines", logo = null, leagueName = "ASEAN", eloRating = 1350.0, formScore = 0.4)
        teamDao.insertTeams(listOf(vietnam, pakistan, thailand, philippines))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_league_preserves_child_match_foreign_key_reference() = runBlocking {
        // 1. Insert League(id = 2239057)
        val initialLeague = LeagueEntity(
            id = 2239057,
            name = "FIFA ASEAN Cup",
            logo = "https://example.com/asean.png"
        )
        leagueDao.insertLeagues(listOf(initialLeague))

        // 2. Insert Match(id = 908568, leagueId = 2239057)
        val match = MatchEntity(
            id = 908568L,
            homeTeamId = 101,
            awayTeamId = 102,
            homeScore = 4,
            awayScore = 2,
            startTimeDate = "2026-10-02 19:30:00",
            status = "8",
            leagueId = 2239057,
            season = "2026",
            minutes = null
        )
        matchDao.upsertMatches(listOf(match))

        // Verify match has leagueId set
        val matchBefore = matchDao.getMatchById(908568L).first()
        assertNotNull(matchBefore)
        assertEquals(2239057, matchBefore!!.match.leagueId)
        assertEquals("FIFA ASEAN Cup", matchBefore.league?.name)

        // 3. Upsert lại League(id = 2239057) with updated info (simulating sync of Page 2 containing same league)
        val updatedLeague = LeagueEntity(
            id = 2239057,
            name = "FIFA ASEAN Cup Updated",
            logo = "https://example.com/asean_updated.png"
        )
        leagueDao.insertLeagues(listOf(updatedLeague))

        // 4. Assert Match.leagueId still == 2239057 and league name is updated
        val matchAfter = matchDao.getMatchById(908568L).first()
        assertNotNull(matchAfter)
        assertEquals(2239057, matchAfter!!.match.leagueId)
        assertEquals("FIFA ASEAN Cup Updated", matchAfter.league?.name)
        assertEquals("https://example.com/asean_updated.png", matchAfter.league?.logo)
    }

    @Test
    fun multiple_matches_in_same_league_preserve_leagueId_across_multiple_upserts() = runBlocking {
        // 1. Insert initial League
        val league = LeagueEntity(id = 2239057, name = "FIFA ASEAN Cup", logo = "logo_v1")
        leagueDao.insertLeagues(listOf(league))

        // 2. Insert multiple matches belonging to this league
        val match1 = MatchEntity(
            id = 908568L,
            homeTeamId = 101,
            awayTeamId = 102,
            homeScore = 4,
            awayScore = 2,
            startTimeDate = "2026-10-02 19:30:00",
            status = "8",
            leagueId = 2239057,
            season = "2026",
            minutes = null
        )
        val match2 = MatchEntity(
            id = 908567L,
            homeTeamId = 103,
            awayTeamId = 104,
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-10-02 20:00:00",
            status = "8",
            leagueId = 2239057,
            season = "2026",
            minutes = null
        )
        matchDao.upsertMatches(listOf(match1, match2))

        // 3. Perform repeated upserts for the same league (simulating multiple pages or repeat syncs)
        for (iteration in 1..5) {
            val repeatLeague = LeagueEntity(
                id = 2239057,
                name = "FIFA ASEAN Cup v$iteration",
                logo = "logo_v$iteration"
            )
            leagueDao.insertLeagues(listOf(repeatLeague))

            // Verify both matches still hold the leagueId after each upsert
            val retrieved1 = matchDao.getMatchById(908568L).first()
            val retrieved2 = matchDao.getMatchById(908567L).first()

            assertNotNull(retrieved1)
            assertNotNull(retrieved2)
            assertEquals(2239057, retrieved1!!.match.leagueId)
            assertEquals(2239057, retrieved2!!.match.leagueId)
            assertEquals("FIFA ASEAN Cup v$iteration", retrieved1.league?.name)
            assertEquals("FIFA ASEAN Cup v$iteration", retrieved2.league?.name)
        }
    }

    @Test
    fun match_with_null_leagueId_can_be_re_hydrated_via_match_upsert() = runBlocking {
        // 1. Insert league
        val league = LeagueEntity(id = 2239057, name = "FIFA ASEAN Cup", logo = null)
        leagueDao.insertLeagues(listOf(league))

        // 2. Insert match initially with null leagueId
        val matchWithNullLeague = MatchEntity(
            id = 908568L,
            homeTeamId = 101,
            awayTeamId = 102,
            homeScore = 4,
            awayScore = 2,
            startTimeDate = "2026-10-02 19:30:00",
            status = "8",
            leagueId = null,
            season = "2026",
            minutes = null
        )
        matchDao.upsertMatches(listOf(matchWithNullLeague))

        val retrievedBefore = matchDao.getMatchById(908568L).first()
        assertNotNull(retrievedBefore)
        assertEquals(null, retrievedBefore!!.match.leagueId)
        assertEquals(null, retrievedBefore.league)

        // 3. Upsert match with resolved leagueId = 2239057
        val matchWithHydratedLeague = matchWithNullLeague.copy(leagueId = 2239057)
        matchDao.upsertMatches(listOf(matchWithHydratedLeague))

        // 4. Verify match is now hydrated with leagueId = 2239057
        val retrievedAfter = matchDao.getMatchById(908568L).first()
        assertNotNull(retrievedAfter)
        assertEquals(2239057, retrievedAfter!!.match.leagueId)
        assertEquals("FIFA ASEAN Cup", retrievedAfter.league?.name)
    }
}
