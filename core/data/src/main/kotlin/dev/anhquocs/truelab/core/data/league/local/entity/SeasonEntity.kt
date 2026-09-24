package dev.anhquocs.truelab.core.data.league.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "seasons",
    foreignKeys = [
        ForeignKey(
            entity = LeagueEntity::class,
            parentColumns = ["id"],
            childColumns = ["leagueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["leagueId"]),
        Index(value = ["year"]),
        Index(value = ["leagueId", "year"], unique = true)
    ]
)
data class SeasonEntity(
    @PrimaryKey
    val id: String,
    val leagueId: Int,
    val name: String,
    val year: Int,
    val isCurrent: Boolean = false,
    val startDate: String? = null,
    val endDate: String? = null
)
