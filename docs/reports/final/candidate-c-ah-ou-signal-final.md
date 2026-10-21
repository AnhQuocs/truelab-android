# Báo Cáo Nghiệm Thu & Đóng Nghiên Cứu: Candidate C — Asian Handicap & Over/Under Signal

- **Giai đoạn:** Phase C — Odds-Derived Incremental Signal Evaluation
- **Trạng thái:** **CHÍNH THỨC ĐÓNG (CLOSED — EXPERIMENTAL EVALUATION COMPLETED)**
- **Quyết định chính thức:** **REJECT AS PRODUCTION DRAW REPLACEMENT / PRESERVE AS EXPERIMENTAL ADDITIVE SIGNAL**
  - **Candidate A** tiếp tục giữ vai trò mặc định Production cho Draw decision policy.
  - **Candidate B:** REJECTED / CLOSED.
  - **Candidate C:** Không đưa vào Production pipeline; giữ lại trong `:core:domain` dưới dạng module mở rộng phục vụ nghiên cứu độc lập.
  - **Không thay đổi 6 baseline weights, `WeightedScorer`, hay `PredictMatchOutcomeUseCase`.**

---

## 1. Mục Tiêu Nghiên Cứu (Objective)

Nghiên cứu khả năng khai thác thông tin chuyên sâu từ hai thị trường cược phụ phổ biến:
1. **Asian Handicap (AH — Cược chấp Châu Á):** Đo lường chênh lệch sức mạnh ngầm định (Implied Spread) giữa hai đội và phân bố xác suất 3 cửa tương ứng.
2. **Over/Under (O/U — Cược Tài/Xỉu tổng bàn thắng):** Ước tính tổng bàn thắng kỳ vọng ($\lambda_{\text{total}}$) nhằm điều biến xác suất hòa theo lý thuyết Poisson/bàn thắng thấp.

**Mục tiêu trọng tâm:** Xác định xem việc bổ sung tín hiệu AH ($S_{\text{ah}}$) và O/U ($S_{\text{ou}}$) vào mô hình dự đoán có giúp cải thiện năng lực phát hiện trận hòa (Draw F1 / Draw Recall) mà không làm suy giảm độ chính xác Home/Away và không phá vỡ tính ổn định của hệ thống hay không.

---

## 2. Giao Thức Thực Nghiệm Đăng Ký Trước (Experimental Protocol)

Toàn bộ quy trình thực nghiệm được đăng ký trước và tuân thủ nghiêm ngặt theo tài liệu [`docs/research/candidate-c-experimental-protocol.md`](../../research/candidate-c-experimental-protocol.md):

1. **Tập Dữ Liệu Lịch Sử & Phân Tách Thời Gian (Chronological Split):**
   - **Tổng Cohort:** 791 trận đấu có snapshot tỷ lệ cược Pre-match hợp lệ.
   - **Tập Calibration (60% đầu):** $N = 476$ trận (từ `2026-09-12T17:00:00Z` đến `2026-09-27T13:00:00Z`).
   - **Tập Frozen Holdout (40% sau):** $N = 315$ trận (từ `2026-09-27T13:00:00Z` đến `2026-10-06T23:30:00Z`).
   - *Quy tắc phân định:* Xác định phân tách thời gian nghiêm ngặt, 5 trận đấu tại mốc `2026-09-27T13:00:00Z` được phân bổ xác định vào tập Holdout; $100\%$ không sử dụng dữ liệu Holdout trong quá trình calibration.
2. **Không Gian 7 Biến Thể Đăng Ký Trước (7 Variants V0–V6):**
   - **V0:** Baseline 6 Signals + Raw Argmax
   - **V1:** Baseline 6 Signals + Candidate A (Active Production)
   - **V2:** Baseline 6 Signals + Candidate B ($\tau^* = 0.120$, Rejected)
   - **V3:** Baseline + Signal C1 (Asian Handicap) + Raw Argmax
   - **V4:** Baseline + Signal C2 (Over/Under) + Raw Argmax
   - **V5:** Baseline + Signal C1 + Signal C2 + Raw Argmax
   - **V6:** Baseline + Signal C1 + Signal C2 + Candidate A Policy

---

## 3. Quá Trình Hiệu Chuẩn Tham Số (Full 7D Joint Calibration)

Quá trình Joint Calibration được thực hiện quét toàn bộ không gian lưới 7 chiều ($50.000$ tổ hợp tham số $\boldsymbol{\theta} = (\alpha, \beta, \gamma, \eta, \kappa, w_{\text{c1}}, w_{\text{c2}})$) độc lập trên **Calibration Set ($N = 476$)** với hàm mục tiêu tối đa hóa Macro F1 và tie-break bằng việc tối thiểu hóa Multi-class Brier Score:

