# Implementation Plan: Rest Advantage Signal Integration (Phase R3)

> **Trạng thái:** PLANNING ONLY (No production code modifications in this phase)  
> **Tài liệu tham chiếu:**  
> - [Rest Advantage Mathematical Modeling Report](../audits/rest-advantage-mathematical-modeling.md)  
> - [Rest Advantage Feasibility Audit](../audits/rest-advantage-feasibility-audit.md)  
> - [Home Advantage Semantics Audit](../audits/home-advantage-semantics-audit.md)  
> - [Draw Underprediction Audit](../audits/draw-underprediction-audit.md)  
> - [Domain Signal Modeling Spec](../reports/sub/domain-signal-modeling.md)  
> **Ngày lập kế hoạch:** 02/10/2026

---

## 1. Objective

Triển khai thay thế hoàn toàn tín hiệu **Home Advantage** (vốn có semantic mismatch và thiên vị danh nghĩa fixture-side) bằng tín hiệu **Rest Advantage** trong Prediction Pipeline của TrueLab.

### Các mục tiêu cốt lõi:
1. **Duy trì đúng 6 Signals trong Production Pipeline:**
   $$\text{Production Signals} = \{\text{Odds (20\%)}, \text{Elo (20\%)}, \text{Form (25\%)}, \text{Goals (15\%)}, \text{H2H (10\%)}, \mathbf{Rest\ Advantage\ (10\%)}\}$$
   *(Tuyệt đối không tăng lên 7 signals, không làm loãng trọng số).*
2. **Thực thi Contract Toán học đã chốt (Locked Mathematical Contract):**
   $$\Delta \text{Rest} = \text{homeRestDays} - \text{awayRestDays}$$
   $$P(\text{Home}) = 0.37 + 0.09 \cdot \tanh\left(\frac{\Delta \text{Rest}}{3.0}\right), \quad P(\text{Draw}) = 0.26, \quad P(\text{Away}) = 0.37 - 0.09 \cdot \tanh\left(\frac{\Delta \text{Rest}}{3.0}\right)$$
3. **Bảo tồn Home Advantage làm Baseline cho Benchmark:** Thiết kế kiến trúc sạch cho phép chạy đối soát đối đầu (Home Advantage vs Rest Advantage) trên cùng tập dữ liệu, cùng pipeline và cùng cấu hình trọng số ở phase benchmark tiếp theo mà không làm ô nhiễm production code.
4. **Bảo đảm Zero Temporal Data Leakage & $O(1)$ RAM Performance:** Tái sử dụng dữ liệu lịch sử đã có trong `MatchPredictionContext`, không thêm truy vấn Room SQLite, không sinh lỗi $N+1$.

---

## 2. Current Architecture vs Target Architecture

```text
CURRENT ARCHITECTURE (Production Pipeline):
MatchPredictionContext ────────────────────────────────────────────────────────┐
  ├─ homeRecentMatches ──> FormSignalTransformer (25%)                         │
  ├─ homeElo, awayElo   ──> EloSignalTransformer (20%)                          │
  ├─ latestOdds         ──> OddsSignalTransformer (20%)                         │
  ├─ meanGoals          ──> GoalsSignalTransformer (15%)                        │
  ├─ h2hMatches         ──> H2hSignalTransformer (10%)                          │
  └─ isNeutralVenue     ──> HomeAdvantageSignalTransformer (10%) [STATIC 46/26/28]
                                │
                                ▼
                      WeightedScorer.predictOutcome() ──> PredictionResult

================================================================================

TARGET ARCHITECTURE (Production Pipeline with Configurable Benchmark Support):
MatchPredictionContext ────────────────────────────────────────────────────────┐
  ├─ homeRecentMatches ──> FormSignalTransformer (25%)                         │
  ├─ homeElo, awayElo   ──> EloSignalTransformer (20%)                          │
  ├─ latestOdds         ──> OddsSignalTransformer (20%)                         │
  ├─ meanGoals          ──> GoalsSignalTransformer (15%)                        │
  ├─ h2hMatches         ──> H2hSignalTransformer (10%)                          │
  ├─ targetKickoff,     ──> RestAdvantageSignalTransformer (10%) [DYNAMIC TANH] │
  │  homeHistory[0],    │   (Production Default)                               │
  │  awayHistory[0]     │                                                      │
  └─────────────────────┴─> [OR HomeAdvantageSignalTransformer in Benchmark] ──┘
                                │
                                ▼
                      WeightedScorer.predictOutcome() ──> PredictionResult
```

---

## 3. Dependency Analysis & Classification

