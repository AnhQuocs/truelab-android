# Issue Tracker: Triển Khai & Đánh Giá Thực Nghiệm Dự Đoán Hòa (Phase B: Draw Implementation & Evaluation)

## 1. Mục Tiêu (Goal)
Triển khai kỹ thuật và đánh giá thực nghiệm 2 phương án ứng viên **Candidate A (Decision Margin)** và **Candidate B (Dynamic Draw Prior)** nhằm giải quyết hiện tượng triệt tiêu kết quả hòa (`Draw Underprediction`), nâng cao năng lực nhận diện trận hòa (`Draw Recall` & `Macro F1`) mà không làm suy giảm độ chính xác của Home/Away, bảo toàn tuyệt đối **Baseline** làm nhóm đối chứng.

Tài liệu tham chiếu liên quan:
- Kế hoạch mô hình hóa toán học: [`docs/plans/draw-modeling-plan.md`](../plans/draw-modeling-plan.md)
- Kế hoạch triển khai kỹ thuật: [`docs/plans/draw-implementation-plan.md`](../plans/draw-implementation-plan.md)
- Báo cáo tối ưu hóa Candidate B (Calibration): [`docs/reports/draw-candidate-b-optimization.md`](../reports/draw-candidate-b-optimization.md)
- Báo cáo đánh giá đối chứng Test độc lập: [`docs/reports/draw-independent-test-evaluation.md`](../reports/draw-independent-test-evaluation.md)

---

## 2. Ma Trận Tiến Độ Triển Khai (Implementation Progress)

```text
Phase A: Draw Modeling (Đã hoàn thành)
        ↓
Phase B1: Strategy Abstraction & Baseline Protection (✅ HOÀN TẤT)
        ↓
Phase B2: Candidate A Implementation (Decision Margin) (✅ HOÀN TẤT)
        ↓
Phase B3: Candidate B Implementation (Dynamic Draw Prior) (✅ HOÀN TẤT)
        ↓
Phase B4: Parameter Optimization & Grid Search Calibration (✅ HOÀN TẤT)
        ↓
Phase B5: Parameter Freezing & Independent Test Evaluation (✅ HOÀN TẤT)
        ↓
Phase B6: Finalization & Production Strategy Decision (⏳ KẾ HOẠCH TIẾP THEO)
```

---

## 3. Danh Mục Nhiệm Vụ Chi Tiết (Phase B Tasks)

### A. Kiến Trúc Chiến Lược (Architecture Abstraction — Phase B1)
- [x] **Nhiệm vụ A.1:** Thiết kế enum `DrawModelingStrategy` (BASELINE, DECISION_MARGIN, DYNAMIC_DRAW_PRIOR) trong `:core:domain`.
- [x] **Nhiệm vụ A.2:** Xây dựng `DrawStrategyConfig` bao gồm `DrawMarginConfig` và `DynamicDrawPriorConfig`.
- [x] **Nhiệm vụ A.3:** Đảm bảo chế độ mặc định trong production luôn là `BASELINE` để tương thích ngược $100\%$.

### B. Cài Đặt Candidate A: Decision Margin (Phase B2)
- [x] **Nhiệm vụ B.1:** Xây dựng `DrawDecisionPolicy` phân ngưỡng: $\hat{Y} = \text{DRAW}$ khi $|P_H - P_A| < \delta$ và $P_D \ge \theta$.
- [x] **Nhiệm vụ B.2:** Giữ nguyên raw probability từ `WeightedScorer`, chỉ điều chỉnh `predictedOutcome` và ghi nhận evidence.
- [x] **Nhiệm vụ B.3:** Xử lý chính xác các điều kiện biên và tie-breaking tất định.

### C. Cài Đặt Candidate B: Dynamic Draw Prior (Phase B3)
- [x] **Nhiệm vụ C.1:** Tính toán độ lệch sức mạnh $\Delta_{\text{Elo}}$ và $\Delta_{\text{Form}}$.
- [x] **Nhiệm vụ C.2:** Cài đặt hàm mật độ Gauss đối xứng $P_D(\Delta) = P_{D,\min} + (P_{D,\max} - P_{D,\min}) \cdot \exp\left( - \frac{\Delta^2}{2 \sigma_D^2} \right)$.
- [x] **Nhiệm vụ C.3:** Tích hợp vào `EloSignalTransformer` và `FormSignalTransformer`, đảm bảo điều kiện chuẩn hóa simplex $\sum P = 1.0 \pm 10^{-6}$ và fallback an toàn khi thiếu dữ liệu.

