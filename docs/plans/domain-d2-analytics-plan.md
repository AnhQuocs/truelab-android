# TrueLab – Kế hoạch Triển khai Domain D2 (Domain D2 Implementation Plan)

Tài liệu này xác lập bản thiết kế kiến trúc và kế hoạch triển khai chi tiết cho giai đoạn **Domain D2 (Analytics, Form & Rating Integration)** trong module `:core:domain` của TrueLab, nhằm đưa 4 thuật toán cốt lõi đã đóng băng (**FROZEN**) từ `:core:algorithm` ra phục vụ độc lập thông qua các Domain UseCases chuẩn mực.

---

## 1. Hiện trạng Kiến trúc & Bối cảnh (Current State)

### 1.1. Những gì Domain đã hoàn thành:
- **Phase 7 (Prediction - Weighted Scoring)**: Đã tích hợp hoàn chỉnh thông qua `PredictMatchOutcomeUseCase`, 6 Signal Transformers và `PredictionWeightConfig` (commit `f4aaad5`).
- **Phase 5 & Phase 6**: Đang được sử dụng gián tiếp bên trong `PredictMatchOutcomeUseCase` để tạo đặc trưng dự đoán (Form 5 trận gần nhất và tính điểm kỳ vọng $E_H$).

### 1.2. Khoảng trống Kiến trúc cần giải quyết trong D2:
- **Thiếu UseCase độc lập**:
  - Phase 5 (`LinearDecayFormEvaluator`): Chưa có UseCase tính phong độ độc lập để phục vụ màn hình `TeamDetailScreen` và `StandingsScreen`.
  - Phase 6 (`EloRatingCalculator`): Chưa có UseCase tính điểm kỳ vọng và biến thiên Elo đối đầu độc lập.
  - Phase 4 (`SimpleMovingAverageCalculator`): Chưa có UseCase làm mịn chuỗi odds để phát hiện xu hướng biến động kèo phục vụ `OddsTrendCard` / màn hình `Analytics`.
  - Phase 3 (`DescriptiveStatisticsCalculator`): Chưa có UseCase thống kê phân phối bàn thắng/bàn thua (Mean, Median, StdDev, Skewness) cho từng đội bóng.
- **Logic Trùng lặp & Legacy**:
  - `SeasonRanking.calculateFormScore()` đang tính điểm phong độ thô sơ trên chuỗi ký tự `W/D/L` không có time-decay.
  - `GetMatchesUseCase`, `GetSeasonRankingUseCase`, `GetOddsHistoryUseCase` đang dùng Kotlin stdlib (`sortedBy`, `average`) thay vì khai thác `:core:algorithm`.

---

## 2. Kiến trúc Domain D2 (D2 Architecture)

```text
Presentation Layer (ViewModels: TeamDetail, Standings, Analytics, OddsTrend)
                     │
                     ▼
    ┌────────────────────────────────────────────────────────┐
    │                  :core:domain (D2 UseCases)            │
    │  • CalculateTeamFormUseCase    (Phase 5 Form Score)    │
    │  • CalculateEloRatingUseCase   (Phase 6 Elo Rating)    │
    │  • AnalyzeOddsTrendUseCase     (Phase 4 SMA Trend)     │
    │  • GetTeamStatisticsUseCase    (Phase 3 Statistics)    │
    └──────────────────┬──────────────────┬──────────────────┘
                       │                  │
        (Trích xuất dữ liệu)     (Tính toán thuật toán thuần)
                       │                  │
                       ▼                  ▼
             [Repository Interfaces]   [:core:algorithm]
                       │               (Pure Kotlin / JVM)
                       ▼
                  :core:data
             (Room Database / API)
```

### Ranh giới Kiến trúc (Architecture Boundary):
1. **`:core:algorithm`**: Pure Kotlin/JVM, bất biến (**FROZEN 122/122 tests**), không biết bất kỳ khái niệm bóng đá hay Android nào.
2. **`:core:domain`**: Đóng vai trò cầu nối nghiệp vụ (Business Orchestration), trích xuất và sắp xếp dữ liệu bóng đá, gọi `:core:algorithm`, và trả về các Domain Models/Entities giàu ngữ nghĩa.
3. **`:core:data`**: Cung cấp dữ liệu qua Repository Interfaces, phụ trách caching và persistence.

