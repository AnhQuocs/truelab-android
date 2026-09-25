# Presentation P2 — Presentation Expansion & Tools Master Plan

> **Tài liệu Kế hoạch Triển khai Tổng thể (Master Single Source of Truth cho Presentation Phase P2)**  
> **Phiên bản:** 1.0 (Chuẩn hóa toàn diện 3 Sub-phases: P2.1 Benchmark Runner UI, P2.2 Team Home/Away Splits UI, P2.3 Dynamic Dataset Overview & League/Season Filters)  
> **Trạng thái:** **FINAL — READY FOR IMPLEMENTATION**

---

## 1. Tổng quan & Đối chiếu Mục tiêu (Traceability Matrix)

Đối chiếu chi tiết các cam kết trong [docs/roadmap.md](../../docs/roadmap.md) (Mục 3 & Mục 5) và `README.md` (Mục 7 — Application Screens) với nền tảng Domain/Data hiện tại của TrueLab:

| Tính năng / Màn hình | Sub-phase | Domain / Data Dependencies | Trạng thái Hiện tại | Mục tiêu Triển khai Presentation P2 |
| :--- | :---: | :--- | :--- | :--- |
| **1. ⏱️ Benchmark Screen** | **P2.1** | [RunAlgorithmBenchmarkUseCase](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/usecase/RunAlgorithmBenchmarkUseCase.kt)<br/>[BenchmarkModels](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/model/BenchmarkModels.kt)<br/>[SyntheticDatasetGenerator](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/generator/SyntheticDatasetGenerator.kt) | Prototype UI tĩnh (`BenchmarkScreen.kt`), dữ liệu hardcode ("1.420 ms"), nút Run có `onClick = {}`, chưa có ViewModel | Xây dựng `BenchmarkViewModel` + `BenchmarkUiState`, thực thi usecase trên `Dispatchers.Default`, chọn size 1K/10K/50K, hiển thị Linear vs Binary Search, Quick vs Merge Sort, speedup factor, faster algorithm và thẻ $O(\cdot)$ |
| **2. 🛡️ Team Home/Away Splits** | **P2.2** | [CalculateHomeAwaySplitsUseCase](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateHomeAwaySplitsUseCase.kt)<br/>[TeamHomeAwaySplits](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/TeamHomeAwaySplits.kt)<br/>[TeamRepository](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/repository/TeamRepository.kt)<br/>[MatchRepository](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/repository/MatchRepository.kt) | [TeamAnalyticsCard.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/components/TeamAnalyticsCard.kt) hiển thị "—" cho Home/Away do [TeamUiMapper.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/mapper/TeamUiMapper.kt) gán `null` | Tích hợp usecase vào `TeamsViewModel`, ánh xạ số trận thắng/hòa/thua (W-D-L), hiệu số bàn thắng và tỷ lệ thắng sân nhà/sân khách động lên `TeamAnalyticsCard` |
| **3. 🏠 Dynamic Dataset Overview** | **P2.3** | [DatasetMetadataRepository](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/metadata/repository/DatasetMetadataRepository.kt)<br/>[DatasetMetadata](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/metadata/model/DatasetMetadata.kt) | [DatasetOverviewCard.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/home/presentation/components/DatasetOverviewCard.kt) dùng số liệu tĩnh ("75,000", "120", "4", "11"), chưa có `HomeViewModel` | Xây dựng `HomeViewModel` thu thập `Flow<DatasetMetadata?>`, cập nhật live tổng số trận, số đội, nhà cái, thuật toán và format `lastSyncTimestamp` chuẩn Triple-locale |
| **4. ⚽ League / Season Filters** | **P2.3** | [LeagueRepository](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/league/repository/LeagueRepository.kt)<br/>[MatchRepository.getMatchesByLeagueAndSeason](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/repository/MatchRepository.kt) | [MatchesScreen.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/MatchesScreen.kt) chỉ có bộ lọc trạng thái (`ALL`, `ENDED`, `SCHEDULED`), chưa lọc theo League | Kết nối `LeagueRepository` vào `MatchesViewModel`, thêm hàng Filter Chips chọn League ("All", "Premier League", v.v.) lọc danh sách trận đấu mượt mà |

