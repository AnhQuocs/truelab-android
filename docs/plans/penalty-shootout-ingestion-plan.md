# TrueLab — Kế Hoạch Triển Khai: Penalty Shootout Ingestion & Display

Tài liệu này là bản kế hoạch kỹ thuật chi tiết (Technical Implementation Plan) cho việc tiếp nhận, lưu trữ và hiển thị thông tin Loạt sút luân lưu (Penalty Shootout) trong ứng dụng TrueLab, dựa trên cấu trúc dữ liệu thực tế từ hệ thống upstream TrueScore.

---

## 1. Mục Tiêu (Goal)

1. **Tiếp nhận & Tiếp nối dữ liệu Penalty Shootout từ TrueScore:**
   - Tiếp nhận các trường dữ liệu Penalty từ TrueScore API: `attributes.is_penalty`, `attributes.penalty_result.home_score`, `attributes.penalty_result.away_score`.
   - Lưu trữ và chuyển tải thông tin luân lưu xuyên suốt qua các tầng: **Remote DTO $\to$ Room Database Entity $\to$ Domain Entities $\to$ Presentation UI Models**.
2. **Minh bạch thông tin kết quả thi đấu trên giao diện (UI Clarity):**
   - Giúp người dùng hiểu rõ bản chất kết quả thi đấu của các trận knock-out/cup có phân định thắng thua bằng loạt sút luân lưu (Ví dụ: `2 - 2 (PEN 4-2)`), tránh hiểu nhầm rằng trận đấu chỉ đơn giản kết thúc với tỷ số hòa 90/120 phút.
3. **Bảo tồn tuyệt đối Kiến trúc & Mô hình toán học 1X2 hiện tại:**
   - **Ground Truth 1X2 giữ nguyên $100\%$**: Tỷ số chính thức FT/AET (`homeScore` vs `awayScore`) tiếp tục được dùng làm nhãn `ActualOutcome` duy nhất cho bài toán dự đoán 1X2.
   - **Bảo vệ tuyệt đối Candidate A (Phase B6)**: Giữ nguyên siêu tham số $\delta = 0.04, \theta = 0.255$, không làm thay đổi hay xáo trộn pipeline dự đoán, các chỉ số đánh giá (Accuracy, Precision, Recall, Macro F1, Brier Score) và Odds Signal logic.

---

## 2. Bối Cảnh & Vấn Đề (Background & Problem Statement)

### 2.1. Hiện trạng trong TrueLab
- TrueLab hiện tại chỉ lưu trữ tỷ số `homeScore` và `awayScore` trong `MatchEntity` và `Match`.
- Đối với các trận đấu knock-out phải bước vào loạt sút luân lưu (như trận Indonesia vs Thailand có tỷ số 2-2 sau 120 phút, Indonesia thắng luân lưu 4-2):
  - TrueLab nhận `homeScore = 2, awayScore = 2`.
  - Màn hình Backtest / Prediction hiển thị: `Thực tế: Hòa | 2 - 2`.
  - Dù thuật toán đưa ra dự đoán `HOME_WIN` hoặc `AWAY_WIN`, hệ thống vẫn chấm `ActualOutcome = DRAW`.
- Điều này gây ra sự hiểu nhầm cho người dùng khi xem giao diện Backtest/Chi tiết trận đấu, vì trên thực tế đã có một đội giành chiến thắng chung cuộc qua loạt sút luân lưu.

### 2.2. Dữ liệu có sẵn từ Upstream TrueScore
Khảo sát API TrueScore xác nhận hệ thống backend đã hỗ trợ sẵn các trường thông tin penalty trong Match API:
- `attributes.is_penalty: Boolean`
- `attributes.penalty_result.home_score: Int`
- `attributes.penalty_result.away_score: Int`

*Ví dụ payload từ TrueScore:*
```json
{
  "attributes": {
    "home_score": 2,
    "away_score": 2,
    "is_penalty": true,
    "penalty_result": {
      "home_score": 4,
      "away_score": 2
    }
  }
}
```

---

## 3. Hành Vi Hiện Tại vs Hành Vi Kỳ Vọng (Current vs Expected Behavior)

