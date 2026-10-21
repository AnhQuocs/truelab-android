# TrueLab — Kế Hoạch Kỹ Thuật Presentation Phase P3: Advanced Analysis & Evaluation Visualizer

Tài liệu này xác lập kế hoạch kỹ thuật tổng thể, thiết kế kiến trúc, mô hình trạng thái và tiêu chí nghiệm thu cho **Presentation Phase P3 (Advanced Analysis & Evaluation Visualizer)** trong dự án **TrueLab**. Giai đoạn P3 hoàn thiện hai công cụ phân tích và trực quan hóa chuyên sâu dựa trên 100% **DỮ LIỆU THẬT** từ Room Database và tầng Domain/Algorithm cốt lõi:
1. **P3.1 — H2H Team Comparison Tool**: Công cụ chọn 2 đội bóng và đối sánh ma trận đối đầu trực tiếp, phong độ, Elo và hiệu suất sân nhà/khách.
2. **P3.2 — Prediction Backtest & Evaluation Visualizer**: Bảng điều khiển trực quan hóa độ chính xác mô hình dự đoán (Ma trận nhầm lẫn Confusion Matrix, Precision, Recall, F1-Score) trên tập dữ liệu lịch sử.

---

## 1. Mục Tiêu của Presentation Phase P3

1. **Hoàn Thiện Bộ Công Cụ Phân Tích Chuyên Sâu (Advanced Analysis Tools)**: Mở rộng khả năng tương tác của người dùng từ việc chỉ xem danh sách trận đấu sang việc trực tiếp lựa chọn đối kháng và đánh giá thuật toán.
2. **Khai Thác Tối Đa Dữ Liệu Thực Tế từ Data D3**:
   - Sử dụng kho dữ liệu đã được nạp tự động (`matches`, `teams`, `leagues`, `seasons`, `odds`).
   - Tuyệt đối không sử dụng dữ liệu giả lập (mock/fake) hay hardcode kết quả để làm đẹp giao diện.
3. **Tái Sử Dụng Triệt Để Hạ Tầng Domain D1–D4 & Algorithm Phase 1–7**:
   - Tái sử dụng các UseCase: `CalculateTeamFormUseCase`, `CalculateEloRatingUseCase`, `CalculateHomeAwaySplitsUseCase`, `GetTeamStatisticsUseCase`, `SearchTeamsUseCase`, `BacktestPredictionUseCase`, `CalculateEvaluationMetricsUseCase`.
   - Giữ nguyên ranh giới kiến trúc Clean Architecture & MVVM.
4. **Trải Nghiệm Giao Diện Cao Cấp & Nhất Quán (Premium Material 3 UI)**:
   - Áp dụng triệt để Design System (`:core:ui`): Color tokens, Dimensions, Typography, Canvas Visualizations, Triple-Locale (`values`, `values-en`, `values-vi`).

---

## 2. Kế Hoạch Chi Tiết Từng Sub-Task

```text
┌────────────────────────────────────────────────────────────────────────┐
│                     PRESENTATION PHASE P3 ROADMAP                      │
├───────────────────────────────────┬────────────────────────────────────┤
│   Sub-task P3.1                   │   Sub-task P3.2                    │
│   H2H Team Comparison Tool        │   Prediction Backtest Visualizer   │
│   • Team Selection (A vs. B)      │   • Backtest Runner Execution      │
│   • Direct Clash Record (W-D-L)   │   • 3x3 Confusion Matrix Heatmap   │
│   • Form & Elo Comparison Bars    │   • Precision, Recall, F1 Metrics  │
│   • Home/Away Splits Comparison   │   • Historical Prediction Timeline │
│   • Goals Descriptive Statistics  │   • Filter Correct/Incorrect       │
└───────────────────────────────────┴────────────────────────────────────┘
```

---

### 2.1. Sub-task P3.1 — H2H Team Comparison Tool

