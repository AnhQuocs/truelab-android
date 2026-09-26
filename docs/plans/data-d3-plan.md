# TrueLab — Kế Hoạch Kỹ Thuật Data Phase D3: Initial Data Bootstrap & Runtime Sync

Tài liệu này xác lập kế hoạch kỹ thuật, thiết kế luồng dữ liệu và tiêu chí nghiệm thu cho **Data Phase D3 (Initial Data Bootstrap & Runtime Sync)** trong dự án **TrueLab**, nhằm kết nối toàn bộ hạ tầng Data D2 đã hoàn thành vào runtime ứng dụng thực tế, giải quyết triệt để tình trạng cơ sở dữ liệu Room rỗng khi người dùng cài mới hoặc mở app.

---

## 1. Mục Tiêu của Data Phase D3

1. **Kích hoạt Cơ chế Đồng bộ Dữ liệu Lần đầu (Initial One-Time Sync)**: Tự động phát hiện trạng thái cài mới (Fresh Install / DB rỗng) và kích hoạt nạp dữ liệu nền thông qua WorkManager mà không làm gián đoạn UI.
2. **Tích hợp Lập lịch Chạy nền Định kỳ (Periodic Sync Runtime Integration)**: Đưa `SyncWorkScheduler.schedulePeriodicSync()` vào vòng đời khởi động của ứng dụng (`TrueLabApplication`).
3. **Đồng bộ Kiến trúc & Tái sử dụng Tối đa Hạ tầng Data D2**: Tái sử dụng 100% các thành phần đã được kiểm thử: `DataSyncWorker`, `DataSyncEngine`, `RetryExecutor`, `CacheFreshnessChecker`, `DataFreshnessPolicy`, `DatasetMetadataRepository`.
4. **Làm rõ và Xử lý Giới hạn League/Season Dữ liệu**: Xác định hiện trạng nguồn dữ liệu giải đấu từ remote API, thiết lập ranh giới trách nhiệm và open issues mà không hardcode dữ liệu giả.

---

## 2. Phân Tích Hiện Trạng & Nguyên Nhân Gốc Rễ (Root Cause Analysis)

### 2.1 Hiện trạng Quan sát
- Ứng dụng khởi động mượt mà, nhưng toàn bộ các màn hình hiển thị số liệu rỗng:
  - `HomeScreen`: Tổng số trận = 0, Tổng số đội = 0, Tỷ lệ cược = 0, Lần đồng bộ cuối = "—".
  - `MatchesScreen`: Danh sách trận đấu rỗng (`emptyList()`).
  - `TeamsScreen`: Bảng xếp hạng rỗng (`emptyList()`).
  - `DatasetMetadata`: Chưa có bản ghi snapshot nào được lưu vào Room.

### 2.2 Nguyên nhân Kỹ thuật Đã Xác Minh
1. **Chưa có Mã nguồn Kích hoạt Sync Lần đầu (Missing Initial Sync Trigger)**:
   - `TrueLabApplication` chỉ mới cấu hình `Configuration.Provider` cho `HiltWorkerFactory`, chưa có hàm `onCreate()` để điều phối sync.
   - `MainActivity` chỉ đóng vai trò render UI Compose, không gọi sync.
   - Các ViewModels (`HomeViewModel`, `MatchesViewModel`) chỉ quan sát (`observe`) Room Flow từ Repository theo mô hình Offline-First, không tự ý gọi network.
2. **`SyncWorkScheduler` chưa được đưa vào Runtime**:
   - Lớp `DefaultSyncWorkScheduler` đã triển khai và đạt 5/5 unit tests, nhưng chưa có nơi nào trong runtime app thực sự gọi `schedulePeriodicSync()`.
3. **Hạn chế Nguồn Dữ liệu League/Season từ Backend**:
   - Backend API hiện tại (`MatchApi`, `OddsApi`, `RankingApi`) **chưa cung cấp endpoint danh mục giải đấu** (`/sport/v1.0/leagues`) và DTO `MatchRecord` hiện không chứa trường `league_id` hay `season`.
   - `DataSyncEngine.syncLeaguesAndSeasons()` tồn tại nhưng không có nguồn remote API để nạp tự động.

---

## 3. Kiến Trúc Luồng Đồng Bộ Runtime (Runtime Sync Architecture)

Hệ thống Data D3 tuân thủ nguyên tắc **Single Source of Worker Execution**: Cả Initial Sync và Periodic Sync đều hội tụ về duy nhất một worker `DataSyncWorker` và một động cơ `DataSyncEngine`.

