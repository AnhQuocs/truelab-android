# Báo Cáo Triển Khai: Domain D4.2 — Prediction Backtest Engine

> **Module**: `:core:domain`  
> **Trạng thái**: Hoàn thành  
> **Thuộc kế hoạch**: `docs/plans/domain-d4-plan.md` (Domain D4.2)  
> **Kiểm thử**: 14/14 tests mới PASS — Toàn bộ hệ thống: 350/350 tests PASS  

---

## 1. Tổng Quan Mục Tiêu & Kiến Trúc

Sub-phase **Domain D4.2** thiết lập công cụ kiểm thử ngược lịch sử (**Prediction Backtest Engine**) chạy thuần túy trên Pure Kotlin/JVM tại `:core:domain`.

Mục tiêu cốt lõi:
- Chạy `PredictMatchOutcomeUseCase` trên tập trận lịch sử đã kết thúc (`isEnded == true` và có tỷ số hợp lệ).
- So sánh `Predicted Outcome` vs `Actual Outcome` của từng trận.
- Tổng hợp độ chính xác và ma trận nhầm lẫn thông qua `CalculateEvaluationMetricsUseCase` (D4.1).
- **Tuyệt đối ngăn ngừa Temporal Data Leakage** (Rò rỉ dữ liệu tương lai vào bối cảnh dự đoán).

---

## 2. Pipeline Xử Lý Chi Tiết

```text
Input Matches (historical list)
   │
   ▼
1. Filter Valid Matches (isEnded == true && homeScore != null && awayScore != null)
   │
   ▼
2. Strict Chronological Sort (startTimeDate ASC, matchId ASC)
   │
   ▼
3. For each match T_i in chronological order:
   ├─ Build Temporal-Safe Historical Context:
   │    ├─ Filter past matches strictly before T_i (startTimeDate < T_i.startTimeDate)
   │    ├─ Filter homeRecentMatches: max 5 past matches of homeTeam
   │    ├─ Filter awayRecentMatches: max 5 past matches of awayTeam
   │    ├─ Filter h2hMatches: max 5 past matches between homeTeam & awayTeam
   │    ├─ Lookup team Elo (Home & Away) from teamEloMap
   │    └─ Lookup pre-match Odds from matchOddsMap
   │
   ├─ Execute PredictMatchOutcomeUseCase(context)
   │    └─ Returns PredictionResult (homeWinProb, drawProb, awayWinProb, predictedOutcome, confidenceScore)
   │
   ├─ Derive Actual Outcome:
   │    ├─ homeScore > awayScore  => HOME_WIN
   │    ├─ homeScore == awayScore => DRAW
   │    └─ homeScore < awayScore  => AWAY_WIN
   │
   └─ Build BacktestMatchRecord (matchId, date, teams, predicted, actual, probs, confidence, isCorrect)
   │
   ▼
4. Aggregate Evaluation Metrics:
   ├─ Collect List<Pair<String, String>> (predicted, actual)
   ├─ Call CalculateEvaluationMetricsUseCase(pairs) => ModelEvaluationResult
   └─ Compute correctMatches count
   │
   ▼
Output: PredictionBacktestResult
```

---

## 3. Cơ Chế Chống Rò Rỉ Dữ Liệu Thời Gian (Temporal Data Leakage Prevention)

### 3.1. Ranh Giới Thời Gian Tuyệt Đối
Trong mỗi bước dự đoán trận $T_i$:
- **Bất biến**: Mọi dữ liệu bối cảnh (Recent Form, H2H) chỉ được trích xuất từ tập các trận $M_k$ thỏa mãn quan hệ thứ tự nghiêm ngặt về mặt thời gian:
  $$\text{startTimeDate}(M_k) < \text{startTimeDate}(T_i)$$
- Trận đang xét $T_i$, các trận diễn ra cùng thời điểm ($\text{startTimeDate}(M_k) == \text{startTimeDate}(T_i)$) và toàn bộ các trận diễn ra sau $T_i$ **tuyệt đối không xuất hiện** trong `homeRecentMatches`, `awayRecentMatches`, hoặc `h2hMatches`.
- Loại bỏ hoàn toàn giả định sai lầm dùng `matchId` làm tie-breaker cho thứ tự thi đấu giữa các trận cùng timestamp. Chỉ có timestamp thực tế mới quyết định tính lịch sử. Hàm kiểm tra `isStrictlyBefore(matchA, matchB)` tuân thủ nghiêm ngặt `matchA.startTimeDate < matchB.startTimeDate`.

### 3.2. Form Lịch Sử
- Lọc danh sách các trận quá khứ của đội nhà / đội khách (sắp xếp giảm dần theo thời gian, lấy tối đa `maxFormMatches = 5` trận gần nhất trước $T_i$).

