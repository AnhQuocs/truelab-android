# TrueLab – Kế hoạch Tích hợp Domain sau Core Algorithm (Domain Integration Plan)

Tài liệu này xác lập bản thiết kế kiến trúc và kế hoạch tích hợp chi tiết các thuật toán đã frozen từ module `:core:algorithm` (Phase 1–7) vào tầng nghiệp vụ `:core:domain` của dự án **TrueLab**, tuân thủ nghiêm ngặt nguyên tắc **Clean Architecture** và tính độc lập của các layer.

---

## 1. Tổng quan Kiến trúc Hiện tại (Current Architecture Overview)

Hệ thống TrueLab được tổ chức theo mô hình Đa Module (Multi-Module) với đồ thị phụ thuộc một chiều rõ ràng:

```text
                        :app (Application, Navigation, Presentation, Screens)
                      /   |   \
                     ↓    ↓    ↓
           :core:data  :core:ui (Material 3 Theme, Tokens, Components)
                ↓
           :core:domain (Pure Kotlin/JVM - Entities, UseCases, Repositories)
                ↓
         :core:algorithm (Pure Kotlin/JVM - Phase 1–7: Generic Pure Math/Logic)
```

### Hiện trạng Module & Phụ thuộc:
- **`:core:algorithm`**: Pure Kotlin/JVM (`java-library`). Tuyệt đối **0 phụ thuộc** vào Android SDK hay bất kỳ module nào trong dự án. 122/122 Unit Tests đã vượt qua, toàn bộ API Phase 1–7 đã đóng băng (**FROZEN**).
- **`:core:domain`**: Pure Kotlin/JVM (`java-library`). Phụ thuộc vào `project(":core:algorithm")` và `kotlinx.coroutines.core`. Không phụ thuộc vào `:core:data`, `:core:ui`, hay `:app`.
- **`:core:data`**: Android Library (`com.android.library`). Phụ thuộc vào `:core:domain`, AndroidX Room, Retrofit, OkHttp, Hilt, Coroutines. Chịu trách nhiệm DAO, Entity, Database Room (`TrueLabDatabase`), Mapper và Repository Implementations.
- **`:core:ui`**: Android Library (`com.android.library`). Material 3 Design System, tokens, dimensions, typography, reusable components.
- **`:app`**: Android Application (`com.android.application`). Đóng gói DI (Hilt), điều hướng Navigation Compose, ViewModel và các màn hình (`Home`, `Match`, `Team`, `Analytics`, `Prediction`, `Benchmark`, `Setting`).

---

## 2. Frozen Algorithm Layer (:core:algorithm - Phase 1–7)

Tất cả các thuật toán đã được kiểm thử độc lập (122 tests pass), API generic, không chứa bất kỳ khái niệm bóng đá nào. Đối chiếu trực tiếp với Source Code thực tế:

| Phase | Package / Class | Method / Contract thực tế | Input | Output | Trách nhiệm cốt lõi |
|---|---|---|---|---|---|
| **1. Searching** | `dev.anhquocs.truelab.core.algorithm.searching`<br>• `LinearSearch`<br>• `BinarySearch` | `search(dataset, target, ...)` | `List<T>`, `target: T`, `predicate: (T) -> Boolean` hoặc `comparator: Comparator<T>` | `SearchResult` (index, found, comparisonsCount, executionTimeNs) | Tìm kiếm phần tử tuyến tính hoặc nhị phân (mảng đã sắp xếp) |
| **2. Sorting** | `dev.anhquocs.truelab.core.algorithm.sorting`<br>• `QuickSort`<br>• `MergeSort` | `sort(dataset, comparator)` | `List<T>`, `comparator: Comparator<T>` | `List<T>` (danh sách mới đã sắp xếp, bất biến) | Sắp xếp ổn định (`MergeSort`) hoặc hiệu năng cao (`QuickSort`) |
| **3. Statistics** | `dev.anhquocs.truelab.core.algorithm.statistics`<br>• `DescriptiveStatisticsCalculator` | `calculate(dataset)`<br>`mean(dataset)`<br>`median(dataset)`... | `List<Double>` | `DescriptiveStatistics`<br>(mean, median, variance, stdDev, min, max, iqr, skewness) | Tính toán các chỉ số thống kê mô tả phân phối dữ liệu số (thuật toán Two-Pass chống sai số dấu phẩy động) |
| **4. Trend** | `dev.anhquocs.truelab.core.algorithm.trend`<br>• `SimpleMovingAverageCalculator` | `calculate(dataset, windowSize)` | `data: List<Double>`, `windowSize: Int` | `List<Double>` (dãy giá trị trung bình động) | Làm mịn chuỗi thời gian, phát hiện xu hướng biến động |
| **5. Evaluation** | `dev.anhquocs.truelab.core.algorithm.evaluation`<br>• `FormEvaluator`<br>• `LinearDecayFormEvaluator` | `evaluate(outcomes, windowSize = 5)` | `List<MatchOutcome>` (`WIN`, `DRAW`, `LOSS`), `windowSize: Int` | `FormScore`<br>(score $[0.0, 100.0]$, rawScore, matchesCount, wins, draws, losses, totalPoints, maxPoints) | Đánh giá phong độ gần đây với trọng số suy giảm tuyến tính theo thời gian |
| **6. Rating** | `dev.anhquocs.truelab.core.algorithm.rating`<br>• `EloRatingCalculator` | `expectedScore(rating, opponentRating)`<br>`updateRating(rating, opponentRating, actualScore, kFactor)` | `rating`, `opponentRating`, `actualScore`, `kFactor` | `Double` (điểm Elo cập nhật hoặc kỳ vọng thắng) | Cập nhật và tính toán kỳ vọng điểm sức mạnh đối đầu (Zero-sum Elo) |
| **7. Prediction** | `dev.anhquocs.truelab.core.algorithm.prediction`<br>• `WeightedScorer`<br>• `DefaultWeightedScorer` | **1. 1D Scoring:**<br>`calculateScore(features: List<WeightedFeature>): Double`<br>`calculateScore(dataset, scoreSelector, weightSelector): Double`<br><br>**2. 3-Way Prediction:**<br>`predict3Way(homeScore, drawScore, awayScore): OutcomeProbabilities`<br>`predictOutcome(signals: List<Signal3Way>): OutcomeProbabilities` | **1D:** `List<WeightedFeature>` (score $[0.0, 1.0]$, weight $\ge 0$)<br><br>**3-Way:** `List<Signal3Way>` (homeProb, drawProb, awayProb với tổng $\approx 1.0$, weight $\ge 0$) | **1D:** `Double` $[0.0, 1.0]$<br><br>**3-Way:** `OutcomeProbabilities`<br>(homeWinProb, drawProb, awayWinProb, predictedOutcome, confidenceScore) | **1D:** Tính điểm số tổng hợp tuyến tính $S = \frac{\sum w_i x_i}{\sum w_i}$.<br><br>**3-Way:** Dự đoán phân phối xác suất trận đấu bằng Linear Mixture: $P_c = \frac{\sum w_k \cdot p_{k,c}}{\sum w_k}$ (KHÔNG dùng Softmax, KHÔNG có temperature). |

---

## 3. Trạng thái Hiện tại của :core:domain

### 3.1. Các Domain Model Hiện có:
- `Match`: ID, homeTeam, awayTeam, league, score, matchDate, status, isHomeWin, isAwayWin, isDraw, goalDifference, totalGoals.
- `TeamDetail`: ID, name, logoUrl, league, eloRating, formScore, recentMatches.
- `SeasonRanking`: position, team, played, won, drawn, lost, goalsFor, goalsAgainst, points. *(Hiện có hàm tạm `calculateFormScore()` tự tính toán sơ sài)*.
- `MatchOdds`: matchId, company, homeWinOdds, drawOdds, awayWinOdds.
- `OddsRecordItem`: item chi tiết lịch sử odds. *(Hiện có hàm `calculateImpliedProbability()` chuyển đổi tỷ lệ cược 1X2 thành xác suất ngụ ý)*.
- `PredictionResult`: homeWinProbability, drawProbability, awayWinProbability, confidenceScore, factors. *(Hiện có `companion object` tạm `computeWeightedScoring` chứa logic tính offset hardcode)*.