---

## 3. Thiết kế Chi tiết 4 Domain UseCases (D2 Specifications)

---

### 3.1. D2.1 — `CalculateTeamFormUseCase` (Phase 5: Form Score)

#### a. Mục tiêu:
Expose Phase 5 `FormEvaluator` (`LinearDecayFormEvaluator`) thành UseCase tính toán thuần túy độc lập để đánh giá phong độ thi đấu chuẩn hóa có trọng số suy giảm thời gian (Time-Decay) cho một đội bóng.

#### b. File / Class Dự kiến:
- Package: `dev.anhquocs.truelab.core.domain.team.usecase`
- Class: `CalculateTeamFormUseCase`

#### c. Contract & Signature:
```kotlin
class CalculateTeamFormUseCase(
    private val formEvaluator: FormEvaluator = LinearDecayFormEvaluator()
) {
    /**
     * Tính toán phong độ thi đấu từ danh sách trận đấu của một đội bóng.
     *
     * @param teamId ID của đội bóng cần tính phong độ.
     * @param matches Danh sách trận đấu lịch sử.
     * @param windowSize Số trận đánh giá trong cửa sổ (mặc định 5).
     * @param currentMatchId ID trận đấu hiện tại cần loại trừ (chống Data Leakage nếu gọi trong ngữ cảnh match).
     * @return [FormScore] chứa điểm phong độ có time-decay [0.0, 100.0], rawScore, W-D-L counts.
     */
    operator fun invoke(
        teamId: Int,
        matches: List<Match>,
        windowSize: Int = 5,
        currentMatchId: Long? = null
    ): FormScore
}
```

#### d. Data Flow & Mapping:
1. **Lọc dữ liệu**: Lọc các trận `it.isEnded && it.id != currentMatchId` và có tỷ số hợp lệ (`homeScore != null && awayScore != null`).
2. **Sắp xếp thời gian**: `FormEvaluator` yêu cầu danh sách theo thứ tự thời gian **tăng dần** (phần tử cuối là trận mới nhất). Nếu đầu vào sắp xếp giảm dần (`startTimeDate DESC`), UseCase tự động đảo ngược thứ tự trước khi chuyển đổi.
3. **Chuyển đổi sang `MatchOutcome`**:
   - $Score_{\text{team}} > Score_{\text{opp}} \implies \text{MatchOutcome.WIN}$
   - $Score_{\text{team}} == Score_{\text{opp}} \implies \text{MatchOutcome.DRAW}$
   - $Score_{\text{team}} < Score_{\text{opp}} \implies \text{MatchOutcome.LOSS}$
4. **Gọi Algorithm**: `formEvaluator.evaluate(outcomes, windowSize)`.

#### e. Edge Cases:
- `matches` rỗng hoặc không có trận nào `isEnded`: Trả về `FormScore(score = 0.0, rawScore = 0.0, matchesCount = 0, wins = 0, draws = 0, losses = 0, totalPoints = 0.0, maxPoints = 0.0)`.
- Số trận $N < windowSize$: `FormEvaluator` tự động tính trên số trận thực tế $N$.

---

### 3.2. D2.2 — `CalculateEloRatingUseCase` (Phase 6: Elo Rating)

#### a. Mục tiêu:
Expose Phase 6 `RatingCalculator` (`EloRatingCalculator`) thành UseCase độc lập để tính toán xác suất kỳ vọng và cập nhật điểm Elo đối đầu giữa 2 đội bóng sau một trận đấu.

#### b. File / Class Dự kiến:
- Package: `dev.anhquocs.truelab.core.domain.team.usecase`
- Class: `CalculateEloRatingUseCase`

