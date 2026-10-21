# TrueLab Odds Market Audit

**Ngày audit:** 2026-10-07  
**Phạm vi:** chỉ đọc pipeline và dữ liệu Room trên thiết bị; không gọi Odds API, không sửa production, không đổi weights/policy và không tạo candidate/model.

## 1. Executive Summary

Snapshot Room v5 được đọc trực tiếp từ thiết bị Android đang kết nối (`dev.anhquocs.truelab`), kèm WAL/SHM; SQLite `integrity_check` trả `ok`. Database có 30.485 matches, trong đó 29.987 match đã ended và có score. Bảng `odds` có 120.787 records thuộc bốn `oddsType` thực tế: `eu`, `asia`, `bs`, `cr`. Không có loại khác.

Để so sánh với research gần đây, cohort được cố định là 791 match đầu trong frozen source `device_cohort_results.tsv` theo thứ tự kickoff. Cả 791 match đều được tìm thấy trong snapshot thiết bị, và kickoff, kết quả, status đều khớp. Cohort này vốn được chọn từ các trận có EU Odds pre-match; vì vậy EU coverage 100% trong cohort **không** đại diện coverage trên toàn database.

| Thị trường trong Room | Raw records / match có raw | Usable pre-match trong cohort (N=791) | Đánh giá |
|---|---:|---:|---|
| EU (`eu`) | 27.620 / 791 (100%) | 4.082 records / 791 match (100%) | Đang được dùng; lựa chọn cần phân biệt DAO với selector theo match |
| AH (`asia`) | 19.454 / 787 (99,5%) | 3.273 records / 644 match (81,4%) | **Promising but needs data cleanup** |
| O/U (`bs`) | 26.104 / 791 (100%) | 3.108 records / 553 match (69,9%) | **Promising but needs data cleanup** |
| `cr` (ý nghĩa chưa xác định) | 4.759 / 620 (78,4%) | 425 pre-time/non-live two-way line records trên 346 match; **0 scoreline representation xác minh được** | **Not worth pursuing yet** |
| Others | 0 | 0 | Không có |

AH/O-U có usable event-time pre-match data sau khi áp dụng cutoff và kiểm tra payload. Tuy vậy, giá odds của hai market này chưa có format được xác nhận, quarter-line settlement semantics không được lưu, và không có ingestion timestamp để chứng minh snapshot đã có sẵn tại thời điểm dự đoán lịch sử. Vì vậy chưa thể gọi dữ liệu là point-in-time sạch hoàn toàn.

Trên 125 match có cùng nhà cung cấp Pinnacle với EU+AH+O/U, line AH và O/U tương quan mạnh với xác suất EU; exploratory conditional mutual information nhỏ và permutation test không cho thấy incremental Draw information đáng tin cậy. Mẫu nhỏ (32 Draw), chỉ kiểm soát EU P(D) theo tercile, nên kết quả là **chưa có bằng chứng đủ mạnh**, không phải chứng minh market không có giá trị.

## 2. Current EU Odds Pipeline

Đường dữ liệu đọc được trong code:

```text
Odds API `matches/{matchId}/odds`
  → OddsRecord DTO
  → OddsEntity Room (`odds`)
  → OddsRepository / getMatchOdds
  → PreMatchOddsSelector (EU trên đường dự đoán)
  → OddsRecordItem.calculateImpliedProbability
  → OddsSignalTransformer
  → 6 Signal3Way
  → WeightedScorer
```

