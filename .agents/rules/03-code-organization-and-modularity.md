# Tổ chức Code & Tính Mô-đun (Code Organization & Modularity)

## 1. Giới hạn 400 Dòng Mỗi File (400-Line Limit)

- **Quy tắc cứng**: Không có bất kỳ file mã nguồn (`.kt`) nào được vượt quá **400 dòng code**.
- **Chiến lược phân tách chủ động**: Khi một file đạt ngưỡng ~350 dòng, hãy tiến hành tái cấu trúc:
  - Tách các Composable con thành các file riêng trong `components/`.
  - Tách logic chuyển đổi dữ liệu thành các file Mapper chuyên biệt (`*Mapper.kt`).
  - Tách Domain Models, UI State, Event/Intent sang file riêng.
  - Tách các helper toán học/thống kê vào file chuyên trách.
- **Giữ trọn tính dễ đọc**: Không bao giờ nén dòng hay bỏ định dạng chỉ để ép số dòng code xuống.

---

## 2. Quy ước Đặt tên & Code Style

- **Thể hiện rõ ý định (Intent-Revealing Naming)**:
  - UseCase: `GetMatchesUseCase`, `PredictMatchUseCase`, `CalculateEloUseCase`.
  - ViewModel: `MatchDetailViewModel`, `AlgorithmBenchmarkViewModel`.
  - Repository: `MatchRepository` (interface), `MatchRepositoryImpl` (implementation).
  - State/Event: `MatchUiState`, `MatchUiEvent`.
- **Nghiêm cấm các file "God Object" / "God Utils"**:
  - Không tạo các class/file vô định hình như `Utils.kt`, `CommonHelper.kt`, `Manager.kt`.
  - Gom nhóm các hàm tiện ích theo đúng ngữ cảnh nghiệp vụ.

---

## 3. Tổ chức Extension Functions

Chỉ tạo Extension Function khi logic có tính tái sử dụng cao, không gắn chặt với state riêng của một màn hình và có ngữ nghĩa rõ ràng.

Tổ chức các file extension tập trung theo từng domain:
- `DateTimeExtensions.kt`: Chuyển đổi timestamp, định dạng ngày giờ thi đấu.
- `OddsExtensions.kt`: Định dạng tỷ lệ kèo (Decimal, Asian Handicap, Over/Under).
- `StatisticsExtensions.kt`: Định dạng phần trăm, điểm phong độ, form score.
- `ModifierExtensions.kt`: Custom Composable Modifiers (hiệu ứng shimmer, bounce click).

---

## 4. Áp dụng Design Patterns Có Kiểm Soát

Không áp dụng pattern chỉ để làm code phức tạp hơn. Mỗi khi dùng một pattern quan trọng, Agent phải giải thích:
1. Pattern nào được sử dụng?
2. Tại sao cần sử dụng?
3. Nó giải quyết vấn đề gì?
4. Có thể đơn giản hóa không?

| Design Pattern | Áp dụng trong TrueLab |
|---|---|
| **Repository Pattern** | Trừu tượng hóa nguồn dữ liệu API và Room DB sau interface domain. |
| **Use Case / Interactor** | Đóng gói từng tác vụ nghiệp vụ độc lập với toán tử `operator fun invoke`. |
| **Observer / Reactive** | Sử dụng Kotlin `Flow` và `StateFlow` cho luồng dữ liệu bất đồng bộ. |
| **Mapper Pattern** | Viết hàm mở rộng chuyển đổi giữa DTO ↔ Entity ↔ Domain Model. |
| **Strategy Pattern** | Cho phép thay thế linh hoạt giữa các thuật toán sắp xếp, tìm kiếm hoặc tính điểm. |
| **Factory / Builder** | Khởi tạo dataset mẫu, cấu hình tham số thuật toán phức tạp. |
| **Dependency Injection** | Hilt DI trong `:app` để inject repository, usecase và dispatchers. |
