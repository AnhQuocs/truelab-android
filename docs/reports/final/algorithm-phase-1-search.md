# Algorithm Phase 1 — Searching Algorithms Specification & Report

Tài liệu đặc tả kỹ thuật, thiết kế contract và báo cáo nghiệm thu hồi tố (Retrospective Report) của **Algorithm Phase 1 — Searching Algorithms** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Mục tiêu

Xây dựng các thuật toán tìm kiếm (Searching) cốt lõi thuần túy trên nền tảng Pure Kotlin/JVM (tuyệt đối không phụ thuộc vào Android SDK hay các layer bên ngoài) nhằm:
- Cung cấp cơ chế tra cứu chính xác (Exact Match) và tra cứu linh hoạt theo điều kiện (Partial Match/Filter) trên các tập dữ liệu tổng quát (Generic).
- Tối ưu hóa tra cứu với độ phức tạp thời gian $\mathcal{O}(\log n)$ khi dữ liệu đã được sắp xếp có thứ tự (Binary Search).
- Cung cấp giải pháp tìm kiếm tuần tự $\mathcal{O}(n)$ an toàn trên tập dữ liệu chưa có thứ tự hoặc tìm kiếm theo chuỗi con (Linear Search).
- Bảo toàn tính bất biến (Immutability) của tập dữ liệu đầu vào.

---

## 2. Phạm vi Triển khai

