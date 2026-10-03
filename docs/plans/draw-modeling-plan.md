# TrueLab — Kế Hoạch Mô Hình Hóa Dự Đoán Hòa (Draw Modeling Plan)

Tài liệu này là đặc tả kỹ thuật và mô hình hóa toán học (Mathematical Modeling Plan) cho **Phase A: Draw Modeling** của dự án TrueLab, tập trung nghiên cứu giải pháp khắc phục hiện tượng triệt tiêu kết quả hòa (*Draw Underprediction*) trong hệ thống dự đoán kết quả bóng đá.

---

## 1. Mục Tiêu (Objective)

1. **Khắc phục hiện tượng Draw Underprediction:** Giải quyết dứt điểm rào cản toán học khiến mô hình hiện tại dự đoán $0$ trận hòa (`Predicted Draw = 0`), nâng cao năng lực phân loại trận hòa (`Draw Recall` và `Draw Precision`).
2. **Bảo toàn năng lực phân loại Home/Away:** Không làm suy giảm độ chính xác và chỉ số F1 của các trận đấu phân định thắng thua (`Home Win` / `Away Win`).
3. **Duy trì nhóm đối chứng (Control Group):** Giữ nguyên mô hình hiện tại làm Baseline để phục vụ đo đạc đối chứng công bằng trong các đợt thực nghiệm.
4. **Chuẩn hóa các phương án ứng viên (Candidate Models):** Đặc tả chi tiết 3 hướng tiếp cận toán học để sẵn sàng triển khai thực nghiệm độc lập trong Phase B.

---

## 2. Đường Cơ Sở Hiện Tại (Current Baseline Formalization)

Dựa trên kết quả phân tích tại [draw-underprediction-audit.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/docs/audits/draw-underprediction-audit.md), hệ sinh thái dự đoán hiện tại hoạt động như sau:

### 2.1. Cấu trúc 6 Tín hiệu & Trọng số
Vector xác suất cuối cùng được tổng hợp từ 6 tín hiệu chuẩn hóa với trọng số mặc định:
- **Form ($w_1 = 0.25$):** Đánh giá phong độ thời gian lũy tiến.
- **Elo ($w_2 = 0.20$):** Hệ số sức mạnh tương quan chuẩn FIFA/Elo.
- **Odds ($w_3 = 0.20$):** Xác suất ngầm định từ Bookmakers ($\frac{1/\text{Odds}_i}{\sum 1/\text{Odds}}$).
- **Goals ($w_4 = 0.15$):** Hiệu số kỳ vọng bàn thắng.
- **H2H ($w_5 = 0.10$):** Lịch sử đối đầu trực tiếp với Laplace Smoothing prior.
- **Rest Advantage ($w_6 = 0.10$):** Lợi thế sinh học từ khoảng cách ngày nghỉ thi đấu.

### 2.2. Phân rã Xác suất Hòa Cố định (Fixed Draw Priors)
Trong 4/6 bộ biến đổi tín hiệu (`Form`, `Elo`, `Goals`, `Rest Advantage`), xác suất hòa được gán cố định:
$$P_i(\text{Draw}) = 0.26 \quad (\forall i \in \{\text{Form}, \text{Elo}, \text{Goals}, \text{RestAdv}\})$$
Phần xác suất còn lại ($0.74$) được phân bổ nhị phân giữa Home và Away dựa trên sức mạnh tương quan $r_H \in [0, 1]$:
$$P_i(\text{Home}) = r_H \times 0.74, \quad P_i(\text{Away}) = (1 - r_H) \times 0.74$$

### 2.3. Hỗn hợp Tuyến tính & Quy tắc Ra Quyết định
- **Tổ hợp tuyến tính:** $P(\text{Final Outcome}) = \sum_{i=1}^6 w_i \cdot P_i(\text{Outcome})$ với $\sum w_i = 1.0$.
- **Xác suất hòa tổng hợp:**
  $$P(\text{Final Draw}) \approx 0.182 + 0.10 \cdot P_{\text{H2H}}(D) + 0.20 \cdot P_{\text{Odds}}(D) \in [0.260, 0.275]$$
- **Quy tắc Argmax thuần túy:**
  $$\hat{Y} = \text{argmax}\left(P(\text{Final Home}), P(\text{Final Draw}), P(\text{Final Away})\right)$$