#### c. Contract & Signature:
```kotlin
class CalculateEloRatingUseCase(
    private val ratingCalculator: RatingCalculator = EloRatingCalculator()
) {
    /**
     * Tính xác suất điểm số kỳ vọng chiến thắng của Đội A trước Đội B.
     */
    fun calculateExpectedScore(ratingA: Double, ratingB: Double): Double =
        ratingCalculator.expectedScore(ratingA, ratingB)

    /**
     * Tính toán cập nhật điểm Elo đối kháng sau khi trận đấu kết thúc (Zero-Sum Conservation).
     *
     * @param ratingHome Điểm Elo hiện tại của đội nhà.
     * @param ratingAway Điểm Elo hiện tại của đội khách.
     * @param homeScore Số bàn thắng thực tế của đội nhà.
     * @param awayScore Số bàn thắng thực tế của đội khách.
     * @param kFactor Hệ số K-factor (mặc định 32.0).
     * @return [RatingMatchResult] chứa điểm mới của cả 2 đội và lượng biến thiên ratingChange.
     */
    fun calculateMatchResult(
        ratingHome: Double,
        ratingAway: Double,
        homeScore: Int,
        awayScore: Int,
        kFactor: Double = RatingCalculator.DEFAULT_K
    ): RatingMatchResult {
        val actualScoreHome = when {
            homeScore > awayScore -> 1.0
            homeScore == awayScore -> 0.5
            else -> 0.0
        }
        return ratingCalculator.calculateMatch(ratingHome, ratingAway, actualScoreHome, kFactor)
    }
}
```

#### d. Phân định Ranh giới Persistence:
- **UseCase này là Pure Computation**: Chịu trách nhiệm tính toán toán học chính xác $E_H$ và $(\text{newRating}_H, \text{newRating}_A, \Delta R)$.
- Việc lưu trữ cập nhật `TeamEntity.eloRating` xuống Room DB thuộc về Pipeline đồng bộ dữ liệu (`Data Sync Engine`), không nhúng side-effect ghi DB vào UseCase tính toán thuần túy.

---

### 3.3. D2.3 — `AnalyzeOddsTrendUseCase` (Phase 4: SMA Trend)

#### a. Mục tiêu:
Expose Phase 4 `MovingAverageCalculator` (`SimpleMovingAverageCalculator`) thành UseCase độc lập để làm mịn chuỗi biến động tỷ lệ cược (Odds Time Series) và nhận diện xu hướng biến động kèo.

#### b. File / Class Dự kiến:
- Package: `dev.anhquocs.truelab.core.domain.odds.usecase`
- Class: `AnalyzeOddsTrendUseCase`
- Model: `OddsTrendAnalysis.kt`