- DTO `OddsRecord` và `OddsEntity` mang các trường: `companyId`, `companyName`, `oddsType`, `handicap`, `over`, `under`, `homeWin`, `draw`, `awayWin`, `changeTime`, `marketPhase`. `CompanyInfo.shortName` có trong DTO nhưng mapper chỉ lưu `company.name`; short name không được persist.
- Repository lưu các record API trả về, không chỉ EU; các market được biểu diễn chung trong cùng các cột nullable.
- `RunDailyBacktestUseCase` fetch odds on-demand rồi gọi `selectPreMatchEuropeanOdds`; `PredictMatchOutcomeUseCase` truyền `context.latestOdds` vào `OddsSignalTransformer`.
- EU selector yêu cầu `oddsType=eu`, đủ ba giá H/D/A và mỗi giá `>1.0`, `changeTime < kickoff`, đồng thời loại các phase `rolling_ball`, `in_play`, `live`, `running`, `inplay`. Nó ưu tiên `immediate`/`instant`/phase rỗng, rồi `initial`, sau đó `changeTime` mới nhất.
- `OddsRecordItem.calculateImpliedProbability()` tính `1/odds` cho H/D/A rồi chia từng giá trị cho tổng reciprocal; đây là normalization loại bỏ overround. Transformer gán Odds weight khi probabilities hợp lệ; khi thiếu/không hợp lệ thì fallback xác suất trung tính nhưng weight bằng 0.
- `DefaultWeightedScorer` kết hợp xác suất 3 chiều bằng weighted average. Production hiện giữ Odds là một signal 3-way EU; AH/O-U/CR chưa được đưa vào scorer.

`OddsSignalTransformer` không tự kiểm tra `oddsType`; tính hợp lệ về market hiện dựa vào selector upstream. Truyền trực tiếp AH/O-U/CR vào transformer không tạo được 3-way implied probability hợp lệ vì các cột H/D/A tương ứng bị null.

## 3. Odds Market Inventory

### Toàn bộ device DB

| `oddsType` thực tế | Records | Match ID duy nhất | Companies | Market phase records |
|---|---:|---:|---:|---|
| `eu` | 42.184 | 1.089 | 17 | `rolling_ball` 35.367; `instant` 6.248; `initial` 569 |
| `asia` | 30.087 | 1.085 | 15 | `rolling_ball` 24.296; `instant` 5.400; `initial` 391 |
| `bs` | 41.318 | 1.089 | 16 | `rolling_ball` 35.953; `instant` 4.799; `initial` 566 |
| `cr` | 7.198 | 808 | 7 | `rolling_ball` 6.661; `instant` 293; `initial` 244 |

Không có `immediate`, `as`, phase null hoặc loại khác trong snapshot này. Selector hỗ trợ alias `as` cho AH và nhiều phase khác, nhưng chúng không xuất hiện trong database kiểm tra.

Nếu lấy mọi match trong DB làm mẫu số, raw match coverage chỉ khoảng 3,6% cho EU/AH/O-U và 2,7% cho `cr` (30.485 match tổng). Trên 29.987 ended+scored matches, điều kiện pre-match/payload của EU tìm được 794 match, AH 690 match, O/U 587 match. `cr` có 382 match với một payload pre-time/non-live dạng line/two-way, nhưng chưa có cơ sở gọi đó là Correct Score.

### Cohort historical frozen (N=791)

| Market | Raw records | Raw match coverage | Pre timestamp records (`changeTime<kickoff`) | Pre, non-live, payload hợp lệ | Match có usable snapshot | Coverage |
|---|---:|---:|---:|---:|---:|---:|
| EU | 27.620 | 791/791 | 4.088 | 4.082 | 791 | 100% |
| AH (`asia`) | 19.454 | 787/791 | 3.276 | 3.273 | 644 | 81,4% |
| O/U (`bs`) | 26.104 | 791/791 | 3.110 | 3.108 | 553 | 69,9% |
| `cr` | 4.759 | 620/791 | 425 | 425 pre-time/non-live two-way line rows | 346 | 43,7% candidate line coverage; 0 verified scoreline |

“Payload hợp lệ” trong bảng là kiểm tra cấu trúc tối thiểu, không khẳng định format giá/settlement đã chuẩn:

- EU: đủ H/D/A và cả ba giá >1.
- AH: `handicap` có giá trị, H/A price có giá trị >0.
- O/U: `handicap` có giá trị, over/under có giá trị >0.
- `cr`: thống kê pre-time/non-live; loại này bị loại khỏi usable scoreline vì payload không chứa scoreline.

## 4. Coverage, Missingness và Snapshot Counts

