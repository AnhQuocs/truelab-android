package dev.anhquocs.truelab.core.data.league.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "leagues",
    indices = [
        Index(value = ["name"]),
        Index(value = ["country"])
    ]
)
data class LeagueEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val shortName: String? = null,
    val logo: String? = null,
    val country: String? = null,
    val category: String? = null
)
