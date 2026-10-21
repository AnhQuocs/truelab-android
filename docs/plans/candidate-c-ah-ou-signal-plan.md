# Candidate C — Kế Hoạch Triển Khai Tín Hiệu Gia Tăng Asian Handicap & Over/Under (AH/O-U Incremental Signal Plan)

> **Trạng thái:** DRAFT — Kế hoạch Kỹ thuật & Thực nghiệm (Implementation Plan Only)  
> **Mục tiêu:** Xây dựng lộ trình kỹ thuật cô lập để đánh giá thực nghiệm: *Tỷ lệ cược Asian Handicap (AH) và Over/Under (O/U) tiền trận có bổ sung giá trị dự đoán gia tăng (Incremental Predictive Value) ngoài 6 tín hiệu Baseline của TrueLab hay không?*  
> **Ràng buộc tuyệt đối:** **CHỈ LẬP KẾ HOẠCH.** Tuyệt đối không thay đổi mã nguồn, không sửa trọng số Production, không thay đổi Candidate A (mặc định hiện tại), không mở lại Candidate B (đã Rejected/Closed), không can thiệp Live/MQTT.

---

## 1. Bối Cảnh & Vị Trí Của Candidate C Trong TrueLab

TrueLab hiện tại vận hành mô hình dự đoán kết quả bóng đá 1X2 dựa trên **Weighted Scoring Pipeline 6 Tín Hiệu (Baseline Pipeline)**:

$$\begin{aligned}
\text{Form (Phong độ)} &: 0.25 \\
\text{Elo (Xếp hạng Elo)} &: 0.20 \\
\text{Goals (Hiệu suất bàn thắng kỳ vọng)} &: 0.15 \\
\text{Odds 1X2 (Tỷ lệ cược Châu Âu)} &: 0.20 \\
\text{H2H (Lịch sử đối đầu)} &: 0.10 \\
\text{Rest Advantage (Ưu thế ngày nghỉ)} &: 0.10 \\
\hline
\mathbf{\sum \text{Weights}} &: \mathbf{1.00}
\end{aligned}$$

Sau khi vector xác suất thô $P = (P_H, P_D, P_A)$ được tính toán bởi `WeightedScorer`, nhãn dự đoán cuối cùng được quyết định bởi **Candidate A (Decision Margin Policy - Mặc định Production)**:

$$\text{Decision} = \begin{cases} 
\text{DRAW} & \text{nếu } |P_H - P_A| < 0.04 \ \text{VÀ} \ P_D \ge 0.255 \\
\operatorname{argmax}(P_H, P_D, P_A) & \text{ngược lại}
\end{cases}$$

### Phân Định Bản Chất Kỹ Thuật: Candidate C vs Candidate A/B
Cần phân định dứt khoát bản chất giữa các Candidate trong hệ thống:
- **Candidate A & Candidate B** là các **Post-Mixture Decision Policies** (Quy tắc cắt ngưỡng/phân ranh nhãn Hòa ở tầng đầu ra sau khi đã tổng hợp xác suất 1X2).
- **Candidate C** là **Odds-Derived Additional Information Signals** (Tín hiệu bổ sung ở tầng Feature Engineering / Data Transformation từ các thị trường cược phụ AH & O/U).

```text
[Data Feeds]               [Signal Transformers]                  [Mixture]                [Decision Policy]
Form Data         ───────> FormSignalTransformer (0.25) ──┐
Elo Data          ───────> EloSignalTransformer  (0.20) ──┤
Goals Data        ───────> GoalsSignalTransformer(0.15) ──┼──> WeightedScorer ──> (P_H, P_D, P_A) ──> DrawDecisionPolicy
Odds 1X2 (EU)     ───────> OddsSignalTransformer (0.20) ──┤                                            ├── Candidate A (Active)
H2H Data          ───────> H2hSignalTransformer  (0.10) ──┤                                            └── Candidate B (Closed)
Rest Advantage    ───────> RestAdvTransformer    (0.10) ──┘
========================================================================================================
Candidate C (AH)  ───────> AsianHandicapTransformer [EXPERIMENTAL]
Candidate C (O/U) ───────> OverUnderTransformer     [EXPERIMENTAL]
```

---

## 2. Mục Tiêu Triển Khai

1. **Thiết lập Data Adapter & Transformers chuẩn hóa** cho hai loại thị trường cược:
   - Asian Handicap (`oddsType = 'asia'`)
   - Over / Under Total Goals (`oddsType = 'bs'`)