Số snapshot raw trên mỗi match trong frozen cohort; các thống kê per-match tính trên toàn bộ 791 match, kể cả market-missing:

| Market | Raw snapshots/match: mean / median / min-covered / max | Usable pre snapshots/match: mean / median / min-covered / max |
|---|---|---|
| EU | 34,92 / 15 / 1 / 3.807 | 5,16 / 2 / 1 / 630 |
| AH | 24,59 / 12 / 1 / 2.543 | 4,14 / 1 / 1 / 574 |
| O/U | 33,00 / 15 / 1 / 3.413 | 3,93 / 1 / 1 / 573 |
| `cr` | 6,02 / 3 / 1 / 926 | Không coi là usable Correct Score |

Phân phối lệch mạnh do live time-series; median hữu ích hơn mean khi mô tả “độ sâu snapshot”. Record count cao không đồng nghĩa nhiều match có pre-match data.

Coverage usable theo company trong cohort, đếm match có ít nhất một snapshot đã chọn cho company đó (các company **overlap**, không cộng thành coverage toàn market):

- EU: SNAI 429; Macauslot 205; HKJC 168; 10BET 142; Pinnacle 125.
- AH: Ladbrokes 306; Macauslot 182; Pinnacle 131; 18Bet 107; Wewbet 91.
- O/U: Macauslot 194; HKJC 165; Pinnacle 131; 18Bet 104; 10BET 100.

Độ phủ theo competition cũng không đồng đều. Một số ví dụ trong cohort:

| Competition | Matches | EU | AH pre | O/U pre | `cr` raw |
|---|---:|---:|---:|---:|---:|
| UEFA Nations League | 60 | 60 | 57 | 25 | 60 |
| FA Cup | 53 | 53 | 39 | 42 | 7 |
| CONCACAF Nations League | 35 | 35 | 12 | 13 | 35 |
| United States Major League Soccer | 32 | 32 | 31 | 19 | 32 |
| Spanish Segunda Division | 22 | 22 | 20 | 12 | 22 |

Frozen cohort bao phủ rải rác từ 2026-09-12 đến 2026-10-06 UTC. Ví dụ, 2026-10-03 có 134 trận (AH 95, O/U 96), trong khi 2026-10-05 có 15 trận (AH 9, O/U 4). Không nên ngoại suy một tỷ lệ coverage chung sang competition/ngày khác.

## 5. Pre-match Availability và Lead Time

Trên cohort, timestamp kickoff parse được cho toàn bộ 791 match. `changeTime` là epoch seconds; kickoff được parse từ `matches.startTimeDate`. Không có record nào có `changeTime == kickoff`.

Lead time tính cho snapshot pre-match được chọn một lần per match/market theo phase priority rồi time:

| Market | Selected match | Min | Median | Mean | P90 | P95 | Max | Trong 60s trước kickoff | Trong 5 phút |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| EU | 791 | 1s | 56m 7s | 1,08 ngày | 2,79 ngày | 6,09 ngày | 130,1 ngày | 26 (3,3%) | 116 (14,7%) |
| AH | 644 | 1s | 5h 2s | 14,65h | 23h 49m | 34h 24m | 130,1 ngày | 24 (3,7%) | 104 (16,1%) |
| O/U | 553 | 5s | 1h 49m 30s | 1,03 ngày | 2,33 ngày | 4,24 ngày | 130,1 ngày | 20 (3,6%) | 98 (17,7%) |

Các snapshot chỉ cách kickoff vài giây vẫn thỏa invariant timestamp nhưng nhạy với sai số đồng hồ, độ chính xác kickoff và thời điểm người dùng thực sự ra quyết định. Không thể xác minh các yếu tố này từ `OddsEntity`.

## 6. Snapshot Selection và Khác biệt các đường truy vấn

### On-demand / historical context

