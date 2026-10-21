# SOURCE OF TRUTH — Prediction Runtime Sync, Timezone & Status Refresh Plan

Tài liệu này là bản quy hoạch kiến trúc và nguồn chân lý (Source of Truth) cho việc chuẩn hóa cơ chế đồng bộ runtime (Runtime Match Sync), xử lý ánh xạ múi giờ (Date/Timezone Mapping), cơ chế làm mới dữ liệu (Pull-to-Refresh / Open Screen Flow) và xác định trạng thái trận đấu (Match Status Resolution) cho màn hình **PredictionScreen** trong ứng dụng **TrueLab**.

---

## 1. Problem Statement

### 1.1. Các sự thật đã được xác nhận (Confirmed Facts)
1. **Lệch pha múi giờ giữa UI Local Time và API Remote**:
   - `PredictionScreen` cho phép người dùng chọn ngày xem theo giờ cục bộ ứng dụng / Việt Nam (`Asia/Ho_Chi_Minh` — UTC+7).
   - Remote Endpoint `GET /sport/v1.0/matches?date=YYYY-MM-DD` có semantics lọc theo **UTC Date** (`00:00:00Z` đến `23:59:59Z`).
   - Do đó, các trận đấu diễn ra vào sáng sớm giờ Việt Nam (từ 00:00 đến 06:59:59 VN, tức `17:00:00Z` đến `23:59:59Z` ngày hôm trước) hoàn toàn nằm trong UTC Date của ngày hôm trước.
2. **Minh chứng trường hợp Greece vs Netherlands (Match ID 129243)**:
   - `start_time_date`: `2026-10-01T18:45:00Z` (quy đổi giờ Việt Nam: `01:45 ngày 02/10/2026`).
   - Khi gọi trực tiếp endpoint chi tiết `GET /sport/v1.0/matches/129243`: API trả về `status: "ended"`, `minutes: "FT"`, `home_score: 2`, `away_score: 2`, `attributes.status: 8`.
   - Khi gọi `GET /sport/v1.0/matches?date=2026-10-02`: API trả về 20 trận thuộc UTC 2026-10-02, **hoàn toàn không có Match 129243**.
   - Match 129243 thực tế nằm ở **Trang 9** của `GET /sport/v1.0/matches?date=2026-10-01`.
3. **Room Database bị Stale (Dữ liệu cũ mồ côi)**:
   - Khi người dùng mở ngày `2026-10-02`, ViewModel gọi sync `date = 2026-10-02`. Vì API không trả về Match 129243, bản ghi trong Room không bao giờ được cập nhật và giữ nguyên `status = "pending"`, tỷ số `0-0` từ đợt crawl cũ.
4. **Lỗi logic suy đoán trạng thái trên UI**:
   - Hàm `resolveDisplayStatus()` trong `PredictableMatchCard.kt` chứa logic: Nếu `match.status` là `SCHEDULED` (`"pending"`) và `kickoff <= now`, hàm tự động trả về `DisplayMatchStatus.LIVE`.
   - Do không có giới hạn thời gian kết thúc, trận đấu 01:45 sáng dù đã kết thúc từ lâu nhưng vẫn bị hiển thị `LIVE` nhấp nháy vào lúc 10:30 sáng.
5. **Nghẽn mạng ~30 giây khi Pull-to-Refresh & Open Screen**:
   - `DataSyncEngine.syncFullPipelineForDate()` mặc định chạy `syncLeaguesAndSeasons = true`.
   - Hàm này gọi `syncLeaguesAndSeasonsFromRemoteInternal(maxPages = 2, syncSeasons = true)`, tải 100 giải đấu và thực hiện **100 request HTTP tuần tự** đến `GET /sport/v1.0/competitions/{id}/seasons`.
   - Tổng cộng phát sinh hơn **104 requests HTTP**, làm tê liệt luồng refresh trong 28-30 giây chỉ để lấy vài trận đấu trong ngày.

