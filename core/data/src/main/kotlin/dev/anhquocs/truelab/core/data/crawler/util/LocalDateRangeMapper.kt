package dev.anhquocs.truelab.core.data.crawler.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Data class representing the UTC query window corresponding to a Local Calendar Date.
 *
 * @property utcDates The distinct list of UTC date strings (YYYY-MM-DD) needed to cover the local date.
 * @property startInstant The inclusive start instant of the local date in UTC.
 * @property endInstant The exclusive end instant of the local date in UTC.
 */
data class UtcQueryWindow(
    val utcDates: List<String>,
    val startInstant: Instant,
    val endInstant: Instant
) {
    /**
     * Checks whether a match's start time string falls strictly within this local calendar day window:
     * [startInstant <= matchInstant < endInstant].
     */
    fun isInWindow(startTimeDateStr: String?): Boolean {
        if (startTimeDateStr.isNullOrBlank()) return false
        val instant = LocalDateRangeMapper.parseToInstant(startTimeDateStr) ?: return false
        return instant >= startInstant && instant < endInstant
    }
}

/**
 * Utility mapping local calendar dates (e.g., Vietnam Time Asia/Ho_Chi_Minh UTC+7)
 * to UTC date ranges and API query dates.
 */
object LocalDateRangeMapper {

    val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    private val ISO_OFFSET_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val STANDARD_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Converts a local date string ("yyyy-MM-dd") into a [UtcQueryWindow].
     */
    fun toUtcQueryWindow(
        localDateString: String,
        zoneId: ZoneId = DEFAULT_ZONE_ID
    ): UtcQueryWindow {
        val parsedDate = try {
            LocalDate.parse(localDateString)
        } catch (_: Exception) {
            LocalDate.now(zoneId)
        }
        return toUtcQueryWindow(parsedDate, zoneId)
    }

    /**
     * Converts a [LocalDate] into a [UtcQueryWindow] for the given [zoneId].
     */
    fun toUtcQueryWindow(
        localDate: LocalDate,
        zoneId: ZoneId = DEFAULT_ZONE_ID
    ): UtcQueryWindow {
        val startInstant = localDate.atStartOfDay(zoneId).toInstant()
        val endInstant = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        val startUtcDate = startInstant.atZone(ZoneOffset.UTC).toLocalDate()
        // Minus 1 millisecond to get the exact last UTC day covering the window
        val endUtcDate = endInstant.minusMillis(1).atZone(ZoneOffset.UTC).toLocalDate()

        val utcDates = mutableListOf<String>()
        var current = startUtcDate
        while (!current.isAfter(endUtcDate)) {
            utcDates.add(current.format(DATE_FORMATTER))
            current = current.plusDays(1)
        }

        return UtcQueryWindow(
            utcDates = utcDates.distinct(),
            startInstant = startInstant,
            endInstant = endInstant
        )
    }

    /**
     * Safely parses diverse ISO / Standard datetime representations to [Instant].
     */
    fun parseToInstant(rawDateTime: String?): Instant? {
        if (rawDateTime.isNullOrBlank()) return null
        val trimmed = rawDateTime.trim()

        return try {
            Instant.parse(trimmed)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(trimmed, ISO_OFFSET_FORMATTER).toInstant()
            } catch (_: Exception) {
                try {
                    val normalized = if (trimmed.contains(" ") && !trimmed.contains("T")) {
                        trimmed.replace(" ", "T") + "Z"
                    } else if (!trimmed.endsWith("Z") && !trimmed.contains("+")) {
                        trimmed + "Z"
                    } else {
                        trimmed
                    }
                    Instant.parse(normalized)
                } catch (_: Exception) {
                    try {
                        val localDateTime = LocalDateTime.parse(trimmed, STANDARD_DATE_TIME_FORMATTER)
                        localDateTime.toInstant(ZoneOffset.UTC)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }
}
