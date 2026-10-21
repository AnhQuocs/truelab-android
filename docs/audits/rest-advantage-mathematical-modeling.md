# Rest Advantage Mathematical Modeling Report (Phase R2)

> **Trạng thái:** RESEARCH / MATHEMATICAL MODELING (Complete)  
> **Tài liệu căn cứ:** [Implementation Plan: Rest Advantage Mathematical Modeling](../plans/rest-advantage-mathematical-modeling-plan.md), [Issue: Rest Advantage Mathematical Modeling](../issues/rest-advantage-mathematical-modeling.md)  
> **Tài liệu tham chiếu:** [Rest Advantage Feasibility Audit](rest-advantage-feasibility-audit.md), [Home Advantage Semantics Audit](home-advantage-semantics-audit.md)  
> **Ngày hoàn thành:** 02/10/2026

---

## 1. Dữ liệu & Phạm vi phân tích

### 1.1. Nguồn dữ liệu kiểm chứng
Phân tích được thực hiện trực tiếp trên bản snapshot cơ sở dữ liệu SQLite chính thức của ứng dụng:
- **Tập tin Database:** `app/src/main/assets/database/truelab_database.db` (Schema v4 Room Database).
- **Tổng số trận đấu trong DB:** $30,000$ trận.
- **Tổng số trận đấu đã kết thúc (`isEnded == true`):** $29,632$ trận (trạng thái `'8'`, `'ended'`, `'determined'`, `'finished'`, `'ft'`, `'aet'`, `'pen'`).

### 1.2. Quy trình trích xuất mốc nghỉ ngơi (Rest Extraction Process)
Với mỗi trận đấu mục tiêu $M_{\text{target}}$ tại thời điểm $T_{\text{kickoff}}$:
1. Sắp xếp toàn bộ dữ liệu lịch sử theo trật tự thời gian nghiêm ngặt `startTimeDate ASC, id ASC`.
2. Trích xuất $M_{\text{home,prev}}$: Trận đấu đã kết thúc gần nhất của đội Home thỏa mãn $M_{\text{home,prev}}.\text{startTimeDate} < T_{\text{kickoff}}$ và $M_{\text{home,prev}}.\text{id} \neq M_{\text{target}}.\text{id}$.
3. Trích xuất $M_{\text{away,prev}}$: Trận đấu đã kết thúc gần nhất của đội Away thỏa mãn $M_{\text{away,prev}}.\text{startTimeDate} < T_{\text{kickoff}}$ và $M_{\text{away,prev}}.\text{id} \neq M_{\text{target}}.\text{id}$.
4. Tính toán thời gian nghỉ ngơi (tính bằng ngày):
   $$\text{homeRestDays} = \frac{T_{\text{kickoff}} - M_{\text{home,prev}}.\text{startTimeDate}}{86400.0}$$
   $$\text{awayRestDays} = \frac{T_{\text{kickoff}} - M_{\text{away,prev}}.\text{startTimeDate}}{86400.0}$$
   $$\Delta \text{Rest} = \text{homeRestDays} - \text{awayRestDays}$$

---

## 2. Thống kê Phân phối Thực tế của `deltaRest` (Distribution Statistics)

### 2.1. Tỷ lệ phủ dữ liệu (Coverage Breakdown)

| Phân loại | Số lượng trận | Tỷ lệ phần trăm | Ý nghĩa kỹ thuật |
| :--- | :--- | :--- | :--- |
| **Tổng số trận hợp lệ (`isEnded`)** | **$29,632$** | **$100.00\%$** | Toàn bộ các trận đã hoàn thành trong dataset. |
| **Có đủ cả 2 trận trước đó** | **$26,705$** | **$90.12\%$** | Đầy đủ dữ liệu để tính $\Delta \text{Rest}$ trọn vẹn. |
| **Chỉ thiếu lịch sử Home** | $586$ | $1.98\%$ | Trận mở màn của Home (Away đã có lịch sử). |
| **Chỉ thiếu lịch sử Away** | $657$ | $2.22\%$ | Trận mở màn của Away (Home đã có lịch sử). |
| **Thiếu cả 2 lịch sử** | $1,684$ | $5.68\%$ | Vòng đấu mở màn mùa giải của cả 2 đội. |
| **Tổng số trận cần Fallback** | $2,927$ | $9.88\%$ | Áp dụng cơ chế xử lý dữ liệu khuyết thiếu. |