### 1.2. Quyết định thiết kế (Design Decisions)
- Cần có tầng trừu tượng **Request Date Range Mapper** để chuyển đổi 1 Local Date (24h) thành tập các UTC Date query tương ứng (thường là 2 ngày UTC: `previousUtcDate` và `currentUtcDate`), kèm cơ chế lọc chuẩn xác theo khung giờ local `[startOfDayUtc, endOfDayUtc)`.
- Tách toàn bộ việc đồng bộ danh mục giải đấu / mùa giải toàn cầu ra khỏi Critical Path của màn hình `PredictionScreen`.
- Trạng thái trận đấu phải ưu tiên tuyệt đối dữ liệu xác nhận từ Server (`MatchStatus`), loại bỏ hoàn toàn việc biến `pending` thành `LIVE` hoặc tự suy diễn `FT` từ thời gian trôi qua.

### 1.3. Điểm cần xác minh bổ sung trong quá trình thực thi (To Verify)
- Hành vi phân trang (`MetaResponse.last_page`) trên cả 2 ngày UTC khi số lượng trận đấu lớn.
- Thời gian sống của Cache (Cache TTL Policy) và cơ chế hủy job (Job Cancellation) khi người dùng đổi ngày liên tục.

---

## 2. Goals

1. **Chuẩn hóa Timezone Boundary**: Ngày được chọn trên UI (`selectedDate`) được hiểu theo múi giờ ứng dụng (mặc định `Asia/Ho_Chi_Minh`), và ánh xạ chính xác sang phạm vi UTC để gọi API.
2. **Đồng bộ đầy đủ dữ liệu Match theo ngày (Full Coverage Sync)**: Runtime match sync phải query toàn bộ các UTC date liên quan (bao gồm cả phân trang) để không bỏ sót các trận đấu sáng sớm hoặc đêm muộn.
3. **Cập nhật Room Database nhất quán**: Dữ liệu trả về từ API phải được ghi ngay vào Room Database, và UI tự động cập nhật phản ứng thông qua Room DAO Flow (Single Source of Truth).
4. **Tối ưu hóa thời gian Refresh**: Loại bỏ hoàn toàn 100+ requests tuần tự của League/Season bootstrap khỏi luồng runtime, đưa thời gian pull-to-refresh về mức tối ưu (1-2 network roundtrips).
5. **Chuẩn hóa logic hiển thị trạng thái**: Trận đấu `pending` không bao giờ bị tự động biến thành `LIVE` vô thời hạn. Tôn trọng `MatchStatus` từ server và fallback an toàn.
6. **Xử lý Concurrency an toàn**: Tránh race conditions khi kéo refresh nhiều lần hoặc đổi ngày liên tục.

---

## 3. Non-Goals

- **Không** crawl lại toàn bộ 30.000+ matches của historical dataset.
- **Không** rebuild file asset database SQLite (`truelab_database.db`).
- **Không** thay đổi thuật toán dự đoán, trọng số Elo, Poisson, Form score hay ML model.
- **Không** tự động suy diễn `FT` (Finished) chỉ bằng công thức `kickoff + 130 phút`.
- **Không** redesign layout UI của `PredictionScreen` ngoài phần fix hiển thị status và refresh indicator.
- **Không** thay đổi `CompetitionQualityPolicy` ngoài việc áp dụng cho runtime match filtering.

---

## 4. Current Architecture Trace

Dưới đây là luồng thực thi chi tiết được inspect trực tiếp từ mã nguồn:

```text
[UI] PredictionScreen.kt
  │  (Quan sát `uiState` & `availableMatches` từ PredictionViewModel)
  │  (Sử dụng PullToRefreshBox gọi `viewModel.refresh()`)
  ▼
[ViewModel] PredictionViewModel.kt
  │  - `_selectedDate`: StateFlow<String> (định dạng YYYY-MM-DD theo local time)
  │  - `init`: collect `_selectedDate` -> gọi `syncDateIfNeeded(date)`
  │  - `refresh()`: gọi `matchRepository.refreshMatchesForDate(_selectedDate.value)`
  │  - `matchesFlow`: combine filters -> gọi `matchRepository.getPredictableMatchesFiltered()`
  ▼
[Repository] MatchRepositoryImpl.kt (core:data)
  │  - `refreshMatchesForDate(date)`: gọi `dataSyncEngine.syncFullPipelineForDate(date, forceRefresh = true)`
  │  - `getPredictableMatchesFiltered(...)`: query Room `MatchDao`
  ▼
[Engine] DataSyncEngine.kt (core:data)
  │  - Entry Point: `syncFullPipelineForDate(date, syncLeaguesAndSeasons = true, forceRefresh = true)`
  │  - Line 93: Gọi `syncLeaguesAndSeasonsFromRemoteInternal(maxPages = 2, syncSeasons = true)` (GÂY NGHẼN 30s)
  │  - Line 106: Gọi `matchApi.getMatches(date = date, page = currentPage)` (chỉ gửi 1 ngày string đơn)
  ▼
[Network API] MatchApi.kt (core:data)
  │  - `@GET("/sport/v1.0/matches") suspend fun getMatches(@Query("date") date: String, ...)`
  ▼
[DTO & Mapper] MatchDto.kt & RoomMappers.kt (core:data)
  │  - `MatchRecord` -> `RoomMappers.toMatchEntity()` -> `MatchEntity`
  ▼
[Database] MatchDao.kt & Room Database (core:data)
  │  - `database.matchDao().upsertMatches(matches)`
  │  - `getPredictableMatchesFiltered(...)` emit `Flow<List<MatchWithTeams>>`
  ▼
[Presentation Status Resolution] PredictableMatchCard.kt (app)
  │  - `resolveDisplayStatus(match, selectedDate, now)`:
  │    Line 100: `if (kickoffZoned.isBefore(now)) DisplayMatchStatus.LIVE` (LỖI LIVE VÔ THỜI HẠN)
```

---

## 5. Proposed Architecture

```text
Local UI Date (YYYY-MM-DD, e.g. 2026-10-02 Asia/Ho_Chi_Minh)
       │
       ▼
[Request Date Range Mapper] (Domain / Data Abstraction)
       │  Chuyển đổi: 2026-10-02 Local [2026-10-01T17:00:00Z .. 2026-10-02T17:00:00Z)
       │  Xác định danh sách UTC query dates cần fetch: ["2026-10-01", "2026-10-02"]
       ▼
[Runtime Match Sync Engine] (DataSyncEngine - Match Only Mode)
       │  Lặp qua từng UTC query date:
       │    └── Fetch MatchApi.getMatches(date, page) (Hỗ trợ phân trang đầy đủ)
       │  Lọc match: chỉ giữ các match có startTimeDate nằm trong Local Day window [startUtc, endUtc)
       │  Áp dụng CompetitionQualityPolicy (Chỉ persist matches của accepted competitions)
       ▼
[Atomic Database Transaction]
       │  Upsert Leagues (từ match payload)
       │  Upsert Teams (từ match payload)
       │  Upsert Matches
       ▼
[Room Database SSOT]
       │  Room invalidates -> MatchDao Flow emits updated MatchWithTeams
       ▼
[MatchRepositoryImpl -> PredictionViewModel]
       │  StateFlow updates UI State
       ▼
[Presentation Layer Status Resolver]
       │  Tôn trọng Server Status:
       │  - IN_PROGRESS -> LIVE
       │  - ENDED -> ENDED / FT
       │  - CANCELLED -> CANCELLED
       │  - SCHEDULED -> UPCOMING (hoặc Fallback nếu kickoff đã qua nhưng chưa có update)
```

---

## 6. Date/Timezone Strategy

### 6.1. Semantics của Remote API
- Query param `date=YYYY-MM-DD` trên server tương đương `startTimeDate >= YYYY-MM-DDT00:00:00Z AND startTimeDate <= YYYY-MM-DDT23:59:59Z`.