---

## 2. Bối cảnh & Hiện trạng Hệ thống (Baseline Architecture)

### 2.1. Mã nguồn Hiện tại:
- **Baseline Test Suite**: **469/469 tests PASS** (100% pass rate).
  - `:core:algorithm`: 122 tests
  - `:core:domain`: 206 tests (Bao gồm Domain D4: Benchmark, HomeAwaySplits, Backtest, Evaluation)
  - `:core:data`: 95 tests (Bao gồm Data D1 Metadata, Data D2 Retry Engine, Cache Freshness, WorkManager Sync)
  - `:app`: 46 tests (ViewModel tests của P1)
- **Build**: `./gradlew assembleDebug` SUCCESS.
- **Git Status**: Clean working tree trên nhánh `main`, đồng bộ `origin/main`.

### 2.2. Khoảng trống Tầng Presentation (Gaps):
1. **Thiếu ViewModel & State cho Benchmark**: `BenchmarkScreen.kt` chưa gắn kết với Hilt ViewModel, chưa gọi `RunAlgorithmBenchmarkUseCase`.
2. **Thiếu Consumer cho `CalculateHomeAwaySplitsUseCase`**: Usecase đã được kiểm thử 100% ở Domain D4 nhưng `TeamsViewModel` chưa gọi, mapper để trống các trường Home/Away.
3. **Thiếu Consumer cho `DatasetMetadataRepository`**: Repository metadata (Data D1.4) đã cung cấp `Flow<DatasetMetadata?>` nhưng `HomeScreen` chưa thu thập dữ liệu động.
4. **Thiếu Consumer cho `LeagueRepository` trên UI**: `LeagueRepository` đã hoàn thiện ở Data D1 nhưng `MatchesViewModel` chưa có state để chọn lọc theo League.
5. **Thiếu Hilt DI Bindings cho Domain D4**: [DomainUseCaseModule.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/di/DomainUseCaseModule.kt) hiện mới chỉ bind 9 usecase của D1–D3; cần bổ sung bindings cho `RunAlgorithmBenchmarkUseCase` và `CalculateHomeAwaySplitsUseCase`.

---

## 3. Kiến trúc Tổng thể & Luồng Dữ liệu (Target Architecture)

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (:app)                       │
│                                                                        │
│  ┌──────────────────────┐  ┌────────────────────┐  ┌────────────────┐ │
│  │   BenchmarkScreen    │  │    TeamsScreen     │  │  HomeScreen &  │ │
│  │  (BenchmarkViewModel)│  │  (TeamsViewModel)  │  │ MatchesScreen  │ │
│  └──────────┬───────────┘  └─────────┬──────────┘  └───────┬────────┘ │
└─────────────┼────────────────────────┼─────────────────────┼──────────┘
              │                        │                     │
              │ Dispatchers.Default    │ viewModelScope      │ Flow StateIn
              ▼                        ▼                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       DOMAIN LAYER (:core:domain)                      │
│                                                                        │
│  ┌────────────────────────┐  ┌──────────────────────────────────────┐ │
│  │RunAlgorithmBenchmarkUC │  │     CalculateHomeAwaySplitsUseCase   │ │
│  └──────────┬─────────────┘  └──────────────────┬───────────────────┘ │
│             │                                   │                     │
│  ┌──────────▼─────────────┐  ┌──────────────────▼───────────────────┐ │
│  │ Search & Sort Runners  │  │   MatchRepository & LeagueRepository │ │
│  │ (Synthetic Generator)  │  │      & DatasetMetadataRepository     │ │
│  └────────────────────────┘  └──────────────────┬───────────────────┘ │
└─────────────────────────────────────────────────┼─────────────────────┘
                                                  │
                                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         DATA LAYER (:core:data)                        │
