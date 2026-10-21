# TrueLab – Kế hoạch Triển khai Domain D3: Search & Sort Integration

Tài liệu này xác lập bản thiết kế kiến trúc và kế hoạch triển khai chi tiết cho giai đoạn **Domain D3 (Search & Sort Integration)** trong module `:core:domain` của TrueLab, nhằm đưa 2 thuật toán nền tảng đã đóng băng (**FROZEN**) từ `:core:algorithm` gồm **Phase 1 (Searching)** và **Phase 2 (Sorting)** ra phục vụ độc lập thông qua các Pure Computation Domain UseCases.

---

## 1. Bối cảnh & Hiện trạng Kiểm toán (Current State Audit)

### 1.1. Hiện trạng sau Giai đoạn D2:
- Giai đoạn **Domain D2 (Analytics, Form & Rating Integration)** đã hoàn tất 100% với 4 UseCases độc lập:
  - D2.1: `CalculateTeamFormUseCase` (Phase 5 Form Score) — commit `5d54e0c`
  - D2.2: `CalculateEloRatingUseCase` (Phase 6 Elo Rating) — commit `d1f7386`
  - D2.3: `AnalyzeOddsTrendUseCase` (Phase 4 SMA & Phase 3 Volatility) — commit `a88fd0f`
  - D2.4: `GetTeamStatisticsUseCase` (Phase 3 Descriptive Statistics) — commit `57ec936`
- **Regression Baseline**:
  - `:core:algorithm:test`: **122 / 122 PASS** (FROZEN).
  - `:core:domain:test`: **94 / 94 PASS** (35 tests tiền nhiệm + 59 tests mới của D2).
  - Working tree Git hiện tại: Sạch hoàn toàn (`clean`), đã đồng bộ với `origin/main`.

### 1.2. Kết quả Kiểm toán Trực tiếp từ Disk (Source Code Audit):

#### a. Kiểm toán Phase 1 — Searching (`dev.anhquocs.truelab.core.algorithm.searching`):
- **Contract Interface**:
  ```kotlin
  interface SearchAlgorithm<T, K> {
      fun search(dataset: List<T>, target: K, selector: (T) -> K): Int
  }
  ```
- **`LinearSearch<T, K>`**:
  1. `search(dataset: List<T>, target: K, selector: (T) -> K): Int` — Tìm kiếm chính xác (Exact Match), trả về index đầu tiên hoặc `-1` ($O(n)$).
  2. `searchPartial(dataset: List<T>, predicate: (T) -> Boolean): Int` — Tìm kiếm tương đối qua predicate, trả về index đầu tiên hoặc `-1`.
  3. `searchAllPartial(dataset: List<T>, predicate: (T) -> Boolean): List<Int>` — Tìm kiếm tương đối qua predicate, trả về `List<Int>` chứa toàn bộ các index thỏa mãn điều kiện.
- **`BinarySearch<T, K : Comparable<K>>`**:
  - `search(dataset: List<T>, target: K, selector: (T) -> K): Int` — Tìm kiếm nhị phân chia để trị ($O(\log n)$).
  - **Precondition Bắt buộc**: `dataset` **BẮT BUỘC phải được sắp xếp tăng dần** theo khóa trích xuất bởi `selector(T)`.
  - **Ràng buộc Thuật toán**: Chỉ hỗ trợ Exact Match trên khóa so sánh được (`Comparable<K>`), **KHÔNG hỗ trợ Partial Match** (không tìm kiếm chuỗi con `contains`).
- **Phân tích Nghiệp vụ Tìm kiếm**:
  - *Tìm kiếm trận đấu theo tên đội (`query: String`)*: Do tên đội bóng người dùng nhập là chuỗi con (substring) và một trận đấu chứa cả `homeTeam` lẫn `awayTeam` (không thể cùng lúc sắp xếp danh sách trận theo cả 2 tên đội), đây là bài toán **Partial Search** đa kết quả $\implies$ `LinearSearch.searchAllPartial` là giải pháp kỹ thuật duy nhất đúng đắn và khả thi.
  - *Tra cứu theo ID (`id: Int` hoặc `id: Long`)*: Khi danh sách đã được sắp xếp theo ID $\implies$ áp dụng `BinarySearch` để đạt độ phức tạp $O(\log n)$; nếu danh sách chưa sắp xếp $\implies$ fallback sang `LinearSearch` $O(n)$.

