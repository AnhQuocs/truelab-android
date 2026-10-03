# Issue Tracker: Lộ Trình Các Giai Đoạn Tiếp Theo Của TrueLab (Roadmap Next Phases)

## 1. Mục Tiêu Tổng Thể (Goal)
Hoàn thiện toàn bộ các hạng mục nghiên cứu, thực nghiệm thuật toán, đo chuẩn hiệu năng, trực quan hóa và tài liệu hóa đồ án **TrueLab** phục vụ báo cáo và bảo vệ kết quả học thuật.

Tài liệu kế hoạch chi tiết tham chiếu: [`truelab-roadmap-next-phases-plan.md`](../plans/truelab-roadmap-next-phases-plan.md).

---

## 2. Trạng Thái Hiện Tại (Current State)
- **Hạ tầng cốt lõi:** Hoàn thành 5 module Clean Architecture (`:core:algorithm`, `:core:domain`, `:core:data`, `:core:ui`, `:app`).
- **Pipeline Thuật toán:** Hoàn thành 7 phases (Search, Sorting, Statistics, Trend, Form, Elo, Weighted Prediction).
- **Tính ổn định:** Đã xử lý triệt để lỗi SQLite FK Wipe bằng `@Upsert`, cơ chế Cache-First Loading phản hồi tức thì $< 50\text{ms}$, Anti-Collision Grouping.
- **Hiện trạng Baseline Dự đoán (03/10/2026):** Tổng 42 trận, 22 đúng (52.4%), 12 trận hòa thực tế, 0 trận dự đoán hòa $\implies$ 60% lỗi liên quan đến Draw.
- **Audit Cơ sở:** Đã xác nhận nguyên nhân toán học tại [`draw-underprediction-audit.md`](../audits/draw-underprediction-audit.md).

---

## 3. Các Giai Đoạn Thực Hiện & Tiêu Chí Chấp Nhận (Phases & Acceptance Criteria)

### 📌 Phase A — Nghiên Cứu & Thiết Kế Mô Hình Dự Đoán Hòa (Draw Modeling)
- [ ] **Nhiệm vụ 1:** Đặc tả công thức toán học cho Phương án 1: Quy tắc phân ngưỡng (Decision Margin / Relative Threshold).
- [ ] **Nhiệm vụ 2:** Đặc tả công thức toán học cho Phương án 2: Tiên nghiệm hòa động (Dynamic Draw Prior trong Elo & Form).
- [ ] **Nhiệm vụ 3:** Đặc tả công thức toán học cho Phương án 3: Mô hình phân phối bàn thắng Poisson (Bivariate Poisson).
- [ ] **Nhiệm vụ 4:** Thiết lập tiêu chuẩn đối chứng bảo tồn `BaselineScorer` làm nhóm đối chứng (Control Group).
- **Tiêu chí chấp nhận (Acceptance Criteria):**
  - Tài liệu đặc tả toán học hoàn chỉnh, xác định rõ điểm tích hợp, độ phức tạp $O(N)$ và hành vi an toàn thời gian.
  - Không sửa đổi mã nguồn sản phẩm trong phase này.

---

### 📌 Phase B — Triển Khai & Đánh Giá Thực Nghiệm Mô Hình Hòa (Draw Implementation & Evaluation)
- [ ] **Nhiệm vụ 1:** Cài đặt Candidate Model (Decision Margin hoặc Dynamic Prior) trong `:core:algorithm` theo Pluggable Strategy Pattern.
- [ ] **Nhiệm vụ 2:** Xây dựng bộ đo chỉ số đánh giá đa lớp: Accuracy, Precision, Recall, F1, Macro F1, $3 \times 3$ Confusion Matrix.
- [ ] **Nhiệm vụ 3:** Viết bộ Unit Test kiểm tra tính đúng đắn xác suất ($\sum P = 1.0$) và bảo toàn logic không làm sụp đổ Home/Away.
- **Tiêu chí chấp nhận (Acceptance Criteria):**
  - Draw Recall tăng từ $0.0\% \to > 25\%$, Draw Precision $> 30\%$.
  - Macro F1-Score cải thiện rõ rệt so với Baseline.
  - Home/Away F1-Score không bị suy giảm nghiêm trọng.

---

### 📌 Phase C — Kiểm Thử Hồi Quy Toàn Diện (Full Backtest & Model Comparison)
- [ ] **Nhiệm vụ 1:** Mở rộng `DailyBacktestUseCase` để hỗ trợ chạy hàng loạt trên dải ngày liên tiếp ($N \ge 300 \to 1000$ trận).
- [ ] **Nhiệm vụ 2:** Xuất bảng so sánh đối chứng song song giữa Baseline Model vs Candidate Model.
- [ ] **Nhiệm vụ 3:** Phân tích ma trận nhầm lẫn và tổng hợp số liệu cho báo cáo đồ án.
- **Tiêu chí chấp nhận (Acceptance Criteria):**
  - Có báo cáo thực nghiệm Backtest đa ngày chi tiết chứng minh tính hiệu quả của mô hình mới.

