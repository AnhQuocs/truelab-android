# Prediction Pipeline Performance Optimization

## Status
OPEN

## Severity
HIGH

## Problem

Luồng `PredictScreen` hiện đang thực hiện công việc dự đoán ngay trong lúc người dùng chọn trận. Audit đã ghi nhận ba hiện tượng:

1. **Pull-to-refresh mất khoảng 15–20 giây** trong lần đo audit.
2. **Chọn một trận mất khoảng 2–3 giây** trước khi UI hoàn tất việc chuyển focus/hiển thị trạng thái dự đoán.
3. **Odds API được gọi trước khi người dùng nhấn Predict**, dù việc lấy odds chỉ cần thiết khi người dùng thực sự yêu cầu kết quả dự đoán.

Các số liệu trên là baseline đã đo trong audit, không phải kết quả benchmark chuẩn hóa trên mọi thiết bị, dataset hoặc điều kiện mạng.

## Root Cause

### 1. Selection và prediction dùng chung critical path

Trong [`PredictionViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt), `uiState` combine danh sách trận, selected match và selected date rồi chạy prediction flow khi selected match thay đổi. Flow hiện tại:

- gọi `oddsRepository.fetchAndCacheOddsForMatch(selectedMatch.id)` ngay sau khi chọn/auto-select trận (khoảng dòng 173–178);
- combine `teamRepository.getTeamDetail`, `matchRepository.getAllMatches()` và odds flow;
- xây dựng lại toàn bộ context rồi gọi `predictMatchOutcomeUseCase(context)` (khoảng dòng 180–228).

Vì vậy, thao tác chọn trận không chỉ đổi selection state mà còn chờ hoặc cạnh tranh với việc đọc dữ liệu, hydrate odds và tính toán prediction. Nút Predict cũng không thể được coi là trạng thái sẵn sàng độc lập với pipeline này.

### 2. Mỗi prediction đọc và xử lý toàn bộ dataset

[`MatchDao.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/dao/MatchDao.kt) cung cấp `getAllMatches()` (khoảng dòng 104–105). ViewModel sử dụng flow này để:

- truyền toàn bộ matches vào [`CalculateDynamicEloUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateDynamicEloUseCase.kt);
- lọc và sort lại toàn bộ danh sách cho home history, away history và H2H trước kickoff.

`CalculateDynamicEloUseCase` replay các trận đã kết thúc theo thứ tự thời gian cho mỗi lần invoke. Đây là cách bảo đảm point-in-time hiện tại, nhưng chi phí tăng theo kích thước dataset và bị lặp lại khi người dùng đổi trận.

### 3. Refresh vẫn đi qua pipeline đồng bộ nặng

`PredictionViewModel.refresh()` gọi `matchRepository.refreshMatchesForDate(...)` (khoảng dòng 329 trở đi). Trong [`DataSyncEngine.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt), đường tương thích `syncMatchesByDate()` vẫn gọi `syncFullPipelineForDate()` (khoảng dòng 380–390). Pipeline đầy đủ có thể:

- bootstrap leagues/seasons trước khi lấy matches khi `syncLeaguesAndSeasons` bật (khoảng dòng 197–199);
- phân trang và retry network;
- ghi Room theo từng page trong transaction;
- thực hiện thêm các bước liên quan nếu tùy chọn odds/rankings được bật.

Đồng thời, `syncMatchesForLocalDate()` đã có đường sync tập trung hơn, có cache freshness, UTC query window, pagination và transaction theo page (khoảng dòng 66–150). Audit cần xác định và thống nhất đường runtime nào là đường refresh chính, thay vì giả định rằng việc thay thế pipeline sẽ tự động cải thiện thời gian.

## Expected Behavior

