# Issue: Candidate B — Relative Draw Gap: Implementation & Evaluation

- **Phân loại:** Thuật toán / Decision Policy / Nghiên cứu thực nghiệm
- **Trạng thái:** **ĐÃ ĐÓNG (Closed - REJECTED)**
- **Quyết định:** Candidate B bị từ chối theo tiêu chuẩn nghiệm thu. Candidate A tiếp tục là mặc định production.

---

## 1. Vấn Đề & Bối Cảnh

Candidate A (`|P(H) - P(A)| < 0.04 AND P(D) >= 0.255`) cải thiện khả năng phát hiện trận hòa nhưng đồng thời thay thế 52 dự đoán Home/Away đúng thành Draw sai trên cohort 791 trận Odds lịch sử (làm giảm Accuracy từ 52,5% xuống 49,7%).

Mục tiêu nghiên cứu: Đánh giá một decision policy thay thế (**Candidate B — Relative Draw Gap**) với công thức $\text{drawDeficit} = \max(P(H), P(A)) - P(D) \le \tau$ để kiểm tra xem có thể cải thiện khả năng phát hiện Draw mà không làm tổn hại đến hiệu năng Home/Away hay không.

---

## 2. Toàn Bộ Vòng Đời Triển Khai (Lifecycle Tracker)

- [x] **Xác định giả thuyết & cơ sở toán học** (`drawDeficit = max(P(H), P(A)) - P(D)`)
- [x] **Xây dựng Protocol & Pre-registration** (Chia tập theo thời gian 60/40, xác định Non-inferiority bounds, lưới $\tau$)
- [x] **Triển khai kiến trúc Candidate B** (Tách `DrawDecisionRule` độc lập, tích hợp vào `DrawDecisionPolicy`, bảo toàn 100% `DrawModelingStrategy` và `WeightedScorer`)
- [x] **Kiểm thử Unit Tests & Architecture Regression Tests** (17 tests policy, 14 tests abstraction, 325 tests domain đạt 100%)
- [x] **Xác minh tính bất biến của xác suất Raw** (Raw probabilities giống hệt giữa Baseline, Candidate A và Candidate B)
- [x] **Trích xuất dữ liệu thực tế từ thiết bị qua ADB** (Xác định 791 trận lịch sử có Odds pre-match hợp lệ)
- [x] **Thực nghiệm Temporal Calibration trên 476 trận** (Quét 11 điểm $\tau \in [0.000, 0.150]$)
- [x] **Khóa cứng tham số tối ưu** $\tau^* = 0.120$
- [x] **Đánh giá một lần duy nhất trên Frozen Holdout (315 trận)**
- [x] **Phân tích độ bất định Bootstrap theo cặp** ($B = 2.000$ iterations với Cluster Bootstrap & Stratified Bootstrap)
- [x] **Đánh giá & đối soát độc lập (Independent Audit)** (Xác nhận tính toàn vẹn dữ liệu, giải thích nguyên nhân từng trận)
- [x] **Lập báo cáo tổng kết chính thức** ([candidate-b-relative-draw-gap-final.md](../reports/final/candidate-b-relative-draw-gap-final.md))
- [x] **Đưa ra quyết định:** **REJECT Candidate B**

---

## 3. Tóm Tắt Kết Quả Nghiệm Thu (Holdout N = 315)

- **Candidate A:** Accuracy 53,65%, Macro F1 47,76%, Draw F1 24,35%, Draw TP 14, Draw FP 35, Dự đoán Draw 49.
- **Candidate B ($\tau^* = 0.120$):** Accuracy 54,60% (172/315), Macro F1 47,80%, Draw F1 21,43%, Draw TP 12, Draw FP 34, Dự đoán Draw 46.
- **Lý do Reject:**
  - $\text{Draw F1}$ giảm từ $24,35\% \to 21,43\%$ ($\Delta = -2,92\%$ $\implies$ Vi phạm tiêu chí 1).
  - $\text{Draw TP}$ giảm từ $14 \to 12$ trận ($\Delta = -2$ TP $\implies$ Vi phạm tiêu chí 2).
  - Mặc dù Candidate B giúp tăng Accuracy $+0,95\%$ và giảm ròng 5 trận Home/Away bị đoán sai, nhưng không đạt mục tiêu ưu tiên số 1 về phát hiện Draw.
  - Các khoảng tin cậy $95\%\text{ CI}$ đều bao gồm 0 $\implies$ Không có ý nghĩa thống kê vượt trội.

---

## 4. Kết Luận & Đóng Issue

`Candidate B rejected. Candidate A remains production default.`

- Candidate A tiếp tục là cấu hình mặc định trong toàn bộ hệ thống (`DrawStrategyConfig.DEFAULT`).
- Không triển khai Candidate B lên production.
- Issue chính thức đóng.
