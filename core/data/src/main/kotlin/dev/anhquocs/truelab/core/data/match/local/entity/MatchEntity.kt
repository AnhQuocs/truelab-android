package dev.anhquocs.truelab.core.data.match.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import dev.anhquocs.truelab.core.data.league.local.entity.LeagueEntity
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
        ),
        ForeignKey(
            entity = LeagueEntity::class,
            parentColumns = ["id"],
            childColumns = ["leagueId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["homeTeamId"]),
        Index(value = ["awayTeamId"]),
        Index(value = ["homeTeamId", "awayTeamId"]),
        Index(value = ["startTimeDate"]),
        Index(value = ["status"]),
        Index(value = ["leagueId"]),
        Index(value = ["season"]),
        Index(value = ["leagueId", "season"])
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
    val status: String,
    val leagueId: Int? = null,
    val season: String? = null
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