---

### 2.2. Các chỉ số thống kê mô tả (Descriptive Statistics)
Trên tập $N = 26,705$ trận đấu có đầy đủ dữ liệu:

| Chỉ số Thống kê | Giá trị (Ngày) | Nhận xét toán học |
| :--- | :--- | :--- |
| **Sample Count ($N$)** | $26,705$ | Cỡ mẫu đủ lớn cho ý nghĩa thống kê cao ($p < 0.001$). |
| **Giá trị nhỏ nhất (Min)** | $-339.1979$ | Khoảng nghỉ cực đoan giữa 2 mùa giải (Away nghỉ rất dài). |
| **Giá trị lớn nhất (Max)** | $+339.1979$ | Khoảng nghỉ cực đoan giữa 2 mùa giải (Home nghỉ rất dài). |
| **Giá trị trung bình (Mean)** | $\mathbf{+0.0484}$ | **Xấp xỉ $0.00$ ngày** — Cho thấy tính đối xứng tự nhiên của lịch thi đấu. |
| **Trung vị (Median / P50)** | $\mathbf{0.0000}$ | **Chính xác $0.00$ ngày** — Đa số các trận đấu có ngày nghỉ ngang nhau. |
| **Độ lệch chuẩn (Std Dev)** | $19.2912$ | Bị kéo dài bởi các kỳ nghỉ giữa 2 mùa giải ($> 60$ ngày). |
| **Phân vị P5** | $-7.1250$ | $5\%$ trận đấu Away được nghỉ nhiều hơn $\ge 7.1$ ngày. |
| **Phân vị P10** | $-3.8750$ | $10\%$ trận đấu Away được nghỉ nhiều hơn $\ge 3.9$ ngày (Đá cúp giữa tuần). |
| **Phân vị P25 (Q1)** | $-0.9792$ | $25\%$ trận đấu Away được nghỉ nhiều hơn $\ge 1$ ngày. |
| **Phân vị P50 (Median)** | $0.0000$ | Trạng thái cân bằng hoàn hảo. |
| **Phân vị P75 (Q3)** | $+0.9792$ | $25\%$ trận đấu Home được nghỉ nhiều hơn $\ge 1$ ngày. |
| **Phân vị P90** | $+3.9375$ | $10\%$ trận đấu Home được nghỉ nhiều hơn $\ge 3.9$ ngày (Đá cúp giữa tuần). |
| **Phân vị P95** | $+7.9583$ | $5\%$ trận đấu Home được nghỉ nhiều hơn $\ge 8.0$ ngày. |

---

### 2.3. Phân nhóm theo độ lệch tuyệt đối ($|\Delta \text{Rest}|$)

```text
  |ΔRest| < 0.25d  ██████████████████████████████ 38.59% (10,306 matches)
0.25 <= |ΔRest| < 0.5d █ 1.18% (315 matches)
 0.5 <= |ΔRest| < 1.0d █████████ 11.75% (3,139 matches)
 1.0 <= |ΔRest| < 2.0d ██████████████ 18.60% (4,968 matches)
 2.0 <= |ΔRest| < 3.0d ████ 5.47% (1,461 matches)
 3.0 <= |ΔRest| < 5.0d ██████ 8.00% (2,137 matches)
 5.0 <= |ΔRest| < 7.0d ███ 4.17% (1,113 matches)
       |ΔRest| >= 7.0d █████████ 12.23% (3,266 matches)
```

