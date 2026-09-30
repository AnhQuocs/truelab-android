# Phase C — Scaled Historical Dataset

## 1. Objective
Mục tiêu của Phase C là mở rộng quy mô dữ liệu lịch sử của TrueLab từ ~5.5k lên quy mô lớn $\approx 15,000$ historical matches, mở rộng từ Top 5 giải đấu châu Âu lên Top 11 giải đấu (bao gồm các giải vô địch quốc gia hạng nhất, hạng nhì và cúp châu lục), đồng thời nạp sâu 5 mùa giải liên tiếp cho Top 5 giải châu Âu nhằm tạo nền tảng vững chắc cho thuật toán Rolling Form, Head-to-Head (H2H), thống kê bàn thắng và mô hình Elo tuần tự theo dòng thời gian.

---

## 2. Deterministic Scope

Tổng scope gồm **11 giải đấu** và **43 mùa giải lịch sử đã kết thúc** ($15\text{ mùa Phase B} + 28\text{ mùa mới Phase C}$), đem lại tổng cộng **15,305 matches** từ API:

| # | Competition | Competition ID | Season | Season ID | API Total | Phase |
|:---:|:---|:---:|:---|:---:|:---:|:---:|
| 1 | Premier League | `927` | 2023-2024 | 26528 | 380 | Phase B |
| 2 | Premier League | `927` | 2022-2023 | 25870 | 380 | Phase B |
| 3 | Premier League | `927` | 2021-2022 | 25233 | 380 | Phase B |
| 4 | Premier League | `927` | 2020-2021 | 24740 | 380 | Phase C Deep |
| 5 | Premier League | `927` | 2019-2020 | 24090 | 380 | Phase C Deep |
| 6 | La Liga | `954` | 2023-2024 | 26542 | 380 | Phase B |
| 7 | La Liga | `954` | 2022-2023 | 25904 | 380 | Phase B |
| 8 | La Liga | `954` | 2021-2022 | 25270 | 380 | Phase B |
| 9 | La Liga | `954` | 2020-2021 | 24774 | 380 | Phase C Deep |
| 10 | La Liga | `954` | 2019-2020 | 24161 | 380 | Phase C Deep |
| 11 | Serie A | `999` | 2023-2024 | 26622 | 380 | Phase B |
| 12 | Serie A | `999` | 2022-2023 | 25908 | 381 | Phase B |
| 13 | Serie A | `999` | 2021-2022 | 25326 | 380 | Phase B |
| 14 | Serie A | `999` | 2020-2021 | 24779 | 380 | Phase C Deep |
| 15 | Serie A | `999` | 2019-2020 | 24236 | 380 | Phase C Deep |
| 16 | Bundesliga | `1017` | 2023-2024 | 26584 | 306 | Phase B |
| 17 | Bundesliga | `1017` | 2022-2023 | 25880 | 306 | Phase B |
| 18 | Bundesliga | `1017` | 2021-2022 | 25256 | 306 | Phase B |
| 19 | Bundesliga | `1017` | 2020-2021 | 24700 | 306 | Phase C Deep |
| 20 | Bundesliga | `1017` | 2019-2020 | 24136 | 306 | Phase C Deep |
| 21 | Ligue 1 | `1065` | 2023-2024 | 26574 | 306 | Phase B |
| 22 | Ligue 1 | `1065` | 2022-2023 | 25881 | 380 | Phase B |
| 23 | Ligue 1 | `1065` | 2021-2022 | 25257 | 380 | Phase B |
| 24 | Ligue 1 | `1065` | 2020-2021 | 24640 | 380 | Phase C Deep |
| 25 | Ligue 1 | `1065` | 2019-2020 | 24095 | 279 | Phase C Deep |
| 26 | EFL Championship | `930` | 2023-2024 | 26543 | 557 | Phase C New |
| 27 | EFL Championship | `930` | 2022-2023 | 25906 | 557 | Phase C New |
| 28 | EFL Championship | `930` | 2021-2022 | 25251 | 557 | Phase C New |
| 29 | Segunda Division | `955` | 2023-2024 | 26566 | 468 | Phase C New |
| 30 | Segunda Division | `955` | 2022-2023 | 25912 | 468 | Phase C New |
| 31 | Segunda Division | `955` | 2021-2022 | 25273 | 468 | Phase C New |
| 32 | Serie B | `1007` | 2023-2024 | 26651 | 390 | Phase C New |
| 33 | Serie B | `1007` | 2022-2023 | 25998 | 390 | Phase C New |
| 34 | Serie B | `1007` | 2021-2022 | 25372 | 390 | Phase C New |
| 35 | Eredivisie | `1054` | 2023-2024 | 26571 | 309 | Phase C New |
| 36 | Eredivisie | `1054` | 2022-2023 | 25871 | 312 | Phase C New |
| 37 | Eredivisie | `1054` | 2021-2022 | 25223 | 312 | Phase C New |
| 38 | UEFA Champions League | `1398` | 2023-2024 | 26539 | 214 | Phase C New |
| 39 | UEFA Champions League | `1398` | 2022-2023 | 25867 | 214 | Phase C New |
| 40 | UEFA Champions League | `1398` | 2021-2022 | 25234 | 218 | Phase C New |
| 41 | UEFA Europa League | `1411` | 2023-2024 | 26720 | 175 | Phase C New |
| 42 | UEFA Europa League | `1411` | 2022-2023 | 26035 | 175 | Phase C New |
| 43 | UEFA Europa League | `1411` | 2021-2022 | 25377 | 175 | Phase C New |
| **Tổng** | **11 Giải đấu** | — | **43 Mùa giải** | — | **15,305** | **100% Hoàn thành** |

---

## 3. Crawl Execution

