package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.searching.BinarySearch
import dev.anhquocs.truelab.core.algorithm.searching.LinearSearch
import dev.anhquocs.truelab.core.algorithm.searching.SearchAlgorithm
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTeamsUseCaseTest {

    private val useCase = SearchTeamsUseCase()

    // Dataset chưa sắp xếp
    private val unsortedTeams = listOf(
        TeamSummary(3, "Chelsea"),
        TeamSummary(1, "Arsenal"),
        TeamSummary(5, "Liverpool"),
        TeamSummary(2, "Manchester City"),
        TeamSummary(4, "Manchester United")
    )

    // Dataset đã sắp xếp theo ID tăng dần
    private val sortedTeams = listOf(
        TeamSummary(1, "Arsenal"),
        TeamSummary(2, "Manchester City"),
        TeamSummary(3, "Chelsea"),
        TeamSummary(4, "Manchester United"),
        TeamSummary(5, "Liverpool")
    )

    // ==========================================
    // 1. searchByName TESTS (Linear Partial Search)
    // ==========================================

    @Test
    fun `searchByName - matches exact name`() {
        val result = useCase.searchByName(unsortedTeams, "Arsenal")
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Arsenal", result[0].name)
    }

    @Test
    fun `searchByName - matches partial substring`() {
        val result = useCase.searchByName(unsortedTeams, "Manchester")
        assertEquals(2, result.size)
        assertEquals(listOf(2, 4), result.map { it.id })
    }

    @Test
    fun `searchByName - is case-insensitive`() {
        val lower = useCase.searchByName(unsortedTeams, "chelsea")
        val upper = useCase.searchByName(unsortedTeams, "CHELSEA")
        val mixed = useCase.searchByName(unsortedTeams, "ChElSeA")

        assertEquals(1, lower.size)
        assertEquals(3, lower[0].id)
        assertEquals(lower.map { it.id }, upper.map { it.id })
        assertEquals(lower.map { it.id }, mixed.map { it.id })
    }

    @Test
    fun `searchByName - trims query whitespace`() {
        val result = useCase.searchByName(unsortedTeams, "   Liverpool   ")
        assertEquals(1, result.size)
        assertEquals(5, result[0].id)
    }

    @Test
    fun `searchByName - returns all teams when query is empty`() {
        val result = useCase.searchByName(unsortedTeams, "")
        assertEquals(unsortedTeams.size, result.size)
        assertSame(unsortedTeams, result)
    }

    @Test
    fun `searchByName - returns all teams when query is blank spaces`() {
        val result = useCase.searchByName(unsortedTeams, "   ")
        assertEquals(unsortedTeams.size, result.size)
        assertSame(unsortedTeams, result)
    }

    @Test
    fun `searchByName - returns empty list when no team matches`() {
        val result = useCase.searchByName(unsortedTeams, "Juventus")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchByName - returns empty list when teams dataset is empty`() {
        val result = useCase.searchByName(emptyList(), "Arsenal")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchByName - preserves immutability and does not mutate input`() {
        val originalCopy = ArrayList(unsortedTeams)
        val result = useCase.searchByName(unsortedTeams, "Manchester")

        assertEquals(5, unsortedTeams.size)
        assertEquals(originalCopy, unsortedTeams)
        assertEquals(2, result.size)
    }

    // ==========================================
    // 2. findById TESTS with UNSORTED dataset (LinearSearch)
    // ==========================================

    @Test
    fun `findById - unsorted - finds first element in list`() {
        // ID = 3 nằm ở vị trí đầu tiên trong unsortedTeams
        val result = useCase.findById(unsortedTeams, 3, isSortedById = false)
        assertNotNull(result)
        assertEquals(3, result?.id)
        assertEquals("Chelsea", result?.name)
    }

    @Test
    fun `findById - unsorted - finds middle element in list`() {
        // ID = 5 nằm ở vị trí thứ 3 trong unsortedTeams
        val result = useCase.findById(unsortedTeams, 5, isSortedById = false)
        assertNotNull(result)
        assertEquals(5, result?.id)
        assertEquals("Liverpool", result?.name)
    }

    @Test
    fun `findById - unsorted - finds last element in list`() {
        // ID = 4 nằm ở vị trí cuối cùng trong unsortedTeams
        val result = useCase.findById(unsortedTeams, 4, isSortedById = false)
        assertNotNull(result)
        assertEquals(4, result?.id)
        assertEquals("Manchester United", result?.name)
    }

    @Test
    fun `findById - unsorted - returns null when id does not exist`() {
        val result = useCase.findById(unsortedTeams, 999, isSortedById = false)
        assertNull(result)
    }

    @Test
    fun `findById - unsorted - returns null for empty list`() {
        val result = useCase.findById(emptyList(), 1, isSortedById = false)
        assertNull(result)
    }

    // ==========================================
    // 3. findById TESTS with SORTED dataset (BinarySearch)
    // ==========================================

    @Test
    fun `findById - sorted - finds first element via binary search`() {
        // ID = 1 ở index 0
        val result = useCase.findById(sortedTeams, 1, isSortedById = true)
        assertNotNull(result)
        assertEquals(1, result?.id)
        assertEquals("Arsenal", result?.name)
    }

    @Test
    fun `findById - sorted - finds middle element via binary search`() {
        // ID = 3 ở index 2
        val result = useCase.findById(sortedTeams, 3, isSortedById = true)
        assertNotNull(result)
        assertEquals(3, result?.id)
        assertEquals("Chelsea", result?.name)
    }

    @Test
    fun `findById - sorted - finds last element via binary search`() {
        // ID = 5 ở index 4
        val result = useCase.findById(sortedTeams, 5, isSortedById = true)
        assertNotNull(result)
        assertEquals(5, result?.id)
        assertEquals("Liverpool", result?.name)
    }

    @Test
    fun `findById - sorted - returns null when id is smaller than min ID (out of bounds left)`() {
        val result = useCase.findById(sortedTeams, 0, isSortedById = true)
        assertNull(result)
    }

    @Test
    fun `findById - sorted - returns null when id is larger than max ID (out of bounds right)`() {
        val result = useCase.findById(sortedTeams, 100, isSortedById = true)
        assertNull(result)
    }

    @Test
    fun `findById - sorted - returns null when id is missing within bounds`() {
        // Tạo tập dữ liệu có lỗ hổng ID: 1, 2, 4, 5 (thiếu 3)
        val gapDataset = listOf(
            TeamSummary(1, "Arsenal"),
            TeamSummary(2, "Man City"),
            TeamSummary(4, "Man United"),
            TeamSummary(5, "Liverpool")
        )
        val result = useCase.findById(gapDataset, 3, isSortedById = true)
        assertNull(result)
    }

    @Test
    fun `findById - sorted - returns null for empty list`() {
        val result = useCase.findById(emptyList(), 1, isSortedById = true)
        assertNull(result)
    }

    @Test
    fun `findById - preserves immutability and does not mutate input list`() {
        val copy = ArrayList(sortedTeams)
        useCase.findById(sortedTeams, 3, isSortedById = true)
        assertEquals(copy, sortedTeams)
    }

    // ==========================================
    // 4. DELEGATION VERIFICATION
    // ==========================================

    @Test
    fun `searchTeamsUseCase - delegates to custom injected algorithms`() {
        var linearIdCalled = false
        var binaryIdCalled = false

        val customLinearId = object : SearchAlgorithm<TeamSummary, Int> {
            override fun search(dataset: List<TeamSummary>, target: Int, selector: (TeamSummary) -> Int): Int {
                linearIdCalled = true
                return dataset.indexOfFirst { selector(it) == target }
            }
        }
        val customBinaryId = object : SearchAlgorithm<TeamSummary, Int> {
            override fun search(dataset: List<TeamSummary>, target: Int, selector: (TeamSummary) -> Int): Int {
                binaryIdCalled = true
                return dataset.indexOfFirst { selector(it) == target }
            }
        }

        val customUseCase = SearchTeamsUseCase(
            linearSearchByName = LinearSearch(),
            linearSearchById = customLinearId,
            binarySearchById = customBinaryId
        )

        // Khi isSortedById = false -> phải gọi linearSearchById
        val res1 = customUseCase.findById(unsortedTeams, 3, isSortedById = false)
        assertTrue(linearIdCalled)
        assertEquals(3, res1?.id)

        // Khi isSortedById = true -> phải gọi binarySearchById
        val res2 = customUseCase.findById(sortedTeams, 3, isSortedById = true)
        assertTrue(binaryIdCalled)
        assertEquals(3, res2?.id)
    }
}