#### b. Kiểm toán Phase 2 — Sorting (`dev.anhquocs.truelab.core.algorithm.sorting`):
- **Contract Interface**:
  ```kotlin
  interface SortAlgorithm<T> {
      fun sort(dataset: List<T>, comparator: Comparator<T>): List<T>
  }
  ```
- **`QuickSort<T>`**:
  - Độ phức tạp trung bình $O(n \log n)$, chọn pivot ở giữa mảng.
  - **Không ổn định (Unstable)**: Có thể làm thay đổi thứ tự tương đối của các phần tử có giá trị so sánh bằng nhau.
  - Bảo toàn tính bất biến (thao tác trên bản sao nội bộ).
  - *Phạm vi QuickSort trong D3*: Mặc dù `QuickSort` đã hoàn thiện và đóng băng ở `:core:algorithm` (122/122 tests), các UseCase trong D3 ưu tiên sử dụng `MergeSort` làm implementation mặc định do các yêu cầu nghiệp vụ hiện tại (lịch thi đấu, bảng xếp hạng) đều đòi hỏi tính ổn định (Stable Ordering). Caller vẫn có thể inject `QuickSort` qua interface `SortAlgorithm<T>` nếu có nhu cầu benchmark hoặc các trường hợp không yêu cầu bảo toàn thứ tự ban đầu.
- **`MergeSort<T>`**:
  - Độ phức tạp luôn là $O(n \log n)$ trong mọi trường hợp.
  - **Ổn định (Stable)**: Đảm bảo giữ nguyên thứ tự xuất hiện ban đầu của các phần tử có cùng giá trị so sánh (equivalent).
  - Bảo toàn tính bất biến (trả về `List<T>` mới).
- **Phân tích Nghiệp vụ Sắp xếp**:
  - *Lịch thi đấu / Danh sách trận đấu*: Nhiều trận đấu diễn ra cùng ngày giờ (`startTimeDate`). Tính **Stable** của `MergeSort` đảm bảo thứ tự ban đầu của các trận cùng giờ không bị xáo trộn ngẫu nhiên.
  - *Bảng xếp hạng Mùa giải (`SeasonRanking`)*: Các đội bóng thường xuyên có cùng điểm số. Lưu ý rằng **Composite Comparator** trong Domain mới là cơ chế chính xác để xử lý chuỗi tie-breakers (Điểm $\to$ Hiệu số $\to$ Số trận thắng $\to$ Thứ hạng). Thuật toán `MergeSort` đóng vai trò bảo toàn tính ổn định (**Stable Ordering**) cho các trường hợp mà Comparator đánh giá hai phần tử là tương đương (equivalent), giúp giữ nguyên trật tự tương đối ban đầu thay vì xáo trộn như các thuật toán không ổn định.

#### c. Kiểm toán các UseCases Hiện có trong `:core:domain`:
- **`GetMatchesUseCase`**: Đang sử dụng Kotlin stdlib `.filter` và `.sortedBy { it.startTimeDate }`.
- **`GetSeasonRankingUseCase`**: Đang sử dụng Kotlin stdlib `.sortedBy { it.position }`.
- **`GetOddsHistoryUseCase`**: Đang sử dụng Kotlin stdlib `.sortedBy { it.changeTime }`.
- **Đánh giá Migration**: Các UseCase trên hiện đang gắn liền với Repository và cung cấp Flow cho Presentation layer. Để tránh breaking changes trong D3, ta giữ nguyên contract của các UseCase hiện tại và xây dựng các UseCase Search/Sort mới độc lập dưới dạng **Pure Computation UseCases**.

---

## 2. Mục tiêu Giai đoạn D3 (D3 Goals)

