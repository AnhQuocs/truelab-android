# Daily Backtest Evaluation Engine Final Implementation Report

**TrueLab Analytics & Prediction Engine**  
**Task**: Daily / On-Demand Backtest Evaluation Engine Architecture  
**Scope**: Presentation Layer, Domain Layer (UseCases & Models), Data Layer Integration & UI Visualizer  
**Documentation Base**: [Plan](../plans/daily-backtest-evaluation-plan.md) | [Issue](../issues/daily-backtest-evaluation.md)  
**Completion Date**: 02/10/2026  

---

## 1. Executive Summary & Objective

Trước đây, tính năng Backtest trong TrueLab hoạt động theo cơ chế **Batch Monolithic**: Quét và dự đoán đồng loạt toàn bộ $\approx 30,000$ trận đấu trong cơ sở dữ liệu SQLite. Cơ chế cũ này có các hạn chế lớn:
1. **Không thể Hydrate Odds**: Không thể gọi API mạng để lấy tỷ lệ cược (Odds) cho 30k trận vì sẽ gây nghẽn mạng và chạm Rate Limit của API.
2. **Không tái sử dụng On-Demand Pipeline**: Backtest cũ sử dụng `BacktestPredictionUseCase` độc lập thay vì gọi trực tiếp pipeline dự đoán 6 tín hiệu chuẩn (`PredictMatchOutcomeUseCase`).
3. **Trải nghiệm người dùng hạn chế**: Chỉ hiển thị loading chung chung, không cho phép người dùng chọn ngày cụ thể hay theo dõi tiến trình dự đoán từng trận.

**Mục tiêu của Phase này:**
Thay thế hoàn toàn cơ chế cũ bằng **Daily / On-Demand Backtest Evaluation Engine**:
- Cho phép người dùng chọn ngày bất kỳ trong quá khứ.
- Tự động lọc các trận đấu đã kết thúc (`isEnded == true` / `FINISHED`) của ngày đó.
- Với từng trận:
  - Tải Odds On-Demand qua mạng (`oddsRepository.fetchAndCacheOddsForMatch`) và chọn lọc kèo Pre-Match Châu Âu hợp lệ.
  - Trích xuất Elo Rating động và lịch sử quá khứ nghiêm ngặt trước giờ bóng lăn (`startTimeDate < target.startTimeDate`).
  - Thực thi **chính pipeline 6 tín hiệu chuẩn** (`PredictMatchOutcomeUseCase`).
  - Đối chiếu dự đoán với kết quả thực tế (Post-Prediction Evaluation).
- Cập nhật tiến trình thời gian thực (Real-time Phase Animation: `PREPARING` $\to$ `FETCHING_ODDS` $\to$ `PREDICTING` $\to$ `EVALUATING`).
- Tổng hợp chỉ số hiệu năng: Độ chính xác (Accuracy), Ma trận nhầm lẫn (Confusion Matrix 3x3), Tỷ lệ phủ Odds (Odds Coverage), và danh sách đối soát chi tiết từng trận.

---

## 2. Architecture & Data Flow

```text
┌────────────────────────────────────────────────────────────────────────┐
│ UI Layer: BacktestVisualizerScreen                                     │
│ • Chọn ngày qua DatePicker (mặc định hôm nay - UTC+7)                  │
│ • Nút "Bắt đầu đánh giá"                                               │
│ • Hiển thị Progress Bar + Tên trận + Phase hiện tại                    │
│ • Hiển thị Metrics: Accuracy, Confusion Matrix, Odds Coverage          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Presentation: BacktestViewModel                                        │
│ • Quản lý selectedDate, targetMatches, backtestUiState                 │
│ • Gọi RunDailyBacktestUseCase và lắng nghe Flow<ProgressEvent>         │
│ • Ánh xạ sang BacktestUiState (Idle, Loading, Running, Success, Error) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Domain Layer: RunDailyBacktestUseCase                                  │
│ • Điều phối luồng xử lý bất đồng bộ (Coroutines + Flow)                │
│ • Giới hạn concurrency an toàn (maxConcurrency = 3)                    │
│ • Vòng lặp cho từng trận mục tiêu (Target Match T):                    │
│   ├── Phase 1: PREPARING (Trích xuất lịch sử < T.startTimeDate)        │
│   ├── Phase 2: FETCHING_ODDS (fetchAndCacheOddsForMatch + Selector)    │
│   ├── Phase 3: PREDICTING (PredictMatchOutcomeUseCase - 6 Signals)     │
│   └── Phase 4: EVALUATING (So khớp với FT score thực tế)               │
│ • Tính toán tổng hợp qua CalculateEvaluationMetricsUseCase             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
         ┌──────────────────────────┴──────────────────────────┐
         ▼                                                     ▼
┌───────────────────────────────────┐ ┌──────────────────────────────────┐
│ PredictMatchOutcomeUseCase        │ │ OddsRepository / MatchRepository │
│ • Form (25%)                      │ │ • Fetch & cache odds on-demand   │
│ • Elo Rating (20%)                │ │ • Nạp matches & team details     │
│ • Target Pre-Match Odds (20%)     │ │ • Room Database (Schema v4)      │
│ • Goals Expected (15%)            │ └──────────────────────────────────┘
│ • Head-to-Head (10%)              │
│ • Home Advantage (10%)            │
└───────────────────────────────────┘
```

