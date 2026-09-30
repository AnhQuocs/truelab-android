package dev.anhquocs.truelab.core.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class MetaResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parse legacy format correctly maps to effective fields`() {
        val jsonString = """
            {
                "current_page": 1,
                "total_page": 5,
                "total_count": 250,
                "page_size": 50
            }
        """.trimIndent()

        val meta = json.decodeFromString<MetaResponse>(jsonString)

        assertEquals(1, meta.currentPage)
        assertEquals(5, meta.effectiveLastPage)
        assertEquals(250, meta.effectiveTotal)
        assertEquals(50, meta.effectivePageSize)
    }

    @Test
    fun `parse new API format correctly maps to effective fields`() {
        val jsonString = """
            {
                "current_page": 2,
                "last_page": 10,
                "total": 1000,
                "per_page": 100
            }
        """.trimIndent()

        val meta = json.decodeFromString<MetaResponse>(jsonString)

        assertEquals(2, meta.currentPage)
        assertEquals(10, meta.effectiveLastPage)
        assertEquals(1000, meta.effectiveTotal)
        assertEquals(100, meta.effectivePageSize)
    }

    @Test
    fun `effectiveLastPage does not fallback to 1 if last_page is provided`() {
        val meta = MetaResponse(lastPage = 4)
        assertEquals(4, meta.effectiveLastPage)
    }

    @Test
    fun `effectiveLastPage fallbacks to total_page if last_page is missing`() {
        val meta = MetaResponse(lastPage = null, totalPage = 3)
        assertEquals(3, meta.effectiveLastPage)
    }

    @Test
    fun `effectiveLastPage fallbacks to 1 if neither is provided to prevent infinite loop`() {
        val meta = MetaResponse(lastPage = null, totalPage = null)
        assertEquals(1, meta.effectiveLastPage)
    }
}