2. **Bảo đảm toàn vẹn Point-in-Time & Zero Data Leakage**: Enforce tuyệt đối `changeTime < kickoffEpoch` và loại bỏ hoàn toàn các phase live (`rolling_ball`, `in_play`).
3. **Xây dựng Experimental Evaluation Path**: Cho phép kiểm thử hiệu năng độc lập của Candidate C trên tập dữ liệu cố định 791 trận mà không gây ảnh hưởng đến luồng Production.
4. **Trả lời các câu hỏi thực nghiệm cốt lõi**:
   - AH có cung cấp thông tin tương quan chênh lệch sức mạnh tốt hơn/bổ trợ cho EU Odds không?
   - O/U có giúp cải thiện độ chính xác phân loại trận Hòa thông qua tổng bàn thắng kỳ vọng không?
   - Cặp tín hiệu kết hợp (AH + O/U) có tạo ra Incremental Value vượt trội so với Baseline 6 tín hiệu không?

---

## 3. Upstream Data Contract & Normalization Specification (VERIFIED FACTS)

Căn cứ theo hợp đồng dữ liệu tại [`docs/research/odds-normalization-data-contract.md`](../research/odds-normalization-data-contract.md):

### 3.1. Asian Handicap (`oddsType = 'asia'`)
- **Handicap Line ($H_{\text{line}}$):** Thể hiện số bàn chấp theo góc nhìn của đội chủ nhà (Home Perspective).
  - $H_{\text{line}} > 0$: Home chấp Away (ví dụ: $0.5, 0.75, 1.0$).
  - $H_{\text{line}} < 0$: Away chấp Home (ví dụ: $-0.5, -0.75, -1.0$).
  - $H_{\text{line}} = 0.0$: Kèo đồng banh (Level ball).
- **Price Format:** Định dạng **Hong Kong Odds (HK Odds)** — Lợi nhuận ròng trên 1 đơn vị vốn.

### 3.2. Over / Under (`oddsType = 'bs'`)
- **Handicap Line ($G_{\text{line}}$):** Thể hiện mốc tổng bàn thắng cả trận (Full Match Total Goals).
  - $G_{\text{line}} > 0$: Bội số của $0.25$ (ví dụ: $2.25, 2.50, 2.75, 3.00$).
- **Price Format:** Định dạng **Hong Kong Odds (HK Odds)** cho hai cửa Tài (`over`) và Xỉu (`under`).

### 3.3. Quy Tắc Chuẩn Hóa Giá Cược Bắt Buộc (Verified Normalization Contract)
> **TUYỆT ĐỐI CẤM:** Không dùng $1 / \text{Odds}_{\text{HK}}$ trực tiếp vì sẽ gây sai lệch xác suất trầm trọng.

**Thuật toán chuẩn hóa 3 bước:**
1. Chuyển sang Decimal Odds:
   $$\text{Decimal}_{\text{side}} = \text{HK}_{\text{side}} + 1.0$$
2. Tính Implied Probability thô:
   $$P_{\text{raw}}(\text{Side}_1) = \frac{1}{\text{Decimal}_1}, \quad P_{\text{raw}}(\text{Side}_2) = \frac{1}{\text{Decimal}_2}$$
3. Khử Margin nhà cung cấp (Fair Probability Normalization):
   $$P_{\text{fair}}(\text{Side}_1) = \frac{P_{\text{raw}}(\text{Side}_1)}{P_{\text{raw}}(\text{Side}_1) + P_{\text{raw}}(\text{Side}_2)}, \quad P_{\text{fair}}(\text{Side}_2) = 1.0 - P_{\text{fair}}(\text{Side}_1)$$

### 3.4. Multi-Line Identity & Main Line Selection
- Composite Key: $\langle \text{matchId}, \text{companyId}, \text{oddsType}, \text{marketPhase}, \text{handicap}, \text{changeTime} \rangle$.
- **Main Line Selection Policy (Kèo chính):** Chọn line có $|\text{price}_1 - \text{price}_2|$ nhỏ nhất (gần mức cân tiền $50/50$ nhất).

---

## 4. Frozen Cohort & Phân Nhóm Dữ Liệu Thực Nghiệm

Nghiên cứu Candidate C sử dụng tập dữ liệu cố định **Frozen 791 Cohort** (khung thời gian từ `2026-09-12` đến `2026-10-06`):

```text
┌──────────────────────────────────────────────────────────────┐
│                    Full Frozen 791 Cohort                    │
│                                                              │
│   ┌────────────────────────┬─────────────────────────────┐   │
│   │    AH-Covered Subset   │      O/U-Covered Subset     │   │
│   │     (645 / 791 matches │      (553 / 791 matches     │   │
│   │         - 81.54%)      │          - 69.91%)          │   │
│   │                        │                             │   │
│   │             ┌──────────┴───────────┐                 │   │
│   │             │  Both-Covered Subset │                 │   │
│   │             │   (497 / 791 matches │                 │   │
│   │             │       - 62.83%)      │                 │   │
│   │             └──────────┬───────────┘                 │   │
│   └────────────────────────┴─────────────────────────────┘   │
│                                                              │
│   Missing AH only: 146 matches (18.46%)                      │
│   Missing O/U only: 238 matches (30.09%)                     │
│   Missing Both AH & O/U: 90 matches (11.38%)                 │
└──────────────────────────────────────────────────────────────┘
```

