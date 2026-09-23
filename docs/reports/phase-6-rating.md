# Phase 6 – Rating Algorithms

Tài liệu đặc tả kỹ thuật, mô hình toán học và báo cáo nghiệm thu chính thức của **Phase 6 – Rating: Elo Rating System** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Objective

Triển khai thuật toán **Đánh giá sức mạnh tương đối (Elo Rating System)** nhằm:
- Cung cấp primitive toán học thuần túy để cập nhật điểm số Elo sau từng trận đấu đơn lẻ giữa hai đối thủ.
- Tính toán xác suất kỳ vọng (Expected Score) dựa trên chênh lệch điểm số hiện tại của hai đội.
- Bảo toàn nguyên tắc đối xứng và bảo toàn tổng điểm (Zero-sum conservation) trong toàn hệ thống.
- Đảm bảo độ phức tạp thời gian đạt $\mathcal{O}(1)$ và không gian phụ trợ (Auxiliary Space) đạt $\mathcal{O}(1)$ tuyệt đối.
- Tách bạch trách nhiệm kiến trúc: Thuật toán Elo trong `:core:algorithm` chỉ cung cấp phép tính toán toán học không trạng thái (Stateless Primitive); Domain/Application Layer chịu trách nhiệm lưu trữ điểm số, sắp xếp lịch sử trận đấu theo thứ tự thời gian tăng dần (Chronological Order) và thực hiện duyệt tích lũy (Folding/Reducing).

---

## 2. Scope

### In Scope
- **Expected Score**: Tính xác suất điểm số kỳ vọng trong khoảng $(0.0, 1.0)$.
- **Elo Rating Update**: Cập nhật điểm Elo cho một đội dựa trên kết quả thực tế.
- **Match-level Rating Calculation**: Cập nhật đồng thời điểm số cho cả hai đội trong một trận đấu (`calculateMatch`).
- **Configurable K-Factor**: Hỗ trợ K-factor mặc định cấp instance (`defaultKFactor`) và K-factor tùy biến cấp method call.
- **Initial Rating Constant**: Hằng số chuẩn hóa điểm khởi đầu `DEFAULT_INITIAL_RATING = 1500.0`.
- **Validation**: Kiểm tra biên số học toàn diện cho ratings, K-factor và actual score.
- **Zero-Sum Rating Update**: Bảo toàn tuyệt đối $\Delta R_A + \Delta R_B = 0.0$ và $(R'_A + R'_B) == (R_A + R_B)$.
- **Unit Tests**: Bộ kiểm thử hành vi toàn diện 22 ca test.

### Out of Scope
- **Home Advantage**: Không đưa điểm thưởng sân nhà vào algorithm primitive (để Domain layer xử lý nếu cần).
- **Rating History Persistence**: Không lưu trữ bảng xếp hạng hay snapshot lịch sử trong `:core:algorithm`.
- **Domain Entities**: Không tham chiếu đến `Match`, `Team`, `League`.
- **Data / Repository / API**: Không phụ thuộc Room Database hay Retrofit.
- **Prediction / Machine Learning**: Không bao gồm mô hình hồi quy hay học máy (dành cho Phase 7, 8, 9).
- **UI / Data Visualization**: Không kéo Compose hay Chart components vào thuật toán.

---

## 3. Architecture

### 3.1. Ranh giới module
- Package: `dev.anhquocs.truelab.core.algorithm.rating`.
- Pure Kotlin/JVM Library (Java Toolchain 11, chỉ phụ thuộc JUnit 4 khi test).
- Tuyệt đối không phụ thuộc Android SDK, Room Database, Retrofit API hay Domain Entities.
- Hoàn toàn độc lập với `dev.anhquocs.truelab.core.algorithm.evaluation` (không phụ thuộc vào enum `MatchOutcome` của Phase 5 để đảm bảo tính module hóa độc lập).

### 3.2. Luồng Dữ Liệu Tích Hợp (Data Flow)

```text
               Lịch sử trận đấu (Match History)
                               ↓
             Domain Layer (Chronological Ordering)
      [Sắp xếp tăng dần theo thời gian bằng Phase 2 Sorting]
                               ↓
                 Domain Layer (Data Mapping)
    [Trích xuất tỷ số, xác định Home/Away -> actualScore: Double]
    [Win = 1.0, Draw = 0.5, Loss = 0.0]
                               ↓
             RatingCalculator (:core:algorithm)
    [Tính expectedScore, updateRating hoặc calculateMatch theo O(1)]
                               ↓
                    Điểm Elo mới (New Ratings)
                               ↓
         Domain UseCase cập nhật Map<TeamId, Double>
              và tiếp tục fold cho trận kế tiếp
```

---

## 4. Elo Formula

