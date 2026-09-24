# TrueLab – Kế hoạch Triển khai Presentation & UI Integration (Domain D4 Plan)

> **Tài liệu tham chiếu chuẩn xác (Single Source of Truth cho Giai đoạn D4)**  
> **Phiên bản:** 2.1 (Chuẩn hóa 9 Domain UseCases, Tách bạch Scope Home/Away & H2H, Đối chiếu toàn diện với **README Mục 7 — Cấu trúc màn hình ứng dụng**)  
> **Trạng thái:** DRAFT FOR REVIEW (Sẵn sàng phê duyệt trước khi code)  
> **Nguyên tắc bất biến:** KHÔNG CODE, KHÔNG SỬA THUẬT TOÁN (:core:algorithm FROZEN 122/122), KHÔNG SỬA TEST BASELINE (:core:domain 156/156), KHÔNG BREAKING CHANGES.

---

## 1. D4 Scope & README Section 7 Traceability

Đối chiếu chi tiết từng yêu cầu được cam kết trong **README Mục 7 (Application Screens)** với nền tảng Domain/Data hiện tại của TrueLab, phân loại rành mạch trạng thái triển khai trong Giai đoạn D4:

| README Screen | Phân loại | Tính năng / Yêu cầu chi tiết | Trạng thái D4 | Phân kỳ Triển khai | Ghi chú & Phụ thuộc Kỹ thuật |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **1. 🏠 Home Screen** | Bottom Nav 1 | Thống kê Dataset (Số đội, trận, provider, DB version, last update) | **DEFERRED** | D6 (Data Pipeline / DB Sync) | Hiện chưa có DAO `COUNT(*)` và usecase tổng hợp metadata. Giữ nguyên số liệu thống kê tĩnh trên `DatasetOverviewCard`. |
| | | Core Feature Cards điều hướng (Prediction, Benchmark, Settings) | **EXISTING / PRESERVED** | D4.0 (Preserve) | Đã hoạt động mượt mà trong `HomeScreen.kt` và `MainScreen.kt`. D4 bảo toàn 100% flow điều hướng này. |
| | | Algorithm Development Progress Card | **EXISTING / PRESERVED** | D4.0 (Preserve) | Giữ nguyên hiển thị tiến độ 11 thuật toán trong `AlgorithmProgressCard`. |
| **2. ⚽ Matches Screen** | Bottom Nav 2 | Duyệt toàn bộ tập dữ liệu trận đấu (Dataset browser) | **IMPLEMENT IN D4** | D4.1.A | Nạp từ `MatchRepository.getMatches()`. Thay thế mock `sampleRecords` bằng real `StateFlow`. |
| | | Tìm kiếm trận đấu (Search Matches) | **IMPLEMENT IN D4** | D4.1.A | Tích hợp `SearchMatchesUseCase` (Phase 1 Linear Search). |
| | | Sắp xếp đa tiêu chí (Multi-criteria Sorting) | **IMPLEMENT IN D4** | D4.1.A | Tích hợp `SortMatchesUseCase` (Phase 2 MergeSort) với `MatchSortCriteria`. |
| | | Bộ lọc theo Trạng thái / Dữ liệu (Status Filter) | **IMPLEMENT IN D4** | D4.1.A | Lọc theo `MatchStatus` (`ALL`, `ENDED`, `SCHEDULED`). |
| | | Bộ lọc nâng cao theo Giải đấu / Mùa giải (League/Season Filter) | **DEFERRED** | D6 (Schema Expansion) | **Data Gap**: `MatchEntity` và Domain `Match` hiện chưa có cột/trường `league` hay `season`. Trì hoãn cho tới khi mở rộng schema. |
| | | Chi tiết trận đấu (Match Detail) | **IMPLEMENT IN D4** | D4.1.B | Nạp từ `MatchRepository.getMatchDetail(matchId)`. Tích hợp `MatchDetailBottomSheet` khi bấm vào thẻ trận đấu, hỗ trợ nút bấm "Dự đoán trận này". |
| **3. 🛡️ Teams Screen** | Bottom Nav 3 | Hồ sơ đội bóng (Team Profile: Rank, Avatar, Tên, Giải đấu) | **IMPLEMENT IN D4** | D4.2 | Nguồn từ `TeamRepository.getSeasonRanking` / `TeamSummary`. |
| | | Tìm kiếm đội bóng theo tên & ID | **IMPLEMENT IN D4** | D4.2 | Tích hợp `SearchTeamsUseCase` (Linear Search / Binary Search). |
| | | Bảng xếp hạng & Tie-breakers đa tầng | **IMPLEMENT IN D4** | D4.2 | Tích hợp `SortSeasonRankingUseCase` (Phase 2 MergeSort) với `StandingsSortCriteria`. |
| | | Đánh giá phong độ (Form Score 5 trận gần nhất, W-D-L badges) | **IMPLEMENT IN D4** | D4.2 | Tích hợp `CalculateTeamFormUseCase` (Phase 5 Linear Time-Decay). |
| | | Hệ số sức mạnh Elo Rating | **IMPLEMENT IN D4** | D4.2 | Tích hợp `CalculateEloRatingUseCase` (Phase 6 Elo Rating) / `TeamDetail.eloRating`. |
| | | Thống kê sân nhà / sân khách (Home/Away Splits) | **DEFERRED** | D5 (Domain Expansion) | **Domain Gap**: Domain hiện chưa có UseCase chuyên biệt (`CalculateHomeAwaySplitsUseCase`). Mapper không tự ý chứa business logic tính toán. Hiển thị placeholder "—" trên UI. |
| | | Ma trận đối đầu trực tiếp (H2H Matrix) trên Teams Screen | **DEFERRED** | D5+ (Team Comparison Tool) | **Scope Gap**: Teams Screen là danh sách từng đội đơn lẻ. Ma trận đối đầu chi tiết yêu cầu công cụ chọn 2 đội đối kháng. DEFER hoàn toàn trên Teams Screen. |
| **4. 📈 Analytics Screen** | Bottom Nav 4 | Thống kê mô tả (Count, Mean, Median, StdDev, Variance, Skewness) | **IMPLEMENT IN D4** | D4.3 | Tích hợp `GetTeamStatisticsUseCase` (Phase 3 Stats) kết nối vào `DescriptiveStatsCard`. |
| | | So sánh tỷ lệ kèo đa nguồn (Multi-provider Odds Matrix) | **IMPLEMENT IN D4** | D4.3 | Nạp từ `OddsRepository.getMatchOdds(matchId)` và tổng hợp min, max, average odds vào `MultiProviderOddsCard`. |
| | | Biểu đồ xu hướng kèo & Biến động (Moving Average & Volatility) | **IMPLEMENT IN D4** | D4.3 | Tích hợp `AnalyzeOddsTrendUseCase` (Phase 4 SMA & Volatility) kết nối vào `OddsTrendCard`. |
| **5. 🔮 Prediction Screen** | Secondary 1 | Luồng chọn trận đấu (Match Selection Flow) | **IMPLEMENT IN D4** | D4.4 | Cho phép chọn trận từ danh sách trận đấu sẵn có hoặc nhận `matchId` từ Matches Screen / Match Detail. |
| | | Trích xuất đặc trưng (Feature Extraction Context) | **IMPLEMENT IN D4** | D4.4 | Thu thập Elo (Home/Away), 5 recent matches, H2H matches, latest odds để tạo `MatchPredictionContext`. |
| | | Lịch sử đối đầu 2 đội (H2H Matches for Prediction) | **IMPLEMENT IN D4** | D4.4 | Nạp trực tiếp từ `MatchRepository.getH2HMatches(homeTeamId, awayTeamId)` phục vụ dự đoán cặp trận cụ thể. |
| | | Chạy thuật toán dự đoán (Execute Prediction Engine) | **IMPLEMENT IN D4** | D4.4 | Tích hợp `PredictMatchOutcomeUseCase` (D1 Unified Weighted Scoring). |
| | | Hiển thị xác suất và kết quả dự đoán (Home/Draw/Away %) | **IMPLEMENT IN D4** | D4.4 | Render xác suất 3 chiều, độ tin cậy và giải thích tín hiệu vào `PredictionScreen`. |
| **6. ⏱️ Benchmark Screen** | Secondary 2 | Benchmark Tìm kiếm (Linear Search vs Binary Search) | **DEFERRED** | D5 (Benchmark & Evaluation) | Prototype UI `BenchmarkScreen.kt` đã có. Logic đo lường thực tế (execution time runner, synthetic dataset) sẽ triển khai trong Phase D5. |
| | | Benchmark Sắp xếp (QuickSort vs MergeSort) | **DEFERRED** | D5 (Benchmark & Evaluation) | Sẽ triển khai runner đo lường N = 1K, 10K, 50K trong Phase D5. |
| | | Đánh giá độ chính xác mô hình (Prediction Accuracy / Backtest) | **DEFERRED** | D5 (Benchmark & Evaluation) | Cần engine backtest đối chiếu với tập trận đã kết thúc (`isEnded == true`) và xây dựng Confusion Matrix trong Phase D5. |