### Xử Lý Missing Market Trong Thực Nghiệm
- **Không loại bỏ âm thầm (No Silent Drop):** Tất cả 791 trận đều phải được đưa vào báo cáo tổng hợp.
- **Tách bạch 4 Không Gian Đánh Giá:**
  1. `Full 791 Cohort`: Toàn bộ tập dữ liệu (áp dụng graceful fallback về baseline nếu thiếu market).
  2. `AH-Covered Subset` ($N = 645$): Đo lường hiệu năng riêng của tín hiệu AH.
  3. `O/U-Covered Subset` ($N = 553$): Đo lường hiệu năng riêng của tín hiệu O/U.
  4. `Both-Covered Subset` ($N = 497$): Đo lường hiệu năng kết hợp khi có đầy đủ dữ liệu thị trường phụ.

---

## 5. Frozen Scope & Candidate Transformations (Research Hypotheses)

Dựa trên kết luận từ feasibility và data contract audit, phạm vi tín hiệu được xác định như sau:

```text
Candidate C Scope Matrix:
├── AH Implied Spread Signal (Latest Pre-match)    --> [IN SCOPE / APPROVED HYPOTHESIS]
├── O/U Goal Expectancy Signal (Latest Pre-match)  --> [IN SCOPE / APPROVED HYPOTHESIS]
├── Combined AH + O/U Joint Signal                 --> [IN SCOPE / APPROVED HYPOTHESIS]
├── Smart Money / Market Movement (Opening->Latest)--> [DEFERRED - Low initial coverage ~28%]
├── Live / In-Play / Rolling Ball Odds             --> [REJECTED / OUT OF SCOPE]
└── Direct 1/HK Probability Estimation             --> [REJECTED / MATHEMATICALLY INVALID]
```

> **LƯU Ý QUAN TRỌNG VỀ TÍNH CHẤT KHOA HỌC:**  
> Trong khi **bước chuẩn hóa Hong Kong Odds $\to$ Fair Probabilities (Mục 3.3)** là sự thật kỹ thuật đã được kiểm toán xác thực (Verified Technical Fact), thì **các công thức chuyển đổi từ Fair Probabilities sang Signal3Way dưới đây là các giả thuyết nghiên cứu (Research Hypotheses / Candidate Transformations)**. Các tham số đi kèm là các giá trị khởi tạo ứng viên (Heuristic Candidate Values) hoặc tham số cần hiệu chỉnh (Calibration Parameters), **tuyệt đối không được xem là định lý đã được chứng minh**.

---

### 5.1. Signal C1 — Asian Handicap Implied Spread Signal (`Signal3Way`)

#### Luồng xử lý dữ liệu:
```text
Raw AH Records 
      ↓ [Filter: changeTime < kickoff, phase not live, price > 0]
Valid Pre-match Snapshots
      ↓ [Select: Latest Pre-match changeTime, Main Line min |price1 - price2|]
Selected Main Line: (H_line, HK_home, HK_away)
      ↓ [Normalize: Decimal = HK + 1.0, Fair Probabilities] (FACT)
(P_fair_home, P_fair_away)
      ↓ [Transform: Spread calculation & Logistic 3-Way Mapping] (HYPOTHESIS)
Signal3Way(homeProb, drawProb, awayProb, weight = w_c1)
```

#### Công thức toán học ứng viên (Candidate Transformation Hypothesis):
1. **Độ lệch ưu thế thị trường ứng viên (Candidate Market Implied Superiority $\Delta_{\text{AH}}$):**
   $$\Delta_{\text{AH}} = H_{\text{line}} + \alpha \cdot \left(P_{\text{fair}}(\text{AH\_Home}) - 0.5\right)$$
2. **Phân phối 3 chiều ứng viên ($P_H, P_D, P_A$):**
   Sử dụng hàm Logistic để ánh xạ chênh lệch bàn chấp sang xác suất Thắng/Thua, kết hợp xác suất Hòa cơ sở $P_0(D)$:
   $$P_{\text{raw}}(H) = \frac{1.0 - P_0(D)}{1.0 + e^{-\beta \cdot \Delta_{\text{AH}}}}$$
   $$P_{\text{raw}}(A) = (1.0 - P_0(D)) - P_{\text{raw}}(H)$$
   $$P_{\text{raw}}(D) = P_0(D) \cdot \exp\left(-\gamma \cdot \Delta_{\text{AH}}^2\right)$$
