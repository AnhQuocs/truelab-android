# Báo Cáo Tổng Kết Data Phase D3 — Initial Data Bootstrap & Runtime Sync

**Dự án:** TrueLab — Football Data Analytics & Prediction Engine (Android Jetpack Compose)  
**Giai đoạn:** Data Phase D3 (*Initial Data Bootstrap & Runtime Sync*)  
**Trạng thái:** **HOÀN THÀNH TOÀN DIỆN (100% COMPLETE)**  
**Commit mốc:** `b009aba` (D3.1), `6d97664` (D3.2), `362c9f2` (Runtime Integration Fixes), `b2089f9` (D3.3)  
**Tổng số Unit Tests toàn hệ thống:** **516 / 516 tests PASS (100%)** *(84/84 tasks executed/up-to-date)*

---

## 1. Mục Tiêu & Phạm Vi (Scope)

Data Phase D3 kết nối toàn bộ hạ tầng thu thập và lưu trữ dữ liệu `:core:data` đã xây dựng từ D1 và D2 vào vòng đời hoạt động thực tế của ứng dụng TrueLab, giải quyết triệt để vấn đề cơ sở dữ liệu Room rỗng khi cài mới hoặc khởi động ứng dụng:

1. **D3.1 — Initial Sync & Runtime Trigger**: Tự động phát hiện trạng thái khởi chạy lần đầu (hoặc sau khi xóa dữ liệu ứng dụng) và kích hoạt tác vụ nạp dữ liệu nền `OneTimeWorkRequest` qua Android WorkManager mà không làm treo hoặc gián đoạn giao diện người dùng.
2. **D3.2 — Periodic Sync Runtime Integration**: Khởi tạo và kích hoạt bộ lập lịch đồng bộ ngầm định kỳ `SyncWorkScheduler.schedulePeriodicSync()` ngay tại `TrueLabApplication.onCreate()`.
3. **D3.3 — League/Season Data Bootstrap**: Kết nối remote REST API thực tế (`CompetitionApi`) để nạp danh mục Giải đấu (Competitions/Leagues) và Mùa giải (Seasons), liên kết trực tiếp `MatchEntity.leagueId` với `LeagueEntity.id`, bảo toàn ràng buộc khóa ngoại (Foreign Key Integrity) và cấp nguồn dữ liệu thật cho bộ lọc giải đấu trên UI.

---

## 2. Chi Tiết Triển Khai Kỹ Thuật

### 2.1. Sub-phase D3.1 — Initial Sync & Runtime Trigger
*(Commit: `b009aba` — `feat(data): implement D3.1 initial sync runtime trigger`)*

- **Phát hiện khởi chạy lần đầu (`DefaultSyncWorkScheduler`)**:
  - Triển khai phương thức `scheduleInitialSync(forceRefresh: Boolean)`.
  - Sử dụng `ExistingWorkPolicy.KEEP` với định danh duy nhất `INITIAL_SYNC_WORK_NAME = "TrueLabInitialDataSyncWork"`. Khi người dùng mở hoặc đóng ứng dụng liên tục trong khi initial sync đang chạy, WorkManager duy trì tác vụ nền hiện tại mà không tạo bản sao trùng lặp.
  - Áp dụng ràng buộc phần cứng an toàn: `NetworkType.CONNECTED`.
- **Kích hoạt tại điểm nhập của ứng dụng (`TrueLabApplication`)**:
  - Tích hợp điều phối đồng bộ trong `TrueLabApplication.onCreate()` thông qua `CoroutineScope(Dispatchers.IO)`.
  - `DataSyncWorker` nhận lệnh và gọi `DataSyncEngine.syncFullPipelineForDate()`, nạp tuần tự: Teams $\to$ Matches $\to$ Odds $\to$ Rankings $\to$ Cập nhật `DatasetMetadata`.
  - Nhờ cơ chế `Flow` phản ứng của Room, giao diện người dùng tự động cập nhật ngay khi dữ liệu được ghi vào SQLite mà không cần reload màn hình.

### 2.2. Sub-phase D3.2 — Periodic Sync Runtime Integration
*(Commit: `6d97664` — `feat(data): integrate periodic sync at runtime`)*

- **Lập lịch đồng bộ định kỳ**:
  - Đăng ký `syncWorkScheduler.schedulePeriodicSync()` ngay khi ứng dụng khởi chạy.
  - Sử dụng `ExistingPeriodicWorkPolicy.KEEP` với `PERIODIC_SYNC_WORK_NAME = "TrueLabPeriodicDataSyncWork"` và chu kỳ mặc định (15 phút).
  - Áp dụng kết hợp ràng buộc mạng `NetworkType.CONNECTED` và pin `RequiresBatteryNotLow(true)`.

