# Issue: Tiếp Nhận & Hiển Thị Thông Tin Loạt Sút Luân Lưu (Penalty Shootout Ingestion & Presentation)

## 1. Title
**[Enhancement / Data Completeness] Tiếp nhận & hiển thị thông tin Penalty Shootout từ TrueScore API trên giao diện TrueLab**

---

## 2. Problem
Hiện tại trong TrueLab:
- Hệ thống chỉ lưu trữ và hiển thị hai trường tỷ số thời gian thi đấu chính thức `homeScore` và `awayScore`.
- Đối với các trận đấu knock-out/cup kết thúc hòa 90/120 phút và phân định bằng loạt sút luân lưu (ví dụ Indonesia vs Thailand hòa 2-2, Indonesia thắng pen 4-2), giao diện Backtest/Chi tiết trận đấu chỉ hiển thị `2 - 2` và nhãn `Thực tế: Hòa`.
- Điều này gây ra sự hiểu nhầm cho người dùng khi đối chiếu kết quả Backtest thực tế, vì giao diện không phản ánh trận đấu đã có đội chiến thắng chung cuộc qua loạt sút luân lưu.

---

## 3. Evidence
- Khảo sát mã nguồn TrueLab:
  - DTO [`MatchDto.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/remote/dto/MatchDto.kt): Chỉ parse `home_score` và `away_score`.
  - Room Entity [`MatchEntity.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/entity/MatchEntity.kt): Chỉ lưu `homeScore` và `awayScore`.
  - Domain Model [`Match.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/Match.kt): Không có thuộc tính penalty.
- Khảo sát API upstream TrueScore:
  - Match API trả về đầy đủ: `attributes.is_penalty`, `attributes.penalty_result.home_score`, `attributes.penalty_result.away_score`.

---

## 4. Root Cause
- Kiến trúc dữ liệu ban đầu của TrueLab tập trung $100\%$ vào bài toán dự đoán 1X2 cho thời gian thi đấu chính thức (FT/AET) nên đã gạt bỏ các trường dữ liệu phụ liên quan đến loạt sút luân lưu trong tầng Data Ingestion & Presentation Layer.

---

## 5. Impact
- **Mức độ ảnh hưởng:** Thấp (Non-blocking).
- **Phạm vi:** Trải nghiệm người dùng (UX) và tính minh bạch của thông tin tỷ số trên giao diện hiển thị.
- **Hoàn toàn không ảnh hưởng:** Không ảnh hưởng đến thuật toán dự đoán 1X2, không ảnh hưởng đến Candidate A / Phase B6, không ảnh hưởng đến Brier Score hay các chỉ số Metrics 1X2.

---

## 6. Current Behavior vs 7. Expected Behavior

| Trường Hợp Trận Đấu | Current Behavior (Hành vi hiện tại) | Expected Behavior (Hành vi kỳ vọng) |
| :--- | :--- | :--- |
| **Trận 2-2, Pen 4-2** | Hiển thị: `2 - 2` \| ActualOutcome: `DRAW` | Hiển thị: **`2 - 2 (PEN 4-2)`** \| ActualOutcome: **`DRAW`** ($100\%$ giữ nguyên 1X2) |
| **Trận 1-0 (Không Pen)** | Hiển thị: `1 - 0` \| ActualOutcome: `HOME_WIN` | Hiển thị: **`1 - 0`** \| ActualOutcome: **`HOME_WIN`** |
| **Dữ liệu cũ thiếu Pen** | Hiển thị: `2 - 2` | Hiển thị giữ nguyên: **`2 - 2`** (Fallback an toàn không crash) |

---

## 8. Proposed Solution
1. **Data Layer**:
   - Cập nhật `MatchRecord` DTO bổ sung `is_penalty` và `penalty_result`.
   - Cập nhật `MatchEntity` Room DB bổ sung 3 cột `isPenalty`, `homePenaltyScore`, `awayPenaltyScore` và viết Room Migration script.
2. **Domain Layer**:
   - Cập nhật `Match` & `BacktestMatchRecord` bổ sung 3 trường penalty tương ứng.
3. **Presentation Layer**:
   - Viết Score Formatter tập trung hiển thị định dạng `homeScore - awayScore (PEN homePen-awayPen)` khi `isPenalty == true`.
   - Cập nhật `BacktestVisualizerScreen`, `BacktestItemCard` và `PredictionScreen`.

---

## 9. Scope vs 10. Non-Scope

### Scope
- Bổ sung 3 trường `isPenalty`, `homePenaltyScore`, `awayPenaltyScore`.
- Cập nhật DTO, Room Entity, Domain Models, Mappers và UI Score Formatter.
- Viết Unit Tests cho Mapping và Formatter.

### Non-Scope
- ❌ **NOT:** Prediction Algorithm Bug
- ❌ **NOT:** Ground Truth 1X2 Bug (Giữ nguyên `ActualOutcome` 1X2 90/120p)
- ❌ **NOT:** B6 Regression (Giữ nguyên $\delta=0.04, \theta=0.255$)
- ❌ **NOT:** Odds Issue
- ❌ **NOT:** Metric "Winner of Tie" (Xem như định hướng mở rộng tương lai)

---

## 11. Acceptance Criteria

- [ ] Parse đúng `is_penalty` và `penalty_result` từ TrueScore Match API.
- [ ] Room DB lưu chính xác `isPenalty`, `homePenaltyScore`, `awayPenaltyScore`.
- [ ] Trận `2-2` có Pen `4-2` hiển thị tỷ số `2 - 2 (PEN 4-2)`.
- [ ] Trận `2-2` không có Pen tiếp tục hiển thị `2 - 2`.
- [ ] `ActualOutcome` 1X2 cho trận `2-2` có Pen vẫn giữ nguyên là `DRAW` ($100\%$ không đổi).
- [ ] Dữ liệu cũ trong DB không bị lỗi hay crash.
- [ ] $100\%$ Unit Tests vượt qua thành công và build sạch không lỗi.

---

## 12. Phân Loại Nhiệm Vụ
- **Category:** Data/UI Presentation
- **Type:** Enhancement / Data Completeness
- **Priority:** Low / Non-blocking
- **Severity:** Minor

---

## 13. Related Components / Files Dự Kiến Thay Đổi

1. [`core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/remote/dto/MatchDto.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/remote/dto/MatchDto.kt)
2. [`core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/entity/MatchEntity.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/entity/MatchEntity.kt)
3. [`core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt)
4. [`core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/Match.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/Match.kt)
5. [`core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt)
6. [`core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt)
7. [`app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/mapper/BacktestUiMapper.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/mapper/BacktestUiMapper.kt)
8. [`app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/components/BacktestItemCard.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/components/BacktestItemCard.kt)
