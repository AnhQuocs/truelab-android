# TrueLab – Báo cáo Hoàn thành Triển khai Domain D2 (Domain D2 Analytics, Form & Rating Integration Report)

Tài liệu này tổng kết toàn diện quá trình triển khai giai đoạn **Domain D2 (Analytics, Form & Rating Integration)** trong module `:core:domain` của TrueLab, đưa 4 thuật toán cốt lõi đã đóng băng (**FROZEN**) từ `:core:algorithm` ra phục vụ độc lập thông qua các Pure Computation Domain UseCases.

---

## 1. Mục tiêu Giai đoạn D2 (D2 Objectives)

1. **Độc lập hóa Thuật toán**:
   - Expose 4 thuật toán nền tảng từ `:core:algorithm` thành các UseCase tính toán nghiệp vụ độc lập trong `:core:domain`:
     - **Phase 5 (Form Evaluation)**: Đánh giá phong độ suy giảm thời gian (Linear Time-Decay) $\to$ `CalculateTeamFormUseCase`.
     - **Phase 6 (Elo Rating)**: Tính toán điểm kỳ vọng đối đầu và cập nhật biến thiên điểm Elo $\to$ `CalculateEloRatingUseCase`.
     - **Phase 4 (Trend Smoothing)** & **Phase 3 (Volatility)**: Làm mịn chuỗi thời gian tỷ lệ cược (SMA) và đo lường độ biến động kèo $\to$ `AnalyzeOddsTrendUseCase`.
     - **Phase 3 (Descriptive Statistics)**: Tổng hợp toàn diện 8 chỉ số thống kê phân phối bàn thắng $\to$ `GetTeamStatisticsUseCase`.
2. **Chuẩn hóa Kiến trúc Clean Architecture**:
   - Đảm bảo toàn bộ UseCases D2 là **Pure Computation** (không chứa side-effects, không phụ thuộc Repository, không import Room/DAO/Android SDK).
   - Làm cầu nối nghiệp vụ giữa Domain Models (`Match`, `OddsRecordItem`) và Data Structures của `:core:algorithm`.
3. **Bảo toàn Tuyệt đối Thuật toán & Code Hiện hữu**:
   - Module `:core:algorithm` giữ trạng thái **FROZEN** (122/122 test cases pass 100%).
   - Giữ nguyên các hàm legacy và pipeline dự đoán (`PredictMatchOutcomeUseCase`, `PredictionResult`, `SeasonRanking`).

---

## 2. Ranh giới Kiến trúc & Phân định Trách nhiệm (Architecture Boundaries)

```text
Presentation Layer (ViewModels: TeamDetail, Standings, Analytics, OddsTrend)
                     │
                     ▼
    ┌────────────────────────────────────────────────────────┐
    │                  :core:domain (D2 UseCases)            │
    │  • CalculateTeamFormUseCase    (Phase 5 Form Score)    │
    │  • CalculateEloRatingUseCase   (Phase 6 Elo Rating)    │
    │  • AnalyzeOddsTrendUseCase     (Phase 4 SMA & Volatility)
    │  • GetTeamStatisticsUseCase    (Phase 3 Statistics)    │
    └──────────────────┬──────────────────┬──────────────────┘
                       │                  │
        (Trích xuất dữ liệu)     (Tính toán thuật toán thuần)
                       │                  │
                       ▼                  ▼
             [Repository Interfaces]   [:core:algorithm]
                       │               (Pure Kotlin/JVM - FROZEN)
                       ▼
                  :core:data
             (Room Database / API)
```

### Phân định Ranh giới:
1. **`:core:algorithm` (Pure Math/JVM)**: Hoàn toàn bất biến, không biết bất kỳ khái niệm bóng đá (`Match`, `Team`, `Odds`) hay Android SDK nào.
2. **`:core:domain` (Business Logic & Pure Computation)**:
   - Tiếp nhận các thực thể Domain, thực hiện xác thực (validation), lọc dữ liệu biên (anti data leakage), cách ly đội bóng, sắp xếp thứ tự thời gian.
   - Ánh xạ sang các kiểu dữ liệu thuật toán thuần túy (`List<MatchOutcome>`, `List<Double>`, v.v.).
   - Gọi trực tiếp Interface của `:core:algorithm` và đóng gói kết quả thành Domain Models giàu ngữ nghĩa.
   - **Tuyệt đối không gây side-effect ghi DB**.
