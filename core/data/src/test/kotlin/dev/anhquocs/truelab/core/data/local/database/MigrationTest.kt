package dev.anhquocs.truelab.core.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MigrationTest {

    private lateinit var db: TestSupportSQLiteDatabase

    @Before
    fun setupV1Database() {
        db = TestSupportSQLiteDatabase()

        // 1. Setup V1 Schema matching 1.json
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `teams` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `logo` TEXT,
                `leagueName` TEXT,
                `eloRating` REAL NOT NULL,
                `formScore` REAL NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `matches` (
                `id` INTEGER NOT NULL,
                `homeTeamId` INTEGER NOT NULL,
                `awayTeamId` INTEGER NOT NULL,
                `homeScore` INTEGER,
                `awayScore` INTEGER,
                `startTimeDate` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`homeTeamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`awayTeamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_homeTeamId` ON `matches` (`homeTeamId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_awayTeamId` ON `matches` (`awayTeamId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_homeTeamId_awayTeamId` ON `matches` (`homeTeamId`, `awayTeamId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_startTimeDate` ON `matches` (`startTimeDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_status` ON `matches` (`status`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `odds` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `matchId` INTEGER NOT NULL,
                `companyId` INTEGER NOT NULL,
                `companyName` TEXT NOT NULL,
                `oddsType` TEXT NOT NULL,
                `handicap` REAL,
                `over` REAL,
                `under` REAL,
                `homeWin` REAL,
                `draw` REAL,
                `awayWin` REAL,
                `changeTime` INTEGER NOT NULL,
                `marketPhase` TEXT,
                FOREIGN KEY(`matchId`) REFERENCES `matches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `season_rankings` (
                `matchId` INTEGER NOT NULL,
                `teamId` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `won` INTEGER NOT NULL,
                `draw` INTEGER NOT NULL,
                `loss` INTEGER NOT NULL,
                `goalDiff` INTEGER NOT NULL,
                `recentlyStr` TEXT NOT NULL,
                PRIMARY KEY(`matchId`, `teamId`),
                FOREIGN KEY(`matchId`) REFERENCES `matches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`teamId`) REFERENCES `teams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `predictions` (
                `matchId` INTEGER NOT NULL,
                `algorithmName` TEXT NOT NULL,
                `homeWinProb` REAL NOT NULL,
                `drawProb` REAL NOT NULL,
                `awayWinProb` REAL NOT NULL,
                `predictedOutcome` TEXT NOT NULL,
                `confidenceScore` REAL NOT NULL,
                PRIMARY KEY(`matchId`),
                FOREIGN KEY(`matchId`) REFERENCES `matches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // 2. Insert representative legacy data into V1 database
        db.insertRow("teams", mapOf(
            "id" to 1,
            "name" to "Arsenal",
            "logo" to "https://example.com/arsenal.png",
            "leagueName" to "Premier League",
            "eloRating" to 1750.0,
            "formScore" to 0.85
        ))
        db.insertRow("teams", mapOf(
            "id" to 2,
            "name" to "Chelsea",
            "logo" to "https://example.com/chelsea.png",
            "leagueName" to "Premier League",
            "eloRating" to 1620.0,
            "formScore" to 0.60
        ))

        db.insertRow("matches", mapOf(
            "id" to 1001L,
            "homeTeamId" to 1,
            "awayTeamId" to 2,
            "homeScore" to 2,
            "awayScore" to 1,
            "startTimeDate" to "2024-05-10 15:00:00",
            "status" to "8"
        ))
    }

    @Test
    fun migration_1_2_executes_all_migration_steps_successfully() {
        val initialSqlCount = db.executedSqls.size
        MIGRATION_1_2.migrate(db)
        val migratedSqlCount = db.executedSqls.size

        assertTrue("Migration should execute DDL statements", migratedSqlCount > initialSqlCount)
    }

    @Test
    fun migration_1_2_creates_leagues_table_and_indexes() {
        MIGRATION_1_2.migrate(db)

        val leaguesTable = db.tables["leagues"]
        assertNotNull("leagues table should exist after migration", leaguesTable)
        assertEquals("INTEGER", leaguesTable?.columns?.get("id"))
        assertEquals("TEXT", leaguesTable?.columns?.get("name"))
        assertEquals("TEXT", leaguesTable?.columns?.get("shortName"))
        assertEquals("TEXT", leaguesTable?.columns?.get("logo"))
        assertEquals("TEXT", leaguesTable?.columns?.get("country"))
        assertEquals("TEXT", leaguesTable?.columns?.get("category"))

        assertNotNull("index_leagues_name should exist", db.indexes["index_leagues_name"])
        assertEquals("leagues", db.indexes["index_leagues_name"]?.tableName)
        assertEquals(listOf("name"), db.indexes["index_leagues_name"]?.columns)

        assertNotNull("index_leagues_country should exist", db.indexes["index_leagues_country"])
        assertEquals("leagues", db.indexes["index_leagues_country"]?.tableName)
        assertEquals(listOf("country"), db.indexes["index_leagues_country"]?.columns)
    }

    @Test
    fun migration_1_2_creates_seasons_table_and_indexes_and_foreign_key() {
        MIGRATION_1_2.migrate(db)

        val seasonsTable = db.tables["seasons"]
        assertNotNull("seasons table should exist after migration", seasonsTable)
        assertEquals("TEXT", seasonsTable?.columns?.get("id"))
        assertEquals("INTEGER", seasonsTable?.columns?.get("leagueId"))
        assertEquals("TEXT", seasonsTable?.columns?.get("name"))
        assertEquals("INTEGER", seasonsTable?.columns?.get("year"))
        assertEquals("INTEGER", seasonsTable?.columns?.get("isCurrent"))
        assertEquals("TEXT", seasonsTable?.columns?.get("startDate"))
        assertEquals("TEXT", seasonsTable?.columns?.get("endDate"))

        assertTrue("seasons table should have FK to leagues",
            seasonsTable?.foreignKeys?.any { it.contains("leagues", ignoreCase = true) && it.contains("CASCADE", ignoreCase = true) } == true
        )

        assertNotNull("index_seasons_leagueId should exist", db.indexes["index_seasons_leagueId"])
        assertNotNull("index_seasons_year should exist", db.indexes["index_seasons_year"])
        val uniqueIndex = db.indexes["index_seasons_leagueId_year"]
        assertNotNull("index_seasons_leagueId_year should exist", uniqueIndex)
        assertTrue("index_seasons_leagueId_year should be UNIQUE", uniqueIndex?.isUnique == true)
        assertEquals(listOf("leagueId", "year"), uniqueIndex?.columns)
    }

    @Test
    fun migration_1_2_creates_dataset_metadata_table() {
        MIGRATION_1_2.migrate(db)

        val metadataTable = db.tables["dataset_metadata"]
        assertNotNull("dataset_metadata table should exist after migration", metadataTable)
        assertEquals("TEXT", metadataTable?.columns?.get("key"))
        assertEquals("INTEGER", metadataTable?.columns?.get("lastSyncTimestamp"))
        assertEquals("INTEGER", metadataTable?.columns?.get("totalMatches"))
        assertEquals("INTEGER", metadataTable?.columns?.get("totalTeams"))
        assertEquals("INTEGER", metadataTable?.columns?.get("totalOddsRecords"))
        assertEquals("INTEGER", metadataTable?.columns?.get("totalLeagues"))
        assertEquals("INTEGER", metadataTable?.columns?.get("totalSeasons"))
        assertEquals("TEXT", metadataTable?.columns?.get("earliestMatchDate"))
        assertEquals("TEXT", metadataTable?.columns?.get("latestMatchDate"))
        assertEquals("INTEGER", metadataTable?.columns?.get("schemaVersion"))
    }

    @Test
    fun migration_1_2_alters_matches_table_with_leagueId_and_season() {
        MIGRATION_1_2.migrate(db)

        val matchesTable = db.tables["matches"]
        assertNotNull(matchesTable)
        assertEquals("INTEGER", matchesTable?.columns?.get("leagueId"))
        assertEquals("TEXT", matchesTable?.columns?.get("season"))

        assertNotNull("index_matches_leagueId should exist", db.indexes["index_matches_leagueId"])
        assertNotNull("index_matches_season should exist", db.indexes["index_matches_season"])
        assertNotNull("index_matches_leagueId_season should exist", db.indexes["index_matches_leagueId_season"])
    }

    @Test
    fun migration_1_2_preserves_existing_team_data() {
        MIGRATION_1_2.migrate(db)

        val teams = db.rows["teams"]
        assertNotNull(teams)
        assertEquals(2, teams?.size)

        val arsenal = teams?.first { it["id"] == 1 }
        assertEquals("Arsenal", arsenal?.get("name"))
        assertEquals("https://example.com/arsenal.png", arsenal?.get("logo"))
        assertEquals("Premier League", arsenal?.get("leagueName"))
        assertEquals(1750.0, arsenal?.get("eloRating"))
        assertEquals(0.85, arsenal?.get("formScore"))
    }

    @Test
    fun migration_1_2_preserves_existing_match_data_with_null_new_fields() {
        MIGRATION_1_2.migrate(db)

        val matches = db.rows["matches"]
        assertNotNull(matches)
        assertEquals(1, matches?.size)

        val match = matches?.first()
        assertEquals(1001L, match?.get("id"))
        assertEquals(1, match?.get("homeTeamId"))
        assertEquals(2, match?.get("awayTeamId"))
        assertEquals(2, match?.get("homeScore"))
        assertEquals(1, match?.get("awayScore"))
        assertEquals("2024-05-10 15:00:00", match?.get("startTimeDate"))
        assertEquals("8", match?.get("status"))
        assertNull("Migrated legacy match should have null leagueId", match?.get("leagueId"))
        assertNull("Migrated legacy match should have null season", match?.get("season"))
    }

    @Test
    fun migration_1_2_allows_new_league_season_match_relationship() {
        MIGRATION_1_2.migrate(db)

        // 1. Insert League
        db.insertRow("leagues", mapOf(
            "id" to 39,
            "name" to "Premier League",
            "shortName" to "EPL",
            "country" to "England"
        ))

        // 2. Insert Season with FK to League
        db.insertRow("seasons", mapOf(
            "id" to "39_2024",
            "leagueId" to 39,
            "name" to "2023-2024",
            "year" to 2024,
            "isCurrent" to 1
        ))

        // 3. Insert new Match referencing leagueId and season
        db.insertRow("matches", mapOf(
            "id" to 2001L,
            "homeTeamId" to 1,
            "awayTeamId" to 2,
            "homeScore" to 3,
            "awayScore" to 0,
            "startTimeDate" to "2024-05-19 16:00:00",
            "status" to "8",
            "leagueId" to 39,
            "season" to "2023-2024"
        ))

        val leagues = db.rows["leagues"]
        val seasons = db.rows["seasons"]
        val matches = db.rows["matches"]

        assertEquals(1, leagues?.size)
        assertEquals(1, seasons?.size)
        assertEquals(2, matches?.size)

        val newMatch = matches?.find { it["id"] == 2001L }
        assertNotNull(newMatch)
        assertEquals(39, newMatch?.get("leagueId"))
        assertEquals("2023-2024", newMatch?.get("season"))
    }
}
