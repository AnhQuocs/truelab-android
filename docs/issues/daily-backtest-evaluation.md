# Issue: Chuyển Đổi Kiến Trúc Backtest Sang Daily On-Demand Evaluation

## 1. Context

Ứng dụng TrueLab sở hữu hệ thống dự đoán kết quả bóng đá (Prediction Pipeline) kết hợp 6 tín hiệu: Phong độ (Form - 25%), Elo (20%), Bàn thắng (Goals - 15%), Tỷ lệ cược (Odds - 20%), Đối đầu (H2H - 10%), Lợi thế sân nhà (Home Advantage - 10%).

Hiện tại, ứng dụng có màn hình Backtest chạy hàng loạt trên gần 30.000 trận đấu lịch sử trong Room database. Tuy nhiên, tính năng này đang gặp phải một số điểm bất cập về mặt kiến trúc và tính nhất quán của dữ liệu.

---

## 2. Problem

1. **Độ phủ Odds lịch sử không đồng nhất**:
   - Dữ liệu tỷ lệ cược (Odds) hiện tại được nạp theo cơ chế On-Demand khi người dùng xem trận đấu.
   - Tập dữ liệu 30k lịch sử ban đầu không có Odds, dẫn đến việc phần lớn các trận trong Backtest cũ bị thiếu tín hiệu Odds và chỉ chạy 5 tín hiệu còn lại.
2. **Không phản ánh đúng Prediction Pipeline thực tế**:
   - Người dùng trải nghiệm tính năng Dự đoán (Prediction) với đầy đủ 6 tín hiệu (có nạp Odds on-demand), nhưng Backtest cũ lại không phản ánh trung thực năng lực của mô hình 6 tín hiệu này.
3. **Quản lý bộ nhớ và hiệu năng UI**:
   - Việc tải toàn bộ 30.000 trận đấu vào bộ nhớ của `BacktestViewModel` gây lãng phí tài nguyên và tạo ra trạng thái chờ đợi lâu với loading spinner đơn điệu.

---

## 3. Root Cause / Findings

- Kiến trúc Backtest cũ được thiết kế cho kịch bản "Offline Dataset Static Replay", giả định toàn bộ dữ liệu (bao gồm cả Odds) đã có sẵn từ trước trong database cục bộ.
- Khi chuyển đổi sang cơ chế nạp Odds động (On-Demand Odds Ingestion), việc gọi API hàng loạt cho 30.000 trận là bất khả thi (nguy cơ vượt quá giới hạn API quota và thời gian chờ quá lâu).
- Việc chia nhỏ phạm vi đánh giá theo ngày cụ thể (Daily FT Evaluation) giải quyết triệt để bài toán nạp Odds, giảm tải tài nguyên và cung cấp trải nghiệm đánh giá sinh động, thực tế hơn cho người dùng.

---

## 4. Proposed Solution

Chuyển đổi Backtest thành quy trình **"Daily / On-Demand Evaluation"**:
1. **Lựa chọn theo ngày**: Người dùng chọn một ngày cụ thể trong quá khứ và lấy danh sách các trận đấu đã kết thúc (Full-Time - FT) trong ngày đó (~20 đến ~100 trận).
2. **Nạp Odds On-Demand & Đảm bảo Tính toàn vẹn thời gian (Temporal Integrity)**:
   - Tự động kiểm tra và nạp Odds pre-match cho từng trận FT với cơ chế giới hạn luồng đồng thời (Concurrency Semaphore = 3).
   - Đảm bảo tất cả dữ liệu lịch sử (Form, Elo, H2H, Goals, Odds) đều diễn ra nghiêm ngặt trước thời điểm bắt đầu trận đấu (`changeTime < kickoff`, `startTimeDate < kickoff`).
3. **Tái sử dụng 100% Prediction Pipeline**:
   - Sử dụng trực tiếp `PredictMatchOutcomeUseCase` và 6 Signal Transformers hiện tại để đưa ra xác suất Thắng / Hòa / Thua.
