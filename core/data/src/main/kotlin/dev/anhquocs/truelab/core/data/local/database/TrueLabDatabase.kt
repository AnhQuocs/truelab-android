package dev.anhquocs.truelab.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.odds.local.entity.OddsEntity
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.data.prediction.local.entity.PredictionEntity
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.ranking.local.entity.SeasonRankingEntity
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity

@Database(
    entities = [
        TeamEntity::class,
        MatchEntity::class,
        OddsEntity::class,
        SeasonRankingEntity::class,
        PredictionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class TrueLabDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun matchDao(): MatchDao
    abstract fun oddsDao(): OddsDao
    abstract fun rankingDao(): RankingDao
    abstract fun predictionDao(): PredictionDao
}
