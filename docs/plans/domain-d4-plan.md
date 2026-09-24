# Domain D4 Plan — Domain Expansion, Evaluation & Benchmark Suite

> **Trạng thái**: Draft / Plan Only  
> **Mục tiêu**: Xây dựng phân kỳ nghiệp vụ Domain D4 mở rộng các UseCase đánh giá mô hình (Evaluation Metrics, Backtest Engine), tính toán thống kê sân nhà/sân khách (Home/Away Splits), đo lường hiệu năng thuật toán thuần túy (Benchmark Suite) và dọn dẹp triệt để các thành phần `@Deprecated` trong `:core:domain`.

---

## 1. Phạm vi Triển khai (Scope)

Phase **Domain D4** tập trung 100% vào logic nghiệp vụ thuần túy (Pure Kotlin/JVM) trong module `:core:domain` (với các thuật toán nền tảng trong `:core:algorithm`), bao gồm 5 cấu phần chính:

1. **D4.1 — Evaluation Metrics**:
   - Tính toán ma trận nhầm lẫn 3 chiều ($3 \times 3$ Confusion Matrix: Home Win, Draw, Away Win).
   - Tính toán các chỉ số phân loại đa lớp: Accuracy, Per-class Precision, Per-class Recall, Per-class F1-score, Macro-averaged Precision/Recall/F1.
   - Xử lý an toàn các trường hợp biên: chia cho 0, tập dữ liệu rỗng, nhãn thiếu.

2. **D4.2 — Prediction Backtest Engine**:
   - Kiểm thử hồi quy mô hình dự đoán (`PredictMatchOutcomeUseCase`) trên tập dữ liệu trận đấu lịch sử đã kết thúc (`isEnded == true`).
   - Khai thác đặc trưng lịch sử an toàn chống rò rỉ dữ liệu (Temporal Data Leakage Prevention).
   - So sánh kết quả dự đoán với kết quả thực tế để tổng hợp chỉ số đánh giá `ModelEvaluationResult`.

3. **D4.3 — Home/Away Splits**:
   - Tổng hợp thống kê thành tích riêng biệt khi thi đấu trên sân nhà (Home Split) và sân khách (Away Split) cho một đội bóng.
   - Tính toán số trận thi đấu, Thắng/Hòa/Thua, Bàn thắng/Bàn thua/Hiệu số, Điểm số và Tỷ lệ thắng (Win Rate).
   - Xử lý trường hợp không có trận đấu, tỷ lệ chia cho 0 và sân trung lập.

4. **D4.4 — Algorithm Benchmark Suite**:
   - Bộ sinh dữ liệu thử nghiệm giả lập (Synthetic Dataset Generator) có thể tùy biến quy mô $N \in \{1,000; 10,000; 50,000\}$ với cơ chế hạt giống ngẫu nhiên (Deterministic Seed).
   - Bộ đo lường hiệu năng thời gian thực thi (Execution Time Runner) đo đạc thuật toán Tìm kiếm (`LinearSearchMatcher` vs `BinarySearchMatcher`) và Sắp xếp (`QuickSortOrder` vs `MergeSortOrder`).
   - Cơ chế JVM Warmup để ổn định JIT Compiler trước khi ghi nhận kết quả đo trung bình/trung vị.

5. **D4.5 — Legacy Deprecated Cleanup**:
   - Dọn dẹp an toàn các API đã đánh dấu `@Deprecated` từ Phase P1.5 (`computeWeightedScoring`, `SeasonRanking.calculateFormScore`, `PredictMatchUseCase`).
   - Cập nhật module DI `:core:data` (`PredictionDataModule`) loại bỏ binding legacy sau khi xác nhận 0 còn caller.

---

## 2. Ngoài Phạm vi (Non-Scope)