#### 2.1.1. Mục Tiêu & User Flow
Cho phép người dùng chọn linh hoạt hai đội bóng bất kỳ (`Team A` và `Team B`) từ danh mục đội bóng thực tế và hiển thị báo cáo đối đầu toàn diện:
1. Người dùng mở màn hình **H2H Comparison** (từ thẻ *Analysis Tools* trên `HomeScreen` hoặc nút *Compare* trên `TeamsScreen` / `MatchDetailBottomSheet`).
2. Giao diện ban đầu hiển thị bộ chọn hai đội bóng (`TeamSelectorHeader`).
3. Người dùng chọn Đội A $\rightarrow$ chọn Đội B (thông qua Bottom Sheet tìm kiếm nhanh `TeamSelectionBottomSheet`).
4. Hệ thống tự động nạp dữ liệu đối đầu, tính toán các chỉ số thống kê và hiển thị Dashboard so sánh.
5. Nếu đổi đội bóng, kết quả tự động tính toán lại tức thì trên luồng StateFlow phản ứng.

#### 2.1.2. Phân Tích Ranh Giới Kỹ Thuật (Existing Assets vs. New Additions)
- **Tầng Data (`:core:data`)**:
  - *Đã có sẵn*: `MatchDao.getH2HMatches(teamAId, teamBId)` hỗ trợ query hai chiều; `TeamDao.getAllTeams()`, `TeamDao.getTeamById()`.
  - *Cần bổ sung*: Expose `MatchRepository.getH2HMatches(teamAId: Int, teamBId: Int): Flow<List<Match>>` trong `MatchRepository` và `MatchRepositoryImpl`.
- **Tầng Domain (`:core:domain`)**:
  - *Đã có sẵn*: `CalculateTeamFormUseCase`, `CalculateEloRatingUseCase`, `CalculateHomeAwaySplitsUseCase`, `GetTeamStatisticsUseCase`, `SearchTeamsUseCase`, `H2hSignalTransformer`.
  - *Cần bổ sung*: UseCase điều phối `GetHeadToHeadComparisonUseCase(matchRepository, teamRepository)` tổng hợp lịch sử chạm trán thành Domain Entity `HeadToHeadComparisonSummary`.
- **Tầng Presentation (`:app`)**:
  - Xây dựng `H2HComparisonViewModel`, `H2HComparisonScreen`, `H2HUiMapper`, `H2HComparisonUiState`.
  - Định tuyến Navigation: `TrueLabDestinations.H2H_COMPARISON`.

#### 2.1.3. Mô Hình Trạng Thái Giao Diện (`H2HComparisonUiState`)
```kotlin
sealed interface H2HComparisonUiState {
    data class Loading(
        val availableTeams: List<TeamSummary> = emptyList(),
        val selectedTeamAId: Int? = null,
        val selectedTeamBId: Int? = null
    ) : H2HComparisonUiState

    data class TeamSelectionRequired(
        val availableTeams: List<TeamSummary>,
        val selectedTeamAId: Int? = null,
        val selectedTeamBId: Int? = null,
        val message: UiText
    ) : H2HComparisonUiState

    data class Success(
        val availableTeams: List<TeamSummary>,
        val teamA: TeamDetail,
        val teamB: TeamDetail,
        val comparison: H2HComparisonUiRecord,
        val matchHistory: List<MatchDataRecord>
    ) : H2HComparisonUiState

    data class Error(
        val message: UiText,
        val availableTeams: List<TeamSummary> = emptyList()
    ) : H2HComparisonUiState
}
```

#### 2.1.4. Các Khối Giao Diện Thành Phần (UI Components)
1. **`TeamSelectorHeader`**: Khung chọn 2 đội tương tác (Logo, Tên, nút đổi đội), tự động chặn việc chọn cùng một đội ($A \neq B$).
2. **`H2HOverallClashCard`**: Thanh tiến trình phân đoạn 3 màu (Thắng A - Hòa - Thắng B), tỷ lệ phần trăm và tổng số bàn thắng ghi được giữa 2 đội.
3. **`ComparativeMetricsCard`**: Các thanh đo so sánh ngang hai chiều (Bi-directional Comparative Bars):
   - **Elo Rating**: So sánh điểm sức mạnh kỳ vọng (ví dụ: 1540 vs. 1480).
   - **Form Score**: So sánh điểm phong độ 5 trận gần nhất (thang điểm 0.0 – 10.0).
   - **Bàn thắng kỳ vọng**: Trung bình bàn thắng ghi được và bàn thua mỗi trận ($\mu, \sigma$).
4. **`HomeAwaySplitsGrid`**: So sánh thành tích Sân nhà của Team A vs. Thành tích Sân khách của Team B.
5. **`H2HMatchHistoryList`**: Danh sách lịch sử các trận đối đầu quá khứ (ngày đấu, giải đấu, tỷ số, huy hiệu kết quả). Nếu $N=0$, hiển thị Empty State thông báo hai đội chưa từng chạm trán.