| Khoảng chênh lệch $|\Delta \text{Rest}|$ | Số trận | Tỷ lệ (%) | Ý nghĩa thực tế trong bóng đá |
| :--- | :--- | :--- | :--- |
| **$< 0.25$ ngày ($< 6$ giờ)** | $10,306$ | **$38.59\%$** | Hai đội thi đấu cùng ngày/giờ ở vòng trước (**Cân bằng hoàn toàn**). |
| **$0.25 - 0.5$ ngày ($6 - 12$ giờ)** | $315$ | $1.18\%$ | Chênh lệch giờ thi đấu trong cùng ngày (trận trưa vs trận tối). |
| **$0.5 - 1.0$ ngày ($12 - 24$ giờ)** | $3,139$ | $11.75\%$ | Chênh lệch 1 ngày thi đấu (Thứ Bảy vs Chủ Nhật). |
| **$1.0 - 2.0$ ngày ($24 - 48$ giờ)** | $4,968$ | **$18.60\%$** | Chênh lệch 1–2 ngày (Thứ Sáu vs Chủ Nhật, hoặc Thứ Bảy vs Thứ Hai). |
| **$2.0 - 3.0$ ngày ($48 - 72$ giờ)** | $1,461$ | $5.47\%$ | Một đội đá cúp giữa tuần (Thứ Năm), một đội đá cuối tuần (Chủ Nhật). |
| **$3.0 - 5.0$ ngày** | $2,137$ | $8.00\%$ | Một đội đá cúp C1/C2 (Thứ Ba/Tư), một đội nghỉ trọn tuần. |
| **$5.0 - 7.0$ ngày** | $1,113$ | $4.17\%$ | Một đội đá bù/đá cúp liên tục, một đội nghỉ 1–2 tuần. |
| **$\ge 7.0$ ngày** | $3,266$ | $12.23\%$ | Trận đấu bù cách nhiều tuần hoặc sau kỳ nghỉ FIFA Days / Nghỉ đông. |

---

### 2.4. Phân bổ theo hướng ưu thế (Sign Breakdown)

| Hướng ưu thế | Số trận | Tỷ lệ (%) | Diễn giải |
| :--- | :--- | :--- | :--- |
| **$\Delta \text{Rest} < -0.25$ ngày** | $8,166$ | **$30.58\%$** | **Away có ưu thế thể lực** (Home nghỉ ít hơn). |
| **$|\Delta \text{Rest}| \le 0.25$ ngày** | $10,396$ | **$38.93\%$** | **Cân bằng thể lực tuyệt đối**. |
| **$\Delta \text{Rest} > +0.25$ ngày** | $8,143$ | **$30.49\%$** | **Home có ưu thế thể lực** (Away nghỉ ít hơn). |

> **Nhận xét then chốt:**  
> Tỷ lệ các trận Home có ưu thế ($30.49\%$) và Away có ưu thế ($30.58\%$) là **gần như tương đồng tuyệt đối** (độ lệch chỉ $0.09\%$). Điều này khẳng định biến thể lực $\Delta \text{Rest}$ là một đại lượng khách quan, không mang định kiến tiên nghiệm thiên vị Home như cơ chế cũ.

---

## 3. Đánh giá So sánh các Candidate Models

Mục tiêu là ánh xạ $\Delta \text{Rest} \to \text{Signal3Way}(P_H, P_D, P_A)$ sao cho:
- Baseline neutral: $P_0 = [P_{H,0}, P_{D,0}, P_{A,0}] = [0.37, 0.26, 0.37]$.
- Xác suất Hòa bảo toàn: $P(D) = 0.26$ cố định theo cấu hình hệ thống.
- Ngân sách xác suất còn lại cho Home/Away: $1.0 - 0.26 = 0.74$.
- Độ lệch tối đa khỏi baseline: $\delta_{\max} = 0.09$ (sao cho $P_{\max} = 0.37 + 0.09 = 0.46$ và $P_{\min} = 0.37 - 0.09 = 0.28$, tương thích chuẩn dynamic range của FR-14).

### 3.1. Bảng đối chiếu tiêu chí kỹ thuật