│                                                                        │
│   Room Database (MatchDao, LeagueDao, MetadataDao)                     │
│   ◄── Background DataSyncWorker (WorkManager Scheduled Sync)           │
└────────────────────────────────────────────────────────────────────────┘
```

### Nguyên tắc Bất biến:
1. **Unidirectional Data Flow (UDF)**: Toàn bộ màn hình tuân thủ mô hình StateFlow $\rightarrow$ UI State $\rightarrow$ Events.
2. **Pure Domain Boundary**: Không đưa Android SDK, Room hay Dagger annotations vào `:core:domain`. Toàn bộ providers nằm ở [DomainUseCaseModule.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/di/DomainUseCaseModule.kt) trong `:app`.
3. **Off-Main-Thread Execution**: Các tác vụ tính toán chuyên sâu (CPU-bound) như chạy benchmark $N = 50\text{K}$ bắt buộc thực thi trên `Dispatchers.Default` (`withContext(defaultDispatcher)`).
4. **Domain Speedup & UI Metadata**: Trường `speedupFactor` và `fasterAlgorithm` lấy trực tiếp từ Domain model [ComparisonBenchmarkResult.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/model/BenchmarkModels.kt). Các nhãn độ phức tạp lý thuyết $O(N)$, $O(\log N)$, $O(N \log N)$ là metadata biểu diễn của tầng Presentation.
5. **Giới hạn 400 dòng/file & Triple-Locale**: Tách các sub-component vào package `components/`. Toàn bộ chuỗi giao diện phải được định nghĩa đầy đủ ở `values`, `values-en`, `values-vi`.

---

## 4. Kế hoạch Chi tiết Từng Sub-phase

---

### Sub-phase P2.1 — Benchmark Runner & Visual Performance UI

#### 1. Trách nhiệm Nghiệp vụ:
- Xây dựng giao diện đo lường hiệu năng thực tế cho 4 thuật toán cốt lõi:
  - **Tìm kiếm**: Linear Search ($O(N)$) vs Binary Search ($O(\log N)$).
  - **Sắp xếp**: Quick Sort ($O(N \log N)$) vs Merge Sort ($O(N \log N)$).
- Cho phép người dùng chọn kích thước tập dữ liệu giả lập chuẩn hóa ($N = 1\text{K}, 10\text{K}, 50\text{K}$) và bấm chạy đo thời gian thực tế nano-giây qua [RunAlgorithmBenchmarkUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/usecase/RunAlgorithmBenchmarkUseCase.kt).

#### 2. Thiết kế `BenchmarkUiState`:
```kotlin
package dev.anhquocs.truelab.feature.benchmark.presentation.model

import dev.anhquocs.truelab.core.domain.benchmark.model.ComparisonBenchmarkResult
import dev.anhquocs.truelab.core.ui.utils.UiText

sealed interface BenchmarkUiState {
    val selectedDatasetSize: Int

    data class Idle(
        override val selectedDatasetSize: Int = DEFAULT_DATASET_SIZE
    ) : BenchmarkUiState

    data class Running(
        override val selectedDatasetSize: Int
    ) : BenchmarkUiState

    data class Success(
        override val selectedDatasetSize: Int,
        val searchResult: ComparisonBenchmarkResult,
        val sortResult: ComparisonBenchmarkResult,
        val timestamp: Long
    ) : BenchmarkUiState

    data class Error(
        override val selectedDatasetSize: Int,
        val message: UiText
    ) : BenchmarkUiState

