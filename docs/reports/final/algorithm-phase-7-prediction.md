# Phase 7 – Prediction Algorithms: Weighted Scoring

Tài liệu đặc tả kỹ thuật, mô hình toán học và báo cáo nghiệm thu chính thức của **Phase 7 – Prediction: Weighted Scoring** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Objective (Mục tiêu)

Triển khai phân hệ thuật toán **Phase 7 – Prediction: Weighted Scoring**, là **Final Phase** của Core Algorithm Roadmap (Phase 1–7) nhằm:
- Cung cấp một Prediction Engine toán học thuần túy Kotlin/JVM (Java Toolchain 11, không phụ thuộc Android SDK hay bất kỳ thư viện ngoài nào ngoài JUnit 4 khi kiểm thử).
- Triển khai các primitive tính điểm trọng số chuẩn hóa (Normalized Weighted Average) và chuẩn hóa xác suất 3 kết quả thi đấu (*Home Win / Draw / Away Win*) tất định (deterministic), ổn định số học (numerically stable).
- Triển khai thuật toán tổ hợp tuyến tính (Linear Mixture) từ nhiều tín hiệu xác suất 3 chiều độc lập.
- Áp dụng cơ chế phân định hòa điểm đối xứng (Symmetrical Tie-Breaking Rule), giải quyết triệt để sự phụ thuộc vào thứ tự khai báo enum hay cấu trúc rẽ nhánh `if/when`.
- Bảo toàn nguyên tắc **Clean Architecture**:
  - `:core:algorithm` thuần túy là generic mathematical engine, chỉ thao tác trên các abstraction số học (`WeightedFeature`, `Signal3Way`) và phân phối xác suất (`OutcomeProbabilities`).
  - `:core:domain` chịu trách nhiệm thu thập dữ liệu lịch sử, gọi các thuật toán thống kê (Phase 3), phong độ (Phase 5), Elo rating (Phase 6), chuẩn hóa các đặc trưng thành khoảng $[0.0, 1.0]$, quản lý cấu hình tỷ trọng bóng đá (Form 25%, Elo 20%,...) và truyền vào prediction engine.
  - **Khẳng định phạm vi**: Toàn bộ việc tích hợp tầng Domain và các hằng số tỷ trọng bóng đá cụ thể là **hoàn toàn nằm ngoài phạm vi (OUT OF SCOPE)** của Phase 7 algorithm implementation.

---

## 2. Scope (Phạm vi)

### In Scope
- **Generic Weighted Scoring Primitive**:
  - Tính điểm tổng hợp trọng số:
    $$S = \frac{\sum_{i=1}^m w_i \cdot x_i}{\sum_{i=1}^m w_i}$$
  - Hỗ trợ feature score $x_i \in [0.0, 1.0]$, weight $w_i \ge 0.0$, $\sum w_i > 0$.
  - Tự động chuẩn hóa tổng trọng số (scale-invariant).
- **3-Way Match Outcome Probability Normalization**:
  - Chuẩn hóa 3 điểm số thô không âm ($S_H, S_D, S_A \ge 0.0$, $S_{\text{total}} > 0.0$) thành xác suất:
    $$P(H) = \frac{S_H}{S_{\text{total}}}, \quad P(D) = \frac{S_D}{S_{\text{total}}}, \quad P(A) = \frac{S_A}{S_{\text{total}}}$$
  - Giữ nguyên công thức toán học thuần túy, tuyệt đối không can thiệp cưỡng ép $P(A) = 1.0 - P(H) - P(D)$.
- **Linear Mixture of Probability Vectors**:
  - Tổ hợp xác suất từ danh sách $m$ tín hiệu xác suất 3 chiều ($\mathbf{p}_k = (p_{k,H}, p_{k,D}, p_{k,A})$) với trọng số $w_k \ge 0.0$.