### 4.1. Điểm số kỳ vọng (Expected Score)
Với hai đội có điểm Elo hiện tại là $R_A$ và $R_B$:

$$E_A = \frac{1}{1 + 10^{\frac{R_B - R_A}{400}}}$$

$$E_B = \frac{1}{1 + 10^{\frac{R_A - R_B}{400}}} = 1.0 - E_A$$

- $E_A \in (0.0, 1.0)$: Xác suất kỳ vọng của đội A.
- Nếu $R_A = R_B \implies E_A = E_B = 0.5$.
- Tổng xác suất bảo toàn: $E_A + E_B = 1.0$.

### 4.2. Cập nhật điểm số sau trận đấu (Rating Update)
Sau khi trận đấu kết thúc với kết quả thực tế $S_A$ của đội A:

$$R'_A = R_A + K \times (S_A - E_A)$$

$$R'_B = R_B + K \times (S_B - E_B)$$

Trong đó:
- $R_A, R_B$: Điểm Elo hiện tại trước trận đấu.
- $R'_A, R'_B$: Điểm Elo mới sau trận đấu.
- $E_A, E_B$: Điểm số kỳ vọng của hai đội.
- $S_A, S_B$: Kết quả thực tế (Actual Score) chuẩn hóa của Elo bóng đá:
  - **Thắng (Win)**: $S = 1.0$
  - **Hòa (Draw)**: $S = 0.5$
  - **Thua (Loss)**: $S = 0.0$
- $K$: Hệ số K-factor.

### 4.3. Tính chất bảo toàn Zero-Sum
Trong một trận đấu giữa hai đội, kết quả thực tế luôn đối xứng: $S_B = 1.0 - S_A$. Do đó:
$$\Delta R_B = K(S_B - E_B) = K((1.0 - S_A) - (1.0 - E_A)) = K(E_A - S_A) = -\Delta R_A$$
$$\implies \Delta R_A + \Delta R_B = 0.0$$
$$\implies R'_A + R'_B = R_A + R_B$$

---

## 5. API Contract

Để tránh lỗi nhầm lẫn default parameter giữa Interface và Implementation của Kotlin, API chính thức sử dụng **Method Overloading**:

```kotlin
package dev.anhquocs.truelab.core.algorithm.rating

interface RatingCalculator {

    fun expectedScore(
        rating: Double,
        opponentRating: Double
    ): Double

    // Overload không truyền K: Sử dụng defaultKFactor của instance
    fun updateRating(
        rating: Double,
        opponentRating: Double,
        actualScore: Double
    ): Double

    // Overload có K: Ghi đè K-factor trực tiếp cho lần gọi này
    fun updateRating(
        rating: Double,
        opponentRating: Double,
        actualScore: Double,
        kFactor: Double
    ): Double

    // Overload không truyền K: Sử dụng defaultKFactor của instance
    fun calculateMatch(
        ratingA: Double,
        ratingB: Double,
        actualScoreA: Double
    ): RatingMatchResult

    // Overload có K: Ghi đè K-factor trực tiếp cho lần gọi này
    fun calculateMatch(
        ratingA: Double,
        ratingB: Double,
        actualScoreA: Double,
        kFactor: Double
    ): RatingMatchResult

    companion object {
        const val DEFAULT_K: Double = 32.0
        const val DEFAULT_INITIAL_RATING: Double = 1500.0
    }
}
```

### Cơ chế hoạt động của K-Factor:
- Khi caller gọi hàm không truyền tham số `kFactor`: Thuật toán sử dụng giá trị `defaultKFactor` được cấu hình khi khởi tạo instance `EloRatingCalculator(defaultKFactor)`.
- Khi caller truyền tham số `kFactor` tường minh: Giá trị này sẽ ghi đè (override) `defaultKFactor` của instance trong phạm vi lời gọi hàm đó.
- Hành vi này đã được kiểm định nghiêm ngặt qua regression tests.

---

## 6. Implementation

Các thành phần được triển khai tại package `dev.anhquocs.truelab.core.algorithm.rating`:

1. [RatingMatchResult.kt](../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/rating/RatingMatchResult.kt):
   - Data class bất biến chứa kết quả cập nhật Elo cho cả hai đội sau một trận đấu.
2. [RatingCalculator.kt](../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/rating/RatingCalculator.kt):
   - Interface contract định nghĩa các hàm tính toán xác suất kỳ vọng và cập nhật điểm số thông qua method overloading.
3. [EloRatingCalculator.kt](../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/rating/EloRatingCalculator.kt):
   - Lớp hiện thực Standard Elo Rating System.
   - Nhận `defaultKFactor` tại constructor (mặc định là `DEFAULT_K = 32.0`), validate giá trị ngay tại `init` block.
   - Cài đặt các overload không có `kFactor` ủy quyền trực tiếp cho `defaultKFactor`.
   - Áp dụng kỹ thuật $\Delta R_B = -\Delta R_A$ trong `calculateMatch` để đảm bảo bảo toàn số học tuyệt đối.

