package dev.anhquocs.truelab.core.domain.match.usecase

import dev.anhquocs.truelab.core.algorithm.searching.LinearSearch
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMatchesUseCaseTest {

    private val useCase = SearchMatchesUseCase()

    private fun createMatch(
        id: Long,
        homeId: Int,
        homeName: String,
        awayId: Int,
        awayName: String,
        startTime: String = "2026-09-23 19:00",
        status: MatchStatus = MatchStatus.SCHEDULED
    ) = Match(
        id = id,
        homeTeam = TeamSummary(id = homeId, name = homeName),
        awayTeam = TeamSummary(id = awayId, name = awayName),
        homeScore = null,
        awayScore = null,
        startTimeDate = startTime,
        status = status
    )

    private val sampleMatches = listOf(
        createMatch(1L, 10, "Arsenal", 20, "Chelsea"),
        createMatch(2L, 30, "Manchester City", 40, "Liverpool"),
        createMatch(3L, 50, "Real Madrid", 60, "Barcelona"),
        createMatch(4L, 70, "Manchester United", 10, "Arsenal")
    )

    @Test
    fun `invoke - matches by home team name`() {
        val result = useCase(sampleMatches, "Real Madrid")
        assertEquals(1, result.size)
        assertEquals(3L, result[0].id)
        assertEquals("Real Madrid", result[0].homeTeam.name)
    }

    @Test
    fun `invoke - matches by away team name`() {
        val result = useCase(sampleMatches, "Chelsea")
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("Chelsea", result[0].awayTeam.name)
    }

    @Test
    fun `invoke - matches team appearing as both home and away`() {
        // "Arsenal" là home trong match 1L và là away trong match 4L
        val result = useCase(sampleMatches, "Arsenal")
        assertEquals(2, result.size)
        assertEquals(listOf(1L, 4L), result.map { it.id })
    }

    @Test
    fun `invoke - is case-insensitive for lowercase, uppercase and mixed`() {
        val lower = useCase(sampleMatches, "liverpool")
        val upper = useCase(sampleMatches, "LIVERPOOL")
        val mixed = useCase(sampleMatches, "LiVeRpOoL")

        assertEquals(1, lower.size)
        assertEquals(2L, lower[0].id)
        assertEquals(lower.map { it.id }, upper.map { it.id })
        assertEquals(lower.map { it.id }, mixed.map { it.id })
    }

    @Test
    fun `invoke - partial substring matching`() {
        // "Manchester" khớp cả "Manchester City" (match 2L) và "Manchester United" (match 4L)
        val result = useCase(sampleMatches, "Manchester")
        assertEquals(2, result.size)
        assertEquals(listOf(2L, 4L), result.map { it.id })
    }

    @Test
    fun `invoke - trims query whitespace`() {
        val result = useCase(sampleMatches, "   Barcelona   ")
        assertEquals(1, result.size)
        assertEquals(3L, result[0].id)
    }

    @Test
    fun `invoke - returns all matches when query is empty`() {
        val result = useCase(sampleMatches, "")
        assertEquals(sampleMatches.size, result.size)
        assertSame(sampleMatches, result)
    }

    @Test
    fun `invoke - returns all matches when query is blank spaces`() {
        val result = useCase(sampleMatches, "    ")
        assertEquals(sampleMatches.size, result.size)
        assertSame(sampleMatches, result)
    }

    @Test
    fun `invoke - returns empty list when no match found`() {
        val result = useCase(sampleMatches, "Bayern Munich")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - returns empty list when matches dataset is empty`() {
        val result = useCase(emptyList(), "Arsenal")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke - preserves immutability and does not mutate input list`() {
        val originalCopy = ArrayList(sampleMatches)
        val result = useCase(sampleMatches, "Manchester")

        assertEquals(4, sampleMatches.size)
        assertEquals(originalCopy, sampleMatches)
        assertEquals(2, result.size)
    }

    @Test
    fun `invoke - uses custom searchAlgorithm when injected`() {
        val customSearchAlgorithm = LinearSearch<Match, String>()
        val customUseCase = SearchMatchesUseCase(searchAlgorithm = customSearchAlgorithm)
        val result = customUseCase(sampleMatches, "Arsenal")
        assertEquals(2, result.size)
    }
}
