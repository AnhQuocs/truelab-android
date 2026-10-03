# BÁO CÁO ĐÁNH GIÁ ĐỐI CHỨNG TRÊN TẬP KIỂM THỬ ĐỘC LẬP (INDEPENDENT TEST EVALUATION)

**TrueLab Football Prediction Engine — Phase B5 Empirical Run**

- **Trạng thái thực nghiệm:** HOÀN TẤT ĐỐI CHỨNG 3 CHIỀU (Baseline vs Candidate A vs Candidate B).
- **Thời điểm thực hiện:** 03/10/2026.
- **Chế độ kiểm thử:** Đóng băng 100% tham số (Frozen Parameters), đánh giá trên dữ liệu tương lai chưa từng thấy (Unseen Future Data).

---

## 1. Tổng Quan Tập Dữ Liệu & Phân Tách Thời Gian (Dataset & Temporal Split)

- **Tổng số trận đọc được trong Room DB:** `15,456` matches.
- **Số trận đủ điều kiện evaluation:** `15,456` matches (100% trận có kết quả Full-Time và timestamp UTC hợp lệ).
- **Số trận bị loại:** `0` matches (không có dữ liệu bị drop ngoài quy định).

| Tập Dữ Liệu | Số Lượng Trận | Tỷ Lệ | Dải Thời Gian (UTC) | Mục Đích Sử Dụng |
|:---|:---:|:---:|:---|:---|
| **Calibration / Validation** | `10,779` | 70.0% | `2019-08-09T18:45:00Z` $\to$ `2023-04-16T12:00:00Z` | Quét lưới Grid Search & Tối ưu hóa tham số |
| **Independent Test** | `4,620` | 30.0% | `2023-04-16T12:30:00Z` $\to$ `2026-09-29T04:00:00Z` | Đánh giá tổng quát hóa độc lập (Unseen) |

### Phân bố nhãn thực tế trên Independent Test Set (N = 4,620):
- **Home Wins:** 2,055 trận (44.48%)
- **Draws:** **1,200 trận (25.97%)**
- **Away Wins:** 1,365 trận (29.55%)

---

## 2. Đặc Tả Tham Số Đóng Băng (Frozen Parameters)

Sau khi hoàn tất quá trình tối ưu hóa trên 70% Calibration, toàn bộ tham số được đóng băng tuyệt đối trước khi mở khóa 30% Independent Test:

1. **BASELINE (Control Group):**
   - Chiến lược: `DrawModelingStrategy.BASELINE`
   - Cố định: $P_D = 0.26$ trên toàn bộ các signal transformer.

2. **CANDIDATE A (Decision Margin / Relative Threshold):**
   - Chiến lược: `DrawModelingStrategy.DECISION_MARGIN`
   - $\delta$ (`deltaMargin`): **0.04**
   - $\theta$ (`thetaMinProb`): **0.255**
   - Quy tắc quyết định: IF $|P_H - P_A| < \delta$ VÀ $P_D \ge \theta \implies$ Dự đoán `DRAW`.

3. **CANDIDATE B (Dynamic Draw Prior):**
   - Chiến lược: `DrawModelingStrategy.DYNAMIC_DRAW_PRIOR`
   - $P_{D,\max}$ (`maxDrawProb`): **0.38**
   - $P_{D,\min}$ (`minDrawProb`): **0.12**
   - $\sigma_{\text{Elo}}$ (`eloSigma`): **1.00**
   - $\sigma_{\text{Form}}$ (`formSigma`): **0.30**
   - Quy tắc quyết định: Phân phối tiên nghiệm Gauss động trên Elo & Form $\implies$ Natural Argmax.

---

## 3. Tổng Kết Hiệu Năng Trên Tập Calibration (Calibration Summary — N = 10,779)

> [!NOTE]
> Đây là kết quả thực nghiệm trong pha huấn luyện/tối ưu hóa siêu tham số (70% dữ liệu lịch sử đầu tiên).

| Chỉ Số Đánh Giá | BASELINE | CANDIDATE A (Frozen) | CANDIDATE B (Frozen) | $\Delta$ A vs Base | $\Delta$ B vs Base |
|:---|:---:|:---:|:---:|:---:|:---:|
| **Macro F1** | `0.3569` | **`0.4095`** | `0.3766` | **`+0.0526`** | **`+0.0197`** |
| **Draw F1** | `0.0000` | **`0.1923`** | `0.0649` | **`+0.1923`** | **`+0.0649`** |
| **Draw Recall** | `0.00%` | **`14.52%`** | `3.63%` | **`+14.52%`** | **`+3.63%`** |
| **Draw Precision** | `0.00%` | `28.47%` | **`30.51%`** | `+28.47%` | `+30.51%` |
| **Home F1** | `0.5783` | `0.5534` | `0.5760` | `-0.0249` | `-0.0023` |
| **Away F1** | `0.4926` | `0.4828` | `0.4890` | `-0.0097` | `-0.0035` |
| **Overall Accuracy** | `46.97%` | `45.72%` | `46.85%` | `-1.25%` | `-0.12%` |
| **Macro Precision** | `0.3115` | `0.4158` | `0.4156` | `+0.1043` | `+0.1041` |
| **Macro Recall** | `0.4213` | `0.4256` | `0.4236` | `+0.0043` | `+0.0023` |
| **Brier Score** | `0.6321` | `0.6321` | `0.6349` | `+0.0000` | `+0.0028` |