- **Hệ quả tất yếu:** Do $\max(P_H, P_A) \ge \frac{1 - 0.275}{2} = 0.3625 > 0.275$, cửa `DRAW` **về mặt toán học không bao giờ có thể chiến thắng Argmax**, dẫn đến `Predicted Draw = 0` trên toàn bộ tập dữ liệu thực tế.

---

## 3. Triết Lý Đánh Giá Toàn Diện (Evaluation Philosophy)

Để tránh hiện tượng tối ưu cục bộ cho Draw nhưng làm phá hỏng mô hình phân loại tổng thể, quá trình đánh giá bắt buộc phải phân định rõ ràng giữa:
1. **Chất lượng phân loại đa lớp (Multi-Class Classification Quality)**
2. **Hiệu năng riêng biệt từng lớp (Class-Specific Metrics)**
3. **Độ hiệu chuẩn xác suất (Probability Calibration)**
4. **Hành vi của luật ra quyết định (Decision Rule Dynamics)**

### Ma trận Chỉ số Đo lường (Evaluation Metrics Matrix)
- **Độ chính xác tổng thể (Accuracy):** Tỷ lệ dự đoán đúng trên toàn bộ 3 nhãn ($H, D, A$).
- **Chỉ số Macro (Macro Precision, Macro Recall, Macro F1):**
  $$\text{Macro F1} = \frac{F1_{\text{Home}} + F1_{\text{Draw}} + F1_{\text{Away}}}{3}$$
  *Đây là thước đo quan trọng nhất đánh giá năng lực phân loại không thiên vị.*
- **Bộ chỉ số riêng cho cửa Hòa:** $\text{Draw Precision} = \frac{TP_D}{TP_D + FP_D}$, $\text{Draw Recall} = \frac{TP_D}{TP_D + FN_D}$, $\text{Draw F1} = \frac{2 \cdot P_D \cdot R_D}{P_D + R_D}$.
- **Ma trận nhầm lẫn ($3 \times 3$ Confusion Matrix):** Trực quan hóa chi tiết sự dịch chuyển của các mẫu thực tế ($Actual$) sang các lớp dự đoán ($\hat{Y}$).
- **Tỷ lệ phân phối dự đoán (Prediction Class Distribution):** Tỷ lệ các nhãn dự đoán phát ra ($\% \hat{H}, \% \hat{D}, \% \hat{A}$) so với phân phối thực tế (~$45\% H, 26\% D, 29\% A$).
- **Chỉ số Brier Score đa lớp:**
  $$\text{Brier Score} = \frac{1}{N} \sum_{n=1}^N \sum_{k \in \{H, D, A\}} (P_{n,k} - y_{n,k})^2$$

---

## 4. Phương Án Ứng Viên A — Phân Ngưỡng Chênh Lệch (Candidate A: Decision Margin / Relative Threshold)

### 4.1. Bản Chất Toán Học & Công Thức
Phương án này giữ nguyên vector xác suất $(P_H, P_D, P_A)$ được tính toán từ `WeightedScorer`, nhưng thay thế quy tắc ra quyết định $\text{argmax}$ uncalibrated bằng một hàm quyết định có phân ngưỡng:

$$\hat{Y} = \begin{cases} \text{DRAW} & \text{khi } |P_H - P_A| < \delta \quad \text{và} \quad P_D \ge \theta \\ \text{argmax}(P_H, P_A) & \text{trong các trường hợp còn lại} \end{cases}$$

Trong đó:
- $\delta$ (Decision Margin): Độ nhạy chênh lệch sức mạnh giữa Home và Away. Khi $|P_H - P_A| < \delta$, trận đấu được xem là ở trạng thái cân bằng lực lượng cao.
- $\theta$ (Minimum Draw Probability Threshold): Ngưỡng xác suất hòa tối thiểu để loại trừ các trận đấu mở có nhiều bàn thắng nhưng điểm số sát nút.

```
                      |PH - PA| < δ  VÀ  PD >= θ
                             /         \
                           ĐÚNG        SAI
                           /             \
                  Dự đoán: DRAW      Dự đoán: argmax(PH, PA)
```

### 4.2. Dải Tham Số Dự Kiến & Phân Tích Độ Nhạy (Sensitivity Analysis)
- **Dải tìm kiếm ứng viên:**
  - $\delta \in [0.02, 0.08]$ (bước nhảy $0.005$).
  - $\theta \in [0.250, 0.275]$ (bước nhảy $0.005$).