### 2.3. Sub-phase D3.3 — League/Season Remote Bootstrap
*(Commit: `b2089f9` — `feat(data): implement D3.3 league and season bootstrap from remote API`)*

- **Remote API Client (`CompetitionApi`)**:
  - `GET /sport/v1.0/competitions/list/?page={page}&page_size={pageSize}&sort={sort}`: Truy vấn danh mục giải đấu có phân trang.
  - `GET /sport/v1.0/competitions/{competition_id}/seasons`: Truy vấn danh sách mùa giải và các giai đoạn thi đấu (Stages) của từng giải đấu.
- **DTO Modeling & Serialization (`CompetitionDto`)**:
  - Xây dựng các mô hình dữ liệu `@Serializable`: `CompetitionListResponseBase`, `CompetitionMetaResponse`, `CompetitionItemDto`, `SeasonDto`, `StageDto`.
  - Mở rộng `MatchRecord` để tiếp nhận trường `competition_id: Int?` và `competition: CompetitionSummaryInfo?`.
- **Ánh xạ DTO sang Thực thể Room (`RoomMappers`)**:
  - `CompetitionItemDto.toLeagueEntity()` $\to$ `LeagueEntity(id, name, shortName, logo, country, category)`.
  - `SeasonDto.toSeasonEntity()` $\to$ `SeasonEntity(id, leagueId, name, year, isCurrent, startDate, endDate)`.
  - `MatchRecord.toMatchEntity()` $\to$ Gán trực tiếp `leagueId = competitionId`.
  - `MatchRecord.toLeagueEntity()` $\to$ Tự động trích xuất thông tin giải đấu phát sinh từ response trận đấu.
- **Quy trình Ingestion Đa cấp & Bảo toàn Khóa Ngoại (`DataSyncEngine`)**:
  - Triển khai `syncLeaguesAndSeasonsFromRemote(maxPages = 2, syncSeasons = true)`.
  - Trước khi chèn danh sách trận đấu (`matches`), `DataSyncEngine` trích xuất và chèn toàn bộ `LeagueEntity` vào bảng `leagues` trong cùng một database transaction. Nhờ vậy, 100% bản ghi `matches.leagueId` đều tham chiếu tới một giải đấu hợp lệ trong cơ sở dữ liệu (`ON DELETE SET NULL`, `seasons.leagueId -> leagues.id` với `ON DELETE CASCADE`), triệt tiêu hoàn toàn nguy cơ vi phạm ràng buộc toàn vẹn khóa ngoại (Foreign Key Violation).

---

## 3. Kiến Trúc & Luồng Dữ Liệu Tổng Thể (Data Flow)

```text
[TrueLabApplication.onCreate()]
          │
          ├──► scheduleInitialSync()  (OneTimeWorkRequest, KEEP)
          └──► schedulePeriodicSync() (PeriodicWorkRequest, KEEP)
                    │
                    ▼
           [DataSyncWorker]
                    │
                    ▼
           [DataSyncEngine.syncFullPipelineForDate()]
                    │
   ┌────────────────┴──────────────────────────────────────────────────────┐
   │ 1. syncLeaguesAndSeasonsFromRemote(maxPages = 2)                      │
   │    ├── GET /competitions/list/  ──► Upsert LeagueEntity               │
   │    └── GET /{id}/seasons        ──► Upsert SeasonEntity               │
   │                                                                       │
   │ 2. fetchMatchesForDate(targetDate)                                    │
   │    └── GET /sport/v1.0/matches                                        │
   │                                                                       │
   │ 3. Database Atomic Transaction                                        │
   │    ├── Upsert Teams (home/away)                                       │
   │    ├── Upsert Extracted Match Leagues (Bảo toàn Foreign Key)          │
   │    └── Upsert Matches (MatchEntity.leagueId = competition_id)         │
   │                                                                       │
   │ 4. Batch Ingestion (Odds & Season Rankings per Match)                 │
   │    ├── GET /sport/v1.0/odds/{matchId}     ──► Upsert OddsEntity       │
   │    └── GET /sport/v1.0/ranking/{matchId}  ──► Upsert SeasonRanking    │
   │                                                                       │
   │ 5. Dataset Snapshot Refresh                                           │
   │    └── DatasetMetadataRepository.refreshSnapshot()                    │
   │        (Total Matches, Teams, Odds, Leagues, Seasons)                 │
   └────────────────┬──────────────────────────────────────────────────────┘
                    │
                    ▼
          [Room Database Tables]
    (leagues, seasons, teams, matches, odds, season_rankings, dataset_metadata)
                    │
                    ▼
        [Repository Flow & UseCases]
                    │
                    ▼
          [Compose UI Render]
  (HomeScreen, MatchesScreen, TeamsScreen, AnalyticsScreen, PredictionScreen)
```

