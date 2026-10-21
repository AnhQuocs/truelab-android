# Plan: Kế Hoạch Triển Khai Controlled Odds Backfill Cho Nghiên Cứu Candidate C

> **Mục tiêu:** Kế hoạch hành động từng bước triển khai thu thập và kiểm toán Odds lịch sử (1X2, Asian Handicap, Over/Under) phục vụ đánh giá thực nghiệm Candidate C.  
> **Nguyên tắc cốt lõi:** Ưu tiên Pilot nhỏ, đảm bảo an toàn Point-in-Time, cô lập dữ liệu thử nghiệm, không over-engineer và không sửa code production.  
> **Thời điểm lập kế hoạch:** 17/10/2026

---

## 1. Phân Kỳ Triển Khai (Phased Roadmap)

```text
[Phase 1: Pilot N=50] ───> [Checkpoint 1: Pilot Review] ───> [Phase 2: Targeted Backfill]
                                     │                                    │
                                     ▼ (Nếu thất bại)                     ▼
                             Dừng / Giữ Cohort N=791           [Checkpoint 2: Data Audit]
                                                                          │
                                                                          ▼
                                                               [Phase 3: Candidate C Eval]
                                                                          │
                                                                          ▼
                                                               [Phase 4: Closure & Report]
```

---

## 2. Chi Tiết Từng Phase

### Phase 1: Pilot Thực Nghiệm (N = 50 Trận) — [MUST HAVE]

- **Mục tiêu:** Kiểm chứng toàn diện API stability, dung lượng payload, cơ chế Point-in-Time filtering và khả năng lưu trữ trên một mẫu nhỏ có kiểm soát trước khi quyết định mở rộng.
- **Tiêu chí chọn mẫu N = 50 (Stratified Sampling):**
  - **Nhóm 1 (20 trận):** Top 5 giải VĐQG Châu Âu (EPL: 6, La Liga: 5, Serie A: 5, Bundesliga: 2, Ligue 1: 2) trong giai đoạn từ `01/03/2026` đến `31/05/2026`.
  - **Nhóm 2 (15 trận):** Cúp Châu Âu & Châu Mỹ (Europa Conference League: 5, MLS: 5, Brazil Serie A: 5) từ `01/04/2026` đến `30/08/2026`.
  - **Nhóm 3 (15 trận):** Giải hạng dưới & Giải quốc nội khác (EFL Championship: 5, Segunda: 5, J1 League: 5) từ `01/03/2026` đến `30/09/2026`.
- **Cơ chế kỹ thuật thực hiện:**
  - Viết script Python độc lập `scratch/run_pilot_odds_backfill.py`.
  - **Endpoint sử dụng:** `GET /sport/v1.0/matches/{matchId}/odds-history?company_id=2` (Bet365).
  - **Tốc độ gọi an toàn (Rate Limit):** Khởi đầu ở **1 request / giây** (sleep $1,000\text{ms}$ giữa các calls).
  - **Cơ chế lưu trữ:** Ghi vào file SQLite tạm biệt lập `scratch/pilot_odds.db` (có schema giống bảng `odds` của Room). **Tuyệt đối không ghi vào DB chính**.
  - **Circuit Breaker (Dừng khẩn cấp):** Dừng ngay lập tức nếu gặp mã `429 Too Many Requests`, `403 Forbidden`, hoặc gặp 3 lỗi mạng liên tiếp.
  - **Checkpointing:** Lưu tiến trình sau từng trận vào `scratch/pilot_checkpoint.json`.

---

### Checkpoint 1: Đánh Giá & Quyết Định Sau Pilot (Decision Gate)

Sau khi hoàn thành 50 trận Pilot, tiến hành kiểm toán dữ liệu và đối chiếu với tiêu chí dừng/tiếp tục:

1. **Điều kiện PASS để tiến hành Phase 2:**
   - Tỷ lệ có Odds $\ge 80\%$ trên Nhóm 1 và $\ge 60\%$ trên toàn bộ 50 trận.
   - Không gặp lỗi HTTP `429` hay `403`.
   - $100\%$ các bản ghi được chọn làm Closing Odds có $T_{\text{change}} \le T_{\text{kickoff}}$ và `market_phase != 'rolling_ball'`.
   - Tái dựng thành công đồng thời cả 3 loại kèo `eu`, `asia`, `bs` cho các trận có Odds.
2. **Kịch bản điều chỉnh hoặc DỪNG (Stop Criteria):**
   - Nếu tỷ lệ có Odds $< 50\%$ hoặc upstream chặn IP/rate limit gắt gao: **Dừng ngay việc backfill**. Giữ nguyên tập dữ liệu $N = 791$ hiện có và ghi nhận rõ hạn chế này trong báo cáo nghiên cứu.

---

### Phase 2: Targeted Backfill Có Chọn Lọc (N ~ 4,000 – 6,000 Trận) — [SHOULD HAVE]

- **Điều kiện kích hoạt:** Chỉ chạy khi Checkpoint 1 đã `PASS`.
- **Phạm vi thu thập:**
  - Giới hạn trong **Top 25 giải đấu chuyên nghiệp** có tỷ lệ có Odds cao đã kiểm chứng trong Pilot.
  - Giới hạn thời gian: Từ `06/02/2026` đến `10/2026`.
