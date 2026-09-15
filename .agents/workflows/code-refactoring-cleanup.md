# Quy trình Tái cấu trúc & Dọn dẹp Code (Code Refactoring & Cleanup Workflow)

Quy trình này áp dụng khi tối ưu hóa mã nguồn, phân rã các file vượt quá giới hạn 400 dòng, hoặc dọn dẹp các thành phần trùng lặp.

---

## 1. Các Tình huống Kích hoạt Tái cấu trúc

1. **File vượt quá 350 dòng**: Sắp chạm hoặc vượt ngưỡng 400 dòng tối đa.
2. **Trùng lặp thành phần UI**: Cùng một kiểu Card, Button hay Header xuất hiện từ 2 màn hình trở lên.
3. **Hardcode Token/Màu sắc**: Phát hiện mã màu, kích thước tùy tiện trong file giao diện.
4. **Vi phạm ranh giới Module**: Xuất hiện import Android trong Domain hay Algorithm module.

---

## 2. Quy trình Tái cấu trúc 5 Bước

```text
1. Đọc & Định vị Điểm nóng (Identify Hotspots)
         ↓
2. Lập Kế hoạch Phân rã (Decomposition Plan)
         ↓
3. Trích xuất Thành phần Độc lập (Extract Classes / Composables)
         ↓
4. Chuyển vị trí & Cập nhật Import (Move to Correct Module)
         ↓
5. Kiểm thử & Xác thực Hồi quy (Regression Testing & Build)
```

---

## 3. Hướng dẫn Trích xuất Cụ thể

### Trích xuất Màn hình Lớn (> 350 dòng)
1. Tạo thư mục `screens/<feature>/components/`.
2. Trích xuất từng phần Composable độc lập (Header, Body List, Filter Bar, Bottom Action) thành các file riêng biệt (mỗi file dưới 150 dòng).
3. Đặt các file `*UiState.kt` và `*UiEvent.kt` sang file riêng.

### Trích xuất Component Tái sử dụng
1. Di chuyển component sang `:core:ui/components/`.
2. Đổi tên có tiền tố `App*` (ví dụ: `AppMatchCard.kt`).
3. Chuẩn hóa padding, radius theo `Dimensions.kt` và theme theo `MaterialTheme.colorScheme`.

### Trích xuất Mapper Dữ liệu
1. Đặt tên file là `<Model>Mapper.kt` trong `:core:data/remote/mapper/` hoặc `:core:data/local/mapper/`.
2. Viết dưới dạng hàm mở rộng `fun MatchDto.toDomain(): Match`.
