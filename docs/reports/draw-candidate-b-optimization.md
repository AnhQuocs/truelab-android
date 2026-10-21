# TrueLab — Báo Cáo Thực Nghiệm Quét Tham Số Candidate B (Dynamic Draw Prior Optimization)

> **Mã giai đoạn:** Phase B — B4 (Empirical Grid Search Run)
> **Môi trường thực thi:** Pure Kotlin/JVM Domain UseCases trên cơ sở dữ liệu thực tế `truelab.db`
> **Nguyên tắc cốt lõi:** 100% Temporal Isolation — Không sử dụng Independent Test Set để chọn cấu hình tối ưu.

---

## 1. Tổng Quan Tập Dữ Liệu & Phân Tách Thời Gian (Dataset Summary)

- **Tổng số trận đấu nạp được từ DB:** 15,456 trận (tất cả đều có tỉ số Full-Time và thời gian thi đấu hợp lệ).
- **Số trận có tỷ lệ cược pre-match (Odds):** 133 trận.
- **Phân tách thời gian (Chronological Temporal Split):**
  - **Tập Calibration / Validation (70% đầu):** **10,819** trận (Khoảng thời gian: `2019-08-09T18:45:00Z` $\to$ `2023-04-19T16:45:00Z`).
  - **Tập Independent Test (30% sau):** **4,637** trận (Khoảng thời gian: `2023-04-19T18:45:00Z` $\to$ `2026-09-29T10:30:00Z`). *[Đóng băng hoàn toàn, không đụng đến trong B4]*.

---

## 2. Kết Quả Nhóm Đối Chứng (Baseline Reference Metrics)

Được đánh giá trên chính xác cùng tập Calibration (10,819 trận) với `baselineDrawProb = 0.26`:

| Chỉ số | Giá trị Baseline |
|:---|:---:|
| **Tổng số mẫu (Sample Count)** | 10,819 |
| **Độ chính xác tổng thể (Accuracy)** | 46.97% |
| **Macro Precision** | 31.15% |
| **Macro Recall** | 42.14% |
| **Macro F1-Score** | **0.3570** |
| **Home F1-Score** | 0.5782 (Precision: 52.78%, Recall: 63.93%) |
| **Away F1-Score** | 0.4927 (Precision: 40.67%, Recall: 62.48%) |
| **Draw F1-Score** | **0.0000** (Precision: 0.00%, Recall: 0.00%) |

### Ma trận nhầm lẫn Baseline ($3 \times 3$ Confusion Matrix):
```text
                     Dự đoán (Predicted)
                HOME_WIN      DRAW      AWAY_WIN
Thực tế HOME   :   2972           0        1677   (Tổng: 4649)
Thực tế DRAW   :   1392           0        1401   (Tổng: 2793)
Thực tế AWAY   :   1267           0        2110   (Tổng: 3377)
Tổng dự đoán   :   5631           0        5188
```

---

## 3. Kết Quả Quét Lưới 64 Cấu Hình Candidate B (Grid Search Summary)

- **Tổng số cấu hình quét:** 64/64 cấu hình.
- **Số cấu hình đạt chuẩn ràng buộc (Eligible):** **64 / 64** (chiếm 100.00%).
- **Ràng buộc bảo vệ:** $\text{Home F1} \ge 0.5482$ VÀ $\text{Away F1} \ge 0.4627$.

### Bảng Top 10 Cấu Hình Tối Ưu Nhất Trên Tập Calibration:

