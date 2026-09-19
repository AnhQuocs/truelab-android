package dev.anhquocs.truelab.core.data.ranking.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity

@Entity(
    tableName = "season_rankings",
    primaryKeys = ["matchId", "teamId"], // Composite PK: A team's ranking snapshot for a specific match
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["teamId"]),
        Index(value = ["matchId"]),
        Index(value = ["position"])
    ]
)
data class SeasonRankingEntity(
    val matchId: Long,
    val teamId: Int,
    val position: Int,
    val won: Int,
    val draw: Int,
    val loss: Int,
    val goalDiff: Int,
    val recentlyStr: String 
)