---

## 7. Validation

Mọi hàm trong `EloRatingCalculator` đều thực thi kiểm tra điều kiện tiên quyết nghiêm ngặt trước khi tính toán:

| Tham số | Quy tắc kiểm tra | Hành vi khi vi phạm |
| :--- | :--- | :--- |
| `rating`, `opponentRating` | Phải là số thực hữu hạn (`rating.isFinite()`) | Ném `IllegalArgumentException` nếu là `NaN`, `+Infinity`, `-Infinity` |
| `actualScore` | Phải hữu hạn và nằm trong đoạn $[0.0, 1.0]$ | Ném `IllegalArgumentException` nếu $< 0.0$ hoặc $> 1.0$ hoặc `NaN` |
| `kFactor` | Phải là số thực hữu hạn và $> 0.0$ | Ném `IllegalArgumentException` nếu $\le 0.0$ hoặc phi số thực |
| `defaultKFactor` (Constructor) | Phải là số thực hữu hạn và $> 0.0$ | Ném `IllegalArgumentException` ngay tại `init` block khi khởi tạo |

- **Điểm số âm**: Điểm số âm (Negative Ratings) vẫn được chấp nhận hợp lệ vì công thức Elo phụ thuộc hoàn toàn vào hiệu số $(R_B - R_A)$.

---

## 8. RatingMatchResult

Data class đóng gói kết quả của hàm `calculateMatch`:

```kotlin
data class RatingMatchResult(
    val newRatingA: Double,
    val newRatingB: Double,
    val ratingChange: Double,
    val expectedScoreA: Double,
    val expectedScoreB: Double
)
```

- `newRatingA`: Điểm Elo mới của Đội A ($R_A + \Delta R_A$).
- `newRatingB`: Điểm Elo mới của Đội B ($R_B - \Delta R_A$).
- `ratingChange`: Lượng điểm biến thiên của Đội A ($\Delta R_A = newRatingA - ratingA$). Đội B biến thiên lượng đối nghịch hoàn toàn ($-\Delta R_A$).
- `expectedScoreA`: Xác suất kỳ vọng của Đội A ($E_A \in (0.0, 1.0)$).
- `expectedScoreB`: Xác suất kỳ vọng của Đội B ($E_B = 1.0 - E_A$).

---

## 9. Complexity

- **Time Complexity**:
  - `expectedScore`: $\mathcal{O}(1)$
  - `updateRating`: $\mathcal{O}(1)$
  - `calculateMatch`: $\mathcal{O}(1)$
  - Toàn bộ các phép tính chỉ bao gồm số học cơ bản và 1 phép lũy thừa cơ số 10 (`Math.pow`).
- **Auxiliary Space**: $\mathcal{O}(1)$ tuyệt đối, không cấp phát mảng hoặc danh sách trung gian.
- **Lưu ý tích hợp**: Khi Domain UseCase thực hiện duyệt (fold) lịch sử $N$ trận đấu của một đội hoặc một giải đấu, tổng thời gian xử lý toàn chuỗi là $\mathcal{O}(N)$ tuyến tính tối ưu.

---

## 10. Testing

Bộ Unit Test toàn diện tại [EloRatingAlgorithmsTest.kt](../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/rating/EloRatingAlgorithmsTest.kt):

- **Số test Phase 6**: **22 / 22 tests PASSED (100%)**
- **Toàn bộ module `:core:algorithm`**: **95 / 95 tests PASSED (100%)**

### Phân bổ Test Cases toàn bộ Module:
- Phase 1 – Searching: 12 tests
- Phase 2 – Sorting: 9 tests
- Phase 3 – Statistics: 16 tests
- Phase 4 – Trend: 21 tests
- Phase 5 – Evaluation: 15 tests
- **Phase 6 – Rating: 22 tests**