4. **Đối soát & Đánh giá Hiệu năng**:
   - Lấy tỷ số FT thực tế để đối chiếu với kết quả dự đoán.
   - Tính toán Ma trận nhầm lẫn 3x3 (Confusion Matrix), Accuracy, Macro F1 và Thống kê tỷ lệ phủ Odds (Odds Coverage %).
5. **Trải nghiệm giao diện sống động (Animated Processing Experience)**:
   - Hiển thị Animated Processing Card cập nhật tiến độ $x / N$, tên trận đang phân tích và trạng thái từng bước (Context, Odds, Prediction, Evaluation).

---

## 5. Scope

- **Domain Layer**:
  - Tạo mới các models: `OddsCoverageStats`, `DailyBacktestProgressEvent`, `DailyBacktestResult` trong [`EvaluationModels.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt).
  - Triển khai `RunDailyBacktestUseCase` tích hợp giới hạn Concurrency và quản lý luồng đánh giá.
- **Presentation Layer**:
  - Cập nhật [`BacktestViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/viewmodel/BacktestViewModel.kt) hỗ trợ chọn ngày, lắng nghe Progress Flow và quản lý UI State.
  - Cập nhật [`BacktestVisualizerScreen.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/BacktestVisualizerScreen.kt) với Date Header, Animated Processing Card, Thẻ thống kê Odds Coverage và danh sách kết quả từng trận.
- **Unit Testing**:
  - Viết Unit Tests kiểm tra `RunDailyBacktestUseCase`, tính toán chỉ số và `BacktestViewModel`.

---

## 6. Acceptance Criteria

- [ ] Người dùng có thể chọn ngày và xem số lượng trận FT sẵn có.
- [ ] Bấm "Đánh giá" khởi chạy tiến trình mượt mà với Animated Processing Card thể hiện đúng tiến độ $x / N$.
- [ ] Tự động nạp Odds on-demand và đảm bảo 0% Temporal Data Leakage.
- [ ] Hiển thị đầy đủ Accuracy, Ma trận nhầm lẫn 3x3, Macro F1 và % Độ phủ Odds.
- [ ] Cho phép lọc danh sách trận theo "Tất cả", "Đúng", "Sai".
- [ ] Hỗ trợ nút "Hủy" tiến trình đang chạy an toàn.
- [ ] Toàn bộ Unit tests liên quan vượt qua thành công và build sạch không lỗi.

---

## 7. Non-Goals

- **Không thay thế Algorithm Benchmark**: Tính năng Benchmark thuật toán (Search, Sort, Stats) trên tập dữ liệu lớn vẫn được giữ nguyên độc lập.
- **Không tự ý sửa đổi mô hình / trọng số Prediction**: Không can thiệp sửa đổi thuật toán dự đoán chỉ để làm đẹp chỉ số ma trận nhầm lẫn.
- **Không lưu trữ vĩnh viễn kết quả Backtest**: Kết quả được quản lý in-memory trong UI State của ViewModel.

---

## 8. Related Components

- **Plan chi tiết**: [`daily-backtest-evaluation-plan.md`](../plans/daily-backtest-evaluation-plan.md)
- **Domain UseCases & Selectors**:
  - [`PredictMatchOutcomeUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt)
  - [`RunDailyBacktestUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt)
  - [`PreMatchOddsSelector.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/selector/PreMatchOddsSelector.kt)
  - [`CalculateDynamicEloUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateDynamicEloUseCase.kt)
  - [`CalculateEvaluationMetricsUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/CalculateEvaluationMetricsUseCase.kt)
- **Data Repositories**:
  - [`OddsRepository.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/repository/OddsRepository.kt)
  - [`MatchRepository.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/repository/MatchRepository.kt)
- **UI Components**:
  - [`BacktestViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/viewmodel/BacktestViewModel.kt)
  - [`BacktestVisualizerScreen.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/BacktestVisualizerScreen.kt)

---

## 9. Status

**Completed** (Đã hoàn thành triển khai mã nguồn, unit tests và xác thực build).

