package dev.anhquocs.truelab.core.data.prediction.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import dev.anhquocs.truelab.core.data.match.local.entity.MatchEntity

@Entity(
    tableName = "predictions",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PredictionEntity(
    @PrimaryKey
    val matchId: Long,
    val algorithmName: String,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val predictedOutcome: String,
    val confidenceScore: Double
)
