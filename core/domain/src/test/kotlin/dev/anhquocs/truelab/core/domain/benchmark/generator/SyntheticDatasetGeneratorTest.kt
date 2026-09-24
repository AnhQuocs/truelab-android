package dev.anhquocs.truelab.core.domain.benchmark.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntheticDatasetGeneratorTest {

    @Test
    fun `1 Generator produces exact requested sizes for 1K 10K and 50K`() {
        val list1K = SyntheticDatasetGenerator.generateRandomIntList(1_000)
        val list10K = SyntheticDatasetGenerator.generateRandomIntList(10_000)
        val list50K = SyntheticDatasetGenerator.generateRandomIntList(50_000)

        assertEquals(1_000, list1K.size)
        assertEquals(10_000, list10K.size)
        assertEquals(50_000, list50K.size)
    }

    @Test
    fun `2 Generator is deterministic and reproducible with identical seed`() {
        val listA = SyntheticDatasetGenerator.generateRandomIntList(100, seed = 12345L)
        val listB = SyntheticDatasetGenerator.generateRandomIntList(100, seed = 12345L)

        assertEquals(listA, listB)
    }

    @Test
    fun `3 Generator produces different sequences with different seeds`() {
        val listA = SyntheticDatasetGenerator.generateRandomIntList(100, seed = 123L)
        val listB = SyntheticDatasetGenerator.generateRandomIntList(100, seed = 456L)

        assertNotEquals(listA, listB)
    }

    @Test
    fun `4 Sorted dataset generator produces strictly ascending ordered list`() {
        val sortedList = SyntheticDatasetGenerator.generateSortedIntList(1_000, seed = 999L)

        assertEquals(1_000, sortedList.size)
        for (i in 0 until sortedList.size - 1) {
            assertTrue("Element at $i should be <= element at ${i + 1}", sortedList[i] <= sortedList[i + 1])
        }
    }

    @Test
    fun `5 Non positive sizes return empty lists safely`() {
        val emptyZero = SyntheticDatasetGenerator.generateRandomIntList(0)
        val emptyNegative = SyntheticDatasetGenerator.generateRandomIntList(-10)
        val emptySortedZero = SyntheticDatasetGenerator.generateSortedIntList(0)

        assertTrue(emptyZero.isEmpty())
        assertTrue(emptyNegative.isEmpty())
        assertTrue(emptySortedZero.isEmpty())
    }
}