3. **Chuẩn hóa tổng xác suất = 1.0:**
   $$P(H) = \frac{P_{\text{raw}}(H)}{\sum P_{\text{raw}}}, \quad P(D) = \frac{P_{\text{raw}}(D)}{\sum P_{\text{raw}}}, \quad P(A) = \frac{P_{\text{raw}}(A)}{\sum P_{\text{raw}}}$$

#### Bảng truy nguyên nguồn gốc tham số C1:
| Tham số | Ý nghĩa | Trạng thái nguồn gốc | Giá trị khởi tạo / Ghi chú |
|:---:|---|---|---|
| $P_0(D)$ | Xác suất hòa cơ sở tiên nghiệm | **HEURISTIC / BASELINE CONSTANT** | $0.26$ (Kế thừa từ `PredictionWeightConfig.baselineDrawProb` hiện tại của TrueLab) |
| $\alpha$ | Hệ số độ nhạy giá AH | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $0.50$ (Chưa có empirical proof) |
| $\beta$ | Hệ số độ dốc hàm Logistic | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $1.20$ (Chưa có empirical proof) |
| $\gamma$ | Hệ số suy giảm hòa khi trận lệch kèo | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $0.30$ (Chưa có empirical proof) |
| $w_{\text{c1}}$ | Trọng số tín hiệu AH trong mixture | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $0.10$ (Chưa có empirical proof) |

---

### 5.2. Signal C2 — Over/Under Total Goal Expectancy Signal (`Signal3Way`)

#### Luồng xử lý dữ liệu:
```text
Raw O/U Records 
      ↓ [Filter: changeTime < kickoff, phase not live, price > 0, line > 0]
Valid Pre-match Snapshots
      ↓ [Select: Latest Pre-match changeTime, Main Line min |over - under|]
Selected Main Line: (G_line, HK_over, HK_under)
      ↓ [Normalize: Decimal = HK + 1.0, Fair Probabilities] (FACT)
(P_fair_over, P_fair_under)
      ↓ [Transform: Expected Goals λ_total & Draw Likelihood Modulation] (HYPOTHESIS)
Signal3Way(homeProb, drawProb, awayProb, weight = w_c2)
```

#### Công thức toán học ứng viên (Candidate Transformation Hypothesis):
1. **Tổng bàn thắng kỳ vọng ứng viên ($\lambda_{\text{total}}$):**
   $$\lambda_{\text{total}} = G_{\text{line}} + \eta \cdot \left(P_{\text{fair}}(\text{Over}) - 0.5\right)$$
2. **Điều biến xác suất Hòa ứng viên theo tổng bàn thắng:**
   Giả thuyết nghiên cứu: Trận cầu có mốc bàn thắng thấp ($\lambda < 2.25$) có xác suất Hòa cao hơn, và ngược lại khi $\lambda > 3.0$ xác suất Hòa giảm:
   $$P_{\text{mod}}(D) = \operatorname{clamp}\left(P_0(D) + \kappa \cdot (2.50 - \lambda_{\text{total}}), \ D_{\min}, \ D_{\max}\right)$$
3. **Phân phối phần xác suất còn lại cho Home/Away:**
   $$P(D) = P_{\text{mod}}(D)$$
   $$P(H) = (1.0 - P(D)) \cdot 0.50, \quad P(A) = (1.0 - P(D)) \cdot 0.50$$

#### Bảng truy nguyên nguồn gốc tham số C2:
| Tham số | Ý nghĩa | Trạng thái nguồn gốc | Giá trị khởi tạo / Ghi chú |
|:---:|---|---|---|
| $\eta$ | Hệ số độ nhạy giá Tài/Xỉu | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $0.50$ (Chưa có empirical proof) |
| $\kappa$ | Hệ số nhạy xác suất hòa theo $\lambda_{\text{total}}$ | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $0.08$ (Chưa có empirical proof) |
| $[D_{\min}, D_{\max}]$ | Giới hạn kẹp xác suất hòa | **HEURISTIC / ASSUMPTION** | $[0.16, 0.38]$ (Khoảng chặn an toàn heuristic) |
| $w_{\text{c2}}$ | Trọng số tín hiệu O/U trong mixture | **TBD — requires calibration during experiment** | Khởi tạo heuristic: $0.05$ (Chưa có empirical proof) |

---

## 6. Calibration & Parameter Selection Protocol

