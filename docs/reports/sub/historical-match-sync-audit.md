# Báo Cáo Audit & Thiết Kế Đồng Bộ Trận Đấu Lịch Sử (Historical Match Sync)

## 1. Kiến Trúc Đồng Bộ Hiện Tại (Current Sync Architecture)
Logic đồng bộ dữ liệu hiện tại nằm chủ yếu trong [`DataSyncEngine.kt`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt). Điểm khởi đầu chính là `syncFullPipelineForDate(date, ...)`, cho thấy kiến trúc ban đầu được xây dựng xoay quanh việc đồng bộ theo ngày (date-based).

**Luồng xử lý (Flow):**
1. Gửi truy vấn tới endpoint `/sport/v1.0/matches` qua [`MatchApi.getMatches(date=date, page=currentPage)`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/remote/api/MatchApi.kt).
2. Lặp phân trang `while (currentPage <= totalPages)` (đọc từ `response.data.meta.totalPage`).
3. Với mỗi trang, map dữ liệu sang `LeagueEntity`, `TeamEntity`, và `MatchEntity` rồi insert vào Room Database thông qua `runInTransaction`.
4. Nếu được cấu hình, tiếp tục fetch Odds và Rankings cho từng trận đấu riêng lẻ.

---

## 2. Nguyên Nhân Gốc Của Giới Hạn ~156 Trận Đấu (156-Match Root Cause)
Nguyên nhân gốc rễ khiến cơ sở dữ liệu chỉ có khoảng ~156 trận đấu là do **chiến lược đồng bộ theo ngày (`syncFullPipelineForDate`) kết hợp với phạm vi ngày bị giới hạn**.

- Logic hiện tại được thiết kế để lấy các trận đấu *cho một ngày cụ thể*.
- Dù logic phân trang (`while (currentPage <= totalPages)`) đã được xử lý chính xác cho ngày đó, một ngày trong bóng đá chỉ có số lượng trận đấu nhất định (khi test audit với ngày `2026-09-01` trả về 0 trận, và một ngày thông thường chỉ có khoảng ~100-200 trận đã kết thúc).
- Nếu không có vòng lặp ngoài quét qua *hàng trăm ngày trong quá khứ*, cơ sở dữ liệu sẽ luôn bị giới hạn.
- Ngoài ra, tham số `status` trong `MatchApi` ban đầu mặc định là `null` (không chỉ định rõ `-1` hoặc `8`), nghĩa là không lọc chuyên biệt cho các trận đấu lịch sử đã kết thúc.

---

## 3. TrueScore API Contract Yêu Cầu Cho TrueLab
Để đáp ứng yêu cầu mở rộng quy mô lên 50,000+ trận đấu lịch sử, hệ thống cần hợp đồng API sau:
- **Endpoint**: `/sport/v1.0/competitions/{seasonId}/match-list`
- **Query `status`**: `-1` (Đã xác minh: chuyên lấy các trận đấu lịch sử / đã kết thúc).
- **Query `page_size`**: `100` (Đã xác minh: hoạt động chuẩn và giảm số lượng API calls).
- **Phân trang**: Lặp từ `meta.current_page` đến `meta.last_page`.

---

## 4. Các Khả Năng Còn Thiếu (Missing Capabilities)
1. **Endpoint Lấy Danh Sách Trận Đấu Theo Giải/Mùa**: [`MatchApi`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/remote/api/MatchApi.kt) ban đầu chỉ có `/sport/v1.0/matches` (theo ngày), thiếu endpoint `/sport/v1.0/competitions/{seasonId}/match-list`.
2. **Bộ Lọc Tham Số Status**: Việc đồng bộ theo ngày chưa lọc tường minh `status=-1`.
3. **Cơ Chế Quét Lịch Sử Chuyên Sâu (Deep Historical Sweeper)**: [`DataSyncEngine`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt) thiếu hàm lặp qua các mùa giải lịch sử (trước đó chỉ có `syncLeaguesAndSeasonsFromRemoteInternal` nhưng chưa tận dụng các seasonId này để fetch trận đấu).

---

## 5. Thiết Kế Đồng Bộ Lịch Sử (Historical Sync Design)

**Luồng Xử Lý:**
1. Lấy danh sách giải đấu (`CompetitionList`).
2. Với mỗi giải đấu, lấy danh sách mùa giải (`SeasonList`).
3. Xác định các mùa giải lịch sử (ví dụ: các mùa trước năm hiện tại).
4. Với từng `seasonId` đã chọn, gọi `/sport/v1.0/competitions/{seasonId}/match-list?status=-1&page_size=100`.
5. Lặp từ `page=1` đến `last_page`.
6. Với mỗi trang, map DTO sang Entity.
7. Thực thi `runInTransaction` để lưu Teams và Matches.

