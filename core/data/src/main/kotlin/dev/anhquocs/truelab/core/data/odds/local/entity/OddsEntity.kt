package dev.anhquocs.truelab.core.data.odds.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity

@Entity(
    tableName = "odds",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["matchId"]), // Basic FK index
        Index(value = ["matchId", "companyId", "oddsType", "changeTime"], unique = true) // Crucial for preventing duplicate snapshots during idempotent syncs
    ]
)
data class OddsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchId: Long,
    val companyId: Int,
    val companyName: String,
    val oddsType: String,
    val handicap: Double?,
    val over: Double?,
    val under: Double?,
    val homeWin: Double?,
    val draw: Double?,
    val awayWin: Double?,
    val changeTime: Long,
    val marketPhase: String?
)
