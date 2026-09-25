# TrueLab — Data D2.3 Report: WorkManager Background Sync

**Phase:** Data D2.3  
**Status:** COMPLETED (Pending Review)  
**Parent Plan:** [docs/plans/data-d2-plan.md](../plans/data-d2-plan.md)  
**Verification:** 469/469 Tests PASS (100%) | `assembleDebug` SUCCESS

---

## 1. Executive Summary

Giai đoạn **Data D2.3** đã hoàn thành việc tích hợp **WorkManager Background Synchronization** cho TrueLab. Đây là sub-phase cuối cùng của **Data D2**, đưa hệ thống đồng bộ dữ liệu vào chu trình nền tự động, định kỳ và bền bỉ, tái sử dụng toàn bộ nền tảng đã xây dựng ở **D2.1 (Resilience & Retry Engine)** và **D2.2 (Cache Freshness Policy)** mà không làm biến dạng ranh giới Clean Architecture.

### Các hạng mục chính đã hoàn thành:
1. **WorkManager & Hilt Work Dependencies**: Thiết lập `androidx.work:work-runtime-ktx:2.10.0`, `androidx.hilt:hilt-work:1.2.0`, và `androidx.work:work-testing:2.10.0` trong [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml), cấu hình chuẩn xác cho `:core:data` và `:app`.
2. **`DataSyncWorker`**: Xây dựng Worker kế thừa `CoroutineWorker` với `@HiltWorker` và `@AssistedInject`, ủy quyền toàn bộ luồng đồng bộ cho `DataSyncEngine`.
3. **`SyncWorkScheduler` & `DefaultSyncWorkScheduler`**: Cung cấp abstraction lập lịch nền định kỳ với Unique Work Name (`TrueLabPeriodicDataSyncWork`), chính sách `ExistingPeriodicWorkPolicy.KEEP` (chống duplicate job), và các ràng buộc phần cứng (`NetworkType.CONNECTED`, `requiresBatteryNotLow(true)`).
4. **Hilt WorkManager Application Initialization**: Triển khai `Configuration.Provider` với `HiltWorkerFactory` trong [`TrueLabApplication`](../../app/src/main/kotlin/dev/anhquocs/truelab/TrueLabApplication.kt), đồng thời gỡ bỏ `WorkManagerInitializer` mặc định trong [`AndroidManifest.xml`](../../app/src/main/AndroidManifest.xml) để hỗ trợ On-demand Initialization.
5. **Đồng bộ Semantics & Hủy tác vụ Coroutine**: Xử lý `CancellationException` đúng chuẩn (không nuốt exception), phân loại lỗi `Result.retry()` vs `Result.failure()` bằng `RetryClassifier`.

---

## 2. Chi tiết Kiến trúc & Ranh giới Module

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                          WORKMANAGER BACKGROUND SYNC                        │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
                     [SyncWorkScheduler] (Unique Periodic Work)
                     - Constraints: Network CONNECTED, Battery Not Low
                     - Policy: ExistingPeriodicWorkPolicy.KEEP
                     - Minimum Interval: 15 minutes
                                       │
                                       ▼
                     [DataSyncWorker] (@HiltWorker / @AssistedInject)
                                       │
                                       ▼
                     [DataSyncEngine] (Sync Coordinator)
                      ├─► [D2.2: CacheFreshnessChecker] (TTL Check)
                      ├─► [D2.1: RetryExecutor] (Backoff + Jitter)
                      └─► [Room DB / DatasetMetadata] (Atomic Transactions)
```

### 2.1 `DataSyncWorker`
- **File:** [`DataSyncWorker.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/DataSyncWorker.kt)
- **Cơ chế hoạt động:**
  - Nhận tham số tùy chọn từ `inputData` (`target_date`, `force_refresh`, `dataset_category`) với fallback an toàn (mặc định lấy ngày hôm nay theo định dạng `yyyy-MM-dd` và category `SCHEDULED_MATCHES`).
  - Gọi `dataSyncEngine.syncFullPipelineForDate(...)`.
  - Nếu kết quả là `SyncResult.Success` $\to$ trả về `Result.success()`.
  - Nếu kết quả là `SyncResult.Failure`:
    - Nếu lỗi là transient (mạng timeout, HTTP 5xx) theo `retryClassifier.isRetryable(...)` $\to$ trả về `Result.retry()`.
    - Nếu lỗi là non-retryable (HTTP 4xx, bad argument) $\to$ trả về `Result.failure()`.
  - Nếu xảy ra `CancellationException` $\to$ rethrow trực tiếp để WorkManager xử lý việc hủy tác vụ một cách tự nhiên.

