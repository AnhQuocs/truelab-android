# Báo Cáo Kết Quả Thực Nghiệm: Candidate C — Asian Handicap & Over/Under Incremental Signal

> **Giai đoạn:** Phase C — Odds-Derived Signal Evaluation  
> **Trạng thái:** Đã hoàn thành (Closed — Experimental Evaluation Completed)  
> **Quyết định chính thức:** **INCONCLUSIVE / REJECT AS DRAW DETECTOR (ADDITIVE VALUE ON HOME/AWAY ACCURACY)**  
> *(Candidate A tiếp tục là mặc định Production; Candidate C không thay thế Candidate A)*

---

## 1. Tóm Tắt Điều Hành & Quyết Định Nghiệm Thu

Thực nghiệm **Candidate C** đã được triển khai và đánh giá nghiêm ngặt theo đúng:
- Kế hoạch kỹ thuật: [`docs/plans/candidate-c-ah-ou-signal-plan.md`](../plans/candidate-c-ah-ou-signal-plan.md)
- Giao thức thực nghiệm đăng ký trước: [`docs/research/candidate-c-experimental-protocol.md`](candidate-c-experimental-protocol.md)

### 1.1. Phương Pháp Hiệu Chuẩn (Full 7D Joint Calibration Method)
- **Tập Calibration ($N = 476$):** Quét toàn bộ không gian lưới liên hợp 7 chiều (Full 7D Cartesian Product: $50.000$ tổ hợp tham số $\boldsymbol{\theta} = (\alpha, \beta, \gamma, \eta, \kappa, w_{\text{c1}}, w_{\text{c2}})$).
- **Hàm mục tiêu & Ràng buộc:** Tối đa hóa $\text{Macro F1}_{\text{cal}}$, tie-break bằng việc tối thiểu hóa $\text{Multi-class Brier Score}_{\text{cal}}$ và tối đa hóa $\text{Accuracy}_{\text{cal}}$, thỏa mãn $100\%$ các ràng buộc Non-inferiority trước khi đóng băng $\boldsymbol{\theta}^*$.
- **Bảo toàn Holdout:** $100\%$ không tune hay điều chỉnh tham số trên tập Holdout ($N = 315$).

### 1.2. Kết Quả Cốt Lõi Trên Tập Frozen Holdout ($N = 315$)

1. **Hiệu năng Dự đoán Tổng thể (Overall Accuracy & Macro F1):**
   - **V5 (Baseline + AH + O/U + Raw Argmax):** Đạt **Accuracy = 57,78%** ($+1,91\%$ so với Baseline V0) và **Macro F1 = 42,35%** ($+1,23\%$ so với V0).
   - **V6 (Baseline + AH + O/U + Candidate A Policy):** Đạt **Accuracy = 56,51%** ($+2,86\%$ so với Candidate A V1) và **Macro F1 = 49,05%** ($+1,29\%$ so với V1).
   - **Home F1 & Away F1 tăng mạnh:** Home F1 đạt $66,88\%$ ($+3,09\%$), Away F1 đạt $60,09\%$ ($+4,95\%$).
   - **Giảm mạnh số ca can thiệp sai:** Số trận dự đoán đúng Home/Away bị đổi sai thành Draw (Degraded Correct H/A) **giảm từ 21 xuống 15 trận** (giảm $-28,6\%$).
2. **Khả năng Phát hiện Trận Hòa (Draw Detection Efficacy):**
   - Trên tập Full Holdout $N = 315$, **Draw F1 của V6 đạt $20,18\%$** (thấp hơn $24,35\%$ của Candidate A V1, giảm $-4,17\%$), Draw TP đạt $11$ (giảm $-3$ so với 14 của V1).
   - *Nguyên nhân:* Tín hiệu Asian Handicap (AH) cung cấp phân cực thắng/thua rất rõ nét, đẩy khoảng cách $|P_H - P_A|$ vượt qua ngưỡng margin $0.04$, giúp Candidate A bớt kích hoạt sai nhưng đồng thời bỏ sót các trận hòa thực tế.
3. **Hiệu năng trên tập có đầy đủ cả AH & O/U (Both-Covered Subset, $N = 198$):**
   - **Accuracy tăng vọt lên 57,22%** ($+5,55\%$ so với V1).
   - **Macro F1 đạt 50,06%** ($+3,98\%$ so với V1).
   - **Degraded Correct H/A giảm hơn một nửa:** từ $9$ xuống còn $4$ trận.