- **Symmetrical Tie-Breaking Rule**:
  - Phân định kết quả dự đoán đối xứng: nếu chỉ có duy nhất 1 outcome đạt xác suất cực đại $\to$ chọn outcome đó; nếu từ 2 outcome trở lên cùng đạt cực đại $\to$ chọn `PredictedOutcome.DRAW`.
  - Bao phủ toàn diện 4 kịch bản hòa điểm.
- **Confidence Score**:
  - $\text{confidenceScore} = \max(P_H, P_D, P_A) \in [1/3, 1.0]$.
- **Data Models**:
  - `WeightedFeature`, `Signal3Way`, `PredictedOutcome`, `OutcomeProbabilities`.
- **Validation toàn diện**:
  - Chặn triệt để `NaN`, `±Infinity`, giá trị âm, ngoài khoảng $[0.0, 1.0]$, danh sách rỗng, tổng trọng số $\le 0.0$.
  - Kiểm tra từng xác suất thành phần của `Signal3Way` phải thuộc $[0.0, 1.0]$ và tổng xấp xỉ 1.0 trong ngưỡng sai số $\pm 10^{-4}$.
- **Unit Testing Suite**:
  - 27 test cases độc lập bao phủ 100% logic toán học và các ca bất thường.

### Out of Scope
- **Machine Learning**: Tuyệt đối không dùng mô hình học máy (Phase 8 Logistic Regression và Phase 9 Decision Tree là các extension tùy chọn, chưa triển khai).
- **Model Training / Optimization**: Không tối ưu hóa trọng số tự động (Gradient Descent).
- **Domain Integration & Football Constants**: Không hard-code các tỷ trọng Form 25%, Elo 20%, Goals 15%, Odds 20%, H2H 10%, Home Advantage 10% vào `:core:algorithm`.
- **Domain Entities**: Không tham chiếu đến `Match`, `Team`, `Odds`, `matchId`, `Room`, `Retrofit`.
- **Lưu trữ dữ liệu / State Persistence**: Thuật toán là stateless pure function.

---

## 3. Architecture & Data Flow

### 3.1. Ranh giới Module
- Package: `dev.anhquocs.truelab.core.algorithm.prediction`.
- Pure Kotlin/JVM Library (Java Toolchain 11, chỉ phụ thuộc JUnit 4 khi test).
- Tách bạch rõ ràng giữa Interface contract (`WeightedScorer`) và Implementation (`DefaultWeightedScorer`).

### 3.2. Sơ đồ Tích hợp Phân lớp (Architecture Data Flow)

```text
                      [ Room Database / Remote API ]
                                    │
                                    ▼
                 :core:domain (PredictMatchUseCase)
      ┌─────────────────────────────┼─────────────────────────────┐
      ▼                             ▼                             ▼
FormEvaluator              EloRatingCalculator          DescriptiveStatistics
(Phase 5)                  (Phase 6)                    (Phase 3)
      │                             │                             │
      ▼                             ▼                             ▼
 [Form Score]               [Expected Score]             [Goals / H2H Stats]
      └─────────────────────────────┼─────────────────────────────┘
                                    │
                                    ▼
             Domain Feature Extraction & Normalization
         [Ánh xạ sang List<WeightedFeature> hoặc List<Signal3Way>]
         [Cấu hình tỷ trọng thuộc Domain: 25% | 20% | 15% | 20% | 10% | 10%]
                                    │
                                    ▼
                WeightedScorer (:core:algorithm)
              [DefaultWeightedScorer - Pure Math]
                                    │
                                    ▼
                          OutcomeProbabilities
                                    │
                                    ▼
                            PredictionResult
                          (:core:domain:model)
```

---

## 4. Mathematical Foundation (Cơ sở Toán học)

### 4.1. Single-Dimension Normalized Weighted Average
Cho tập hợp $m$ đặc trưng với điểm đã chuẩn hóa $x_i \in [0.0, 1.0]$ và trọng số $w_i \ge 0.0$ ($\sum_{i=1}^m w_i > 0$):
$$S = \frac{\sum_{i=1}^m w_i \cdot x_i}{\sum_{i=1}^m w_i}$$

