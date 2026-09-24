# Báo Cáo Triển Khai: Domain D4.3 — Home/Away Splits

> **Module**: `:core:domain`  
> **Trạng thái**: Hoàn thành  
> **Thuộc kế hoạch**: `docs/plans/domain-d4-plan.md` (Domain D4.3)  
> **Kiểm thử**: 12/12 tests mới PASS — Toàn bộ hệ thống: 362/362 tests PASS  

---

## 1. Tổng Quan Mục Tiêu & Phạm Vi (Scope)

Sub-phase **Domain D4.3** triển khai UseCase và Data Model tính toán phân tích hiệu suất thi đấu của một đội bóng phân tách theo 3 nhóm độc lập:
1. **Home Split**: Thành tích thi đấu trên sân nhà.
2. **Away Split**: Thành tích thi đấu trên sân khách.
3. **Total Split**: Tổng hợp toàn bộ thành tích thi đấu.

Toàn bộ logic được xây dựng trên Pure Kotlin/JVM tại `:core:domain`, không phụ thuộc vào Android SDK, Room, Compose hay Presentation layer.

---

## 2. Mô Hình Dữ Liệu (Domain Models)

Vị trí: [`core/domain/.../team/model/TeamHomeAwaySplits.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/TeamHomeAwaySplits.kt)

```kotlin
data class TeamPerformanceSplit(
    val played: Int,
    val won: Int,
    val draw: Int,
    val loss: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDiff: Int,
    val points: Int,
    val winRate: Double
)

data class TeamHomeAwaySplits(
    val teamId: Int,
    val homeSplit: TeamPerformanceSplit,
    val awaySplit: TeamPerformanceSplit,
    val totalSplit: TeamPerformanceSplit
)
```

---

## 3. Quy Tắc & Công Thức Tính Toán (Calculation Rules)

1. **Điều kiện lọc trận hợp lệ**:
   - `match.isEnded == true` (trận đấu đã kết thúc).
   - `match.homeScore != null && match.awayScore != null` (có tỷ số hợp lệ).
   - `match.homeTeam.id == teamId || match.awayTeam.id == teamId` (đội bóng thực sự tham gia trận đấu).

2. **Phân loại vai trò**:
   - **Sân nhà (Home)**: `match.homeTeam.id == teamId` $\implies$ `goalsFor = homeScore`, `goalsAgainst = awayScore`.
   - **Sân khách (Away)**: `match.awayTeam.id == teamId` $\implies$ `goalsFor = awayScore`, `goalsAgainst = homeScore`.

3. **Xác định kết quả từng trận**:
   - `goalsFor > goalsAgainst` $\implies$ **WIN** (`won++`).
   - `goalsFor == goalsAgainst` $\implies$ **DRAW** (`draw++`).
   - `goalsFor < goalsAgainst` $\implies$ **LOSS** (`loss++`).

4. **Tổng hợp chỉ số**:
   - **Hiệu số bàn thắng**: $\text{goalDiff} = \text{goalsFor} - \text{goalsAgainst}$.
   - **Điểm số**: $\text{points} = \text{won} \times 3 + \text{draw} \times 1$.
   - **Tỷ lệ thắng**: 
     $$\text{winRate} = \begin{cases} \frac{\text{won}}{\text{played}} & \text{nếu } \text{played} > 0 \\ 0.0 & \text{nếu } \text{played} == 0 \end{cases}$$

5. **Tính nhất quán phân hoạch (Total Split Consistency)**:
   - $\text{Total played} = \text{Home played} + \text{Away played}$
   - $\text{Total points} = \text{Home points} + \text{Away points}$
   - $\text{Total goalsFor} = \text{Home goalsFor} + \text{Away goalsFor}$
   - $\text{Total goalsAgainst} = \text{Home goalsAgainst} + \text{Away goalsAgainst}$
   - $\text{Total goalDiff} = \text{Home goalDiff} + \text{Away goalDiff}$

---

## 4. Xử Lý Trường Hợp Biên (Edge Cases)

- **Danh sách trận rỗng (`matches.isEmpty()`)**: Trả về `TeamHomeAwaySplits` với cả 3 nhóm đều là `TeamPerformanceSplit.EMPTY` (`played = 0`, `winRate = 0.0`, `points = 0`).
- **Chỉ có trận sân nhà / Chỉ có trận sân khách**: Nhóm không có trận đấu trả về `TeamPerformanceSplit.EMPTY`, nhóm có trận đấu tính toán chính xác và khớp với `TotalSplit`.
- **Trận chưa kết thúc (`isEnded == false`)**: Tự động bỏ qua không tính.
- **Trận thiếu tỷ số (`homeScore == null` hoặc `awayScore == null`)**: Tự động bỏ qua.
- **Trận giữa các đội khác (Target team không tham gia)**: Tự động bỏ qua.
- **Hiệu số âm**: `goalDiff` giữ đúng dấu âm mà không bị ép trị tuyệt đối.
- **Thứ tự đầu vào (Scrambled input)**: Xử lý tất định và không phụ thuộc vào thứ tự danh sách đầu vào.

---

## 5. Ranh Giới Kiến Trúc (Architecture Boundary)

- Toàn bộ source code nằm trong `:core:domain` (Pure Kotlin/JVM).
- Không import bất kỳ thư viện Android, Compose, Room, hay Retrofit nào.
- `:core:algorithm` được bảo toàn 100% (Frozen 122/122 tests).
- Presentation & Data layer không bị can thiệp.

---

## 6. Kết Quả Kiểm Thử (Unit Tests & Regression)

### 6.1. Ma Trận Kiểm Thử `CalculateHomeAwaySplitsUseCaseTest` (12/12 PASS)
1. `Empty matches list returns all empty zero splits`
2. `Home-only matches produce valid home split while away split remains empty`
3. `Away-only matches produce valid away split while home split remains empty`
4. `Mixed home and away matches calculate separate and combined splits accurately`
5. `Points calculation obeys won times 3 plus draw rule`
6. `Negative goal difference handles proper sign in goalDiff`
7. `Unfinished and scheduled matches are completely ignored`
8. `Matches with null or missing scores are completely ignored`
9. `Matches not involving the target team are completely ignored`
10. `Total split satisfies mathematical partition consistency across home and away`
11. `Scrambled match input order produces deterministic identical splits`
12. `All wins all draws and all losses subsets calculate boundary winRates and points`

### 6.2. Full Regression Baseline

```text
Module :core:algorithm : 122/122 passed (0 failures)
Module :core:domain    : 194/194 passed (0 failures) [156 gốc + 12 D4.1 + 14 D4.2 + 12 D4.3]
Module :app            :  46/46  passed (0 failures)
--------------------------------------------------------------------------------
TỔNG CỘNG              : 362/362 passed (100% SUCCESS)
assembleDebug          : BUILD SUCCESSFUL
```

---

## 7. Trạng Thái Git & File Thay Đổi

- **Chưa commit / Chưa push** (Tuân thủ workflow an toàn).
- Files thay đổi:
  - `A core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/model/TeamHomeAwaySplits.kt`
  - `A core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateHomeAwaySplitsUseCase.kt`
  - `A core/domain/src/test/kotlin/dev/anhquocs/truelab/core/domain/team/usecase/CalculateHomeAwaySplitsUseCaseTest.kt`
  - `A docs/reports/domain-d4.3-home-away-splits.md`
