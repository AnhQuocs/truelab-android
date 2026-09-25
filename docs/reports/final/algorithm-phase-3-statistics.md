# Algorithm Phase 3 — Descriptive Statistics Specification & Report

Tài liệu đặc tả kỹ thuật, mô hình toán học và báo cáo nghiệm thu hồi tố (Retrospective Report) của **Algorithm Phase 3 — Descriptive Statistics** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Mục tiêu

Xây dựng bộ công cụ thuật toán Thống kê Mô tả (Descriptive Statistics) thuần túy trên nền tảng Pure Kotlin/JVM (tuyệt đối không phụ thuộc vào Android SDK hay các layer bên ngoài) nhằm:
- Tính toán đầy đủ các chỉ số thống kê đo lường xu hướng trung tâm (Central Tendency), độ phân tán (Dispersion), và hình dáng phân phối (Distribution Shape) cho các tập dữ liệu bóng đá (Bàn thắng, Tỷ lệ cược Odds, Điểm phong độ, Điểm số Elo).
- Đảm bảo độ ổn định số học (Numerical Stability), triệt tiêu sai số làm tròn số thực (Floating-point cancellation) thông qua thuật toán Two-Pass với độ lệch chuẩn hóa (Shifted Deviations).
- Cung cấp API tổng hợp `summarize()` tối ưu hóa tính toán 11 chỉ số trong một chu trình xử lý duy nhất.
- Hỗ trợ Generic Selector API cho phép tính toán trực tiếp trên các cấu trúc thực thể nghiệp vụ.
- Bảo toàn tính bất biến (Immutability), không làm biến đổi danh sách đầu vào.

---

## 2. Phạm vi Triển khai