### Bộ Tham Số Tối Ưu Đã Đóng Băng ($\boldsymbol{\theta}^*$):
| Tham số | Ý nghĩa | Giá trị tối ưu $\boldsymbol{\theta}^*$ |
|:---:|---|:---:|
| $P_0(D)$ | Xác suất hòa cơ sở tiên nghiệm | **0.26** (Cố định) |
| $\alpha^*$ | Hệ số nhạy giá kèo Asian Handicap | **0.80** |
| $\beta^*$ | Hệ số độ dốc hàm Logistic Asian Handicap | **1.60** |
| $\gamma^*$ | Hệ số suy giảm hòa khi lệch kèo Asian Handicap | **0.50** |
| $\eta^*$ | Hệ số nhạy giá kèo Over/Under | **0.20** |
| $\kappa^*$ | Hệ số nhạy xác suất hòa theo tổng bàn thắng kỳ vọng $\lambda_{\text{total}}$ | **0.12** |
| $w_{\text{c1}}^*$ | Trọng số thực nghiệm của tín hiệu Asian Handicap | **0.20** |
| $w_{\text{c2}}^*$ | Trọng số thực nghiệm của tín hiệu Over/Under | **0.03** |

- **Kết quả trên Calibration ($N = 476$):**
  - Baseline V0: Accuracy = $49,37\%$, Macro F1 = $36,97\%$, Brier Score = $0.61314$.
  - Candidate C (V5): Accuracy = **51,68%** ($+2,31\%$), Macro F1 = **38,65%** ($+1,68\%$), Brier Score = **0.60002** (Độ hiệu chuẩn xác suất cải thiện).

---

## 4. Kết Quả Trên Frozen Holdout ($N = 315$)

Đánh giá một lần duy nhất trên tập Frozen Holdout độc lập với bộ tham số $\boldsymbol{\theta}^*$ đã đóng băng:

### 4.1. Bảng Tổng Hợp Toàn Diện 7 Biến Thể
| Biến thể | Cấu hình | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw Prec | Draw Rec | Draw TP/FP | H/A đúng bị đổi sai |
|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **V0** | Baseline Raw Argmax | 55,87% | 41,12% | 66,46% | 0,00% | 56,90% | 0,00% | 0,00% | 0 / 0 | 0 |
| **V1** | Baseline + **Candidate A** (Active Prod) | 53,65% | 47,76% | 63,79% | **24,35%** | 55,14% | 28,57% | **21,21%** | **14 / 35** | 21 |
| **V2** | Baseline + Candidate B (Rejected) | 54,60% | 47,80% | 65,36% | 21,43% | 56,60% | 26,09% | 18,18% | 12 / 34 | 16 |
| **V3** | Baseline + **AH (C1)** + Raw Argmax | **57,78%** | 42,35% | **69,28%** | 0,00% | 57,76% | 0,00% | 0,00% | 0 / 0 | 0 |
| **V4** | Baseline + **O/U (C2)** + Raw Argmax | 55,87% | 41,12% | 66,46% | 0,00% | 56,90% | 0,00% | 0,00% | 0 / 0 | 0 |
| **V5** | Baseline + **AH + O/U (C)** + Raw Argmax | **57,78%** | 42,35% | **69,28%** | 0,00% | 57,76% | 0,00% | 0,00% | 0 / 0 | 0 |
| **V6** | Baseline + **AH + O/U** + **Candidate A** | 56,51% | **49,05%** | 66,88% | 20,18% | **60,09%** | 25,58% | 16,67% | 11 / 32 | **15** |

### 4.2. Hiệu Năng Phân Tầng Theo Độ Phủ Dữ Liệu (Covered Subsets)
- **Tập có đầy đủ cả AH & O/U (Both-Covered, $N = 180 \dots 198$):**
  - V6 đạt **Accuracy = 57,22%** ($+5,55\%$ so với V1).
  - V6 đạt **Macro F1 = 50,06%** ($+3,98\%$ so với V1).
  - Số ca can thiệp sai làm hỏng dự đoán đúng (Degraded H/A) **giảm từ 9 xuống 4 trận** (giảm hơn $55\%$).

---

## 5. Kiểm Định Thống Kê (Paired Bootstrap $B = 10.000$)

| So sánh cặp | Chênh lệch trung bình ($\Delta \text{Macro F1}$) | Khoảng tin cậy $95\%$ CI | Đạt ý nghĩa thống kê? |
|---|:---:|:---:|:---:|
| **V5 - V0** (Candidate C Raw vs Baseline) | **+1,23%** | `[-0,46%, +2,99%]` | ❌ Chứa 0 |
| **V6 - V1** (Candidate C + A vs Candidate A) | **+1,29%** | `[-2,74%, +5,27%]` | ❌ Chứa 0 |

---

## 6. Kết Quả Kiểm Toán Xuất Xứ & Khả Năng Tái Lập (Provenance & Reproducibility Audit)