---

## 2. Bối cảnh & Hiện trạng Hệ thống (D4 Context & Current State)

### 2.1. Thực trạng Tầng Presentation (`:app`):
- **Toàn bộ Màn hình Bóng đá Cốt lõi đang dùng Dữ liệu Mock / Prototype**:
  - `MatchesScreen.kt`: Chưa có `MatchesViewModel`. Danh sách trận đấu là `remember { sampleRecords }` gồm 5 item tĩnh. `onMatchClick` chưa được nối vào Navigation hay Dialog nào. Ô tìm kiếm và chip sắp xếp chỉ lưu state cục bộ trong Composable bằng `remember`.
  - `TeamsScreen.kt`: Chưa có `TeamsViewModel`. Danh sách là `remember { sampleTeams }` gồm 6 item tĩnh.
  - `AnalyticsScreen.kt`: Chưa có `AnalyticsViewModel`. Ba component con (`DescriptiveStatsCard`, `MultiProviderOddsCard`, `OddsTrendCard`) hiển thị dữ liệu tĩnh hardcoded.
  - `PredictionScreen.kt`: Chưa có `PredictionViewModel`. Toàn bộ dữ liệu hiển thị ("Man City" vs "Liverpool", tỷ lệ 54% / 24% / 22%) là tĩnh; nút bấm "Calculate" có `onClick = {}`.
  - `BenchmarkScreen.kt`: Chưa có `BenchmarkViewModel`. Giao diện prototype hiển thị kết quả giả lập ("1.420 ms", "8.350 ms") và nút bấm `onClick = {}`.
  - `HomeScreen.kt`: Chưa có `HomeViewModel`. Thẻ `DatasetOverviewCard` hiển thị các số liệu hằng số ("75,000 matches", "120 teams", "4 providers"). Thẻ `AnalysisToolsSection` điều hướng tới `Prediction` và `Benchmark`.
- **ViewModels duy nhất hiện có trong `:app`**: Chỉ có `LanguageViewModel` và `ThemeViewModel` (quản trị đa ngôn ngữ và Light/Dark theme qua DataStore).

### 2.2. Khoảng trống Tiêu thụ UseCase (UseCase Consumption Gap):
Chính xác **9 Pure Computation UseCases** trong `:core:domain` đã hoàn thành 100% (156 tests domain + 122 tests algorithm) nhưng **chưa hề có consumer thực tế nào ở tầng UI**:
1. `SearchMatchesUseCase` (D3.1 — Tìm kiếm trận đấu)
2. `SearchTeamsUseCase` (D3.1 — Tìm kiếm đội bóng)
3. `SortMatchesUseCase` (D3.2 — Sắp xếp trận đấu)
4. `SortSeasonRankingUseCase` (D3.2 — Sắp xếp bảng xếp hạng)
5. `GetTeamStatisticsUseCase` (D2.4 — Thống kê mô tả hiệu suất đội bóng)
6. `AnalyzeOddsTrendUseCase` (D2.3 — Phân tích xu hướng biến động kèo)
7. `CalculateTeamFormUseCase` (D2.1 — Đánh giá phong độ thi đấu time-decay)
8. `CalculateEloRatingUseCase` (D2.2 — Cập nhật hệ số Elo)
9. `PredictMatchOutcomeUseCase` (D1 — Dự đoán xác suất kết quả trận đấu)

> [!NOTE]
> `GetMatchDetailUseCase` là một repository-bound wrapper nằm trong nhóm legacy use cases của `MatchUseCases.kt` (được cung cấp sẵn từ `MatchDataModule` ở `:core:data`). Trong D4, Presentation sẽ gọi trực tiếp `matchRepository.getMatchDetail(matchId)` để nạp chi tiết trận đấu, giữ nguyên baseline 9 Pure Computation UseCases của D1–D3.

### 2.3. Thực trạng Tầng Dữ liệu (`:core:data`):
- Room Database (`TrueLabDatabase`) và `DataSyncEngine` đã có sẵn.
- Đã có các DAO: `MatchDao`, `TeamDao`, `RankingDao`, `OddsDao`, `PredictionDao`.
- Đã có Repositories: `MatchRepositoryImpl`, `TeamRepositoryImpl`, `OddsRepositoryImpl`, `PredictionRepositoryImpl`.
- Tất cả phương thức repository hiện hữu đều trả về `Flow<T>`.

### 2.4. Các Đường dẫn Legacy Cần Bảo toàn:
- `MatchUseCases`, `TeamUseCases`, `OddsUseCases`, `PredictionUseCases` (được inject trong `:core:data` modules) tạm giữ nguyên.
- `PredictionResult.Companion.computeWeightedScoring` và `SeasonRanking.calculateFormScore()` (tạm giữ để bảo đảm tương thích, sẽ đánh dấu `@Deprecated` trong D4.5).
- Không tự ý xóa code legacy trong các sub-phase D4.1–D4.4.

---

## 3. Kiến trúc Mục tiêu D4 (Target Architecture)

