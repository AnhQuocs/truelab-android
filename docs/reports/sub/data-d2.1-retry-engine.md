# TrueLab — Data D2.1 Report: Resilience & Retry Engine

**Phase:** Data D2.1
**Status:** COMPLETED (Pending Review)
**Parent Plan:** [docs/plans/data-d2-plan.md](../plans/data-d2-plan.md)
**Verification:** 438/438 Tests PASS (100%) | `assembleDebug` SUCCESS

---

## 1. Executive Summary

Data D2.1 đã triển khai hoàn tất **Resilience & Retry Engine** cho tầng dữ liệu (`:core:data`), giải quyết triệt để vấn đề mất kết nối mạng tạm thời khi đồng bộ dữ liệu từ xa (`DataSyncEngine`).

### Các thành phần chính đã hoàn thành:
1. **`RetryPolicy`**: Lớp cấu hình chính sách retry với validation chặt chẽ và semantics rõ ràng (`maxAttempts` là tổng số lần thực thi bao gồm lần gọi đầu tiên).
2. **`RetryClassifier` & `DefaultRetryClassifier`**: Bộ phân loại lỗi mạng thông minh, phân biệt chính xác giữa lỗi tạm thời (SocketTimeout, ConnectException, UnknownHost, IOException, HTTP 5xx) và lỗi cố định không thể retry (HTTP 4xx, SerializationException, Coroutine Cancellation, Logic/Database errors).
3. **`RetryExecutor`**: Cơ chế thực thi coroutine linh hoạt áp dụng Exponential Backoff với Jitter, giới hạn độ trễ tối đa (`maxDelayMs`), hỗ trợ inject clock/delay để unit test nhanh mà không phụ thuộc vào wall-clock delay.
4. **`CrawlerDataModule`**: Cấu hình Hilt Dependency Injection cho `RetryClassifier`.
5. **`DataSyncEngine` Integration**: Tích hợp `RetryExecutor` vào toàn bộ luồng gọi Remote API (`matchApi`, `oddsApi`, `rankingApi`) mà vẫn bảo toàn 100% tính nguyên tử của giao dịch Room DB (Atomic Transactions) và cơ chế phân trang.

---

## 2. Chi tiết Implementation

### 2.1 `RetryPolicy`
- **File:** [`RetryPolicy.kt`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryPolicy.kt)
- **Cấu hình mặc định:**
  - `maxAttempts = 3` (1 lần ban đầu + tối đa 2 lần retry)
  - `initialDelayMs = 1000L`
  - `maxDelayMs = 10000L`
  - `factor = 2.0`
  - `jitter = true`
- **Validation:** Bắt buộc `maxAttempts >= 1`, `initialDelayMs >= 0`, `maxDelayMs >= initialDelayMs`, `factor >= 1.0`.

### 2.2 `RetryClassifier`
- **File:** [`RetryClassifier.kt`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryClassifier.kt)
- **Phân loại:**
  - **Retryable (true):** `SocketTimeoutException`, `ConnectException`, `UnknownHostException`, `SocketException`, `IOException` tạm thời, `HttpException` (HTTP 500, 502, 503, 504, 5xx).
  - **Non-retryable (false):** `HttpException` (HTTP 400, 401, 403, 404, 4xx), `SerializationException` (lỗi decode JSON), `CancellationException` (không bao giờ nuốt cancellation coroutine), `IllegalArgumentException`, `IllegalStateException`, `SQLiteException`.

### 2.3 `RetryExecutor`
- **File:** [`RetryExecutor.kt`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryExecutor.kt)
- **Công thức Backoff:**
  $$\text{baseDelay} = \min\left(\text{maxDelayMs}, \left(\text{initialDelayMs} \times \text{factor}^{\text{attempt} - 1}\right).\text{toLong()}\right)$$
  $$\text{jitteredDelay} = \text{baseDelay} \times (0.5 + 0.5 \times \text{random})$$
- **Hành vi:**
  - Gặp lỗi retryable: delay backoff và thử lại cho tới khi hết `maxAttempts`.
  - Gặp lỗi non-retryable: ném lỗi ngay lập tức ở attempt 1 mà không delay.
  - Khi hết lượt thử (exhausted): ném exception gốc ra ngoài để caller xử lý.

### 2.4 Tích hợp `DataSyncEngine`
- **File:** [`DataSyncEngine.kt`](../../../core/data/src/main/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngine.kt)
- Bọc các hàm `matchApi.getMatches`, `oddsApi.getOddsHistory`, `rankingApi.getSeasonRanking` qua `retryExecutor.execute { ... }`.
- Nếu retry cạn kiệt, transaction của trang hiện tại chưa được bắt đầu hoặc bị rollback hoàn toàn, bảo toàn tính toàn vẹn của Room DB và trả về `SyncResult.Failure`.

---

## 3. Ma trận Kiểm thử & Kết quả Test (Test Matrix)

Đã bổ sung **29 unit test cases** mới cho D2.1:

| Test Suite | Số lượng Test | Trọng tâm kiểm thử | Kết quả |
| :--- | :---: | :--- | :---: |
| [`RetryPolicyTest.kt`](../../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryPolicyTest.kt) | 6 | Defaults, custom configs, invalid inputs validation | **PASS** |
| [`RetryClassifierTest.kt`](../../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryClassifierTest.kt) | 10 | Timeouts, connection errors, HTTP 5xx vs 4xx, serialization, cancellation | **PASS** |
| [`RetryExecutorTest.kt`](../../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/retry/RetryExecutorTest.kt) | 7 | Attempts count, exponential backoff, max delay cap, jitter bounds, failure stop | **PASS** |
| [`DataSyncEngineResilienceTest.kt`](../../../core/data/src/test/kotlin/dev/anhquocs/truelab/core/data/crawler/DataSyncEngineResilienceTest.kt) | 6 | Integration test cho Match/Odds/Ranking API retries, HTTP 404 stop, DB consistency | **PASS** |

### Tổng hợp Full Regression:
- `:core:algorithm`: **122 / 122 PASS**
- `:core:domain`: **206 / 206 PASS**
- `:core:data`: **64 / 64 PASS** *(tăng từ 35 lên 64)*
- `:app`: **46 / 46 PASS**
- **TỔNG CỘNG: 438 / 438 PASS (100%)**
- **BUILD:** `./gradlew assembleDebug` **SUCCESS**

---

## 4. Đánh giá Ranh giới Kiến trúc & Tuân thủ Quy chuẩn

1. **Ranh giới Clean Architecture**:
   - `:core:domain` và `:core:algorithm` giữ 100% Pure Kotlin/JVM (0 phụ thuộc Android/WorkManager/Retry).
   - Logic Retry hoàn toàn nằm trong `:core:data` (`dev.anhquocs.truelab.core.data.crawler.retry`).
2. **Quy tắc 400 dòng/file**:
   - `RetryPolicy.kt`: 42 dòng
   - `RetryClassifier.kt`: 58 dòng
   - `RetryExecutor.kt`: 96 dòng
   - `DataSyncEngine.kt`: 210 dòng
   - `DataSyncEngineResilienceTest.kt`: 322 dòng
   - Tất cả các file đều dưới 350 dòng.
3. **Không tạo thay đổi ngoài Scope**:
   - Không can thiệp D2.2 (Cache policy) hay D2.3 (WorkManager).
