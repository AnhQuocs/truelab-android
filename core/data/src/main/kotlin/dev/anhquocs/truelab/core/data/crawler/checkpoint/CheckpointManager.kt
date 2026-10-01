package dev.anhquocs.truelab.core.data.crawler.checkpoint

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CheckpointManager(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
) {

    fun saveCheckpoint(file: File, checkpoint: CrawlerCheckpoint) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val content = json.encodeToString(checkpoint)
        file.writeText(content, Charsets.UTF_8)
    }

    fun loadCheckpoint(file: File): CrawlerCheckpoint? {
        if (!file.exists()) return null
        return try {
            val content = file.readText(Charsets.UTF_8)
            json.decodeFromString<CrawlerCheckpoint>(content)
        } catch (_: Exception) {
            null
        }
    }
}
