package dev.anhquocs.truelab.core.data.crawler.policy

/**
 * QualityTier classifies competitions into hierarchical tiers for dataset integrity.
 */
enum class QualityTier {
    TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,
    TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS,
    TIER_3_OFFICIAL_INTERNATIONAL,
    EXCLUDED_UNQUALIFIED,
    QUARANTINE
}