| Tiêu chí Đánh giá | Candidate 1: Linear Model | Candidate 2: Hyperbolic Tangent ($\tanh$) | Candidate 3: Logistic Sigmoid |
| :--- | :--- | :--- | :--- |
| **Công thức toán học** | $\text{bias} = \text{clamp}\left(\frac{\Delta \text{Rest}}{S}, -1, 1\right)$ | $\text{bias} = \tanh\left(\frac{\Delta \text{Rest}}{S}\right)$ | $\text{bias} = 2\left(\sigma(k \Delta \text{Rest}) - 0.5\right)$ |
| **Tính liên tục & Khả vi** | Không khả vi tại điểm gãy $\pm S$. | **Khả vi vô hạn ($C^\infty$) trơn tru**. | Khả vi vô hạn ($C^\infty$) trơn tru. |
| **Tính đối xứng ($+x$ vs $-x$)** | Đối xứng từng khúc (Piecewise). | **Đối xứng hoàn hảo ($\tanh(-x) = -\tanh(x)$)**. | Đối xứng qua phép biến đổi phụ. |
| **Hành vi bão hòa (Saturation)** | Bị chặn cứng (Hard cut-off) tại $\pm S$. | **Tự nhiên tiệm cận mượt (Asymptotic)**. | Tự nhiên tiệm cận mượt (Asymptotic). |
| **Quy luật Lợi ích giảm dần** | Không có (tăng đều tuyến tính đến $S$). | **Có (Đạo hàm giảm dần khi $\|x\|$ tăng)**. | Có (Đạo hàm giảm dần khi $\|x\|$ tăng). |
| **Tính đơn giản trong Code** | Cần phép chia, deadband, clamp logic. | **1 hàm chuẩn `kotlin.math.tanh`**. | Cần tính số mũ `exp`, phép cộng và nhân tỉ lệ. |
| **Numerical Stability** | Ổn định. | **Ổn định tuyệt đối (Không tràn số khi $x \to \pm \infty$)**. | Nguy cơ tràn số `exp(-kx)` nếu $x$ cực lớn. |

### 3.2. Kết luận lựa chọn Candidate
$$\mathbf{LỰA\ CHỌN:\ CANDIDATE\ 2\ -\ HYPERBOLIC\ TANGENT\ (\tanh)}$$

**Lý do:**
1. $\tanh(x)$ là hàm lẻ thuần túy, đảm bảo tính đối xứng hoàn hảo giữa Home và Away mà không cần thêm code điều kiện.
2. Quy luật sinh học của sự phục hồi thể lực là phi tuyến: Sự khác biệt giữa nghỉ 3 ngày và 6 ngày là rất lớn (vượt qua ngưỡng kiệt sức), trong khi sự khác biệt giữa nghỉ 20 ngày và 30 ngày gần như bằng 0 (cả hai đội đều đã phục hồi 100%). Hàm $\tanh$ phản ánh chính xác hiện tượng bão hòa tự nhiên này.
3. Không tạo ra các điểm gãy đột ngột như hàm tuyến tính Linear clamp.

---

## 4. Mô hình Toán học Chính thức (Mathematical Formula)

### 4.1. Hệ phương trình xác suất
Cho trận đấu mục tiêu với tham số cấu hình:
- $P_{H,0} = 0.37$ (Neutral Home probability).
- $P_{D,0} = 0.26$ (Baseline Draw probability).
- $P_{A,0} = 0.37$ (Neutral Away probability).
- $\delta_{\max} = 0.09$ (Maximum probability shift).
- $S = 3.0$ ngày (Sensitivity scale parameter).

Phương trình tính toán:
$$\text{bias} = \tanh\left(\frac{\Delta \text{Rest}}{S}\right)$$
$$\delta = \delta_{\max} \cdot \text{bias}$$

Xác suất 3 chiều của `Signal3Way`:
$$\begin{cases}
P(\text{Home}) &= P_{H,0} + \delta_{\max} \cdot \tanh\left(\dfrac{\Delta \text{Rest}}{S}\right) \\[8pt]
P(\text{Draw}) &= P_{D,0} = 0.26 \\[8pt]
P(\text{Away}) &= P_{A,0} - \delta_{\max} \cdot \tanh\left(\dfrac{\Delta \text{Rest}}{S}\right)
\end{cases}$$

