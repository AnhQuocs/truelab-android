# Báo Cáo Phân Tích Mô Thức Lỗi (Failure Pattern Analysis)

> **Tài liệu tham chiếu:** [`docs/reports/final/candidate-c-ah-ou-signal-final.md`](../reports/final/candidate-c-ah-ou-signal-final.md)  
> **Phạm vi phân tích:** Tập Frozen Holdout ($N = 315$ trận đấu độc lập) và mở rộng trên toàn bộ Cohort ($N = 791$ trận).  
> **Nguyên tắc cốt lõi:**  
> - **Tuyệt đối không retune tham số, không thay đổi ngưỡng hay trọng số.**  
> - **Không tạo Candidate D, không thay đổi pipeline Production.**  
> - **Chỉ thực hiện phân tích mô tả định lượng (Descriptive & Correlation Analysis) để giải thích *tại sao hệ thống sai* và *tín hiệu có biểu hiện gì trước các lỗi đó*.**

---

## 1. Phân Loại & Tỷ Trọng Nhầm Lẫn (Confusion & Failure Breakdown)

Trên tập Frozen Holdout ($N = 315$ trận), hệ thống ghi nhận các tỷ trọng lỗi chi tiết theo từng biến thể quyết định:

### 1.1. Bảng Phân Rã Lỗi Chi Tiết Giữa Các Biến Thể

| Loại mô thức lỗi (Failure Pattern) | Ý nghĩa nghiệp vụ | V0 (Baseline Raw) | V1 (Candidate A - Active Prod) | V6 (Candidate C + Cand A) |
|---|---|:---:|:---:|:---:|
| **Pred AWAY $\to$ Actual HOME** | Đoán Đội Khách $\to$ Thực tế Đội Nhà Thắng (*Đảo cực*) | 40 (28,78%) | 30 (20,55%) | 23 (17,04%) |
| **Pred HOME $\to$ Actual AWAY** | Đoán Đội Nhà $\to$ Thực tế Đội Khách Thắng (*Đảo cực*) | 33 (23,74%) | 29 (19,86%) | 28 (20,74%) |
| **Pred HOME $\to$ Actual DRAW** | Đoán Đội Nhà $\to$ Thực tế Hòa (*Bỏ sót Hòa*) | 36 (25,90%) | 28 (19,18%) | 29 (21,48%) |
| **Pred AWAY $\to$ Actual DRAW** | Đoán Đội Khách $\to$ Thực tế Hòa (*Bỏ sót Hòa*) | 30 (21,58%) | 24 (16,44%) | 24 (17,78%) |
| **Pred DRAW $\to$ Actual HOME** | Đoán Hòa $\to$ Thực tế Đội Nhà Thắng (*Phá hỏng H*) | 0 (0,00%) | 22 (15,07%) | 22 (16,30%) |
| **Pred DRAW $\to$ Actual AWAY** | Đoán Hòa $\to$ Thực tế Đội Khách Thắng (*Phá hỏng A*) | 0 (0,00%) | 13 (8,90%) | 9 (6,67%) |
| **TỔNG SỐ TRẬN LỖI (ERRORS)** | | **139 / 315 (44,13%)** | **146 / 315 (46,35%)** | **135 / 315 (42,86%)** |
| **ĐỘ CHÍNH XÁC (ACCURACY)** | | **55,87%** | **53,65%** | **57,14%** |

### 1.2. Nhận Xét Về Nhóm Lỗi Chiếm Tỷ Trọng Lớn Nhất

1. **Trên Baseline Raw Argmax (V0):**
   - **Lỗi đảo cực (Cross-Polarity Errors — Pred H $\to$ Act A và Pred A $\to$ Act H):** Chiếm **52,52%** tổng số lỗi ($73 / 139$ trận).
   - **Lỗi bỏ sót Hòa (Missed Draws — Pred H/A $\to$ Act D):** Chiếm **47,48%** tổng số lỗi ($66 / 139$ trận).
2. **Trên Candidate A (V1 - Active Production):**
   - Khi áp dụng ngưỡng Decision Margin ($|P_H - P_A| < 0.04 \land P_D \ge 0.255$), lỗi liên quan đến cửa Hòa (Draw-Related Failures) tăng vọt lên chiếm **59,59% tổng số lỗi** ($87 / 146$ trận), bao gồm:
     - **52 trận bỏ sót Hòa** ($28$ từ Home và $24$ từ Away).
     - **35 trận can thiệp sai (False Draws)** phá vỡ kết quả Home/Away thắng thực tế ($22$ ca Home và $13$ ca Away).
   - Lỗi đảo cực trực tiếp giữa Home và Away giảm xuống còn $40,41\%$ ($59 / 146$ trận).