- Chọn trận chỉ cập nhật selection state và render thông tin trận; không đọc/replay toàn bộ historical dataset, không tính prediction và không gọi Odds API.
- Nút **Predict** xuất hiện và có thể thao tác ngay sau khi selection hoàn tất.
- Chỉ khi nhấn **Predict** mới bắt đầu chuẩn bị context, lấy odds theo chính sách on-demand hiện có và tính prediction.
- Refresh chỉ đồng bộ dữ liệu cần cho ngày đang xem, tôn trọng cache/network policy, không chạy bootstrap toàn cục trên critical path nếu không cần.
- Prediction sau tối ưu giữ nguyên sáu signals, weights, Candidate A và kết quả prediction hiện tại trên cùng một input snapshot.
- Mọi feature historical vẫn strictly point-in-time: chỉ dùng dữ liệu có timestamp trước kickoff; không dùng trận mục tiêu, trận cùng thời điểm hoặc live/post-kickoff odds làm input.

## Acceptance Criteria

### Hard behavioral criteria

- [ ] Khi chọn một match, trong cùng interaction đó không gọi `fetchAndCacheOddsForMatch`, không gọi Odds API, không gọi `PredictMatchOutcomeUseCase` và không replay Dynamic Elo.
- [ ] Nút Predict được render từ selection state, không phụ thuộc completion của prediction pipeline.
- [ ] Khi nhấn Predict, prediction được tính đúng một lần cho một request hợp lệ; các request cũ bị hủy hoặc bị loại bỏ theo cơ chế concurrency của ViewModel.
- [ ] Prediction result, sáu signals, weights, Candidate A và fallback khi thiếu odds giống baseline trên golden fixtures.
- [ ] Với mọi target match, historical inputs thỏa `history.startTimeDate < target.startTimeDate`; không có data leakage từ target hoặc future/live odds.
- [ ] Refresh không gọi league/season bootstrap toàn cục trên runtime path, trừ khi có một policy/trigger rõ ràng yêu cầu việc đó.

### Measurable performance criteria

- [ ] Baseline được ghi lại trên cùng thiết bị, dataset, build type và network profile: selection-to-button-ready, predict latency, refresh latency, query count và bytes transferred.
- [ ] Selection-to-button-ready đạt mục tiêu p95 **≤ 500 ms** trên benchmark profile; đây là mục tiêu cần xác nhận bằng benchmark, không phải số liệu đã đo.
- [ ] Refresh và prediction có p50/p95 riêng, kèm số lần đọc toàn dataset và số request network; không kết luận “đã tối ưu” nếu chưa có before/after evidence.
- [ ] Predict không phát sinh thêm network call nếu dữ liệu odds pre-match hợp lệ đã có trong cache theo policy.
- [ ] Refresh chỉ tải và ghi dữ liệu cần cho local date, không tăng số request ngoài phạm vi đã đặc tả.

## Scope

- Tách selection state khỏi prediction state ở `PredictionViewModel`/`PredictionScreen`.
- Tối ưu cách chuẩn bị historical context và Dynamic Elo nhưng bảo toàn semantics.
- Tối ưu runtime refresh, cache freshness, network sync, Room transaction và loading state.
- Thêm benchmark, regression tests và correctness tests cần thiết để chứng minh hành vi.

## Out of Scope

- Không thay đổi sáu signals, weights, Candidate A, `WeightedScorer` hoặc thuật toán model.
- Không tuning threshold, calibration hoặc thay đổi kết quả prediction nhằm đạt latency tốt hơn.
- Không thay đổi semantics của pre-match odds, point-in-time filtering hoặc fallback policy.
- Không crawl bulk dataset lịch sử ngoài dữ liệu cần thiết cho implementation/benchmark.
- Không redesign tổng thể `PredictionScreen`.

## Test Scope

- **Presentation/unit:** selection không kích hoạt prediction/network; Predict kích hoạt đúng flow; cancellation khi đổi match nhanh; loading và error state.
- **Domain correctness:** so sánh result/evidence/weights của pipeline cũ và mới trên golden fixtures; Dynamic Elo, Form, Goals, H2H và odds đều giữ cùng cutoff trước kickoff.
- **Data/integration:** refresh dùng đúng local-date/UTC window, pagination, cache freshness, transaction atomicity và không bootstrap ngoài runtime scope.
- **Performance:** benchmark selection, prediction và refresh ở p50/p95; đo query count, full-dataset reads, network requests và bytes.

## Related Plan

- [prediction-pipeline-performance-optimization.md](../plans/prediction-pipeline-performance-optimization.md)