### Các nhóm kiểm thử chính trong Phase 6:
1. Hai đội bằng điểm ($R_A = R_B = 1500.0 \implies E_A = 0.5$).
2. Đội mạnh hơn có điểm kỳ vọng $> 0.5$.
3. Đội yếu hơn có điểm kỳ vọng $< 0.5$.
4. Tổng xác suất $E_A + E_B = 1.0$ trên nhiều cặp điểm.
5. Cập nhật khi Thắng ($S = 1.0$) làm tăng điểm.
6. Cập nhật khi Thua ($S = 0.0$) làm giảm điểm.
7. Cập nhật khi Hòa ($S = 0.5$) giữa hai đội bằng điểm giữ nguyên điểm số ($\Delta R = 0$).
8. Cập nhật khi Hòa: Đội mạnh hơn bị trừ điểm.
9. Cập nhật khi Hòa: Đội yếu hơn được cộng điểm.
10. Bảo toàn Zero-sum ($\Delta R_A + \Delta R_B = 0$) cho mọi kết quả.
11. Tỷ lệ biến thiên theo K-factor ($K=64$ gấp đôi $K=32$).
12. Chênh lệch điểm số cực đoan ($3500$ vs $1000$) không gây tràn số hay `NaN`.
13. Xử lý chính xác điểm Elo âm.
14. Validation: $K = 0$ ném ngoại lệ.
15. Validation: $K < 0$ ném ngoại lệ.
16. Validation: $S > 1.0$ ném ngoại lệ.
17. Validation: $S < 0.0$ ném ngoại lệ.
18. Validation: Rating là `NaN` ném ngoại lệ.
19. Validation: Rating là `Infinity` ném ngoại lệ.
20. `defaultKFactor` của constructor được sử dụng khi bỏ qua $K$.
21. Tham số `kFactor` truyền trực tiếp vào method override đúng `defaultKFactor`.
22. Constructor `defaultKFactor` không hợp lệ ($\le 0$, `NaN`, $\infty$) ném ngoại lệ ngay khi khởi tạo.

---

## 11. Integration With Previous Phases

- **Tách biệt với Phase 5 (Evaluation - Form Score)**:
  - Phase 5 sử dụng quy ước điểm giải đấu: Thắng = 3.0, Hòa = 1.0, Thua = 0.0 để lượng hóa phong độ ngắn hạn trong thang $[0, 100]$.
  - Phase 6 sử dụng chuẩn xác suất của Elo: Thắng = 1.0, Hòa = 0.5, Thua = 0.0.
  - Phase 6 không import hoặc phụ thuộc vào `MatchOutcome` của Phase 5, giữ cho hai hệ thống hoàn toàn độc lập và không bị ràng buộc chéo.
- **Kết hợp với Phase 2 (Sorting)**: Domain layer sử dụng `TimSort` hoặc `QuickSort` từ Phase 2 để sắp xếp lịch sử thi đấu theo thứ tự thời gian tăng dần trước khi truyền vào Elo Calculator.
- **Kết hợp với Phase 4 (Trend - SMA)**: Domain layer có thể áp dụng `SimpleMovingAverageCalculator` lên chuỗi điểm số Elo theo thời gian để theo dõi xu hướng phát triển dài hạn của đội bóng.

---

## 12. Project Impact

- **Hoàn thành phân hệ Rating**: Bổ sung mảnh ghép đánh giá sức mạnh nội tại (Intrinsic Strength Rating) vào Algorithm Portfolio của TrueLab.
- **Nền tảng cho Phase 7 (Prediction)**: Hàm `expectedScore(ratingA, ratingB)` cung cấp trực tiếp xác suất chiến thắng kỳ vọng làm tham số đầu vào cho các mô hình dự đoán và tính kèo xác suất ở Phase 7.
- **Bảo toàn kiến trúc Clean Architecture**: Giữ `:core:algorithm` luôn là tập hợp các primitive thuần túy, trao quyền điều phối trạng thái và dữ liệu lịch sử cho Domain Layer.
- **Không gây ảnh hưởng đến Phase 1–5**: 100% mã nguồn và test của các phase trước được giữ nguyên trạng và vượt qua toàn bộ test suite.

---

## 13. Non-Scope / Future Work

Các tính năng không thuộc phạm vi Phase 6 và có thể mở rộng ở các tầng kiến trúc cao hơn:
- Lợi thế sân nhà (Home Advantage - có thể bổ sung tại Domain UseCase).
- Hệ số K động theo số bàn thắng cách biệt (Goal Difference Multiplier).
- Lưu trữ bảng xếp hạng Elo vào cơ sở dữ liệu Room Database.
- Mô hình Machine Learning hoặc dự đoán tỷ số trận đấu (thuộc Phase 7, 8, 9).
- Giao diện biểu đồ Elo trên Jetpack Compose UI.

---

## 14. Verification

Kiểm thử xác thực thực tế trên môi trường Gradle:

```text
> Task :core:algorithm:test

BUILD SUCCESSFUL in 5s
4 actionable tasks: 4 executed
```

- **Kết quả**: 95 / 95 tests toàn bộ `:core:algorithm` đạt 100% PASS.
- **Phase 6**: 22 / 22 tests đạt 100% PASS.
- **Không có regression**: Không có lỗi phát sinh trên các module hiện hữu.

---

## 15. Status

**Phase 6 – Rating: Frozen**

Thuật toán đã được triển khai, kiểm thử toàn diện, khắc phục triệt để các vấn đề phát hiện trong Code Review và chính thức đóng băng theo đúng quy chuẩn dự án TrueLab.
