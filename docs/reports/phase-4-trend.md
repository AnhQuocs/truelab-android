# Phase 4 – Trend: Simple Moving Average (SMA) Specification & Report

Tài liệu đặc tả kiến trúc, thuật toán và báo cáo kết quả triển khai chính thức của **Phase 4 – Trend** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Objective

Triển khai thuật toán **Đường trung bình động (Moving Average)** nhằm:
- Làm mịn chuỗi biến động tỷ lệ cược (Odds Fluctuations) theo thời gian.
- Lọc bỏ nhiễu thị trường ngắn hạn (Market Noise).
- Xác định xu hướng dịch chuyển chủ đạo của dòng tiền thị trường (Odds Drift / Smart Money Flow).
- Đảm bảo độ phức tạp thời gian đạt $\mathcal{O}(n)$ tuyệt đối và độ phức tạp không gian bổ sung $\mathcal{O}(1)$.

---

## 2. Scope

### 2.1. In-Scope
- Interface contract: [MovingAverageCalculator.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/trend/MovingAverageCalculator.kt).
- Lớp hiện thực: [SimpleMovingAverageCalculator.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/trend/SimpleMovingAverageCalculator.kt).
- Thuật toán cốt lõi: **Simple Moving Average (SMA)** với kỹ thuật Rolling Sum $\mathcal{O}(n)$.
- Generic Selector API: Hỗ trợ tính toán trực tiếp trên các entity tùy biến thông qua selector lambda `(T) -> Double`.
- Xử lý biên (Edge cases) và tuân thủ chuẩn số thực IEEE-754 (quản lý trạng thái `NaN`, `+Infinity`, `-Infinity`).
- Bộ Unit Test toàn diện: [MovingAverageAlgorithmsTest.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/trend/MovingAverageAlgorithmsTest.kt).

### 2.2. Non-Scope
- **Exponential Moving Average (EMA)**: Dành cho extension trong các phase nâng cao tiếp theo.
- **Phase 5 – Evaluation (Form Score)** và **Phase 6 – Rating (Elo Rating System)**.
- **Prediction / Machine Learning**: Không bao gồm trong phạm vi phase này.
- **UI / Data Visualization**: Không kéo Compose Canvas, MPAndroidChart hay chart model vào algorithm layer.
- **Data Layer / Persistence**: Không phụ thuộc Room Database, Retrofit API hay Data Sync Engine.

---

## 3. SMA Contract

Interface contract được đóng gói tại package `dev.anhquocs.truelab.core.algorithm.trend`:

```kotlin
interface MovingAverageCalculator {

    /**
     * Tính toán Simple Moving Average (SMA) trên danh sách số thực với kích thước cửa sổ [windowSize].
     *
     * @param dataset Danh sách các giá trị số thực theo thứ tự thời gian.
     * @param windowSize Kích thước cửa sổ trượt (phải > 0).
     * @return Danh sách các giá trị trung bình động hợp lệ (kích thước N - windowSize + 1 khi N >= windowSize).
     * @throws IllegalArgumentException nếu [windowSize] <= 0.
     */
    fun calculate(dataset: List<Double>, windowSize: Int): List<Double>

    /**
     * Overload hỗ trợ Generic Selector cho phép tính toán trên danh sách đối tượng tùy biến
     * mà không làm biến đổi danh sách gốc.
     */
    fun <T> calculate(
        dataset: List<T>,
        windowSize: Int,
        selector: (T) -> Double
    ): List<Double> = calculate(dataset.map(selector), windowSize)
}
```

---

## 4. Output Alignment: Valid Windows Only ($N - K + 1$)

Thuật toán áp dụng chính sách **Valid Windows Only**:
- Khi tập dữ liệu có $N$ phần tử và kích thước cửa sổ là $K$ (với $N \ge K$):
  $$\text{Output Size} = N - K + 1$$
- Phần tử đầu tiên trong mảng kết quả ứng với cửa sổ hoàn chỉnh đầu tiên $[0 \dots K - 1]$.
- Không sinh ra các giá trị `Double.NaN` giả lập ở đầu mảng (prefix padding) để lấp đầy $N$ phần tử.
- Khi $K > N$ hoặc tập dữ liệu rỗng: Trả về danh sách rỗng `emptyList<Double>()`.