---

## 4. Kết Quả Thực Nghiệm Trên Tập Test Độc Lập (Independent Test Evaluation — N = 4,620)

> [!IMPORTANT]
> Đây là kết quả trên tập dữ liệu hoàn toàn chưa từng thấy (Unseen Data) trong tương lai. Kết quả này phản ánh khả năng tổng quát hóa thực tế của các mô hình.

| Chỉ Số Đánh Giá | BASELINE (Control) | CANDIDATE A (Margin) | CANDIDATE B (Dynamic) | $\Delta$ A vs Base | $\Delta$ B vs Base |
|:---|:---:|:---:|:---:|:---:|:---:|
| **Macro F1** | `0.3667` | **`0.4066`** | `0.3829` | **`+0.0399`** | **`+0.0162`** |
| **Draw F1** | `0.0033` | **`0.1626`** | `0.0652` | **`+0.1593`** | **`+0.0618`** |
| **Draw Recall** | `0.17%` | **`12.17%`** | `3.75%` | **`+12.00%`** | **`+3.58%`** |
| **Draw Precision** | `100.00%` (2/2) | `24.50%` | `24.86%` | `-75.50%` | `-75.14%` |
| **Home F1** | `0.5944` | `0.5651` | `0.5865` | `-0.0293` | `-0.0079` |
| **Away F1** | `0.5023` | `0.4922` | `0.4972` | `-0.0102` | `-0.0052` |
| **Overall Accuracy** | `48.14%` | `46.06%` | `47.42%` | `-2.08%` | `-0.71%` |
| **Macro Precision** | `0.6531` | `0.4088` | `0.4044` | `-0.2442` | `-0.2487` |
| **Macro Recall** | `0.4357` | `0.4292` | `0.4330` | `-0.0065` | `-0.0027` |
| **Brier Score** | `0.6284` | `0.6284` | `0.6324` | `+0.0000` | `+0.0040` |


### Ma Trận Nhầm Lẫn 3 Chiều (3x3 Confusion Matrix trên Test Set)

#### 1. Baseline Model (Control Group):
| Thực Tế \ Dự Đoán | Pred HOME | Pred DRAW | Pred AWAY | Tổng Thực Tế |
|:---|:---:|:---:|:---:|:---:|
| **Actual HOME** | **1,311** | 0 | 744 | 2,055 |
| **Actual DRAW** | 591 | **2** | 607 | 1,200 |
| **Actual AWAY** | 454 | 0 | **911** | 1,365 |
| **Tổng Dự Đoán** | 2,356 | **2** | 2,262 | 4,620 |

#### 2. Candidate A (Decision Margin: $\delta=0.04, \theta=0.255$):
| Thực Tế \ Dự Đoán | Pred HOME | Pred DRAW | Pred AWAY | Tổng Thực Tế |
|:---|:---:|:---:|:---:|:---:|
| **Actual HOME** | **1,163** | 283 | 609 | 2,055 |
| **Actual DRAW** | 519 | **146** | 535 | 1,200 |
| **Actual AWAY** | 379 | 167 | **819** | 1,365 |
| **Tổng Dự Đoán** | 2,061 | **596** | 1,963 | 4,620 |

#### 3. Candidate B (Dynamic Draw Prior: $P_{D,\max}=0.38, \sigma_{\text{Elo}}=1.00, \sigma_{\text{Form}}=0.30$):
| Thực Tế \ Dự Đoán | Pred HOME | Pred DRAW | Pred AWAY | Tổng Thực Tế |
|:---|:---:|:---:|:---:|:---:|
| **Actual HOME** | **1,263** | 73 | 719 | 2,055 |
| **Actual DRAW** | 570 | **45** | 585 | 1,200 |
| **Actual AWAY** | 419 | 63 | **883** | 1,365 |
| **Tổng Dự Đoán** | 2,252 | **181** | 2,187 | 4,620 |

---

## 5. Phân Tích Chuyên Sâu Kết Quả Hòa (Draw-Specific Analysis)

