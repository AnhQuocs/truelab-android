---
name: android-unit-test-guide
description: Hướng dẫn viết Unit Test, Coroutine Test, Mock Repository và Benchmark thuật toán cho TrueLab.
---

# Kỹ năng Kiểm thử Đơn vị & Benchmark (Unit Test & Benchmark Guide)

Tài liệu này cung cấp các template mẫu để viết Unit Test và Benchmark cho từng tầng logic trong TrueLab.

## 1. Template Test Thuật toán Thuần túy (`:core:algorithm`)

```kotlin
package dev.anhquocs.truelab.core.algorithm.sorting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickSortTest {

    @Test
    fun `sort handles empty list gracefully`() {
        val input = emptyList<Int>()
        val result = QuickSort.sort(input)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `sort handles single element correctly`() {
        val input = listOf(42)
        val result = QuickSort.sort(input)
        assertEquals(listOf(42), result)
    }

    @Test
    fun `sort handles duplicates and preserves order`() {
        val input = listOf(5, 1, 5, 3, 2, 2, 8)
        val result = QuickSort.sort(input)
        assertEquals(listOf(1, 2, 2, 3, 5, 5, 8), result)
    }

    @Test
    fun `sort handles large dataset correctly`() {
        val largeList = (1..10000).shuffled()
        val result = QuickSort.sort(largeList)
        assertEquals((1..10000).toList(), result)
    }
}
```

---

## 2. Template Test ViewModel Coroutines (`:app`)

```kotlin
package dev.anhquocs.truelab.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading then success`() = runTest {
        // Thực thi test với testDispatcher.scheduler.advanceUntilIdle()
    }
}
```

---

## 3. Template Benchmark Đo lường Thời gian Thực thi Thuật toán

```kotlin
package dev.anhquocs.truelab.core.algorithm.benchmark

import kotlin.system.measureNanoTime

object AlgorithmBenchmarkRunner {
    fun benchmark(name: String, warmupRuns: Int = 5, measuredRuns: Int = 20, block: () -> Unit): Double {
        repeat(warmupRuns) { block() }
        val times = (1..measuredRuns).map {
            measureNanoTime { block() }
        }
        val averageMillis = times.average() / 1_000_000.0
        println("Benchmark [$name]: Average = %.4f ms".format(averageMillis))
        return averageMillis
    }
}
```
