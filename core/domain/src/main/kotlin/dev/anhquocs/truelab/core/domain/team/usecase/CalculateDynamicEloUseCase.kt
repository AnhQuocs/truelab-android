package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.rating.EloRatingCalculator
import dev.anhquocs.truelab.core.algorithm.rating.RatingCalculator
import dev.anhquocs.truelab.core.domain.match.model.Match

/**
 * Pure Kotlin/JVM UseCase calculating dynamic historical Elo ratings for teams up to a given timestamp.
 *
 * Replays all ended matches chronologically and strictly before [beforeTimestamp] using [RatingCalculator].
 * Ensures 100% Zero Temporal Data Leakage and consistent behavior with Backtest evaluation.
 */
class CalculateDynamicEloUseCase(
    private val eloRatingCalculator: RatingCalculator = EloRatingCalculator()
) {

    /**
     * Calculates dynamic Elo rating map for all teams from the provided match list strictly before [beforeTimestamp].
     *
     * @param allMatches All matches in the database.
     * @param beforeTimestamp ISO or date string threshold (matches on or after this timestamp will NOT mutate Elo).
     * @param initialEloMap Optional base Elo map for teams (default 1500.0).
     * @return Map of teamId to Dynamic Elo Rating.
     */
    operator fun invoke(
        allMatches: List<Match>,
        beforeTimestamp: String? = null,
        initialEloMap: Map<Int, Double> = emptyMap()
    ): Map<Int, Double> {
        val sortedEnded = allMatches
            .filter { it.isEnded && it.homeScore != null && it.awayScore != null && it.startTimeDate.isNotBlank() }
            .let { list ->
                if (!beforeTimestamp.isNullOrBlank()) {
                    list.filter { it.startTimeDate < beforeTimestamp }
                } else {
                    list
                }
            }
            .sortedWith(compareBy({ it.startTimeDate }, { it.id }))

        val eloMap = HashMap<Int, Double>(initialEloMap)

        for (m in sortedEnded) {
            val hs = m.homeScore ?: continue
            val as_ = m.awayScore ?: continue
            val hid = m.homeTeam.id
            val aid = m.awayTeam.id

            val hElo = eloMap[hid] ?: 1500.0
            val aElo = eloMap[aid] ?: 1500.0

            val actualScoreHome = when {
                hs > as_ -> 1.0
                hs == as_ -> 0.5
                else -> 0.0
            }

            val result = eloRatingCalculator.calculateMatch(hElo, aElo, actualScoreHome, kFactor = 32.0)
            eloMap[hid] = result.newRatingA
            eloMap[aid] = result.newRatingB
        }

        return eloMap
    }
}