1. **Độc lập hóa Thuật toán Tìm kiếm & Sắp xếp**:
   - Đưa Phase 1 (`LinearSearch`, `BinarySearch`) vào Domain thành UseCases phục vụ lọc/tìm kiếm dữ liệu nghiệp vụ.
   - Đưa Phase 2 (`SortAlgorithm` với triển khai mặc định `MergeSort`) vào Domain thành UseCases phục vụ sắp xếp đa tiêu chí. `QuickSort` tiếp tục nằm trong `:core:algorithm` và có thể inject linh hoạt qua `SortAlgorithm<T>`, nhưng D3 không cần expose UseCase riêng cho QuickSort vì nghiệp vụ Domain hiện tại ưu tiên stable ordering.
2. **Tuân thủ Kiến trúc Pure Computation**:
   - Các UseCases D3 đều là Pure Kotlin/JVM, không phụ thuộc Android SDK, Room, DAO hay Repository.
   - Nhận vào dữ liệu bộ nhớ (`List<T>`), thực hiện thuật toán và trả về dữ liệu mới bất biến (`immutable`).
3. **Bảo toàn Tuyệt đối (Zero Breaking Changes)**:
   - Module `:core:algorithm` giữ trạng thái **FROZEN** (122/122 test cases pass 100%).
   - Không thay đổi signature hay xóa bỏ bất kỳ UseCase/Repository hiện có nào.
   - Bảo toàn các hàm legacy (`PredictionResult.computeWeightedScoring`, `PredictMatchUseCase`, `SeasonRanking.calculateFormScore`, `PredictionRepository.predictMatch`).

---

## 3. Kiến trúc Domain D3 (D3 Architecture)

```text
Presentation Layer (ViewModels: MatchList, Standings, TeamSearch, Analytics)
                     │
                     ▼
    ┌────────────────────────────────────────────────────────┐
    │                  :core:domain (D3 UseCases)            │
    │  • SearchMatchesUseCase       (Phase 1 LinearSearch)   │
    │  • SearchTeamsUseCase         (Phase 1 Linear & Binary)│
    │  • SortMatchesUseCase         (Phase 2 MergeSort)      │
    │  • SortSeasonRankingUseCase   (Phase 2 MergeSort)      │
    └──────────────────┬──────────────────┬──────────────────┘
                       │                  │
        (Trích xuất dữ liệu)     (Tính toán thuật toán thuần)
                       │                  │
                       ▼                  ▼
             [Repository Interfaces]   [:core:algorithm]
                       │               (Pure Kotlin / JVM - FROZEN)
                       ▼
                  :core:data
             (Room Database / API)
```

---

## 4. Thiết kế Chi tiết UseCases D3 (Specifications)

---

### 4.1. D3.1 — Search Integration (Phase 1: Searching)

#### A. `SearchMatchesUseCase`
- **Mục tiêu**: Lọc danh sách trận đấu theo từ khóa tên đội bóng (Home/Away) không phân biệt chữ hoa chữ thường.
- **Package**: `dev.anhquocs.truelab.core.domain.match.usecase`
- **Class**: `SearchMatchesUseCase`
- **Dependency**: `LinearSearch<Match, String>`
- **Contract & Signature**:
  ```kotlin
  class SearchMatchesUseCase(
      private val searchAlgorithm: LinearSearch<Match, String> = LinearSearch()
  ) {
      /**
       * Tìm kiếm các trận đấu có tên đội nhà hoặc đội khách chứa [query].
       *
       * @param matches Danh sách trận đấu cần tìm.
       * @param query Từ khóa tìm kiếm.
       * @return Danh sách các trận đấu thỏa mãn. Nếu [query] rỗng hoặc trắng, trả về [matches].
       */
      operator fun invoke(
          matches: List<Match>,
          query: String
      ): List<Match> {
          if (query.isBlank() || matches.isEmpty()) return matches

          val trimmedQuery = query.trim()
          val matchingIndices = searchAlgorithm.searchAllPartial(matches) { match ->
              match.homeTeam.name.contains(trimmedQuery, ignoreCase = true) ||
                  match.awayTeam.name.contains(trimmedQuery, ignoreCase = true)
          }

          return matchingIndices.map { matches[it] }
      }
  }
  ```
