# Tiêu chuẩn Kiểm thử & Đo kiểm Hiệu năng (Testing & Benchmark Standards)

## 1. Kim tự tháp Kiểm thử (Testing Pyramid)

Mọi logic quan trọng trong TrueLab phải được kiểm thử toàn diện theo mô hình:

```text
       / \
      / UI\          (UI Test Jetpack Compose - End-to-End screens)
     /-----\
    / Integ \        (Integration Test - Room DB, DAO, Sync, Repository)
   /---------\
  / Unit Test \      (Unit Test - Thuật toán, UseCases, ViewModels, Mappers)
 /-------------\
```

---

## 2. Ma trận Test Case theo Từng Tầng

### 2.1. Kiểm thử Thuật toán (`:core:algorithm`)
Mỗi thuật toán cốt lõi phải có bộ test case JUnit độc lập kiểm tra đầy đủ:
- **Dữ liệu rỗng (Empty Input)**: Danh sách rỗng `emptyList()`.
- **Dữ liệu đơn (Single Element)**: Danh sách có đúng 1 phần tử.
- **Trường hợp thông thường (Normal Case)**: Dataset dữ liệu thực tế mẫu.
- **Dữ liệu lớn (Large Input)**: Stress test với $N \ge 10.000$ phần tử.
- **Giá trị trùng lặp (Duplicate Values)**: Trùng điểm số, trùng Elo, trùng timestamp.
- **Giá trị biên (Boundary Cases)**: Giá trị min/max số nguyên/thực, mẫu số bằng 0.
- **Dữ liệu không hợp lệ (Invalid Input)**: NaN, vô cùng, xác suất ngoài đoạn $[0, 1]$.

### 2.2. Kiểm thử ViewModel (`:app`)
Sử dụng `kotlinx-coroutines-test` với `StandardTestDispatcher` hoặc `UnconfinedTestDispatcher`:
- **Trạng thái ban đầu (Initial State)**.
- **Trạng thái đang tải (Loading State)** khi bắt đầu coroutine.
- **Trạng thái thành công (Success State)** nhận dữ liệu domain.
- **Trạng thái lỗi (Error State)** và ánh xạ thông báo lỗi cho người dùng.
- **Trạng thái dữ liệu trống (Empty State)**.
- **Chuyển đổi trạng thái tuần tự (State Transitions)**.

### 2.3. Kiểm thử Repository (`:core:data`)
- **Tải từ Remote thành công** và ghi cache xuống Room Database.
- **Tải từ Remote thất bại** và kích hoạt cơ chế đọc fallback từ Local Cache.
- **Độ chính xác của Mapper**: DTO ↔ Entity ↔ Domain Model không bị mất mát hay sai lệch trường dữ liệu.

---

## 3. Quy chuẩn Benchmark Thuật toán Độc lập

Đề tài TrueLab yêu cầu đo lường và đánh giá thực nghiệm hiệu năng các thuật toán:

1. **Tách biệt hoàn toàn**: Benchmark KHÔNG ĐƯỢC chứa độ trễ mạng (Network latency), thao tác đọc/ghi đĩa (Disk I/O) hay quá trình Recomposition của Compose UI.
2. **Kích thước Dataset chuẩn hóa**:
   - Nhỏ (Small): $N = 100$
   - Vừa (Medium): $N = 1.000$
   - Lớn (Large): $N = 10.000$
   - Căng thẳng (Stress): $N = 50.000+$
3. **Chạy lặp nhiều lần (Warmup & Iterations)**: Chạy vòng lặp khởi động trước khi đo lường chính thức để tránh sai số JVM JIT.
4. **Đối sánh công bằng**: So sánh các thuật toán cùng mục đích (ví dụ: Linear Search vs Binary Search, Quick Sort vs Merge Sort) trên cùng một Dataset đầu vào.
5. **So sánh Lý thuyết vs Thực nghiệm**: Đối chiếu thời gian đo thực tế với độ phức tạp lý thuyết Big-$O$.