| Đặc Điểm / Thành Phần | Hành Vi Hiện Tại (Current Behavior) | Hành Vi Kỳ Vọng (Expected Behavior) |
| :--- | :--- | :--- |
| **Data Ingestion (DTO)** | Chỉ đọc `home_score` và `away_score`. | Đọc thêm `is_penalty`, `penalty_result.home_score`, `penalty_result.away_score` (nullable/optional). |
| **Room Database** | Bảng `matches` chỉ lưu `homeScore` và `awayScore`. | Bảng `matches` bổ sung 3 cột: `isPenalty`, `homePenaltyScore`, `awayPenaltyScore`. |
| **Domain Model (`Match`)** | Chỉ chứa `homeScore` và `awayScore`. | Bổ sung `isPenalty: Boolean = false`, `homePenaltyScore: Int? = null`, `awayPenaltyScore: Int? = null`. |
| **Ground Truth 1X2** | `homeScore == awayScore` $\implies$ `DRAW`. | **GIỮ NGUYÊN $100\%$**: `homeScore == awayScore` $\implies$ `DRAW`. |
| **Prediction Pipeline & B6** | Candidate A ($\delta=0.04, \theta=0.255$). | **GIỮ NGUYÊN $100\%$**: Không thay đổi bất kỳ logic hay tham số toán học nào. |
| **UI Hiển Thị Tỷ Số (Backtest/Detail)** | Hiển thị: `2 - 2` | Hiển thị: `2 - 2 (PEN 4-2)` đối với trận có Penalty. |
| **UI Trận Không Có Penalty** | Hiển thị: `1 - 0` hoặc `1 - 1` | Hiển thị giữ nguyên: `1 - 0` hoặc `1 - 1` (Không có chữ PEN). |

---

## 4. Phạm Vi Triển Khai (Scope & Non-Scope)

### 4.1. Trong Phạm Vi (In-Scope)
- Cập nhật DTO `MatchRecord` & `PenaltyResultDto` để parse dữ liệu từ TrueScore API.
- Cập nhật `MatchEntity` trong Room Database với 3 cột mới (Nullable/Default).
- Cập nhật Domain Model `Match`, `BacktestMatchRecord` và các Data Mappers (`RoomMappers.kt`).
- Cập nhật UI Formatter / UI Components trên màn hình Backtest (`BacktestVisualizerScreen`, `BacktestItemCard`) và Prediction (`PredictionScreen`) để hiển thị chuỗi `homeScore - awayScore (PEN homePen-awayPen)`.
- Viết Unit Tests kiểm tra parsing, Room mapping và UI Score formatting.

### 4.2. Ngoài Phạm Vi (Non-Scope)
- **KHÔNG** thay đổi thuật toán dự đoán (`PredictMatchOutcomeUseCase`, `DefaultWeightedScorer`).
- **KHÔNG** thay đổi cấu hình Candidate A / B6 ($\delta = 0.04, \theta = 0.255$).
- **KHÔNG** thay đổi công thức tính các chỉ số `Accuracy`, `Precision`, `Recall`, `F1`, `Brier Score`.
- **KHÔNG** tạo metric mới "Winner of Tie" hay "To Qualify" trong scope này (xem như định hướng mở rộng tương lai).
- **KHÔNG** đọc hay lưu các trường `home_order`, `away_order`, `full_time_score`, `extra_time_score` không cần thiết.

---

## 5. Cấu Trúc Thay Đổi Dữ Liệu (Data Model Changes)

### 5.1. Remote DTO (`MatchDto.kt`)
```kotlin
@Serializable
data class MatchRecord(
    @SerialName("id") val id: Long,
    @SerialName("home_team") val homeTeam: TeamInfo,
    @SerialName("away_team") val awayTeam: TeamInfo,
    @SerialName("home_score") val homeScore: Int,
    @SerialName("away_score") val awayScore: Int,
    @SerialName("is_penalty") val isPenalty: Boolean? = false,
    @SerialName("penalty_result") val penaltyResult: PenaltyResultDto? = null,
    @SerialName("start_time_date") val startTimeDate: String,
    @SerialName("status") val status: String,
    ...
)

@Serializable
data class PenaltyResultDto(
    @SerialName("home_score") val homeScore: Int? = null,
    @SerialName("away_score") val awayScore: Int? = null
)
```

### 5.2. Local Entity (`MatchEntity.kt`)
```kotlin
@Entity(tableName = "matches", ...)
data class MatchEntity(
    @PrimaryKey val id: Long,
    val homeTeamId: Int,
    val awayTeamId: Int,
    val homeScore: Int?,
    val awayScore: Int?,
    val isPenalty: Boolean = false,
    val homePenaltyScore: Int? = null,
    val awayPenaltyScore: Int? = null,
    val startTimeDate: String,
    val status: String,
    ...
)
```

