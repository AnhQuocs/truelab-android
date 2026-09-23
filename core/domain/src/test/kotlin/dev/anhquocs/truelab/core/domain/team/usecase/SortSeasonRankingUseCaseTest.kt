package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.sorting.SortAlgorithm
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SortSeasonRankingUseCaseTest {

    private val useCase = SortSeasonRankingUseCase()

    private fun createRanking(
        teamId: Int,
        position: Int,
        won: Int = 0,
        draw: Int = 0,
        loss: Int = 0,
        goalDiff: Int = 0,
        recently: List<String> = emptyList()
    ) = SeasonRanking(
        teamId = teamId,
        position = position,
        won = won,
        draw = draw,
        loss = loss,
        goalDiff = goalDiff,
        recently = recently
    )

    // ==========================================
    // 1. BASIC CRITERIA TESTS
    // ==========================================

    @Test
    fun `invoke - default criteria sorts by POSITION_ASC`() {
        val r1 = createRanking(teamId = 1, position = 5)
        val r2 = createRanking(teamId = 2, position = 1)
        val r3 = createRanking(teamId = 3, position = 3)

        val result = useCase(listOf(r1, r2, r3))

        assertEquals(listOf(2, 3, 1), result.map { it.teamId })
    }

    @Test
    fun `invoke - sorts by POSITION_ASC ascending correctly`() {
        val r1 = createRanking(teamId = 1, position = 10)
        val r2 = createRanking(teamId = 2, position = 2)
        val r3 = createRanking(teamId = 3, position = 7)

        val result = useCase(listOf(r1, r2, r3), StandingsSortCriteria.POSITION_ASC)

        assertEquals(listOf(2, 3, 1), result.map { it.teamId })
    }

    @Test
    fun `invoke - sorts by GOAL_DIFF_DESC descending correctly`() {
        // goalDiff: +15, +3, 0, -2, -8
        val r1 = createRanking(teamId = 1, position = 1, goalDiff = 3)
        val r2 = createRanking(teamId = 2, position = 2, goalDiff = 15)
        val r3 = createRanking(teamId = 3, position = 3, goalDiff = -8)
        val r4 = createRanking(teamId = 4, position = 4, goalDiff = 0)
        val r5 = createRanking(teamId = 5, position = 5, goalDiff = -2)

        val result = useCase(listOf(r1, r2, r3, r4, r5), StandingsSortCriteria.GOAL_DIFF_DESC)

        assertEquals(listOf(2, 1, 4, 5, 3), result.map { it.teamId })
    }

    @Test
    fun `invoke - sorts by WINS_DESC descending correctly`() {
        val r1 = createRanking(teamId = 1, position = 1, won = 8)
        val r2 = createRanking(teamId = 2, position = 2, won = 15)
        val r3 = createRanking(teamId = 3, position = 3, won = 3)
        val r4 = createRanking(teamId = 4, position = 4, won = 10)

        val result = useCase(listOf(r1, r2, r3, r4), StandingsSortCriteria.WINS_DESC)

        assertEquals(listOf(2, 4, 1, 3), result.map { it.teamId })
    }

    @Test
    fun `invoke - sorts by LOSSES_ASC ascending correctly`() {
        val r1 = createRanking(teamId = 1, position = 1, loss = 5)
        val r2 = createRanking(teamId = 2, position = 2, loss = 1)
        val r3 = createRanking(teamId = 3, position = 3, loss = 9)
        val r4 = createRanking(teamId = 4, position = 4, loss = 3)

        val result = useCase(listOf(r1, r2, r3, r4), StandingsSortCriteria.LOSSES_ASC)

        assertEquals(listOf(2, 4, 1, 3), result.map { it.teamId })
    }

    // ==========================================
    // 2. POINTS_DESC & COMPOSITE TIE-BREAKER TESTS
    // ==========================================

    @Test
    fun `invoke - POINTS_DESC - sorts by totalPoints DESC when points are distinct`() {
        // Points: r1 -> 3*10 = 30, r2 -> 3*12 + 1 = 37, r3 -> 3*5 + 2 = 17
        val r1 = createRanking(teamId = 1, position = 2, won = 10, draw = 0)
        val r2 = createRanking(teamId = 2, position = 1, won = 12, draw = 1)
        val r3 = createRanking(teamId = 3, position = 3, won = 5, draw = 2)

        val result = useCase(listOf(r1, r2, r3), StandingsSortCriteria.POINTS_DESC)

        assertEquals(listOf(2, 1, 3), result.map { it.teamId })
    }

    @Test
    fun `invoke - POINTS_DESC - tie-breaker 1 resolves by goalDiff DESC when points are equal`() {
        // Cùng 20 điểm: team 1 (GD +10) vs team 2 (GD +5)
        val r1 = createRanking(teamId = 1, position = 2, won = 6, draw = 2, goalDiff = 10) // 20 pts
        val r2 = createRanking(teamId = 2, position = 1, won = 6, draw = 2, goalDiff = 5)  // 20 pts

        val result = useCase(listOf(r2, r1), StandingsSortCriteria.POINTS_DESC)

        assertEquals(listOf(1, 2), result.map { it.teamId })
    }

    @Test
    fun `invoke - POINTS_DESC - tie-breaker 2 resolves by won DESC when points and goalDiff are equal`() {
        // Cùng 20 điểm, cùng GD +5:
        // team 1: 6 wins, 2 draws (6*3 + 2 = 20 pts)
        // team 2: 5 wins, 5 draws (5*3 + 5 = 20 pts)
        val r1 = createRanking(teamId = 1, position = 2, won = 6, draw = 2, goalDiff = 5)
        val r2 = createRanking(teamId = 2, position = 1, won = 5, draw = 5, goalDiff = 5)

        val result = useCase(listOf(r2, r1), StandingsSortCriteria.POINTS_DESC)

        assertEquals(listOf(1, 2), result.map { it.teamId })
    }

    @Test
    fun `invoke - POINTS_DESC - tie-breaker 3 resolves by position ASC when points, goalDiff and won are equal`() {
        // Cùng 20 điểm, cùng GD +5, cùng 6 wins:
        // team 1: position = 1 (ưu tiên hơn)
        // team 2: position = 3
        val r1 = createRanking(teamId = 1, position = 1, won = 6, draw = 2, goalDiff = 5)
        val r2 = createRanking(teamId = 2, position = 3, won = 6, draw = 2, goalDiff = 5)

        val result = useCase(listOf(r2, r1), StandingsSortCriteria.POINTS_DESC)

        assertEquals(listOf(1, 2), result.map { it.teamId })
    }

    @Test
    fun `invoke - POINTS_DESC - full composite tie-breaker hierarchy test`() {
        // Bộ 5 đội kiểm tra trật tự phân cấp: Points -> GoalDiff -> Wins -> Position
        // Team A (id 1): 25 pts (Won 8, Draw 1, GD +12, Pos 1) -> Đứng 1 vì nhiều điểm nhất
        // Team B (id 2): 20 pts, GD +10 (Won 6, Draw 2, Pos 2) -> Đứng 2 vì cùng 20 pts nhưng GD cao nhất
        // Team C (id 3): 20 pts, GD +5, Won 6 (Draw 2, Pos 4)   -> Đứng 3 vì cùng 20 pts, GD +5 nhưng nhiều trận thắng hơn Team D
        // Team D (id 4): 20 pts, GD +5, Won 5 (Draw 5, Pos 3)   -> Đứng 4 vì ít trận thắng hơn Team C
        // Team E (id 5): 15 pts (Won 4, Draw 3, GD +2, Pos 5)  -> Đứng 5 vì ít điểm nhất
        val teamA = createRanking(teamId = 1, position = 1, won = 8, draw = 1, goalDiff = 12)
        val teamB = createRanking(teamId = 2, position = 2, won = 6, draw = 2, goalDiff = 10)
        val teamC = createRanking(teamId = 3, position = 4, won = 6, draw = 2, goalDiff = 5)
        val teamD = createRanking(teamId = 4, position = 3, won = 5, draw = 5, goalDiff = 5)
        val teamE = createRanking(teamId = 5, position = 5, won = 4, draw = 3, goalDiff = 2)

        val input = listOf(teamE, teamD, teamB, teamA, teamC)
        val result = useCase(input, StandingsSortCriteria.POINTS_DESC)

        assertEquals(listOf(1, 2, 3, 4, 5), result.map { it.teamId })
    }

    // ==========================================
    // 3. MERGESORT STABILITY
    // ==========================================

    @Test
    fun `invoke - preserves relative order when comparator evaluates two elements as equal (MergeSort stability)`() {
        // Hai đội hoàn toàn bằng nhau ở cả 4 tiêu chí tie-breaker (points, GD, wins, position)
        val r1 = createRanking(teamId = 10, position = 2, won = 5, draw = 5, goalDiff = 4)
        val r2 = createRanking(teamId = 20, position = 2, won = 5, draw = 5, goalDiff = 4)

        val result = useCase(listOf(r1, r2), StandingsSortCriteria.POINTS_DESC)

        // MergeSort bảo toàn trật tự ban đầu: r1 (teamId 10) trước r2 (teamId 20)
        assertEquals(listOf(10, 20), result.map { it.teamId })

        // Nếu đảo ngược thứ tự đầu vào: r2 trước r1
        val resultReversed = useCase(listOf(r2, r1), StandingsSortCriteria.POINTS_DESC)
        assertEquals(listOf(20, 10), resultReversed.map { it.teamId })
    }

    // ==========================================
    // 4. EDGE CASES & IMMUTABILITY
    // ==========================================

    @Test
    fun `invoke - returns same list when input is empty`() {
        val empty = emptyList<SeasonRanking>()
        val result = useCase(empty, StandingsSortCriteria.POINTS_DESC)

        assertTrue(result.isEmpty())
        assertSame(empty, result)
    }

    @Test
    fun `invoke - returns same list when input has singleton element`() {
        val single = listOf(createRanking(teamId = 1, position = 1))
        val result = useCase(single, StandingsSortCriteria.GOAL_DIFF_DESC)

        assertEquals(1, result.size)
        assertSame(single, result)
    }

    @Test
    fun `invoke - preserves immutability and does not mutate input list`() {
        val r1 = createRanking(teamId = 3, position = 3)
        val r2 = createRanking(teamId = 1, position = 1)
        val r3 = createRanking(teamId = 2, position = 2)
        val originalList = listOf(r1, r2, r3)
        val originalCopy = ArrayList(originalList)

        val result = useCase(originalList, StandingsSortCriteria.POSITION_ASC)

        assertEquals(3, originalList.size)
        assertEquals(originalCopy, originalList)
        assertEquals(listOf(1, 2, 3), result.map { it.teamId })
    }

    // ==========================================
    // 5. DELEGATION VERIFICATION
    // ==========================================

    @Test
    fun `invoke - delegates to custom injected sortAlgorithm`() {
        var algorithmCalled = false

        val customAlgorithm = object : SortAlgorithm<SeasonRanking> {
            override fun sort(dataset: List<SeasonRanking>, comparator: Comparator<SeasonRanking>): List<SeasonRanking> {
                algorithmCalled = true
                return dataset.sortedWith(comparator)
            }
        }

        val customUseCase = SortSeasonRankingUseCase(sortAlgorithm = customAlgorithm)
        val rankings = listOf(
            createRanking(teamId = 2, position = 2),
            createRanking(teamId = 1, position = 1)
        )

        val result = customUseCase(rankings, StandingsSortCriteria.POSITION_ASC)

        assertTrue(algorithmCalled)
        assertEquals(listOf(1, 2), result.map { it.teamId })
    }
}
