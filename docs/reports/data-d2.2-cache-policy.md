# TrueLab — Data D2.2 Report: Cache & Stale Data Policy

**Phase:** Data D2.2  
**Status:** COMPLETED (Pending Review)  
**Parent Plan:** [docs/plans/data-d2-plan.md](../plans/data-d2-plan.md)  
**Verification:** 457/457 Tests PASS (100%) | `assembleDebug` SUCCESS

---

## 1. Executive Summary

Data D2.2 đã triển khai hoàn tất **Cache & Stale Data Policy** cho tầng dữ liệu (`:core:data`), giải quyết bài toán tối ưu hóa tài nguyên mạng, kiểm soát độ tươi của dữ liệu (Freshness & TTL) dựa trên `DatasetMetadata.lastSyncTimestamp` hiện hữu, hỗ trợ cờ ép buộc làm mới (`forceRefresh`), và thiết lập cơ chế Offline-first / Cache Fallback an toàn khi gặp sự cố mạng.

### Các thành phần chính đã hoàn thành:
1. **`DatasetCategory` & `DataFreshnessPolicy`**: Mô hình phân loại nhóm dữ liệu và cấu hình thời gian sống (TTL) linh hoạt, hỗ trợ validation các giá trị thời gian không âm.
2. **`CacheFreshnessChecker` & `DefaultCacheFreshnessChecker`**: Thành phần chuyên biệt đánh giá độ tươi của dữ liệu dựa trên timestamp đồng bộ cuối cùng (`lastSyncTimestamp`), mốc thời gian hiện tại (`currentTime`) và thời gian sống (`ttlMs`), bao quát đầy đủ các trường hợp biên và bất thường đồng hồ (Clock drift).
3. **`CrawlerDataModule`**: Bổ sung Hilt Dependency Injection cho `CacheFreshnessChecker`.
4. **`DataSyncEngine` Integration**: Tích hợp cơ chế kiểm tra Freshness vào pipeline đồng bộ `syncFullPipelineForDate` và `syncMatchesByDate`:
   - `forceRefresh = false`: Nếu cache còn tươi $\to$ Bỏ qua toàn bộ remote API calls, không kích hoạt Retry, không ghi đè DB cục bộ.
   - `forceRefresh = true` hoặc cache đã cũ (stale) $\to$ Thực hiện remote sync qua `RetryExecutor` (D2.1), cập nhật `DatasetMetadata.lastSyncTimestamp` sau khi thành công.
5. **Offline-first & Cache Fallback**: Khi remote sync thất bại (kể cả khi retry cạn kiệt), dữ liệu cũ trong Room DB được bảo toàn nguyên vẹn, metadata timestamp không bị cập nhật sai lệch, và lỗi gốc được trả về qua `SyncResult.Failure`.

---

## 2. Chi tiết Implementation & Ranh giới Kiến trúc

### 2.1 `DataFreshnessPolicy` & `DatasetCategory`
- **File:** [`DataFreshnessPolicy.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/cache/DataFreshnessPolicy.kt)
- **Danh mục và TTL mặc định:**
  - `LIVE_MATCHES` (Trận đấu đang diễn ra / trong ngày): **5 phút** (`300_000L` ms)
  - `SCHEDULED_MATCHES` (Trận đấu sắp diễn ra): **30 phút** (`1_800_000L` ms)
  - `HISTORICAL_MATCHES` (Trận đấu lịch sử đã kết thúc): **24 giờ** (`86_400_000L` ms)
  - `DEFAULT` (Đồng bộ tổng quát): **15 phút** (`900_000L` ms)
- **Validation:** Bắt buộc toàn bộ giá trị TTL $\ge 0$.

### 2.2 `CacheFreshnessChecker`
- **File:** [`CacheFreshnessChecker.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/cache/CacheFreshnessChecker.kt)
- **Quy tắc đánh giá:**
  - `lastSyncTimestamp <= 0` $\to$ **Stale** (chưa từng đồng bộ).
  - `ttlMs <= 0` $\to$ **Stale** (không lưu cache).
  - `currentTime < lastSyncTimestamp` $\to$ **Stale** (đồng hồ bị trôi ngược về quá khứ hoặc timestamp ở tương lai).
  - `currentTime - lastSyncTimestamp < ttlMs` $\to$ **Fresh** (dữ liệu còn hạn).
  - `currentTime - lastSyncTimestamp >= ttlMs` $\to$ **Stale** (hết hạn tại mốc biên và sau đó).

