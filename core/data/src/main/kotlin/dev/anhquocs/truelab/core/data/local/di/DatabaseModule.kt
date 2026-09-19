package dev.anhquocs.truelab.core.data.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.local.dao.MatchDao
import dev.anhquocs.truelab.core.data.odds.local.dao.OddsDao
import dev.anhquocs.truelab.core.data.prediction.local.dao.PredictionDao
import dev.anhquocs.truelab.core.data.ranking.local.dao.RankingDao
import dev.anhquocs.truelab.core.data.team.local.dao.TeamDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTrueLabDatabase(
        @ApplicationContext context: Context
    ): TrueLabDatabase {
        return Room.databaseBuilder(
            context,
            TrueLabDatabase::class.java,
            "truelab_database.db"
        )
        // Fallback to destructive migration is NOT used here 
        // to preserve the huge offline local dataset across versions. 
        // Proper Migrations should be provided when schema changes.
        .build()
    }

    @Provides
    @Singleton
    fun provideTeamDao(database: TrueLabDatabase): TeamDao = database.teamDao()

    @Provides
    @Singleton
    fun provideMatchDao(database: TrueLabDatabase): MatchDao = database.matchDao()

    @Provides
    @Singleton
    fun provideOddsDao(database: TrueLabDatabase): OddsDao = database.oddsDao()

    @Provides
    @Singleton
    fun provideRankingDao(database: TrueLabDatabase): RankingDao = database.rankingDao()

    @Provides
    @Singleton
    fun providePredictionDao(database: TrueLabDatabase): PredictionDao = database.predictionDao()
}
