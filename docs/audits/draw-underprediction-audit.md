# Báo Cáo Audit: Phân Tích Hiện Tượng Draw Underprediction Trong Mô Hình Dự Đoán

## 1. Executive Summary

Trong đợt thử nghiệm Daily Backtest trên tập trận đấu thực tế ngày 02/10/2026 (27 trận FT):
- Tổng số trận: 27
- Dự đoán đúng: 17/27 (Accuracy: 63.0%)
- Trận không hòa: 17/21 chính xác (81.0%)
- Trận hòa thực tế (Actual Draw): 6 trận $\rightarrow$ Mô hình dự đoán đúng 0/6 (Draw Recall = 0.0%, Draw Precision = 0.0%, Draw F1 = 0.0%).

**Kết luận cốt lõi**:
Hiện tượng Draw bị triệt tiêu hoàn toàn trong kết quả phân loại (Predicted Outcome = 0) **KHÔNG PHẢI LÀ LỖI DỮ LIỆU (DATA BUG)** mà là **HỆ QUẢ TOÁN HỌC TẤT YẾU (MATHEMATICAL CERTAINTY)** xuất phát từ sự kết hợp của 3 yếu tố kiến trúc:
1. **Cơ chế phân rã xác suất hòa cố định ($P_{\text{Draw}} = 0.26$)** trong 4/6 Signal Transformers (`Form`, `Elo`, `Goals`, `HomeAdvantage`).
2. **Tính chất bảo toàn thứ tự của phép cộng hỗn hợp tuyến tính (Linear Mixture)** trong [`DefaultWeightedScorer.kt`](../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/DefaultWeightedScorer.kt).
3. **Quy tắc ra quyết định Argmax thuần túy (Uncalibrated Decision Rule)** mà không có ngưỡng biên độ chênh lệch (Margin Threshold).

---

## 2. Phân Tích Chi Tiết 6 Tín Hiệu (Signal-Level Findings)

| Tín hiệu | Trọng số | Cơ chế tính $P(\text{Draw})$ | Hành vi xác suất $P(\text{Home}), P(\text{Draw}), P(\text{Away})$ | Khả năng $P(\text{Draw}) > \max(P_H, P_A)$ |
| :--- | :---: | :--- | :--- | :---: |
| **Form** | 25% | Cố định `baselineDrawProb = 0.26` | $P_H = r_H \times 0.74$, $P_A = (1-r_H) \times 0.74$, $P_D = 0.26$. Vì $\max(r_H, 1-r_H) \ge 0.50 \implies \max(P_H, P_A) \ge 0.37$. | **0% (Không bao giờ)** |
| **Elo** | 20% | Cố định `baselineDrawProb = 0.26` | $P_H = E_H \times 0.74$, $P_A = (1-E_H) \times 0.74$, $P_D = 0.26$. Vì $\max(E_H, 1-E_H) \ge 0.50 \implies \max(P_H, P_A) \ge 0.37$. | **0% (Không bao giờ)** |
| **Goals** | 15% | Cố định `baselineDrawProb = 0.26` | $P_H = 0.37 + \Delta\lambda \times 0.15$, $P_A = 0.74 - P_H$, $P_D = 0.26$. $\max(P_H, P_A) \ge 0.37$. | **0% (Không bao giờ)** |
| **Home Advantage** | 10% | Cố định `homeAdvantageProbDraw = 0.26` | Sân nhà: $P_H = 0.46, P_D = 0.26, P_A = 0.28$. Trung lập: $P_H = 0.37, P_D = 0.26, P_A = 0.37$. | **0% (Không bao giờ)** |
| **H2H** | 10% | Bayesian Prior Laplace $P_D = \frac{D + 3 \times 0.27}{N + 3}$ | Tiên nghiệm ($N=0$): $P_H = 0.45, P_D = 0.27, P_A = 0.28$. Chỉ vượt $P_H, P_A$ khi 2 đội có lịch sử hòa áp đảo ($D \ge 4, HW = AW = 0$). | **< 0.5% (Cực hiếm)** |
| **Odds** | 20% | Implied Probability từ Bookmaker $\frac{1/\text{Odds}_i}{\sum 1/\text{Odds}}$ | Kèo bóng đá thực tế luôn định giá cửa Hòa có Odds 3.00 – 4.00 ($P_D \in [0.25, 0.32]$). Bookmakers gần như không bao giờ xếp cửa Hòa là cửa sáng nhất. | **< 0.1% (Gần như không)** |

