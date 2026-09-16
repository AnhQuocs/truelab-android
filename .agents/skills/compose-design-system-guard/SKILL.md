---
name: compose-design-system-guard
description: Kỹ năng rà soát và bảo vệ Design System trong Jetpack Compose, ngăn ngừa hardcode token, màu sắc và đa ngôn ngữ.
---

# Kỹ năng Rà soát Design System (Compose Design System Guard)

Kỹ năng này được kích hoạt khi xây dựng hoặc review các file giao diện Jetpack Compose trong TrueLab.

## 1. Các Quy tắc Rà soát Tự động

### ❌ Anti-Patterns Cần Phát hiện & Khắc phục:
1. **Hardcode Spacing/Padding/Size**:
   - Phát hiện: `.padding(17.dp)`, `Spacer(Modifier.height(13.dp))`, `Modifier.size(45.dp)`.
   - Khắc phục: Dùng `Dimen.Padding*` (`PaddingS`, `PaddingM`, `PaddingL`, ...), `Dimen.Size*` (`SizeS`, `SizeM`, `SizeL`, ...), hoặc `Spacing*`.
2. **Hardcode Typography / Font Size**:
   - Phát hiện: `fontSize = 15.sp`, `TextStyle(fontWeight = FontWeight.Bold)`.
   - Khắc phục: Dùng `MaterialTheme.typography.s*` từ `TypographyExt.kt` kết hợp fluent modifier `.bold()`, `.semiBold()`, `.medium()`, v.v.
3. **Hardcode Màu sắc**:
   - Phát hiện: `Color(0xFF2196F3)`, `Color.Red`.
   - Khắc phục: Sử dụng `MaterialTheme.colorScheme.primary`, `error`, `surfaceVariant`.
4. **Hardcode Chuỗi hiển thị**:
   - Phát hiện: `Text("Danh sách trận đấu")`, `Button(...) { Text("Tính toán") }`.
   - Khắc phục: Khai báo vào `strings.xml` (cả 3 file `values`, `values-en`, `values-vi`) và gọi `stringResource(R.string.*)`.
5. **Duplicate UI Component**:
   - Phát hiện: Viết lại cùng một kiểu Card tỷ lệ cược hoặc TopBar ở nhiều màn hình.
   - Khắc phục: Đưa vào `:core:ui/components/`.

---

## 2. Bảng Tra cứu Nhanh Token Chuẩn

### Bảng Token Kích thước (`Dimen` & `Spacing`)
| Token Cần Dùng | Giá trị Chuẩn | Mục đích |
|---|---|---|
| `Dimen.PaddingXXS` / `SpacingXXS` | `2.dp` | Khoảng cách siêu nhỏ (giữa icon và badge) |
| `Dimen.PaddingXS` / `SpacingXS` | `4.dp` | Khoảng cách nhỏ bên trong tag/chip |
| `Dimen.PaddingXSPlus` | `6.dp` | Padding tinh chỉnh chip/badge |
| `Dimen.PaddingS` / `SpacingS` | `8.dp` | Khoảng cách tiêu chuẩn giữa các phần tử nhỏ |
| `Dimen.PaddingSM` / `SpacingM` | `12.dp` | Padding nội bộ của Card, Item danh sách |
| `Dimen.PaddingM` / `SpacingL` | `16.dp` | Padding viền màn hình chính |
| `Dimen.PaddingML` | `20.dp` | Padding rộng cho section |
| `Dimen.PaddingL` / `SpacingXL` | `24.dp` | Khoảng cách giữa các Section lớn |
| `Dimen.PaddingXL` / `SpacingXXL` | `32.dp` | Khoảng cách lớn |
| `Dimen.SizeS` | `16.dp` | Icon/Nút siêu nhỏ |
| `Dimen.SizeM` | `24.dp` | Icon chuẩn |
| `Dimen.SizeL` | `32.dp` | Icon/Avatar lớn |
| `RadiusSmall` | `8.dp` | Bo góc tag, chip, button nhỏ |
| `RadiusMedium`| `12.dp`| Bo góc Card thông tin trận đấu |
| `RadiusLarge` | `16.dp`| Bo góc Dialog, BottomSheet |
| `RadiusPill`  | `999.dp`| Bo tròn hoàn toàn (Pill badge) |

### Bảng Typography Helpers (`TypographyExt.kt`)
| Extension | Kích thước Font | Ví dụ Fluent Modifiers |
|---|---|---|
| `typography.s10` | 10.sp (lineHeight 14.sp) | `typography.s10.medium()` |
| `typography.s12` | 12.sp (lineHeight 16.sp) | `typography.s12.semiBold()` |
| `typography.s13` | 13.sp (lineHeight 18.sp) | `typography.s13.normal()` |
| `typography.s14` | 14.sp (lineHeight 20.sp) | `typography.s14.medium()` |
| `typography.s15` | 15.sp (lineHeight 22.sp) | `typography.s15.normal()` |
| `typography.s16` | 16.sp (lineHeight 24.sp) | `typography.s16.bold()` |
| `typography.s18` | 18.sp (lineHeight 26.sp) | `typography.s18.semiBold()` |
| `typography.s20` | 20.sp (lineHeight 28.sp) | `typography.s20.bold()` |
| `typography.s24` | 24.sp (lineHeight 32.sp) | `typography.s24.bold()` |
| `typography.s28` | 28.sp (lineHeight 36.sp) | `typography.s28.bold()` |
| `typography.s32` | 32.sp (lineHeight 40.sp) | `typography.s32.bold()` |
