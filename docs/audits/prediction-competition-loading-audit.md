# Báo cáo Kiểm toán Forensic: Gom nhóm Giải đấu & Trạng thái Loading trên PredictionScreen

**Ngày:** 03/10/2026  
**Phạm vi:** Kiểm toán READ-ONLY & Trace Forensic trên `PredictionScreen`, `PredictionViewModel`, Room DAO/Mappers, Data Sync Engine và Pipeline Phân giải Metadata Giải đấu (Competition Resolution).  
**Mục tiêu Bất biến:** Đảm bảo `PredictionScreen` tuyệt đối không bao giờ gom các giải đấu riêng biệt vào cùng một nhóm fallback nhân tạo `"Giải đấu"`, và không bao giờ hiển thị dữ liệu trung gian chưa hydrate trước khi hoàn tất nạp metadata giải đấu.

---

## 1. Tóm tắt Tổng quan

Một lỗi nghiêm trọng trong cơ chế gom nhóm UI kết hợp với vòng đời đồng bộ bất đồng bộ giữa Room và API trên `PredictionScreen` đã khiến nhiều trận đấu thuộc các giải đấu chính thức khác nhau (ví dụ: FIFA ASEAN Cup, CONCACAF Nations League, UEFA Nations League, El Salvador Primera Division) bị dồn chung vào một tiêu đề giải đấu nhân tạo duy nhất mang tên **"Giải đấu"**.

Quá trình kiểm toán đã xác định được 2 nguyên nhân cốt lõi và 1 khiếm khuyết phụ trong vòng đời ứng dụng:
1. **Xung đột Khóa Gom nhóm (Grouping Key Collision - `PredictionScreen.kt:113`):**  
   Biểu thức `match.leagueId?.toString() ?: match.leagueName ?: "default_group"` gán tất cả các trận đấu chưa phân giải được metadata giải đấu vào chung một khóa `"default_group"`, gom tất cả các trận mồ côi vào một nhóm và hiển thị tiêu đề fallback `"Giải đấu"` qua dòng 119.
2. **Hiển thị Cache Trung gian & Race Condition Đồng bộ:**  
   `PredictionViewModel` phát ngay các bản ghi trong cache SQLite của Room khi mở màn hình trong khi `syncDateIfNeeded` đang chạy ngầm trong background. Nếu các trận trong Room chưa kịp phân giải liên kết `LeagueEntity`, các trận đấu chưa hoàn chỉnh sẽ lập tức được hiển thị mà không có cổng kiểm soát loading ở cấp danh sách (list-level loading gate).
3. **Trạng thái Loading Thiếu Phân cấp:**  
   `PredictionUiState.Loading` trước đây chỉ được dùng như một spinner ở đáy danh sách trong lúc tính toán dự đoán cho trận đấu được chọn, thay vì kiểm soát tính sẵn sàng của toàn bộ danh sách trận đấu và metadata giải đấu.

---

## 2. Luồng Kiến trúc Dữ liệu End-to-End

```
Remote Match API (/sport/v1.0/matches)
    │  (Trả về MatchRecord kèm competition_id & tóm tắt competition)
    ▼
RoomMappers
    ├─ MatchRecord.toMatchEntity()  ──> MatchEntity (leagueId: Int?)
    ├─ MatchRecord.toLeagueEntity() ──> LeagueEntity (id, name, logo)
    └─ MatchRecord.toHome/AwayTeamEntity()
    ▼
Database Transaction (DataSyncEngine / Room)
    ├─ leagueDao.insertLeagues(matchLeagues)
    ├─ teamDao.insertTeams(teams)
    └─ matchDao.upsertMatches(matches)
    ▼
Room Query (MatchDao.getPredictableMatchesFiltered)
    │  MatchWithTeams (@Relation: homeTeam, awayTeam, league)
    ▼
RoomMappers.toDomain()
    │  Match(leagueId = match.leagueId, leagueName = league?.name, leagueLogo = league?.logo)
    ▼
PredictionViewModel (matchesFlow -> availableMatches)
    │  Phát List<Match>
    ▼
PredictionScreen (remember block groupedMatches)
    │  groupBy: match.leagueId?.toString() ?: match.leagueName ?: "default_group"
    ▼
Render UI (CompetitionSectionHeader + PredictableMatchCard)
```

---