### 6.1. Danh mục các tham số cần Calibration
1. **Tín hiệu C1 (AH):** $\alpha$ (độ nhạy giá), $\beta$ (độ dốc logistic), $\gamma$ (suy giảm hòa theo spread).
2. **Tín hiệu C2 (O/U):** $\eta$ (độ nhạy giá tài xỉu), $\kappa$ (hệ số nhạy hòa theo tổng bàn thắng kỳ vọng).
3. **Trọng số thực nghiệm:** $w_{\text{c1}}$ (trọng số tín hiệu AH), $w_{\text{c2}}$ (trọng số tín hiệu O/U) khi hòa trộn cùng 6 tín hiệu Baseline.

### 6.2. Nguyên Tắc Bất Biến Về Dữ Liệu (Data Split & Invariant Rules)
> **NGUYÊN TẮC BẮT BUỘC SỐ 1:**  
> **TUYỆT ĐỐI KHÔNG ĐƯỢC TUNE BẤT KỲ PARAMETER NÀO BẰNG HOLDOUT SET.**

- **Ranh giới phân chia thời gian (Temporal Split):** Nếu thực hiện calibration trên Frozen 791 Cohort, bắt buộc phải chia theo trục thời gian `kickoffTime` (các trận diễn ra sớm cho Calibration Set, các trận muộn cho Holdout Test Set). Tuyệt đối cấm chia ngẫu nhiên (Random K-Fold) để tránh rò rỉ dữ liệu chuỗi thời gian.
- **Quy trình khóa tham số (Freeze Protocol):**
  1. Toàn bộ tham số $(\alpha, \beta, \gamma, \eta, \kappa, w_{\text{c1}}, w_{\text{c2}})$ phải được xác định và đóng băng hoàn toàn trên Calibration Set.
  2. Không được điều chỉnh lại tham số sau khi đã nhìn thấy kết quả trên Holdout Set.
- **Trạng thái quy trình:** `Calibration Protocol: TBD` — Cần được định nghĩa chính thức trong tài liệu nghiên cứu thực nghiệm trước khi tiến hành benchmark.

---

## 7. Chiến Lược Tích Hợp & Cô Lập Thực Nghiệm (Experiment Isolation)

Để bảo đảm an toàn hệ thống và tuân thủ nguyên tắc Clean Architecture:

1. **Production Pipeline Không Thay Đổi (Zero Production Impact):**
   - 6 tín hiệu hiện tại, trọng số hiện tại ($0.25, 0.20, 0.15, 0.20, 0.10, 0.10$), `WeightedScorer`, và Candidate A giữ nguyên $100\%$.
   - Mặc định khởi chạy của ứng dụng không kích hoạt Candidate C.
2. **Experimental Evaluation Orchestrator:**
   - Candidate C được tích hợp qua một usecase/runner thực nghiệm riêng (`CandidateCEvaluationRunner` / `PredictMatchOutcomeUseCase` experimental overload).
   - Cho phép truyền cấu hình `CandidateCConfig(enabled = true, ahWeight = 0.10, ouWeight = 0.05, renormalizeBaseline = true)`.

```text
                               ┌──────────────────────────────────────────────┐
                               │       PredictMatchOutcomeUseCase             │
                               └──────────────────────┬───────────────────────┘
                                                      │
                       ┌──────────────────────────────┴──────────────────────────────┐
                       ▼                                                             ▼
         [Production Default Path]                                     [Candidate C Experimental Path]
         - Form (0.25)                                                 - Baseline 6 Signals (Scaled)
         - Elo (0.20)                                                  - Signal C1: AH (w_ah)
         - Goals (0.15)                                                - Signal C2: O/U (w_ou)
         - Odds EU (0.20)                                              - WeightedScorer (8 Signals)
         - H2H (0.10)                                                  - Draw Policy (Raw / Cand A)
         - RestAdvantage (0.10)                                        
         - Candidate A Decision Margin                                 
```

---

## 8. Architecture & File-Level Plan

### 8.1. Bảng phân loại tệp tin:

| Trạng thái | Đường dẫn tệp tin | Trách nhiệm & Lý do |
|---|---|---|
| **DO NOT MODIFY** | `core/algorithm/.../WeightedScorer.kt` | Interface chấm điểm trọng số thuần túy. Đã hoàn thiện và kiểm thử. |
| **DO NOT MODIFY** | `core/algorithm/.../DefaultWeightedScorer.kt` | Implementation WeightedScorer. Tự động hỗ trợ $N$ signals có tổng trọng số dương. |
| **DO NOT MODIFY** | `core/domain/.../policy/DrawDecisionPolicy.kt` | Chính sách quyết định nhãn Hòa (Candidate A / Candidate B). |
| **DO NOT MODIFY** | `core/domain/.../model/DrawDecisionRule.kt` | Enum phân loại Candidate A, Candidate B, Raw Argmax. |
| **DO NOT MODIFY** | `core/data/.../odds/local/entity/OddsEntity.kt` | Entity Room đã có sẵn đầy đủ fields `asia`, `bs`, `handicap`, `homeWin`, `awayWin`, `over`, `under`. |
| **MODIFY** | `core/domain/.../odds/selector/PreMatchOddsSelector.kt` | Thêm helper `selectPreMatchAsianHandicapMainLine()` và `selectPreMatchOverUnderMainLine()` để chọn Main Line tối ưu và lọc bỏ price $\le 0$. |
| **MODIFY** | `core/domain/.../prediction/model/MatchPredictionContext.kt` | Thêm 2 fields tùy chọn: `asianHandicapOdds: OddsRecordItem? = null`, `overUnderOdds: OddsRecordItem? = null`. |
| **CREATE** | `core/domain/.../prediction/model/CandidateCConfig.kt` | Model chứa tham số cấu hình thực nghiệm cho Candidate C (weights, scaling factors, alpha, beta, kappa). |
| **CREATE** | `core/domain/.../prediction/transformer/AsianHandicapSignalTransformer.kt` | Transformer chuẩn hóa HK Odds AH -> Fair Probabilities -> `Signal3Way`. |
| **CREATE** | `core/domain/.../prediction/transformer/OverUnderSignalTransformer.kt` | Transformer chuẩn hóa HK Odds O/U -> Goal Expectancy -> `Signal3Way`. |
| **CREATE** | `core/domain/.../prediction/usecase/PredictWithCandidateCUseCase.kt` | Experimental Use Case điều phối 8 tín hiệu dự đoán dành riêng cho benchmark và backtest. |
| **CREATE** | `core/domain/.../evaluation/usecase/EvaluateCandidateCHoldoutUseCase.kt` | Use Case chạy benchmark tự động trên Frozen 791 Cohort theo ma trận biến thể. |

---

## 9. Ma Trận Đánh Giá Thực Nghiệm (Evaluation Matrix — 7 Variants)

Để xác định chính xác giá trị của Candidate C mà không gây nhầm lẫn với các cải tiến trước, hệ thống thực nghiệm sẽ chạy **7 biến thể đối chiếu (V0 → V6)**:

| Variant ID | Cấu hình Signal | Decision Policy | Mục tiêu thực nghiệm |
|:---:|---|---|---|
| **V0** | Baseline (6 Signals) | Raw Argmax | Mốc đối chứng gốc tuyệt đối (Ground Zero) |
| **V1** | Baseline (6 Signals) | **Candidate A** | **Mốc chuẩn Production hiện tại** ($\delta=0.04, \theta=0.255$) |
| **V2** | Baseline (6 Signals) | Candidate B (Closed) | Mốc lịch sử đối soát ($\tau=0.120$, đã Rejected) |
| **V3** | Baseline + **Signal C1 (AH)** | Raw Argmax | Đo lường Incremental Value thuần túy của Asian Handicap |
| **V4** | Baseline + **Signal C2 (O/U)** | Raw Argmax | Đo lường Incremental Value thuần túy của Over/Under |
| **V5** | Baseline + **Candidate C (AH + O/U)** | Raw Argmax | Đo lường Incremental Value của toàn bộ Candidate C |
| **V6** | Baseline + **Candidate C (AH + O/U)** | **Candidate A** | **Kịch bản ứng cử viên Production:** Kết hợp thông tin C với policy A |

---

## 10. Bộ Chỉ Số Đánh Giá Chuẩn Hóa (Standard Metrics)

Toàn bộ **7 biến thể** sẽ được đo lường đồng nhất trên các metrics chuẩn từ Phase B:

1. **Phân loại tổng thể:**
   - Overall Accuracy (%)
   - Macro F1-Score (%)
2. **Hiệu năng từng lớp (Class-level Metrics):**
   - Home Win: Precision, Recall, F1-Score
   - Away Win: Precision, Recall, F1-Score
   - **Draw (Trọng tâm):** Precision, Recall, F1-Score, True Positives (TP), False Positives (FP)
3. **Ma trận nhầm lẫn (Confusion Matrix):** $3 \times 3$ ma trận (Actual vs Predicted).
4. **Chỉ số can thiệp quyết định:**
   - Số lượng dự đoán đúng của Home/Away bị đổi sai thành Draw (Degraded Correct H/A).
   - Tỷ lệ kích hoạt và mức đóng góp trọng số thực tế (Effective Weight Contribution).

---

## 11. Kế Hoạch Kiểm Thử Toàn Diện (Test Plan)

