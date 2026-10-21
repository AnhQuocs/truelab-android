# Phase 6 – Rating Engineering Issues

Tài liệu ghi nhận lịch sử xử lý vấn đề kỹ thuật phát sinh trong quá trình triển khai và Code Review của **Phase 6 – Rating: Elo Rating System** thuộc module `:core:algorithm`.

---

### Issue: Constructor `defaultKFactor` không được sử dụng khi bỏ qua tham số K-Factor

## 1. Context

Trong bản implementation ban đầu của Phase 6, Interface `RatingCalculator` sử dụng tính năng Default Argument của ngôn ngữ Kotlin để gán hệ số K-factor mặc định:

```kotlin
fun updateRating(
    rating: Double,
    opponentRating: Double,
    actualScore: Double,
    kFactor: Double = DEFAULT_K
): Double

fun calculateMatch(
    ratingA: Double,
    ratingB: Double,
    actualScoreA: Double,
    kFactor: Double = DEFAULT_K
): RatingMatchResult
```

Đồng thời, lớp triển khai `EloRatingCalculator` hỗ trợ cấu hình hệ số K mặc định cấp instance thông qua constructor:

```kotlin
class EloRatingCalculator(
    private val defaultKFactor: Double = RatingCalculator.DEFAULT_K
) : RatingCalculator
```

---

## 2. Problem

Khi caller khởi tạo một instance với hệ số K tùy biến cấp đối tượng:

```kotlin
val customCalculator = EloRatingCalculator(defaultKFactor = 20.0)
```

nhưng sau đó gọi hàm tính toán mà bỏ qua tham số `kFactor`:

```kotlin
val result = customCalculator.updateRating(1500.0, 1500.0, actualScore = 1.0)
```

Trình biên dịch Kotlin resolve default argument của Interface thành hằng số tĩnh `DEFAULT_K = 32.0`, thay vì sử dụng `defaultKFactor = 20.0` được lưu trong instance. Kết quả điểm số biến thiên $\Delta R = 16.0$ thay vì giá trị kỳ vọng $\Delta R = 10.0$.

Hậu quả là tham số constructor `defaultKFactor` trở thành tham số chết (Dead parameter), không đáp ứng đúng hợp đồng thiết kế mong muốn: *"omitted K uses constructor default"*.

---

## 3. Detection

- Vấn đề được phát hiện trong phiên **Code Review nội bộ** sau khi hoàn thành implementation ban đầu của Phase 6.
- **Phân loại**:
  - Lỗi thiết kế API / Sai lệch hành vi tham số (API & Behavioral Contract Bug).
  - Không phải lỗi công thức toán học Elo.

---

## 4. Root Cause

Trong cơ chế bytecode của Kotlin, Default Argument được xử lý tại vị trí gọi hàm (Call-Site Resolution). Giá trị mặc định được định nghĩa tĩnh trên Interface (`RatingCalculator.DEFAULT_K`) sẽ luôn được compiler chèn vào bytecode tại call-site khi caller không truyền tham số, hoàn toàn không có khả năng đọc trạng thái động của instance đang thực thi (`this.defaultKFactor`).

---

## 5. Fix

Thay thế hoàn toàn Kotlin Default Argument trên Interface bằng kỹ thuật **Method Overloading** tường minh:

### 5.1. Cập nhật Interface `RatingCalculator`
Tách thành 2 nhóm hàm rõ ràng:

```kotlin
// Nhóm 1: Không nhận K, ủy quyền cho instance default K
fun updateRating(rating: Double, opponentRating: Double, actualScore: Double): Double
fun calculateMatch(ratingA: Double, ratingB: Double, actualScoreA: Double): RatingMatchResult

// Nhóm 2: Nhận K tường minh để ghi đè
fun updateRating(rating: Double, opponentRating: Double, actualScore: Double, kFactor: Double): Double
fun calculateMatch(ratingA: Double, ratingB: Double, actualScoreA: Double, kFactor: Double): RatingMatchResult
```

### 5.2. Cập nhật Implementation `EloRatingCalculator`
Hiện thực các hàm không có `kFactor` bằng cách ủy quyền trực tiếp tới `defaultKFactor` của instance:

```kotlin
override fun updateRating(
    rating: Double,
    opponentRating: Double,
    actualScore: Double
): Double = updateRating(rating, opponentRating, actualScore, defaultKFactor)

override fun calculateMatch(
    ratingA: Double,
    ratingB: Double,
    actualScoreA: Double
): RatingMatchResult = calculateMatch(ratingA, ratingB, actualScoreA, defaultKFactor)
```

Các hàm nhận `kFactor` tiếp tục sử dụng trực tiếp giá trị `kFactor` được truyền vào, đảm bảo tính năng override hoạt động chuẩn xác.

---

## 6. Regression Tests

Đã bổ sung 3 ca kiểm thử chuyên biệt vào [EloRatingAlgorithmsTest.kt](../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/rating/EloRatingAlgorithmsTest.kt) để khóa chặt hành vi sau sửa lỗi:

1. `constructor defaultKFactor - is used when kFactor is omitted`: Xác thực `defaultKFactor = 20.0` được áp dụng chính xác khi gọi hàm bỏ qua $K$ ($\Delta R = 10.0$).
2. `method kFactor - explicit kFactor overrides constructor defaultKFactor`: Xác thực `kFactor = 50.0` truyền trực tiếp override đúng giá trị `defaultKFactor = 20.0` của instance ($\Delta R = 25.0$).
3. `constructor validation - invalid defaultKFactor throws IllegalArgumentException`: Xác thực ném ngoại lệ ngay tại constructor khi truyền `0.0`, `-10.0`, `NaN`, `+Infinity`, `-Infinity`.

---

## 7. Verification

- **Trước khi fix**: 19 / 19 tests Phase 6 passed (chưa có ca test kiểm tra default constructor K).
- **Sau khi fix**: 22 / 22 tests Phase 6 passed.
- **Toàn bộ module `:core:algorithm`**: 95 / 95 tests passed (100% tỷ lệ thành công, không có regression).

---

## 8. Impact

- **Toán học Elo**: Giữ nguyên 100% tính đúng đắn toán học và nguyên tắc bảo toàn Zero-Sum.
- **Kiến trúc**: Hoàn toàn độc lập, không ảnh hưởng đến các phân hệ Phase 1–5.
- **API**: Tường minh hơn, tương thích tối đa cả với caller viết bằng Java lẫn Kotlin.

---

## 9. Status

**Resolved**
