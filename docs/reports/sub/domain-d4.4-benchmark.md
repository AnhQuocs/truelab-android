# Báo Cáo Triển Khai: Domain D4.4 — Algorithm Benchmark Suite

> **Module**: `:core:domain`
> **Trạng thái**: Hoàn thành
> **Thuộc kế hoạch**: `docs/plans/domain-d4-plan.md` (Domain D4.4)
> **Kiểm thử**: 12/12 tests mới PASS — Toàn bộ hệ thống: 374/374 tests PASS

---

## 1. Tổng Quan Mục Tiêu & Phạm Vi (Scope)

Sub-phase **Domain D4.4** triển khai bộ công cụ đo lường và đánh giá hiệu năng thuật toán thuần túy (**Algorithm Benchmark Suite**) chạy trên nền tảng Pure Kotlin/JVM trong `:core:domain`.

Mục tiêu cốt lõi:
- Xây dựng bộ sinh dữ liệu giả lập có tính tất định (`SyntheticDatasetGenerator`) hỗ trợ các kích thước $N \in \{1,000; 10,000; 50,000\}$.
- Đo lường và so sánh hiệu năng của các thuật toán Tìm kiếm: **Linear Search** vs **Binary Search**.
- Đo lường và so sánh hiệu năng của các thuật toán Sắp xếp: **Quick Sort** vs **Merge Sort**.
- Cơ chế khởi động JVM Warm-up (100 iterations) giúp ổn định JIT Compiler trước khi ghi nhận đo lường chính thức.
- Tính toán thời gian thực thi trung bình (`executionTimeMs`) và hệ số tăng tốc (`speedupFactor`) một cách an toàn số học.

---

## 2. Kiến Trúc Bộ Benchmark (Benchmark Architecture)

```text
SyntheticDatasetGenerator (Seed = 42L)
         │
         ├── generateSortedIntList(N)  ──► Search Benchmark (Linear vs Binary)
         └── generateRandomIntList(N)  ──► Sort Benchmark (Quick vs Merge)
         │
         ▼
RunAlgorithmBenchmarkUseCase
   ├─ 1. JVM Warm-up (100 iterations on N = 200)
   ├─ 2. Search Benchmarks (N = 1K, 10K, 50K - 5 iterations each):
   │      ├─ Target key: dataset[N / 2] (deterministic)
   │      ├─ Measure LinearSearch.search() with System.nanoTime()
   │      ├─ Measure BinarySearch.search() with System.nanoTime()
   │      └─ ComparisonBenchmarkResult(speedupFactor, fasterAlgorithm)
   ├─ 3. Sort Benchmarks (N = 1K, 10K, 50K - 5 iterations each):
   │      ├─ Measure QuickSort.sort() with System.nanoTime()
   │      ├─ Measure MergeSort.sort() with System.nanoTime()
   │      └─ ComparisonBenchmarkResult(speedupFactor, fasterAlgorithm)
   │
   ▼
Output: ComprehensiveBenchmarkSuiteResult
```

---

## 3. Mô Hình Dữ Liệu (Domain Models)

Vị trí: [`core/domain/.../benchmark/model/BenchmarkModels.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/model/BenchmarkModels.kt)

```kotlin
data class BenchmarkMeasurement(
    val algorithmName: String,
    val algorithmType: String, // "SEARCH" hoặc "SORT"
    val datasetSize: Int,      // 1000, 10000, 50000
    val executionTimeMs: Double,
    val iterationsRun: Int
)

data class ComparisonBenchmarkResult(
    val algorithmA: BenchmarkMeasurement,
    val algorithmB: BenchmarkMeasurement,
    val speedupFactor: Double,
    val fasterAlgorithm: String
)

data class ComprehensiveBenchmarkSuiteResult(
    val searchBenchmarks: List<ComparisonBenchmarkResult>,
    val sortBenchmarks: List<ComparisonBenchmarkResult>,
    val timestamp: Long
)
```

---

## 4. Bộ Sinh Dữ Liệu Giả Lập (`SyntheticDatasetGenerator`)

Vị trí: [`core/domain/.../benchmark/generator/SyntheticDatasetGenerator.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/generator/SyntheticDatasetGenerator.kt)

- **Hạt giống cố định**: `DEFAULT_SEED = 42L` đảm bảo tính tái lập 100% (Reproducible) giữa các lần chạy.
- `generateRandomIntList(size, seed)`: Sinh danh sách số nguyên ngẫu nhiên trong đoạn $[0, 1,000,000)$.
- `generateSortedIntList(size, seed)`: Sinh danh sách số nguyên và sắp xếp tăng dần (`.sorted()`), đảm bảo hợp đồng tiên quyết cho `BinarySearch`.