### 2.3 Tích hợp `DataSyncEngine`
- **File:** [`DataSyncEngine.kt`](../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt)
- **Luồng xử lý:**
  1. Kiểm tra Freshness tại bước đầu tiên nếu `forceRefresh = false`.
  2. Nếu Fresh $\to$ trả về ngay `SyncResult.Success(SyncSummary(matchesSynced = 0, teamsSynced = 0, timestamp = lastSyncTimestamp))` mà không gọi bất kỳ HTTP request nào.
  3. Nếu Stale hoặc `forceRefresh = true` $\to$ tiến hành tải trang từ xa với `RetryExecutor`, nạp vào Room qua Atomic Transaction và cập nhật `DatasetMetadata.lastSyncTimestamp`.
  4. Hỗ trợ inject `timeProvider: () -> Long` giúp unit test hoàn toàn độc lập với wall-clock thời gian thực.

### 2.4 Ranh giới Schema & Không thêm Migration v3
- Giữ nguyên Room Schema v2 hiện tại, sử dụng trường `DatasetMetadata.lastSyncTimestamp` của primary dataset key (`PRIMARY_DATASET`).
- Tuyệt đối không thêm cột, bảng hay migration Room trong D2.2.

---

## 3. Ma trận Kiểm thử & Kết quả Test (Test Matrix)

Đã bổ sung **19 unit test cases** mới cho D2.2:

| Test Suite | Số lượng Test | Trọng tâm kiểm thử | Kết quả |
| :--- | :---: | :--- | :---: |
| [`DataFreshnessPolicyTest.kt`](../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/cache/DataFreshnessPolicyTest.kt) | 7 | Defaults, category mappings, custom configurations, validation rules | **PASS** |
| [`CacheFreshnessCheckerTest.kt`](../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/cache/CacheFreshnessCheckerTest.kt) | 6 | Freshness boundaries, expired timestamps, zero/negative TTL, clock drift | **PASS** |
| [`DataSyncEngineCacheTest.kt`](../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngineCacheTest.kt) | 6 | Fresh cache skips remote sync, stale cache triggers sync, forceRefresh bypass, category TTLs, failure offline fallback | **PASS** |

### Tổng hợp Full Regression:
- `:core:algorithm`: **122 / 122 PASS**
- `:core:domain`: **206 / 206 PASS**
- `:core:data`: **83 / 83 PASS** *(tăng từ 64 lên 83)*
- `:app`: **46 / 46 PASS**
- **TỔNG CỘNG: 457 / 457 PASS (100%)**
- **BUILD:** `./gradlew assembleDebug` **SUCCESS**

---

## 4. Đánh giá Ranh giới Kiến trúc & Tuân thủ Quy chuẩn

1. **Ranh giới Clean Architecture**:
   - `:core:domain` và `:core:algorithm` giữ 100% Pure Kotlin/JVM (0 phụ thuộc Android/WorkManager/Cache).
   - Logic Cache Freshness hoàn toàn nằm trong `:core:data` (`dev.anhquocs.truelab.core.data.crawler.cache`).
2. **Quy tắc 400 dòng/file**:
   - `DataFreshnessPolicy.kt`: 57 dòng
   - `CacheFreshnessChecker.kt`: 40 dòng
   - `DataSyncEngine.kt`: 242 dòng
   - `DataSyncEngineCacheTest.kt`: 326 dòng
   - Tất cả các file đều dưới 350 dòng.
3. **Tuân thủ Scope D2.2**:
   - Không chứa WorkManager (`androidx.work`).
   - Không chứa Paging 3, Multi-provider Identity Resolution hay Delta-sync protocol.
