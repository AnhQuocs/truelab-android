# Prediction On-Demand Odds Hydration

## Status
OPEN

## Severity
HIGH

## Problem
Tín hiệu `1X2 Odds` (chiếm trọng số **20%** trong mô hình dự đoán `WeightedScorer`) thường xuyên rơi vào trạng thái `Unavailable` (không có dữ liệu), khiến mô hình dự đoán buộc phải loại bỏ tỷ lệ xác suất thị trường và chỉ dựa vào 5 tín hiệu còn lại (`Form`, `Elo`, `Goals`, `H2H`, `HomeAdvantage`). Giao diện `PredictionBottomSheet` hiển thị cảnh báo thiếu dữ liệu tỷ lệ cược 1X2 cho hầu hết các trận đấu trong ngày.

---

## Confirmed TrueScore Evidence
Dựa trên kết quả audit hệ sinh thái TrueScore:
1. **Danh sách trận hàng ngày không kèm Odds**: Endpoint `GET /sport/v1.0/matches?date=...` chỉ trả về thông tin lịch thi đấu và tỷ số, không chứa danh sách Odds.
2. **Odds được cung cấp qua Endpoint riêng**: Hệ thống cung cấp dữ liệu tỷ lệ qua endpoint chuyên biệt `GET /sport/v1.0/matches/{matchId}/odds`.
3. **Pre-Match Odds thực sự tồn tại**:
   - Trước giờ thi đấu, API cung cấp các bản ghi với `market_phase = "initial"` hoặc `market_phase = "immediate"`.
   - Các snapshot này có mốc thời gian $\text{change\_time} < \text{kickoff}$.
4. **Hỗ trợ Đa Thị trường (Multi-Market)**: Một request duy nhất tới endpoint trên trả về đồng thời các loại kèo:
   - Kèo Châu Âu 1X2 (`odds_type = "eu"`)
   - Kèo Châu Á Handicap (`odds_type = "asia"` hoặc `"as"`)
   - Kèo Tổng số bàn thắng Over/Under (`odds_type = "bs"`)
5. **Dữ liệu Live Odds phân tách rõ ràng**: Các tỷ lệ biến động trong trận có `market_phase` là `"rolling_ball"` hoặc `"in_play"`, cho phép bộ lọc nhận diện và loại trừ tuyệt đối để tránh rò rỉ dữ liệu trước trận (Zero Data Leakage).

---

## Root Cause
- **Thiếu cơ chế On-Demand Hydration**: TrueLab hiện tại chỉ đồng bộ danh sách trận đấu và thông tin giải đấu/đội bóng trong quá trình daily match sync.
- Khi người dùng chọn xem dự đoán một trận đấu cụ thể trên `PredictionScreen`, ứng dụng chỉ truy vấn dữ liệu Odds từ Room database cục bộ (`oddsRepository.getMatchOdds(matchId)`). Vì daily sync không tải Odds, bảng `odds` trong Room không có dữ liệu của trận đó.
- Ứng dụng chưa có cơ chế chủ động gọi API `GET /sport/v1.0/matches/{matchId}/odds` theo nhu cầu (On-Demand) khi người dùng mở chi tiết dự đoán.

---

## Proposed Direction
1. **On-Demand Fetching**: Khi người dùng chọn hoặc xem dự đoán một trận đấu, kích hoạt luồng fetch Odds của trận đó từ `GET /sport/v1.0/matches/{matchId}/odds`.
2. **Pre-Match Odds Selection**: Lọc snapshot kèo Châu Âu (`eu`) hợp lệ trước giờ bóng lăn ($\text{changeTime} < \text{kickoff}$ và `marketPhase` không phải là `rolling_ball`/`in_play`).
3. **Persistence & Caching**: Lưu trữ các bản ghi nhận được (`eu`, `asia`, `bs`) vào Room DB thông qua `OddsDao` để tái sử dụng, ngăn ngừa việc gọi API lặp lại không cần thiết.
4. **Integration vào Existing Pipeline**: Nạp trực tiếp bản ghi `eu` đã chọn vào `OddsSignalTransformer.transform(...)` trong `PredictMatchOutcomeUseCase` để kích hoạt đầy đủ 20% trọng số của tín hiệu Odds.