---

## 5. Mathematical Definition

Với tập dữ liệu chuỗi thời gian $X = [x_0, x_1, \dots, x_{n-1}]$ và kích thước cửa sổ trượt $K \in \mathbb{N}^+$:
Giá trị Simple Moving Average tại mỗi thời điểm cửa sổ $t$ ($t \ge K - 1$) được định nghĩa:

$$SMA_t = \frac{1}{K} \sum_{i=0}^{K-1} x_{t-i}$$

Thuật toán khai triển qua kỹ thuật đệ quy cửa sổ trượt (Rolling Sum):
- Cửa sổ đầu tiên ($t = K - 1$):
  $$S_{K-1} = \sum_{i=0}^{K-1} x_i \implies SMA_{K-1} = \frac{S_{K-1}}{K}$$
- Các cửa sổ tiếp theo ($t = K, \dots, N - 1$):
  $$S_t = S_{t-1} + x_t - x_{t-K} \implies SMA_t = \frac{S_t}{K}$$

---

## 6. Implementation Architecture

Triển khai tại [SimpleMovingAverageCalculator.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/trend/SimpleMovingAverageCalculator.kt) sử dụng mô hình quản lý trạng thái phân tách số thực hữu hạn và số thực đặc biệt theo $\mathcal{O}(1)$:

```kotlin
var finiteSum = 0.0
var nanCount = 0
var posInfCount = 0
var negInfCount = 0
```

1. **Khởi tạo cửa sổ đầu tiên**: Quét $K$ phần tử đầu tiên qua hàm `addValue()`.
2. **Vòng lặp trượt cửa sổ**:
   - Ở mỗi bước trượt từ $i = K$ đến $N - 1$:
     - Gọi `removeValue(dataset[i - windowSize])` ($\mathcal{O}(1)$).
     - Gọi `addValue(dataset[i])` ($\mathcal{O}(1)$).
     - Đưa `currentAverage()` vào mảng kết quả ($\mathcal{O}(1)$).
3. **Không chứa bất kỳ vòng lặp lồng $\mathcal{O}(k)$ nào**.

---

## 7. Complexity Analysis

| Chỉ số | Độ phức tạp | Ghi chú |
| :--- | :---: | :--- |
| **Time (Best / Average / Worst)** | $\mathcal{O}(n)$ | Khởi tạo mất $K$ bước; $N - K$ bước trượt tiếp theo mỗi bước mất đúng $\mathcal{O}(1)$. Không bị thoái hóa khi dữ liệu dị thường. |
| **Auxiliary Space** | $\mathcal{O}(1)$ | Chỉ sử dụng 4 biến đếm và tích lũy scalar (`finiteSum`, `nanCount`, `posInfCount`, `negInfCount`). |
| **Output Space** | $\mathcal{O}(n)$ | Mảng kết quả `ArrayList<Double>(N - K + 1)`. |

---

## 8. Edge Cases Policy

| Thứ tự | Trường hợp kiểm tra | Điều kiện | Hành vi thuật toán |
| :---: | :--- | :--- | :--- |
| **1** | Cửa sổ không hợp lệ | `windowSize <= 0` | Ném `IllegalArgumentException` ngay lập tức (kể cả khi `dataset` rỗng). |
| **2** | Tập dữ liệu rỗng | `dataset.isEmpty()` và `windowSize > 0` | Trả về `emptyList<Double>()`. |
| **3** | Cửa sổ lớn hơn dữ liệu | `windowSize > dataset.size` | Trả về `emptyList<Double>()`. |
| **4** | Cửa sổ bằng 1 | `windowSize == 1` | Trả về bản sao mới của `dataset` (`ArrayList(dataset)`). |
| **5** | Cửa sổ bằng kích thước dữ liệu | `windowSize == dataset.size` | Trả về danh sách chứa 1 phần tử là trung bình cộng toàn bộ dữ liệu. |
| **6** | Một phần tử duy nhất | `dataset.size == 1` | Trả về `listOf(x)` nếu `windowSize == 1`; trả về `emptyList()` nếu `windowSize > 1`. |
| **7** | Dữ liệu đồng nhất | Mọi phần tử đều bằng hằng số $C$ | Trả về danh sách chứa toàn bộ giá trị $C$. |
| **8** | Số âm, số thập phân | Các giá trị Odds, Handicap | Xử lý chính xác với sai số số thực $\epsilon < 10^{-9}$. |

