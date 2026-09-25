# Data D1.4 — Dataset Metadata Tracking

## 1. Objective
Mục tiêu của Data D1.4 là xây dựng hệ thống theo dõi và tổng hợp Dataset Metadata snapshot (`DatasetMetadata`) một cách chính xác, dựa trên dữ liệu thực tế tồn tại trong CSDL Room Database cục bộ (`matches`, `teams`, `odds`, `leagues`, `seasons`), cung cấp DAO aggregation queries, Repository abstraction trong Clean Architecture và tích hợp với pipeline đồng bộ của `DataSyncEngine`.

---

## 2. Scope
Các thành phần được triển khai trong D1.4:
- **Domain Model**: `DatasetMetadata` (Pure Kotlin/JVM) kèm các hằng số `DEFAULT_KEY`, `CURRENT_SCHEMA_VERSION`.
- **Domain Repository Interface**: `DatasetMetadataRepository` (`getMetadata`, `refreshSnapshot`).
- **Room DAO**: `DatasetMetadataDao` với các câu lệnh SQL aggregation trực tiếp (`COUNT(*)`, `MIN(startTimeDate)`, `MAX(startTimeDate)`) và `insertOrUpdate`.
- **Repository Implementation**: `DatasetMetadataRepositoryImpl` bọc thực thi aggregate và snapshot persistence trên `Dispatchers.IO`.
- **Mappers & DI**: Hai chiều Entity $\leftrightarrow$ Domain trong `RoomMappers.kt`, Hilt binding trong `MetadataDataModule.kt` và `DatabaseModule.kt`.
- **DataSyncEngine Integration**: Kích hoạt `refreshSnapshot` sau khi hoàn tất pipeline đồng bộ trận đấu.
- **Unit Tests**: Kiểm thử toàn diện ranh giới dữ liệu rỗng, dữ liệu thực tế, tính idempotent upsert, luồng Flow và tích hợp pipeline sync.

---

## 3. Architecture

### Luồng tổng hợp Metadata (Aggregation Flow):
```text
Database Tables (matches, teams, odds, leagues, seasons)
    ↓
DatasetMetadataDao (SQL Aggregations: COUNT, MIN, MAX)
    ↓
DatasetMetadataRepositoryImpl.refreshSnapshot(timestamp)
    ↓
DatasetMetadata snapshot entity (key = PRIMARY_DATASET, schemaVersion = 2)
    ↓
DatasetMetadataDao.insertOrUpdate(snapshot)
```

### Luồng tích hợp với DataSyncEngine:
```text
DataSyncEngine.syncFullPipelineForDate(...)
    ↓ (Sync Matches & Teams batch thành công)
DatasetMetadataRepository.refreshSnapshot(timestamp)
    ↓
DatasetMetadataDao
```

### Phân định trách nhiệm (Separation of Concerns):
- **`DataSyncEngine`**: Điều phối (orchestration) pipeline đồng bộ từ remote data source vào Room tables. Tuyệt đối không chứa SQL aggregation, không tự đếm counter metadata và không sở hữu logic snapshot.
- **`DatasetMetadataRepository`**: Sở hữu toàn bộ logic nghiệp vụ tính toán snapshot, tổng hợp số liệu và cập nhật `DatasetMetadata`.
- **`DatasetMetadataDao`**: Trực tiếp thực thi các truy vấn SQL tổng hợp trên Room SQLite tables và lưu trữ bản ghi metadata.

---

## 4. Dataset Metadata Model

```kotlin
data class DatasetMetadata(
    val key: String = DEFAULT_KEY,
    val lastSyncTimestamp: Long,
    val totalMatches: Int,
    val totalTeams: Int,
    val totalOddsRecords: Int,
    val totalLeagues: Int,
    val totalSeasons: Int,
    val earliestMatchDate: String? = null,
    val latestMatchDate: String? = null,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    companion object {
        const val DEFAULT_KEY = "PRIMARY_DATASET"
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
```

- **`DEFAULT_KEY`**: `"PRIMARY_DATASET"` — Khóa định danh duy nhất cho bộ dữ liệu chính của ứng dụng.
- **`CURRENT_SCHEMA_VERSION`**: `2` — Phiên bản Room Schema hiện tại (nâng cấp từ D1.1).

---

## 5. Aggregation

Tất cả chỉ số snapshot được tính toán bằng câu lệnh SQL aggregation trực tiếp trong `DatasetMetadataDao`:
- `getTotalMatches()`: `SELECT COUNT(*) FROM matches`
- `getTotalTeams()`: `SELECT COUNT(*) FROM teams`
- `getTotalOddsRecords()`: `SELECT COUNT(*) FROM odds`
- `getTotalLeagues()`: `SELECT COUNT(*) FROM leagues`
- `getTotalSeasons()`: `SELECT COUNT(*) FROM seasons`
- `getEarliestMatchDate()`: `SELECT MIN(startTimeDate) FROM matches WHERE startTimeDate IS NOT NULL AND startTimeDate != ''`
- `getLatestMatchDate()`: `SELECT MAX(startTimeDate) FROM matches WHERE startTimeDate IS NOT NULL AND startTimeDate != ''`

> [!IMPORTANT]
> Metadata snapshot phản ánh tổng số lượng thực tế của **TOÀN BỘ database** hiện tại, không phải chỉ là số lượng bản ghi của một đợt sync đơn lẻ (`SyncSummary`).

---