### 6.2. Thuật toán ánh xạ Local Date sang UTC Queries
Với bất kỳ `localDate: LocalDate` và `zoneId: ZoneId` (mặc định `Asia/Ho_Chi_Minh`):
1. Tính thời điểm đầu ngày Local theo UTC:
   `startInstant = localDate.atStartOfDay(zoneId).toInstant()`
2. Tính thời điểm cuối ngày Local theo UTC:
   `endInstant = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()`
3. Xác định các ngày UTC chứa khoảng `[startInstant, endInstant)`:
   - `startUtcDate = startInstant.atZone(ZoneOffset.UTC).toLocalDate()`
   - `endUtcDate = endInstant.minusNanos(1).atZone(ZoneOffset.UTC).toLocalDate()`
4. Nếu `startUtcDate == endUtcDate`: Query 1 ngày UTC.
5. Nếu `startUtcDate < endUtcDate`: Query 2 ngày UTC liên tiếp (`startUtcDate` và `endUtcDate`).
   - *Ví dụ:* Ngày `2026-10-02` VN (`UTC+7`):
     - `startInstant` = `2026-10-01T17:00:00Z` -> `startUtcDate` = `2026-10-01`
     - `endInstant` = `2026-10-02T17:00:00Z` -> `endUtcDate` = `2026-10-02`
     - Danh sách query: `["2026-10-01", "2026-10-02"]`.

### 6.3. Lọc chính xác theo cửa sổ thời gian (Local-Day Window Filtering)
Sau khi fetch các trang của cả 2 ngày UTC, bản ghi `MatchRecord` được kiểm tra:
`startInstant <= matchRecord.startTimeDateInstant < endInstant`
Điều này đảm bảo không lẫn các trận của ngày khác vào Room cache của ngày đang chọn.

---

## 7. Runtime Sync Strategy

### 7.1. Tách biệt hoàn toàn League/Season Bootstrap
- Trong luồng runtime của `PredictionScreen` (Open screen / Pull-to-refresh / Date change):
  - Gọi `DataSyncEngine.syncMatchesForLocalDate(localDate, zoneId, forceRefresh = true)`.
  - Tham số `syncLeaguesAndSeasons` **bắt buộc = false**.
  - Leagues và Teams được trích xuất trực tiếp từ payload `MatchRecord` (đã có sẵn `competition.id`, `name`, `logo` và `home_team`, `away_team`) và lưu vào Room trong cùng transaction.
- Pipeline đồng bộ toàn bộ giải đấu / mùa giải (`syncLeaguesAndSeasonsFromRemote`) chuyển sang chế độ One-time Bootstrap hoặc WorkManager định kỳ nền, không can thiệp vào UI critical path.

---

## 8. Status Strategy

### 8.1. Ma trận phân giải trạng thái (Status Resolution Matrix)

| Server `match.status` (Domain `MatchStatus`) | Điều kiện thời gian (`kickoff` vs `now`) | `DisplayMatchStatus` hiển thị trên UI | Mô tả trên Match Card |
| :--- | :--- | :--- | :--- |
| **`IN_PROGRESS`** (`1`, `live`, `playing`, `ht`, `1h`, `2h`) | Bất kỳ | **`LIVE`** | Cột trái: Số phút (ví dụ `70'` nhấp nháy), Score: `Home - Away` |
| **`ENDED`** (`8`, `ended`, `finished`, `ft`, `pen`, `aet`) | Bất kỳ | **`ENDED`** | Cột trái: `FT`, Score: `Home - Away` |
| **`CANCELLED`** (`-1`, `cancelled`, `postponed`, `pst`, `abd`) | Bất kỳ | **`ENDED`** / Phù hợp UI | Cột trái: `Hoãn`/`Huỷ`, Score: `VS` hoặc `-` |
| **`SCHEDULED`** (`0`, `pending`, `not_started`, `fixture`) | `now < kickoff` | **`UPCOMING`** | Cột trái: Giờ đá (ví dụ `01:45`), Score: `VS` |
| **`SCHEDULED`** (`0`, `pending`) | `kickoff <= now < kickoff + 130m` | **`LIVE`** (Grace Period fallback khi server cập nhật chậm) | Cột trái: `LIVE`, Score: `Home - Away` |
| **`SCHEDULED`** (`0`, `pending`) | `now >= kickoff + 130m` (Trận đã qua giờ đá quá lâu) | **`UPCOMING`** / Giữ giờ đá (Tuyệt đối **KHÔNG** để `LIVE`) | Cột trái: Giờ đá (ví dụ `01:45`), Score: `VS` |