---

### 2.2. Sub-task P3.2 — Prediction Backtest & Evaluation Visualizer

#### 2.2.1. Mục Tiêu & Phạm Vi
Xây dựng giao diện báo cáo đánh giá khoa học về độ tin cậy và hiệu năng thực tế của mô hình dự đoán bóng đá `WeightedScorer` trên toàn bộ tập dữ liệu trận đấu lịch sử.

#### 2.2.2. Nguồn Dữ Liệu & Domain Pipeline Tái Sử Dụng
- **Tầng Domain (`:core:domain:evaluation`)**:
  - Tái sử dụng 100% `BacktestPredictionUseCase` và `CalculateEvaluationMetricsUseCase`.
  - Quy tắc bất biến: **Temporal Data Leakage Prevention** (đối với mỗi trận $T_i$, chỉ sử dụng các trận diễn ra trước thời điểm $T_i$ để tính Form và H2H).
  - Kết quả trả về: `PredictionBacktestResult` chứa ma trận nhầm lẫn (`ConfusionMatrix`), các chỉ số phân lớp (`ClassEvaluationMetrics` cho Home, Draw, Away) và danh sách chi tiết từng trận backtest (`BacktestMatchRecord`).

#### 2.2.3. Các Thành Phần Trực Quan Hóa (Visualizer Dashboard)
1. **Bảng Tổng Quan Hiệu Năng (Overall Performance Card)**:
   - Độ chính xác tổng thể (**Overall Accuracy** %).
   - Điểm số **Macro F1-Score**, **Macro Precision**, **Macro Recall**.
   - Tổng số trận đã thẩm định ($N_{\text{samples}}$) và số trận dự đoán chính xác.
2. **Biểu Đồ Ma Trận Nhầm Lẫn (3x3 Confusion Matrix Heatmap)**:
   - Trực quan hóa ma trận $3 \times 3$ (Hàng: Thực tế Home/Draw/Away; Cột: Dự đoán Home/Draw/Away).
   - Cường độ màu sắc (Heatmap gradient) tương ứng với mật độ phân bổ dự đoán.
3. **Thẻ Chi Tiết Phân Lớp (Per-Class Performance Breakdown)**:
   - Bảng so sánh Precision, Recall, F1-Score riêng biệt cho từng kịch bản: **Đội nhà thắng (Home Win)**, **Hòa (Draw)**, **Đội khách thắng (Away Win)**.
4. **Dòng Thời Gian Dự Đoán Lịch Sử (Historical Prediction Timeline)**:
   - Danh sách chi tiết các trận đấu trong tập backtest với bộ lọc: Tất cả / Dự đoán Đúng (Correct) / Dự đoán Sai (Incorrect).
   - Hiển thị xác suất mô hình gán cho từng cửa $[P_H, P_D, P_A]$ so với tỷ số thực tế.

---

## 3. Ràng Buộc Kiến Trúc & Bất Biến Kỹ Thuật (Architectural Constraints)

1. **Phân Định Layer Nghiêm Ngặt**:
   - `:core:algorithm`: Chỉ chứa thuật toán thuần túy (Sort, Search, Stats, Rating, Scoring).
   - `:core:domain`: Chứa logic nghiệp vụ, models và use cases orchestration.
   - `:core:data`: Quản lý Room Database, DAO, Network API và DataSync.
   - `:app` (Presentation): Chỉ quản lý Compose UI, ViewModels, UI States và Mappers.
2. **Tuyệt Đối Không Đưa Business Logic Vào Composable**: Mọi phép tính toán tổng hợp, lọc hay format phức tạp đều phải thông qua UseCase và ViewModel/UiMapper.
3. **Zero Fake Data Policy**: Toàn bộ dữ liệu hiển thị trên giao diện đối đầu H2H và bảng Backtest phải xuất phát từ cơ sở dữ liệu Room thật.
4. **Bảo Toàn Design System**: Tái sử dụng 100% tokens từ `:core:ui` (`Dimen`, `AppTypography`, `Color Tokens`, `Radius`). Không hardcode kích thước dạng số đo trần hay mã màu ad-hoc.
5. **Đa Ngôn Ngữ Bắt Buộc (Triple-Locale)**: Toàn bộ chuỗi giao diện mới phải được khai báo đầy đủ trong cả 3 tập tin:
   - `app/src/main/res/values/strings.xml` (Mặc định)
   - `app/src/main/res/values-en/strings.xml` (Tiếng Anh)
   - `app/src/main/res/values-vi/strings.xml` (Tiếng Việt)

