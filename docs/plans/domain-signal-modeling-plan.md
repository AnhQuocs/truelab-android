# TrueLab – Đặc tả Mô hình hóa Tín hiệu Nghiệp vụ (Domain Signal Modeling Spec)

Tài liệu này xác lập đặc tả toán học, định nghĩa tham số và quy tắc chuyển đổi dữ liệu nghiệp vụ bóng đá thành cấu trúc **`Signal3Way`** trong tầng `:core:domain`, phục vụ trực tiếp cho việc tích hợp thuật toán **Phase 7 (`WeightedScorer.predictOutcome`)**.

---

## 1. Bản chất Kỹ thuật & Bất biến Toán học của Signal3Way

Trong `:core:algorithm`, cấu trúc [Signal3Way.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/Signal3Way.kt) và phương thức `predictOutcome` của [WeightedScorer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/WeightedScorer.kt) quy định các bất biến (Invariants) bắt buộc:

```kotlin
data class Signal3Way(
    val homeProb: Double,  // Thuộc [0.0, 1.0], hữu hạn
    val drawProb: Double,  // Thuộc [0.0, 1.0], hữu hạn
    val awayProb: Double,  // Thuộc [0.0, 1.0], hữu hạn
    val weight: Double,    // >= 0.0, hữu hạn
    val name: String = ""
)
```

### 1.1. Bất biến Bắt buộc (Hard Invariants):
1. **Bảo toàn Xác suất (Conservation of Probability)**:
   Mỗi tín hiệu $k$ phải thỏa mãn:
   $$p_{k,\text{home}} + p_{k,\text{draw}} + p_{k,\text{away}} = 1.0 \quad (\text{sai số số thực cho phép } \le 10^{-4})$$
2. **Miền giá trị**:
   $$0.0 \le p_{k,\text{home}}, p_{k,\text{draw}}, p_{k,\text{away}} \le 1.0$$
3. **Trọng số**:
   $$w_k \ge 0.0 \quad \text{và} \quad \sum_{k=1}^m w_k > 0.0$$
4. **Mô hình Kết hợp Tuyến tính (Linear Mixture)**:
   $$P_c = \frac{\sum_{k=1}^m w_k \cdot p_{k,c}}{\sum_{k=1}^m w_k} \quad \text{với } c \in \{\text{Home, Draw, Away}\}$$
   *(Mô hình hoàn toàn tuyến tính, KHÔNG dùng Softmax, KHÔNG có tham số temperature)*.

---

## 2. Đối chiếu Đặc tả Hệ thống (FR-14 Spec Alignment)