**Tính chất toán học**:
1. **Bảo toàn miền giá trị**: $\forall i, x_i \in [0.0, 1.0] \implies S \in [0.0, 1.0]$.
2. **Bất biến theo tỷ lệ trọng số (Scale Invariance)**:
   $$\forall c > 0, \quad \frac{\sum (c \cdot w_i) x_i}{\sum (c \cdot w_i)} = \frac{c \sum w_i x_i}{c \sum w_i} = \frac{\sum w_i x_i}{\sum w_i} = S$$
3. **Trung lập trọng số 0 (Zero-Weight Neutrality)**: Các đặc trưng có $w_i = 0$ đóng góp 0 vào cả tử số và mẫu số, hoàn toàn không làm sai lệch kết quả của các đặc trưng khác.

### 4.2. 3-Way Sum Normalization
Cho 3 điểm số thô $S_H \ge 0.0, S_D \ge 0.0, S_A \ge 0.0$ với $S_{\text{total}} = S_H + S_D + S_A > 0.0$:
$$P(H) = \frac{S_H}{S_{\text{total}}}, \quad P(D) = \frac{S_D}{S_{\text{total}}}, \quad P(A) = \frac{S_A}{S_{\text{total}}}$$
- Bảo đảm tính toàn vẹn: $P(H) + P(D) + P(A) = 1.0$.
- Không dùng phép gán cưỡng ép phần tử cuối $P(A) = 1.0 - P(H) - P(D)$ nhằm tránh bóp méo xác suất thực tế.

### 4.3. Linear Mixture of Probability Vectors
Cho $m$ tín hiệu độc lập, trong đó tín hiệu $k$ cung cấp vector xác suất $\mathbf{p}_k = (p_{k,H}, p_{k,D}, p_{k,A})$ thỏa mãn $\sum_{c \in \{H, D, A\}} p_{k,c} = 1.0$ kèm trọng số $w_k \ge 0.0$:
$$P_c = \frac{\sum_{k=1}^m w_k \cdot p_{k,c}}{\sum_{k=1}^m w_k}, \quad \forall c \in \{H, D, A\}$$
Do tính chất tuyến tính:
$$\sum_{c} P_c = \frac{\sum_{k=1}^m w_k \left(\sum_c p_{k,c}\right)}{\sum_{k=1}^m w_k} = \frac{\sum_{k=1}^m w_k \cdot 1.0}{\sum_{k=1}^m w_k} = 1.0$$

### 4.4. Symmetrical Tie-Breaking Rule
Đặt $M = \max(P_H, P_D, P_A)$. Đếm số lượng outcome đạt giá trị cực đại $M$:
$$K = \left|\{c \in \{H, D, A\} \mid P_c == M\}\right|$$

- **Nếu duy nhất 1 outcome đạt cực đại ($K = 1$)**:
  - $P_H == M \implies \text{PredictedOutcome.HOME\_WIN}$
  - $P_D == M \implies \text{PredictedOutcome.DRAW}$
  - $P_A == M \implies \text{PredictedOutcome.AWAY\_WIN}$
- **Nếu từ 2 outcome trở lên cùng đạt cực đại ($K \ge 2$)**:
  - Luôn chọn $\implies \text{PredictedOutcome.DRAW}$

**Bao phủ tường minh 4 kịch bản hòa điểm**:
1. $P_H == P_A > P_D \implies \text{DRAW}$ (ví dụ: $0.40, 0.20, 0.40$).
2. $P_H == P_D > P_A \implies \text{DRAW}$ (ví dụ: $0.40, 0.40, 0.20$).
3. $P_D == P_A > P_H \implies \text{DRAW}$ (ví dụ: $0.20, 0.40, 0.40$).
4. $P_H == P_D == P_A = 1/3 \implies \text{DRAW}$ (ví dụ: $0.3333333333, \dots$).

Quy tắc này hoàn toàn đối xứng, khách quan và không phụ thuộc vào thứ tự khai báo enum hay cấu trúc rẽ nhánh.