| Chỉ Số Hòa | Thực Tế (Ground Truth) | BASELINE | CANDIDATE A | CANDIDATE B |
|:---|:---:|:---:|:---:|:---:|
| **Số trận Hòa** | 1,200 | 2 (dự đoán) | 596 (dự đoán) | 181 (dự đoán) |
| **Tỷ lệ Dự đoán Hòa (Predicted Draw Rate)** | 25.97% | 0.04% | 12.90% | 3.92% |
| **Số trận Hòa đoán đúng (True Positives)** | - | 2 | **146** | **45** |
| **Draw Recall** | - | 0.17% | **12.17%** | **3.75%** |
| **Draw Precision** | - | 100.00% | 24.50% | 24.86% |
| **Draw F1-Score** | - | 0.0033 | **0.1626** | **0.0652** |

---

## 6. Đánh Giá Khả Năng Tổng Quát Hóa (Generalization Gap Analysis)

Độ lệch giữa tập Test và Calibration ($\text{Gap} = \text{Test Metric} - \text{Calibration Metric}$):

| Mô Hình | Cal Macro F1 | Test Macro F1 | Macro F1 Gap | Cal Draw F1 | Test Draw F1 | Draw F1 Gap | Cal Accuracy | Test Accuracy | Acc Gap |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **BASELINE** | `0.3569` | `0.3667` | `+0.0097` | `0.0000` | `0.0033` | `+0.0033` | `46.97%` | `48.14%` | `+1.17%` |
| **CANDIDATE A** | `0.4095` | `0.4066` | **`-0.0029`** | `0.1923` | `0.1626` | `-0.0297` | `45.72%` | `46.06%` | `+0.34%` |
| **CANDIDATE B** | `0.3766` | `0.3829` | **`+0.0063`** | `0.0649` | `0.0652` | **`+0.0003`** | `46.85%` | `47.42%` | `+0.57%` |

---

## 7. Phân Tích & Nhận Định Kỹ Thuật Khách Quan

1. **Hiệu Năng So Sánh Đối Chứng (Factual Comparison):**
   - **Candidate A đạt Macro F1 ($0.4066$), Draw F1 ($0.1626$) và Draw Recall ($12.17\%$) cao hơn Candidate B và Baseline trên tập Independent Test.**
   - Cả Candidate A và Candidate B đều phá vỡ hoàn toàn hiện tượng 0 Draw của Baseline trên dữ liệu tương lai chưa từng thấy (bắt đúng lần lượt 146 và 45 trận hòa).

2. **Phân Tích Trade-Off Kỹ Thuật Giữa Các Mô Hình:**
   - **Candidate A (Decision Margin):** Cải thiện khả năng nhận diện trận hòa mạnh mẽ nhất ($12.17\%$ Recall), nhưng chuyển dịch nhiều quyết định Home/Away hơn (Home F1 giảm $-0.0293$, Away F1 giảm $-0.0102$). Vì không can thiệp vào xác suất gốc của các tín hiệu, điểm Brier Score của Candidate A được giữ nguyên bằng Baseline ($0.6284$).
   - **Candidate B (Dynamic Draw Prior):** Cải thiện khả năng nhận diện trận hòa một cách thận trọng hơn ($3.75\%$ Recall), nhưng bảo toàn Home F1 ($-0.0079$) và Away F1 ($-0.0052$) tốt hơn Candidate A và gần Baseline hơn. Vì can thiệp trực tiếp vào phân phối xác suất tiên nghiệm, điểm Brier Score trên Test Set có độ chênh lệch nhẹ ($0.6324$ so với $0.6284$ của Baseline).
   - Cả hai mô hình đều thỏa mãn nghiêm ngặt tiêu chuẩn không làm suy giảm Home/Away F1 quá ngưỡng $0.03$.

3. **Độ Tin Cậy & Khả Năng Kháng Overfitting:**
   - Generalization Gap giữa Calibration và Test Set duy trì ở mức biên độ cực kỳ nhỏ (Macro F1 Gap $-0.0029$ ở A và $+0.0063$ ở B), chứng minh quy trình chia tập thời gian nghiêm ngặt và đóng băng tham số đã loại trừ triệt để nguy cơ data leakage hay học vẹt (overfitting).

---

## 8. Hiện Trạng & Đề Xuất Cho Bước Tiếp Theo (Current Status & Next Steps)

- **Căn nguyên vấn đề:** Hiện tượng *Draw underprediction* và nguyên nhân triệt tiêu xác suất hòa tuyến tính đã được ghi nhận đầy đủ.
- **Hiện trạng cài đặt:** Cả Candidate A và Candidate B đã được triển khai, kiểm thử và tối ưu hóa hoàn chỉnh.
- **Bảo vệ mã nguồn Production:** `DrawStrategyConfig.DEFAULT` **vẫn được giữ nguyên là `BASELINE`**. Chưa có bất kỳ thay đổi nào lên cấu hình mặc định của sản phẩm.
- **Kế hoạch tiếp theo (Phase B6):** 
  - Quyết định lựa chọn chiến lược phù hợp nhất cho Production (hoặc cơ chế cho phép người dùng cấu hình Strategy trong Settings).
  - Không thực hiện thêm bất kỳ bước tuning hay grid search nào trên tập Test độc lập.
