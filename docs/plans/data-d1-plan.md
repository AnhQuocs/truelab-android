# TrueLab — Data D1 Plan: Schema Expansion, League/Season, DataSync & Dataset Metadata

**Phase:** Data D1  
**Status:** DRAFT / PENDING REVIEW  
**Parent Roadmap:** [docs/roadmap.md](../roadmap.md)

---

## 1. Executive Summary & Goals

Sau khi hoàn thành toàn bộ **Domain Roadmap (D1 → D4)** và **Presentation P1**, TrueLab bước vào giai đoạn củng cố và nâng cấp tầng lưu trữ dữ liệu (**Data Layer**).

**Mục tiêu của Data D1:**
1. **Schema Expansion (Room v1 → v2)**: Mở rộng cơ sở dữ liệu SQLite/Room để hỗ trợ đa giải đấu (`League`), đa mùa giải (`Season`), mở rộng `MatchEntity` (thêm `leagueId`, `season`), và thiết lập bảng lưu trữ siêu dữ liệu (`DatasetMetadataEntity`).
2. **Safe Migration (Preserve Data)**: Triển khai Migration tường minh (`MIGRATION_1_2`), tuyệt đối **không** dùng `fallbackToDestructiveMigration()`, bảo toàn 100% dữ liệu đã thu thập của các bảng hiện hữu (`teams`, `matches`, `odds`, `season_rankings`, `predictions`).
3. **League & Season Support**: Xây dựng thực thể dữ liệu và mô hình Domain cho `League` và `Season`, hỗ trợ lọc và phân loại các trận đấu và bảng xếp hạng theo giải đấu / mùa giải.
4. **DataSync Foundation**: Chuẩn hóa quy trình đồng bộ dữ liệu đa tầng trong `DataSyncEngine` theo mô hình giao dịch nguyên tử (Atomic Transaction Pipeline), bảo đảm tính toàn vẹn và idempotency khi nạp dữ liệu.
5. **Dataset Metadata Aggregation**: Cung cấp API và Repository trích xuất metadata tổng quan kho dữ liệu (số lượng đội, trận, kèo, giải đấu, khoảng thời gian, thời điểm cập nhật lần cuối) để phục vụ UI Overview (P2).

---

## 2. Current Data Layer Audit (Hiện Trạng)

### 2.1 Room Database & Entities (Version 1)
- **Database Class**: `TrueLabDatabase` (`version = 1`, `exportSchema = true`).
- **Entities Hiện Hữu**:
  - `TeamEntity`: `id (PK)`, `name`, `logo`, `leagueName`, `eloRating`, `formScore`.
  - `MatchEntity`: `id (PK)`, `homeTeamId (FK)`, `awayTeamId (FK)`, `homeScore`, `awayScore`, `startTimeDate`, `status`. *(Chưa có `leagueId`, `season`)*.
  - `OddsEntity`: `id (PK autogen)`, `matchId (FK)`, `companyId`, `companyName`, `oddsType`, `handicap`, `over`, `under`, `homeWin`, `draw`, `awayWin`, `changeTime`, `marketPhase`. Unique index: `[matchId, companyId, oddsType, changeTime]`.
  - `SeasonRankingEntity`: Composite PK `[matchId, teamId]`, `position`, `won`, `draw`, `loss`, `goalDiff`, `recentlyStr`.
  - `PredictionEntity`: `matchId (PK, FK)`, `algorithmName`, `homeWinProb`, `drawProb`, `awayWinProb`, `predictedOutcome`, `confidenceScore`.

### 2.2 DAOs Hiện Hữu
- `MatchDao`: CRUD và query cơ bản (`getMatchById`, `getMatchesByDate`, `getMatchesByStatus`, `getH2HMatches`, `getRecentMatchesForTeam`). *(Chưa có query theo `leagueId`/`season` và chưa có các hàm aggregate `COUNT(*)`)*.
- `TeamDao`: `insertTeams` (Strategy: `IGNORE`), `getTeamById`, `searchTeams`.
- `OddsDao`: `insertOdds` (Strategy: `REPLACE`), `getOddsHistory`, `getLatestOddsForMatch`.
- `RankingDao`: `insertRankings`, `getRankingsForMatch`, `getLatestSeasonRankings`.
- `PredictionDao`: `savePrediction`, `getPredictionForMatch`.