### 4.5. Confidence Score
$$\text{confidenceScore} = \max(P_H, P_D, P_A) \in \left[\frac{1}{3}, 1.0\right]$$

---

## 5. API Design & Component Contract

### 5.1. Data Models
- **`PredictedOutcome`**:
  ```kotlin
  enum class PredictedOutcome {
      HOME_WIN,
      DRAW,
      AWAY_WIN
  }
  ```
- **`OutcomeProbabilities`**:
  ```kotlin
  data class OutcomeProbabilities(
      val homeWinProb: Double,
      val drawProb: Double,
      val awayWinProb: Double,
      val predictedOutcome: PredictedOutcome,
      val confidenceScore: Double
  )
  ```
- **`WeightedFeature`**:
  ```kotlin
  data class WeightedFeature(
      val score: Double,
      val weight: Double,
      val name: String = ""
  )
  ```
- **`Signal3Way`**:
  ```kotlin
  data class Signal3Way(
      val homeProb: Double,
      val drawProb: Double,
      val awayProb: Double,
      val weight: Double,
      val name: String = ""
  )
  ```

### 5.2. Interface `WeightedScorer`
```kotlin
interface WeightedScorer {
    fun calculateScore(features: List<WeightedFeature>): Double
    fun <T> calculateScore(
        dataset: List<T>,
        scoreSelector: (T) -> Double,
        weightSelector: (T) -> Double
    ): Double
    fun predict3Way(homeScore: Double, drawScore: Double, awayScore: Double): OutcomeProbabilities
    fun predictOutcome(signals: List<Signal3Way>): OutcomeProbabilities
}
```

### 5.3. Implementation `DefaultWeightedScorer`
- Thuần túy Kotlin/JVM, không lưu giữ mutable state (thread-safe, deterministic).
- Cung cấp hằng số dung sai kiểm tra xác suất: `PROBABILITY_SUM_TOLERANCE = 1e-4`.

---

## 6. Validation Rules & Defensive Programming

| Kiểm tra | Điều kiện vi phạm | Loại Ngoại lệ | Thông báo lỗi chuẩn |
| :--- | :--- | :--- | :--- |
| **Empty Input** | `dataset.isEmpty()` | `IllegalArgumentException` | `"Features/signals list must not be empty"` |
| **Non-finite Score** | `!score.isFinite()` | `IllegalArgumentException` | `"Feature score must be finite, but was $score"` |
| **Score Out of Range** | `score !in 0.0..1.0` | `IllegalArgumentException` | `"Feature score must be in range [0.0, 1.0], but was $score"` |
| **Negative Weight** | `weight < 0.0` | `IllegalArgumentException` | `"Feature weight must be non-negative, but was $weight"` |
| **Non-finite Weight** | `!weight.isFinite()` | `IllegalArgumentException` | `"Feature weight must be finite, but was $weight"` |
| **Total Weight Zero** | `totalWeight <= 0.0` | `IllegalArgumentException` | `"Total weight must be positive, but was $totalWeight"` |
| **Negative 3-Way Score** | `homeScore < 0.0 \|\| ...` | `IllegalArgumentException` | `"homeScore must be non-negative, but was $homeScore"` |
| **Total 3-Way Zero** | `total <= 0.0` | `IllegalArgumentException` | `"Total 3-way score must be positive, but was $total"` |
| **Signal3Way Component** | `hp !in 0.0..1.0 \|\| ...` | `IllegalArgumentException` | `"homeProb must be in range [0.0, 1.0], but was $hp"` |
| **Signal3Way Prob Sum** | `abs(sum - 1.0) > 1e-4` | `IllegalArgumentException` | `"Probabilities of signal ... must sum to 1.0 (±1e-4), but was $sum"` |