Trọng số tín hiệu:
$$W_{\text{rest}} = \text{config.homeAdvantageWeight} = 0.10 \quad (10\%)$$

---

## 5. Xác định Tham số Sensitivity ($S$)

### 5.1. Cơ sở lựa chọn $S = 3.0$ ngày
Dựa trên phân tích phân vị thực tế từ $26,705$ trận đấu:
- Khoảng cách Interquartile (P25 đến P75) là $\approx \pm 1.0$ ngày.
- Khoảng cách P10 đến P90 (các trận đấu cúp giữa tuần) là $\approx \pm 3.9$ ngày.
- Khoảng cách P5 đến P95 (chênh lệch 1 tuần trọn vẹn) là $\approx \pm 7.5$ ngày.

Khi đặt $S = 3.0$ ngày:
$$\begin{array}{|c|c|c|c|c|l|}
\hline
\mathbf{\Delta \text{Rest (Ngày)}} & \mathbf{\tanh(\Delta \text{Rest} / 3.0)} & \mathbf{\delta} & \mathbf{P(\text{Home})} & \mathbf{P(\text{Away})} & \mathbf{Ý\ nghĩa\ bóng\ đá} \\
\hline
0.00\text{d} & 0.000 & +0.0000 & 0.3700 & 0.3700 & \text{Cùng lịch thi đấu (Cân bằng hoàn toàn)} \\
\pm 0.25\text{d} & \pm 0.083 & \pm 0.0075 & 0.3775 & 0.3625 & \text{Lệch vài giờ (Tác động không đáng kể)} \\
\pm 0.50\text{d} & \pm 0.165 & \pm 0.0149 & 0.3849 & 0.3551 & \text{Lệch nửa ngày} \\
\pm 1.00\text{d} & \pm 0.322 & \pm 0.0289 & 0.3989 & 0.3411 & \text{Lệch 1 ngày (Thứ 7 vs CN) — Tác động nhẹ} \\
\pm 2.00\text{d} & \pm 0.583 & \pm 0.0525 & 0.4225 & 0.3175 & \text{Lệch 2 ngày (Nghỉ 3d vs 5d)} \\
\pm 3.00\text{d} & \pm 0.762 & \pm 0.0685 & 0.4385 & 0.3015 & \text{Đá cúp giữa tuần (C1/C2) — Ưu thế rõ rệt} \\
\pm 4.00\text{d} & \pm 0.870 & \pm 0.0783 & 0.4483 & 0.2917 & \text{Lệch 4 ngày (Nghỉ 3d vs 7d)} \\
\pm 7.00\text{d} & \pm 0.981 & \pm 0.0883 & 0.4583 & 0.2817 & \text{Lệch 1 tuần — Gần chạm trần bão hòa} \\
\ge \pm 14.00\text{d} & \pm 1.000 & \pm 0.0900 & 0.4600 & 0.2800 & \text{Bão hòa tối đa} \\
\hline
\end{array}$$

---

## 6. Xử lý Bão hòa & Khoảng cách Cực đoan (Saturation & Cap)

- **Hành vi tiệm cận tự nhiên**: Với hàm $\tanh$, khi $\Delta \text{Rest} \to \pm \infty$, giá trị $\tanh(\dots)$ bị chặn nghiêm ngặt trong khoảng $(-1.0, 1.0)$.
- **Không cần Hard Clamp bổ sung**: Dù một đội nghỉ 30 ngày hay 300 ngày (do chuyển giao mùa giải), $\tanh(300 / 3) = 1.0$, xác suất luôn được giữ an toàn tuyệt đối tại:
  $$P(\text{Home}) \le 0.4600 \quad \text{và} \quad P(\text{Away}) \ge 0.2800$$
- **Ngưỡng bão hòa hiệu dụng**: Tại $|\Delta \text{Rest}| = 7.0$ ngày, mô hình đã đạt $98.1\%$ độ lệch tối đa. Điều này hoàn toàn khớp với thực tế: Nghỉ thêm quá 7 ngày so với đối thủ không mang lại thêm ưu thế thể lực nào đáng kể.