### 11.1. Unit Tests (`:core:domain`)
- `AsianHandicapSignalTransformerTest`:
  - Kiểm tra chuyển đổi HK Odds: $0.85 \implies \text{Decimal } 1.85 \implies P_{\text{raw}} \approx 0.5405$.
  - Kiểm tra loại bỏ Margin: $P_{\text{fair\_home}} + P_{\text{fair\_away}} = 1.0$.
  - Kiểm tra kèo đồng banh ($0.0$), chấp nửa trái ($0.5$), chấp 1 trái rưỡi ($1.5$), và kèo âm (Away chấp).
  - Kiểm tra xử lý dữ liệu bất thường: Giá $\le 0.0$, line NaN, hoặc thiếu 1 bên giá $\implies$ trả fallback/weight = 0.
- `OverUnderSignalTransformerTest`:
  - Kiểm tra tính toán $\lambda_{\text{total}}$ từ các line $2.0, 2.5, 3.0$.
  - Kiểm tra điều biến xác suất Hòa: Line thấp $\implies P(D)$ tăng; Line cao $\implies P(D)$ giảm.
- `PreMatchOddsSelectorTest`:
  - Test Point-in-Time: Loại bỏ bản ghi có `changeTime >= kickoffEpoch`.
  - Test Market Phase: Chấp nhận `initial, instant, immediate, ""`, loại bỏ `rolling_ball, in_play`.
  - Test Main Line Selection: Chọn đúng line có $|\text{price}_1 - \text{price}_2|$ nhỏ nhất khi trận đấu có nhiều line.

### 11.2. Integration & Regression Tests
- `CandidateCIntegrationTest`:
  - Kiểm tra khi tắt Candidate C: Pipeline chạy chính xác ra kết quả như Baseline cũ (100% Deterministic).
  - Kiểm tra trên tập trận thiếu AH hoặc thiếu O/U: Pipeline tự động gán trọng số = 0 cho signal thiếu và tái phân bổ trọng số mượt mà (Graceful Degradation).
  - Test không rò rỉ dữ liệu (Zero Temporal Leakage Verification).

---

## 12. Các Giai Đoạn Triển Khai (Implementation Phases)

```text
Phase 1: Domain Models & Contract Specification
  ├── Định nghĩa CandidateCConfig, CandidateCMode
  └── Mở rộng MatchPredictionContext (backward compatible)

Phase 2: PreMatchOddsSelector Enhancement
  ├── Bổ sung Main Line Selector cho AH & O/U
  └── Enforce validation giá dương (> 0.0)

Phase 3: Candidate C Transformers Implementation
  ├── AsianHandicapSignalTransformer
  └── OverUnderSignalTransformer

Phase 4: Experimental Pipeline & Orchestration
  ├── PredictWithCandidateCUseCase
  └── EvaluateCandidateCHoldoutUseCase

Phase 5: Unit & Integration Testing Suite
  ├── 100% Unit Test coverage cho Transformers và Selectors
  └── Regression test đảm bảo Baseline & Candidate A không đổi

Phase 6: Frozen 791 Benchmark Execution
  ├── Chạy 7 biến thể V0 -> V6 trên 4 cohort subsets
  └── Xuất báo cáo đánh giá thực nghiệm chi tiết

Phase 7: Review & Final Decision Gate
  └── Hội đồng kỹ thuật đánh giá Acceptance Criteria
```

---

## 13. Tiêu Chí Chấp Nhận & Nghiệm Thu (Acceptance Criteria)

### 13.1. Technical Acceptance (Kỹ thuật & Kiến trúc)
- [ ] Build dự án thành công $100\%$ không lỗi lint, không breaking change.
- [ ] Toàn bộ test suite cũ của Baseline, Candidate A, và Candidate B tiếp tục PASS $100\%$.
- [ ] Tách biệt hoàn toàn luồng Production và luồng Thực nghiệm.
- [ ] $100\%$ tuân thủ Point-in-Time Rule (`changeTime < kickoffEpoch`).

### 13.2. Research Acceptance (Giá trị khoa học & Dự đoán)
Candidate C chỉ được xem xét đưa vào danh sách ứng cử viên cho Production nếu đáp ứng các tiêu chí nghiên cứu được định nghĩa trước:
1. **Incremental Predictive Value:** Biến thể V5 (Baseline + C) hoặc V6 (Baseline + C + A) đạt cải thiện Macro F1 trên tập Holdout so với Baseline V0 và Candidate A V1. *(Mức ngưỡng tối thiểu: `TBD — to be formally pre-registered in experiment protocol before holdout evaluation`).*
2. **Draw Detection Efficacy:** Tăng Draw F1 hoặc Draw Recall mà không làm suy giảm tỷ lệ đánh đổi (FP/TP Trade-off) so với Candidate A.
3. **Home/Away Non-inferiority:** Hiệu năng trên Home Win và Away Win không bị suy giảm vượt quá biên dung sai được đăng ký trước. *(Biên dung sai: `TBD — to be pre-registered in protocol`).*
4. **Zero Holdout Contamination:** Toàn bộ tham số và ngưỡng được đóng băng trước khi đánh giá trên tập Holdout.