### D. Tích Hợp Chiến Lược Vào UseCases (Strategy Integration)
- [x] **Nhiệm vụ D.1:** Cập nhật `PredictMatchOutcomeUseCase` nhận `DrawStrategyConfig` (mặc định BASELINE).
- [x] **Nhiệm vụ D.2:** Cập nhật `RunDailyBacktestUseCase` cho phép truyền chiến lược đánh giá tùy chọn.

### E. Quét Siêu Tham Số & Tối Ưu Hóa Calibration (Parameter Sweep — Phase B4 & B5)
- [x] **Nhiệm vụ E.1:** Xây dựng `OptimizeCandidateADrawUseCase` quét lưới $42$ cấu hình ($\delta \in [0.02, 0.08], \theta \in [0.250, 0.275]$) trên 70% Calibration.
- [x] **Nhiệm vụ E.2:** Xây dựng `OptimizeCandidateBDrawUseCase` quét lưới $64$ cấu hình ($P_{D,\max} \in [0.32, 0.38], \sigma_{\text{Elo}} \in [0.75, 1.50], \sigma_{\text{Form}} \in [0.15, 0.30]$) trên 70% Calibration.
- [x] **Nhiệm vụ E.3:** Thiết lập hàm mục tiêu tối đa hóa `Macro F1` với ràng buộc bảo vệ Home/Away F1 (suy giảm không quá 0.03 so với Baseline).

### F. Phân Tách Tập Dữ Liệu & Đóng Băng Tham Số (Temporal Split & Freezing)
- [x] **Nhiệm vụ F.1:** Phân chia tập dữ liệu lịch sử $15,456$ trận thành Calibration/Validation ($70\% = 10,779$ trận) và Independent Test ($30\% = 4,620$ trận) theo thứ tự thời gian tăng dần.
- [x] **Nhiệm vụ F.2:** Đóng băng (Freeze) tham số tối ưu sau Calibration trước khi mở khóa Test set:
  - **Baseline:** $P_D = 0.26$ fixed.
  - **Candidate A:** $\delta = 0.04, \quad \theta = 0.255$.
  - **Candidate B:** $P_{D,\max} = 0.38, \quad P_{D,\min} = 0.12, \quad \sigma_{\text{Elo}} = 1.00, \quad \sigma_{\text{Form}} = 0.30$.
- [x] **Nhiệm vụ F.3:** Áp dụng nghiêm ngặt nguyên tắc chống rò rỉ dữ liệu tương lai (Strict Temporal Filter $t < T_{\text{kickoff}}$).

### G. Đánh Giá Đối Chứng Độc Lập 3 Chiều (Independent Test Evaluation — Phase B5)
- [x] **Nhiệm vụ G.1:** Xây dựng `EvaluateDrawModelsOnIndependentTestUseCase` đo đạc đồng thời: Accuracy, Macro Precision/Recall/F1, Class F1s, $3 \times 3$ Confusion Matrix, Multi-class Brier Score.
- [x] **Nhiệm vụ G.2:** Xuất báo cáo khoa học đối chứng 3 mô hình tại [`docs/reports/draw-independent-test-evaluation.md`](../reports/draw-independent-test-evaluation.md).

### H. Bộ Kiểm Thử Tự Động (Automated Testing Suite)
- [x] **Nhiệm vụ H.1:** Viết Unit Tests cho Candidate A, Candidate B, Brier Score và Strategy Abstraction.
- [x] **Nhiệm vụ H.2:** Viết Unit Tests cho quá trình Calibration Grid Search và Independent Test Evaluation.
- [x] **Nhiệm vụ H.3:** Chạy kiểm thử hồi quy xác nhận Baseline không bị thay đổi bất kỳ hành vi nào.

### I. Hoàn Thiện & Quyết Định Sản Phẩm (Finalization & Rollout Decision — Phase B6)
- [ ] **Nhiệm vụ I.1:** Đánh giá trade-off thực tế giữa Candidate A và Candidate B từ dữ liệu Independent Test.
- [ ] **Nhiệm vụ I.2:** Ra quyết định chiến lược cho Production (giữ Baseline mặc định hay chuyển đổi cấu hình sang Candidate A/B).
- [ ] **Nhiệm vụ I.3:** Cập nhật UI hiển thị Draw Modeling Evidence trên Daily Backtest và Prediction Screen nếu cần.

---

## 4. Bảng Tổng Hợp Kết Quả Thực Nghiệm Đối Chứng (Empirical Results Summary)