- **Quy trình thực thi:**
  - Chạy theo từng giải đấu (League-by-League), có checkpoint và xuất log sau mỗi giải.
  - Giữ tốc độ an toàn: $1.5 - 2\text{ requests/giây}$.
  - Ghi vào cơ sở dữ liệu Staging trung gian.
- **Merge dữ liệu an toàn:**
  - Sau khi toàn bộ batch hoàn tất và vượt qua script kiểm toán tính toàn vẹn (Integrity Check), tiến hành chèn (Insert/Ignore) vào Room DB thật.

---

### Phase 3: Đánh Giá Thực Nghiệm Candidate C — [MUST HAVE]

- **Mục tiêu:** Nạp tập dữ liệu Odds mở rộng vào pipeline đánh giá Candidate C để đo lường giá trị gia tăng thực sự của Asian Handicap và Over/Under.
- **Quy trình đánh giá:**
  1. Trích xuất cohort nghiên cứu: Các trận có đầy đủ 6 tín hiệu Baseline + Pre-match AH và O/U.
  2. Phân chia tập dữ liệu chuẩn thời gian (Temporal Split): Ví dụ chia 60% Train/Calibration và 40% Frozen Holdout theo mốc thời gian diễn ra trận đấu.
  3. Đánh giá Candidate C so với Baseline (WeightedScorer + Candidate A) trên cùng một holdout set:
     - Accuracy
     - Macro F1, Home F1, Away F1, Draw F1
     - Draw Precision & Draw Recall
     - Multi-class Brier Score / Log-loss
     - Paired Bootstrap 95% Confidence Interval ($\Delta \text{Macro F1}$, $\Delta \text{Accuracy}$)
  4. Xác định rõ: Tín hiệu AH và O/U có cải thiện Draw F1 mà không làm suy giảm Accuracy hay không?

---

### Phase 4: Tổng Kết & Đóng Nhiệm Vụ — [MUST HAVE]

- **Đầu ra hoàn thành:**
  - Cung cấp script tái lập cohort dữ liệu (`export_candidate_c_cohort.py`).
  - Báo cáo kết quả thực nghiệm hoàn chỉnh: `docs/reports/candidate-c-backfill-evaluation.md`.
  - Kết luận rõ ràng: Chấp thuận (Accept), Điều chỉnh (Tune) hay Bác bỏ (Reject) Candidate C như một tín hiệu bổ trợ cho production.

---

## 3. Bảng Phân Bổ Effort & Mức Độ Ưu Tiên

| Nhiệm vụ (Task) | Phân loại | Ước tính Effort | Phụ thuộc | Mô tả chi tiết |
|---|:---:|:---:|:---:|---|
| **Task 1: Xây dựng script Pilot N=50 & SQLite Staging** | `MUST HAVE` | **2 – 3 giờ** | - | Script gọi Bet365 history, filter pre-match, lưu vào `scratch/pilot_odds.db`. |
| **Task 2: Thực thi Pilot & Báo cáo Audit Checkpoint 1** | `MUST HAVE` | **1 – 2 giờ** | Task 1 | Chạy 50 trận ở tốc độ 1 req/s, đo lường tỷ lệ thành công, kiểm toán leakage. |
| **Task 3: Mở rộng Targeted Backfill (Top 25 giải)** | `SHOULD HAVE` | **3 – 4 giờ** | Task 2 (PASS) | Chạy batch có checkpoint cho ~4,000–6,000 trận, merge vào Room DB. |
| **Task 4: Đánh giá thực nghiệm Candidate C trên cohort mới** | `MUST HAVE` | **3 – 4 giờ** | Task 3 (hoặc N=791) | Chạy evaluation pipeline, paired bootstrap CI và so sánh với Baseline. |
| **Task 5: Báo cáo tổng kết & Đóng nghiên cứu Candidate C** | `MUST HAVE` | **1 – 2 giờ** | Task 4 | Viết báo cáo kết quả, kết luận giá trị gia tăng của AH/OU. |
| **Task 6: Tối ưu hoá đa nhà cung cấp / Auto crawler daemon** | `NICE TO HAVE` | *Hoãn lại* | - | Không cần thiết cho mục tiêu nghiên cứu hiện tại. |

---

## 4. Phạm Vi Tối Thiểu Khả Thi (Minimal Viable Scope)

Nếu nguồn lực và thời gian bị giới hạn:
1. Chỉ thực hiện **Task 1 & Task 2 (Pilot N = 50)**.
2. Nếu Pilot xác nhận tỷ lệ Pre-match Odds cao, chỉ cần lấy thêm **$N \approx 1,500 - 2,000$ trận** của riêng Ngoại Hạng Anh, La Liga và Serie A (thay vì toàn bộ 25 giải).
3. Đánh giá Candidate C ngay trên tập $N \approx 2,000$ trận này. Quy mô này đã gấp gần 3 lần tập cũ ($N = 791$) và hoàn toàn đủ độ tin cậy thống kê để ra phán quyết đóng hoặc mở tiếp Candidate C.
