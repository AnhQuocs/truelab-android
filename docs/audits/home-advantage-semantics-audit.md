# Home Advantage Semantics & Implementation Audit

> **Trạng thái:** AUDIT ONLY (Read-Only Investigation)  
> **Phạm vi:** Core Prediction Pipeline, Room Database Schema, DTO/Entity/Domain Mappers, Neutral Venue Handling  
> **Ngày thực hiện:** 02/10/2026  
> **Tài liệu đối chiếu:** [Draw Underprediction Audit](draw-underprediction-audit.md), [Domain Signal Modeling Spec](../reports/sub/domain-signal-modeling.md), [TrueScore Audit Context](#1-context)

---

## 1. Context

### 1.1. Tóm tắt kết quả Audit TrueScore (Data Source)
Trước khi tiến hành audit trên codebase TrueLab, một cuộc audit READ-ONLY trên repository **TrueScore** đã được thực hiện để xác định ngữ nghĩa (semantics) của `home_team`, `away_team` và `venue`.

Các phát hiện chính từ TrueScore:
1. **Match Detail API**: Chứa các trường `homeTeam`, `awayTeam`, `venue: VenueResponse?`, `twoLeg`, `stage`, `attributes`.
2. **Venue Model (`VenueResponse`)**: Gồm `id`, `name`, `city`, `capacity`, `country`.
3. **Cờ trung lập (Neutral Flag)**: **HOÀN TOÀN KHÔNG TỒN TẠI** bất kỳ field/flag nào như `neutral`, `is_neutral`, `isNeutral`, hay `neutral_venue` trong schema của TrueScore.
4. **Odds Representation**: Dữ liệu Odds 1X2 sử dụng `home_win`, `draw`, `away_win` và được gán cứng theo `home_team` / `away_team` của lịch thi đấu (fixture).
5. **Giới hạn dữ liệu (Confirmed Limitation)**:
   - Trong các giải vô địch quốc gia (Domestic Leagues - e.g. Premier League, La Liga), `home_team` thường trùng khớp với đội chủ nhà thực tế (Physical Home Team).
   - Trong các giải đấu quốc tế (World Cup, Euro, Copa America, Asian Cup), các trận chung kết (Champions League Final), hoặc giải đấu tập trung tại một quốc gia đăng cai, `home_team` chỉ mang ý nghĩa **Side 1 (phía danh nghĩa) của fixture** phục vụ bốc thăm, trang phục và quyền xếp lịch, **không đồng nghĩa với Physical Home Advantage**.
   - Data source TrueScore không cung cấp đủ cờ hiệu để downstream consumer chỉ cần dựa vào `homeId != awayId` là kết luận được đội Home có lợi thế sân nhà thực tế.

---

## 2. Data Flow Trace

Đường đi của dữ liệu từ TrueScore API qua các tầng của TrueLab vào `HomeAdvantageSignalTransformer`:

```text
┌────────────────────────────────────────────────────────┐
│ TrueScore Match Detail / Match List API                │
│ (home_team, away_team, start_time_date, status)        │
│ [KHÔNG CÓ neutral flag, venue bị bỏ qua khi sync]      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ MatchRecord (MatchDto.kt)                              │
│ • homeTeam: TeamInfo (id, name, logo)                  │
│ • awayTeam: TeamInfo (id, name, logo)                  │
│ • competition_id, start_time_date, status, minutes     │
│ [KHÔNG CÓ venue, stadium, neutral_venue]               │
└───────────────────────────┬────────────────────────────┘
                            │ RoomMappers.toMatchEntity()
                            ▼
┌────────────────────────────────────────────────────────┐
│ MatchEntity (MatchEntity.kt)                           │
│ • homeTeamId: Int                                      │
│ • awayTeamId: Int                                      │
│ • leagueId, startTimeDate, status, season              │
│ [KHÔNG CÓ venueId, venueCountry, isNeutral]            │
└───────────────────────────┬────────────────────────────┘
                            │ RoomMappers.toDomain()
                            ▼
┌────────────────────────────────────────────────────────┐
│ Match (Match.kt - Domain Model)                        │
│ • homeTeam: TeamSummary (id, name, logo)               │
│ • awayTeam: TeamSummary (id, name, logo)               │
│ • leagueId, startTimeDate, status, season              │
│ [KHÔNG CÓ isNeutral, venue, venueCountry]              │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ MatchPredictionContext (MatchPredictionContext.kt)     │
│ • matchId, homeTeamId, awayTeamId                      │
│ • homeElo, awayElo, homeRecentMatches, ...             │
│ • isNeutralVenue: Boolean = false (Default Param)      │
│ [Caller sites KHÔNG truyền -> 100% RUNTIME LÀ FALSE]   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ HomeAdvantageSignalTransformer.transform(              │
│     isNeutralVenue = context.isNeutralVenue,           │
│     config = config                                    │
│ )                                                      │
│ [Tính ra Signal3Way cố định [0.46, 0.26, 0.28]]        │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ WeightedScorer.predictOutcome(signals)                 │
│ • Đóng góp trọng số w = 0.10 (10%)                     │
│ • Home win prob nhận thêm +0.046                       │
│ • Draw prob nhận thêm +0.026                           │
│ • Away win prob nhận thêm +0.028                       │
└────────────────────────────────────────────────────────┘
```

---

## 3. Current Implementation

### 3.1. `HomeAdvantageSignalTransformer`
Toàn bộ logic tính toán nằm tại [HomeAdvantageSignalTransformer.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformer.kt):

```kotlin
object HomeAdvantageSignalTransformer {

    fun transform(
        isNeutralVenue: Boolean = false,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Signal3Way {
        return if (isNeutralVenue) {
            val drawProb = config.baselineDrawProb
            val splitProb = (1.0 - drawProb) / 2.0
            Signal3Way(
                homeProb = splitProb,
                drawProb = drawProb,
                awayProb = splitProb,
                weight = config.homeAdvantageWeight,
                name = "Neutral Venue"
            )
        } else {
            Signal3Way(
                homeProb = config.homeAdvantageProbHome,
                drawProb = config.homeAdvantageProbDraw,
                awayProb = config.homeAdvantageProbAway,
                weight = config.homeAdvantageWeight,
                name = "Home Advantage"
            )
        }
    }
}
```

### 3.2. Thông số cấu hình mặc định (`PredictionWeightConfig`)
Theo [PredictionWeightConfig.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/PredictionWeightConfig.kt):
- `homeAdvantageWeight = 0.10` (Trọng số $10\%$)
- `homeAdvantageProbHome = 0.46` ($46\%$)
- `homeAdvantageProbDraw = 0.26` ($26\%$)
- `homeAdvantageProbAway = 0.28` ($28\%$)
- `baselineDrawProb = 0.26` ($26\%$)

### 3.3. Hành vi Runtime thực tế
1. `HomeAdvantageSignalTransformer` **hoàn toàn không sử dụng** `homeTeamId`, `awayTeamId`, `venue`, `leagueId`, hay bất kỳ dữ liệu thi đấu nào của hai đội.
2. Transformer là một **Prior Probability Generator tĩnh**:
   - Nếu `isNeutralVenue == false`: Trả về phân phối lệch hẳn về Home: $[P(H)=0.46, P(D)=0.26, P(A)=0.28]$.
   - Nếu `isNeutralVenue == true`: Trả về phân phối đối xứng: $[P(H)=0.37, P(D)=0.26, P(A)=0.37]$.
3. **Thực tế kích hoạt**: Do tất cả caller sites trong ứng dụng (Presentation ViewModel, RunDailyBacktestUseCase, BacktestPredictionUseCase) đều khởi tạo `MatchPredictionContext` mà không truyền tham số `isNeutralVenue`, giá trị này luôn nhận giá trị mặc định `false`.
4. **Kết quả**: **100% các trận đấu được đưa vào Prediction Pipeline trong TrueLab đều được áp dụng phân phối $[0.46, 0.26, 0.28]$ với trọng số $10\%$**, cộng thẳng $+0.046$ vào xác suất của đội Side 1 và $+0.028$ cho đội Side 2.

---

## 4. Semantic Analysis

| Khái niệm | Định nghĩa Khoa học / Nghiệp vụ | Cách TrueLab biểu diễn trong Code | Độ lệch ngữ nghĩa (Semantic Gap) |
| :--- | :--- | :--- | :--- |
| **Fixture-side Home (Side 1)** | Đội đứng tên trước trong cặp đấu (Side A vs Side B) do bốc thăm, xếp lịch hành chính hoặc chỉ định của ban tổ chức giải đấu. | `Match.homeTeam`, `MatchEntity.homeTeamId`, `MatchPredictionContext.homeTeamId`. | **Không có sự phân biệt**: TrueLab coi Side 1 mặc định là Physical Home Team. |
| **Physical Home Advantage** | Lợi thế sân bãi thực tế phát sinh từ: quen thuộc mặt sân/kích thước sân, khí hậu/độ cao, sự cổ vũ áp đảo của khán giả nhà, không bị kiệt sức do di chuyển, và áp lực tâm lý lên trọng tài. | Được mô hình hóa thành xác suất tiên nghiệm $[P(H)=0.46, P(D)=0.26, P(A)=0.28]$ với trọng số $10\%$. | **Đồng nhất hóa sai**: Mọi đội nằm ở Side 1 đều được hưởng lợi thế này, bất kể trận đấu diễn ra ở đâu. |
| **Neutral Venue** | Trận đấu diễn ra tại sân vận động trung lập, thành phố đăng cai, hoặc quốc gia thứ ba (World Cup, Euro, Chung kết C1, Sân trung lập do án phạt). | Được định nghĩa trong transformer với $[P(H)=0.37, P(D)=0.26, P(A)=0.37]$ thông qua cờ `isNeutralVenue`. | **Mã nguồn bị cô lập (Dead Branch)**: Logic neutral venue có tồn tại trong code nhưng không bao giờ được kích hoạt ở runtime vì không có nguồn dữ liệu cung cấp `isNeutralVenue = true`. |

---

## 5. Neutral Handling

### 5.1. Rà soát trường dữ liệu ở tất cả các tầng
- **Tầng Network DTO ([MatchDto.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/remote/dto/MatchDto.kt))**: Không có `is_neutral`, `neutral`, `venue`, `stadium`.
- **Tầng Database Entity ([MatchEntity.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/entity/MatchEntity.kt))**: Không có cột nào lưu thông tin sân bãi hay tính chất trung lập.
- **Tầng Domain Model ([Match.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/Match.kt))**: Không có thuộc tính `isNeutral`, `venue`, `venueCountry`.
- **Tầng Prediction Context ([MatchPredictionContext.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/MatchPredictionContext.kt))**:
  - Dòng 40: `val isNeutralVenue: Boolean = false`
  - Trường này tồn tại nhưng là default parameter và không có bất kỳ logic mapper/heuristic nào gán giá trị cho nó dựa trên dữ liệu trận đấu.

### 5.2. Rà soát các Caller Sites
- [PredictionViewModel.kt:196-206](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt#L196-L206): Không truyền `isNeutralVenue` $\rightarrow$ `false`.
- [RunDailyBacktestUseCase.kt:157-167](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt#L157-L167): Không truyền `isNeutralVenue` $\rightarrow$ `false`.
- [BacktestPredictionUseCase.kt:89-99](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt#L89-L99): Không truyền `isNeutralVenue` $\rightarrow$ `false`.

> **Khẳng định:** Hiện tại TrueLab **hoàn toàn không có khả năng nhận biết hoặc xử lý neutral venue trong môi trường runtime**. Nhánh `isNeutralVenue = true` chỉ được gọi trong các Unit Test nhân tạo ([PredictMatchOutcomeUseCaseTest.kt](../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCaseTest.kt#L247) và [HomeAdvantageSignalTransformerTest.kt](../../core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformerTest.kt#L28)).

---

## 6. Code Evidence

| STT | Vị trí File | Dòng | Nội dung / Bằng chứng | Nguồn xác minh |
| :--- | :--- | :--- | :--- | :--- |
| 1 | [MatchDto.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/remote/dto/MatchDto.kt) | 14–25 | `MatchRecord` chỉ có `home_team`, `away_team`, `competition_id`, `start_time_date`, `status`, `minutes`. Không có venue/neutral. | `Verified from TrueLab` |
| 2 | [MatchEntity.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/match/local/entity/MatchEntity.kt) | 45–57 | `MatchEntity` schema trong Room SQLite chỉ lưu `homeTeamId`, `awayTeamId`, `leagueId`, `startTimeDate`, `status`, `season`, `minutes`. | `Verified from TrueLab` |
| 3 | [RoomMappers.kt](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/local/mapper/RoomMappers.kt) | 200–210 | `MatchRecord.toMatchEntity()` gán thẳng `homeTeam.id` vào `homeTeamId` mà không có bước kiểm tra venue. | `Verified from TrueLab` |
| 4 | [Match.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/match/model/Match.kt) | 3–16 | Domain `Match` không có property `isNeutral` hay `venue`. | `Verified from TrueLab` |
| 5 | [MatchPredictionContext.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/MatchPredictionContext.kt) | 40 | `val isNeutralVenue: Boolean = false` định nghĩa cờ nhưng để mặc định `false`. | `Verified from TrueLab` |
| 6 | [HomeAdvantageSignalTransformer.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/transformer/HomeAdvantageSignalTransformer.kt) | 11–34 | Logic chuyển đổi chỉ rẽ nhánh theo `isNeutralVenue: Boolean`. Khi `false`, trả về $[0.46, 0.26, 0.28]$. | `Verified from TrueLab` |
| 7 | [PredictMatchOutcomeUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt) | 112 | `val homeAdvSignal = HomeAdvantageSignalTransformer.transform(context.isNeutralVenue, config)` | `Verified from TrueLab` |
| 8 | [PredictMatchOutcomeUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt) | 273–276 | `isAvailable = !context.isNeutralVenue`, gán metadata `isNeutralVenue` vào `PredictionEvidence`. | `Verified from TrueLab` |
| 9 | [PredictionViewModel.kt](../../app/src/main/kotlin/dev/anhquocs/truelab/feature/prediction/presentation/viewmodel/PredictionViewModel.kt) | 196–206 | Gọi `MatchPredictionContext(...)` không truyền `isNeutralVenue`. | `Verified from TrueLab` |
| 10 | [RunDailyBacktestUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt) | 157–167 | Gọi `MatchPredictionContext(...)` không truyền `isNeutralVenue`. | `Verified from TrueLab` |
| 11 | [BacktestPredictionUseCase.kt](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/BacktestPredictionUseCase.kt) | 89–99 | Gọi `MatchPredictionContext(...)` không truyền `isNeutralVenue`. | `Verified from TrueLab` |

---

## 7. Risk Assessment

### 7.1. Phân loại theo nhóm giải đấu

#### 1. Domestic Leagues (Giải VĐQG thông thường: Ngoại Hạng Anh, La Liga, Serie A, Bundesliga, V-League)
- **Mức độ rủi ro:** **THẤP** (Low Risk).
- **Thực tế:** Hầu hết các trận đấu diễn ra theo thể thức lượt đi - lượt về tại sân nhà của đội Side 1.
- **Ngoại lệ:** Trận derby cùng chia sẻ một sân vận động (e.g. AC Milan vs Inter Milan tại San Siro, AS Roma vs Lazio tại Olimpico) hoặc các trận bị phạt thi đấu sân trung lập không khán giả. Ở các trận này, việc gán $+18\%$ xác suất cho Side 1 là có sai lệch nhẹ nhưng chấp nhận được trong mô hình heuristic tổng quát.

#### 2. International Tournaments (World Cup, Euro, Copa America, Asian Cup, CAN, Olympic Football)
- **Mức độ rủi ro:** **RẤT CAO** (Critical Risk).
- **Thực tế:** Các đội thi đấu tại các thành phố/sân vận động của quốc gia đăng cai. Side 1 (e.g. Pháp vs Argentina trong trận Chung kết World Cup 2022 tại Qatar) hoàn toàn không có lợi thế sân nhà vật lý so với Side 2.
- **Hệ quả trong TrueLab:** Đội Side 1 (Pháp) được tự động cộng thêm $+0.046$ vào xác suất thắng và Side 2 (Argentina) chỉ nhận $+0.028$, tạo ra một độ lệch nhân tạo $+1.8\%$ (chưa kể H2H Laplace prior $h2hPriorHome = 0.45$ cũng thiên vị Side 1).

#### 3. Cup Finals & Centralized Stages (Chung kết UEFA Champions League, Europa League, FA Cup Bán kết/Chung kết tại Wembley)
- **Mức độ rủi ro:** **CAO** (High Risk).
- **Thực tế:** Địa điểm thi đấu được chỉ định trước (sân trung lập).
- **Hệ quả trong TrueLab:** Side 1 vẫn được hưởng trọn vẹn $+10\%$ trọng số Home Advantage.

### 7.2. Định lượng ảnh hưởng lên xác suất cuối cùng
Trong pipeline 6 tín hiệu với tổng trọng số chuẩn $W = 1.0$:
- Đóng góp vào xác suất $P(\text{Home})$: $0.10 \times 0.46 = +0.046$ ($4.6\%$).
- Đóng góp vào xác suất $P(\text{Draw})$: $0.10 \times 0.26 = +0.026$ ($2.6\%$).
- Đóng góp vào xác suất $P(\text{Away})$: $0.10 \times 0.28 = +0.028$ ($2.8\%$).
- **Chênh lệch thiên vị Home so với Away do tín hiệu này tạo ra:**
  $$\Delta P(H - A) = 0.046 - 0.028 = +0.018 \quad (+1.8\%)$$

---

## 8. Relationship With Draw Underprediction

Trong báo cáo [Draw Underprediction Audit](draw-underprediction-audit.md), nguyên nhân gốc rễ (Root Cause) của việc tỉ lệ đoán Hòa (Draw Recall) bị $0\%$ đã được chỉ ra là:
1. **Phân rã xác suất Hòa của các tín hiệu (Decomposition Formula)**: $P(D)$ của hầu hết các tín hiệu bị neo chặt vào $0.26 - 0.27$, trong khi $P(H)$ và $P(A)$ nhận toàn bộ phần dư $(1 - P(D))$.
2. **Quy tắc quyết định $\text{argmax}$**: Khi $\max(P(H), P(A)) > P(D)$, kết quả dự đoán luôn chọn Home hoặc Away.

### Phân tích mối liên hệ có bằng chứng:
- **Home Advantage KHÔNG PHẢI là nguyên nhân gốc rễ của Draw Underprediction**:
  - Bản thân `HomeAdvantageSignalTransformer` gán $P(D) = 0.26$ cho cả trường hợp sân nhà lẫn sân trung lập.
  - Ngay cả khi `isNeutralVenue = true`, $P(D)$ vẫn là $0.26$ (và $P(H) = P(A) = 0.37$).
- **Home Advantage ĐÓNG VAI TRÒ LÀ TÁC NHÂN LÀM TRẦM TRỌNG THÊM (Aggravating Factor) độ lệch giữa Home và Draw**:
  - Trong các trận đấu cân bằng (Elo ngang nhau, phong độ ngang nhau), $P(H)$ và $P(A)$ từ các signal khác có thể xấp xỉ $0.37$.
  - Tuy nhiên, Home Advantage cộng thêm $+0.046$ cho Home và chỉ $+0.026$ cho Draw, đẩy $P(\text{Home})$ tổng hợp lên vùng $0.39 - 0.43$, trong khi $P(\text{Draw})$ vẫn bị kẹt ở $0.26 - 0.28$.
  - Điều này đảm bảo $P(\text{Home})$ luôn áp đảo $P(\text{Draw})$ trong phép so sánh $\text{argmax}$, triệt tiêu hoàn toàn khả năng mô hình dự đoán kết quả Hòa cho các trận đấu cân bằng.

---

## 9. Conclusion

### Kết luận phân loại:
$$\mathbf{SEMANTIC\ MISMATCH\ \&\ DATA\ LIMITATION}$$

### Giải thích căn cứ:
1. **Semantic Mismatch (`Verified from TrueLab`)**:
   - Mọi tầng trong TrueLab từ DTO, Entity, Domain Model đến UseCase đều coi `home_team` (Side 1 của fixture) là đội có lợi thế sân nhà thực tế.
   - Không có cơ chế phân biệt giữa danh nghĩa lịch thi đấu và địa điểm vật lý.
2. **Data Limitation (`Verified from TrueScore`)**:
   - Data source TrueScore không cung cấp cờ `is_neutral` hoặc thông tin sân bãi có liên kết vị trí địa lý với đội bóng.
   - Do đó, TrueLab thiếu nguồn dữ liệu đầu vào tin cậy để bật cờ `isNeutralVenue = true`.
3. **Behavioral Impact (`Verified from TrueLab Runtime`)**:
   - Tất cả các trận đấu chạy qua `PredictionViewModel` và `DailyBacktestUseCase` đều bị gán cứng $100\%$ Home Advantage $[0.46, 0.26, 0.28]$, tạo ra sai số thiên vị Side 1 trong các giải đấu cúp, giải đấu quốc tế và các trận chung kết.

---

## 10. Recommended Next Steps (Design / Research Only)

> **Lưu ý:** Đây chỉ là đề xuất định hướng nghiên cứu và thiết kế kiến trúc cho các phase tiếp theo, **TUYỆT ĐỐI KHÔNG triển khai code trong phase audit này**.

### 10.1. Hướng tiếp cận ngắn hạn (Heuristic & Metadata Mapping)
- **League Category Heuristic**: Dựa vào `League.category` hoặc danh sách ID các giải đấu quốc tế đã biết (e.g. World Cup, Euro, Champions League Stage Finals) để gán `isNeutralVenue = true` khi khởi tạo `MatchPredictionContext`.
- **Match Stage / Two-Leg Attributes**: Khai thác trường `stage` hoặc `twoLeg` từ TrueScore (nếu được đồng bộ vào Room) để nhận diện các trận chung kết một lượt tại sân trung lập.

### 10.2. Hướng tiếp cận trung hạn (Schema & Ingestion Enhancement)
- Bổ sung trường `isNeutral: Boolean?` hoặc `venueCountry: String?` vào `MatchEntity` trong Room Migration tiếp theo (Schema v5).
- Tích hợp logic suy luận quốc gia: Nếu `venue.country != homeTeam.country` trong các giải đấu cấp câu lạc bộ quốc tế $\rightarrow$ Coi là sân trung lập.

### 10.3. Hướng tiếp cận mô hình hóa (Prediction Model Calibration)
- Nghiên cứu cơ chế điều chỉnh động trọng số `homeAdvantageWeight`:
  - Giảm trọng số đối với các giải đấu không rõ thông tin sân bãi.
  - Phân bổ lại phần trọng số này sang các tín hiệu khách quan hơn như Elo ($25\%$) hoặc Odds thị trường ($25\%$) khi thiếu độ tin cậy về địa điểm thi đấu.