### 5.3. Domain Entity (`Match.kt` & `EvaluationModels.kt`)
```kotlin
// In Match.kt
data class Match(
    val id: Long,
    val homeTeam: TeamSummary,
    val awayTeam: TeamSummary,
    val homeScore: Int?,
    val awayScore: Int?,
    val isPenalty: Boolean = false,
    val homePenaltyScore: Int? = null,
    val awayPenaltyScore: Int? = null,
    val startTimeDate: String,
    val status: MatchStatus,
    ...
)

// In EvaluationModels.kt (BacktestMatchRecord)
data class BacktestMatchRecord(
    val matchId: Long,
    ...
    val homeScore: Int,
    val awayScore: Int,
    val isPenalty: Boolean = false,
    val homePenaltyScore: Int? = null,
    val awayPenaltyScore: Int? = null,
    val hasUsableOdds: Boolean
)
```

---

## 6. Luồng Dữ Liệu Chi Tiết (DTO $\to$ Entity $\to$ Domain $\to$ UI Data Flow)

```text
[TrueScore Match API Response]
           │
           ▼
   [MatchRecord DTO] (is_penalty, penalty_result)
           │
           ▼ (RoomMappers.toMatchEntity())
   [MatchEntity (Room DB)] (isPenalty, homePenaltyScore, awayPenaltyScore)
           │
           ▼ (RoomMappers.toMatch())
   [Match (Domain Entity)]
           │
           ├──► [PredictMatchOutcomeUseCase] (Dự đoán 1X2 giữ nguyên)
           ├──► [RunDailyBacktestUseCase] (Xác định ActualOutcome 1X2 giữ nguyên: homeScore vs awayScore)
           │         │
           │         ▼
           │    [BacktestMatchRecord] (Mang theo isPenalty, homePen, awayPen)
           │
           ▼ (BacktestUiMapper)
   [BacktestMatchItemUiState] (scoreText = "2 - 2 (PEN 4-2)")
           │
           ▼
   [BacktestVisualizerScreen / UI Components]
```

---

## 7. Phương Án Xử Lý Database & Room Migration

- **Database Schema Version:** Tăng version từ `N` lên `N + 1`.
- **Migration Strategy:** Thêm 3 cột mới có giá trị mặc định, đảm bảo không mất mát dữ liệu hiện có trong SQLite:
```kotlin
val MIGRATION_N_N1 = object : Migration(N, N + 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN isPenalty INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE matches ADD COLUMN homePenaltyScore INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE matches ADD COLUMN awayPenaltyScore INTEGER DEFAULT NULL")
    }
}
```

---

## 8. Quy Ước Hiển Thị Giao Diện (UI Formatting Conventions)

Xây dựng Extension Formatter chuẩn trong Presentation Layer:

```kotlin
fun formatMatchScore(
    homeScore: Int?,
    awayScore: Int?,
    isPenalty: Boolean,
    homePenaltyScore: Int?,
    awayPenaltyScore: Int?
): String {
    if (homeScore == null || awayScore == null) return "- : -"
    val mainScore = "$homeScore - $awayScore"
    return if (isPenalty && homePenaltyScore != null && awayPenaltyScore != null) {
        "$mainScore (PEN $homePenaltyScore-$awayPenaltyScore)"
    } else {
        mainScore
    }
}
```

**Các ví dụ hiển thị thực tế:**
- Trận 90 phút bình thường: `2 - 1`
- Trận hòa 90 phút bình thường: `1 - 1`
- Trận hòa 120 phút có Penalty Shootout: `2 - 2 (PEN 4-2)`

---

## 9. Khả Năng Tương Thích Nguồn Dữ Liệu Cũ (Backward Compatibility)

1. **Dữ liệu lịch sử cũ trong Room DB:**
   - 3 cột mới nhận giá trị mặc định (`isPenalty = false`, `homePenaltyScore = null`, `awayPenaltyScore = null`).
   - UI tự động fallback về hiển thị tỷ số chuẩn `2 - 2` mà không gây crash hay lỗi giao diện.
2. **Backtest & Independent Test Suite:**
   - Tất cả các Unit Test hiện tại (`RunDailyBacktestUseCaseTest`, `EvaluateDrawModelsOnIndependentTestUseCaseTest`) tiếp tục PASS $100\%$ mà không cần sửa đổi giả định kết quả.

