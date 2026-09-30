# SOURCE OF TRUTH - Historical Match Sync Plan

Tài liệu này là bản quy hoạch và nguồn chân lý (Source of Truth) cho việc đồng bộ dữ liệu trận đấu lịch sử từ TrueScore API vào cơ sở dữ liệu Room của TrueLab.

## 1. Project Objective

Mục tiêu cốt lõi: Nâng cấp dataset hiện tại từ 156 matches lên quy mô 50,000+ matches thông qua các bước mở rộng an toàn và có kiểm soát:

156 matches
→ 380 smoke test (1 season)
→ ~5k (Small dataset)
→ ~15k (Scaled dataset)
→ 50k+ (Large dataset)

Historical dataset khổng lồ này là nền tảng bắt buộc phục vụ các thuật toán và Use Cases:
- Form Score
- Elo Rating System
- H2H Comparison
- Goals Statistics
- Odds Trend / Moving Average
- Prediction / Weighted Scoring
- Backtesting
- Algorithm Benchmarking

## 2. Current Verified State

Các bằng chứng đã được **VERIFIED** qua Audit trực tiếp trên TrueScore API và TrueLab Runtime:

**TrueScore API Contract:**
- Có tổng cộng 2677 competitions khả dụng.
- Hierarchy API `competition → seasons` hoạt động chính xác.
- Endpoint `GET /sport/v1.0/competitions/{seasonId}/match-list` khả dụng.
- Dữ liệu test: Premier League (EPL) `competition_id=927` có 24 seasons lịch sử từ 2003-2004.
- Season EPL 2023-2024 có `seasonId=26528`.
- Cấu hình query lấy trận đã kết thúc (Ended): `status=-1`. (Nhưng object Match trả về vẫn mang `status=8`).
- Hỗ trợ tối đa `page_size=100`.
- Pagination được quản lý qua `meta.current_page` và `meta.last_page`. (Đã update `MetaResponse` mapping).
- Có đầy đủ các giải đấu National/International, không chỉ cấp Club.

**TrueLab Runtime State:**
- Current DB size: 156 matches (97 ended), 309 teams.
- **Đặc biệt lưu ý:** 550,962 odds records đã tồn tại.
- Lỗi thiết kế Sync: Sync hiện tại chủ yếu là date-based (chỉ quét từng ngày) và thiếu season match crawler.
- `MatchApi` hiện thiếu endpoint `season match-list`.
- `MetaResponse` DTO đang có vấn đề mapping ảnh hưởng đến pagination (Đã fix ở Phase 0).
- `MatchEntity` và `OddsEntity` đang bị ràng buộc bởi `ForeignKey` với `ON DELETE CASCADE`. Bất kỳ sơ suất nào cập nhật Match dẫn đến Delete/Re-insert đều sẽ xóa sổ hàng triệu Odds records.

## 3. Critical Safety Rules

Các quy tắc bất biến bắt buộc phải tuân thủ trong suốt quá trình implement:

1. Không clear DB.
2. Không delete existing Odds.
3. Không làm mất 550,962 Odds records hiện tại.
4. Không tạo orphan Odds (kèo không có trận).
5. Không dùng historical future data (data của tương lai) cho Form/Elo/H2H/Backtest tại bất kỳ thời điểm t.
6. Historical matches phải có stable `matchId`.
7. Không crawl 50k ngay từ đầu.
8. Mỗi phase phải pass validation trước khi mở rộng.

## 4. Current Root Causes

Vì sao DB chỉ có 156 matches?

### Root Cause 1
Kiến trúc `DataSyncEngine` đang là Date-based architecture (quét theo từng ngày cụ thể), chưa có vòng lặp quét season-based (theo mùa giải). Do số trận đấu trong một ngày hữu hạn, dataset không thể lớn được.

### Root Cause 2
Pagination metadata (`meta`) chưa mapping chuẩn xác trường `last_page`, khiến vòng lặp fetch page bị hỏng hoặc dừng sớm.

### Secondary limitations
- `page_size` hiện tại đang dùng 50, chưa tối ưu bằng mức 100 thực tế của server.
- Current sync chỉ target Date. Mặc dù đã có logic discover season IDs nhưng chưa sử dụng chúng để fetch matches.