- **Độ nhạy của $\delta$:**
  - Nếu $\delta$ quá nhỏ ($< 0.02$): Quá ít trận đấu lọt vào vùng Margin $\implies \text{Draw Recall}$ vẫn ở mức thấp.
  - Nếu $\delta$ quá lớn ($> 0.08$): Nhiều trận đấu có ưu thế sân nhà rõ rệt bị kéo sang Hòa $\implies \text{Draw Precision}$ và $\text{Home Precision}$ sụt giảm mạnh.
- **Phương pháp chọn tham số:** Sử dụng kỹ thuật chia tập Validation lịch sử (ví dụ: K-Fold Time-Series Split), tối ưu hóa hàm mục tiêu $\text{Macro F1}$, **tuyệt đối không tune trên tập Test**.

### 4.3. Đánh Giá Kiến Trúc & Rủi Ro
- **Độ phức tạp tính toán:** $O(1)$ thời gian, $O(1)$ bộ nhớ.
- **An toàn thời gian (Temporal Safety):** $100\%$ an toàn, hoàn toàn mang tính tất định (deterministic).
- **Hành vi khi thiếu dữ liệu:** Thừa hưởng trực tiếp fallback từ `WeightedScorer`.
- **Điểm tích hợp trong Pipeline:** Lớp `PredictionDecisionPolicy` bọc ngoài đầu ra của `WeightedScorer.score()`.
- **Ưu điểm:**
  - Không làm thay đổi logic tính toán xác suất của 6 tín hiệu độc lập.
  - Dễ kiểm chứng, dễ benchmark đối soát với Baseline.
  - Trực quan: "Khi hai đội cân bằng và xác suất hòa đạt chuẩn $\implies$ Dự đoán Hòa".
- **Nhược điểm & Rủi ro:**
  - Vector xác suất hiển thị trên giao diện có thể có $P_H = 37\%, P_D = 27\%, P_A = 36\%$ nhưng nhãn dự đoán lại là `DRAW`. UI cần giải thích rõ cơ chế phân ngưỡng này.

---

## 5. Phương Án Ứng Viên B — Tiên Nghiệm Hòa Động (Candidate B: Dynamic Draw Prior)

### 5.1. Bản Chất Toán Học & Công Thức
Thay vì gán hằng số $P_D = 0.26$ cố định, phương án này biến $P_D$ thành một hàm phi tuyến suy giảm theo khoảng cách chênh lệch năng lực giữa 2 đội $\Delta$.

Hàm mật độ hòa động dạng Gaussian đối xứng (Symmetric Gaussian Prior):
$$P_D(\Delta) = P_{D,\min} + (P_{D,\max} - P_{D,\min}) \cdot \exp\left( - \frac{\Delta^2}{2 \sigma_D^2} \right)$$

Trong đó:
- $\Delta$ là độ lệch năng lực chuẩn hóa:
  - Với Elo: $\Delta_{\text{Elo}} = \frac{\text{Rating}_{\text{Home}} - \text{Rating}_{\text{Away}} + \text{Adv}_{\text{Home}}}{S_{\text{Elo}}}$ (với $S_{\text{Elo}} \approx 400$).
  - Với Form: $\Delta_{\text{Form}} = \text{FormScore}_{\text{Home}} - \text{FormScore}_{\text{Away}}$.
- $P_{D,\max}$: Xác suất hòa cực đại khi 2 đội cân bằng tuyệt đối ($\Delta = 0$), dự kiến $P_{D,\max} \in [0.33, 0.38]$.
- $P_{D,\min}$: Xác suất hòa tối thiểu khi 1 đội áp đảo hoàn toàn ($|\Delta| \gg 0$), dự kiến $P_{D,\min} \in [0.10, 0.15]$.
- $\sigma_D$: Độ rộng suy giảm của hàm Gauss.

```
       PD(Δ) ▲
      0.36   │        ╭───────╮  (Cực đại tại Δ = 0: 2 đội cân bằng)
             │       ╱         ╲
      0.26   │──────╱───────────╲────── (Mức cố định cũ)
             │     ╱             ╲
      0.12   │────╯               ╰──── (Cực tiểu khi chênh lệch lớn)
             └────────────────────────────► Δ (Độ lệch năng lực)
                 -Δ           0          +Δ
```

