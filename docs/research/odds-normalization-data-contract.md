# Chuẩn Hóa Dữ Liệu Odds Tiền Trận: Data Contract & Audit (AH & O/U)

> **Mục đích:** Thiết lập chuẩn hóa dữ liệu (Normalization Contract), quy tắc điểm thời gian (Point-in-Time Rule), và kiểm toán chất lượng dữ liệu cược Chấp Châu Á (Asian Handicap - AH) và Tài/Xỉu (Over/Under - O/U) trên tập dữ liệu lịch sử đã đóng băng (**Frozen 791 Cohort**) nhằm chuẩn bị cơ sở dữ liệu sạch cho nghiên cứu **Candidate C**.
> 
> **Phạm vi:** Chỉ chuẩn bị dữ liệu (Data Preparation & Audit). Không triển khai thuật toán Candidate C, không thay đổi trọng số, không can thiệp live/MQTT, không commit/push.

---

## 1. Normalization Contract (Hợp Đồng Chuẩn Hóa Dữ Liệu)

Dữ liệu Odds từ TrueScore Backend được trích xuất qua hai endpoint REST:
- `GET /sport/v1.0/matches/{matchId}/odds`
- `GET /sport/v1.0/matches/{matchId}/odds-history`

Để đảm bảo khả năng truy vết nguồn (data provenance) và không làm mất mát thông tin, cấu trúc chuẩn hóa cho từng loại thị trường cược được định nghĩa như sau:

### 1.1. Asian Handicap (AH — `oddsType = 'asia'`)
Đại diện cho kèo cược chấp cả trận (Full Match Asian Handicap):

```text
Canonical Asian Handicap Record:
├── matchId       : Long    (Khóa định danh trận đấu)
├── companyId     : Int     (Mã định danh nhà cung cấp / Bookmaker ID)
├── companyName   : String  (Tên thương hiệu nhà cung cấp, ví dụ: Pinnacle, Macauslot, 18Bet)
├── oddsType      : String  (Cố định 'asia')
├── marketPhase   : String  (Giai đoạn thị trường: 'initial', 'instant', 'immediate', '')
├── handicap      : Double  (Tỷ lệ chấp bóng theo góc nhìn Home: >0: Home chấp, <0: Away chấp, 0.0: đồng banh)
├── homeWin       : Double  (Giá tiền / Payout cho cửa Home)
├── awayWin       : Double  (Giá tiền / Payout cho cửa Away)
└── changeTime    : Long    (Thời điểm biến động tỷ lệ, đơn vị Unix Epoch Seconds)
```

### 1.2. Over / Under (O/U — `oddsType = 'bs'`)
Đại diện cho kèo Tài/Xỉu tổng số bàn thắng cả trận (Full Match Total Goals):

```text
Canonical Over/Under Record:
├── matchId       : Long    (Khóa định danh trận đấu)
├── companyId     : Int     (Mã định danh nhà cung cấp / Bookmaker ID)
├── companyName   : String  (Cố định 'bs' - Ball Score / Over Under)
├── oddsType      : String  (Cố định 'bs')
├── marketPhase   : String  (Giai đoạn thị trường: 'initial', 'instant', 'immediate', '')
├── handicap      : Double  (Cột mốc tổng bàn thắng / Goal Line, ví dụ: 2.25, 2.50, 2.75)
├── over          : Double  (Giá tiền / Payout cho cửa Tài - Over)
├── under         : Double  (Giá tiền / Payout cho cửa Xỉu - Under)
└── changeTime    : Long    (Thời điểm biến động tỷ lệ, đơn vị Unix Epoch Seconds)
```

---

## 2. Pre-match Point-in-Time Rule (Quy Tắc Điểm Thời Gian Tiền Trận)

Để loại trừ 100% rủi ro rò rỉ dữ liệu tương lai (Zero Data Leakage), một bản ghi Odds chỉ được coi là **Hợp lệ Tiền Trận (Pre-match Usable)** khi thỏa mãn đồng thời hai điều kiện cứng:

1. **Ràng buộc thời gian tuyệt đối:**
   $$\text{changeTime} < \text{kickoffEpoch}$$
   *(Thời điểm ghi nhận biến động phải diễn ra nghiêm ngặt trước giờ bóng lăn).*
