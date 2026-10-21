# Issue: Triển khai thay thế Home Advantage bằng Rest Advantage Signal

## 1. Bối cảnh

Trong kiến trúc Prediction Pipeline hiện tại của TrueLab, tín hiệu thứ 6 là **Home Advantage** với trọng số $10\%$.

Qua các phase audit trước đó ([Home Advantage Semantics Audit](../audits/home-advantage-semantics-audit.md), [Draw Underprediction Audit](../audits/draw-underprediction-audit.md)), hệ thống đã xác định:
- Trường `home_team` từ TrueScore chỉ mang tính chất danh nghĩa **Side 1 của fixture** do ban tổ chức sắp xếp, không đồng nghĩa với lợi thế sân nhà vật lý thực tế.
- Data source không có cờ `is_neutral`, dẫn đến việc mọi trận đấu (kể cả World Cup, Euro, Chung kết Cúp trung lập) đều bị gán cố định một ưu thế tiên nghiệm $+1.8\%$ cho Side 1 ($[0.46, 0.26, 0.28]$).
- Điều này vừa tạo ra sai lệch nghiệp vụ vừa góp phần đè nén xác suất Hòa trong cơ chế quyết định $\text{argmax}$.

Feasibility Study và Mathematical Modeling (Phase R1, R2) đã hoàn thành và chứng minh:
- **Dữ liệu khả thi 100%**: Dữ liệu trận đấu lịch sử gần nhất của cả 2 đội đã có sẵn trên RAM trong `MatchPredictionContext` mà không cần thêm Room query.
- **Contract toán học đã chốt**: Mô hình Hyperbolic Tangent ($\tanh$) với $S = 3.0$ ngày và $\delta_{\max} = 0.09$ đạt tính đối xứng hoàn hảo, đơn điệu, triệt tiêu thiên vị fixture-side và có độ phủ $\ge 90.12\%$.

---

## 2. Vấn đề cần giải quyết (Problem)

Cần triển khai thay thế hoàn toàn `HomeAdvantageSignalTransformer` bằng `RestAdvantageSignalTransformer` trong `PredictMatchOutcomeUseCase` và giao diện người dùng, đồng thời:
1. Đảm bảo production pipeline duy trì đúng **6 signals** với tổng trọng số chuẩn $1.0$.
2. Bảo tồn khả năng chạy lại mô hình cũ (Home Advantage) làm **Baseline Benchmark** để so sánh đối chiếu hiệu năng thực nghiệm.
3. Không làm rò rỉ dữ liệu thời gian (Zero Temporal Data Leakage) và không gây ra hiện tượng $N+1$ query.

---

## 3. Giải pháp Đề xuất (Proposed Solution)

### 3.1. Triển khai `RestAdvantageSignalTransformer`
- **Công thức:**
  $$\text{homeRestDays} = \frac{T_{\text{target}} - T_{\text{home\_prev}}}{86400.0}, \quad \text{awayRestDays} = \frac{T_{\text{target}} - T_{\text{away\_prev}}}{86400.0}$$
  $$\Delta \text{Rest} = \text{homeRestDays} - \text{awayRestDays}$$
  $$\begin{cases}
  P(\text{Home}) = 0.37 + 0.09 \cdot \tanh\left(\dfrac{\Delta \text{Rest}}{3.0}\right) \\[4pt]
  P(\text{Draw}) = 0.26 \\[4pt]
  P(\text{Away}) = 0.37 - 0.09 \cdot \tanh\left(\dfrac{\Delta \text{Rest}}{3.0}\right)
  \end{cases}$$
- **Fallback an toàn:** Khi thiếu lịch sử của 1 hoặc cả 2 đội $\to$ trả về Neutral Baseline $[0.37, 0.26, 0.37]$ với trọng số $0.10$ và `isAvailable = false`.

### 3.2. Cấu hình Chế độ Tín hiệu thứ 6 (`SixthSignalMode`)
Bổ sung enum `SixthSignalMode { REST_ADVANTAGE, HOME_ADVANTAGE }` vào `PredictionWeightConfig` với mặc định là `REST_ADVANTAGE` cho production, cho phép chuyển sang `HOME_ADVANTAGE` khi thực thi các test suite benchmark.