---

## 7. Xử lý Dữ liệu Khuyết thiếu (Missing-Data Behavior)

Khi một hoặc cả hai đội không có trận đấu đã kết thúc nào trước thời điểm kickoff (`homePreviousMatch == null` hoặc `awayPreviousMatch == null`):

### Quyết định thiết kế: **OPTION A — NEUTRAL BASELINE REGULARIZATION**

```kotlin
if (homePreviousMatch == null || awayPreviousMatch == null) {
    Signal3Way(
        homeProb = config.baselineNeutralProbHome, // 0.37
        drawProb = config.baselineDrawProb,        // 0.26
        awayProb = config.baselineNeutralProbAway, // 0.37
        weight = config.homeAdvantageWeight,       // 0.10
        name = "Rest Advantage (Neutral Fallback)"
    )
}
```

### Rationale:
1. **Bảo toàn chuẩn hóa trọng số**: Giữ nguyên tổng trọng số các tín hiệu $\sum W_i = 1.0$, tránh việc phân bổ lại trọng số phức tạp làm lệch tương quan giữa các signal khác.
2. **Kéo về tâm trung tính**: Phân phối $[0.37, 0.26, 0.37]$ đóng vai trò là một Laplace/Gaussian regularizer kéo xác suất tổng hợp về trạng thái cân bằng khách quan khi thiếu thông tin.
3. **Tính minh bạch trong Evidence**: Trong `PredictionEvidence`, gán `isAvailable = false` và ghi rõ lý do trong `details["reason"] = "MISSING_PREVIOUS_MATCH"`.

---

## 8. Kiểm chứng Toán học (Mathematical Validation)

### 8.1. Kiểm chứng Trạng thái Trung tính ($\Delta \text{Rest} = 0$)
$$\text{bias} = \tanh\left(\frac{0}{3.0}\right) = 0.0$$
$$P(\text{Home}) = 0.37 + 0.09 \times 0.0 = 0.37$$
$$P(\text{Draw}) = 0.26$$
$$P(\text{Away}) = 0.37 - 0.09 \times 0.0 = 0.37$$
$$\implies [0.37, 0.26, 0.37] \quad \mathbf{(PASS)}$$

### 8.2. Kiểm chứng Tính Đối xứng (Symmetry Invariant)
Với mọi $x \in \mathbb{R}$:
$$P_{\text{Home}}(+x) = 0.37 + 0.09 \tanh(x/S) = P_{\text{Away}}(-x)$$
$$P_{\text{Away}}(+x) = 0.37 - 0.09 \tanh(x/S) = P_{\text{Home}}(-x)$$
$$P_{\text{Draw}}(+x) = P_{\text{Draw}}(-x) = 0.26$$
$$\implies \mathbf{Hoàn\ toàn\ đối\ xứng\ 100\%\ (PASS)}$$

### 8.3. Kiểm chứng Tính Đơn điệu (Strict Monotonicity)
Đạo hàm bậc nhất theo $\Delta \text{Rest}$:
$$\frac{\partial P(\text{Home})}{\partial \Delta \text{Rest}} = \frac{\delta_{\max}}{S} \cdot \text{sech}^2\left(\frac{\Delta \text{Rest}}{S}\right) = \frac{0.09}{3.0} \cdot \text{sech}^2\left(\frac{\Delta \text{Rest}}{3.0}\right) > 0 \quad \forall \Delta \text{Rest} \in \mathbb{R}$$
$$\frac{\partial P(\text{Away})}{\partial \Delta \text{Rest}} = -\frac{\delta_{\max}}{S} \cdot \text{sech}^2\left(\frac{\Delta \text{Rest}}{S}\right) < 0 \quad \forall \Delta \text{Rest} \in \mathbb{R}$$
$$\frac{\partial P(\text{Draw})}{\partial \Delta \text{Rest}} = 0$$
$$\implies \mathbf{Đơn\ điệu\ tăng\ ngặt\ với\ Home,\ giảm\ ngặt\ với\ Away\ (PASS)}$$