### 2.1 Thành phần Mã nguồn (`:core:algorithm`)
- **Interface Contract**: [`StatisticsCalculator.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/StatisticsCalculator.kt) định nghĩa contract cho mọi hàm tính toán thống kê và các Generic overloads.
- **Mô hình Dữ liệu Tổng hợp**: [`DescriptiveStatistics.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/DescriptiveStatistics.kt) data class đóng gói toàn bộ 11 chỉ số thống kê.
- **Lớp hiện thực cốt lõi**: [`DescriptiveStatisticsCalculator.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/DescriptiveStatisticsCalculator.kt) triển khai thuật toán Two-Pass kết hợp `SortAlgorithm` (QuickSort) cho việc tìm Trung vị (Median).
- **Package**: `dev.anhquocs.truelab.core.algorithm.statistics`.

### 2.2 Thành phần Kiểm thử (`:core:algorithm:test`)
- **Test Suite**: [`StatisticsAlgorithmsTest.kt`](../../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/StatisticsAlgorithmsTest.kt) bao phủ toàn diện các phép tính thống kê, Generic selector, xử lý trường hợp biên toán học và stress test.

---

## 3. Thiết kế / Contract & Mô hình Toán học

### 3.1 Interface Contract
`StatisticsCalculator` định nghĩa 11 phương thức tính toán thống kê trên `List<Double>` cùng các generic overloads nhận selector lambda `(T) -> Double`:

| Phương thức | Ý nghĩa Thống kê | Công thức Toán học | Quy ước Biên / Fallback |
| :--- | :--- | :---: | :--- |
| `mean(dataset)` | Giá trị trung bình số học (Arithmetic Mean) | $\bar{x} = \frac{1}{N} \sum_{i=1}^{N} x_i$ | Trả về `Double.NaN` nếu $N = 0$ |
| `median(dataset)` | Giá trị trung vị (Median) | Phần tử giữa khi sắp xếp | Trả về `Double.NaN` nếu $N = 0$ |
| `min(dataset)` | Giá trị nhỏ nhất (Minimum) | $\min(x_1, \dots, x_N)$ | Trả về `Double.NaN` nếu $N = 0$ |
| `max(dataset)` | Giá trị lớn nhất (Maximum) | $\max(x_1, \dots, x_N)$ | Trả về `Double.NaN` nếu $N = 0$ |
| `range(dataset)` | Khoảng biến thiên (Range) | $\text{Range} = \max - \min$ | Trả về `Double.NaN` nếu $N = 0$ |
| `populationVariance(dataset)` | Phương sai tổng thể (Population Variance) | $\sigma^2 = \frac{1}{N} \sum_{i=1}^{N} (x_i - \bar{x})^2$ | Trả về `Double.NaN` nếu $N = 0$; `0.0` nếu $N = 1$ |
| `sampleVariance(dataset)` | Phương sai mẫu hiệu chỉnh Bessel | $s^2 = \frac{1}{N - 1} \sum_{i=1}^{N} (x_i - \bar{x})^2$ | Trả về `Double.NaN` nếu $N < 2$ |
| `populationStandardDeviation(dataset)` | Độ lệch chuẩn tổng thể | $\sigma = \sqrt{\sigma^2}$ | Trả về `Double.NaN` nếu $\sigma^2 = \text{NaN}$ |
| `sampleStandardDeviation(dataset)` | Độ lệch chuẩn mẫu | $s = \sqrt{s^2}$ | Trả về `Double.NaN` nếu $s^2 = \text{NaN}$ |
| `skewness(dataset)` | Hệ số bất đối xứng mô-men | $\gamma_1 = \frac{\frac{1}{N} \sum (x_i - \bar{x})^3}{\sigma^3}$ | Trả về `Double.NaN` nếu $N = 0$; `0.0` nếu $\sigma = 0$ hoặc $N = 1$ |
| `summarize(dataset)` | Tổng hợp toàn bộ 11 chỉ số | Gộp chung trong 1 chu trình | Trả về đối tượng `DescriptiveStatistics` |

### 3.2 Data Class `DescriptiveStatistics`
```kotlin
data class DescriptiveStatistics(
    val count: Int,
    val mean: Double,
    val median: Double,
    val min: Double,
    val max: Double,
    val range: Double,
    val populationVariance: Double,
    val sampleVariance: Double,
    val populationStandardDeviation: Double,
    val sampleStandardDeviation: Double,
    val skewness: Double
)
```

---

## 4. Implementation

### 4.1 Thuật toán Two-Pass với Shifted Deviations
Thay vì áp dụng công thức một lượt (Single-pass textbook formula $\sum x^2 - \frac{(\sum x)^2}{N}$) dễ dẫn đến hiện tượng triệt tiêu số thực (Catastrophic Cancellation) khi dữ liệu có giá trị lớn nhưng phương sai nhỏ, `DescriptiveStatisticsCalculator` áp dụng kỹ thuật Two-Pass:
1. **Lượt 1 (Pass 1)**: Duyệt một lần tính tổng $\sum x_i$, tìm $\min$, $\max$, tính giá trị trung bình $\bar{x} = \frac{\sum x_i}{N}$.
2. **Lượt 2 (Pass 2)**: Duyệt lần thứ hai tính các mô-men trung tâm từ độ lệch chuẩn hóa $(x_i - \bar{x})$:
   - Mô-men bậc 2: $M_2 = \sum (x_i - \bar{x})^2$.
   - Mô-men bậc 3: $M_3 = \sum (x_i - \bar{x})^3$.
   - Tính toán $\sigma^2 = \frac{M_2}{N}$, $s^2 = \frac{M_2}{N - 1}$, $\gamma_1 = \frac{M_3 / N}{\sigma^3}$.

### 4.2 Tính Trung vị (Median) Không Biến Đổi Dữ liệu
- Sử dụng `sortAlgorithm: SortAlgorithm<Double>` (mặc định là `QuickSort`) để sắp xếp một bản sao của mảng.
- Với $N$ lẻ: $\text{Median} = \text{sorted}[N / 2]$.
- Với $N$ chẵn: $\text{Median} = \frac{\text{sorted}[N / 2 - 1] + \text{sorted}[N / 2]}{2.0}$.
- Đảm bảo danh sách đầu vào hoàn toàn không bị mutate.

### 4.3 Chu trình Tối ưu hóa trong `summarize()`
Hàm `summarize()` kết hợp Pass 1, Pass 2 và duy nhất một lần sắp xếp mảng để trích xuất đầy đủ cả 11 chỉ số thống kê, giảm thiểu chi phí duyệt dữ liệu lặp lại.

---

## 5. Testing

Bộ kiểm thử [`StatisticsAlgorithmsTest.kt`](../../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/statistics/StatisticsAlgorithmsTest.kt) sử dụng đối tượng `TestMatchData(matchId, homeGoals, homeOdds)` với ngưỡng sai số dấu phẩy động $\epsilon = 10^{-9}$.

Tổng số test cases thực tế: **16 / 16 Unit Tests PASS (100%)**.

### Phân loại 16 Test Cases:
1. **Chỉ số cơ bản (Basic Metrics - 8 tests)**:
   - `mean - calculates correct average for standard dataset`: Trung bình danh sách chuẩn.
   - `mean - handles negative and decimal values correctly`: Trung bình với số âm và số thập phân.
   - `median - calculates correct median for odd sized dataset`: Trung vị tập lẻ.
   - `median - calculates correct median for even sized dataset`: Trung vị tập chẵn.
   - `median - does not mutate original input list`: Khẳng định không mutate danh sách gốc.
   - `min max range - calculates correctly`: Kiểm tra min, max và range.
   - `variance and stdDev - calculates population and sample correctly`: Kiểm tra cả phương sai tổng thể và mẫu hiệu chỉnh Bessel.
   - `skewness - symmetric distribution has zero skewness`: Phân phối đối xứng có skewness = 0.
   - `skewness - right skewed distribution has positive skewness`: Phân phối lệch phải có skewness > 0.
   - `skewness - left skewed distribution has negative skewness`: Phân phối lệch trái có skewness < 0.
2. **Container `summarize()` (1 test)**:
   - `summarize - returns identical metrics to individual calls`: Khẳng định 11 trường trong đối tượng tổng hợp khớp hoàn toàn với các hàm tính toán đơn lẻ.
3. **Generic Selector API (1 test)**:
   - `generic selector - computes metrics correctly on domain objects`: Tính toán thống kê trực tiếp trên trường `homeGoals` và `homeOdds` của thực thể trận đấu.
4. **Trường hợp biên (Edge Cases - 3 tests)**:
   - `edge case - empty dataset returns NaN and count 0`: Mảng rỗng trả về `NaN` và `count = 0`.
   - `edge case - single element dataset`: Mảng 1 phần tử ($N = 1$), `variance = 0.0`, `sampleVariance = NaN`, `skewness = 0.0`.
   - `edge case - all identical values`: Mảng chứa các phần tử giống hệt nhau (Zero Variance), `variance = 0.0`, `skewness = 0.0`.
5. **Kiểm thử Sức chịu tải (Stress Test - 1 test)**:
   - `stress test - processes large dataset safely without exception or overflow`: Xử lý tập dữ liệu lớn $N = 50.000$ số thực ngẫu nhiên an toàn, không ngoại lệ, không tràn số.

---

## 6. Độ phức tạp (Complexity Analysis)

| Hàm / Thao tác | Độ phức tạp Thời gian (Time Complexity) | Không gian Phụ trợ (Auxiliary Space) |
| :--- | :---: | :---: |
| `mean()`, `min()`, `max()`, `range()` | $\mathcal{O}(n)$ (1-pass) | $\mathcal{O}(1)$ |
| `populationVariance()`, `sampleVariance()`, `skewness()` | $\mathcal{O}(n)$ (2-pass) | $\mathcal{O}(1)$ |
| `median()` | $\mathcal{O}(n \log n)$ (sắp xếp QuickSort) | $\mathcal{O}(n)$ (bản sao mảng) |
| `summarize()` | $\mathcal{O}(n \log n)$ (Two-pass $\mathcal{O}(n)$ + 1 lần sắp xếp) | $\mathcal{O}(n)$ (bản sao mảng cho median) |

---

## 7. Kết quả & Ứng dụng Tiếp theo

- **Trạng thái**: **HOÀN THÀNH TOÀN DIỆN (FROZEN)**.
- **Tích hợp Domain**: Thuật toán Phase 3 được tích hợp vào Domain D2 thông qua `GetTeamStatisticsUseCase` để tính toán thống kê bàn thắng và tỷ lệ kèo Odds.
- **Tích hợp Presentation**: Được hiển thị trực tiếp trên thẻ thống kê mô tả tại `AnalyticsScreen` (Presentation P1.3).

---

## 8. Ghi chú Lịch sử (Retrospective Note)

Tài liệu này là **Báo cáo Nghiệm thu Hồi tố (Retrospective Final Report)** được tạo bổ sung nhằm chuẩn hóa hệ thống tài liệu dự án TrueLab theo cấu trúc phân tầng `docs/reports/final/`. Mọi thông số kỹ thuật và test case trong tài liệu này đều được đối chiếu và xác thực trực tiếp từ mã nguồn và test suite thực tế của dự án.
