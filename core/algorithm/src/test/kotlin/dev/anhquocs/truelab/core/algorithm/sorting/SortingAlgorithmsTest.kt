package dev.anhquocs.truelab.core.algorithm.sorting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Lớp Dummy dùng để kiểm tra tính tổng quát (Generic) của thuật toán.
 */
data class TestItem(val id: Int, val value: Int)

class SortingAlgorithmsTest {

    private val quickSort = QuickSort<TestItem>()
    private val mergeSort = MergeSort<TestItem>()

    // Tiêu chí so sánh
    private val ascendingById = Comparator<TestItem> { a, b -> a.id.compareTo(b.id) }
    private val descendingByValue = Comparator<TestItem> { a, b -> b.value.compareTo(a.value) }

    // Dataset test
    private val emptyDataset = emptyList<TestItem>()
    private val singleElementDataset = listOf(TestItem(1, 100))
    
    private val unsortedDataset = listOf(
        TestItem(3, 30),
        TestItem(1, 10),
        TestItem(4, 40),
        TestItem(2, 20),
        TestItem(5, 50)
    )

    private val reverseSortedDataset = listOf(
        TestItem(5, 50),
        TestItem(4, 40),
        TestItem(3, 30),
        TestItem(2, 20),
        TestItem(1, 10)
    )

    private val allIdenticalDataset = listOf(
        TestItem(1, 10),
        TestItem(1, 10),
        TestItem(1, 10)
    )

    // Dùng để test tính ổn định (Stability)
    // Các phần tử có cùng value = 10, nhưng id khác nhau.
    private val unstableDataset = listOf(
        TestItem(1, 10),
        TestItem(2, 20),
        TestItem(3, 10),
        TestItem(4, 30),
        TestItem(5, 10)
    )

    @Test
    fun `quickSort - sorts empty list`() {
        val result = quickSort.sort(emptyDataset, ascendingById)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mergeSort - sorts empty list`() {
        val result = mergeSort.sort(emptyDataset, ascendingById)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `quickSort - sorts single element`() {
        val result = quickSort.sort(singleElementDataset, ascendingById)
        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `quickSort - sorts unsorted list ascending by id`() {
        val result = quickSort.sort(unsortedDataset, ascendingById)
        assertEquals(listOf(1, 2, 3, 4, 5), result.map { it.id })
    }

    @Test
    fun `mergeSort - sorts reverse sorted list ascending by id`() {
        val result = mergeSort.sort(reverseSortedDataset, ascendingById)
        assertEquals(listOf(1, 2, 3, 4, 5), result.map { it.id })
    }

    @Test
    fun `quickSort - sorts identical elements without throwing error`() {
        val result = quickSort.sort(allIdenticalDataset, ascendingById)
        assertEquals(3, result.size)
        assertEquals(listOf(1, 1, 1), result.map { it.id })
    }

    @Test
    fun `mergeSort - generic sorting descending by value`() {
        val result = mergeSort.sort(unsortedDataset, descendingByValue)
        assertEquals(listOf(50, 40, 30, 20, 10), result.map { it.value })
    }

    @Test
    fun `mergeSort - is STABLE (preserves original order of equal items)`() {
        // Sort descending by value (Có nhiều item value = 10)
        // Array gốc: (1,10), (2,20), (3,10), (4,30), (5,10)
        // Sort theo value giảm dần: (4,30), (2,20), X, Y, Z
        // Vì Stable, X Y Z (các giá trị 10) phải giữ nguyên thứ tự ban đầu là: id=1, id=3, id=5
        val result = mergeSort.sort(unstableDataset, descendingByValue)
        
        // Trích xuất các items có value = 10
        val itemsWithValue10 = result.filter { it.value == 10 }
        
        assertEquals(3, itemsWithValue10.size)
        // Thứ tự id phải giữ đúng: 1 -> 3 -> 5
        assertEquals(listOf(1, 3, 5), itemsWithValue10.map { it.id })
    }

    @Test
    fun `quickSort - can handle large random dataset without StackOverflow`() {
        // Tạo mảng 10,000 items ngẫu nhiên
        val largeList = List(10000) { TestItem(Random.nextInt(1, 50000), Random.nextInt(1, 100)) }
        val result = quickSort.sort(largeList, ascendingById)
        
        // Kiểm tra xem list đã sort đúng chưa (O(n) check)
        for (i in 0 until result.size - 1) {
            assertTrue(result[i].id <= result[i + 1].id)
        }
    }
}