```text
                             [TrueLabApplication.onCreate()]
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    │                                               │
                    ▼ (Nếu chưa có dữ liệu)                         ▼ (Luôn đăng ký)
       [Initial One-Time Work]                           [Periodic Background Sync]
    (UniqueWork: "TrueLabInitialSync", KEEP)        (UniqueWork: "TrueLabPeriodicDataSyncWork", KEEP)
                    │                                               │
                    └───────────────────────┬───────────────────────┘
                                            │
                                            ▼
                                   [DataSyncWorker]
                                            │
                                            ▼
                                   [DataSyncEngine]
                     (D2.1 RetryExecutor + D2.2 CacheFreshnessPolicy)
                                            │
                                            ▼
                                [Room Database Storage]
                    (Teams -> Matches -> Odds -> DatasetMetadata)
                                            │
                                            ▼
                           [Repository & ViewModel Flow]
                                            │
                                            ▼
                                   [Compose UI Render]
```

---

## 4. Chi Tiết Kỹ Thuật Từng Sub-Task

### 4.1 Sub-task D3.1 — Initial Sync & Runtime Trigger

#### Mục tiêu:
Khi ứng dụng khởi chạy lần đầu trên thiết bị người dùng (hoặc sau khi Clear Data), hệ thống tự động phát hiện và kích hoạt một tác vụ đồng bộ nền duy nhất.

#### Thiết kế Kỹ thuật:
1. **Cơ chế Phát hiện Lần đầu Khởi chạy (First-Sync Detection)**:
   - Mở rộng `SyncWorkScheduler` với phương thức `scheduleInitialSync(forceRefresh: Boolean = false)`.
   - Kiểm tra điều kiện: Sử dụng `DatasetMetadataDao` / `DatasetMetadataRepository`. Nếu `lastSyncTimestamp <= 0L` hoặc `metadata == null`, kích hoạt Initial Sync.
2. **Cấu hình WorkManager OneTimeWorkRequest**:
   - **Unique Work Name**: `SyncWorkScheduler.INITIAL_SYNC_WORK_NAME = "TrueLabInitialDataSyncWork"`.
   - **ExistingWorkPolicy**: `ExistingWorkPolicy.KEEP` — Đảm bảo nếu người dùng mở/đóng app nhiều lần khi initial sync đang chạy, WorkManager sẽ giữ nguyên tiến trình đang thực thi, tuyệt đối không tạo job trùng lặp.
   - **Hardware Constraints**:
     - `NetworkType.CONNECTED` (Yêu cầu có kết nối mạng Internet).
   - **Input Data**:
     - `KEY_TARGET_DATE`: Ngày hiện tại theo định dạng `yyyy-MM-dd`.
     - `KEY_FORCE_REFRESH`: `true` (ép buộc tải dữ liệu mới bỏ qua TTL ban đầu).
     - `KEY_DATASET_CATEGORY`: `DatasetCategory.SCHEDULED_MATCHES.name`.
3. **Ủy thác Worker & Động cơ**:
   - `DataSyncWorker` tiếp nhận `OneTimeWorkRequest`, chuyển giao cho `DataSyncEngine.syncFullPipelineForDate()`.
   - Dữ liệu được nạp tuần tự vào SQLite Room: Teams $\to$ Matches $\to$ Odds/Rankings $\to$ Cập nhật `DatasetMetadataEntity`.
   - Sau khi ghi vào Room, các UI Flow tự động phát ra dữ liệu mới và cập nhật tức thì trên giao diện mà không cần reload màn hình.

---

### 4.2 Sub-task D3.2 — Periodic Sync Runtime Integration

#### Mục tiêu:
Đưa cơ chế đồng bộ định kỳ chạy ngầm vào vận hành thực tế tại điểm khởi đầu của ứng dụng (`TrueLabApplication`).

#### Thiết kế Kỹ thuật:
1. **Khởi tạo tại `TrueLabApplication.onCreate()`**:
   - Inject `SyncWorkScheduler` vào `TrueLabApplication`.
   - Trong `onCreate()`:
     ```kotlin
     @Inject
     lateinit var syncWorkScheduler: SyncWorkScheduler

     override fun onCreate() {
         super.onCreate()
         syncWorkScheduler.scheduleInitialSync()
         syncWorkScheduler.schedulePeriodicSync()
     }
     ```
2. **Thuộc tính Kỹ thuật của Periodic Work**:
   - **Unique Work Name**: `SyncWorkScheduler.PERIODIC_SYNC_WORK_NAME = "TrueLabPeriodicDataSyncWork"`.
   - **ExistingPeriodicWorkPolicy**: `ExistingPeriodicWorkPolicy.KEEP` (Bảo toàn lịch trình sẵn có, không reset chu kỳ mỗi lần mở app).
   - **Chu kỳ Lập lịch**: Mặc định 60 phút (`DEFAULT_PERIODIC_INTERVAL_MINUTES = 60L`), tối thiểu 15 phút theo chuẩn Android WorkManager (`coerceAtLeast(15L)`).
   - **Hardware Constraints**:
     - `NetworkType.CONNECTED`.
     - `RequiresBatteryNotLow(true)` (Tránh tiêu tốn pin khi thiết bị sắp cạn nguồn).
