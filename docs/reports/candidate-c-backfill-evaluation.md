# Báo Cáo Thực Nghiệm: Targeted Odds Backfill & Đánh Giá Candidate C Trên Cohort N = 1.716 Trận

> **Thời điểm thực hiện:** 17/10/2026  
> **Tài liệu tham chiếu:** [`docs/plans/odds-backfill-plan.md`](../plans/odds-backfill-plan.md), [`docs/audits/pilot-odds-backfill-report.md`](../audits/pilot-odds-backfill-report.md), [`docs/research/candidate-c-experimental-protocol.md`](../research/candidate-c-experimental-protocol.md)  
> **Quyết định:** **REJECT AS PRODUCTION DEFAULT / CONFIRMED AS WIN-LOSS ACCURACY FEATURE**  
> *(Candidate A tiếp tục là mặc định duy nhất trong Production; không đưa Candidate C vào production pipeline)*

---

## 1. Tổng Quan Triển Khai & Kiểm Toán Dữ Liệu Backfill

Đợt **Targeted Odds Backfill** đã được thực thi hoàn tất trên toàn bộ cohort các giải đấu chuyên nghiệp hàng đầu từ tháng 03/2026 đến tháng 09/2026 bằng endpoint `/odds-history` của Bet365 (`company_id = 2`). Toàn bộ dữ liệu được lưu trữ cô lập tại `scratch/targeted_odds_1849.db`, **tuyệt đối không ghi đè hay can thiệp vào Room DB chính**.

### 1.1. Thống Kê Thu Thập & Độ Phủ Dữ Liệu
- **Tổng số trận mục tiêu:** **1.849 trận** (100% kế hoạch).
- **Số trận xử lý thành công:** **1.849 / 1.849 trận** (100%).
- **Số trận có Odds Bet365:** **1.732 trận** (**93,67%** độ phủ).
- **Số trận có đầy đủ bộ 3 kèo Pre-match (EU + Asian Handicap + Over/Under):** **1.716 trận** (**92,81%** độ phủ).
- **Tổng số bản ghi Odds hợp lệ đã nạp:** **224.148 dòng**.
- **Tổng dung lượng tải về:** **612,7 MB** trong **32,0 phút** (tốc độ trung bình $0,96 - 1,20\text{ req/s}$).
- **Circuit breaker & Lỗi mạng:** 0 lần chạm circuit breaker, 0 lỗi HTTP 429/403, 0 lỗi 5xx liên tiếp.

### 1.2. Phân Bổ Độ Phủ Theo Giải Đấu
| Giải đấu | Số trận mục tiêu | Có Odds | Pre-match EU | Pre-match AH | Pre-match O/U | Tỷ lệ hợp lệ |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **United States MLS** | 384 | 369 | 368 | 368 | 368 | 95,83% |
| **Brazilian Serie A** | 240 | 229 | 229 | 229 | 229 | 95,42% |
| **Spanish Segunda Division** | 239 | 226 | 225 | 225 | 225 | 94,14% |
| **EFL Championship** | 224 | 206 | 200 | 201 | 201 | 89,29% |
| **Spanish La Liga** | 189 | 171 | 169 | 169 | 170 | 89,42% |
| **Italian Serie A** | 160 | 149 | 147 | 147 | 149 | 91,88% |
| **English Premier League** | 150 | 139 | 139 | 139 | 139 | 92,67% |
| **French Ligue 1** | 137 | 127 | 127 | 127 | 127 | 92,70% |
| **Bundesliga** | 126 | 116 | 112 | 113 | 112 | 88,89% |
| **Tổng cộng** | **1.849** | **1.732** | **1.716** | **1.718** | **1.720** | **92,81%** |

### 1.3. Kiểm Toán Point-in-Time (Chống Data Leakage)
- **Số bản ghi vi phạm rò rỉ ($T_{\text{change}} > T_{\text{kickoff}}$):** **0 bản ghi** ($0,00\%$).
- **Số bản ghi vi phạm live (`rolling_ball`):** **0 bản ghi** ($0,00\%$).
- **Độ trễ thời gian đóng kèo so với Kickoff ($\Delta_{\text{closing}}$):**
  - Cận trên ($\max$): $0\text{ giây}$ (sát giờ bóng lăn).
  - Giá trị trung bình: $-17.995\text{ giây}$ ($\approx -5,0\text{ giờ}$ trước trận).

---

## 2. Kết Quả Thực Nghiệm Candidate C Trên Cohort Hợp Lệ ($N = 1.716$)

Đánh giá được thực hiện trên tập mẫu hợp lệ $N = 1.716$ trận có đầy đủ cả 6 tín hiệu Baseline và Odds Closing Pre-match (EU, AH, O/U) thông qua full pipeline tái tạo chuẩn xác $100\%$ mã nguồn Kotlin trong `:core:domain` và `:core:algorithm`.

### 2.1. Bảng Tổng Hợp Hiệu Năng Các Biến Thể