### 8.2. Nguyên tắc cấm (Invariants)
- **KHÔNG** được: `pending` + `kickoff đã qua` => Tự động hiển thị `LIVE` vô thời hạn.
- **KHÔNG** được: `kickoff + 130m` => Tự động gán kết quả `FT` hoặc giả định tỷ số khi server chưa xác nhận `status = ENDED`.

---

## 9. Refresh / Open Screen Flow & Concurrency Handling

### 9.1. Quản lý Coroutine Job trong ViewModel
- Trong `PredictionViewModel`, duy trì biến `private var syncJob: Job? = null`.
- Khi người dùng đổi ngày hoặc kéo Pull-to-Refresh:
  1. Hủy job đang chạy dở: `syncJob?.cancel()`.
  2. Khởi chạy job mới cho ngày được chọn.
  3. Quản lý cờ `_isRefreshing`: chỉ kích hoạt animation khi người dùng thao tác pull-to-refresh thủ công, không làm nháy màn hình khi sync ngầm lúc mở màn hình.

### 9.2. Xử lý lỗi & Trạng thái mạng
- Nếu API thất bại (mất mạng, timeout 504): Catch lỗi an toàn, emit `_refreshErrorEvent`, dữ liệu Room cũ vẫn hiển thị bình thường không bị gián đoạn.

---

## 10. Pagination

- Đối với mỗi UTC query date:
  ```kotlin
  var currentPage = 1
  var totalPages = 1
  while (currentPage <= totalPages) {
      val response = retryExecutor.execute {
          matchApi.getMatches(date = utcDateStr, page = currentPage, pageSize = 50)
      }
      val records = response.data.data
      if (records.isEmpty()) break
      
      // Process & Filter
      
      totalPages = response.data.meta?.lastPage ?: 1
      currentPage++
  }
  ```
- Không bao giờ giả định dữ liệu chỉ nằm ở `page = 1`.

---

## 11. Competition Quality Policy

- Mọi `MatchRecord` được fetch về từ runtime sync vẫn phải đi qua `CompetitionQualityPolicy.isAccepted(record)`.
- Chỉ các trận thuộc giải đấu đạt chuẩn mới được đưa vào Room transaction.
- Khi match được accept, League và Teams đi kèm được insert cùng transaction để đảm bảo toàn vẹn ràng buộc khóa ngoại (Foreign Key).

---

## 12. Detailed Implementation Tasks

### Task 1 — Request Date Range Mapper
- **Files:** `core/data/.../LocalDateRangeMapper.kt` (hoặc đặt trong `core:data:crawler:util`)
- **Trách nhiệm:** Nhận vào `LocalDate` và `ZoneId`, trả về `UtcQueryWindow(utcDates: List<String>, startInstant: Instant, endInstant: Instant)`.
- **Unit Tests:** Kiểm tra các mốc giờ 00:00 VN, 06:59 VN, 07:00 VN, 23:59 VN, ngày nhuận, chuyển tháng/năm.

### Task 2 — Runtime Match-Only Sync Method trong DataSyncEngine
- **Files:** [DataSyncEngine.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt)
- **Trách nhiệm:** Thêm hàm `syncMatchesForLocalDate(localDate: String, zoneId: ZoneId, forceRefresh: Boolean): SyncResult<SyncSummary>`.
- **Thực hiện:** Fetch 2 ngày UTC nếu cần, phân trang đầy đủ, lọc window, bỏ qua `syncLeaguesAndSeasons`.

