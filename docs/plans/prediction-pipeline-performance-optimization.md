# Kế hoạch Triển khai: Prediction Pipeline Performance Optimization

## 1. Objective

Tách đường tương tác chọn trận khỏi đường tính prediction, sau đó giảm công việc lặp trong prediction và refresh mà không thay đổi nghiệp vụ. Kết quả phải được chứng minh bằng benchmark before/after và regression tests; mọi con số mục tiêu trong plan là **target dự kiến**, không phải kết quả đã đo.

## 2. Baseline và Invariants

### 2.1. Số liệu đã đo trong audit

| Flow | Baseline audit |
|---|---:|
| Pull-to-refresh | khoảng 15–20 giây |
| Chọn trận | khoảng 2–3 giây |
| Odds API trước Predict | Có |

Đây là số liệu quan sát của audit, chưa phải benchmark chuẩn hóa. Trước khi đánh giá phase performance, cần cố định thiết bị/emulator, build variant, dataset, cache state và network profile.

### 2.2. Invariants bắt buộc

- Giữ nguyên sáu signals, weights, Candidate A, `WeightedScorer` và kết quả prediction với cùng input.
- Historical features chỉ dùng dữ liệu strictly trước kickoff; không đưa target match, same-timestamp future matches hoặc live/post-kickoff odds vào context.
- Không đánh đổi correctness hoặc point-in-time semantics để giảm latency.
- Room vẫn là source of truth cho dữ liệu đã persist; lỗi network phải được biểu diễn rõ theo pattern hiện có.

## 3. Phase 0 — Contract Freeze và Instrumentation

### Phạm vi

Đóng băng behavioral contract trước khi tối ưu và bổ sung đo lường tối thiểu cho ba flow: selection, Predict và refresh. Phase này chỉ tạo evidence/test harness cần thiết, không tuning model.

### File dự kiến thay đổi

- [`PredictionViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt)
- [`PredictionScreen.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/PredictionScreen.kt)
- [`PredictionViewModelTest.kt`](../../app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModelTest.kt)
- Các test/benchmark mới trong module `app`, `core/domain` hoặc `core/data` theo harness hiện có.

### Các bước triển khai

1. Ghi lại state transition hiện tại: auto-select, user selection, refresh, Predict, cancellation và error.
2. Tạo golden fixtures cho cùng match, historical snapshot, odds snapshot và expected `PredictionResult`/evidence.
3. Thêm counters/timing ở ranh giới selection, prediction, repository reads và sync; không log dữ liệu nhạy cảm.
4. Xác định rõ test oracle cho sáu signals, weights, Candidate A và temporal cutoff.

### Rủi ro

- Instrumentation làm nhiễu timing nếu đặt trong critical section.
- Golden fixture không đại diện đủ cho cache hit/miss hoặc cold-start.

### Test

- Snapshot/regression tests cho `PredictionResult` và signal evidence.
- Test state transition và cancellation hiện có trong `PredictionViewModelTest`.
- Smoke check không thay đổi số lượng network call ngoài baseline trước khi sang P0.

### Acceptance criteria

- [ ] Có baseline report chứa p50/p95, query count, full-dataset read count, network request count và bytes cho ba flow.
- [ ] Golden fixtures xác nhận sáu signals, weights, Candidate A và point-in-time cutoff.
- [ ] Có thể phân biệt selection latency với prediction latency.

## 4. Phase P0 — Tách Selection State khỏi Prediction State

### Phạm vi

Click trận chỉ cập nhật selected match và dữ liệu hiển thị cần thiết. Không tính prediction, không hydrate odds và không chờ historical replay. Nút Predict xuất hiện ngay khi match selection hợp lệ.

### File dự kiến thay đổi

- [`PredictionViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt)
- [`PredictionScreen.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/PredictionScreen.kt)
- Prediction UI state/model và resource liên quan nếu cần, theo convention hiện có.
- [`PredictionViewModelTest.kt`](../../app/src/test/java/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModelTest.kt)

### Các bước triển khai

1. Tách `selectedMatch`/selection readiness khỏi `predictionResult` và prediction loading/error state.
2. Di chuyển trigger `fetchAndCacheOddsForMatch` khỏi selection flow sang explicit Predict action.
3. Chặn mọi `getAllMatches()`, Dynamic Elo và `predictMatchOutcomeUseCase` trong `onSelectMatch`.
4. Giữ cancellation cho Predict requests; khi đổi match, kết quả cũ không được ghi đè selection mới.
5. Giữ nguyên display của match card, filter, date và refresh.

### Rủi ro

