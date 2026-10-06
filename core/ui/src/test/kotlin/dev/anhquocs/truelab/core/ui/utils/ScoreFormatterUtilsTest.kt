package dev.anhquocs.truelab.core.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreFormatterUtilsTest {

    @Test
    fun formatScore_normalScores_returnsFormattedString() {
        val result = ScoreFormatterUtils.formatScore(2, 1)
        assertEquals("2 - 1", result)
    }

    @Test
    fun formatScore_normalDraw_returnsFormattedString() {
        val result = ScoreFormatterUtils.formatScore(2, 2)
        assertEquals("2 - 2", result)
    }

    @Test
    fun formatScore_penaltyShootout_returnsScoreWithPen() {
        val result = ScoreFormatterUtils.formatScore(
            homeScore = 2,
            awayScore = 2,
            isPenalty = true,
            homePenaltyScore = 4,
            awayPenaltyScore = 2
        )
        assertEquals("2 - 2 (PEN 4-2)", result)
    }

    @Test
    fun formatScore_isPenaltyTrueButScoresNull_fallbacksToMainScore() {
        val result = ScoreFormatterUtils.formatScore(
            homeScore = 2,
            awayScore = 2,
            isPenalty = true,
            homePenaltyScore = null,
            awayPenaltyScore = null
        )
        assertEquals("2 - 2", result)
    }

    @Test
    fun formatScore_nullScores_returnsPlaceholder() {
        val result = ScoreFormatterUtils.formatScore(null, null)
        assertEquals("- : -", result)
    }

    @Test
    fun formatScore_customDelimiter_returnsFormattedStringWithCustomDelimiter() {
        val result = ScoreFormatterUtils.formatScore(
            homeScore = 2,
            awayScore = 2,
            isPenalty = true,
            homePenaltyScore = 4,
            awayPenaltyScore = 2,
            delimiter = " : "
        )
        assertEquals("2 : 2 (PEN 4-2)", result)
    }
}
