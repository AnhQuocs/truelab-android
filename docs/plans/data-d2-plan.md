# TrueLab — Data D2 Plan: Resilience, Cache Policy & Background Synchronization

**Phase:** Data D2  
**Status:** DRAFT / PLAN ONLY (Pending Review)  
**Parent Roadmap:** [docs/roadmap.md](../roadmap.md)  
**Baseline Verification:** 409 tests PASS (Algorithm: 122, Domain: 206, Data: 35, App: 46) | `assembleDebug` SUCCESS

---

## 1. Executive Summary & Audit Consistency Alignment

Sau khi hoàn thành **Data D1** (mở rộng Room schema v1 → v2, hỗ trợ League/Season, thiết lập pipeline nguyên tử `DataSyncEngine`, và quản lý `DatasetMetadata`), TrueLab bước vào giai đoạn củng cố độ ổn định, hiệu năng và tự động hóa cho tầng lưu trữ dữ liệu (**Data D2**).

### 1.1 Audit Consistency Refinements (Chuẩn hóa kết luận từ Audit)
Dựa trên rà soát thực tế mã nguồn sau Data D1:
1. **DataSyncEngine Pipeline**: Trong Data D1, hàm `syncLeaguesAndSeasons()` là một extension point nạp dữ liệu cục bộ / cấu hình sẵn. Luồng đồng bộ chính `syncFullPipelineForDate()` chỉ gọi remote API cho Matches (kèm Home/Away Teams), Odds và Rankings. Do đó, D2 **không** coi League/Season là một remote API sync stage đã hoàn thiện.
2. **Loại bỏ các claim định lượng chưa kiểm chứng**: Kế hoạch D2 tập trung vào các tiêu chí kiểm thử định tính và cơ chế kiểm soát lỗi có thể chứng minh được (phân loại lỗi mạng, giới hạn retry, backoff) thay vì đưa ra các ước lượng phần trăm định lượng khi chưa có benchmark thực tế.
3. **Mô tả chính xác hành vi Cache**: Hệ thống hiện tại đã lưu trữ dữ liệu trong Room DB và trả về reactive `Flow`, nhưng **chưa có cơ chế Cache Freshness / TTL / Stale-data Policy** rõ ràng để quyết định khi nào cần kích hoạt đồng bộ từ xa (remote sync) và khi nào nên tái sử dụng dữ liệu cục bộ.

### 1.2 D2 Scope & Phased Architecture
Data D2 được chia thành 3 sub-phase tuần tự:
- **D2.1 — Resilience & Retry Engine**: Phân loại lỗi mạng (transient vs permanent), cơ chế Exponential Backoff với Jitter, và giới hạn số lần thử lại cho `DataSyncEngine`.
- **D2.2 — Cache / Stale Data Policy**: Chính sách độ tươi của dữ liệu (TTL), phát hiện dữ liệu cũ (stale), hỗ trợ force refresh và bảo vệ tính sẵn sàng của cache cục bộ khi gặp sự cố mạng.
- **D2.3 — WorkManager Background Sync**: Lập lịch đồng bộ nền định kỳ qua WorkManager với các ràng buộc mạng (Network Constraints) và tích hợp các chính sách Retry / Cache đã hoàn thiện ở D2.1 và D2.2.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        DATA D2 DEPENDENCY FLOW                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
       [D2.1: Resilience & Retry Engine] (Transient Failure, Backoff)
                                    │
                                    ▼
       [D2.2: Cache / Stale Data Policy] (TTL, Freshness, Force Refresh)
                                    │
                                    ▼
       [D2.3: WorkManager Background Sync] (Periodic Sync, Constraints)
