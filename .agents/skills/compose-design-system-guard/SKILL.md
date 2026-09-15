---
name: compose-design-system-guard
description: Kỹ năng rà soát và bảo vệ Design System trong Jetpack Compose, ngăn ngừa hardcode token, màu sắc và đa ngôn ngữ.
---

# Kỹ năng Rà soát Design System (Compose Design System Guard)

Kỹ năng này được kích hoạt khi xây dựng hoặc review các file giao diện Jetpack Compose trong TrueLab.

## 1. Các Quy tắc Rà soát Tự động

### ❌ Anti-Patterns Cần Phát hiện & Khắc phục:
1. **Hardcode Spacing/Padding**:
   - Phát hiện: `.padding(17.dp)`, `Spacer(Modifier.height(13.dp))`.
   - Khắc phục: Đổi thành `SpacingS (8.dp)`, `SpacingM (12.dp)`, `SpacingL (16.dp)`, `SpacingXL (24.dp)`.
2. **Hardcode Màu sắc**:
   - Phát hiện: `Color(0xFF2196F3)`, `Color.Red`.
   - Khắc phục: Sử dụng `MaterialTheme.colorScheme.primary`, `error`, `surfaceVariant`.
3. **Hardcode Chuỗi hiển thị**:
   - Phát hiện: `Text("Danh sách trận đấu")`, `Button(...) { Text("Tính toán") }`.
   - Khắc phục: Khai báo vào `strings.xml` (cả 3 file `values`, `values-en`, `values-vi`) và gọi `stringResource(R.string.*)`.
4. **Duplicate UI Component**:
   - Phát hiện: Viết lại cùng một kiểu Card tỷ lệ cược hoặc TopBar ở nhiều màn hình.
   - Khắc phục: Đưa vào `:core:ui/components/`.

---

## 2. Bảng Tra cứu Nhanh Token Chuẩn

| Token Cần Dùng | Giá trị Chuẩn | Mục đích |
|---|---|---|
| `SpacingXXS` | `2.dp` | Khoảng cách siêu nhỏ (giữa icon và badge) |
| `SpacingXS` | `4.dp` | Khoảng cách nhỏ bên trong tag/chip |
| `SpacingS` | `8.dp` | Khoảng cách tiêu chuẩn giữa các phần tử nhỏ |
| `SpacingM` | `12.dp` | Padding nội bộ của Card, Item danh sách |
| `SpacingL` | `16.dp` | Padding viền màn hình chính |
| `SpacingXL` | `24.dp` | Khoảng cách giữa các Section lớn |
| `RadiusSmall` | `8.dp` | Bo góc tag, chip, button nhỏ |
| `RadiusMedium`| `12.dp`| Bo góc Card thông tin trận đấu |
| `RadiusLarge` | `16.dp`| Bo góc Dialog, BottomSheet |
| `RadiusPill`  | `999.dp`| Bo tròn hoàn toàn (Pill badge) |
