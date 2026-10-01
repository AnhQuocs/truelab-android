package dev.anhquocs.truelab.core.data.crawler.bulk

import dev.anhquocs.truelab.core.data.crawler.checkpoint.CheckpointManager
import dev.anhquocs.truelab.core.data.crawler.checkpoint.CrawlerCheckpoint
import dev.anhquocs.truelab.core.data.crawler.policy.CompetitionQualityPolicy
import dev.anhquocs.truelab.core.data.crawler.policy.DefaultCompetitionQualityPolicy
import dev.anhquocs.truelab.core.data.crawler.policy.QualityTier
import dev.anhquocs.truelab.core.data.crawler.policy.QuarantineManager
import dev.anhquocs.truelab.core.data.crawler.retry.RetryExecutor
import dev.anhquocs.truelab.core.data.local.database.TrueLabDatabase
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toAwayTeamEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toHomeTeamEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toLeagueEntity
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toMatchEntity
import dev.anhquocs.truelab.core.data.match.remote.api.MatchApi
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RecentFirstBulkCrawler(
    private val matchApi: MatchApi,
    private val database: TrueLabDatabase,
    private val qualityPolicy: CompetitionQualityPolicy = DefaultCompetitionQualityPolicy(),
    private val quarantineManager: QuarantineManager = QuarantineManager(),
    private val checkpointManager: CheckpointManager = CheckpointManager(),
    private val retryExecutor: RetryExecutor = RetryExecutor(),
    private val checkpointFile: File? = null,
    private val quarantineFile: File? = null,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
    private val interPageDelayMs: Long = 150L,
    private val interDayDelayMs: Long = 250L,
    private val onProgress: ((currentAccepted: Int, target: Int, currentDate: String, page: Int) -> Unit)? = null
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun crawlUntilTarget(
        targetUniqueMatches: Int = 30000,
        initialStartDate: String = "2026-10-01"
    ): BulkCrawlResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var acceptedUniqueMatches = 0
        val existingMatchIds = mutableSetOf<Long>()
        val tierCounts = mutableMapOf<String, Int>()
        val failedDates = mutableListOf<String>()

        // 1. Resume from checkpoint if available
        var currentDate = LocalDate.parse(initialStartDate, dateFormatter)
        var totalProcessedDays = 0

        if (checkpointFile != null && checkpointFile.exists()) {
            val savedCheckpoint = checkpointManager.loadCheckpoint(checkpointFile)
            if (savedCheckpoint != null) {
                acceptedUniqueMatches = savedCheckpoint.currentAcceptedUniqueMatches
                totalProcessedDays = savedCheckpoint.totalProcessedDays
                failedDates.addAll(savedCheckpoint.failedDates)
                savedCheckpoint.qualityTiersCount.forEach { (k, v) -> tierCounts[k] = v }
                if (savedCheckpoint.lastProcessedDate != null) {
                    currentDate = LocalDate.parse(savedCheckpoint.lastProcessedDate, dateFormatter).minusDays(1)
                }
            }
        }

        if (quarantineFile != null && quarantineFile.exists()) {
            quarantineManager.loadFromFile(quarantineFile, json)
        }

        var earliestDateReached = currentDate.format(dateFormatter)
        var isTargetReached = false

        while (acceptedUniqueMatches < targetUniqueMatches) {
            val dateStr = currentDate.format(dateFormatter)
            earliestDateReached = dateStr

            try {
                val dayAcceptedCount = processDay(
                    dateStr = dateStr,
                    targetUniqueMatches = targetUniqueMatches,
                    currentAcceptedCountProvider = { acceptedUniqueMatches },
                    onMatchAccepted = { record, tier ->
                        acceptedUniqueMatches++
                        tierCounts[tier.name] = (tierCounts[tier.name] ?: 0) + 1
                        existingMatchIds.add(record.id)
                    }
                )

                totalProcessedDays++

                // Update & save checkpoint after each day
                saveCurrentState(
                    targetUniqueMatches = targetUniqueMatches,
                    acceptedUniqueMatches = acceptedUniqueMatches,
                    lastProcessedDate = dateStr,
                    totalProcessedDays = totalProcessedDays,
                    failedDates = failedDates,
                    tierCounts = tierCounts,
                    startTime = startTime
                )

                if (acceptedUniqueMatches >= targetUniqueMatches) {
                    isTargetReached = true
                    break
                }

                currentDate = currentDate.minusDays(1)
                delay(interDayDelayMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failedDates.add(dateStr)
                currentDate = currentDate.minusDays(1)
                delay(interDayDelayMs)
            }
        }

        // Final save
        saveCurrentState(
            targetUniqueMatches = targetUniqueMatches,
            acceptedUniqueMatches = acceptedUniqueMatches,
            lastProcessedDate = earliestDateReached,
            totalProcessedDays = totalProcessedDays,
            failedDates = failedDates,
            tierCounts = tierCounts,
            startTime = startTime
        )

        BulkCrawlResult(
            targetUniqueMatches = targetUniqueMatches,
            acceptedUniqueMatches = acceptedUniqueMatches,
            totalProcessedDays = totalProcessedDays,
            startDate = initialStartDate,
            earliestDateReached = earliestDateReached,
            qualityTiersCount = tierCounts.toMap(),
            quarantinedCompetitionsCount = quarantineManager.count(),
            quarantinedMatchesCount = quarantineManager.totalQuarantinedMatches(),
            failedDates = failedDates.toList(),
            isTargetReached = isTargetReached || acceptedUniqueMatches >= targetUniqueMatches,
            durationMs = System.currentTimeMillis() - startTime
        )
    }

    private suspend fun processDay(
        dateStr: String,
        targetUniqueMatches: Int,
        currentAcceptedCountProvider: () -> Int,
        onMatchAccepted: (MatchRecord, QualityTier) -> Unit
    ): Int {
        var currentPage = 1
        var effectiveLastPage = 1
        var dayAcceptedCount = 0

        while (currentPage <= effectiveLastPage) {
            val response = retryExecutor.execute {
                matchApi.getMatches(
                    date = dateStr,
                    page = currentPage,
                    pageSize = 50,
                    sort = "time_asc"
                )
            }

            val meta = response.data.meta
            effectiveLastPage = meta?.effectiveLastPage ?: meta?.lastPage ?: 1
            val rawRecords = response.data.data

            if (rawRecords.isEmpty()) break

            val acceptedForPage = mutableListOf<Pair<MatchRecord, QualityTier>>()

            for (record in rawRecords) {
                val tier = qualityPolicy.evaluate(record)
                if (tier == QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL ||
                    tier == QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS ||
                    tier == QualityTier.TIER_3_OFFICIAL_INTERNATIONAL
                ) {
                    acceptedForPage.add(record to tier)
                }
            }

            if (acceptedForPage.isNotEmpty()) {
                val recordsToPersist = acceptedForPage.map { it.first }
                val leagues = recordsToPersist.mapNotNull { it.toLeagueEntity() }.distinctBy { it.id }
                val teams = recordsToPersist.flatMap {
                    listOf(it.toHomeTeamEntity(), it.toAwayTeamEntity())
                }.distinctBy { it.id }
                val matches = recordsToPersist.map { it.toMatchEntity() }

                database.runInTransaction {
                    if (leagues.isNotEmpty()) {
                        database.leagueDao().insertLeagues(leagues)
                    }
                    if (teams.isNotEmpty()) {
                        database.teamDao().insertTeams(teams)
                    }
                    database.matchDao().upsertMatches(matches)
                }

                for ((record, tier) in acceptedForPage) {
                    onMatchAccepted(record, tier)
                    dayAcceptedCount++
                    if (currentAcceptedCountProvider() >= targetUniqueMatches) {
                        break
                    }
                }
            }

            onProgress?.invoke(
                currentAcceptedCountProvider(),
                targetUniqueMatches,
                dateStr,
                currentPage
            )

            if (currentAcceptedCountProvider() >= targetUniqueMatches) {
                break
            }

            currentPage++
            delay(interPageDelayMs)
        }

        return dayAcceptedCount
    }

    private fun saveCurrentState(
        targetUniqueMatches: Int,
        acceptedUniqueMatches: Int,
        lastProcessedDate: String,
        totalProcessedDays: Int,
        failedDates: List<String>,
        tierCounts: Map<String, Int>,
        startTime: Long
    ) {
        if (checkpointFile != null) {
            val checkpoint = CrawlerCheckpoint(
                targetUniqueMatches = targetUniqueMatches,
                currentAcceptedUniqueMatches = acceptedUniqueMatches,
                lastProcessedDate = lastProcessedDate,
                lastProcessedPage = 1,
                totalProcessedDays = totalProcessedDays,
                failedDates = failedDates,
                sessionStartTime = startTime,
                lastUpdatedTime = System.currentTimeMillis(),
                qualityTiersCount = tierCounts
            )
            checkpointManager.saveCheckpoint(checkpointFile, checkpoint)
        }

        if (quarantineFile != null) {
            quarantineManager.saveToFile(quarantineFile, json)
        }
    }
}
