package dev.anhquocs.truelab.core.algorithm.searching

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Lớp Dummy dùng để kiểm tra tính tổng quát (Generic) của thuật toán,
 * đảm bảo tuyệt đối không import Domain Model từ module khác.
 */
data class TestTeam(val id: Int, val name: String, val points: Double)

class SearchingAlgorithmsTest {

    private val linearSearchById = LinearSearch<TestTeam, Int>()
    private val binarySearchById = BinarySearch<TestTeam, Int>()

    private val emptyDataset = emptyList<TestTeam>()
    
    // Dataset CHƯA SORTED (Cho Linear Search)
    private val unsortedDataset = listOf(
        TestTeam(3, "Chelsea", 60.5),
        TestTeam(1, "Arsenal", 75.0),
        TestTeam(4, "Liverpool", 70.0),
        TestTeam(2, "Man City", 80.0),
        TestTeam(4, "Liverpool Duplicate", 65.0) // Test duplicate key
    )

    // Dataset ĐÃ SORTED theo ID Tăng Dần (Cho Binary Search)
    private val sortedDataset = listOf(
        TestTeam(1, "Arsenal", 75.0),
        TestTeam(2, "Man City", 80.0),
        TestTeam(3, "Chelsea", 60.5),
        TestTeam(4, "Liverpool", 70.0),
        TestTeam(5, "Man Utd", 55.0)
    )

    // ==========================================
    // LINEAR SEARCH TESTS
    // ==========================================

    @Test
    fun `linearSearch - returns -1 for empty list`() {
        val result = linearSearchById.search(emptyDataset, 1) { it.id }
        assertEquals(-1, result)
    }

    @Test
    fun `linearSearch - returns correct index for exact match`() {
        // Tìm "Arsenal" (id = 1) -> Vị trí thứ 2 trong unsorted (index 1)
        val result = linearSearchById.search(unsortedDataset, 1) { it.id }
        assertEquals(1, result)
    }

    @Test
    fun `linearSearch - returns first occurrence for duplicate values`() {
        // Có 2 đội có id = 4, Linear Search phải trả về cái đầu tiên gặp được (index 2)
        val result = linearSearchById.search(unsortedDataset, 4) { it.id }
        assertEquals(2, result)
    }

    @Test
    fun `linearSearch - returns -1 when target not found`() {
        val result = linearSearchById.search(unsortedDataset, 99) { it.id }
        assertEquals(-1, result)
    }

    @Test
    fun `linearSearch - searchPartial - finds string containing keyword`() {
        val stringSearch = LinearSearch<TestTeam, String>()
        
        // Tìm chữ "Man" -> Sẽ trúng "Man City" (index 3)
        val result = stringSearch.searchPartial(unsortedDataset) { it.name.contains("Man") }
        assertEquals(3, result)
    }

    // ==========================================
    // BINARY SEARCH TESTS
    // ==========================================

    @Test
    fun `binarySearch - returns -1 for empty list`() {
        val result = binarySearchById.search(emptyDataset, 1) { it.id }
        assertEquals(-1, result)
    }

    @Test
    fun `binarySearch - returns correct index in a sorted list`() {
        // Tìm "Chelsea" (id = 3) -> Vị trí thứ 3 (index 2)
        val result = binarySearchById.search(sortedDataset, 3) { it.id }
        assertEquals(2, result)
    }

    @Test
    fun `binarySearch - returns correct index for first element`() {
        // Tìm "Arsenal" (id = 1) -> index 0
        val result = binarySearchById.search(sortedDataset, 1) { it.id }
        assertEquals(0, result)
    }

    @Test
    fun `binarySearch - returns correct index for last element`() {
        // Tìm "Man Utd" (id = 5) -> index 4
        val result = binarySearchById.search(sortedDataset, 5) { it.id }
        assertEquals(4, result)
    }

    @Test
    fun `binarySearch - returns -1 when target is smaller than all elements (Out of bounds left)`() {
        val result = binarySearchById.search(sortedDataset, 0) { it.id }
        assertEquals(-1, result)
    }

    @Test
    fun `binarySearch - returns -1 when target is greater than all elements (Out of bounds right)`() {
        val result = binarySearchById.search(sortedDataset, 99) { it.id }
        assertEquals(-1, result)
    }

    @Test
    fun `binarySearch - returns -1 when target not found within bounds`() {
        // Test với một số thập phân (Points)
        val binarySearchByPoints = BinarySearch<TestTeam, Double>()
        val sortedByPointsDataset = sortedDataset.sortedBy { it.points }
        
        // Có 55.0, 60.5, 70.0, 75.0, 80.0
        // Tìm 65.0 (không có)
        val result = binarySearchByPoints.search(sortedByPointsDataset, 65.0) { it.points }
        assertEquals(-1, result)
    }
}