Luồng kiến trúc tuân thủ nghiêm ngặt Unidirectional Data Flow (UDF) và Clean Architecture:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      Presentation Layer (:app)                         │
│                                                                        │
│   Compose Screens / BottomSheets ◄── StateFlow<UiState> ─── ViewModel  │
│         │                                                       │      │
│         └──────── User Actions (Search, Sort, SelectMatch) ─────┘      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Domain Layer (:core:domain)                     │
│                                                                        │
│     9 Pure Computation UseCases          Repository Interfaces         │
│     - SearchMatchesUseCase               - MatchRepository             │
│     - SortMatchesUseCase                 - TeamRepository              │
│     - PredictMatchOutcomeUseCase         - OddsRepository              │
│     - GetTeamStatisticsUseCase           (Return Flow<T>)              │
│     - CalculateTeamFormUseCase                                         │
│     - CalculateEloRatingUseCase                                        │
│     - AnalyzeOddsTrendUseCase                                          │
│     - SearchTeamsUseCase                                               │
│     - SortSeasonRankingUseCase                                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         Data Layer (:core:data)                        │
│                                                                        │
│   Repository Implementations ────► Room DAOs ────► SQLite Database     │
│   (MatchRepositoryImpl, etc.)                      (TrueLabDatabase)   │
└────────────────────────────────────────────────────────────────────────┘
```

### Nguyên tắc Bất biến:
1. **ViewModel là Single Source of Truth**: UI State (`UiState`) được quản lý bằng `StateFlow` trong ViewModel. Screen chỉ lắng nghe qua `collectAsStateWithLifecycle()` và phát sinh event.
2. **Không Bypass Domain**: ViewModel không được gọi trực tiếp Room DAO hay Retrofit API; mọi thao tác dữ liệu phải đi qua Repository Interface.
3. **Phân định Pure Computation vs Data Ingestion**:
   - Repository nạp dữ liệu thô (`List<Match>`, `List<OddsRecordItem>`).
   - 9 Pure Computation UseCases xử lý sắp xếp, lọc, tính điểm phong độ, Elo, và suy luận xác suất.
4. **Không Đặt Business Logic vào Mapper**: UI Mapper (`toUiRecord`) chỉ làm nhiệm vụ biến đổi kiểu dữ liệu (data transformation/formatting) giữa Domain Model và UI Model. Mọi phép tính toán nghiệp vụ (phong độ, Elo, phân phối) phải do Domain UseCase đảm nhiệm.
5. **Không Tạo Abstraction Thừa**: Không tạo các lớp BaseViewModel, MviEffect phức tạp khi dự án chưa có nhu cầu.

---

## 4. D4.1 — Matches Presentation Integration

Giai đoạn D4.1 được phân chia thành 2 sub-phases độc lập và rõ ràng:

### 4.1. D4.1.A — Matches List Integration (Browse, Search, Sort & Status Filter)
- **UseCases**:
  - `SearchMatchesUseCase` (Phase 1 Linear Search).
  - `SortMatchesUseCase` (Phase 2 MergeSort).
- **Data Source**: `MatchRepository.getMatches(date: String): Flow<List<Match>>`.
- **Bộ lọc Trạng thái (Status Filter)**:
  - Do `MatchEntity` và Domain `Match` chưa có thuộc tính `league` hay `season` (Data Gap hoãn sang D6), D4.1.A triển khai bộ lọc theo trạng thái thực tế:
    - `ALL`: Tất cả trận đấu.
    - `ENDED`: Trận đấu đã kết thúc (`match.isEnded == true`).
    - `SCHEDULED`: Trận đấu sắp diễn ra / chưa đấu.
- **Trạng thái Giao diện (`MatchesUiState`)**:
```kotlin
sealed interface MatchesUiState {
    data object Loading : MatchesUiState
    data class Success(
        val matches: List<MatchDataRecord>,
        val rawMatchesCount: Int,
        val searchQuery: String,
        val selectedSort: MatchSortCriteria,
        val selectedStatusFilter: MatchStatusFilter,
        val selectedMatchDetail: Match? = null
    ) : MatchesUiState
    data class Empty(val message: String) : MatchesUiState
    data class Error(val message: String) : MatchesUiState
}

