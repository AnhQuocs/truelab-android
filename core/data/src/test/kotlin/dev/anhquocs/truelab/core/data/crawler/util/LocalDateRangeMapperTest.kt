package dev.anhquocs.truelab.core.data.crawler.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class LocalDateRangeMapperTest {

    private val vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun toUtcQueryWindow_vietnamDate_producesCorrectUtcRangeAndTwoUtcDates() {
        val localDate = LocalDate.parse("2026-10-02")
        val window = LocalDateRangeMapper.toUtcQueryWindow(localDate, vietnamZone)

        // 2026-10-02 00:00:00+07:00 = 2026-10-01 17:00:00 UTC
        assertEquals(Instant.parse("2026-10-01T17:00:00Z"), window.startInstant)
        // 2026-10-03 00:00:00+07:00 = 2026-10-02 17:00:00 UTC
        assertEquals(Instant.parse("2026-10-02T17:00:00Z"), window.endInstant)

        assertEquals(listOf("2026-10-01", "2026-10-02"), window.utcDates)
    }

    @Test
    fun toUtcQueryWindow_utcDate_producesSingleUtcDate() {
        val localDate = LocalDate.parse("2026-10-02")
        val window = LocalDateRangeMapper.toUtcQueryWindow(localDate, ZoneOffset.UTC)

        assertEquals(Instant.parse("2026-10-02T00:00:00Z"), window.startInstant)
        assertEquals(Instant.parse("2026-10-03T00:00:00Z"), window.endInstant)
        assertEquals(listOf("2026-10-02"), window.utcDates)
    }

    @Test
    fun isInWindow_greeceMatch129243_returnsTrueForOctoberSecondVietnam() {
        val window = LocalDateRangeMapper.toUtcQueryWindow("2026-10-02", vietnamZone)

        // Greece vs Netherlands: 2026-10-01 18:45 UTC (= 01:45 VN on 2026-10-02)
        assertTrue(window.isInWindow("2026-10-01T18:45:00Z"))
    }

    @Test
    fun isInWindow_exactBoundaries_evaluatedCorrectly() {
        val window = LocalDateRangeMapper.toUtcQueryWindow("2026-10-02", vietnamZone)

        // 1 second before Vietnam day starts (2026-10-01 23:59:59 VN = 2026-10-01 16:59:59 UTC)
        assertFalse(window.isInWindow("2026-10-01T16:59:59Z"))

        // Exact start of Vietnam day (2026-10-02 00:00:00 VN = 2026-10-01 17:00:00 UTC)
        assertTrue(window.isInWindow("2026-10-01T17:00:00Z"))

        // Early morning VN (06:59:59 VN = 2026-10-01 23:59:59 UTC)
        assertTrue(window.isInWindow("2026-10-01T23:59:59Z"))

        // Midday boundary (07:00:00 VN = 2026-10-02 00:00:00 UTC)
        assertTrue(window.isInWindow("2026-10-02T00:00:00Z"))

        // Late evening VN (23:59:59 VN = 2026-10-02 16:59:59 UTC)
        assertTrue(window.isInWindow("2026-10-02T16:59:59Z"))

        // Exact start of next Vietnam day (2026-10-03 00:00:00 VN = 2026-10-02 17:00:00 UTC) - Exclusive bound
        assertFalse(window.isInWindow("2026-10-02T17:00:00Z"))
    }

    @Test
    fun parseToInstant_handlesVariousFormats() {
        val instantIso = LocalDateRangeMapper.parseToInstant("2026-10-01T18:45:00Z")
        val instantSpace = LocalDateRangeMapper.parseToInstant("2026-10-01 18:45:00")
        val instantOffset = LocalDateRangeMapper.parseToInstant("2026-10-01T18:45:00+00:00")

        assertEquals(Instant.parse("2026-10-01T18:45:00Z"), instantIso)
        assertEquals(Instant.parse("2026-10-01T18:45:00Z"), instantSpace)
        assertEquals(Instant.parse("2026-10-01T18:45:00Z"), instantOffset)
        assertEquals(null, LocalDateRangeMapper.parseToInstant(null))
        assertEquals(null, LocalDateRangeMapper.parseToInstant(""))
    }
}
