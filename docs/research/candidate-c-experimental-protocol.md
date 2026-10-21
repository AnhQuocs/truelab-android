# Giao Thức Thực Nghiệm Candidate C: Asian Handicap & Over/Under Incremental Signal (Candidate C Experimental Protocol)

> **Trạng thái:** DRAFT — Giao thức Thực nghiệm Đóng băng (Pre-Registration & Experimental Protocol)  
> **Mục tiêu:** Thiết lập quy chuẩn phân rã dữ liệu (Temporal Split), quy trình hiệu chuẩn tham số (Calibration Protocol), điều kiện khóa tập kiểm thử (Holdout Freeze Rule), và tiêu chí nghiệm thu (Acceptance Criteria & Final Decision Gate) cho nghiên cứu **Candidate C (AH & O/U Incremental Signal)** trước khi triển khai code.  
> **Ràng buộc nghiên cứu:** **CHỈ LẬP TÀI LIỆU.** Tuyệt đối không thay đổi mã nguồn, không chạy calibration trên Holdout, không thay đổi trọng số Production hiện tại.

---

## 1. Phân Tách Dữ Liệu Theo Thời Gian (Temporal Split Protocol)

### 1.1. Đánh Giá Khả Năng Tái Sử Dụng Split Từ Phase B (Candidate B)
Trong nghiên cứu Phase B (Candidate B — Relative Draw Gap), tập dữ liệu **Frozen 791 Cohort** (khung thời gian `2026-09-12T17:00:00Z` $\to$ `2026-10-06T23:30:00Z`) đã được phân tách theo trục thời gian với mốc cutoff cố định:

$$\text{Temporal Cutoff} = \mathbf{2026\text{-}09\text{-}27T13:00:00Z}$$

- **Tập Calibration (60% đầu theo thời gian):** **476 trận** (`2026-09-12T17:00:00Z` $\to$ `2026-09-27T13:00:00Z`).
- **Tập Frozen Holdout (40% sau theo thời gian):** **315 trận** (`2026-09-27T13:00:00Z` $\to$ `2026-10-06T23:30:00Z`).

### 1.2. Quyết Định Chính Thức
> **QUYẾT ĐỊNH:** **TÁI SỬ DỤNG 100% TEMPORAL SPLIT TỪ PHASE B CHO CANDIDATE C.**

**Lý do:**
1. **Đảm bảo khả năng so sánh đối đầu trực tiếp (Direct Apples-to-Apples Comparison):** Việc dùng chung tập Frozen Holdout $N = 315$ cho phép đối soát trực tiếp kết quả của Candidate C với Baseline ($N=315$), Candidate A ($N=315$), và Candidate B ($N=315$) mà không chịu ảnh hưởng từ sự sai lệch mẫu.
2. **Bảo đảm tính toàn vẹn chuỗi thời gian (Zero Lookahead / Future Leakage):** Mọi quyết định tối ưu tham số chỉ được đưa ra dựa trên các trận đấu diễn ra trước ngày `2026-09-27T13:00:00Z`.

### 1.3. Phân Bố Độ Phủ Thị Trường Trên Từng Tập
Căn cứ theo kiểm toán độ phủ từ [`docs/research/odds-normalization-data-contract.md`](odds-normalization-data-contract.md), phân bố các tập con cụ thể như sau:

| Tập dữ liệu | Tổng số trận | AH Coverage (`asia`) | O/U Coverage (`bs`) | Both Coverage (AH + O/U) |
|---|:---:|:---:|:---:|:---:|
| **Toàn bộ Cohort (Full 791)** | 791 (100%) | 645 (81,54%) | 553 (69,91%) | 497 (62,83%) |
| **Calibration Set (Pre-split)** | **476 (60,18%)** | **388 (81,51%)** | **332 (69,75%)** | **299 (62,82%)** |
| **Frozen Holdout Set (Test)** | **315 (39,82%)** | **257 (81,59%)** | **221 (70,16%)** | **198 (62,86%)** |

*(Nhận xét: Tỷ lệ độ phủ trên Calibration và Holdout hoàn toàn đồng nhất $\approx 81,5\%$ cho AH và $\approx 70,0\%$ cho O/U, khẳng định tính đại diện và ổn định của mốc cutoff).*

---

## 2. Quy Trình Hiệu Chuẩn Tham Số (Calibration Protocol)