## 3. Phân tích Chuyên sâu 12 Điểm Forensic

### Điểm 1: Thông tin Giải đấu từ Match API
- **Endpoint:** `GET /sport/v1.0/matches?date={date}&page={page}&page_size=50&sort=time_asc`
- **Cấu trúc DTO (`MatchDto.kt`):**
  ```kotlin
  data class MatchRecord(
      val id: Long,
      val homeTeam: TeamInfo,
      val awayTeam: TeamInfo,
      val homeScore: Int,
      val awayScore: Int,
      val startTimeDate: String,
      val status: String,
      val minutes: String? = null,
      val competition_id: Int? = null,
      val competition: CompetitionSummaryInfo? = null
  )
  ```
- **Kết luận:** Match API cung cấp đầy đủ cả `competition_id` lẫn đối tượng `competition` (chứa `id`, `name`, `short_name`, `logo`).

---

### Điểm 2: Ánh xạ DTO → MatchEntity
- **Triển khai (`RoomMappers.kt:200-210`):**
  ```kotlin
  fun MatchRecord.toMatchEntity() = MatchEntity(
      id = id,
      homeTeamId = homeTeam.id,
      awayTeamId = awayTeam.id,
      homeScore = homeScore,
      awayScore = awayScore,
      startTimeDate = startTimeDate,
      status = status,
      leagueId = competitionId ?: competition?.id,
      minutes = minutes
  )
  ```
- `leagueId` được trích xuất từ `competitionId ?: competition?.id`.
- `leagueName` **không** được lưu trực tiếp trong `MatchEntity` (theo chuẩn schema quan hệ chuẩn hóa).
- Nếu cả `competitionId` và `competition?.id` đều null, `MatchEntity.leagueId` sẽ lưu giá trị `null`.

---

### Điểm 3: Ánh xạ MatchEntity + LeagueEntity → Domain Match
- **Triển khai (`RoomMappers.kt:151-164`):**
  ```kotlin
  fun MatchWithTeams.toDomain() = Match(
      id = match.id,
      homeTeam = TeamSummary(id = homeTeam.id, name = homeTeam.name, logo = homeTeam.logo),
      awayTeam = TeamSummary(id = awayTeam.id, name = awayTeam.name, logo = awayTeam.logo),
      homeScore = match.homeScore,
      awayScore = match.awayScore,
      startTimeDate = match.startTimeDate,
      status = MatchStatus.fromCode(match.status),
      leagueId = match.leagueId,
      leagueName = league?.name,
      leagueLogo = league?.logo,
      season = match.season,
      minutes = match.minutes
  )
  ```
- `Match.leagueName` được lấy hoàn toàn từ `league?.name` (`LeagueEntity` được phân giải thông qua `@Relation` của Room).
- Nếu `match.leagueId` là null HOẶC bản ghi `LeagueEntity` được tham chiếu chưa tồn tại trong bảng `leagues` khi Room thực thi truy vấn, `league` sẽ là `null`, dẫn đến `leagueName = null` và `leagueLogo = null`.

---

### Điểm 4: Ràng buộc Khóa Ngoại & Quan hệ trong Room
- **Định nghĩa Entity (`MatchEntity.kt:28-32`):**
  ```kotlin
  ForeignKey(
      entity = LeagueEntity::class,
      parentColumns = ["id"],
      childColumns = ["leagueId"],
      onDelete = ForeignKey.SET_NULL
  )
  ```
- **Định nghĩa Relation (`MatchEntity.kt:74-78`):**
  ```kotlin
  @Relation(
      parentColumn = "leagueId",
      entityColumn = "id"
  )
  val league: LeagueEntity? = null
  ```
- `@Relation` của Room thực thi một truy vấn con tương tự như `LEFT JOIN`.
- **Race Condition:** Nếu các trận đấu được chèn vào trước khi các bản ghi `LeagueEntity` tương ứng được commit, hoặc nếu các bản ghi `matches` tồn tại từ lần crawl cũ chưa có `LeagueEntity`, Room sẽ gán `league = null`.

---

### Điểm 5: Phân tích Cơ chế Fallback ("Giải đấu")
- **Vị trí Code Chính xác (`PredictionScreen.kt:116-122`):**
  ```kotlin
  val leagueId = matchesInGroup.firstNotNullOfOrNull { it.leagueId }
  val leagueName = matchesInGroup.firstNotNullOfOrNull { it.leagueName }
      ?: leagueId?.let { id -> leagueNameMap[id] }
      ?: "Giải đấu"
  ```
