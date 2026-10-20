package dev.anhquocs.truelab.core.data.crawler.util

import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import kotlin.math.pow

/**
 * Idempotent, incremental Elo rating updater.
 *
 * Updates Elo ratings for home and away teams when a match transitions to ended status.
 * Reuses the standard Elo rating system with K-factor = 32.0.
 */
object IncrementalEloUpdater {

    private const val DEFAULT_K_FACTOR = 32.0
    private val ENDED_STATUSES = setOf("ended", "determined", "finished", "ft", "aet", "pen", "100")

    fun updateEloForMatches(database: TrueLabDatabase, records: List<MatchRecord>) {
        val endedRecords = records.filter {
            val status = it.status?.lowercase()
            status in ENDED_STATUSES && it.homeScore != null && it.awayScore != null
        }
        if (endedRecords.isEmpty()) return

        for (record in endedRecords) {
            val existing = database.matchDao().getMatchByIdDirect(record.id)
            val existingStatus = existing?.status?.lowercase()
            val alreadyEnded = existingStatus in ENDED_STATUSES

            // Only update Elo when match is newly transitioned to ended (prevent double counting)
            if (!alreadyEnded) {
                val homeId = record.homeTeam.id
                val awayId = record.awayTeam.id
                val hs = record.homeScore ?: continue
                val as_ = record.awayScore ?: continue

                val homeTeam = database.teamDao().getTeamByIdDirect(homeId) ?: continue
                val awayTeam = database.teamDao().getTeamByIdDirect(awayId) ?: continue

                val rHome = homeTeam.eloRating
                val rAway = awayTeam.eloRating

                val expHome = 1.0 / (1.0 + 10.0.pow((rAway - rHome) / 400.0))
                val actualHome = when {
                    hs > as_ -> 1.0
                    hs == as_ -> 0.5
                    else -> 0.0
                }
                val delta = DEFAULT_K_FACTOR * (actualHome - expHome)

                database.teamDao().updateTeamElo(homeId, rHome + delta)
                database.teamDao().updateTeamElo(awayId, rAway - delta)
            }
        }
    }
}
