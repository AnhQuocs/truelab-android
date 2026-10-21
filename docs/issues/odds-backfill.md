# Issue: Controlled Odds Backfill cho Nghiên Cứu Candidate C

## Trạng thái
OPEN

## Mức độ ưu tiên
HIGH (Blocker cho việc mở rộng kiểm chứng Candidate C trên quy mô thống kê tin cậy)

---

## 1. Context (Bối cảnh)

Trong hệ thống dự đoán bóng đá của TrueLab, dữ liệu tỷ lệ cược (Odds) đóng vai trò kép:
1. **Production Baseline Signal:** Tín hiệu Kèo Châu Âu 1X2 (`OddsSignalTransformer`) chiếm **20% trọng số** trong `WeightedScorer`. Khi thiếu Odds, hệ thống buộc phải kích hoạt cơ chế tái chuẩn hóa trọng số (Linear Renormalization), làm tăng tỷ trọng của Form ($25\% \to 31.25\%$) và Elo ($20\% \to 25\%$), từ đó khuếch đại Favorite Bias (thổi phồng tỷ lệ Home Win và triệt tiêu xác suất Draw).
2. **Nghiên cứu Candidate C:** Nghiên cứu Candidate C (Asian Handicap Implied Spread & Over/Under Goal Expectancy) được thiết kế nhằm kiểm chứng xem hai thị trường cược phụ này có bổ sung giá trị gia tăng (Incremental Value) độc lập cho bài toán phân loại kết quả trận đấu hay không.

Tuy nhiên, nghiên cứu Candidate C trước đây chỉ thực hiện trên tập dữ liệu cố định $N = 791$ trận có sẵn Odds trong Room DB (tập này được hình thành do người dùng duyệt app hoặc bấm backtest on-demand). Cohort $N = 791$ không đủ lớn để phân rã kiểm định ngoài mẫu (Holdout) kết hợp Paired Bootstrap một cách vững chắc.

---

## 2. Problem Statement (Vấn đề & Hạn chế dữ liệu hiện tại)

1. **Độ phủ Odds cục bộ cực thấp:** Cơ sở dữ liệu hiện tại của TrueLab có **29,666 trận đã kết thúc** (`runtime_db.db`), nhưng chỉ có **115 đến 133 trận** có bản ghi Odds tương ứng. Con số 791 trận trước đây chỉ tồn tại trên thiết bị kiểm thử cũ. Hơn 99% số trận trong database hoàn toàn thiếu Odds.
2. **Cửa sổ lưu trữ hữu hạn của Upstream (Retention Horizon):** Spike kiểm tra thực tế trên 30 trận và các mùa giải của Ngoại hạng Anh (EPL) đã xác minh rằng: Upstream backend **chỉ lưu trữ Odds trong khoảng ~8 tháng gần nhất (từ 06/02/2026 đến nay)**. Các trận đấu diễn ra trước tháng 02/2026 hoặc thuộc các mùa giải 2019–2025 đều trả về danh sách rỗng (`data: []`), dù thông tin trận đấu và tỷ số vẫn còn đầy đủ.
3. **Thách thức trích xuất Pre-match Odds (Live Odds Overwrite):** Endpoint snapshot `GET /sport/v1.0/matches/{matchId}/odds` trả về **hơn 90% là kèo live (`rolling_ball`)** cho các trận đã kết thúc. Pre-match odds của các nhà cung cấp lớn (Bet365, Crown) bị ghi đè bởi kèo phút 90 ($D=1.01$). Để lấy được Pre-match Odds thực sự (Closing Odds), hệ thống **bắt buộc phải truy vấn chuỗi lịch sử biến động** từ `GET /sport/v1.0/matches/{matchId}/odds-history`.

---

## 3. Goal (Mục tiêu)

Xây dựng và thực thi quy trình **backfill có kiểm soát (Controlled Backfill)** để bổ sung dữ liệu Odds Pre-match hợp lệ (gồm 1X2, Asian Handicap và Over/Under) từ Upstream API vào cơ sở dữ liệu cục bộ, tạo ra một **Dataset Cohort chuẩn hóa ($N \ge 3,000 - 5,000$ trận)** phục vụ kiểm chứng thực nghiệm Candidate C và tối ưu hóa hệ thống dự đoán.

---

## 4. Scope (Phạm vi công việc)

