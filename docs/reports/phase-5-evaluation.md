# Phase 5 – Evaluation: Form Score Specification & Report

Tài liệu đặc tả kỹ thuật, mô hình toán học và báo cáo nghiệm thu chính thức của **Phase 5 – Evaluation: Form Score** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Objective

Triển khai thuật toán **Đánh giá phong độ thi đấu (Form Evaluation)** nhằm:
- Lượng hóa chuỗi kết quả thi đấu ngắn hạn của một đội bóng thành chỉ số định lượng chuẩn hóa trên thang điểm $[0.0, 100.0]$.
- Cung cấp cơ chế đánh giá xung lực (Momentum) và độ nhạy thời gian (Recency Sensitivity) thông qua mô hình trọng số suy giảm tuyến tính (Linear Time-Decay).
- Nhận dữ liệu đầu vào là chuỗi kết quả thi đấu đã được sắp xếp tăng dần theo thời gian (Chronological Ascending: phần tử cuối danh sách là trận đấu mới nhất).
- Kích thước cửa sổ mặc định được chuẩn hóa là $k = 5$ trận gần nhất (`windowSize = 5`).
- Đảm bảo độ phức tạp thời gian đạt $\mathcal{O}(\min(N, k))$ và độ phức tạp không gian phụ trợ (Auxiliary Space) đạt $\mathcal{O}(1)$ tuyệt đối mà không cấp phát collection trung gian.

---

## 2. Scope

### 2.1. In-Scope
- **Enum kết quả thi đấu**: [MatchOutcome.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/MatchOutcome.kt) quy ước điểm số chuẩn bóng đá (Thắng = 3, Hòa = 1, Thua = 0).
- **Mô hình kết quả**: [FormScore.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormScore.kt) đóng gói đầy đủ các trường thống kê và hai thang điểm phong độ.
- **Interface Contract**: [FormEvaluator.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormEvaluator.kt) định nghĩa API chuẩn và Generic Selector overload.
- **Lớp hiện thực cốt lõi**: [LinearDecayFormEvaluator.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/LinearDecayFormEvaluator.kt).
- **Hai mô hình phong độ**:
  - *Raw Form (`rawScore`)*: Phong độ tiêu chuẩn không trọng số (Unweighted Standard Form).
  - *Weighted Form (`score`)*: Phong độ có trọng số thời gian tuyến tính (Linear Time-Decay Weighted Form).
- **Generic Selector API**: Cho phép tính toán trực tiếp trên danh sách đối tượng tùy biến thông qua selector lambda `(T) -> MatchOutcome` mà không tạo mảng trung gian.
- **Bộ Unit Test toàn diện**: [FormScoreAlgorithmsTest.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormScoreAlgorithmsTest.kt) kiểm thử đầy đủ các điều kiện biên và bất biến toán học.

### 2.2. Non-Scope
- **Elo Rating System**: Thuộc phạm vi của Phase 6 – Rating.
- **Prediction / Machine Learning**: Không bao gồm mô hình dự đoán xác suất trận đấu hay hồi quy học máy.
- **UI / Presentation Layer**: Không chứa bất kỳ định dạng hiển thị, chuỗi đa ngôn ngữ hay Compose UI nào.
- **Data Layer / Room / Retrofit**: Không phụ thuộc vào cơ sở dữ liệu Room, Retrofit API hay DataSyncEngine.
- **Mã nguồn Phase 1–4**: Đã được đóng băng hoàn toàn, không thay đổi.

---

## 3. Design & API

### 3.1. MatchOutcome
Biểu diễn kết quả thi đấu với điểm số theo luật bóng đá quốc tế:

```kotlin
package dev.anhquocs.truelab.core.algorithm.evaluation

enum class MatchOutcome(val points: Double) {
    WIN(3.0),
    DRAW(1.0),
    LOSS(0.0)
}
```

### 3.2. FormScore
Đóng gói toàn bộ các chỉ số thống kê phản ánh cửa sổ được chọn (Selected Window):

```kotlin
package dev.anhquocs.truelab.core.algorithm.evaluation

data class FormScore(
    val score: Double,
    val rawScore: Double,
    val matchesCount: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val totalPoints: Double,
    val maxPoints: Double
)
```