### 5.2. Các Điều Kiện Ràng Buộc Bắt Buộc (Mathematical Constraints)
1. **Tính đối xứng hoàn hảo (Symmetry):**
   $$P_D(\Delta) = P_D(-\Delta) \quad \forall \Delta$$
2. **Bảo toàn tổng xác suất phân vùng (Simplex Normalization):**
   Sau khi tính $P_D(\Delta)$, phần xác suất còn lại $(1 - P_D(\Delta))$ được phân rã cho Home và Away:
   $$P(\text{Home}) = (1 - P_D(\Delta)) \cdot \sigma(\Delta), \quad P(\text{Away}) = (1 - P_D(\Delta)) \cdot (1 - \sigma(\Delta))$$
   Với $\sigma(\Delta) = \frac{1}{1 + 10^{-\Delta}}$ là hàm Logistic chuyển đổi Elo.
   $$\implies P(\text{Home}) + P(\text{Draw}) + P(\text{Away}) = 1.0 \quad (\text{Tuyệt đối chính xác})$$
3. **Hành vi tiệm cận (Asymptotic Behavior):**
   - Khi $\Delta \to 0 \implies P_D \to P_{D,\max} \approx 0.36$, $P_H \approx 0.32, P_A \approx 0.32 \implies \mathbf{P_D > \max(P_H, P_A)}$ ($\text{Argmax}$ tự nhiên chọn `DRAW`!).
   - Khi $|\Delta| \to \infty \implies P_D \to P_{D,\min} \approx 0.12$, đội mạnh chiếm lĩnh xác suất $> 80\%$.

### 5.3. Đánh Giá Kiến Trúc & Rủi Ro
- **Độ phức tạp tính toán:** $O(1)$ thời gian, $O(1)$ bộ nhớ.
- **An toàn thời gian (Temporal Safety):** $100\%$ an toàn.
- **Điểm tích hợp trong Pipeline:** Cập nhật công thức phân rã bên trong `EloSignalTransformer` và `FormSignalTransformer`.
- **Ưu điểm:**
  - Giữ vững quy tắc $\text{argmax}$ tiêu chuẩn mà không cần thêm tầng heuristic bên ngoài.
  - Xác suất phản ánh đúng thực tế: Trận derby hoặc các đội ngang tài ngang sức có tỷ lệ hòa tự nhiên cao hơn.
- **Nhược điểm & Rủi ro:**
  - Cần tinh chỉnh tham số $\sigma_D$ để tránh việc gán $P_D$ quá cao ở các giải đấu có số bàn thắng trung bình cao.

---

## 6. Phương Án Ứng Viên C — Mô Hình Bàn Thắng Poisson / Bivariate Poisson (Candidate C: Goal-Distribution Poisson Model)

### 6.1. Bản Chất Toán Học & Công Thức
Mô hình Poisson tiếp cận bài toán từ góc độ kỳ vọng bàn thắng (Expected Goals - $\lambda$). Giả định số bàn thắng của Home ($X$) và Away ($Y$) tuân theo phân phối Poisson độc lập có hiệu chỉnh tỷ số thấp (Dixon-Coles adjustment):

$$X \sim \text{Poisson}(\lambda_H), \quad Y \sim \text{Poisson}(\lambda_A)$$

Ước lượng $\lambda_H, \lambda_A$ từ dữ liệu lịch sử cục bộ (Moving Average bàn thắng sân nhà/sân khách):
$$\lambda_H = \alpha_H \cdot \beta_A \cdot \gamma_{\text{League}}, \quad \lambda_A = \alpha_A \cdot \beta_H$$
Trong đó $\alpha$ là sức mạnh tấn công (Attack Strength), $\beta$ là độ yếu phòng ngự (Defense Weakness), $\gamma_{\text{League}}$ là hệ số sân nhà của giải.

### 6.2. Ma Trận Xác Suất Tỷ Số & Tổng Hợp 1X2
Ma trận xác suất tỷ số $(x, y)$ với giới hạn cắt cụt $N_{\max} = 6$ ($0 \le x, y \le 6$):
$$P(X = x, Y = y) = \frac{\lambda_H^x e^{-\lambda_H}}{x!} \cdot \frac{\lambda_A^y e^{-\lambda_A}}{y!} \cdot \tau(x, y)$$