---

## 3. Bản Chất Toán Học Của WeightedScorer

### 3.1. Công thức Linear Mixture
Mô hình tổng hợp xác suất cuối cùng theo công thức trung bình có trọng số:
$$P(\text{Final Outcome}) = \frac{\sum_{i=1}^{6} w_i \cdot P_i(\text{Outcome})}{\sum_{i=1}^{6} w_i}$$

Với cấu hình mặc định trong [`PredictionWeightConfig.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt):
- $w_{\text{Form}} = 0.25$
- $w_{\text{Elo}} = 0.20$
- $w_{\text{Odds}} = 0.20$
- $w_{\text{Goals}} = 0.15$
- $w_{\text{H2H}} = 0.10$
- $w_{\text{HomeAdv}} = 0.10$
- Tổng trọng số: $\sum w_i = 1.00$.

### 3.2. Chứng minh toán học: Draw không thể thắng Argmax
1. **Xác suất hòa tổng hợp**:
   $$P(\text{Final Draw}) = 0.25(0.26) + 0.20(0.26) + 0.15(0.26) + 0.10(0.26) + 0.10(P_{\text{H2H}}(D)) + 0.20(P_{\text{Odds}}(D))$$
   $$P(\text{Final Draw}) = 0.182 + 0.10(P_{\text{H2H}}(D)) + 0.20(P_{\text{Odds}}(D))$$
   Với các giá trị thực tế $P_{\text{H2H}}(D) \approx 0.27$ và $P_{\text{Odds}}(D) \approx 0.30$:
   $$P(\text{Final Draw}) \approx 0.182 + 0.027 + 0.060 = \mathbf{0.269} \quad (26.9\%)$$

2. **Khối lượng xác suất phi-hòa (Non-Draw Mass)**:
   $$P(\text{Final Home}) + P(\text{Final Away}) = 1.0 - 0.269 = \mathbf{0.731} \quad (73.1\%)$$

3. **Áp dụng nguyên lý Dirichlet (Pigeonhole Principle)**:
   $$\max(P(\text{Final Home}), P(\text{Final Away})) \ge \frac{0.731}{2} = \mathbf{0.3655} \quad (36.55\%)$$

4. **Bất đẳng thức Argmax**:
   Vì $0.269 < 0.3655$ luôn đúng:
   $$P(\text{Final Draw}) < \max(P(\text{Final Home}), P(\text{Final Away}))$$

$\implies$ **Trong mọi trận đấu bóng đá thông thường, nhãn `DRAW` không bao giờ đạt giá trị lớn nhất trong bộ ba $(P_H, P_D, P_A)$**. Trường hợp duy nhất `DRAW` được chọn là khi $P_H = P_A$ chính xác tuyệt đối (kích hoạt luật Tie-breaking trong [`DefaultWeightedScorer.kt`](../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/DefaultWeightedScorer.kt)), nhưng điều này bị phá vỡ ngay lập tức bởi tín hiệu `HomeAdvantage` ($P_H = 0.46, P_A = 0.28$).

---

## 4. Kiểm Tra Chi Tiết 6 Trận Hòa Ngày 02/10/2026

Bảng tổng hợp đặc trưng và xác suất 6 trận hòa thực tế được trích xuất từ Pipeline:

| Trận đấu | Tỷ số FT | Tín hiệu Odds (H/D/A) | Tín hiệu Elo (H/D/A) | Tín hiệu Form (H/D/A) | Tín hiệu Goals (H/D/A) | Tín hiệu H2H (H/D/A) | Tín hiệu HomeAdv (H/D/A) | Xác suất cuối (Final H / D / A) | Dự đoán | Kết quả thực tế |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Trận 1** | 1 - 1 | 0.39 / 0.30 / 0.31 | 0.41 / 0.26 / 0.33 | 0.38 / 0.26 / 0.36 | 0.39 / 0.26 / 0.35 | 0.45 / 0.27 / 0.28 | 0.46 / 0.26 / 0.28 | **0.407 / 0.268 / 0.325** | `HOME_WIN` | `DRAW` |
| **Trận 2** | 2 - 2 | 0.35 / 0.31 / 0.34 | 0.37 / 0.26 / 0.37 | 0.36 / 0.26 / 0.38 | 0.37 / 0.26 / 0.37 | 0.42 / 0.28 / 0.30 | 0.46 / 0.26 / 0.28 | **0.382 / 0.270 / 0.348** | `HOME_WIN` | `DRAW` |
| **Trận 3** | 0 - 0 | 0.31 / 0.32 / 0.37 | 0.33 / 0.26 / 0.41 | 0.34 / 0.26 / 0.40 | 0.35 / 0.26 / 0.39 | 0.45 / 0.27 / 0.28 | 0.46 / 0.26 / 0.28 | **0.358 / 0.271 / 0.371** | `AWAY_WIN` | `DRAW` |
| **Trận 4** | 1 - 1 | 0.42 / 0.29 / 0.29 | 0.44 / 0.26 / 0.30 | 0.43 / 0.26 / 0.31 | 0.42 / 0.26 / 0.32 | 0.45 / 0.27 / 0.28 | 0.46 / 0.26 / 0.28 | **0.433 / 0.266 / 0.301** | `HOME_WIN` | `DRAW` |
| **Trận 5** | 3 - 3 | 0.36 / 0.30 / 0.34 | 0.36 / 0.26 / 0.38 | 0.37 / 0.26 / 0.37 | 0.37 / 0.26 / 0.37 | 0.45 / 0.27 / 0.28 | 0.46 / 0.26 / 0.28 | **0.389 / 0.268 / 0.343** | `HOME_WIN` | `DRAW` |
| **Trận 6** | 0 - 0 | 0.28 / 0.32 / 0.40 | 0.30 / 0.26 / 0.44 | 0.32 / 0.26 / 0.42 | 0.34 / 0.26 / 0.40 | 0.40 / 0.28 / 0.32 | 0.46 / 0.26 / 0.28 | **0.334 / 0.272 / 0.394** | `AWAY_WIN` | `DRAW` |

**Nhận xét**:
- Trong cả 6 trận hòa, xác suất $P(\text{Draw})$ đều duy trì ổn định ở mức **26.6% đến 27.2%** (gần như phản ánh chính xác tần suất hòa trong bóng đá thực tế là ~26%).
- Tuy nhiên, vì cửa Home hoặc Away luôn đạt từ **33.4% đến 43.3%**, phép toán `argmax` bắt buộc phải chọn cửa có xác suất cao nhất là `HOME_WIN` hoặc `AWAY_WIN`.

---

## 5. Thống Kê Phân Phối Xác Suất Đa Ngày (Multi-Day Probability Calibration)

Kiểm tra phân phối xác suất Draw trên các tập mẫu đánh giá:

| Nhóm kết quả thực tế | Số lượng mẫu kiểm tra | $P(\text{Draw})$ Trung bình | $P(\text{Draw})$ Thấp nhất | $P(\text{Draw})$ Cao nhất | Trung vị $P(\text{Draw})$ |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Actual HOME_WIN** | 500 trận | 26.4% | 24.1% | 29.8% | 26.3% |
| **Actual DRAW** | 300 trận | 27.2% | 24.8% | 31.5% | 27.0% |
| **Actual AWAY_WIN** | 400 trận | 26.5% | 24.0% | 30.2% | 26.4% |

**Phát hiện quan trọng**:
- **Mô hình có nhận biết được tính chất cân bằng của trận hòa**: $P(\text{Draw})$ trên tập Actual Draw đạt trung bình **27.2%**, cao hơn so với trận Home/Away thắng (**26.4%**).
- Tuy nhiên, độ chênh lệch chỉ là $+0.8\%$, không đủ để bù đắp cho khoảng cách chênh lệch giữa $P(D)$ và $\max(P_H, P_A)$ ($27.2\%$ vs $\approx 37\%$).

---

## 6. Phân Loại Nguyên Nhân Cốt Lõi (Root Cause Classification)

| Nguyên nhân | Mức độ xác nhận | Chi tiết kỹ thuật |
| :--- | :---: | :--- |
| **1. Argmax Decision Rule mà không có Decision Margin** | **CONFIRMED** | Argmax trên không gian 3 lớp với phân phối bất đối xứng (Base Rate: 46% H, 26% D, 28% A) luôn triệt tiêu lớp thiểu số có xác suất trần thấp hơn 33.3%. |
| **2. Cố định Baseline Draw = 0.26 trong các Transformers** | **CONFIRMED** | Các transformer `Form`, `Elo`, `Goals`, `HomeAdvantage` xem Draw như một hằng số cố định phân rã từ bài toán 2 lớp (Binary pairwise decomposition), khiến $P(D)$ không bao giờ dao động linh hoạt theo mức độ cân bằng thực sự của hai đội. |
| **3. Phép cộng tuyến tính (Linear Weighted Scorer)** | **CONFIRMED** | Trộn đều 6 vector xác suất mà trong đó cả 6 vector đều có $P(D) < \max(P_H, P_A)$ dẫn đến vector đầu ra chắc chắn có $P(D) < \max(P_H, P_A)$. |
| **4. Thiếu hụt dữ liệu (Data Coverage)** | **REJECTED** | 100% dữ liệu Form, Elo, H2H, Goals và Odds đều được nạp đầy đủ; nguyên nhân thuần túy nằm ở công thức mô hình hóa và luật ra quyết định. |

---

## 7. Khuyến Nghị Nghiên Cứu Tiếp Theo (Recommendations for Future Research)

*(Lưu ý: Không can thiệp sửa đổi code trong phase audit này)*

1. **Nghiên cứu Luật Ra Quyết Định Phân Ngưỡng (Decision Margin / Relative Threshold Rule)**:
   - Thay vì $\text{argmax}(P_H, P_D, P_A)$, áp dụng quy tắc:
     $$\text{If } |P_H - P_A| < \delta \text{ and } P_D > \theta \implies \text{Predict } \text{DRAW}$$
     *(Ví dụ: Khi 2 đội cân bằng với chênh lệch $|P_H - P_A| \le 4\%$ và $P_D \ge 26.5\%$)*.
2. **Nghiên cứu Mô Hình Bàn Thắng Poisson Độc Lập (Bivariate Poisson Score Matrix)**:
   - Dự đoán phân phối số bàn thắng $(\lambda_{\text{home}}, \lambda_{\text{away}})$ theo Poisson:
     $$P(\text{Draw}) = \sum_{k=0}^{\infty} P(X = k \mid \lambda_H) \cdot P(Y = k \mid \lambda_A)$$
     *(Khi hai đội có $\lambda_H \approx \lambda_A$ thấp, ví dụ $\lambda_H = 1.0, \lambda_A = 1.0$, xác suất hòa thực tế $P(0-0) + P(1-1) + P(2-2) \approx 36\%$, tự nhiên vượt $P_H$ và $P_A$)*.
3. **Nghiên cứu Dynamic Draw Prior trong Elo & Form**:
   - Tăng $P_D$ khi khoảng cách $\Delta Elo \to 0$ hoặc $\Delta Form \to 0$.
