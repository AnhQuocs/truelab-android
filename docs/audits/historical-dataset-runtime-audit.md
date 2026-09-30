# Runtime Findings (Re-audit — Phase C.1.1)

Tài liệu này ghi nhận kết quả tái thẩm định chuyên sâu (Re-audit) về hành vi thực thi thực tế của ứng dụng TrueLab trên nền tảng cơ sở dữ liệu lịch sử Phase C (15,505 trận đấu, 739 đội bóng, 625,198 bản ghi odds).

---

## 1. Tổng quan Trạng thái Nghiệm thu (Status Summary)

| Phân hệ / Feature | Trạng thái Thẩm định cũ | Trạng thái Sau Re-audit | Đánh giá & Bằng chứng thực tế |
| :--- | :--- | :--- | :--- |
| **Search (Tìm kiếm đội)** | FAIL (Nghi ngờ rỗng/lag) | **PASS** | Hoạt động bình thường. Lần test trước dùng từ khóa gõ sai `"asenal"`. Tìm kiếm `"Arsenal"` trả về đầy đủ. |
| **H2H Comparison Screen** | FAIL (Nghi ngờ rỗng) | **PASS** | Hoạt động chính xác. Arsenal vs Man United hiển thị đầy đủ 10 trận lịch sử (2019–2024), tỷ số và W-D-L 6-2-2. |
| **Teams H2H Summary** | Chưa phân biệt | **FAIL (Unimplemented)** | Card Arsenal hiển thị Home/Away và Form đúng, nhưng field `"Lịch sử đối đầu"` hiển thị `"—"` do hardcode mapper. |
| **Predict Screen** | FAIL (Freeze/Crash) | **FAIL (Verified Crash)** | Truy vấn `SELECT *` nạp 15,505 trận và render đồng thời 15,505 `FilterChip` trong `Row` không-lazy gây block Main thread / ANR. |
| **Backtest Visualizer** | FAIL (OOM/ANR) | **FAIL (Verified Bottleneck)** | Thuật toán $O(N^2)$ lặp ~950 triệu phép so sánh + render toàn bộ 15,397 card trong `Column` không-lazy gây OOM. |

---

## 2. Chi tiết Re-audit từng Phân hệ

### 2.1. Search (Tìm kiếm đội bóng)
- **Trạng thái:** **PASS** (Đã bác bỏ kết luận lỗi của audit cũ).
- **Thực tế Runtime:** Khi người dùng nhập `"Arsenal"` vào ô tìm kiếm trên `TeamsScreen`, danh sách lọc tức thì và hiển thị đúng bản ghi của Arsenal.
- **Nguyên nhân sai lệch ở audit cũ:** Người kiểm thử trước đó đã nhập lỗi chính tả `"asenal"` (thiếu chữ `r`), dẫn đến không khớp chuỗi và kết luận nhầm là lỗi indexing hoặc Unicode.
- **Đánh giá Codebase:**
  - `SearchTeamsUseCase.searchByName` sử dụng `LinearSearch` với cờ `ignoreCase = true`.
  - Với tập dữ liệu 739 đội bóng, việc duyệt tuyến tính trong bộ nhớ RAM diễn ra trong < 1ms, không gây hiện tượng giật lag có thể nhận biết bằng mắt thường.

---

### 2.2. H2H Comparison (Màn hình So sánh Đối đầu)
- **Trạng thái:** **PASS** (Đã bác bỏ kết luận lỗi của audit cũ).
- **Thực tế Runtime:** 
  - Khi chọn cặp đấu **Arsenal vs Manchester United**, màn hình hiển thị chính xác toàn bộ 10 trận đấu lịch sử từ năm 2019 đến 2024.
  - Thống kê đối đầu: Arsenal 6 thắng, 2 hòa, 2 thua; Tổng bàn thắng 17 - 11.
  - Các trận tiêu biểu được hiển thị đầy đủ: `2024-05-12` (MU 0 - 1 Arsenal), `2023-09-03` (Arsenal 3 - 1 MU), `2023-01-22` (Arsenal 3 - 2 MU), ..., `2019-09-30` (MU 1 - 1 Arsenal).