---

## 10. Kế Hoạch Kiểm Thử (Test Plan)

1. **Unit Test - DTO & Room Mapping (`RoomMappersTest`):**
   - Verify `MatchRecord` chứa `is_penalty = true` và `penalty_result = {home_score: 4, away_score: 2}` được map đúng sang `MatchEntity` và `Match`.
   - Verify `MatchRecord` không có penalty (`is_penalty = null` hoặc `false`) được map đúng với `isPenalty = false`, `homePenaltyScore = null`.
2. **Unit Test - UI Score Formatter (`ScoreFormatterTest`):**
   - Test format cho trận đấu bình thường, trận đấu có penalty, và trận đấu thiếu score.
3. **Unit Test - Backtest Orchestration (`RunDailyBacktestUseCaseTest`):**
   - Confirm `ActualOutcome` cho trận `2-2 (PEN 4-2)` vẫn trả về `DRAW`.
   - Confirm `BacktestMatchRecord` lưu trữ đầy đủ `isPenalty = true`, `homePenaltyScore = 4`, `awayPenaltyScore = 2`.
4. **Integration Test - Database Migration (`DatabaseMigrationTest`):**
   - Kiểm tra chạy migration thêm 3 cột mới vào Room DB sạch sẽ không gây mất dữ liệu.

---

## 11. Tiêu Chí Nộp Bài (Acceptance Criteria)

- [ ] Parse thành công `is_penalty`, `home_score`, `away_score` từ TrueScore Match API.
- [ ] Room DB lưu trữ chính xác 3 trường `isPenalty`, `homePenaltyScore`, `awayPenaltyScore`.
- [ ] Trận đấu `2-2` có Pen `4-2` được UI hiển thị đúng định dạng `2 - 2 (PEN 4-2)`.
- [ ] Trận đấu không có Penalty hiển thị tỷ số bình thường `2 - 2` hoặc `1 - 0`.
- [ ] `ActualOutcome` 1X2 cho trận `2-2` có Pen vẫn giữ nguyên là `DRAW` ($100\%$ không đổi).
- [ ] Toàn bộ thuật toán dự đoán (Candidate A / B6, thresholds $\delta=0.04, \theta=0.255$, Odds) được giữ nguyên $100\%$.
- [ ] $100\%$ Unit Tests vượt qua thành công và build sạch không cảnh báo.

---

## 12. Quản Lý Rủi Ro (Risk Management)

| Rủi Ro Nhận Diện | Mức Độ | Biện Pháp Tối Ưu / Giảm Thượng |
| :--- | :---: | :--- |
| API trả về `is_penalty = true` nhưng `penalty_result` bị `null` | Thấp | Kiểm tra điều kiện kết hợp: Chỉ hiển thị chuỗi `(PEN ...)` khi cả `isPenalty == true` VÀ 2 trường penalty score `!= null`. |
| Migration Room DB làm chậm lần đầu khởi động | Thấp | Sử dụng câu lệnh `ALTER TABLE` mặc định cực nhanh của SQLite, không cần rebuild lại toàn bộ bảng. |
| Người dùng nhầm lẫn `ActualOutcome` với kết quả Pen | Trung bình | Đã giữ nguyên `ActualOutcome = DRAW` cho bài toán 1X2 và hiển thị rõ thông tin Pen phụ bên cạnh. |

---

## 13. Các Bước Triển Khai Kỹ Thuật Dự Kiến (Implementation Steps)

1. **Bước 1:** Cập nhật DTO `MatchDto.kt` bổ sung `isPenalty` và `PenaltyResultDto`.
2. **Bước 2:** Cập nhật Room Entity `MatchEntity.kt` và tăng DB Version / Migration script.
3. **Bước 3:** Cập nhật Domain Models `Match.kt` và `EvaluationModels.kt` (`BacktestMatchRecord`).
4. **Bước 4:** Cập nhật `RoomMappers.kt` để chuyển đổi dữ liệu penalty qua các tầng.
5. **Bước 5:** Cập nhật `RunDailyBacktestUseCase.kt` truyền thông tin penalty vào `BacktestMatchRecord`.
6. **Bước 6:** Cập nhật Score Formatter và Presentation UI Components (`BacktestItemCard`, `PredictionScreen`).
7. **Bước 7:** Viết Unit Tests bổ sung và xác nhận build thành công.
