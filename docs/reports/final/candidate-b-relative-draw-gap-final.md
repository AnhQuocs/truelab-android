# Báo Cáo Nghiệm Thu Thực Nghiệm: Candidate B — Relative Draw Gap

- **Giai đoạn:** Phase B — Draw Decision Policy Evaluation
- **Trạng thái:** Đã hoàn thành (Closed)
- **Quyết định chính thức:** **REJECT Candidate B** (Candidate A tiếp tục là mặc định production)

---

## 1. Mục Tiêu & Giả Thuyết

Nghiên cứu và đánh giá một chính sách quyết định hòa thay thế (**Candidate B — Relative Draw Gap**) áp dụng lên xác suất raw sau khi tổng hợp 6 tín hiệu, nhằm cải thiện khả năng phát hiện trận hòa (Draw F1, Draw TP) mà không làm suy giảm độ chính xác của Home/Away, không làm thay đổi các tín hiệu đầu vào hoặc trọng số của `WeightedScorer`.

### Mô hình tham chiếu (Baseline & Candidate A):
- **Raw Argmax (Baseline):** Nhãn có xác suất raw lớn nhất $\arg\max(P(H), P(D), P(A))$.
- **Candidate A (Decision Margin - Production Default):**
  $$\text{Nếu } |P(H) - P(A)| < 0.04 \text{ VÀ } P(D) \ge 0.255 \implies \text{DRAW; ngược lại dùng Raw Argmax}$$

### Định nghĩa Candidate B (Relative Draw Gap):
$$\text{drawDeficit} = \max(P(H), P(A)) - P(D)$$
$$\text{Quyết định: Nếu Raw Argmax là DRAW } \implies \text{giữ DRAW; ngược lại nếu } \text{drawDeficit} \le \tau \implies \text{DRAW; ngược lại giữ Raw Argmax}$$

---

## 2. Quy Trình Hiệu Chuẩn (Calibration Protocol) & Khóa $\tau^*$

- **Cohort cố định:** 791 trận đấu lịch sử có snapshot pre-match EU 1X2 hợp lệ từ [candidate-a-historical-odds-audit.md](../audits/candidate-a-historical-odds-audit.md).
- **Phân tách thời gian (Chronological Temporal Split):**
  - **Tập Calibration (60% đầu):** **476 trận** (Khung thời gian: `2026-09-12T17:00:00Z` $\to$ `2026-09-27T13:00:00Z`).
  - **Tập Frozen Holdout (40% sau):** **315 trận** (Khung thời gian: `2026-09-27T13:00:00Z` $\to$ `2026-10-06T23:30:00Z`).
- **Mục tiêu Calibration:** $\tau^* = \arg\max_{\tau} \text{Macro F1}_{\text{cal}}(\tau)$ thỏa mãn 5 ràng buộc Non-inferiority:
  1. $\text{Draw F1} > 17,20\%$ (Candidate A)
  2. $\text{Draw TP} \ge 16$ (Candidate A)
  3. $\Delta \text{Accuracy} \ge -0,50\%$
  4. $\Delta \text{Home F1} \ge -1,00\%$
  5. $\Delta \text{Away F1} \ge -1,00\%$

### Kết quả quét lưới trên Calibration (N = 476):

| $\tau$ | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw Prec | Draw Rec | Draw TP | Draw FP | Số dự đoán Draw (%) | H/A đúng đổi sang Draw | Trạng thái |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **0.000** | 49,37% | 36,97% | 60,04% | 0,00% | 50,88% | 0,00% | 0,00% | 0 | 0 | 0 (0,0%) | 0 | FAIL (c1, c2) |
| **0.060** | 49,37% | 37,01% | 60,16% | 0,00% | 50,88% | 0,00% | 0,00% | 0 | 1 | 1 (0,2%) | 0 | FAIL (c1, c2) |
| **0.070** | 49,37% | 37,06% | 60,16% | 0,00% | 51,03% | 0,00% | 0,00% | 0 | 2 | 2 (0,4%) | 0 | FAIL (c1, c2) |
| **0.080** | 49,37% | 38,41% | 60,12% | 4,80% | 50,30% | 37,50% | 2,56% | 3 | 5 | 8 (1,7%) | 3 | FAIL (c1, c2) |
| **0.090** | 49,16% | 39,27% | 59,38% | 7,52% | 50,90% | 31,25% | 4,27% | 5 | 11 | 16 (3,4%) | 6 | FAIL (c1, c2) |
| **0.100** | 48,95% | 40,24% | 58,92% | 11,35% | 50,46% | 33,33% | 6,84% | 8 | 16 | 24 (5,0%) | 10 | FAIL (c1, c2) |
| **0.110** | 48,95% | 41,76% | 58,47% | 15,09% | 51,71% | 28,57% | 10,26% | 12 | 30 | 42 (8,8%) | 14 | FAIL (c1, c2) |
| **0.120** | **47,27%** | **42,07%** | **56,95%** | **19,25%** | **50,00%** | **25,71%** | **15,38%** | **18** | **52** | **70 (14,7%)** | **28** | ✅ **ELIGIBLE** |
| **0.130** | 45,59% | 43,10% | 54,12% | 27,23% | 47,95% | 27,12% | 27,35% | 32 | 86 | 118 (24,8%) | 50 | FAIL (c3, c4, c5) |
| **0.140** | 46,01% | 44,55% | 54,28% | 32,09% | 47,27% | 28,48% | 36,75% | 43 | 108 | 151 (31,7%) | 59 | FAIL (c4, c5) |
| **0.150** | 45,80% | 45,10% | 52,93% | 35,45% | 46,92% | 29,12% | 45,30% | 53 | 129 | 182 (38,2%) | 70 | FAIL (c3, c4, c5) |

