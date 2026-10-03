# Audit Report: Rest Advantage Implementation (Phase R5)

## 1. Executive Summary

| Metric | Details |
| :--- | :--- |
| **Audit Phase** | Phase R5 – Code Review & Architecture Audit |
| **Target Implementation** | Phase R4 – Rest Advantage Replacement |
| **Status** | **PASS** |
| **Production 6th Signal** | `Rest Advantage` (Weight: 10%, Sensitivity: 3.0 days, MaxShift: 0.09) |
| **Evaluation Baseline** | `Home Advantage` preserved via `SixthSignalMode.HOME_ADVANTAGE` |
| **Temporal Safety** | Strict $T_{prev} < T_{target}$ invariant verified across all callers |
| **Test Suite Status** | 123/123 tests passed (`:core:algorithm`, `:core:domain`, `:core:ui`, `:core:data`, `:app`) |
| **Build Status** | `./gradlew assembleDebug` SUCCESSFUL |

---

## 2. Scope & Files Inspected

### 2.1 Scope
Audit toàn diện việc thay thế `Home Advantage` bằng `Rest Advantage` trong Production Prediction Pipeline theo các cam kết kiến trúc tại [rest-advantage-implementation-plan.md](../plans/rest-advantage-implementation-plan.md) và [rest-advantage-mathematical-modeling.md](./rest-advantage-mathematical-modeling.md).

### 2.2 Files Inspected
1. **Domain Logic & Transformers**:
   - [`RestAdvantageSignalTransformer.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/RestAdvantageSignalTransformer.kt)
   - [`HomeAdvantageSignalTransformer.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformer.kt)
   - [`PredictMatchOutcomeUseCase.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt)
   - [`PredictionWeightConfig.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt)
   - [`MatchPredictionContext.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/MatchPredictionContext.kt)
   - [`PredictionEvidence.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionEvidence.kt)
2. **Evaluation & Backtest Engines**:
   - [`RunDailyBacktestUseCase.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt)
   - [`BacktestPredictionUseCase.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt)
3. **Presentation & UI**:
   - [`PredictionViewModel.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt)
   - [`PredictionEvidenceComponents.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/components/PredictionEvidenceComponents.kt)
   - `strings.xml` (Default, EN, VI)
4. **Unit & Integration Test Suites**:
   - [`RestAdvantageSignalTransformerTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/RestAdvantageSignalTransformerTest.kt)
   - [`PredictMatchOutcomeUseCaseTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCaseTest.kt)
   - [`PredictionWeightConfigTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfigTest.kt)
   - [`PredictionDistributionOfflineTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/PredictionDistributionOfflineTest.kt)
   - [`PredictionViewModelTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModelTest.kt)

---

## 3. Rest History Flow Verification

### 3.1 Trace Flow
```text
Data Source (Room/Repository)
   │
   ├── [PredictionViewModel]: allMatches.filter { it.isEnded && it.id != selectedMatch.id && it.startTimeDate < targetTime }.sortedByDescending { it.startTimeDate }
   ├── [RunDailyBacktestUseCase]: historicalEndedMatches.filter { it.id != target.id && it.startTimeDate < targetTime }.sortedByDescending { it.startTimeDate }
   └── [BacktestPredictionUseCase]: chronological sequential insertion -> getRecentMatches(history) yields reversed index [size - 1 downTo start] (newest first)
   │
   ▼
MatchPredictionContext(homeRecentMatches, awayRecentMatches, matchStartTimeDate)
   │
   ▼
PredictMatchOutcomeUseCase
   ├── homePrev = homeRecentMatches.firstOrNull { it.isEnded && it.id != context.matchId }
   └── awayPrev = awayRecentMatches.firstOrNull { it.isEnded && it.id != context.matchId }
   │
   ▼
RestAdvantageSignalTransformer.transform(homePrev, awayPrev, targetKickoffTime)
   ├── Strict epoch parsing via PreMatchOddsSelector.parseKickoffEpochSeconds
   ├── Invariant check: homePrevEpoch < targetEpoch && awayPrevEpoch < targetEpoch
   └── Delta calculation: deltaRest = ((targetEpoch - homePrevEpoch) - (targetEpoch - awayPrevEpoch)) / 86400.0
```

### 3.2 Verification Results
- **Sorting Guaranteed**: Tất cả caller sites đều đảm bảo danh sách `homeRecentMatches` và `awayRecentMatches` được sắp xếp giảm dần theo thời gian (`sortedByDescending { it.startTimeDate }`), do đó `firstOrNull()` luôn trỏ tới trận đấu kết thúc gần nhất trong quá khứ.
- **Strict Exclusion**: Trận đấu hiện tại (`id == matchId`), trận chưa kết thúc (`!isEnded`), trận tương lai (`startTimeDate >= targetTime`) đều bị loại bỏ triệt để.

---

## 4. Temporal Leakage Audit

| Caller / Component | Filter Rule | Strict Check Before Kickoff | Leakage Risk |
| :--- | :--- | :--- | :--- |
| **PredictionViewModel** | `it.isEnded && it.id != selectedMatch.id && it.startTimeDate < targetTime` | **YES** | Zero |
| **RunDailyBacktestUseCase** | `it.id != target.id && it.startTimeDate < targetTime` (từ `historicalEndedMatches`) | **YES** | Zero |
| **BacktestPredictionUseCase** | Duyệt mốc thời gian tăng dần, chỉ nạp lịch sử các mốc $T < T_{current}$ | **YES** | Zero |
| **RestAdvantageSignalTransformer** | `homePrevEpoch >= targetEpoch \|\| awayPrevEpoch >= targetEpoch` $\rightarrow$ Neutral | **YES** (Double Defense) | Zero |

---

## 5. Mathematical Contract Verification

Công thức triển khai trong [`RestAdvantageSignalTransformer.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/RestAdvantageSignalTransformer.kt):
$$\Delta \text{Rest} = \text{homeRestDays} - \text{awayRestDays}$$
$$P(\text{Home}) = 0.37 + 0.09 \cdot \tanh\left(\frac{\Delta \text{Rest}}{3.0}\right)$$
$$P(\text{Draw}) = 0.26$$
$$P(\text{Away}) = 0.37 - 0.09 \cdot \tanh\left(\frac{\Delta \text{Rest}}{3.0}\right)$$

