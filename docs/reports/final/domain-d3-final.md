# TrueLab – Báo cáo Hoàn thành Triển khai Domain D3 (Domain D3 Search & Sort Integration Report)

Tài liệu này tổng kết toàn diện quá trình triển khai giai đoạn **Domain D3 (Search & Sort Integration)** trong module `:core:domain` của TrueLab, kết nối các thuật toán tìm kiếm (Phase 1: LinearSearch, BinarySearch) và sắp xếp (Phase 2: MergeSort) đã đóng băng (**FROZEN**) từ `:core:algorithm` ra phục vụ độc lập thông qua các Pure Computation Domain UseCases.

---

## 1. Mục tiêu & Phạm vi Giai đoạn D3 (D3 Objectives & Scope)

1. **Độc lập hóa Thuật toán Tìm kiếm & Sắp xếp (Search & Sort Orchestration)**:
   - Expose các thuật toán từ `:core:algorithm` thành các UseCase tính toán nghiệp vụ độc lập trong `:core:domain`:
     - **Phase 1 (Searching Algorithms)**:
       - Tìm kiếm trận đấu theo tên đội bóng (Home/Away partial matching) $\to$ `SearchMatchesUseCase`.
       - Tìm kiếm đội bóng theo tên (Linear substring) và tra cứu theo ID (BinarySearch $O(\log n)$ khi đã sắp xếp, LinearSearch $O(n)$ fallback) $\to$ `SearchTeamsUseCase`.
     - **Phase 2 (Sorting Algorithms)**:
       - Sắp xếp danh sách trận đấu đa tiêu chí (`START_TIME_ASC/DESC`, `TOTAL_GOALS_DESC`, `GOAL_DIFF_DESC`, `ID_ASC`) với **MergeSort** và **Signed Goal Difference** $\to$ `SortMatchesUseCase`.
       - Sắp xếp bảng xếp hạng mùa giải (`POSITION_ASC`, `POINTS_DESC`, `GOAL_DIFF_DESC`, `WINS_DESC`, `LOSSES_ASC`) với **Composite Comparator 4 cấp** và **MergeSort Stability** $\to$ `SortSeasonRankingUseCase`.
2. **Kiến trúc Clean Architecture & Bất biến Dữ liệu (Immutability)**:
   - Toàn bộ UseCases D3 là **Pure Computation** (không side-effect, không phụ thuộc Repository/DAO, không phụ thuộc Android SDK).
   - Tuyệt đối không thay đổi (mutate) danh sách đầu vào; trả về danh sách mới đã được sắp xếp/lọc, hoặc trả về chính danh sách gốc an toàn khi `size <= 1` hoặc query rỗng/blank.
3. **Bảo tồn Tuyệt đối Hệ thống Hiện hữu (Zero Regressions)**:
   - Module `:core:algorithm` duy trì trạng thái **FROZEN** (122/122 test cases pass 100%).
   - Bảo toàn toàn bộ các UseCase legacy (`GetMatchesUseCase`, `GetSeasonRankingUseCase`, `PredictMatchOutcomeUseCase`), không gây breaking change đến ViewModel hay Presentation layer.

---

## 2. Ranh giới Kiến trúc & Luồng Dữ liệu (Architecture & Layer Boundaries)

```text
Presentation Layer (MatchList, SearchScreen, StandingsScreen, TeamPicker)
                       │
                       ▼
      ┌────────────────────────────────────────────────────────┐
      │               :core:domain (D3 UseCases)               │
      │  • SearchMatchesUseCase     (LinearSearch Partial)     │
      │  • SearchTeamsUseCase       (LinearSearch & Binary)    │
      │  • SortMatchesUseCase       (MergeSort & Signed GD)    │
      │  • SortSeasonRankingUseCase (Composite Tie-breakers)   │
      └───────────────────┬──────────────────┬─────────────────┘
                          │                  │
           (Domain Data In)                  (Orchestration thuần)
                          │                  │
                          ▼                  ▼
                [Domain Entities]     [:core:algorithm]
                - Match, TeamSummary  - LinearSearch, BinarySearch
                - SeasonRanking       - MergeSort, SortAlgorithm
                          │           (Pure Kotlin/JVM - FROZEN)
                          ▼
                     :core:data
                (Repositories / Room)
```