| Biến thể | Cấu hình & Chính sách | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw P/R | Draw TP/FP | Brier Score |
|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **V0** | Baseline (6 Signals) + Raw Argmax | 47,14% | 35,31% | 59,17% | 0,00% | 46,78% | 0,0%/0,0% | 0 / 0 | 0.6285 |
| **V1** | Baseline + **Candidate A** (Active Prod) | 46,85% | **42,21%** | 58,56% | **23,53%** | 44,55% | **31,1%/18,9%** | **84 / 186** | 0.6285 |
| **V3** | Baseline + **Asian Handicap (AH)** + Raw | **48,25%** | 35,78% | **61,07%** | 0,00% | 46,28% | 0,0%/0,0% | 0 / 0 | **0.6194** |
| **V4** | Baseline + **Over/Under (O/U)** + Raw | 47,14% | 35,31% | 59,17% | 0,00% | 46,78% | 0,0%/0,0% | 0 / 0 | 0.6290 |
| **V5** | Baseline + **Candidate C (AH+OU)** + Raw | **48,25%** | 35,78% | **61,07%** | 0,00% | 46,28% | 0,0%/0,0% | 0 / 0 | 0.6198 |
| **V6** | Baseline + **Candidate C** + **Candidate A** | 47,03% | 41,05% | 59,81% | 19,33% | 44,01% | 27,6%/14,9% | 66 / 173 | 0.6198 |

*Ghi chú ca can thiệp sai H/A (Degraded Correct H/A): V1 làm sai 89 trận; V6 làm sai 87 trận (chênh lệch: $-2$ trận).*

---

### 2.2. Kiểm Định Ý Nghĩa Thống Kê (Paired Bootstrap Resampling $B = 10.000$)

| So sánh cặp | Chỉ số | Chênh lệch trung bình ($\Delta$) | Khoảng tin cậy 95% CI | Chứa 0? | Kết luận thống kê |
|---|---|:---:|:---:|:---:|---|
| **V5 - V0**<br>*(Candidate C Raw vs Baseline Raw)* | **Accuracy**<br>**Macro F1** | **+1,11%**<br>+0,47% | **`[+0,06%, +2,16%]`**<br>`[-0,38%, +1,32%]` | **KHÔNG**<br>CÓ | **TĂNG ACCURACY CÓ Ý NGHĨA THỐNG KÊ**<br>Chưa đạt ý nghĩa thống kê về Macro F1 |
| **V6 - V1**<br>*(Candidate C + A vs Candidate A)* | **Accuracy**<br>**Macro F1** | +0,19%<br>**-1,15%** | `[-1,11%, +1,52%]`<br>`[-2,69%, +0,40%]` | CÓ<br>CÓ | Không khác biệt ý nghĩa về Accuracy<br>Suy giảm Macro F1 (do giảm Draw TP) |

---

## 3. Phân Tích Chuyên Sâu & Kết Luận Nghiên Cứu

### 3.1. Giá Trị Cốt Lõi Của Candidate C
1. **Khả năng Phân Cực Thắng / Thua Vượt Trội:**  
   Tín hiệu Asian Handicap (AH) mang lại bước nhảy đáng kể về mặt xác suất thực nghiệm:
   - **Tăng Accuracy tổng thể:** $+1,11\%$ (từ $47,14\%$ lên $48,25\%$, $95\%$ CI: `[+0,06%, +2,16%]`, $0 \notin \text{CI}$).
   - **Tăng Home F1:** $+1,90\%$ (từ $59,17\%$ lên $61,07\%$).
   - **Cải thiện độ chuẩn hóa xác suất (Probability Calibration):** Brier Score giảm rõ rệt từ $0.6285$ xuống $0.6194$.
2. **Vai trò của Over/Under (O/U):**  
   Tín hiệu O/U ($w_{\text{ou}} = 0.03$) có tác động thứ yếu và không đủ mạnh để tạo ra sự khác biệt độc lập ở tầng raw argmax, nhưng đóng góp vào việc làm mịn phân phối xác suất.

### 3.2. Vì Sao Candidate C Thất Bại Trong Vai Trò Draw Detector?
- Khi hoạt động đơn lẻ ở tầng Raw Argmax (V5), Candidate C **không bao giờ dự đoán Draw** (Draw F1 = $0,00\%$).
- Khi kết hợp với chính sách Decision Margin của Candidate A (V6), tín hiệu AH tạo ra sự chênh lệch phân cực $|P_H - P_A|$ lớn hơn, khiến điều kiện $|P_H - P_A| < \delta$ ($0.04$) ít được thỏa mãn hơn.
- Kết quả: Số trận bắt đúng Hòa (Draw TP) **giảm từ 84 xuống 66 trận**, khiến Draw F1 sụt giảm từ $23,53\%$ xuống $19,33\%$ (giảm $-4,20\%$), dẫn đến Macro F1 giảm $-1,15\%$.

---

## 4. Quyết Định Kiến Trúc & Hành Động Tiếp Theo

1. **Giữ nguyên Production Default:**  
   **Candidate A** tiếp tục là chính sách hòa mặc định duy nhất được kích hoạt trên TrueLab. Tuyệt đối **không thay thế Candidate A bằng Candidate C**.
2. **Không commit / push code thực nghiệm:**  
   Dữ liệu SQLite Staging (`scratch/targeted_odds_1849.db`), file checkpoint và các script thực nghiệm được bảo lưu trong thư mục `scratch/` cho mục đích kiểm chứng khoa học, không ghi vào Room DB chính của ứng dụng.
3. **Định hướng nghiên cứu tương lai:**  
   Nếu muốn khai thác sức mạnh của Asian Handicap để tăng Accuracy phân cực Thắng/Thua mà không làm triệt tiêu khả năng phát hiện Hòa của Candidate A, cần nghiên cứu cơ chế tách biệt 2 giai đoạn (Hierarchical Two-Stage Policy): Giai đoạn 1 dùng Candidate A phát hiện Hòa, nếu không Hòa thì Giai đoạn 2 mới dùng AH để phân định Thắng/Thua.
