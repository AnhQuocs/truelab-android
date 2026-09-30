# Odds & Runtime Data Coverage Audit

**Date:** 2026-09-29  
**Goal:** Audit runtime database to verify data availability, odds distribution, and prediction signals, avoiding blind assumptions.

## 1. Runtime Snapshot
- **Current git HEAD**: `d285a14` (docs: finalize Presentation P3)
- **Database Status**: The local `truelab.db` has been extracted and queried via Python Pandas.

## 2. Match Coverage
- **Total Matches**: 156
- **Ended Matches (`status='ended'`)**: 97
- **Pending/Other Matches**: 59 (pending, live, determined, cancelled, postponed)

*Finding:* Dataset is extremely small (156 matches), far from the target "50,000–75,000 matches". This is a severely limited test dataset.

## 3. Odds Record Coverage
- **Total Odds Records**: 550,962
- **Avg records per match**: ~4,142
- **Earliest timestamp**: 1785625961
- **Latest timestamp**: 1790666187

*Finding:* While we only have 156 matches, they possess an incredible depth of odds history (avg 4k+ snapshots per match).

## 4. Provider Coverage
- **Distinct Providers**: 17 companies.
- **Top Providers by Volume**:
  1. Sbobet (63,330)
  2. Easybets (62,666)
  3. BET365 (60,942)
  4. Vcbet (55,216)
  5. Pinnacle (46,635)
- **Providers per match**: Median is 14 providers covering a single match.

*Finding:* Multi-provider coverage is excellent for the matches we do have.

## 5. 1X2 Odds Coverage
- **Odds Types**: 
  - `bs` (Over/Under): 210,450
  - `eu` (1X2): 171,008
  - `asia` (Asian Handicap): 140,193
  - `cr` (Correct Score): 29,311
- **1X2 (`eu`) Usable Coverage**: 133 distinct matches possess complete (Home+Draw+Away) `eu` odds.

*Finding:* 133/156 (~85%) of the database matches have valid 1X2 odds. `getLatestOddsForMatch()` will return non-null 1X2 data for these 133 matches.

## 6. Odds History Coverage
- **Matches with >= 10 snapshots**: 133 matches.
- **Market Phases**: `rolling_ball` (In-play) dominates with 464,324 records, `instant` (Pre-match updates) has 81,726, and `initial` (Opening) has 4,912.

*Finding:* Odds movement analysis (Trend, Volatility, Moving Average) is highly viable due to the massive depth of snapshots per match.

## 7. Form Coverage
- **Total Teams**: 309
- **Teams with 0 ended matches**: 115
- **Teams with 1 ended match**: 194
- **Teams with >= 2 ended matches**: 0

*Finding:* **SEVERE DATA SPARSITY**. No team in the current local dataset has played more than 1 match. Form Score calculation (which requires 5 matches) is currently impossible to evaluate meaningfully.

## 8. Elo Coverage
- **Teams with Elo = 1500**: 309 (100%)
- **Teams with Elo != 1500**: 0

*Finding:* **DOMAINE PIPELINE GAP**. Elo Rating is completely static at the default 1500. No historical Elo calculation or updating mechanism has run on this dataset.

## 9. H2H Coverage
- **Team pairs with historical meetings**: 97 pairs.
- **Distribution**: Every pair has exactly 1 match (the 97 ended matches). No pair has met twice.

*Finding:* **DATA SPARSITY**. H2H comparison tool will only ever show 1 historical match at most.

## 10. Goals Coverage
- **Matches with valid Home/Away scores**: 97 (All ended matches).

*Finding:* Goals signal can be calculated for 97 matches.

## 11. Prediction Signal Distribution
- **Total predictions stored in DB**: 0.

*Finding:* The `predictions` table is completely empty. The previous report stating "42 correct / 89" implies backtesting was likely run entirely in-memory during UI interaction, not persisted. Due to Form=0 and Elo=1500 for everyone, the WeightedScoring model essentially defaults to heavily weighting the Odds Implied Probability + Home Advantage, explaining why the prediction variation is extremely low and favors the Home team.

## 12. Match UI Odds Investigation
*Finding:* If the Match UI displays "-" for Odds while the DB clearly has 133 matches with complete `eu` 1X2 odds, it indicates a **UI INTEGRATION GAP** or **MAPPING BUG**. `OddsDao.getLatestOddsForMatch()` works, but the pipeline from `OddsRepository` -> `UseCase` -> `ViewModel` -> `UI` is breaking or filtering out the data incorrectly.

## 13. Root Cause Classification
- **DATA SPARSITY**: The root cause of Form, H2H, and Elo failing is simply the lack of historical matches (only 1 match per team max). We need a much deeper historical fetch.
- **DOMAIN PIPELINE GAP**: Elo ratings are never calculated/updated.
- **UI INTEGRATION GAP**: Odds are failing to render on UI despite massive DB availability.

## 14. Recommended Next Phase
Based strictly on runtime evidence, the next immediate phase must be:
1. **Fix UI Odds Integration**: Resolve why the rich Odds data in the DB is failing to reach the UI.
2. **Execute Deep Historical Sync**: We need the actual 50,000 matches. The current dataset is too shallow (time-wise) to demonstrate Form, Elo, or H2H algorithms effectively.
3. **Persist Predictions/Elo**: Establish an offline job to calculate and persist Elo and Predictions.

## 15. Limitations
- Python `sqlite3` was used on a pulled copy of `truelab.db`.
- Backtest/Prediction runtime logic could not be fully analyzed since no prediction records were persisted to DB.