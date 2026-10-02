# Prediction Runtime Sync: Timezone, Refresh & Match Status

## Status
OPEN

## Severity
HIGH

## Summary
Màn hình `PredictionScreen` gặp 3 lỗi nghiêm trọng liên quan đến cơ chế dữ liệu và trạng thái trận đấu:
1. Thiếu các trận đấu sáng sớm (từ 00:00 đến 06:59:59 giờ Việt Nam) do lệch pha múi giờ giữa UI Local Time (UTC+7) và Remote API (lọc theo UTC Date).
2. Hiển thị sai trạng thái `LIVE` nhấp nháy cho các trận đấu đã kết thúc từ nhiều giờ trước nhưng database cục bộ đang lưu trạng thái `pending` cũ.
3. Độ trễ Pull-to-Refresh và mở màn hình kéo dài ~30 giây do cơ chế đồng bộ vô tình thực thi 100+ requests HTTP tuần tự của danh mục giải đấu/mùa giải toàn cầu trên Critical Path.

---

## Symptoms
- **Bỏ sót trận đấu sáng sớm**: Khi chọn ngày hôm nay trên giao diện, các trận diễn ra lúc 01:00 - 05:00 sáng không được cập nhật dữ liệu mới từ API.
- **Trận đấu Greece vs Netherlands (Match ID 129243) bị stale**: Trận đấu diễn ra lúc 01:45 sáng ngày 02/10/2026 (giờ VN), thực tế đã kết thúc với tỷ số 2-2, nhưng đến 10:30 sáng vẫn hiển thị `LIVE` với tỷ số 0-0.
- **Pull-to-refresh bị đơ/chậm ~30 giây**: Mỗi lần vuốt làm mới, ứng dụng mất gần nửa phút mới tắt biểu tượng loading.

---

## Root Cause
Đã được xác minh qua code audit và kiểm tra trực tiếp API:

1. **Lệch pha múi giờ khi gửi Query Param (`date=YYYY-MM-DD`)**:
   - UI dùng ngày địa phương Việt Nam (`2026-10-02`).
   - Server API lọc theo UTC Date (`00:00:00Z` đến `23:59:59Z`).
   - Trận Greece đá lúc `2026-10-01T18:45:00Z` thuộc UTC `2026-10-01`. Khi UI gọi `getMatches(date = "2026-10-02")`, API không trả về trận này, khiến Room không nhận được dữ liệu mới để cập nhật.
2. **Logic hiển thị trạng thái thiếu chặn trên thời gian (`resolveDisplayStatus`)**:
   - Tại `PredictableMatchCard.kt`, nhánh `SCHEDULED` kiểm tra `kickoff <= now` mà không có giới hạn thời gian kết thúc, khiến bất kỳ trận `pending` cũ nào có giờ đá đã qua đều bị ép thành `LIVE` vĩnh viễn suốt ngày.
3. **Gọi lặp tuần tự 100 HTTP requests không cần thiết (`DataSyncEngine`)**:
   - `syncFullPipelineForDate` mặc định gọi `syncLeaguesAndSeasonsFromRemoteInternal`, lặp qua 100 giải đấu và gọi `getCompetitionSeasons` tuần tự từng cái một (~28s) trước khi fetch trận đấu trong ngày.

---

## Evidence
- **Match ID 129243 (Greece vs Netherlands)**:
  - Kickoff UTC: `2026-10-01T18:45:00Z` (Giờ Việt Nam: `01:45 ngày 02/10/2026`).
  - Gọi trực tiếp `GET /sport/v1.0/matches/129243`: Trả về `status: "ended"`, `minutes: "FT"`, `home_score: 2`, `away_score: 2`, `attributes.status: 8`.
  - Gọi `GET /sport/v1.0/matches?date=2026-10-02`: Trả về 20 trận, **0 trận Match 129243**.
  - Gọi `GET /sport/v1.0/matches?date=2026-10-01`: Match 129243 nằm tại **Trang 9**.
  - Database Room trên máy test: Lưu `status: "pending"`, `homeScore: 0`, `awayScore: 0`.