---

## Scope
- **Kèo Châu Âu 1X2 (`eu`)**: Tích hợp đầu cuối vào pipeline tính toán dự đoán và hiển thị bằng chứng trên giao diện `PredictionBottomSheet`.
- **Nền tảng Kèo Châu Á (`asia`) & Tổng số bàn thắng (`bs`)**: Tải về, chuẩn hóa và lưu trữ vào Room DB nhằm chuẩn bị sẵn dữ liệu cho giai đoạn mở rộng tín hiệu (Odds Signal Expansion) sau này.
- **Xử lý Cache & Concurrency**: Quản lý coroutine an toàn, tự động huỷ bỏ request cũ khi người dùng chuyển đổi trận đấu liên tục, chống race condition.

---

## Out of Scope
- **KHÔNG** crawl hoặc tải bulk Odds cho toàn bộ 30,000 trận đấu trong dataset lịch sử.
- **KHÔNG** thay đổi thuật toán dự đoán cốt lõi (`WeightedScorer`) hay bảng cấu hình trọng số (`PredictionWeightConfig`).
- **KHÔNG** đưa Kèo Châu Á (AH) và Tổng số bàn thắng (O/U) vào công thức tính điểm của `WeightedScorer` trong giai đoạn này.
- **KHÔNG** thêm mô hình Machine Learning hoặc thay đổi cấu trúc database asset ban đầu.
- **KHÔNG** yêu cầu kiểm thử trên thiết bị vật lý (Physical Device Testing).

---

## Acceptance Criteria
- [ ] Khi người dùng chọn trận đấu trên `PredictionScreen`, hệ thống có thể kích hoạt lấy Odds on-demand cho trận đó.
- [ ] Endpoint `GET /sport/v1.0/matches/{matchId}/odds` được gọi chính xác theo `matchId`.
- [ ] Chỉ có Pre-Match Odds được chọn cho mô hình dự đoán (loại bỏ tuyệt đối `rolling_ball` và `in_play`).
- [ ] Bản ghi Kèo Châu Âu 1X2 được chuyển đổi thành công qua `OddsSignalTransformer`.
- [ ] Trọng số 20% của Odds được kích hoạt đầy đủ trong `PredictionResult` khi có dữ liệu.
- [ ] Khi không có Odds hoặc lỗi mạng, ứng dụng fallback an toàn (weight = 0.0), không bị crash.
- [ ] Dữ liệu `asia` và `bs` được lưu trữ an toàn trong Room DB nhưng không làm thay đổi kết quả tính điểm hiện tại.
- [ ] Kiểm soát chống trùng lặp request và huỷ bỏ request nền khi người dùng đổi trận nhanh.
- [ ] 100% Unit Tests và Integration Tests mức mã nguồn vượt qua.

---

## Test Scope (Code-Level Only)
- **Unit Tests**:
  - DTO serialization & deserialization cho multi-market (`eu`, `asia`, `bs`).
  - Mapper `OddsRecord` $\leftrightarrow$ `OddsEntity` $\leftrightarrow$ `OddsRecordItem`.
  - Nghiệp vụ chọn lọc Pre-Match Odds (`PreMatchOddsSelector`): loại trừ post-kickoff, loại trừ live phase, ưu tiên snapshot gần nhất.
  - `OddsSignalTransformer`: kiểm tra tính xác suất và fallback khi thiếu dữ liệu.
- **Integration Tests**:
  - Luồng dữ liệu `OddsApi` $\rightarrow$ `OddsRepository` $\rightarrow$ `OddsDao` $\rightarrow$ `PredictMatchOutcomeUseCase`.
  - Kiểm tra tính ổn định khi API trả về rỗng hoặc gặp sự cố mạng.

---

## Related Plan
- [prediction-on-demand-odds-plan.md](../plans/prediction-on-demand-odds-plan.md)