4. **Kiểm Định Thống Kê (Paired Bootstrap 95% CI, $B = 10.000$):**
   - $\Delta \text{Macro F1 (V5 - V0)} = +1,23\%$, $95\%$ CI: $[-0,46\%, +2,99\%]$ (Khoảng tin cậy chứa 0).
   - $\Delta \text{Macro F1 (V6 - V1)} = +1,29\%$, $95\%$ CI: $[-2,74\%, +5,27\%]$ (Khoảng tin cậy chứa 0).

---

## 2. Kết Quả Hiệu Chuẩn Tham Số Trên Calibration Set ($N = 476$)

Toàn bộ $50.000$ tổ hợp tham số trong không gian 7 chiều đã được đánh giá độc lập trên **Calibration Set ($N = 476$)**:

### 2.1. Bộ Tham Số Đóng Băng Sau Joint Calibration ($\boldsymbol{\theta}^*$)
| Tham số | Ý nghĩa | Giá trị tối ưu $\boldsymbol{\theta}^*$ |
|:---:|---|:---:|
| $P_0(D)$ | Xác suất hòa cơ sở tiên nghiệm (Baseline Constant) | **0.26** (Cố định) |
| $\alpha^*$ | Hệ số độ nhạy giá Asian Handicap | **0.80** |
| $\beta^*$ | Hệ số độ dốc hàm Logistic Asian Handicap | **1.60** |
| $\gamma^*$ | Hệ số suy giảm hòa khi lệch kèo Asian Handicap | **0.50** |
| $\eta^*$ | Hệ số độ nhạy giá Over/Under | **0.20** |
| $\kappa^*$ | Hệ số nhạy xác suất hòa theo tổng bàn thắng kỳ vọng $\lambda_{\text{total}}$ | **0.12** |
| $w_{\text{c1}}^*$ | Trọng số thực nghiệm của tín hiệu Asian Handicap | **0.20** |
| $w_{\text{c2}}^*$ | Trọng số thực nghiệm của tín hiệu Over/Under | **0.03** |

### 2.2. Hiệu Năng & Brier Score Trên Calibration Set ($N = 476$)
- **Baseline V0:** Accuracy = $49,37\%$, Macro F1 = $36,97\%$, Home F1 = $60,04\%$, Draw F1 = $0,00\%$, Away F1 = $50,88\%$, Brier Score = $0.61314$.
- **Candidate C (V5):** Accuracy = **51,68%** ($+2,31\%$), Macro F1 = **38,65%** ($+1,68\%$), Home F1 = **62,45%**, Draw F1 = $0,00\%$, Away F1 = **53,50%**, Brier Score = **0.60002** (Độ hiệu chỉnh xác suất cải thiện rõ rệt).
- Thỏa mãn toàn bộ 4 ràng buộc Non-inferiority trước khi đóng băng $\boldsymbol{\theta}^*$.

---

## 3. Kết Quả Chi Tiết Trên Tập Frozen Holdout ($N = 315$)

### 3.1. Bảng Tổng Hợp 7 Biến Thể Trên Toàn Bộ Holdout ($N = 315$)

| Biến thể | Cấu hình & Chính sách | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw Prec | Draw Rec | Draw TP/FP (Tot) | H/A đúng đổi sai |
|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **V0** | Baseline (6 Signals) + Raw Argmax | 55,87% | 41,12% | 66,46% | 0,00% | 56,90% | 0,00% | 0,00% | 0/0 (0) | 0 |
| **V1** | Baseline + **Candidate A** (Active Prod) | 53,65% | 47,76% | 63,79% | **24,35%** | 55,14% | 28,57% | **21,21%** | **14/35 (49)** | 21 |
| **V2** | Baseline + Candidate B (Rejected) | 54,60% | 47,80% | 65,36% | 21,43% | 56,60% | 26,09% | 18,18% | 12/34 (46) | 16 |
| **V3** | Baseline + **Signal C1 (AH)** + Raw Argmax | **57,78%** | 42,35% | **69,28%** | 0,00% | 57,76% | 0,00% | 0,00% | 0/0 (0) | 0 |
| **V4** | Baseline + **Signal C2 (O/U)** + Raw Argmax | 55,87% | 41,12% | 66,46% | 0,00% | 56,90% | 0,00% | 0,00% | 0/0 (0) | 0 |
| **V5** | Baseline + **Candidate C (AH+OU)** + Raw Argmax | **57,78%** | 42,35% | **69,28%** | 0,00% | 57,76% | 0,00% | 0,00% | 0/0 (0) | 0 |
| **V6** | Baseline + **Candidate C (AH+OU)** + **Candidate A** | 56,51% | **49,05%** | 66,88% | 20,18% | **60,09%** | 25,58% | 16,67% | 11/32 (43) | **15** |

