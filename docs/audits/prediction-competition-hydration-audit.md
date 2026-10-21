# Báo cáo Điều tra Forensic: Cơ chế Hydration Metadata Giải đấu & Lỗi Khóa Ngoại Cascade SQLite

**Ngày:** 03/10/2026  
**Phạm vi:** Điều tra READ-ONLY trực tiếp từ SQLite DB trên thiết bị thật, Payload API mạng, `DataSyncEngine`, `LeagueDao`, và cơ chế Khóa ngoại Cascade trong Room.  
**Các trận mục tiêu:** Vietnam vs Pakistan (`id = 908568`), Thailand vs Philippines (`id = 908567`), Greece vs Netherlands (`id = 129243`), Germany vs Serbia (`id = 129244`), Wales vs Norway (`id = 129245`), Denmark vs Portugal (`id = 129246`).

---

## 1. Tóm tắt & Nguyên nhân Cốt lõi Xác thực 100%

Cuộc điều tra đã xác định chính xác nguyên nhân khiến các trận như **Vietnam vs Pakistan** (và **Greece vs Netherlands**, **Germany vs Serbia**, v.v.) bị rơi vào nhóm `"Chưa phân loại"` trên `PredictionScreen`, trong khi trận **Thailand vs Philippines** thi đấu cùng ngày trong cùng giải đấu (**FIFA ASEAN Cup**, `competitionId = 2239057`) lại hiển thị đúng tên giải.

### 🔴 Tương tác Gây Lỗi: `OnConflictStrategy.REPLACE` + `ON DELETE SET NULL`
1. **API Remote HOÀN TOÀN KHÔNG LỖI:** API `/sport/v1.0/matches` trả về đầy đủ `competition_id = 2239057` và `competition.name = "FIFA ASEAN Cup"` cho trận **Vietnam vs Pakistan**.
2. **Mapper HOÀN TOÀN KHÔNG LỖI:** `RoomMappers.toMatchEntity()` gán `leagueId = competitionId ?: competition?.id` chính xác.
3. **Lỗi Nằm Tại `LeagueDao.kt:13-14`:**
   ```kotlin
   @Dao
   interface LeagueDao {
       @Insert(onConflict = OnConflictStrategy.REPLACE)
       fun insertLeagues(leagues: List<LeagueEntity>): LongArray
   }
   ```
4. **Ràng buộc Khóa Ngoại trong `MatchEntity.kt:28-32`:**
   ```kotlin
   ForeignKey(
       entity = LeagueEntity::class,
       parentColumns = ["id"],
       childColumns = ["leagueId"],
       onDelete = ForeignKey.SET_NULL
   )
   ```
5. **Cơ chế Xử lý Xung đột của SQLite:**
   - Trong SQLite, lệnh `INSERT OR REPLACE` (sinh ra từ `OnConflictStrategy.REPLACE` của Room) khi gặp một khóa chính đã tồn tại trên bảng cha (`leagues`) sẽ thực hiện ngầm lệnh **`DELETE`** bản ghi cũ trước rồi mới **`INSERT`** bản ghi mới.
   - Khi SQLite `DELETE` bản ghi cũ trong bảng `leagues`, ràng buộc khóa ngoại **`ON DELETE SET NULL`** trên bảng con `matches` được kích hoạt ngay lập tức.
   - **SQLite tự động cập nhật `matches.leagueId = NULL` cho toàn bộ các trận đấu đã chèn trước đó trong cơ sở dữ liệu có tham chiếu tới `leagueId` này!**

---

## 2. Trace Tiến trình Phân trang: Vì sao Thái Lan đúng nhưng Việt Nam bị lỗi

Trong quá trình thực thi `syncMatchesForLocalDate("2026-10-02")`:

```
Trang 1 API (2026-10-02):
  ├─ Match 908568 (Vietnam vs Pakistan): competition_id = 2239057
  ├─ DataSyncEngine chèn LeagueEntity(id = 2239057, name = "FIFA ASEAN Cup")
  └─ DataSyncEngine chèn Match 908568 với leagueId = 2239057.
     ==> Trạng thái SQLite: Match 908568 có leagueId = 2239057. [OK]

Trang 2 API (2026-10-02):
  ├─ Match 908567 (Thailand vs Philippines): competition_id = 2239057
  ├─ DataSyncEngine trích xuất matchLeagues -> LeagueEntity(id = 2239057)
  ├─ DataSyncEngine gọi leagueDao.insertLeagues(matchLeagues)
  ├─ Room thực thi: INSERT OR REPLACE INTO leagues (id, name, ...) VALUES (2239057, ...)
  ├─ 💥 SQLite DELETE dòng cũ (2239057) -> Kích hoạt ON DELETE SET NULL trên bảng `matches`!
  ├─ 💥 Match 908568 (Vietnam vs Pakistan) bị xóa trắng leagueId thành NULL!
  ├─ SQLite INSERT dòng mới (2239057) vào bảng `leagues`.
  └─ DataSyncEngine chèn Match 908567 (Thailand vs Philippines) với leagueId = 2239057.
     ==> Trạng thái cuối trong SQLite:
         - Match 908567 (Thailand) có leagueId = 2239057 [RESOLVED]
         - Match 908568 (Vietnam) bị leagueId = NULL [WIPED -> "Chưa phân loại"]
```

Vì **Thailand vs Philippines** nằm ở **trang cuối cùng** có chứa `competitionId = 2239057`, nên không có trang nào sau đó gọi `REPLACE` đè lên `LeagueEntity(2239057)` nữa, giúp trận Thái Lan giữ nguyên được `2239057`. Tất cả các trận ở các trang trước đó đều bị xóa sạch `leagueId`.

Hiện tượng hoàn toàn tương tự xảy ra với `Greece vs Netherlands`, `Germany vs Serbia`, `Wales vs Norway`, `Denmark vs Portugal` (`leagueId = 2473` - UEFA Nations League): các trận này nằm ở `2026-10-01` (Trang 4), và khi có một trận UEFA Nations League khác được sync ở `2026-10-02` (Trang 5), `LeagueEntity(2473)` bị replace, xóa trắng `leagueId` của tất cả các trận ngày hôm trước về `NULL`.

---

## 3. Kết quả Kiểm tra Trực tiếp từ SQLite Database trên Thiết bị

Dữ liệu truy vấn trực tiếp từ file database `/data/data/dev.anhquocs.truelab/databases/truelab_database.db`:

| matchId | Đội nhà | Đội khách | Giờ thi đấu (UTC) | Trạng thái | match.leagueId trong DB | League có trong `leagues`? | Tên giải qua JOIN |
|---|---|---|---|---|---|---|---|
| **908568** | **Vietnam** | **Pakistan** | `2026-10-02T09:00:00Z` | `ended` | **`NULL`** 🔴 | ✅ Có (`id=2239057`) | `NULL` |
| **908567** | **Thailand** | **Philippines** | `2026-10-02T09:40:00Z` | `ended` | **`2239057`** ✅ | ✅ Có (`id=2239057`) | **FIFA ASEAN Cup** |
| **129243** | **Greece** | **Netherlands** | `2026-10-01T18:45:00Z` | `ended` | **`NULL`** 🔴 | ✅ Có (`id=2473`) | `NULL` |
| **129244** | **Germany** | **Serbia** | `2026-10-01T18:45:00Z` | `ended` | **`NULL`** 🔴 | ✅ Có (`id=2473`) | `NULL` |
| **129245** | **Wales** | **Norway** | `2026-10-01T18:45:00Z` | `ended` | **`NULL`** 🔴 | ✅ Có (`id=2473`) | `NULL` |
| **129246** | **Denmark** | **Portugal** | `2026-10-01T18:45:00Z` | `ended` | **`NULL`** 🔴 | ✅ Có (`id=2473`) | `NULL` |
| **2759533** | **China (W)** | **South Korea (W)** | `2026-10-02T06:00:00Z` | `ended` | **`NULL`** 🔴 | ✅ Có (`id=1756`) | `NULL` |
| **2759534** | **North Korea (W)**| **Japan (W)** | `2026-10-02T10:30:00Z` | `ended` | **`1756`** ✅ | ✅ Có (`id=1756`) | **OCA Women's Asian Games** |

---

## 4. Kiểm tra Dữ liệu API (Wire Trace Thực tế)

Truy vấn thực tế `GET https://apigw-dev.9pooltv.com/sport/v1.0/matches?date=2026-10-02&page=1`:

```json
{
  "id": 908568,
  "competition_id": 2239057,
  "start_time_date": "2026-10-02T09:00:00Z",
  "status": "ended",
  "competition": {
    "id": 2239057,
    "name": "FIFA ASEAN Cup",
    "short_name": "FIFA ASEAN Cup",
    "slug": "fifa-asean-cup",
    "logo": "https://s3-cdn.truescore.world/football/competition/logo/fifa-asean-cup/50x50/75b6ce8a3018d34793e1aa86ec375795.png"
  },
  "home_team": { "id": 127052, "name": "Vietnam" },
  "away_team": { "id": 137359, "name": "Pakistan" }
}
```

- **Xác nhận:** API trả về đầy đủ thông tin giải đấu (`competition_id = 2239057` và `competition.name = "FIFA ASEAN Cup"`).

---

## 5. Thử nghiệm Độc lập Cơ chế Xử lý Xung đột SQLite

### Đoạn mã kiểm tra với `REPLACE` (`scratch/test_fk.py`):
```python
import sqlite3
con = sqlite3.connect(':memory:')
con.execute('PRAGMA foreign_keys = ON;')
con.execute('CREATE TABLE leagues (id INTEGER PRIMARY KEY, name TEXT);')
con.execute('CREATE TABLE matches (id INTEGER PRIMARY KEY, leagueId INTEGER, FOREIGN KEY(leagueId) REFERENCES leagues(id) ON DELETE SET NULL);')

# 1. Chèn League 2239057 & Match
con.execute("INSERT INTO leagues VALUES (2239057, 'FIFA ASEAN Cup');")
con.execute("INSERT INTO matches VALUES (908568, 2239057);")

# 2. Mô phỏng Room @Insert(onConflict = OnConflictStrategy.REPLACE)
con.execute("INSERT OR REPLACE INTO leagues VALUES (2239057, 'FIFA ASEAN Cup Updated');")
print("Matches after REPLACE:", con.execute("SELECT * FROM matches;").fetchall())
```
**Kết quả đầu ra:**
```
Matches after REPLACE: [(908568, None)]  <-- leagueId BỊ XÓA TRẮNG VỀ NULL!
```

### Đoạn mã kiểm tra với `@Upsert` / `ON CONFLICT DO UPDATE` (`scratch/test_upsert.py`):
```python
con.execute("""
INSERT INTO leagues (id, name) VALUES (2239057, 'FIFA ASEAN Cup Updated')
ON CONFLICT(id) DO UPDATE SET name = excluded.name;
""")
print("Matches after UPSERT:", con.execute("SELECT * FROM matches;").fetchall())
```
**Kết quả đầu ra:**
```
Matches after UPSERT: [(908568, 2239057)]  <-- leagueId ĐƯỢC BẢO TOÀN 100%!
```

---

## 6. Kết luận Phân loại Nguyên nhân

| Giả thuyết nguyên nhân | Kết luận | Chi tiết giải thích |
|---|---|---|
| **A. API thiếu data** | ❌ Phủ định | API trả về đầy đủ `competition_id` & `competition.name`. |
| **B. Mapper làm mất data** | ❌ Phủ định | Mapper chuyển đổi chính xác sang `MatchEntity(leagueId=2239057)`. |
| **C. Match upsert không cập nhật leagueId** | ❌ Phủ định | `MatchDao_Impl` bind `leagueId` và cập nhật bình thường. |
| **D. LeagueEntity không được chèn** | ❌ Phủ định | `LeagueEntity` tồn tại đầy đủ trong bảng `leagues`. |
| **E. Thứ tự transaction Sync sai** | ❌ Phủ định | `LeagueEntity` được chèn trước `MatchEntity` trong cùng transaction. |
| **F. Loading gate mở sớm** | ❌ Phủ định | Loading gate hoạt động đúng sau khi sync xong, nhưng dữ liệu trong DB đã bị cascade wipe. |
| **G. Room Foreign Key Cascade Wipe** | 🔥 **NGUYÊN NHÂN GỐC 100%** | `LeagueDao.insertLeagues` dùng `OnConflictStrategy.REPLACE`. Mỗi lần replace bảng cha `leagues`, SQLite kích hoạt `ON DELETE SET NULL` xóa trắng `leagueId` của toàn bộ các trận đấu trước đó. |

