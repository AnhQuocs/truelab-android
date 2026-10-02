package dev.anhquocs.truelab.core.domain.match.model

/**
 * Filter status modes specifically designed for Match Prediction workflows.
 */
enum class PredictionStatusFilter {
    LIVE_AND_UPCOMING,
    LIVE,
    UPCOMING,
    FINISHED,
    ALL
}