---

## 9. Generic Selector API

Hỗ trợ xử lý trực tiếp trên các Entity nghiệp vụ thông qua hàm mở rộng trên interface:
```kotlin
fun <T> calculate(
    dataset: List<T>,
    windowSize: Int,
    selector: (T) -> Double
): List<Double> = calculate(dataset.map(selector), windowSize)
```
- Ví dụ kiểm thử thực tế với đối tượng biến động Odds:
  ```kotlin
  data class TestOddsPoint(val matchId: String, val timestamp: Long, val odds: Double)
  val smoothedOdds = calculator.calculate(oddsHistory, windowSize = 3) { it.odds }
  ```
- **Ưu điểm**: Tầng Domain/Presentation không cần chuyển đổi mảng thủ công trước khi gọi thuật toán.

---

## 10. Immutability

- Thuật toán chỉ nhận `List<Double>` và `List<T>` (read-only interfaces trong Kotlin).
- Không có bất kỳ thao tác in-place mutation hay hoán đổi vị trí nào trên dataset đầu vào.
- Khi `windowSize == 1`, thuật toán trả về `ArrayList(dataset)` thay vì trả về chính tham chiếu đầu vào, đảm bảo tính đóng gói độc lập.
- Được kiểm chứng bằng unit test `immutability - does not mutate original input list`.

---

## 11. Non-finite Value Handling (IEEE-754 Compliance)

1. **`Double.NaN`**:
   - Khi `nanCount > 0`, cửa sổ bị nhiễm `NaN` $\rightarrow$ Trả về `Double.NaN`.
   - Khi toàn bộ `NaN` trượt ra khỏi cửa sổ (`nanCount == 0`), thuật toán tự động phục hồi giá trị trung bình hữu hạn sạch tức thì mà không cần quét lại.
2. **`Double.POSITIVE_INFINITY` / `Double.NEGATIVE_INFINITY`**:
   - Cửa sổ chứa duy nhất `+Infinity` $\rightarrow$ Trả về `+Infinity`.
   - Cửa sổ chứa duy nhất `-Infinity` $\rightarrow$ Trả về `-Infinity`.
   - Cửa sổ chứa đồng thời cả `+Infinity` và `-Infinity` $\rightarrow$ Trả về `Double.NaN` (do $+\infty + (-\infty) = \text{NaN}$).
   - `Infinity` không tham gia vào `finiteSum`, loại bỏ hoàn toàn hiện tượng triệt tiêu không xác định $\infty - \infty$.

---

## 12. Design Decisions & Rationale

### 12.1. Vì sao chọn SMA thay vì EMA?
- **Tại sao**:
  - SMA là định nghĩa cơ bản, trực quan và chuẩn hóa nhất của Moving Average, phản ánh trung thực yêu cầu thời gian $\mathcal{O}(n)$ trong Algorithm Portfolio của dự án.
  - EMA tuy giảm độ trễ (lag) tốt hơn cho phân tích tài chính/odds, nhưng phụ thuộc vào tham số làm mịn $\alpha = \frac{2}{K + 1}$ và yêu cầu xác định giá trị khởi tạo ($EMA_0$). Việc đưa EMA vào Phase 4 khi chưa có yêu cầu nghiệp vụ cụ thể sẽ gây mở rộng scope không cần thiết (Scope Creep).
  - EMA được giữ lại dưới dạng extension độc lập trong tương lai nếu tầng Domain/Prediction thực sự cần.