2. **Ràng buộc giai đoạn thị trường (Market Phase):**
   $$\text{marketPhase} \in \{\text{'initial'}, \text{'instant'}, \text{'immediate'}, \text{''}\} \quad \text{VÀ} \quad \text{marketPhase} \notin \{\text{'rolling\_ball'}, \text{'in\_play'}\}$$
   *(Loại bỏ hoàn toàn các biến động diễn ra trong trận / cược rung).*

---

## 3. Opening vs Latest Pre-match Policy (Chính Sách Odds Mở Kèo & Cận Trận)

Thị trường tỷ lệ cược luôn biến động theo thời gian từ lúc mở kèo đến khi bóng lăn:

1. **Opening Odds (Tỷ lệ mở kèo ban đầu):**
   - Được định danh bởi `marketPhase = 'initial'` hoặc bản ghi hợp lệ có $\min(\text{changeTime})$ xa giờ bóng lăn nhất.
   - Đại diện cho nhận định ban đầu của nhà cung cấp khi chưa có dòng tiền thị trường (Market Consensus).
2. **Latest Pre-match Odds (Tỷ lệ cận giờ bóng lăn / Closing pre-match):**
   - Được định nghĩa là bản ghi có $\max(\text{changeTime})$ thỏa mãn $\text{changeTime} < \text{kickoffEpoch}$ theo từng bộ $(\text{matchId}, \text{companyId}, \text{handicap})$.
   - Đại diện cho thông tin thị trường hoàn chỉnh và chính xác nhất trước khi trận đấu diễn ra.
3. **Quy tắc trích xuất cho Candidate C:**
   - **Ưu tiên 1 (Primary Baseline):** Sử dụng **Latest Pre-match Odds** để mô hình hóa xác suất tại thời điểm ra quyết định dự đoán.
   - **Ưu tiên 2 (Market Movement Signal):** Sự chênh lệch giữa $\text{Opening Odds}$ và $\text{Latest Odds}$ ($\Delta \text{Handicap}$, $\Delta \text{Price}$) đại diện cho dòng tiền thông minh (Smart Money / Market Drift), có thể dùng làm feature phụ trợ.

---

## 4. Multi-Line Identity (Định Danh Kèo Đa Line)

Trong cược Châu Á, một nhà cung cấp thường mở **nhiều line tỷ lệ cùng lúc** cho một trận đấu (ví dụ: Kèo chính chấp $-0.5$, Kèo phụ chấp $-0.25$ và $-0.75$; Kèo Tài Xỉu chính $2.5$, kèo phụ $2.25$ và $2.75$).

### Quy tắc bất biến:
- **Tuyệt đối không gộp (collapse) các line khác nhau** thành một bản ghi duy nhất.
- Khóa định danh duy nhất (Composite Primary Key) cho từng snapshot được xác định bởi bộ 6 thành phần:
  $$\text{Identity} = \langle \text{matchId}, \text{companyId}, \text{oddsType}, \text{marketPhase}, \text{handicap}, \text{changeTime} \rangle$$
- **Canonical Line Selection Policy:**
  - Khi cần chọn ra 1 Line duy nhất đại diện cho trận đấu: Chọn **Main Line (Kèo chính)** — Line có giá tiền $(\text{price}_1, \text{price}_2)$ cân bằng nhất (tức $|\text{price}_1 - \text{price}_2|$ nhỏ nhất, thường quanh mức $0.85 - 0.95$).

---

## 5. Odds Format Policy (Chính Sách Định Dạng Giá Cược)

### 5.1. Kết quả kiểm tra thực tế trên TrueLab Database:
Kiểm tra phân bố giá trị trên $6.382$ snapshot tiền trận thực tế:
- **Asian Handicap:** $\text{Min} = 0.00$, $\text{Max} = 1.55$, $\text{Mean} = 0.893$, $\text{Median} = 0.880$.
- **Over / Under:** $\text{Min} = 0.28$, $\text{Max} = 1.62$, $\text{Mean} = 0.881$, $\text{Median} = 0.890$.
- $100\%$ các giá trị hợp lệ đều nằm trong khoảng $(0.0, 2.0)$, tập trung quanh $0.80 - 1.00$.

### 5.2. Kết luận định dạng:
> **Toàn bộ Odds AH và O/U trong TrueLab Database được lưu trữ theo Định Dạng Hong Kong (HK Odds / Asian Payout Format)** — đại diện cho mức lợi nhuận ròng (Net Profit) trên 1 đơn vị tiền cược (chưa bao gồm tiền gốc hoàn trả).

