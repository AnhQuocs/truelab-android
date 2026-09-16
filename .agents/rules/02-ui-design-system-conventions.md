# Quy ước UI & Design System (UI & Design System Conventions)

## 1. Hệ thống Kích thước & Khoảng cách dùng chung (Shared Dimensions & Dimen)

Tất cả khoảng cách, padding, kích thước icon và thành phần UI phải được định nghĩa tập trung tại `:core:ui/theme/Dimensions.kt`.

### Thang đo Khoảng cách Tiêu chuẩn (Spacing Scale)
```kotlin
val SpacingXXS = 2.dp
val SpacingXS  = 4.dp
val SpacingS   = 8.dp
val SpacingM   = 12.dp
val SpacingL   = 16.dp
val SpacingXL  = 24.dp
val SpacingXXL = 32.dp
```

### Kích thước Component Tiêu chuẩn
```kotlin
val IconSizeSmall   = 16.dp
val IconSizeMedium  = 24.dp
val IconSizeLarge   = 32.dp

val ButtonHeightSmall  = 36.dp
val ButtonHeightMedium = 48.dp
val ButtonHeightLarge  = 56.dp

val TopBarHeight    = 64.dp
val BottomBarHeight = 72.dp
```

### Hệ thống Token `Dimen` (Object `Dimen`)
Để chuẩn hóa Padding và Size cho toàn bộ màn hình, ưu tiên sử dụng `Dimen.*`:

```kotlin
object Dimen {
    // Padding Scale
    val PaddingXXS   = 2.dp
    val PaddingXS    = 4.dp
    val PaddingXSPlus = 6.dp
    val PaddingS     = 8.dp
    val PaddingSM    = 12.dp
    val PaddingM     = 16.dp
    val PaddingML    = 20.dp
    val PaddingL     = 24.dp
    val PaddingXL    = 32.dp
    val PaddingXXL   = 48.dp
    val PaddingUltra = 68.dp

    // Size Scale (Icon, Button, Avatar, Card Dimension...)
    val SizeS        = 16.dp
    val SizeS2       = 18.dp
    val SizeSM       = 20.dp
    val SizeM        = 24.dp
    val SizeML       = 28.dp
    val SizeL        = 32.dp
    val SizeXL       = 36.dp
    val SizeXLPlus   = 40.dp
    val SizeXXL      = 50.dp
    val SizeXXLPlus  = 70.dp
    val SizeMega     = 80.dp
    val SizeUltra    = 120.dp
}
```

### Điều cấm kỵ (Prohibitions):
- **CẤM Hardcode dp tùy tiện**: Tuyệt đối không viết các giá trị tự biên tự diễn như `.padding(17.dp)`, `.padding(19.dp)`, `.padding(23.dp)`. Luôn dùng các hằng số `Dimen.Padding*`, `Dimen.Size*` hoặc `Spacing*`.
- Nếu có yêu cầu thiết kế mới, hãy khai báo bổ sung vào `Dimensions.kt` có ngữ nghĩa rõ ràng trước khi sử dụng.

---

## 2. Màu sắc & Theme (Color & Theme System)

TrueLab hỗ trợ đầy đủ cả **Light Theme** và **Dark Theme** theo chuẩn Material 3:

- **Nguồn chân lý duy nhất (Single Source of Truth)**: Toàn bộ bảng màu được định nghĩa tập trung trong `:core:ui/theme/Color.kt` và `:core:ui/theme/Theme.kt` (`LightColorScheme` / `DarkColorScheme`).
- **Sử dụng Semantic Color**: Trong các Composable của từng màn hình, luôn gọi qua `MaterialTheme.colorScheme.*` (ví dụ: `primary`, `onPrimary`, `surfaceVariant`, `outline`).
- **CẤM Hardcode mã màu**: Không viết `Color(0xFF1E88E5)` trực tiếp trong code Composable của các tính năng.

---

## 3. Typography & Typography Extensions (`TypographyExt.kt`)

Hệ thống Typography chuẩn định nghĩa tại `:core:ui/utils/TypographyExt.kt` kết hợp Material 3 Typography (`MaterialTheme.typography`).