### Task 3 — Repository Integration
- **Files:** [MatchRepositoryImpl.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/repository/MatchRepositoryImpl.kt)
- **Trách nhiệm:** Cập nhật `refreshMatchesForDate(date: String)` để sử dụng phương thức sync theo timezone mới.

### Task 4 — Presentation Status Resolution Fix
- **Files:** [PredictableMatchCard.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/components/PredictableMatchCard.kt)
- **Trách nhiệm:** Cập nhật `resolveDisplayStatus` theo ma trận tại Mục 8.

### Task 5 — ViewModel Refresh & Concurrency Refactor
- **Files:** [PredictionViewModel.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt)
- **Trách nhiệm:** Quản lý `syncJob`, tránh race condition khi pull-to-refresh hoặc đổi ngày nhanh.

### Task 6 — Unit & Integration Test Suite
- **Files:** Test files trong `:core:data` và `:app`.
- **Trách nhiệm:** Viết test xác thực match 129243, timezone boundary và regression tests.

---

## 13. Test Plan

### 13.1. Unit Tests
- `LocalDateRangeMapperTest`: Kiểm tra timezone conversion và UTC boundary splits.
- `ResolveDisplayStatusTest`:
  - Match pending + kickoff đã qua 8 tiếng -> Trả về `UPCOMING` (không trả về `LIVE`).
  - Match live từ API -> Trả về `LIVE`.
  - Match ended từ API -> Trả về `ENDED`.
- `DataSyncEngineRuntimeTest`:
  - Mock API trả về nhiều trang trên 2 ngày UTC -> Kiểm tra lưu đủ và lọc đúng ngày local.

### 13.2. Integration & Verification
- Kiểm tra trực tiếp trên Match 129243 (Greece vs Netherlands): Khi chọn ngày `2026-10-02`, match 129243 được kéo về, lưu vào Room với `status = ended`, tỷ số `2-2` và hiển thị `FT` trên UI.

---

## 14. Acceptance Criteria

- [ ] Match ID 129243 (Greece vs Netherlands) xuất hiện đầy đủ khi chọn ngày `2026-10-02` trên `PredictionScreen`.
- [ ] Match ID 129243 hiển thị trạng thái `FT` (Ended) với tỷ số `2 - 2`.
- [ ] Không chỉ query duy nhất `date=2026-10-02` (UTC) cho ngày cục bộ `2026-10-02` (Việt Nam).
- [ ] Pagination được duyệt đầy đủ cho đến `last_page`.
- [ ] Pull-to-refresh không gọi `getCompetitionsList` hay `getCompetitionSeasons` tuần tự (loại bỏ 100 HTTP requests).
- [ ] Thời gian Pull-to-refresh giảm xuống mức tối ưu (< 1 giây trên mạng tiêu chuẩn).
- [ ] Match `pending` có kickoff trong quá khứ quá lâu không bị hiển thị `LIVE`.
- [ ] Room Database luôn là Single Source of Truth cho UI.
- [ ] Toàn bộ thuật toán dự đoán, Elo, Form, H2H giữ nguyên tính đúng đắn.
- [ ] Toàn bộ Unit Tests hiện có tiếp tục PASS (Green).

---

## 15. Risks & Open Questions

1. **Rate Limiting trên Remote API khi query 2 ngày UTC liên tiếp**:
   - *Đánh giá:* 2 ngày UTC x 1-2 pages = tối đa 3-4 requests, hoàn toàn an toàn và nhỏ hơn rất nhiều so với 104 requests hiện tại.
2. **Xử lý DST (Daylight Saving Time)**:
   - Sử dụng `ZoneId.of("Asia/Ho_Chi_Minh")` hoặc `ZoneId.systemDefault()` thông qua Java `java.time` API tự động xử lý chính xác DST mà không cần hardcode offset.