---

## Expected Behavior
1. Khi người dùng mở ngày `2026-10-02` hoặc kéo refresh:
   - Hệ thống tự động query các UTC date tương ứng (`2026-10-01` và `2026-10-02`) kèm phân trang đầy đủ.
   - Match 129243 được cập nhật vào Room với `status = ENDED`, tỷ số `2-2`.
   - UI hiển thị `FT` và tỷ số `2 - 2`.
2. Trận `pending` có kickoff quá lâu trong quá khứ không bao giờ hiển thị `LIVE`.
3. Thời gian Pull-to-refresh hoàn thành nhanh chóng (< 1 giây).

---

## Proposed Fix
1. **Tạo `LocalDateRangeMapper`**: Chuyển đổi ngày local sang dải UTC query dates và lọc chính xác các trận thuộc khung giờ local `[startOfDayUtc, endOfDayUtc)`.
2. **Tách biệt Match Sync**: Thêm `syncMatchesForLocalDate()` trong `DataSyncEngine` chỉ tập trung sync matches + inline leagues/teams, loại bỏ hoàn toàn việc gọi `syncLeaguesAndSeasonsFromRemoteInternal` trong runtime path.
3. **Sửa `resolveDisplayStatus`**: Tôn trọng Server Status; chỉ hiển thị `LIVE` khi Server xác nhận `IN_PROGRESS` hoặc trong khoảng thời gian diễn ra hợp lệ.
4. **Quản lý Concurrency**: Hủy sync job cũ khi user thao tác nhanh hoặc đổi ngày liên tục.

---

## Acceptance Criteria
- [ ] Match ID 129243 xuất hiện đầy đủ khi chọn ngày `2026-10-02` trên `PredictionScreen`.
- [ ] Match ID 129243 hiển thị trạng thái `FT` với tỷ số `2 - 2`.
- [ ] Query đầy đủ các ngày UTC tương ứng cho ngày cục bộ Việt Nam.
- [ ] Phân trang được duyệt đầy đủ cho đến `last_page`.
- [ ] Pull-to-refresh không gọi tải danh mục 100 giải đấu/mùa giải toàn cầu.
- [ ] Thời gian Pull-to-refresh đạt chuẩn hiệu năng (< 1 giây).
- [ ] Trận `pending` quá giờ đá không bị hiển thị `LIVE`.
- [ ] Room Database là Single Source of Truth duy nhất.
- [ ] Toàn bộ thuật toán dự đoán, Elo, Form giữ nguyên tính đúng đắn.
- [ ] Unit Tests hiện có và mới đều PASS (Green).

---

## Test Cases
1. `LocalDateRangeMapperTest`: Kiểm tra chuyển đổi múi giờ Việt Nam (UTC+7) sang UTC query dates.
2. `ResolveDisplayStatusTest`: Kiểm tra ma trận trạng thái trận đấu (không rơi vào vòng lặp LIVE).
3. `DataSyncEngineRuntimeSyncTest`: Kiểm tra fetch đa trang trên 2 ngày UTC và lọc đúng cửa sổ local.
4. `PredictionViewModelRefreshTest`: Kiểm tra hủy job cũ và cập nhật `isRefreshing`.

---

## Out of Scope
- Không crawl lại dataset lịch sử 30.000+ matches.
- Không sửa file asset database SQLite `truelab_database.db`.
- Không thay đổi model thuật toán dự đoán, trọng số Elo/Poisson.
- Không redesign UI tổng thể của `PredictionScreen`.

---

## Related Plan
- [prediction-runtime-sync-timezone-plan.md](../plans/prediction-runtime-sync-timezone-plan.md)

---

## Verification
- [ ] TODO: Implementation & Device Verification (Sẽ thực hiện ở Implementation Phase sau khi User phê duyệt).
