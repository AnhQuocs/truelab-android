# Domain D4.1 Report — Evaluation Metrics & 3-Way Confusion Matrix

Báo cáo chi tiết triển khai cấu phần **Domain D4.1 — Evaluation Metrics** thuộc lộ trình **Domain D4 (Domain Expansion, Evaluation & Benchmark Suite)**.

---

## 1. Mục tiêu Triển khai (Objectives)

- Xây dựng mô hình dữ liệu và UseCase nghiệp vụ thuần túy (Pure Kotlin/JVM) tính toán các chỉ số đánh giá phân loại đa lớp (Multi-class Classification Metrics) cho bài toán dự đoán bóng đá 3 cửa (`HOME_WIN`, `DRAW`, `AWAY_WIN`).
- Tính toán Ma trận nhầm lẫn 3 chiều ($3 \times 3$ Confusion Matrix) với định hướng hàng là kết quả Thực tế (Actual) và cột là kết quả Dự đoán (Predicted).
- Tính toán Accuracy, Class-level Precision / Recall / F1 / Support, và Macro-averaged Precision / Recall / F1.
- Xử lý an toàn 100% các trường hợp biên (tập dữ liệu rỗng, chia cho 0, lớp không xuất hiện).
- Kiểm soát chặt chẽ tính toàn vẹn dữ liệu: ném ngoại lệ `IllegalArgumentException` khi phát hiện nhãn kết quả không hợp lệ.

---

## 2. Danh sách File Đã Tạo (Created Files)

1. **`EvaluationModels.kt`**:
   - Vị trí: [`core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/model/EvaluationModels.kt)
   - Chứa: `ConfusionMatrix3Way`, `ClassEvaluationMetrics`, `ModelEvaluationResult`.
2. **`CalculateEvaluationMetricsUseCase.kt`**:
   - Vị trí: [`core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/CalculateEvaluationMetricsUseCase.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/CalculateEvaluationMetricsUseCase.kt)
   - Chứa logic tính toán thống kê và kiểm tra tính hợp lệ của nhãn.
