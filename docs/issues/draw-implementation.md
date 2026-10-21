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
Phase B6: Finalization & Production Strategy Decision (✅ HOÀN TẤT)
```

---

## 3. Danh Mục Nhiệm Vụ Chi Tiết (Phase B Tasks)

### A. Kiến Trúc Chiến Lược (Architecture Abstraction — Phase B1)
- [x] **Nhiệm vụ A.1:** Thiết kế enum `DrawModelingStrategy` (BASELINE, DECISION_MARGIN, DYNAMIC_DRAW_PRIOR) trong `:core:domain`.
- [x] **Nhiệm vụ A.2:** Xây dựng `DrawStrategyConfig` bao gồm `DrawMarginConfig` và `DynamicDrawPriorConfig`.
- [x] **Nhiệm vụ A.3:** Đảm bảo chế độ mặc định ban đầu luôn là `BASELINE` để tương thích ngược $100\%$.

### B. Cài Đặt Candidate A: Decision Margin (Phase B2)
- [x] **Nhiệm vụ B.1:** Xây dựng `DrawDecisionPolicy` phân ngưỡng: $\hat{Y} = \text{DRAW}$ khi $|P_H - P_A| < \delta$ và $P_D \ge \theta$.
- [x] **Nhiệm vụ B.2:** Giữ nguyên raw probability từ `WeightedScorer`, chỉ điều chỉnh `predictedOutcome` và ghi nhận evidence.
- [x] **Nhiệm vụ B.3:** Xử lý chính xác các điều kiện biên và tie-breaking tất định.

### C. Cài Đặt Candidate B: Dynamic Draw Prior (Phase B3)
- [x] **Nhiệm vụ C.1:** Tính toán độ lệch sức mạnh $\Delta_{\text{Elo}}$ và $\Delta_{\text{Form}}$.
- [x] **Nhiệm vụ C.2:** Cài đặt hàm mật độ Gauss đối xứng $P_D(\Delta) = P_{D,\min} + (P_{D,\max} - P_{D,\min}) \cdot \exp\left( - \frac{\Delta^2}{2 \sigma_D^2} \right)$.
- [x] **Nhiệm vụ C.3:** Tích hợp vào `EloSignalTransformer` và `FormSignalTransformer`, đảm bảo điều kiện chuẩn hóa simplex $\sum P = 1.0 \pm 10^{-6}$ và fallback an toàn khi thiếu dữ liệu.

### D. Tích Hợp Chiến Lược Vào UseCases (Strategy Integration)
- [x] **Nhiệm vụ D.1:** Cập nhật `PredictMatchOutcomeUseCase` nhận `DrawStrategyConfig` (mặc định production).
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

### I. Hoàn Thiện & Quyết Định Sản Phẩm (Finalization & Production Switch Decision — Phase B6)
- [x] **Nhiệm vụ I.1:** Đánh giá trade-off thực tế giữa Candidate A và Candidate B từ dữ liệu Independent Test.
- [x] **Nhiệm vụ I.2:** Ra quyết định chiến lược cho Production: Chuyển cấu hình `DrawStrategyConfig.DEFAULT` sang `DECISION_MARGIN` (Candidate A frozen: $\delta = 0.04, \theta = 0.255$).
- [x] **Nhiệm vụ I.3:** Giữ nguyên vẹn Baseline và Candidate B để phục vụ đối chứng, benchmark và kiểm thử hồi quy.
- [x] **Nhiệm vụ I.4:** Xác thực toàn bộ test suite và build hoàn tất (100% PASS).

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

## 5. Quyết Định Sản Phẩm & Trạng Thái Hoàn Tất Phase B6 (Production Decision & Phase B6 Status)

1. **Quyết định Chiến lược Sản xuất (Production Strategy Decision):**
   - **Candidate A (`DECISION_MARGIN`)** được chọn làm **Production Strategy** theo tiêu chí cân bằng phân loại 3 nhãn và kết quả Independent Test đã được frozen trước đó.
   - **Cấu hình Production Mặc định:**
     - Strategy: `DrawModelingStrategy.DECISION_MARGIN`
     - $\delta_{\text{margin}} = 0.04$
     - $\theta_{\text{minProb}} = 0.255$
   - **Đánh đổi Thực nghiệm (Empirical Trade-off):**
     - Macro F1 cải thiện mạnh: $0.3667 \to 0.4066$ ($+10.9\%$).
     - Draw Recall tăng vọt: $0.17\% \to 12.17\%$, Draw F1 tăng $0.0033 \to 0.1626$.
     - Bảo toàn $100\%$ phân phối xác suất và độ hiệu chuẩn (Brier Score giữ nguyên $0.6284$).
     - Đánh đổi: Home F1 giảm nhẹ ($0.5944 \to 0.5651$), Away F1 giảm nhẹ ($0.5023 \to 0.4922$), Overall Accuracy giảm nhẹ ($48.14\% \to 46.06\%$).

2. **Bảo tồn Kiến trúc Đối chứng:**
   - **`BASELINE`**: Tiếp tục được duy trì đầy đủ làm Control Group phục vụ kiểm thử hồi quy và đối soát khoa học.
   - **`DYNAMIC_DRAW_PRIOR` (Candidate B)**: Tiếp tục được duy trì đầy đủ với tham số đóng băng ($P_{\max}=0.38, P_{\min}=0.12, \sigma_{\text{Elo}}=1.00, \sigma_{\text{Form}}=0.30$) phục vụ nghiên cứu, so chuẩn và thực nghiệm tương lai.