### 8.4. Kiểm chứng Boundedness & Probability Normalization
1. **Miền giá trị xác suất**:
   $$\forall \Delta \text{Rest} \in \mathbb{R}: \quad \tanh(\dots) \in (-1, 1)$$
   $$P(\text{Home}) \in (0.37 - 0.09, 0.37 + 0.09) = (0.28, 0.46) \subset [0, 1] \quad \mathbf{(PASS)}$$
   $$P(\text{Away}) \in (0.28, 0.46) \subset [0, 1] \quad \mathbf{(PASS)}$$
   $$P(\text{Draw}) = 0.26 \in [0, 1] \quad \mathbf{(PASS)}$$
2. **Tổng xác suất bằng 1**:
   $$P(\text{Home}) + P(\text{Draw}) + P(\text{Away}) = (0.37 + \delta) + 0.26 + (0.37 - \delta) = 0.37 + 0.26 + 0.37 = 1.000000 \quad \mathbf{(PASS)}$$

### 8.5. Kiểm chứng Độ ổn định Số học (Numerical Stability)
- $\Delta \text{Rest} \to +\infty$: $\tanh(+\infty) = 1.0 \implies P = [0.46, 0.26, 0.28]$ (Không tràn số).
- $\Delta \text{Rest} \to -\infty$: $\tanh(-\infty) = -1.0 \implies P = [0.28, 0.26, 0.46]$ (Không tràn số).
- $\Delta \text{Rest} = \text{NaN}$ hoặc không hợp lệ: Xử lý fallback về neutral $[0.37, 0.26, 0.37]$.

---

## 9. Final Contract cho Implementation Phase

### 9.1. Contract Interface & Transformer

```kotlin
package dev.anhquocs.truelab.core.domain.prediction.transformer

import dev.anhquocs.truelab.core.algorithm.prediction.Signal3Way
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.prediction.model.PredictionWeightConfig
import kotlin.math.tanh

object RestAdvantageSignalTransformer {

    /**
     * Chuyển đổi chênh lệch ngày nghỉ giữa hai đội thành [Signal3Way].
     *
     * @param homePreviousMatch Trận đấu gần nhất đã kết thúc của đội nhà trước kickoff.
     * @param awayPreviousMatch Trận đấu gần nhất đã kết thúc của đội khách trước kickoff.
     * @param targetKickoffEpochSeconds Mốc thời gian bắt đầu trận đấu mục tiêu (Epoch Seconds).
     * @param config Cấu hình trọng số và tham số dự đoán.
     * @return [Signal3Way] chuẩn hóa theo hàm Hyperbolic Tangent.
     */
    fun transform(
        homePreviousMatch: Match?,
        awayPreviousMatch: Match?,
        targetKickoffEpochSeconds: Long?,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        val drawProb = config.baselineDrawProb // 0.26
        val neutralProb = (1.0 - drawProb) / 2.0 // 0.37

        if (homePreviousMatch == null || awayPreviousMatch == null || targetKickoffEpochSeconds == null || targetKickoffEpochSeconds <= 0L) {
            return Signal3Way(
                homeProb = neutralProb,
                drawProb = drawProb,
                awayProb = neutralProb,
                weight = config.homeAdvantageWeight,
                name = "Rest Advantage (Neutral Fallback)"
            )
        }

        val homePrevKickoff = parseEpochSeconds(homePreviousMatch.startTimeDate)
        val awayPrevKickoff = parseEpochSeconds(awayPreviousMatch.startTimeDate)

        if (homePrevKickoff == null || awayPrevKickoff == null || homePrevKickoff >= targetKickoffEpochSeconds || awayPrevKickoff >= targetKickoffEpochSeconds) {
            return Signal3Way(
                homeProb = neutralProb,
                drawProb = drawProb,
                awayProb = neutralProb,
                weight = config.homeAdvantageWeight,
                name = "Rest Advantage (Neutral Fallback)"
            )
        }

        val homeRestDays = (targetKickoffEpochSeconds - homePrevKickoff) / 86400.0
        val awayRestDays = (targetKickoffEpochSeconds - awayPrevKickoff) / 86400.0
        val deltaRest = homeRestDays - awayRestDays

        val sensitivity = config.restAdvantageSensitivity // 3.0
        val maxDelta = config.restAdvantageMaxShift // 0.09

        val bias = tanh(deltaRest / sensitivity)
        val shift = maxDelta * bias

        val homeProb = neutralProb + shift
        val awayProb = neutralProb - shift

        return Signal3Way(
            homeProb = homeProb,
            drawProb = drawProb,
            awayProb = awayProb,
            weight = config.homeAdvantageWeight,
            name = "Rest Advantage"
        )
    }
}
```