- UI có thể tiếp tục quan sát prediction state cũ sau khi đổi match.
- Navigation có `navMatchId` hoặc auto-select có thể bỏ qua state transition mới.
- Double tap Predict có thể tạo request trùng.

### Test

- Chọn trận không gọi odds repository/API, không gọi use case và không đọc full dataset.
- Predict gọi pipeline đúng một lần và hiển thị loading/error đúng state.
- Đổi trận trong lúc Predict hủy/invalidates request cũ.
- Nút Predict visible/enabled ngay sau selection.

### Acceptance criteria

- [ ] Selection-to-button-ready p95 đạt mục tiêu ≤ 500 ms trên benchmark profile; kết quả phải có before/after evidence.
- [ ] Zero Odds API calls trước explicit Predict.
- [ ] Zero prediction calculation trước explicit Predict.
- [ ] Không thay đổi prediction result khi Predict cùng snapshot như baseline.

## 5. Phase P1 — Tối ưu Prediction Pipeline

### Phạm vi

Giảm việc đọc/replay toàn bộ dataset mỗi lần dự đoán và tối ưu historical query/Dynamic Elo mà vẫn giữ nguyên ordering, cutoff, tie-break và semantics point-in-time. Không thay đổi thuật toán model.

### File dự kiến thay đổi

- [`PredictionViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt)
- [`MatchDao.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/dao/MatchDao.kt)
- [`MatchRepository.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/repository/MatchRepository.kt)
- [`MatchRepositoryImpl.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/repository/MatchRepositoryImpl.kt)
- [`CalculateDynamicEloUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateDynamicEloUseCase.kt)
- [`PredictMatchOutcomeUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt)
- Domain/data tests liên quan đến query và chronological replay.

### Các bước triển khai

1. Đo riêng chi phí mapping Room-to-domain, full-dataset read, filter/sort history và Elo replay.
2. Thiết kế query/repository API lấy đúng historical subset cần cho target: ended matches, team IDs, H2H và cutoff trước kickoff.
3. Đánh giá cache/index hoặc accumulator cho Dynamic Elo; cache phải có key theo dataset snapshot và cutoff, không được tái sử dụng state sau cutoff.
4. Bảo toàn thứ tự `startTimeDate`, `id`, điều kiện score hợp lệ và initial Elo hiện tại.
5. So sánh context và output mới với golden fixtures trước khi bật đường tối ưu.
6. Giữ odds selector và fallback semantics hiện tại; P1 không mở rộng market hay thay đổi Candidate A.

### Rủi ro

- Query subset sai cutoff gây data leakage hoặc thay đổi Form/H2H.
- Cache Elo dùng sai snapshot/version làm stale hoặc không deterministic.
- Index/query mới trả thứ tự khác, ảnh hưởng tie-break và replay.
- Tối ưu memory có thể làm tăng allocation hoặc lock contention trên Room.

### Test

- DAO/repository tests cho strict-before cutoff, ended-only, team scope, H2H và deterministic ordering.
- Property/regression tests so sánh toàn bộ signal evidence và probabilities với baseline.
- Dynamic Elo tests cho same-timestamp batch, missing scores, initial map và target boundary.
- Benchmark full replay so với optimized path trên dataset cố định.

### Acceptance criteria

- [ ] Kết quả prediction và evidence khớp baseline trong tolerance đã định nghĩa cho mọi golden fixture; khác biệt phải được giải thích và được phê duyệt, không tự động chấp nhận.
- [ ] Không có historical input với timestamp >= target kickoff.
- [ ] Có số đo chứng minh giảm full-dataset reads/replay hoặc latency; không tuyên bố tối ưu trước benchmark.
- [ ] Cache invalidation/snapshot key được test và không tạo stale prediction.

## 6. Phase P2 — Tối ưu Refresh, Cache, Sync, Transaction và Loading

### Phạm vi

Giảm chi phí Pull-to-refresh bằng cách dùng đúng local-date sync path, cache freshness và network policy; tối ưu transaction/Room writes và làm rõ loading/error state. Không thay đổi dữ liệu prediction hoặc model.

### File dự kiến thay đổi