Tuyệt đối **KHÔNG** thực hiện các hạng mục sau trong Phase Domain D4:
- ❌ **Không triển khai UI / Jetpack Compose**: Giao diện `BenchmarkScreen`, đồ thị biểu diễn hiệu năng hay bộ lọc Home/Away Splits thuộc về **Presentation P2**.
- ❌ **Không thay đổi Room Database Schema**: Các trường `leagueId`, `season` và migrations thuộc về **Data D1**.
- ❌ **Không can thiệp API Crawler / Sync Engine**: `DataSyncEngine` updates thuộc về **Data D1**.
- ❌ **Không sửa đổi core algorithm implementations**: Các thuật toán Phase 1–7 trong `:core:algorithm` đã đạt 122/122 test và ở trạng thái **Frozen**.
- ❌ **Không thêm thư viện Machine Learning bên ngoài**: Toàn bộ pipeline đánh giá và backtest sử dụng toán học và thống kê thuần túy Kotlin Standard Library.

---

## 3. Rà soát Mã nguồn Hiện tại (Source Code Audit)

| Thành phần | Hiện trạng | Phân loại | Khả năng Tái sử dụng & Yêu cầu Mới |
| :--- | :--- | :--- | :--- |
| **Confusion Matrix & Classification Metrics** | Chưa có trong repo | **Missing** | Cần thiết kế mới `ConfusionMatrix3Way`, `ModelEvaluationResult` và `CalculateEvaluationMetricsUseCase`. |
| **Prediction Outcome Comparison** | Đã có `PredictionProbabilities` & `PredictionResult` | **Reusable** | `PredictionResult.predictedOutcome` ("HOME_WIN", "DRAW", "AWAY_WIN") sẵn sàng cho so khớp kết quả thực tế. |
| **Match Outcome Derivation** | Đã có `MatchOutcome` (WIN, DRAW, LOSS) | **Reusable** | Tái sử dụng `Match.isEnded`, `Match.homeScore`, `Match.awayScore` để xác định actual outcome. |
| **PredictMatchOutcomeUseCase** | Đã hoàn thiện trong Domain D1 | **Reusable** | Động cơ dự đoán cốt lõi dùng cho Backtest Engine. |
| **Home/Away Splits Calculation** | Chưa có UseCase chuyên biệt | **Missing** | Cần thiết kế `TeamHomeAwaySplits` model và `CalculateHomeAwaySplitsUseCase`. |
| **Search & Sort Algorithms** | Đã có 4 thuật toán trong `:core:algorithm` | **Reusable** | `LinearSearchMatcher`, `BinarySearchMatcher`, `QuickSortOrder`, `MergeSortOrder` dùng làm đối tượng benchmark. |
| **Synthetic Dataset Generator** | Chỉ có test fixtures nhỏ lẻ | **Missing** | Cần xây dựng `SyntheticDatasetGenerator` hỗ trợ $N = 1\text{K}, 10\text{K}, 50\text{K}$. |
| **Benchmark Timing Runner** | Chưa có trong Domain | **Missing** | Cần xây dựng `AlgorithmBenchmarkRunner` với cơ chế JVM Warmup. |
| **Legacy `@Deprecated` Components** | Đã đánh dấu trong P1.5 | **Legacy** | Sẵn sàng dọn dẹp trong D4.5 sau khi các usecase D4 hoàn tất. |

---

## 4. Ranh giới Kiến trúc Đa Tầng (Architecture Boundary)

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        :core:domain (Pure Kotlin/JVM)                  │
│                                                                        │
│   ┌───────────────────────────┐    ┌───────────────────────────────┐   │
│   │     evaluation/           │    │          benchmark/           │   │
│   │  • ConfusionMatrix3Way    │    │  • SyntheticDatasetGenerator  │   │
│   │  • ModelEvaluationResult  │    │  • SearchBenchmarkRunner      │   │
│   │  • CalculateEvaluation-   │    │  • SortBenchmarkRunner        │   │
│   │    MetricsUseCase         │    │  • BenchmarkSuiteResult       │   │
│   │  • BacktestPrediction-    │    │  • RunAlgorithmBenchmark-     │   │
│   │    UseCase                │    │    UseCase                    │   │
│   └─────────────┬─────────────┘    └───────────────┬───────────────┘   │
│                 │                                  │                   │
│   ┌─────────────┴─────────────┐                    │                   │
│   │          team/            │                    │                   │
│   │  • TeamHomeAwaySplits     │                    │                   │
│   │  • CalculateHomeAway-     │                    │                   │
│   │    SplitsUseCase          │                    │                   │
│   └───────────────────────────┘                    │                   │
└───────────────────┬────────────────────────────────┼───────────────────┘
                    │                                │
                    ▼                                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       :core:algorithm (Pure Kotlin/JVM)                │
