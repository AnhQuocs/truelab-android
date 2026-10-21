# Engineering Issue History: Phase 4 – Trend (Simple Moving Average)

Tài liệu này ghi lại lịch sử kỹ thuật (Engineering History) trong quá trình phát triển Phase 4 – Trend thuộc module `:core:algorithm`. Tài liệu phản ánh chi tiết quy trình: **Initial Implementation → Code Review → Issues Found → Root Cause Analysis → Architectural Fix → Verification**.

---

## 1. Context & Initial Implementation

Trong lần triển khai đầu tiên của `SimpleMovingAverageCalculator`, thuật toán áp dụng kỹ thuật Cửa sổ trượt (Sliding Window / Rolling Sum) để đạt độ phức tạp thời gian $\mathcal{O}(n)$:
- Cửa sổ đầu tiên tính tổng $K$ phần tử: $S_0 = \sum_{i=0}^{K-1} x_i$.
- Các bước trượt tiếp theo ($i = K \dots N - 1$):
  $$S_t = S_{t-1} + x_i - x_{i-K}$$
- Nhằm xử lý trường hợp cửa sổ chứa `NaN` và phục hồi lại giá trị số thực sạch khi `NaN` rời khỏi cửa sổ, implementation ban đầu đã sử dụng đoạn logic:
  ```kotlin
  val entering = dataset[i]
  val leaving = dataset[i - windowSize]
  if (currentSum.isNaN() || leaving.isNaN()) {
      var windowSum = 0.0
      for (j in (i - windowSize + 1)..i) {
          windowSum += dataset[j]
      }
      currentSum = windowSum
  } else {
      currentSum += entering - leaving
  }
  result.add(currentSum / windowSize)
  ```
Implementation này đã vượt qua 17 test cases ban đầu, nhưng trong quá trình Code Review chuyên sâu về số học IEEE-754 và phân tích biên độ phức tạp thuật toán, hai vấn đề kỹ thuật nghiêm trọng đã được phát hiện.

---

## 2. Issue 1: Infinity Sliding Out of Window Causes Incorrect NaN

### 2.1. Hiện tượng (Symptom)
Khi một tập dữ liệu chuỗi thời gian có chứa giá trị vô hạn (`Double.POSITIVE_INFINITY` hoặc `Double.NEGATIVE_INFINITY`), cửa sổ chứa phần tử này trả về `+Infinity` hoặc `-Infinity` (đúng). Tuy nhiên, khi phần tử vô hạn đó trượt ra khỏi cửa sổ và cửa sổ tiếp theo chỉ còn các số thực hữu hạn hoàn toàn bình thường, kết quả của cửa sổ mới này lại bị tính thành `Double.NaN` thay vì giá trị trung bình số học chính xác.

Ví dụ:
- Dataset: `[Double.POSITIVE_INFINITY, 2.0, 4.0, 6.0]`, `windowSize = 2`.
- Window 0: `[+Infinity, 2.0]` $\rightarrow$ `+Infinity`.
- Window 1: `[2.0, 4.0]` $\rightarrow$ Kết quả thực tế bị thành `Double.NaN` (Kỳ vọng: `3.0`).

### 2.2. Nguyên nhân cốt lõi (Root Cause)
Theo chuẩn số thực IEEE-754:
$$\infty - \infty = \text{NaN}$$
Trong implementation ban đầu:
- Điều kiện kiểm tra chỉ là `if (currentSum.isNaN() || leaving.isNaN())`.
- Khi `leaving = Double.POSITIVE_INFINITY`:
  - `leaving.isNaN()` trả về `false` (vì `Infinity` không phải là `NaN`).
  - `currentSum` ở bước trước đang là `+Infinity`, nên `currentSum.isNaN()` cũng trả về `false`.
- Do đó, luồng thực thi bị rơi vào nhánh `else`:
  $$\text{currentSum} \mathrel{+}= \text{entering} - \text{leaving} \implies \infty + 4.0 - \infty = \text{NaN}$$
- Giá trị tích lũy `currentSum` bị biến thành `NaN` do phép toán không xác định $\infty - \infty$, làm hỏng toàn bộ kết quả của cửa sổ sạch tiếp theo.