---

## 4. Các Vấn Đề Phát Hiện & Giải Pháp Xử Lý (Issues Discovered & Fixes)
*(Commit: `362c9f2` — `fix(runtime): resolve match status mapping, presentation formatting and UI resilience`)*

Trong quá trình tích hợp dữ liệu thực tế từ backend vào runtime, hệ thống đã phát hiện và xử lý triệt để 4 vấn đề kỹ thuật:

1. **Khớp mã trạng thái trận đấu dạng chuỗi văn bản (String Match Status Mapping)**:
   - *Vấn đề*: Backend trả về trạng thái trận đấu dưới dạng văn bản (`"ended"`, `"determined"`, `"scheduled"`, `"pending"`) song song với mã số (`"8"`). Điều này khiến câu truy vấn SQL lấy trận đấu quá khứ trong `MatchDao` bị rỗng và các trận đã có tỷ số vẫn hiển thị tag "Sắp diễn ra".
   - *Xử lý*: Bổ sung ánh xạ văn bản trong `MatchStatus.fromCode()`, cập nhật `MatchDao` lọc danh sách trạng thái kết thúc: `('8', 'ended', 'determined', 'finished', 'ft', 'aet', 'pen')`.
2. **Khả năng chịu lỗi theo từng trận đấu trong Batch Odds/Ranking Sync**:
   - *Vấn đề*: Khi một trận đấu đơn lẻ gặp lỗi 404/500 khi gọi Odds hoặc Ranking, toàn bộ tiến trình sync ngày bị hủy bỏ.
   - *Xử lý*: Bọc khối `try-catch` riêng biệt cho từng trận trong vòng lặp batch của `DataSyncEngine`, cho phép bỏ qua bản ghi lỗi cục bộ mà vẫn bảo đảm toàn vẹn kho dữ liệu chung.
3. **Câu truy vấn tỷ lệ cược mới nhất (`OddsDao.getLatestOddsForMatch`)**:
   - *Vấn đề*: SQLite so sánh chuỗi ngày giờ có thể sai lệch nếu `changeTime` lưu epoch milliseconds.
   - *Xử lý*: Chuẩn hóa câu truy vấn `ORDER BY changeTime DESC LIMIT 1` và bảo đảm tính tương thích với DTO.
4. **Định dạng hiển thị an toàn trên giao diện người dùng**:
   - *Vấn đề*: Một số chỉ số hiển thị chuỗi kỹ thuật hoặc format không tương thích khi thiếu dữ liệu.
   - *Xử lý*: Cập nhật `MatchUiMapper`, `TeamUiMapper`, `DescriptiveStatsCard`, `DatasetOverviewCard` với cơ chế fallback giá trị mặc định an toàn (`"—"`, `0.0`), bổ sung resource strings triple-locale (`values`, `values-en`, `values-vi`).

---

## 5. Xác Thực Runtime Thực Tế Trên Thiết Bị (Device Runtime Verification)

- **Thiết bị thử nghiệm:** Android Device `13195704AS018155` (Infinix X6880, Android 15), kết nối qua ADB Wireless Debugging.
- **Quy trình kiểm thử:** Cài đặt gói ứng dụng `app-debug.apk`, xóa sạch dữ liệu (`pm clear dev.anhquocs.truelab`), khởi chạy `MainActivity` và xác minh cơ sở dữ liệu SQLite sau Initial Sync.

### Kết quả truy vấn cơ sở dữ liệu Room:

| Chỉ số / Bảng dữ liệu | Kết quả thực tế | Trạng thái |
| :--- | :---: | :---: |
| **`leagues` count** | **123** bản ghi | **PASS** |
| **`seasons` count** | **34** bản ghi | **PASS** |
| **`matches` count** | **50** bản ghi | **PASS** |
| **`matches_with_leagueId`** | **50 / 50 (100%)** có `leagueId` | **PASS** |
| **`seasons_fk_violation`** | **0** vi phạm | **PASS** |
| **`odds` records** | **232,672** bản ghi tỷ lệ kèo | **PASS** |
| **`dataset_metadata`** | Khởi tạo đầy đủ metadata snapshot | **PASS** |