### 3.2. Repository Interfaces Hiện có:
- `MatchRepository`: `getMatches()`, `getMatchDetail(id)`.
- `TeamRepository`: `getTeamDetail(id)`, `getSeasonRanking(leagueId)`.
- `OddsRepository`: `getMatchOdds(matchId)`, `getOddsHistory(matchId, companyId, oddsType)`.
- `PredictionRepository`: `predictMatch(matchId)`.

### 3.3. UseCases Hiện có:
- `GetMatchesUseCase`: Hiện chỉ lọc bằng Kotlin stdlib `filter` và `sortedBy`.
- `GetMatchDetailUseCase`: Lấy chi tiết trận đấu.
- `GetTeamDetailUseCase`: Lấy chi tiết đội bóng.
- `GetSeasonRankingUseCase`: Lấy BXH giải đấu.
- `GetMatchOddsUseCase`: Lấy tỷ lệ cược 1X2.
- `GetOddsHistoryUseCase`: Lấy lịch sử biến động kèo.
- `PredictMatchUseCase`: Gọi `PredictionRepository.predictMatch(matchId)` (Data layer đang mock/hardcode).

---

## 4. Trạng thái Hiện tại của :core:data

### 4.1. Room Database (`TrueLabDatabase`):
- `MatchEntity` / `MatchDao`:
  - `getRecentMatchesForTeam(teamId, limit = 5)`: Truy vấn 5 trận gần nhất của một đội.
  - `getH2HMatches(teamAId, teamBId)`: Truy vấn các trận đối đầu giữa 2 đội.
  - `getMatchesByLeague(leagueId)`: Lấy danh sách trận theo giải.
- `TeamEntity` / `TeamDao`:
  - Lưu `eloRating` (mặc định `1500.0`) và `formScore` (mặc định `0.0`).
- `OddsEntity` / `OddsDao`:
  - `getOddsHistory(matchId, companyId, oddsType)`: Dãy odds theo thời gian.
  - `getLatestOddsForMatch(matchId)`: Tỷ lệ kèo mới nhất.
- `PredictionEntity` / `PredictionDao`:
  - Lưu kết quả dự đoán xuống local cache.

---

## 5. Mapping Algorithm → Domain (Điểm tích hợp)

| Algorithm | Domain Target UseCase | Trách nhiệm của UseCase trong Domain |
|---|---|---|
| **Searching** | `SearchMatchesUseCase` | Nhận query từ UI, mapping domain match list thành searchable index, gọi `LinearSearch` / `BinarySearch` để trả về danh sách kết quả phù hợp. |
| **Sorting** | `SortMatchesUseCase`<br>`SortStandingsUseCase` | Sắp xếp lịch thi đấu hoặc BXH đa tiêu chí (theo điểm số, hiệu số, phong độ, Elo) bằng `MergeSort` (bảo toàn thứ tự) hoặc `QuickSort`. |
| **Statistics** | `GetTeamStatisticsUseCase`<br>`GetMatchAnalyticsUseCase` | Trích xuất chuỗi bàn thắng, bàn thua từ các trận đã qua, chuyển thành `List<Double>`, gọi `DescriptiveStatisticsCalculator` để tính Mean/Variance/IQR. |
| **Trend** | `AnalyzeOddsTrendUseCase` | Lấy danh sách odds từ `OddsRepository`, chuyển chuỗi tỷ lệ cược thành `List<Double>`, gọi `SimpleMovingAverageCalculator` với window size $k$ để phân tích biến động dòng tiền. |
| **Evaluation** | `CalculateTeamFormUseCase` | Lấy các trận gần nhất từ `MatchRepository`, chuyển đổi kết quả thành `List<MatchOutcome>` (gắn cờ Home/Away), gọi `LinearDecayFormEvaluator.evaluate()` để tính toán `FormScore`. |
| **Rating** | `CalculateEloRatingUseCase`<br>`UpdateTeamEloUseCase` | Lấy điểm Elo hiện tại của 2 đội, gọi `EloRatingCalculator.expectedScore()` hoặc `updateRating()` để tính kỳ vọng thắng hoặc điểm sau trận. |
| **Prediction** | `PredictMatchOutcomeUseCase` | **3-Way Prediction Orchestrator**:<br>1. Thu thập dữ liệu từ các Repository (Match, Team, Odds, Recent Form).<br>2. Chuyển đổi từng yếu tố nghiệp vụ thành `Signal3Way` (gồm `homeProb`, `drawProb`, `awayProb`, `weight`, `name`).<br>3. Đưa danh sách `List<Signal3Way>` vào `WeightedScorer.predictOutcome(signals)`.<br>4. Nhận về `OutcomeProbabilities` và map sang `PredictionResult`. |