- **Seasons mới thực thi trong Phase C:** `28 / 28` seasons thành công 100%.
- **Pages fetched trong Phase C:** `111` pages (Tổng cộng cả Phase B + Phase C: `171` pages).
- **API Requests:** `111` requests.
- **Retries:** `0` (Tất cả 111 requests đều thành công ngay ở lần thử đầu tiên).
- **Failed requests:** `0` (100% phản hồi HTTP 200 OK).
- **Rate limiting:** $200\text{ms}$ delay giữa các request HTTP.
- **Persistence Strategy:** Xử lý và nạp dữ liệu tuần tự theo từng trang (page-by-page transaction) với `OnConflictStrategy.IGNORE`. Không lưu trữ toàn bộ 15k records vào RAM cùng lúc.

---

## 4. Dataset Result

- **Phase C Historical Matches:** `15,300` matches ($15,305$ API records nhận về; $5$ records trùng giữa các giai đoạn/mùa giải được `IGNORE` an toàn).
- **Existing Baseline Matches:** `156` matches.
- **Combined DB Total Matches:** **15,456 matches** ($15,300 + 156 = 15,456$).
- **Ended Matches:** `15,397` matches.
- **Matches with valid scores:** `15,456` matches ($100\%$).
- **Total Teams:** `655` teams (Tăng từ 428 lên 655 teams, $+227$ teams mới).

---

## 5. Odds Safety

- **Odds records Before Phase C:** `550,962`
- **Odds records After Phase C:** `550,962`
- **Odds Loss:** `0` ($0\%$ suy hao).
- **Orphan Odds:** `0` (100% Odds liên kết đúng `matchId` hợp lệ).
- **Persistence Semantics:** `OnConflictStrategy.IGNORE` hoạt động tuyệt đối an toàn, không kích hoạt cơ chế `CASCADE DELETE` xóa dữ liệu con như `REPLACE`.

---

## 6. Data Integrity

- **Duplicate Match IDs:** `0` (100% 15,456 IDs đều là unique).
- **Orphan Teams:** `0` (Toàn bộ `homeTeamId` và `awayTeamId` trong 15,456 trận đều tồn tại trong bảng `teams`).
- **Orphan Odds:** `0`.
- **Invalid timestamps:** `0` (100% 15,456 bản ghi parse đúng chuẩn ISO 8601 UTC).
- **Historical Date Range:** `2019-08-09T18:45:00Z` $\to$ `2024-06-23T16:30:00Z` (Bao phủ trọn vẹn 5 năm lịch sử bóng đá châu Âu).
- **Baseline Date Range:** `2026-09-27T00:00:00Z` $\to$ `2026-09-29T10:30:00Z`.
- **Combined DB Date Range:** `2019-08-09T18:45:00Z` $\to$ `2026-09-29T10:30:00Z`.
- **Season / Competition mapping:** Phân bổ chính xác theo 43 mùa giải, không có trận nào bị gán nhầm leagueId hoặc season.

---

## 7. Algorithm Readiness

- **Rolling Form Depth (5-match window):**
  - `294` đội bóng có $\ge 5$ trận đấu.
  - `229` đội bóng có $\ge 30$ trận đấu ($\ge 1$ mùa giải).
  - `158` đội bóng có $\ge 100$ trận đấu ($\ge 3$ mùa giải).
  - `92` đội bóng có $\ge 150$ trận đấu (Trọn vẹn 5 mùa giải liên tiếp).
- **Head-to-Head (H2H) Encounters:**
  - `3,599` cặp đối đầu có $\ge 2$ lần chạm trán.
  - `1,141` cặp đối đầu có $\ge 6$ lần chạm trán.
  - `332` cặp đối đầu có $\ge 10$ lần chạm trán.
- **Sequential Elo & Goals Modeling:**
  - $15,300$ trận đấu lịch sử có đầy đủ tỷ số và thứ tự thời gian chuẩn xác (`startTimeDate`), sẵn sàng phục vụ tính toán Elo Rating động và thống kê phong độ mà không bị rò rỉ dữ liệu tương lai.

---

## 8. Runtime Limitation

> [!CAUTION]
> **Host Validation vs. Android Device Runtime:**
> 
> Dataset của Phase C ($15,456$ matches, $655$ teams, $550,962$ odds) hiện đang nằm trong file cơ sở dữ liệu xác thực **`truelab.db` trên máy host**, được thu thập và xác thực an toàn thông qua Python/SQLite engine do môi trường Gradle/Android trên Windows gặp lỗi `AndroidLocationsBuildService`.
> 
> Ứng dụng Android khi cài đặt và chạy trên thiết bị sử dụng một file Room Database riêng biệt: **`truelab_database.db`** (nằm tại `/data/data/dev.anhquocs.truelab/databases/truelab_database.db`).
> 
> Database runtime trên thiết bị hiện chứa **212 matches gần nhất** (được nạp tự động qua `DataSyncWorker` / `DataSyncEngine` cho 4 ngày gần đây: 27/09 $\to$ 30/09/2026) và **CHƯA ĐƯỢC POPULATE** với dataset 15.4k matches của Phase C.
> 
> Việc tích hợp nạp dữ liệu 15.4k matches vào Android runtime (ví dụ qua `createFromAsset()` hoặc pre-seed database) là công việc thuộc phạm vi của task riêng biệt tiếp theo và **KHÔNG** thuộc phạm vi của Phase C.

---

## 9. Final Status

- **Phase 0:** `VERIFIED`
- **Phase A:** `VERIFIED`
- **Phase B:** `VERIFIED`
- **Phase C:** **`VERIFIED`**
- **Phase C.1 / Android Runtime Population:** `NOT STARTED`
- **Phase D:** `NOT STARTED`