    companion object {
        const val DEFAULT_DATASET_SIZE = 10_000
        val AVAILABLE_SIZES = listOf(1_000, 10_000, 50_000)
    }
}
```

#### 3. Thiết kế `BenchmarkViewModel`:
```kotlin
package dev.anhquocs.truelab.feature.benchmark.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.benchmark.usecase.RunAlgorithmBenchmarkUseCase
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.benchmark.presentation.model.BenchmarkUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val runAlgorithmBenchmarkUseCase: RunAlgorithmBenchmarkUseCase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    fun onSelectDatasetSize(size: Int) {
        val currentState = _uiState.value
        if (currentState is BenchmarkUiState.Running) return
        _uiState.value = BenchmarkUiState.Idle(selectedDatasetSize = size)
    }

    fun runBenchmark() {
        val currentSize = _uiState.value.selectedDatasetSize
        _uiState.value = BenchmarkUiState.Running(selectedDatasetSize = currentSize)

        viewModelScope.launch {
            try {
                val suiteResult = withContext(defaultDispatcher) {
                    runAlgorithmBenchmarkUseCase(datasetSizes = listOf(currentSize))
                }
                val searchComparison = suiteResult.searchBenchmarks.firstOrNull()
                val sortComparison = suiteResult.sortBenchmarks.firstOrNull()

                if (searchComparison != null && sortComparison != null) {
                    _uiState.value = BenchmarkUiState.Success(
                        selectedDatasetSize = currentSize,
                        searchResult = searchComparison,
                        sortResult = sortComparison,
                        timestamp = suiteResult.timestamp
                    )
                } else {
                    _uiState.value = BenchmarkUiState.Error(
                        selectedDatasetSize = currentSize,
                        message = UiText.StringResource(R.string.benchmark_error_default)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = BenchmarkUiState.Error(
                    selectedDatasetSize = currentSize,
                    message = if (e.message.isNullOrBlank()) {
                        UiText.StringResource(R.string.benchmark_error_default)
                    } else {
                        UiText.DynamicString(e.message ?: "")
                    }
                )
            }
        }
    }
}
```

#### 4. UI Components Tách biệt:
- `DatasetSizeSelector.kt`: Hàng Filter Chips ($1\text{K}, 10\text{K}, 50\text{K}$).
- `AlgorithmBenchmarkCard.kt`: Thẻ so sánh kết quả 2 thuật toán, badge độ phức tạp $O(\cdot)$, badge speedup xanh lá (`#10B981`) hiển thị `$fasterAlgorithm is ${speedupFactor}x Faster`.

#### 5. Acceptance Criteria P2.1:
- [x] Nút "Run Benchmark Suite" kích hoạt usecase thật trên `Dispatchers.Default`.
- [x] Không còn giá trị hardcoded trong `BenchmarkScreen.kt`.
- [x] Xử lý đầy đủ 4 trạng thái giao diện: `Idle`, `Running`, `Success`, `Error`.
- [x] `BenchmarkViewModelTest` PASS 100%.

---

### Sub-phase P2.2 — Team Home/Away Splits & Advanced Statistics UI

#### 1. Trách nhiệm Nghiệp vụ:
- Tích hợp kết quả phân tách hiệu năng sân nhà / sân khách từ [CalculateHomeAwaySplitsUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateHomeAwaySplitsUseCase.kt) vào màn hình Đội bóng ([TeamsScreen.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/TeamsScreen.kt)).
- Hiển thị tỷ lệ thắng sân nhà/khách (`homeWinRate`, `awayWinRate`) và thành tích chi tiết (`homeRecord: "4W-1D-0L"`, `awayRecord: "2W-2D-1L"`).

#### 2. Mở rộng `TeamsViewModel`:
```kotlin
// Inject CalculateHomeAwaySplitsUseCase
@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val teamRepository: TeamRepository,
    private val matchRepository: MatchRepository,
    private val searchTeamsUseCase: SearchTeamsUseCase,
    private val sortSeasonRankingUseCase: SortSeasonRankingUseCase,
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase,
    private val calculateHomeAwaySplitsUseCase: CalculateHomeAwaySplitsUseCase
) : ViewModel() {
    // Trong luồng combine(), gọi:
    // val formScore = calculateTeamFormUseCase(team.id, allMatches, windowSize = 5)
    // val splits = calculateHomeAwaySplitsUseCase(team.id, allMatches)
    // TeamUiMapper.toRecord(team, ranking, formScore, splits)
}
```

#### 3. Cập nhật `TeamUiMapper`:
```kotlin
fun toRecord(
    team: TeamDetail,
    ranking: SeasonRanking?,
    formScore: FormScore? = null,
    splits: TeamHomeAwaySplits? = null
): TeamAnalyticsRecord {
    val homeWinRate = splits?.let { (it.homeSplit.winRate * 100).roundToInt() }
    val homeRecord = splits?.let { "${it.homeSplit.won}W-${it.homeSplit.draw}D-${it.homeSplit.loss}L" } ?: "—"
    val awayWinRate = splits?.let { (it.awaySplit.winRate * 100).roundToInt() }
    val awayRecord = splits?.let { "${it.awaySplit.won}W-${it.awaySplit.draw}D-${it.awaySplit.loss}L" } ?: "—"

    return TeamAnalyticsRecord(
        id = team.id.toString(),
        name = team.name,
        logo = team.logo,
        league = team.leagueName ?: "Premier League • England",
        eloRating = team.eloRating.roundToInt(),
        rank = ranking?.position ?: 0,
        played = ranking?.totalMatches ?: 0,
        wins = ranking?.won ?: 0,
        draws = ranking?.draw ?: 0,
        losses = ranking?.loss ?: 0,
        form = formBadges,
        formScore = calculatedScore,
        homeWinRate = homeWinRate,
        homeRecord = homeRecord,
        awayWinRate = awayWinRate,
        awayRecord = awayRecord,
        h2hHighlight = "—"
    )
}
```

#### 4. Acceptance Criteria P2.2:
- [x] [TeamAnalyticsCard.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/components/TeamAnalyticsCard.kt) hiển thị đúng số liệu Home/Away W-D-L và % thắng thực tế.
- [x] Hiệu năng tính toán mượt mà khi cuộn danh sách lớn.
- [x] `TeamsViewModelTest` kiểm thử đầy đủ ánh xạ Splits data.

---

### Sub-phase P2.3 — Dynamic Dataset Overview & League/Season Filter

#### 1. Trách nhiệm Nghiệp vụ:
- **HomeScreen**: Thay thế các số liệu tĩnh ("75,000", "120") trên [DatasetOverviewCard.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/home/presentation/components/DatasetOverviewCard.kt) bằng dữ liệu động từ [DatasetMetadataRepository.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/metadata/repository/DatasetMetadataRepository.kt).
- **MatchesScreen**: Mở rộng [MatchesViewModel.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/match/presentation/viewmodel/MatchesViewModel.kt) tích hợp [LeagueRepository.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/league/repository/LeagueRepository.kt) để cung cấp bộ lọc giải đấu (League Filter Chips).

#### 2. Thiết kế `HomeUiState` & `HomeViewModel`:
```kotlin
package dev.anhquocs.truelab.feature.home.presentation.model

import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.ui.utils.UiText

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val metadata: DatasetMetadata,
        val formattedLastSync: String
    ) : HomeUiState
    data class Error(val message: UiText) : HomeUiState
}
```

```kotlin
package dev.anhquocs.truelab.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anhquocs.truelab.R
import dev.anhquocs.truelab.core.domain.metadata.model.DatasetMetadata
import dev.anhquocs.truelab.core.domain.metadata.repository.DatasetMetadataRepository
import dev.anhquocs.truelab.core.ui.utils.UiText
import dev.anhquocs.truelab.feature.home.presentation.model.HomeUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val datasetMetadataRepository: DatasetMetadataRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = datasetMetadataRepository.getMetadata()
        .map { metadata ->
            if (metadata != null) {
                val formattedDate = formatTimestamp(metadata.lastSyncTimestamp)
                HomeUiState.Success(metadata = metadata, formattedLastSync = formattedDate)
            } else {
                HomeUiState.Success(
                    metadata = DatasetMetadata(
                        totalMatches = 0,
                        totalTeams = 0,
                        totalProviders = 0,
                        totalAlgorithms = 11,
                        schemaVersion = 1,
                        lastSyncTimestamp = System.currentTimeMillis()
                    ),
                    formattedLastSync = formatTimestamp(System.currentTimeMillis())
                )
            }
        }.catch { error ->
            emit(HomeUiState.Error(UiText.DynamicString(error.message ?: "Failed to load metadata")))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
```

#### 3. Mở rộng `MatchesViewModel` với League Filter:
- Bổ sung `private val _selectedLeagueId = MutableStateFlow<Int?>(null)`.
- Lắng nghe `leagueRepository.getLeagues()` và thêm vào luồng `combine()`.
- Lọc danh sách trận đấu theo `leagueId` khi `_selectedLeagueId.value != null`.

#### 4. Acceptance Criteria P2.3:
- [x] `HomeScreen` hiển thị chính xác tổng số trận, đội, nhà cái, thuật toán và thời gian sync từ DB.
- [x] `MatchesScreen` có Filter Chips giải đấu, lọc danh sách trận tức thì.
- [x] `HomeViewModelTest` và `MatchesViewModelTest` PASS 100%.

---

## 5. Danh mục File Dự kiến Thay đổi (Master Files Table)

| Đường dẫn File | Sub-phase | Hành động | Trách nhiệm Nghiệp vụ & Rationale |
| :--- | :---: | :---: | :--- |
| `app/.../feature/benchmark/presentation/model/BenchmarkUiState.kt` | **P2.1** | **Create** | Sealed interface định nghĩa 4 trạng thái: `Idle`, `Running`, `Success`, `Error`. |
| `app/.../feature/benchmark/presentation/viewmodel/BenchmarkViewModel.kt` | **P2.1** | **Create** | `@HiltViewModel` điều phối đo benchmark CPU-bound trên `Dispatchers.Default`. |
| `app/.../feature/benchmark/presentation/components/AlgorithmBenchmarkCard.kt` | **P2.1** | **Create** | Reusable card hiển thị kết quả so sánh, $O(\cdot)$ và Speedup Badge. |
| `app/.../feature/benchmark/presentation/components/DatasetSizeSelector.kt` | **P2.1** | **Create** | Component chọn kích thước dataset ($1\text{K}, 10\text{K}, 50\text{K}$) bằng FilterChips. |
| `app/.../feature/benchmark/presentation/BenchmarkScreen.kt` | **P2.1** | **Modify** | Kết nối `BenchmarkViewModel`, render UI theo UDF. |
| `app/.../di/DomainUseCaseModule.kt` | **P2.1, P2.2** | **Modify** | Cung cấp provider Hilt cho `RunAlgorithmBenchmarkUseCase` và `CalculateHomeAwaySplitsUseCase`. |
| `app/.../feature/benchmark/presentation/viewmodel/BenchmarkViewModelTest.kt` | **P2.1** | **Test** | Unit test toàn diện cho `BenchmarkViewModel`. |
| `docs/reports/presentation-p2.1-benchmark-ui.md` | **P2.1** | **Doc** | Báo cáo kiểm thử & hoàn thành sub-phase P2.1. |
| `app/.../feature/team/presentation/viewmodel/TeamsViewModel.kt` | **P2.2** | **Modify** | Tích hợp `CalculateHomeAwaySplitsUseCase` vào luồng `combine()`. |
| `app/.../feature/team/presentation/mapper/TeamUiMapper.kt` | **P2.2** | **Modify** | Ánh xạ Home/Away Splits sang `TeamAnalyticsRecord`. |
| `app/.../feature/team/presentation/components/TeamAnalyticsCard.kt` | **P2.2** | **Modify** | Hiển thị số liệu Splits động thay vì placeholder "—". |
| `app/.../feature/team/presentation/viewmodel/TeamsViewModelTest.kt` | **P2.2** | **Test** | Bổ sung test cases cho Home/Away Splits. |
| `docs/reports/presentation-p2.2-splits-ui.md` | **P2.2** | **Doc** | Báo cáo hoàn thành sub-phase P2.2. |
| `app/.../feature/home/presentation/model/HomeUiState.kt` | **P2.3** | **Create** | Sealed interface trạng thái cho `HomeScreen`. |
| `app/.../feature/home/presentation/viewmodel/HomeViewModel.kt` | **P2.3** | **Create** | ViewModel thu thập `DatasetMetadataRepository` Flow. |
| `app/.../feature/home/presentation/HomeScreen.kt` | **P2.3** | **Modify** | Kết nối `HomeViewModel`. |
| `app/.../feature/home/presentation/components/DatasetOverviewCard.kt` | **P2.3** | **Modify** | Hiển thị live metrics từ `HomeUiState`. |
| `app/.../feature/match/presentation/viewmodel/MatchesViewModel.kt` | **P2.3** | **Modify** | Tích hợp `LeagueRepository` và bộ lọc `selectedLeagueId`. |
| `app/.../feature/match/presentation/MatchesScreen.kt` | **P2.3** | **Modify** | Bổ sung hàng League Filter Chips. |
| `app/.../feature/home/presentation/viewmodel/HomeViewModelTest.kt` | **P2.3** | **Test** | Unit test cho `HomeViewModel`. |
| `app/.../feature/match/presentation/viewmodel/MatchesViewModelTest.kt` | **P2.3** | **Test** | Bổ sung test cases lọc League/Season. |
| `docs/reports/presentation-p2.3-overview-filters.md` | **P2.3** | **Doc** | Báo cáo hoàn thành sub-phase P2.3. |
| `docs/reports/presentation-p2-final.md` | **P2 Final** | **Doc** | Báo cáo tổng kết toàn bộ Presentation Phase P2. |
| `app/src/main/res/values/strings.xml` (và `values-en`, `values-vi`) | **P2.1–P2.3** | **Modify** | Bổ sung string resources đồng bộ Triple-Locale. |

---

## 6. Chiến lược Tài nguyên Chuỗi (Triple-Locale Resources)

Bổ sung đồng bộ trên cả 3 file tài nguyên (`res/values/strings.xml`, `res/values-en/strings.xml`, `res/values-vi/strings.xml`):

| Resource Name | Tiếng Việt (`values-vi` & `values`) | Tiếng Anh (`values-en`) | Dùng cho |
| :--- | :--- | :--- | :---: |
| `benchmark_speedup_format` | `%1$s nhanh hơn %2$.1fx` | `%1$s is %2$.1fx faster` | P2.1 |
| `benchmark_equal_speed` | `Tốc độ thực thi tương đương` | `Execution speeds are equal` | P2.1 |
| `benchmark_running_status` | `Đang đo lường hiệu năng (%1$d mục)...` | `Running benchmark (%1$d items)...` | P2.1 |
| `benchmark_idle_hint` | `Nhấn nút bên dưới để bắt đầu đo lường hiệu năng thuật toán.` | `Press the button below to start benchmarking algorithm performance.` | P2.1 |
| `benchmark_error_default` | `Đã xảy ra lỗi trong quá trình đo lường hiệu năng.` | `An error occurred while running the benchmark.` | P2.1 |
| `benchmark_last_run` | `Lần đo gần nhất: %1$s` | `Last benchmark run: %1$s` | P2.1 |
| `matches_filter_all_leagues` | `Tất cả giải đấu` | `All Leagues` | P2.3 |

---

## 7. Chiến lược Kiểm thử & Xác thực (Testing Strategy)

### 7.1. Nguyên tắc Bất biến:
1. **Bảo toàn 100% Baseline Tests**: Toàn bộ **469/469 tests hiện tại bắt buộc phải PASS** liên tục sau mỗi thay đổi mã nguồn.
2. **Không Hardcode Số lượng Test Tương lai**: Báo cáo tổng số test thực tế đạt được sau mỗi sub-phase trong báo cáo hoàn thành tương ứng.
3. **Coroutine Testing Chuẩn mực**: Inject `TestDispatcher` vào ViewModel constructor (`defaultDispatcher: CoroutineDispatcher = Dispatchers.Default`) để kiểm thử StateFlow đồng bộ mà không gặp race conditions.

### 7.2. Ma trận Test Cases Chi tiết:
- **`BenchmarkViewModelTest`**:
  - `initialStateIsIdleWithDefaultSize`
  - `onSelectDatasetSizeUpdatesState`
  - `runBenchmarkTransitionsFromIdleToRunningToSuccess`
  - `runBenchmarkMapsComparisonResultCorrectly`
  - `runBenchmarkTransitionsToErrorOnException`
  - `runBenchmarkPreservesDatasetSizeOnRerun`
- **`TeamsViewModelTest`**:
  - `homeAwaySplitsMappedCorrectlyToRecord`
  - `emptyMatchesYieldsZeroSplitsRecord`
- **`HomeViewModelTest`**:
  - `metadataFlowMappedToSuccessState`
  - `nullMetadataYieldsDefaultFallbackSuccessState`
- **`MatchesViewModelTest`**:
  - `leagueFilterAppliesCorrectlyWithStatusAndSearch`

---

## 8. Quy trình Triển khai Tuần tự (Implementation Order & Review Lifecycle)

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Triển khai Sub-phase P2.1 (Benchmark Runner UI)                    │
│    • DomainUseCaseModule DI Binding                                    │
│    • BenchmarkUiState & BenchmarkViewModel                             │
│    • BenchmarkViewModelTest                                            │
│    • AlgorithmBenchmarkCard, DatasetSizeSelector & BenchmarkScreen     │
│    • Triple-Locale Strings                                             │
│    • ./gradlew test & assembleDebug ──► Report & Review ──► Commit     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. Triển khai Sub-phase P2.2 (Team Home/Away Splits UI)                │
│    • DomainUseCaseModule DI Binding                                    │
│    • TeamsViewModel Integration & TeamUiMapper                         │
│    • TeamsViewModelTest (Splits verification)                          │
│    • TeamAnalyticsCard dynamic rendering                               │
│    • ./gradlew test & assembleDebug ──► Report & Review ──► Commit     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. Triển khai Sub-phase P2.3 (Dataset Overview & League Filters)       │
│    • HomeUiState & HomeViewModel                                       │
│    • HomeViewModelTest & DatasetOverviewCard dynamic rendering         │
│    • MatchesViewModel League filter & MatchesScreen chips              │
│    • MatchesViewModelTest                                              │
│    • ./gradlew test & assembleDebug ──► Report & Review ──► Commit     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. Tổng kết Presentation Phase P2                                      │
│    • docs/reports/presentation-p2-final.md                             │
│    • Final Independent Code Review & Finalize Phase P2                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Tiêu chí Hoàn thành Toàn diện (Definition of Done)

- [ ] **P2.1 Hoàn tất**: `BenchmarkViewModel`, UI cards, đo background `Dispatchers.Default`, speedup presentation, test PASS.
- [ ] **P2.2 Hoàn tất**: `TeamsViewModel` tích hợp `CalculateHomeAwaySplitsUseCase`, hiển thị động Home/Away splits trên `TeamAnalyticsCard`, test PASS.
- [ ] **P2.3 Hoàn tất**: `HomeViewModel` live metadata trên `DatasetOverviewCard`, `MatchesScreen` lọc League, test PASS.
- [ ] **Không Regression**: 100% baseline tests (469) + toàn bộ test mới PASS.
- [ ] **Biên dịch Thành công**: `./gradlew assembleDebug` SUCCESS không lỗi lint.
- [ ] **Tài liệu Hoàn chỉnh**: Đầy đủ các báo cáo `docs/reports/presentation-p2.1-benchmark-ui.md`, `presentation-p2.2-splits-ui.md`, `presentation-p2.3-overview-filters.md`, `presentation-p2-final.md`.
- [ ] **Clean Architecture & Design System**: 100% tuân thủ ranh giới layer, không hardcode token, phân rã file $< 400$ dòng.