│   • Search: LinearSearchMatcher, BinarySearchMatcher                   │
│   • Sort: QuickSortOrder, MergeSortOrder                               │
│   • Prediction: WeightedScorer, DefaultWeightedScorer                  │
│   • Rating: EloRatingCalculator                                        │
│   • Evaluation: LinearDecayFormEvaluator                               │
└────────────────────────────────────────────────────────────────────────┘
```

- **`:core:domain`**: Đảm bảo 100% không import `android.*`, `androidx.*`, `javax.inject.*` (ngoại trừ `@Inject` nếu có cho DI), `io.ktor.*` hay Room annotations.
- **`:core:algorithm`**: Giữ nguyên trạng thái 0 dependency, không chỉnh sửa code đã frozen.

---

## 5. Thiết kế Chi tiết D4.1 — Evaluation Metrics

### 5.1. Mô hình Dữ liệu (Domain Models)

```kotlin
package dev.anhquocs.truelab.core.domain.evaluation.model

/**
 * Ma trận nhầm lẫn 3 chiều cho bài toán phân loại kết quả trận đấu.
 *
 * Cấu trúc ma trận:
 *                 Predicted
 *               HOME  DRAW  AWAY
 * Actual HOME [ C_hh  C_hd  C_ha ]
 *        DRAW [ C_dh  C_dd  C_da ]
 *        AWAY [ C_ah  C_ad  C_aa ]
 */
data class ConfusionMatrix3Way(
    val homeAsHome: Int, // True Home
    val homeAsDraw: Int,
    val homeAsAway: Int,
    val drawAsHome: Int,
    val drawAsDraw: Int, // True Draw
    val drawAsAway: Int,
    val awayAsHome: Int,
    val awayAsDraw: Int,
    val awayAsAway: Int  // True Away
) {
    val totalSamples: Int get() =
        homeAsHome + homeAsDraw + homeAsAway +
        drawAsHome + drawAsDraw + drawAsAway +
        awayAsHome + awayAsDraw + awayAsAway

    val correctPredictions: Int get() =
        homeAsHome + drawAsDraw + awayAsAway
}

/**
 * Chỉ số đánh giá chi tiết cho từng lớp (Class-level Metrics).
 */
data class ClassEvaluationMetrics(
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val support: Int
)

/**
 * Kết quả đánh giá tổng thể mô hình dự đoán.
 */
data class ModelEvaluationResult(
    val accuracy: Double,
    val macroPrecision: Double,
    val macroRecall: Double,
    val macroF1: Double,
    val homeMetrics: ClassEvaluationMetrics,
    val drawMetrics: ClassEvaluationMetrics,
    val awayMetrics: ClassEvaluationMetrics,
    val confusionMatrix: ConfusionMatrix3Way,
    val totalEvaluated: Int
)
```

### 5.2. Hợp đồng Tính toán (UseCase Contract)

```kotlin
package dev.anhquocs.truelab.core.domain.evaluation.usecase

class CalculateEvaluationMetricsUseCase {

