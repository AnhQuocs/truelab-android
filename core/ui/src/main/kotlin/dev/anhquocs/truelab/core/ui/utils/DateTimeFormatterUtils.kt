package dev.anhquocs.truelab.core.ui.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Standard DateTime Formatter utility enforcing Vietnam Timezone (Asia/Ho_Chi_Minh - UTC+7)
 * across all presentation layers in TrueLab.
 */
object DateTimeFormatterUtils {

    val VIETNAM_ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /**
     * Formats raw timestamp/ISO string into Vietnam date-time string ("dd-MM-yyyy HH:mm").
     * Example: "2026-09-30T06:00:00Z" -> "30-09-2026 13:00"
     */
    fun formatToVietnamDateTime(rawDateTime: String?): String {
        if (rawDateTime.isNullOrBlank()) return "--"
        return try {
            val zonedDateTime = parseToVietnamZonedDateTime(rawDateTime)
            zonedDateTime.format(DATE_TIME_FORMATTER)
        } catch (_: Exception) {
            rawDateTime
        }
    }

    /**
     * Formats raw timestamp/ISO string into Vietnam time string ("HH:mm").
     * Example: "2026-09-30T06:00:00Z" -> "13:00"
     */
    fun formatToVietnamTime(rawDateTime: String?): String {
        if (rawDateTime.isNullOrBlank()) return "--"
        return try {
            val zonedDateTime = parseToVietnamZonedDateTime(rawDateTime)
            zonedDateTime.format(TIME_FORMATTER)
        } catch (_: Exception) {
            rawDateTime
        }
    }

    /**
     * Formats raw timestamp/ISO string into Vietnam date string ("dd-MM-yyyy").
     * Example: "2026-09-30T06:00:00Z" -> "30-09-2026"
     */
    fun formatToVietnamDate(rawDateTime: String?): String {
        if (rawDateTime.isNullOrBlank()) return "--"
        return try {
            val zonedDateTime = parseToVietnamZonedDateTime(rawDateTime)
            zonedDateTime.format(DATE_FORMATTER)
        } catch (_: Exception) {
            rawDateTime
        }
    }

    /**
     * Parses various raw date/time representations (ISO-8601 with Z, ISO with offset,
     * local datetime string "YYYY-MM-DD HH:MM:SS", unix epoch millis/seconds, or local date)
     * into a [ZonedDateTime] pegged to [VIETNAM_ZONE_ID].
     */
    fun parseToVietnamZonedDateTime(raw: String): ZonedDateTime {
        val trimmed = raw.trim()
        return when {
            // ISO instant with 'Z' suffix or explicit timezone offset (+00:00, -05:00)
            trimmed.endsWith("Z", ignoreCase = true) ||
                trimmed.contains("+") ||
                (trimmed.contains("-") && trimmed.lastIndexOf('-') > 10) -> {
                try {
                    val instant = Instant.parse(trimmed)
                    instant.atZone(VIETNAM_ZONE_ID)
                } catch (_: Exception) {
                    val offsetDateTime = OffsetDateTime.parse(trimmed)
                    offsetDateTime.atZoneSameInstant(VIETNAM_ZONE_ID)
                }
            }
            // Standard ISO without offset (treated as UTC server timestamp)
            trimmed.contains("T") -> {
                val localDateTime = LocalDateTime.parse(trimmed)
                localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(VIETNAM_ZONE_ID)
            }
            // SQL standard "YYYY-MM-DD HH:MM:SS" (treated as UTC server timestamp)
            trimmed.contains(" ") -> {
                val normalized = trimmed.replace(" ", "T")
                val localDateTime = LocalDateTime.parse(normalized)
                localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(VIETNAM_ZONE_ID)
            }
            // Epoch timestamp in seconds or millis
            trimmed.toLongOrNull() != null -> {
                val num = trimmed.toLong()
                val instant = if (num > 100_000_000_000L) Instant.ofEpochMilli(num) else Instant.ofEpochSecond(num)
                instant.atZone(VIETNAM_ZONE_ID)
            }
            // Date only "YYYY-MM-DD"
            else -> {
                val localDate = LocalDate.parse(trimmed)
                localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(VIETNAM_ZONE_ID)
            }
        }
    }
}