3. **`CalculateEvaluationMetricsUseCaseTest.kt`**:
   - Vị trí: [`core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/CalculateEvaluationMetricsUseCaseTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/CalculateEvaluationMetricsUseCaseTest.kt)
   - Chứa 12 test cases bao phủ toàn diện các kịch bản thực tế và trường hợp biên.

---

## 3. Thiết kế Mô hình Dữ liệu (Domain Models)

### 3.1. Ma trận Nhầm lẫn 3 Chiều (`ConfusionMatrix3Way`)

```text
                        Dự đoán (Predicted)
                   HOME_WIN    DRAW    AWAY_WIN
Thực tế HOME_WIN [ homeAsHome, homeAsDraw, homeAsAway ]
Thực tế DRAW     [ drawAsHome, drawAsDraw, drawAsAway ]
Thực tế AWAY_WIN [ awayAsHome, awayAsDraw, awayAsAway ]
```

- **`totalSamples`**: Tổng số mẫu $\sum_{i,j} C_{ij} = \text{homeAsHome} + \text{homeAsDraw} + \dots + \text{awayAsAway}$.
- **`correctPredictions`**: Tổng số mẫu nằm trên đường chéo chính $= \text{homeAsHome} + \text{drawAsDraw} + \text{awayAsAway}$.

### 3.2. Chỉ số Đánh giá Cấp Lớp (`ClassEvaluationMetrics`)

- `precision: Double`: Độ chuẩn xác của lớp $C$.
- `recall: Double`: Độ nhạy / Độ bao phủ của lớp $C$.
- `f1Score: Double`: Điểm F1 trung hòa giữa Precision và Recall.
- `support: Int`: Số lượng mẫu thực tế thuộc về lớp $C$.

### 3.3. Kết quả Tổng thể (`ModelEvaluationResult`)

- `accuracy: Double`: Độ chính xác tổng thể.
- `macroPrecision: Double`: Trung bình cộng Precision của 3 lớp.
- `macroRecall: Double`: Trung bình cộng Recall của 3 lớp.
- `macroF1: Double`: Trung bình cộng F1 của 3 lớp.
- `homeMetrics: ClassEvaluationMetrics`: Chỉ số cho lớp `HOME_WIN`.
- `drawMetrics: ClassEvaluationMetrics`: Chỉ số cho lớp `DRAW`.
- `awayMetrics: ClassEvaluationMetrics`: Chỉ số cho lớp `AWAY_WIN`.
- `confusionMatrix: ConfusionMatrix3Way`: Ma trận nhầm lẫn chi tiết.
- `totalEvaluated: Int`: Tổng số mẫu đánh giá.

---

## 4. Công thức Toán học Triển khai

### 4.1. Overall Accuracy
$$\text{Accuracy} = \frac{\text{homeAsHome} + \text{drawAsDraw} + \text{awayAsAway}}{N}$$
*Nếu $N = 0 \implies \text{Accuracy} = 0.0$.*

### 4.2. Class-level Precision
$$\text{Precision}_c = \frac{\text{TP}_c}{\text{Tổng số lần dự đoán lớp } c}$$
- $\text{Precision}_{\text{Home}} = \frac{\text{homeAsHome}}{\text{homeAsHome} + \text{drawAsHome} + \text{awayAsHome}}$
- $\text{Precision}_{\text{Draw}} = \frac{\text{drawAsDraw}}{\text{homeAsDraw} + \text{drawAsDraw} + \text{awayAsDraw}}$
- $\text{Precision}_{\text{Away}} = \frac{\text{awayAsAway}}{\text{homeAsAway} + \text{drawAsAway} + \text{awayAsAway}}$

*Nếu mẫu số $= 0$ (mô hình không bao giờ dự đoán lớp $c$) $\implies \text{Precision}_c = 0.0$.*

### 4.3. Class-level Recall
$$\text{Recall}_c = \frac{\text{TP}_c}{\text{Tổng số mẫu thực tế thuộc lớp } c}$$
- $\text{Recall}_{\text{Home}} = \frac{\text{homeAsHome}}{\text{homeAsHome} + \text{homeAsDraw} + \text{homeAsAway}}$
- $\text{Recall}_{\text{Draw}} = \frac{\text{drawAsDraw}}{\text{drawAsHome} + \text{drawAsDraw} + \text{drawAsAway}}$
- $\text{Recall}_{\text{Away}} = \frac{\text{awayAsAway}}{\text{awayAsHome} + \text{awayAsDraw} + \text{awayAsAway}}$

*Nếu mẫu số $= 0$ (thực tế không có trận nào thuộc kết quả $c$) $\implies \text{Recall}_c = 0.0$.*

### 4.4. Class-level F1-Score
$$\text{F1}_c = \begin{cases} 2 \times \frac{\text{Precision}_c \times \text{Recall}_c}{\text{Precision}_c + \text{Recall}_c} & \text{khi } \text{Precision}_c + \text{Recall}_c > 0 \\ 0.0 & \text{khi } \text{Precision}_c + \text{Recall}_c = 0 \end{cases}$$

### 4.5. Macro-Averaging
$$\text{Macro Precision} = \frac{\text{Precision}_{\text{Home}} + \text{Precision}_{\text{Draw}} + \text{Precision}_{\text{Away}}}{3.0}$$
$$\text{Macro Recall} = \frac{\text{Recall}_{\text{Home}} + \text{Recall}_{\text{Draw}} + \text{Recall}_{\text{Away}}}{3.0}$$
$$\text{Macro F1} = \frac{\text{F1}_{\text{Home}} + \text{F1}_{\text{Draw}} + \text{F1}_{\text{Away}}}{3.0}$$

---

## 5. Xử lý Trường hợp Biên & Tính Toàn vẹn Dữ liệu

1. **Tập dữ liệu rỗng (`emptyList()`)**:
   - Trả về `ModelEvaluationResult` an toàn với mọi chỉ số $= 0.0$, `support = 0`, `totalEvaluated = 0` và ma trận toàn số 0.
   - Không gây crash, không xảy ra chia cho 0.
2. **Triệt tiêu chia cho 0 (Zero-division Protection)**:
   - Các phép chia kiểm tra điều kiện mẫu số $> 0$. Nếu mẫu số $= 0 \implies$ trả về `0.0`.
   - Tuyệt đối không phát sinh giá trị `Double.NaN` hay `Double.POSITIVE_INFINITY`.
3. **Kiểm tra nhãn không hợp lệ (Invalid Label Handling)**:
   - Bất kỳ chuỗi nhãn nào ngoài tập `{"HOME_WIN", "DRAW", "AWAY_WIN"}` ở cả chiều `predicted` hoặc `actual` đều lập tức ném `IllegalArgumentException` kèm thông báo chi tiết, ngăn chặn sai lệch kết quả thống kê.

---

## 6. Kết quả Kiểm thử Unit Test & Hồi quy

### 6.1. Danh mục 12 Test Cases trong `CalculateEvaluationMetricsUseCaseTest`

| STT | Tên Test Case | Mục đích Kiểm thử | Kết quả |
| :---: | :--- | :--- | :---: |
| 1 | `1 Perfect prediction returns 1_0 for accuracy precision recall and F1` | Xác thực mô hình đúng 100% đạt Accuracy=1.0, F1=1.0 | **PASS** |
| 2 | `2 Completely wrong predictions return 0_0 accuracy and 0_0 F1` | Xác thực mô hình sai 100% đạt Accuracy=0.0, F1=0.0 | **PASS** |
| 3 | `3 Mixed dataset across 3 classes computes exact metrics and confusion matrix` | Xác thực ma trận $12$ mẫu với số liệu tính toán chi tiết bằng tay | **PASS** |
| 4 | `4 Empty dataset returns all zero metrics without crashing or dividing by zero` | Kiểm tra tập rỗng không crash và trả về kết quả 0.0 chuẩn hóa | **PASS** |
| 5 | `5 Class missing in actual labels handles zero recall and support safely` | Xử lý an toàn khi một lớp không có mẫu thực tế | **PASS** |
| 6 | `6 Class missing in predicted labels handles zero precision safely` | Xử lý an toàn khi mô hình không bao giờ dự đoán một lớp | **PASS** |
| 7 | `7 Zero division edge case produces 0_0 rather than NaN or Infinity` | Đảm bảo không phát sinh `NaN` / `Infinity` khi chia cho 0 | **PASS** |
| 8 | `8 Confusion matrix orientation and cell counts are verified strictly` | Xác thực chính xác 9 ô trong ma trận nhầm lẫn | **PASS** |
| 9 | `9 Macro precision is calculated as exact arithmetic mean of 3 class precisions` | Kiểm tra tính đúng đắn của Macro Precision | **PASS** |
| 10 | `10 Macro recall is calculated as exact arithmetic mean of 3 class recalls` | Kiểm tra tính đúng đắn của Macro Recall | **PASS** |
| 11 | `11 Macro F1 is calculated as exact arithmetic mean of 3 class F1 scores` | Kiểm tra tính đúng đắn của Macro F1 | **PASS** |
| 12 | `12 Invalid predicted or actual outcome label throws IllegalArgumentException` | Kiểm tra ném ngoại lệ khi nhãn không hợp lệ | **PASS** |

### 6.2. Kết quả Kiểm thử Hồi quy Toàn bộ Dự án

```text
> Task :core:algorithm:test     -> 122 tests PASS (0 failures, 0 errors, 0 skipped)
> Task :core:domain:test        -> 168 tests PASS (0 failures, 0 errors, 0 skipped) [+12 new tests]
> Task :app:testDebugUnitTest   ->  46 tests PASS (0 failures, 0 errors, 0 skipped)
====================================================================================
TỔNG CỘNG                       -> 336 tests PASS (0 failures, 0 errors, 0 skipped)
> Task :app:assembleDebug       -> BUILD SUCCESSFUL
```

---

## 7. Xác thực Ranh giới Kiến trúc (Architecture Verification)

- **Pure Kotlin/JVM**: Cả 2 file mã nguồn `EvaluationModels.kt` và `CalculateEvaluationMetricsUseCase.kt` trong `:core:domain` đều 100% không chứa phụ thuộc Android SDK (`android.*`, `androidx.*`), Room DB, Retrofit hay UI Compose.
- **Tính Bất biến (Immutability)**: Các data class `ConfusionMatrix3Way`, `ClassEvaluationMetrics`, `ModelEvaluationResult` đều là immutable value objects.
- **Tính độc lập của Module**: `:core:algorithm` giữ nguyên vẹn trạng thái Frozen (122 tests).
