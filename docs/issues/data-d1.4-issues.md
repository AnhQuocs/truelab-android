# Engineering Issue History: Data D1.4 – Dataset Metadata Tracking

Tài liệu này ghi lại lịch sử kỹ thuật (Engineering History) trong quá trình phát triển và Code Review của **Data D1.4 – Dataset Metadata Tracking** thuộc module `:core:data` và `:core:domain`. Tài liệu phản ánh chi tiết quy trình: **Initial Implementation → Code Review → Issues Found → Root Cause Analysis → Fixes → Verification → Impact**.

---

## 1. Context & Initial Implementation

Trong lần triển khai đầu tiên của Data D1.4 theo [data-d1-plan.md](../plans/data-d1-plan.md):
- **Domain Layer**: Định nghĩa `DatasetMetadata` domain model (Pure Kotlin/JVM) và `DatasetMetadataRepository` interface (`getMetadata`, `refreshSnapshot`).
- **Data Layer**: Xây dựng `DatasetMetadataDao` với các câu lệnh SQL aggregation (`COUNT(*)`, `MIN(startTimeDate)`, `MAX(startTimeDate)`) và `insertOrUpdate`, liên kết qua `DatasetMetadataEntity`.
- **Repository Implementation**: `DatasetMetadataRepositoryImpl` chịu trách nhiệm tổng hợp chỉ số từ DAO, khởi tạo snapshot và lưu trữ vào Room.
- **DataSyncEngine Integration**: Tích hợp cơ chế metadata hook thông qua callback `onMetadataHook` khi pipeline đồng bộ hoàn tất.
- **Unit Tests**: Xây dựng bộ test `DatasetMetadataRepositoryImplTest` kiểm tra aggregation CSDL rỗng, CSDL có dữ liệu, tính idempotent và mapping Flow.

Sau khi hoàn thành bản triển khai ban đầu, phiên Code Review chuyên sâu đã phát hiện 4 vấn đề kỹ thuật cần được tái cấu trúc và chuẩn hóa trước khi nghiệm thu.

---

## 2. Issue 1: DataSyncEngine không tự động kích hoạt refresh snapshot

### 2.1. Hiện tượng (Symptom)
Sau khi `DataSyncEngine` đồng bộ thành công các batch trận đấu và đội bóng từ API về CSDL Room, bản ghi `dataset_metadata` không tự động được cập nhật trừ khi caller truyền tường minh một callback `onMetadataHook`. Nếu caller gọi `syncFullPipelineForDate` thông thường, CSDL lưu dữ liệu mới nhưng metadata snapshot vẫn ở trạng thái cũ hoặc rỗng.

### 2.2. Nguyên nhân cốt lõi (Root Cause)
`DataSyncEngine` ban đầu chỉ thiết kế hook dưới dạng delegate callback:
```kotlin
suspend fun syncFullPipelineForDate(
    date: String,
    onMetadataHook: ((Long) -> Unit)? = null
): SyncResult
```
Do `DataSyncEngine` không sở hữu instance của `DatasetMetadataRepository`, khi `onMetadataHook == null`, pipeline kết thúc mà không phát sinh bất kỳ lời gọi nào tới cơ chế snapshot refresh.

### 2.3. Giải pháp khắc phục (Fix)
- Inject `DatasetMetadataRepository? = null` (optional) vào `DataSyncEngine` qua Hilt Constructor Injection.
- Khi pipeline sync hoàn tất thành công:
  - Nếu `onMetadataHook != null`: ưu tiên thực thi callback của caller (bảo toàn backward compatibility).
  - Nếu `onMetadataHook == null`: tự động ủy quyền thực thi `metadataRepository?.refreshSnapshot(timestamp)`.
- `DataSyncEngine` giữ nguyên vai trò Orchestration thuần túy, không chứa SQL aggregation hay logic nghiệp vụ của metadata.

### 2.4. Xác thực (Verification)
- Bổ sung test `syncFullPipelineForDate_withMetadataRepository_triggersRefreshSnapshot` trong [DataSyncEngineTest.kt](../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngineTest.kt).
- Suite test `:core:data:test` PASS 21/21.

---

## 3. Issue 2: Phân tán hằng số cố định (Magic Constants)

### 3.1. Hiện tượng (Symptom)
Các hằng số quan trọng gồm khóa dataset chính `"PRIMARY_DATASET"` và phiên bản Room Schema `2` bị gán cứng (hardcoded) rải rác ở nhiều file: `DatasetMetadataEntity.kt`, `RoomMappers.kt`, `DatasetMetadataRepositoryImpl.kt` và các file test.

### 3.2. Nguyên nhân cốt lõi (Root Cause)
Thiếu định nghĩa tập trung (Single Source of Truth) tại tầng Domain dẫn đến việc mỗi layer tự định nghĩa hoặc hardcode string literal / integer literal độc lập. Khi schema thay đổi hoặc có nhu cầu đổi key, nguy cơ sai lệch đồng bộ là rất cao.

### 3.3. Giải pháp khắc phục (Fix)
Định nghĩa tập trung các hằng số trong Companion Object của Domain model `DatasetMetadata`:
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
Cập nhật toàn bộ Entity, Mappers, Repository và Test sử dụng `DatasetMetadata.DEFAULT_KEY` và `DatasetMetadata.CURRENT_SCHEMA_VERSION`.

### 3.4. Xác thực (Verification)
Toàn bộ 395 unit tests trên 4 modules biên dịch và chạy thành công mà không còn string/int hardcode.