**Khóa cứng tham số:** $\tau^* = 0.120$ là điểm vận hành duy nhất thỏa mãn toàn bộ 5 ràng buộc trên Calibration và được đóng băng trước khi đánh giá Holdout.

---

## 3. Kết Quả Nghiệm Thu Trên Frozen Holdout (N = 315)

Đánh giá một lần duy nhất trên tập Holdout đóng băng với $\tau^* = 0.120$:

### Bảng đối chứng chi tiết:

| Chỉ số | Raw Argmax (Baseline) | Candidate A ($\delta=0.04, \theta=0.255$) | Candidate B ($\tau^* = 0.120$) | Chênh lệch (B vs A) |
|---|:---:|:---:|:---:|:---:|
| **Accuracy** | 55,87% (176/315) | 53,65% (169/315) | **54,60% (172/315)** | **+0,95%** |
| **Macro F1** | 41,12% | 47,76% | **47,80%** | **+0,04%** |
| **Home F1** | 66,46% | 63,79% | **65,36%** | **+1,57%** |
| **Draw F1** | 0,00% | **24,35%** | 21,43% | **-2,92%** |
| **Away F1** | 56,90% | 55,14% | **56,60%** | **+1,46%** |
| **Draw Precision** | 0,00% | **28,57%** | 26,09% | -2,48% |
| **Draw Recall** | 0,00% | **21,21%** | 18,18% | -3,03% |
| **Draw True Positives** | 0 | **14** | 12 | **-2** |
| **Draw False Positives** | 0 | 35 | **34** | **-1** |
| **Số dự đoán Draw** | 0 (0,0%) | 49 (15,56%) | **46 (14,60%)** | -3 |
| **H/A đúng đổi sang Draw**| 0 | 21 | **16** | **-5 (Ròng ít phá hơn)** |
| **Tổng số lần đổi nhãn** | 0 | 49 | **46** | -3 |

### Ma trận nhầm lẫn trên Holdout (Hàng: Thực tế H/D/A, Cột: Dự đoán H/D/A):

**Raw Argmax (Baseline):**
```text
[[108,   0,  40],
 [ 36,   0,  30],
 [ 33,   0,  68]]
```

**Candidate A:**
```text
[[ 96,  22,  30],
 [ 28,  14,  24],
 [ 29,  13,  59]]
```

**Candidate B ($\tau^* = 0.120$):**
```text
[[100,  21,  27],
 [ 30,  12,  24],
 [ 28,  13,  60]]
```
*(Đường chéo ma trận Candidate B: $100 + 12 + 60 = 172$ trận đúng $\implies \text{Accuracy} = 172/315 = 54,6032\%$.)*

---

## 4. Đối Chiếu Tiêu Chí Nghiệm Thu (Acceptance Criteria)

| STT | Tiêu chí nghiệm thu đã đăng ký | Kết quả Holdout (Candidate B vs Candidate A) | Đánh giá |
|:---:|---|:---:|:---:|
| 1 | $\text{Draw F1}(B) > \text{Draw F1}(A)$ | $21,43\%$ vs $24,35\%$ ($\Delta = -2,92\%$) | ❌ **FAIL** |
| 2 | $\text{Draw TP}(B) \ge \text{Draw TP}(A)$ | $12$ vs $14$ ($\Delta = -2$ TP) | ❌ **FAIL** |
| 3 | $\text{H/A đúng đổi thành Draw}(B) \le (A)$ | $16$ vs $21$ ($\Delta = -5$ trận) | ✅ **PASS** |
| 4 | $\Delta \text{Accuracy} \ge -0,50\%$ | $+0,95\%$ ($54,60\%$ vs $53,65\%$) | ✅ **PASS** |
| 5 | $\Delta \text{Home F1} \ge -1,00\%$ | $+1,57\%$ ($65,36\%$ vs $63,79\%$) | ✅ **PASS** |
| 6 | $\Delta \text{Away F1} \ge -1,00\%$ | $+1,46\%$ ($56,60\%$ vs $55,14\%$) | ✅ **PASS** |
| 7 | $\text{Macro F1}(B) > \text{Macro F1}(A)$ | $+0,04\%$ ($47,80\%$ vs $47,76\%$) | ✅ **PASS** |
| **Tổng kết** | **Toàn bộ 7 tiêu chí bắt buộc** | **Không thỏa mãn tiêu chí 1 và 2** | ❌ **REJECT** |