`OddsDao.getOddsForMatch()` trả toàn bộ odds của match, sắp xếp `changeTime DESC`; đường dự đoán và historical cohort sau đó gọi `PreMatchOddsSelector`. Selector chọn **một** record cho mỗi match/market sau khi lọc time/phase/payload; không tổng hợp consensus giữa các bookmakers. Thứ tự ưu tiên là `immediate`/`instant`/rỗng > `initial` > phase khác được chấp nhận, sau đó `changeTime` mới nhất.

### Bulk EU query

`getLatestPreMatchEuropeanOddsForAllMatches()`:

- chỉ lọc `oddsType='eu'`;
- chỉ chấp nhận `marketPhase IN ('instant','initial')`;
- lọc `changeTime < kickoff`;
- yêu cầu đủ ba odds khác null và >0;
- sort match, instant trước initial, time giảm dần, `id DESC`.

`getLatestEuropeanOddsMap()` gọi `.distinctBy { matchId }`, nên kết quả là một bookmaker snapshot trên mỗi match, không phải một snapshot per company.

### Khác biệt và tie-breaking

- Selector on-demand chấp nhận mọi phase không nằm trong live set (kể cả phase rỗng/không biết), còn bulk DAO chỉ nhận `instant` và `initial`. Bulk có thể bỏ một record mà selector on-demand nhận.
- EU selector đòi giá >1.0; bulk DAO chỉ đòi >0. Với các giá 0<odds<=1, hai đường có thể chọn khác nhau.
- Bulk có `id DESC` để tie-break; selector market-level dùng phase/time nhưng không tie-break bằng company/id. `getOddsForMatch()` cũng chỉ order theo `changeTime`; nếu các bookmaker có cùng phase/time thì record cuối của list có thể phụ thuộc thứ tự DB.
- Historical export trong research trước đây chọn latest theo `(matchId,companyId)` trước khi đưa qua selector, còn bulk DAO xếp toàn bộ rows rồi lấy một match-level row. Với snapshot hiện tại, cách khác nhau có thể làm đổi bookmaker ở tie hoặc khi phase ngoài tập bulk.
- `getLatestOddsForMatch()` chỉ lấy max `changeTime` per company/type, không lọc kickoff/live. Không dùng truy vấn này làm snapshot pre-match nếu không áp selector tiếp.
- Cache guard của `fetchAndCacheOddsForMatch()` xét EU/phase/giá nhưng không xét `changeTime < kickoff`; phase blocklist ở đó cũng không gồm `inplay` dù selector có. Điều này có thể ảnh hưởng quyết định fetch lại/coverage, nhưng selector vẫn phải được áp trước khi dùng prediction.

Database có unique index `(matchId, companyId, oddsType, changeTime)` và insert dùng `REPLACE`. Index ngăn duplicate key trong DB, nhưng không chứa line trong key. Nếu upstream gửi nhiều line AH/O-U cho cùng match/company/type/timestamp, schema có thể ghi đè một dòng; chưa có raw API capture để xác nhận upstream có tạo tình huống này trong dataset hay không.

## 7. Leakage Audit

### Event-time leakage

Trong frozen cohort:

- Bản ghi được chọn cho EU/AH/O-U đều thỏa `changeTime < kickoff`.
- `changeTime == kickoff`: 0 record.
- Pre-time nhưng mang phase live `rolling_ball`: EU 1, AH 2, O/U 2; selector loại các record này.
- Có post-kickoff records mang phase trông như pre-match (`initial`/`instant`): EU 63, AH 51, O/U 66, `cr` 4. Ví dụ AH match `129258` có `instant` 41 giây sau kickoff. Strict timestamp cutoff loại chúng.
- Sau kickoff có lượng lớn `rolling_ball` (EU 23.469, AH 16.127, O/U 22.928, `cr` 4.330 records trên cohort); raw row count vì vậy không phải pre-match coverage.
- Không có timestamp equality; các record pre-time/live phase được loại bởi phase, còn post-time/instant bị loại bởi timestamp. Đây là evidence cụ thể rằng cả hai điều kiện đều cần thiết.

**Phân loại event-time:** không có bằng chứng snapshot post-kickoff đi vào tập được chọn nếu selector được gọi đúng; raw DB chứa post-kickoff/live records nhưng chúng không được dùng bởi selector.