---

## 4. Issue 3: DAO method dư thừa không được sử dụng (`getMetadataDirect`)

### 4.1. Hiện tượng (Symptom)
Trong `DatasetMetadataDao` tồn tại phương thức `getMetadataDirect(key: String): DatasetMetadataEntity?`.

### 4.2. Nguyên nhân cốt lõi (Root Cause)
Phương thức này được tạo ra trong lúc phác thảo ban đầu cho việc đọc synchronous blocking, nhưng trên thực tế kiến trúc reactive sử dụng `getMetadata(key): Flow<DatasetMetadataEntity?>` và `refreshSnapshot` sử dụng các aggregation queries trực tiếp. Phương thức trở thành Dead Code.

### 4.3. Giải pháp khắc phục (Fix)
Loại bỏ hoàn toàn `getMetadataDirect(key: String)` khỏi `DatasetMetadataDao`, tinh giản API surface của DAO.

### 4.4. Xác thực (Verification)
Biên dịch `:core:data` sạch sẽ, không có cảnh báo unreferenced/dead code.

---

## 5. Issue 4: Room KSP compatibility với Suspend Aggregate Queries

### 5.1. Hiện tượng (Symptom)
Các câu lệnh SQL aggregation trong `DatasetMetadataDao` khai báo dạng `suspend fun`:
```kotlin
@Query("SELECT COUNT(*) FROM matches")
suspend fun getTotalMatches(): Int
```
gây ra xung đột kiểu trả về nguyên thủy trong quá trình sinh mã của Room KSP Java Annotation Processing trên môi trường CI/Gradle.

### 5.2. Nguyên nhân cốt lõi (Root Cause)
Room KSP đối với các hàm trả về scalar primitives (`Int`, `String?`) khi kết hợp với `suspend` có thể tạo ra wrapper `Continuation` phức tạp, không tối ưu cho các truy vấn tính toán nội bộ trong transaction/repository.

### 5.3. Giải pháp khắc phục (Fix)
- Chuyển đổi các câu lệnh SQL aggregation trong DAO sang synchronous blocking methods:
  ```kotlin
  @Query("SELECT COUNT(*) FROM matches")
  fun getTotalMatches(): Int
  ```
- Trong `DatasetMetadataRepositoryImpl`, bao bọc toàn bộ chuỗi truy vấn aggregation và thao tác ghi snapshot bên trong `withContext(Dispatchers.IO)`.

### 5.4. Xác thực (Verification)
- Khắc phục triệt để lỗi biên dịch Room KSP.
- `./gradlew.bat assembleDebug` BUILD SUCCESSFUL in 34s.

---

## 6. Summary of Resolved Issues

| Issue ID | Vấn đề kỹ thuật | Phát hiện tại | Giải pháp kiến trúc | Trạng thái |
|---|---|---|---|---|
| **D1.4-TECH-01** | DataSyncEngine không tự động refresh metadata | Code Review | Inject `DatasetMetadataRepository` + auto-refresh khi `onMetadataHook == null` | **Resolved** |
| **D1.4-TECH-02** | Magic constants (`PRIMARY_DATASET`, `2`) | Code Review | Khởi tạo `DEFAULT_KEY` & `CURRENT_SCHEMA_VERSION` tại Domain Companion Object | **Resolved** |
| **D1.4-TECH-03** | Dead code `getMetadataDirect()` trong DAO | Code Review | Loại bỏ method khỏi DAO | **Resolved** |
| **D1.4-TECH-04** | Room KSP compatibility với suspend aggregation | Code Review | Synchronous DAO methods + `Dispatchers.IO` trong Repository | **Resolved** |

---

## 7. Open Issues

> No unresolved issues were identified during D1.4 review.

Toàn bộ 4 vấn đề kỹ thuật phát hiện trong phiên Code Review đều đã được giải quyết triệt để, kiểm thử hồi quy đạt tỷ lệ 100%. Không còn bất kỳ khiếm khuyết nào tồn đọng trong D1.4.

---

## 8. Future Improvements

Các định hướng nâng cấp trong tương lai (không phải lỗi):
- **Multi-dataset snapshot support**: Hỗ trợ snapshot nhiều bộ dữ liệu độc lập với các `key` động khác nhau khi ứng dụng có tính năng custom dataset import.
- **Incremental Metadata Tracking**: Bổ sung cơ chế cập nhật counter gia tăng (incremental counting) nếu kích thước database phát triển lên hàng triệu bản ghi để giảm tải I/O.

---

## 9. Out of Scope

- **Data D1.5**: Kiểm thử migration tự động giữa các phiên bản Room Schema (`MIGRATION_1_2`) với `MigrationTestHelper`.
- **Data D2**: Tích hợp Jetpack `WorkManager` cho background sync định kỳ, phân trang Paging 3 / `RemoteMediator`, và cơ chế retry exponential backoff.

---

## 10. Final Status & Verification

- **Algorithm Tests**: 122/122 PASS (100%)
- **Domain Tests**: 206/206 PASS (100%)
- **Data Tests**: 21/21 PASS (100%)
- **App Tests**: 46/46 PASS (100%)
- **Total Suite**: **395/395 PASS (100%)**
- **Build Status**: `./gradlew assembleDebug` **SUCCESSFUL**
- **Final Verdict**: **D1.4 REVIEW PASS — READY FOR INTEGRATION**