- `score`: Điểm phong độ có trọng số thời gian (Linear Time-Decay) trên thang $[0.0, 100.0]$.
- `rawScore`: Điểm phong độ không trọng số (Unweighted Standard) trên thang $[0.0, 100.0]$.
- `matchesCount`: Số trận thực tế được đưa vào tính toán ($m = \min(N, windowSize)$).
- `wins`: Số trận thắng trong selected window.
- `draws`: Số trận hòa trong selected window.
- `losses`: Số trận thua trong selected window.
- `totalPoints`: Tổng điểm thực tế giành được ($W=3, D=1, L=0$).
- `maxPoints`: Điểm tối đa lý thuyết có thể đạt được trong cửa sổ ($3.0 \times matchesCount$).

### 3.3. FormEvaluator
Interface contract định nghĩa API công khai:

```kotlin
package dev.anhquocs.truelab.core.algorithm.evaluation

interface FormEvaluator {

    fun evaluate(
        outcomes: List<MatchOutcome>,
        windowSize: Int = 5
    ): FormScore

    fun <T> evaluate(
        dataset: List<T>,
        windowSize: Int = 5,
        selector: (T) -> MatchOutcome
    ): FormScore
}
```

### 3.4. LinearDecayFormEvaluator
Lớp hiện thực tối ưu hóa bộ nhớ và hiệu năng:
- Áp dụng kỹ thuật duyệt trực tiếp danh sách từ chỉ số `startIndex = maxOf(0, n - windowSize)`.
- Hàm nội bộ `evaluateInternal` được đánh dấu `private inline` để triệt tiêu chi phí phân bổ đối tượng lambda cho cả hai overload.
- Áp dụng `selector` trực tiếp on-the-fly trên từng phần tử, loại bỏ hoàn toàn việc tạo danh sách trung gian (`dataset.map(selector)`).

---

## 4. Mathematical Model

### 4.1. Unweighted Standard Form (`rawScore`)
Mô hình phong độ truyền thống xem mọi trận đấu trong cửa sổ có tầm quan trọng ngang nhau:

$$\text{rawScore} = \frac{\text{totalPoints}}{\text{maxPoints}} \times 100.0 = \frac{\sum_{i=1}^{m} points_i}{3 \times m} \times 100.0$$

Trong đó:
- $m = \min(N, windowSize)$: Số trận trong cửa sổ đánh giá ($m > 0$).
- $points_i \in \{0.0, 1.0, 3.0\}$ tương ứng với kết quả của trận thứ $i$.
- $\text{maxPoints} = 3 \times m$: Điểm số tối đa nếu thắng tất cả các trận trong cửa sổ.

### 4.2. Linear Time-Decay Weighted Form (`score`)
Mô hình gán trọng số tăng dần theo thời gian, đặt trọng số cao hơn cho các trận đấu diễn ra gần thời điểm hiện tại nhất:

$$\text{score} = \frac{\sum_{i=1}^{m} (w_i \cdot points_i)}{3 \times \sum_{i=1}^{m} w_i} \times 100.0$$

Quy tắc gán trọng số:
- Trọng số $w_i = (i - startIndex + 1)$ với $i$ chạy từ $startIndex$ đến $N - 1$.
- Trận cũ nhất trong selected window luôn có trọng số $w = 1$.
- Trận mới nhất trong selected window luôn có trọng số $w = m$.
- Tổng trọng số:
  $$\sum_{i=1}^{m} w_i = \frac{m(m + 1)}{2}$$

> [!NOTE]
> **Lưu ý về ý nghĩa kỹ thuật**: Điểm số `score` (Weighted Form) không được xem là "chính xác hơn" `rawScore`. Cả hai đều là thước đo hợp lệ; trong đó `score` thể hiện **độ nhạy thời gian cao hơn (Higher Recency Sensitivity)**, phản ánh xung lực phục hồi hoặc đà sa sút gần đây của đội bóng nhạy bén hơn.

### 4.3. Ví dụ so sánh Recency Sensitivity
Xét hai đội bóng cùng đá 5 trận ($m = 5$), đều giành 1 chiến thắng và 4 thất bại:

| Kịch bản | Chuỗi kết quả ($t_1 \to t_5$) | `totalPoints` | `rawScore` | Weighted Calculation | `score` | Nhận xét xung lực |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Đội A (Thắng trận mới nhất)** | $[L, L, L, L, W]$ | 3.0 | **20.0%** | $\frac{1(0)+2(0)+3(0)+4(0)+5(3)}{3 \times 15} \times 100 = \frac{15}{45} \times 100$ | **33.33%** | Có xu hướng phục hồi phong độ |
| **Đội B (Thắng trận xa nhất)** | $[W, L, L, L, L]$ | 3.0 | **20.0%** | $\frac{1(3)+2(0)+3(0)+4(0)+5(0)}{3 \times 15} \times 100 = \frac{3}{45} \times 100$ | **6.67%** | Đang trên đà khủng hoảng |

Mô hình `rawScore` xem hai đội hoàn toàn tương đương ($20.0\%$), trong khi `score` phân tách rõ ràng xung lực phong độ ($33.33\%$ so với $6.67\%$).

---

## 5. Window Selection & Edge Cases

### 5.1. Thuật toán lựa chọn cửa sổ
```kotlin
val n = dataset.size
val startIndex = maxOf(0, n - windowSize)
val matchesCount = n - startIndex
```

### 5.2. Các trường hợp biên (Edge Cases)

1. **`windowSize <= 0`**:
   - Ném ngoại lệ `IllegalArgumentException` thông qua `require(windowSize > 0)`.
   - Điều kiện kiểm tra được kích hoạt ngay đầu hàm, ném lỗi kể cả khi `dataset` rỗng.
2. **Dataset rỗng (`dataset.isEmpty()`)**:
   - Trả về đối tượng `FormScore` với toàn bộ giá trị số bằng `0` hoặc `0.0`.
   - Không thực hiện phép chia cho 0.
3. **Dataset nhỏ hơn cửa sổ ($N < k$)**:
   - $startIndex = 0$, sử dụng toàn bộ $N$ trận hiện có ($m = N$).
   - Trọng số được gán chuẩn hóa từ $1$ đến $N$.
4. **Dataset lớn hơn cửa sổ ($N > k$)**:
   - $startIndex = N - k$, chỉ duyệt đúng $k$ trận đấu gần nhất ($m = k$).
   - Trọng số được gán từ $1$ đến $k$. Tuyệt đối không dùng global index của dataset.
5. **Cửa sổ đơn (`windowSize = 1`)**:
   - $W \implies 100.0\%$, $D \implies 33.3333333333\%$, $L \implies 0.0\%$.
6. **Các kết quả cực biên**:
   - Toàn thắng (All Wins): `rawScore = 100.0%`, `score = 100.0%`.
   - Toàn hòa (All Draws): `rawScore = 33.3333333333%`, `score = 33.3333333333%`.
   - Toàn thua (All Losses): `rawScore = 0.0%`, `score = 0.0%`.

### 5.3. Ràng buộc trường dữ liệu
- Các trường `wins`, `draws`, `losses`, `totalPoints`, `matchesCount`, `maxPoints` chỉ được đếm và tích lũy trên chính **selected window**, không phản ánh các phần tử bị trượt ra ngoài cửa sổ.
- $\text{maxPoints} = 3.0 \times matchesCount$.
- Mẫu số của weighted score là $3.0 \times \sum w_i$, tuyệt đối không nhầm lẫn với `maxPoints`.

---

## 6. Complexity & Memory

### 6.1. Phân tích độ phức tạp lý thuyết
- **Time Complexity**: $\mathcal{O}(\min(N, k))$. Khi kích thước lịch sử $N \ge k$, thuật toán đạt $\mathcal{O}(k)$ hoàn toàn độc lập với kích thước tổng thể $N$. Vòng lặp chỉ thực hiện đúng $k$ phép tính.
- **Auxiliary Space**: $\mathcal{O}(1)$ tuyệt đối.
  - Thuật toán không sử dụng `takeLast()`, `subList()`, hay `slice()`.
  - Các biến trạng thái trong hàm (`wins`, `draws`, `losses`, `totalPoints`, `weightedPointsSum`, `totalWeights`) đều là biến nguyên thủy trên stack.
  - Kết quả trả về duy nhất một thực thể `FormScore` $\mathcal{O}(1)$.