### Audit Checklist:
- [x] **Sensitivity**: $3.0$ ngày (`config.restAdvantageSensitivity = 3.0`).
- [x] **Max Shift**: $0.09$ (`config.restAdvantageMaxShift = 0.09`).
- [x] **Neutral Baseline**: $\Delta \text{Rest} = 0 \rightarrow [0.37, 0.26, 0.37]$.
- [x] **Missing History Fallback**: Khi thiếu trận đấu trước đó của một hoặc cả hai đội $\rightarrow$ Trả về trung tính $[0.37, 0.26, 0.37]$ với `isAvailable = false`.
- [x] **Probability Normalization**: $P(H) + P(D) + P(A) = 1.000000$ với mọi $\Delta \text{Rest} \in (-\infty, +\infty)$.
- [x] **Probability Bounds**: $P(H), P(A) \in [0.28, 0.46]$, $P(D) = 0.26$.
- [x] **Symmetry**: $P(H \mid +\delta) = P(A \mid -\delta)$ và $P(A \mid +\delta) = P(H \mid -\delta)$.
- [x] **Monotonicity**: Đạo hàm $\frac{dP(H)}{d\Delta} = \frac{0.09}{3.0}\operatorname{sech}^2(\Delta/3.0) > 0$ bảo đảm đơn điệu tăng nghiêm ngặt theo $\Delta \text{Rest}$.

---

## 6. Production Signal & Pipeline Integration

### 6.1 Danh sách 6 Tín hiệu trong Production Pipeline
1. **Odds 1X2**: Trọng số $0.20$ ($20\%$)
2. **Elo Rating**: Trọng số $0.20$ ($20\%$)
3. **Form (Phong độ)**: Trọng số $0.25$ ($25\%$)
4. **Goals Expected**: Trọng số $0.15$ ($15\%$)
5. **Head-to-Head (H2H)**: Trọng số $0.10$ ($10\%$)
6. **Rest Advantage**: Trọng số $0.10$ ($10\%$)

**Tổng trọng số**: $0.20 + 0.20 + 0.25 + 0.15 + 0.10 + 0.10 = 1.00$ ($100\%$).
- Không tạo thêm tín hiệu thứ 7.
- `Home Advantage` không nằm trong danh sách tín hiệu mặc định của production pipeline.

---

## 7. HomeAdvantage Baseline Preservation

- [`HomeAdvantageSignalTransformer.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformer.kt) được giữ nguyên vẹn 100% công thức cũ $[0.46, 0.26, 0.28]$ (hoặc $[0.37, 0.26, 0.37]$ khi neutral).
- [`PredictionWeightConfig.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt) hỗ trợ `sixthSignalMode: SixthSignalMode = SixthSignalMode.REST_ADVANTAGE` (mặc định) và `SixthSignalMode.HOME_ADVANTAGE`.
- Khi thiết lập `config.copy(sixthSignalMode = SixthSignalMode.HOME_ADVANTAGE)`, `PredictMatchOutcomeUseCase` chuyển đổi chính xác sang chế độ Home Advantage để phục vụ đối soát/benchmark Phase R7.

---

## 8. Evidence & UI Verification

- **Data Structure**: `PredictionEvidence.restAdvantage` thay thế hoàn toàn `homeAdvantage`.
- **Metadata**:
  - `homeRestDays`: Số ngày nghỉ đội nhà (e.g. `5.0d`).
  - `awayRestDays`: Số ngày nghỉ đội khách (e.g. `3.0d`).
  - `deltaRestDays`: Chênh lệch ngày nghỉ (e.g. `+2.0d`).
  - `isAvailable`: Đánh dấu `false` khi thiếu dữ liệu hoặc không hợp lệ.