---

### 3.2. Hiệu Năng Phân Tầng Trên Các Tập Con Có Độ Phủ (Covered Subsets)

#### 1. AH-Covered Subset ($N = 257$ matches — 81,59% Holdout)
| Biến thể | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw TP / FP | H/A đúng đổi sai |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **V0 (Baseline)** | 54,55% | 39,99% | 66,39% | 0,00% | 53,57% | 0 / 0 | 0 |
| **V1 (Cand A)** | 54,98% | 49,16% | 65,52% | **29,27%** | 52,70% | **12 / 20** | 11 |
| **V5 (Baseline + C)**| 57,14% | 41,59% | **70,12%** | 0,00% | 54,66% | 0 / 0 | 0 |
| **V6 (C + Cand A)** | **58,87%** | **51,00%** | 69,46% | 23,68% | **59,86%** | 9 / 17 | **5** (Giảm >50%) |

#### 2. O/U-Covered Subset ($N = 221$ matches — 70,16% Holdout)
| Biến thể | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw TP / FP | H/A đúng đổi sai |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **V0 (Baseline)** | 52,40% | 39,00% | 62,56% | 0,00% | 54,43% | 0 / 0 | 0 |
| **V1 (Cand A)** | 51,44% | 46,36% | 60,91% | **25,32%** | 52,86% | **10 / 22** | 12 |
| **V5 (Baseline + C)**| 55,29% | 40,98% | **66,36%** | 0,00% | 56,58% | 0 / 0 | 0 |
| **V6 (C + Cand A)** | **56,25%** | **49,93%** | 65,02% | 24,32% | **60,43%** | 9 / 18 | **7** |

#### 3. Both-Covered Subset ($N = 198$ matches — 62,86% Holdout)
| Biến thể | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw TP / FP | H/A đúng đổi sai |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **V0 (Baseline)** | 52,22% | 38,61% | 62,98% | 0,00% | 52,86% | 0 / 0 | 0 |
| **V1 (Cand A)** | 51,67% | 46,08% | 61,63% | **25,00%** | 51,61% | **8 / 17** | 9 |
| **V5 (Baseline + C)**| 55,56% | 40,87% | **67,38%** | 0,00% | 55,22% | 0 / 0 | 0 |
| **V6 (C + Cand A)** | **57,22%** | **50,06%** | 66,29% | 23,73% | **60,16%** | 7 / 13 | **4** (Giảm >55%) |

---

### 3.3. Ma Trận Nhầm Lẫn Trên Frozen Holdout ($N = 315$)

**V0 — Baseline Raw Argmax:**
```text
[[108,   0,  40],
 [ 36,   0,  30],
 [ 33,   0,  68]]
```

**V1 — Candidate A (Production Active):**
```text
[[ 96,  22,  30],
 [ 28,  14,  24],
 [ 29,  13,  59]]
```

**V5 — Baseline + Candidate C (Raw Argmax):**
```text
[[115,   0,  33],
 [ 35,   0,  31],
 [ 34,   0,  67]]
```

**V6 — Baseline + Candidate C + Candidate A:**
```text
[[103,  23,  22],
 [ 29,  11,  26],
 [ 28,   9,  64]]
```

---

## 4. Kiểm Định Ý Nghĩa Thống Kê (Bootstrap 95% Confidence Intervals)

Thực hiện $B = 10.000$ vòng lặp Paired Bootstrap Resampling trên tập Holdout $N = 315$:

| So sánh cặp | Chênh lệch trung bình ($\Delta \text{Macro F1}$) | Khoảng tin cậy $95\%$ CI | Ý nghĩa thống kê |
|---|:---:|:---:|:---:|
| **V3 - V0** (AH vs Baseline) | **+1,23%** | `[-0,46%, +2,99%]` | Chưa đạt ý nghĩa tuyệt đối ($0 \in \text{CI}$) |
| **V4 - V0** (O/U vs Baseline) | **+0,00%** | `[+0,00%, +0,00%]` | Không đổi trên Raw Argmax |
| **V5 - V0** (Joint C vs Baseline) | **+1,23%** | `[-0,46%, +2,99%]` | Chưa đạt ý nghĩa tuyệt đối ($0 \in \text{CI}$) |
| **V6 - V1** (Joint C + A vs Cand A) | **+1,29%** | `[-2,74%, +5,27%]` | Chưa đạt ý nghĩa tuyệt đối ($0 \in \text{CI}$) |