### Kiểm tra hành vi giao diện người dùng (UI Verification):
- **`HomeScreen`**: Thẻ `Dataset Overview` hiển thị 50 Trận đấu, 100 Đội bóng, 232,672 Bản ghi Tỷ lệ cược.
- **`MatchesScreen`**: Thanh cuộn ngang `LeagueFilterChips` nạp danh sách giải đấu thật (USA MLS, MEX LP, v.v.). Bấm chọn chip lọc hoặc tìm kiếm theo tên đội/giải (ví dụ: `Vancouver`) cập nhật danh sách ngay lập tức.
- **`PredictionScreen`**: Khởi chạy thuật toán dự đoán trực tiếp (On-demand `PredictMatchOutcomeUseCase`) từ dữ liệu thật, xuất phân phối xác suất 3 chiều thỏa mãn $P_H, P_D, P_A \in [0, 1]$ và $P_H + P_D + P_A \approx 1.0$.
- **Chống trùng lặp (Duplicate Prevention)**: Khởi động lại ứng dụng (Restart/Re-sync), số lượng bản ghi trong các bảng giữ nguyên trạng thái nhất quán, không xảy ra xung đột khóa chính hay nhân bản dữ liệu.

---

## 6. Giới Hạn Đã Biết (Known Limitations)

1. **Cận phân trang Bootstrap `maxPages = 2`**:
   - *Hiện trạng API*: Backend `/sport/v1.0/competitions/list/` chứa tổng cộng **2,677 giải đấu** trên **54 pages** (`per_page = 50`).
   - *Nguyên nhân kỹ thuật*: Việc nạp tuần tự 54 trang giải đấu kèm toàn bộ danh sách mùa giải cho từng giải trong lần khởi chạy đầu tiên trên thiết bị di động sẽ tạo ra hơn 100 cuộc gọi mạng liên tiếp, gây nghẽn băng thông và kéo dài thời gian khởi động.
   - *Giải pháp đã áp dụng*: Giới hạn chủ đích `maxPages = 2` để nạp nhanh 100 giải đấu phổ biến nhất làm danh mục ban đầu. Toàn bộ các giải đấu phát sinh từ lịch thi đấu thực tế trong ngày đều được tự động trích xuất và chèn bổ sung vào SQLite, đảm bảo 100% trận đấu luôn có thông tin giải đấu mà không cần nạp toàn bộ 2,677 giải.
2. **Phạm vi hiển thị Mùa giải trên UI**:
   - Giai đoạn D3 tập trung hoàn thiện tầng lưu trữ và đồng bộ dữ liệu (Data Layer). Thông tin mùa giải (`seasons`) đã được lưu trữ và lập chỉ mục đầy đủ trong Room Database, sẵn sàng để phục vụ các bộ lọc nâng cao trong các phân kỳ Presentation tiếp theo.

---

## 7. Ma Trận Kiểm Thử Hệ Thống (Test Matrix & Build Verification)

Toàn bộ hệ thống vượt qua **100% các bài kiểm thử đơn vị** và build thành công:

- **Unit Tests:** **516 / 516 tests PASS (100%)**
  - `:core:algorithm:test` $\to$ **PASS**
  - `:core:domain:test` $\to$ **PASS**
  - `:core:ui:test` $\to$ **PASS**
  - `:core:data:test` $\to$ **PASS** (bao gồm `CompetitionDtoTest`, `RoomMappersTest`, `DataSyncEngineBootstrapTest`, `DataSyncWorkerTest`, `SyncWorkSchedulerTest`)
  - `:app:test` $\to$ **PASS** (bao gồm `MatchUiMapperTest`, `TeamUiMapperTest`)
- **Gradle Build:** `./gradlew assembleDebug` $\to$ **BUILD SUCCESSFUL**.
- **Code Style & Format:** `git diff --check` $\to$ **0 errors (clean)**.

---

## 8. Kết Luận & Đánh Giá Nghiệm Thu

> **KẾT LUẬN CHÍNH THỨC: `DATA D3 — COMPLETE & INTEGRATED`**
>
> Toàn bộ 3 mục tiêu của Data Phase D3 (**D3.1 Initial Sync**, **D3.2 Periodic Sync**, **D3.3 League/Season Bootstrap**) đã được hoàn thành, xác thực trên thiết bị thật và hòa nhập hoàn toàn vào nhánh `main` (`origin/main`). Kho dữ liệu TrueLab đã chính thức hoạt động với nguồn dữ liệu bóng đá trực tiếp.