| Xếp hạng | P_D,max | σ_Elo | σ_Form | Macro F1 | Draw F1 | Draw Recall | Home F1 | Away F1 | Accuracy | Eligible |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| #1 | 0.38 | 1.00 | 0.30 | **0.3768** | 0.0653 | 0.0365 | 0.5760 | 0.4892 | 0.4686 | ✅ YES |
| #2 | 0.38 | 1.50 | 0.30 | **0.3767** | 0.0658 | 0.0369 | 0.5757 | 0.4886 | 0.4682 | ✅ YES |
| #3 | 0.38 | 1.25 | 0.30 | **0.3765** | 0.0652 | 0.0365 | 0.5756 | 0.4887 | 0.4682 | ✅ YES |
| #4 | 0.38 | 0.75 | 0.30 | **0.3760** | 0.0617 | 0.0344 | 0.5764 | 0.4898 | 0.4688 | ✅ YES |
| #5 | 0.38 | 1.00 | 0.25 | **0.3735** | 0.0569 | 0.0315 | 0.5749 | 0.4886 | 0.4674 | ✅ YES |
| #6 | 0.38 | 0.75 | 0.25 | **0.3734** | 0.0558 | 0.0308 | 0.5755 | 0.4889 | 0.4679 | ✅ YES |
| #7 | 0.38 | 1.25 | 0.25 | **0.3732** | 0.0568 | 0.0315 | 0.5746 | 0.4880 | 0.4670 | ✅ YES |
| #8 | 0.38 | 1.50 | 0.25 | **0.3730** | 0.0568 | 0.0315 | 0.5744 | 0.4878 | 0.4667 | ✅ YES |
| #9 | 0.38 | 1.50 | 0.20 | **0.3690** | 0.0447 | 0.0243 | 0.5744 | 0.4879 | 0.4662 | ✅ YES |
| #10 | 0.38 | 1.00 | 0.20 | **0.3690** | 0.0441 | 0.0240 | 0.5746 | 0.4882 | 0.4665 | ✅ YES |

---

## 4. Cấu Hình Tối Ưu (Calibration Best Candidate B)

Cấu hình đạt hiệu năng Macro F1 cao nhất thỏa mãn toàn bộ ràng buộc trên tập Calibration là:
- **$P_{D,\max}$:** `0.38`
- **$\sigma_{\text{Elo}}$:** `1.0`
- **$\sigma_{\text{Form}}$:** `0.3`
- **$P_{D,\min}$:** `0.12` (Cố định)

### So sánh Đối chứng: Baseline vs Calibration Best Candidate B:

| Chỉ số | Baseline | Best Candidate B | Chênh lệch (Delta) |
|:---|:---:|:---:|:---:|
| **Macro F1** | **0.3570** | **0.3768** | **+0.0199** |
| **Draw F1** | 0.0000 | 0.0653 | +0.0653 |
| **Draw Recall** | 0.00% | 3.65% | +3.65% |
| **Draw Precision** | 0.00% | 30.72% | +30.72% |
| **Home F1** | 0.5782 | 0.5760 | -0.0022 |
| **Away F1** | 0.4927 | 0.4892 | -0.0035 |
| **Accuracy** | 46.97% | 46.86% | -0.11% |

### Ma trận nhầm lẫn Best Candidate B ($3 \times 3$ Confusion Matrix):
```text
                     Dự đoán (Predicted)
                HOME_WIN      DRAW      AWAY_WIN
Thực tế HOME   :   2918         133        1598   (Tổng: 4649)
Thực tế DRAW   :   1335         102        1356   (Tổng: 2793)
Thực tế AWAY   :   1230          97        2050   (Tổng: 3377)
Tổng dự đoán   :   5483         332        5004
```

---

## 5. Phân Tích Phân Bố Dự Đoán (Prediction Distribution Analysis)

| Đại lượng thống kê | Baseline | Best Candidate B | Thực tế (Ground Truth) |
|:---|:---:|:---:|:---:|
| **Số trận dự đoán Hòa (Predicted Draws)** | 0 (0.00%) | 332 (3.07%) | 2793 (25.82%) |
| **Số trận dự đoán Home Win** | 5631 (52.05%) | 5483 (50.68%) | 4649 (42.97%) |
| **Số trận dự đoán Away Win** | 5188 (47.95%) | 5004 (46.25%) | 3377 (31.21%) |

- **Số cấu hình có phát ra dự đoán Hòa (> 0 predicted draws):** **64 / 64** cấu hình.

---