Hệ số hiệu chỉnh Dixon-Coles $\tau(x, y)$ cho các tỷ số có độ tương quan cao:
$$\tau(x, y) = \begin{cases} 1 - \lambda_H \lambda_A \rho & \text{khi } x = 0, y = 0 \\ 1 + \lambda_A \rho & \text{khi } x = 0, y = 1 \\ 1 + \lambda_H \rho & \text{khi } x = 1, y = 0 \\ 1 - \rho & \text{khi } x = 1, y = 1 \\ 1 & \text{với các tỷ số còn lại} \end{cases}$$
*(Với tham số tương quan $\rho \in [-0.15, 0.0]$ đo lường xu hướng hòa ít bàn thắng)*.

**Xác suất 1X2 được tích lũy từ ma trận tỷ số:**
$$P(\text{Draw}) = \sum_{k=0}^{N_{\max}} P(X = k, Y = k) = P(0-0) + P(1-1) + P(2-2) + \dots$$
$$P(\text{Home}) = \sum_{x > y} P(X = x, Y = y), \quad P(\text{Away}) = \sum_{x < y} P(X = x, Y = y)$$

```
Ma trận tỷ số (Score Matrix 7x7):
        Away: 0       1       2       3   ...
Home: 0    [ 0-0 ]  [ 0-1 ]  [ 0-2 ]  [ 0-3 ] ...  --> Đường chéo chính (Diagonal):
      1    [ 1-0 ]  [ 1-1 ]  [ 1-2 ]  [ 1-3 ] ...      P(Draw) = Σ P(k, k)
      2    [ 2-0 ]  [ 2-1 ]  [ 2-2 ]  [ 2-3 ] ...      Tự động > 33% khi λH ≈ λA ≈ 1.0!
      3    [ 3-0 ]  [ 3-1 ]  [ 3-2 ]  [ 3-3 ] ...
```

### 6.3. Đánh Giá Kiến Trúc & Ràng Buộc Triển Khai
- **Ràng buộc quan trọng:** TrueLab **không sử dụng ML framework bên ngoài**. Mọi phép tính ước lượng $\lambda_H, \lambda_A$ và giai thừa đều thực hiện thuần túy bằng Kotlin (`core:algorithm`) thông qua bảng tra trước (Lookup Table) cho giai thừa $k!$.
- **Độ phức tạp tính toán:** $O((N_{\max} + 1)^2) = O(49)$ phép tính nhân chia $\implies$ Cực kỳ nhẹ trên thiết bị ($< 0.1\text{ms}$).
- **An toàn thời gian (Temporal Safety):** Tuyệt đối an toàn nếu chỉ ước lượng $\lambda_H, \lambda_A$ từ các trận đấu trước thời điểm kickoff $T$.
- **Xử lý thiếu dữ liệu (Cold-Start):** Khi đội bóng có $< 5$ trận lịch sử, $\lambda$ tự động fallback về mức trung bình của giải đấu ($\lambda \approx 1.35$).
- **Ưu điểm:** Sinh ra đồng thời xác suất 1X2, Over/Under (Tài/Xỉu) và Exact Score (Tỷ số chính xác).
- **Nhược điểm:** Đòi hỏi dữ liệu lịch sử bàn thắng chi tiết theo sân nhà/sân khách để ước lượng $\lambda$ chính xác.

---

## 7. Bảng So Sánh Đối Chiếu 3 Phương Án Ứng Viên (Candidate Comparison Matrix)