### 2.1. Danh Mục Các Tham Số Cần Hiệu Chuẩn
Toàn bộ các tham số dưới đây được xếp loại là `TBD / CALIBRATION PARAMETERS` và bắt buộc phải được xác định duy nhất trên **Calibration Set ($N = 476$)**:

1. **Nhóm tham số Tín hiệu Asian Handicap (Signal C1):**
   - $\alpha$: Hệ số độ nhạy giá AH (điều chỉnh chênh lệch giá hai cửa so với mốc chấp $H_{\text{line}}$).
   - $\beta$: Hệ số độ dốc Logistic (ánh xạ độ lệch ưu thế $\Delta_{\text{AH}}$ sang xác suất Thắng/Thua).
   - $\gamma$: Hệ số suy giảm xác suất Hòa khi độ lệch chấp bàn $\Delta_{\text{AH}}$ tăng cao.
   - $w_{\text{c1}}$: Trọng số thực nghiệm của tín hiệu AH ($w_{\text{ah}}$).
2. **Nhóm tham số Tín hiệu Over/Under (Signal C2):**
   - $\eta$: Hệ số độ nhạy giá Tài/Xỉu (điều chỉnh mốc bàn thắng $G_{\text{line}}$ theo chênh lệch giá Over/Under).
   - $\kappa$: Hệ số nhạy xác suất Hòa theo tổng bàn thắng kỳ vọng $\lambda_{\text{total}}$.
   - $w_{\text{c2}}$: Trọng số thực nghiệm của tín hiệu O/U ($w_{\text{ou}}$).
3. **Tham số Hằng số Kế thừa (Fixed Baseline Constant):**
   - $P_0(D) = 0.26$ (Kế thừa cố định từ `PredictionWeightConfig.baselineDrawProb`, không tune).

### 2.2. Không Gian Tìm Kiếm & Lưới Tham Số Ứng Viên (Candidate Grid)

```text
Không gian quét lưới trên Calibration Set (Grid Search Range):
├── α   ∈ [0.20, 0.40, 0.50, 0.60, 0.80]
├── β   ∈ [0.80, 1.00, 1.20, 1.40, 1.60]
├── γ   ∈ [0.10, 0.20, 0.30, 0.40, 0.50]
├── η   ∈ [0.20, 0.40, 0.50, 0.60, 0.80]
├── κ   ∈ [0.04, 0.06, 0.08, 0.10, 0.12]
├── w_c1∈ [0.05, 0.10, 0.15, 0.20]
└── w_c2∈ [0.03, 0.05, 0.08, 0.10]
```

### 2.3. Hàm Mục Tiêu & Ràng Buộc Hiệu Chuẩn (Calibration Objective)
Bộ tham số tối ưu $(\alpha^*, \beta^*, \gamma^*, \eta^*, \kappa^*, w_{\text{c1}}^*, w_{\text{c2}}^*)$ được lựa chọn theo tiêu chí:

$$\boldsymbol{\theta}^* = \arg\max_{\boldsymbol{\theta}} \text{Macro F1}_{\text{cal}}(\boldsymbol{\theta})$$

**Thỏa mãn các ràng buộc Non-inferiority trên Calibration Set:**
1. $\text{Accuracy}_{\text{cal}}(\boldsymbol{\theta}) \ge \text{Accuracy}_{\text{cal}}(\text{Baseline}) - 0.50\%$
2. $\text{Home F1}_{\text{cal}}(\boldsymbol{\theta}) \ge \text{Home F1}_{\text{cal}}(\text{Baseline}) - 1.00\%$
3. $\text{Away F1}_{\text{cal}}(\boldsymbol{\theta}) \ge \text{Away F1}_{\text{cal}}(\text{Baseline}) - 1.00\%$
4. $\text{Multi-class Brier Score}_{\text{cal}}(\boldsymbol{\theta}) \le \text{Brier Score}_{\text{cal}}(\text{Baseline})$

---

## 3. Quy Ước Đăng Ký Trước (Pre-Registration Specifications)

Toàn bộ các mục dưới đây được **ĐÓNG BĂNG TUYỆT ĐỐI (FROZEN)** trước khi chạy đánh giá trên Holdout Set:

### 3.1. Danh Mục 7 Biến Thể Đánh Giá (Pre-Registered Variants V0 → V6)
1. **V0 (Baseline + Raw Argmax):** Hệ thống 6 tín hiệu chuẩn, ra quyết định bằng $\arg\max(P_H, P_D, P_A)$.
2. **V1 (Baseline + Candidate A):** Hệ thống 6 tín hiệu chuẩn kết hợp Decision Margin ($\delta = 0.04, \theta = 0.255$) — **Chuẩn Production Active**.
3. **V2 (Baseline + Candidate B):** Hệ thống 6 tín hiệu chuẩn kết hợp Relative Draw Gap ($\tau = 0.120$) — **Mốc lịch sử Rejected**.
4. **V3 (Baseline + AH Signal C1):** 7 tín hiệu (Baseline scaled + AH với $w_{\text{c1}}^*$), quyết định bằng Raw Argmax.
5. **V4 (Baseline + O/U Signal C2):** 7 tín hiệu (Baseline scaled + O/U với $w_{\text{c2}}^*$), quyết định bằng Raw Argmax.
6. **V5 (Baseline + Candidate C Joint):** 8 tín hiệu (Baseline scaled + AH + O/U với $w_{\text{c1}}^*, w_{\text{c2}}^*$), quyết định bằng Raw Argmax.
7. **V6 (Baseline + Candidate C + Candidate A):** 8 tín hiệu kết hợp với chính sách quyết định hòa Candidate A.

### 3.2. Hệ Thống Chỉ Số Đánh Giá (Evaluation Metrics Hierarchy)
- **Chỉ số chính (Primary Metrics):**
  - Macro F1-Score (%)
  - Overall Accuracy (%)
  - Draw F1-Score (%)
- **Chỉ số phụ (Secondary Metrics):**
  - Home F1-Score (%), Away F1-Score (%)
  - Draw Precision (%), Draw Recall (%)
  - Draw True Positives (TP), Draw False Positives (FP)
  - Số lượng dự đoán đúng Home/Away bị đổi sai thành Draw (Degraded Correct H/A)
  - Multi-class Brier Score (Đo lường độ hiệu chỉnh xác suất)
- **Không gian phân tích:** Báo cáo trên 4 tập: `Full 315`, `AH-Covered (257)`, `O/U-Covered (221)`, `Both-Covered (198)`.

### 3.3. Kiểm Định Ý Nghĩa Thống Kê (Statistical Significance & Robustness Check)
- **Paired Bootstrap Resampling:** $B = 10.000$ iterations với $95\%$ Confidence Interval (CI) cho chênh lệch $\Delta \text{Macro F1} = \text{Macro F1}_{\text{V5/V6}} - \text{Macro F1}_{\text{V0/V1}}$.
- **McNemar Test:** Đánh giá mức độ khác biệt có ý nghĩa thống kê giữa các dự đoán đúng/sai của V5/V6 so với V1 ($p\text{-value} < 0.05$).

---

## 4. Quy Tắc Khóa Tập Kiểm Thử (Holdout Freeze Rule)

> **RÀNG BUỘC KHOA HỌC BẤT BIẾN:**  
> Sau khi tập dữ liệu **Frozen Holdout ($N = 315$)** được nạp và chạy đánh giá, **NGHIÊM CẤM 100% CÁC HÀNH VI SAU**:

1. Không được thay đổi công thức tính toán của $\Delta_{\text{AH}}, \text{Logistic}, \lambda_{\text{total}}, P(D)$.
2. Không được điều chỉnh, "retune" hoặc sửa đổi bất kỳ giá trị nào trong bộ tham số $\boldsymbol{\theta}^* = (\alpha^*, \beta^*, \gamma^*, \eta^*, \kappa^*, w_{\text{c1}}^*, w_{\text{c2}}^*)$.
3. Không được thay đổi mốc thời gian phân tách `Temporal Cutoff` hoặc chuyển trận đấu giữa Calibration và Holdout.
4. Không được thay đổi định nghĩa của 7 biến thể V0 → V6.
5. Không được thay đổi tiêu chí nghiệm thu (Acceptance Criteria) sau khi đã biết kết quả.

---

## 5. Cổng Quyết Định Nghiệm Thu (Final Decision Gate)

Dựa trên kết quả thực nghiệm trên tập Frozen Holdout $N = 315$, kết luận cuối cùng cho Candidate C sẽ rơi vào đúng một trong 3 nhánh:

```text
                                 [Holdout Evaluation Results (N = 315)]
                                                   │
                       ┌───────────────────────────┼───────────────────────────┐
                       ▼                           ▼                           ▼
            [ACCEPT / CONTINUE]             [REJECT / CLOSE]            [INCONCLUSIVE]
         - Macro F1 tăng có ý nghĩa      - Macro F1 không tăng       - Kết quả mâu thuẫn
         - Draw F1 cải thiện             - H/A F1 bị suy giảm        - 95% CI chứa 0
         - H/A Non-inferiority đạt       - Tỷ lệ FP/TP xấu đi        - Cần thêm dữ liệu
```

### 5.1. Nhánh ACCEPT / CONTINUE (Chấp thuận để xem xét Production)
Điều kiện tiên quyết (Thỏa mãn đồng thời):
1. **Incremental Predictive Value:** $\text{Macro F1}(\text{V5}) > \text{Macro F1}(\text{V0})$ VÀ $\text{Macro F1}(\text{V6}) \ge \text{Macro F1}(\text{V1})$ trên tập Holdout.
2. **Draw Quality:** $\text{Draw F1}(\text{V6}) \ge \text{Draw F1}(\text{V1})$ ($24,35\%$) VÀ $\text{Draw TP}(\text{V6}) \ge 14$ mà không làm tăng đột biến Draw FP ($\text{FP} \le 36$).
3. **Non-inferiority trên Home/Away:**
   - $\text{Home F1}(\text{V6}) \ge \text{Home F1}(\text{V1}) - 0.50\%$ (tối thiểu $\ge 63,29\%$).
   - $\text{Away F1}(\text{V6}) \ge \text{Away F1}(\text{V1}) - 0.50\%$ (tối thiểu $\ge 54,64\%$).
4. **Ý nghĩa thống kê:** Khoảng tin cậy $95\%$ Bootstrap của $\Delta \text{Macro F1}$ nằm hoàn toàn ở miền dương ($> 0$).

### 5.2. Nhánh REJECT / CLOSE (Bác bỏ & Đóng nghiên cứu)
Kích hoạt khi vi phạm một trong các điều sau:
1. Candidate C không tạo ra cải thiện Macro F1 so với Baseline V0 trên Holdout ($\Delta \text{Macro F1} \le 0$).
2. Candidate C kết hợp policy (V6) cho Draw F1 hoặc Accuracy thấp hơn Candidate A (V1).
3. Suy giảm độ chính xác Home/Away vượt quá biên non-inferiority ($\Delta \text{Home/Away F1} < -0.50\%$).
4. Số ca can thiệp sai làm hỏng dự đoán Home/Away vốn đúng (Degraded Correct H/A) tăng cao bất hợp lý.

### 5.3. Nhánh INCONCLUSIVE (Chưa đủ bằng chứng kết luận)
Kích hoạt khi:
- Macro F1 có cải thiện nhẹ nhưng khoảng tin cậy $95\%$ Bootstrap chứa $0$.
- Hiệu năng phân hóa mạnh giữa các giải đấu do cỡ mẫu bị giới hạn trên tập con Covered ($N=198$).
- *Hành động:* Giữ nguyên Candidate A ở Production; hoãn việc triển khai Candidate C cho đến khi tích lũy đủ $N \ge 1.500$ trận với độ phủ Odds cao hơn.

---

## 6. Trạng Thái Các Quyết Định Nghiên Cứu (Status of Open Decisions)

| Quyết định nghiên cứu | Trạng thái | Giải pháp / Quy chuẩn chốt |
|---|:---:|---|
| **Temporal Split Strategy** | **RESOLVED** | Tái sử dụng $100\%$ mốc cutoff `2026-09-27T13:00Z` (476 Cal / 315 Holdout). |
| **Data Leakage Prevention** | **RESOLVED** | Enforce `changeTime < kickoffEpoch` và loại bỏ market phase live. |
| **Multi-line Selection** | **RESOLVED** | Canonical Main Line (chọn line có $|\text{price}_1 - \text{price}_2|$ nhỏ nhất). |
| **Danh mục 7 Biến thể V0 → V6** | **RESOLVED & FROZEN** | Đã định nghĩa và đóng băng trong Mục 3.1. |
| **Bộ tiêu chí nghiệm thu & Thresholds** | **RESOLVED & FROZEN** | Đã khóa trong Mục 5.1 và 5.2. |
| **Giá trị tham số cụ thể ($\boldsymbol{\theta}^*$)** | **TBD (Pending Calibration)** | Sẽ được xác định qua bước quét lưới trên tập Calibration ($N=476$) trước khi chạy Holdout. |