3. **Phối hợp Tối ưu với Data D2**:
   - `DataSyncWorker` khi chạy định kỳ sẽ kiểm tra `DataFreshnessPolicy` và `CacheFreshnessChecker`. Nếu cache cục bộ chưa hết hạn (chưa quá TTL), worker trả về `Result.success()` ngay lập tức mà không gọi mạng dư thừa.
   - Nếu xảy ra lỗi mạng tạm thời, `RetryClassifier` và `RetryExecutor` tự động thử lại theo Exponential Backoff với Full Jitter.

---

### 4.3 Sub-task D3.3 — League / Season Data Bootstrap & Phân Tích Giới Hạn

#### Khảo sát & Xác minh Hiện trạng Mã nguồn:
- **Tầng Remote API**: Hiện tại `MatchApi`, `OddsApi`, `RankingApi` **không có endpoint** trả về danh sách `League` hoặc `Season`.
- **DTO `MatchRecord`**: Chỉ chứa thông tin trận đấu, hai đội bóng (`home_team`, `away_team`), tỷ số và thời gian. Không có trường `league_id` hoặc `season_id`.
- **Tầng Database cục bộ**: Đã có sẵn bảng `leagues` (`LeagueEntity`) và `seasons` (`SeasonEntity`), cùng DAO và `LeagueRepositoryImpl`.

#### Ranh giới Trách nhiệm & Hướng xử lý D3.3:
1. **Tuân thủ Nguyên tắc Trung thực Kỹ thuật (Zero Fake Data)**:
   - **KHÔNG** tự ý bịa endpoint Retrofit không tồn tại trên máy chủ.
   - **KHÔNG** hardcode danh sách giải đấu tĩnh (Premier League, La Liga, Serie A...) vào code nếu nguồn backend không cung cấp.
2. **Phạm vi Triển khai D3.3**:
   - Ghi nhận `League/Season Remote Ingestion` là một **Open Issue / Backend Dependency**.
   - Giữ nguyên `DataSyncEngine.syncLeaguesAndSeasons(leagues, seasons)` như một Extension Point chuẩn mực.
   - Đảm bảo `LeagueRepositoryImpl` và UI `MatchesViewModel` tiếp tục hoạt động an toàn (fallback về danh sách rỗng hoặc hiển thị toàn bộ trận đấu khi không chọn League filter).

---

## 5. Bảng Phân Định Thành Phần Tái Sử Dụng vs Bổ Sung Mới

| Thành phần | Trạng thái Hiện tại | Định hướng trong Data D3 |
| :--- | :--- | :--- |
| `DataSyncEngine` | Đã hoàn thành trong D1/D2 | **Tái sử dụng 100%** (Không viết lại logic sync, transaction hay metadata hook). |
| `DataSyncWorker` | Đã hoàn thành trong D2.3 | **Tái sử dụng 100%** cho cả Initial Sync và Periodic Sync. |
| `SyncWorkScheduler` | Đã có `schedulePeriodicSync()` | **Tái sử dụng & Mở rộng thêm** hàm `scheduleInitialSync()`. |
| `TrueLabApplication` | Chỉ có `Configuration.Provider` | **Bổ sung `onCreate()`** để inject và kích hoạt `SyncWorkScheduler`. |
| `RetryExecutor` / `RetryClassifier` | Đã hoàn thành trong D2.1 | **Tái sử dụng 100%** xử lý transient network failures. |
| `CacheFreshnessPolicy` / Checker | Đã hoàn thành trong D2.2 | **Tái sử dụng 100%** kiểm soát độ tươi cache. |
| `DatasetMetadataRepository` | Đã hoàn thành trong D1.4 | **Tái sử dụng 100%** để kiểm tra first-sync và cập nhật snapshot. |
| `LeagueRepository` / `MatchRepository` | Đã hoàn thành trong D1 | **Tái sử dụng 100%** làm Single Source of Truth cho Presentation. |

---

## 6. Danh Mục Files Dự Kiến Thay Đổi

### Files Sửa Đổi:
1. `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/SyncWorkScheduler.kt`:
   - Thêm `scheduleInitialSync(forceRefresh: Boolean = false)`.
   - Bổ sung hằng số `INITIAL_SYNC_WORK_NAME = "TrueLabInitialDataSyncWork"`.
