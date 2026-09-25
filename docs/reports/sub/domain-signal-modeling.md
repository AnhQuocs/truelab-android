# TrueLab – Báo cáo Triển khai Mô hình hóa Tín hiệu Domain (Domain Signal Modeling Report)

Tài liệu này tổng kết quá trình triển khai cấu hình trọng số theo đặc tả **FR-14** và 6 Pure Kotlin Signal Transformers trong tầng `:core:domain`, phục vụ cho việc tích hợp mô hình dự đoán **Weighted Scoring (Phase 7)**.

---

## 1. Tổng quan Triển khai (Implementation Overview)

- **Module**: `:core:domain` (Pure Kotlin/JVM).
- **Phạm vi**:
  - Tạo [PredictionWeightConfig.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt) chuẩn hóa cấu hình trọng số và các tham số mô hình hóa.
  - Xây dựng **6 Signal Transformers** chuyển đổi dữ liệu bóng đá thành [Signal3Way.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/Signal3Way.kt).
  - Viết bộ Unit Test toàn diện cho toàn bộ cấu hình và transformers.
- **Ranh giới Kiến trúc**:
  - Tuyệt đối **KHÔNG sửa đổi `:core:algorithm`** (tiếp tục FROZEN).
  - Không thêm Machine Learning, Softmax, Temperature hay thuật toán mới.
  - Không sửa đổi UI, ViewModel hay Data layer.

---

## 2. Chi tiết 6 Signal Transformers

| STT | Transformer Class | Input Nghiệp vụ | Output | Cơ chế Xử lý & Tham số |
|:---:|---|---|---|---|
| **1** | [OddsSignalTransformer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/OddsSignalTransformer.kt) | `OddsRecordItem?` | `Signal3Way` | Gọi `calculateImpliedProbability()`, chuẩn hóa biên lợi nhuận nhà cái. Fallback `weight = 0.0` nếu thiếu odds. |
| **2** | [EloSignalTransformer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/EloSignalTransformer.kt) | `homeElo: Double?`<br>`awayElo: Double?` | `Signal3Way` | Tính kỳ vọng logistic 2 chiều $E_H, E_A$ qua `EloRatingCalculator` và phân rã với Baseline Draw $P_D = 0.26$. |
| **3** | [FormSignalTransformer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/FormSignalTransformer.kt) | `homeForm: FormScore?`<br>`awayForm: FormScore?` | `Signal3Way` | Chuẩn hóa điểm phong độ $[0, 100]$, tính tỷ trọng tương đối có hệ số làm mịn $\epsilon = 0.10$ kết hợp Baseline Draw $P_D = 0.26$. |
| **4** | [H2hSignalTransformer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/H2hSignalTransformer.kt) | `homeWins: Int`<br>`draws: Int`<br>`awayWins: Int` | `Signal3Way` | Áp dụng Laplace Smoothing với phân phối tiên nghiệm $\boldsymbol{\alpha} = [0.45, 0.27, 0.28]$ và độ mạnh mẫu $K = 3.0$. |
| **5** | [HomeAdvantageSignalTransformer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformer.kt) | `isNeutralVenue: Boolean` | `Signal3Way` | Tạo tín hiệu độc lập $[0.46, 0.26, 0.28]$ với trọng số $10\%$ theo FR-14 (hoặc $[0.37, 0.26, 0.37]$ nếu sân trung lập). |
| **6** | [GoalsSignalTransformer.kt](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/GoalsSignalTransformer.kt) | `homeMeanScored, conceded`<br>`awayMeanScored, conceded` | `Signal3Way` | Tính chênh lệch bàn thắng kỳ vọng $\Delta \lambda$, ánh xạ tuyến tính với độ nhạy $\beta = 0.15$ và chặn biên $[0.05, 0.69]$ cùng $P_D = 0.26$. |

---

## 3. Danh mục Tham số & Trọng số (FR-14 Configuration)

```kotlin
data class PredictionWeightConfig(
    val formWeight: Double = 0.25,          // 25% (FR-14)
    val eloWeight: Double = 0.20,           // 20% (FR-14)
    val oddsWeight: Double = 0.20,          // 20% (FR-14)
    val goalsWeight: Double = 0.15,         // 15% (FR-14)
    val h2hWeight: Double = 0.10,           // 10% (FR-14)
    val homeAdvantageWeight: Double = 0.10,  // 10% (FR-14)
    // Business Heuristic Parameters
    val baselineDrawProb: Double = 0.26,
    val formSmoothingEpsilon: Double = 0.10,
    val h2hPriorK: Double = 3.0,
    val h2hPriorHome: Double = 0.45,
    val h2hPriorDraw: Double = 0.27,
    val h2hPriorAway: Double = 0.28,
    val homeAdvantageProbHome: Double = 0.46,
    val homeAdvantageProbDraw: Double = 0.26,
    val homeAdvantageProbAway: Double = 0.28,
    val goalsSensitivity: Double = 0.15,
    val goalsMinProbHome: Double = 0.05,
    val goalsMaxProbHome: Double = 0.69
)
```

---

## 4. Kết quả Kiểm thử & Độ phủ (Test Coverage)

### 4.1. Danh mục Unit Tests mới trong `:core:domain`:
1. **`PredictionWeightConfigTest`**:
   - Khớp 100% trọng số FR-14 (tổng = 1.0).
   - Kiểm tra validation tham số âm, tổng trọng số bằng 0, phân phối tiên nghiệm không hợp lệ.
2. **`OddsSignalTransformerTest`**:
   - Chuẩn hóa tỷ lệ cược 1X2 thực tế, bảo toàn xác suất $\sum P = 1.0$ và $P \in [0, 1]$.
   - Xử lý missing / invalid odds fallback sang `weight = 0.0` không gây crash.
3. **`EloSignalTransformerTest`**:
   - Đối xứng khi Elo bằng nhau ($P_H = 0.37, P_D = 0.26, P_A = 0.37$).
   - Elo phân hóa tăng giảm xác suất tương ứng. Fallback Elo null / âm về 1500.0.
4. **`FormSignalTransformerTest`**:
   - Đội toàn thắng vs toàn thua không bị xác suất 0% nhờ $\epsilon = 0.10$.
   - Xử lý $F_H = F_A = 0$ và dữ liệu null an toàn.
5. **`H2hSignalTransformerTest`**:
   - Kiểm tra $N = 0$ khớp prior, $N = 1$ Laplace smoothing tăng mượt mà ($58.75\%$).
   - Khử số đếm âm an toàn.
6. **`HomeAdvantageSignalTransformerTest`**:
   - Kiểm tra sân nhà chuẩn $[0.46, 0.26, 0.28]$ và sân trung lập $[0.37, 0.26, 0.37]$.
7. **`GoalsSignalTransformerTest`**:
   - Chênh lệch bàn thắng bằng 0 trả về $P_H = 0.37, P_D = 0.26, P_A = 0.37$.
   - Kiểm tra chặn biên $[0.05, 0.69]$ với đầu vào cực đoan $\Delta \lambda = \pm 10.0$.

### 4.2. Kết quả Chạy Toàn bộ Test Suite:
- **`:core:domain:test`**: **PASS 100%** (7 test suites, 26 unit tests).
- **`:core:algorithm:test`**: **PASS 100%** (122/122 tests FROZEN tiếp tục pass).
- **Toàn bộ dự án**: **BUILD SUCCESSFUL** (80 actionable tasks executed).