- **UI Component**: [`PredictionEvidenceComponents.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/components/PredictionEvidenceComponents.kt) render đúng tiêu đề `Rest Advantage (10%)`, badge chênh lệch ngày nghỉ, và chi tiết ngày nghỉ của từng đội.
- **Đa ngôn ngữ**: Đã bổ sung string resources đầy đủ trong `values/strings.xml`, `values-en/strings.xml`, và `values-vi/strings.xml`.

---

## 9. `isNeutralVenue` Cleanup Verification

| Token / Reference | File Location | Classification | Status / Note |
| :--- | :--- | :--- | :--- |
| `isNeutralVenue` | `MatchPredictionContext.kt` | **Production Field** | **ĐÃ XÓA** khỏi Context production |
| `isNeutralVenue` | `PredictionViewModel.kt` | **Caller Parameter** | **ĐÃ XÓA** |
| `isNeutralVenue` | `RunDailyBacktestUseCase.kt` | **Caller Parameter** | **ĐÃ XÓA** |
| `isNeutralVenue` | `BacktestPredictionUseCase.kt` | **Caller Parameter** | **ĐÃ XÓA** |
| `isNeutralVenue` | `PredictMatchOutcomeUseCase.kt#L112` | **Baseline Adapter** | **GIỮ LẠI** (truyền `false` cho nhánh Benchmark `HOME_ADVANTAGE`) |
| `isNeutralVenue` | `HomeAdvantageSignalTransformer.kt#L12` | **Baseline Transformer** | **GIỮ LẠI** cho Benchmark Baseline |
| `isNeutralVenue` | `HomeAdvantageSignalTransformerTest.kt` | **Test Baseline** | **GIỮ LẠI** |

---

## 10. Regression Analysis

- **Các Signal Khác**: Odds, Elo, Form, Goals, H2H giữ nguyên 100% logic tính toán và trọng số.
- **Draw Logic**: Giữ nguyên $0.26$ baseline draw probability, không thêm heuristic can thiệp argmax.
- **WeightedScorer**: Giữ nguyên thuật toán Linear Mixture của `PredictionEngine` / `DefaultWeightedScorer`.
- **Database & Network**: Không có thay đổi Room Entity, DAO, Migration, Retrofit API endpoints. Không có N+1 query.

---

## 11. Test Coverage Summary

- **[`RestAdvantageSignalTransformerTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/RestAdvantageSignalTransformerTest.kt)** (15 test cases):
  1. `zero delta yields neutral probabilities`
  2. `positive delta favors home team`
  3. `negative delta favors away team`
  4. `plus minus 1 day evaluation matches tanh contract`
  5. `plus minus 3 days evaluation matches tanh contract`
  6. `extreme positive delta saturates cleanly without exceeding 0_46`
  7. `extreme negative delta saturates cleanly without dropping below 0_28`
  8. `missing home previous match returns neutral baseline`
  9. `missing away previous match returns neutral baseline`
  10. `missing both previous matches returns neutral baseline`
  11. `symmetry holds for all x where Home(plus x) equals Away(minus x)`
  12. `monotonicity holds strictly across delta range`
  13. `probability sum is always exactly 1_0`
  14. `probability bounds are strictly respected in [0_28, 0_46]`
  15. `temporal safety prevents future match leakage and respects kickoff order`
- **[`PredictMatchOutcomeUseCaseTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCaseTest.kt)**: Kiểm tra pipeline 6 signals đầy đủ, benchmark `HOME_ADVANTAGE` mode, missing signals fallback, và data leakage prevention.
- **[`PredictionWeightConfigTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfigTest.kt)**: Kiểm tra cấu hình trọng số 6 signals, độ nhạy sensitivity/maxShift, và validation bounds.
- **[`PredictionViewModelTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModelTest.kt)** & **[`BacktestViewModelTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/app/src/test/java/dev/anhquocs/truelab/feature/backtest/presentation/viewmodel/BacktestViewModelTest.kt)**: Kiểm tra tích hợp toàn diện giao diện và orchestration.

---

## 12. Findings

1. **Khả năng đối soát Baseline hoàn hảo**: Việc bổ sung `SixthSignalMode` trong `PredictionWeightConfig` cho phép chạy so sánh $\text{A/B}$ giữa `REST_ADVANTAGE` và `HOME_ADVANTAGE` trên cùng một tập dữ liệu đầu vào mà không cần nhân bản usecase hay pipeline.
2. **Cơ chế an toàn 2 lớp (Double Defense)**: Ngay cả khi caller truyền nhầm trận đấu trong tương lai, `RestAdvantageSignalTransformer` vẫn kiểm tra `homePrevEpoch >= targetEpoch || awayPrevEpoch >= targetEpoch` để tự động trả về neutral $[0.37, 0.26, 0.37]$ với `isAvailable = false`, triệt tiêu hoàn toàn rủi ro Data Leakage.

---

## 13. Final Verdict

# **PASS**

Toàn bộ các tiêu chuẩn toán học, kiến trúc Clean Architecture đa module, an toàn thời gian và bảo toàn baseline đối soát đều được đáp ứng đầy đủ và chính xác.