---

## 4. Kế Hoạch Kiểm Thử (Testing Strategy)

### 4.1. Domain & Data Layer Tests
- **`MatchRepositoryH2HTest`**: Xác minh truy vấn `getH2HMatches` trả về đúng các trận đấu hai chiều giữa 2 đội.
- **`GetHeadToHeadComparisonUseCaseTest`**:
  - Test trường hợp 2 đội có nhiều trận đối đầu ($N > 0$).
  - Test trường hợp 2 đội chưa từng gặp nhau ($N = 0$).
  - Test tính toàn vẹn của tỷ lệ phần trăm thắng/hòa/thua và hiệu số bàn thắng.
  - Test trường hợp thiếu dữ liệu Elo/Form (fallback an toàn).

### 4.2. Presentation Layer Tests
- **`H2HComparisonViewModelTest`**: Kiểm thử toàn bộ chuyển đổi trạng thái (`Loading` $\to$ `TeamSelectionRequired` $\to$ `Success` $\to$ `Error`).
- **`H2HUiMapperTest`**: Kiểm thử format chuỗi, phần trăm và mapping dữ liệu sang `H2HComparisonUiRecord`.
- **`BacktestEvaluationViewModelTest`**: Kiểm thử nạp dữ liệu lịch sử, kích hoạt backtest trên coroutine `Dispatchers.Default` và xuất `BacktestUiState`.

---

## 5. Tiêu Chí Nghiệm Thu Toàn Phase P3 (Acceptance Criteria)

### Sub-task P3.1 — H2H Team Comparison
- [ ] `MatchRepository` expose `getH2HMatches(teamAId, teamBId)` và hoạt động mượt mà.
- [ ] Người dùng có thể chọn 2 đội bất kỳ từ kho dữ liệu Room.
- [ ] Hiển thị đầy đủ ma trận đối đầu: W-D-L, Tỷ lệ %, Bàn thắng, Form Score 5 trận, Elo Rating, Home/Away Splits, Danh sách trận đấu đối đầu quá khứ.
- [ ] Xử lý an toàn trường hợp $N = 0$ trận đối đầu (không crash, không hiển thị `NaN`).

### Sub-task P3.2 — Prediction Backtest Visualizer
- [ ] Khởi chạy quy trình Backtest trên tập dữ liệu lịch sử mà không làm đơ giao diện (`Dispatchers.Default`).
- [ ] Hiển thị trực quan Ma trận nhầm lẫn 3x3 Heatmap.
- [ ] Hiển thị bảng chỉ số Precision, Recall, F1-Score cho từng cửa Home/Draw/Away và Macro F1.
- [ ] Danh sách trận đấu backtest có bộ lọc Đúng/Sai hoạt động chính xác.

### Chất Lượng Mã Nguồn & Hệ Thống
- [ ] Toàn bộ Unit Tests đạt **100% PASS** (`.\gradlew test`).
- [ ] Build Debug APK thành công (`.\gradlew assembleDebug`).
- [ ] Định dạng mã nguồn chuẩn mực: `git diff --check` $\to$ **0 errors**.
- [ ] Tất cả các tập tin mới tuân thủ giới hạn $\le 400$ dòng/file.

---

## 6. Những Gì KHÔNG Làm Trong Phase P3 (Out of Scope)

1. **Không tạo mô hình Machine Learning**: Không tích hợp TensorFlow, PyTorch hay các thư viện AI ngoài JVM.
2. **Không sửa đổi thuật toán dự đoán (`WeightedScorer`)**: Phase P3 chỉ tập trung vào việc trực quan hóa và đối sánh dữ liệu.
3. **Không tạo migration Room Database không cần thiết**: Tận dụng triệt để Schema v2 hiện tại của Room đã hoàn thành ở Data D1–D3.
4. **Không tạo dữ liệu giả lập cho UI**: Mọi số liệu hiển thị đều là kết quả tính toán từ dữ liệu thực tế.
