# Historical Match Sync Audit & Design

## 1. Current Sync Architecture
The current data sync logic resides primarily in `DataSyncEngine.kt`. The main entry point is `syncFullPipelineForDate(date, ...)`, which implies the current architecture is overwhelmingly date-based.

Flow:
1. It queries `/sport/v1.0/matches` via `MatchApi.getMatches(date=date, page=currentPage)`.
2. It loops `while (currentPage <= totalPages)` (reading `response.data.meta.totalPage`).
3. For each page, it maps to `LeagueEntity`, `TeamEntity`, and `MatchEntity` and inserts them into Room via `runInTransaction`.
4. If configured, it proceeds to fetch Odds and Rankings for each match individually.

## 2. Current 156-Match Root Cause
The root cause of having only ~156 matches in the database is the **Date-based sync strategy (`syncFullPipelineForDate`) coupled with limited date coverage**. 

- The current implementation is designed to fetch matches *for a specific date*.
- Even though the pagination logic (`while (currentPage <= totalPages)`) is correctly implemented for that single date, a single day in football only has a limited number of matches (in our Python audit test, calling the date endpoint for `2026-09-01` returned 0 matches, and a typical day might only yield ~100-200 ended matches).
- Unless this function is wrapped in an external loop that iterates over *hundreds of past dates*, the database will remain sparse.
- Furthermore, the `status` parameter in `MatchApi` defaults to `null` (not explicitly `-1` or `8`), meaning it might not even be requesting historical/ended matches exclusively.

## 3. TrueScore API Contract Required by TrueLab
To fulfill the requirement of 50,000+ historical matches, we need the following API contract:
- **Endpoint**: `/sport/v1.0/competitions/{seasonId}/match-list`
- **Query `status`**: `-1` (Verified to retrieve historical/ended matches).
- **Query `page_size`**: `100` (Verified to work and reduce API calls).
- **Pagination**: Iterating from `meta.current_page` to `meta.last_page`.

## 4. Missing Capabilities
1. **Competition/Season Match List Endpoint**: `MatchApi` currently only has `/sport/v1.0/matches` (date-based). It lacks the crucial `/sport/v1.0/competitions/{seasonId}/match-list` endpoint.
2. **Status Parameter Filtering**: The current date sync does not explicitly filter for `status=-1`.
3. **Deep Historical Sweeper**: `DataSyncEngine` lacks a function to iterate over historical seasons (it only fetches `syncLeaguesAndSeasonsFromRemoteInternal` but doesn't use those seasons to fetch matches).

## 5. Historical Sync Design
**Flow:**
1. Fetch `CompetitionList`.
2. For each Competition, fetch `SeasonList`.
3. Identify historical seasons (e.g., prior to the current year).
4. For each selected `seasonId`, call `/sport/v1.0/competitions/{seasonId}/match-list?status=-1&page_size=100`.
5. Loop through `page=1` to `last_page`.
6. For each page, map DTOs to Entities.
7. Perform `runInTransaction` to upsert Teams and Matches.

**Scope Rollout:**
- **Phase A**: 1 Competition (e.g., EPL id=927) × 1 historical season (e.g., id=26528). Yields ~380 matches.
- **Phase B**: Top 5 European Leagues × last 5 seasons. Yields ~9,500 matches.
- **Phase C**: Top 50 Competitions × last 5 seasons. Yields ~75,000 matches.

## 6. Pagination Design
The pagination must rely on the API's `meta` object:
```kotlin
var currentPage = 1
var lastPage = 1
while (currentPage <= lastPage) {
    val response = matchApi.getSeasonMatches(seasonId, status = -1, pageSize = 100, page = currentPage)
    // process data...
    lastPage = response.data.meta?.lastPage ?: 1
    currentPage++
}
```

## 7. Deduplication Strategy
- **API Level**: Because we are fetching by distinct `seasonId`, cross-season duplication is minimal.
- **Kotlin Level**: `teams.distinctBy { it.id }` within each page before insertion.
- **Database Level**: `MatchDao` and `TeamDao` must use `OnConflictStrategy.REPLACE` or `IGNORE`. Currently, `MatchDao` uses `REPLACE` (Upsert), which perfectly handles existing matches without violating constraints.

## 8. Room / Performance Considerations
- **Memory**: Processing page-by-page (100 matches per list) guarantees low memory footprint. We never hold 50k matches in memory.
- **Transactions**: `database.runInTransaction` per page (100 matches + ~200 teams) is optimal for SQLite.
- **Rate Limiting**: A `delay(1000)` or `delay(500)` between page fetches is highly recommended to prevent API rate limiting or Cloudflare blocking during a 50k crawl.

## 9. Odds Data Safety
- **Constraint Check**: `OddsEntity` has a `ForeignKey` to `MatchEntity` with `onDelete = ForeignKey.CASCADE`.
- **Safety**: Re-syncing a `MatchEntity` using `OnConflictStrategy.REPLACE` in Room actually executes an `UPDATE` if the row exists (or a `DELETE`/`INSERT` depending on the SQLite implementation under the hood, but Room `REPLACE` usually preserves child rows if the PK doesn't change). However, to be absolutely safe, `OnConflictStrategy.IGNORE` for historical matches that are already `status='8'` or `status='ended'` might be safer to guarantee Odds aren't orphaned, OR we rely on the fact that `matchId` is completely stable from the API.

## 10. Incremental Migration Plan
1. **Retain Existing Data**: Do not clear the DB.
2. **Add Endpoint**: Add `getSeasonMatches` to `MatchApi`.
3. **Add Sync Function**: Create `syncHistoricalMatchesBySeason(seasonId)` in `DataSyncEngine`.
4. **Iterative Crawl**: Write a simple one-off script/worker to loop through a predefined list of historical `seasonId`s, calling `syncHistoricalMatchesBySeason` for each, with error catching to resume on the next ID if one fails.

## 11. Implementation Checklist

| Item | Current TrueLab | Required |
|------|-----------------|----------|
| Ended status | `null` (in MatchApi) | `-1` |
| Page size | `50` (default) | `100` |
| Pagination | `totalPage` | `current_page` → `last_page` |
| Historical seasons | `syncLeaguesAndSeasonsFromRemote` | Yes (Need Match sync loop) |
| Competition discovery | Exists | Required |
| Deduplication | `distinctBy { it.id }` | `matchId` |
| Resume | Try/Catch present | Yes (Per season loop) |
| Bulk insert | Yes (per page) | Yes |