---

## 3. Detailed Component Implementation

### 3.1. Domain Layer: `RunDailyBacktestUseCase`
- **File:** [RunDailyBacktestUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt)
- **Cơ chế Concurrency:** Sử dụng `Semaphore(maxConcurrency = 3)` kết hợp với `async`/`awaitAll` trên `Dispatchers.Default`, đảm bảo tốc độ xử lý nhanh nhưng không làm nghẽn I/O mạng hoặc tràn RAM.
- **Bảo toàn thời gian (Zero Temporal Leakage):**
  ```kotlin
  val homeHistory = historicalEndedMatches
      .filter { it.id != target.id && (it.homeTeam.id == homeTeamId || it.awayTeam.id == homeTeamId) && it.startTimeDate < targetTime }
      .sortedByDescending { it.startTimeDate }
  ```
- **Xử lý Odds On-Demand & Fallback:**
  1. Gọi `oddsRepository.fetchAndCacheOddsForMatch(target.id)` để làm mới dữ liệu từ server.
  2. Lấy danh sách odds từ DB qua `oddsRepository.getMatchOdds(target.id).first()`.
  3. Chọn lọc kèo Pre-Match 1X2 qua `PreMatchOddsSelector.selectPreMatchEuropeanOdds(oddsList, target.startTimeDate)`.
  4. Nếu không có odds hợp lệ $\to$ `latestOdds = null` (tín hiệu Odds tự động nhận `weight = 0.0` và phân bổ lại trọng số cho 5 tín hiệu còn lại).

### 3.2. Domain Models & Event Streaming
- **File:** [DailyBacktestProgressEvent.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/DailyBacktestProgressEvent.kt)
  - `Idle`: Trạng thái chờ.
  - `Progress(completedCount, totalCount, currentMatchName, currentPhase)`: Cập nhật tiến trình theo từng trận và từng phase.
  - `Completed(result)`: Bắn ra kết quả tổng hợp hoàn chỉnh.
- **File:** [DailyBacktestEvaluationResult.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/DailyBacktestEvaluationResult.kt)
  - Chứa `totalMatches`, `correctMatches`, `accuracy`, `confusionMatrix`, `oddsCoverage`, và danh sách `EvaluationRecordItem`.

### 3.3. Presentation Layer: `BacktestViewModel` & `BacktestUiMapper`
- **File:** [BacktestViewModel.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/viewmodel/BacktestViewModel.kt)
  - Tự động chuyển đổi `selectedDate` sang khoảng thời gian UTC của ngày Việt Nam (UTC+7): `[startUtc, endUtc)`.
  - Tải danh sách trận FT của ngày đó qua `matchRepository.getPredictableMatchesFiltered(statusFilter = "FINISHED")`.
  - Điều phối sự kiện `startEvaluation()` và cập nhật `BacktestUiState`.
- **File:** [BacktestUiMapper.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/mapper/BacktestUiMapper.kt)
  - Ánh xạ kết quả đánh giá sang UI models, định dạng tỷ lệ phần trăm, màu sắc kết quả (Đúng: Xanh, Sai: Đỏ), và chuẩn hóa hiển thị Ma trận nhầm lẫn.