### 12.2. Vì sao chọn Valid Windows Only ($N - K + 1$)?
- **Tại sao**:
  - `:core:algorithm` là tầng tính toán thuần túy (Pure Mathematics). Về mặt toán học, trung bình động của một cửa sổ $K$ phần tử chỉ tồn tại khi có đủ $K$ phần tử.
  - Việc tự ý padding $K - 1$ giá trị `Double.NaN` vào đầu mảng sẽ làm bẩn tập dữ liệu kết quả và buộc các tầng sử dụng toán học khác phải liên tục kiểm tra `isNaN()`.
  - Giữ output sạch giúp thuật toán độc lập tuyệt đối với cách hiển thị của tầng giao diện.

### 12.3. Vì sao algorithm layer không chứa Timestamp / ChartPoint?
- **Tại sao**:
  - Module `:core:algorithm` có mục tiêu trở thành thư viện thuật toán thuần túy (Pure Kotlin/JVM, zero-dependency).
  - Khái niệm thời gian (`Timestamp`, `Instant`, `LocalDateTime`) và mô hình hiển thị (`ChartPoint`, `TrendPoint`) thuộc về tầng Domain và Presentation.
  - Trách nhiệm của tầng trên (Use Case / Domain) là lấy dữ liệu từ Repository, sắp xếp theo thời gian (sử dụng Sorting từ Phase 2 nếu cần), đưa chuỗi số vào Moving Average, sau đó ghép kết quả $SMA[i]$ với $Timestamp[i + K - 1]$ để tạo model hiển thị.
  - Cách phân tách này tuân thủ triệt để nguyên lý Single Responsibility và Clean Architecture.

### 12.4. Vì sao dùng `finiteSum + nanCount + posInfCount + negInfCount`?
- **Tại sao**:
  - Trong phép toán số thực, `NaN - NaN = NaN` và `Infinity - Infinity = NaN`. Do đó, rolling sum truyền thống không thể tự phục hồi nếu các giá trị này tham gia vào biến tích lũy.
  - Phương pháp kiểm tra và quét lại cửa sổ $\mathcal{O}(k)$ khi gặp `NaN` sẽ phá vỡ cam kết độ phức tạp nếu dữ liệu có nhiều `NaN` liên tiếp.
  - Bằng cách sử dụng 4 biến trạng thái $\mathcal{O}(1)$, ta cô lập hoàn toàn các giá trị dị thường, bảo vệ biến `finiteSum` luôn sạch sẽ và xác định kết quả cửa sổ trong $\mathcal{O}(1)$ thông qua pattern matching.

### 12.5. Vì sao thiết kế này đảm bảo $\mathcal{O}(n)$ trong cả Worst-Case?
- **Tại sao**:
  - Không có bất kỳ nhánh rẽ nào chứa vòng lặp `for` hay `while` bên trong bước trượt của cửa sổ.
  - Mỗi bước trượt chỉ bao gồm: kiểm tra điều kiện scalar, tăng/giảm biến đếm nguyên, cộng/trừ số thực.
  - Dù dữ liệu là $50.000$ số thực bình thường hay $50.000$ giá trị `NaN` liên tiếp, số phép toán ở mỗi bước trượt luôn là hằng số $\mathcal{O}(1)$, đảm bảo thời gian chạy toàn chu kỳ là $\mathcal{O}(n)$ bất biến.

---

## 13. Implementation Review & Refinement

Tóm tắt toàn bộ vòng đời phát triển (Development Lifecycle) của Phase 4 theo trình tự kỹ thuật:
$$\text{Initial Implementation} \longrightarrow \text{Code Review} \longrightarrow \text{Issues Found} \longrightarrow \text{Refinement / Fix} \longrightarrow \text{Verification}$$

### 13.1. Initial Implementation
- Triển khai Simple Moving Average (SMA) sử dụng kỹ thuật Sliding Window Rolling Sum với độ phức tạp danh định $\mathcal{O}(n)$.
- Áp dụng cơ chế tái tính toán tổng cục bộ vòng lặp $\mathcal{O}(k)$ khi cửa sổ gặp hoặc rời khỏi giá trị `NaN` để phục hồi dữ liệu số thực.