- **Không phân bổ bộ nhớ trung gian**:
  - Generic Selector được thực thi on-the-fly (`selector(dataset[i])`).
  - Hàm `evaluateInternal` là `inline` giúp compiler tối ưu mã nguồn trực tiếp tại điểm gọi.
- **Tính bất biến (Immutability)**: Danh sách đầu vào `List<T>` là read-only, không bị sao chép, không bị thay đổi trật tự phần tử và không bị mutate.

### 6.2. Kiểm tra hiệu năng (Sanity Check)
Stress test trên tập dữ liệu lịch sử $N = 50.000$ trận với $k = 5$ được thực thi và hoàn tất trong thời gian tiệm cận $0\text{ ms}$, xác thực rằng thuật toán không duyệt toàn bộ mảng khi chỉ cần đánh giá cửa sổ cuối.

---

## 7. Architecture & Integration

### 7.1. Pipeline Kiến trúc Hệ thống

```text
       Data Source / API (Lịch sử thi đấu, Tỷ số)
                           ↓
     DataSyncEngine (:core:data) / Repository
                           ↓
                  Local Data / Room
                           ↓
                   Domain / UseCase
          [Lọc finished matches của team]
          [Sắp xếp chronological bằng Phase 2 Sorting]
          [Chuyển Match -> MatchOutcome dựa vào Home/Away]
                           ↓
               FormEvaluator (:core:algorithm)
                           ↓
             Presentation / Jetpack Compose UI
```

### 7.2. Phân định Ranh giới Trách nhiệm (Separation of Concerns)

1. **Domain Layer**:
   - Chịu trách nhiệm truy vấn danh sách các trận đấu đã hoàn thành (`finished matches`) của đội bóng.
   - Sắp xếp lịch sử thi đấu theo thứ tự thời gian tăng dần (`startTime` ascending) sử dụng thuật toán sắp xếp từ **Phase 2 – Sorting** (`TimSort` / `QuickSort`).
   - Xác định kết quả thắng/hòa/thua của đội bóng mục tiêu dựa vào ngữ cảnh sân nhà / sân khách (`homeScore` vs `awayScore`), chuyển đổi thành `MatchOutcome`.
2. **Algorithm Layer (`:core:algorithm`)**:
   - Hoàn toàn độc lập với Android SDK, Room Database, Retrofit API và Compose UI.
   - Không biết về Domain Model (`Match`, `Team`), không xử lý ngày tháng, timestamp hay múi giờ.
   - Chỉ tiếp nhận `List<MatchOutcome>` hoặc `List<T>` kèm `selector: (T) -> MatchOutcome` và trả về cấu trúc toán học thuần túy `FormScore`.

### 7.3. Mối quan hệ tương hỗ với các Phase khác
- **Phase 2 – Sorting**: Chuẩn bị dữ liệu đầu vào đúng thứ tự thời gian (Chronological Order) trước khi đưa vào `FormEvaluator`.
- **Phase 3 – Statistics**: Domain có thể kết hợp `FormScore` với các chỉ số thống kê bàn thắng, độ phân tán (Variance / Standard Deviation) để đánh giá toàn diện sức mạnh hàng công/thủ.
- **Phase 4 – Trend**: Domain có thể áp dụng thuật toán `SimpleMovingAverageCalculator` (SMA) lên chuỗi điểm phong độ theo thời gian để quan sát xu thế dài hạn của đội bóng qua nhiều vòng đấu.
- **Phase 5**: Đóng vai trò là evaluation primitive, tập trung tính toán phong độ trên cửa sổ trượt, không thực hiện điều phối dữ liệu (orchestration).

---

## 8. Verification

Toàn bộ hệ thống kiểm thử tự động của module `:core:algorithm` đã được thực thi và xác nhận hoàn tất:

```text
> Task :core:algorithm:test

BUILD SUCCESSFUL in 8s
4 actionable tasks: 4 executed
```

### Ma trận Test Suite

| Test Suite / Phase | Số lượng Test Cases | Trạng thái | Ghi chú |
| :--- | :---: | :---: | :--- |
| **Phase 1 – Search** | 12 / 12 | **PASSED** | Đóng băng, không có regression |
| **Phase 2 – Sorting** | 9 / 9 | **PASSED** | Đóng băng, không có regression |
| **Phase 3 – Statistics** | 16 / 16 | **PASSED** | Đóng băng, không có regression |
| **Phase 4 – Trend** | 21 / 21 | **PASSED** | Đóng băng, không có regression |
| **Phase 5 – Evaluation** | 15 / 15 | **PASSED** | Kiểm thử toàn diện 100% hợp đồng |
| **Tổng cộng module** | **73 / 73** | **PASSED** | **Tỷ lệ vượt qua: 100%** |

### Kết quả Code Review
- **Final Verdict**: **PASS**.
- **Issues Found**: 0 issue.
- **Required Fixes**: Không có yêu cầu sửa đổi.
- **Git Diff**: Chỉ chứa thay đổi của Phase 5, file placeholder `Evaluation.kt` đã bị xóa, Phase 1–4 hoàn toàn nguyên vẹn.

---

## 9. Artifacts & Files

Các file mã nguồn và kiểm thử được triển khai trong Phase 5:

1. [`MatchOutcome.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/MatchOutcome.kt): Enum định nghĩa kết quả và điểm quy ước.
2. [`FormScore.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormScore.kt): Data class đóng gói kết quả đánh giá phong độ.
3. [`FormEvaluator.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormEvaluator.kt): Interface contract định nghĩa API đánh giá phong độ.
4. [`LinearDecayFormEvaluator.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/LinearDecayFormEvaluator.kt): Lớp triển khai thuật toán trọng số suy giảm tuyến tính $\mathcal{O}(k)$ và $\mathcal{O}(1)$ bộ nhớ.
5. [`FormScoreAlgorithmsTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/evaluation/FormScoreAlgorithmsTest.kt): Bộ Unit Test gồm 15 ca kiểm thử.
6. `Evaluation.kt`: File placeholder rỗng đã được xóa bỏ khỏi kho mã nguồn.

---

## 10. Project Impact

Việc hoàn thành **Phase 5 – Evaluation: Form Score** mang lại những giá trị then chốt cho TrueLab:
- **Chuẩn hóa đo lường phong độ**: Biến chuỗi kết quả thi đấu rời rạc (W/D/L) thành một chỉ số định lượng liên tục và chuẩn hóa trên thang điểm $[0.0, 100.0]$.
- **Đa chiều hóa góc nhìn phong độ**: Cung cấp song song cả `rawScore` (phản ánh thành tích tổng thể của cửa sổ) và `score` (phản ánh xung lực và đà phong độ gần nhất).
- **Cung cấp Primitive chất lượng cao**: Đóng vai trò là khối xây dựng toán học nền tảng cho các Use Case thuộc Domain Layer, phục vụ hiển thị Form Guide trực quan trên UI hoặc làm tham số đầu vào cho các thuật toán đánh giá tiếp theo (như Elo Rating ở Phase 6).
- **Bảo toàn tính toàn vẹn kiến trúc**: Giữ cho `:core:algorithm` luôn là module Pure Kotlin/JVM thuần túy, không bị pha tạp logic nghiệp vụ hay chi tiết công nghệ hạ tầng.
- **Ranh giới công nghệ minh bạch**: Thuật toán được định vị chính xác là công cụ đánh giá số học (Mathematical Form Evaluation), không tự nhận là mô hình học máy (Machine Learning) hay mô hình dự đoán xác suất (Prediction Model).

---

## 11. Status

**Phase 5 – Evaluation: Form Score** đã được triển khai hoàn chỉnh, vượt qua toàn bộ các bài kiểm thử tự động, hoàn tất kiểm tra mã nguồn (Code Review) và đáp ứng 100% đặc tả kỹ thuật.

**Final Status**: **PASS / READY TO FREEZE**
