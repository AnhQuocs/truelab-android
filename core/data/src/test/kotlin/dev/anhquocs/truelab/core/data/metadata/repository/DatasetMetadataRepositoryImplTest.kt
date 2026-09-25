package dev.anhquocs.truelab.core.data.metadata.repository

import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toDomain
import dev.anhquocs.truelab.core.data.local.mapper.RoomMappers.toEntity
import dev.anhquocs.truelab.core.data.metadata.local.dao.DatasetMetadataDao
import dev.anhquocs.truelab.core.data.metadata.local.entity.DatasetMetadataEntity
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatasetMetadataRepositoryImplTest {

    private class FakeDatasetMetadataDao : DatasetMetadataDao {
        val metadataStorage = mutableMapOf<String, DatasetMetadataEntity>()
        var matchesCount = 0
        var teamsCount = 0
        var oddsCount = 0
        var leaguesCount = 0
        var seasonsCount = 0
        var earliestDate: String? = null
        var latestDate: String? = null

        override fun getMetadata(key: String): Flow<DatasetMetadataEntity?> =
            flowOf(metadataStorage[key])

        override fun insertOrUpdate(metadata: DatasetMetadataEntity): Long {
            metadataStorage[metadata.key] = metadata
            return 1L
        }

        override fun getTotalMatches(): Int = matchesCount
        override fun getTotalTeams(): Int = teamsCount
        override fun getTotalOddsRecords(): Int = oddsCount
        override fun getTotalLeagues(): Int = leaguesCount
        override fun getTotalSeasons(): Int = seasonsCount
        override fun getEarliestMatchDate(): String? = earliestDate
        override fun getLatestMatchDate(): String? = latestDate
    }

    private lateinit var fakeDao: FakeDatasetMetadataDao
    private lateinit var repository: DatasetMetadataRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeDatasetMetadataDao()
        repository = DatasetMetadataRepositoryImpl(fakeDao)
    }

    @Test
    fun refreshSnapshot_emptyDatabase_persistsZeroCountsAndNullDates() = runTest {
        val timestamp = 1700000000000L
        val result = repository.refreshSnapshot(timestamp)

        assertTrue(result.isSuccess)
        val metadata = result.getOrNull()
        assertNotNull(metadata)
        assertEquals("PRIMARY_DATASET", metadata?.key)
        assertEquals(timestamp, metadata?.lastSyncTimestamp)
        assertEquals(0, metadata?.totalMatches)
        assertEquals(0, metadata?.totalTeams)
        assertEquals(0, metadata?.totalOddsRecords)
        assertEquals(0, metadata?.totalLeagues)
        assertEquals(0, metadata?.totalSeasons)
        assertNull(metadata?.earliestMatchDate)
        assertNull(metadata?.latestMatchDate)
        assertEquals(2, metadata?.schemaVersion)
    }

    @Test
    fun refreshSnapshot_populatedDatabase_persistsAggregatedData() = runTest {
        fakeDao.matchesCount = 380
        fakeDao.teamsCount = 20
        fakeDao.oddsCount = 1520
        fakeDao.leaguesCount = 1
        fakeDao.seasonsCount = 1
        fakeDao.earliestDate = "2023-08-11 20:00:00"
        fakeDao.latestDate = "2024-05-19 16:00:00"

        val timestamp = 1716134400000L
        val result = repository.refreshSnapshot(timestamp)

        assertTrue(result.isSuccess)
        val metadata = result.getOrThrow()
        assertEquals(380, metadata.totalMatches)
        assertEquals(20, metadata.totalTeams)
        assertEquals(1520, metadata.totalOddsRecords)
        assertEquals(1, metadata.totalLeagues)
        assertEquals(1, metadata.totalSeasons)
        assertEquals("2023-08-11 20:00:00", metadata.earliestMatchDate)
        assertEquals("2024-05-19 16:00:00", metadata.latestMatchDate)
        assertEquals(2, metadata.schemaVersion)
    }

    @Test
    fun refreshSnapshot_upsertBehavior_overwritesPreviousSnapshot() = runTest {
        // Initial snapshot
        repository.refreshSnapshot(1000L)
        assertEquals(1000L, fakeDao.metadataStorage["PRIMARY_DATASET"]?.lastSyncTimestamp)

        // Updated database state & new snapshot
        fakeDao.matchesCount = 50
        repository.refreshSnapshot(2000L)

        assertEquals(1, fakeDao.metadataStorage.size)
        val updated = fakeDao.metadataStorage["PRIMARY_DATASET"]
        assertEquals(2000L, updated?.lastSyncTimestamp)
        assertEquals(50, updated?.totalMatches)
    }

    @Test
    fun getMetadata_returnsMappedDomainFlow() = runTest {
        val entity = DatasetMetadataEntity(
            key = "PRIMARY_DATASET",
            lastSyncTimestamp = 123456789L,
            totalMatches = 10,
            totalTeams = 4,
            totalOddsRecords = 40,
            totalLeagues = 1,
            totalSeasons = 1,
            earliestMatchDate = "2024-01-01",
            latestMatchDate = "2024-01-10",
            schemaVersion = 2
        )
        fakeDao.insertOrUpdate(entity)

        val domain = repository.getMetadata("PRIMARY_DATASET").first()
        assertNotNull(domain)
        assertEquals(123456789L, domain?.lastSyncTimestamp)
        assertEquals(10, domain?.totalMatches)
        assertEquals("2024-01-01", domain?.earliestMatchDate)
    }

    @Test
    fun roomMappers_biDirectionalMapping_preservesAllFields() {
        val domain = DatasetMetadata(
            key = "PRIMARY_DATASET",
            lastSyncTimestamp = 999999L,
            totalMatches = 100,
            totalTeams = 20,
            totalOddsRecords = 500,
            totalLeagues = 2,
            totalSeasons = 3,
            earliestMatchDate = "2023-01-01",
            latestMatchDate = "2023-12-31",
            schemaVersion = 2
        )

        val entity = domain.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(domain, mappedBack)
    }
}
