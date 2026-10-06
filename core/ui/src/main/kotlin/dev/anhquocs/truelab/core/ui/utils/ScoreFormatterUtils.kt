package dev.anhquocs.truelab.core.ui.utils

/**
 * Utility for formatting match score text with optional penalty shootout details.
 */
object ScoreFormatterUtils {

    /**
     * Formats match score with penalty shootout result if present.
     *
     * @param homeScore Normal time home score.
     * @param awayScore Normal time away score.
     * @param isPenalty Whether the match went to penalty shootout.
     * @param homePenaltyScore Home team penalty score.
     * @param awayPenaltyScore Away team penalty score.
     * @param delimiter Score delimiter (default " - ").
     * @return Formatted score string (e.g., "2 - 2 (PEN 4-2)", "2 - 1", "- : -").
     */
    fun formatScore(
        homeScore: Int?,
        awayScore: Int?,
        isPenalty: Boolean = false,
        homePenaltyScore: Int? = null,
        awayPenaltyScore: Int? = null,
        delimiter: String = " - "
    ): String {
        if (homeScore == null || awayScore == null) return "- : -"
        val mainScore = "$homeScore$delimiter$awayScore"
        return if (isPenalty && homePenaltyScore != null && awayPenaltyScore != null) {
            "$mainScore (PEN $homePenaltyScore-$awayPenaltyScore)"
        } else {
            mainScore
        }
    }
}