enum class MatchStatusFilter { ALL, ENDED, SCHEDULED }
```

### 4.2. D4.1.B — Match Detail Integration (MatchDetailBottomSheet & Prediction CTA)
- **Vấn đề**: Hiện tại `MatchesScreen` có tham số `onMatchClick = {}` nhưng không làm gì, khiến tính năng xem chi tiết trận đấu trong README Section 7.1.2 chưa có kết nối.
- **Data Source**: `MatchRepository.getMatchDetail(matchId: Long): Flow<Match?>`.
- **Giải pháp Triển khai**:
  - Tạo Composable component `MatchDetailBottomSheet.kt` trong `feature/match/presentation/components/`.
  - Khi user chạm vào `MatchDataCard`, ViewModel gọi `onMatchClicked(matchId)`.
  - `MatchDetailBottomSheet` hiển thị đầy đủ: Tên 2 đội, Logo, Tỷ số trực tiếp/kết quả, Thời gian diễn ra, Trạng thái trận đấu.
  - Cung cấp nút CTA **"Dự đoán trận đấu này"**: Chuyển hướng ngay sang `PredictionScreen` kèm tham số `matchId` (khép kín luồng người dùng từ Matches $\to$ Match Detail $\to$ Prediction).

### 4.3. Thiết kế `MatchesViewModel`:
```kotlin
@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val searchMatchesUseCase: SearchMatchesUseCase,
    private val sortMatchesUseCase: SortMatchesUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortCriteria = MutableStateFlow(MatchSortCriteria.START_TIME_ASC)
    private val _statusFilter = MutableStateFlow(MatchStatusFilter.ALL)
    private val _selectedDate = MutableStateFlow("") // Rỗng = nạp tất cả hoặc ngày gần nhất
    private val _selectedMatchDetail = MutableStateFlow<Match?>(null)

    val uiState: StateFlow<MatchesUiState> = combine(
        _selectedDate.flatMapLatest { date -> matchRepository.getMatches(date) },
        _searchQuery,
        _sortCriteria,
        _statusFilter,
        _selectedMatchDetail
    ) { rawMatches, query, sort, filter, detail ->
        if (rawMatches.isEmpty()) {
            MatchesUiState.Empty("Không có trận đấu nào trong cơ sở dữ liệu")
        } else {
            // 1. Lọc theo trạng thái thực tế
            val statusFiltered = when (filter) {
                MatchStatusFilter.ALL -> rawMatches
                MatchStatusFilter.ENDED -> rawMatches.filter { it.isEnded }
                MatchStatusFilter.SCHEDULED -> rawMatches.filter { it.status == MatchStatus.SCHEDULED }
            }
            // 2. Tìm kiếm (LinearSearch partial theo tên đội bóng)
            val searchFiltered = searchMatchesUseCase(statusFiltered, query)
            // 3. Sắp xếp (MergeSort với signed goal diff hoặc start time)
            val sorted = sortMatchesUseCase(searchFiltered, sort)
            
            MatchesUiState.Success(
                matches = sorted.map { it.toUiRecord() },
                rawMatchesCount = rawMatches.size,
                searchQuery = query,
                selectedSort = sort,
                selectedStatusFilter = filter,
                selectedMatchDetail = detail
            )
        }
    }.catch { e ->
        emit(MatchesUiState.Error(e.message ?: "Lỗi tải danh sách trận đấu"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MatchesUiState.Loading
    )

    fun onSearchQueryChanged(newQuery: String) { _searchQuery.value = newQuery }
    fun onSortCriteriaChanged(newSort: MatchSortCriteria) { _sortCriteria.value = newSort }
    fun onStatusFilterChanged(newFilter: MatchStatusFilter) { _statusFilter.value = newFilter }
    
    fun onMatchClicked(matchId: Long) {
        viewModelScope.launch {
            matchRepository.getMatchDetail(matchId).collect { match ->
                _selectedMatchDetail.value = match
            }
        }
    }
    fun onDismissMatchDetail() { _selectedMatchDetail.value = null }
}
```

### 4.4. UI Mapper (`Match` $\to$ `MatchDataRecord`):
File `MatchUiMapper.kt`:
```kotlin
fun Match.toUiRecord(): MatchDataRecord = MatchDataRecord(
    id = this.id.toString(),
    league = "Football League",
    date = this.startTimeDate,
    homeTeam = this.homeTeam.name,
    awayTeam = this.awayTeam.name,
    homeScore = this.homeScore ?: 0,
    awayScore = this.awayScore ?: 0,
    actualResult = when {
        isHomeWin -> "HOME_WIN"
        isAwayWin -> "AWAY_WIN"
        isDraw -> "DRAW"
        else -> "SCHEDULED"
    },
    avgHomeOdds = 0.0,
    avgDrawOdds = 0.0,
    avgAwayOdds = 0.0,
    providerCount = 0,
    eloDiff = 0,
    totalGoals = this.totalGoals,
    isNormalized = true,
    predictedProb = ""
)
```

---

## 5. D4.2 — Teams & Standings Presentation Integration

### 5.1. Trách nhiệm & UseCases Sử dụng:
- **UseCases**: 
  - `SearchTeamsUseCase` (Phase 1): Tìm kiếm tên đội bóng và tra cứu ID.
  - `SortSeasonRankingUseCase` (Phase 2): Sắp xếp BXH với composite tie-breakers 4 tầng (`POINTS_DESC`, `GOAL_DIFF_DESC`, v.v.).
  - `CalculateEloRatingUseCase` (Phase 6): Tính toán / cập nhật điểm sức mạnh đối kháng.
  - `CalculateTeamFormUseCase` (Phase 5): Tính toán chuỗi phong độ time-decay (Linear Decayed FormScore trên thang 100).
- **Data Source**: `TeamRepository.getSeasonRanking(matchId: Long)` và `MatchRepository.getMatches(date: String)`.

### 5.2. Xử lý Minh bạch Khoảng trống Kỹ thuật (Scope Gaps):
- **1. Home/Away Splits**:
  - **Hiện trạng Domain**: Tầng Domain hiện **chưa có** `CalculateHomeAwaySplitsUseCase`.
  - **Quy tắc Kiến trúc**: Tuyệt đối **không** đưa logic tính toán W-D-L hay lọc trận sân nhà/sân khách vào `TeamUiMapper` (Mapper chỉ chịu trách nhiệm biến đổi kiểu dữ liệu).
  - **Quyết định D4**: Đánh dấu Home/Away Splits là **DEFERRED sang Phase D5 (Domain Expansion)** để xây dựng UseCase chuẩn chỉnh. Trong D4.2, các trường `homeWinRate`, `homeRecord`, `awayWinRate`, `awayRecord` trên UI sẽ hiển thị placeholder an toàn (`"—"` hoặc `"N/A"`).
- **2. H2H trên Teams Screen**:
  - **Hiện trạng**: Màn hình Teams Screen là danh sách từng đội đơn lẻ; ma trận đối đầu trực tiếp (H2H Matrix) giữa các cặp đội đòi hỏi bối cảnh chọn 2 đội đối kháng (Team Comparison Tool).
  - **Quyết định D4**: **DEFER HOÀN TOÀN** H2H trên Teams Screen sang **Phase D5+ (Team Comparison Tool)**. Không chắp vá trường `h2hHighlight` ad-hoc khi chưa có hợp đồng nghiệp vụ rõ ràng. Trường này trên UI sẽ để giá trị rỗng/mặc định.
  - **Phân biệt rành mạch**: Lịch sử đối đầu 2 đội (H2H) phục vụ cho dự đoán trận đấu cụ thể trong **Prediction D4.4** vẫn được triển khai đầy đủ 100% vì đã có contract `MatchPredictionContext.h2hMatches` và `MatchRepository.getH2HMatches(teamAId, teamBId)`.

### 5.3. Thiết kế `TeamsUiState`:
```kotlin
sealed interface TeamsUiState {
    data object Loading : TeamsUiState
    data class Success(
        val teams: List<TeamAnalyticsRecord>,
        val rawTeamsCount: Int,
        val searchQuery: String,
        val standingsSort: StandingsSortCriteria
    ) : TeamsUiState
    data class Empty(val message: String) : TeamsUiState
    data class Error(val message: String) : TeamsUiState
}
```

### 5.4. Thiết kế `TeamsViewModel`:
```kotlin
@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val searchTeamsUseCase: SearchTeamsUseCase,
    private val sortSeasonRankingUseCase: SortSeasonRankingUseCase,
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _standingsSort = MutableStateFlow(StandingsSortCriteria.POINTS_DESC)

    val uiState: StateFlow<TeamsUiState> = combine(
        matchRepository.getMatches(""),
        _searchQuery,
        _standingsSort
    ) { allMatches, query, sort ->
        if (allMatches.isEmpty()) {
            TeamsUiState.Empty("Không có dữ liệu đội bóng")
        } else {
            val uniqueTeams = extractUniqueTeams(allMatches)
            
            val teamRecords = uniqueTeams.map { team ->
                val teamMatches = allMatches.filter { it.homeTeam.id == team.id || it.awayTeam.id == team.id }
                val formScore = calculateTeamFormUseCase(team.id, teamMatches, windowSize = 5)
                
                TeamAnalyticsRecord(
                    id = team.id.toString(),
                    name = team.name,
                    league = "Football League",
                    eloRating = 1500, // Hoặc lấy từ TeamDetail nếu nạp
                    rank = 1,
                    played = teamMatches.size,
                    wins = teamMatches.count { it.isHomeWin && it.homeTeam.id == team.id || it.isAwayWin && it.awayTeam.id == team.id },
                    draws = teamMatches.count { it.isDraw },
                    losses = teamMatches.count { it.isAwayWin && it.homeTeam.id == team.id || it.isHomeWin && it.awayTeam.id == team.id },
                    form = formScore.recentOutcomes.map { when (it) { MatchOutcome.WIN -> 'W'; MatchOutcome.DRAW -> 'D'; MatchOutcome.LOSS -> 'L' } },
                    formScore = formScore.points,
                    homeWinRate = 0.0,
                    homeRecord = "—",
                    awayWinRate = 0.0,
                    awayRecord = "—",
                    h2hHighlight = ""
                )
            }

            // Lọc theo từ khóa tìm kiếm (LinearSearch partial theo tên)
            val filtered = if (query.isBlank()) teamRecords else {
                teamRecords.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }

            // Sắp xếp danh sách
            val sorted = sortTeamRecords(filtered, sort)

            TeamsUiState.Success(
                teams = sorted,
                rawTeamsCount = teamRecords.size,
                searchQuery = query,
                standingsSort = sort
            )
        }
    }.catch { e ->
        emit(TeamsUiState.Error(e.message ?: "Lỗi tải danh sách đội bóng"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeamsUiState.Loading
    )

    fun onSearchQueryChanged(newQuery: String) { _searchQuery.value = newQuery }
    fun onSortChanged(newSort: StandingsSortCriteria) { _standingsSort.value = newSort }
}
```

---

## 6. D4.3 — Analytics Presentation Integration (Thống kê Mô tả & Phân tích Odds)

### 6.1. Trách nhiệm & UseCases Sử dụng:
- **UseCases**:
  - `GetTeamStatisticsUseCase` (Phase 3 Statistics): Tính toán 8 chỉ số thống kê mô tả phân phối bàn thắng (Mean, Median, StdDev, Variance, Skewness, Min, Max, Range).
  - `AnalyzeOddsTrendUseCase` (Phase 4 Trend): Làm mịn chuỗi thời gian tỷ lệ cược (SMA) và đo lường độ biến động (Volatility).
- **Data Source**: `OddsRepository.getMatchOdds(matchId: Long)` và `OddsRepository.getOddsHistory(matchId, companyId, oddsType)`.

### 6.2. Phân tích So sánh Odds Đa Nguồn (Multi-Provider Odds Matrix):
- **Hiện trạng**: `MultiProviderOddsCard.kt` hiện hiển thị 4 nhà cung cấp mẫu: Bet365, William Hill, 1xBet, Pinnacle.
- **Nguồn Dữ liệu Thật**: `OddsRepository.getMatchOdds(matchId: Long)` trả về `MatchOdds(matchId, oddsList: List<OddsRecordItem>)`. Mỗi `OddsRecordItem` đã chứa: `companyName`, `homeWin`, `draw`, `awayWin`.
- **Triển khai D4**:
  - `AnalyticsViewModel` nạp dữ liệu odds từ trận đấu được chọn (hoặc trận đấu đầu tiên có dữ liệu odds).
  - Ánh xạ `oddsList` sang cấu trúc bảng so sánh của `MultiProviderOddsCard`.
  - Tính toán: Tỷ lệ trung bình (Average Odds), Tỷ lệ cao nhất (Max Odds), Độ lệch biên lợi nhuận giữa các nhà cái.

### 6.3. Thiết kế `AnalyticsUiState`:
```kotlin
sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val teamStats: TeamPerformanceStatistics?,
        val oddsAnalysis: OddsTrendAnalysis?,
        val multiProviderOdds: List<OddsRecordItem>,
        val analyzedMatchName: String,
        val selectedWindowSize: Int
    ) : AnalyticsUiState
    data class Empty(val message: String) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}