### 4.1. Tham số đã đóng băng (Frozen Parameters)
- **BASELINE (Control Group):** $P_D = 0.26$ (Cố định).
- **CANDIDATE A (Decision Margin):** $\delta = 0.04, \quad \theta = 0.255$.
- **CANDIDATE B (Dynamic Draw Prior):** $P_{D,\max} = 0.38, \quad P_{D,\min} = 0.12, \quad \sigma_{\text{Elo}} = 1.00, \quad \sigma_{\text{Form}} = 0.30$.

### 4.2. So sánh hiệu năng trên Tập Test Độc Lập (Independent Test — N = 4,620)
*(Dữ liệu tương lai chưa từng thấy: `2023-04-16T12:30:00Z` $\to$ `2026-09-29T04:00:00Z`)*

| Chỉ Số Đánh Giá | BASELINE (Control) | CANDIDATE A (Margin) | CANDIDATE B (Dynamic) | So Sánh & Đặc Điểm Kỹ Thuật |
|:---|:---:|:---:|:---:|:---|
| **Macro F1** | `0.3667` | **`0.4066`** | `0.3829` | Candidate A cao nhất ($+0.0399$), Candidate B tăng $+0.0162$ vs Base |
| **Draw F1** | `0.0033` | **`0.1626`** | `0.0652` | Cả 2 đều thoát khỏi hiện tượng 0 Draw |
| **Draw Recall** | `0.17%` | **`12.17%`** | `3.75%` | Candidate A bắt được 146 trận hòa, Candidate B bắt được 45 trận hòa |
| **Draw Precision** | `100.00%` (2/2) | `24.50%` | `24.86%` | Cả 2 đạt độ chính xác xấp xỉ tỷ lệ tự nhiên ($25.97\%$) |
| **Home F1** | `0.5944` | `0.5651` | `0.5865` | Candidate B bảo toàn Home F1 tốt hơn ($-0.0079$ vs $-0.0293$) |
| **Away F1** | `0.5023` | `0.4922` | `0.4972` | Candidate B bảo toàn Away F1 tốt hơn ($-0.0052$ vs $-0.0102$) |
| **Overall Accuracy** | `48.14%` | `46.06%` | `47.42%` | Candidate B giữ độ chính xác tổng thể gần Baseline hơn |
| **Brier Score** | `0.6284` | `0.6284` | `0.6324` | Candidate A giữ nguyên phân phối xác suất; Candidate B lệch $+0.0040$ |

---

## 5. Hiện Trạng & Kế Hoạch Tiếp Theo (Current Status & Next Steps)

1. **Hiện trạng hệ thống:**
   - Căn nguyên của hiện tượng *Draw underprediction* đã được ghi nhận và phân tích đầy đủ.
   - Cả Candidate A và Candidate B đã được cài đặt hoàn chỉnh theo kiến trúc Strategy Clean Architecture.
   - Quá trình quét lưới tối ưu hóa siêu tham số đã hoàn tất trên tập Calibration 70% theo thứ tự thời gian.
   - Toàn bộ tham số đã được đóng băng trước khi đánh giá trên tập Independent Test 30%.
   - Cả hai mô hình ứng viên đều chứng minh cải thiện Macro F1 so với Baseline trên dữ liệu kiểm thử độc lập.
   - **Trade-off quan trọng:**
     - Candidate A đạt Macro F1, Draw F1 và Draw Recall cao hơn Candidate B trên tập Independent Test.
     - Candidate B bảo toàn năng lực phân loại Home F1 và Away F1 tốt hơn và gần với Baseline hơn.
     - Candidate A không làm thay đổi phân phối xác suất gốc (Brier Score bằng Baseline), trong khi Candidate B biến đổi phân phối tiên nghiệm và có Brier Score chênh lệch nhẹ ($0.6324$ vs $0.6284$).
   - **Cấu hình Production:** Vẫn giữ nguyên mặc định là `DrawModelingStrategy.BASELINE`. Tuyệt đối chưa tự ý chuyển đổi cấu hình sản phẩm.

2. **Kế hoạch tiếp theo (Phase B6):**
   - Đưa ra quyết định lựa chọn chiến lược phù hợp nhất cho Production (hoặc cơ chế cho phép người dùng tùy chọn chiến lược trong Settings).
   - Triển khai tích hợp UI nếu được phê duyệt.
   - Tuyệt đối không tinh chỉnh thêm tham số trên tập Test độc lập.