> [!IMPORTANT]
> **Bảo vệ chống lọt xác suất không hợp lệ**:
> Việc kiểm tra độc lập từng thành phần `require(prob in 0.0..1.0)` ngăn chặn triệt để trường hợp xác suất âm hoặc $> 1.0$ lọt qua ngay cả khi tổng của chúng vô tình bằng 1.0 (ví dụ: $1.2, -0.1, -0.1$).

---

## 7. Complexity Analysis (Phân tích Độ phức tạp)

| Thao tác | Time Complexity | Auxiliary Space Complexity | Cơ chế cài đặt |
| :--- | :---: | :---: | :--- |
| `calculateScore(features)` | $\mathcal{O}(m)$ | $\mathcal{O}(1)$ | Duyệt 1 vòng lặp tích lũy qua biến thanh ghi |
| `calculateScore(dataset, sel1, sel2)` | $\mathcal{O}(m)$ | $\mathcal{O}(1)$ | Duyệt trực tiếp, không gọi `.map()` tạo mảng |
| `predict3Way(home, draw, away)` | $\mathcal{O}(1)$ | $\mathcal{O}(1)$ | 3 phép chia cố định |
| `predictOutcome(signals)` | $\mathcal{O}(m)$ | $\mathcal{O}(1)$ | Duyệt 1 vòng lặp tích lũy 3 xác suất độc lập |

---

## 8. Test Suite & Verification Matrix (Ma trận Kiểm thử)

Bộ kiểm thử [WeightedScoringAlgorithmsTest.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/WeightedScoringAlgorithmsTest.kt) gồm **27 test cases** độc lập:

### Nhóm 1: Single-Dimension Weighted Scoring (12 tests)
1. `calculateScore - single feature returns exact score`
2. `calculateScore - equal weights equals arithmetic mean`
3. `calculateScore - unequal weights calculates correct weighted average`
4. `calculateScore - zero weight feature does not affect result`
5. `calculateScore - scale invariance when multiplying weights by constant`
6. `calculateScore - boundary values (all zeros and all ones)`
7. `calculateScore - generic selector overload matches direct list calculation`
8. `calculateScore - throws on empty list`
9. `calculateScore - throws on negative weight`
10. `calculateScore - throws on all weights zero`
11. `calculateScore - throws on NaN or infinite score or weight`
12. `calculateScore - throws on score outside range 0 to 1`

### Nhóm 2: 3-Way Outcome Normalization (7 tests)
13. `predict3Way - dominant home score predicts HOME_WIN`
14. `predict3Way - dominant draw score predicts DRAW`
15. `predict3Way - dominant away score predicts AWAY_WIN`
16. `predict3Way - probabilities sum to 1_0 within 1e-9 tolerance`
17. `predict3Way - throws on negative score`
18. `predict3Way - throws on all zeros`
19. `predict3Way - throws on NaN or infinite score`

### Nhóm 3: Toàn bộ 4 Kịch bản Hòa điểm / Tie-Breaking (4 tests)
20. `predict3Way - tie HOME == AWAY greater than DRAW yields DRAW`
21. `predict3Way - tie HOME == DRAW greater than AWAY yields DRAW`
22. `predict3Way - tie DRAW == AWAY greater than HOME yields DRAW`
23. `predict3Way - tie HOME == DRAW == AWAY yields DRAW`

### Nhóm 4: Linear Mixture & Determinism (4 tests)
24. `predictOutcome - linear mixture computes correct combined probabilities`
25. `predictOutcome - tie breaking in linear mixture yields DRAW`
26. `predictOutcome - confidence score equals max probability`
27. `predictOutcome - immutability and determinism across repeated invocations`

---

## 9. Verification Results & Regression Check

Kết quả thực thi kiểm thử toàn bộ module `:core:algorithm`:
```powershell
.\gradlew.bat :core:algorithm:test --rerun-tasks
```

```text
BUILD SUCCESSFUL in 9s
4 actionable tasks: 4 executed
```

### Bảng tổng hợp số lượng ca kiểm thử theo Phase:

