package dev.anhquocs.truelab.core.data.crawler.policy

import dev.anhquocs.truelab.core.data.match.remote.dto.CompetitionSummaryInfo
import dev.anhquocs.truelab.core.data.match.remote.dto.MatchRecord
import dev.anhquocs.truelab.core.data.match.remote.dto.TeamInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompetitionQualityPolicyTest {

    private lateinit var quarantineManager: QuarantineManager
    private lateinit var policy: DefaultCompetitionQualityPolicy

    @Before
    fun setup() {
        quarantineManager = QuarantineManager()
        policy = DefaultCompetitionQualityPolicy(quarantineManager)
    }

    private fun createDummyRecord(
        compId: Int?,
        compName: String,
        compShortName: String? = null,
        homeTeam: String = "Home FC",
        awayTeam: String = "Away FC"
    ): MatchRecord {
        return MatchRecord(
            id = 1001L,
            homeTeam = TeamInfo(id = 1, name = homeTeam, logo = null),
            awayTeam = TeamInfo(id = 2, name = awayTeam, logo = null),
            homeScore = 2,
            awayScore = 1,
            startTimeDate = "2026-10-01 15:00:00",
            status = "8",
            competitionId = compId,
            competition = compId?.let {
                CompetitionSummaryInfo(
                    id = it,
                    name = compName,
                    shortName = compShortName,
                    logo = null
                )
            }
        )
    }

    @Test
    fun `test explicit whitelist returns correct tier 1`() {
        val premierLeague = createDummyRecord(927, "Premier League", "EPL")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(premierLeague))
        assertTrue(policy.isAccepted(premierLeague))

        val ucl = createDummyRecord(1398, "UEFA Champions League", "UCL")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(ucl))
        assertTrue(policy.isAccepted(ucl))
    }

    @Test
    fun `test explicit whitelist returns correct tier 2`() {
        val championship = createDummyRecord(930, "Championship", "CHA")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(championship))
        assertTrue(policy.isAccepted(championship))
    }

    @Test
    fun `test explicit whitelist returns correct tier 3`() {
        val affCup = createDummyRecord(2239057, "FIFA ASEAN Cup", "AFF", homeTeam = "Vietnam", awayTeam = "Pakistan")
        assertEquals(QualityTier.TIER_3_OFFICIAL_INTERNATIONAL, policy.evaluate(affCup))
        assertTrue(policy.isAccepted(affCup))
    }

    @Test
    fun `test asean championship fallback returns tier 3`() {
        val aseanChampionship = createDummyRecord(1748, "ASEAN Championship", "ASEAN Championship", homeTeam = "Vietnam", awayTeam = "Pakistan")
        assertEquals(QualityTier.TIER_3_OFFICIAL_INTERNATIONAL, policy.evaluate(aseanChampionship))
        assertTrue(policy.isAccepted(aseanChampionship))
    }

    @Test
    fun `test international friendly for national teams is rejected`() {
        val friendly = createDummyRecord(820, "International Friendly", homeTeam = "Vietnam", awayTeam = "Pakistan")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(friendly))
        assertFalse(policy.isAccepted(friendly))
    }

    @Test
    fun `test youth international for national teams is rejected`() {
        val youthMatch = createDummyRecord(2239057, "FIFA ASEAN Cup", homeTeam = "Vietnam U19", awayTeam = "Pakistan U19")
        // Whitelist ID 2239057 takes precedence if whitelisted, BUT for unwhitelisted or general youth:
        val unwhitelistedYouth = createDummyRecord(9995, "ASEAN Championship", homeTeam = "Vietnam U19", awayTeam = "Pakistan U19")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(unwhitelistedYouth))
        assertFalse(policy.isAccepted(unwhitelistedYouth))
    }

    @Test
    fun `test explicit blacklist is rejected`() {
        val friendly = createDummyRecord(820, "International Friendly")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(friendly))
        assertFalse(policy.isAccepted(friendly))

        val u20 = createDummyRecord(1379, "Chinese FA U-20 League")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(u20))
        assertFalse(policy.isAccepted(u20))
    }

    @Test
    fun `test strong exclusion on youth patterns`() {
        val youthMatch = createDummyRecord(9999, "State Youth League", homeTeam = "Arsenal U19", awayTeam = "Chelsea U19")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(youthMatch))
        assertFalse(policy.isAccepted(youthMatch))

        val primaveraMatch = createDummyRecord(9998, "Campionato Primavera 1")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(primaveraMatch))
    }

    @Test
    fun `test strong exclusion on reserves and friendlies`() {
        val reserveMatch = createDummyRecord(9997, "National Reserves League")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(reserveMatch))

        val friendlyMatch = createDummyRecord(9996, "Club Friendly Game")
        assertEquals(QualityTier.EXCLUDED_UNQUALIFIED, policy.evaluate(friendlyMatch))
    }

    @Test
    fun `test accepted fallback rules for national cups`() {
        val faCup = createDummyRecord(8888, "English FA Cup")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(faCup))
        assertTrue(policy.isAccepted(faCup))

        val copaDelRey = createDummyRecord(8887, "Spanish Copa del Rey")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(copaDelRey))
        assertTrue(policy.isAccepted(copaDelRey))

        val dfbPokal = createDummyRecord(8885, "DFB-Pokal")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(dfbPokal))

        val coppaItalia = createDummyRecord(8884, "Coppa Italia")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(coppaItalia))

        val coupeDeFrance = createDummyRecord(8883, "Coupe de France")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(coupeDeFrance))

        val eflCup = createDummyRecord(8882, "EFL Cup")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(eflCup))
    }

    @Test
    fun `test accepted fallback rules for international competitions`() {
        val wcq = createDummyRecord(8886, "FIFA World Cup Asian Qualifiers")
        assertEquals(QualityTier.TIER_3_OFFICIAL_INTERNATIONAL, policy.evaluate(wcq))
        assertTrue(policy.isAccepted(wcq))

        val nationsLeague = createDummyRecord(8881, "UEFA Nations League")
        assertEquals(QualityTier.TIER_3_OFFICIAL_INTERNATIONAL, policy.evaluate(nationsLeague))

        val asianCup = createDummyRecord(8880, "AFC Asian Cup")
        assertEquals(QualityTier.TIER_3_OFFICIAL_INTERNATIONAL, policy.evaluate(asianCup))
    }

    @Test
    fun `test unknown competition is quarantined and deduplicated`() {
        val unknown1 = createDummyRecord(7777, "Random Mystery League 2026")
        val tier1 = policy.evaluate(unknown1)
        assertEquals(QualityTier.QUARANTINE, tier1)

        assertEquals(1, quarantineManager.count())
        assertEquals(1, quarantineManager.totalQuarantinedMatches())

        // Same competition observed again
        val unknown2 = createDummyRecord(7777, "Random Mystery League 2026")
        val tier2 = policy.evaluate(unknown2)
        assertEquals(QualityTier.QUARANTINE, tier2)

        assertEquals(1, quarantineManager.count()) // Deduplicated
        assertEquals(2, quarantineManager.totalQuarantinedMatches())
    }

    @Test
    fun `test whitelist takes absolute precedence over exclusion keywords`() {
        // Suppose a whitelisted ID has a tricky name
        val whitelisted = createDummyRecord(927, "Premier League - Youth Showcase Exhibition")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(whitelisted))
        assertTrue(policy.isAccepted(whitelisted))
    }

    // ==========================================
    // V2 REGRESSION TESTS
    // ==========================================

    @Test
    fun `test regression bundesliga 5 is not tier 1`() {
        val bundesliga5 = createDummyRecord(911, "German Bundesliga 5")
        val tier = policy.evaluate(bundesliga5)
        assertEquals(QualityTier.QUARANTINE, tier)
        assertFalse(policy.isAccepted(bundesliga5))

        val bundesliga3 = createDummyRecord(913, "Bundesliga 3")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(bundesliga3))
    }

    @Test
    fun `test official bundesliga fallback is tier 1 and 2 bundesliga is tier 2`() {
        val premierBundesliga = createDummyRecord(9991, "German Bundesliga")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(premierBundesliga))

        val standardBundesliga = createDummyRecord(9992, "Bundesliga")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(standardBundesliga))

        val bundesliga2 = createDummyRecord(9993, "2. Bundesliga")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(bundesliga2))

        val bundesliga2Variant = createDummyRecord(9994, "Bundesliga 2")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(bundesliga2Variant))
    }

    @Test
    fun `test regression club qualifications are tier 1 not tier 3`() {
        val uclQual = createDummyRecord(9981, "UEFA Champions League Qualifying")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(uclQual))

        val uelQual = createDummyRecord(9982, "UEFA Europa League Qualifying")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(uelQual))

        val afcQual = createDummyRecord(9983, "AFC Champions League Qualifiers")
        assertEquals(QualityTier.TIER_1_TOP_DOMESTIC_AND_CONTINENTAL, policy.evaluate(afcQual))
    }

    @Test
    fun `test generic qualification without high confidence tournament is quarantined`() {
        val genericQual = createDummyRecord(9984, "National Qualification Tournament")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(genericQual))
        assertFalse(policy.isAccepted(genericQual))
    }

    @Test
    fun `test generic cup is quarantined`() {
        val localCup = createDummyRecord(9971, "Mizoram Governor Cup")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(localCup))
        assertFalse(policy.isAccepted(localCup))

        val obscureTrophy = createDummyRecord(9972, "FA Trophy")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(obscureTrophy))

        val obscureSuperCup = createDummyRecord(9973, "Regional Super Cup")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(obscureSuperCup))
    }

    @Test
    fun `test generic primera division is quarantined`() {
        // Unlisted Primera Division (e.g., Bolivia) should NOT auto-accept as Tier 1
        val genericPrimera = createDummyRecord(9961, "Bolivia Primera Division")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(genericPrimera))
        assertFalse(policy.isAccepted(genericPrimera))
    }

    @Test
    fun `test whitelisted primera division retains correct tier`() {
        val uruguay = createDummyRecord(1101, "Uruguay Primera Division")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(uruguay))
        assertTrue(policy.isAccepted(uruguay))

        val elSalvador = createDummyRecord(1106, "El Salvador Primera Division")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(elSalvador))
        assertTrue(policy.isAccepted(elSalvador))
    }

    @Test
    fun `test generic super league and pro league are quarantined`() {
        val genericSuperLeague = createDummyRecord(9951, "Malawi Super League")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(genericSuperLeague))

        val genericProLeague = createDummyRecord(9952, "Regional Pro League")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(genericProLeague))
    }

    @Test
    fun `test usl league two is quarantined not tier 2`() {
        val uslTwo = createDummyRecord(9941, "USL League Two")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(uslTwo))
        assertFalse(policy.isAccepted(uslTwo))

        // Whitelisted USL Championship is still Tier 2
        val uslChampionship = createDummyRecord(1117, "USL Championship")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(uslChampionship))
        assertTrue(policy.isAccepted(uslChampionship))
    }

    @Test
    fun `test segunda rfef is quarantined not tier 2`() {
        val segundaRfef = createDummyRecord(9931, "Segunda RFEF")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(segundaRfef))
        assertFalse(policy.isAccepted(segundaRfef))

        // Whitelisted Segunda Division is still Tier 2
        val segundaDiv = createDummyRecord(955, "Segunda Division")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(segundaDiv))
        assertTrue(policy.isAccepted(segundaDiv))
    }

    @Test
    fun `test generic championship is quarantined not tier 2`() {
        val randomChampionship = createDummyRecord(9921, "National Championship")
        assertEquals(QualityTier.QUARANTINE, policy.evaluate(randomChampionship))
        assertFalse(policy.isAccepted(randomChampionship))

        // Unambiguous EFL Championship matches Tier 2 fallback
        val eflChampionship = createDummyRecord(9922, "EFL Championship")
        assertEquals(QualityTier.TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS, policy.evaluate(eflChampionship))
        assertTrue(policy.isAccepted(eflChampionship))
    }
}
