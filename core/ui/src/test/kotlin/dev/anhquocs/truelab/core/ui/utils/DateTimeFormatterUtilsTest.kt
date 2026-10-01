package dev.anhquocs.truelab.core.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeFormatterUtilsTest {

    @Test
    fun formatToVietnamDateTime_isoUtc_convertsToUtcPlus7() {
        // 06:00 UTC -> 13:00 UTC+7 (Vietnam)
        val isoString = "2026-09-30T06:00:00Z"
        val formatted = DateTimeFormatterUtils.formatToVietnamDateTime(isoString)
        assertEquals("30-09-2026 13:00", formatted)
    }

    @Test
    fun formatToVietnamTime_isoUtc_convertsToUtcPlus7Time() {
        val isoString = "2026-09-30T06:00:00Z"
        val formatted = DateTimeFormatterUtils.formatToVietnamTime(isoString)
        assertEquals("13:00", formatted)
    }

    @Test
    fun formatToVietnamDateTime_spaceUtc_convertsCorrectly() {
        val sqlString = "2026-09-30 06:00:00"
        val formatted = DateTimeFormatterUtils.formatToVietnamDateTime(sqlString)
        assertEquals("30-09-2026 13:00", formatted)
    }

    @Test
    fun formatToVietnamDate_returnsFormattedDate() {
        val isoString = "2026-09-30T06:00:00Z"
        val formatted = DateTimeFormatterUtils.formatToVietnamDate(isoString)
        assertEquals("30-09-2026", formatted)
    }

    @Test
    fun formatToVietnamDateTime_nullOrEmpty_returnsPlaceholder() {
        assertEquals("--", DateTimeFormatterUtils.formatToVietnamDateTime(null))
        assertEquals("--", DateTimeFormatterUtils.formatToVietnamDateTime(""))
    }
}