```

### 6.4. Kết nối UI Components:
- **`DescriptiveStatsCard`**: Đọc dữ liệu từ `uiState.teamStats.totalGoalsStats` thay cho các con số tĩnh.
- **`MultiProviderOddsCard`**: Render danh sách các nhà cái thực tế từ `uiState.multiProviderOdds`.
- **`OddsTrendCard`**: Đọc chuỗi `uiState.oddsAnalysis.smaSeries`, `currentOdds`, `openingOdds`, và `volatility` để vẽ biểu đồ và hiển thị badge biến động thực tế.

---

## 7. D4.4 — Prediction Presentation Integration (Dự đoán Trận đấu 3 Chiều)

Đây là phân kỳ trọng tâm và đòi hỏi sự phối hợp chặt chẽ nhất giữa các tầng.

### 7.1. Luồng Người dùng Chuẩn theo README Section 7.2:
Theo đặc tả trong README:
$$\text{Chọn trận đấu} \longrightarrow \text{Trích xuất đặc trưng} \longrightarrow \text{Chạy thuật toán} \longrightarrow \text{Xác suất \& Kết quả}$$

Luồng chọn trận đấu được hỗ trợ theo 2 lối vào:
1. **Lối vào 1 (Từ Matches Screen / Match Detail)**: User xem danh sách trận đấu $\to$ bấm vào một trận $\to$ bấm "Dự đoán trận này" $\to$ Điều hướng sang `PredictionScreen` truyền theo `matchId`.
2. **Lối vào 2 (Từ Home Screen)**: User bấm thẻ "Prediction" $\to$ `PredictionScreen` tự động chọn trận đấu sắp diễn ra đầu tiên trong cơ sở dữ liệu, đồng thời cung cấp bộ chọn (Dropdown / Selector) để đổi sang trận đấu khác nếu muốn.

### 7.2. Phân tích Dữ liệu Đầu vào của `PredictMatchOutcomeUseCase`:
`PredictMatchOutcomeUseCase` yêu cầu ngữ cảnh [MatchPredictionContext](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/MatchPredictionContext.kt):
1. `matchId: Long`, `homeTeamId: Int`, `awayTeamId: Int`
2. `homeElo: Double?`, `awayElo: Double?`
3. `homeRecentMatches: List<Match>`, `awayRecentMatches: List<Match>`
4. `h2hMatches: List<Match>`
5. `latestOdds: OddsRecordItem?`
6. `isNeutralVenue: Boolean`

### 7.3. Khoảng trống Dữ liệu (Data Layer Gap) & Kế hoạch Khắc phục:
- **Thực tế trong Room DAO (`MatchDao`)**:
  `MatchDao` đã có sẵn các câu query Room hoàn chỉnh:
  - `getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<MatchWithTeams>>`
  - `getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<MatchWithTeams>>`
- **Khoảng trống tại Repository Interface (`MatchRepository`)**:
  Interface `MatchRepository` hiện chỉ mới có:
  ```kotlin
  fun getMatches(date: String): Flow<List<Match>>
  fun getMatchDetail(matchId: Long): Flow<Match?>
  ```
  Chưa expose `getRecentMatchesForTeam` và `getH2HMatches` lên Domain.

#### 🎯 Giải pháp Mở rộng Chuẩn mực (Clean Architecture Extension):
1. **Tại `:core:domain` ([MatchRepository.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/repository/MatchRepository.kt))**:
   Bổ sung 2 contracts (không breaking change, chỉ mở rộng):
   ```kotlin
   fun getRecentMatchesForTeam(teamId: Int, limit: Int = 5): Flow<List<Match>>
   fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>>
   ```
2. **Tại `:core:data` ([MatchRepositoryImpl.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/repository/MatchRepositoryImpl.kt))**:
   Triển khai ủy thác trực tiếp từ `matchDao`:
   ```kotlin
   override fun getRecentMatchesForTeam(teamId: Int, limit: Int): Flow<List<Match>> {
       return matchDao.getRecentMatchesForTeam(teamId, limit).map { list -> list.map { it.toDomain() } }
   }

   override fun getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>> {
       return matchDao.getH2HMatches(teamAId, teamBId).map { list -> list.map { it.toDomain() } }
   }
   ```
3. **Nguồn Elo & Odds**:
   - `homeElo`, `awayElo`: Lấy từ `TeamRepository.getTeamDetail(teamId)` (`TeamDetail.eloRating`).
   - `latestOdds`: Lấy từ `OddsRepository.getOddsHistory(matchId, null, null)` phần tử mới nhất.

### 7.4. Thiết kế `PredictionUiState`:
```kotlin
sealed interface PredictionUiState {
    data object Idle : PredictionUiState
    data object Loading : PredictionUiState
    data class Success(
        val match: Match,
        val availableMatches: List<Match>,
        val prediction: PredictionResult,
        val homeElo: Double,
        val awayElo: Double,
        val homeRecentForm: List<Char>,
        val awayRecentForm: List<Char>
    ) : PredictionUiState
    data class Error(val message: String) : PredictionUiState
}
```

### 7.5. Thiết kế `PredictionViewModel`:
```kotlin
@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val oddsRepository: OddsRepository,
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PredictionUiState>(PredictionUiState.Idle)
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    private val _availableMatches = MutableStateFlow<List<Match>>(emptyList())
    val availableMatches: StateFlow<List<Match>> = _availableMatches.asStateFlow()

    init {
        loadAvailableMatches()
    }

    private fun loadAvailableMatches() {
        viewModelScope.launch {
            matchRepository.getMatches("").collect { matches ->
                _availableMatches.value = matches
                if (_uiState.value is PredictionUiState.Idle && matches.isNotEmpty()) {
                    // Mặc định chọn trận đầu tiên để phân tích
                    predictMatch(matches.first().id)
                }
            }
        }
    }

    fun predictMatch(matchId: Long) {
        viewModelScope.launch {
            _uiState.value = PredictionUiState.Loading
            try {
                val match = matchRepository.getMatchDetail(matchId).firstOrNull()
                    ?: throw IllegalArgumentException("Không tìm thấy trận đấu với ID $matchId")

                val homeTeam = teamRepository.getTeamDetail(match.homeTeam.id).firstOrNull()
                val awayTeam = teamRepository.getTeamDetail(match.awayTeam.id).firstOrNull()
                val homeRecent = matchRepository.getRecentMatchesForTeam(match.homeTeam.id, 5).firstOrNull() ?: emptyList()
                val awayRecent = matchRepository.getRecentMatchesForTeam(match.awayTeam.id, 5).firstOrNull() ?: emptyList()
                val h2h = matchRepository.getH2HMatches(match.homeTeam.id, match.awayTeam.id).firstOrNull() ?: emptyList()
                val oddsHistory = oddsRepository.getOddsHistory(matchId, null, null).firstOrNull() ?: emptyList()

                val context = MatchPredictionContext(
                    matchId = matchId,
                    homeTeamId = match.homeTeam.id,
                    awayTeamId = match.awayTeam.id,
                    homeElo = homeTeam?.eloRating ?: 1500.0,
                    awayElo = awayTeam?.eloRating ?: 1500.0,
                    homeRecentMatches = homeRecent,
                    awayRecentMatches = awayRecent,
                    h2hMatches = h2h,
                    latestOdds = oddsHistory.firstOrNull()
                )

                val result = predictMatchOutcomeUseCase(context)

                _uiState.value = PredictionUiState.Success(
                    match = match,
                    availableMatches = _availableMatches.value,
                    prediction = result,
                    homeElo = context.homeElo ?: 1500.0,
                    awayElo = context.awayElo ?: 1500.0,
                    homeRecentForm = homeRecent.map { if (it.isHomeWin) 'W' else if (it.isDraw) 'D' else 'L' },
                    awayRecentForm = awayRecent.map { if (it.isAwayWin) 'W' else if (it.isDraw) 'D' else 'L' }
                )
            } catch (e: Exception) {
                _uiState.value = PredictionUiState.Error(e.message ?: "Lỗi tính toán dự đoán")
            }
        }
    }
}
```

### 7.6. Kết nối `PredictionScreen`:
- Thay thế các Card tĩnh:
  - `MatchSelectionCard`: Hiển thị tên đội nhà vs đội khách từ `state.match`, cho phép bấm đổi trận.
  - `ProbabilityResultsCard`: Hiển thị 3 thanh xác suất `state.prediction.probabilities.homeWinProb`, `drawProb`, `awayWinProb` (tính theo phần trăm % chuẩn xác, bảo toàn tổng = 100%).
  - `ModelWeightsCard`: Hiển thị độ tin cậy `state.prediction.confidenceScore` và giải thích phân bổ trọng số mô hình.
  - Nút "Calculate": Gọi `viewModel.predictMatch(selectedMatchId)`.

---

## 8. Hilt Dependency Injection Strategy

### 8.1. Đặc điểm Cốt lõi của Tầng Domain:
- `:core:domain` là Pure Kotlin/JVM module, **không chứa Dagger/Hilt/`javax.inject` dependencies**.
- Do đó, các UseCase trong `:core:domain` **không thể** gắn annotation `@Inject constructor`.

### 8.2. Phương án Triển khai Hilt DI Chuẩn xác trong `:app`:
Tạo module Hilt chuyên biệt `DomainUseCaseModule.kt` trong `:app` để cung cấp **chính xác 9 Pure Computation UseCases**:

```kotlin
package dev.anhquocs.truelab.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.anhquocs.truelab.core.domain.match.usecase.SearchMatchesUseCase
import dev.anhquocs.truelab.core.domain.match.usecase.SortMatchesUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SearchTeamsUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.SortSeasonRankingUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.GetTeamStatisticsUseCase
import dev.anhquocs.truelab.core.domain.odds.usecase.AnalyzeOddsTrendUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateTeamFormUseCase
import dev.anhquocs.truelab.core.domain.team.usecase.CalculateEloRatingUseCase
import dev.anhquocs.truelab.core.domain.prediction.usecase.PredictMatchOutcomeUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainUseCaseModule {

    @Provides @Singleton fun provideSearchMatchesUseCase(): SearchMatchesUseCase = SearchMatchesUseCase()
    @Provides @Singleton fun provideSortMatchesUseCase(): SortMatchesUseCase = SortMatchesUseCase()
    @Provides @Singleton fun provideSearchTeamsUseCase(): SearchTeamsUseCase = SearchTeamsUseCase()
    @Provides @Singleton fun provideSortSeasonRankingUseCase(): SortSeasonRankingUseCase = SortSeasonRankingUseCase()
    @Provides @Singleton fun provideGetTeamStatisticsUseCase(): GetTeamStatisticsUseCase = GetTeamStatisticsUseCase()
    @Provides @Singleton fun provideAnalyzeOddsTrendUseCase(): AnalyzeOddsTrendUseCase = AnalyzeOddsTrendUseCase()
    @Provides @Singleton fun provideCalculateTeamFormUseCase(): CalculateTeamFormUseCase = CalculateTeamFormUseCase()
    @Provides @Singleton fun provideCalculateEloRatingUseCase(): CalculateEloRatingUseCase = CalculateEloRatingUseCase()
    @Provides @Singleton fun providePredictMatchOutcomeUseCase(): PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase()
}
```

**Lợi ích**:
- Compile-safe 100%, không xâm phạm tính thuần túy của `:core:domain`.
- Cho phép Hilt tự động inject các UseCase này vào bất kỳ ViewModel nào qua `@Inject constructor`.
- Dễ dàng thay thế mock/fake khi viết Unit Test.

---

## 9. Xử lý Trạng thái Dữ liệu Trống & Lỗi (Database Empty & Error Handling)

Trong trường hợp app mới cài đặt, SQLite Database rỗng hoặc `DataSyncEngine` chưa chạy:
1. **Không Crash Ứng dụng**: Repositories trả về `emptyList()` hoặc `null`.
2. **Hiển thị Empty View Thân thiện**:
   - `MatchesUiState.Empty`: Hiển thị thông báo "Chưa có dữ liệu trận đấu trong cơ sở dữ liệu."
   - `PredictionUiState.Idle`: Hiển thị "Vui lòng chọn trận đấu để tính toán dự đoán."
3. **Graceful Fallback**:
   - Nếu tỷ lệ kèo null $\implies$ `OddsSignal` nhận trọng số `0.0`.
   - Nếu chưa có trận gần nhất $\implies$ `FormSignal` mặc định `0.0` an toàn.

---

## 10. Quản lý Vòng đời & Recomposition (Lifecycle & Recomposition)

1. **Thu gom State An toàn**:
   - Mọi Composable Screen sử dụng `collectAsStateWithLifecycle()` từ thư viện `androidx.lifecycle:lifecycle-runtime-compose` (đã được cấu hình trong `app/build.gradle.kts`).
2. **Dừng thu gom khi Background**:
   - Các `StateFlow` trong ViewModel sử dụng cấu hình `SharingStarted.WhileSubscribed(5_000)` để tự động hủy coroutine thu thập dữ liệu sau 5 giây màn hình bị ẩn, ngăn rò rỉ tài nguyên.

---

## 11. Các Hạng mục Hoãn lại (Deferred / Future Scope)

Nhằm đảm bảo tính tập trung của Giai đoạn D4 và không gây phình to scope (over-scope), các hạng mục sau được phân kỳ rõ ràng:

### 11.1. ⏱️ Benchmark Screen (Chuyển sang Phase D5):
- **Lý do**: Benchmark Screen yêu cầu các runner đo đạc thời gian thực thi (execution time in ms), mức tiêu thụ bộ nhớ (RAM in MB) với tập dữ liệu quy mô $N = 1,000; 10,000; 50,000$, cũng như engine backtest dự đoán đối chứng với kết quả thực tế để sinh Confusion Matrix.
- **Dependencies cần xây dựng trong D5**:
  - `SearchBenchmarkRunner`: Đo Linear Search vs. Binary Search.
  - `SortBenchmarkRunner`: Đo QuickSort vs. MergeSort.
  - `PredictionBacktestEvaluator`: Kiểm thử độ chính xác trên tập trận đã kết thúc (`isEnded == true`), tính Precision, Recall, F1-score và Confusion Matrix.
- **Hiện trạng trong D4**: Giữ nguyên `BenchmarkScreen.kt` prototype để bảo toàn UI và luồng điều hướng, không xóa bỏ.

### 11.2. ⚽ Bộ lọc League / Season trên Matches Screen (Chuyển sang Phase D6):
- **Lý do**: Cả `MatchEntity` trong Room và Domain `Match` đều chưa có thuộc tính `leagueId` và `season`.
- **Dependencies**: Cần cập nhật Room Database Schema (Migration), mở rộng model `Match` trong Domain và cập nhật `DataSyncEngine`.

### 11.3. 🛡️ Home/Away Splits trên Teams Screen (Chuyển sang Phase D5):
- **Lý do**: Tầng Domain hiện chưa có UseCase nghiệp vụ `CalculateHomeAwaySplitsUseCase`. UI Mapper tuyệt đối không tự tính toán business logic.
- **Dependencies**: Cần định nghĩa `CalculateHomeAwaySplitsUseCase` và model `HomeAwaySplits` trong `:core:domain`.

### 11.4. 🛡️ Ma trận Đối đầu H2H toàn diện trên Teams Screen (Chuyển sang Phase D5+):
- **Lý do**: Màn hình Teams Screen hiện tại là danh sách phân tích từng đội. Tính năng tra cứu đối đầu giữa 2 đội bất kỳ đòi hỏi giao diện chọn cặp đội (Team Comparison Tool).
- **Hiện trạng trong D4**: DEFER HOÀN TOÀN tính năng này trên Teams Screen; không chắp vá hiển thị ad-hoc. Lịch sử H2H cho cặp trận cụ thể vẫn được triển khai đầy đủ trong Prediction Screen (D4.4).

### 11.5. 🏠 Thống kê Dataset Động trên Home Screen (Chuyển sang Phase D6):
- **Lý do**: Cần các truy vấn `COUNT(*)` từ `MatchDao`, `TeamDao`, `OddsDao`, `ProviderDao` và metadata phiên bản Room DB.
- **Hiện trạng trong D4**: Giữ nguyên thẻ tĩnh `DatasetOverviewCard`.

---

## 12. D4.5 — Kế hoạch Dọn dẹp Legacy & Xác thực Cuối (Legacy Cleanup & Verification)

### 12.1. Ma trận Di chuyển Legacy (Legacy Migration Matrix):

| Thành phần Legacy | Vị trí Hiện tại | Hiện trạng Sử dụng | Hành động trong D4 | Kế hoạch Xóa bỏ |
|---|---|---|---|---|
| `PredictionResult.Companion.computeWeightedScoring` | `core/domain/.../Prediction.kt` | 0 usages (không ai gọi) | Đánh dấu `@Deprecated("Use PredictMatchOutcomeUseCase instead")` | Giữ nguyên trong D4; xóa tại Phase D5 Cleanup. |
| `SeasonRanking.calculateFormScore()` | `core/domain/.../Team.kt` | 0 usages (không ai gọi) | Đánh dấu `@Deprecated("Use CalculateTeamFormUseCase instead")` | Giữ nguyên trong D4; xóa tại Phase D5 Cleanup. |
| `PredictMatchUseCase` | `core/domain/.../PredictionUseCases.kt` | Được bind trong `PredictionDataModule` | Đánh dấu `@Deprecated` | Xóa khi loại bỏ `PredictionDataModule.providePredictionUseCases`. |
| `PredictionRepository` | `core/domain/.../PredictionRepository.kt` | Được bind trong `PredictionDataModule` | Giữ nguyên | Giữ nguyên làm local cache reader nếu cần. |
| `MatchUseCases` cũ | `core/domain/.../MatchUseCases.kt` | Được bind trong `MatchDataModule` | Giữ nguyên | Có thể refactor nội bộ để ủy thác cho UseCase mới. |
| `TeamUseCases` cũ | `core/domain/.../TeamUseCases.kt` | Được bind trong `TeamDataModule` | Giữ nguyên | Giữ nguyên. |

---

## 13. Chiến lược Kiểm thử D4 (Testing Strategy)

1. **ViewModel Unit Tests (Trọng tâm)**:
   - Viết Unit Test cho từng ViewModel mới:
     - `MatchesViewModelTest`: Kiểm tra phát sinh trạng thái khi tìm kiếm, khi đổi tiêu chí sắp xếp, khi lọc trạng thái, khi mở chi tiết trận đấu, khi danh sách rỗng.
     - `TeamsViewModelTest`: Kiểm tra lọc đội bóng và sắp xếp BXH tie-breakers.
     - `AnalyticsViewModelTest`: Kiểm tra tính toán phân phối bàn thắng, SMA odds và so sánh đa nhà cái.
     - `PredictionViewModelTest`: Kiểm tra thu thập dữ liệu đa nguồn và tính toán xác suất 3 chiều.
   - Thêm `kotlinx-coroutines-test` vào `app/build.gradle.kts` (sử dụng `runTest` / `StandardTestDispatcher`).
2. **Bảo tồn Tuyệt đối Test Suite Hiện hữu (Zero Regressions)**:
   - `:core:algorithm:test`: Duy trì **122 / 122 PASS (100%)**.
   - `:core:domain:test`: Duy trì **156 / 156 PASS (100%)** (+ các tests mới nếu có mở rộng Repository).

---

## 14. Danh mục File Triển khai Chi tiết (File-Level Implementation Plan)

### 🔹 Files Tạo Mới trong `:app`:
1. `app/src/main/kotlin/dev/anhquocs/truelab/di/DomainUseCaseModule.kt` (Hilt Module cung cấp 9 Pure Computation UseCases)
2. `app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/viewmodel/MatchesViewModel.kt`
3. `app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/model/MatchesUiState.kt`
4. `app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/mapper/MatchUiMapper.kt`
5. `app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/components/MatchDetailBottomSheet.kt`
6. `app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/viewmodel/TeamsViewModel.kt`
7. `app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/model/TeamsUiState.kt`
8. `app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/mapper/TeamUiMapper.kt`
9. `app/src/main/kotlin/dev/anhquocs/truelab/feature/analytics/presentation/viewmodel/AnalyticsViewModel.kt`
10. `app/src/main/kotlin/dev/anhquocs/truelab/feature/analytics/presentation/model/AnalyticsUiState.kt`
11. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt`
12. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/model/PredictionUiState.kt`

### 🔹 Test Files Tạo Mới trong `app/src/test`:
13. `app/src/test/java/dev/anhquocs/truelab/feature/match/presentation/viewmodel/MatchesViewModelTest.kt`
14. `app/src/test/java/dev/anhquocs/truelab/feature/team/presentation/viewmodel/TeamsViewModelTest.kt`
15. `app/src/test/java/dev/anhquocs/truelab/feature/analytics/presentation/viewmodel/AnalyticsViewModelTest.kt`
16. `app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModelTest.kt`

### 🔹 Files Chỉnh Sửa để Kết nối:
17. `app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/MatchesScreen.kt` (kết nối `MatchesViewModel` + `MatchDetailBottomSheet`)
18. `app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/TeamsScreen.kt` (kết nối `TeamsViewModel`)
19. `app/src/main/kotlin/dev/anhquocs/truelab/feature/analytics/presentation/AnalyticsScreen.kt` (kết nối `AnalyticsViewModel`)
20. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/PredictionScreen.kt` (kết nối `PredictionViewModel`)
21. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/repository/MatchRepository.kt` (bổ sung recent/h2h contracts)
22. `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/repository/MatchRepositoryImpl.kt` (implement recent/h2h)
23. `gradle/libs.versions.toml` & `app/build.gradle.kts` (bổ sung `kotlinx-coroutines-test` cho test)

### ⛔ Files Tuyệt đối KHÔNG SỬA:
- Toàn bộ module `:core:algorithm` (FROZEN 122/122).
- Toàn bộ thuật toán và transformer trong `:core:domain/prediction`.
- Navigation routes trong `MainNavDestination.kt` và `MainNavBar.kt`.
- `HomeScreen.kt` và `BenchmarkScreen.kt` (bảo toàn cấu trúc hiện hữu).

---

## 15. Thứ tự Phân kỳ Triển khai Từng bước (Granular Execution Order)

```text
D4.1: Matches Integration
  ├── D4.1.A: Matches List (DomainUseCaseModule -> MatchUiMapper -> MatchesViewModel -> MatchesScreen -> ViewModelTest)
  └── D4.1.B: Match Detail (MatchDetailBottomSheet -> Connect click -> CTA Predict)
       │
       ▼
