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
