# AH / O-U Feasibility Research

**Ngày:** 2026-10-07
**Kết luận:** **NOT READY — MORE DATA EVIDENCE REQUIRED**
**Phạm vi:** chỉ xác minh semantics, chất lượng dữ liệu, khả năng chọn snapshot và khả năng chuẩn hóa Asian Handicap (`asia`) và Over/Under (`bs`). Không thiết kế/triển khai candidate, không sửa production, không calibration/tuning, không tạo issue.

## Quy ước mức độ bằng chứng

- **VERIFIED** — thấy trực tiếp trong code/schema hoặc đo trực tiếp trên snapshot Room.
- **INFERRED** — phù hợp với tên market và dạng dữ liệu, nhưng chưa có contract/source độc lập xác nhận.
- **UNKNOWN** — evidence hiện có không đủ để xác định; không dùng như assumption.

## 1. Tóm tắt quyết định

| Câu hỏi | Kết luận | Lý do chính |
|---|---|---|
| AH có khả thi để đi tiếp? | **PARTIALLY** | Có line số, hai phía và dữ liệu pre-kickoff. Chưa xác minh perspective của line, quy ước giá, settlement và ý nghĩa các snapshot theo bookmaker. |
| O/U có khả thi để đi tiếp? | **PARTIALLY** | Có line số và cặp `over`/`under` pre-kickoff. Total-goals là suy luận hợp lý, nhưng market period, odds convention và settlement chưa được xác nhận. |
| Normalization có khả thi? | **PARTIALLY** | Có thể giữ tuple thô `(line, price fields, company, time)`. Chưa đủ căn cứ đổi giá thành implied probability hoặc loại margin. |
| Dùng historical theo point-in-time có đủ bảo vệ? | **LIMITED** | `changeTime < kickoff` cho event-time filter; không có `fetchedAt`/`receivedAt` để chứng minh availability trước kickoff. |
| Overall | **NOT READY — MORE DATA EVIDENCE REQUIRED** | Cần evidence từ contract/feed và thời điểm ingestion trước khi thực hiện research dựa trên normalized market data. |

Candidate B vẫn đóng/rejected; Candidate A và production default không nằm trong phạm vi thay đổi. Không có Candidate C, issue, code change, commit hoặc push trong task này.

## 2. Data source và cohort

Room database được đọc chỉ-đọc từ thiết bị Android đang kết nối, gồm file DB cùng WAL/SHM. Snapshot có `user_version=5` và `PRAGMA integrity_check` trả `ok`. Dữ liệu được đọc trực tiếp từ `dev.anhquocs.truelab`; không gọi Odds API.

Snapshot hiện có 30.485 trận và 121.506 odds rows:

| `oddsType` | Raw rows | Match IDs có row | Company IDs |
|---|---:|---:|---:|
| `asia` | 30.172 | 1.085 | 15 |
| `bs` | 41.707 | 1.089 | 16 |
| `eu` | 42.429 | 1.089 | 17 |
| `cr` | 7.198 | 808 | 7 |

Frozen cohort dùng trong lần kiểm tra này lấy 791 match IDs duy nhất từ prediction artifact đã lưu `truelab-candidate-a-historical-odds-2026-10-07-candidate-a-predictions.tsv`, khung kickoff `2026-09-12T17:00:00Z` đến `2026-10-06T23:30:00Z`. Cả 791 IDs đều còn trong Room snapshot. Artifact ghi nhận EU odds available cho cả 791; điều này chỉ mô tả EU cohort, không chứng minh lịch sử availability tại thời điểm kickoff.

Định nghĩa coverage trong bảng dưới đây:

- **Structurally selectable**: áp dụng điều kiện market, `changeTime < kickoff`, phase không thuộc live blocklist, và điều kiện nullable-field của selector hiện tại.
- **Positive price pair**: cả hai price field đều có giá trị dương. Đây là kiểm tra dữ liệu số, **không** xác nhận odds convention hoặc tính hợp lệ theo contract.

