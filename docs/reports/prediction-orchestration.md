# TrueLab – Báo cáo Triển khai Tầng Điều phối Dự đoán (Prediction Orchestration Report)

Tài liệu này tổng kết quá trình triển khai lớp Orchestration điều phối dự đoán bóng đá thông qua `PredictMatchOutcomeUseCase` trong tầng `:core:domain`, tích hợp toàn diện 6 Signal Transformers với thuật toán **Weighted Scoring (Phase 7)**.

---

## 1. Sơ đồ Luồng Dự đoán (Prediction Pipeline)

```text
                  [MatchPredictionContext]
                             │
                             ▼
               [PredictMatchOutcomeUseCase]
                             │
       ┌─────────────────────┼─────────────────────┐
       ▼                     ▼                     ▼
[FormSignalTransformer] [EloSignalTransformer] [GoalsSignalTransformer]
       │                     │                     │
       ▼                     ▼                     ▼
[OddsSignalTransformer] [H2hSignalTransformer] [HomeAdvantageSignalTransformer]
       │                     │                     │
       └─────────────────────┼─────────────────────┘
                             │
                             ▼
                    [List<Signal3Way>]
                             │
                             ▼
              [WeightedScorer.predictOutcome()]
                             │
                             ▼
                   [OutcomeProbabilities]
                             │
                             ▼
                    [PredictionResult]
```

---

## 2. Luồng Phụ thuộc & Trách nhiệm (Dependency Flow)

1. **`MatchPredictionContext`**: Đóng gói toàn bộ thông tin đầu vào của một trận đấu (ID, Elo, trận đấu gần đây, đối đầu H2H, tỷ lệ kèo odds, thông tin sân bãi).
2. **`PredictMatchOutcomeUseCase`**: Lớp UseCase duy nhất chịu trách nhiệm gom dữ liệu, chống rò rỉ dữ liệu lịch sử (Data Leakage Prevention), gọi 6 Transformer, kiểm soát trọng số và gọi `WeightedScorer`.
3. **6 Pure Signal Transformers**:
   - `FormSignalTransformer`: Chuyển đổi điểm phong độ thành xác suất có làm mịn $\epsilon = 0.10$.
   - `EloSignalTransformer`: Chuyển đổi kỳ vọng Elo logistic thành xác suất có baseline hòa $0.26$.
   - `GoalsSignalTransformer`: Chuyển đổi chênh lệch bàn thắng kỳ vọng $\Delta \lambda$ thành xác suất có chặn biên $[0.05, 0.69]$.
   - `OddsSignalTransformer`: Chuyển đổi tỷ lệ cược 1X2 thành xác suất triệt tiêu margin (gán $w = 0.0$ nếu thiếu odds).
   - `H2hSignalTransformer`: Áp dụng Laplace Smoothing với $K=3.0$ và prior $[\alpha_H = 0.45, \alpha_D = 0.27, \alpha_A = 0.28]$.
   - `HomeAdvantageSignalTransformer`: Áp dụng xác suất tiên nghiệm sân nhà $[0.46, 0.26, 0.28]$ (hoặc $[0.37, 0.26, 0.37]$ nếu sân trung lập).
4. **`WeightedScorer.predictOutcome(signals)`**: Thuật toán Phase 7 (Linear Mixture) tính toán phân phối xác suất cuối cùng $P_c = \frac{\sum w_k p_{k,c}}{\sum w_k}$.
5. **`PredictionResult`**: Domain Entity chứa `homeWinProb`, `drawProb`, `awayWinProb`, `predictedOutcome` và `confidenceScore`.

---

## 3. Cơ chế Xử lý Dữ liệu Biên & Chống Rò rỉ Dữ liệu (Data Leakage & Edge Cases)

1. **Chống rò rỉ dữ liệu (Anti Data Leakage)**:
   - Khi tính toán Form, Goals và H2H từ danh sách trận đấu gần đây, UseCase tự động lọc `it.isEnded && it.id != context.matchId`.
   - Các trận đấu chưa kết thúc (Scheduled/In-Progress) hoặc chính trận đấu đang được dự đoán tuyệt đối không bị đưa vào dữ liệu lịch sử.
2. **Missing Odds / Dữ liệu khuyết thiếu**:
   - Nếu trận đấu chưa có odds hoặc odds lỗi: `OddsSignalTransformer` tự động gán `weight = 0.0`. Thuật toán Linear Mixture tự động chia tỷ lệ trên 5 tín hiệu còn lại mà không gây crash.
   - Nếu thiếu Elo / Form / Goals / H2H: Tự động fallback về các baseline trung tính an toàn.
3. **Kiểm tra Tổng trọng số**:
   - Nếu toàn bộ tín hiệu có tổng $weight \le 0.0$: Ném `IllegalStateException` rõ ràng theo contract của `WeightedScorer`, không tự sinh số giả.

---

## 4. Hiện trạng Code Prototype & Kế hoạch Dọn dẹp (Cleanup Strategy)

| Thành phần Cũ / Prototype | Vị trí Hiện tại | Trạng thái | Đánh giá & Rủi ro khi Xóa |
|---|---|---|---|
| `PredictionResult.computeWeightedScoring` | `dev.anhquocs.truelab.core.domain.prediction.model.PredictionResult` | Companion Object chứa logic hardcode cũ (offset $+0.10$, $+0.05$) | Đã được thay thế hoàn toàn bởi `PredictMatchOutcomeUseCase`. Có thể dọn dẹp an toàn khi không còn màn hình UI cũ gọi trực tiếp. |
| `PredictionRepository.predictMatch` | `dev.anhquocs.truelab.core.domain.prediction.repository.PredictionRepository` | Interface trả về `Flow<PredictionResult>` từ Room DB cache | Giữ lại để phục vụ lưu trữ / đọc cache kết quả dự đoán từ local database. |
| `PredictMatchUseCase` | `dev.anhquocs.truelab.core.domain.prediction.usecase.PredictionUseCases` | Gọi `repository.predictMatch(matchId)` | Giữ lại để tránh breaking change với `PredictionDataModule` trong `:core:data`. Sẽ migrate Presentation layer sang `PredictMatchOutcomeUseCase`. |

---

## 5. Kết quả Kiểm thử & Regression Test

- **`:core:domain:test`**: **32/32 tests PASS 100%**.
  - Kiểm tra Happy path đầy đủ 6 tín hiệu.
  - Kiểm tra missing odds fallback ($w=0.0$).
  - Kiểm tra missing all optional data fallback.
  - Kiểm tra chống rò rỉ dữ liệu (Current Match Exclusion & Scheduled filter).
  - Kiểm tra điều chỉnh tính đối xứng của sân trung lập (Neutral Venue).
  - Kiểm tra validation cấu hình trọng số.
- **`:core:algorithm:test`**: **122/122 tests PASS 100%** (Phase 1–7 FROZEN tiếp tục bảo toàn).
- **Toàn bộ Gradle Build**: **BUILD SUCCESSFUL** (80 actionable tasks executed).