Rà soát toàn bộ các thành phần liên quan đến `Home Advantage` và `isNeutralVenue` trong codebase hiện tại:

| Thành phần / Symbol | Vị trí Code | Phân loại | Hành động trong Implementation Phase |
| :--- | :--- | :---: | :--- |
| `HomeAdvantageSignalTransformer` | [HomeAdvantageSignalTransformer.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformer.kt) | **D** | **Giữ lại cho Benchmark**: Chuyển thành Baseline Transformer hoặc giữ trong package transformer để phục vụ kiểm thử đối chiếu. |
| `isNeutralVenue: Boolean` | [MatchPredictionContext.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/MatchPredictionContext.kt#L40) | **A** | **Deprecated / Loại bỏ khỏi Production**: Đánh dấu `@Deprecated` hoặc loại bỏ trong production context vì không có data source nào cung cấp và Rest Advantage không sử dụng. |
| `homeAdvantageWeight: Double` | [PredictionWeightConfig.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt#L33) | **B** | **Thay thế / Alias**: Đổi tên thành `restAdvantageWeight: Double = 0.10` (hoặc giữ getter tương thích `sixthSignalWeight`). |
| `homeAdvantageProbHome/Draw/Away` | [PredictionWeightConfig.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt#L40-L42) | **B** | **Thay thế bằng tham số Rest**: Bổ sung `restAdvantageSensitivity = 3.0` và `restAdvantageMaxShift = 0.09`. Giữ các hằng số cũ ở baseline benchmark config. |
| `PredictionEvidence.homeAdvantage` | [PredictionEvidence.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionEvidence.kt#L41) | **B** | **Thay thế**: Thay bằng `val restAdvantage: SignalEvidence`. |
| `PredictionEvidenceComponents` (UI) | [PredictionEvidenceComponents.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/components/PredictionEvidenceComponents.kt#L266-L280) | **B** | **Cập nhật UI**: Hiển thị thẻ Rest Advantage (số ngày nghỉ của Home, Away, $\Delta \text{Rest}$, và độ lệch xác suất). |
| `strings.xml` (Resource Strings) | [strings.xml](../../app/src/main/res/values/strings.xml#L192) | **B** | **Cập nhật đa ngôn ngữ**: Bổ sung chuỗi `prediction_signal_rest_adv_title`, `prediction_signal_rest_adv_detail` cho cả 3 locales (`values`, `values-en`, `values-vi`). |
| `HomeAdvantageSignalTransformerTest` | [HomeAdvantageSignalTransformerTest.kt](../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformerTest.kt) | **D** | **Giữ lại cho Baseline Test**: Giữ nguyên để kiểm chứng baseline benchmark transformer. |

### Chú thích phân loại:
- **A**: Có thể xóa / loại bỏ an toàn khỏi luồng production.
- **B**: Cần thay thế trực tiếp bằng phiên bản Rest Advantage.
- **C**: Tiếp tục sử dụng bởi các tính năng khác (không có trường hợp nào).
- **D**: Giữ lại phục vụ mục đích kiểm thử / đánh giá benchmark (Baseline Preservation).

---

## 4. Rest Data Flow & Temporal Safety

### 4.1. Luồng trích xuất dữ liệu không sinh truy vấn mới ($O(1)$ RAM)
Trong `PredictMatchOutcomeUseCase`, dữ liệu được trích xuất trực tiếp từ `MatchPredictionContext`:

```text
MatchPredictionContext
  ├── matchStartTimeDate (Chuỗi ISO 8601 UTC của trận mục tiêu T)
  ├── homeRecentMatches  (Danh sách trận đã lọc isEnded == true && startTimeDate < T)
  └── awayRecentMatches  (Danh sách trận đã lọc isEnded == true && startTimeDate < T)
```

1. **Trích xuất trận gần nhất:**
   ```kotlin
   val homePrevMatch = context.homeRecentMatches.firstOrNull { it.isEnded && it.id != context.matchId }
   val awayPrevMatch = context.awayRecentMatches.firstOrNull { it.isEnded && it.id != context.matchId }
   ```
2. **Phân giải Epoch Seconds:**
   ```kotlin
   val targetEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(context.matchStartTimeDate)
   val homePrevEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(homePrevMatch?.startTimeDate)
   val awayPrevEpoch = PreMatchOddsSelector.parseKickoffEpochSeconds(awayPrevMatch?.startTimeDate)
   ```
3. **Tính toán $\Delta \text{Rest}$:**
   - Nếu thiếu bất kỳ mốc thời gian nào $\to$ Kích hoạt `Neutral Fallback`.
   - Nếu hợp lệ:
     $$\text{homeRestDays} = \frac{\text{targetEpoch} - \text{homePrevEpoch}}{86400.0}$$
     $$\text{awayRestDays} = \frac{\text{targetEpoch} - \text{awayPrevEpoch}}{86400.0}$$
     $$\Delta \text{Rest} = \text{homeRestDays} - \text{awayRestDays}$$

### 4.2. Bảo đảm Zero Temporal Data Leakage
- `homeRecentMatches` và `awayRecentMatches` đã được lọc nghiêm ngặt tại ViewModel / UseCase (`startTimeDate < targetTime`).
- Transformer kiểm tra điều kiện bổ sung:
  `homePrevEpoch < targetEpoch && awayPrevEpoch < targetEpoch`.
- Nếu có bất kỳ sự cố dữ liệu nào dẫn đến `prevEpoch >= targetEpoch`, transformer lập tức từ chối và trả về `Neutral Fallback`.

---

## 5. Mathematical Contract Specification

Contract toán học chính thức cho `RestAdvantageSignalTransformer`:

```kotlin
object RestAdvantageSignalTransformer {

    fun transform(
        homePreviousMatch: Match?,
        awayPreviousMatch: Match?,
        targetKickoffTime: String?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way
}
```

### Công thức tính toán:
1. **Tham số cấu hình:**
   - $P_{\text{draw}} = \text{config.baselineDrawProb} = 0.26$
   - $P_{\text{neutral}} = \frac{1.0 - P_{\text{draw}}}{2.0} = 0.37$
   - $S = \text{config.restAdvantageSensitivity} = 3.0$ (ngày)
   - $\delta_{\max} = \text{config.restAdvantageMaxShift} = 0.09$
   - $W = \text{config.restAdvantageWeight} = 0.10$

2. **Trường hợp Đầy đủ Dữ liệu (Normal Path):**
   $$\text{bias} = \tanh\left(\frac{\Delta \text{Rest}}{S}\right)$$
   $$\delta = \delta_{\max} \cdot \text{bias}$$
   $$\begin{cases}
   P(\text{Home}) = 0.37 + \delta \\[4pt]
   P(\text{Draw}) = 0.26 \\[4pt]
   P(\text{Away}) = 0.37 - \delta
   \end{cases}$$

3. **Trường hợp Thiếu Dữ liệu (Missing History / Fallback Path):**
   - Khi `homePreviousMatch == null` hoặc `awayPreviousMatch == null` hoặc timestamp không hợp lệ:
     $$P = [0.37, 0.26, 0.37], \quad W = 0.10, \quad \text{name} = \text{"Rest Advantage (Neutral Fallback)"}$$

---

## 6. Production Replacement Strategy

### 6.1. Đúng 6 Tín hiệu trong Production
Trong `PredictMatchOutcomeUseCase.kt`:
```kotlin
val signals: List<Signal3Way> = listOf(
    formSignal,       // 25%
    eloSignal,        // 20%
    goalsSignal,      // 15%
    oddsSignal,       // 20%
    h2hSignal,        // 10%
    restAdvSignal     // 10% (Replaced Home Advantage)
)
```

### 6.2. Cấu trúc `PredictionEvidence`
```kotlin
data class PredictionEvidence(
    val elo: SignalEvidence,
    val form: SignalEvidence,
    val odds: SignalEvidence,
    val goals: SignalEvidence,
    val h2h: SignalEvidence,
    val restAdvantage: SignalEvidence,
    val totalWeight: Double,
    val signals: List<SignalEvidence> = listOf(elo, form, odds, goals, h2h, restAdvantage)
)
```

### 6.3. Metadata trong `SignalEvidence.details`
- `homeRestDays`: e.g. `"7.06"`
- `awayRestDays`: e.g. `"2.69"`
- `deltaRestDays`: e.g. `"+4.37"`
- `isAvailable`: `"true"` (hoặc `"false"` nếu rơi vào fallback)
- `fallbackReason`: e.g. `"NONE"` hoặc `"MISSING_PREVIOUS_MATCH"`

---

## 7. HomeAdvantage Baseline Preservation Strategy (For Benchmark)

Để phục vụ phase benchmark (so sánh đối đầu Home Advantage vs Rest Advantage trên cùng tập dữ liệu thực nghiệm mà không duplicate code), kiến trúc hỗ trợ cơ chế chuyển đổi chiến lược tín hiệu thứ 6 (Sixth Signal Strategy):

### 7.1. Định nghĩa `SixthSignalStrategy` trong `PredictionWeightConfig`

```kotlin
enum class SixthSignalMode {
    REST_ADVANTAGE,      // Production Default
    HOME_ADVANTAGE       // Evaluation / Benchmark Baseline Only
}

data class PredictionWeightConfig(
    val formWeight: Double = 0.25,
    val eloWeight: Double = 0.20,
    val oddsWeight: Double = 0.20,
    val goalsWeight: Double = 0.15,
    val h2hWeight: Double = 0.10,
    val restAdvantageWeight: Double = 0.10,
    val sixthSignalMode: SixthSignalMode = SixthSignalMode.REST_ADVANTAGE,
    val restAdvantageSensitivity: Double = 3.0,
    val restAdvantageMaxShift: Double = 0.09,
    // Baseline parameters for benchmark
    val homeAdvantageProbHome: Double = 0.46,
    val homeAdvantageProbDraw: Double = 0.26,
    val homeAdvantageProbAway: Double = 0.28,
    ...
)
```

### 7.2. Điều phối trong `PredictMatchOutcomeUseCase`
```kotlin
val sixthSignal = when (config.sixthSignalMode) {
    SixthSignalMode.REST_ADVANTAGE -> RestAdvantageSignalTransformer.transform(
        homePreviousMatch = homePrev,
        awayPreviousMatch = awayPrev,
        targetKickoffTime = context.matchStartTimeDate,
        config = config
    )
    SixthSignalMode.HOME_ADVANTAGE -> HomeAdvantageSignalTransformer.transform(
        isNeutralVenue = context.isNeutralVenue,
        config = config
    )
}
```

### Ưu điểm kiến trúc:
1. **Zero Production Overhead:** Production chạy 100% với `SixthSignalMode.REST_ADVANTAGE` mặc định.
2. **Không Duplicate Pipeline:** Cùng một `PredictMatchOutcomeUseCase` có thể chạy benchmark bằng cách truyền `config.copy(sixthSignalMode = SixthSignalMode.HOME_ADVANTAGE)`.
3. **Deterministic Evaluation:** Đảm bảo 5 signals còn lại (Form, Elo, Odds, Goals, H2H) và bộ dữ liệu đầu vào hoàn toàn đồng nhất khi so sánh hiệu năng giữa hai mô hình.

---

## 8. File-Level Change Plan

### A. Các file được tạo mới (New Files)
1. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/RestAdvantageSignalTransformer.kt`: Triển khai transformer chính.
2. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/RestAdvantageSignalTransformerTest.kt`: Bộ Unit Test chuyên sâu cho Rest Advantage.

### B. Các file được sửa đổi (Modified Files)
1. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt`: Thêm `SixthSignalMode`, `restAdvantageWeight`, `restAdvantageSensitivity`, `restAdvantageMaxShift`.
2. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/MatchPredictionContext.kt`: Thêm `matchStartTimeDate: String? = null`.
3. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionEvidence.kt`: Đổi `homeAdvantage` thành `restAdvantage`.
4. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt`: Tích hợp `RestAdvantageSignalTransformer` và ánh xạ `PredictionEvidence`.
5. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt`: Truyền `matchStartTimeDate = selectedMatch.startTimeDate` vào `MatchPredictionContext`.
6. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt`: Truyền `matchStartTimeDate = target.startTimeDate`.
7. `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt`: Truyền `matchStartTimeDate = match.startTimeDate`.
8. `app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/components/PredictionEvidenceComponents.kt`: Cập nhật thẻ UI từ Home Advantage sang Rest Advantage.
9. `app/src/main/res/values/strings.xml`, `values-en/strings.xml`, `values-vi/strings.xml`: Cập nhật chuỗi giao diện đa ngôn ngữ.

### C. Các file test được cập nhật (Updated Test Files)
1. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCaseTest.kt`: Cập nhật assert sang `evidence.restAdvantage`.
2. `core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfigTest.kt`: Kiểm thử validate cấu hình mới.

---

## 9. Testing Strategy

### 9.1. Unit Test Suite: `RestAdvantageSignalTransformerTest`
- **Zero difference:** `deltaRest = 0.0` $\to P = [0.37, 0.26, 0.37]$, `weight = 0.10`.
- **Symmetry:** `transform(+x)` và `transform(-x)` có $P_{\text{Home}}(+x) == P_{\text{Away}}(-x)$ và $P_{\text{Away}}(+x) == P_{\text{Home}}(-x)$.
- **Small differences:** $\pm 1.0$ ngày $\to P \approx [0.3989, 0.26, 0.3411]$.
- **Midweek Cup difference:** $\pm 3.0$ ngày $\to P \approx [0.4385, 0.26, 0.3015]$.
- **One week difference:** $\pm 7.0$ ngày $\to P \approx [0.4583, 0.26, 0.2817]$.
- **Extreme difference / Saturation:** $\pm 30.0$ ngày, $\pm 100.0$ ngày $\to P \in [0.28, 0.46]$, không vượt ngưỡng.
- **Missing Data Handling:**
  - `homePreviousMatch == null` $\to$ Neutral fallback $[0.37, 0.26, 0.37]$.
  - `awayPreviousMatch == null` $\to$ Neutral fallback $[0.37, 0.26, 0.37]$.
  - Cả 2 đều `null` $\to$ Neutral fallback $[0.37, 0.26, 0.37]$.
- **Temporal Safety check:** `prevKickoff >= targetKickoff` $\to$ Fallback an toàn.
- **Probability Invariants:** $P(H) + P(D) + P(A) == 1.0$ với mọi input ($1\text{e-}6$ tolerance).

### 9.2. Integration & Regression Tests
- **PredictMatchOutcomeUseCaseTest**: Xác nhận pipeline có đúng 6 signals, tổng trọng số $1.0$, `evidence.restAdvantage` chứa đầy đủ metadata (`homeRestDays`, `awayRestDays`, `deltaRestDays`).
- **Regression Invariant**: Đảm bảo 5 signals (Elo, Form, Odds, Goals, H2H) và thuật toán `WeightedScorer` không bị thay đổi logic hay sai lệch kết quả.

---

## 10. Benchmark Preparation (Phase R4 Preview)

Sau khi hoàn tất implementation trong Phase tiếp theo, một phase Benchmark độc lập sẽ được kích hoạt để so sánh đối chiếu:
- **Tập dữ liệu:** Đánh giá trên tập Daily Backtest thực tế và offline test set.
- **Quy trình:**
  1. Chạy `RunDailyBacktestUseCase` với `sixthSignalMode = HOME_ADVANTAGE` $\to$ Ghi nhận Metrics A.
  2. Chạy `RunDailyBacktestUseCase` với `sixthSignalMode = REST_ADVANTAGE` $\to$ Ghi nhận Metrics B.
  3. Đối chiếu: Accuracy, Confusion Matrix, Home Recall, Draw Recall, Away Recall, và độ ổn định phân phối.

---

## 11. Risks & Mitigations

| Rủi ro kỹ thuật | Mức độ | Biện pháp giảm thiểu |
| :--- | :---: | :--- |
| **Thiếu ngày bắt đầu của trận mục tiêu (`matchStartTimeDate == null`)** | Thấp | Fallback an toàn về Neutral Baseline $[0.37, 0.26, 0.37]$. |
| **Lỗi parse timestamp thời gian** | Rất thấp | Tái sử dụng `PreMatchOddsSelector.parseKickoffEpochSeconds` đã được test đầy đủ với mọi định dạng ISO / SQL. |
| **Vòng mở màn mùa giải thiếu lịch sử** | Trung bình | Option A Neutral Fallback tự động xử lý mượt mà, gán `isAvailable = false`. |
| **Xung đột UI khi hiển thị Evidence** | Thấp | Cập nhật đồng bộ `PredictionEvidenceComponents` và bộ resource strings Triple-locale (`values`, `values-en`, `values-vi`). |

---

## 12. Non-Scope (Ràng buộc Nghiêm ngặt)

- **KHÔNG** thay đổi trọng số của 5 tín hiệu còn lại (Odds 20%, Elo 20%, Form 25%, Goals 15%, H2H 10%).
- **KHÔNG** thay đổi Draw decomposition logic hay tăng xác suất Hòa nhân tạo trong phase này.
- **KHÔNG** chỉnh sửa database schema SQLite hay Room Migration.
- **KHÔNG** viết mã nguồn production trong phase R3 (Planning only).

---

## 13. Exit Criteria của Phase R3

- [x] Đã xác định toàn diện dependency của Home Advantage và kế hoạch phân loại A/B/C/D.
- [x] Đã chốt kiến trúc bảo tồn Home Advantage làm baseline benchmark thông qua `SixthSignalMode`.
- [x] Đã xác định chi tiết Rest Data Flow $O(1)$ RAM access không gây rò rỉ thời gian.
- [x] Đã lập danh sách chi tiết các file cần tạo, sửa đổi và kiểm thử.
- [x] Đã lập Test Specification cho Unit, Integration và Regression tests.
- [x] Đã tạo đầy đủ Implementation Plan và Issue tài liệu hóa.
- [x] Không có bất kỳ file mã nguồn production nào bị chỉnh sửa.