### 5.3. Quy tắc chuyển đổi bắt buộc (CẤM dùng $1 / \text{Odds}$ trực tiếp):
Nếu áp dụng công thức $1 / \text{Odds}$ lên giá HK $0.90$, sẽ thu được $1 / 0.90 = 1.111$ ($111,1\%$ xác suất — SAI LỆCH HOÀN TOÀN).

**Công thức chuẩn hóa chuẩn xác:**
1. **Chuyển sang Decimal Odds (Tỷ lệ Châu Âu):**
   $$\text{Odds}_{\text{Decimal}} = \text{Odds}_{\text{HK}} + 1.0$$
   *(Ví dụ: $\text{HK } 0.88 \implies \text{Decimal } 1.88$)*
2. **Tính Xác suất Hàm ý Thô (Raw Implied Probability):**
   $$P_{\text{raw}}(\text{Home}) = \frac{1}{\text{homeWin}_{\text{HK}} + 1.0}, \quad P_{\text{raw}}(\text{Away}) = \frac{1}{\text{awayWin}_{\text{HK}} + 1.0}$$
3. **Loại bỏ Margin Nhà cung cấp (Overround Normalization):**
   $$\text{Margin} = P_{\text{raw}}(\text{Home}) + P_{\text{raw}}(\text{Away}) - 1.0$$
   $$P_{\text{fair}}(\text{Home}) = \frac{P_{\text{raw}}(\text{Home})}{P_{\text{raw}}(\text{Home}) + P_{\text{raw}}(\text{Away})}$$

---

## 6. Data Quality Rules (Bộ Quy Tắc Kiểm Tra Chất Lượng Dữ Liệu)

Một bản ghi AH / O/U bị coi là **Không hợp lệ (Invalid / Corrupted)** và bị loại bỏ nếu vi phạm một trong các điều sau:

1. `handicap IS NULL` hoặc `changeTime IS NULL` hoặc `kickoffEpoch IS NULL`.
2. Giá tiền $\le 0.0$ (ví dụ các bản ghi placeholder $0.0 / 0.0$ của nhà cung cấp khi đóng/treo kèo).
3. `changeTime >= kickoffEpoch` (rò rỉ dữ liệu trong trận).
4. `marketPhase` thuộc danh sách live (`rolling_ball`, `in_play`).
5. Cột mốc bàn thắng Tài Xỉu $\text{handicap} \le 0.0$ (bất khả thi về mặt vật lý).

---

## 7. Frozen 791 Cohort Audit (Kiểm Toán Trên Tập Dữ Liệu 791 Trận)

Kiểm toán toàn diện trên **791 trận lịch sử cố định** từ [candidate-a-historical-odds-audit.md](../audits/candidate-a-historical-odds-audit.md):

### 7.1. Tổng quan số lượng & Độ phủ (Coverage)

| Chỉ số kiểm toán | Asian Handicap (`asia`) | Over / Under (`bs`) | Giao nhau (Cả AH & O/U) |
|---|:---:|:---:|:---:|
| **Tổng số snapshot thô (gồm cả Live)** | 22.861 bản ghi | 22.697 bản ghi | 45.558 bản ghi |
| **Snapshot Tiền trận Hợp lệ** | **3.274 bản ghi** | **3.108 bản ghi** | **6.382 bản ghi** |
| **Bản ghi Live bị loại bỏ** | 19.587 bản ghi | 19.589 bản ghi | 39.176 bản ghi |
| **Độ phủ trận đấu (Match Coverage)** | **645 / 791 trận (81,54%)** | **553 / 791 trận (69,91%)** | **497 / 791 trận (62,83%)** |
| **Số nhà cung cấp đại diện (Bookmakers)** | 15 nhà cung cấp | 16 nhà cung cấp | 16 nhà cung cấp |

### 7.2. Độ phủ theo Nhà cung cấp (Top Bookmakers)

- **Asian Handicap:** Pinnacle (131 trận), 18Bet (107 trận), Wewbet (91 trận), Ladbrokes (306 trận snapshot), Macauslot (182 trận), 12bet (57 trận), Vcbet (33 trận), BET365 (33 trận)...
- **Over / Under:** Macauslot (194 trận), HKJC (165 trận), Pinnacle (131 trận), 18Bet (104 trận), 10BET (100 trận), Wewbet (88 trận), 12bet (57 trận), Vcbet (42 trận)...