D4.2: Teams & Standings Integration
  (TeamUiMapper -> TeamsViewModel -> TeamsScreen -> ViewModelTest)
       │
       ▼
D4.3: Analytics Integration
  (AnalyticsViewModel -> AnalyticsScreen -> DescriptiveStatsCard / MultiProviderOddsCard / OddsTrendCard -> ViewModelTest)
       │
       ▼
D4.4: Prediction Integration
  (Expose MatchRepository recent/h2h -> MatchRepositoryImpl -> PredictionViewModel -> PredictionScreen -> ViewModelTest)
       │
       ▼
D4.5: Legacy Deprecation & Final Verification
  (@Deprecated legacy methods -> Full Regression: Algorithm 122 + Domain 156 + ViewModels -> D4 Final Report)
```

Mỗi phân kỳ (D4.1 $\to$ D4.5) sẽ được thực hiện độc lập, kiểm thử chặt chẽ, tự kiểm tra mã nguồn (self-review) và tạo commit atomic riêng biệt.

---

## 16. Bảng Tiêu chuẩn Hoàn thành (Definition of Done for D4)

- [ ] **1. Hoàn thành 4 ViewModels**: `MatchesViewModel`, `TeamsViewModel`, `AnalyticsViewModel`, và `PredictionViewModel` được triển khai chuẩn mực với Hilt `@HiltViewModel`.
- [ ] **2. Kết nối Toàn bộ 9 Pure Computation UseCases**: Tất cả 9 Domain UseCases từ D1, D2, D3 đều có consumer thực tế ở tầng Presentation.
- [ ] **3. Xóa Bỏ Dữ liệu Mock Tĩnh**: Thay thế toàn bộ `sampleRecords`, `sampleTeams`, và các chuỗi hardcode tại 4 màn hình chính bằng dữ liệu động từ StateFlow.
- [ ] **4. Tích hợp Match Detail (D4.1.B)**: Hoàn thành `MatchDetailBottomSheet` khi nhấn vào trận đấu trên `MatchesScreen` (truy xuất qua `matchRepository.getMatchDetail`), hỗ trợ nút bấm điều hướng sang `PredictionScreen`.
- [ ] **5. Xử lý Toàn diện UI State**: Mọi màn hình đều hỗ trợ đầy đủ 4 trạng thái: Loading, Success, Empty, và Error.
- [ ] **6. Mở rộng MatchRepository An toàn**: Expose `getRecentMatchesForTeam` và `getH2HMatches` phục vụ Prediction mà không phá vỡ Clean Architecture.
- [ ] **7. Rõ ràng Trạng thái Toàn bộ 6 Màn hình README**:
  - Home: Bảo toàn static dashboard + điều hướng.
  - Matches: Dynamic data + Search + Sort + Status Filter + Match Detail.
  - Teams: Dynamic data + Search + Standings Sort + Form Score + Elo (Home/Away & H2H Matrix xác nhận DEFERRED sang D5).
  - Analytics: Dynamic data + Descriptive Stats + Multi-provider Odds Matrix + Odds Trend.
  - Prediction: Dynamic flow chọn trận + trích xuất đặc trưng (bao gồm H2H giữa 2 đội của trận đấu) + chạy dự đoán 3 chiều.
  - Benchmark: Xác nhận DEFERRED sang Phase D5, bảo tồn prototype UI.
- [ ] **8. 100% Tests Vượt qua**:
  - Module `:core:algorithm:test`: **122 / 122 PASS**.
  - Module `:core:domain:test`: **$\ge 156$ PASS**.
  - Module `:app:test`: Toàn bộ các Unit Test mới của 4 ViewModels đều PASS.
- [ ] **9. Bảo tồn Legacy An toàn**: Đánh dấu `@Deprecated` các hàm prototype cũ, không gây breaking changes.
- [ ] **10. Báo cáo Tổng kết Hoàn chỉnh**: Hoàn thành tài liệu `docs/reports/domain-d4-presentation.md`.