### 2.3. Tác động (Impact)
- Sai lệch tính toán toán học: Dữ liệu hữu hạn hợp lệ bị chuyển thành `NaN`.
- Vi phạm tính độc lập và khả năng tự phục hồi của cửa sổ trượt.

---

## 3. Issue 2: Dense NaN Degrades Worst-Case Complexity to $\mathcal{O}(n \cdot k)$

### 3.1. Hiện tượng (Symptom)
Khi tập dữ liệu đầu vào chứa nhiều giá trị `NaN` liên tiếp hoặc toàn bộ là `NaN` (`[NaN, NaN, NaN, ...]`), thời gian thực thi của thuật toán tăng phi mã tỷ lệ thuận với $N \times K$, thay vì duy trì tuyến tính $\mathcal{O}(n)$.

### 3.2. Nguyên nhân cốt lõi (Root Cause)
Trong điều kiện:
```kotlin
if (currentSum.isNaN() || leaving.isNaN()) {
    var windowSum = 0.0
    for (j in (i - windowSize + 1)..i) {
        windowSum += dataset[j]
    }
    currentSum = windowSum
}
```
- Khi cửa sổ chứa `NaN`, `currentSum` có giá trị là `NaN`.
- Do đó, ở bước tiếp theo, biểu thức `currentSum.isNaN()` luôn đánh giá là `true`.
- Vòng lặp bên trong `for (j in (i - windowSize + 1)..i)` với kích thước $K$ phần tử sẽ bị kích hoạt lặp đi lặp lại ở **mọi bước trượt** $i = K \dots N - 1$.
- Dù cửa sổ hiện tại chắc chắn vẫn chứa `NaN` (do phần tử rời khỏi không phải `NaN`), thuật toán vẫn duyệt lại toàn bộ $K$ phần tử chỉ để nhận về kết quả `NaN`.
- Tổng số thao tác trong trường hợp này:
  $$\text{Total Operations} = (N - K) \times K = \mathcal{O}(n \cdot k)$$
- Nếu $K = \frac{N}{2}$, độ phức tạp suy biến thành $\mathcal{O}(n^2)$, phá vỡ cam kết độ phức tạp $\mathcal{O}(n)$ trong Algorithm Portfolio của dự án.

### 3.3. Tác động (Impact)
- Vi phạm cam kết hiệu năng $\mathcal{O}(n)$ trong Worst-case.
- Nguy cơ tắc nghẽn CPU khi xử lý chuỗi dữ liệu lớn có nhiều khoảng trống (missing data / NaN gaps).

---

## 4. Code Review Findings

Báo cáo Code Review chính thức của Phase 4 đã đánh giá trạng thái **PASS WITH ISSUES** và đưa ra 2 chỉ thị kỹ thuật bắt buộc:
1. **Triệt tiêu phép toán $\infty - \infty$**: Tuyệt đối không để `+Infinity` hoặc `-Infinity` tham gia vào phép cộng trừ của biến tích lũy rolling sum.
2. **Loại bỏ vòng lặp tính lại $\mathcal{O}(k)$**: Không quét lại $K$ phần tử chỉ để xác định trạng thái `NaN`. Cần cơ chế quản lý trạng thái $\mathcal{O}(1)$ thuần túy.

---

## 5. Architectural Fix (State-based $\mathcal{O}(1)$ Management)

Thay vì cố gắng "sửa vá" vòng lặp phục hồi, một giải pháp kiến trúc số học mới được thiết kế: **Tách biệt hoàn toàn tổng số thực hữu hạn khỏi các giá trị đặc biệt không hữu hạn (Non-finite Values)**.

### 5.1. Thiết kế 4 biến trạng thái $\mathcal{O}(1)$
```kotlin
var finiteSum = 0.0
var nanCount = 0
var posInfCount = 0
var negInfCount = 0
```
- `finiteSum`: Chỉ tích lũy các số thực hữu hạn (`isFinite()`). Tuyệt đối không bao giờ cộng/trừ `NaN` hay `Infinity`.
- `nanCount`: Đếm số lượng phần tử `NaN` đang nằm trong cửa sổ hiện tại.
- `posInfCount`: Đếm số lượng phần tử `Double.POSITIVE_INFINITY` trong cửa sổ hiện tại.
- `negInfCount`: Đếm số lượng phần tử `Double.NEGATIVE_INFINITY` trong cửa sổ hiện tại.