- **Điều kiện Kích hoạt:**
  1. `it.leagueName` là `null` cho mọi trận đấu trong nhóm.
  2. `leagueId` hoặc là `null`, hoặc không tồn tại trong `leagueNameMap` (lấy từ `leagueRepository.getLeagues()`).
  3. Cơ chế fallback mặc định trả về chuỗi hardcode `"Giải đấu"`.

---

### Điểm 6: Khiếm khuyết trong Logic Gom nhóm
- **Vị trí Code Chính xác (`PredictionScreen.kt:111-115`):**
  ```kotlin
  availableMatches
      .groupBy { match ->
          match.leagueId?.toString() ?: match.leagueName ?: "default_group"
      }
  ```
- **Phân tích Lỗi:**
  Khi nhiều trận đấu khác nhau có `leagueId == null` và `leagueName == null`:
  - Biểu thức `match.leagueId?.toString() ?: match.leagueName ?: "default_group"` sẽ trả về giá trị `"default_group"` cho **tất cả các trận này**.
  - Kết quả là các trận đấu thuộc hơn 10 giải đấu hoàn toàn không liên quan bị gom chung vào một nhóm mang khóa `"default_group"`.
  - Tiêu đề nhóm hiển thị `"Giải đấu"`, biến tất cả chúng thành một giải đấu nhân tạo duy nhất.

---

### Điểm 7: Trace Vòng đời PredictionViewModel
1. Mở màn hình $\to$ `PredictionViewModel` được khởi tạo.
2. `_selectedDate` nhận giá trị mặc định là ngày hiện tại.
3. `init` kích hoạt `syncDateIfNeeded(date)` trong coroutine chạy nền (`viewModelScope.launch(Dispatchers.IO)`).
4. Đồng thời, `matchesFlow` kết hợp các bộ lọc và gọi `matchRepository.getPredictableMatchesFiltered(...)`.
5. Room lập tức phát các bản ghi đã lưu trong cache SQLite trước khi quá trình đồng bộ mạng kết thúc.
6. `availableMatches` phát danh sách cache $\to$ `PredictionScreen` tính toán `groupedMatches` $\to$ Giao diện render ngay lập tức.
7. `uiState` tính toán dự đoán cho `autoSelectMatch(...)` và phát `PredictionUiState.Success`.
8. Khi `syncDateIfNeeded` hoàn tất và ghi dữ liệu mới vào Room, `matchesFlow` phát lại lần thứ hai, gây hiện tượng giật giao diện hoặc nhảy nhóm.

---

### Điểm 8: Hiện tượng Render Dữ liệu Cache Trung gian / Chưa hoàn chỉnh
- **Xác nhận:** `PredictionScreen.kt` render Phần 2 (`groupedMatches`) bất cứ khi nào `groupedMatches.isNotEmpty()`.
- Màn hình **không** chờ `isRefreshing` hay `syncDateIfNeeded` hoàn thành trước khi hiển thị danh sách trận đấu.
- Nếu dữ liệu trong cache chứa các trận đấu chưa được phân giải tên giải đấu, chúng sẽ lập tức bị hiển thị dưới nhóm `"Giải đấu"`.

---

### Điểm 9: Kiến trúc Trạng thái Loading
- **Hiện trạng:** `PredictionUiState` bao gồm:
  - `Loading`: Chỉ bao bọc quá trình tính toán của `predictMatchOutcomeUseCase` cho `selectedMatch`.
  - `Empty`: Chỉ hiển thị khi `matches.isEmpty()`.
  - `Error`: Hiển thị khi xảy ra lỗi tính toán.
  - `Success`: Hiển thị bảng kết quả dự đoán chi tiết.
- **Khiếm khuyết:** Không có trạng thái biểu thị `LoadingMatches` (Đang tải danh sách) hoặc `SyncingMetadata` (Đang đồng bộ metadata). Danh sách trận đấu luôn render vô điều kiện phía trên spinner dự đoán.

---

### Điểm 10 & 11: Trace Các Trường hợp Cụ thể

