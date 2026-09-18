package dev.anhquocs.truelab.core.domain.prediction.model

data class PredictionResult(
    val matchId: Long,
    val algorithmName: String,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val predictedOutcome: String,
    val confidenceScore: Double
) {
    companion object {
        fun computeWeightedScoring(
            matchId: Long,
            homeFormScore: Double,
            awayFormScore: Double,
            homeElo: Double,
            awayElo: Double,
            oddsHomeProb: Double = 0.4,
            oddsDrawProb: Double = 0.3,
            oddsAwayProb: Double = 0.3,
            homeAdvantageWeight: Double = 0.10
        ): PredictionResult {
            val normHomeForm = homeFormScore / 100.0
            val normAwayForm = awayFormScore / 100.0

            val eloDiff = homeElo - awayElo
            val eloHomeProb = 1.0 / (1.0 + Math.pow(10.0, -eloDiff / 400.0))
            val eloAwayProb = 1.0 - eloHomeProb

            val rawHome = (normHomeForm * 0.25) + (eloHomeProb * 0.20) + (oddsHomeProb * 0.20) + (homeAdvantageWeight * 0.10) + 0.10
            val rawAway = (normAwayForm * 0.25) + (eloAwayProb * 0.20) + (oddsAwayProb * 0.20) + 0.05
            val rawDraw = (oddsDrawProb * 0.20) + 0.10

            val total = rawHome + rawAway + rawDraw
            val hProb = rawHome / total
            val dProb = rawDraw / total
            val aProb = rawAway / total

            val outcome = when {
                hProb >= dProb && hProb >= aProb -> "HOME_WIN"
                aProb >= hProb && aProb >= dProb -> "AWAY_WIN"
                else -> "DRAW"
            }

            val confidence = maxOf(hProb, dProb, aProb)

            return PredictionResult(
                matchId = matchId,
                algorithmName = "Weighted Scoring",
                homeWinProb = hProb,
                drawProb = dProb,
                awayWinProb = aProb,
                predictedOutcome = outcome,
                confidenceScore = confidence
            )
        }
    }
}
