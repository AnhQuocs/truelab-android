package dev.anhquocs.truelab.core.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create leagues table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `leagues` (
                `id` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `shortName` TEXT,
                `logo` TEXT,
                `country` TEXT,
                `category` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_leagues_name` ON `leagues` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_leagues_country` ON `leagues` (`country`)")

        // 2. Create seasons table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `seasons` (
                `id` TEXT NOT NULL,
                `leagueId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `year` INTEGER NOT NULL,
                `isCurrent` INTEGER NOT NULL,
                `startDate` TEXT,
                `endDate` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`leagueId`) REFERENCES `leagues`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_seasons_leagueId` ON `seasons` (`leagueId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_seasons_year` ON `seasons` (`year`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_seasons_leagueId_year` ON `seasons` (`leagueId`, `year`)")

        // 3. Create dataset_metadata table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dataset_metadata` (
                `key` TEXT NOT NULL,
                `lastSyncTimestamp` INTEGER NOT NULL,
                `totalMatches` INTEGER NOT NULL,
                `totalTeams` INTEGER NOT NULL,
                `totalOddsRecords` INTEGER NOT NULL,
                `totalLeagues` INTEGER NOT NULL,
                `totalSeasons` INTEGER NOT NULL,
                `earliestMatchDate` TEXT,
                `latestMatchDate` TEXT,
                `schemaVersion` INTEGER NOT NULL,
                PRIMARY KEY(`key`)
            )
            """.trimIndent()
        )

        // 4. Alter matches table to add leagueId and season
        db.execSQL("ALTER TABLE `matches` ADD COLUMN `leagueId` INTEGER REFERENCES `leagues`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL")
        db.execSQL("ALTER TABLE `matches` ADD COLUMN `season` TEXT DEFAULT NULL")

        // 5. Create new indices on matches
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_leagueId` ON `matches` (`leagueId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_season` ON `matches` (`season`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_matches_leagueId_season` ON `matches` (`leagueId`, `season`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("PRAGMA table_info(`matches`)")
        var hasMinutes = false
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex != -1 && cursor.getString(nameIndex) == "minutes") {
                hasMinutes = true
                break
            }
        }
        cursor.close()
        if (!hasMinutes) {
            db.execSQL("ALTER TABLE `matches` ADD COLUMN `minutes` TEXT DEFAULT NULL")
        }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new odds table with nullable fields and correct types matching OddsEntity
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `odds_new` (
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

        // 2. Copy data from old table to new table (casting changeTime to INTEGER)
        db.execSQL(
            """
            INSERT INTO `odds_new` (
                `id`, `matchId`, `companyId`, `companyName`, `oddsType`,
                `handicap`, `over`, `under`, `homeWin`, `draw`, `awayWin`,
                `changeTime`, `marketPhase`
            )
            SELECT
                `id`, `matchId`, `companyId`, `companyName`, `oddsType`,
                `handicap`, `over`, `under`, `homeWin`, `draw`, `awayWin`,
                CAST(`changeTime` AS INTEGER), `marketPhase`
            FROM `odds`
            """.trimIndent()
        )

        // 3. Drop old table
        db.execSQL("DROP TABLE `odds`")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE `odds_new` RENAME TO `odds`")

        // 5. Recreate indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_odds_matchId` ON `odds` (`matchId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_odds_matchId_companyId_oddsType_changeTime` ON `odds` (`matchId`, `companyId`, `oddsType`, `changeTime`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `matches` ADD COLUMN `isPenalty` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `matches` ADD COLUMN `homePenaltyScore` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `matches` ADD COLUMN `awayPenaltyScore` INTEGER DEFAULT NULL")
    }
}


