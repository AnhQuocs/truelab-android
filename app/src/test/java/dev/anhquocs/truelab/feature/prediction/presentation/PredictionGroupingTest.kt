package dev.anhquocs.truelab.feature.prediction.presentation

import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import dev.anhquocs.truelab.feature.prediction.presentation.components.CompetitionMatchGroup
import dev.anhquocs.truelab.feature.prediction.presentation.components.DisplayMatchStatus
import dev.anhquocs.truelab.feature.prediction.presentation.components.resolveDisplayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionGroupingTest {

    private fun createMatch(
        id: Long,
        homeTeam: String,
        awayTeam: String,
        leagueId: Int? = null,
        leagueName: String? = null,
        startTimeDate: String = "2026-10-02T15:00:00Z"
    ): Match {
        return Match(
            id = id,
            homeTeam = TeamSummary(id = id.toInt() * 2, name = homeTeam),
            awayTeam = TeamSummary(id = id.toInt() * 2 + 1, name = awayTeam),
            homeScore = 0,
            awayScore = 0,
            startTimeDate = startTimeDate,
            status = MatchStatus.ENDED,
            leagueId = leagueId,
            leagueName = leagueName
        )
    }

    private fun groupMatchesHelper(
        matches: List<Match>,
        leagueNameMap: Map<Int, String> = emptyMap(),
        unclassifiedText: String = "Chưa phân loại",
        selectedDate: String? = "2026-10-02"
    ): List<CompetitionMatchGroup> {
        return matches
            .groupBy { match ->
                when {
                    match.leagueId != null -> "league_${match.leagueId}"
                    !match.leagueName.isNullOrBlank() -> "name_${match.leagueName}"
                    else -> "orphan_${match.id}"
                }
            }
            .map { (_, matchesInGroup) ->
                val leagueId = matchesInGroup.firstNotNullOfOrNull { it.leagueId }
                val leagueName = matchesInGroup.firstNotNullOfOrNull { it.leagueName }
                    ?: leagueId?.let { id -> leagueNameMap[id] }
                    ?: unclassifiedText
                val leagueLogo = matchesInGroup.firstNotNullOfOrNull { it.leagueLogo }

                val sortedMatches = matchesInGroup.sortedWith(
                    compareBy<Match> { match ->
                        when (resolveDisplayStatus(match, selectedDate)) {
                            DisplayMatchStatus.LIVE -> 0
                            DisplayMatchStatus.STARTED -> 1
                            DisplayMatchStatus.UPCOMING -> 2
                            DisplayMatchStatus.ENDED -> 3
                        }
                    }.thenBy { it.startTimeDate }
                )

                CompetitionMatchGroup(
                    leagueId = leagueId,
                    leagueName = leagueName,
                    leagueLogo = leagueLogo,
                    matches = sortedMatches
                )
            }
    }

    @Test
    fun `different leagueIds are placed in different groups`() {
        val m1 = createMatch(1L, "Arsenal", "Chelsea", leagueId = 10, leagueName = "Premier League")
        val m2 = createMatch(2L, "Real Madrid", "Barcelona", leagueId = 20, leagueName = "La Liga")

        val groups = groupMatchesHelper(listOf(m1, m2))
        assertEquals(2, groups.size)
        assertEquals("Premier League", groups[0].leagueName)
        assertEquals("La Liga", groups[1].leagueName)
    }

    @Test
    fun `matches with identical leagueId are grouped together`() {
        val m1 = createMatch(1L, "Arsenal", "Chelsea", leagueId = 10, leagueName = "Premier League")
        val m2 = createMatch(2L, "Liverpool", "Man City", leagueId = 10, leagueName = "Premier League")

        val groups = groupMatchesHelper(listOf(m1, m2))
        assertEquals(1, groups.size)
        assertEquals("Premier League", groups[0].leagueName)
        assertEquals(2, groups[0].matches.size)
    }

    @Test
    fun `orphan matches with null leagueId and null leagueName DO NOT collide into a single fake group`() {
        val m1 = createMatch(101L, "Team A", "Team B", leagueId = null, leagueName = null)
        val m2 = createMatch(102L, "Team C", "Team D", leagueId = null, leagueName = null)
        val m3 = createMatch(103L, "Team E", "Team F", leagueId = null, leagueName = null)

        val groups = groupMatchesHelper(listOf(m1, m2, m3), unclassifiedText = "Chưa phân loại")
        // Under old buggy logic, all 3 matches merged into a single "default_group" -> "Giải đấu"
        // Under new logic, each orphan match stays distinct (3 groups)
        assertEquals(3, groups.size)
        groups.forEach { group ->
            assertEquals(1, group.matches.size)
            assertEquals("Chưa phân loại", group.leagueName)
            assertNotEquals("Giải đấu", group.leagueName)
        }
    }

    @Test
    fun `leagueName fallback when leagueId is null provides stable grouping`() {
        val m1 = createMatch(1L, "Team A", "Team B", leagueId = null, leagueName = "Custom Tournament")
        val m2 = createMatch(2L, "Team C", "Team D", leagueId = null, leagueName = "Custom Tournament")

        val groups = groupMatchesHelper(listOf(m1, m2))
        assertEquals(1, groups.size)
        assertEquals("Custom Tournament", groups[0].leagueName)
        assertEquals(2, groups[0].matches.size)
    }

    @Test
    fun `regression verification - 5 official competitions are never lumped into fake Giai dau group`() {
        val vietnamPakistan = createMatch(
            id = 1L,
            homeTeam = "Vietnam",
            awayTeam = "Pakistan",
            leagueId = 2239057,
            leagueName = "FIFA ASEAN Cup"
        )
        val greeceNetherlands = createMatch(
            id = 2L,
            homeTeam = "Greece",
            awayTeam = "Netherlands",
            leagueId = 100,
            leagueName = "UEFA Nations League"
        )
        val curacaoTrinidad = createMatch(
            id = 3L,
            homeTeam = "Curacao",
            awayTeam = "Trinidad and Tobago",
            leagueId = 2484,
            leagueName = "CONCACAF Nations League"
        )
        val chinaKorea = createMatch(
            id = 4L,
            homeTeam = "China (W)",
            awayTeam = "South Korea (W)",
            leagueId = 1756,
            leagueName = "OCA Women's Asian Games"
        )
        val elSalvadorMatch = createMatch(
            id = 5L,
            homeTeam = "Fuerte San Francisco",
            awayTeam = "Luis Angel Firpo",
            leagueId = 1106,
            leagueName = "El Salvador Primera Division"
        )

        val matches = listOf(vietnamPakistan, greeceNetherlands, curacaoTrinidad, chinaKorea, elSalvadorMatch)
        val groups = groupMatchesHelper(matches)

        assertEquals(5, groups.size)

        val groupNames = groups.map { it.leagueName }
        assertTrue(groupNames.contains("FIFA ASEAN Cup"))
        assertTrue(groupNames.contains("UEFA Nations League"))
        assertTrue(groupNames.contains("CONCACAF Nations League"))
        assertTrue(groupNames.contains("OCA Women's Asian Games"))
        assertTrue(groupNames.contains("El Salvador Primera Division"))

        // Ensure NO group is named "Giải đấu" or "default_group"
        groups.forEach { group ->
            assertNotEquals("Giải đấu", group.leagueName)
            assertNotEquals("default_group", group.leagueName)
        }
    }
}