**Lộ Trình Quy Mô (Scope Rollout):**
- **Phase A**: 1 Giải đấu (EPL id=927) × 1 mùa giải lịch sử (id=26528) → Thu được ~380 trận.
- **Phase B**: Top 5 Giải Vô Địch Quốc Gia Châu Âu × 3 mùa gần nhất → Thu được ~5,405 trận.
- **Phase C**: 11 Giải đấu × 43 mùa giải → Thu được ~15,300 trận lịch sử (kết hợp baseline đạt 15,456 trận).
- **Phase D**: Mở rộng toàn diện → Đạt mốc 50,000+ trận.

---

## 6. Thiết Kế Phân Trang (Pagination Design)
Cơ chế phân trang dựa vào object `meta` trả về từ API:
```kotlin
var currentPage = 1
var lastPage = 1
while (currentPage <= lastPage) {
    val response = matchApi.getSeasonMatches(seasonId, status = -1, pageSize = 100, page = currentPage)
    // Xử lý dữ liệu...
    lastPage = response.data.meta?.lastPage ?: 1
    currentPage++
}
```

---

## 7. Chiến Lược Chống Trùng Lặp (Deduplication Strategy)
- **Cấp độ API**: Fetch theo từng `seasonId` riêng biệt giúp hạn chế tối đa trùng lặp giữa các mùa.
- **Cấp độ Kotlin**: `teams.distinctBy { it.id }` trong từng trang trước khi insert.
- **Cấp độ Database**: `MatchDao` và `TeamDao` sử dụng `OnConflictStrategy.IGNORE` để bảo toàn dữ liệu liên kết và tránh kích hoạt CASCADE DELETE trên bảng Odds.

---

## 8. Tối Ưu Hiệu Năng & Room Database
- **Bộ nhớ (Memory)**: Xử lý theo từng trang (100 trận/trang) đảm bảo memory footprint thấp, không load đồng thời hàng chục ngàn trận vào RAM.
- **Transactions**: `database.runInTransaction` cho mỗi trang (100 trận + ~200 teams) đạt hiệu năng tối ưu với SQLite.
- **Giới hạn tần suất (Rate Limiting)**: Sử dụng `delay(200)` hoặc cấu hình phù hợp giữa các lượt gọi trang để tránh bị rate limit hoặc chặn kết nối trong quá trình quét dữ liệu lớn.

---

## 9. An Toàn Dữ Liệu Odds (Odds Data Safety)
- **Ràng buộc khóa ngoại**: `OddsEntity` có `ForeignKey` tham chiếu tới `MatchEntity` với `onDelete = ForeignKey.CASCADE`.
- **Độ an toàn**: Để đảm bảo tuyệt đối không làm mất dữ liệu Odds hiện có khi import hoặc sync thêm trận đấu, bắt buộc dùng `OnConflictStrategy.IGNORE` trên `MatchDao`.

---

## 10. Kế Hoạch Di Trú Từng Bước (Incremental Migration Plan)
1. **Giữ nguyên dữ liệu hiện có**: Tuyệt đối không xóa hay reset database.
2. **Bổ sung Endpoint**: Thêm `getSeasonMatches` vào [`MatchApi`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/remote/api/MatchApi.kt).
3. **Thêm Hàm Đồng Bộ**: Xây dựng `syncHistoricalMatchesBySeason(seasonId)` trong [`DataSyncEngine`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt).
4. **Đồng Bộ Từng Giai Đoạn**: Thực hiện quét tuần tự theo danh sách mùa giải lịch sử với cơ chế try/catch để xử lý lỗi cục bộ mà không gián đoạn toàn bộ tiến trình.

---

## 11. Bảng Đối Chiếu Hiện Trạng (Implementation Checklist)

| Mục kiểm tra | TrueLab Ban Đầu | Yêu Cầu Thiết Kế |
|---|---|---|
| Ended status | `null` (trong MatchApi) | `-1` |
| Kích thước trang (Page size) | `50` (mặc định) | `100` |
| Phân trang | `totalPage` | `current_page` → `last_page` |
| Mùa giải lịch sử | `syncLeaguesAndSeasonsFromRemote` | Có (Cần vòng lặp Match sync) |
| Nhận diện giải đấu | Đã có | Yêu cầu |
| Chống trùng lặp | `distinctBy { it.id }` | `matchId` |
| Khả năng tiếp tục (Resume) | Có Try/Catch | Có (Theo từng season loop) |
| Bulk insert | Có (theo trang) | Có |