## 5. Target Architecture

Luồng thiết kế kiến trúc chuẩn cho Crawler lịch sử:

Competition
→ Seasons
→ Selected historical season
→ Season Match List endpoint
→ Query params: `status=-1`, `page_size=100`
→ Loop: `page 1..last_page`
→ Map DTO to Entity
→ Safe persistence (Bảo toàn Odds/ID)
→ Validation
→ Checkpoint

## 6. Implementation Phases

### Phase 0 — Safety / Infrastructure
- Verify Room conflict semantics (`REPLACE` vs `IGNORE` vs `Upsert`).
- Verify `Match ↔ Odds` FK cascade behavior.
- Fix `MetaResponse` DTO.
- Add Unit Tests.

### Phase A — Smoke Test
Scope:
- `competitionId=927` (EPL)
- `seasonId=26528` (2023-2024)
- Expected: 380 matches, ~4 pages.

Acceptance:
- Có đúng 380 matches trong DB.
- Fetch qua 4 pages.
- Odds count KHÔNG BỊ THAY ĐỔI / GIẢM.
- Không có orphan odds.
- Không trùng lặp `matchId`.
- Tất cả các tests passed.
- `assembleDebug` passed.

### Phase B — Small Dataset
- Top 5 leagues × 3 seasons.
- Target: ~5,000 matches.
- Validate: Form, H2H, historical ordering, và khởi chạy luồng tính toán Elo ban đầu.

### Phase C — Scaled Dataset
- Top 10 competitions + European cups.
- Target: ~15,000 matches.

### Phase D — Large Dataset
- Top ~30 competitions, 5–10 seasons.
- Target: 50,000 – 75,000+ matches.

## 7. Files Expected To Change

Dựa trên bản Audit, các file dự kiến sẽ bị thay đổi (không ép buộc nếu thực tế implement chứng minh khác đi):

- `BaseResponse.kt` (Hoặc các DTO liên quan đến Meta) - Đã sửa.
- `MatchApi.kt` (Thêm Season Match List endpoint)
- `MatchDao.kt` (Điều chỉnh conflict strategy nếu cần)
- `DataSyncEngine.kt` (Thêm logic season-based crawler)
- `RoomMappers.kt` (Thêm mapping tương ứng nếu response trả về cấu trúc mới)
- Relevant Tests
- Checkpoint/State storage (Nếu cần lưu tiến độ).

## 8. API Contract

Ghi chú rõ rành contract của Season endpoint:

- **Endpoint:** `GET /sport/v1.0/competitions/{seasonId}/match-list`
- **Parameters:**
  - `status=-1` (Lọc các trận đã kết thúc)
  - `page_size=100`
  - `page=1..last_page`
- **Response Structure (Pagination):**
  - `meta.current_page`
  - `meta.last_page`
  - `meta.per_page`
  - `meta.total`
- **Response match status:** Field `status` trong body của match vẫn là `8` (ended).

## 9. Persistence Strategy

Đây là chiến lược bắt buộc cho việc chèn dữ liệu:
KHÔNG premature dictate (ấn định vội vàng) việc dùng `REPLACE` vs `IGNORE` vs `@Upsert`.

**Yêu cầu sống còn số 1:**
Trường hợp: Existing Match + Existing Odds → Có lịch sử resync → **Odds MUST remain intact (Giữ nguyên vẹn).**

Implementation phải tạo unit test/kiểm tra thực tế với Room/SQLite behavior trước khi đưa ra quyết định chọn Conflict Strategy cuối cùng.

## 10. Pagination Strategy

Xử lý từng trang (Page-by-page processing). **Không bao giờ load 50k matches vào memory cùng một lúc.**

Pseudo-flow:
```text
page = 1
while page <= lastPage:
    fetch page
    map page
    persist page
    read lastPage from meta
    page++
    rate-limit delay (e.g., delay(1000))
```

## 11. Resume / Checkpoint Strategy

Hành vi mong muốn:
- Các mùa giải (seasons) đã crawl xong không cần crawl lại không cần thiết.
- Season đang crawl bị đứt gãy mạng phải resume được.
- Một season bị lỗi không được làm chết cả tiến trình đồng bộ toàn dataset.
- Progress có thể quan sát được (observable).