### Phân định Trách nhiệm:
1. **`:core:algorithm` (Pure Data Structures & Algorithms)**:
   - Bất biến, 0 khái niệm bóng đá (`Match`, `TeamSummary`, `SeasonRanking`), 0 Android SDK.
   - Cung cấp `LinearSearch<T, K>`, `BinarySearch<T, K>`, `MergeSort<T>`, `QuickSort<T>`, `SortAlgorithm<T>`.
2. **`:core:domain` (Business Orchestration & Pure Computation)**:
   - Tiếp nhận danh sách thực thể Domain, áp dụng tiêu chí so sánh (Comparators), xử lý chuỗi tie-breakers, phân giải query tìm kiếm không phân biệt hoa thường (`ignoreCase = true`), kiểm tra điều kiện tiên quyết của BinarySearch (`isSortedById`).
   - Độc lập hoàn toàn với Room Database, Retrofit API và ViewModel lifecycle.
3. **`:core:data` & Presentation**:
   - `:core:data` chịu trách nhiệm nạp dữ liệu thô từ Remote/Local DB.
   - Presentation Layer (ViewModel) sẽ kết hợp Search và Sort UseCases theo luồng Unidirectional Data Flow (UDF) tại Phase D4.

---

## 3. Chi tiết Triển khai D3.1 — Search Integration (Phase 1: Searching)

### 3.1. `SearchMatchesUseCase`
- **File nguồn**: [`SearchMatchesUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SearchMatchesUseCase.kt)
- **File kiểm thử**: [`SearchMatchesUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SearchMatchesUseCaseTest.kt) (12 tests)
- **Algorithm Dependency**: [`LinearSearch<Match, String>`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/searching/LinearSearch.kt).
- **Contract & Signature**:
  ```kotlin
  class SearchMatchesUseCase(
      private val searchAlgorithm: LinearSearch<Match, String> = LinearSearch()
  ) {
      operator fun invoke(
          matches: List<Match>,
          query: String
      ): List<Match>
  }
  ```
- **Nghiệp vụ & Xử lý Dữ liệu**:
  1. **Khớp hai chiều (Home & Away)**: Một trận đấu được tính là khớp nếu tên đội nhà **hoặc** tên đội khách chứa chuỗi tìm kiếm:
     ```kotlin
     match.homeTeam.name.contains(trimmedQuery, ignoreCase = true) ||
     match.awayTeam.name.contains(trimmedQuery, ignoreCase = true)
     ```
  2. **Xử lý Blank / Empty Query**: Khi `query.isBlank()` hoặc `matches.isEmpty()`, trả về ngay lập tức chính `matches` (early return), không mutate danh sách và không cấp phát bộ nhớ không cần thiết.
  3. **Cắt tỉa khoảng trắng (Trim)**: Loại bỏ khoảng trắng thừa đầu/cuối của từ khóa tìm kiếm (`query.trim()`).
  4. **Ủy thác thuật toán**: Sử dụng `searchAlgorithm.searchAllPartial(matches) { ... }` nhận danh sách indices khớp và trích xuất kết quả `map { matches[it] }`.