#### Trường hợp 1: Vietnam vs Pakistan (02/10/2026)
- **Giờ thi đấu:** 02/10/2026 (Giờ địa phương Việt Nam)
- **Tỷ số:** Vietnam 4 - 2 Pakistan (Kết thúc)
- **Giải đấu gốc:** FIFA ASEAN Cup (ID Giải đấu: `2239057`)
- **Phản hồi API:** `MatchRecord(competition_id = 2239057, competition = { id: 2239057, name: "FIFA ASEAN Cup" })`
- **Chính sách Chất lượng (Quality Policy):** Nằm trong whitelist `DefaultCompetitionQualityPolicy` ở mục `TIER_3_OFFICIAL_INTERNATIONAL` (`2239057`).
- **Nơi thất thoát metadata:** Khi lưu trong cache trước khi đồng bộ giải đấu, `leagueId = null` hoặc `LeagueEntity` chưa có trong Room.
- **Kết quả gom nhóm:** Được đánh giá thành `"default_group"`, bị dồn vào `"Giải đấu"` cùng với các trận mồ côi khác.

#### Trường hợp 2: Greece vs Netherlands (02/10/2026)
- **Tỷ số:** 2 - 2 (Kết thúc)
- **Giải đấu gốc:** UEFA Nations League (Competition: `UEFA Nations League`)
- **Kết quả gom nhóm:** Hiển thị dưới `"Giải đấu"`.

#### Trường hợp 3: Curacao vs Trinidad and Tobago (02/10/2026)
- **Tỷ số:** 1 - 0 (Kết thúc)
- **Giải đấu gốc:** CONCACAF Nations League (ID Giải đấu: `2484`)
- **Chính sách Chất lượng:** Nằm trong whitelist `DefaultCompetitionQualityPolicy` ở mục `TIER_3_OFFICIAL_INTERNATIONAL` (`2484`).
- **Kết quả gom nhóm:** Hiển thị dưới `"Giải đấu"`.

#### Trường hợp 4: China (W) vs South Korea (W) (02/10/2026)
- **Tỷ số:** 0 - 1 (Kết thúc)
- **Giải đấu gốc:** OCA Women's Asian Games (ID Giải đấu: `1756`)
- **Chính sách Chất lượng:** Nằm trong whitelist `DefaultCompetitionQualityPolicy` ở mục `TIER_3_OFFICIAL_INTERNATIONAL` (`1756`).
- **Kết quả gom nhóm:** Hiển thị dưới `"Giải đấu"`.

#### Trường hợp 5: Fuerte San Francisco vs Luis Angel Firpo (02/10/2026)
- **Tỷ số:** 0 - 3 (Kết thúc)
- **Giải đấu gốc:** El Salvador Primera Division (ID Giải đấu: `1106`)
- **Chính sách Chất lượng:** Nằm trong whitelist `DefaultCompetitionQualityPolicy` ở mục `TIER_2_SECOND_TIER_AND_DOMESTIC_CUPS` (`1106`).
- **Kết quả gom nhóm:** Hiển thị dưới `"Giải đấu"`.

---

### Điểm 12: Phân loại & Kết luận Nguyên nhân Gốc

| Loại nguyên nhân | Đánh giá | Chi tiết |
|---|---|---|
| **A. API không trả thông tin giải đấu** | ❌ Phủ định | API `/sport/v1.0/matches` có trả đầy đủ `competition_id` & tóm tắt `competition`. |
| **B. Mapper làm mất thông tin giải đấu** | ❌ Phủ định | `RoomMappers` ánh xạ đúng `competitionId ?: competition?.id`. |
| **C. Race condition đồng bộ Room** | ✅ **XÁC NHẬN** | Trận đấu được truy vấn và phát từ Room trước khi `LeagueEntity` được đồng bộ hoặc phân giải xong. |
| **D. Race condition JOIN / Relation** | ✅ **XÁC NHẬN** | `@Relation` trả `league = null` khi bảng `leagues` chưa có bản ghi tương ứng. |
| **E. ViewModel render trạng thái cache trung gian** | ✅ **XÁC NHẬN** | `PredictionViewModel` phát `availableMatches` ngay từ cache Room mà không có cổng kiểm soát đồng bộ. |
| **F. Lỗi Gom nhóm / Fallback** | 🔥 **NGUYÊN NHÂN GỐC NGHIÊM TRỌNG** | `match.leagueId?.toString() ?: match.leagueName ?: "default_group"` gom toàn bộ trận mồ côi vào cùng 1 khóa `"default_group"`. |

