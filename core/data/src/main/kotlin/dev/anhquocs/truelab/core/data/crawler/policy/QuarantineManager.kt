package dev.anhquocs.truelab.core.data.crawler.policy

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuarantineManager {

    private val entries = ConcurrentHashMap<Int, QuarantineEntry>()

    fun record(
        competitionId: Int,
        name: String,
        shortName: String?,
        date: String,
        homeTeam: String? = null,
        awayTeam: String? = null,
        reason: String = "UNKNOWN_COMPETITION"
    ) {
        entries.compute(competitionId) { _, existing ->
            if (existing == null) {
                QuarantineEntry(
                    competitionId = competitionId,
                    name = name,
                    shortName = shortName,
                    sampleMatchCount = 1,
                    firstObservedDate = date,
                    lastObservedDate = date,
                    sampleHomeTeam = homeTeam,
                    sampleAwayTeam = awayTeam,
                    reason = reason
                )
            } else {
                existing.copy(
                    sampleMatchCount = existing.sampleMatchCount + 1,
                    lastObservedDate = date,
                    sampleHomeTeam = existing.sampleHomeTeam ?: homeTeam,
                    sampleAwayTeam = existing.sampleAwayTeam ?: awayTeam
                )
            }
        }
    }

    fun getAllEntries(): List<QuarantineEntry> {
        return entries.values.sortedByDescending { it.sampleMatchCount }
    }

    fun count(): Int = entries.size

    fun totalQuarantinedMatches(): Int = entries.values.sumOf { it.sampleMatchCount }

    fun exportToJson(json: Json): String {
        return json.encodeToString(getAllEntries())
    }

    fun saveToFile(file: File, json: Json) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val content = exportToJson(json)
        file.writeText(content, Charsets.UTF_8)
    }

    fun loadFromFile(file: File, json: Json) {
        if (!file.exists()) return
        try {
            val content = file.readText(Charsets.UTF_8)
            val list = json.decodeFromString<List<QuarantineEntry>>(content)
            list.forEach { entry ->
                entries[entry.competitionId] = entry
            }
        } catch (_: Exception) {
            // Ignore parse errors on resume
        }
    }

    fun clear() {
        entries.clear()
    }
}