- **Xử lý Biên (Edge Cases)**:
  - `query` rỗng/khoảng trắng $\implies$ trả về toàn bộ danh sách `matches` ban đầu.
  - `matches` rỗng $\implies$ trả về `emptyList()`.
  - Không tìm thấy $\implies$ trả về `emptyList()`.
  - Tính bất biến: Danh sách gốc không bị mutate.

---

#### B. `SearchTeamsUseCase`
- **Mục tiêu**: Tìm kiếm danh sách đội bóng theo tên (Partial query) và tra cứu chính xác đội bóng theo ID (Exact ID Lookup với BinarySearch).
- **Package**: `dev.anhquocs.truelab.core.domain.team.usecase`
- **Class**: `SearchTeamsUseCase`
- **Dependencies**: `LinearSearch<TeamSummary, String>`, `BinarySearch<TeamSummary, Int>`
- **Contract & Signature**:
  ```kotlin
  class SearchTeamsUseCase(
      private val linearSearch: LinearSearch<TeamSummary, String> = LinearSearch(),
      private val binarySearch: BinarySearch<TeamSummary, Int> = BinarySearch()
  ) {
      /**
       * Tìm kiếm danh sách đội bóng có tên chứa [query] (không phân biệt hoa thường).
       */
      fun searchByName(
          teams: List<TeamSummary>,
          query: String
      ): List<TeamSummary> {
          if (query.isBlank() || teams.isEmpty()) return teams

          val trimmedQuery = query.trim()
          val matchingIndices = linearSearch.searchAllPartial(teams) { team ->
              team.name.contains(trimmedQuery, ignoreCase = true)
          }

          return matchingIndices.map { teams[it] }
      }

      /**
       * Tra cứu chính xác một đội bóng theo ID.
       *
       * @param teams Danh sách các đội bóng.
       * @param teamId ID đội bóng cần tra cứu.
       * @param isSortedById Nếu true, danh sách đã được sắp xếp tăng dần theo ID và UseCase
       *                     sẽ sử dụng BinarySearch O(log n). Nếu false, sử dụng LinearSearch O(n).
       * @return [TeamSummary] nếu tìm thấy, ngược lại trả về null.
       */
      fun findById(
          teams: List<TeamSummary>,
          teamId: Int,
          isSortedById: Boolean = false
      ): TeamSummary? {
          if (teams.isEmpty()) return null

          val index = if (isSortedById) {
              binarySearch.search(teams, teamId) { it.id }
          } else {
              linearSearch.search(teams, teamId) { it.id }
          }

          return if (index != -1) teams[index] else null
      }
  }
  ```
- **Rationale**:
  - `searchByName` tận dụng `LinearSearch.searchAllPartial` cho tìm kiếm chuỗi con.
  - `findById` tận dụng `BinarySearch` $O(\log n)$ khi tập dữ liệu đã được sắp xếp sẵn theo ID (ví dụ sau khi qua bước Sort), đồng thời cung cấp fallback `LinearSearch` $O(n)$ an toàn khi tập dữ liệu chưa được sắp xếp.

---

### 4.2. D3.2 — Sort Integration (Phase 2: Sorting)