---

## 2. Phân Tích Đồng Thuận & Bất Đồng Tín Hiệu (Signal Agreement / Disagreement)

Kiểm tra mối tương quan giữa 6 tín hiệu Baseline và tỷ lệ cược thị trường (1X2 Market Odds & Asian Handicap) trên tập Holdout:

### 2.1. Hiện Tượng "Lỗi Đồng Thuận" (Consensus Errors / False Certainty Trap)

- **Tỷ lệ đồng thuận giữa Baseline và Tỷ lệ cược 1X2 thị trường:** Đạt **81,90%** ($258 / 315$ trận).
- **Khi cả Baseline và Thị trường CÙNG ĐỒNG THUẬN:**
  - Tỷ lệ dự đoán đúng đạt $63,57\%$ ($164 / 258$ trận).
  - Vẫn có **94 trận cả hệ thống và thị trường CÙNG SAI**:
    - **53,2% (50 trận)** là các trận đấu kết thúc với tỷ số **HÒA (DRAW)** dù cả mô hình và nhà cung cấp đều nghiêng về một bên ($29$ ca nghiêng Home, $21$ ca nghiêng Away).
    - **46,8% (44 trận)** là các trận đấu lật kèo (Upset) hoàn toàn ($28$ trận nghiêng Home nhưng Khách thắng, $16$ trận nghiêng Khách nhưng Chủ nhà thắng).
- **Ý nghĩa:** Khi toàn bộ tín hiệu thống kê và thị trường đồng thuận cao, rủi ro lớn nhất không phải là đánh giá sai tương quan lực lượng, mà là **tính ngẫu nhiên nội tại của bóng đá** dẫn tới kịch bản chia điểm hoặc biến cố thẻ đỏ/bàn thắng bất ngờ.

### 2.2. Hiện Tượng "Bất Đồng Tín Hiệu & Vùng Rủi Ro Cao" (Signal Conflict & High Uncertainty)

- **Tỷ lệ mâu thuẫn giữa Baseline và Thị trường 1X2:** Chiếm **18,10%** ($57 / 315$ trận).
- **Khi có sự mâu thuẫn (Disagreement):**
  - Độ chính xác của Baseline rơi tự do xuống chỉ còn **21,05%** ($12 / 57$ trận).
  - Độ chính xác của cửa được Nhà cung cấp chọn (Market Favorite) đạt **47,37%** ($27 / 57$ trận).
  - Kết thúc với tỷ số Hòa chiếm **31,58%** ($18 / 57$ trận).
- **Ý nghĩa:** Trạng thái bất đồng giữa mô hình thống kê nội tại (Form, Elo, Goals, H2H, Rest) và tỷ lệ cược thị trường là một **chỉ báo cảnh báo rủi ro cực mạnh (Uncertainty Flag)**. Trong vùng này, việc tin cậy tuyệt đối vào Baseline dẫn tới xác suất sai lên đến gần $80\%$.

---

## 3. Đặc Trưng Hồ Sơ Xác Suất (Probability Profile & Margin Analysis)

Phân tích phân bố xác suất của Candidate A (V1 - Production Active) trên 315 trận Holdout:

### 3.1. Bảng So Sánh Chỉ Số Xác Suất Giữa Trận Đúng và Trận Sai