### Availability-time / retrospective leakage

`OddsEntity` chỉ có `changeTime` của thị trường, không có `fetchedAt`, `receivedAt` hay ingestion timestamp. Odds được nạp qua on-demand API và có thể được persist sau khi trận đấu kết thúc. Do đó `changeTime < kickoff` chứng minh thời điểm biến động được khai báo trước trận, nhưng **không chứng minh** ứng dụng đã sở hữu record đó trước kickoff. Trong backtest hồi cứu, availability-time leakage vẫn là **potential leakage**, áp dụng cả EU lẫn market mới. Không có bằng chứng đủ để gọi là confirmed leakage, cũng không thể xác nhận point-in-time availability.

### Phase/timestamp mâu thuẫn

Có record `rolling_ball` nhưng timestamp trước kickoff (ví dụ `646976`/`664106` ở AH; cách kickoff lần lượt khoảng 10,6h và 26,7h theo schedule). Selector hiện loại theo phase nên không đưa chúng vào. Đây là quality/label discrepancy cần theo dõi, không phải bằng chứng chúng đã lọt vào prediction.

## 8. Market Normalization và Representation

### EU

Home/Draw/Away là ba cột odds riêng. Trong cohort, 4.082 record vượt điều kiện selector; có 5 record pre/non-live khác không vượt kiểm tra giá >1. Công thức hiện tại `q_i=1/o_i`, `p_i=q_i/Σq` tạo 3-way no-vig implied probabilities. Đây là market có representation và normalization rõ nhất trong production.

### AH (`asia`)

Có `handicap` dạng số và hai giá `homeWin`/`awayWin`; không có draw odds. Trong toàn DB, handicap nằm trong `[-6.25, 7.25]`; có whole, half và quarter lines (15.806 quarter-line records). Đây là đủ để giữ line và hai phía theo từng record, nhưng chưa đủ để quyết định:

- dấu handicap được quy ước theo perspective home hay away trên từng feed/bookmaker;
- cách chuyển odds về decimal/probability (giá AH thực tế thường không giống decimal EU);
- quarter-line split stake, push/refund/half-win semantics;
- snapshot có nhiều line cùng thời điểm và line nào là comparable consensus line.

`homeWin`/`awayWin` trong toàn bộ `asia` records thường <=1 (22.337/30.087 và 22.881/30.087 tương ứng), có giá trị 0; không thể áp trực tiếp công thức reciprocal decimal EU.

### O/U (`bs`)

`handicap` lưu total line; `over`/`under` lưu hai giá. Có quarter lines (14.727 records), half lines (20.340) và whole lines. Trong raw DB `over<=1` ở 26.210/41.318 rows, `under<=1` ở 35.997/41.318 rows; range có giá trị 0 và outlier tới 35. Đây không phải distribution giá decimal EU sạch để reciprocal-normalize ngay.

Line và hai phía đủ cho phân tích mô tả total-goal environment sau khi xác minh odds convention. Quarter totals cũng cần xử lý stake split/push semantics đúng; hiện schema chỉ giữ `Double handicap`, không mô tả settlement.

### `cr`

Entity không có field scoreline, outcome key hay vector giá cho `0-0`, `1-0`, `1-1`, v.v. Thực tế 7.198 rows của `cr` đều populate `handicap`, `over`, `under`; không row nào có H/D/A fields. `handicap` có range 1,5–22,5 và `over`/`under` là hai giá, phù hợp representation của một line/two-way market hơn một bảng Correct Score, nhưng tên chính xác không thể xác nhận từ dữ liệu.

Documentation nội bộ mâu thuẫn: `prediction-data-coverage-audit.md` gọi `cr` là Corner, `odds-data-coverage-audit.md` gọi là Correct Score. Không có upstream type dictionary/raw response contract trong project để giải quyết. Không được suy ra `0-0/1-1/2-2` concentration từ các cột hiện có.

## 9. Incremental Information vs EU (Exploratory, không phải model)