- [`PredictionViewModel.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt)
- [`MatchRepositoryImpl.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/repository/MatchRepositoryImpl.kt)
- [`DataSyncEngine.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt)
- [`MatchDao.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/dao/MatchDao.kt)
- [`DataFreshnessPolicy.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/cache/DataFreshnessPolicy.kt)
- [`PredictionScreen.kt`](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/PredictionScreen.kt)
- Data sync/repository/ViewModel tests.

### Các bước triển khai

1. Trace `refresh()` đến repository và xác nhận runtime dùng `syncMatchesForLocalDate` hoặc một path tương đương có local-date/UTC window và pagination đầy đủ.
2. Loại bootstrap leagues/seasons toàn cục khỏi critical path; chỉ giữ trigger explicit/background phù hợp với policy.
3. Giữ cache freshness và force refresh semantics; không bỏ qua refresh khi user yêu cầu force refresh.
4. Đánh giá batch size, transaction boundary và Room invalidation để tránh recomposition/loading thừa; chỉ thay đổi sau khi đo.
5. Chuẩn hóa `isRefreshing`, cancellation, retry failure và success event; không che giấu lỗi bằng success-shaped fallback.
6. Đo cache hit, cache miss, network retry, page count, transaction count và UI loading duration.

### Rủi ro

- Bỏ bootstrap có thể làm thiếu metadata nếu match payload không đủ league/team.
- Transaction quá lớn gây lock/UI delay; quá nhỏ làm tăng commit overhead.
- Cache TTL không phù hợp làm dữ liệu stale hoặc refresh gọi network dư thừa.
- Thay đổi loading state có thể ảnh hưởng Compose recomposition và accessibility.

### Test

- Runtime sync tests cho UTC window, pagination, cache hit/miss, force refresh và cancellation.
- Transaction atomicity tests cho leagues/teams/matches và failure giữa các page.
- Verify không gọi global competition bootstrap trong refresh path.
- UI/ViewModel tests cho loading, retry, error event và refresh race.

### Acceptance criteria

- [ ] Refresh path chỉ sync local-date data theo policy và không gọi global bootstrap ngoài trigger được chỉ định.
- [ ] Cache hit không tạo network request; force refresh vẫn bypass TTL đúng contract.
- [ ] Không mất hoặc tạo bản ghi partial khi transaction/page fail.
- [ ] Có benchmark before/after cho refresh p50/p95, request/page/transaction count; mục tiêu latency chỉ được kết luận sau khi đo.

## 7. Phase P3 — Benchmark, Regression và Correctness Sign-off

### Phạm vi

Đo end-to-end trước/sau trên cùng profile, chạy regression suite và xác nhận output/temporal correctness trước khi đề xuất merge. Phase này không sửa model để đạt số đẹp hơn.

### File dự kiến thay đổi

- Benchmark/test files được tạo hoặc cập nhật ở `app`, `core/data`, `core/domain`.
- Báo cáo kết quả trong issue/PR workflow hiện có; không tạo thêm tài liệu ngoài hai file của task này.

### Các bước triển khai

1. Chạy baseline và optimized build với cùng dataset, cache state, device/emulator, build variant và network profile.
2. Đo selection-to-button-ready, explicit Predict latency, refresh latency; báo cáo p50/p95 và sample count.
3. Đo network requests/bytes, Room query count, full-dataset reads, Elo replay size, transaction count và error/cancellation rate.
4. Chạy golden regression cho prediction probabilities, signal evidence, weights, Candidate A và fallback odds.
5. Chạy temporal leakage checks với target, same-timestamp và post-kickoff/live odds cases.
6. Nếu benchmark không đạt target hoặc output lệch, quay lại phase liên quan; không hạ acceptance criteria để hợp thức hóa kết quả.

### Rủi ro

- Benchmark không tái lập do network/device variance.
- Tolerance quá rộng che giấu thay đổi semantics.
- Test pass nhưng không bao phủ cache miss, empty history hoặc cancellation.

### Test

- Full targeted unit/integration suite của các module bị ảnh hưởng.
- Benchmark repeat đủ sample để có p50/p95.
- Golden output diff và temporal leakage audit.
- Regression cho refresh failure, retry, cancellation và stale state.

### Acceptance criteria

- [ ] Có bảng before/after với môi trường và sample count rõ ràng.
- [ ] P0–P2 behavioral criteria đều pass.
- [ ] Prediction output/evidence khớp baseline trong tolerance đã phê duyệt.
- [ ] Không có evidence của data leakage hoặc odds API trước Predict.
- [ ] Chỉ khi evidence đầy đủ mới kết luận phase performance đạt; nếu chưa benchmark thì ghi rõ “chưa xác nhận”.

## 8. Requirement Mapping

| Requirement | Phase |
|---|---|
| Tách selection khỏi prediction, không gọi odds trước Predict, nút Predict xuất hiện ngay | P0 |
| Tối ưu historical query và Dynamic Elo, bảo toàn semantics | P1 |
| Tối ưu refresh, cache, network sync, Room transaction và loading | P2 |
| Benchmark before/after, regression và correctness sign-off | P3 |
| Giữ sáu signals, weights, Candidate A, prediction result và point-in-time | P0–P3 |
