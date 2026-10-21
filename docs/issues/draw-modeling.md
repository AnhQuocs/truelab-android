# Issue Tracker: Nghiên Cứu & Mô Hình Hóa Dự Đoán Hòa (Phase A: Draw Modeling)

## 1. Mục Tiêu (Goal)
Nghiên cứu lý thuyết và thiết kế mô hình hóa toán học giải quyết hiện tượng triệt tiêu kết quả hòa (*Draw Underprediction*) trong hệ sinh thái dự đoán của TrueLab, thiết lập cơ sở cho việc triển khai thực nghiệm (Phase B) và kiểm thử hồi quy đa ngày (Phase C).

Tài liệu kế hoạch chi tiết tham chiếu: [`draw-modeling-plan.md`](../plans/draw-modeling-plan.md).

---

## 2. Tài Liệu Căn Cứ (Source Documents)
1. **Báo cáo Audit Hòa:** [`draw-underprediction-audit.md`](../audits/draw-underprediction-audit.md)
2. **Kế hoạch Lộ trình Toàn diện:** [`truelab-roadmap-next-phases-plan.md`](../plans/truelab-roadmap-next-phases-plan.md)
3. **Issue Lộ trình Toàn diện:** [`truelab-roadmap-next-phases.md`](../issues/truelab-roadmap-next-phases.md)

---

## 3. Danh Mục Nhiệm Vụ Chi Tiết (Tasks)

### A. Chuẩn Hóa Mô Hình Cơ Sở (Baseline Formalization)
- [x] **Nhiệm vụ A.1:** Đặc tả cấu trúc 6 tín hiệu (Form, Elo, Odds, Goals, H2H, Rest Advantage) và cơ chế tổng hợp tuyến tính `WeightedScorer`.
- [x] **Nhiệm vụ A.2:** Phân tích nguyên nhân toán học dẫn đến `Predicted Draw = 0` (Phân rã nhị phân $P_D = 0.26$ cố định và luật $\text{argmax}$).
- [x] **Nhiệm vụ A.3:** Thiết lập mô hình hiện tại làm nhóm đối chứng (**Control Group**) không bị thay đổi trong suốt quá trình thử nghiệm.

### B. Phương Án Ứng Viên A — Phân Ngưỡng Chênh Lệch (Decision Margin / Relative Threshold)
- [x] **Nhiệm vụ B.1:** Xây dựng công thức ra quyết định phân ngưỡng: $\hat{Y} = \text{DRAW}$ khi $|P_H - P_A| < \delta$ và $P_D \ge \theta$.
- [x] **Nhiệm vụ B.2:** Xác định dải tham số ứng viên: $\delta \in [0.02, 0.08]$, $\theta \in [0.250, 0.275]$.
- [x] **Nhiệm vụ B.3:** Đánh giá độ phức tạp tính toán $O(1)$, an toàn thời gian và tính độc lập kiến trúc (Non-invasive wrapper).

### C. Phương Án Ứng Viên B — Tiên Nghiệm Hòa Động (Dynamic Draw Prior)
- [x] **Nhiệm vụ C.1:** Thiết lập hàm mật độ hòa động Gaussian đối xứng: $P_D(\Delta) = P_{D,\min} + (P_{D,\max} - P_{D,\min}) \cdot \exp\left( - \frac{\Delta^2}{2 \sigma_D^2} \right)$.
- [x] **Nhiệm vụ C.2:** Kiểm tra điều kiện bảo toàn xác suất $\sum P = 1.0$, tính đối xứng qua $\Delta = 0$ và hành vi tiệm cận.
- [x] **Nhiệm vụ C.3:** Xác định điểm tích hợp vào `EloSignalTransformer` và `FormSignalTransformer`.

### D. Phương Án Ứng Viên C — Mô Hình Bàn Thắng Poisson (Bivariate / Dixon-Coles Poisson)
- [x] **Nhiệm vụ D.1:** Xây dựng mô hình kỳ vọng bàn thắng $\lambda_H, \lambda_A$ và ma trận tỷ số $7 \times 7$ ($N_{\max} = 6$).
- [x] **Nhiệm vụ D.2:** Áp dụng hệ số hiệu chỉnh Dixon-Coles $\tau(x, y)$ cho các tỷ số tương quan thấp.
- [x] **Nhiệm vụ D.3:** Phân tích ràng buộc tính toán cục bộ Pure Kotlin (không dùng ML framework) và xử lý Cold-Start.

### E. So Sánh Đối Chiếu Các Phương Án (Candidate Comparison Matrix)
- [x] **Nhiệm vụ E.1:** Lập bảng so sánh 13 tiêu chí khách quan (Toán học, Cài đặt, Runtime, Dữ liệu, Giải thích, Overfitting, v.v.).
- [x] **Nhiệm vụ E.2:** Đưa ra khuyến nghị kỹ thuật: Ưu tiên Candidate A cho Phase B và nghiên cứu mở rộng Candidate B.

### F. Thiết Kế Thực Nghiệm & Chống Rò Rỉ Dữ Liệu (Experimental Design & Anti-Leakage)
- [x] **Nhiệm vụ F.1:** Thiết kế quy trình thực nghiệm đối chứng công bằng (cùng dataset, cùng temporal cutoff, cùng input features).
- [x] **Nhiệm vụ F.2:** Ban hành quy tắc chống rò rỉ dữ liệu tương lai (Strict Temporal Filter $t < T_{\text{kickoff}}$).
- [x] **Nhiệm vụ F.3:** Xác lập ma trận chỉ số đánh giá: Accuracy, Precision, Recall, F1, Macro F1, $3 \times 3$ Confusion Matrix, Brier Score.

### G. Tiêu Chí Nghiệm Thu (Acceptance Criteria)
- [x] **Nhiệm vụ G.1:** Hoàn thành đầy đủ tài liệu toán học chi tiết mà không chỉnh sửa bất kỳ dòng mã nguồn sản phẩm nào.

---

## 4. Ma Trận Phụ Thuộc (Dependencies)

```text
Draw Underprediction Audit (Hoàn thành)
        ↓
Phase A: Draw Modeling Plan & Issue (Hoàn thành)
        ↓
Phase B: Draw Implementation & Metrics Evaluation (Kế tiếp)
        ↓
Phase C: Multi-Day Backtest & Model Comparison
        ↓
Phase D: Algorithm Benchmark Suite
```

---

## 5. Tiêu Chuẩn Hoàn Thành (Definition of Done)
- [x] Tài liệu kỹ thuật [`draw-modeling-plan.md`](../plans/draw-modeling-plan.md) hoàn thiện 12 phần quy chuẩn.
- [x] Issue tracker [`draw-modeling.md`](draw-modeling.md) cập nhật đầy đủ trạng thái nhiệm vụ.
- [x] Bảo toàn $100\%$ tính bất biến của mã nguồn sản phẩm hiện tại (`git diff --check` sạch sẽ).
- [x] Sẵn sàng chuyển giao sang Phase B (Implementation).