*Quy định:* Không implement Checkpoint cho đến khi Phase A (Smoke Test) chứng minh basic crawler hoạt động đúng.

## 12. Validation Matrix

Đối với mỗi Phase, phải báo cáo theo format sau:

| Metric | Before | Expected | Actual | Status |
|--------|--------|----------|--------|--------|
| Matches | ... | ... | ... | ... |
| Unique Match IDs | ... | ... | ... | ... |
| Ended Matches | ... | ... | ... | ... |
| Teams | ... | ... | ... | ... |
| Odds Records | 550,962 | >= 550,962 | ... | ... |
| Orphan Odds | 0 | 0 | ... | ... |
| Duplicate Odds | 0 | 0 | ... | ... |
| Competitions | ... | ... | ... | ... |
| Seasons | ... | ... | ... | ... |
| API Pages fetched | ... | ... | ... | ... |
| Failed Requests | ... | ... | ... | ... |

## 13. Algorithm/Data Integrity

- Dữ liệu lịch sử phải mang tính tuần tự thời gian (Chronological).
- **Với Elo:** Update Elo phải theo luồng: `match(t1) → update Elo → match(t2) → update Elo`. Không bao giờ gán Elo hiện tại (current) cho một trận lịch sử.
- **Với Form/H2H:** Chỉ các trận đấu có `timestamp < target_match` mới được phép đóng góp vào tính toán.
- **Với Backtest:** Không được rò rỉ dữ liệu tương lai (No future information leakage).

## 14. Current Status / Checklist

### Phase 0
- [x] Room conflict semantics verified
- [x] MetaResponse fixed
- [x] Tests added

### Phase A
- [x] Season endpoint added
- [x] Pagination implemented
- [x] Safe Match persistence implemented
- [x] EPL 2023-24 synced
- [x] 380 matches verified
- [x] Odds count unchanged
- [x] No orphan Odds
- [ ] Build passed

### Phase B Result
- [x] **Top 5 Leagues × 3 Seasons crawled (15 seasons total)**
  - **Historical Scope:** 5,405 matches (API expected 5,405, fetched 5,405)
  - **Existing Baseline:** 156 matches
  - **Combined DB:** 5,561 matches (5,405 + 156 = 5,561)
  - **Competitions:** 5 (EPL 927, La Liga 954, Serie A 999, Bundesliga 1017, Ligue 1 1065)
  - **Seasons:** 15 seasons (100% completed)
  - **Successful Seasons:** 15 / 15
  - **Failed API Requests:** 0 (60 / 60 requests 200 OK)
  - **Odds Records:** 550,962 → 550,962 (0 loss, 100% preserved)
  - **Orphan Odds:** 0
  - **Duplicate Match IDs:** 0
  - **Algorithm Data Readiness:** Verified (119 teams $\ge 5$ matches, 1,300 H2H pairs $\ge 2$, 100% valid ISO timestamps)

### Phase C [VERIFIED]
- [x] **Top 11 Competitions × Historical Seasons (43 seasons total)**
  - **Historical Scope:** 15,300 matches (API expected 15,305 matches across 43 seasons, 5 duplicates ignored safely)
  - **Existing Baseline:** 156 matches
  - **Combined DB:** 15,456 matches ($15,300 + 156 = 15,456$)
  - **Competitions:** 11 competitions (EPL 927, La Liga 954, Serie A 999, Bundesliga 1017, Ligue 1 1065, EFL Championship 930, Segunda Division 955, Serie B 1007, Eredivisie 1054, Champions League 1398, Europa League 1411)
  - **Seasons:** 43 seasons total (15 from Phase B + 28 new in Phase C)
  - **Successful Seasons:** 43 / 43 (100% completed)
  - **Failed API Requests:** 0 (171 / 171 requests total: 60 Phase B + 111 Phase C)
  - **Teams:** 428 → 655 teams (100% unique, 0 orphan references)
  - **Odds Records:** 550,962 → 550,962 (0 loss, 100% preserved)
  - **Orphan Odds:** 0
  - **Duplicate Match IDs:** 0
  - **Historical Date Range:** `2019-08-09T18:45:00Z` → `2024-06-23T16:30:00Z` (5 full seasons depth)
  - **Algorithm Data Readiness:** Verified (294 teams $\ge 5$ matches, 229 teams $\ge 30$, 158 teams $\ge 100$, 3,599 H2H pairs $\ge 2$, 1,141 H2H pairs $\ge 6$, 332 H2H pairs $\ge 10$)

