package dev.anhquocs.truelab.core.domain.prediction.model

data class PredictionResult(
    val matchId: Long,
    val algorithmName: String,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val predictedOutcome: String,
    val confidenceScore: Double
)