Để tránh trộn bookmaker, dùng cùng một snapshot pre-match per market từ Pinnacle (`companyId=22`) và chỉ xét **125 match** có đủ EU+AH+O/U; 32 match trong số đó là Draw. EU probabilities tính từ Pinnacle EU H/D/A bằng normalization no-vig như trên.

Relationship Pearson trên intersection này:

- Signed AH line với `P(EU Home) − P(EU Away)`: **r=+0,984** (không diễn giải chiều vì sign convention theo provider chưa được xác minh).
- `|AH line|` với EU implied Draw probability: **r=−0,868**.
- O/U total line với EU implied Draw probability: **r=−0,761**.

Đây là dấu hiệu AH/O-U line chứa/biểu diễn mạnh cùng trạng thái market consensus mà EU đã phản ánh; correlation không tự nó đo incremental prediction.

Exploratory conditional analysis, feature bins định trước để audit (không tune): AH balance `|line|<=0.25`, `0.25<|line|<=0.75`, `>0.75`; O/U line `<=2.25`, `2.5–2.75`, `>=3.0`. Actual Draw rates:

| Feature group | N | Draw | Draw rate | Mean EU P(D) |
|---|---:|---:|---:|---:|
| AH `|line|<=0.25` | 48 | 18 | 37,5% | 0,267 |
| AH `0.25<|line|<=0.75` | 40 | 9 | 22,5% | 0,241 |
| AH `|line|>0.75` | 37 | 5 | 13,5% | 0,176 |
| O/U `<=2.25` | 15 | 7 | 46,7% | 0,294 |
| O/U `2.5–2.75` | 47 | 12 | 25,5% | 0,245 |
| O/U `>=3.0` | 63 | 13 | 20,6% | 0,205 |

Low-goal / balanced-market buckets có Draw rate cao hơn, nhưng EU P(D) trung bình cũng cao hơn rõ rệt trong chính các buckets đó. Conditional mutual information giữa binary Draw outcome và bucket, sau khi phân tầng theo tercile của EU P(D), lần lượt là **0,0335 bits** cho AH và **0,0117 bits** cho O/U; permutation test trong-stratum 1.999 lượt cho `p=0,388` và `p=0,863`. Không thấy bằng chứng incremental đáng tin cậy trong mẫu này.

Đây chỉ là một exploratory check: N=125 và 32 Draw; tercile chỉ điều kiện hóa EU P(D), không điều kiện hóa toàn bộ vector EU H/D/A hoặc consensus/bookmaker effects; bins và selection of Pinnacle limit generalization. Kết quả không bác bỏ khả năng có incremental information ở market/time/company khác.

## 10. Draw-specific Analysis

- **AH:** handicap gần cân bằng đi kèm Draw rate cao hơn trong mẫu Pinnacle, nhưng đồng thời cũng đi kèm EU P(D) cao hơn. Chưa thấy bằng chứng rằng AH bổ sung đủ thông tin Draw ngoài EU.
- **O/U:** line thấp có Draw rate mô tả cao hơn; nhóm `<=2.25` chỉ có 15 trận, nên uncertainty lớn. Chênh lệch phần lớn đi cùng EU P(D); conditional test không significant.
- **`cr`:** không thể kiểm tra Draw concentration vào scoreline `0-0`, `1-1`, `2-2` vì representation hiện không có scoreline. Chưa có cơ sở đánh giá thông tin Draw.
- Đây là mô tả/association, không phải causal evidence hay chứng minh cải thiện prediction.

## 11. Market-by-market Assessment

