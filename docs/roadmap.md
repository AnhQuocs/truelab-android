# TrueLab — Layer-Based Engineering Roadmap

> **Nguyên tắc Phân kỳ Độc lập theo Layer**:
> Kể từ sau khi hoàn thành Presentation Phase P1, hệ thống lộ trình (Roadmap) của TrueLab được phân định và đánh số **độc lập theo từng tầng kiến trúc** (Domain, Presentation, Data).
> 
> *Lưu ý về tài liệu lịch sử*: Các tài liệu và báo cáo hoàn thành trước thời điểm chuẩn hóa có thể chứa các tên gọi phase cũ (ví dụ: gộp chung D1→D2→D3→P1→D5→D6). Tài liệu này là **Single Source of Truth** quy định phân kỳ hiện tại và tương lai.

---

## 1. Bản đồ Tổng thể các Tầng Kiến trúc (Architecture Roadmap Map)

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION ROADMAP                            │
│  [P1: Matches + Teams + Analytics + Prediction + Deprecation] (DONE)   │
│  [P2: Benchmark Runner UI + Splits UI + Filters UI + Dataset UI] (DONE)│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN ROADMAP                               │
│  [D1: Prediction Scoring] (DONE) ──► [D2: Analytics/Stats/Rating] (DONE)│
│  ──► [D3: Search & Sort] (DONE)  ──► [D4: Benchmark & Splits] (NEXT)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            DATA ROADMAP                                │
│  [D1: Schema / League / Season / DataSync / Dataset Metadata] (NEXT)   │
│  [D2: Advanced Ingestion / Cache Optimization] (FUTURE)                │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Lộ trình Tầng Domain (Domain Roadmap)

Tầng Domain chịu trách nhiệm về Pure Kotlin/JVM business rules, orchestration thuật toán, đánh giá hiệu năng, tính toán thống kê và mô hình dự đoán. Không phụ thuộc Android SDK, Room hay UI.

| Phase | Phạm vi & Trách nhiệm Nghiệp vụ | Trạng thái | Kế hoạch Chi tiết / Deliverables |
| :--- | :--- | :---: | :--- |
| **Domain D1** | **Prediction / Weighted Scoring Engine** | **DONE** | • `PredictMatchOutcomeUseCase`<br/>• 6 Signal Transformers (Form, Elo, Goals, Odds, H2H, HomeAdvantage)<br/>• `DefaultWeightedScorer` orchestration |
| **Domain D2** | **Analytics / Descriptive Stats / Rating** | **DONE** | • `GetTeamStatisticsUseCase` (Mean, Median, StdDev, Variance, Skewness)<br/>• `CalculateTeamFormUseCase` (Linear Decay Form Evaluator)<br/>• `CalculateEloRatingUseCase` (Elo Calculator)<br/>• `AnalyzeOddsTrendUseCase` (Moving Average MA(k) & Volatility)<br/>• `CompareOddsUseCase` (Spread Calculator) |
| **Domain D3** | **Search & Sort Algorithms Orchestration** | **DONE** | • `SearchMatchesUseCase` & `SearchTeamsUseCase` (Linear / Binary Search)<br/>• `SortMatchesUseCase` & `SortSeasonRankingUseCase` (QuickSort / MergeSort) |
| **Domain D4** | **Domain Expansion, Benchmark & Evaluation** | **NEXT** | • `CalculateHomeAwaySplitsUseCase` & `HomeAwaySplits` model<br/>• Algorithm Benchmark Runners (`SearchBenchmarkRunner`, `SortBenchmarkRunner`)<br/>• Synthetic Dataset Generator ($N = 1\text{K}, 10\text{K}, 50\text{K}$)<br/>• Prediction Backtest Engine & Accuracy Evaluation (Confusion Matrix, Precision, Recall, F1)<br/>• Dọn dẹp triệt để các API đã đánh dấu `@Deprecated` trong Domain |

---

## 3. Lộ trình Tầng Presentation (Presentation Roadmap)

Tầng Presentation chịu trách nhiệm về Jetpack Compose UI, ViewModel, UiState, StateFlow lifecycle collection, Mappers và Visual Charts.