---

## 6. Data Availability Matrix

| Algorithm | Input cần thiết | Source hiện tại | Transformation Domain cần thực hiện | Phần còn thiếu |
|---|---|---|---|---|
| **Phase 1: Searching** | `List<Match>`, `query: String` | `MatchRepository.getMatches()` | Trích xuất `match.homeTeam.name`, `match.awayTeam.name`, `match.league.name` | Không thiếu |
| **Phase 2: Sorting** | `List<Match>`, `Comparator` | `MatchRepository.getMatches()` | Định nghĩa `Comparator<Match>` (theo ngày, theo thứ hạng, theo giải) | Không thiếu |
| **Phase 3: Statistics** | `List<Double>` (bàn thắng, bàn thua) | `MatchDao.getRecentMatchesForTeam()` | Map `Match.score` thành `List<Double>` (goals scored/conceded) | Cần method `MatchRepository.getRecentMatches(teamId, limit)` |
| **Phase 4: Trend** | `List<Double>` (odds series), `windowSize` | `OddsRepository.getOddsHistory()` | Map `List<OddsRecordItem>` thành `List<Double>` (homeOdds series) | Cần chuẩn hóa time series |
| **Phase 5: Evaluation** | `List<MatchOutcome>` | `MatchDao.getRecentMatchesForTeam()` | So sánh `score.home` vs `score.away` dựa trên `isHome` để tạo `MatchOutcome.WIN/DRAW/LOSS` | Cần method `MatchRepository.getRecentMatches(teamId, limit)` |
| **Phase 6: Rating** | `ratingA`, `ratingB`, `actualScore` | `TeamRepository.getTeamDetail()` | Lấy `eloRating` của 2 đội | Cần lưu trữ lịch sử Elo cập nhật |
| **Phase 7: Prediction (3-Way)** | `List<Signal3Way>` (Odds, Form, Elo, H2H, HomeAdv) | `MatchRepository`, `TeamRepository`, `OddsRepository`, `CalculateTeamFormUseCase` | Chuyển đổi từng tín hiệu bóng đá thành xác suất 3 chiều (`Signal3Way`):<br>• **Odds Signal**: `OddsRecordItem.calculateImpliedProbability()` $\to$ `Signal3Way`<br>• **Form/Elo/H2H Signals**: Cần xác định công thức chuẩn hóa nghiệp vụ (Business Transformation - Xem Mục 14). | Cần Orchestration UseCase & xác định công thức chuyển đổi tín hiệu nghiệp vụ. |

---

## 7. Next Phase Nhỏ Nhất Hợp Lý: Phase D1 – Domain Form & Prediction Integration

Thay vì refactor ồ ạt toàn bộ hệ thống, **Phase D1** là một **Vertical Slice hoàn chỉnh, độc lập và có giá trị cao nhất**:
Tích hợp **Phase 5 (`LinearDecayFormEvaluator`)** và **Phase 7 (`WeightedScorer.predictOutcome`)** vào tầng Domain để thay thế toàn bộ code prototype/hardcode tạm thời trong `PredictionResult` và `SeasonRanking`.

