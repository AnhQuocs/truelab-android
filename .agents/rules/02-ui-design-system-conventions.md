# Quy ước UI & Design System (UI & Design System Conventions)

## 1. Hệ thống Kích thước & Khoảng cách dùng chung (Shared Dimensions)

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

### Điều cấm kỵ (Prohibitions):
- **CẤM Hardcode dp tùy tiện**: Tuyệt đối không viết các giá trị tự biên tự diễn như `.padding(17.dp)`, `.padding(19.dp)`, `.padding(23.dp)`. Luôn ưu tiên dùng các hằng số `Spacing*`.
- Nếu có yêu cầu thiết kế mới, hãy khai báo bổ sung vào `Dimensions.kt` có ngữ nghĩa rõ ràng trước khi sử dụng.

---

## 2. Màu sắc & Theme (Color & Theme System)

TrueLab hỗ trợ đầy đủ cả **Light Theme** và **Dark Theme** theo chuẩn Material 3:

- **Nguồn chân lý duy nhất (Single Source of Truth)**: Toàn bộ bảng màu được định nghĩa tập trung trong `:core:ui/theme/Color.kt` và `:core:ui/theme/Theme.kt` (`LightColorScheme` / `DarkColorScheme`).
- **Sử dụng Semantic Color**: Trong các Composable của từng màn hình, luôn gọi qua `MaterialTheme.colorScheme.*` (ví dụ: `primary`, `onPrimary`, `surfaceVariant`, `outline`).
- **CẤM Hardcode mã màu**: Không viết `Color(0xFF1E88E5)` trực tiếp trong code Composable của các tính năng.

---

## 3. Typography có ngữ nghĩa (Semantic Typography)

- Sử dụng hệ thống Typography chuẩn của Material 3 (`MaterialTheme.typography`).
- **Phân loại theo vai trò (Semantic Roles)**:
  - Tiêu đề màn hình (`screenTitle`): `typography.headlineMedium` hoặc `headlineSmall`.
  - Tiêu đề nhóm (`sectionTitle`): `typography.titleMedium` hoặc `titleSmall`.
  - Nội dung chính (`body`): `typography.bodyMedium` hoặc `bodyLarge`.
  - Phụ đề / Metadata (`caption`): `typography.bodySmall` kết hợp với màu `colorScheme.onSurfaceVariant`.
  - Nhãn / Badge (`label`): `typography.labelSmall` hoặc `labelMedium`.
- Không hardcode `fontSize = 15.sp` rải rác trong từng màn hình.

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