- **Audit Status:** **VERIFIED (100% REPRODUCIBLE)**
- Mọi mốc thời gian, bộ lọc Pre-match (`changeTime < kickoff_epoch`, loại bỏ Live Phases), hàm chuyển đổi Transformer (`AsianHandicapSignalTransformer`, `OverUnderSignalTransformer`) và Selector tỷ lệ cược chính (`PreMatchOddsSelector`) đều đạt $100\%$ tính nhất quán giữa tài liệu kỹ thuật, mã nguồn Kotlin và script đánh giá Python.
- $100\%$ Unit test trong `:core:domain` thực thi thành công (`BUILD SUCCESSFUL`).

---

## 7. Đánh Giá Bản Chất & Phát Hiện Chính (Findings)

1. **Candidate C mang lại giá trị gia tăng rõ rệt cho việc phân biệt Thắng / Thua (Home Win / Away Win):**
   - Tín hiệu Asian Handicap (AH) cung cấp thông tin thị trường sắc nét, giúp tăng tổng thể Accuracy từ $53,65\%$ lên $56,51\%$ ($+2,86\%$), tăng Home F1 ($+3,09\%$) và Away F1 ($+4,95\%$).
   - Giảm thiểu đáng kể số ca bắt nhầm hòa làm hỏng dự đoán Home/Away đúng (từ 21 xuống 15 trận trên toàn bộ holdout, và từ 9 xuống 4 trận trên tập có đầy đủ Odds).
2. **Candidate C thất bại trong vai trò phát hiện trận hòa (Draw Detector):**
   - Về mặt lý thuyết và thực nghiệm, bản chất tỷ lệ chấp Asian Handicap được thiết kế để phân cực và loại bỏ cửa hòa (hoặc hoàn tiền khi hòa kèo). Do đó, tín hiệu AH đẩy mạnh độ lệch $|P_H - P_A|$, vô tình triệt tiêu xác suất hòa ở tầng Raw Argmax (V5 Draw F1 = 0%) và làm giảm hiệu quả phát hiện hòa của Candidate A từ 14 TP xuống 11 TP.
3. **Cỡ mẫu hiện tại ($N = 315$) chưa đủ sức mạnh thống kê:**
   - Mặc dù mức tăng điểm tuyệt đối là tích cực ($\Delta \text{Macro F1} = +1,29\%$), khoảng tin cậy $95\%$ CI vẫn chứa 0 do phương sai mẫu còn lớn.

---

## 8. Quyết Định Nghiệm Thu Cuối Cùng (Final Decision)

$$\mathbf{Quyết\ Định:\ REJECT\ AS\ PRODUCTION\ DRAW\ REPLACEMENT}$$
$$\mathbf{Trạng\ Thái\ Phase\ C:\ CHÍNH\ THỨC\ ĐÓNG\ (CLOSED)}$$

1. **Quyết định đối với Production Pipeline:**
   - **Candidate A tiếp tục là chính sách mặc định duy nhất trong Production.**
   - Không thay thế Candidate A bằng Candidate C.
   - Không đưa Candidate C vào pipeline chính thức ở thời điểm hiện tại.
   - Không thay đổi 6 trọng số baseline, `WeightedScorer`, hay `PredictMatchOutcomeUseCase`.
2. **Bảo tồn mã nguồn & Tài nguyên nghiên cứu:**
   - Toàn bộ logic chuẩn hóa Domain gồm `AsianHandicapSignalTransformer`, `OverUnderSignalTransformer`, `CandidateCConfig`, và `PreMatchOddsSelector` được giữ lại hoàn chỉnh trong `:core:domain` với kiểm thử đầy đủ làm nền tảng cho các nghiên cứu mô hình hóa nâng cao sau này.

---

## 9. Tác Động Tới Production (Production Impact)

- **Zero Breaking Changes:** Không có bất kỳ thay đổi nào ảnh hưởng đến API hay hành vi runtime hiện hành của ứng dụng TrueLab.
- **Zero Performance Regression:** Không có tác động tiêu cực đến tốc độ tính toán hay thời gian khởi chạy ứng dụng.

---

## 10. Định Hướng Nghiên Cứu Tương Lai (Future Research Directions)

1. **Hiệu chỉnh xác suất (Probability Calibration & Uncertainty Estimation):** Nghiên cứu các phương pháp như Isotonic Regression, Temperature Scaling hoặc Dirichlet Calibration để xác suất đầu ra phản ánh chính xác độ tin cậy thực tế.
2. **Mô hình hóa Tương tác Đặc trưng (Feature Interaction Modeling):** Khám phá mô hình học máy phi tuyến (như Gradient Boosted Trees hoặc Neural Scoring) để xử lý các vùng mâu thuẫn giữa tín hiệu thống kê cơ sở và tỷ lệ cược thị trường thay vì cộng trọng số tuyến tính cố định.
3. **Nghiên cứu Tối ưu hóa Accuracy (Accuracy Optimization Objective):** Thiết kế mục tiêu nghiên cứu chuyên biệt tập trung vào việc tối đa hóa độ chính xác phân loại 3 cửa (3-way Accuracy) khi quy mô dữ liệu lịch sử đạt $N \ge 1.500$ trận.