## 6. Phân Tích Độ Nhạy Siêu Tham Số (Parameter Sensitivity Analysis)

### 6.1. Ảnh hưởng của $P_{D,\max}$ (Xác suất hòa cực đại khi 2 đội cân bằng):

| P_D,max | Mean Macro F1 | Mean Draw F1 | Mean Draw Recall |
|:---:|:---:|:---:|:---:|
| 0.32 | 0.3563 | 0.0000 | 0.00% |
| 0.34 | 0.3573 | 0.0030 | 0.15% |
| 0.36 | 0.3634 | 0.0227 | 1.18% |
| 0.38 | 0.3712 | 0.0502 | 2.76% |

### 6.2. Ảnh hưởng của $\sigma_{\text{Elo}}$ (Độ rộng hàm Gauss theo thang Elo):

| σ_Elo | Mean Macro F1 | Mean Draw F1 | Mean Draw Recall |
|:---:|:---:|:---:|:---:|
| 0.75 | 0.3619 | 0.0182 | 0.98% |
| 1.00 | 0.3621 | 0.0190 | 1.03% |
| 1.25 | 0.3621 | 0.0192 | 1.04% |
| 1.50 | 0.3621 | 0.0194 | 1.05% |

### 6.3. Ảnh hưởng của $\sigma_{\text{Form}}$ (Độ rộng hàm Gauss theo thang Phong độ):

| σ_Form | Mean Macro F1 | Mean Draw F1 | Mean Draw Recall |
|:---:|:---:|:---:|:---:|
| 0.15 | 0.3601 | 0.0144 | 0.76% |
| 0.20 | 0.3614 | 0.0174 | 0.93% |
| 0.25 | 0.3628 | 0.0207 | 1.13% |
| 0.30 | 0.3639 | 0.0233 | 1.28% |

---

## 7. Đánh Giá Bản Chất & Ranh Giới Kỹ Thuật (Key Technical Insights)

1. **Candidate B đã kích hoạt được dự đoán Hòa:**
   - Khác với Baseline ($0$ predicted draws, $\text{Draw Recall} = 0.0\%$), Candidate B đã giải phóng được **332 trận dự đoán Hòa** trên tập Calibration, đạt $\text{Draw Precision} = 30.72\%$ và $\text{Draw Recall} = 3.65\%$.
   - Macro F1 cải thiện từ $0.3570 \to 0.3768$ ($+0.0199$).
2. **Hiện tượng Argmax Dampening:**
   - Do chỉ can thiệp ở 2/6 tín hiệu (Elo $w=0.20$, Form $w=0.25$), khi tổ hợp với các tín hiệu khác và áp dụng $\text{argmax}$ thuần túy, $P_D$ tổng hợp chỉ vượt qua $P_H$ và $P_A$ khi cả Elo và Form cùng ở trạng thái cực kỳ cân bằng.
   - Đây là cơ sở quan trọng để đối chứng với **Candidate A (Decision Margin)** trong Phase B5.

---

## 8. Kết Luận & Đề Xuất Bước Kế Tiếp (Next Steps for Phase B5)

1. **Đóng băng tham số tối ưu (Freeze Best Parameters):**
   - `Frozen Calibration Best Candidate B`: `DynamicDrawPriorConfig(maxDrawProb = 0.38, minDrawProb = 0.12, eloSigma = 1.0, formSigma = 0.3)`.
2. **Không áp dụng tham số này vào Production:** Giữ nguyên `DrawStrategyConfig.DEFAULT` là `BASELINE`.
3. **Bước B5 Đánh Giá Độc Lập (Phase B5: Independent Test Evaluation):**
   - Mở khóa tập **Independent Test (4,637 trận)** để đối chứng lần cuối 3 phương án:
     $$\mathbf{Baseline} \quad \text{vs} \quad \mathbf{Candidate\ A\ (Decision\ Margin)} \quad \text{vs} \quad \mathbf{Candidate\ B\ (Dynamic\ Draw\ Prior)}$$