**Kết luận:** Sự kết hợp giữa **Xung đột Khóa Gom nhóm (F)** và **Render Cache Trung gian / Race Condition Quan hệ trong Room (C, D, E)**.

---

## 4. Phase 2 — Kế hoạch Triển khai Tối thiểu

### 4.1 Bất biến Kiến trúc
> **Bất biến Cốt lõi:** `PredictionScreen` TUYỆT ĐỐI KHÔNG ĐƯỢC render các trận đấu ứng viên cho đến khi dữ liệu giải đấu được phân giải hoặc quá trình đồng bộ ban đầu hoàn tất. Các giải đấu riêng biệt TUYỆT ĐỐI KHÔNG ĐƯỢC dùng chung một khóa gom nhóm.

### 4.2 Các bước Triển khai Đề xuất

#### Bước 1: Khóa Gom nhóm An toàn & Fallback Không Trùng lặp (`PredictionScreen.kt`)
- Thay đổi khóa gom nhóm để ngăn ngừa xung đột khóa:
  ```kotlin
  val groupKey = when {
      match.leagueId != null -> "league_${match.leagueId}"
      !match.leagueName.isNullOrBlank() -> "name_${match.leagueName}"
      else -> "orphan_match_${match.id}" // Giữ các trận chưa phân loại ở các nhóm riêng biệt
  }
  ```
- Nếu một trận đấu thực sự không có metadata giải đấu từ nguồn, hiển thị chuỗi fallback đa ngôn ngữ: `stringResource(R.string.competition_unclassified)` (ví dụ: "Chưa phân loại" / "Unclassified") mà không gộp các giải đấu khác nhau lại với nhau.

#### Bước 2: Kiến trúc Loading 2 Giai đoạn trong `PredictionUiState`
- Cập nhật `PredictionUiState` hoặc bổ sung `isMatchListLoading: StateFlow<Boolean>`:
  - Giai đoạn 1: `isSyncing` / `isMatchListLoading`: Trong khi tiến trình đồng bộ ban đầu cho ngày được chọn đang chạy và cache trận đấu đang trống hoặc ở trạng thái trung gian, hiển thị skeleton/spinner loading toàn màn hình hoặc toàn danh sách.
  - Giai đoạn 2: `isPredictionLoading`: Khi danh sách trận đấu đã sẵn sàng và người dùng chọn một trận, tính toán prediction context và chỉ hiển thị loading indicator trên bottom sheet dự đoán / nút hành động.

#### Bước 3: Đảm bảo Giao dịch Nguyên tử & Hydration Giải đấu (`DataSyncEngine.kt` / `PredictionViewModel.kt`)
- Đảm bảo `syncMatchesForLocalDate` luôn commit `matchLeagues` vào `leagueDao` trong cùng một transaction cơ sở dữ liệu trước khi gọi `matchDao.upsertMatches(matches)`.
- Trong `PredictionViewModel`, kết hợp state `leagues` với `matchesFlow` để danh sách trận đấu chỉ được trang trí và phát ra khi metadata `leagues` đã được nạp đầy đủ.

#### Bước 4: Kiểm thử & Viết Unit Test
- Viết Unit test cho `PredictionViewModel` để xác thực danh sách trận không bị phát ra ở trạng thái gom nhóm chưa hydrate.
- Viết Unit test cho `PredictionScreen` xác nhận các trận Vietnam vs Pakistan, Greece vs Netherlands, và các trận khác được render dưới tiêu đề giải đấu riêng biệt hoặc tiêu đề fallback độc lập, không bao giờ bị gộp chung làm một.

---

## 5. Bảng Tổng kết Xác nhận

| Thành phần | Hành vi Lỗi Trước đây | Giải pháp Sửa đổi |
|---|---|---|
| **Khóa Gom nhóm** | `"default_group"` cho tất cả các giải null | `league_${id}` hoặc `name_${name}` hoặc `orphan_${id}` |
| **Tiêu đề Fallback** | Hardcode `"Giải đấu"` | Phân giải `leagueName` $\to$ `leagueNameMap[id]` $\to$ chuỗi `competition_unclassified` |
| **UX Loading Danh sách** | Render tức thì vô điều kiện dữ liệu cache Room chưa hoàn chỉnh | Loading 2 giai đoạn: Cổng loading danh sách trận + Cổng loading tính toán dự đoán |
| **Transaction Đồng bộ** | Tiềm ẩn race condition với Room flow | Đồng bộ hóa transaction + Kết hợp Flow hydration |

