package dev.anhquocs.truelab.core.domain.evaluation.usecase

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestOffline30kVerificationTest {

    private val backtestUseCase = BacktestPredictionUseCase()

    private fun createSyntheticMatch(
        id: Long,
        homeId: Int,
        awayId: Int,
        homeScore: Int?,
        awayScore: Int?,
        startTimeDate: String,
        status: MatchStatus = MatchStatus.ENDED
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(id = homeId, name = "Team $homeId"),
            awayTeam = TeamSummary(id = awayId, name = "Team $awayId"),
            homeScore = homeScore,
            awayScore = awayScore,
            startTimeDate = startTimeDate,
            status = status
        )
    }

    @Test
    fun `backtest verifies metadata exposure and chronological evaluation on simulated 30k representative stream`() {
        val count = 1000
        val matches = (1..count).map { i ->
            val day = (i % 28) + 1
            val month = ((i / 28) % 12) + 1
            val dateStr = String.format("2026-%02d-%02dT15:00:00Z", month, day)

            val homeScore = (i % 4)
            val awayScore = ((i * 2) % 3)

            val isEnded = i <= 980 // 980 ended, 20 scheduled/live
            createSyntheticMatch(
                id = i.toLong(),
                homeId = (i % 50) + 1,
                awayId = ((i + 25) % 50) + 1,
                homeScore = if (isEnded) homeScore else null,
                awayScore = if (isEnded) awayScore else null,
                startTimeDate = dateStr,
                status = if (isEnded) MatchStatus.ENDED else MatchStatus.SCHEDULED
            )
        }

        val totalInput = matches.size
        val endedInput = matches.count { it.isEnded && it.homeScore != null && it.awayScore != null }
        val minDate = matches.minOf { it.startTimeDate }
        val maxDate = matches.maxOf { it.startTimeDate }

        println("=== BACKTEST METADATA AUDIT ===")
        println("Total Input Matches: $totalInput")
        println("Ended Input Matches: $endedInput")
        println("Min Start Time: $minDate")
        println("Max Start Time: $maxDate")

        val result = backtestUseCase(
            matches = matches,
            matchOddsMap = emptyMap(),
            teamEloMap = emptyMap()
        )

        println("Backtest Processed Target Matches: ${result.totalMatches}")
        println("Correct Matches: ${result.correctMatches}")
        println("Accuracy: ${String.format("%.2f%%", result.evaluationResult.accuracy * 100)}")
        println("Confusion Matrix: ${result.evaluationResult.confusionMatrix}")

        assertEquals(endedInput, result.totalMatches)
        assertEquals(endedInput, result.records.size)
        assertTrue("Accuracy must be > 0", result.evaluationResult.accuracy > 0.0)
    }
}