#### c. Contract & Signature:
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
    ): OddsTrendAnalysis {
        // 1. Sắp xếp theo thứ tự thời gian tăng dần
        val sortedHistory = oddsHistory.sortedBy { it.changeTime }

        // 2. Trích xuất dãy số thực hợp lệ theo targetField (lọc null, <= 0.0, non-finite)
        val validPairs = sortedHistory.mapNotNull { item ->
            val value = when (targetField) {
                TargetOddsField.HOME_WIN -> item.homeWin
                TargetOddsField.DRAW -> item.draw
                TargetOddsField.AWAY_WIN -> item.awayWin
                TargetOddsField.OVER -> item.over
                TargetOddsField.UNDER -> item.under
                TargetOddsField.HANDICAP -> item.handicap
            }
            if (value != null && value.isFinite() && value > 0.0) {
                Pair(item.changeTime, value)
            } else {
                null
            }
        }

        val timestamps = validPairs.map { it.first }
        val rawSeries = validPairs.map { it.second }

        // 3. Gọi Phase 4 SMA Calculator (O(n) Rolling Sum)
        val smaSeries = if (rawSeries.size >= windowSize) {
            movingAverageCalculator.calculate(rawSeries, windowSize)
        } else {
            emptyList()
        }

        // 4. Tính Volatility (Sample StdDev từ Phase 3)
        val volatility = if (rawSeries.size >= 2) {
            statisticsCalculator.sampleStandardDeviation(rawSeries)
        } else {
            0.0
        }

        val openingOdds = rawSeries.firstOrNull()
        val currentOdds = rawSeries.lastOrNull()

        return OddsTrendAnalysis(
            matchId = matchId,
            targetField = targetField,
            windowSize = windowSize,
            timestamps = timestamps,
            rawOddsSeries = rawSeries,
            smaSeries = smaSeries,
            currentOdds = currentOdds,
            openingOdds = openingOdds,
            volatility = if (volatility.isNaN()) 0.0 else volatility,
            hasSufficientData = smaSeries.isNotEmpty()
        )
    }
}
```

#### d. Xử lý Biên & Dữ liệu Khuyết thiếu (Edge Cases):
- Dữ liệu $N < windowSize$: `smaSeries = emptyList()`, `hasSufficientData = false`, không ném exception.
- Tỷ lệ cược lỗi (âm, 0, NaN): Tự động lọc bỏ trước khi đưa vào SMA.

---

### 3.4. D2.4 — `GetTeamStatisticsUseCase` (Phase 3: Statistics)

#### a. Mục tiêu:
Expose Phase 3 `StatisticsCalculator` (`DescriptiveStatisticsCalculator`) thành UseCase độc lập để tổng hợp toàn diện các chỉ số phân phối thống kê mô tả (Mean, Median, Min, Max, Range, Variance, StdDev, Skewness) cho đội bóng từ lịch sử các trận đã đấu.

#### b. File / Class Dự kiến:
- Package: `dev.anhquocs.truelab.core.domain.team.usecase`
- Class: `GetTeamStatisticsUseCase`
- Model: `TeamPerformanceStatistics.kt`

#### c. Contract & Signature:
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
    ): TeamPerformanceStatistics {
        val endedMatches = matches.filter {
            it.isEnded && (currentMatchId == null || it.id != currentMatchId) &&
            it.homeScore != null && it.awayScore != null
        }

        val goalsScored = endedMatches.map { m ->
            (if (m.homeTeam.id == teamId) m.homeScore else m.awayScore)!!.toDouble()
        }
        val goalsConceded = endedMatches.map { m ->
            (if (m.homeTeam.id == teamId) m.awayScore else m.homeScore)!!.toDouble()
        }
        val totalGoals = endedMatches.map { m ->
            (m.homeScore!! + m.awayScore!!).toDouble()
        }
        val goalDiffs = endedMatches.map { m ->
            val scored = if (m.homeTeam.id == teamId) m.homeScore!! else m.awayScore!!
            val conceded = if (m.homeTeam.id == teamId) m.awayScore!! else m.homeScore!!
            (scored - conceded).toDouble()
        }

        return TeamPerformanceStatistics(
            teamId = teamId,
            matchesCount = endedMatches.size,
            goalsScoredStats = statisticsCalculator.summarize(goalsScored),
            goalsConcededStats = statisticsCalculator.summarize(goalsConceded),
            totalGoalsStats = statisticsCalculator.summarize(totalGoals),
            goalDiffStats = statisticsCalculator.summarize(goalDiffs)
        )
    }
}
```

---

## 4. Kế hoạch Tái sử dụng & Ràng buộc Legacy (Legacy & Compatibility Guardrails)

1. **TUYỆT ĐỐI KHÔNG XÓA trong D2**:
   - `PredictionResult.Companion.computeWeightedScoring`
   - `PredictMatchUseCase`
   - `SeasonRanking.calculateFormScore()`
   - `PredictionRepository.predictMatch`
2. **Khả năng tương thích (Zero Breaking Change)**:
   - Các UseCase mới hoạt động độc lập và bổ trợ, không làm thay đổi signature của bất kỳ public API hiện có nào.
   - Helper mapping `Match.toOutcomeForTeam(teamId: Int)` được tái sử dụng qua package private hoặc internal helper.

---

## 5. Chiến lược Kiểm thử (Testing Strategy)

Mỗi UseCase mới trong D2 sẽ có bộ Unit Test tương ứng trong `:core:domain/src/test`:

1. **`CalculateTeamFormUseCaseTest`**:
   - Happy path: Chuỗi 5 trận W-W-D-L-W trả về `FormScore` khớp công thức Time-Decay của Phase 5.
   - Trận đấu đảo ngược thứ tự thời gian: Đảm bảo tự động sắp xếp đúng thứ tự thời gian tăng dần.
   - Tập dữ liệu $N < 5$ (ví dụ 2 trận) và danh sách rỗng.
   - Bỏ qua các trận chưa kết thúc (`status != ENDED`) và trận có `id == currentMatchId`.
2. **`CalculateEloRatingUseCaseTest`**:
   - Happy path: Đội nhà thắng ($Score_H > Score_A$) $\implies$ Elo đội nhà tăng, đội khách giảm đúng lượng $\Delta R$.
   - Trận hòa: Điểm thực tế $0.5$, bảo toàn nguyên tắc Zero-Sum.
   - Tính toán xác suất kỳ vọng $E_H$ đối xứng khi Elo bằng nhau ($0.5$).