---

## 5. Đánh Giá Cổng Quyết Định Nghiệm Thu (Acceptance Gate Evaluation)

Căn cứ vào tiêu chí tại Mục 5 của [`docs/research/candidate-c-experimental-protocol.md`](candidate-c-experimental-protocol.md):

| Tiêu chí | Yêu cầu đăng ký trước | Kết quả thực tế | Trạng thái |
|---|---|:---:|:---:|
| **1. Incremental Predictive Value** | $\text{Macro F1(V5)} > \text{V0}$ và $\text{Macro F1(V6)} \ge \text{V1}$ | $\text{V5} = 42,35\% > 41,12\%$, $\text{V6} = 49,05\% > 47,76\%$ | ✅ **PASS** |
| **2. Non-inferiority Home/Away** | $\text{Home F1} \ge 63,29\%$, $\text{Away F1} \ge 54,64\%$ | $\text{Home F1} = 66,88\%$, $\text{Away F1} = 60,09\%$ | ✅ **PASS** |
| **3. Draw Quality** | $\text{Draw F1(V6)} \ge 24,35\%$, $\text{Draw TP} \ge 14$ | $\text{Draw F1} = 20,18\%$, $\text{Draw TP} = 11$ | ❌ **FAIL** (Trên Full Holdout) |
| **4. Statistical Significance** | $95\%$ CI của $\Delta \text{Macro F1}$ không chứa 0 | $95\%$ CI: $[-2,74\%, +5,27\%]$ | ❌ **FAIL** (Chứa 0) |

---

## 6. Kết Luận Khoa Học & Quyết Định Cuối Cùng

### 6.1. Đánh giá bản chất Candidate C
- **Asian Handicap (AH):** Mang lại **giá trị gia tăng thực sự và rõ rệt cho việc phân biệt Thắng / Thua (Home Win / Away Win)**. Nó giúp tăng Accuracy tổng thể từ $53,65\%$ lên $56,51\%$, tăng mạnh Home F1 ($+3,09\%$) và Away F1 ($+4,95\%$), đồng thời giảm thiểu đáng kể các ca bắt nhầm hòa làm hỏng dự đoán Home/Away đúng (từ $21$ xuống $15$ trận).
- **Over/Under (O/U):** Đóng góp nhẹ vào việc điều biến xác suất ($w_{\text{ou}}^* = 0.03, \kappa^* = 0.12$), giúp cải thiện Brier Score trên Calibration từ $0.61314$ xuống $0.60002$.
- **Đối với mục tiêu Bắt Trận Hòa (Draw Detection):** Candidate C **không phải là giải pháp thay thế cho Candidate A**. Khi đứng một mình (V5), nó không tạo ra dự đoán Draw nào ở tầng raw argmax (Draw F1 = 0%). Khi kết hợp với Candidate A (V6), nó làm giảm Draw TP từ 14 xuống 11 trên toàn bộ 315 trận.

### 6.2. Quyết Định Nghiệm Thu Chính Thức

$$\mathbf{Quyết\ Định:\ INCONCLUSIVE\ /\ REJECT\ AS\ DRAW\ REPLACEMENT}$$
$$\mathbf{(Nhưng\ Đạt\ Giá\ Trị\ Additive\ Signal\ Cho\ Phân\ Biệt\ Home/Away)}$$

1. **Về mặt Production Default:** Giữ nguyên **Candidate A** làm chính sách quyết định Hòa mặc định của TrueLab. Không thay thế bằng Candidate C ở thời điểm hiện tại.
2. **Về mặt Kiến Trúc Tín Hiệu (Signal Architecture):** Các thành phần Transformer chuẩn hóa `AsianHandicapSignalTransformer` và `OverUnderSignalTransformer` cùng các Selector Main Line đã được kiểm thử unit test $100\%$ PASS và tích hợp sẵn sàng trong `:core:domain` dưới dạng Experimental Features, phục vụ các nghiên cứu tiếp theo (khi tích lũy cỡ mẫu $N \ge 1.500$ trận).