    /**
     * @param predictions Danh sách các cặp (predictedOutcome, actualOutcome).
     *        Giá trị chuỗi chuẩn hóa: "HOME_WIN", "DRAW", "AWAY_WIN".
     * @return [ModelEvaluationResult] chứa đầy đủ metrics.
     */
    operator fun invoke(
        predictions: List<Pair<String, String>>
    ): ModelEvaluationResult
}
```

### 5.3. Công thức Toán học & Quy tắc Xử lý Biên

1. **Overall Accuracy**:
   $$\text{Accuracy} = \frac{C_{hh} + C_{dd} + C_{aa}}{\sum_{i,j} C_{ij}}$$
   *Nếu tổng số mẫu $= 0 \implies \text{Accuracy} = 0.0$.*

2. **Per-Class Precision**:
   $$\text{Precision}_c = \frac{C_{cc}}{\sum_{r} C_{rc}}$$
   *Nếu mẫu số $= 0$ (mô hình không bao giờ dự đoán lớp $c$) $\implies \text{Precision}_c = 0.0$.*

3. **Per-Class Recall**:
   $$\text{Recall}_c = \frac{C_{cc}}{\sum_{k} C_{ck}}$$
   *Nếu mẫu số $= 0$ (thực tế không có mẫu nào thuộc lớp $c$) $\implies \text{Recall}_c = 0.0$.*

4. **Per-Class F1-Score**:
   $$\text{F1}_c = \begin{cases} 2 \times \frac{\text{Precision}_c \times \text{Recall}_c}{\text{Precision}_c + \text{Recall}_c} & \text{khi } \text{Precision}_c + \text{Recall}_c > 0 \\ 0.0 & \text{ngược lại} \end{cases}$$

5. **Macro-Averaging**:
   $$\text{Macro-F1} = \frac{\text{F1}_{\text{Home}} + \text{F1}_{\text{Draw}} + \text{F1}_{\text{Away}}}{3.0}$$

---

## 6. Thiết kế Chi tiết D4.2 — Prediction Backtest Engine

### 6.1. Quy trình Backtest An toàn Chống Rò rỉ Dữ liệu (Zero Data Leakage)

```text
Danh sách Trận đấu Đã kết thúc (Sorted by startTimeDate)
         │
         ▼
Vòng lặp qua từng trận đấu T_i:
   ├── 1. Lọc Recent Matches của Đội Nhà: matches có thời gian < T_i.startTimeDate
   ├── 2. Lọc Recent Matches của Đội Khách: matches có thời gian < T_i.startTimeDate
   ├── 3. Lọc H2H Matches giữa 2 đội: matches có thời gian < T_i.startTimeDate
   ├── 4. Lấy Odds ghi nhận cho trận T_i
   ├── 5. Tạo MatchPredictionContext (Không chứa kết quả của chính trận T_i)
   ├── 6. Chạy PredictMatchOutcomeUseCase(context) ──► predictedOutcome
   ├── 7. Trích xuất actualOutcome từ (T_i.homeScore vs T_i.awayScore)
   └── 8. Ghi nhận BacktestMatchRecord
         │
         ▼
Tổng hợp danh sách các cặp (predicted, actual) ──► CalculateEvaluationMetricsUseCase
         │
         ▼
Trả về PredictionBacktestResult
```

### 6.2. Mô hình Dữ liệu & UseCase Contract

```kotlin
package dev.anhquocs.truelab.core.domain.evaluation.model

data class BacktestMatchRecord(
    val matchId: Long,
    val matchDate: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val predictedOutcome: String,
    val actualOutcome: String,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val confidenceScore: Double,
    val isCorrect: Boolean
)

data class PredictionBacktestResult(
    val evaluationResult: ModelEvaluationResult,
    val records: List<BacktestMatchRecord>,
    val totalMatches: Int,
    val correctMatches: Int
)
```

```kotlin
package dev.anhquocs.truelab.core.domain.evaluation.usecase

class BacktestPredictionUseCase(
    private val predictMatchOutcomeUseCase: PredictMatchOutcomeUseCase = PredictMatchOutcomeUseCase(),
    private val calculateEvaluationMetricsUseCase: CalculateEvaluationMetricsUseCase = CalculateEvaluationMetricsUseCase()
) {
    operator fun invoke(
        endedMatches: List<Match>,
        matchOddsMap: Map<Long, dev.anhquocs.truelab.core.domain.odds.model.OddsRecordItem> = emptyMap(),
        teamEloMap: Map<Int, Double> = emptyMap()
    ): PredictionBacktestResult
}
```

---

## 7. Thiết kế Chi tiết D4.3 — Home/Away Splits

### 7.1. Mô hình Dữ liệu (Domain Models)

```kotlin
package dev.anhquocs.truelab.core.domain.team.model

data class TeamPerformanceSplit(
    val played: Int,
    val won: Int,
    val draw: Int,
    val loss: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDiff: Int,
    val points: Int,
    val winRate: Double // Giá trị trong đoạn [0.0, 1.0]
)