3. **`:core:data` (Data Sync & Persistence)**:
   - Các tác vụ lưu trữ (ví dụ: cập nhật `TeamEntity.eloRating` mới sau trận đấu, cache kết quả thống kê) thuộc về Pipeline đồng bộ dữ liệu (`Data Sync Engine`), hoàn toàn độc lập với các UseCase tính toán thuần túy của D2.
4. **Presentation Layer**:
   - Inject trực tiếp các UseCase này vào ViewModel để tính toán dữ liệu tức thời theo luồng Unidirectional Data Flow (UDF).

---

## 3. Chi tiết Triển khai 4 UseCases D2

---

### 3.1. D2.1 — `CalculateTeamFormUseCase` (Phase 5: Form Score)

- **File nguồn**: [`CalculateTeamFormUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateTeamFormUseCase.kt)
- **File kiểm thử**: [`CalculateTeamFormUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateTeamFormUseCaseTest.kt) (13 tests)
- **Algorithm Dependency**: [`FormEvaluator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormEvaluator.kt) (mặc định [`LinearDecayFormEvaluator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/LinearDecayFormEvaluator.kt)).
- **Contract**:
  ```kotlin
  class CalculateTeamFormUseCase(
      private val formEvaluator: FormEvaluator = LinearDecayFormEvaluator()
  ) {
      operator fun invoke(
          teamId: Int,
          matches: List<Match>,
          windowSize: Int = 5,
          currentMatchId: Long? = null
      ): FormScore
  }
  ```
- **Nghiệp vụ & Xử lý Dữ liệu**:
  1. **Lọc dữ liệu hợp lệ**: Lọc các trận `match.isEnded && match.homeScore != null && match.awayScore != null`.
  2. **Chống rò rỉ dữ liệu (Anti Data Leakage)**: Loại trừ trận hiện tại `match.id != currentMatchId`.
  3. **Cách ly đội bóng**: Bắt buộc `match.homeTeam.id == teamId || match.awayTeam.id == teamId`.
  4. **Sắp xếp thời gian**: Tự động sắp xếp tăng dần theo thời gian (`startTimeDate ASC`), đảm bảo trận mới nhất nằm ở cuối danh sách tương thích với cơ chế trọng số tuyến tính ($w_i = i$).
  5. **Ánh xạ kết quả**: Ánh xạ sang `MatchOutcome.WIN`, `DRAW`, `LOSS` theo góc nhìn của `teamId`.
  6. **Xử lý biên**: Nếu danh sách rỗng $\implies$ gọi `formEvaluator.evaluate(emptyList(), windowSize)` trả về `FormScore` chuẩn (score = 0.0). Nếu $N < windowSize \implies$ tính trên số trận thực tế $N$.
- **Commit**: [`5d54e0c`](https://github.com/AnhQuocs/truelab-android/commit/5d54e0c).

---

### 3.2. D2.2 — `CalculateEloRatingUseCase` (Phase 6: Elo Rating)

- **File nguồn**: [`CalculateEloRatingUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateEloRatingUseCase.kt)
- **File kiểm thử**: [`CalculateEloRatingUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateEloRatingUseCaseTest.kt) (13 tests)
- **Algorithm Dependency**: [`RatingCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/rating/RatingCalculator.kt) (mặc định [`EloRatingCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/rating/EloRatingCalculator.kt)).
- **Contract**:
  ```kotlin
  class CalculateEloRatingUseCase(
      private val ratingCalculator: RatingCalculator = EloRatingCalculator()
  ) {
      fun calculateExpectedScore(ratingA: Double, ratingB: Double): Double
      fun calculateMatchResult(
          ratingHome: Double,
          ratingAway: Double,
          homeScore: Int,
          awayScore: Int,
          kFactor: Double = RatingCalculator.DEFAULT_K
      ): RatingMatchResult
  }
  ```
- **Nghiệp vụ & Xử lý Dữ liệu**:
  1. `calculateExpectedScore`: Ủy thác trực tiếp cho `ratingCalculator.expectedScore(ratingA, ratingB)`.
  2. `calculateMatchResult`: Ánh xạ kết quả thực tế dựa trên tỷ số bàn thắng:
     - `homeScore > awayScore` $\implies actualScoreHome = 1.0$ (Home Win).
     - `homeScore == awayScore` $\implies actualScoreHome = 0.5$ (Draw).
     - `homeScore < awayScore` $\implies actualScoreHome = 0.0$ (Away Win).
  3. Ủy thác cho `ratingCalculator.calculateMatch(ratingHome, ratingAway, actualScoreHome, kFactor)` trả về [`RatingMatchResult`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/rating/RatingMatchResult.kt) bảo toàn tính chất Zero-Sum ($\Delta R_H = -\Delta R_A$).