### 3.3. Cập nhật Evidence & UI
- Thay thế trường `homeAdvantage` trong `PredictionEvidence` bằng `restAdvantage`.
- Cập nhật `PredictionEvidenceComponents` hiển thị thông tin ngày nghỉ và độ chênh lệch thể lực $\Delta \text{Rest}$.
- Bổ sung bộ chuỗi ngôn ngữ đa quốc gia (`values`, `values-en`, `values-vi`).

---

## 4. Yêu cầu Kỹ thuật (Technical Requirements)

1. **Clean Architecture & Multi-Module Invariants:**
   - `RestAdvantageSignalTransformer` đặt trong `:core:domain` (Pure Kotlin/JVM, không phụ thuộc Android SDK).
   - `PredictionWeightConfig` và `PredictionEvidence` duy trì tính bất biến (Immutable Data Classes).
2. **Zero Temporal Leakage:**
   - Trận đấu trước đó bắt buộc phải thỏa mãn: `isEnded == true && startTimeDate < targetKickoff`.
3. **Hiệu năng Bộ nhớ ($O(1)$ RAM Access):**
   - Trích xuất `homeRecentMatches.firstOrNull()` và `awayRecentMatches.firstOrNull()` đã có sẵn trong `MatchPredictionContext`.
4. **Bảo toàn Xác suất:**
   - $P(H) + P(D) + P(A) = 1.000000$ với mọi trường hợp đầu vào.

---

## 5. Tiêu chí Chấp thuận (Acceptance Criteria)

- [ ] `RestAdvantageSignalTransformer` được tạo mới và implement đầy đủ công thức $\tanh$ với $S = 3.0$ và $\delta_{\max} = 0.09$.
- [ ] `PredictMatchOutcomeUseCase` sử dụng đúng 6 tín hiệu (Odds, Elo, Form, Goals, H2H, Rest Advantage).
- [ ] `PredictionEvidence` chứa `restAdvantage: SignalEvidence` với đầy đủ metadata ngày nghỉ.
- [ ] `MatchPredictionContext` bổ sung trường `matchStartTimeDate: String?`.
- [ ] `PredictionViewModel`, `RunDailyBacktestUseCase`, `BacktestPredictionUseCase` truyền `startTimeDate` vào context.
- [ ] `PredictionEvidenceComponents` và resource strings triple-locale hiển thị thông tin Rest Advantage chuẩn Material 3.
- [ ] Unit Test Suite `RestAdvantageSignalTransformerTest` đạt PASS 100% trên các kịch bản: $\Delta \text{Rest} = 0$, $\pm 1\text{d}$, $\pm 3\text{d}$, $\pm 7\text{d}$, extreme saturation, missing history, symmetry, monotonicity.
- [ ] `PredictMatchOutcomeUseCaseTest` được cập nhật và pass 100%.
- [ ] Hỗ trợ chế độ `SixthSignalMode.HOME_ADVANTAGE` phục vụ benchmark đối soát.
- [ ] Không có thay đổi logic đối với 5 signals còn lại hoặc Draw baseline ($0.26$).
- [ ] Toàn bộ test suite `:core:domain:test` và `:app:test` chạy thành công không có regression.

---

## 6. Yêu cầu Benchmark (Benchmark Requirement)

Sau khi hoàn thành implementation, hệ thống phải sẵn sàng để thực hiện benchmark đối chiếu:
- **Mục tiêu:** Đo lường tác động của Rest Advantage so với Home Advantage trên tập $N$ trận đấu thực tế của Daily Backtest.
- **Chỉ số theo dõi:** Accuracy, Confusion Matrix, Home Recall, Draw Recall, Away Recall, và tỷ lệ phân bổ xác suất.

---

## 7. Ràng buộc Ngoài phạm vi (Non-Scope)

- Không thay đổi trọng số 5 tín hiệu: Form (25%), Elo (20%), Odds (20%), Goals (15%), H2H (10%).
- Không sửa đổi cấu trúc bảng SQLite hay Room Migration.
- Không thay đổi thuật toán `WeightedScorer`.
