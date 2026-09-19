package dev.anhquocs.truelab.core.data.match.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import dev.anhquocs.truelab.core.data.team.local.entity.TeamEntity

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["homeTeamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["awayTeamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["homeTeamId"]),
        Index(value = ["awayTeamId"]),
        Index(value = ["homeTeamId", "awayTeamId"]), 
        Index(value = ["startTimeDate"]), 
        Index(value = ["status"]) 
    ]
)
data class MatchEntity(
    @PrimaryKey
    val id: Long,
    val homeTeamId: Int,
    val awayTeamId: Int,
    val homeScore: Int?,
    val awayScore: Int?,
    val startTimeDate: String,
    val status: String
)

data class MatchWithTeams(
    @Embedded val match: MatchEntity,
    
    @Relation(
        parentColumn = "homeTeamId",
        entityColumn = "id"
    )
    val homeTeam: TeamEntity,
    
    @Relation(
        parentColumn = "awayTeamId",
        entityColumn = "id"
    )
    val awayTeam: TeamEntity
)