### Thang đo Typography Kích thước Cố định (`Typography.s*`)
Các extension giúp gọi nhanh style theo font size (sp) và line height tương ứng:
- `Typography.s10` (10.sp / line 14.sp)
- `Typography.s12` (12.sp / line 16.sp)
- `Typography.s13` (13.sp / line 18.sp)
- `Typography.s14` (14.sp / line 20.sp)
- `Typography.s15` (15.sp / line 22.sp)
- `Typography.s16` (16.sp / line 24.sp)
- `Typography.s18` (18.sp / line 26.sp)
- `Typography.s20` (20.sp / line 28.sp)
- `Typography.s22` (22.sp / line 30.sp)
- `Typography.s24` (24.sp / line 32.sp)
- `Typography.s28` (28.sp / line 36.sp)
- `Typography.s32` (32.sp / line 40.sp)

### Fluent Style Modifiers cho TextStyle
Thay vì dùng `.copy(fontWeight = ...)`, sử dụng chuỗi hàm mở rộng ngắn gọn:
```kotlin
// Font Weight Extensions
fun TextStyle.bold(): TextStyle
fun TextStyle.semiBold(): TextStyle
fun TextStyle.medium(): TextStyle
fun TextStyle.normal(): TextStyle
fun TextStyle.light(): TextStyle

// Style & Decoration Extensions
fun TextStyle.italic(): TextStyle
fun TextStyle.underline(): TextStyle
fun TextStyle.lineThrough(): TextStyle
```

**Ví dụ sử dụng:**
```kotlin
Text(
    text = "TrueLab Engine",
    style = MaterialTheme.typography.s16.semiBold(),
    color = MaterialTheme.colorScheme.primary
)
```

- **CẤM**: Không hardcode `fontSize = 15.sp` rải rác trong từng màn hình mà hãy dùng `MaterialTheme.typography.s*` hoặc Material 3 tokens.

---

## 4. Hình dạng & Bo góc (Shapes)

Tất cả giá trị bo góc (Corner Radius) được định nghĩa tập trung trong `:core:ui/theme/Shape.kt`:
```kotlin
val RadiusSmall   = 8.dp
val RadiusMedium  = 12.dp
val RadiusLarge   = 16.dp
val RadiusPill    = 999.dp
```

---

## 5. UI Component Tái sử dụng (Reusable UI in `:core:ui`)

Mọi component xuất hiện từ 2 màn hình trở lên hoặc phục vụ mục đích dùng chung phải được đưa vào `:core:ui/components/`:
- `AppButton`: Nút bấm chuẩn hóa với các variant (Filled, Outlined, Text) kèm trạng thái Loading.
- `AppTextField`: Ô nhập liệu có hỗ trợ icon, label và thông báo lỗi.
- `AppCard`: Thẻ chứa nội dung với bo góc và elevation chuẩn.
- `AppTopBar`: Thanh điều hướng đầu trang thống nhất title và action icons.
- `AppLoading`: Trạng thái đang tải (shimmer / spinner).
- `AppError`: Khung hiển thị lỗi kèm nút thử lại (Retry).
- `EmptyState`: Khung hiển thị khi không có dữ liệu/kết quả.
- `charts/`: Các biểu đồ trực quan hóa dữ liệu thống kê bóng đá.

---

## 6. Chuẩn Đa ngôn ngữ (Triple-Locale Localization)

**CẤM TUYỆT ĐỐI hardcode chuỗi hiển thị người dùng (user-facing text) trong code Kotlin.**

Mọi chuỗi phải được khai báo đầy đủ trong cả 3 file tài nguyên:
1. `res/values/strings.xml` (Ngôn ngữ mặc định)
2. `res/values-en/strings.xml` (Tiếng Anh)
3. `res/values-vi/strings.xml` (Tiếng Việt)

Trong Composable, luôn sử dụng:
```kotlin
Text(stringResource(R.string.match_predictions_title))
```
Khi thêm hoặc chỉnh sửa string mới, bắt buộc phải cập nhật đồng bộ cả 3 file trên.