```

---

## 2. Kiến trúc & Ranh giới Module (Module Boundaries)

Để duy trì tính toàn vẹn của Clean Architecture:
- **`:core:domain` (Pure Kotlin/JVM)**: Tuyệt đối **không** phụ thuộc vào Android SDK, WorkManager, OkHttp/Retrofit, Room hoặc bất kỳ cài đặt Retry/Cache cụ thể nào. Giữ nguyên Domain Repository interfaces.
- **`:core:algorithm` (Pure Kotlin/JVM)**: Giữ nguyên 100%, không bị ảnh hưởng bởi Data D2.
- **`:core:data` (Android Library)**: Nơi triển khai toàn bộ logic Retry Engine, Cache Freshness Policy, `DataSyncEngine` orchestration và `CoroutineWorker` của WorkManager.
- **`:app` (Android Application)**: Đóng vai trò entry point cho Hilt `Configuration.Provider` và kích hoạt WorkManager periodic scheduling khi khởi động ứng dụng nếu cần.

---

## 3. Chi tiết các Sub-phases

```text
================================================================================
D2.1 — RESILIENCE & RETRY ENGINE
================================================================================
```

### 3.1.1 Objective
Xây dựng engine xử lý lỗi mạng tự động cho các tác vụ đồng bộ trong `DataSyncEngine`, giúp phân loại chính xác các lỗi tạm thời (transient) và lỗi cố định (non-retryable), áp dụng chiến lược thử lại có kiểm soát (Exponential Backoff kèm Jitter) nhằm tăng độ bền vững của ứng dụng khi gặp môi trường mạng không ổn định.

### 3.1.2 Current Gap
- Hiện tại, `DataSyncEngine` bắt toàn bộ lỗi mạng thông qua `try/catch (e: Exception)` và trả về `SyncResult.Failure(e)` ngay tại lần thất bại đầu tiên.
- Chưa có phân biệt giữa lỗi mạng tạm thời (Timeout, SocketException, HTTP 5xx) và lỗi nghiệp vụ/dữ liệu cố định (HTTP 4xx như 400, 401, 403, 404).
- Chưa có cơ chế retry với backoff khiến tác vụ đồng bộ thất bại tức thì dù kết nối có thể phục hồi sau vài giây.

### 3.1.3 Scope
1. **Network Exception Classifier**:
   - `RetryableError`: `SocketTimeoutException`, `ConnectException`, `UnknownHostException`, `IOException` tạm thời, HTTP 500, 502, 503, 504.
   - `NonRetryableError`: HTTP 400, 401, 403, 404, `JsonDecodingException`, lỗi logic/schema Room.
2. **Retry Policy Configuration**:
   - Định nghĩa `RetryPolicy(maxAttempts: Int = 3, initialDelayMs: Long = 1000L, maxDelayMs: Long = 10000L, factor: Double = 2.0, jitter: Boolean = true)`.
   - **Semantics của `maxAttempts`**: `maxAttempts` là **tổng số lần thực thi (total attempts)**, bao gồm 1 lần gọi ban đầu (initial attempt) + tối đa `(maxAttempts - 1)` lần retry. Ví dụ: `maxAttempts = 3` có nghĩa là 1 lần gọi đầu tiên và tối đa 2 lần retry nếu gặp transient failure.
3. **Retry Executor Utility**:
   - Hàm tiện ích `retryWithBackoff(...)` trong coroutine scope của Data layer để bọc các remote API calls.
4. **DataSyncEngine Integration**:
   - Tích hợp `RetryPolicy` vào các bước gọi Remote API (`matchApi`, `oddsApi`, `rankingApi`).
   - Đảm bảo tính Transactional: Nếu retry cạn kiệt (exhausted sau `maxAttempts` lần thử), toàn bộ batch transaction hiện tại không bị commit dở dang (no partial dirty state).

### 3.1.4 Non-goals
- Không retry vô hạn (unbounded retry).
- Không retry các mã lỗi HTTP 4xx hoặc lỗi parsing JSON.
- Không can thiệp vào OkHttp Interceptor ở mức low-level nếu logic retry mức application-level trong `DataSyncEngine` đáp ứng đủ yêu cầu và dễ kiểm thử hơn.

### 3.1.5 Dependencies
- Sẵn sàng triển khai ngay, chỉ phụ thuộc vào `DataSyncEngine` và Retrofit DTO hiện có.

### 3.1.6 Files / Modules Likely Affected
- `:core:data`:
  - `dev.anhquocs.truelab.core.data.crawler.retry.RetryPolicy.kt` (New)
  - `dev.anhquocs.truelab.core.data.crawler.retry.RetryClassifier.kt` (New)
  - `dev.anhquocs.truelab.core.data.crawler.retry.RetryExecutor.kt` (New)
  - `dev.anhquocs.truelab.core.data.crawler.DataSyncEngine.kt` (Update)
  - Unit tests trong `core/data/src/test/.../crawler/retry/` (New)

### 3.1.7 Acceptance Criteria
- [ ] Xảy ra lỗi HTTP 5xx (500, 502, 503) hoặc Connection Timeout → Tự động retry theo đúng số lần quy định (`maxAttempts` tổng số lần thử).
- [ ] Xảy ra lỗi HTTP 4xx (400, 401, 404) hoặc Parse Error → Thất bại ngay lập tức ở lần thử đầu tiên, không retry lặp lại.
- [ ] Khi retry thành công ở lần thử thứ $k$ ($1 \le k \le \text{maxAttempts}$) → Đồng bộ tiếp tục bình thường và trả về `SyncResult.Success`.
- [ ] Khi retry cạn kiệt (exhausted sau $\text{maxAttempts}$ lần thử thất bại liên tiếp) → Trả về `SyncResult.Failure` chứa nguyên nhân gốc (root cause).
- [ ] Cơ chế Atomic Transaction của Room DB được bảo toàn: khi một batch thất bại sau khi cạn retry, dữ liệu batch đó bị rollback hoàn toàn.

### 3.1.8 Test Strategy
- Unit tests độc lập cho `RetryClassifier` với các loại `Throwable` khác nhau.
- Unit tests cho `RetryExecutor` mô phỏng:
  - 100% thành công lần đầu (attempt 1/maxAttempts).
  - Thất bại lần đầu rồi thành công ở lần thử thứ 2 hoặc 3 (verifying backoff delay & attempts count).
  - Thất bại liên tiếp đủ `maxAttempts` lần thử (exhausted).
  - Gặp non-retryable exception (dừng ngay ở attempt 1).
- Unit tests cho `DataSyncEngine` khi tích hợp `RetryPolicy` với Mock API.

### 3.1.9 Risks & Mitigations
- *Risk*: Delay backoff trong Unit Test làm chậm thời gian chạy test suite.
- *Mitigation*: Sử dụng Coroutine `TestDispatcher` / `StandardTestDispatcher` để virtualize thời gian delay (`advanceTimeBy`).

### 3.1.10 Definition of Done (D2.1)
- `RetryPolicy` & `RetryClassifier` được implement và tích hợp hoàn chỉnh vào `DataSyncEngine`.
- Toàn bộ unit tests mới cho Retry Engine PASS 100%.
- Không gây ảnh hưởng xấu tới 409 tests hiện có.

---

```text
================================================================================
D2.2 — CACHE & STALE DATA POLICY
================================================================================
```

### 3.2.1 Objective
Xây dựng cơ chế xác thực độ tươi của dữ liệu (Cache Freshness & TTL) dựa trên `DatasetMetadata.lastSyncTimestamp` và chính sách TTL cho các nhóm dữ liệu (Matches theo ngày, Odds, Rankings), giúp ứng dụng chủ động nhận diện khi nào dữ liệu đã cũ (stale) cần đồng bộ lại, khi nào dữ liệu còn tươi (fresh) để phục vụ ngay từ Room DB, và hỗ trợ tính năng Force Refresh từ UI.

### 3.2.2 Current Gap
- Hiện tại, `MatchRepositoryImpl` truy vấn trực tiếp từ `MatchDao` (Room Flow), trong khi `DataSyncEngine` nạp dữ liệu khi được gọi thủ công.
- Chưa có policy quy định thời gian sống (TTL - Time To Live) cho từng nhóm dữ liệu (ví dụ: dữ liệu trong ngày cần sync thường xuyên hơn dữ liệu lịch sử đã kết thúc).
- Chưa có cơ chế phối hợp giữa `DatasetMetadataRepository` và `DataSyncEngine` để tự động bỏ qua remote sync khi cache vẫn còn trong thời hạn tươi (fresh).
- *Lưu ý về Schema*: `DatasetMetadata` (Room v2) hiện lưu trữ một `lastSyncTimestamp` chung cho primary dataset. Hệ thống chưa có các timestamp độc lập cho từng bảng/bản ghi riêng lẻ trong Room DB.

### 3.2.3 Scope
1. **Data Freshness / TTL Configuration**:
   - `DataFreshnessPolicy`: Định nghĩa TTL cấu hình được (ví dụ: Default Sync TTL: 15-30 phút, Historical Finished Data: 24 giờ).
2. **Stale Evaluation Engine**:
   - Cơ chế kiểm tra `isCacheStale(lastSyncTimestamp, currentTime, ttl)` dựa trên `DatasetMetadata.lastSyncTimestamp` của primary dataset (hoặc policy theo dataset key).
   - Không mở rộng thêm schema Room Database (giữ nguyên Room schema v2 hiện tại, không thêm migration v3 trong D2).
3. **Repository / Sync Orchestration**:
   - Hỗ trợ cờ `forceRefresh: Boolean = false` trong các phương thức đồng bộ:
     - `forceRefresh = false`: Nếu cache còn tươi (fresh), bỏ qua remote fetch và trả về kết quả thành công ngay.
     - `forceRefresh = true`: Bỏ qua kiểm tra TTL, bắt buộc gọi remote sync và cập nhật timestamp mới.
4. **Cache Fallback Protection**:
   - Khi remote sync thất bại do mất kết nối hoàn toàn, dữ liệu hiện có trong Room DB vẫn khả dụng và được giữ nguyên để phục vụ UI (Offline-first resilient fallback).

### 3.2.4 Non-goals
- **Không tự ý triển khai Delta-sync protocol**: Backend hiện tại (`/sport/v1.0/matches`) chưa hỗ trợ HTTP conditional headers (`If-Modified-Since`, `ETag`) hoặc tham số `updated_since`. Do đó, delta-sync được phân loại là **Future Scope** khi backend có API contract hỗ trợ.
- **Không mở rộng Room Schema v3**: Không thêm các cột timestamp vào từng entity đơn lẻ (`MatchEntity`, `OddsEntity`, v.v.) trong D2; sử dụng `DatasetMetadata.lastSyncTimestamp` hiện có.
- Không nhúng Cache Policy vào trong Domain Entities.

### 3.2.5 Dependencies
- Phụ thuộc vào `DatasetMetadataRepository` (từ Data D1.4) và `DataSyncEngine` (từ Data D1.3 & D2.1).

### 3.2.6 Files / Modules Likely Affected
- `:core:data`:
  - `dev.anhquocs.truelab.core.data.crawler.cache.DataFreshnessPolicy.kt` (New)
  - `dev.anhquocs.truelab.core.data.crawler.cache.CacheFreshnessChecker.kt` (New)
  - `dev.anhquocs.truelab.core.data.crawler.DataSyncEngine.kt` (Update để tích hợp freshness check)
  - `dev.anhquocs.truelab.core.data.match.repository.MatchRepositoryImpl.kt` (Tùy chọn phối hợp refresh)
  - Unit tests trong `core/data/src/test/.../crawler/cache/` (New)

### 3.2.7 Acceptance Criteria
- [ ] Dữ liệu còn tươi (`currentTime - lastSyncTimestamp < TTL`) và `forceRefresh = false` → Không gọi Remote API, không làm tốn tài nguyên mạng.
- [ ] Dữ liệu đã cũ (`currentTime - lastSyncTimestamp >= TTL`) → Kích hoạt remote sync và cập nhật `lastSyncTimestamp` sau khi sync thành công.
- [ ] Yêu cầu `forceRefresh = true` → Luôn kích hoạt remote sync bất kể TTL.
- [ ] Remote sync thất bại → Dữ liệu cũ trong Room DB không bị xóa, cache vẫn phục vụ tốt cho UI.
- [ ] Metadata `lastSyncTimestamp` được cập nhật chính xác và nhất quán trong Room `dataset_metadata`.

### 3.2.8 Test Strategy
- Unit tests cho `CacheFreshnessChecker` với các mốc thời gian: Fresh, Expired/Stale, Zero/Unset timestamp.
- Unit tests tích hợp trong `DataSyncEngine` kiểm chứng:
  - Cache fresh → Mock API không được gọi (`verify(exactly = 0)`).
  - Cache stale → Mock API được gọi và metadata được cập nhật.
  - Force refresh → Mock API luôn được gọi.

### 3.2.9 Risks & Mitigations
- *Risk*: Đồng hồ hệ thống thiết bị bị lệch (System Clock Drift) làm sai lệch tính toán TTL.
- *Mitigation*: Sử dụng timestamp nhất quán từ `System.currentTimeMillis()` hoặc monotonic clock khi kiểm tra độ trôi thời gian.

### 3.2.10 Definition of Done (D2.2)
- Toàn bộ cơ chế kiểm tra Freshness/Stale và Force Refresh hoạt động chính xác dựa trên `DatasetMetadata.lastSyncTimestamp`.
- Unit tests cho Cache Policy PASS 100%.
- Không gây hồi quy lên các test hiện có.

---

```text
================================================================================
D2.3 — WORKMANAGER BACKGROUND SYNC
================================================================================
```

### 3.3.1 Objective
Thiết lập cơ chế đồng bộ nền định kỳ thông qua Android `WorkManager`, đảm bảo dữ liệu giải đấu, trận đấu và tỷ lệ kèo được cập nhật tự động ngay cả khi ứng dụng đang chạy nền hoặc sau khi thiết bị khởi động lại, tuân thủ các điều kiện ràng buộc phần cứng (Network Unmetered/Connected, Battery Not Low).

### 3.3.2 Current Gap
- Hiện chưa có cơ chế background scheduling tự động cho `DataSyncEngine`.
- Chưa có `CoroutineWorker` hoặc cơ chế Background Job Scheduling tự động.
- Chưa tích hợp thư viện `androidx.work:work-runtime-ktx` và `androidx.hilt:hilt-work` vào `core:data` và `app`.

### 3.3.3 Scope
1. **WorkManager Dependencies Setup**:
   - Khai báo `androidx.work:work-runtime-ktx` và `androidx.hilt:hilt-work` trong `gradle/libs.versions.toml`.
   - Cấu hình dependencies cho `:core:data` và `:app`.
2. **TrueLabSyncWorker Implementation**:
   - Xây dựng `DataSyncWorker` kế thừa `CoroutineWorker` với `@HiltWorker` và `@AssistedInject`.
   - Worker ủy quyền toàn bộ tác vụ cho `DataSyncEngine` (đã có Retry Engine từ D2.1 và Cache Policy từ D2.2).
3. **Work Constraints & Scheduling Configuration**:
   - Thiết lập `Constraints` (ví dụ: `NetworkType.CONNECTED`, `requiresBatteryNotLow(true)`).
   - Thiết lập `PeriodicWorkRequest` (chu kỳ tối thiểu 15 phút theo chuẩn Android WorkManager) với `ExistingPeriodicWorkPolicy.KEEP` (Unique Work) để chống tạo trùng lặp tác vụ.
4. **Hilt WorkManager Initialization**:
   - Cấu hình `Custom WorkManager Configuration` trong `:app` (`TrueLabApplication` thực thi `Configuration.Provider`) để Hilt có thể inject dependencies vào Worker.

### 3.3.4 Non-goals
- Không đưa WorkManager vào `:core:domain` hay `:core:algorithm`.
- Không tạo các tác vụ Foreground Service không cần thiết gây tiêu hao pin.

### 3.3.5 Dependencies
- Phụ thuộc vào `DataSyncEngine` (hoàn thiện D2.1 & D2.2) và cấu hình Hilt trong `:app`.

### 3.3.6 Files / Modules Likely Affected
- `gradle/libs.versions.toml` (Thêm workManager & hiltWork)
- `core/data/build.gradle.kts` (Thêm dependencies)
- `app/build.gradle.kts` (Thêm dependencies)
- `:core:data`:
  - `dev.anhquocs.truelab.core.data.crawler.worker.DataSyncWorker.kt` (New)
  - `dev.anhquocs.truelab.core.data.crawler.worker.SyncWorkScheduler.kt` (New)
- `:app`:
  - `dev.anhquocs.truelab.TrueLabApplication.kt` (Implement `Configuration.Provider`)
- Unit tests & Robolectric/Instrumentation tests cho Worker trong `core/data/src/test/.../crawler/worker/`.

### 3.3.7 Acceptance Criteria
- [ ] `DataSyncWorker` khởi tạo thành công qua Hilt Worker Factory và thực thi `doWork()` mà không bị crash.
- [ ] Khi `DataSyncEngine` trả về `SyncResult.Success` → Worker trả về `Result.success()`.
- [ ] Khi `DataSyncEngine` trả về `SyncResult.Failure` do lỗi mạng tạm thời → Worker trả về `Result.retry()`.
- [ ] Đăng ký Unique Periodic Work thành công, không tạo ra các Scheduled Work trùng lặp khi ứng dụng mở lại nhiều lần (`ExistingPeriodicWorkPolicy.KEEP`).
- [ ] Ràng buộc mạng (`NetworkType.CONNECTED`) được áp dụng đúng vào WorkRequest.

### 3.3.8 Test Strategy
- Unit test cho `DataSyncWorker` sử dụng `TestListenableWorkerBuilder` / `TestWorkerBuilder` của `androidx.work:work-testing`.
- Mock `DataSyncEngine` để verify kết quả trả về `Result.success()`, `Result.retry()`, `Result.failure()`.
- Unit test cho `SyncWorkScheduler` đảm bảo các tham số constraints và unique work name được truyền chính xác.

### 3.3.9 Risks & Mitigations
- *Risk*: Xung đột khởi tạo mặc định của WorkManager (Default WorkManager Initializer vs On-demand Hilt Initialization).
- *Mitigation*: Tắt `androidx.startup` WorkManager default initializer trong `AndroidManifest.xml` của `:app` và cài đặt `Configuration.Provider` đúng chuẩn Hilt WorkManager.

### 3.3.10 Definition of Done (D2.3)
- `DataSyncWorker` và `SyncWorkScheduler` hoàn thiện và có unit test bao phủ đầy đủ.
- WorkManager khởi tạo thành công với Hilt trong `:app`.
- Toàn bộ test suite PASS và `assembleDebug` SUCCESS.

---

## 4. Các Hạng mục Ngoài Phạm vi & Tương lai (Out of Scope / Future)

### 4.1 Paging 3 (Optional / Future)
- **Đánh giá**:
  - Màn hình `MatchesScreen` hiện tại hiển thị danh sách trận đấu được lọc theo ngày cụ thể và giải đấu.
  - Số lượng trận đấu trong một ngày thông thường dao động từ vài chục đến khoảng một trăm trận, hoàn toàn nằm trong khả năng xử lý hiệu quả của Room Flow và Jetpack Compose `LazyColumn` mà không gây tràn bộ nhớ.
  - Backend API hỗ trợ `page` và `page_size`, nhưng `DataSyncEngine` đã xử lý phân trang tự động khi fetch toàn bộ dữ liệu theo ngày.
- **Quyết định**: Đưa Paging 3 vào **Optional / Future Scope**. Chỉ xem xét triển khai khi xuất hiện màn hình tra cứu toàn bộ lịch sử trận đấu (Historical All-Matches) hoặc khi có yêu cầu Infinite Scrolling rõ ràng từ Presentation.

### 4.2 Multi-provider Identity Resolution (Future / Out of Scope)
- **Đánh giá**:
  - Backend hiện tại cung cấp canonical IDs thống nhất cho các thực thể Đội bóng (`Team`) và Trận đấu (`Match`).
  - Dữ liệu tỷ lệ kèo (`Odds`) đã hỗ trợ đa nhà cái thông qua thuộc tính `companyId` và `companyName`.
- **Quyết định**: Đưa cơ chế phân giải định danh đa nhà cung cấp (Identity Resolution) vào **Future Scope** khi hệ thống tích hợp thêm nguồn cấp dữ liệu thứ hai ngoài REST API hiện hữu.

### 4.3 Backend Delta Sync (Future Dependency)
- **Đánh giá**:
  - Delta Sync yêu cầu backend hỗ trợ timestamp query (`updated_since`) hoặc conditional headers (`ETag`, `Last-Modified`).
- **Quyết định**: Tạm thời chưa triển khai giao thức delta-sync ở mức protocol; thay vào đó sử dụng Cache Freshness Policy (D2.2) để tối ưu tần suất gọi API.

---

## 5. Kế hoạch Kiểm thử & Định nghĩa Hoàn thành Tổng thể (D2 Overall DoD)

### 5.1 Kim tự tháp Kiểm thử D2
1. **Unit Tests (Core Data)**:
   - `RetryClassifierTest`, `RetryExecutorTest`.
   - `CacheFreshnessCheckerTest`, `DataFreshnessPolicyTest`.
   - `DataSyncEngineResilienceTest` (Tích hợp Retry + Cache).
   - `DataSyncWorkerTest` (WorkManager testing).
2. **Regression Verification**:
   - Toàn bộ 409 test cases nền tảng (Algorithm: 122, Domain: 206, Data: 35, App: 46) phải tiếp tục PASS 100%.

### 5.2 Overall Definition of Done (D2)
Toàn bộ giai đoạn **Data D2** chỉ được coi là HOÀN THÀNH khi:
- [ ] **D2.1** hoàn thành: Resilience & Retry Engine hoạt động ổn định, phân loại lỗi chuẩn xác, unit test PASS.
- [ ] **D2.2** hoàn thành: Cache Freshness & Stale Data Policy hoạt động đúng logic, hỗ trợ force refresh, unit test PASS.
- [ ] **D2.3** hoàn thành: WorkManager Background Sync được thiết lập chuẩn Hilt, unique work được bảo vệ, unit test PASS.
- [ ] **Ranh giới kiến trúc được bảo toàn**: `:core:domain` và `:core:algorithm` giữ 100% Pure Kotlin/JVM (0 phụ thuộc Android/WorkManager).
- [ ] **Full Test Suite PASS**: Số lượng test tăng lên tương ứng với các tính năng mới, 0 bài test nào bị fail hoặc disable.
- [ ] **Build Verification**: `./gradlew assembleDebug` thực thi thành công không có cảnh báo nghiêm trọng.
- [ ] **Issue Tracking**: Không còn bất kỳ technical issue mở nào thuộc phạm vi D2.