### 2.2 `SyncWorkScheduler` & `DefaultSyncWorkScheduler`
- **File:** [`SyncWorkScheduler.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/SyncWorkScheduler.kt)
- **Ràng buộc phần cứng (Constraints):**
  - `setRequiredNetworkType(NetworkType.CONNECTED)`: Chỉ chạy khi thiết bị có kết nối mạng.
  - `setRequiresBatteryNotLow(true)`: Chỉ chạy khi pin không ở mức yếu để bảo vệ trải nghiệm người dùng.
- **Tính duy nhất (Unique Work):**
  - Sử dụng `workManager.enqueueUniquePeriodicWork("TrueLabPeriodicDataSyncWork", ExistingPeriodicWorkPolicy.KEEP, request)`.
  - Gọi nhiều lần từ app lifecycle sẽ không tạo ra các worker chạy song song.
- **Chu kỳ lập lịch:**
  - Áp dụng `intervalMinutes.coerceAtLeast(15L)` tuân thủ giới hạn tối thiểu của Android WorkManager.

### 2.3 Dependency Injection (`CrawlerDataModule`)
- **File:** [`CrawlerDataModule.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/di/CrawlerDataModule.kt)
- Binds `DefaultSyncWorkScheduler` vào `SyncWorkScheduler`.
- Cung cấp `WorkManager` singleton instance thông qua `WorkManager.getInstance(context)`.
- Cung cấp explicit `@Provides` methods cho `DataSyncEngine`, `RetryExecutor`, và `DataFreshnessPolicy`.

---

## 3. Ma trận Kiểm thử & Kết quả Test (Test Matrix)

Đã bổ sung **12 unit test cases** mới cho D2.3 (7 tests cho `DataSyncWorkerTest` và 5 tests cho `SyncWorkSchedulerTest`):

| Test Suite | Số lượng Test | Trọng tâm kiểm thử | Kết quả |
| :--- | :---: | :--- | :---: |
| [`DataSyncWorkerTest.kt`](../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/DataSyncWorkerTest.kt) | 7 | Worker success, retryable failure (`Result.retry()`), non-retryable failure (`Result.failure()`), CancellationException rethrow, input data parameters, invalid category fallback, default date fallback | **PASS** |
| [`SyncWorkSchedulerTest.kt`](../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/SyncWorkSchedulerTest.kt) | 5 | Network CONNECTED constraint, BatteryNotLow constraint, 15-minute minimum interval enforcement, custom intervals, unique work cancellation | **PASS** |

### Tổng hợp Full Regression qua các giai đoạn:
- `:core:algorithm`: **122 / 122 PASS**
- `:core:domain`: **206 / 206 PASS**
- `:core:data`: **95 / 95 PASS** *(D1: 35 $\to$ D2.1: 64 $\to$ D2.2: 83 $\to$ D2.3: 95)*
- `:app`: **46 / 46 PASS**
- **TỔNG CỘNG TEST SUITE:** **469 / 469 PASS (100%)**
- **BUILD:** `./gradlew assembleDebug` **SUCCESS**

---

## 4. Các Hạng mục Cố tình Không Triển khai (Out of Scope)

Theo đúng định hướng chiến lược trong [`docs/plans/data-d2-plan.md`](../plans/data-d2-plan.md):
1. **Không đưa WorkManager vào `:core:domain` hay `:core:algorithm`**: Các module này duy trì 100% Pure Kotlin/JVM.
2. **Không triển khai Paging 3**: Dữ liệu ngày hiện tại được xử lý tối ưu qua Room Flow và `LazyColumn`.
3. **Không tạo Foreground Service không cần thiết**: WorkManager định kỳ đáp ứng đầy đủ yêu cầu chạy nền mà không làm phiền người dùng.
4. **Không triển khai Delta-sync protocol**: Chờ backend bổ sung conditional headers (`If-Modified-Since`, `ETag`).
5. **Không thêm Room Schema v3**: Giữ nguyên Room v2 hiện tại.

---

## 5. Danh sách Files Tạo mới & Thay đổi

### Files tạo mới:
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/DataSyncWorker.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/SyncWorkScheduler.kt`
- `core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/DataSyncWorkerTest.kt`
- `core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/SyncWorkSchedulerTest.kt`
- `docs/reports/data-d2.3-workmanager-sync.md`

### Files sửa đổi:
- `gradle/libs.versions.toml`
- `core/data/build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/dev/anhquocs/truelab/TrueLabApplication.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryExecutor.kt`
- `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/di/CrawlerDataModule.kt`