## 6. Snapshot Refresh

Hàm `refreshSnapshot(timestamp: Long): Result<DatasetMetadata>` trong `DatasetMetadataRepositoryImpl`:
1. Truy vấn các chỉ số tổng hợp thực tế từ `DatasetMetadataDao`.
2. Khởi tạo đối tượng `DatasetMetadataEntity` với:
   - `key = DatasetMetadata.DEFAULT_KEY` (`PRIMARY_DATASET`)
   - `lastSyncTimestamp = timestamp` (thời điểm hoàn tất đồng bộ)
   - `schemaVersion = DatasetMetadata.CURRENT_SCHEMA_VERSION` (`2`)
   - `totalMatches`, `totalTeams`, `totalOddsRecords`, `totalLeagues`, `totalSeasons`
   - `earliestMatchDate`, `latestMatchDate` (hoặc `null` nếu CSDL rỗng)
3. Ghi đè/upsert bản ghi snapshot vào bảng `dataset_metadata`.
4. Trả về `Result.success(domainModel)` hoặc `Result.failure(e)` nếu xảy ra lỗi.

---

## 7. DataSync Integration

Behavior thực tế sau Code Review:
- `DataSyncEngine` inject `DatasetMetadataRepository? = null` thông qua Hilt Constructor Injection.
- Khi hoàn tất sync các batch trận đấu, `DataSyncEngine` kiểm tra:
  - Nếu có callback `onMetadataHook` được truyền vào, ưu tiên thực thi callback đó.
  - Nếu `onMetadataHook == null`, tự động kích hoạt `metadataRepository?.refreshSnapshot(timestamp)`.
- `DataSyncEngine` hoàn toàn không can thiệp vào SQL hay việc tính toán aggregate.
- Đảm bảo 100% backward compatibility với các test double và callers hiện hữu.

---

## 8. Transaction / Atomicity

- **Snapshot Upsert**: `DatasetMetadataDao.insertOrUpdate` áp dụng `OnConflictStrategy.REPLACE` trên khóa chính `key = 'PRIMARY_DATASET'`, đảm bảo thao tác ghi snapshot luôn mang tính nguyên tử (atomic).
- **Ranh giới Transaction**: Quá trình `refreshSnapshot` thực hiện đọc các aggregation queries và ghi 1 bản ghi metadata. Do `DataSyncEngine` đồng bộ dữ liệu theo từng page batch trong database transaction riêng, metadata snapshot được tính toán sau khi toàn bộ các batch đã commit thành công vào DB, đảm bảo snapshot luôn phản ánh trạng thái nhất quán của CSDL.

---

## 9. Tests

Các test case chuyên sâu cho D1.4 trong `DatasetMetadataRepositoryImplTest` và `DataSyncEngineTest`:
1. `refreshSnapshot_emptyDatabase_persistsZeroCountsAndNullDates`: Xác thực CSDL rỗng cho ra tất cả count = 0, date = null, schemaVersion = 2.
2. `refreshSnapshot_populatedDatabase_persistsAggregatedData`: Xác thực CSDL có dữ liệu cho ra đúng các chỉ số tổng và khoảng ngày `earliestMatchDate`/`latestMatchDate`.
3. `refreshSnapshot_upsertBehavior_overwritesPreviousSnapshot`: Xác thực việc cập nhật snapshot mới ghi đè bản ghi cũ trên khóa `PRIMARY_DATASET`.
4. `getMetadata_returnsMappedDomainFlow`: Xác thực `getMetadata` phát ra Flow domain model chính xác.
5. `roomMappers_biDirectionalMapping_preservesAllFields`: Xác thực chuyển đổi 2 chiều Entity $\leftrightarrow$ Domain bảo toàn 100% trường dữ liệu.
6. `syncFullPipelineForDate_withMetadataRepository_triggersRefreshSnapshot`: Xác thực `DataSyncEngine` tự động gọi `refreshSnapshot` khi kết thúc pipeline đồng bộ.

---

## 10. Verification

Kết quả kiểm thử toàn diện trên toàn bộ 4 modules:
- **`:core:algorithm`**: 122/122 PASS
- **`:core:domain`**: 206/206 PASS
- **`:core:data`**: 21/21 PASS (+6 tests mới)
- **`:app`**: 46/46 PASS
- **Tổng Unit Tests**: **395/395 PASS (100%)**
- **Build**: `./gradlew assembleDebug` **BUILD SUCCESSFUL** in 34s

---

## 11. Scope Verification

- **D1.3 Regression**: **NO** (Toàn bộ contract và test của D1.3 hoạt động hoàn hảo).
- **D1.5 Creep**: **NO** (Không có code migration verification hay test migration).
- **D2 Creep**: **NO** (Không có WorkManager, Paging 3, RemoteMediator, scheduled background sync, hay retry backoff).

---

## 12. Files Changed

### Production Sources:
- `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/metadata/model/DatasetMetadata.kt`
- `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/metadata/repository/DatasetMetadataRepository.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/metadata/local/dao/DatasetMetadataDao.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/metadata/local/entity/DatasetMetadataEntity.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/metadata/repository/DatasetMetadataRepositoryImpl.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/metadata/di/MetadataDataModule.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/database/TrueLabDatabase.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/di/DatabaseModule.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt`

### Test Sources:
- `core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/metadata/repository/DatasetMetadataRepositoryImplTest.kt`
- `core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngineTest.kt`