3. **`AnalyzeOddsTrendUseCaseTest`**:
   - Happy path: Chuỗi 10 bản ghi odds $\to$ Dãy SMA cửa sổ $k=3$ có kích thước $10 - 3 + 1 = 8$.
   - Dữ liệu $N < windowSize$ trả về `smaSeries` rỗng an toàn (`hasSufficientData = false`), không ném exception.
   - Kiểm tra lọc bỏ các bản ghi odds lỗi (null, $\le 0.0$).
   - Tính toán Volatility (Sample StdDev) chính xác.
4. **`GetTeamStatisticsUseCaseTest`**:
   - Happy path: Danh sách trận đấu tính ra Mean, Median, Min, Max, StdDev, Skewness của bàn thắng/bàn thua.
   - Danh sách rỗng: Xử lý an toàn các giá trị `Double.NaN`.
5. **Regression Requirement**:
   - 122/122 tests của `:core:algorithm` tiếp tục PASS 100%.
   - 35/35 tests của `:core:domain` hiện có tiếp tục PASS 100%.

---

## 6. Danh mục File Triển khai (File Change Plan)

### 🔹 Files Mới trong `:core:domain/src/main`:
1. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateTeamFormUseCase.kt`
2. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateEloRatingUseCase.kt`
3. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/model/OddsTrendAnalysis.kt`
4. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/usecase/AnalyzeOddsTrendUseCase.kt`
5. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/TeamPerformanceStatistics.kt`
6. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/GetTeamStatisticsUseCase.kt`

### 🔹 Files Mới trong `:core:domain/src/test`:
7. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateTeamFormUseCaseTest.kt`
8. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateEloRatingUseCaseTest.kt`
9. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/odds/usecase/AnalyzeOddsTrendUseCaseTest.kt`
10. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/GetTeamStatisticsUseCaseTest.kt`

### 🔹 Files Báo cáo:
11. `docs/reports/domain-d2-analytics.md`

### ⛔ Files Tuyệt đối KHÔNG SỬA:
- Toàn bộ module `:core:algorithm` (FROZEN).
- UI / ViewModel / Presentation layer.
- Database Schema / Room Entities trong `:core:data`.

---

## 7. Thứ tự Thực thi Đề xuất (Execution Order)

1. **Bước 1 — Triển khai `CalculateTeamFormUseCase` (Phase 5)**: Tách logic đánh giá phong độ độc lập, tái sử dụng mapper `MatchOutcome` và viết Unit Test.
2. **Bước 2 — Triển khai `CalculateEloRatingUseCase` (Phase 6)**: Triển khai tính toán kỳ vọng và cập nhật điểm đối kháng Elo, viết Unit Test.
3. **Bước 3 — Triển khai `AnalyzeOddsTrendUseCase` (Phase 4)**: Tạo model `OddsTrendAnalysis`, tích hợp `SimpleMovingAverageCalculator` và viết Unit Test.
4. **Bước 4 — Triển khai `GetTeamStatisticsUseCase` (Phase 3)**: Tạo model `TeamPerformanceStatistics`, tích hợp `DescriptiveStatisticsCalculator` và viết Unit Test.
5. **Bước 5 — Xác thực Toàn diện (Verification)**: Chạy `./gradlew :core:domain:test :core:algorithm:test` và `./gradlew test` để đảm bảo 100% test suites vượt qua.
6. **Bước 6 — Tạo Báo cáo**: Lập tài liệu tổng kết `docs/reports/domain-d2-analytics.md`.

---

## 8. Tiêu chí Hoàn thành (Definition of Done)

- [ ] Cả 4 UseCases (`CalculateTeamFormUseCase`, `CalculateEloRatingUseCase`, `AnalyzeOddsTrendUseCase`, `GetTeamStatisticsUseCase`) hoạt động độc lập trong `:core:domain`.
- [ ] 100% Unit Tests mới viết cho 4 UseCases đều PASS.
- [ ] Toàn bộ 122/122 Algorithm Tests tiếp tục PASS (FROZEN).
- [ ] Toàn bộ Domain Tests (35 test cũ + test mới) đều PASS.
- [ ] Không có breaking change trong `:core:data`, `:core:ui`, hay `:app`.
- [ ] Tài liệu `docs/reports/domain-d2-analytics.md` ghi nhận đầy đủ quá trình triển khai.