| Market | Rows thỏa filter pre-match cấu trúc | Match coverage | Snapshot chọn có cặp giá dương | Diễn giải |
|---|---:|---:|---:|---|
| AH (`asia`) | 3.274 | 645/791 (81,5%) | 644/791 (81,4%) | Một snapshot được selector chấp nhận có giá `0/0`; không xem nó là usable price pair. |
| O/U (`bs`) | 3.108 | 553/791 (69,9%) | 553/791 (69,9%) | Cả snapshot được chọn đều có hai số dương, nhưng convention vẫn chưa biết. |

Coverage ở đây là event-time/field coverage, không phải chứng minh point-in-time availability, cũng không phải xác nhận có thể suy ra xác suất.

## 3. Trace lifecycle trong implementation

```text
Odds API DTO
  → mapper giữ nguyên field
  → Room odds table
  → DAO/repository đọc snapshot
  → PreMatchOddsSelector (có nhánh asia/bs)
  → OddsRecordItem (các số vẫn giữ nguyên)
```

Nguồn chính:

- DTO/API: [`OddsDto.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/remote/dto/OddsDto.kt), [`OddsApi.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/remote/api/OddsApi.kt)
- Mapper: [`RoomMappers.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt)
- Room schema: [`OddsEntity.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/local/entity/OddsEntity.kt)
- Truy vấn/repository: [`OddsDao.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/local/dao/OddsDao.kt), [`OddsRepositoryImpl.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/repository/OddsRepositoryImpl.kt)
- Filter và model: [`PreMatchOddsSelector.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/selector/PreMatchOddsSelector.kt), [`Odds.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/model/Odds.kt)

| Mục | Trạng thái | Evidence |
|---|---|---|
| DTO có `oddsType`, `handicap`, `homeWin`, `awayWin`, `over`, `under`, `marketPhase`, `companyId`, `changeTime` | **VERIFIED** | Các field được decode từ JSON key tương ứng. |
| Mapper/Room/domain giữ nguyên số và market fields, không đổi convention/đơn vị | **VERIFIED** | Mapping là field-to-field; không có transform AH/O-U. |
| `companyId` được lưu; Room giữ `company.name` nhưng không lưu `CompanyInfo.shortName` | **VERIFIED** | Mapper `companyName = company?.name ?: "Unknown"`; Entity không có `shortName`. |
| `changeTime` lưu dưới `Long`/SQLite integer và selector so sánh với kickoff epoch | **VERIFIED** | Model, schema, query và snapshot cho thấy giá trị khớp Unix epoch seconds. |
| Ý nghĩa odds convention do API provider định nghĩa | **UNKNOWN** | Không tìm thấy provider contract hoặc API capture gốc trong repo. |
| Các JSON examples trong unit test mô tả đúng feed thật | **UNKNOWN** | Test chỉ xác minh deserialize/mapping bằng fixture tự viết; không xác nhận provider semantics. |

`DATA_API_INTERNAL.md` ghi chú nội bộ rằng `asia` là Chấp và `bs` là Tài/Xỉu, nhưng không định nghĩa perspective, odds format, settlement hay period; phần `change_time` còn ghi “seconds/ms”. Vì vậy, tài liệu đó xác nhận mapping tên market trong dự án, không phải contract đủ để giải mã giá.

## 4. Asian Handicap (`asia`)

### 4.1 Line / handicap

**VERIFIED — dạng lưu:** Room có `handicap` kiểu `REAL`; tất cả 30.172 AH rows hiện có đều có line. Mọi line nằm trên lưới bội số 0,25.

| Granularity trên toàn Room DB | Rows |
|---|---:|
| Whole line | 8.724 |
| Half line, không phải whole | 5.603 |
| Quarter line, không phải half/whole | 15.845 |

Trong các snapshot được chọn trên frozen cohort có 93 whole, 320 half và 232 quarter lines. Các giá trị như `-0.25`, `+0.25`, `-0.5`, `-0.75` có thể biểu diễn nhất quán thành số/quarter-unit ở tầng lưu trữ.