---

## 14. Rủi Ro & Phân Loại Câu Hỏi Mở (Risks & Open Questions)

| Vấn đề | Phân loại | Giải pháp / Định hướng |
|---|:---:|---|
| **Độ phủ O/U thấp (69.91%) và AH (81.54%)** | **RESOLVED** | Áp dụng dynamic signal weight fallback (gán weight = 0 khi thiếu market) và báo cáo phân tầng 4 subsets. |
| **Sai lệch do định dạng Hong Kong Odds** | **RESOLVED** | Enforce công thức $\text{Decimal} = \text{HK} + 1.0$ trong toàn bộ Transformers. |
| **Lựa chọn Main Line khi có nhiều handicap lines** | **RESOLVED** | Áp dụng quy tắc Canonical Main Line: Chọn line có chênh lệch giá $|\text{price}_1 - \text{price}_2|$ nhỏ nhất. |
| **Selection Bias do On-demand Fetching** | **OPEN QUESTION** | Cần ghi nhận rõ trong kết quả: Dữ liệu lịch sử có thể phản ánh các trận đấu được người dùng quan tâm nhiều hơn. |
| **Xác định Calibration Split Protocol** | **OPEN QUESTION** | Cần chốt tỷ lệ và mốc thời gian chia tập Calibration / Holdout trước khi chạy thực nghiệm. |
| **Tác động của Smart Money (Movement Signals)** | **DEFERRED** | Hoãn lại do Opening Odds chỉ phủ $\approx 30\%$ dữ liệu. |
| **Tích hợp cược Rung / Live Odds** | **OUT OF SCOPE** | Loại trừ hoàn toàn khỏi phạm vi bài toán dự đoán tiền trận. |

---

## 15. Danh Mục Ngoài Phạm Vi (Explicit Non-Scope)

1. **Tuyệt đối không:** Sửa đổi hoặc điều chỉnh lại tham số của Candidate B ($\tau = 0.120$). Candidate B đã hoàn thành và đóng lại.
2. **Tuyệt đối không:** Thay đổi trọng số mặc định 6 tín hiệu của Production.
3. **Tuyệt đối không:** Thay đổi ngưỡng của Candidate A ($\delta = 0.04, \theta = 0.255$).
4. **Tuyệt đối không:** Tích hợp luồng MQTT hoặc xử lý Live Odds.
5. **Tuyệt đối không:** Tự ý đề xuất hay triển khai Candidate D/E trong phase này.

---

## 16. Thứ Tự Triển Khai Thực Tế

$$\begin{matrix}
\text{Data Contract \& Audit} & \longrightarrow & \text{Implementation Plan (Tài liệu này)} \\
\downarrow & & \downarrow \\
\text{Calibration Protocol Setup} & \longrightarrow & \text{Transformers \& Selectors Implementation} \\
\downarrow & & \downarrow \\
\text{Unit \& Leakage Tests} & \longrightarrow & \text{Experimental Orchestrator} \\
\downarrow & & \downarrow \\
\text{Calibration Tuning (Train Split)} & \longrightarrow & \text{Benchmark trên Frozen Holdout (7 Variants)} \\
\downarrow & & \downarrow \\
\text{So sánh V0 -> V6} & \longrightarrow & \mathbf{Final\ Decision\ Gate}
\end{matrix}$$

---

## 17. Final Checklist Trước Khi Bắt Đầu Triển Khai

- [x] Đã xác định rõ ràng nguồn gốc dữ liệu từ Data Contract Audit.
- [x] Đã phân định rạch ròi bản chất giữa Candidate A/B (Decision Policy) và Candidate C (Feature Signal).
- [x] Đã phân biệt FACT (chuẩn hóa Hong Kong Odds) với RESEARCH HYPOTHESIS (ánh xạ sang Signal3Way).
- [x] Đã truy nguyên trạng thái nguồn gốc rõ ràng cho từng tham số ($\alpha, \beta, \gamma, P_0(D), \eta, \kappa$).
- [x] Đã thiết lập nguyên tắc Calibration bắt buộc: Tuyệt đối không tune bằng Holdout set.
- [x] Đã thiết lập cơ chế cô lập thực nghiệm (Experiment Isolation) bảo vệ tuyệt đối luồng Production.
- [x] Đã xác định chi tiết danh sách tệp tin cần `CREATE`, `MODIFY`, `DO NOT MODIFY`.
- [x] Đã xác lập ma trận **7 biến thể đánh giá (V0 → V6)** và bộ metrics chuẩn.
- [x] Không can thiệp mã nguồn trong bước lập kế hoạch.