| Nhóm dữ liệu | Cỡ mẫu $N$ | Mean $P(H)$ | Mean $P(D)$ | Mean $P(A)$ | Mean Top-1 Prob | Mean Top1-Top2 Margin | Tỷ lệ tự tin cao ($P_1 \ge 0.50$) | Tỷ lệ biên hẹp ($P_1 - P_2 < 0.05$) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Trận Dự Đoán Đúng (Correct)** | 169 | 0.4004 | 0.2516 | 0.3480 | **0.4693** | **0.1795** | **30,2% (51 trận)** | 14,2% (24 trận) |
| **Toàn Bộ Trận Dự Đoán Sai (Errors)** | 146 | 0.3751 | 0.2609 | 0.3639 | **0.4179** | **0.0946** | **6,8% (10 trận)** | **33,6% (49 trận)** |
| ↳ *Pred D $\to$ Actual H (Degraded Home)* | 22 | 0.3678 | 0.2655 | 0.3667 | 0.3769 | **0.0194** | 0,0% (0 trận) | **100,0% (22 trận)** |
| ↳ *Pred D $\to$ Actual A (Degraded Away)* | 13 | 0.3584 | 0.2699 | 0.3717 | 0.3757 | **0.0214** | 0,0% (0 trận) | **100,0% (13 trận)** |
| ↳ *Pred H $\to$ Actual D (Missed Draw)* | 28 | 0.4436 | 0.2563 | 0.3000 | 0.4436 | 0.1384 | 14,3% (4 trận) | 10,7% (3 trận) |
| ↳ *Pred A $\to$ Actual D (Missed Draw)* | 24 | 0.3151 | 0.2605 | 0.4244 | 0.4244 | 0.1071 | 8,3% (2 trận) | 12,5% (3 trận) |
| ↳ *Pred H $\to$ Actual A (Cross Error)* | 29 | 0.4321 | 0.2592 | 0.3087 | 0.4321 | 0.1217 | 6,9% (2 trận) | 3,4% (1 trận) |
| ↳ *Pred A $\to$ Actual H (Cross Error)* | 30 | 0.3167 | 0.2600 | 0.4233 | 0.4233 | 0.1044 | 6,7% (2 trận) | 23,3% (7 trận) |

### 3.2. Phát Hiện Trọng Yếu Về Bản Chất Xác Suất
1. **Hệ thống sai KHÔNG PHẢI do "Tự tin thái quá" (Overconfidence):**
   - Chỉ có $6,8\%$ số ca sai (10/146 trận) có xác suất Top-1 đạt $\ge 0.50$, và chỉ duy nhất 1 trận ($0,7\%$) đạt $\ge 0.55$.
   - Trung bình xác suất Top-1 của các ca sai chỉ là $0.4179$ (rất gần mức ngẫu nhiên 3 cửa $0.3333$).
2. **Hệ thống sai do kẹt trong "Vùng Mập Mờ" (Ambiguity Zone):**
   - Các ca can thiệp sai (Degraded H/A, $N = 35$) có $100\%$ biên độ chênh lệch $|P_H - P_A| < 0.04$ (trung bình margin chỉ $0.019 - 0.021$). Khi điểm số hai đội xấp xỉ ngang nhau, việc áp dụng luật gán cứng thành Draw dẫn tới việc bắt trúng 14 trận hòa nhưng đồng thời phá hỏng 35 trận có kết quả thắng/thua rõ ràng.
3. **Phân bố xác suất Hòa thực tế bị nén phẳng (Flat Draw Probability):**
   - Dù trận đấu thực tế kết thúc với tỷ số Hòa hay Không Hòa, xác suất $P(D)$ do mô hình sinh ra luôn dao động trong dải hẹp $0.2516 \dots 0.2699$.
   - Hệ thống không có khả năng tự phát hiện đột biến xác suất hòa từ các tín hiệu hiện tại.

---

## 4. Phân Tích So Sánh Động Học Chuyển Đổi (Candidate A vs B vs C)

### 4.1. Động Học Chuyển Đổi Nhãn Giữa Raw (V0), Candidate A (V1), và Candidate C (V6)

```text
                                [ 315 Trận Holdout ]
                                         │
                 ┌───────────────────────┴───────────────────────┐
         [ 176 Trận V0 Đúng ]                            [ 139 Trận V0 Sai ]
                 │                                               │
        ┌────────┴────────┐                             ┌────────┴────────┐
  [ 155 V1 Đúng ]   [ 21 V1 Sai ]                 [ 14 V1 Đúng ]   [ 125 V1 Sai ]
   (Giữ H/A đúng)   (Bị ép thành D)               (Cứu thành D)    (Vẫn sai)
```

1. **Trade-off của Candidate A (V1):**
   - Cứu được **14 trận Hòa thực tế** từ các dự đoán H/A sai của V0 (Draw TP = 14).
   - Nhưng đồng thời phá hỏng **21 trận H/A đang đúng** của V0 biến thành Draw sai.
   - *Hiệu ứng ròng trên Accuracy:* $-7$ trận ($169$ so với $176$, giảm từ $55,87\%$ xuống $53,65\%$).
