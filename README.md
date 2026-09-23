# ⚽ TrueLab — Sports Data Analytics & Prediction

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-FF6F00?style=for-the-badge)
![Database](https://img.shields.io/badge/Storage-Room%20DB-00599C?style=for-the-badge&logo=sqlite&logoColor=white)

<br/>

**Đồ án môn học:** Thuật toán ứng dụng  
**Tên đề tài:** *Ứng dụng thuật toán trong phân tích dữ liệu thể thao đa nguồn*  
**Repository:** `truelab-android` • **Package:** `dev.anhquocs.truelab` • **Version:** `1.0`

</div>

---

## 📋 Thông tin tổng quan dự án

| Thuộc tính | Chi tiết |
| :--- | :--- |
| **Ứng dụng** | **TrueLab** (Sports Data Analytics & Prediction) |
| **Loại đồ án** | Academic Project (Đồ án học thuật môn *Thuật toán ứng dụng*) |
| **Nền tảng** | Android Native (Kotlin + Jetpack Compose) |
| **Kiến trúc** | Clean Architecture (Presentation, Domain, Data) + MVVM |
| **Lưu trữ dữ liệu** | Room Database (Offline-first architecture) |
| **Nhóm sinh viên thực hiện** | • **Bùi Anh Quốc** — *System Architect & Mobile Lead*<br/>• **Hà Mạnh Long** — *Algorithm Analyst & QA Engineer* |
| **Phân công nhiệm vụ** | Xem chi tiết tại [docs/PHAN_CONG_CONG_VIEC.md](docs/PHAN_CONG_CONG_VIEC.md) |
| **Mục tiêu cốt lõi** | Phân tích phong độ, thống kê dữ liệu đa nguồn, đánh giá Elo, thử nghiệm dự đoán kết quả và benchmark thuật toán |

---

# 📖 1. Giới thiệu (Introduction)

## 1.1. Mục tiêu (Purpose)
**TrueLab** là ứng dụng Android phục vụ việc **thu thập, lưu trữ, phân tích và trực quan hóa dữ liệu thể thao đa nguồn**, đồng thời áp dụng các thuật toán kinh điển và hiện đại để phân tích phong độ đội bóng và thử nghiệm mô hình dự đoán kết quả trận đấu.

Ứng dụng được xây dựng nhằm minh họa trực quan việc áp dụng các thuật toán vào bài toán dữ liệu thực tế:
* 🔄 **Xử lý & hợp nhất dữ liệu:** Data cleaning, normalization, deduplication từ nhiều nguồn.
* 🔍 **Tìm kiếm & Sắp xếp:** Áp dụng và benchmark các thuật toán tìm kiếm (*Linear Search, Binary Search*) và sắp xếp (*Quick Sort, Merge Sort*).
* 📈 **Thống kê chuyên sâu:** Tính toán các chỉ số thống kê mô tả (*Mean, Median, Min/Max, Variance, Standard Deviation*).
* ⚡ **Đánh giá sức mạnh & phong độ:** Xây dựng Form Score, phân tích Home/Away, phân tích đối đầu trực tiếp (H2H) và hệ thống xếp hạng **Elo Rating**.
* 📊 **Phân tích biến động Odds:** Theo dõi độ biến động (*Volatility*), chênh lệch giữa các nhà cung cấp, xu hướng (*Moving Average*).
* 🎯 **Mô hình dự đoán (Prediction):** Áp dụng mô hình tính điểm trọng số (*Weighted Scoring*) và mở rộng mô hình máy học (*Logistic Regression, Decision Tree*).
* ⏱️ **Benchmark hiệu năng:** Đo lường thời gian thực thi, mức tiêu thụ bộ nhớ và độ chính xác theo quy mô dữ liệu.

---

## 1.2. Phạm vi (Scope)
Dữ liệu tập trung vào môn bóng đá, bao gồm:
- 📌 Thông tin đội bóng & giải đấu.
- 📅 Lịch thi đấu & kết quả trận đấu theo thời gian thực / lịch sử.
- 💰 Dữ liệu Odds từ nhiều nhà cung cấp (Providers / Companies).
- 🕒 Lịch sử biến động tỷ lệ kèo (Odds movement snapshots).
- 🔮 Kết quả dự đoán & ma trận đánh giá thuật toán.

> [!CAUTION]
> ### ⚠️ Tuyên bố miễn trừ trách nhiệm (Non-scope)
> **TrueLab KHÔNG PHẢI là ứng dụng cá cược.**  
> Ứng dụng **hoàn toàn không hỗ trợ**:
> - ❌ Đặt cược, nạp tiền, rút tiền hoặc giao dịch tài chính.
> - ❌ Chuyển hướng liên kết đến các trang cá cược / nhà cung cấp tỷ lệ.
> - ❌ Quản lý tài khoản cá cược.
> 
> *Dữ liệu Odds được sử dụng thuần túy như một nguồn dữ liệu xác suất thị trường phục vụ nghiên cứu khoa học và thuật toán.*

---

# 🏛️ 2. Kiến trúc hệ thống (Product Overview)

## 2.1. Sơ đồ kiến trúc tổng quan (High-level Architecture)

```text
                  DATA SOURCES
                       │
             ┌─────────┴─────────┐
             │                   │
          REST API           Other Data
             │                   │
             └─────────┬─────────┘
                       ▼
                Data Collector
                       │
                       ▼
                  Raw Dataset
                       │
                 Preprocessing
                       │
                       ▼
                 Local Storage
                    (Room)
                       │
                       ▼
                    TrueLab
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
     Analysis       Algorithms     Prediction
        │              │              │
        └──────────────┼──────────────┘
                       ▼
                 Visualization
```

* **Data Collection Layer:** Thu thập dữ liệu từ các REST API hợp lệ, lưu vào Raw Dataset.
* **Offline-First:** Room Database đóng vai trò là single-source-of-truth cho toàn bộ ứng dụng.
* **MQTT:** Không nằm trong core scope của Android app, có thể hỗ trợ ở data collection layer nếu cần streaming.

---

# 👥 3. Đối tượng sử dụng (Users)

```text
┌────────────────────────────────────────────────────────┐
│                        TRUELAB                         │
├───────────────────────────┬────────────────────────────┤
│   🎓 Student / Researcher │   💻 Developer / Evaluator │
├───────────────────────────┼────────────────────────────┤
│ • Xem dữ liệu & phân tích │ • Kiểm tra thuật toán      │
│ • Tìm kiếm đội / trận đấu │ • Chạy Benchmark hiệu năng │
│ • Phân tích biến động Odds│ • Đánh giá mô hình dự đoán │
│ • So sánh thuật toán      │ • Kiểm thử tính toàn vẹn   │
└───────────────────────────┴────────────────────────────┘
```

---

# ⚙️ 4. Yêu cầu chức năng (Functional Requirements)

| Mã YC | Chức năng | Chi tiết kỹ thuật |
| :--- | :--- | :--- |
| **FR-01** | **Quản lý Dataset** | Quản lý đầy đủ các thực thể: `Team`, `Match`, `Odds`, `Provider`, `Prediction`. |
| **FR-02** | **Đồng bộ dữ liệu** | Quy trình: `Fetch -> Validate -> Normalize -> Deduplicate -> Local DB`. Chạy offline sau khi đồng bộ. |
| **FR-03** | **Lưu trữ cục bộ** | Sử dụng **Room Database** lưu trữ quan hệ và phục vụ truy vấn thuật toán tốc độ cao. |
| **FR-04** | **Tìm kiếm** | Hỗ trợ tìm kiếm Đội, Trận, Giải đấu bằng **Linear Search** & **Binary Search** kèm benchmark. |
| **FR-05** | **Sắp xếp** | Hỗ trợ sắp xếp đa tiêu chí với **Quick Sort** & **Merge Sort**, đo lường thời gian thực thi. |
| **FR-06** | **Thống kê mô tả** | Tính *Mean, Median, Min, Max, Variance, Standard Deviation* cho bàn thắng, tỷ lệ thắng, odds. |
| **FR-07** | **Phân tích phong độ** | Tính toán chuỗi trận gần nhất, bàn thắng/thua, hiệu số và **Form Score** (Thắng=3, Hòa=1, Thua=0). |
| **FR-08** | **Sân nhà / Sân khách** | Đánh giá hiệu suất riêng biệt: Tỷ lệ thắng sân nhà/khách, số bàn thắng trung bình, điểm số/trận. |
| **FR-09** | **Lịch sử đối đầu (H2H)** | Thống kê lịch sử chạm trán giữa 2 đội (Số trận, Thắng - Hòa - Thua, tổng bàn thắng). |
| **FR-10** | **Xếp hạng Elo** | Thuật toán **Elo Rating** cập nhật động sau mỗi trận đấu để phản ánh sức mạnh thực tế của đội bóng. |
| **FR-11** | **Phân tích Odds đa nguồn** | So sánh tỷ lệ kèo giữa các Provider (Odds trung bình, cao nhất, thấp nhất, độ lệch chuẩn). |
| **FR-12** | **Xu hướng Odds** | Phân tích biến động kèo theo chuỗi thời gian: *Opening vs Current, Moving Average, Volatility*. |
| **FR-13** | **Dự đoán kết quả** | Tính toán xác suất: **Home Win (%) / Draw (%) / Away Win (%)** dựa trên tổ hợp đặc trưng. |
| **FR-14** | **Thuật toán Weighted Scoring** | Mô hình trọng số kết hợp: *Form (25%), Elo (20%), Goals (15%), Odds (20%), H2H (10%), Home Advantage (10%)*. |
| **FR-15** | **Mở rộng Machine Learning** | Thử nghiệm mô hình **Logistic Regression** và **Decision Tree** để so sánh với Weighted Scoring. |
| **FR-16** | **Benchmark thuật toán** | Đo đạc và đối chiếu: *Execution Time (ms), Memory Usage (MB), Input Size (N), Accuracy (%)*. |
| **FR-17** | **Trực quan hóa (Charts)** | Vẽ biểu đồ trực quan: *Line Chart, Bar Chart, Donut Chart* cho phong độ, Elo, Odds và xác suất. |

---

# 🛡️ 5. Yêu cầu phi chức năng (Non-functional Requirements)

```text
  [NFR-01] Hiệu năng (Performance)      ──► Xử lý mượt mà dataset quy mô 10,000+ matches
  [NFR-02] Hoạt động ngoại tuyến (Offline) ──► Tìm kiếm, sắp xếp, thống kê, dự đoán không cần mạng
  [NFR-03] Độ tin cậy (Reliability)      ──► Không crash khi mất mạng, tự động fallback về Room DB
  [NFR-04] Cấu trúc chuẩn (Clean Arch)   ──► Presentation (Compose) -> Domain -> Data (Room/Retrofit)
  [NFR-05] Toàn vẹn dữ liệu (Integrity)  ──► Kiểm tra ràng buộc khóa ngoại (Foreign Keys) trước khi lưu
```

---

# 🗄️ 6. Thiết kế cơ sở dữ liệu (Database Schema)

```text
┌──────────────────────┐              ┌──────────────────────┐
│     TeamEntity       │              │    ProviderEntity    │
├──────────────────────┤              ├──────────────────────┤
│ PK  teamId: String   │              │ PK  companyId: String│
│     name: String     │              │     name: String     │
│     leagueId: String │              │     shortName: String│
│     leagueName: String              └──────────┬───────────┘
│     country: String  │                         │
└──────────┬───────────┘                         │
           │                                     │
           ▼                                     ▼
┌────────────────────────────────────────────────────────────┐
│                        MatchEntity                         │
├────────────────────────────────────────────────────────────┤
│ PK  matchId: String                                        │
│ FK  homeTeamId: String ──► TeamEntity.teamId               │
│ FK  awayTeamId: String ──► TeamEntity.teamId               │
│     matchTime: Long                                        │
│     homeScore: Int?                                        │
│     awayScore: Int?                                        │
│     status: String                                         │
└──────────┬─────────────────────────────────────────────────┘
           │
           ├─────────────────────────────────────┐
           ▼                                     ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│             OddsEntity               │  │           PredictionEntity           │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ PK  id: Long (Auto)                  │  │ PK  id: Long (Auto)                  │
│ FK  matchId: String ──► MatchEntity  │  │ FK  matchId: String ──► MatchEntity  │
│ FK  companyId: String ──► Provider   │  │     algorithm: String                │
│     oddsType: String                 │  │     homeProb: Double                 │
│     changeTime: Long                 │  │     drawProb: Double                 │
│     homeWin: Double                  │  │     awayProb: Double                 │
│     draw: Double                     │  │     actualResult: String?            │
│     awayWin: Double                  │  │     createdAt: Long                  │
│     over / under / handicap: Double? │  └──────────────────────────────────────┘
└──────────────────────────────────────┘
```

---

# 📱 7. Cấu trúc màn hình ứng dụng (Application Screens)

Ứng dụng gồm **6 màn hình chức năng**, được phân bổ khoa học giữa thanh điều hướng đáy (**Bottom Navigation**) và các luồng điều hướng chuyên sâu (**Secondary Screens**):

```text
TrueLab Navigation Architecture
│
├── 📱 Bottom Navigation Destinations (4 Tab chính)
│   ├── 🏠 Home Screen           ── Màn hình mặc định, thống kê tổng quan dataset & Core Feature Cards điều hướng
│   ├── ⚽ Matches Screen        ── Danh sách trận đấu, bộ lọc, tìm kiếm, sắp xếp đa tiêu chí & Match Detail
│   ├── 🛡️ Teams Screen          ── Thông tin đội bóng, Form Score, thống kê sân nhà/khách, H2H & Elo Rating
│   └── 📈 Analytics Screen      ── Thống kê mô tả toàn diện, so sánh Odds đa nguồn & biểu đồ xu hướng
│
└── 🧭 Secondary Screens (Điều hướng chuyên sâu từ Home Screen)
    ├── 🔮 Prediction Screen     ── Flow: Chọn trận đấu ➔ Trích xuất đặc trưng ➔ Chạy thuật toán ➔ Xác suất dự đoán
    └── ⏱️ Benchmark Screen      ── So sánh hiệu năng các thuật toán Search, Sort & Prediction Accuracy
```

### 7.1. Bottom Navigation Destinations (4 Tab chính)

1. **🏠 Home Screen (Màn hình mặc định):**
   * Hiển thị bảng điều khiển tổng quan về Dataset: số đội bóng, số trận đấu, số nhà cung cấp dữ liệu, phiên bản Room DB và lần cập nhật cuối.
   * Chứa các **Core Feature Cards** để điều hướng trực tiếp đến các tính năng chuyên sâu (*Prediction Screen* và *Benchmark Screen*).
2. **⚽ Matches Screen:**
   * Tab chính trên Bottom Navigation để duyệt toàn bộ tập dữ liệu trận đấu.
   * Tích hợp thanh tìm kiếm (Linear/Binary Search), bộ lọc nâng cao theo giải đấu/mùa giải, sắp xếp đa tiêu chí (QuickSort/MergeSort) và xem chi tiết trận đấu (Match Detail).
3. **🛡️ Teams Screen:**
   * Tab chính trên Bottom Navigation hiển thị hồ sơ phân tích từng đội bóng.
   * Đánh giá điểm phong độ (**Form Score** 5 trận gần nhất), thống kê hiệu suất sân nhà/sân khách (**Home/Away Splits**), ma trận đối đầu trực tiếp (**H2H**) và hệ số **Elo Rating**.
4. **📈 Analytics Screen:**
   * Tab chính trên Bottom Navigation chuyên biệt cho phân tích khám phá dữ liệu (EDA).
   * Cung cấp thống kê mô tả toàn diện (Mean, Median, Std Dev, Variance, Skewness), ma trận so sánh Odds đa nguồn từ các nhà cung cấp và biểu đồ xu hướng đường trung bình trượt (**Moving Average**).

### 7.2. Secondary Screens (Màn hình chức năng chuyên sâu)

5. **🔮 Prediction Screen:**
   * **Không nằm trên Bottom Navigation** — Được mở từ Home Screen thông qua nút/card **"Dự đoán"**.
   * **Luồng xử lý (Flow):** Chọn trận đấu $\rightarrow$ Trích xuất đặc trưng $\rightarrow$ Chạy thuật toán dự đoán $\rightarrow$ Hiển thị xác suất và kết quả dự đoán.
6. **⏱️ Benchmark Screen:**
   * **Không nằm trên Bottom Navigation** — Được mở từ Home Screen thông qua nút/card **"Benchmark"**.
   * Dùng để đo lường và so sánh trực tiếp hiệu năng các thuật toán Search (Linear vs. Binary), Sort (QuickSort vs. MergeSort) và độ chính xác mô hình dự đoán (Prediction Accuracy).

---

# 🔄 8. Pipeline dữ liệu (Data Pipeline) & Cập nhật Dataset

```text
    [Remote Sources: REST API / Optional MQTT]
                       │
                       ▼
                 [Raw Dataset]
                       │
                       ▼
          [Data Cleaning & Validation]
                       │
                       ▼
           [Normalization & Mapping]
                       │
                       ▼
         [Deduplication & Conflict Res]
                       │
                       ▼
          [Local Storage: Room Database]
                       │
      ┌────────────────┼────────────────┐
      ▼                ▼                ▼
[Search / Sort]  [Data Analytics]  [Match Prediction]
      │                │                │
      └────────────────┼────────────────┘
                       ▼
         [Jetpack Compose Visualization]
```

### Chiến lược Versioning Dataset
Dataset được quản lý theo từng phiên bản để phục vụ việc kiểm thử và báo cáo học thuật:
* `Dataset v1` (01/09/2026): 50,000 matches — Dữ liệu baseline.
* `Dataset v2` (15/09/2026): 62,000 matches — Mở rộng tập dữ liệu.
* `Dataset v3` (01/10/2026): 75,000 matches — Tập dữ liệu chính thức phục vụ báo cáo.

---

# 🧮 9. Danh mục thuật toán (Algorithm Portfolio)

| Phân nhóm | Thuật toán | Mục đích ứng dụng | Độ phức tạp lý thuyết |
| :--- | :--- | :--- | :---: |
| **Searching** | **Linear Search** | Tìm kiếm tuần tự trong danh sách chưa sắp xếp | $\mathcal{O}(n)$ |
| **Searching** | **Binary Search** | Tìm kiếm nhị phân trên danh sách đã sắp xếp | $\mathcal{O}(\log n)$ |
| **Sorting** | **Quick Sort** | Sắp xếp dữ liệu đa chỉ số phân chia theo Pivot | $\mathcal{O}(n \log n)$ |
| **Sorting** | **Merge Sort** | Sắp xếp chia để trị ổn định (Stable Sort) | $\mathcal{O}(n \log n)$ |
| **Statistics** | **Descriptive Stats** | Tính Mean, Median, Variance, Standard Deviation, Min, Max, Range, Skewness | $\mathcal{O}(n)$ / $\mathcal{O}(n \log n)$ |
| **Trend** | **Moving Average** | Làm mịn chuỗi biến động Odds theo thời gian | $\mathcal{O}(n)$ |
| **Evaluation** | **Form Score** | Đánh giá phong độ $k$ trận gần nhất | $\mathcal{O}(k)$ |
| **Rating** | **Elo Rating System** | Cập nhật hệ số sức mạnh tương đối sau mỗi trận đấu | $\mathcal{O}(1)$ / trận |
| **Prediction** | **Weighted Scoring** | Dự đoán kết quả theo trọng số đặc trưng | $\mathcal{O}(1)$ / trận |
| **ML Extension**| **Logistic Regression** | Phân loại đa lớp dự đoán kết quả trận đấu | Dự đoán: $\mathcal{O}(d)$ |
| **ML Extension**| **Decision Tree** | Cây quyết định phân loại kết quả | Dự đoán: $\mathcal{O}(h)$ |

---

# 📊 10. Đánh giá & Benchmark (Evaluation)

### 10.1. Benchmark hiệu năng thuật toán
* So sánh thời gian thực thi giữa **Linear Search vs. Binary Search** trên các kích thước dữ liệu khác nhau ($N = 1.000, 10.000, 50.000, 100.000$).
* So sánh **Quick Sort vs. Merge Sort** về thời gian thực thi (ms) và mức chiếm dụng bộ nhớ (MB).

### 10.2. Đánh giá độ chính xác mô hình dự đoán
* Sử dụng tập dữ liệu lịch sử để backtest và tính toán các chỉ số:
  $$	ext{Accuracy} = rac{	ext{Số trận dự đoán đúng}}{	ext{Tổng số trận}}$$
* Đánh giá ma trận nhầm lẫn (**Confusion Matrix**), **Precision**, **Recall**, **F1-Score**.

---

# 📅 11. Kế hoạch triển khai (Timeline - 6 Tuần)

```text
Tuần 1 ──► Thu thập Dataset + Xây dựng API Client & Schema Room Database
Tuần 2 ──► Triển khai thuật toán Tìm kiếm (Linear/Binary), Sắp xếp (Quick/Merge) & Thống kê
Tuần 3 ──► Xây dựng thuật toán phân tích phong độ, Home/Away, H2H & Hệ thống Elo Rating
Tuần 4 ──► Phân tích Odds đa nguồn, xu hướng Odds & Mô hình dự đoán Weighted Scoring
Tuần 5 ──► Thiết kế giao diện Jetpack Compose, Trực quan hóa dữ liệu & Màn hình Benchmark
Tuần 6 ──► Kiểm thử toàn diện, đánh giá hiệu năng, hoàn thiện báo cáo & Slide thuyết trình
```

---

# 🛠️ 12. Công nghệ sử dụng (Tech Stack)

* **Language:** [Kotlin 2.0+](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Architecture:** Clean Architecture + MVVM + Android Architecture Components
* **Local Database:** [Room Database](https://developer.android.com/training/data-storage/room)
* **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
* **Networking & JSON:** [Retrofit 2](https://square.github.io/retrofit/) + [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
* **Asynchronous:** Kotlin Coroutines & Flow
* **Visualization:** Compose Canvas Charts / Vico / MPAndroidChart

---

<div align="center">
<b>TrueLab © 2026 — Đồ án Thuật toán ứng dụng</b>
</div>