### Phase C.1 [VERIFIED]
- [x] **Prepackaged Historical Asset Database (`Room.createFromAsset`)**
  - **Asset:** `app/src/main/assets/database/truelab_database.db` (58.68 MB vacuumed)
  - **Room Version & Hash:** Room v2 (`9dd083e47a7d9305f0d21f5dc1682349`)
  - **Clean Install Testing:** Verified on physical Android device (`adb uninstall` -> fresh install)
  - **Runtime Database Counts:** 15,505 matches (15,456 asset + 49 live), 739 teams, 625,198 odds, 0 orphan odds, 0 duplicates.
  - **UI Verification:** Dashboard, MatchesScreen, Search, Filter, Sort, Teams, H2H, and Analytics operational.
  - **Sync Stability:** `DataSyncEngine` successfully appends live matches with `OnConflictStrategy.IGNORE` without overwriting historical dataset.

### Phase D (Mass Historical Dataset ~50k+)
- [ ] Top 50+ Leagues/Tournaments
- [ ] Target 50,000+ matches
- *Status:* **NOT STARTED**

> [!IMPORTANT]
> **Persistence Semantics (IGNORE):**
> `MatchDao` sử dụng `@Insert(onConflict = OnConflictStrategy.IGNORE)`.
> Nếu Match đã tồn tại trong DB, khi crawl lại dữ liệu lịch sử sẽ bị IGNORE (không update) để bảo vệ toàn vẹn các bản ghi Odds con (tránh Cascade Delete của REPLACE).

## 15. Decision Log

- **Decision:** Season-based crawl thay vì Date sweep.
  - *Reason:* Historical seasons cung cấp mật độ dày đặc, deterministic scope và tiết kiệm request.
- **Decision:** `page_size=100`.
  - *Reason:* Verified trực tiếp API có support, giảm một nửa request thay vì default 50.
- **Decision:** Phase A isolation trước khi mass-crawl.
  - *Reason:* Bảo vệ tính toàn vẹn của 550k+ Existing Odds records, test validation an toàn.
- **Decision:** No production changes until safety semantics verified.
  - *Reason:* Ngăn chặn cascading deletes trong Room.
- **Decision (Phase 0):** `MatchDao` requires `@Upsert` or `@Insert(IGNORE)` instead of `REPLACE` to prevent CASCADE DELETE on existing Odds.
  - *Reason:* Tested Room semantics. `REPLACE` acts as `DELETE` + `INSERT`, which wipes all child Odds records. `@Upsert` safely updates without deleting.
- **Decision (Phase B):** Deterministic Scope: 5 Leagues (EPL 927, La Liga 954, Serie A 999, Bundesliga 1017, Ligue 1 1065) x 3 Seasons (2023-24, 2022-23, 2021-22) = 15 Seasons. Sequential crawl page-by-page with 200ms delay. 100% success (60/60 API requests, 0 failed).
- **Decision (Phase C):** Scaled Dataset Scope: 11 Competitions x 43 Seasons (Top 5 Leagues x 5 seasons 2019-2024 + 6 Secondary/Continental Competitions x 3 seasons 2021-2024). Sequential crawl 111 pages with 200ms delay. 100% success (111/111 API requests, 0 failed, 9,900 matches fetched). Total DB: 15,456 matches, 655 teams, 550,962 odds intact.

## 16. Rules For Future AI Agents

**BEFORE DOING ANY WORK, READ THIS PLAN.**

Then:
1. Read this plan.
2. Inspect current repository state.
3. Check completed checklist.
4. Continue from first unchecked item.
5. Do not redo completed work without reason.
6. Do not skip validation.
7. Do not expand scope without explicit approval.
8. Never clear/reset the database.
9. Never risk deleting existing Odds.
10. Update this plan after meaningful implementation milestones.
11. Do not commit/push unless explicitly instructed.
