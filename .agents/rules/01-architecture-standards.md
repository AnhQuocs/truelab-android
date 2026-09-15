# Tiêu chuẩn Kiến trúc & Phân tầng (Architecture Standards)

## 1. Bản đồ Phụ thuộc Một chiều (Unidirectional Dependency Graph)

Hệ thống TrueLab được xây dựng theo kiến trúc đa module nghiêm ngặt:

```text
                    :app
                  /  |  \
                 ↓   ↓   ↓
       :core:data   :core:ui
            ↓
       :core:domain
            ↓
     :core:algorithm
```

### Quy tắc Bất biến của Phụ thuộc:
1. **`:app`** phụ thuộc vào `:core:data`, `:core:ui`, `:core:domain`, và `:core:algorithm`.
2. **`:core:data`** CHỈ ĐƯỢC PHÉP phụ thuộc vào `:core:domain`. Tuyệt đối không phụ thuộc `:core:ui` hoặc `:app`.
3. **`:core:domain`** CHỈ ĐƯỢC PHÉP phụ thuộc vào `:core:algorithm` (+ thư viện Kotlin thuần như `kotlinx.coroutines.core`). Tuyệt đối KHÔNG phụ thuộc `:core:data`, `:core:ui` hay `:app`.
4. **`:core:algorithm`** là **Pure Kotlin/JVM Module**, không có bất kỳ phụ thuộc nào vào các module khác hoặc Android SDK.
5. **`:core:ui`** là Android Library cung cấp Design System, không phụ thuộc vào `:core:data` hay `:core:domain`.
6. **Tuyệt đối cấm tạo Circular Dependency** (phụ thuộc vòng tròn).

---

## 2. Phân tầng Clean Architecture & Trách nhiệm

```text
Tầng Trình diễn (Presentation Layer: :app, :core:ui)
         ↓
Tầng Nghiệp vụ (Domain Layer: :core:domain)
      ↓                   ↓
Tầng Dữ liệu (Data Layer)   Tầng Thuật toán (Algorithm Layer)
(:core:data)                (:core:algorithm)
```

### 2.1. Presentation Layer (`:app`, `:core:ui`)
- Chứa Composables (Màn hình), ViewModels, UI State (`StateFlow`), Navigation graphs.
- **Quy tắc MVVM**:
  - `Composable` ➔ `ViewModel` ➔ `UseCase` ➔ `Repository`.
  - UI State phải là bất biến (immutable) và được expose qua `StateFlow<UiState>`.
  - Composable chỉ làm nhiệm vụ render giao diện và gửi user event/intent tới ViewModel.
  - Composable **KHÔNG** chứa business logic, tính toán nặng hoặc truy cập dữ liệu trực tiếp.
  - ViewModel **KHÔNG** được trực tiếp gọi Retrofit Service, Room DAO hoặc import DTO/Entity.

### 2.2. Domain Layer (`:core:domain`)
- Chứa Domain Model (`data class`), Repository Interface (`interface`), UseCase (`class ...UseCase`).
- **Quy tắc Pure Kotlin**:
  - 100% Kotlin thuần, không import `android.*`, `androidx.*`.
  - Mỗi UseCase chỉ giải quyết một nhiệm vụ duy nhất (Single Responsibility) và khai báo `operator fun invoke(...)`.
  - Luồng dữ liệu stream bất đồng bộ phải dùng Kotlin `Flow`.

### 2.3. Data Layer (`:core:data`)
- Chứa Retrofit API Service, Room Database, Entity, DAO, Repository Implementation, Data Crawler / Sync Engine.
- **Quy tắc Data**:
  - Thực thi (implement) các Repository interface định nghĩa từ `:core:domain`.
  - Toàn bộ việc ánh xạ dữ liệu (DTO ↔ Entity ↔ Domain Model) phải diễn ra bên trong Data layer.
  - Room Entity và DTO tuyệt đối **KHÔNG ĐƯỢC LỌT** ra tầng Domain hoặc Presentation.

### 2.4. Algorithm Layer (`:core:algorithm`)
- Chứa các thuật toán toán học, phân tích và thống kê thuần túy:
  - `searching/` (Linear Search, Binary Search)
  - `sorting/` (Quick Sort, Merge Sort)
  - `statistics/` (Descriptive Statistics: Mean, Median, Variance, StdDev)
  - `trend/` (Moving Average: SMA, EMA)
  - `evaluation/` (Form Score)
  - `rating/` (Elo Rating System)
  - `prediction/` (Weighted Scoring)
- **Quy tắc Algorithm**:
  - 100% Pure Kotlin/JVM. Cấm tuyệt đối import Android SDK, Jetpack Compose, Room, Retrofit, Hilt Android, Context, Activity, Fragment.
  - Thuật toán phải là hàm thuần (pure function) hoặc class có input/output rõ ràng, có thể benchmark và viết Unit Test độc lập.