data class TeamHomeAwaySplits(
    val teamId: Int,
    val homeSplit: TeamPerformanceSplit,
    val awaySplit: TeamPerformanceSplit,
    val totalSplit: TeamPerformanceSplit
)
```

### 7.2. Hợp đồng Nghiệp vụ (UseCase Contract)

```kotlin
package dev.anhquocs.truelab.core.domain.team.usecase

class CalculateHomeAwaySplitsUseCase {

    /**
     * Tính toán thống kê phân tách sân nhà / sân khách cho một đội bóng từ danh sách trận đấu.
     *
     * @param teamId ID đội bóng cần phân tích.
     * @param matches Danh sách trận đấu liên quan (chỉ tính các trận isEnded == true và có tỷ số).
     * @return [TeamHomeAwaySplits] chứa thống kê Sân nhà, Sân khách và Tổng hợp.
     */
    operator fun invoke(
        teamId: Int,
        matches: List<dev.anhquocs.truelab.core.domain.match.model.Match>
    ): TeamHomeAwaySplits
}
```

### 7.3. Quy tắc Biên
- Nếu `played == 0` $\implies$ `winRate = 0.0`, các chỉ số bàn thắng và điểm số $= 0$.
- `goalDiff = goalsFor - goalsAgainst`.
- `points = won * 3 + draw * 1`.
- `winRate = won.toDouble() / played.toDouble()`.

---

## 8. Thiết kế Chi tiết D4.4 — Algorithm Benchmark Suite

### 8.1. Mô hình Dữ liệu (Domain Models)

```kotlin
package dev.anhquocs.truelab.core.domain.benchmark.model

data class BenchmarkMeasurement(
    val algorithmName: String,
    val algorithmType: String, // "SEARCH" hoặc "SORT"
    val datasetSize: Int,      // 1000, 10000, 50000
    val executionTimeMs: Double,
    val iterationsRun: Int
)

data class ComparisonBenchmarkResult(
    val algorithmA: BenchmarkMeasurement,
    val algorithmB: BenchmarkMeasurement,
    val speedupFactor: Double, // executionTimeA / executionTimeB
    val fasterAlgorithm: String
)

data class ComprehensiveBenchmarkSuiteResult(
    val searchBenchmarks: List<ComparisonBenchmarkResult>,
    val sortBenchmarks: List<ComparisonBenchmarkResult>,
    val timestamp: Long
)
```

### 8.2. Bộ Sinh Dữ liệu Giả lập (Synthetic Dataset Generator)

```kotlin
package dev.anhquocs.truelab.core.domain.benchmark.generator

object SyntheticDatasetGenerator {
    /**
     * Sinh danh sách số nguyên ngẫu nhiên xác định theo seed.
     */
    fun generateRandomIntList(size: Int, seed: Long = 42L): List<Int>

    /**
     * Sinh danh sách đã sắp xếp phục vụ đo Binary Search.
     */
    fun generateSortedIntList(size: Int, seed: Long = 42L): List<Int>
}
```

### 8.3. Hợp đồng Thực thi Benchmark (UseCase Contract)

```kotlin
package dev.anhquocs.truelab.core.domain.benchmark.usecase

class RunAlgorithmBenchmarkUseCase {