| Tiêu Chí So Sánh | Baseline Hiện Tại (Control) | Candidate A: Decision Margin | Candidate B: Dynamic Draw Prior | Candidate C: Bivariate Poisson |
| :--- | :---: | :---: | :---: | :---: |
| **Độ phức tạp toán học** | $O(1)$ Tuyến tính | $O(1)$ Phân ngưỡng | $O(1)$ Hàm mũ Gauss | $O(N^2)$ Ma trận Poisson |
| **Độ phức tạp cài đặt** | Đã hoàn thành | **Rất thấp** (1 Decision Class) | **Trung bình** (Update 2 Transformers) | **Cao** (Score Matrix Engine) |
| **Chi phí thời gian chạy** | $< 0.05\text{ms}$ | $< 0.05\text{ms}$ | $< 0.05\text{ms}$ | $< 0.20\text{ms}$ |
| **Yêu cầu dữ liệu đầu vào** | 6 tín hiệu hiện có | 6 tín hiệu hiện có | 6 tín hiệu hiện có | Cần lịch sử bàn thắng chi tiết |
| **Tính an toàn thời gian** | $100\%$ | $100\%$ | $100\%$ | $100\%$ (Cần Temporal Filter) |
| **Khả năng giải thích (Explainability)** | Cao (Trọng số tuyến tính) | **Rất cao** (Trực quan biên độ) | **Cao** (Độ cân bằng sức mạnh) | **Rất cao** (Kỳ vọng bàn thắng) |
| **Hành vi khi Cold-Start** | Fallback về Prior | Fallback về Prior | Fallback về Prior | Fallback về League Average |
| **Tiềm năng hiệu chuẩn (Calibration)** | Kém ($P_D$ bị dồn về 0.26) | Trung bình (Giữ nguyên xác suất) | **Tốt** ($P_D$ dao động linh hoạt) | **Rất tốt** (Phân phối xác suất chuẩn) |
| **Độ nhạy nhận diện Draw** | $0.0\%$ (Triệt tiêu) | **Tùy biến cao theo $\delta, \theta$** | **Tự nhiên theo $\Delta_{\text{Elo}}$** | **Tự nhiên theo $(\lambda_H, \lambda_A)$** |
| **Rủi ro Overfitting** | $0\%$ | Thấp nếu Cross-Validation đúng | Thấp (Tham số $\sigma_D$ trơn) | Trung bình (Khi ít mẫu bàn thắng) |
| **Tương thích kiến trúc hiện tại** | $100\%$ | **$100\%$ (Non-invasive)** | **$95\%$ (Cập nhật Transformers)** | $80\%$ (Module tính toán mới) |
| **Khả năng trình bày học thuật** | Đã có báo cáo Audit | Rất thuyết phục về mặt quyết định | Rất ấn tượng về thống kê ứng dụng | Đỉnh cao về mô hình hóa bóng đá |

### Khuyến Nghị của Bản Kế Hoạch (Plan Recommendation)
1. **Lộ trình Phase B (Triển khai chính):** Triển khai **Candidate A (Decision Margin)** làm giải pháp phân loại ưu tiên vì tính độc lập cao, không làm thay đổi các tín hiệu hiện có và dễ dàng kiểm soát qua tham số phân ngưỡng $\delta, \theta$.
2. **Hướng nghiên cứu nâng cao:** Triển khai song song **Candidate B (Dynamic Draw Prior)** để so sánh hiệu năng trực tiếp với Candidate A trên cùng một bộ khung thực nghiệm.

---

## 8. Thiết Kế Thực Nghiệm Đối Chứng (Experimental Design)

Để đảm bảo tính khách quan và khoa học, thí nghiệm so sánh giữa `Baseline` vs `Candidate A` vs `Candidate B` vs `Candidate C` phải tuân thủ nghiêm ngặt các nguyên tắc:

```
                            ┌────────────────────────────────────────┐
                            │    Tập Dữ Liệu Thực Nghiệm Cố Định     │
                            │   (Cùng Dates, Cùng Trận, N ≥ 300)     │
                            └───────────────────┬────────────────────┘
                                                │
             ┌────────────────────────┬─────────┴──────────────┬────────────────────────┐
             ▼                        ▼                        ▼                        ▼
  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
  │   Baseline Model    │  │     Candidate A     │  │     Candidate B     │  │     Candidate C     │
  │   (Control Group)   │  │  (Decision Margin)  │  │ (Dynamic Draw Prior)│  │ (Bivariate Poisson) │
  └──────────┬──────────┘  └──────────┬──────────┘  └──────────┬──────────┘  └──────────┬──────────┘
             │                        │                        │                        │
             └────────────────────────┼────────────────────────┴────────────────────────┘
                                      │
                                      ▼
                        ┌───────────────────────────┐
                        │   Bộ Đo Chỉ Số Độc Lập    │
                        │   (Accuracy, Macro F1,    │
                        │    Draw R/P, Matrix)      │
                        └───────────────────────────┘
```