---

## 5. Phân Tích Độ Bất Định Theo Cặp (Paired Bootstrap, B = 2.000)

1. **Cluster Bootstrap theo Giải đấu (48 Competitions, B = 2.000):**
   - $\Delta \text{Macro F1}$: Trung vị = $+0,08\%$, **$95\%\text{ CI} = [-2,67\%, +3,27\%]$** (chứa 0).
   - $\Delta \text{Draw F1}$: Trung vị = $-2,95\%$, **$95\%\text{ CI} = [-10,16\%, +6,33\%]$** (chứa 0).
   - $\Delta \text{Accuracy}$: Trung vị = $+0,94\%$, **$95\%\text{ CI} = [-1,06\%, +3,27\%]$** (chứa 0).

2. **Stratified Paired Bootstrap theo Nhãn H/D/A (B = 2.000):**
   - $\Delta \text{Macro F1}$: Trung vị = $+0,02\%$, **$95\%\text{ CI} = [-3,29\%, +3,12\%]$** (chứa 0).
   - $\Delta \text{Draw F1}$: Trung vị = $-2,90\%$, **$95\%\text{ CI} = [-10,92\%, +4,78\%]$** (chứa 0).
   - $\Delta \text{Accuracy}$: Trung vị = $+0,95\%$, **$95\%\text{ CI} = [-1,27\%, +3,49\%]$** (chứa 0).

---

## 6. Phân Tích Cơ Chế & Hạn Chế Của Thực Nghiệm (Limitations)

1. **Phân tích cơ chế theo từng trận đấu:**
   - Candidate B bỏ lỡ 4 trận hòa mà Candidate A bắt được (`129243`, `805231`, `658116`, `933146`) do giá trị `drawDeficit` của 4 trận này dao động từ $0.1210 \to 0.1281$ (vượt nhẹ qua ngưỡng $\tau^* = 0.120$).
   - Candidate B bắt thành công 2 trận hòa mà Candidate A bỏ lỡ (`905790`, `689403`) do chênh lệch xác suất $|P(H) - P(A)| > 0.04$ đã chặn Candidate A, nhưng `drawDeficit \le 0.120` cho phép Candidate B nhận diện đúng.
   - Về phía False Positives: Candidate B tránh được 7 lỗi đổi sai của Candidate A nhưng gây ra 2 lỗi mới $\implies$ Cứu ròng được 5 trận Home/Away đúng, giúp Accuracy cao hơn Candidate A $+0,95\%$.
2. **Hạn chế về Temporal Split:**
   - Ranh giới phân tách thời gian `2026-09-27T13:00:00Z` xuất hiện ở cả cuối tập Calibration và đầu tập Holdout do quy tắc tie-break theo `matchId`. Đây là hạn chế thực nghiệm được ghi nhận và giữ nguyên theo protocol đóng băng.
   - Tập Holdout có sự tập trung giải đấu cao hơn đáng kể (Top 10 giải chiếm 63,2% so với 30,0% ở Calibration) và tỷ lệ hòa thực tế giảm từ $24,58\%$ xuống $20,95\%$.
3. **Kết luận khoa học:**
   - Kết quả thực nghiệm chứng minh Candidate B với $\tau^* = 0.120$ thiên về trường phái thận trọng (bảo vệ Home/Away tốt hơn, nhưng hụt KPI về độ nhạy Draw).
   - Không suy diễn rằng khái niệm Relative Draw Gap nói chung là sai, nhưng với cấu hình và protocol đã đăng ký trên tập dữ liệu này, **Candidate B không đủ điều kiện để thay thế Candidate A**.

---

## 7. Quyết Định & Trạng Thái Hệ Thống

- **Kết luận:** **REJECT Candidate B**.
- **Chính sách Production:** Tiếp tục giữ nguyên **Candidate A làm mặc định production** trong `DrawStrategyConfig.DEFAULT`.
- **Bảo toàn mã nguồn:** Không thay đổi trọng số, không đổi pipeline tín hiệu, không sửa schema Room, và không thay đổi cấu hình mặc định.