---

## 7. Giải pháp Đề xuất

1. **Cập nhật `LeagueDao.kt`:**
   Thay `@Insert(onConflict = OnConflictStrategy.REPLACE)` bằng `@Upsert` (hoặc `@Insert(onConflict = OnConflictStrategy.IGNORE)` tương tự `TeamDao`):
   ```kotlin
   @Dao
   interface LeagueDao {
       @Upsert
       fun insertLeagues(leagues: List<LeagueEntity>): LongArray
       ...
   }
   ```
2. **Đồng bộ lại dữ liệu các ngày thi đấu:**
   Khi `insertLeagues` sử dụng `@Upsert`, `matches.leagueId` sẽ không bao giờ bị cascade reset về `NULL`, và 100% các trận như Vietnam vs Pakistan, Greece vs Netherlands sẽ luôn hiển thị đúng giải đấu chính thức.

---

## 8. Kết quả Triển khai & Xác thực Tự động (Implementation Results)

### 8.1 Source Fix: Sửa `LeagueDao.kt`
- **File:** `core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/league/local/dao/LeagueDao.kt`
- **Thay đổi:** Chuyển từ `@Insert(onConflict = OnConflictStrategy.REPLACE)` sang `@Upsert`:
  ```kotlin
  @Dao
  interface LeagueDao {
      @Upsert
      fun insertLeagues(leagues: List<LeagueEntity>): LongArray

      @Query("SELECT * FROM leagues ORDER BY name ASC")
      fun getLeagues(): Flow<List<LeagueEntity>>

      @Query("SELECT * FROM leagues WHERE id = :leagueId")
      fun getLeagueById(leagueId: Int): Flow<LeagueEntity?>
  }
  ```

### 8.2 Cơ chế Kỹ thuật: Vì sao loại bỏ REPLACE giải quyết triệt để FK Cascade
- `@Upsert` trong Room 2.6 sinh ra mã SQL `INSERT INTO leagues ... ON CONFLICT(id) DO UPDATE SET ...`.
- SQLite hoàn toàn **không thực thi lệnh `DELETE`** trên bản ghi cũ của bảng `leagues`.
- Do không có thao tác xóa bản ghi cha, ràng buộc khóa ngoại `ON DELETE SET NULL` trên bảng `matches` không bao giờ bị kích hoạt.
- Toàn bộ các trận đấu đã chèn trước đó trong cơ sở dữ liệu (`matches.leagueId`) được bảo toàn 100% qua mọi chu kỳ sync và phân trang.

### 8.3 Xác thực Logic Match Upsert
- Đã kiểm tra `MatchDao.kt` và `MatchDao_Impl.java`: Phương thức `upsertMatches` đã sử dụng `@Upsert`.
- Khi cơ sở dữ liệu có bản ghi `matches.leagueId = NULL` từ trước, nếu lần sync tiếp theo cung cấp `MatchEntity` với `leagueId` hợp lệ (e.g. `2239057`), SQLite sẽ tự động cập nhật lại trường `leagueId` mới mà không cần sửa đổi thêm logic trong `MatchDao`.

### 8.4 Automated Regression Tests
- Đã tạo test suite chuyên biệt: [`LeagueDaoRoomTest.kt`](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/league/local/LeagueDaoRoomTest.kt) với 3 test scenarios:
  1. `upsert_league_preserves_child_match_foreign_key_reference`: Xác thực chèn League $\to$ Chèn Match với `leagueId` $\to$ Upsert lại League $\to$ `Match.leagueId` giữ nguyên vẹn và metadata giải đấu được cập nhật.
  2. `multiple_matches_in_same_league_preserve_leagueId_across_multiple_upserts`: Xác thực nhiều trận đấu cùng giải $\to$ Upsert lặp lại nhiều lần $\to$ Tất cả các trận vẫn giữ đúng `leagueId`.
  3. `match_with_null_leagueId_can_be_re_hydrated_via_match_upsert`: Xác thực trận đấu có `leagueId = NULL` có thể được re-hydrate thành công thông qua `upsertMatches`.

### 8.5 Kết quả Build & Test
- `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL (83 tasks, 100% PASS)**
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL (109 tasks, APK generated)**