### 5.2. Cập nhật cửa sổ $\mathcal{O}(1)$ không vòng lặp
Khi một phần tử $v$ gia nhập (`addValue`) hoặc rời khỏi (`removeValue`) cửa sổ:
```kotlin
fun addValue(v: Double) {
    when {
        v.isNaN() -> nanCount++
        v == Double.POSITIVE_INFINITY -> posInfCount++
        v == Double.NEGATIVE_INFINITY -> negInfCount++
        else -> finiteSum += v
    }
}

fun removeValue(v: Double) {
    when {
        v.isNaN() -> nanCount--
        v == Double.POSITIVE_INFINITY -> posInfCount--
        v == Double.NEGATIVE_INFINITY -> negInfCount--
        else -> finiteSum -= v
    }
}
```

### 5.3. Trích xuất giá trị trung bình $\mathcal{O}(1)$ theo IEEE-754
```kotlin
fun currentAverage(): Double = when {
    nanCount > 0 -> Double.NaN
    posInfCount > 0 && negInfCount > 0 -> Double.NaN // +Inf + (-Inf) = NaN
    posInfCount > 0 -> Double.POSITIVE_INFINITY
    negInfCount > 0 -> Double.NEGATIVE_INFINITY
    else -> finiteSum / windowSize
}
```

### 5.4. Đánh giá giải pháp
- **Xử lý triệt để Issue 1**: Khi `Infinity` rời khỏi cửa sổ, `posInfCount` giảm về 0 trong $\mathcal{O}(1)$. `finiteSum` vốn không hề bị nhiễm bẩn bởi `Infinity`, do đó giá trị trung bình số thực sạch được trả về ngay lập tức.
- **Xử lý triệt để Issue 2**: Không có bất kỳ vòng lặp $\mathcal{O}(k)$ nào tồn tại. Ở mọi bước trượt, thuật toán chỉ thực hiện đúng 1 hàm `removeValue` ($\mathcal{O}(1)$) và 1 hàm `addValue` ($\mathcal{O}(1)$). Worst-case duy trì nghiêm ngặt $\mathcal{O}(n)$.

---

## 6. Verification Sau Fix

### 6.1. Bổ sung Test Cases chuyên biệt
Đã bổ sung 4 test cases trực tiếp kiểm định các biên số học vừa fix trong [MovingAverageAlgorithmsTest.kt](../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/trend/MovingAverageAlgorithmsTest.kt):
1. `numerical - POSITIVE_INFINITY sliding out of window recovers finite average correctly`: Xác minh phục hồi chính xác khi `+Infinity` trượt ra.
2. `numerical - NEGATIVE_INFINITY sliding out of window recovers finite average correctly`: Xác minh phục hồi chính xác khi `-Infinity` trượt ra.
3. `numerical - window containing both positive and negative infinity yields NaN`: Xác minh chuẩn IEEE-754 khi cả $+\infty$ và $-\infty$ cùng xuất hiện trong một cửa sổ.
4. `numerical - dense and all NaN dataset processes in linear time without O(k) recomputation`: Kiểm tra mảng 1.000 phần tử toàn `NaN` với `windowSize = 50`.

### 6.2. Kết quả kiểm thử toàn diện
Chạy `./gradlew.bat :core:algorithm:test --rerun-tasks`:
- **Phase 4 (MovingAverageAlgorithmsTest)**: **21/21 passed (100%)**.
- **Phase 1 (SearchingAlgorithmsTest)**: **12/12 passed (100%)**.
- **Phase 2 (SortingAlgorithmsTest)**: **9/9 passed (100%)**.
- **Phase 3 (StatisticsAlgorithmsTest)**: **16/16 passed (100%)**.
- **Toàn bộ Algorithm Module**: **58/58 tests passed (100%)**.
- **Stress Test**: $N = 50.000$, `windowSize = 100` thực thi hoàn tất trong **16 ms**.

### 6.3. Chỉ số kỹ thuật sau xác thực
- **Time Complexity**: Tuyến tính $\mathcal{O}(n)$ trong cả Best, Average và Worst-Case.
- **Auxiliary Space**: $\mathcal{O}(1)$ (chỉ dùng 4 biến đếm scalar).
- **IEEE-754 Compliance**: 100% tuân thủ chuẩn số thực quốc tế.