---

## 6. Kết quả Triển khai (Phase 2)

### 6.1 Các File Đã Chỉnh Sửa
1. `app/src/main/res/values/strings.xml`: Bổ sung `competition_unclassified` ("Unclassified").
2. `app/src/main/res/values-en/strings.xml`: Bổ sung `competition_unclassified` ("Unclassified").
3. `app/src/main/res/values-vi/strings.xml`: Bổ sung `competition_unclassified` ("Chưa phân loại").
4. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt`:
   - Thêm `isMatchListLoading: StateFlow<Boolean>`.
   - Gắn `isMatchListLoading` vào `syncDateIfNeeded(date)` và `refresh()`.
   - Reset `isMatchListLoading = true` khi chuyển ngày trong tiến trình đồng bộ ban đầu.
5. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/PredictionScreen.kt`:
   - Áp dụng khóa gom nhóm chống va chạm: `when { match.leagueId != null -> "league_${match.leagueId}" ... }`.
   - Thay thế tiêu đề fallback hardcode `"Giải đấu"` bằng `stringResource(R.string.competition_unclassified)`.
   - Triển khai Giao diện Loading Giai đoạn 1: Hiển thị loading indicator cho danh sách khi `isMatchListLoading == true`, ngăn chặn render trung gian/chưa hydrate.
   - Bảo toàn vòng đời tính toán Dự đoán Giai đoạn 2 và hiển thị evidence.
6. `app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/PredictionGroupingTest.kt`:
   - Thêm bộ test suite kiểm tra chống va chạm khóa gom nhóm, gom nhóm fallback ổn định và các test case regression.
7. `app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModelTest.kt`:
   - Thêm các test case xác thực chuyển đổi trạng thái `isMatchListLoading` và reset khi chuyển ngày.

### 6.2 Xác thực Build & Kiểm thử
- `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL (83 tasks, 100% pass)**
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL (109 tasks, APK generated)**

---

## 7. Checklist Xác thực Thủ công trên Thiết bị Thật
1. Chạy `./gradlew installDebug` để cài đặt bản build mới lên thiết bị thử nghiệm.
2. Mở **PredictionScreen** từ thanh điều hướng dưới đáy màn hình.
3. Quan sát **Giao diện Loading Giai đoạn 1** (spinner ở giữa vùng danh sách) trong khi tiến trình đồng bộ ban đầu cho ngày được chọn đang chạy.
4. Xác nhận danh sách trận đấu hiển thị với các tiêu đề giải đấu chính xác:
   - **Vietnam vs Pakistan** hiển thị dưới nhóm **FIFA ASEAN Cup** (KHÔNG nằm dưới `"Giải đấu"`).
   - **Greece vs Netherlands** hiển thị dưới nhóm **UEFA Nations League**.
   - **Curacao vs Trinidad and Tobago** hiển thị dưới nhóm **CONCACAF Nations League**.
   - **China (W) vs South Korea (W)** hiển thị dưới nhóm **OCA Women's Asian Games**.
   - **Fuerte San Francisco vs Luis Angel Firpo** hiển thị dưới nhóm **El Salvador Primera Division**.
5. Chuyển ngày sang "Hôm qua" (02/10/2026) / "Hôm nay":
   - Quan sát hiệu ứng chuyển đổi trạng thái Loading Giai đoạn 1 mượt mà.
   - Xác nhận không còn xuất hiện tiêu đề nhóm nhân tạo `"Giải đấu"`.
6. Chọn một trận đấu bất kỳ và bấm "Dự đoán trận này →":
   - Xác nhận **Loading Dự đoán Giai đoạn 2** và các phép tính trong bottom sheet (xác suất, trọng số, Form, Goals, Elo, Rest) hoạt động trơn tru.
7. Thực hiện thao tác vuốt để làm mới (Pull-to-refresh) trên PredictionScreen:
   - Xác nhận quá trình refresh hoàn tất mà không bị giật giao diện từ "Giải đấu" sang các tiêu đề giải đấu thực tế.