### Mục tiêu Phase D1:
1. Tạo `CalculateTeamFormUseCase` sử dụng `LinearDecayFormEvaluator.evaluate(outcomes, windowSize)`.
2. Tạo `PredictMatchOutcomeUseCase` trong `:core:domain`:
   - Lấy dữ liệu trận đấu, tỷ lệ kèo odds, phong độ 2 đội.
   - Chuyển đổi dữ liệu thành danh sách `List<Signal3Way>`.
   - Gọi `WeightedScorer.predictOutcome(signals)` để tính toán phân phối xác suất `OutcomeProbabilities`.
   - Chuyển đổi `OutcomeProbabilities` thành `PredictionResult` cho Presentation layer.
3. Xóa bỏ logic tính toán hardcode prototype `computeWeightedScoring` trong `PredictionResult`.
4. Viết Domain Unit Tests độc lập với mock repository.

---

## 8. Đề xuất Domain Contracts cho Phase D1 (Proposed Contracts)

### 8.1. Use Cases Mới & Chỉnh Sửa:

```kotlin
// [PROPOSED] dev.anhquocs.truelab.core.domain.usecase.form.CalculateTeamFormUseCase
class CalculateTeamFormUseCase(
    private val matchRepository: MatchRepository,
    private val formEvaluator: FormEvaluator = LinearDecayFormEvaluator()
) {
    /**
     * Lấy các trận gần nhất của đội bóng và đánh giá phong độ thi đấu.
     * @return Result chứa FormScore (score [0.0, 100.0], wins, draws, losses, totalPoints).
     */
    suspend operator fun invoke(teamId: String, windowSize: Int = 5): Result<FormScore>
}
```

```kotlin
// [PROPOSED] dev.anhquocs.truelab.core.domain.usecase.prediction.PredictMatchOutcomeUseCase
class PredictMatchOutcomeUseCase(
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository,
    private val oddsRepository: OddsRepository,
    private val calculateTeamFormUseCase: CalculateTeamFormUseCase,
    private val weightedScorer: WeightedScorer = DefaultWeightedScorer()
) {
    /**
     * Tổng hợp các tín hiệu xác suất (Odds, Form, Elo, H2H) thành List<Signal3Way>,
     * sau đó gọi WeightedScorer.predictOutcome() để dự đoán kết quả 3 chiều.
     */
    suspend operator fun invoke(
        matchId: String,
        config: PredictionWeightConfig = PredictionWeightConfig.DEFAULT
    ): Result<PredictionResult>
}
```

### 8.2. Cấu hình Trọng số Domain (Domain Configuration):

```kotlin
// [PROPOSED] dev.anhquocs.truelab.core.domain.model.prediction.PredictionWeightConfig
data class PredictionWeightConfig(
    val oddsWeight: Double = 0.35,
    val formWeight: Double = 0.25,
    val eloWeight: Double = 0.20,
    val h2hWeight: Double = 0.10,
    val homeAdvantageWeight: Double = 0.10
) {
    init {
        require(oddsWeight >= 0.0 && formWeight >= 0.0 && eloWeight >= 0.0 && 
                h2hWeight >= 0.0 && homeAdvantageWeight >= 0.0) {
            "Trọng số không được âm"
        }
        val total = oddsWeight + formWeight + eloWeight + h2hWeight + homeAdvantageWeight
        require(total > 0.0) { "Tổng trọng số phải lớn hơn 0" }
    }

    companion object {
        val DEFAULT = PredictionWeightConfig()
    }
}
```

---

## 9. Sơ đồ Luồng Dữ liệu (Data Flow - Phase D1)