**UNKNOWN — chiều và settlement:** code không ghi line là handicap của Home hay Away, không có field `homeHandicap`/`awayHandicap`, và không ghi split-stake/refund/half-win semantics. Tên `homeWin` và `awayWin` chỉ gắn giá vào hai phía theo key; không chứng minh dấu line thuộc perspective nào. Có thể canonicalize cú pháp số, nhưng **chưa thể canonicalize ý nghĩa** hoặc diễn giải một line âm/dương là đội nào chấp.

Quarter/whole-line behavior theo quy tắc cá cược phổ biến không được xem là đã xác minh cho feed này chỉ từ các giá trị `Double`.

### 4.2 Giá `homeWin` / `awayWin`

**VERIFIED — số liệu snapshot:**

- Hai cột có mặt trên 30.172 rows.
- `homeWin`: 10 giá bằng 0; 22.401 giá trong `(0, 1]`; 7.761 giá lớn hơn 1.
- `awayWin`: 3 giá bằng 0; 22.960 giá trong `(0, 1]`; 7.209 giá lớn hơn 1.
- Trong các cặp giá: 15.199 rows có cả hai trong `(0,1]`; 14.963 rows có một phía `(0,1]` và phía kia `>1`; không có cặp nào có cả hai `>1`.
- Giá lớn nhất quan sát được là 14,59.

Trong 645 snapshot AH được chọn của cohort: 385 cặp có cả hai giá trong `(0,1]`, 259 cặp là hỗn hợp một giá `<=1` và một giá `>1`, và một snapshot có `homeWin=0`, `awayWin=0` (match `765100`, company `Easybets`, line `-1.0`, phase `initial`). Selector vẫn chấp nhận snapshot đó vì line hiện diện.

**UNKNOWN:** các giá này là decimal, Hong Kong, Malay, Indonesian, payout/return khác hay encoding riêng; giá >1 có ý nghĩa gì; 0 là sentinel, lỗi feed hay giá hợp lệ trong contract; margin/overround được mã hóa ra sao. Không đủ evidence để tính implied probability hay no-vig probability. Không áp dụng `1 / price`.

### 4.3 Kết luận khả thi AH

**PARTIALLY.** Đủ evidence để giữ và phân tích mô tả raw tuple `(line, homeWin, awayWin, companyId, changeTime, marketPhase)`, với cutoff event-time. Chưa đủ evidence để tạo representation xác suất reproducible hoặc diễn giải line/sign/settlement. Trước đó cần contract/sample từ upstream theo bookmaker và giải thích cụ thể về line perspective, price format, zero values, quarter-line settlement và line identity.

## 5. Over/Under (`bs`)

### 5.1 Total line

**VERIFIED — dạng lưu:** Room có `handicap`, `over`, `under`; cả 41.707 rows có line. Tất cả line đều nằm trên lưới 0,25.

| Granularity trên toàn Room DB | Rows |
|---|---:|
| Whole line | 6.314 |
| Half line, không phải whole | 20.506 |
| Quarter line, không phải half/whole | 14.887 |

Trong frozen cohort, snapshot được chọn có 97 whole, 184 half và 272 quarter lines.

**INFERRED:** `bs` là market Tài/Xỉu và `handicap` là total-goals line; các tên field và các line thường gặp như 2.25/2.5/2.75 phù hợp với cách hiểu này.
**UNKNOWN:** upstream xác nhận đó là tổng số bàn thắng của cả trận hay một period/market scope khác; quy tắc settlement/push của whole/quarter lines; line nào được chọn khi provider có nhiều line cùng thời điểm.

### 5.2 Giá `over` / `under`

**VERIFIED — số liệu snapshot:**

- Cả hai field đều khác null trên 41.707 rows.
- `over`: 26.553 giá trong `(0,1]`; 15.154 giá lớn hơn 1; range 0,01–35.
- `under`: 225 giá bằng 0; 36.108 giá trong `(0,1]`; 5.374 giá lớn hơn 1; range 0–10.
- Có 21.179 rows mà cả hai giá trong `(0,1]`, 20.303 rows có một giá `(0,1]` và giá kia `>1`; không có cặp nào cả hai `>1`.
- Trong frozen cohort, cả 553 snapshot được chọn có giá dương ở cả hai phía; không vì thế mà convention được xác nhận.