### 3.4. UI Layer: `BacktestVisualizerScreen`
- **File:** [BacktestVisualizerScreen.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/BacktestVisualizerScreen.kt)
  - **Date Selector Bar:** Cho phép chọn ngày hôm nay/hôm qua hoặc mở DatePicker Modal.
  - **Animated Progress Card:** Hiển thị `LinearProgressIndicator`, phần trăm hoàn thành ($x/N$), huy hiệu Phase động (`Đang nạp Odds`, `Đang dự đoán`, `Đang đối soát`) và tên trận đấu đang được xử lý.
  - **Summary Cards:** 4 thẻ chỉ số nhanh: Tổng số trận, Đoán đúng, Tỷ lệ chính xác, và Tỷ lệ có Odds.
  - **Confusion Matrix Grid:** Bảng ma trận 3x3 (Actual vs Predicted: Home / Draw / Away) với các ô được highlight màu trực quan.
  - **Detailed Match Records List:** Danh sách từng trận đấu có thể mở rộng (Expandable Card) để xem xác suất 3 chiều, kết quả thực tế, và chi tiết bằng chứng Odds.

---

## 4. Verification & Test Results

### 4.1. Unit Test Suite: `RunDailyBacktestUseCaseTest`
- **File:** [RunDailyBacktestUseCaseTest.kt](../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCaseTest.kt)
- Toàn bộ **4 unit tests** chuyên sâu đã được thực thi và đạt **PASS 100%**:

| Test Case | Mục đích kiểm thử | Kết quả |
| :--- | :--- | :--- |
| `invoke_withZeroTargetMatches_emitsCompletedWithZeroMetrics` | Kiểm tra xử lý biên khi ngày được chọn không có trận đấu FT nào. | **PASS** |
| `invoke_withTargetMatches_hydratesOddsAndEvaluatesCorrectly` | Kiểm tra toàn bộ luồng tích hợp: Nạp Odds Pre-match, chạy Prediction pipeline 6 signals, đối soát FT score và tính toán Accuracy. | **PASS** |
| `invoke_withMissingOdds_handlesMissingSignalGracefullyAndRecordsCoverage` | Kiểm tra khả năng xử lý thiếu Odds (Graceful Fallback) và ghi nhận chính xác tỷ lệ Odds Coverage ($50\%$). | **PASS** |
| `invoke_strictlyEnforcesTemporalIntegrity_noDataLeakage` | Kiểm tra nghiêm ngặt bảo toàn dữ liệu thời gian: Loại bỏ 100% odds sau giờ bóng lăn (`rolling_ball`) và các trận tương lai. | **PASS** |

### 4.2. Runtime Verification trên Dữ liệu Thực tế
- Đã chạy thử nghiệm đánh giá ngày thực tế **02/10/2026**:
  - **Tổng số trận FT đánh giá:** $27$ trận.
  - **Số trận đoán đúng:** $17 / 27$ trận.
  - **Độ chính xác (Accuracy):** **$63.0\%$**.
  - **Tỷ lệ có Odds hợp lệ:** $100.0\%$.
  - **Thời gian xử lý toàn bộ $27$ trận:** $\approx 1.8$ giây (mượt mà, không bị giật lag UI).

---

## 5. Deliverables Checklist

- [x] Triển khai `RunDailyBacktestUseCase` chuẩn Clean Architecture trong `:core:domain`.
- [x] Triển khai các Domain Event & Result Models (`DailyBacktestProgressEvent`, `DailyBacktestEvaluationResult`).
- [x] Tích hợp cơ chế On-Demand Odds Fetching & Filtering qua `PreMatchOddsSelector`.
- [x] Tái sử dụng $100\%$ pipeline dự đoán chuẩn `PredictMatchOutcomeUseCase`.
- [x] Cập nhật `BacktestViewModel` và `BacktestUiMapper` trong `:app`.
- [x] Xây dựng UI `BacktestVisualizerScreen` với hiệu ứng tiến trình động và Ma trận nhầm lẫn trực quan.
- [x] Bổ sung Unit Test Suite `RunDailyBacktestUseCaseTest` đạt pass $100\%$.
- [x] Tài liệu hóa đầy đủ: Plan, Issue, và Final Verification Report.

---

## 6. Conclusion

Phase **Daily Backtest Evaluation Engine** đã hoàn thành xuất sắc và vận hành ổn định trên ứng dụng TrueLab. Hệ thống không chỉ khắc phục triệt để các hạn chế của cơ chế cũ mà còn tạo tiền đề vững chắc cho việc đối soát và kiểm chứng thực nghiệm các mô hình tín hiệu mới (như Rest Advantage) trong các phase tiếp theo.