Theo [README.md](../README.md#L148) (**FR-14 – Weighted Scoring**), mô hình chuẩn quy định cấu trúc 6 yếu tố trọng số:

| Tín hiệu (Signal) | Ký hiệu | Trọng số Mặc định (FR-14) | Tỷ trọng |
|---|:---:|:---:|:---:|
| **Form** (Phong độ 5 trận) | $S_{\text{Form}}$ | $w_{\text{Form}} = 0.25$ | 25% |
| **Elo** (Sức mạnh đối đầu tương đối) | $S_{\text{Elo}}$ | $w_{\text{Elo}} = 0.20$ | 20% |
| **Goals** (Hiệu suất bàn thắng) | $S_{\text{Goals}}$ | $w_{\text{Goals}} = 0.15$ | 15% |
| **Odds** (Tỷ lệ cược thị trường) | $S_{\text{Odds}}$ | $w_{\text{Odds}} = 0.20$ | 20% |
| **H2H** (Lịch sử đối đầu trực tiếp) | $S_{\text{H2H}}$ | $w_{\text{H2H}} = 0.10$ | 10% |
| **Home Advantage** (Ưu thế sân nhà) | $S_{\text{HomeAdv}}$ | $w_{\text{HomeAdv}} = 0.10$ | 10% |
| **Tổng cộng** | | $\sum w = 1.00$ | **100%** |

---

## 3. Phân tích Chi tiết Từng Tín hiệu Nghiệp vụ (Signal Specifications)

---

### 3.1. Odds Signal (Tỷ lệ Cược Thị trường)

#### a. Nguồn dữ liệu & Trạng thái:
- **Dữ liệu hiện có**: `OddsDao.getLatestOddsForMatch(matchId)` $\to$ `OddsRecordItem`.
- **Hàm hiện có**: [Odds.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/model/Odds.kt#L21) (`calculateImpliedProbability()`).

#### b. Công thức Toán học:
$$raw_H = \frac{1.0}{O_{\text{home}}}, \quad raw_D = \frac{1.0}{O_{\text{draw}}}, \quad raw_A = \frac{1.0}{O_{\text{away}}}$$
$$totalMargin = raw_H + raw_D + raw_A$$
$$P_H = \frac{raw_H}{totalMargin}, \quad P_D = \frac{raw_D}{totalMargin}, \quad P_A = \frac{raw_A}{totalMargin}$$

#### c. Phân loại Tham số & Bản chất:
- **Bản chất**: Công thức toán học tài chính chuẩn (Overround Normalization).
- **Tham số**: Không có tham số heuristic tự định nghĩa; dữ liệu hoàn toàn do nhà cái cung cấp.

#### d. Xử lý Biên & Dữ liệu Bất thường (Edge Cases):
- Nếu tỷ lệ cược bị `null`, $\le 0$, hoặc trận đấu chưa mở kèo:
  - Hàm `calculateImpliedProbability()` trả về `null`.
  - **Quy tắc an toàn**: Gán $w_{\text{Odds}} = 0.0$.
  - Khi đó, trong phép tính Linear Mixture, tín hiệu Odds bị bỏ qua và các tín hiệu còn lại tự động chia sẻ trọng số mà không làm sai lệch hay crash hệ thống.

---

### 3.2. Elo Rating Signal (Sức mạnh Đối đầu Tương đối)

#### a. Nguồn dữ liệu & Trạng thái:
- **Dữ liệu hiện có**: `homeElo` ($R_H$) và `awayElo` ($R_A$) từ `TeamEntity` (mặc định $1500.0$).
- **Thuật toán hiện có**: `EloRatingCalculator.expectedScore(R_H, R_A)` trong Phase 6.

#### b. Vấn đề Nghiệp vụ & Bản chất:
Thuật toán Elo chuẩn 2 chiều chỉ tính xác suất kỳ vọng $E_H \in [0.0, 1.0]$ và $E_A = 1.0 - E_H$ (áp dụng cho cờ vua/thể thao không hòa hoặc xem hòa = 0.5 điểm). Trong bóng đá, kết quả Hòa ($P_D$) là một trạng thái độc lập chiếm $\approx 25-28\%$.

#### c. Công thức Đề xuất (Baseline Draw Decomposition):
$$E_H = \frac{1.0}{1.0 + 10^{-(R_H - R_A)/400.0}}, \quad E_A = 1.0 - E_H$$
$$P_D = P_{D,\text{base}}$$
$$P_H = E_H \times (1.0 - P_{D,\text{base}})$$
$$P_A = E_A \times (1.0 - P_{D,\text{base}})$$

#### d. Chứng minh Toán học:
$$P_H + P_D + P_A = E_H(1 - P_{D,\text{base}}) + P_{D,\text{base}} + E_A(1 - P_{D,\text{base}}) = (E_H + E_A)(1 - P_{D,\text{base}}) + P_{D,\text{base}} = 1.0(1 - P_{D,\text{base}}) + P_{D,\text{base}} = 1.0$$
*(Bảo toàn tổng xác suất bằng 1.0 với mọi giá trị $R_H, R_A$ và $P_{D,\text{base}} \in [0, 1]$)*.

#### e. Phân loại Tham số:
- **$P_{D,\text{base}} = 0.26$**: Là **Business Heuristic / Configurable Parameter** (dựa trên tỷ lệ hòa trung bình lý thuyết của bóng đá quốc tế, không phải số liệu nội bộ của TrueLab).
- Nếu hai đội cân bằng ($R_H = R_A = 1500 \implies E_H = E_A = 0.5$):
  $$P_H = 0.5 \times 0.74 = 0.37, \quad P_D = 0.26, \quad P_A = 0.37$$

---

### 3.3. Form Signal (Phong độ 5 Trận Gần Nhất)

#### a. Nguồn dữ liệu & Trạng thái:
- **Dữ liệu hiện có**: `MatchDao.getRecentMatchesForTeam(teamId, limit = 5)`.
- **Thuật toán hiện có**: Phase 5 `LinearDecayFormEvaluator.evaluate(outcomes, windowSize = 5)` $\to$ `FormScore.score` $\in [0.0, 100.0]$.

#### b. Vấn đề Nghiệp vụ:
`FormEvaluator` tính ra hai điểm số phong độ độc lập $F_H, F_A \in [0.0, 100.0]$. Cần ánh xạ cặp điểm $(F_H, F_A)$ sang phân phối 3 chiều $(P_H, P_D, P_A)$.

#### c. Công thức Đề xuất (Relative Strength Ratio with Smoothing):
1. Chuẩn hóa điểm phong độ về thang $[0.0, 1.0]$:
   $$s_H = \frac{F_H}{100.0}, \quad s_A = \frac{F_A}{100.0}$$
2. Tính tỷ trọng phong độ tương đối có thành phần làm mịn (Smoothing Heuristic $\epsilon = 0.10$):
   $$r_H = \frac{s_H + \epsilon}{(s_H + \epsilon) + (s_A + \epsilon)} = \frac{s_H + 0.10}{s_H + s_A + 0.20}, \quad r_A = 1.0 - r_H$$
3. Phân phối xác suất với Baseline Draw $P_{D,\text{base}} = 0.26$:
   $$P_D = 0.26, \quad P_H = r_H \times 0.74, \quad P_A = r_A \times 0.74$$

#### d. Chứng minh & Hành vi Biên (Behavior Analysis):
- **Bảo toàn xác suất**: $P_H + P_D + P_A = r_H(0.74) + 0.26 + (1 - r_H)(0.74) = 0.74 + 0.26 = 1.0$.
- **Trường hợp hai đội bằng điểm phong độ ($F_H = F_A$)**:
  $$s_H = s_A \implies r_H = \frac{s_H + 0.1}{2s_H + 0.2} = 0.5 \implies P_H = 0.37, \ P_D = 0.26, \ P_A = 0.37$$
- **Trường hợp một đội toàn thua ($F_H = 0.0$), đội kia toàn thắng ($F_A = 100.0$)**:
  $$s_H = 0.0, \ s_A = 1.0 \implies r_H = \frac{0.1}{1.2} = 0.0833, \ r_A = 0.9167$$
  $$P_H = 0.0833 \times 0.74 = 0.0616 \ (6.2\%), \quad P_D = 0.26 \ (26.0\%), \quad P_A = 0.6784 \ (67.8\%)$$
  *(Nhờ hằng số $\epsilon = 0.10$, xác suất của đội yếu không bị sập về 0% tuyệt đối, phản ánh đúng tính bất định của bóng đá)*.
- **Trường hợp cả 2 đội đều có $F_H = F_A = 0.0$**:
  $$r_H = \frac{0.1}{0.2} = 0.5 \implies P_H = 0.37, \ P_D = 0.26, \ P_A = 0.37 \quad (\text{Không bị lỗi chia 0})$$

#### e. Phân loại Tham số:
- **$\epsilon = 0.10$**: Là **Business Heuristic Parameter** (tham số làm mịn để tránh phân phối cực đoan khi $F=0$).
- **$P_{D,\text{base}} = 0.26$**: Là **Business Heuristic Parameter**.

---

### 3.4. Head-to-Head Signal (Lịch sử Đối đầu Trực tiếp)

#### a. Nguồn dữ liệu & Trạng thái:
- **Dữ liệu hiện có**: `MatchDao.getH2HMatches(teamAId, teamBId)` $\to$ danh sách $N$ trận đối đầu lịch sử.
- **Các giá trị đếm**: $Wins_H$ (số trận đội nhà thắng), $Draws$ (số trận hòa), $Wins_A$ (số trận đội khách thắng).
- Luôn thỏa mãn: $Wins_H + Draws + Wins_A = N$.

#### b. Vấn đề Nghiệp vụ:
Mẫu số $N$ thường rất nhỏ ($N = 0, 1, 2, 3$). Nếu dùng tần suất mẫu thô $Wins_H / N$, khi $N=1$ xác suất sẽ là $100\%$ hoặc $0\%$, gây overfit nghiêm trọng.

#### c. Công thức Đề xuất (Bayesian Prior / Laplace Smoothing):
Áp dụng phân phối tiên nghiệm chuẩn với trọng số mẫu giả định $K = 3.0$ trận:

$$P_H = \frac{Wins_H + K \cdot \alpha_H}{N + K} = \frac{Wins_H + 3.0 \times 0.45}{N + 3.0}$$
$$P_D = \frac{Draws + K \cdot \alpha_D}{N + K} = \frac{Draws + 3.0 \times 0.27}{N + 3.0}$$
$$P_A = \frac{Wins_A + K \cdot \alpha_A}{N + K} = \frac{Wins_A + 3.0 \times 0.28}{N + 3.0}$$

#### d. Chứng minh Toán học:
$$P_H + P_D + P_A = \frac{(Wins_H + Draws + Wins_A) + K(\alpha_H + \alpha_D + \alpha_A)}{N + K} = \frac{N + K(1.0)}{N + K} = 1.0$$
*(Bảo toàn tổng xác suất luôn bằng 1.0 với mọi $N \ge 0$)*.

#### e. Phân loại Tham số & Hành vi Biên:
- **Tham số mô hình**:
  - Vector phân phối tiên nghiệm $\boldsymbol{\alpha} = [\alpha_H = 0.45, \alpha_D = 0.27, \alpha_A = 0.28]$ (thỏa mãn $\sum \alpha = 1.0$).
  - Độ mạnh tiên nghiệm $K = 3.0$ (tương đương 3 trận đấu giả định trung tính).
  - Cả $\boldsymbol{\alpha}$ và $K$ là **Configurable Modeling Parameters**.
- **Khi $N = 0$ (Chưa từng gặp nhau)**:
  $$P_H = \alpha_H = 0.45, \quad P_D = \alpha_D = 0.27, \quad P_A = \alpha_A = 0.28$$
- **Khi $N = 1$ (Đội nhà thắng 1 trận duy nhất)**:
  $$P_H = \frac{1 + 1.35}{4} = 0.5875, \quad P_D = \frac{0 + 0.81}{4} = 0.2025, \quad P_A = \frac{0 + 0.84}{4} = 0.2100$$
  *(Xác suất đội nhà tăng từ 45% lên 58.75%, không bị vọt lên 100%)*.

---

### 3.5. Home Advantage Signal (Lợi thế Sân nhà)

#### a. Nguồn dữ liệu & Bản chất:
- Xác định dựa trên cờ địa điểm thi đấu (`Match.homeTeam` vs `Match.awayTeam`).
- **Phân định rõ với FR-08**:
  - **FR-08** là chức năng Thống kê lịch sử (Historical Analytics) hiển thị trên màn hình Team Detail (tính trung bình tỷ lệ thắng sân nhà/khách thực tế của từng đội từ database).
  - **Signal 3.5** là Tín hiệu Tiên nghiệm Dự đoán (Prediction Prior Signal) độc lập cho bài toán dự đoán trận đấu theo đặc tả **FR-14**.

#### b. Output & Vector Xác suất:
`Signal3Way(homeProb = 0.46, drawProb = 0.26, awayProb = 0.28, weight = 0.10, name = "HomeAdvantage")`

#### c. Phân loại Tham số:
- Vector $[P_H = 0.46, P_D = 0.26, P_A = 0.28]$ là **Baseline Prior / Configurable Parameter** (tham khảo từ phân phối trung bình các giải đấu bóng đá lớn).
- **Lý do tách thành Signal riêng**: Tuân thủ nghiêm ngặt đặc tả FR-14 (trọng số 10%), tăng tính minh bạch khi hiển thị bảng phân rã đóng góp yếu tố trên UI (Feature Contribution Breakdown).

---

### 3.6. Goals Signal (Hiệu suất Bàn thắng)

#### a. Nguồn dữ liệu & Hiện trạng:
- Dữ liệu bàn thắng `homeScore`, `awayScore` từ các trận đã đấu trong `MatchEntity`.
- Phase 3 `DescriptiveStatisticsCalculator.mean()` có thể tính trung bình số bàn thắng ghi được ($\mu_{\text{scored}}$) và bàn thua ($\mu_{\text{conceded}}$) của mỗi đội trong 5 trận gần nhất.

#### b. Vấn đề Nghiệp vụ:
Cần chuyển đổi thống kê số bàn thắng kỳ vọng thành phân phối xác suất 3 chiều $(P_H, P_D, P_A)$ mà không dùng mô hình Poisson phức tạp hay Softmax.

#### c. Công thức Đề xuất (Expected Goal Differential with Clamped Linear Mapping):
1. Tính sức mạnh ghi bàn và phòng ngự trong 5 trận gần nhất:
   $$\text{Attack}_H = \text{MeanGoalsScored}(H), \quad \text{Defense}_A = \text{MeanGoalsConceded}(A)$$
   $$\text{Attack}_A = \text{MeanGoalsScored}(A), \quad \text{Defense}_H = \text{MeanGoalsConceded}(H)$$
2. Bàn thắng kỳ vọng đại diện (Proxy Expected Goals $\lambda$):
   $$\lambda_H = \frac{\text{Attack}_H + \text{Defense}_A}{2.0}, \quad \lambda_A = \frac{\text{Attack}_A + \text{Defense}_H}{2.0}$$
3. Chênh lệch bàn thắng kỳ vọng:
   $$\Delta \lambda = \lambda_H - \lambda_A$$
4. Ánh xạ tuyến tính có chặn biên (Clamped Linear Map):
   - Baseline Home Win khi cân bằng ($\Delta \lambda = 0$): $P_{H,\text{base}} = 0.37$.
   - Hệ số độ nhạy (Sensitivity Slope): $\beta = 0.15$ (mỗi 1 bàn chênh lệch kỳ vọng làm tăng/giảm $15\%$ xác suất thắng).
   - Baseline Draw: $P_D = 0.26$.
   - Giới hạn chặn biên xác suất thắng đội nhà: $P_H \in [0.05, 0.69]$ (đảm bảo $P_A = 0.74 - P_H \in [0.05, 0.69]$).
   
   $$P_D = 0.26$$
   $$P_H = \max\Big(0.05, \ \min\big(0.69, \ 0.37 + \Delta \lambda \times 0.15\big)\Big)$$
   $$P_A = 0.74 - P_H$$

#### d. Chứng minh Toán học & Giải thích Tham số:
- **Chứng minh tổng bằng 1.0**:
  $$P_H + P_D + P_A = P_H + 0.26 + (0.74 - P_H) = 1.0 \quad (\text{Luôn đúng với mọi } \Delta \lambda)$$
- **Chứng minh miền giá trị $[0.0, 1.0]$**:
  - Do $P_H \in [0.05, 0.69] \implies P_H \ge 0.0$ và $P_H \le 1.0$.
  - $P_A = 0.74 - P_H \implies P_A \in [0.74 - 0.69, \ 0.74 - 0.05] = [0.05, 0.69] \implies P_A \ge 0.0$ và $P_A \le 1.0$.
  - $P_D = 0.26 \in [0.0, 1.0]$.
- **Nguồn gốc các hằng số**:
  - $0.37$: Xác suất thắng cơ sở khi hai đội có cùng số bàn thắng kỳ vọng ($\Delta \lambda = 0$), tương ứng với $0.5 \times (1.0 - 0.26) = 0.37$.
  - $0.15$: **Heuristic Sensitivity Parameter** (mức nhạy cảm của biến động bàn thắng).
  - $[0.05, 0.69]$: **Boundary Bounds** (đảm bảo xác suất tối thiểu của đội yếu là 5% và không vượt quá $74\% - 5\% = 69\%$).
- **Xử lý thiếu dữ liệu bàn thắng**:
  - Nếu đội chưa thi đấu trận nào: gán $\Delta \lambda = 0.0 \implies P_H = 0.37, P_D = 0.26, P_A = 0.37$.

---

## 4. Bảng Tổng hợp Đặc tả Tín hiệu (Signal Specification Matrix)

| Signal | Trọng số (FR-14) | Công thức Cốt lõi | Danh mục Tham số | Bản chất / Nguồn gốc | Trạng thái | Quyết định Mở (Open Decision) |
|---|:---:|---|---|---|:---:|---|
| **1. Odds** | **20%** | $P_c = \frac{1/O_c}{\sum 1/O}$ | Không có | Toán tài chính / Dữ liệu nhà cái thực tế | ✅ **Ready** | Fallback $w=0.0$ khi thiếu odds. |
| **2. Elo** | **20%** | $P_H = E_H \cdot (1 - P_{D,\text{base}})$<br>$P_D = P_{D,\text{base}}$<br>$P_A = E_A \cdot (1 - P_{D,\text{base}})$ | $P_{D,\text{base}} = 0.26$ | Elo Logistic (Phase 6) + Business Heuristic Baseline | 🟡 **Spec Defined** | Xác nhận giá trị $P_{D,\text{base}} = 0.26$. |
| **3. Form** | **25%** | $r_H = \frac{s_H + \epsilon}{s_H + s_A + 2\epsilon}$<br>$P_H = r_H \cdot 0.74$<br>$P_D = 0.26$<br>$P_A = (1 - r_H) \cdot 0.74$ | $\epsilon = 0.10$<br>$P_{D,\text{base}} = 0.26$ | Linear Decay (Phase 5) + Relative Smoothing Heuristic | 🟡 **Spec Defined** | Xác nhận tham số làm mịn $\epsilon = 0.10$. |
| **4. H2H** | **10%** | $P_c = \frac{Count_c + K \cdot \alpha_c}{N + K}$ | $K = 3.0$<br>$\boldsymbol{\alpha} = [0.45, 0.27, 0.28]$ | Bayesian Prior / Laplace Smoothing | 🟡 **Spec Defined** | Xác nhận tham số $K=3.0$ và vector $\boldsymbol{\alpha}$. |
| **5. Home Adv** | **10%** | $P_H = 0.46$<br>$P_D = 0.26$<br>$P_A = 0.28$ | Vector $[0.46, 0.26, 0.28]$ | Configurable Baseline Prior (Độc lập với FR-08) | 🟡 **Spec Defined** | Thống nhất giữ Signal riêng 10% theo FR-14. |
| **6. Goals** | **15%** | $\Delta \lambda = \lambda_H - \lambda_A$<br>$P_H = \text{clamp}(0.37 + 0.15\Delta\lambda)$<br>$P_D = 0.26$<br>$P_A = 0.74 - P_H$ | Baseline: $0.37$<br>$\beta = 0.15$<br>Bounds: $[0.05, 0.69]$ | Proxy Expected Goals + Clamped Linear Heuristic | 🟡 **Spec Defined** | Xác nhận triển khai Goals Signal ngay trong Phase D1. |

---

## 5. Đặc tả Mô hình Khả thi Tối thiểu (Minimum Viable Modeling Spec)

Phần này liệt kê các quyết định kỹ thuật đã **đủ rõ ràng, chặt chẽ và không còn ambiguity** để sẵn sàng chuyển sang bước Implementation:

1. **Không sửa đổi `:core:algorithm`**: Tiếp tục sử dụng nguyên vẹn API của Phase 7:
   - `Signal3Way(homeProb, drawProb, awayProb, weight, name)`
   - `WeightedScorer.predictOutcome(signals: List<Signal3Way>): OutcomeProbabilities`
2. **Cấu hình Trọng số Bất biến theo FR-14**:
   ```kotlin
   data class PredictionWeightConfig(
       val formWeight: Double = 0.25,
       val eloWeight: Double = 0.20,
       val oddsWeight: Double = 0.20,
       val goalsWeight: Double = 0.15,
       val h2hWeight: Double = 0.10,
       val homeAdvantageWeight: Double = 0.10,
       // Configurable Heuristic Parameters
       val baselineDrawProb: Double = 0.26,
       val formSmoothingEpsilon: Double = 0.10,
       val h2hPriorK: Double = 3.0,
       val goalsSensitivity: Double = 0.15
   )
   ```
3. **Tính Độc lập của Transformer**:
   - Tất cả 6 hàm chuyển đổi tín hiệu sẽ được tổ chức dưới dạng pure Kotlin transformer functions trong `:core:domain` (ví dụ: `toOddsSignal()`, `toEloSignal()`, `toFormSignal()`, `toH2hSignal()`, `toHomeAdvantageSignal()`, `toGoalsSignal()`).
   - Mỗi transformer độc lập, có Unit Test riêng, đảm bảo 100% trả về `Signal3Way` hợp lệ thỏa mãn $\sum P = 1.0$ và $P \in [0.0, 1.0]$.
4. **Cơ chế Dự phòng An toàn (Graceful Degradation)**:
   - Nếu bất kỳ tín hiệu nào bị thiếu dữ liệu không thể bù đắp (ví dụ: thiếu hoàn toàn tỷ lệ kèo Odds), tín hiệu đó được gán `weight = 0.0`. Thuật toán Linear Mixture của Phase 7 sẽ tự động chuẩn hóa trên tổng các trọng số còn lại mà không gây crash ứng dụng.