**UNKNOWN:** odds convention, cách chuyển đổi sang xác suất, cách xử lý 0 và các extreme values, market margin và settlement. Các giá >1 tới 35 được ghi nhận là outlier cần giải thích, không tự kết luận là lỗi hoặc giá trị hợp lệ.

### 5.3 Kết luận khả thi O/U

**PARTIALLY.** Có coverage pre-match đáng kể và raw tuple đủ để lưu line cùng hai giá. Chưa đủ evidence để khẳng định `handicap` là total goals ở period mong muốn hoặc chuyển `over`/`under` thành implied probability. Cần upstream contract/sample theo market và bookmaker, gồm period, odds format, settlement và nghĩa của zero/extreme prices.

## 6. Normalization feasibility

| Hướng biểu diễn | Trạng thái | Ghi chú |
|---|---|---|
| Lưu raw line + raw two-side prices + company/time/phase | **YES** | Đây chính là dữ liệu hiện có; không đòi hỏi odds interpretation. |
| Chuẩn hóa cú pháp line thành quarter units | **YES, syntactic only** | Tất cả line hiện có nằm trên lưới 0,25; không suy ra perspective/settlement. |
| `1 / odds` hoặc normalize reciprocal như EU | **NO với evidence hiện tại** | Ở cả hai market, không có cặp hai giá `>1`; dùng công thức decimal EU sẽ không có căn cứ. |
| No-vig / overround removal | **NO với evidence hiện tại** | Cần odds convention và outcome/push/quarter settlement semantics trước khi chọn phép biến đổi. |
| Market imbalance / implied probability | **NO với evidence hiện tại** | Không thể diễn giải hai con số thành xác suất hoặc so sánh chéo bookmaker một cách hợp lệ. |
| Line-relative price representation | **PARTIAL** | Có thể báo cáo raw line và raw prices theo từng record; ý nghĩa sign, payoff và line comparability chưa được xác nhận. |

Không tạo công thức thay thế hoặc proxy trong task này.

## 7. Point-in-time feasibility (phạm vi giới hạn)

`PreMatchOddsSelector` có sẵn method riêng cho AH và O/U:

- AH nhận `asia`/`as`, yêu cầu `changeTime < kickoff`, loại phase `rolling_ball`, `in_play`, `live`, `running`, `inplay`; payload chỉ cần có line **hoặc** cặp giá không null.
- O/U nhận `bs`, áp cùng cutoff/phase filter; payload cần `over` và `under` khác null nhưng không yêu cầu dương.
- Snapshot xếp theo phase (`immediate`/`instant`/rỗng trước `initial`) rồi `changeTime` mới nhất. Selector chọn một row cho mỗi market, không tổng hợp hay giữ consensus per bookmaker.

Selector có thể tái sử dụng để enforcing **event-time** cho hai market, nhưng không chứng minh giá/line hợp lệ và tie-break giữa bookmaker không được xác định đầy đủ. DAO bulk hiện chỉ có query cho EU. `getLatestOddsForMatch()` lấy latest per company/market mà không tự lọc kickoff; phải có filter tiếp theo nếu dùng dữ liệu đó.

Trên frozen cohort trong Room snapshot:

- Có 51 AH rows và 66 O/U rows mang phase `initial`/`instant` nhưng timestamp sau kickoff. Chỉ lọc phase sẽ để chúng lọt; strict cutoff loại chúng.
- AH selector cấu trúc chọn một row `0/0` như mô tả ở trên, cho thấy phase/time filter không thay thế validation giá.

**VERIFIED:** Entity/DTO không có `fetchedAt`, `receivedAt` hoặc ingestion timestamp; repository persist odds response mà không ghi thời điểm nhận.
**Kết luận:** **LIMITED.** `changeTime < kickoff` là event-time condition, không phải bằng chứng app đã nhận dữ liệu trước kickoff. Historical availability-time chưa chứng minh được.