- **Commit**: [`3802535`](https://github.com/AnhQuocs/truelab-android/commit/3802535).

---

### 3.2. `SearchTeamsUseCase`
- **File nguồn**: [`SearchTeamsUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SearchTeamsUseCase.kt)
- **File kiểm thử**: [`SearchTeamsUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SearchTeamsUseCaseTest.kt) (23 tests)
- **Algorithm Dependencies**:
  - `LinearSearch<TeamSummary, String>` (cho tìm kiếm theo tên)
  - `SearchAlgorithm<TeamSummary, Int>` (cho tra cứu ID tuyến tính)
  - `SearchAlgorithm<TeamSummary, Int>` (mặc định [`BinarySearch`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/searching/BinarySearch.kt) cho tra cứu ID nhị phân)
- **Contract & Signature**:
  ```kotlin
  class SearchTeamsUseCase(
      private val linearSearchByName: LinearSearch<TeamSummary, String> = LinearSearch(),
      private val linearSearchById: SearchAlgorithm<TeamSummary, Int> = LinearSearch(),
      private val binarySearchById: SearchAlgorithm<TeamSummary, Int> = BinarySearch()
  ) {
      fun searchByName(teams: List<TeamSummary>, query: String): List<TeamSummary>
      fun findById(teams: List<TeamSummary>, teamId: Int, isSortedById: Boolean = false): TeamSummary?
  }
  ```
- **Nghiệp vụ & Xử lý Điều kiện Tiên quyết (Precondition Handling)**:
  1. **`searchByName`**:
     - Tìm kiếm chuỗi con không phân biệt hoa thường (`ignoreCase = true`).
     - Early return `teams` khi `query.isBlank()`.
     - Trả về danh sách rỗng khi không có đội bóng thỏa mãn.
  2. **`findById` & BinarySearch Precondition**:
     - Thuật toán `BinarySearch` có điều kiện tiên quyết nghiêm ngặt: **tập dữ liệu đầu vào bắt buộc phải được sắp xếp tăng dần theo khóa tìm kiếm (ID)**.
     - Tham số `isSortedById: Boolean = false` cho phép caller khai báo trạng thái của tập dữ liệu:
       - Khi `isSortedById = true`: Hệ thống ủy thác trực tiếp cho `binarySearchById.search(...)` đạt hiệu năng tối ưu $O(\log n)$.
       - Khi `isSortedById = false` (mặc định): Hệ thống kích hoạt fallback an toàn qua `linearSearchById.search(...)` $O(n)$, ngăn ngừa lỗi logic hoặc bỏ sót phần tử khi dữ liệu chưa được sắp xếp.
     - Trả về `teams[index]` khi tìm thấy hoặc `null` nếu index là `-1`.
- **Commit**: [`3802535`](https://github.com/AnhQuocs/truelab-android/commit/3802535).

---

## 4. Chi tiết Triển khai D3.2 — Sort Integration (Phase 2: Sorting)

### 4.1. `SortMatchesUseCase`
- **File model**: [`MatchSortCriteria.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/MatchSortCriteria.kt)
- **File nguồn**: [`SortMatchesUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SortMatchesUseCase.kt)
- **File kiểm thử**: [`SortMatchesUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SortMatchesUseCaseTest.kt) (12 tests)
- **Algorithm Dependency**: [`SortAlgorithm<Match>`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/SortAlgorithm.kt) (mặc định [`MergeSort<Match>()`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/MergeSort.kt)).
- **Contract & Signature**:
  ```kotlin
  enum class MatchSortCriteria {
      START_TIME_ASC,      // Thời gian thi đấu tăng dần (sớm nhất trước)
      START_TIME_DESC,     // Thời gian thi đấu giảm dần (mới nhất trước)
      TOTAL_GOALS_DESC,    // Tổng số bàn thắng nhiều nhất trước
      GOAL_DIFF_DESC,      // Hiệu số bàn thắng có dấu giảm dần (+3 > +1 > 0 > -2)
      ID_ASC               // Theo ID tăng dần (chuẩn bị dữ liệu cho BinarySearch)
  }

  class SortMatchesUseCase(
      private val sortAlgorithm: SortAlgorithm<Match> = MergeSort()
  ) {
      operator fun invoke(
          matches: List<Match>,
          criteria: MatchSortCriteria = MatchSortCriteria.START_TIME_ASC
      ): List<Match>
  }
  ```
- **Nghiệp vụ Sắp xếp & Semantics**:
  1. **Signed Goal Difference trong `GOAL_DIFF_DESC`**:
     - Hiệu số bàn thắng là đại lượng có dấu: `goalDifference = (homeScore ?: 0) - (awayScore ?: 0)`.
     - Phép so sánh sử dụng trực tiếp:
       ```kotlin
       b.goalDifference.compareTo(a.goalDifference)
       ```
     - **Tuyệt đối không dùng `abs()`**: Đảm bảo trận thắng cách biệt (+3) đứng trước trận thắng sít sao (+1), trước trận hòa (0), và trước trận thua (-1, -2). Cụ thể: $+1$ phải luôn đứng trước $-2$.
  2. **`ID_ASC`**: Sắp xếp tăng dần theo ID (`a.id.compareTo(b.id)`), hỗ trợ chuẩn bị tập dữ liệu đáp ứng điều kiện tiên quyết cho `SearchTeamsUseCase.findById(..., isSortedById = true)`.
  3. **Bảo toàn Immutability & Early Return**:
     - Khi `matches.size <= 1` $\implies$ early return `matches` trực tiếp, không mutate và không cấp phát thêm bộ nhớ.
     - Khi `matches.size > 1` $\implies$ `sortAlgorithm.sort(matches, comparator)` trả về bản sao danh sách mới.
- **Commit**: [`294a52f`](https://github.com/AnhQuocs/truelab-android/commit/294a52f).

---

### 4.2. `SortSeasonRankingUseCase`
- **File model**: [`StandingsSortCriteria.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/StandingsSortCriteria.kt)
- **File nguồn**: [`SortSeasonRankingUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SortSeasonRankingUseCase.kt)
- **File kiểm thử**: [`SortSeasonRankingUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SortSeasonRankingUseCaseTest.kt) (15 tests)
- **Algorithm Dependency**: [`SortAlgorithm<SeasonRanking>`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/SortAlgorithm.kt) (mặc định [`MergeSort<SeasonRanking>()`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/MergeSort.kt)).
- **Contract & Signature**:
  ```kotlin
  enum class StandingsSortCriteria {
      POSITION_ASC,        // Thứ hạng chính thức tăng dần (1, 2, 3...)
      POINTS_DESC,         // Tổng điểm giảm dần (kèm composite tie-breakers)
      GOAL_DIFF_DESC,      // Hiệu số bàn thắng giảm dần
      WINS_DESC,           // Số trận thắng nhiều nhất trước
      LOSSES_ASC           // Số trận thua ít nhất trước
  }

  class SortSeasonRankingUseCase(
      private val sortAlgorithm: SortAlgorithm<SeasonRanking> = MergeSort()
  ) {
      operator fun invoke(
          rankings: List<SeasonRanking>,
          criteria: StandingsSortCriteria = StandingsSortCriteria.POSITION_ASC
      ): List<SeasonRanking>
  }
  ```
- **Nghiệp vụ Xếp hạng & Phân định Vai trò**:
  1. **Composite Comparator 4 Cấp (`POINTS_DESC`)**:
     - Khi sắp xếp theo điểm số, bảng xếp hạng bóng đá đòi hỏi giải quyết chuỗi tie-breakers nghiêm ngặt theo thứ tự ưu tiên:
       $$\text{Points DESC} \longrightarrow \text{GoalDiff DESC} \longrightarrow \text{Wins DESC} \longrightarrow \text{Position ASC}$$
     - Triển khai cụ thể:
       ```kotlin
       StandingsSortCriteria.POINTS_DESC -> Comparator<SeasonRanking> { a, b ->
           val ptsComp = b.totalPoints.compareTo(a.totalPoints)
           if (ptsComp != 0) return@Comparator ptsComp

           val gdComp = b.goalDiff.compareTo(a.goalDiff)
           if (gdComp != 0) return@Comparator gdComp

           val wonComp = b.won.compareTo(a.won)
           if (wonComp != 0) return@Comparator wonComp

           a.position.compareTo(b.position)
       }
       ```
  2. **Phân biệt Rạch ròi giữa Composite Comparator và MergeSort Stability**:
     - **Composite Comparator** là cơ chế nghiệp vụ duy nhất giải quyết thứ tự ưu tiên giữa các tiêu chí (Điểm $\to$ Hiệu số $\to$ Thắng $\to$ Vị trí).
     - **MergeSort Stability** **không** thay thế tie-breakers; tính ổn định của MergeSort chỉ phát huy tác dụng khi Comparator trả về `0` (khi hai đội bóng hoàn toàn bằng nhau ở cả 4 tiêu chí). Khi đó, MergeSort bảo đảm giữ nguyên thứ tự tương đối ban đầu trong input list mà không bị xáo trộn ngẫu nhiên.
  3. **Các tiêu chí đơn khác**:
     - `POSITION_ASC`: `a.position.compareTo(b.position)`
     - `GOAL_DIFF_DESC`: `b.goalDiff.compareTo(a.goalDiff)`
     - `WINS_DESC`: `b.won.compareTo(a.won)`
     - `LOSSES_ASC`: `a.loss.compareTo(b.loss)` (đội thua ít nhất lên đầu)
- **Commit**: [`294a52f`](https://github.com/AnhQuocs/truelab-android/commit/294a52f).

---

## 5. Báo cáo Kết quả Kiểm thử Chi tiết (Test Results & Coverage)

Kiểm thử được thực thi với task tái chạy toàn diện:
```bash
.\gradlew :core:domain:test :core:algorithm:test --rerun-tasks
```

### 5.1. Bảng Chi tiết Test Suites D3:

| STT | Test Suite Class | File | Số Tests | Trạng thái | Nội dung Kiểm thử Trọng tâm |
|:---:|---|---|:---:|:---:|---|
| **1** | `SearchMatchesUseCaseTest` | [`SearchMatchesUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SearchMatchesUseCaseTest.kt) | 12 | **PASS 100%** | Khớp theo Home team, Away team, cả hai đội, không phân biệt hoa thường (`ignoreCase`), tìm kiếm substring, cắt tỉa khoảng trắng (`trim`), query rỗng/blank trả về toàn bộ input (`assertSame`), không tìm thấy, input rỗng, immutability, custom delegation injection. |
| **2** | `SearchTeamsUseCaseTest` | [`SearchTeamsUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SearchTeamsUseCaseTest.kt) | 23 | **PASS 100%** | `searchByName`: Khớp chính xác, chuỗi con, hoa thường, query blank/rỗng, input rỗng, immutability.<br>`findById`: Fallback LinearSearch khi `isSortedById = false` (đầu, giữa, cuối, không tồn tại, rỗng); BinarySearch tối ưu khi `isSortedById = true` (đầu, giữa, cuối, ngoài biên trái/phải, lỗ hổng ID, rỗng); xác thực chuyển mạch delegation giữa LinearSearch và BinarySearch. |
| **3** | `SortMatchesUseCaseTest` | [`SortMatchesUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SortMatchesUseCaseTest.kt) | 12 | **PASS 100%** | Default criteria (`START_TIME_ASC`), thời gian tăng/giảm dần, tổng bàn thắng giảm dần, **Signed Goal Difference** (+3 > +1 > 0 > -1 > -2, khẳng định `+1` trước `-2`), ID tăng dần, MergeSort stability khi bằng giá trị, xử lý tỷ số null an toàn, empty/singleton list, immutability, custom delegation. |
| **4** | `SortSeasonRankingUseCaseTest` | [`SortSeasonRankingUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SortSeasonRankingUseCaseTest.kt) | 15 | **PASS 100%** | Default criteria (`POSITION_ASC`), Position ASC, GD DESC, Wins DESC, Losses ASC, Points DESC khi điểm khác nhau, Tie-breaker 1 (GD DESC khi điểm bằng), Tie-breaker 2 (Wins DESC khi điểm & GD bằng), Tie-breaker 3 (Position ASC khi điểm, GD & Wins bằng), Kịch bản phân cấp tổng hợp 5 đội bóng, MergeSort stability khi bằng tuyệt đối (kiểm thử 2 chiều), empty/singleton, immutability, delegation. |
| **Tổng** | **4 Suites mới của D3** | | **62** | **PASS 100%** | **Toàn bộ 62/62 tests mới hoàn thành xuất sắc.** |

---

## 6. Trạng thái Kiểm thử Hồi quy Toàn diện (Full Regression Status)

```text
================================================================================
                    TRUELAB GRADLE TEST EXECUTION SUMMARY
================================================================================
  Module :core:algorithm:test  ===>  122 / 122 PASSED  (100% - FROZEN)
  Module :core:domain:test     ===>  156 / 156 PASSED  (100%)
    - Prediction Suite (Pre-existing) :  35 tests
    - Analytics Suite D2              :  59 tests
    - Search & Sort Suite D3 (New)    :  62 tests
  Build Output                 ===>  BUILD SUCCESSFUL (8 actionable tasks executed)
================================================================================
```

### Chi tiết Phân rã Test Suites trong Toàn Dự án:

#### 1. Module `:core:algorithm` (122 tests — FROZEN):
- `FormScoreAlgorithmsTest`: 15 tests
- `WeightedScoringAlgorithmsTest`: 27 tests
- `EloRatingAlgorithmsTest`: 22 tests
- `SearchingAlgorithmsTest`: 12 tests
- `SortingAlgorithmsTest`: 9 tests
- `StatisticsAlgorithmsTest`: 16 tests
- `MovingAverageAlgorithmsTest`: 21 tests

#### 2. Module `:core:domain` (156 tests):
- **Prediction Pipeline**: 35 tests (7 weight config, 22 transformers, 6 outcome prediction).
- **D2 Analytics Pipeline**: 59 tests (13 form, 13 elo, 16 odds trend, 17 team stats).
- **D3 Search & Sort Pipeline**: 62 tests (12 search matches, 23 search teams, 12 sort matches, 15 sort rankings).

**Độ ổn định Hồi quy**: 100% tests PASS, 0 failure, 0 skipped, 0 flaky test.

---

## 7. Bảo tồn Ràng buộc Legacy & Cách ly Ranh giới (Legacy Isolation)

Trong suốt quá trình thực thi D3, các nguyên tắc bảo vệ hệ thống đã được tuân thủ nghiêm ngặt:

1. **Bảo toàn `:core:algorithm`**: Không thêm, bớt, hoặc sửa đổi bất kỳ ký tự nào trong module `:core:algorithm` (FROZEN 122/122).
2. **Bảo toàn UseCases Legacy**:
   - `GetMatchesUseCase` và `GetSeasonRankingUseCase` được giữ nguyên vẹn 100%, đảm bảo các ViewModels hiện tại của ứng dụng không bị ảnh hưởng.
   - Quá trình chuyển đổi ViewModel sang sử dụng `SearchMatchesUseCase` và `SortMatchesUseCase` được bảo lưu sang **Phase D4**.
3. **Bảo tồn Pipeline Dự đoán**:
   - `PredictMatchOutcomeUseCase`, `PredictionResult.Companion.computeWeightedScoring`, `PredictionRepository` tiếp tục hoạt động ổn định.
4. **Cách ly Module**: Không can thiệp vào `:core:data`, `:core:ui`, hay `:app`.

---

## 8. Lịch sử Commits Triển khai D3 (Git Manifest)

Toàn bộ giai đoạn D3 được triển khai và đưa lên remote qua 2 atomic commits độc lập:

| Sub-phase | Commit SHA | Conventional Commit Message | Nội dung Triển khai |
|:---:|:---:|---|---|
| **D3.1** | [`3802535`](https://github.com/AnhQuocs/truelab-android/commit/3802535) | `feat(domain): implement D3.1 search use cases` | Triển khai `SearchMatchesUseCase`, `SearchTeamsUseCase`, 35 unit tests và cập nhật tài liệu thiết kế `domain-d3-search-sort-plan.md`. |
| **D3.2** | [`294a52f`](https://github.com/AnhQuocs/truelab-android/commit/294a52f) | `feat(domain): implement D3.2 sort use cases` | Triển khai `MatchSortCriteria`, `SortMatchesUseCase`, `StandingsSortCriteria`, `SortSeasonRankingUseCase`, và 27 unit tests. |

---

## 9. Giới hạn Đã biết & Định hướng Tích hợp Tương lai (Known Limitations & Future Work)

1. **Phạm vi QuickSort**:
   - Thuật toán `QuickSort` trong `:core:algorithm` không được đóng gói thành UseCase riêng trong D3 do các màn hình bóng đá hiện tại ưu tiên tính ổn định (Stable Ordering) của `MergeSort`. Caller khi cần có thể chủ động inject `QuickSort` thông qua interface `SortAlgorithm<T>`.
2. **Kế hoạch Tích hợp Phase D4 (Presentation Integration & Legacy Cleanup)**:
   - Tích hợp `SearchMatchesUseCase` và `SortMatchesUseCase` vào `MatchesViewModel` để hỗ trợ tìm kiếm trực tiếp và lọc lịch thi đấu linh hoạt trên giao diện Jetpack Compose.
   - Tích hợp `SortSeasonRankingUseCase` vào `StandingsViewModel` để người dùng có thể đổi tiêu chí sắp xếp bảng xếp hạng (theo điểm số, hiệu số, hoặc số trận thắng).
   - Tái cấu trúc (refactor) hoặc deprecate an toàn các phương thức legacy trong `GetMatchesUseCase` sau khi Presentation Layer đã kết nối hoàn chỉnh với các UseCases D3.

---

## 10. Bảng Đối chiếu Tiêu chí Hoàn thành (Definition of Done Checklist)

- [x] **Đầy đủ 4 UseCases & 2 Enums**: Cả 4 UseCases (`SearchMatchesUseCase`, `SearchTeamsUseCase`, `SortMatchesUseCase`, `SortSeasonRankingUseCase`) và 2 Enums (`MatchSortCriteria`, `StandingsSortCriteria`) đã được triển khai chuẩn mực trong `:core:domain`.
- [x] **Xử lý Điều kiện Tiên quyết BinarySearch**: `SearchTeamsUseCase.findById` cung cấp cờ `isSortedById` với cơ chế fallback sang `LinearSearch` an toàn.
- [x] **Xử lý Signed Goal Difference**: `MatchSortCriteria.GOAL_DIFF_DESC` so sánh giá trị có dấu chính xác, khẳng định `+1` đứng trước `-2`, không dùng `abs()`.
- [x] **Composite Tie-breakers Chuẩn mực**: `SortSeasonRankingUseCase` triển khai đầy đủ chuỗi tie-breakers 4 tầng theo thứ tự: Điểm $\to$ Hiệu số $\to$ Trận thắng $\to$ Thứ hạng.
- [x] **Phân định MergeSort Stability**: Làm rõ và xác thực tính ổn định của MergeSort chỉ can thiệp khi Comparator trả về 0.
- [x] **Bảo toàn Immutability**: Không mutate bất kỳ danh sách đầu vào nào; early return an toàn khi `size <= 1` hoặc query blank.
- [x] **100% Tests Mới Vượt qua**: 62/62 Unit Tests mới của D3 đều PASS 100%.
- [x] **Bảo toàn Tuyệt đối Thuật toán**: 122/122 Tests của `:core:algorithm` PASS (FROZEN không bị xâm phạm).
- [x] **Tổng số Test Domain Đạt Chuẩn**: 156/156 Tests của `:core:domain` PASS 100%.
- [x] **Bảo tồn Tuyệt đối Legacy Code**: Không gây bất kỳ breaking change nào cho hệ thống dự đoán và các UseCase cũ.
- [x] **Commits Chuẩn Conventional**: Hai commits D3.1 (`3802535`) và D3.2 (`294a52f`) đã được đẩy thành công lên `origin/main`.
- [x] **Báo cáo Hoàn thành Đầy đủ**: Tài liệu `docs/reports/final/domain-d3-final.md` phản ánh trung thực mã nguồn thực tế và đã được lưu trữ trên disk.

---

## 11. Kết luận & Trạng thái Cuối cùng (Final D3 Status)

> **TRẠNG THÁI GIAI ĐOẠN D3**: **HOÀN THÀNH TOÀN DIỆN (APPROVED & INTEGRATED)**
>
> Toàn bộ các thuật toán Tìm kiếm (Phase 1) và Sắp xếp (Phase 2) cốt lõi đã được đưa ra tầng `:core:domain` thành công, tạo nên nền tảng tìm kiếm, lọc và phân hạng dữ liệu bóng đá mạnh mẽ, ổn định và an toàn cho TrueLab. Hệ thống sẵn sàng bước vào giai đoạn tiếp theo theo lộ trình dự án.
