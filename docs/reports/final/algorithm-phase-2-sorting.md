# Algorithm Phase 2 — Sorting Algorithms Specification & Report

Tài liệu đặc tả kỹ thuật, thiết kế contract và báo cáo nghiệm thu hồi tố (Retrospective Report) của **Algorithm Phase 2 — Sorting Algorithms** thuộc module `:core:algorithm` trong dự án **TrueLab**.

---

## 1. Mục tiêu

Xây dựng các thuật toán sắp xếp (Sorting) cốt lõi thuần túy trên nền tảng Pure Kotlin/JVM (tuyệt đối không phụ thuộc vào Android SDK hay các layer bên ngoài) nhằm:
- Cung cấp cơ chế sắp xếp dữ liệu linh hoạt dựa trên `Comparator<T>` tùy biến cho các thực thể bóng đá (Trận đấu, Bảng xếp hạng, Tỷ số, Phong độ).
- Triển khai thuật toán **QuickSort** với kỹ thuật chọn pivot tối ưu, phục vụ các tác vụ sắp xếp hiệu năng cao.
- Triển khai thuật toán **MergeSort** bảo toàn tuyệt đối tính ổn định (**Stable Sort**), đáp ứng các nghiệp vụ phân hạng đa tiêu chí (Composite Tie-breakers).
- Bảo toàn tính bất biến (Immutability), không làm biến đổi danh sách đầu vào.

---

## 2. Phạm vi Triển khai

### 2.1 Thành phần Mã nguồn (`:core:algorithm`)
- **Interface Contract**: [`SortAlgorithm.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/SortAlgorithm.kt) định nghĩa contract chung cho các thuật toán sắp xếp.
- **Lớp hiện thực QuickSort**: [`QuickSort.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/QuickSort.kt) sắp xếp nhanh chia để trị với in-place partition nội bộ.
- **Lớp hiện thực MergeSort**: [`MergeSort.kt`](../../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/MergeSort.kt) sắp xếp trộn đệ quy ổn định.
- **Package**: `dev.anhquocs.truelab.core.algorithm.sorting`.

### 2.2 Thành phần Kiểm thử (`:core:algorithm:test`)
- **Test Suite**: [`SortingAlgorithmsTest.kt`](../../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/SortingAlgorithmsTest.kt) bao phủ toàn diện các kịch bản kiểm thử cho QuickSort và MergeSort.

---

## 3. Thiết kế / Contract

### 3.1 Interface Contract
```kotlin
interface SortAlgorithm<T> {
    fun sort(dataset: List<T>, comparator: Comparator<T>): List<T>
}
```
- **Tham số Generic `T`**: Kiểu dữ liệu của phần tử trong danh sách cần sắp xếp.
- **Tham số `comparator: Comparator<T>`**: Tiêu chí so sánh thứ tự giữa 2 phần tử, hỗ trợ sắp xếp tăng dần, giảm dần hoặc kết hợp nhiều tầng tie-breakers.
- **Bảo toàn Immutability**: Hàm nhận `List<T>` và trả về một `List<T>` mới đã được sắp xếp, tuyệt đối không thay đổi mảng đầu vào.
- **Xử lý Biên (Early Return)**: Nếu `dataset.size <= 1`, thuật toán trả về ngay danh sách ban đầu mà không thực hiện chia đệ quy hay cấp phát bộ nhớ.

---

## 4. Implementation

### 4.1 `QuickSort<T>`
- **Bảo toàn danh sách gốc**: Sao chép `dataset.toMutableList()` nội bộ để thực hiện hoán vị in-place mà không làm ảnh hưởng đến collection bên ngoài.
- **Chiến lược chọn Pivot (Pivot Selection)**:
  - Chọn phần tử ở giữa `val mid = low + (high - low) / 2`.
  - Hoán vị phần tử giữa với phần tử cuối cùng `swap(array, mid, high)` trước khi phân đoạn.
  - Kỹ thuật này giúp giảm thiểu đáng kể nguy cơ rơi vào độ phức tạp xấu nhất $\mathcal{O}(n^2)$ khi tập dữ liệu đã được sắp xếp sẵn hoặc sắp xếp ngược.
- **Phân đoạn Lomuto**: Duyệt mảng từ `low` đến `high - 1`, gom các phần tử có `comparator.compare(array[j], pivot) <= 0` về bên trái, sau đó đặt pivot vào đúng vị trí phân cách.
- **Đệ quy**: Gọi đệ quy trên hai phân vùng `[low, pivotIndex - 1]` và `[pivotIndex + 1, high]`.
- **Tính chất**: Không ổn định (**Unstable Sort**).

### 4.2 `MergeSort<T>`
- **Chia để trị (Divide and Conquer)**:
  - Tính điểm chia `val mid = dataset.size / 2`.
  - Tách mảng thành hai nửa `subList(0, mid)` và `subList(mid, dataset.size)`.
  - Gọi đệ quy `sort(left, comparator)` và `sort(right, comparator)`.