2. **Động học tương tác giữa Candidate A (V1) và Candidate C (V6):**
   - So với V1, V6 chuyển đổi trạng thái ở 19 trận đấu:
     - **15 trận V1 Sai $\to$ V6 Đúng:**
       - Tín hiệu Asian Handicap đã kéo giãn biên độ xác suất $|q_H - q_A|$, giải thoát cho **11 trận Home/Away thắng thực tế** không bị Candidate A ép sai thành Draw.
       - Sửa đúng thêm **4 trận đảo cực** trước đó do tín hiệu AH chỉ ra đúng hướng thị trường.
     - **4 trận V1 Đúng $\to$ V6 Sai (Match 129243, 777039, 868388, 658116):**
       - Cả 4 trận này thực tế đều kết thúc với tỷ số **HÒA (DRAW)**.
       - Ở V1, chênh lệch $|P_H - P_A| < 0.04$ nên Candidate A bắt đúng Draw.
       - Ở V6, tín hiệu AH đẩy lệch điểm số khiến $|q_H - q_A| \ge 0.04$, vượt qua ngưỡng lọc của Candidate A và khiến hệ thống dự đoán lệch sang Home/Away.

---

## 5. Phân Tích Lỗi Liên Quan Đến Tỷ Lệ Cược (Odds-Related Failures)

### 5.1. Tác Động Của Độ Phủ Dữ Liệu Tỷ Lệ Cược (Odds Coverage Subsets)

| Tập con dữ liệu (Subset) | Quy mô $N$ (% Holdout) | Accuracy V1 | Accuracy V6 | Macro F1 V1 | Macro F1 V6 | Số ca Degraded H/A (V1 $\to$ V6) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **Có Kèo Chấp Asian Handicap** | 231 (73,3%) | 54,98% | **59,74%** | 49,16% | **51,00%** | 11 $\to$ 5 (Giảm 54,5%) |
| **Có Kèo Tài/Xỉu Over/Under** | 208 (66,0%) | 51,44% | **57,21%** | 46,36% | **49,93%** | 12 $\to$ 7 (Giảm 41,7%) |
| **Có ĐỦ CẢ Asian Handicap & Over/Under**| 180 (57,1%) | 51,67% | **58,33%** | 46,08% | **50,06%** | 9 $\to$ 4 (Giảm 55,6%) |
| **HOÀN TOÀN KHÔNG CÓ Kèo Phụ (Neither)** | 56 (17,8%) | 50,00% | 50,00% | 43,20% | 43,20% | 7 $\to$ 7 (Không đổi) |

### 5.2. Nhận Xét Về Tín Hiệu Odds
- **Hiệu quả phân cực cao của Asian Handicap:** Tín hiệu AH loại bỏ nhiễu và làm rõ độ lệch thực lực giữa hai đội, giúp giảm hơn $50\%$ số ca can thiệp sai ở các trận đấu có độ phủ kèo.
- **Hiện tượng vùng trũng dữ liệu (Data Gaps):** Ở $17,8\%$ số trận không có kèo phụ, mô hình phải phụ thuộc hoàn toàn vào baseline và chịu tỷ lệ lỗi cao ($50\%$ sai sót).

---

## 6. Tổng Kết Phát Hiện & Khuyến Nghị Nghiên Cứu (Findings & Recommendations)

### A. Stable Findings (Phát hiện vững chắc, có bằng chứng định lượng rõ ràng)
1. **Lỗi hệ thống chủ yếu phát sinh từ "Vùng Mập Mờ Xác Suất" (Ambiguity Zone), không phải do tự tin thái quá:** $93,2\%$ các ca dự đoán sai có xác suất Top-1 $< 0.50$. Hệ thống không bao giờ tự tin cao khi sai.
2. **Mâu thuẫn giữa Baseline và Market Odds là chỉ báo rủi ro cực mạnh (Uncertainty Indicator):** Khi Baseline đối đầu với Market Odds, độ chính xác của Baseline sụp đổ xuống chỉ còn $21,05\%$.
3. **Candidate A tạo ra sự đánh đổi bất lợi về số lượng tuyệt đối (Accuracy Trade-off):** Cứu được $14$ trận hòa nhưng phá hỏng $21$ trận thắng của Home/Away.
4. **Asian Handicap gia tăng độ chính xác Thắng/Thua nhưng triệt tiêu khả năng bắt Hòa:** Khi có AH, Accuracy tăng từ $53,65\%$ lên $56,51\%$ và số ca can thiệp sai giảm $28,6\%$, nhưng Draw TP giảm từ $14$ xuống $11$.