- **Commit**: [`d1f7386`](https://github.com/AnhQuocs/truelab-android/commit/d1f7386).

---

### 3.3. D2.3 — `AnalyzeOddsTrendUseCase` (Phase 4: SMA Trend & Phase 3: Volatility)

- **File models**: [`OddsTrendAnalysis.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/model/OddsTrendAnalysis.kt)
- **File nguồn**: [`AnalyzeOddsTrendUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/usecase/AnalyzeOddsTrendUseCase.kt)
- **File kiểm thử**: [`AnalyzeOddsTrendUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/odds/usecase/AnalyzeOddsTrendUseCaseTest.kt) (16 tests)
- **Algorithm Dependencies**:
  - [`MovingAverageCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/trend/MovingAverageCalculator.kt) (mặc định [`SimpleMovingAverageCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/trend/SimpleMovingAverageCalculator.kt))
  - [`StatisticsCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/StatisticsCalculator.kt) (mặc định [`DescriptiveStatisticsCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/DescriptiveStatisticsCalculator.kt))
- **Contract**:
  ```kotlin
  enum class TargetOddsField { HOME_WIN, DRAW, AWAY_WIN, OVER, UNDER, HANDICAP }

  data class OddsTrendAnalysis(
      val matchId: Long,
      val targetField: TargetOddsField,
      val windowSize: Int,
      val timestamps: List<Long>,
      val rawOddsSeries: List<Double>,
      val smaSeries: List<Double>,
      val currentOdds: Double?,
      val openingOdds: Double?,
      val volatility: Double,
      val hasSufficientData: Boolean
  )

  class AnalyzeOddsTrendUseCase(
      private val movingAverageCalculator: MovingAverageCalculator = SimpleMovingAverageCalculator(),
      private val statisticsCalculator: StatisticsCalculator = DescriptiveStatisticsCalculator()
  ) {
      operator fun invoke(
          matchId: Long,
          oddsHistory: List<OddsRecordItem>,
          targetField: TargetOddsField = TargetOddsField.HOME_WIN,
          windowSize: Int = 3
      ): OddsTrendAnalysis
  }
  ```
- **Nghiệp vụ & Tinh chỉnh So với Plan**:
  1. **Sắp xếp thời gian**: Sắp xếp lịch sử tỷ lệ cược theo `changeTime ASC`.
  2. **Ghép cặp 1-1 an toàn**: Lọc và ánh xạ trực tiếp thành `Pair(changeTime, value)` với điều kiện `value != null && value.isFinite() && value > 0.0`. Tinh chỉnh này giúp `timestamps` và `rawOddsSeries` luôn có độ dài bằng nhau tuyệt đối, không xảy ra mốc thời gian mồ côi nếu giá trị kèo bị null/lỗi.
  3. **Tính toán SMA**: Khi $N \ge windowSize$, gọi `movingAverageCalculator.calculate(rawSeries, windowSize)`; ngược lại trả về `emptyList()`.
  4. **Tính toán Volatility**: Khi $N \ge 2$, gọi `statisticsCalculator.sampleStandardDeviation(rawSeries)`; ngược lại gán `0.0`. Khử các giá trị `NaN` và non-finite an toàn về `0.0`.
  5. **Tính nhất quán tên gọi**: Đặt tên test class và file test là `AnalyzeOddsTrendUseCaseTest.kt` đồng nhất tuyệt đối với UseCase (thay vì nhầm lẫn thành Calculate... như dự kiến ban đầu).
- **Commit**: [`a88fd0f`](https://github.com/AnhQuocs/truelab-android/commit/a88fd0f).

---

### 3.4. D2.4 — `GetTeamStatisticsUseCase` (Phase 3: Statistics)

- **File models**: [`TeamPerformanceStatistics.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/TeamPerformanceStatistics.kt)
- **File nguồn**: [`GetTeamStatisticsUseCase.kt`](../../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/GetTeamStatisticsUseCase.kt)
- **File kiểm thử**: [`GetTeamStatisticsUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/GetTeamStatisticsUseCaseTest.kt) (17 tests)
- **Algorithm Dependency**: [`StatisticsCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/StatisticsCalculator.kt) (mặc định [`DescriptiveStatisticsCalculator`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/DescriptiveStatisticsCalculator.kt)).
- **Contract**:
  ```kotlin
  data class TeamPerformanceStatistics(
      val teamId: Int,
      val matchesCount: Int,
      val goalsScoredStats: DescriptiveStatistics,
      val goalsConcededStats: DescriptiveStatistics,
      val totalGoalsStats: DescriptiveStatistics,
      val goalDiffStats: DescriptiveStatistics
  )

  class GetTeamStatisticsUseCase(
      private val statisticsCalculator: StatisticsCalculator = DescriptiveStatisticsCalculator()
  ) {
      operator fun invoke(
          teamId: Int,
          matches: List<Match>,
          currentMatchId: Long? = null
      ): TeamPerformanceStatistics
  }
  ```
- **Nghiệp vụ & Cải tiến So với Sketch Ban đầu**:
  1. **Cách ly đội bóng chặt chẽ**: Bổ sung điều kiện bắt buộc `(match.homeTeam.id == teamId || match.awayTeam.id == teamId)` trong bộ lọc (điểm mà sketch sơ bộ trong plan vô tình bỏ sót). Nhờ đó, các trận giữa hai đội bóng khác không bị lọt vào thống kê của đội bóng mục tiêu.
  2. **Lọc trận đấu**: Chỉ nhận các trận `isEnded == true`, `homeScore != null`, `awayScore != null`, và `id != currentMatchId`.
  3. **Chuẩn hóa Sân nhà/Sân khách**:
     - `goalsScored`: Nếu là chủ nhà $\implies homeScore$; nếu là đội khách $\implies awayScore$.
     - `goalsConceded`: Nếu là chủ nhà $\implies awayScore$; nếu là đội khách $\implies homeScore$.
     - `totalGoals`: $homeScore + awayScore$.
     - `goalDiff`: $goalsScored - goalsConceded$.
  4. **Ủy thác Toàn diện cho Phase 3**: Không tự tính toán bất kỳ công thức thống kê nào (Mean, Median, StdDev, Variance, Skewness). Ủy thác 100% cho `statisticsCalculator.summarize(...)`.
  5. **Bảo toàn Contract Danh sách Rỗng**: Khi không có trận đấu hợp lệ $\implies$ gọi `statisticsCalculator.summarize(emptyList())`, trả về `matchesCount = 0` và các chỉ số thống kê mang giá trị `Double.NaN` theo đúng hợp đồng của Phase 3, không tự ý gán số 0 tùy tiện.
- **Commit**: [`57ec936`](https://github.com/AnhQuocs/truelab-android/commit/57ec936).

---

## 4. Báo cáo Độ phủ Kiểm thử Thực tế (Actual Test Coverage)

Toàn bộ 4 UseCases trong D2 đều sở hữu bộ Unit Test độc lập, kiểm thử từ các kịch bản chuẩn đến các kịch bản biên khắc nghiệt nhất:

| STT | Test Suite Class | File | Số Tests | Trạng thái | Các khía cạnh bao phủ chính |
|:---:|---|---|:---:|:---:|---|
| **1** | `CalculateTeamFormUseCaseTest` | [`CalculateTeamFormUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateTeamFormUseCaseTest.kt) | 13 | **PASS 100%** | Happy path (W-W-D-L-W), tự động đảo ngược thứ tự thời gian (`startTimeDate DESC`), xử lý $N < windowSize$, danh sách rỗng, lọc trận chưa đấu (`isEnded = false`), lọc tỷ số null, loại trừ `currentMatchId`, cách ly đội bóng khác, xác thực `windowSize <= 0`, bảo toàn tính bất biến (immutability), ủy thác delegation. |
| **2** | `CalculateEloRatingUseCaseTest` | [`CalculateEloRatingUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateEloRatingUseCaseTest.kt) | 13 | **PASS 100%** | Xác suất kỳ vọng đối xứng ($E_H = 0.5$ khi Elo bằng nhau), kỳ vọng không đối xứng, Home win ($S_H = 1.0$), Away win ($S_H = 0.0$), Draw ($S_H = 0.5$), bảo toàn Zero-Sum, tùy chỉnh K-Factor, Elo phân cực cao, ủy thác delegation, tính toán độc lập không side-effect. |
| **3** | `AnalyzeOddsTrendUseCaseTest` | [`AnalyzeOddsTrendUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/odds/usecase/AnalyzeOddsTrendUseCaseTest.kt) | 16 | **PASS 100%** | Happy path SMA ($k=3$), kiểm tra đủ 6 trường `TargetOddsField`, tự động sắp xếp theo `changeTime`, lọc bỏ odds null/âm/0.0/NaN, xử lý $N < windowSize$ (`smaSeries = emptyList()`, `hasSufficientData = false`), chuỗi rỗng an toàn, tính Volatility (Sample StdDev) với $N \ge 2$ và $N < 2$, xác thực `windowSize <= 0`, bảo toàn immutability. |
| **4** | `GetTeamStatisticsUseCaseTest` | [`GetTeamStatisticsUseCaseTest.kt`](../../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/GetTeamStatisticsUseCaseTest.kt) | 17 | **PASS 100%** | Happy path phân phối bàn thắng, kiểm tra riêng đội chủ nhà (Home), đội khách (Away), lịch sử kết hợp (Mixed Home/Away), xác minh 4 phân phối số liệu (`goalsScored`, `goalsConceded`, `totalGoals`, `goalDiff`), lọc trận chưa đấu, lọc tỷ số null, loại trừ `currentMatchId`, cách ly đội bóng không liên quan, danh sách rỗng, không có trận hợp lệ, ủy thác delegation, bảo toàn immutability. |
| **Tổng** | **4 Suites mới của D2** | | **59** | **PASS 100%** | |

---

## 5. Trạng thái Kiểm thử Hồi quy Toàn diện (Full Regression Status)

```text
================================================================================
                    TRUELAB GRADLE TEST EXECUTION SUMMARY
================================================================================
  Module :core:algorithm:test  ===>  122 / 122 PASSED  (100% - FROZEN)
  Module :core:domain:test     ===>   94 /  94 PASSED  (100%)
    - Prediction Suite (Pre-existing) : 35 tests
    - Analytics Suite D2 (New)        : 59 tests
  Build Output                 ===>  BUILD SUCCESSFUL (8 actionable tasks executed)
================================================================================
```

- **`:core:algorithm:test`**: **122 / 122 tests PASS (100%)** — Toàn bộ 7 Phase của Module Thuật toán cốt lõi tiếp tục được bảo toàn nguyên vẹn, 0 sửa đổi, 0 vi phạm ranh giới.
- **`:core:domain:test`**: **94 / 94 tests PASS (100%)** — Toàn bộ 35 tests tiền nhiệm của hệ thống dự đoán (Weighted Scoring, 6 Signal Transformers, Prediction Weight Config) cùng 59 tests mới của D2 đều vượt qua kiểm thử hoàn hảo.
- **Biên dịch & Build**: Không có lỗi cú pháp, không có warning rò rỉ bộ nhớ, build sạch hoàn toàn.

---

## 6. Bảo tồn Ràng buộc Legacy (Legacy & Compatibility Guardrails)

Trong suốt quá trình triển khai D2, các thành phần legacy và pipeline hiện có được giữ nguyên vẹn 100%, không phát sinh bất kỳ breaking change nào:

1. **`PredictionResult.Companion.computeWeightedScoring`**: Giữ nguyên vẹn để phục vụ các màn hình UI cũ đang tham chiếu.
2. **`PredictMatchUseCase`**: Giữ nguyên contract và pipeline đọc từ Room DB/Repository.
3. **`SeasonRanking.calculateFormScore()`**: Giữ nguyên vẹn, không gây ảnh hưởng đến logic xếp hạng hiện hữu.
4. **`PredictionRepository.predictMatch`**: Giữ nguyên vẹn interface và repository implementation.
5. **Ranh giới Phụ thuộc**: Tuyệt đối không thay đổi mã nguồn trong `:core:data`, `:core:ui`, hay `:app`.

---

## 7. Danh mục Files & Commits Đã Triển khai (Manifest)

### 7.1. Danh mục Files Mới Triển khai:

```text
core/domain/
├── src/main/kotlin/dev/anhquocs/truelab/core/domain/
│   ├── odds/
│   │   ├── model/
│   │   │   └── OddsTrendAnalysis.kt                      [D2.3 Model]
│   │   └── usecase/
│   │       └── AnalyzeOddsTrendUseCase.kt                [D2.3 UseCase]
│   └── team/
│       ├── model/
│       │   └── TeamPerformanceStatistics.kt              [D2.4 Model]
│       └── usecase/
│           ├── CalculateTeamFormUseCase.kt               [D2.1 UseCase]
│           ├── CalculateEloRatingUseCase.kt              [D2.2 UseCase]
│           └── GetTeamStatisticsUseCase.kt               [D2.4 UseCase]
└── src/test/kotlin/dev/anhquocs/truelab/core/domain/
    ├── odds/usecase/
    │   └── AnalyzeOddsTrendUseCaseTest.kt                [D2.3 Test - 16 tests]
    └── team/usecase/
        ├── CalculateTeamFormUseCaseTest.kt               [D2.1 Test - 13 tests]
        ├── CalculateEloRatingUseCaseTest.kt              [D2.2 Test - 13 tests]
        └── GetTeamStatisticsUseCaseTest.kt               [D2.4 Test - 17 tests]
```

### 7.2. Lịch sử Commits Từng Bước (Granular Commits):

| Sub-phase | Commit Hash | Conventional Commit Message | Tóm tắt Nội dung |
|:---:|:---:|---|---|
| **D2.1** | [`5d54e0c`](https://github.com/AnhQuocs/truelab-android/commit/5d54e0c) | `feat(domain): implement D2.1 CalculateTeamFormUseCase with Phase 5 form evaluation` | Triển khai UseCase đánh giá phong độ thời gian suy giảm, mapper `MatchOutcome`, 13 tests, kèm tài liệu thiết kế D2 Plan. |
| **D2.2** | [`d1f7386`](https://github.com/AnhQuocs/truelab-android/commit/d1f7386) | `feat(domain): implement D2.2 CalculateEloRatingUseCase` | Triển khai UseCase tính toán xác suất kỳ vọng Elo và cập nhật điểm đối kháng Zero-Sum, 13 tests. |
| **D2.3** | [`a88fd0f`](https://github.com/AnhQuocs/truelab-android/commit/a88fd0f) | `feat(domain): implement D2.3 AnalyzeOddsTrendUseCase` | Triển khai UseCase làm mịn tỷ lệ cược SMA và độ biến động Volatility, model `OddsTrendAnalysis`, 16 tests. |
| **D2.4** | [`57ec936`](https://github.com/AnhQuocs/truelab-android/commit/57ec936) | `feat(domain): implement D2.4 GetTeamStatisticsUseCase` | Triển khai UseCase tổng hợp thống kê mô tả 8 chỉ số phân phối bàn thắng, model `TeamPerformanceStatistics`, 17 tests. |

---

## 8. Bảng Đối chiếu Tiêu chí Hoàn thành (Definition of Done Checklist)

- [x] **Đầy đủ 4 UseCases**: Cả 4 UseCases (`CalculateTeamFormUseCase`, `CalculateEloRatingUseCase`, `AnalyzeOddsTrendUseCase`, `GetTeamStatisticsUseCase`) hoạt động độc lập và chuẩn mực trong `:core:domain`.
- [x] **100% Tests Mới Vượt qua**: 59/59 Unit Tests mới được viết cho 4 UseCases đều PASS 100%.
- [x] **Bảo toàn Thuật toán**: 122/122 Algorithm Tests tiếp tục PASS (FROZEN không bị xâm phạm).
- [x] **Không Hồi quy Domain**: 94/94 Domain Tests (35 tests cũ + 59 tests mới) đều PASS 100%.
- [x] **Không Breaking Changes**: Không làm gãy `:core:data`, `:core:ui`, hay `:app`. Các hàm legacy tiếp tục tồn tại an toàn.
- [x] **Báo cáo Hoàn tất**: Báo cáo `docs/reports/final/domain-d2-final.md` ghi nhận đầy đủ, chuẩn xác theo mã nguồn thực tế trên đĩa.

---

## 9. Kết luận & Trạng thái Cuối cùng (Final D2 Status)

> **TRẠNG THÁI GIAI ĐOẠN D2**: **HOÀN THÀNH TOÀN DIỆN (APPROVED & INTEGRATED)**
>
> Toàn bộ 4 thuật toán cốt lõi từ `:core:algorithm` đã được đưa ra tầng `:core:domain` thành công, phục vụ trực tiếp cho các màn hình phân tích, xếp hạng và thống kê chuyên sâu của TrueLab. Hệ thống sẵn sàng cho việc tích hợp hiển thị tại Presentation Layer hoặc mở rộng Data Sync Pipeline khi có yêu cầu tiếp theo.