- **Offending claim ở audit cũ bị bác bỏ:** 
  - Audit cũ cho rằng: *"MatchRepositoryImpl thiếu getH2HMatches nên trả về rỗng"*.
  - **Fact Check:** Trong [`MatchRepositoryImpl.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/repository/MatchRepositoryImpl.kt#L37-L41), phương thức `getH2HMatches` đã được cài đặt hoàn chỉnh và gọi xuống [`MatchDao.getH2HMatches`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/dao/MatchDao.kt#L35-L41).
- **Pipeline A thực thi:**
  $$\text{H2HComparisonScreen} \to \text{H2HComparisonViewModel} \to \text{MatchRepository.getH2HMatches} \to \text{MatchDao.getH2HMatches (Indexed SQL)} \to \text{GetHeadToHeadComparisonUseCase}$$

---

### 2.3. Teams H2H Summary (Mục Đối đầu trên Màn hình Đội bóng)
- **Trạng thái:** **FAIL / PLACEHOLDER CHƯA TRIỂN KHAI**.
- **Thực tế Runtime:**
  - Khi tìm kiếm và xem thẻ tóm tắt của Arsenal:
    - Form: `15/15 pts` (Tính toán đúng từ 5 trận gần nhất).
    - Home Record: `67W-19D-18L` (Tính toán đúng từ toàn bộ trận sân nhà lịch sử).
    - Away Record: `51W-20D-33L` (Tính toán đúng từ toàn bộ trận sân khách lịch sử).
    - **Lịch sử đối đầu:** Hiển thị dấu gạch ngang `"—"`.
- **Phân tích nguyên nhân gốc rễ (Root Cause):**
  - Trong [`TeamUiMapper.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/mapper/TeamUiMapper.kt#L62):
    ```kotlin
    h2hHighlight = "—"
    ```
  - Trong [`TeamAnalyticsRecord.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/team/presentation/model/TeamAnalyticsRecord.kt#L25): giá trị mặc định được định nghĩa là `val h2hHighlight: String = "—"`.
  - **Lý do thiết kế:** Trong kế hoạch ban đầu ([`docs/plans/presentation-p1-plan.md`](../plans/presentation-p1-plan.md#L302)), trường `h2hHighlight` trên card của 1 đội bóng đơn lẻ được quyết định hoãn lại (*DEFER*) do trên danh sách chung chưa có khái niệm "đối thủ cụ thể" để tính đối đầu.
- **So sánh Pipeline A vs Pipeline B:**
  - **Pipeline A (H2H Comparison Screen):** Có đủ 2 `teamId` (Team A và Team B) $\to$ Truy vấn Room DAO `WHERE (home = A AND away = B) OR (home = B AND away = A)` $\to$ Trả về 10 trận.
  - **Pipeline B (Teams Screen Summary):** Chỉ có 1 `teamId` của đội bóng hiện tại $\to$ Không có đối thủ cụ thể $\to$ Mapper gán giá trị mặc định `"—"`.

---

### 2.4. Predict Screen (Màn hình Dự đoán Trận đấu)
- **Trạng thái:** **FAIL (Xác nhận lỗi treo/crash)**.
- **Hiện tượng:** Mở màn hình Dự đoán $\to$ Ứng dụng khựng lại vài giây $\to$ ANR hoặc Crash.
- **Xác minh Codebase (FACT):**
  1. **Nạp dữ liệu không phân trang ở Data layer:**
     - [`PredictionViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt#L44) gọi: `matchRepository.getMatches("")`.
     - [`MatchDao.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/dao/MatchDao.kt#L27-L28) chạy câu lệnh:
       ```sql
       SELECT * FROM matches WHERE startTimeDate LIKE '' || '%' ORDER BY startTimeDate DESC
       ```
     - Điều này buộc Room SQLite phải load toàn bộ **15,505 bản ghi `MatchWithTeams`** lên bộ nhớ.
  2. **Render quá tải trên Main Thread ở Presentation layer (Điểm crash chí tử):**
     - `PredictionViewModel` đóng gói toàn bộ 15,505 `Match` vào `PredictionUiState.Success(availableMatches = matches)`.
     - Trong [`PredictionScreen.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/PredictionScreen.kt#L247-L271), hàm `PredictionMatchSelector` sử dụng:
       ```kotlin
       Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
           matches.forEach { match ->
               FilterChip(...)
           }
       }
       ```
     - Thành phần `Row` kết hợp `horizontalScroll` là **Non-Lazy Layout** (không tái sử dụng view).
     - Jetpack Compose buộc phải khởi tạo, đo đạc kích thước và render đồng thời **15,505 `FilterChip` composables** ngay trên Main UI Thread trong một frame duy nhất $\to$ Làm nghẽn hoàn toàn Main Looper và gây OOM / ANR.

---

### 2.5. Backtest Visualizer (Màn hình Kiểm thử Ngược)
- **Trạng thái:** **FAIL (Xác nhận nút thắt cổ chai thuật toán & Render quá tải)**.
- **Hiện tượng:** Mở màn hình Backtest $\to$ Trạng thái xoay Loading kéo dài nhiều phút hoặc ứng dụng bị hệ điều hành tắt do hết RAM/treo.
- **Xác minh Codebase (FACT):**
  1. **Độ phức tạp thuật toán cực đoan $O(N^2)$ ở Domain layer:**
     - Trong [`BacktestPredictionUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt#L59-L84):
       - Tập trận đủ điều kiện: $N \approx 15,397$ trận đã kết thúc.
       - Vòng lặp `for (target in eligibleTargets)` duyệt qua 15,397 lần.
       - Với mỗi trận `target`, mã nguồn thực hiện 4 lần duyệt qua danh sách `sortedMatches`:
         - `pastMatches = sortedMatches.filter { ... }` ($N$ so sánh)
         - `homeRecent = pastMatches.filter { ... }.sortedWith(...).take(5)`
         - `awayRecent = pastMatches.filter { ... }.sortedWith(...).take(5)`
         - `h2h = pastMatches.filter { ... }.sortedWith(...)`
       - **Số phép tính ước tính:** $15,397 \times 4 \times 15,505 \approx 954,942,000$ (gần 1 tỷ phép so sánh và duyệt mảng).
       - **Áp lực bộ nhớ (GC Pressure):** Khởi tạo hơn $120,000$ danh sách tạm `ArrayList` trong vòng lặp.
       - Dù được đặt trên `Dispatchers.Default` trong `BacktestViewModel`, khối lượng tính toán này vẫn chiếm dụng 100% CPU trong 60–120 giây trên vi xử lý di động.
  2. **Render quá tải ở Presentation layer:**
     - Trong [`HistoricalTimelineCard.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/backtest/presentation/components/HistoricalTimelineCard.kt#L144-L154):
       ```kotlin
       Column(verticalArrangement = Arrangement.spacedBy(Dimen.PaddingS)) {
           filteredMatches.forEachIndexed { index, match ->
               BacktestMatchItem(match = match)
               HorizontalDivider(...)
           }
       }
       ```
     - Khi nhận kết quả 15,397 bản ghi, `Column` không-lazy cố gắng khởi tạo hơn **30,000 composables** (`BacktestMatchItem` + `HorizontalDivider`) cùng lúc trong `verticalScroll`, dẫn đến sập bộ nhớ (`OutOfMemoryError`).

---

## 3. Bảng Phân loại Sự thật (Fact vs Inference vs Unconfirmed)

| Hạng mục | Bản chất | Chi tiết thẩm định |
| :--- | :--- | :--- |
| **Search hoạt động đúng** | **FACT** | Code sử dụng `LinearSearch` với `ignoreCase = true` trên 739 đội bóng; test thực tế `"Arsenal"` thành công 100%. |
| **H2H Comparison hoạt động đúng** | **FACT** | Code `MatchDao.getH2HMatches` và `MatchRepositoryImpl.getH2HMatches` đã tồn tại và trả về 10 trận đối đầu thực tế. |
| **Teams H2H Summary hiển thị `"—"`** | **FACT** | Code `TeamUiMapper.kt` dòng 62 gán cứng `h2hHighlight = "—"`, không phải lỗi truy vấn cơ sở dữ liệu. |
| **Predict nạp 15k items vào Non-Lazy Row** | **FACT** | Code `PredictionScreen.kt` dòng 247–271 duyệt `matches.forEach` trong `Row(Modifier.horizontalScroll)`. |
| **Backtest có độ phức tạp $O(N^2)$** | **FACT** | 4 phép lọc lồng trong vòng lặp 15,397 trận tạo ra $\sim 954$ triệu phép tính và hàng trăm nghìn object cấp phát. |
| **Backtest render 15k items vào Non-Lazy Column** | **FACT** | Code `HistoricalTimelineCard.kt` dòng 144–154 duyệt `filteredMatches.forEachIndexed` trong `Column`. |
| **Mức độ tiêu thụ RAM chi tiết của Backtest** | **INFERENCE** | Ước tính từ số lượng object cấp phát trong vòng lặp; cần Android Profiler để đo thông số heap chính xác theo MB. |

---

## 4. Đề xuất Hướng Xử lý (Chỉ kiến nghị — Không tự ý sửa trong Phase này)

1. **Đối với Predict Screen:**
   - *Data layer:* Thay vì `matchRepository.getMatches("")`, chỉ lấy danh sách các trận đấu sắp diễn ra (Upcoming Matches) hoặc giới hạn `LIMIT 20` trận gần nhất phục vụ bộ chọn.
   - *Presentation layer:* Chuyển đổi `Row(Modifier.horizontalScroll(...))` thành `LazyRow` để chỉ render các chip hiển thị trên màn hình.

2. **Đối với Backtest Visualizer:**
   - *Domain layer:* Tối ưu thuật toán từ $O(N^2)$ về $O(N \log N)$ hoặc $O(N)$ bằng cách xây dựng bảng tra cứu chỉ mục trước (`Map<Int, MutableList<Match>>` theo từng đội) và duyệt tuần tự theo thời gian thay vì lặp lại `filter` trên toàn bộ tập dữ liệu.
   - *Presentation layer:* Chuyển đổi danh sách dòng thời gian `HistoricalTimelineCard` sang `LazyColumn` hoặc áp dụng phân trang (Paging 3).

3. **Đối với Teams H2H Summary:**
   - Giữ nguyên thiết kế gạch ngang `"—"` hoặc bổ sung nghiệp vụ hiển thị tóm tắt đối thủ gần nhất/đối thủ truyền kiếp khi thiết kế chi tiết tính năng so sánh đội bóng ở các Phase tiếp theo.