- **Hợp nhất ổn định (Stable Merge)**:
  - Khởi tạo mảng kết quả `ArrayList<T>(left.size + right.size)`.
  - Sử dụng điều kiện so sánh `if (comparator.compare(left[i], right[j]) <= 0)`: Khi hai phần tử có giá trị so sánh bằng nhau (`== 0`), thuật toán luôn ưu tiên chọn phần tử từ mảng con bên trái (`left[i]`).
  - Quy tắc này bảo toàn tuyệt đối thứ tự ban đầu của các phần tử có cùng giá trị (**Stable Sort**).
  - Nối các phần tử còn thừa của mảng `left` hoặc `right` vào kết quả.

---

## 5. Testing

Bộ kiểm thử [`SortingAlgorithmsTest.kt`](../../../core/algorithm/src/test/kotlin/dev/anhquocs/truelab/core/algorithm/sorting/SortingAlgorithmsTest.kt) sử dụng đối tượng độc lập `TestItem(id: Int, value: Int)` để kiểm thử tính tổng quát và tính ổn định.

Tổng số test cases thực tế: **9 / 9 Unit Tests PASS (100%)**.

### Danh sách 9 Test Cases Chi tiết:
1. `quickSort - sorts empty list`: Danh sách rỗng trả về rỗng an toàn.
2. `mergeSort - sorts empty list`: MergeSort xử lý danh sách rỗng an toàn.
3. `quickSort - sorts single element`: Danh sách 1 phần tử trả về nguyên vẹn.
4. `quickSort - sorts unsorted list ascending by id`: Sắp xếp danh sách chưa có thứ tự tăng dần theo ID.
5. `mergeSort - sorts reverse sorted list ascending by id`: Sắp xếp danh sách bị đảo ngược hoàn toàn.
6. `quickSort - sorts identical elements without throwing error`: Mảng chứa toàn bộ các phần tử có giá trị giống nhau không gây lỗi vòng lặp.
7. `mergeSort - generic sorting descending by value`: Sắp xếp giảm dần theo trường `value` bằng Custom Comparator.
8. `mergeSort - is STABLE (preserves original order of equal items)`: Kiểm thử tính ổn định với mảng có nhiều phần tử cùng `value = 10` nhưng khác `id` (`1, 3, 5`), khẳng định thứ tự `id` sau khi sắp xếp giảm dần theo `value` được giữ nguyên vẹn là `1 -> 3 -> 5`.
9. `quickSort - can handle large random dataset without StackOverflow`: Kiểm thử sức chịu tải với tập dữ liệu ngẫu nhiên $N = 10.000$ phần tử, kiểm tra tính đúng đắn theo độ phức tạp $\mathcal{O}(n)$ sau sắp xếp và đảm bảo không xảy ra `StackOverflowError`.

---

## 6. Độ phức tạp (Complexity Analysis)

| Thuật toán | Thời gian Tốt nhất (Best Case) | Thời gian Trung bình (Average Case) | Thời gian Xấu nhất (Worst Case) | Không gian Bổ sung (Auxiliary Space) | Tính Ổn định (Stability) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **QuickSort** | $\mathcal{O}(n \log n)$ | $\mathcal{O}(n \log n)$ | $\mathcal{O}(n^2)$ | $\mathcal{O}(\log n)$ (Stack đệ quy) + $\mathcal{O}(n)$ (List sao chép) | ❌ Unstable |
| **MergeSort** | $\mathcal{O}(n \log n)$ | $\mathcal{O}(n \log n)$ | $\mathcal{O}(n \log n)$ | $\mathcal{O}(n)$ (Mảng đệm hợp nhất) |  **Stable** |

---

## 7. Kết quả & Ứng dụng Tiếp theo

- **Trạng thái**: **HOÀN THÀNH TOÀN DIỆN (FROZEN)**.
- **Tích hợp Domain**: Thuật toán Phase 2 được tích hợp vào Domain D3 thông qua:
  - `SortMatchesUseCase`: Sắp xếp danh sách trận đấu theo thời gian, tổng bàn thắng, hiệu số bàn thắng.
  - `SortSeasonRankingUseCase`: Sắp xếp bảng xếp hạng giải đấu với chuỗi tie-breakers 4 tầng (Điểm số $\to$ Hiệu số bàn thắng $\to$ Số trận thắng $\to$ Thứ hạng ban đầu) dựa trên tính chất Stable của MergeSort.
- **Tích hợp Benchmark**: Được đưa vào bộ đo hiệu năng thuật toán tại Domain D4.4 và giao diện Presentation P2.1 (`BenchmarkScreen`).

---

## 8. Ghi chú Lịch sử (Retrospective Note)

Tài liệu này là **Báo cáo Nghiệm thu Hồi tố (Retrospective Final Report)** được tạo bổ sung nhằm chuẩn hóa hệ thống tài liệu dự án TrueLab theo cấu trúc phân tầng `docs/reports/final/`. Mọi thông số kỹ thuật và test case trong tài liệu này đều được đối chiếu và xác thực trực tiếp từ mã nguồn và test suite thực tế của dự án.
