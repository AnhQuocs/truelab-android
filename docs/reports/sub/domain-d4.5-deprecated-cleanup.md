# TrueLab — Báo cáo Dọn dẹp Code Không còn Sử dụng (Domain D4.5 Deprecated Cleanup)

**Giai đoạn con:** D4.5 — Deprecated Cleanup
**Trạng thái:** HOÀN THÀNH (Đã kiểm thử & xác thực)
**Ngày:** 24/09/2026
**Người thực hiện:** AI Agent (TrueLab Clean Arch Engine)

---

## 1. Tóm tắt Tổng quan

Giai đoạn con Domain **D4.5 — Deprecated Cleanup** tập trung vào việc loại bỏ các API cũ đã đánh dấu `@Deprecated` và các ràng buộc DI không còn sử dụng được sinh ra trước khi tái cấu trúc mô hình dự đoán đa tín hiệu (P1/D4).

Toàn bộ 4 mục tiêu được chỉ định trong `docs/plans/domain-d4-plan.md` (mục D4.5) đã được kiểm toán và loại bỏ sạch sẽ, không gây ảnh hưởng hay làm gián đoạn pipeline dự đoán trên production (`PredictMatchOutcomeUseCase` + 6 signal transformers + `WeightedScorer`).

---

## 2. Kiểm toán Tham chiếu API Cũ (Legacy API Audit)

Trước khi tiến hành xóa code, một cuộc tìm kiếm trên toàn bộ repository đã được thực hiện đối với tất cả các biểu tượng (symbols) cũ trong mã nguồn production, test, các module DI và tài liệu:

| Biểu tượng Cũ Mục tiêu | Trạng thái Trước Dọn dẹp | Sử dụng trên Production | Hành động Đã Thực hiện |
| :--- | :--- | :--- | :--- |
| `PredictionResult.Companion.computeWeightedScoring` | Đánh dấu `@Deprecated` trong `Prediction.kt` | 0 vị trí gọi | Đã xóa companion object & method |
| `SeasonRanking.calculateFormScore()` | Đánh dấu `@Deprecated` trong `Team.kt` | 0 vị trí gọi | Đã xóa method |
| `PredictMatchUseCase` & `PredictionUseCases` | Wrapper UseCase đơn lẻ cũ trong `PredictionUseCases.kt` | 0 vị trí gọi | Đã xóa file `PredictionUseCases.kt` |
| Ràng buộc DI cũ trong `PredictionDataModule` | `providePredictionUseCases()` cung cấp wrapper cũ | 0 vị trí gọi trong presentation/app | Đã xóa provider, thay bằng `providePredictMatchOutcomeUseCase()` |

---

## 3. Tóm tắt Code Đã Xóa

### 3.1 `Prediction.kt`
- Xóa `PredictionResult.Companion` chứa phép tính cũ `computeWeightedScoring(homeForm, awayForm, h2h)`.
- Giữ nguyên các mô hình dữ liệu bất biến: `PredictionResult`, `PredictionWeights`, `HistoricalMatchResult`, `OutcomeProbabilities`, `KeyFactor`, `FactorImpact`.

### 3.2 `Team.kt`
- Xóa extension/member function `SeasonRanking.calculateFormScore()`.
- Giữ nguyên data class `SeasonRanking` với đầy đủ các trường sạch sẽ (`teamId`, `position`, `won`, `draw`, `loss`, `goalDiff`, `recently`, `totalMatches`, `totalPoints`).

### 3.3 `PredictionUseCases.kt`
- Đã xóa hoàn toàn file `core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictionUseCases.kt`.
- Không còn bất kỳ tham chiếu nào tới `PredictMatchUseCase` hoặc data class wrapper `PredictionUseCases`.

### 3.4 `PredictionDataModule.kt`
- Xóa `providePredictionUseCases(...)` inject `PredictMatchUseCase` cũ.
- Xóa provider không dùng, chỉ giữ `@Binds abstract fun bindPredictionRepository(...)`. (Lưu ý: `PredictMatchOutcomeUseCase` đã được cung cấp qua `@Provides @Singleton` trong `DomainUseCaseModule` tại `:app`, đảm bảo không trùng lặp binding).

---

## 4. Xác thực Pipeline Dự đoán Production

Pipeline dự đoán hoạt động trên production được xác nhận vẫn giữ nguyên vẹn 100%:

```text
PredictionViewModel
    ↓
PredictMatchOutcomeUseCase
    ↓
MatchPredictionContext (Elo, Form, H2H, Venue, Odds, Rest)
    ↓
6 Signal Transformers (EloSignal, FormSignal, H2HSignal, VenueSignal, OddsSignal, RestSignal)
    ↓
WeightedScorer (Chuẩn hóa trọng số động & Đánh giá Softmax)
    ↓
PredictionResult (Xác suất, Độ tin cậy, Nhân tố then chốt, Dự đoán tỷ số)
```

- `PredictionViewModel` inject và sử dụng trực tiếp `PredictMatchOutcomeUseCase`.
- `BacktestPredictionUseCase` inject và sử dụng trực tiếp `PredictMatchOutcomeUseCase`.
- Không còn sót lại bất kỳ luồng dự đoán cũ nào trên production.

---

## 5. Kết quả Kiểm thử & Regression

### 5.1 Kiểm toán Test
- Không có test nào chỉ kiểm thử riêng các API cũ đã bị xóa.
- Toàn bộ các bài test hiện có đều kiểm thử tính năng production và use case domain hiện tại.

### 5.2 Kết quả Test
```bash
./gradlew :core:algorithm:test :core:domain:test :app:testDebugUnitTest assembleDebug
```
- **`:core:algorithm`**: 122/122 PASS (Frozen)
- **`:core:domain`**: 206/206 PASS
- **`:app`**: 46/46 PASS
- **Tổng số Unit Tests**: **374/374 PASS (100%)**
- **Build**: `assembleDebug` **BUILD SUCCESSFUL**

---

## 6. Xác thực Tìm kiếm Sau Khi Dọn dẹp

Tìm kiếm trên toàn bộ repository xác nhận:
- `computeWeightedScoring`: 0 lần xuất hiện trong code Kotlin (chỉ có trong tài liệu báo cáo lịch sử).
- `calculateFormScore`: 0 lần xuất hiện trong code Kotlin (chỉ có trong tài liệu báo cáo lịch sử).
- `PredictMatchUseCase`: 0 lần xuất hiện trong code Kotlin (chỉ có trong tài liệu báo cáo lịch sử).
- `providePredictionUseCases`: 0 lần xuất hiện trong code Kotlin.

---

## 7. Tuân thủ Kiến trúc

- **Kiến trúc Clean Đa Module**: Các ranh giới module được bảo toàn tuyệt đối.
- **Pure Kotlin/JVM**: `:core:domain` duy trì 100% không phụ thuộc Android SDK.
- **Giới hạn số dòng file**: Tất cả các file chỉnh sửa đều tuân thủ nghiêm ngặt `< 400 dòng`.