    /**
     * Thực thi toàn bộ bộ đo lường hiệu năng:
     * 1. Warmup JVM (100 iterations)
     * 2. Search Benchmark (Linear Search vs Binary Search trên N = 1K, 10K, 50K)
     * 3. Sort Benchmark (QuickSort vs MergeSort trên N = 1K, 10K, 50K)
     *
     * @param datasetSizes Danh sách kích thước đo lường (mặc định [1000, 10000, 50000]).
     * @param iterationsPerSize Số lần đo lường để tính trung bình (mặc định 5).
     * @return [ComprehensiveBenchmarkSuiteResult]
     */
    operator fun invoke(
        datasetSizes: List<Int> = listOf(1_000, 10_000, 50_000),
        iterationsPerSize: Int = 5
    ): ComprehensiveBenchmarkSuiteResult
}
```

---

## 9. Thiết kế Chi tiết D4.5 — Legacy Deprecated Cleanup

### 9.1. Danh mục Xóa bỏ & Điều chỉnh

1. **`PredictionResult.Companion.computeWeightedScoring`**:
   - Xóa bỏ hoàn toàn companion object trong `core/domain/.../prediction/model/Prediction.kt`.
   - Xác nhận 0 caller sau Phase P1.

2. **`SeasonRanking.calculateFormScore()`**:
   - Xóa bỏ method trong `core/domain/.../team/model/Team.kt`.
   - Toàn bộ tính toán phong độ đã chuyển sang `CalculateTeamFormUseCase`.

3. **`PredictMatchUseCase` & `PredictionDataModule`**:
   - Xóa bỏ class `PredictMatchUseCase` trong `core/domain/.../prediction/usecase/PredictionUseCases.kt`.
   - Cập nhật `core/data/.../prediction/di/PredictionDataModule.kt` để dọn dẹp provider bindings tương ứng.

---

## 10. Chiến lược Kiểm thử (Test Strategy Matrix)

Mọi cấu phần của Domain D4 phải được kiểm thử toàn diện với tỉ lệ bao phủ cao, tuân thủ tiêu chuẩn Unit Test thuần túy.

| Nhóm Tính năng | Tên Test Class | Số Test Cases Tối thiểu | Kịch bản Kiểm thử Trọng tâm |
| :--- | :--- | :---: | :--- |
| **Evaluation Metrics** | `CalculateEvaluationMetricsUseCaseTest` | $\ge 8$ | • Dự đoán chính xác 100% (Perfect Accuracy)<br/>• Dự đoán sai hoàn toàn (0% Accuracy)<br/>• Phân phối 3 lớp cân bằng & không cân bằng<br/>• Danh sách dự đoán rỗng (`emptyList()`)<br/>• Không có mẫu nào thuộc một lớp cụ thể (Zero division)<br/>• Kiểm tra tính đúng đắn của Macro F1 vs Micro Accuracy |
| **Prediction Backtest** | `BacktestPredictionUseCaseTest` | $\ge 6$ | • Backtest trên tập trận lịch sử giả định<br/>• Kiểm tra chống rò rỉ dữ liệu (loại trừ chính trận đang xét)<br/>• Xử lý trận đấu thiếu tỷ số hoặc chưa kết thúc<br/>• Tập trận rỗng<br/>• Xác thực tính nhất quán giữa Backtest Result và Evaluation Metrics |
| **Home/Away Splits** | `CalculateHomeAwaySplitsUseCaseTest` | $\ge 7$ | • Đội chỉ đá sân nhà / Chỉ đá sân khách<br/>• Đội có cả trận thắng, hòa, thua ở 2 sân<br/>• Đội chưa đấu trận nào (Empty matches $\implies$ 0.0 winRate)<br/>• Xử lý trận đấu trung lập<br/>• Kiểm tra công thức `goalDiff` và `points` |
| **Synthetic Generator** | `SyntheticDatasetGeneratorTest` | $\ge 4$ | • Độ dài tập dữ liệu sinh ra chính xác ($N=1\text{K}, 10\text{K}, 50\text{K}$)<br/>• Tính xác định với cùng một seed ngẫu nhiên<br/>• Mảng sorted thực sự được sắp xếp tăng dần |
| **Algorithm Benchmark** | `RunAlgorithmBenchmarkUseCaseTest` | $\ge 6$ | • Chạy benchmark Search trên $N=1\text{K}, 10\text{K}$<br/>• Chạy benchmark Sort trên $N=1\text{K}, 10\text{K}$<br/>• Binary Search phải nhanh hơn Linear Search ($O(\log N) < O(N)$)<br/>• Thời gian thực thi là số dương hữu hạn<br/>• Speedup factor tính toán chính xác |
| **Regression Suite** | Toàn bộ repo | $\ge 355$ | • `:core:algorithm`: **122 / 122 PASS**<br/>• `:core:domain`: **$\ge 185$ PASS**<br/>• `:app`: **46 / 46 PASS** |

---

## 11. Phân tích Rủi ro & Giải pháp (Risks & Mitigations)

1. **Rủi ro Rò rỉ Dữ liệu Lịch sử trong Backtest (Temporal Data Leakage)**:
   - *Nguy cơ*: Sử dụng các trận đấu diễn ra *sau* trận đang backtest để tính điểm phong độ hoặc Elo.
   - *Giải pháp*: `BacktestPredictionUseCase` sắp xếp tập trận theo thời gian tăng dần (`startTimeDate`), chỉ cấp cho bộ tạo context các trận đấu có mốc thời gian diễn ra nghiêm ngặt *trước* thời điểm trận đấu đang được đánh giá.

2. **Rủi ro Dao động Thời gian Đo lường JVM (JVM Timing Noise)**:
   - *Nguy cơ*: JIT compilation, Garbage Collection (GC) và luồng hệ điều hành gây nhiễu kết quả đo lường thời gian thực thi thuật toán.
   - *Giải pháp*:
     - Thực hiện JVM Warmup ít nhất 100 lần lặp trước khi bắt đầu đo lường chính thức.
     - Chạy nhiều lần lặp (5 iterations) và lấy giá trị trung bình (`executionTimeMs = elapsedNanos / (iterations * 1_000_000.0)`).
     - Giữ kích thước benchmark trong phạm vi hợp lý ($N \le 50,000$) để tránh quá tải bộ nhớ và kích hoạt GC ngoài ý muốn.

3. **Rủi ro Breaking Changes khi Dọn dẹp `@Deprecated`**:
   - *Nguy cơ*: Xóa API legacy làm hỏng các test cũ hoặc module `:core:data`.
   - *Giải pháp*: Rà soát toàn bộ project references trước khi xóa; cập nhật đồng bộ các DI binding trong `PredictionDataModule`.

---

## 12. Thứ tự Triển khai Đề xuất (Implementation Sequence)

```text
D4.1: Evaluation Metrics Models & CalculateEvaluationMetricsUseCase + Tests
  │
  ▼
