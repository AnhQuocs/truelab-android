package dev.anhquocs.truelab.core.domain.match.usecase

import dev.anhquocs.truelab.core.algorithm.sorting.SortAlgorithm
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria
import dev.anhquocs.truelab.core.domain.match.model.MatchStatus
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SortMatchesUseCaseTest {

    private val useCase = SortMatchesUseCase()

    private fun createMatch(
        id: Long,
        homeScore: Int? = null,
        awayScore: Int? = null,
        startTime: String = "2026-09-23 19:00",
        homeName: String = "HomeTeam",
        awayName: String = "AwayTeam",
        status: MatchStatus = MatchStatus.ENDED
    ) = Match(
        id = id,
        homeTeam = TeamSummary(id = id.toInt() * 10, name = homeName),
        awayTeam = TeamSummary(id = id.toInt() * 10 + 1, name = awayName),
        homeScore = homeScore,
        awayScore = awayScore,
        startTimeDate = startTime,
        status = status
    )

    // ==========================================
    // 1. CRITERIA TESTS
    // ==========================================

    @Test
    fun `invoke - default criteria sorts by START_TIME_ASC`() {
        val m1 = createMatch(id = 1L, startTime = "2026-09-25 20:00")
        val m2 = createMatch(id = 2L, startTime = "2026-09-23 18:00")
        val m3 = createMatch(id = 3L, startTime = "2026-09-24 15:00")

        val result = useCase(listOf(m1, m2, m3))

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `invoke - sorts by START_TIME_ASC ascending correctly`() {
        val m1 = createMatch(id = 1L, startTime = "2026-09-25 20:00")
        val m2 = createMatch(id = 2L, startTime = "2026-09-23 18:00")
        val m3 = createMatch(id = 3L, startTime = "2026-09-24 15:00")

        val result = useCase(listOf(m1, m2, m3), MatchSortCriteria.START_TIME_ASC)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `invoke - sorts by START_TIME_DESC descending correctly`() {
        val m1 = createMatch(id = 1L, startTime = "2026-09-23 18:00")
        val m2 = createMatch(id = 2L, startTime = "2026-09-25 20:00")
        val m3 = createMatch(id = 3L, startTime = "2026-09-24 15:00")

        val result = useCase(listOf(m1, m2, m3), MatchSortCriteria.START_TIME_DESC)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `invoke - sorts by TOTAL_GOALS_DESC descending correctly`() {
        // totalGoals: m1 -> 1, m2 -> 5, m3 -> 3, m4 -> 0
        val m1 = createMatch(id = 1L, homeScore = 1, awayScore = 0)
        val m2 = createMatch(id = 2L, homeScore = 3, awayScore = 2)
        val m3 = createMatch(id = 3L, homeScore = 1, awayScore = 2)
        val m4 = createMatch(id = 4L, homeScore = 0, awayScore = 0)

        val result = useCase(listOf(m1, m2, m3, m4), MatchSortCriteria.TOTAL_GOALS_DESC)

        assertEquals(listOf(2L, 3L, 1L, 4L), result.map { it.id })
    }

    @Test
    fun `invoke - sorts by GOAL_DIFF_DESC using signed goal difference (+1 before -2)`() {
        // Goal difference:
        // mWinBig: 4 - 1 = +3
        // mWinSmall: 2 - 1 = +1
        // mDraw: 1 - 1 = 0
        // mLossSmall: 0 - 1 = -1
        // mLossBig: 0 - 2 = -2
        // Với signed goal difference DESC: +3 > +1 > 0 > -1 > -2
        // ĐẶC BIỆT: +1 (mWinSmall) PHẢI đứng trước -2 (mLossBig). Nếu dùng abs(), |-2|=2 > |+1|=1 sẽ sai.
        val mWinBig = createMatch(id = 1L, homeScore = 4, awayScore = 1)     // GD = +3
        val mLossBig = createMatch(id = 2L, homeScore = 0, awayScore = 2)    // GD = -2
        val mWinSmall = createMatch(id = 3L, homeScore = 2, awayScore = 1)   // GD = +1
        val mLossSmall = createMatch(id = 4L, homeScore = 0, awayScore = 1)  // GD = -1
        val mDraw = createMatch(id = 5L, homeScore = 1, awayScore = 1)       // GD = 0

        val input = listOf(mLossBig, mWinSmall, mLossSmall, mWinBig, mDraw)
        val result = useCase(input, MatchSortCriteria.GOAL_DIFF_DESC)

        assertEquals(listOf(1L, 3L, 5L, 4L, 2L), result.map { it.id })
        // Khẳng định chắc chắn +1 (mWinSmall, id 3) đứng trước -2 (mLossBig, id 2)
        val indexWinSmall = result.indexOfFirst { it.id == 3L }
        val indexLossBig = result.indexOfFirst { it.id == 2L }
        assertTrue(indexWinSmall < indexLossBig)
    }

    @Test
    fun `invoke - sorts by ID_ASC ascending correctly`() {
        val m1 = createMatch(id = 50L)
        val m2 = createMatch(id = 10L)
        val m3 = createMatch(id = 30L)
        val m4 = createMatch(id = 5L)

        val result = useCase(listOf(m1, m2, m3, m4), MatchSortCriteria.ID_ASC)

        assertEquals(listOf(5L, 10L, 30L, 50L), result.map { it.id })
    }

    // ==========================================
    // 2. EQUAL-VALUE & MERGESORT STABILITY
    // ==========================================

    @Test
    fun `invoke - preserves original relative order when values are equal (MergeSort stability)`() {
        // Ba trận đấu có cùng startTimeDate và cùng totalGoals
        val m1 = createMatch(id = 10L, startTime = "2026-09-23 19:00", homeScore = 1, awayScore = 1)
        val m2 = createMatch(id = 20L, startTime = "2026-09-23 19:00", homeScore = 2, awayScore = 0)
        val m3 = createMatch(id = 30L, startTime = "2026-09-23 19:00", homeScore = 0, awayScore = 2)

        val result = useCase(listOf(m1, m2, m3), MatchSortCriteria.START_TIME_ASC)

        // Do startTimeDate bằng nhau, MergeSort bảo toàn trật tự ban đầu: m1 -> m2 -> m3
        assertEquals(listOf(10L, 20L, 30L), result.map { it.id })
    }

    // ==========================================
    // 3. EDGE CASES & IMMUTABILITY
    // ==========================================

    @Test
    fun `invoke - returns same list when input is empty`() {
        val empty = emptyList<Match>()
        val result = useCase(empty, MatchSortCriteria.START_TIME_ASC)

        assertTrue(result.isEmpty())
        assertSame(empty, result)
    }

    @Test
    fun `invoke - returns same list when input has singleton element`() {
        val single = listOf(createMatch(id = 1L))
        val result = useCase(single, MatchSortCriteria.GOAL_DIFF_DESC)

        assertEquals(1, result.size)
        assertSame(single, result)
    }

    @Test
    fun `invoke - preserves immutability and does not mutate input list`() {
        val m1 = createMatch(id = 3L)
        val m2 = createMatch(id = 1L)
        val m3 = createMatch(id = 2L)
        val originalList = listOf(m1, m2, m3)
        val originalCopy = ArrayList(originalList)

        val result = useCase(originalList, MatchSortCriteria.ID_ASC)

        assertEquals(3, originalList.size)
        assertEquals(originalCopy, originalList)
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun `invoke - handles matches with null scores gracefully`() {
        // Match với null scores: totalGoals = 0, goalDifference = 0
        val mScheduled = createMatch(id = 1L, homeScore = null, awayScore = null)
        val mPlayed = createMatch(id = 2L, homeScore = 2, awayScore = 1) // GD = +1, TG = 3

        val resultTG = useCase(listOf(mScheduled, mPlayed), MatchSortCriteria.TOTAL_GOALS_DESC)
        assertEquals(listOf(2L, 1L), resultTG.map { it.id })

        val resultGD = useCase(listOf(mScheduled, mPlayed), MatchSortCriteria.GOAL_DIFF_DESC)
        assertEquals(listOf(2L, 1L), resultGD.map { it.id })
    }

    // ==========================================
    // 4. DELEGATION VERIFICATION
    // ==========================================

    @Test
    fun `invoke - delegates to custom injected sortAlgorithm`() {
        var algorithmCalled = false

        val customAlgorithm = object : SortAlgorithm<Match> {
            override fun sort(dataset: List<Match>, comparator: Comparator<Match>): List<Match> {
                algorithmCalled = true
                return dataset.sortedWith(comparator)
            }
        }

        val customUseCase = SortMatchesUseCase(sortAlgorithm = customAlgorithm)
        val matches = listOf(createMatch(id = 2L), createMatch(id = 1L))

        val result = customUseCase(matches, MatchSortCriteria.ID_ASC)

        assertTrue(algorithmCalled)
        assertEquals(listOf(1L, 2L), result.map { it.id })
    }
}