### 3.3. H2H Lịch Sử
- Lọc các trận đối đầu trực tiếp giữa đúng hai đội đã diễn ra trước thời điểm $T_i$ (lấy tối đa `maxH2HMatches = 5` trận gần nhất).

---

## 4. Phân Tích & Giới Hạn Hiện Tại (Known Limitations / GAP)

### 4.1. Odds Handling
- **Hiện trạng**: Domain model `OddsRecordItem` hiện chưa có trường timestamp snapshot để phân biệt giữa opening odds, live odds và closing odds.
- **Giải pháp trong D4.2**: `BacktestPredictionUseCase` nhận `matchOddsMap: Map<Long, OddsRecordItem>` được chuẩn bị trước. Nếu một trận không có odds trong map, bối cảnh sẽ nhận `latestOdds = null` và 6 signal transformers sẽ tự động fallback sang default neutral signal (33.33% / 33.33% / 33.34%) theo đúng contract của `OddsSignalTransformer`.
- **GAP Ghi nhận**: Khi có module Data Sync/Storage hoàn chỉnh cho historical odds time-series, cần bổ sung snapshot timestamp hợp lệ trước trận đấu.

### 4.2. Elo Rating Handling
- **Hiện trạng**: `teamEloMap: Map<Int, Double>` nhận vào snapshot Elo hiện tại hoặc điểm Elo đầu vào cố định. Pipeline D4.2 chưa thực hiện dynamic chronological rolling Elo recalculation sau mỗi trận backtest.
- **GAP Ghi nhận**: Sẽ được mở rộng ở sub-phase tiếp theo nếu cần rolling Elo engine mô phỏng diễn biến xếp hạng qua từng vòng đấu.

---

## 5. Danh Mục Models & UseCase Mới

| Model / UseCase | Vị Trí | Mô Tả |
|:---|:---|:---|
| [`BacktestMatchRecord`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt) | `:core:domain` | Record chi tiết từng trận backtest (matchId, date, teams, predicted, actual, probs, confidence, isCorrect). |
| [`PredictionBacktestResult`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt) | `:core:domain` | Kết quả tổng hợp (evaluationResult, records, totalMatches, correctMatches). |
| [`BacktestPredictionUseCase`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt) | `:core:domain` | Orchestrator chính thực thi pipeline backtest chống rò rỉ dữ liệu. |

---

## 6. Kết Quả Kiểm Thử & Xác Thực Toàn Diện

### 6.1. Unit Test Matrix (`BacktestPredictionUseCaseTest`) — 14/14 PASS
1. `backtest normal historical dataset produces valid results`: Chạy dataset chuẩn và kiểm tra kết quả.
2. `empty dataset returns empty backtest result`: Tập rỗng trả về kết quả rỗng không crash.
3. `unended matches are filtered out`: Trận chưa kết thúc bị lọc bỏ.
4. `matches with missing scores are filtered out`: Trận thiếu điểm số bị loại bỏ.
5. `current match being evaluated is NOT included in its own historical context`: Trận đang xét không tự làm dữ liệu quá khứ của chính nó.
6. `matches occurring AFTER evaluated match are NOT used`: Trận tương lai không xuất hiện trong form/H2H.
7. `matches occurring BEFORE evaluated match ARE used`: Trận quá khứ được đưa vào form/H2H đầy đủ.
8. `future H2H matches are NOT used`: Đối đầu tương lai không bị rò rỉ.
9. `future form matches are NOT used`: Phong độ tương lai không bị rò rỉ.
10. `prediction result mapped correctly to BacktestMatchRecord`: Tỷ lệ xác suất và confidence được map chính xác.
11. `actual outcome derived correctly from score`: Suy diễn kết quả thực tế từ tỉ số (Home Win, Draw, Away Win).
12. `evaluationResult matches records count and distribution`: ModelEvaluationResult khớp toàn bộ tập bản ghi.
13. `correctMatches count matches actual correct predictions`: Số lượng dự đoán đúng khớp thực tế.
14. `same timestamp matches handled deterministically with id tiebreaker`: Xử lý ổn định và tất định các trận có cùng timestamp.

### 6.2. Full Regression Baseline

```text
Module :core:algorithm : 122/122 passed (0 failures)
Module :core:domain    : 182/182 passed (0 failures) [156 gốc + 12 D4.1 + 14 D4.2]
Module :app            :  46/46  passed (0 failures)
--------------------------------------------------------------------------------
TỔNG CỘNG              : 350/350 passed (100% SUCCESS)
assembleDebug          : BUILD SUCCESSFUL
```

---

## 7. Trạng Thái Git & File Thay Đổi

- **Chưa commit / Chưa push** (Tuân thủ yêu cầu dừng lại chờ Code Review).
- File thay đổi:
  - `M core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt`
  - `A core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt`
  - `A core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCaseTest.kt`
  - `A docs/reports/domain-d4.2-backtest.md`