```text
[UI / ViewModel]
      │
      ▼ calls
[PredictMatchOutcomeUseCase]
      ├── 1. Lấy MatchDetail, TeamDetail(Home, Away), MatchOdds từ Repositories
      ├── 2. Gọi CalculateTeamFormUseCase -> Lấy 5 trận gần nhất -> LinearDecayFormEvaluator.evaluate()
      ├── 3. Chuyển đổi Odds Implied Probability -> Signal3Way(homeProb, drawProb, awayProb, weight = config.oddsWeight)
      ├── 4. Chuyển đổi Form comparison -> Signal3Way(homeProb, drawProb, awayProb, weight = config.formWeight)
      ├── 5. Chuyển đổi Elo difference -> Signal3Way(homeProb, drawProb, awayProb, weight = config.eloWeight)
      ├── 6. Chuyển đổi H2H / HomeAdvantage -> Signal3Way(...)
      ├── 7. Tập hợp List<Signal3Way>
      ▼
[WeightedScorer.predictOutcome(signals)]
      │ (Linear Mixture: P_c = Σ(w_k * p_k,c) / Σw_k)
      ▼
[OutcomeProbabilities (homeWinProb, drawProb, awayWinProb, predictedOutcome, confidenceScore)]
      │
      ▼ Map sang
[PredictionResult (Domain Entity)]
      │
      ▼ return
[UI / Presentation]
```

---

## 10. Chiến lược Kiểm thử (Test Strategy)

1. **Domain Unit Tests (`:core:domain`)**:
   - `CalculateTeamFormUseCaseTest`: Kiểm thử tính điểm phong độ với danh sách trận đấu W-W-D-L-W, kiểm tra mapping `Match` sang `MatchOutcome` và kiểm tra kết quả `FormScore`.
   - `PredictMatchOutcomeUseCaseTest`: Kiểm thử việc chuyển đổi dữ liệu bóng đá thành `List<Signal3Way>`, kiểm thử gọi `WeightedScorer.predictOutcome()` và nhận `PredictionResult` hợp lệ với 3 xác suất có tổng xấp xỉ $1.0$.
   - `PredictionWeightConfigTest`: Kiểm tra validation trọng số âm hoặc tổng trọng số bằng 0.
2. **Deterministic Test Data**:
   - Khởi tạo mock repository trả về dữ liệu cố định (deterministic fixtures) để xác nhận kết quả dự đoán khớp chính xác với công thức tuyến tính.
3. **Regression Safety**:
   - **BẮT BUỘC**: Toàn bộ 122/122 unit tests của `:core:algorithm` tiếp tục **PASS 100%**.
   - Tuyệt đối không sửa mã nguồn hay test trong `:core:algorithm`.

---

## 11. Ràng buộc Kiến trúc (Architecture Constraints)

1. **`:core:algorithm` Bất biến**: Tiếp tục giữ nguyên trạng thái Pure Kotlin/JVM, không biết bất kỳ class nào trong `:core:domain`, `:core:data`, Room, Retrofit, Android SDK.
2. **Không mang Logic bóng đá vào Algorithm**: Trọng số bóng đá (`PredictionWeightConfig`) và việc chuyển đổi tỷ số/thống kê thành `Signal3Way` hoàn toàn thuộc về `:core:domain`.
3. **Phân định rõ ràng giữa `WeightedFeature` và `Signal3Way`**:
   - `WeightedFeature` chỉ dùng cho bài toán 1 chiều: `calculateScore(features: List<WeightedFeature>): Double`.
   - `Signal3Way` chỉ dùng cho bài toán dự đoán 3 chiều: `predictOutcome(signals: List<Signal3Way>): OutcomeProbabilities`.
4. **Không rò rỉ Persistence vào Domain**: Domain chỉ nhận data thông qua Repository interfaces, không biết Room Entity hay DAO.

---

## 12. Ngoài Phạm vi (Out of Scope)

- ❌ **Machine Learning**: Không triển khai Logistic Regression, Decision Tree hay bất kỳ mô hình ML nào.
- ❌ **Thuật toán mới**: Không thêm bất kỳ thuật toán nào ngoài Phase 1–7 đã hoàn thành.
- ❌ **Sửa đổi API Phase 1–7**: Giữ nguyên toàn bộ interface và implementation của `:core:algorithm`.
- ❌ **Softmax / Temperature**: Phase 7 không sử dụng Softmax hay Temperature, không đưa các khái niệm này vào Domain.
- ❌ **Refactor UI**: Không thay đổi UI/Theme của `:core:ui` hay viết lại toàn bộ giao diện màn hình.