### 7.3. Phân bố Kèo & Đa Line (Multi-Line & Handicap Distribution)

* **Asian Handicap (30 distinct lines):**
  - Số line trung bình mỗi trận: **1,26 lines** (Min: 1, Max: 6).
  - Số snapshot trung bình mỗi trận: **5,08 snapshots** (Min: 1, Max: 574).
  - *Top 5 Line phổ biến nhất:* Chấp $0.5$ ($24,3\%$), Chấp $0.25$ ($13,3\%$), Chấp $-1.0$ ($10,7\%$), Chấp $1.25$ ($9,3\%$), Chấp $1.0$ ($8,7\%$).
* **Over / Under (18 distinct lines):**
  - Số line trung bình mỗi trận: **1,33 lines** (Min: 1, Max: 6).
  - Số snapshot trung bình mỗi trận: **5,62 snapshots** (Min: 1, Max: 573).
  - *Top 5 Line phổ biến nhất:* Tài Xỉu $2.5$ ($30,0\%$), $2.75$ ($20,6\%$), $2.25$ ($13,4\%$), $3.5$ ($12,0\%$), $3.0$ ($9,6\%$).

### 7.4. Khoảng cách thời gian đến giờ bóng lăn (Lead Time to Kickoff)
- **Asian Handicap:** Trung vị $= 5,40$ giờ trước trận ($\text{P25} = 0,44$h, $\text{P75} = 14,87$h, $\text{Mean} = 19,07$h).
- **Over / Under:** Trung vị $= 8,06$ giờ trước trận ($\text{P25} = 0,48$h, $\text{P75} = 22,93$h, $\text{Mean} = 25,76$h).
- **Opening Odds (`marketPhase = 'initial'`):** Có sẵn ở **212 trận AH (26,8%)** và **261 trận O/U (33,0%)**.

### 7.5. Tính toàn vẹn dữ liệu (Data Integrity)
- Số bản ghi có `handicap` bị Null: **0** ($0,0\%$).
- Số bản ghi có giá tiền bị Null: **0** ($0,0\%$).
- Số bản ghi bị trùng lặp khóa 6 thành phần: **0** ($0,0\%$).
- Số bản ghi tiền trận có giá $\le 0.0$: **1 bản ghi** (chiếm $0,015\%$, đã được định vị và lọc bỏ bởi Quality Rule).

---

## 8. Known Limitations (Các Hạn Chế Đã Ghi Nhận)

1. **Độ lệch mẫu do On-Demand Ingestion:** Dữ liệu Odds trong Room phản ánh hành vi người dùng mở xem trận đấu, không phải toàn bộ trận trong lịch sử đều có Odds AH/OU (Độ phủ đạt $81,5\%$ cho AH và $69,9\%$ cho O/U).
2. **Độ phủ Opening Odds thấp hơn Latest Odds:** Opening Odds (`initial`) chỉ đạt khoảng $\approx 30\%$ độ phủ. Nghiên cứu Candidate C nên dựa vào **Latest Pre-match Odds** làm tín hiệu chính để đảm bảo cỡ mẫu lớn.
3. **Định dạng Hong Kong cần chuẩn hóa:** Phải đảm bảo toàn bộ các hàm transformer chuyển đổi qua Decimal $+ 1.0$ trước khi đưa vào tính xác suất Poisson hoặc Goal Expectancy.

---

## 9. Readiness for Candidate C (Đánh Giá Mức Độ Sẵn Sàng)

Dựa trên kết quả kiểm toán thực nghiệm:
- Hợp đồng dữ liệu (Data Contract) đã được chuẩn hóa rõ ràng cho cả AH và O/U.
- Point-in-Time Rule đã tách bạch sạch sẽ $6.382$ bản ghi tiền trận hợp lệ và loại bỏ $39.176$ bản ghi live/rung.
- Định dạng giá cược Hong Kong đã được xác định và có công thức chuẩn hóa bảo vệ.
- Dữ liệu đạt $100\%$ tính toàn vẹn (không null, không duplicate, đa line được bảo toàn).

### Kết luận chính thức:

$$\mathbf{READY\ FOR\ CANDIDATE\ C\ RESEARCH}$$
