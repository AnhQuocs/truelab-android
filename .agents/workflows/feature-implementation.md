# Quy trình Phát triển Tính năng (Feature Implementation Workflow)

Quy trình chuẩn 12 bước bắt buộc thực hiện khi phát triển bất kỳ tính năng mới nào trong TrueLab.

```text
 1. Phân tích Yêu cầu (Requirement Analysis)
            ↓
 2. Kiểm tra Kiến trúc Hiện tại (Architecture Verification)
            ↓
 3. Xác định Module Phù hợp (Module Boundary Mapping)
            ↓
 4. Thiết kế Domain Model (:core:domain)
            ↓
 5. Thiết kế Repository & UseCase (:core:domain)
            ↓
 6. Implement Data Layer (:core:data - API, Room, DTO, DAO, RepoImpl)
            ↓
 7. Implement Algorithm (:core:algorithm - nếu có logic toán học)
            ↓
 8. Implement ViewModel & UI State (:app)
            ↓
 9. Implement Composable UI (:app / :core:ui)
            ↓
10. Viết & Chạy Unit Tests
            ↓
11. Build & Xác thực Hệ thống (./gradlew assembleDebug)
            ↓
12. Review & Báo cáo Tổng kết
```

---

## Chi tiết Từng Bước

### Bước 1 & 2: Phân tích & Kiểm tra
- Đọc kỹ mô tả tính năng.
- Xác định xem tính năng liên quan đến Data (API/Room), Algorithm (toán/xếp hạng) hay chỉ là Presentation (UI).
- Đảm bảo hướng phụ thuộc không bị vi phạm.

### Bước 3, 4 & 5: Thiết kế Domain Layer trước (Domain-Driven)
- Tạo Entity thuần túy tại `:core:domain/model/`.
- Khai báo Repository interface tại `:core:domain/repository/`.
- Tạo UseCase giải quyết nghiệp vụ với toán tử `operator fun invoke`.

### Bước 6 & 7: Hoàn thiện Data và Algorithm
- Nếu cần lưu trữ offline: Tạo Room Entity, DAO và viết Mapper.
- Nếu cần thuật toán phân tích: Viết class/function tại `:core:algorithm` kèm phân tích độ phức tạp.

### Bước 8 & 9: Presentation & UI
- Tạo `UiState` bất biến trong ViewModel.
- Xây dựng UI Composable sử dụng Design System từ `:core:ui`.
- Cập nhật chuỗi giao diện vào cả 3 file `strings.xml`.

### Bước 10 & 11: Kiểm thử & Build
- Chạy test: `./gradlew test`
- Build thử toàn project: `./gradlew assembleDebug`
- Đảm bảo 100% không có lỗi biên dịch hay cảnh báo phá vỡ ranh giới module.

### Bước 12: Báo cáo
- Trình bày kết quả theo đúng mẫu chuẩn trong `04-development-behavior-and-safety.md`.