2. `app/src/main/kotlin/dev/anhquocs/truelab/TrueLabApplication.kt`:
   - Inject `SyncWorkScheduler`.
   - Triển khai `onCreate()` kích hoạt `scheduleInitialSync()` và `schedulePeriodicSync()`.

### Files Kiểm Thử Cần Bổ Sung / Cập Nhật:
1. `core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/worker/SyncWorkSchedulerTest.kt`:
   - Test `scheduleInitialSync` tạo đúng `OneTimeWorkRequest`, ràng buộc mạng `CONNECTED`, và policy `KEEP`.
   - Test ngăn ngừa duplicate initial sync.

---

## 7. Chiến Lược Kiểm Thử (Testing Strategy)

### 7.1 Unit Tests cho D3.1 & D3.2
- **Initial Sync Enqueue Test**: Xác minh `scheduleInitialSync()` tạo `OneTimeWorkRequest` chứa đúng `KEY_FORCE_REFRESH = true`, `KEY_TARGET_DATE`, và unique work name `TrueLabInitialDataSyncWork`.
- **ExistingWorkPolicy Test**: Xác minh policy sử dụng là `ExistingWorkPolicy.KEEP`.
- **Periodic Work Constraints Test**: Tiếp tục bảo đảm 5/5 tests hiện tại của `SyncWorkSchedulerTest` về `CONNECTED`, `BatteryNotLow`, và chu kỳ $\ge 15$ phút.

### 7.2 Integration & Regression Verification
- Chạy toàn bộ test suite dự án (`./gradlew test`): Khẳng định toàn bộ **484+ tests PASS 100%**.
- Biên dịch ứng dụng (`./gradlew assembleDebug`): Khẳng định build thành công 100%.

---

## 8. Tiêu Chí Nghiệm Thu (Acceptance Criteria)

1. [ ] **Kích hoạt Sync Lần đầu**: Khi ứng dụng khởi chạy lần đầu (DB rỗng), `scheduleInitialSync()` được kích hoạt tự động.
2. [ ] **Không Duplicate Initial Work**: Sử dụng `ExistingWorkPolicy.KEEP` với Unique Work Name để ngăn chặn enqueue lặp lại khi người dùng mở lại app trong lúc sync đang chạy.
3. [ ] **Tái sử dụng Toàn diện Data D2**: Cả Initial Sync và Periodic Sync đều sử dụng chung `DataSyncWorker`, `DataSyncEngine`, `RetryExecutor` và `CacheFreshnessChecker`.
4. [ ] **Dữ liệu được Ghi vào Room**: Sau khi sync thành công, các bảng `teams`, `matches`, `odds_records`, `season_rankings` được nạp đầy đủ.
5. [ ] **Cập nhật Metadata Tức thì**: `DatasetMetadataEntity` ghi nhận đúng số lượng bản ghi và `lastSyncTimestamp > 0`.
6. [ ] **Lập lịch Định kỳ Hoạt động**: `SyncWorkScheduler.schedulePeriodicSync()` được gọi khi khởi động ứng dụng với `ExistingPeriodicWorkPolicy.KEEP`.
7. [ ] **Bảo Toàn Ranh Giới Kiến Trúc**: Không tạo network call trực tiếp từ Presentation hay ViewModels; Presentation tiếp tục chỉ đọc từ Room qua Repository Flow.
8. [ ] **Xử lý Đúng đắn League/Season**: Không hardcode dữ liệu giả; ghi nhận rõ ràng sự phụ thuộc vào backend.
9. [ ] **Không Thay đổi Algorithm Layer**: Module `:core:algorithm` tiếp tục giữ trạng thái FROZEN tuyệt đối.
10. [ ] **Test Suite & Build Đạt Chuẩn**: 100% unit tests của `:core:data` và `:app` vượt qua, build `assembleDebug` thành công.

---

## 9. Các Hạng Mục Cố Tình Nằm Ngoài Phạm Vi (Out of Scope)

Theo đúng ranh giới của Data Phase D3:
1. **Không can thiệp vào UI / Giao diện người dùng**: Các tính năng Manual Pull-to-Refresh, Animation Loading, Dialogs sẽ thuộc về Presentation Layer.
2. **Không thay đổi Room Database Schema**: Giữ nguyên Room Schema v2 hiện tại (`TrueLabDatabase`).
3. **Không tạo endpoint giả lập (Mocking Endpoints)** cho League/Season nếu API thực tế không hỗ trợ.
4. **Không triển khai Paging 3 hay Streaming socket**: Giữ nguyên cơ chế Room Flow và batch ingestion ổn định.