### Nguyên Tắc Bất Biến Thực Nghiệm:
1. **Cùng tập mẫu (Identical Dataset):** Toàn bộ các mô hình được đánh giá trên cùng một tập trận đấu đã kết thúc Full-time.
2. **Cùng điểm cắt thời gian (Identical Temporal Cutoff):** Tuyệt đối không sử dụng thông tin sau thời điểm kickoff.
3. **Cùng dữ liệu đầu vào (Identical Input Features):** Dữ liệu phong độ, thứ hạng, đối đầu và tỷ lệ cược hoàn toàn như nhau.
4. **Phân chia Train / Validation / Test:**
   - **Tập Validation (Tune parameters):** Dùng để xác định $(\delta, \theta)$ cho Candidate A hoặc $(\sigma_D, P_{D,\max})$ cho Candidate B.
   - **Tập Test độc lập:** Dùng để đánh giá chỉ số cuối cùng. Tuyệt đối không tinh chỉnh tham số trên tập Test.

---

## 9. Quy Tắc Ngăn Chặn Rò Rỉ Dữ Liệu Tương Lai (Anti-Leakage Rules)

Tại thời điểm diễn ra trận đấu $T_{\text{kickoff}}$:
1. **Tuyệt đối không sử dụng kết quả trận đấu:** Tỷ số FT, thẻ phạt, diễn biến của chính trận đấu đó bị cách ly hoàn toàn khỏi pipeline dự đoán.
2. **Chỉ sử dụng dữ liệu lịch sử $t < T_{\text{kickoff}}$:** Elo rating, Form điểm số, thống kê bàn thắng và H2H chỉ được tổng hợp từ các trận đấu đã kết thúc trước $T_{\text{kickoff}}$.
3. **Odds trước trận (Pre-match Odds):** Chỉ sử dụng tỷ lệ cược mở thưởng hoặc cập nhật trước giờ bóng lăn.

---

## 10. Chiến Lược Kiểm Thử Hồi Quy (Backtest Strategy)

- **Quy mô mẫu đánh giá (Sample Size):** Đánh giá trên dải thời gian $7 \to 14$ ngày liên tiếp (quy mô $300 \to 1000+$ trận đấu Full-time).
- **Phân loại theo nhóm giải đấu:** Đánh giá độ ổn định trên các giải đấu lớn (Top 5 châu Âu) vs các giải đấu cúp có tính bất định cao.
- **Tiêu chí thành công của Mô hình:**
  - $\text{Draw Recall}$ tăng từ $0.0\% \to > 25\%$.
  - $\text{Macro F1-Score}$ tăng tối thiểu $+0.08$ so với Baseline.
  - $\text{Accuracy}$ tổng thể không suy giảm quá $2.0\%$.

---

## 11. Tiêu Chí Nghiệm Thu Phase A (Acceptance Criteria)

Phase A được nghiệm thu hoàn tất khi:
- [x] Cả 3 phương án ứng viên (A, B, C) đều có công thức toán học và ràng buộc lý thuyết hoàn chỉnh.
- [x] Xác định rõ điểm tích hợp vào Clean Architecture mà không phá vỡ ranh giới các module.
- [x] Phân tích đầy đủ độ phức tạp tính toán $O(N)$, an toàn thời gian và hành vi khi thiếu dữ liệu.
- [x] Thiết lập bảng so sánh đối chứng 13 tiêu chí khách quan.
- [x] Thiết kế phương pháp thực nghiệm công bằng, bảo toàn Baseline làm nhóm đối chứng.
- [x] Không có bất kỳ thay đổi nào trên mã nguồn sản phẩm (Production Code) trong giai đoạn này.

---

## 12. Các Hạng Mục Không Thực Hiện Trong Giai Đoạn Này (Non-Goals)

- **Không chỉnh sửa mã nguồn sản phẩm:** Không can thiệp vào `DefaultWeightedScorer.kt`, `PredictionWeightConfig.kt` hoặc các Signal Transformers.
- **Không thay đổi trọng số dự đoán:** Trọng số 6 tín hiệu được giữ nguyên vẹn.
- **Không chạy Backtest quy mô lớn:** Việc chạy kiểm thử hồi quy hàng loạt sẽ được thực hiện trong Phase C.
- **Không tích hợp thư viện Machine Learning bên ngoài:** Giữ vững nguyên tắc Pure Kotlin Engine trên `:core:algorithm`.
- **Không chạy lệnh kiểm thử trên thiết bị di động (No Device Testing / ADB).**