### 2.1 Thành phần Mã nguồn (`:core:algorithm`)
- **Interface Contract**: [`SearchAlgorithm.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/searching/SearchAlgorithm.kt) định nghĩa contract chuẩn cho các thuật toán tìm kiếm.
- **Lớp hiện thực Linear Search**: [`LinearSearch.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/searching/LinearSearch.kt) hỗ trợ tìm kiếm chính xác và tìm kiếm cục bộ/predicate.
- **Lớp hiện thực Binary Search**: [`BinarySearch.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/searching/BinarySearch.kt) triển khai tìm kiếm nhị phân chia để trị.
- **Package**: `dev.anhquocs.truelab.core.algorithm.searching`.

### 2.2 Thành phần Kiểm thử (`:core:algorithm:test`)
- **Test Suite**: [`SearchingAlgorithmsTest.kt`](../../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/searching/SearchingAlgorithmsTest.kt) bao phủ toàn diện các trường hợp kiểm thử cho Linear Search và Binary Search.

---

## 3. Thiết kế / Contract

### 3.1 Interface Contract
```kotlin
interface SearchAlgorithm<T, K> {
    fun search(dataset: List<T>, target: K, selector: (T) -> K): Int
}
```
- **Tham số Generic**:
  - `T`: Kiểu dữ liệu của phần tử trong danh sách (Dataset).
  - `K`: Kiểu dữ liệu của khóa tìm kiếm (Target Key).
- **Hàm `selector: (T) -> K`**: Trích xuất khóa tìm kiếm từ phần tử kiểu `T`, cho phép tìm kiếm trên bất kỳ cấu trúc dữ liệu hoặc đối tượng nghiệp vụ nào mà không cần sửa đổi thuật toán.
- **Giá trị trả về**: Trả về `index: Int` (0-indexed) của phần tử nếu tìm thấy; trả về `-1` nếu không tìm thấy hoặc nếu danh sách đầu vào rỗng.

### 3.2 Mở rộng của `LinearSearch`
`LinearSearch<T, K>` kế thừa `SearchAlgorithm<T, K>` và cung cấp thêm hai hàm tìm kiếm theo điều kiện logic (Predicate):
- `fun searchPartial(dataset: List<T>, predicate: (T) -> Boolean): Int`: Trả về vị trí của phần tử đầu tiên thỏa mãn điều kiện `predicate`, hoặc `-1` nếu không có phần tử nào thỏa mãn.
- `fun searchAllPartial(dataset: List<T>, predicate: (T) -> Boolean): List<Int>`: Trả về danh sách tất cả các chỉ số (Indices) thỏa mãn điều kiện phục vụ các bộ lọc đa phần tử.

### 3.3 Ràng buộc của `BinarySearch`
- `BinarySearch<T, K : Comparable<K>>`: Ràng buộc kiểu khóa `K` phải thực thi giao diện `Comparable<K>` để phục vụ so sánh thứ tự.
- **Tiền đề bắt buộc**: Danh sách đầu vào `dataset` phải được sắp xếp tăng dần theo tiêu chí của `selector` trước khi gọi `search()`.

---

## 4. Implementation

### 4.1 `LinearSearch`
- Duyệt tuần tự từ vị trí `0` đến `dataset.size - 1`.
- So sánh `selector(dataset[i]) == target` (hoặc kiểm tra `predicate(dataset[i])`).
- Trả về ngay lập tức chỉ số `i` khi gặp phần tử đầu tiên khớp điều kiện, đảm bảo tính chất First Occurrence khi có phần tử trùng lặp.
- Không cấp phát bộ nhớ phụ trong quá trình tìm kiếm đơn lẻ.

### 4.2 `BinarySearch`
- Khởi tạo hai con trỏ biên: `left = 0`, `right = dataset.size - 1`.
- Vòng lặp `while (left <= right)`:
  - Tính điểm giữa an toàn: `val mid = left + (right - left) / 2` (ngăn ngừa nguy cơ tràn số nguyên 32-bit khi kích thước mảng lớn).
  - So sánh `midVal.compareTo(target)`:
    - Nếu bằng 0: Tìm thấy phần tử, trả về `mid`.
    - Nếu nhỏ hơn 0: `left = mid + 1` (khóa cần tìm nằm ở nửa phải).
    - Nếu lớn hơn 0: `right = mid - 1` (khóa cần tìm nằm ở nửa trái).
- Kết thúc vòng lặp mà không tìm thấy: Trả về `-1`.

---

## 5. Testing

Bộ kiểm thử [`SearchingAlgorithmsTest.kt`](../../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/searching/SearchingAlgorithmsTest.kt) sử dụng đối tượng độc lập `TestTeam(id: Int, name: String, points: Double)` để kiểm thử tính tổng quát, bảo đảm không import model từ các layer khác.

Tổng số test cases thực tế: **12 / 12 Unit Tests PASS (100%)**.

### 5.1 Nhóm kiểm thử Linear Search (5 tests)
1. `linearSearch - returns -1 for empty list`: Danh sách rỗng trả về `-1`.
2. `linearSearch - returns correct index for exact match`: Tìm thấy phần tử chính xác trong danh sách chưa sắp xếp.
3. `linearSearch - returns first occurrence for duplicate values`: Khi có nhiều phần tử trùng khóa, trả về vị trí xuất hiện đầu tiên.
4. `linearSearch - returns -1 when target not found`: Khóa không tồn tại trong danh sách trả về `-1`.
5. `linearSearch - searchPartial - finds string containing keyword`: Tìm kiếm chuỗi con theo điều kiện `contains()`.

### 5.2 Nhóm kiểm thử Binary Search (7 tests)
1. `binarySearch - returns -1 for empty list`: Danh sách rỗng trả về `-1`.
2. `binarySearch - returns correct index in a sorted list`: Tìm thấy phần tử chính xác ở giữa mảng đã sắp xếp.
3. `binarySearch - returns correct index for first element`: Tìm phần tử đầu tiên (index 0).
4. `binarySearch - returns correct index for last element`: Tìm phần tử cuối cùng (index size - 1).
5. `binarySearch - returns -1 when target is smaller than all elements (Out of bounds left)`: Khóa nhỏ hơn toàn bộ mảng.
6. `binarySearch - returns -1 when target is greater than all elements (Out of bounds right)`: Khóa lớn hơn toàn bộ mảng.
7. `binarySearch - returns -1 when target not found within bounds`: Khóa nằm trong khoảng giá trị nhưng không tồn tại trong mảng số thực.

---

## 6. Độ phức tạp (Complexity Analysis)

| Thuật toán | Thao tác | Độ phức tạp Thời gian (Best / Average / Worst) | Không gian Bổ sung (Auxiliary Space) |
| :--- | :--- | :---: | :---: |
| **Linear Search** | `search()` / `searchPartial()` | $\mathcal{O}(1) \;/\; \mathcal{O}(n) \;/\; \mathcal{O}(n)$ | $\mathcal{O}(1)$ |
| **Linear Search** | `searchAllPartial()` | $\mathcal{O}(n) \;/\; \mathcal{O}(n) \;/\; \mathcal{O}(n)$ | $\mathcal{O}(k)$ ($k$ là số kết quả khớp) |
| **Binary Search** | `search()` | $\mathcal{O}(1) \;/\; \mathcal{O}(\log n) \;/\; \mathcal{O}(\log n)$ | $\mathcal{O}(1)$ (Vòng lặp không đệ quy) |

---

## 7. Kết quả & Ứng dụng Tiếp theo

- **Trạng thái**: **HOÀN THÀNH TOÀN DIỆN (FROZEN)**.
- **Tích hợp Domain**: Thuật toán Phase 1 được tích hợp vào Domain D3 thông qua `SearchMatchesUseCase` và `SearchTeamsUseCase`.
- **Tích hợp Benchmark**: Được đưa vào bộ đo hiệu năng thuật toán tại Domain D4.4 và giao diện Presentation P2.1 (`BenchmarkScreen`).

---

## 8. Ghi chú Lịch sử (Retrospective Note)

Tài liệu này là **Báo cáo Nghiệm thu Hồi tố (Retrospective Final Report)** được tạo bổ sung nhằm chuẩn hóa hệ thống tài liệu dự án TrueLab theo cấu trúc phân tầng `docs/reports/final/`. Mọi thông số kỹ thuật và test case trong tài liệu này đều được đối chiếu và xác thực trực tiếp từ mã nguồn và test suite thực tế của dự án.