### 2.3 Remote API & DTOs
- `MatchApi`: `/sport/v1.0/matches` (tham số `date`, `status`, `page`, `page_size`, `sort`).
- `OddsApi`: `/sport/v1.0/matches/{matchId}/odds` & `/sport/v1.0/matches/{matchId}/odds-history`.
- `RankingApi`: `/sport/v1.0/season/{matchId}/ranking`.

### 2.4 DataSyncEngine Hiện Hữu
- Đã có `syncMatchesByDate(date)`, `syncOddsHistoryForMatch(matchId)`, `syncSeasonRankingForMatch(matchId)`.
- **Hạn chế**: Chưa đồng bộ `League`/`Season`, chưa có theo dõi trạng thái / tiến trình đồng bộ (`SyncStatus`), chưa ghi nhận metadata thời gian cập nhật.

---

## 3. Detailed Architecture of Data D1 Sub-phases

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        DATA D1 ARCHITECTURE                            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    ▼                               ▼                               ▼
[D1.1 Schema Expansion]     [D1.2 League/Season]       [D1.3 DataSync Foundation]
 • Room Migration 1 -> 2     • LeagueEntity/Model       • Atomic Transaction Pipeline
 • MatchEntity columns       • SeasonEntity/Model       • Idempotent Batch Ingestion
 • DatasetMetadataEntity     • LeagueDao/SeasonDao      • Sync Error/Status Handling
 • Foreign Keys & Indices    • League/Season Repos      • Dependency Ordering
    │                               │                               │
    └───────────────────────────────┼───────────────────────────────┘
                                    ▼
                        [D1.4 Dataset Metadata]
                         • Metadata Aggregate Queries
                         • DatasetMetadata Model
                         • DatasetMetadataRepository
                                    │
                                    ▼
                        [D1.5 Verification & Migration]
                         • Migration 1->2 Unit Tests
                         • Repository & DAO Unit Tests
                         • Full Regression (374+ tests)