#### A. `SortMatchesUseCase`
- **Mục tiêu**: Sắp xếp danh sách trận đấu theo các tiêu chí thời gian, hiệu số bàn thắng có dấu, tổng bàn thắng và ID.
- **Package**: `dev.anhquocs.truelab.core.domain.match.usecase`
- **Class**: `SortMatchesUseCase`
- **Model**: `MatchSortCriteria.kt`
- **Dependency**: `SortAlgorithm<Match>` (mặc định `MergeSort<Match>()` để đảm bảo tính ổn định Stable Ordering).
- **Contract & Signature**:
  ```kotlin
  enum class MatchSortCriteria {
      START_TIME_ASC,      // Thời gian thi đấu tăng dần (sớm nhất trước)
      START_TIME_DESC,     // Thời gian thi đấu giảm dần (mới nhất trước)
      TOTAL_GOALS_DESC,    // Tổng bàn thắng nhiều nhất trước
      GOAL_DIFF_DESC,      // Hiệu số bàn thắng có dấu giảm dần (+3 > +1 > 0 > -2)
      ID_ASC               // Theo ID tăng dần (chuẩn bị dữ liệu cho BinarySearch)
  }

  class SortMatchesUseCase(
      private val sortAlgorithm: SortAlgorithm<Match> = MergeSort()
  ) {
      operator fun invoke(
          matches: List<Match>,
          criteria: MatchSortCriteria = MatchSortCriteria.START_TIME_ASC
      ): List<Match> {
          if (matches.size <= 1) return matches

          val comparator = when (criteria) {
              MatchSortCriteria.START_TIME_ASC -> Comparator<Match> { a, b ->
                  a.startTimeDate.compareTo(b.startTimeDate)
              }
              MatchSortCriteria.START_TIME_DESC -> Comparator<Match> { a, b ->
                  b.startTimeDate.compareTo(a.startTimeDate)
              }
              MatchSortCriteria.TOTAL_GOALS_DESC -> Comparator<Match> { a, b ->
                  b.totalGoals.compareTo(a.totalGoals)
              }
              MatchSortCriteria.GOAL_DIFF_DESC -> Comparator<Match> { a, b ->
                  b.goalDifference.compareTo(a.goalDifference)
              }
              MatchSortCriteria.ID_ASC -> Comparator<Match> { a, b ->
                  a.id.compareTo(b.id)
              }
          }

          return sortAlgorithm.sort(matches, comparator)
      }
  }
  ```
- **Rationale**:
  - `GOAL_DIFF_DESC`: `goalDifference` là số có dấu (`homeScore - awayScore`). Khi sort giảm dần, trận thắng cách biệt (+3) đứng trước trận thắng sít sao (+1), sau đó là hòa (0) và các trận thua (-1, -2). Tuyệt đối không dùng `abs()` để giữ đúng ngữ nghĩa so sánh có dấu.
  - `MergeSort` đảm bảo các trận có cùng giá trị so sánh (ví dụ cùng giờ thi đấu) giữ nguyên thứ tự tương đối ban đầu.
  - Hỗ trợ `ID_ASC` giúp chuẩn bị dữ liệu đầu vào cho `BinarySearch`.

---

