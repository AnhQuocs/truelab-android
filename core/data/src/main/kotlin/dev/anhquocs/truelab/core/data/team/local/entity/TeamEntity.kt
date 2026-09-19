package dev.anhquocs.truelab.core.data.team.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val logo: String?,
    val leagueName: String?,
    val eloRating: Double = 1500.0,
    val formScore: Double = 0.0
)