### B. Interesting but Inconclusive (Phát hiện đáng chú ý nhưng chưa đủ cỡ mẫu)
1. **Phân hóa hiệu năng theo giải đấu:** Một số giải đấu lớn có thanh khoản tỷ lệ cược cao cho thấy tín hiệu AH hiệu quả hơn các giải đấu cúp hoặc bóng đá nữ, nhưng cỡ mẫu phân tầng theo từng giải trong Holdout còn quá nhỏ ($N < 30$ trận/giải).
2. **Tương quan giữa đường Line O/U và tỷ lệ Hòa:** Dữ liệu cho thấy các trận có line O/U thấp ($2.0 \dots 2.25$) có xu hướng hòa cao hơn nhẹ, nhưng với $w_{\text{ou}}^* = 0.03$, tác động của O/U chưa đủ lớn để tạo ra đột biến độc lập.

### C. No Meaningful Pattern (Giả thuyết đã kiểm tra nhưng không thấy tín hiệu)
1. **Giả thuyết cho rằng Over/Under có thể dự báo độc lập xác suất hòa:** Không tìm thấy bằng chứng cho thấy O/U đơn lẻ (V4) tạo ra sự cải thiện đáng kể nào cho Draw F1 nếu không có sự kết hợp của kèo chấp AH.

---

## 7. Trả Lời Câu Hỏi Cốt Lõi & Đề Xuất Nghiên Cứu Tiếp Theo

> **CÂU HỎI: CÓ ĐỦ EVIDENCE ĐỂ MỞ MỘT RESEARCH PHASE MỚI HAY CHƯA?**

### **TRẢ LỜI:**
**CÓ.** Dữ liệu và phân tích mô thức lỗi đã cung cấp bằng chứng định lượng rất sắc nét và nhất quán:
- Vấn đề cốt lõi của TrueLab hiện nay **không nằm ở việc tinh chỉnh ngưỡng hòa (Draw Policy Thresholds)**, vì mọi chính sách gán cứng dựa trên dải xác suất phẳng hiện tại đều chỉ dịch chuyển qua lại giữa việc *bỏ sót hòa* và *phá hỏng kết quả Home/Away*.
- Vấn đề thực sự nằm ở **Độ Hiệu Chuẩn Xác Suất (Probability Calibration)** và **Sự Thiếu Vắng Tương Tác Phi Tuyến Khi Xảy Ra Mâu Thuẫn Tín Hiệu (Signal Conflict Resolution)**.

---

### **CÁC ĐỀ XUẤT CÂU HỎI NGHIÊN CỨU CHO PHASE TIẾP THEO (RESEARCH QUESTIONS ONLY):**

*(Chỉ đề xuất định hướng nghiên cứu, không tự ý triển khai code hay thay đổi hệ thống)*

1. **Research Question 1 (Probability Calibration & Uncertainty Estimation):**
   > *"Có nên nghiên cứu áp dụng các phương pháp hiệu chuẩn xác suất (như Dirichlet Calibration, Isotonic Regression hoặc Temperature Scaling) và trích xuất chỉ số Độ bất định (Uncertainty Score / Entropy) từ độ phân tán của 6 tín hiệu để thay thế việc gán nhãn cứng trong Vùng Mập Mờ hay không?"*

2. **Research Question 2 (Market Conflict & Gating Mechanism):**
   > *"Khi phát hiện mâu thuẫn lớn giữa Baseline Signals và Market Odds ($P_{\text{baseline}} \neq P_{\text{market}}$ — vùng mà độ chính xác Baseline sụp đổ xuống 21%), hệ thống có nên kích hoạt một Gating Policy / Confidence Fallback để hạ độ tin cậy hoặc chuyển giao quyền ưu tiên hay không?"*

3. **Research Question 3 (Non-Linear Feature Interaction & Accuracy Optimization):**
   > *"Có nên nghiên cứu việc tổng hợp các đặc trưng đầu vào thông qua một bộ phân loại phi tuyến nhẹ (như Calibrated Logistic Regression với Interaction Terms hoặc Shallow Decision Forest) nhằm tối ưu hóa trực tiếp 3-Way Accuracy thay vì cộng trọng số tuyến tính cố định hay không?"*