### 13.2. Code Review
- Đánh giá tổng thể: Contract, tính đúng đắn toán học cơ bản, tính bất biến (Immutability), Generic Selector và ranh giới kiến trúc Clean Architecture đều đạt yêu cầu.
- Quá trình rà soát số học chuyên sâu phát hiện **2 vấn đề kỹ thuật**:
  1. *Infinity Handling*: Khi `Double.POSITIVE_INFINITY` hoặc `NEGATIVE_INFINITY` trượt ra khỏi cửa sổ, biểu thức `currentSum += entering - leaving` dẫn tới $\infty - \infty = \text{NaN}$, làm cửa sổ sạch tiếp theo bị nhiễm `NaN`.
  2. *Dense NaN Worst-case*: Khi dữ liệu dày đặc hoặc toàn bộ là `NaN`, điều kiện `currentSum.isNaN()` liên tục kích hoạt vòng lặp quét lại $K$ phần tử ở mọi bước trượt, đẩy độ phức tạp worst-case lên $\mathcal{O}(n \cdot k)$ (nguy cơ thoái hóa $\mathcal{O}(n^2)$ khi $K = N/2$).

### 13.3. Refinement
- Loại bỏ hoàn toàn cơ chế quét lại lặp lồng $\mathcal{O}(k)$.
- Chuyển sang mô hình quản lý trạng thái phân tách số thực hữu hạn và số thực đặc biệt bằng 4 biến scalar $\mathcal{O}(1)$:
  `finiteSum`, `nanCount`, `posInfCount`, `negInfCount`.
- Mọi bước trượt cửa sổ (`addValue`, `removeValue`, `currentAverage`) đều thực thi trong thời gian $\mathcal{O}(1)$ tuyệt đối.

### 13.4. Verification
- **Test suite Phase 4**: 21/21 tests passed (100%).
- **Toàn bộ Test suite dự án**: 58/58 tests passed (Zero Regression).
- **Độ phức tạp sau sửa**: Đảm bảo $\mathcal{O}(n)$ trong cả Best, Average và Worst-case; Auxiliary Space giữ vững $\mathcal{O}(1)$.
- **Bổ sung kiểm thử**: Thêm 4 test cases chuyên biệt cho `+Infinity`, `-Infinity`, hỗn hợp vô hạn và dữ liệu dày đặc `NaN`.

> Detailed engineering history is documented in `docs/issues/phase-4-trend.md`.

---

## 14. Testing & Verification

Suite kiểm thử toàn diện được tổ chức tại [MovingAverageAlgorithmsTest.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/trend/MovingAverageAlgorithmsTest.kt):

- **Tổng số test cases**: **21 tests** (100% passed).
- **Phân nhóm kiểm thử**:
  1. *Basic Correctness*: Cửa sổ kích thước lẻ (3), chẵn (2, 4), cửa sổ bằng 1, cửa sổ bằng $N$.
  2. *Edge Cases*: Cửa sổ $\le 0$ ném ngoại lệ, tập dữ liệu rỗng, cửa sổ lớn hơn $N$, một phần tử duy nhất.
  3. *Numerical Precision*: Tỷ lệ cược thập phân, số âm, mảng hằng số.
  4. *IEEE-754 & Recovery*: `NaN` trượt khỏi cửa sổ, `+Infinity` trượt khỏi cửa sổ, `-Infinity` trượt khỏi cửa sổ, cửa sổ chứa đồng thời cả `+Inf` và `-Inf`.
  5. *Dense NaN Complexity*: Kiểm thử mảng 1.000 phần tử toàn `NaN` hoàn thành trong thời gian tuyến tính $\mathcal{O}(n)$.
  6. *Immutability*: Xác minh mảng gốc không bị biến đổi.
  7. *Generic Selector*: Kiểm thử trên model dữ liệu Odds lịch sử `TestOddsPoint`.
  8. *Stress Test*: Tập dữ liệu $N = 50.000$ phần tử, `windowSize = 100` thực thi mượt mà trong **16 ms**.
- **Hệ thống hồi quy (Zero Regression)**: Toàn bộ **58/58 tests** của cả 4 Phase (Searching, Sorting, Statistics, Trend) đều PASS.

---

## 15. Project Impact