---

### 📌 Phase D — Đo Chuẩn Hiệu Năng Thuật Toán Đa Kích Thước (Algorithm Scalability Benchmark)
- [ ] **Nhiệm vụ 1:** Xây dựng Benchmark Harness cho các thuật toán Tìm kiếm: Linear Search, Binary Search, Inverted Index.
- [ ] **Nhiệm vụ 2:** Xây dựng Benchmark Harness cho các thuật toán Sắp xếp: QuickSort, MergeSort, TimSort.
- [ ] **Nhiệm vụ 3:** Xây dựng Benchmark Harness cho Thuật toán Thống kê (Moments) và Phân tích Xu hướng (EMA, SMA, WMA).
- [ ] **Nhiệm vụ 4:** Chạy đo đạc thời gian thực thi (Execution Time), Throughput trên 5 quy mô dữ liệu: $1\text{K}, 5\text{K}, 10\text{K}, 25\text{K}, 50\text{K}+$ phần tử.
- [ ] **Nhiệm vụ 5:** Đối chiếu kết quả thực nghiệm với lý thuyết độ phức tạp Big-O ($O(N), O(N \log N), O(N^2)$).
- **Tiêu chí chấp nhận (Acceptance Criteria):**
  - Phân định rõ ràng giữa Algorithm Benchmark và Prediction Model Evaluation.
  - Bảng số liệu đo lường nano-second và đồ thị tăng trưởng thời gian chạy hoàn chỉnh.

---

### 📌 Phase E — Trực Quan Hóa & Giao Diện Thống Kê (Analytics & Visualization)
- [ ] **Nhiệm vụ 1:** Xây dựng Radar Chart 6 tín hiệu (Form, Elo, Odds, Goals, H2H, Rest Advantage) trên màn hình chi tiết trận đấu.
- [ ] **Nhiệm vụ 2:** Xây dựng Màn hình Đo chuẩn Thuật toán (Algorithm Benchmark Screen) trực quan hóa so sánh thời gian chạy bằng Bar/Line Charts.
- [ ] **Nhiệm vụ 3:** Nâng cấp Màn hình Backtest với Confusion Matrix trực quan và biểu đồ phân phối xác suất.
- **Tiêu chí chấp nhận (Acceptance Criteria):**
  - Giao diện Material 3 mượt mà, hỗ trợ Dark/Light Theme và Triple-locale (en, vi).

---

### 📌 Phase F — Tài Liệu Kỹ Thuật, Slide Báo Cáo & Kịch Bản Demo (Documentation, PPT & Demo)
- [ ] **Nhiệm vụ 1:** Tổng hợp tài liệu báo cáo kỹ thuật toàn diện theo luồng: Problem $\to$ Data $\to$ Algorithms $\to$ Prediction $\to$ Evaluation $\to$ Benchmark $\to$ Results.
- [ ] **Nhiệm vụ 2:** Chuẩn bị Slide thuyết trình bảo vệ đồ án (PPT) với các biểu đồ thực nghiệm sắc nét.
- [ ] **Nhiệm vụ 3:** Xây dựng kịch bản Demo hoàn chỉnh theo 3 phân cảnh: Live Prediction $\to$ Daily Backtest $\to$ Interactive Algorithm Benchmark.
- **Tiêu chí chấp nhận (Acceptance Criteria):**
  - Bộ tài liệu, slide và kịch bản demo sẵn sàng cho buổi bảo vệ đồ án.

---

## 4. Ma Trận Phụ Thuộc (Dependencies)

```text
Prediction Infrastructure (Xong)
        ↓
Phase A: Draw Modeling (Nghiên cứu & Thiết kế)
        ↓
Phase B: Draw Implementation & Metrics Evaluation
        ↓
Phase C: Full Multi-Day Backtest & Model Comparison
        ↓
Phase D: Algorithm Scalability Benchmark (1K -> 50K)
        ↓
Phase E: Analytics & Benchmark Visualization
        ↓
Phase F: Documentation, Slide PPT & Demo Flow
```

---

## 5. Tiêu Chuẩn Hoàn Thành (Definition of Done)
- [ ] 100% tài liệu thiết kế và báo cáo thực nghiệm được lưu trữ chuẩn mực trong `docs/`.
- [ ] Toàn bộ mã nguồn tuân thủ Clean Architecture và Design System quy chuẩn.
- [ ] Bộ kiểm thử `./gradlew testDebugUnitTest` đạt tỷ lệ Pass 100%.
- [ ] Bản build `./gradlew assembleDebug` thành công tuyệt đối, không có lỗi runtime.
- [ ] Không có thay đổi ngoài phạm vi và không có breaking changes trên hệ thống cũ.