---

## 5. Phương Pháp Đo Lường & Ngữ Nghĩa Tăng Tốc (Timing & Speedup Semantics)

1. **Đo thời gian**:
   - Sử dụng `System.nanoTime()` đo thời gian chạy của từng lần lặp thuật toán.
   - Thời gian trung bình: `executionTimeMs = (totalNanos / iterations) / 1_000_000.0`.
   - Không tính thời gian sinh dữ liệu hoặc thao tác phụ vào `executionTimeMs`.
   - Kết quả được tiêu thụ qua biến blackhole để ngăn chặn tối ưu hóa Dead Code Elimination của JIT.

2. **Hệ số tăng tốc (`speedupFactor`)**:
   - Nếu $timeA > 0$ và $timeB > 0$:
     $$\text{speedupFactor} = \frac{\max(timeA, timeB)}{\min(timeA, timeB)}$$
   - Thuật toán nhanh hơn được ghi nhận tương ứng trong `fasterAlgorithm` ("Linear Search", "Binary Search", "Quick Sort", "Merge Sort", hoặc "Equal").
   - Xử lý an toàn mẫu số $\le 0$ (trả về `1.0`), chống chia cho 0.

---

## 6. Giới Hạn Kỹ Thuật (Known Limitations)

- **Nhiễu thời gian JVM (JVM Timing Noise)**: Kết quả đo lường thời gian phụ thuộc vào phần cứng, tải CPU hệ điều hành, JIT compilation và hoạt động Garbage Collection (GC).
- **Không dùng timing để khẳng định tính đúng đắn (No Timing-based Correctness)**: Bộ test không assert `binaryTime < linearTime` hay định trước thuật toán nào "chiến thắng", mà chỉ kiểm tra cấu trúc dữ liệu, tính hợp lệ của chỉ số và tính xác định của thuật toán.

---

## 7. Ranh Giới Kiến Trúc (Architecture Boundary)

- Nằm hoàn toàn trong `:core:domain` (Pure Kotlin/JVM).
- Không import bất kỳ thư viện Android SDK, Compose, Room hay Retrofit nào.
- Thuật toán cốt lõi trong `:core:algorithm` (`LinearSearch`, `BinarySearch`, `QuickSort`, `MergeSort`) được giữ nguyên trạng thái Frozen (122/122 test).

---

## 8. Kết Quả Kiểm Thử (Unit Tests & Regression)

### 8.1. `SyntheticDatasetGeneratorTest` (5/5 PASS)
1. `Generator produces exact requested sizes for 1K 10K and 50K`
2. `Generator is deterministic and reproducible with identical seed`
3. `Generator produces different sequences with different seeds`
4. `Sorted dataset generator produces strictly ascending ordered list`
5. `Non positive sizes return empty lists safely`

### 8.2. `RunAlgorithmBenchmarkUseCaseTest` (7/7 PASS)
1. `Benchmark execution returns comprehensive result structure with default dataset sizes`
2. `Search benchmark measurements contain valid algorithm names types and non-negative times`
3. `Sort benchmark measurements contain valid algorithm names types and non-negative times`
4. `Empty dataset sizes list returns empty benchmark comparisons safely`
5. `Zero iterations per size is coerced to at least one iteration`
6. `Zero warmup iterations executes measurements without failure`
7. `Search and sort algorithms produce mathematically correct outputs on synthetic data`

### 8.3. Full Regression Baseline

```text
Module :core:algorithm : 122/122 passed (0 failures)
Module :core:domain    : 206/206 passed (0 failures) [156 gốc + 12 D4.1 + 14 D4.2 + 12 D4.3 + 12 D4.4]
Module :app            :  46/46  passed (0 failures)
--------------------------------------------------------------------------------
TỔNG CỘNG              : 374/374 passed (100% SUCCESS)
assembleDebug          : BUILD SUCCESSFUL
```

---

## 9. Trạng Thái Git & File Thay Đổi

- **Chưa commit / Chưa push** (Tuân thủ workflow an toàn).
- Files thay đổi:
  - `A core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/model/BenchmarkModels.kt`
  - `A core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/generator/SyntheticDatasetGenerator.kt`
  - `A core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/benchmark/usecase/RunAlgorithmBenchmarkUseCase.kt`
  - `A core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/benchmark/generator/SyntheticDatasetGeneratorTest.kt`
  - `A core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/benchmark/usecase/RunAlgorithmBenchmarkUseCaseTest.kt`
  - `A docs/reports/domain-d4.4-benchmark.md`