### 15.1. Giải quyết bài toán cốt lõi của TrueLab
Trong phân tích thể thao chuyên sâu, tỷ lệ cược (Odds) là dữ liệu chuỗi thời gian (Time-Series) phản ánh đánh giá liên tục của thị trường về xác suất xảy ra của các biến cố trận đấu. Tuy nhiên, dữ liệu Odds thô luôn tồn tại các biến động vi mô ngắn hạn (Micro-fluctuations) do các giao dịch nhỏ lẻ, dòng tiền ngắn hạn hoặc sự điều chỉnh biên độ tạm thời từ nhà cung cấp Odds (Market Noise).
- **Vấn đề**: Nếu người dùng hoặc mô hình phân tích chỉ nhìn vào từng điểm Odds thô tức thời, sẽ rất dễ đưa ra nhận định sai lầm về xu thế thị trường.
- **Giải pháp**: Thuật toán SMA trong `:core:algorithm` đóng vai trò là một **Numerical Primitive** thiết yếu, làm mịn đường cong biến động Odds, triệt tiêu nhiễu tức thời để làm lộ rõ xu hướng dịch chuyển chủ đạo (Odds Drift) và hành vi của dòng tiền lớn (Smart Money Flow).
- **Tiềm năng tích hợp các Phase sau**: Là một nền tảng tính toán số học độc lập, kết quả làm mịn của SMA có khả năng trở thành đặc trưng đầu vào (Input Feature) giá trị cho các bài toán phân tích phong độ (Form Score ở Phase 5) hoặc mô hình dự đoán (Prediction ở các Phase sau), mà không tạo ra bất kỳ sự ràng buộc cứng nào về kiến trúc.

### 15.2. Kiến trúc luồng dữ liệu thực tế (Separation of Concerns)
TrueLab duy trì ranh giới kiến trúc Clean Architecture đa module nghiêm ngặt, trong đó `DataSyncEngine` ở `:core:data` đảm nhiệm toàn bộ việc đồng bộ dữ liệu thời gian thực:

```text
Data Source / API (Odds từ nhà cung cấp, Lịch thi đấu, Tỷ số)
    │
    ▼
DataSyncEngine (:core:data)  [Worker / Sync Engine điều phối tải dữ liệu]
    │
    ▼
Repository / Local Data (:core:data)  [Room Database lưu trữ Odds lịch sử]
    │
    ▼
Domain / UseCase (:core:domain)  [Orchestrator: Trích xuất chuỗi Odds, sắp xếp thời gian]
    │
    ▼
SMA (:core:algorithm)  [Numerical Primitive: Tính toán trung bình trượt O(n)]
    │
    ▼
Trend Result (:core:domain)  [Ghép SMA[i] với Timestamp[i + K - 1] thành TrendPoint]
    │
    ▼
Presentation / Chart (:app / :core:ui)  [Hiển thị biểu đồ Compose trực quan]
```

- **`:core:data`**: Chịu trách nhiệm hoàn toàn về I/O, mạng và lưu trữ: `DataSyncEngine` liên tục fetch/sync dữ liệu biến động Odds từ nguồn API ngoài và lưu trữ an toàn vào Room Database.
- **`:core:domain`**: Đóng vai trò nhạc trưởng (Orchestration). UseCase lấy danh sách Odds lịch sử từ Repository, chuẩn bị chuỗi số thực theo đúng thứ tự thời gian, gọi `MovingAverageCalculator` và kết hợp kết quả trả về với mốc thời gian để sinh ra các model phục vụ vẽ biểu đồ (như `TrendPoint`).
- **`:core:algorithm`**: Tách biệt tuyệt đối với thế giới bên ngoài. Không biết Timestamp, không biết ChartPoint, không phụ thuộc Room Database, Retrofit API hay Compose UI. Thuật toán chỉ nhận `List<Double>` và trả về `List<Double>` với hiệu năng tối ưu nhất.
- **Presentation / UI**: Tiếp nhận dữ liệu đã được Domain chuẩn bị sẵn để hiển thị biểu đồ xu hướng trực quan trên giao diện ứng dụng.