| Phase | Phạm vi Giao diện & Trải nghiệm Người dùng | Trạng thái | Kế hoạch Chi tiết / Deliverables |
| :--- | :--- | :---: | :--- |
| **Presentation P1** | **Core Football Screens Integration & Deprecation** | **DONE** | • `MatchesScreen` + `MatchesViewModel` (Dynamic data, search, sort, status filter, match detail bottom sheet)<br/>• `TeamsScreen` + `TeamsViewModel` (Dynamic standings, tie-breakers, search, form score, Elo rating)<br/>• `AnalyticsScreen` + `AnalyticsViewModel` (Descriptive stats cards, odds matrix, `OddsTrendLineChart` Canvas)<br/>• `PredictionScreen` + `PredictionViewModel` (Match selector, 3-way probabilities card, prediction factors card)<br/>• Triển khai Triple-Locale (`values`, `values-en`, `values-vi`) và `@Deprecated` legacy paths |
| **Presentation P2** | **Presentation Expansion & Tools** | **DONE** | • **P2.1**: `BenchmarkScreen` Runner UI (Đo lường thời gian thực thi thuật toán trên `Dispatchers.Default`, so sánh Search/Sort, Speedup banner, $O(\cdot)$ badge)<br/>• **P2.2**: Hiển thị Home/Away Splits chi tiết (W-D-L và Win Rate động) trên `TeamAnalyticsCard`<br/>• **P2.3**: Dynamic Dataset Overview trên `HomeScreen` + Bộ lọc League/Season trên `MatchesScreen` |

---

## 4. Lộ trình Tầng Data (Data Roadmap)

Tầng Data chịu trách nhiệm về Room Database Schema, SQLite Entities, DAO, Migrations, Repository Implementations, REST API Ingestion và DataSyncEngine.

| Phase | Phạm vi Dữ liệu & Lưu trữ | Trạng thái | Kế hoạch Chi tiết / Deliverables |
| :--- | :--- | :---: | :--- |
| **Data D1** | **Schema Expansion, League/Season & Dataset Metadata** | **NEXT** | • Mở rộng `MatchEntity` và Room Database Migration (thêm cột `leagueId`, `season`)<br/>• Mở rộng `MatchDao` với bộ lọc League/Season và các hàm tổng hợp `COUNT(*)`<br/>• `DataSyncEngine` updates để đồng bộ cấu trúc dữ liệu mới<br/>• Expose metadata kho dữ liệu (số đội, số trận, số nhà cái, phiên bản DB, thời gian cập nhật) qua Repository |
| **Data D2** | **Advanced Ingestion & Offline Sync Engine** | **FUTURE** | • Tối ưu hóa cơ chế caching và streaming Room Flow<br/>• Background Worker định kỳ đồng bộ dữ liệu đa nguồn |

---

## 5. Bảng Phân chia Trách nhiệm Tính năng Đa Tầng (Cross-Layer Feature Matrix)

Một số tính năng lớn trải dài qua nhiều tầng kiến trúc. Bảng dưới đây làm rõ ranh giới trách nhiệm:

| Tính năng | Tầng Data | Tầng Domain | Tầng Presentation |
| :--- | :--- | :--- | :--- |
| **Benchmark & Evaluation** | Không can thiệp (chạy trên in-memory dataset) | **Domain D4**: Benchmark runners, đo thời gian, synthetic dataset, backtest engine, confusion matrix | **Presentation P2**: UI Benchmark screen, kích hoạt runner, vẽ biểu đồ hiệu năng |
| **Home/Away Splits** | Query các trận sân nhà/khách từ `MatchDao` (đã có) | **Domain D4**: `CalculateHomeAwaySplitsUseCase` tổng hợp win rate và record | **Presentation P2**: Render số liệu vào Team Detail Card |
| **Bộ lọc League / Season** | **Data D1**: Migration thêm cột `leagueId`, `season`, DAO query | Model `Match` nhận trường mới | **Presentation P2**: Dropdown/Chip filter trên `MatchesScreen` |
| **Thống kê Dataset Động** | **Data D1**: DAO queries `COUNT(*)`, metadata snapshot | Domain metadata entity nếu cần | **Presentation P2**: Cập nhật `DatasetOverviewCard` trên `HomeScreen` |
| **H2H Comparison Tool** | Query đối đầu giữa 2 đội bất kỳ | **Domain D4 / Future**: H2H Matrix aggregator | **Presentation P2**: Giao diện chọn 2 đội đối kháng |

---

## 6. Quy chuẩn Đặt tên Tài liệu (Documentation Naming Convention)

Mọi tài liệu kế hoạch (Plans) và báo cáo (Reports) mới cần tuân thủ quy tắc tiền tố định danh theo layer:

- **Domain Plans/Reports**:
  - `docs/plans/domain-d1-*.md`, `docs/reports/domain-d1-*.md`
  - `docs/plans/domain-d2-*.md`, `docs/reports/domain-d2-*.md`
  - `docs/plans/domain-d3-*.md`, `docs/reports/domain-d3-*.md`
  - `docs/plans/domain-d4-*.md`, `docs/reports/domain-d4-*.md`
- **Presentation Plans/Reports**:
  - `docs/plans/presentation-p1-plan.md`, `docs/reports/presentation-p1-final.md`
  - `docs/plans/presentation-p2-*.md`, `docs/reports/presentation-p2-*.md`
- **Data Plans/Reports**:
  - `docs/plans/data-d1-*.md`, `docs/reports/data-d1-*.md`
  - `docs/plans/data-d2-*.md`, `docs/reports/data-d2-*.md`
