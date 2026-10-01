package dev.anhquocs.truelab.core.data.crawler.policy

import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord

interface CompetitionQualityPolicy {
    fun evaluate(record: MatchRecord): QualityTier

    fun isAccepted(record: MatchRecord): Boolean {
        val tier = evaluate(record)
        return tier == QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL ||
            tier == QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS ||
            tier == QualityTier.TIER_3_OFFICIAL_INTERNATIONAL
    }
}

class DefaultCompetitionQualityPolicy(
    private val quarantineManager: QuarantineManager? = null
) : CompetitionQualityPolicy {

    companion object {
        val EXPLICIT_WHITELIST = mapOf(
            // Tier 1: Top Domestic & Continental
            927 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,   // English Premier League
            954 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,   // Spanish La Liga
            999 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,   // Italian Serie A
            1017 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,  // German Bundesliga
            1065 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,  // French Ligue 1
            1054 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,  // Dutch Eredivisie
            758 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,   // United States Major League Soccer (MLS)
            761 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,   // Mexico Liga MX
            1398 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,  // UEFA Champions League
            1411 to QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL,  // UEFA Europa League

            // Tier 2: 2nd Divisions & Domestic Cups
            930 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS,  // EFL Championship
            955 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS,  // Segunda Division
            1007 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, // Serie B
            1809 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, // J.League Yamazaki Biscuit Levain Cup
            1117 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, // USL Championship
            1101 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, // Uruguay Primera Division
            1106 to QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, // El Salvador Primera Division

            // Tier 3: Official International / National Teams
            2239057 to QualityTier.TIER_3_OFFICIAL_INTERNATIONAL,    // FIFA ASEAN Cup
            2484 to QualityTier.TIER_3_OFFICIAL_INTERNATIONAL,       // CONCACAF Nations League
            1756 to QualityTier.TIER_3_OFFICIAL_INTERNATIONAL        // OCA Women's Asian Games
        )

        val EXPLICIT_BLACKLIST = setOf(
            820,   // International Friendly
            1103,  // International Club Friendly
            1379,  // Chinese Football Association U-20 League
            2292,  // Colombian U19 League
            2510,  // Myanmar U20 League
            1253,  // Uruguay Reserve League
            1122,  // Guatemala Division 4
            2108   // Indian Mizoram Premier League
        )

        private val YOUTH_PATTERNS = listOf(
            Regex("""\bU-?1[5-9]\b""", RegexOption.IGNORE_CASE),
            Regex("""\bU-?2[0-3]\b""", RegexOption.IGNORE_CASE),
            Regex("""\bYouth\b""", RegexOption.IGNORE_CASE),
            Regex("""\bJunior\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCadete\b""", RegexOption.IGNORE_CASE),
            Regex("""\bJuvenil\b""", RegexOption.IGNORE_CASE),
            Regex("""\bPrimavera\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSub-?1[5-9]\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSub-?2[0-3]\b""", RegexOption.IGNORE_CASE)
        )

        private val RESERVE_PATTERNS = listOf(
            Regex("""\bReserves?\b""", RegexOption.IGNORE_CASE),
            Regex("""\bB-Team\b""", RegexOption.IGNORE_CASE),
            Regex("""\bDevelopment League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSub-?\d+\b""", RegexOption.IGNORE_CASE)
        )

        private val FRIENDLY_PATTERNS = listOf(
            Regex("""\bFriendly\b""", RegexOption.IGNORE_CASE),
            Regex("""\bAmistoso\b""", RegexOption.IGNORE_CASE),
            Regex("""\bExhibition\b""", RegexOption.IGNORE_CASE),
            Regex("""\bClub Friendly\b""", RegexOption.IGNORE_CASE),
            Regex("""\bInternational Friendly\b""", RegexOption.IGNORE_CASE)
        )

        private val AMATEUR_PATTERNS = listOf(
            Regex("""\bDivision [4-9]\b""", RegexOption.IGNORE_CASE),
            Regex("""\b5th League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bAmateur\b""", RegexOption.IGNORE_CASE),
            Regex("""\bRegional League\b""", RegexOption.IGNORE_CASE)
        )

        // Specific Bundesliga Guards
        private val BUNDESLIGA_KEYWORD = Regex("""\bBundesliga\b""", RegexOption.IGNORE_CASE)
        private val BUNDESLIGA_LOWER_DIVISIONS = Regex("""(2\.?\s*Bundesliga|Bundesliga\s*[2-9])""", RegexOption.IGNORE_CASE)
        private val BUNDESLIGA_TIER_2_REGEX = Regex("""(2\.?\s*Bundesliga|Bundesliga\s*2\b)""", RegexOption.IGNORE_CASE)

        // Unambiguous Tier 1 Patterns (Premier Domestic & Continental)
        private val TIER_1_FALLBACK_PATTERNS = listOf(
            Regex("""\bUEFA Champions League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bChampions League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bUEFA Europa League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bEuropa League\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(UEFA )?Conference League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCopa Libertadores\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCopa Sudamericana\b""", RegexOption.IGNORE_CASE),
            Regex("""\bAFC Champions League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCAF Champions League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCONCACAF Champions\b""", RegexOption.IGNORE_CASE),
            Regex("""\bPremier League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bLa Liga\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSerie A\b""", RegexOption.IGNORE_CASE),
            Regex("""\bLigue 1\b""", RegexOption.IGNORE_CASE),
            Regex("""\bEredivisie\b""", RegexOption.IGNORE_CASE),
            Regex("""\bMajor League Soccer\b""", RegexOption.IGNORE_CASE),
            Regex("""\bMLS\b""", RegexOption.IGNORE_CASE),
            Regex("""\bBrasileir[aã]o\b""", RegexOption.IGNORE_CASE),
            Regex("""\bLiga MX\b""", RegexOption.IGNORE_CASE),
            Regex("""\bJ1 League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bK League 1\b""", RegexOption.IGNORE_CASE),
            Regex("""\bV-League 1\b""", RegexOption.IGNORE_CASE),
            Regex("""\bV\.League 1\b""", RegexOption.IGNORE_CASE),
            Regex("""\bNWSL\b""", RegexOption.IGNORE_CASE),
            Regex("""\bWomen's Super League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bFrauen-Bundesliga\b""", RegexOption.IGNORE_CASE),
            Regex("""\bLiga F\b""", RegexOption.IGNORE_CASE),
            Regex("""\bPremiere Ligue\b""", RegexOption.IGNORE_CASE),
            Regex("""\bWomen's Champions League\b""", RegexOption.IGNORE_CASE)
        )

        // Unambiguous Tier 2 Patterns (High-Confidence 2nd Tier & Major Official Domestic Cups)
        private val TIER_2_FALLBACK_PATTERNS = listOf(
            Regex("""\bEFL Championship\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSerie B\b""", RegexOption.IGNORE_CASE),
            Regex("""\bLigue 2\b""", RegexOption.IGNORE_CASE),
            Regex("""\bFA Cup\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCopa del Rey\b""", RegexOption.IGNORE_CASE),
            Regex("""\bDFB-Pokal\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCoupe de France\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCoppa Italia\b""", RegexOption.IGNORE_CASE),
            Regex("""\bEFL Cup\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCarabao Cup\b""", RegexOption.IGNORE_CASE),
            Regex("""\bJ2 League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bK League 2\b""", RegexOption.IGNORE_CASE),
            Regex("""\bV-League 2\b""", RegexOption.IGNORE_CASE),
            Regex("""\bV\.League 2\b""", RegexOption.IGNORE_CASE),
            Regex("""\bSuperettan\b""", RegexOption.IGNORE_CASE),
            Regex("""\bEerste Divisie\b""", RegexOption.IGNORE_CASE)
        )

        // Unambiguous Tier 3 Patterns (Official National Team Competitions)
        private val TIER_3_FALLBACK_PATTERNS = listOf(
            Regex("""\b(FIFA\s+)?World Cup\b""", RegexOption.IGNORE_CASE),
            Regex("""\bUEFA Nations League\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCONCACAF Nations League\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(AFC\s+)?Asian Cup\b""", RegexOption.IGNORE_CASE),
            Regex("""\bCopa America\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(FIFA\s+)?ASEAN (Championship|Cup)\b""", RegexOption.IGNORE_CASE),
            Regex("""\bAFF (Championship|Cup)\b""", RegexOption.IGNORE_CASE),
            Regex("""\bAsian Games\b""", RegexOption.IGNORE_CASE),
            Regex("""\bOlympic(s)? Football\b""", RegexOption.IGNORE_CASE),
            Regex("""\bUEFA (European Championship|Euro)\b""", RegexOption.IGNORE_CASE),
            Regex("""\bEuro(pean)? Championship\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(CAF\s+)?Africa Cup of Nations\b""", RegexOption.IGNORE_CASE),
            Regex("""\bAFCON\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(CONCACAF\s+)?Gold Cup\b""", RegexOption.IGNORE_CASE)
        )
    }

    override fun evaluate(record: MatchRecord): QualityTier {
        val compId = record.competitionId ?: record.competition?.id

        // 1. Explicit Whitelist Check (Ultimate Priority)
        if (compId != null) {
            val whitelistedTier = EXPLICIT_WHITELIST[compId]
            if (whitelistedTier != null) {
                return whitelistedTier
            }
        }

        // 2. Explicit Blacklist Check
        if (compId != null && EXPLICIT_BLACKLIST.contains(compId)) {
            return QualityTier.EXCLUDED_UNQUALIFIED
        }

        val compName = record.competition?.name.orEmpty()
        val compShortName = record.competition?.shortName.orEmpty()
        val homeTeamName = record.homeTeam.name
        val awayTeamName = record.awayTeam.name

        val textToAudit = listOf(compName, compShortName, homeTeamName, awayTeamName)
        val compTextOnly = listOf(compName, compShortName)

        // 3. Strong Exclusion Patterns (Youth, Reserves, Friendly, Deep Amateur)
        if (matchesAny(textToAudit, YOUTH_PATTERNS) ||
            matchesAny(textToAudit, RESERVE_PATTERNS) ||
            matchesAny(textToAudit, FRIENDLY_PATTERNS) ||
            matchesAny(compTextOnly, AMATEUR_PATTERNS)
        ) {
            return QualityTier.EXCLUDED_UNQUALIFIED
        }

        // 4. Unambiguous Accepted Fallback Patterns
        // 4.1 Tier 1 Fallback Check
        if (matchesTier1(compTextOnly)) {
            return QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL
        }

        // 4.2 Tier 2 Fallback Check
        if (matchesTier2(compTextOnly)) {
            return QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS
        }

        // 4.3 Tier 3 Fallback Check
        if (matchesAny(compTextOnly, TIER_3_FALLBACK_PATTERNS)) {
            return QualityTier.TIER_3_OFFICIAL_INTERNATIONAL
        }

        // 5. Quarantine for Unknown or Ambiguous Competitions
        if (compId != null) {
            quarantineManager?.record(
                competitionId = compId,
                name = if (compName.isNotBlank()) compName else "Unknown #$compId",
                shortName = compShortName.ifBlank { null },
                date = record.startTimeDate.take(10),
                homeTeam = homeTeamName,
                awayTeam = awayTeamName,
                reason = "UNKNOWN_OR_AMBIGUOUS_COMPETITION"
            )
        }

        return QualityTier.QUARANTINE
    }

    private fun matchesTier1(texts: List<String>): Boolean {
        for (text in texts) {
            if (text.isBlank()) continue

            // Guarded Bundesliga check: Must not be lower divisions (2. Bundesliga, Bundesliga 3/4/5)
            if (BUNDESLIGA_KEYWORD.containsMatchIn(text)) {
                if (!BUNDESLIGA_LOWER_DIVISIONS.containsMatchIn(text)) {
                    return true
                }
            }

            for (pattern in TIER_1_FALLBACK_PATTERNS) {
                if (pattern.containsMatchIn(text)) {
                    return true
                }
            }
        }
        return false
    }

    private fun matchesTier2(texts: List<String>): Boolean {
        for (text in texts) {
            if (text.isBlank()) continue

            // 2. Bundesliga check
            if (BUNDESLIGA_TIER_2_REGEX.containsMatchIn(text)) {
                return true
            }

            for (pattern in TIER_2_FALLBACK_PATTERNS) {
                if (pattern.containsMatchIn(text)) {
                    return true
                }
            }
        }
        return false
    }

    private fun matchesAny(texts: List<String>, patterns: List<Regex>): Boolean {
        for (text in texts) {
            if (text.isBlank()) continue
            for (pattern in patterns) {
                if (pattern.containsMatchIn(text)) {
                    return true
                }
            }
        }
        return false
    }
}