#### B. `SortSeasonRankingUseCase`
- **Mục tiêu**: Sắp xếp bảng xếp hạng mùa giải đa tiêu chí thông qua Composite Comparator chuẩn bóng đá.
- **Package**: `dev.anhquocs.truelab.core.domain.team.usecase`
- **Class**: `SortSeasonRankingUseCase`
- **Model**: `StandingsSortCriteria.kt`
- **Dependency**: `SortAlgorithm<SeasonRanking>` (mặc định `MergeSort<SeasonRanking>()`).
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
      ): List<SeasonRanking> {
          if (rankings.size <= 1) return rankings

          val comparator = when (criteria) {
              StandingsSortCriteria.POSITION_ASC -> Comparator<SeasonRanking> { a, b ->
                  a.position.compareTo(b.position)
              }
              StandingsSortCriteria.POINTS_DESC -> Comparator<SeasonRanking> { a, b ->
                  // Composite Tie-breaker: Points DESC -> GoalDiff DESC -> Wins DESC -> Position ASC
                  val ptsComp = b.totalPoints.compareTo(a.totalPoints)
                  if (ptsComp != 0) return@Comparator ptsComp

                  val gdComp = b.goalDiff.compareTo(a.goalDiff)
                  if (gdComp != 0) return@Comparator gdComp

                  val wonComp = b.won.compareTo(a.won)
                  if (wonComp != 0) return@Comparator wonComp

                  a.position.compareTo(b.position)
              }
              StandingsSortCriteria.GOAL_DIFF_DESC -> Comparator<SeasonRanking> { a, b ->
                  b.goalDiff.compareTo(a.goalDiff)
              }
              StandingsSortCriteria.WINS_DESC -> Comparator<SeasonRanking> { a, b ->
                  b.won.compareTo(a.won)
              }
              StandingsSortCriteria.LOSSES_ASC -> Comparator<SeasonRanking> { a, b ->
                  a.loss.compareTo(b.loss)
              }
          }

          return sortAlgorithm.sort(rankings, comparator)
      }
  }
  ```
- **Rationale**:
  - **Composite Comparator** là cơ chế nghiệp vụ duy nhất giải quyết chuỗi tie-breakers: Điểm $\to$ Hiệu số $\to$ Số trận thắng $\to$ Thứ hạng ban đầu.
  - Thuật toán `MergeSort` chỉ can thiệp để bảo toàn tính ổn định (**Stable Ordering**) khi Comparator đánh giá hai phần tử là hoàn toàn tương đương (comparator trả về 0), giúp trật tự tương đối ban đầu giữa chúng không bị xáo trộn ngẫu nhiên.

---

## 5. Phạm vi Triển khai & Ràng buộc Legacy (In Scope vs Out of Scope)

### 5.1. Trong Phạm vi (In Scope):
- Triển khai 4 Pure Computation UseCases độc lập:
  1. `SearchMatchesUseCase` (Phase 1 LinearSearch)
  2. `SearchTeamsUseCase` (Phase 1 LinearSearch & BinarySearch)
  3. `SortMatchesUseCase` (Phase 2 MergeSort)
  4. `SortSeasonRankingUseCase` (Phase 2 MergeSort)
- Định nghĩa 2 Enums tiêu chí nghiệp vụ:
  1. `MatchSortCriteria`
  2. `StandingsSortCriteria`
- Viết bộ Unit Test toàn diện cho từng UseCase với 100% test cases bao phủ kịch bản chuẩn và biên.
- Duy trì trạng thái build sạch và bảo toàn toàn bộ test suite hiện có.

### 5.2. Ngoài Phạm vi (Out of Scope / Để lại D4):
- **QuickSort Scope**: `QuickSort` đã hoàn thành và frozen ở `:core:algorithm` (122/122 tests). Không cần tạo UseCase riêng cho QuickSort trong D3 vì các màn hình nghiệp vụ hiện tại (lịch thi đấu, bảng xếp hạng) đều ưu tiên Stable Ordering của `MergeSort`. Caller vẫn có thể inject `QuickSort` qua interface `SortAlgorithm<T>` nếu cần.
- **Không thay đổi `:core:data`**: Không sửa `MatchDataModule`, `TeamDataModule`, Room DAOs hay Repositories.
- **Không thay đổi UseCases tiền nhiệm**:
  - Giữ nguyên `GetMatchesUseCase` và `GetSeasonRankingUseCase` để không phá vỡ liên kết ViewModel hiện tại.
  - Việc kết nối các màn hình UI và refactor `GetMatchesUseCase` dùng `SearchMatchesUseCase`/`SortMatchesUseCase` sẽ được thực hiện tại **Phase D4 (Presentation Integration & Legacy Cleanup)**.
- **Bảo tồn Tuyệt đối Legacy Code**:
  - `PredictionResult.Companion.computeWeightedScoring`
  - `PredictMatchUseCase`
  - `SeasonRanking.calculateFormScore()`
  - `PredictionRepository.predictMatch`
- **Bảo toàn Tuyệt đối `:core:algorithm`**: Không thêm, sửa, hay đổi bất kỳ ký tự nào trong module `:core:algorithm` (FROZEN 122/122).

---

## 6. Chiến lược Kiểm thử (Testing Strategy)

Mỗi UseCase mới trong D3 sẽ sở hữu bộ Unit Test độc lập trong `core/domain/src/test`:

1. **`SearchMatchesUseCaseTest`**:
   - Khớp trận đấu theo tên đội nhà (Home team match).
   - Khớp trận đấu theo tên đội khách (Away team match).
   - Khớp cả hai đội trong các trận khác nhau.
   - Không phân biệt chữ hoa chữ thường (`ignoreCase = true`).
   - Query rỗng / blank $\to$ trả về danh sách đầy đủ.
   - Query không khớp trận nào $\to$ trả về danh sách rỗng.
   - Danh sách đầu vào rỗng $\to$ trả về danh sách rỗng.
   - Bảo toàn tính bất biến (không mutate danh sách gốc).
   - Ủy thác đúng cho `LinearSearch.searchAllPartial`.

2. **`SearchTeamsUseCaseTest`**:
   - `searchByName`:
     - Khớp substring, không phân biệt hoa thường.
     - Query blank trả về toàn bộ list.
     - Query không tồn tại trả về `emptyList()`.
     - Danh sách rỗng.
   - `findById`:
     - `LinearSearch` (`isSortedById = false`): tìm thấy ở đầu, giữa, cuối danh sách chưa sắp xếp; trả về `null` nếu ID không có.
     - `BinarySearch` (`isSortedById = true`): tìm thấy ở đầu, giữa, cuối danh sách đã sắp xếp theo ID; trả về `null` khi ID ngoài khoảng hoặc không tồn tại.
     - Danh sách rỗng trả về `null`.
     - Xác nhận delegation tương ứng giữa `LinearSearch` và `BinarySearch`.

3. **`SortMatchesUseCaseTest`**:
   - Sắp xếp `START_TIME_ASC`: thời gian tăng dần.
   - Sắp xếp `START_TIME_DESC`: thời gian giảm dần.
   - Sắp xếp `TOTAL_GOALS_DESC`: tổng bàn thắng giảm dần.
   - Sắp xếp `GOAL_DIFF_DESC`: hiệu số bàn thắng có dấu giảm dần (+3 > +1 > 0 > -2), không dùng `abs()`.
   - Sắp xếp `ID_ASC`: ID tăng dần.
   - Kiểm tra tính ổn định (Stable) của `MergeSort`: Các trận có cùng giá trị so sánh (equivalent) giữ nguyên thứ tự tương đối ban đầu.
   - Danh sách rỗng, 1 phần tử, nhiều phần tử có giá trị bằng nhau.
   - Bảo toàn tính bất biến.
   - Ủy thác đúng cho `SortAlgorithm.sort`.

4. **`SortSeasonRankingUseCaseTest`**:
   - Sắp xếp `POSITION_ASC`: thứ hạng tăng dần (1, 2, 3...).
   - Sắp xếp `POINTS_DESC`: kiểm tra Composite Comparator xử lý đầy đủ chuỗi tie-breakers (Điểm $\to$ Hiệu số $\to$ Số trận thắng $\to$ Position).
   - Sắp xếp `GOAL_DIFF_DESC`: hiệu số giảm dần.
   - Sắp xếp `WINS_DESC`: số trận thắng giảm dần.
   - Sắp xếp `LOSSES_ASC`: số trận thua tăng dần (thua ít nhất lên đầu).
   - Kiểm tra tính ổn định (Stable) của `MergeSort`: Giữ nguyên thứ tự ban đầu khi hai phần tử hoàn toàn tương đương (comparator trả về 0).
   - Danh sách rỗng, 1 phần tử.
   - Bảo toàn tính bất biến.
   - Ủy thác đúng cho `SortAlgorithm.sort`.

5. **Yêu cầu Hồi quy (Regression Requirements)**:
   - `:core:algorithm:test`: **122 / 122 PASS (100%)**.
   - `:core:domain:test`: **$\ge 94$ PASS (100%)** (94 tests hiện tại + toàn bộ tests mới của D3).

---

## 7. Danh mục File Triển khai (File Plan)

### 🔹 Files Mới trong `:core:domain/src/main`:
1. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SearchMatchesUseCase.kt`
2. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/MatchSortCriteria.kt`
3. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SortMatchesUseCase.kt`
4. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SearchTeamsUseCase.kt`
5. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/StandingsSortCriteria.kt`
6. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SortSeasonRankingUseCase.kt`

### 🔹 Files Mới trong `:core:domain/src/test`:
7. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SearchMatchesUseCaseTest.kt`
8. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/match/usecase/SortMatchesUseCaseTest.kt`
9. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SearchTeamsUseCaseTest.kt`
10. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/SortSeasonRankingUseCaseTest.kt`

### 🔹 File Tài liệu:
11. `docs/plans/domain-d3-search-sort-plan.md` (Kế hoạch này)
12. `docs/reports/domain-d3-search-sort.md` (Báo cáo tổng kết sau khi hoàn thành)

### ⛔ Files Tuyệt đối KHÔNG SỬA:
- Toàn bộ module `:core:algorithm` (FROZEN 122/122).
- Các Repository interfaces trong `:core:domain`.
- Module `:core:data` và module `:app`.
- Các file UseCase cũ (`GetMatchesUseCase`, `GetSeasonRankingUseCase`, `PredictMatchOutcomeUseCase`, v.v.).

---

## 8. Thứ tự Triển khai Từng Bước Đề xuất (Execution Order)

Để đảm bảo chất lượng và khả năng review độc lập (tương tự quy trình thành công của D2):

1. **Bước 1 — D3.1 Search Integration**:
   - Triển khai `SearchMatchesUseCase` + `SearchMatchesUseCaseTest`.
   - Triển khai `SearchTeamsUseCase` + `SearchTeamsUseCaseTest`.
   - Review & Commit: `feat(domain): implement D3.1 SearchMatchesUseCase and SearchTeamsUseCase`.
2. **Bước 2 — D3.2 Sort Integration**:
   - Triển khai `MatchSortCriteria` + `SortMatchesUseCase` + `SortMatchesUseCaseTest`.
   - Triển khai `StandingsSortCriteria` + `SortSeasonRankingUseCase` + `SortSeasonRankingUseCaseTest`.
   - Review & Commit: `feat(domain): implement D3.2 SortMatchesUseCase and SortSeasonRankingUseCase`.
3. **Bước 3 — Toàn diện Kiểm thử & Báo cáo**:
   - Xác thực hồi quy: `./gradlew :core:domain:test :core:algorithm:test`.
   - Tạo báo cáo tổng kết: `docs/reports/domain-d3-search-sort.md`.
   - Review & Commit: `docs(domain): add D3 search and sort implementation report`.

---

## 9. Tiêu chí Hoàn thành (Definition of Done)

- [ ] Cả 4 UseCases (`SearchMatchesUseCase`, `SearchTeamsUseCase`, `SortMatchesUseCase`, `SortSeasonRankingUseCase`) hoạt động độc lập và thuần túy trong `:core:domain`.
- [ ] 100% Unit Tests mới viết cho 4 UseCases đều PASS.
- [ ] Toàn bộ 122/122 Algorithm Tests tiếp tục PASS (FROZEN được bảo toàn).
- [ ] Toàn bộ Domain Tests (94 tests cũ + các tests mới) đều PASS 100%.
- [ ] Không có breaking change trong `:core:data`, `:core:ui`, hay `:app`.
- [ ] Tài liệu `docs/reports/domain-d3-search-sort.md` ghi nhận đầy đủ quá trình triển khai.

---

## 10. Rủi ro & Điểm Kỹ thuật Cần Nhớ (Risks & Technical Notes)

1. **Rủi ro Precondition của BinarySearch**:
   - `BinarySearch` sẽ trả về kết quả sai hoặc `-1` nếu mảng chưa được sắp xếp theo đúng khóa tìm kiếm.
   - *Biện pháp kiểm soát*: `SearchTeamsUseCase.findById` yêu cầu cờ tường minh `isSortedById: Boolean = false`. Mặc định an toàn sử dụng `LinearSearch` $O(n)$, chỉ khi caller khẳng định `isSortedById = true` thì mới kích hoạt `BinarySearch` $O(\log n)$.
2. **Định dạng `startTimeDate` trong Match**:
   - Trong `Match.kt`, `startTimeDate` có kiểu `String` (ví dụ "2026-09-23 19:00" hoặc ISO-8601).
   - So sánh chuỗi ngày giờ theo định dạng chuẩn này tương thích trực tiếp với thứ tự thời gian tự nhiên qua `String.compareTo`.
3. **Phân định rõ Ranh giới giữa Composite Comparator và Tính Stable**:
   - *Composite Comparator*: Là cơ chế nghiệp vụ xử lý thứ bậc các tiêu chí phụ (tie-breakers: Điểm $\to$ Hiệu số $\to$ Số trận thắng $\to$ Thứ hạng).
   - *Tính Stable của MergeSort*: Đảm bảo khi Comparator đánh giá hai phần tử là tương đương (`compare == 0`), thứ tự tương đối xuất hiện ban đầu giữa chúng được giữ nguyên 100%, bảo đảm tính deterministic tuyệt đối.