```

---

## 4. D1.1 — Schema Expansion & Room Migration

### 4.1 Thực thể mới (New Entities)

#### 1. `LeagueEntity`
```kotlin
@Entity(
    tableName = "leagues",
    indices = [
        Index(value = ["name"]),
        Index(value = ["country"])
    ]
)
data class LeagueEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val shortName: String? = null,
    val logo: String? = null,
    val country: String? = null,
    val category: String? = null
)
```

#### 2. `SeasonEntity`
```kotlin
@Entity(
    tableName = "seasons",
    foreignKeys = [
        ForeignKey(
            entity = LeagueEntity::class,
            parentColumns = ["id"],
            childColumns = ["leagueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["leagueId"]),
        Index(value = ["year"]),
        Index(value = ["leagueId", "year"], unique = true)
    ]
)
data class SeasonEntity(
    @PrimaryKey
    val id: String, // Format: "${leagueId}_${year}" hoặc UUID
    val leagueId: Int,
    val name: String, // Ví dụ: "2023-2024" hoặc "2024"
    val year: Int,
    val isCurrent: Boolean = false,
    val startDate: String? = null,
    val endDate: String? = null
)
```

#### 3. `DatasetMetadataEntity`
```kotlin
@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey
    val key: String = "PRIMARY_DATASET",
    val lastSyncTimestamp: Long,
    val totalMatches: Int,
    val totalTeams: Int,
    val totalOddsRecords: Int,
    val totalLeagues: Int,
    val totalSeasons: Int,
    val earliestMatchDate: String?,
    val latestMatchDate: String?,
    val schemaVersion: Int = 2
)
```

### 4.2 Cập nhật Thực thể Hiện Hữu (Modified Entities)

#### `MatchEntity`
Thêm 2 cột mới (nullable để tương thích hoàn toàn dữ liệu cũ):
```kotlin
@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["homeTeamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["awayTeamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LeagueEntity::class,
            parentColumns = ["id"],
            childColumns = ["leagueId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["homeTeamId"]),
        Index(value = ["awayTeamId"]),
        Index(value = ["homeTeamId", "awayTeamId"]),
        Index(value = ["startTimeDate"]),
        Index(value = ["status"]),
        Index(value = ["leagueId"]),
        Index(value = ["season"]),
        Index(value = ["leagueId", "season"])
    ]
)
data class MatchEntity(
    @PrimaryKey
    val id: Long,
    val homeTeamId: Int,
    val awayTeamId: Int,
    val homeScore: Int?,
    val awayScore: Int?,
    val startTimeDate: String,
    val status: String,
    val leagueId: Int? = null,    // MỚI: Liên kết giải đấu
    val season: String? = null     // MỚI: Mùa giải (vd: "2023-2024")
)
```

### 4.3 Kế hoạch Migration 1 → 2 (`MIGRATION_1_2`)
Cung cấp migration rõ ràng qua SQLite DDL:
1. `CREATE TABLE IF NOT EXISTS leagues (...)`
2. `CREATE TABLE IF NOT EXISTS seasons (...)`
3. `CREATE TABLE IF NOT EXISTS dataset_metadata (...)`
4. `ALTER TABLE matches ADD COLUMN leagueId INTEGER REFERENCES leagues(id) ON DELETE SET NULL;`
5. `ALTER TABLE matches ADD COLUMN season TEXT;`
6. `CREATE INDEX IF NOT EXISTS index_matches_leagueId ON matches (leagueId);`
7. `CREATE INDEX IF NOT EXISTS index_matches_season ON matches (season);`
8. `CREATE INDEX IF NOT EXISTS index_matches_leagueId_season ON matches (leagueId, season);`

---

## 5. D1.2 — League & Season Domain Models, DAOs & Repositories

### 5.1 Domain Models (`:core:domain`)
- **`League`**:
  ```kotlin
  data class League(
      val id: Int,
      val name: String,
      val shortName: String? = null,
      val logo: String? = null,
      val country: String? = null,
      val category: String? = null
  )
  ```
- **`Season`**:
  ```kotlin
  data class Season(
      val id: String,
      val leagueId: Int,
      val name: String,
      val year: Int,
      val isCurrent: Boolean = false,
      val startDate: String? = null,
      val endDate: String? = null
  )
  ```
- **`Match` (Mở rộng trường không làm vỡ code cũ)**:
  ```kotlin
  data class Match(
      val id: Long,
      val homeTeam: TeamSummary,
      val awayTeam: TeamSummary,
      val homeScore: Int?,
      val awayScore: Int?,
      val startTimeDate: String,
      val status: MatchStatus,
      val leagueId: Int? = null,
      val season: String? = null
  )
  ```

### 5.2 DAOs (`:core:data`)
- **`LeagueDao`**:
  - `insertLeagues(leagues: List<LeagueEntity>): LongArray`
  - `getLeagues(): Flow<List<LeagueEntity>>`
  - `getLeagueById(leagueId: Int): Flow<LeagueEntity?>`
- **`SeasonDao`**:
  - `insertSeasons(seasons: List<SeasonEntity>): LongArray`
  - `getSeasonsByLeague(leagueId: Int): Flow<List<SeasonEntity>>`
  - `getCurrentSeason(leagueId: Int): Flow<SeasonEntity?>`
- **Mở rộng `MatchDao`**:
  - `getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<MatchWithTeams>>`
  - `getMatchesByLeague(leagueId: Int): Flow<List<MatchWithTeams>>`

### 5.3 Repositories (`:core:domain` interface & `:core:data` impl)
- `LeagueRepository`:
  - `getLeagues(): Flow<List<League>>`
  - `getLeagueDetail(leagueId: Int): Flow<League?>`
  - `getSeasons(leagueId: Int): Flow<List<Season>>`
- `MatchRepository` (Mở rộng):
  - `getMatchesByLeagueAndSeason(leagueId: Int, season: String): Flow<List<Match>>`

---

## 6. D1.3 — DataSync Foundation

### 6.1 Thứ tự phụ thuộc Đồng bộ (Ingestion Dependency Pipeline)

Để đảm bảo toàn vẹn khóa ngoại (Foreign Keys), dữ liệu phải được nạp theo đúng thứ tự:

$$\text{1. Leagues \& Seasons} \longrightarrow \text{2. Teams} \longrightarrow \text{3. Matches} \longrightarrow \text{4. Rankings \& Odds} \longrightarrow \text{5. Dataset Metadata Snapshot}$$

### 6.2 Chiến lược Giao dịch Nguyên tử (Atomic Transactions)
- **Batch Processing**: Xử lý nạp dữ liệu theo từng lô (Batch/Page) gói trọn trong `database.withTransaction { ... }`.
- **Idempotent Upsert Strategy**:
  - `TeamDao`: `OnConflictStrategy.IGNORE` để bảo vệ các chỉ số đã tính toán (`eloRating`, `formScore`).
  - `LeagueDao` / `SeasonDao` / `MatchDao`: `OnConflictStrategy.REPLACE` để cập nhật trạng thái tỉ số và lịch đấu mới nhất.
  - `OddsDao`: `OnConflictStrategy.REPLACE` kết hợp unique index `[matchId, companyId, oddsType, changeTime]` loại bỏ trùng lặp snapshot thời gian thực.
  - `RankingDao`: `OnConflictStrategy.REPLACE` trên composite PK `[matchId, teamId]`.

### 6.3 Trạng thái Đồng bộ (`SyncResult` & `SyncProgress`)
Xây dựng model quản lý trạng thái nạp dữ liệu:
```kotlin
sealed interface SyncStatus {
    object Idle : SyncStatus
    data class InProgress(val progressPercent: Int, val currentStep: String) : SyncStatus
    data class Success(val itemsSynced: Int, val timestamp: Long) : SyncStatus
    data class Failure(val error: Throwable, val timestamp: Long) : SyncStatus
}
```

---

## 7. D1.4 — Dataset Metadata

### 7.1 Mục tiêu Nghiệp vụ
Trả lời chính xác 2 câu hỏi cốt lõi cho UI/System:
1. *Kho dữ liệu hiện tại đang có quy mô như thế nào?* (Số giải, số mùa, số đội, số trận, số bản ghi tỷ lệ kèo).
2. *Dữ liệu được cập nhật lần cuối vào thời điểm nào và khoảng thời gian diễn ra các trận đấu là bao lâu?*

### 7.2 Domain Model
```kotlin
data class DatasetMetadata(
    val lastSyncTimestamp: Long,
    val totalMatches: Int,
    val totalTeams: Int,
    val totalOddsRecords: Int,
    val totalLeagues: Int,
    val totalSeasons: Int,
    val earliestMatchDate: String?,
    val latestMatchDate: String?,
    val isPopulated: Boolean get() = totalMatches > 0
)
```

### 7.3 DAO Aggregate Queries
Thêm các query đếm và tổng hợp nhanh trên Room SQLite:
- `MatchDao.getMatchCount(): Flow<Int>`
- `MatchDao.getDateRange(): Flow<DateRangeTuple>` (`MIN(startTimeDate)`, `MAX(startTimeDate)`)
- `TeamDao.getTeamCount(): Flow<Int>`
- `OddsDao.getOddsCount(): Flow<Int>`
- `LeagueDao.getLeagueCount(): Flow<Int>`
- `SeasonDao.getSeasonCount(): Flow<Int>`

### 7.4 `DatasetMetadataRepository`
Cung cấp Flow phản ánh real-time kích thước và trạng thái dataset trong Room:
```kotlin
interface DatasetMetadataRepository {
    fun getDatasetMetadata(): Flow<DatasetMetadata>
    suspend fun refreshMetadataSnapshot()
}
```

---

## 8. D1.5 — Migration & Verification Strategy

### 8.1 Bảo Toàn Dữ Liệu (Zero Destructive Fallback)
- **Tuyệt đối cấm** sử dụng `fallbackToDestructiveMigration()` trong `DatabaseModule.kt`.
- Sử dụng `addMigrations(MIGRATION_1_2)` trong `Room.databaseBuilder`.

### 8.2 Kiểm Thử Migration Tự Động (`MigrationTest`)
Viết `MigrationTest` sử dụng `androidx.room.testing.MigrationTestHelper`:
1. Tạo database version 1 và chèn dữ liệu mẫu (teams, matches v1, odds, rankings, predictions).
2. Chạy migration lên version 2 (`MIGRATION_1_2`).
3. Kiểm tra tính toàn vẹn:
   - Tất cả dữ liệu cũ vẫn nguyên vẹn.
   - Các bảng mới (`leagues`, `seasons`, `dataset_metadata`) đã được tạo với schema chuẩn.
   - Cột mới `leagueId`, `season` trong `matches` có giá trị `NULL` cho dữ liệu cũ và nhận giá trị mới khi chèn.
   - Các index mới hoạt động đúng.

---

## 9. Data Flow Overview

```text
[Remote API (REST)]
       │
       ▼
[Retrofit Services (MatchApi, OddsApi, RankingApi, LeagueApi)]
       │ (JSON Response)
       ▼
[Retrofit DTOs]
       │
       ▼
[DataSyncEngine (Orchestrator)]
       │ (Transaction boundary: League -> Team -> Match -> Odds -> Ranking)
       ▼
[Room Mappers (RoomMappers.kt)]
       │ (DTO -> Entity)
       ▼
[Room DAOs & Database (TrueLabDatabase v2)]
  ├── leagues (LeagueDao)
  ├── seasons (SeasonDao)
  ├── teams (TeamDao)
  ├── matches (MatchDao)
  ├── odds (OddsDao)
  ├── season_rankings (RankingDao)
  ├── predictions (PredictionDao)
  └── dataset_metadata (MetadataDao)
       │
       ▼
[Repository Implementations (:core:data)]
  ├── MatchRepositoryImpl
  ├── TeamRepositoryImpl
  ├── OddsRepositoryImpl
  ├── LeagueRepositoryImpl
  ├── PredictionRepositoryImpl
  └── DatasetMetadataRepositoryImpl
       │ (Entity -> Domain Model)
       ▼
[Domain Repositories & UseCases (:core:domain)]
       │
       ▼
[Presentation ViewModels & StateFlows (:app)]
```

---

## 10. Duplication & Normalization Audit

### 10.1 Xử Lý Trùng Lặp (Deduplication Rules)
1. **Teams**: Khi sync danh sách trận đấu từ API, một đội bóng có thể xuất hiện nhiều lần (vừa đá sân nhà, vừa đá sân khách). `DataSyncEngine` deduplicate theo `id` trước khi chèn (`distinctBy { it.id }`), và `TeamDao` sử dụng `OnConflictStrategy.IGNORE` để không ghi đè điểm số Elo/Form.
2. **Matches**: Sử dụng `OnConflictStrategy.REPLACE` theo `id: Long` để cập nhật tỉ số mới khi trận đấu kết thúc.
3. **Odds Records**: Tỷ lệ kèo biến động theo thời gian. Unique index `[matchId, companyId, oddsType, changeTime]` bảo đảm không chèn trùng snapshot cùng thời điểm.
4. **Rankings**: Composite PK `[matchId, teamId]` đảm bảo mỗi đội chỉ có 1 vị trí xếp hạng cho 1 trận đấu cụ thể.
5. **Leagues & Seasons**: `SeasonEntity` dùng unique index `[leagueId, year]` để đảm bảo mỗi mùa giải của một giải đấu là duy nhất.

---

## 11. D1 vs D2 Boundary (Scope Clarification)

| Thành phần / Tính năng | Thuộc Data D1 | Thuộc Data D2 (Future) |
| :--- | :---: | :---: |
| **Room Schema Migration 1 → 2** | **Xong trong D1** | Không |
| **Bảng League, Season, Metadata** | **Xong trong D1** | Không |
| **DataSyncEngine Pipeline cơ bản & Transaction** | **Xong trong D1** | Không |
| **Dataset Metadata Repository & Aggregation** | **Xong trong D1** | Không |
| **Background WorkManager Recurring Sync** | Không | **D2** |
| **Paging 3 Streaming RemoteMediator** | Không | **D2** |
| **Multi-Provider Identity Conflict Resolution** | Không | **D2** |
| **Advanced In-Memory Cache Optimization** | Không | **D2** |

---

## 12. Implementation Breakdown & Task Sequence

Data D1 được chia thành 5 sub-tasks tuần tự, độc lập và có thể kiểm thử riêng biệt:

### Task D1.1 — Schema Expansion & Room Migration (v1 → v2)
- **Mục tiêu**: Định nghĩa các Entity mới (`LeagueEntity`, `SeasonEntity`, `DatasetMetadataEntity`), cập nhật `MatchEntity`, viết `MIGRATION_1_2` và nâng version database lên 2.
- **Files chính**:
  - `core/data/.../league/local/entity/LeagueEntity.kt` (New)
  - `core/data/.../ranking/local/entity/SeasonEntity.kt` (hoặc `season/local/entity/SeasonEntity.kt`) (New)
  - `core/data/.../metadata/local/entity/DatasetMetadataEntity.kt` (New)
  - `core/data/.../match/local/entity/MatchEntity.kt` (Modified)
  - `core/data/.../local/database/TrueLabDatabase.kt` (Modified - version 2)
  - `core/data/.../local/database/Migrations.kt` (New)
  - `core/data/.../local/di/DatabaseModule.kt` (Modified)
- **Acceptance Criteria**: Schema compile thành công, file schema `2.json` được sinh ra tự động qua KSP Room.

---

### Task D1.2 — League & Season Domain Models, DAOs & Repositories
- **Mục tiêu**: Xây dựng toàn bộ tầng Domain models và Data repository cho League và Season. Mở rộng `MatchDao` hỗ trợ query theo League/Season.
- **Files chính**:
  - `core/domain/.../league/model/League.kt` & `Season.kt` (New)
  - `core/domain/.../league/repository/LeagueRepository.kt` (New)
  - `core/domain/.../match/model/Match.kt` (Modified - thêm `leagueId`, `season`)
  - `core/domain/.../match/repository/MatchRepository.kt` (Modified)
  - `core/data/.../league/local/dao/LeagueDao.kt` & `SeasonDao.kt` (New)
  - `core/data/.../match/local/dao/MatchDao.kt` (Modified)
  - `core/data/.../local/mapper/RoomMappers.kt` (Modified)
  - `core/data/.../league/repository/LeagueRepositoryImpl.kt` (New)
  - `core/data/.../match/repository/MatchRepositoryImpl.kt` (Modified)
  - `core/data/.../league/di/LeagueDataModule.kt` (New)
- **Acceptance Criteria**: Query danh sách giải đấu, mùa giải và lọc trận đấu theo giải đấu hoạt động chính xác.

---

### Task D1.3 — DataSync Foundation & Transaction Pipeline
- **Mục tiêu**: Nâng cấp `DataSyncEngine` với thứ tự nạp dữ liệu chuẩn, xử lý transaction nguyên tử cho từng batch, cơ chế deduplication và cập nhật metadata snapshot.
- **Files chính**:
  - `core/data/.../crawler/DataSyncEngine.kt` (Modified)
  - `core/data/.../crawler/model/SyncStatus.kt` (New)
- **Acceptance Criteria**: Đồng bộ theo ngày hoặc theo giải đấu bảo đảm tính toàn vẹn, không xảy ra lỗi foreign key constraint, bảo toàn điểm số Elo/Form của đội bóng.

---

### Task D1.4 — Dataset Metadata Aggregation
- **Mục tiêu**: Triển khai `DatasetMetadataRepository` và DAO aggregate queries cung cấp số liệu thống kê kho dữ liệu theo thời gian thực.
- **Files chính**:
  - `core/domain/.../metadata/model/DatasetMetadata.kt` (New)
  - `core/domain/.../metadata/repository/DatasetMetadataRepository.kt` (New)
  - `core/data/.../metadata/local/dao/DatasetMetadataDao.kt` (New)
  - `core/data/.../metadata/repository/DatasetMetadataRepositoryImpl.kt` (New)
  - `core/data/.../metadata/di/MetadataDataModule.kt` (New)
- **Acceptance Criteria**: Trả về chính xác số lượng trận, đội, tỷ lệ kèo, giải đấu và thời điểm sync gần nhất từ Room DB.

---

### Task D1.5 — Migration & Data Layer Verification
- **Mục tiêu**: Viết Unit Tests cho Room Migration (1 → 2), DAOs mới, Repositories mới, DataSyncEngine và chạy full regression suite.
- **Files chính**:
  - `core/data/src/test/kotlin/.../local/database/MigrationTest.kt` (New)
  - `core/data/src/test/kotlin/.../league/repository/LeagueRepositoryTest.kt` (New)
  - `core/data/src/test/kotlin/.../metadata/repository/DatasetMetadataRepositoryTest.kt` (New)
  - `core/data/src/test/kotlin/.../crawler/DataSyncEngineTest.kt` (New)
  - `docs/reports/data-d1-final.md` (New Report)
- **Acceptance Criteria**: Tất cả unit tests mới PASS, Migration test PASS 100%, toàn bộ 374 unit test baseline của hệ thống giữ vững không regression, `assembleDebug` SUCCESS.

---

## 13. Test Strategy & Baseline Preservation

### 13.1 Regression Baseline
Trước khi bắt đầu Data D1:
- `:core:algorithm`: **122/122 PASS** (Frozen)
- `:core:domain`: **206/206 PASS**
- `:app`: **46/46 PASS**
- **Tổng Unit Tests**: **374/374 PASS (100%)**

### 13.2 Mục tiêu Kiểm Thử Data D1
- Thêm ~20–30 Unit Tests cho Data Layer (`core:data`):
  1. `MigrationTest`: Xác thực migration từ schema 1 lên schema 2 với dữ liệu mẫu.
  2. `RoomMappersTest`: Kiểm tra chuyển đổi 2 chiều DTO ↔ Entity ↔ Domain cho League, Season, Metadata.
  3. `LeagueRepositoryTest` & `MatchRepositoryFilterTest`: Kiểm tra các câu truy vấn lọc giải đấu / mùa giải.
  4. `DatasetMetadataRepositoryTest`: Kiểm tra tính toán aggregate `COUNT(*)` và cập nhật timestamp.
  5. `DataSyncEngineTest`: Kiểm tra thứ tự gọi API, transaction rollback khi có lỗi và idempotency khi nạp trùng.

---

## 14. Risks & Mitigation

| Rủi Ro | Mức Độ | Biện Pháp Giảm Thiểu |
| :--- | :---: | :--- |
| **Data Loss khi nâng cấp Room** | CAO | Tuyệt đối không dùng `fallbackToDestructiveMigration()`. Viết `MigrationTest` kiểm tra bảo toàn dữ liệu trước khi merge. |
| **Foreign Key Constraint Failure khi Sync** | TRUNG BÌNH | Tuân thủ nghiêm ngặt thứ tự nạp: `League/Season` → `Team` → `Match` → `Ranking/Odds`. |
| **Overwriting Elo/Form khi Sync Matches** | CAO | `TeamDao.insertTeams` sử dụng `OnConflictStrategy.IGNORE` để giữ nguyên các chỉ số tính toán nội bộ của TrueLab. |
| **Duplicate Binding trong Hilt DI** | THẤP | Kiểm tra kỹ các Module trong `:core:data` và `DomainUseCaseModule` trong `:app`, tránh bind trùng. |
| **Hiệu năng chậm khi đếm Aggregate `COUNT(*)`** | THẤP | Sử dụng Indexed columns và cache snapshot trong `DatasetMetadataEntity` sau mỗi đợt sync lớn. |

---

## 15. Definition of Done (DoD) for Data D1

Data D1 được coi là **DONE** khi đáp ứng đầy đủ các tiêu chí sau:
1. Room Database đã nâng lên Version 2 với đầy đủ bảng `leagues`, `seasons`, `dataset_metadata` và 2 cột mới trong `matches`.
2. `MIGRATION_1_2` hoạt động ổn định và được kiểm chứng bằng automated `MigrationTest`.
3. Mô hình Domain `League`, `Season`, `DatasetMetadata` và các repository interface tương ứng được thiết kế thuần Kotlin/JVM không phụ thuộc Android SDK.
4. `DataSyncEngine` hỗ trợ đồng bộ an toàn có transaction và cập nhật metadata.
5. Bộ lọc `leagueId` và `season` trên `MatchDao` và `MatchRepository` hoạt động chính xác.
6. Toàn bộ Unit Tests mới trong `:core:data` PASS.
7. Toàn bộ 374 Unit Tests hiện hữu của `:core:algorithm`, `:core:domain`, `:app` PASS 100% (Zero regression).
8. Gradle task `assembleDebug` BUILD SUCCESSFUL.
9. Báo cáo hoàn thành `docs/reports/data-d1-final.md` được phê duyệt qua Code Review.