D4.2: Prediction Backtest Models & BacktestPredictionUseCase + Tests
  │
  ▼
D4.3: Home/Away Splits Models & CalculateHomeAwaySplitsUseCase + Tests
  │
  ▼
D4.4: Synthetic Generator, Benchmark Suite & RunAlgorithmBenchmarkUseCase + Tests
  │
  ▼
D4.5: Deprecated Cleanup & PredictionDataModule refactor
  │
  ▼
Full Regression Verification (Algorithm + Domain + App Tests + assembleDebug)
```

---

## 13. Tiêu chí Hoàn thành (Definition of Done)

- [ ] **1. Hoàn thành Evaluation Metrics (D4.1)**: `ConfusionMatrix3Way`, `ModelEvaluationResult`, `CalculateEvaluationMetricsUseCase` với đầy đủ tests biên.
- [ ] **2. Hoàn thành Prediction Backtest (D4.2)**: `BacktestPredictionUseCase` chạy trên tập trận lịch sử, chống leakage 100%.
- [ ] **3. Hoàn thành Home/Away Splits (D4.3)**: `CalculateHomeAwaySplitsUseCase` và `TeamHomeAwaySplits` model với Unit Tests bao phủ đầy đủ.
- [ ] **4. Hoàn thành Benchmark Suite (D4.4)**: `SyntheticDatasetGenerator`, `RunAlgorithmBenchmarkUseCase` đo đạc Linear vs Binary Search, Quick vs Merge Sort.
- [ ] **5. Hoàn tất Dọn dẹp Deprecated (D4.5)**: Xóa bỏ sạch sẽ các hàm `@Deprecated` trong Domain và binding trong Data.
- [ ] **6. 100% Pure Kotlin/JVM**: Không đưa bất kỳ phụ thuộc Android nào vào `:core:domain`.
- [ ] **7. Kiểm thử Hồi quy Toàn diện**:
  - `:core:algorithm:test`: **122 / 122 PASS**.
  - `:core:domain:test`: **$\ge 185$ PASS** (thêm $\ge 30$ tests mới).
  - `:app:testDebugUnitTest`: **46 / 46 PASS**.
  - `assembleDebug`: **BUILD SUCCESSFUL**.
- [ ] **8. Báo cáo Nghiệm thu**: Tạo tài liệu tổng kết `docs/reports/domain-d4-final.md`.