### 9.2. Tham số bổ sung trong `PredictionWeightConfig`
- `restAdvantageSensitivity: Double = 3.0` (Hệ số độ nhạy ngày nghỉ).
- `restAdvantageMaxShift: Double = 0.09` (Độ lệch xác suất tối đa khỏi baseline neutral $0.37$).

---

## 10. Các Giới hạn & Điểm chưa thể kết luận từ Dataset hiện tại

1. **Thiếu Full-time Whistle Timestamp**:
   - Khoảng nghỉ hiện tại tính từ `targetKickoff - previousKickoff`, bỏ qua thời lượng trận đấu thực tế ($90 - 120$ phút). Sai số này là không đáng kể đối với thang đo ngày, nhưng cần ghi nhận là một *modeling approximation*.
2. **Không có dữ liệu xoay tua đội hình (Squad Rotation / Lineup Quality)**:
   - Một đội bóng đá cúp giữa tuần có thể tung đội hình dự bị (B-team) để giữ sức cho đội hình chính vào cuối tuần. Mô hình Rest Advantage chỉ đo lường thời gian nghỉ của câu lạc bộ, không đo lường thể lực cá nhân từng cầu thủ.
3. **Ảnh hưởng của Di chuyển Địa lý (Travel Distance)**:
   - Dataset hiện tại không lưu khoảng cách di chuyển (km) giữa các thành phố thi đấu. Đội phải bay sang quốc gia khác đá cúp sẽ tiêu hao thể lực nhiều hơn đội đá sân nhà liên tiếp.

---

## 11. Bảng Tổng kết Exit Criteria (Phase R2)

- [x] **Distribution `deltaRest` đã được phân tích:** Hoàn thành trên $29,632$ trận ($26,705$ valid pairs).
- [x] **Các candidate formula đã được so sánh:** Linear, Tanh, Sigmoid.
- [x] **Một mathematical model được chọn:** Hyperbolic Tangent ($\tanh$).
- [x] **Sensitivity đã được xác định:** $S = 3.0$ ngày (dựa trên phân vị P10/P90).
- [x] **Saturation behavior đã được xác định:** Tiệm cận trơn tru tại $\pm 0.09$, bão hòa thực tế tại $\pm 7$ ngày.
- [x] **Missing-data behavior đã được xác định:** Option A — Neutral Baseline $[0.37, 0.26, 0.37]$ với `isAvailable = false`.
- [x] **Neutral baseline đã được xác định:** $[0.37, 0.26, 0.37]$ tại $\Delta \text{Rest} = 0$.
- [x] **Symmetry đã được kiểm chứng:** $P_H(+x) = P_A(-x)$ hoàn hảo.
- [x] **Monotonicity đã được kiểm chứng:** Đạo hàm $> 0$ cho Home, $< 0$ cho Away.
- [x] **Probability normalization đã được kiểm chứng:** $P(H) + P(D) + P(A) = 1.000000$ và $P \in [0.28, 0.46]$.
- [x] **Không có production source change:** $0$ file production bị sửa đổi.
- [x] **Không thay đổi Draw logic:** Giữ nguyên baseline $0.26$.
- [x] **Không tuning theo prediction accuracy:** Lựa chọn hoàn toàn dựa trên nguyên lý toán học và phân phối dữ liệu.
- [x] **Có đủ contract để viết implementation plan tiếp theo:** Đã có contract code chi tiết và sẵn sàng cho phase tiếp theo.