Schema có unique index `(matchId, companyId, oddsType, changeTime)` nhưng không gồm `handicap`; insert dùng `REPLACE`. Vì vậy, nếu upstream gửi nhiều line cho cùng match/company/type/time, các row có thể xung đột/ghi đè. Đây là rủi ro cấu trúc đã xác minh từ schema; chưa có API capture để kết luận upstream thực sự gửi nhiều line cùng timestamp.

## 8. Exploratory analysis và giới hạn

Không chạy correlation, MI, significance test hay outcome analysis mới trong feasibility này: odds convention, line semantics và settlement chưa đủ rõ để định nghĩa input có ý nghĩa. Coverage/distribution ở trên là kiểm tra cấu trúc, không phải evidence về predictive value.

Kết quả exploratory N=125 Pinnacle từ Odds Market Audit trước đó vẫn chỉ là phân tích mô tả, không xác nhận semantics hoặc incremental information đã chuẩn hóa. Không dùng nó để biện minh cho signal hay kết luận rằng AH/O/U cải thiện Draw detection.

## 9. Evidence còn thiếu trước khi tiếp tục

1. Tài liệu/API contract hoặc provider-confirmed fixture giải thích từng `oddsType`, unit của `changeTime`, odds convention và ý nghĩa giá 0/outlier theo `companyId`.
2. Contract xác định AH handicap perspective (Home/Away), settlement của whole/half/quarter line và cách chọn/định danh line.
3. Contract xác định O/U scope/period, total unit, settlement/push và odds convention.
4. Một nguồn point-in-time hoặc ingestion timestamp để phân biệt event time với thời điểm dữ liệu thực sự khả dụng.
5. Bằng chứng upstream về uniqueness của line theo `(match, company, market, changeTime)` hoặc một định danh line riêng; xác minh việc unique index hiện tại có làm mất line hay không.

## 10. Kết luận cuối

- **AH:** **PARTIALLY feasible** — raw line/two-side data và event-time filtering tồn tại; odds/sign/settlement chưa đủ rõ để normalize.
- **O/U:** **PARTIALLY feasible** — raw total-line/two-side candidate data và event-time filtering tồn tại; total scope/odds/settlement chưa đủ rõ để normalize.
- **Normalization:** **PARTIALLY feasible** chỉ với dữ liệu raw/syntactic; implied probabilities và no-vig **chưa khả thi** theo evidence hiện có.
- **Point-in-time:** **LIMITED** — event-time cutoff có thể áp dụng; availability-time không được chứng minh.
- **Overall:** **NOT READY — MORE DATA EVIDENCE REQUIRED**. Chưa có cơ sở để tiến vào research sử dụng normalized AH/O/U signals. Không có kết luận nhân quả hoặc predictive claim.

## Sources

- API DTO và API surface: [`OddsDto.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/remote/dto/OddsDto.kt), [`OddsApi.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/remote/api/OddsApi.kt)
- Mapper/Room: [`RoomMappers.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt), [`OddsEntity.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/local/entity/OddsEntity.kt), [`Migrations.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/database/Migrations.kt)
- DAO/repository: [`OddsDao.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/local/dao/OddsDao.kt), [`OddsRepositoryImpl.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/repository/OddsRepositoryImpl.kt)
- Domain selection/model: [`PreMatchOddsSelector.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/selector/PreMatchOddsSelector.kt), [`Odds.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/model/Odds.kt)
- Internal project notes: [`DATA_API_INTERNAL.md`](../../DATA_API_INTERNAL.md) (market labels only; not provider odds/settlement contract).
- Frozen cohort IDs and EU prediction context: session artifact `truelab-candidate-a-historical-odds-2026-10-07-candidate-a-predictions.tsv`.
- Device DB snapshot: read-only Room v5 on 2026-10-07; integrity check `ok`. No DB, code, or schema was modified.