---

## 13. Tiêu chí Chấp nhận (Acceptance Criteria cho Phase D1)

1. `CalculateTeamFormUseCase` và `PredictMatchOutcomeUseCase` hoạt động độc lập trong `:core:domain`.
2. Không còn logic hardcode/tính nhẩm trong companion object của `PredictionResult` hay `SeasonRanking`.
3. Mọi phép tính phong độ và dự đoán đều chạy qua `LinearDecayFormEvaluator.evaluate()` và `WeightedScorer.predictOutcome()`.
4. 100% Domain Unit Tests mới viết đều PASS.
5. 122/122 Algorithm Tests tiếp tục PASS.
6. Build toàn bộ project thành công không lỗi biên dịch (`./gradlew assembleDebug`).

---

## 14. Quyết định Mở về Chuyển đổi Tín hiệu Nghiệp vụ (Open Business Decisions)

Trong tầng Domain, việc chuyển đổi từ dữ liệu bóng đá sang `Signal3Way` (gồm `homeProb`, `drawProb`, `awayProb`) cần thống nhất công thức nghiệp vụ cụ thể:

1. **Odds Signal** (Đã có sẵn):
   - `OddsRecordItem.calculateImpliedProbability()` đã có sẵn trong Domain để tính $(P_H, P_D, P_A)$ ngụ ý từ odds 1X2 sau khi loại bỏ margin nhà cái.
2. **Elo Signal** (Cần quyết định công thức):
   - `EloRatingCalculator.expectedScore(eloHome, eloAway)` trả về kỳ vọng $E_H \in [0.0, 1.0]$ và $E_A = 1.0 - E_H$.
   - *Cần xác định*: Cách phân bổ xác suất Hòa $P_D$ từ chênh lệch Elo (ví dụ: mô hình gán $P_D = 0.25$ và chia phần còn lại theo tỉ lệ $E_H / E_A$, hoặc mô hình hàm khoảng cách).
3. **Form Signal** (Cần quyết định công thức):
   - `LinearDecayFormEvaluator.evaluate()` trả về `FormScore.score` $\in [0.0, 100.0]$ cho từng đội ($F_H, F_A$).
   - *Cần xác định*: Cách chuẩn hóa $(F_H, F_A)$ thành bộ ba $(P_H, P_D, P_A)$.
4. **H2H / Home Advantage Signal**:
   - *Cần xác định*: Cách tính xác suất 3 chiều từ số trận thắng đối đầu trong quá khứ hoặc việc gán baseline xác suất sân nhà.

*(Các công thức trên là nghiệp vụ thuần túy thuộc Domain, sẽ được định nghĩa rõ ràng khi bước vào implementation Phase D1 mà không làm ảnh hưởng đến `:core:algorithm`)*.

---

## 15. Thứ tự Triển khai Đề xuất (Suggested Implementation Order)

1. **Bước 1**: Bổ sung method `getRecentMatches(teamId: String, limit: Int): Flow<List<Match>>` vào `MatchRepository` interface trong `:core:domain` và implement trong `:core:data`.
2. **Bước 2**: Implement `CalculateTeamFormUseCase` tích hợp Phase 5 `LinearDecayFormEvaluator`.
3. **Bước 3**: Tạo `PredictionWeightConfig` và các mapper chuyển đổi Domain Data thành `Signal3Way`.
4. **Bước 4**: Implement `PredictMatchOutcomeUseCase` tích hợp Phase 7 `WeightedScorer.predictOutcome(signals)`.
5. **Bước 5**: Dọn dẹp code prototype trong `PredictionResult` và `SeasonRanking`.
6. **Bước 6**: Viết bộ Unit Test toàn diện cho các UseCase trong `:core:domain/src/test`.
7. **Bước 7**: Xác thực toàn bộ test suite (`:core:algorithm` + `:core:domain`) và chạy `./gradlew assembleDebug`.