| Criterion | EU | AH (`asia`) | O/U (`bs`) | `cr` |
|---|---|---|---|---|
| Cohort usable pre-match coverage | 791/791 (100%) | 644/791 (81,4%) | 553/791 (69,9%) | 346/791 có pre-time line/two-way; 0 scoreline xác minh |
| Pre-match availability | Tốt theo event timestamp | Khá, company-dependent | Trung bình-khá, company-dependent | Có một số pre rows nhưng semantics không rõ |
| Snapshot quality | H/D/A rõ, margin normalization đã có | Line+2 prices; format và sign cần xác minh | Total line+2 prices; price outlier/format cần xác minh | Schema/name conflict; không có scoreline |
| Leakage risk | Không thấy event-time leak khi selector chạy; historical availability vẫn potential | Như EU; raw có live/post và phải lọc | Như EU; raw có live/post và phải lọc | Chưa có selector semantics; potential, không dùng |
| Normalization feasibility | Sẵn có | Cần xác nhận odds convention và settlement | Cần xác nhận odds convention, line và settlement | Chưa khả thi như Correct Score |
| Draw-specific information | Baseline market signal | Cân bằng AH mô tả liên quan Draw; incremental chưa rõ | Low total line mô tả liên quan Draw; incremental chưa rõ | Không đo được |
| Incremental information vs EU | Baseline | Tương quan cao; chưa có bằng chứng phần dư đáng tin cậy | Tương quan cao; chưa có bằng chứng phần dư đáng tin cậy | Không đánh giá được |
| Research readiness | **Ready for research** (đường baseline hiện có) | **Promising but needs data cleanup** | **Promising but needs data cleanup** | **Not worth pursuing yet** |

## 12. Research Recommendations

Không đề xuất Candidate C hoặc decision rule. Các **Research hypothesis candidate** chỉ ở mức câu hỏi:

1. AH line có còn liên hệ với actual outcome/Draw sau khi kiểm soát đầy đủ EU H/D/A trên sample nhiều competition và temporal holdout độc lập không?
2. O/U total line có residual association với Draw sau khi điều kiện hóa EU 1X2; quan sát low total line hiện tại có bền khi tăng N không?
3. Liệu cùng-bookmaker snapshots, chuẩn hóa odds convention đúng và line selection nhất quán có làm kết luận khác với snapshot đang chọn cross-company không?
4. Có thể tái dựng point-in-time availability bằng API capture/log/fetch timestamp thay vì chỉ dùng `changeTime` không?
5. Upstream định nghĩa `cr` là Corner, Correct Score hay market khác; payload/line semantics nào thực sự được trả?

## 13. Limitations

- 791-match cohort được chọn vì có historical EU data; mọi so sánh AH/O/U là complete-case subset và có selection bias.
- Device DB có odds cho khoảng 1.089 match trong 30.485; tỷ lệ cao trong cohort không đồng nghĩa database-wide coverage cao.
- `changeTime` biểu diễn market event time, không có record-arrival/fetch time; historical point-in-time availability chưa chứng minh được.
- Odds encoding theo bookmaker/market chưa có field metadata; không áp dụng phép no-vig EU cho AH/O/U.
- `cr` naming conflicting; không suy luận semantic Correct Score từ label.
- Conditional analysis N=125, 32 Draw, chỉ là descriptive/exploratory; không train, tune hoặc đánh giá candidate/model.
- Snapshot lần này là bản đọc của DB thiết bị hiện tại; không phải bản archive API bất biến tại thời điểm từng match diễn ra.

## 14. Sources và audit artifacts

- Room model / mapping: `../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/local/entity/OddsEntity.kt`, `../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/remote/dto/OddsDto.kt`, `../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt`.
- DAO/repository/API: `../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/local/dao/OddsDao.kt`, `../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/repository/OddsRepositoryImpl.kt`, `../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/odds/remote/api/OddsApi.kt`.
- Selection and scoring: `../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/selector/PreMatchOddsSelector.kt`, `../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/odds/model/Odds.kt`, `../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/OddsSignalTransformer.kt`, `../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt`, `../../core/algorithm/src/main/kotlin/dev/anhquocs/truelab/core/algorithm/prediction/DefaultWeightedScorer.kt`.
- Local frozen prediction source: `device_cohort_results.tsv` from the session scratch directory. The audit pulled the current Room DB read-only from the attached device; no database or production source was modified.
- Existing documentation disagreement about `cr`: `prediction-data-coverage-audit.md` labels it Corner; `odds-data-coverage-audit.md` labels it Correct Score.
- No exploratory script or generated dataset was added to the repository.