| Phân hệ thuật toán | Package | Số lượng test | Trạng thái |
| :--- | :--- | :---: | :---: |
| **Phase 1 – Searching** | `searching` | 12 | ✅ **PASS** |
| **Phase 2 – Sorting** | `sorting` | 9 | ✅ **PASS** |
| **Phase 3 – Statistics** | `statistics` | 16 | ✅ **PASS** |
| **Phase 4 – Trend** | `trend` | 21 | ✅ **PASS** |
| **Phase 5 – Evaluation** | `evaluation` | 15 | ✅ **PASS** |
| **Phase 6 – Rating** | `rating` | 22 | ✅ **PASS** |
| **Phase 7 – Prediction** | `prediction` | **27** | ✅ **PASS** |
| **TỔNG CỘNG** | `:core:algorithm` | **122** | ✅ **122/122 PASS** |

- **Không có bất kỳ lỗi (failure/error)**.
- **Không có hồi quy (zero regression)** trên các Phase 1–6 đã Frozen.

---

## 10. Code Review Audit Summary

| Hạng mục đánh giá | Kết quả | Ghi chú |
| :--- | :---: | :--- |
| **Mathematical Correctness** | **PASS** | Công thức $S$, 3-Way Sum Normalization, Linear Mixture cài đặt chính xác |
| **Signal3Way Validation** | **PASS** | Kiểm soát đa tầng, chặn triệt để xác suất âm hoặc $> 1.0$ |
| **Tie-Breaking Symmetry** | **PASS** | Đối xứng tuyệt đối, xử lý đúng cả 4 trường hợp hòa điểm |
| **Numerical Stability** | **PASS** | Hoạt động chính xác trên số thực IEEE 754, tolerance $10^{-9}$ trong test |
| **API Contract** | **PASS** | Đúng 100% hợp đồng đã duyệt, không API thừa |
| **Immutability & Determinism**| **PASS** | Các data models bất biến, class stateless, kết quả nhất quán 100% |
| **Complexity** | **PASS** | Đúng $\mathcal{O}(m)$ và $\mathcal{O}(1)$ thời gian, $\mathcal{O}(1)$ bộ nhớ phụ trợ |
| **Architecture Boundaries** | **PASS** | Thuần túy Kotlin/JVM, 0 dependencies ngoài, không football constants |
| **Test Quality** | **PASS** | 27 ca test phủ toàn diện biên, ngoại lệ và cả 4 trường hợp hòa |
| **Git Scope** | **PASS** | Chỉ thay đổi đúng phạm vi Phase 7, chưa commit/push |
| **OVERALL VERDICT** | **PASS** | **Chính thức nghiệm thu kỹ thuật Phase 7** |

---

## 11. Artifacts & Git Scope Summary

### Danh sách file thực tế thay đổi:
- **Tạo mới trong `:core:algorithm:main`**:
  - `dev/anhquocs/truelab/core/algorithm/prediction/WeightedScorer.kt`
  - `dev/anhquocs/truelab/core/algorithm/prediction/DefaultWeightedScorer.kt`
  - `dev/anhquocs/truelab/core/algorithm/prediction/WeightedFeature.kt`
  - `dev/anhquocs/truelab/core/algorithm/prediction/Signal3Way.kt`
  - `dev/anhquocs/truelab/core/algorithm/prediction/OutcomeProbabilities.kt`
  - `dev/anhquocs/truelab/core/algorithm/prediction/PredictedOutcome.kt`
- **Tạo mới trong `:core:algorithm:test`**:
  - `dev/anhquocs/truelab/core/algorithm/prediction/WeightedScoringAlgorithmsTest.kt`
- **Xóa bỏ**:
  - `dev/anhquocs/truelab/core/algorithm/prediction/Prediction.kt`
- **Tài liệu nghiệm thu**:
  - `docs/reports/final/algorithm-phase-7-prediction.md`

### Xác nhận trạng thái Git:
- Không sửa file nào thuộc Phase 1–6.
- Không sửa `README.md`.
- Không sửa `:core:domain`.
- **Chưa thực hiện commit, chưa push**.