- **Thu thập dữ liệu chọn lọc:** Chỉ nhắm vào các trận đấu đã kết thúc thuộc cửa sổ lưu trữ nóng ($\ge 06/02/2026$) và thuộc **Top 25–35 giải đấu chuyên nghiệp** (EPL, La Liga, Serie A, Bundesliga, Ligue 1, MLS, Brazil Serie A, Cúp C1/C2/C3...).
- **Benchmark Bookmaker:** Tập trung trích xuất Closing Pre-match Odds của **Bet365 (`company_id = 2`)** — nhà cung cấp có thanh khoản cao nhất và hỗ trợ đầy đủ cả 3 loại kèo `eu`, `asia`, `bs`.
- **Bảo đảm Point-in-Time:** Thiết lập bộ lọc chặt chẽ $T_{\text{change}} \le T_{\text{kickoff}}$ và loại trừ tuyệt đối `market_phase = 'rolling_ball'`.
- **Pilot & Safety Isolation:** Thực hiện Pilot nhỏ ($N = 50$ trận) lưu vào SQLite độc lập (`scratch/pilot_odds.db`) trước khi xem xét tích hợp vào Room DB chính.

---

## 5. Non-Scope (Ngoài phạm vi)

- **KHÔNG** cố gắng crawl Odds cho toàn bộ 29,666 trận (vì 9,000+ trận trước 02/2026 đã được xác minh là không có Odds trên upstream).
- **KHÔNG** hỗ trợ scraping/backfill đa nhà cung cấp hàng loạt (> 10 bookmakers) trong giai đoạn này.
- **KHÔNG** can thiệp hoặc sửa đổi mã nguồn Production (`PredictMatchOutcomeUseCase`, `WeightedScorer`, `Candidate A`).
- **KHÔNG** xây dựng hạ tầng crawler phức tạp phục vụ production người dùng thực tế; chỉ dùng script tự động phục vụ nghiên cứu offline.

---

## 6. Risks & Mitigations (Rủi ro & Biện pháp)

| Rủi ro | Mức độ | Hậu quả tiềm ẩn | Biện pháp kiểm soát |
|---|:---:|---|---|
| **Data Leakage (Rò rỉ kèo trong trận)** | **CRITICAL** | Lẫn kèo `rolling_ball` khiến mô hình học dữ liệu tương lai, thổi phồng độ chính xác giả tạo. | Ép điều kiện lọc cứng: $T_{\text{change}} \le T_{\text{kickoff}}$ VÀ `market_phase != 'rolling_ball'`. Kiểm toán 100% bản ghi pre-match. |
| **API Rate Limit / IP Ban** | **HIGH** | Bị Cloudflare WAF chặn IP hoặc trả mã `429/403`. | Khởi đầu Pilot ở tốc độ thận trọng **1 request/giây**, tích hợp Exponential Backoff và Circuit Breaker dừng ngay nếu gặp `429/403`. |
| **Bùng nổ băng thông (Network Bloat)** | **MEDIUM** | Payload `/odds-history` nặng (300–500 KB/request) làm tăng dung lượng tải về tới hàng Gigabytes. | Truy vấn đích danh `?company_id=2` để thu hẹp số bản ghi từ 52,000 xuống ~4,000 dòng/trận. |
| **Ghi đè hoặc hỏng DB chính** | **HIGH** | Corrupt database hoặc làm sai lệch 115 trận Odds sẵn có. | Chạy hoàn toàn trên Staging DB / SQLite trung gian; chỉ merge vào Room DB khi vượt qua nghiệm thu. |

---

## 7. Definition of Done & Acceptance Criteria (Tiêu chí nghiệm thu)

Nhiệm vụ được coi là hoàn thành khi đạt đủ các tiêu chí sau:

1. **Khả năng tái dựng Closing Odds Pre-match:** Trích xuất được bộ 3 kèo hợp lệ cho mỗi trận:
   - European 1X2: có đủ $H, D, A > 1.0$.
   - Asian Handicap: có mức chấp $Handicap$ và $H, A > 0$.
   - Over/Under: có mức tổng bàn thắng $Handicap$ và $Over, Under > 0$.
2. **Zero Point-in-Time Leakage:** $100\%$ các bản ghi được lưu trữ có $T_{\text{change}} \le T_{\text{kickoff}}$ và `market_phase` thuộc tập hợp `{'initial', 'instant', ''}`.
3. **Tỷ lệ hoàn thành Pilot ($N = 50$):**
   - Đạt tỷ lệ có Odds $\ge 80\%$ trên các giải đấu lớn.
   - Tỷ lệ lỗi mạng/HTTP error $= 0\%$.
4. **Cohort Đánh Giá Sẵn Sàng:** Cung cấp được một dataset có ít nhất **$N \ge 3,000$ trận** có đủ 3 loại kèo pre-match, sẵn sàng để nạp vào pipeline đánh giá thực nghiệm của Candidate C.
