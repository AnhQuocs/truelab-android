# PHÂN CÔNG CÔNG VIỆC — DỰ ÁN TRUELAB

> **Đồ án:** Ứng dụng thuật toán trong phân tích dữ liệu thể thao đa nguồn
> **Ứng dụng:** TrueLab (`dev.anhquocs.truelab`)

---

## 1. Tổng quan phân công

| Thành viên | Vai trò | Nhiệm vụ chính |
| :--- | :--- | :--- |
| **Bùi Anh Quốc** | **System Architect & Mobile Lead** | Phụ trách ý tưởng, phân tích, thiết kế và toàn bộ phần phát triển kỹ thuật của TrueLab: dữ liệu, kiến trúc, API, Room, thuật toán, Prediction Engine, UI/UX, debugging, testing, backtest và thực nghiệm. |
| **Hà Mạnh Long** | **Report** | Phụ trách tổng hợp, biên soạn và hoàn thiện báo cáo đồ án. |

## 2. Chi tiết công việc của từng thành viên

### 2.1. Bùi Anh Quốc — System Architect & Mobile Lead

Phụ trách phần kỹ thuật và triển khai sản phẩm:

* 💡 **Ý tưởng và phân tích bài toán**
  * Xác định mục tiêu, phạm vi và các chức năng chính của TrueLab.
  * Phân tích yêu cầu về dữ liệu, thuật toán, dự đoán và trải nghiệm người dùng.
* 🏛️ **Kiến trúc hệ thống**
  * Thiết kế kiến trúc ứng dụng Android theo module, Clean Architecture và MVVM.
  * Xác định trách nhiệm giữa Presentation, Domain, Data và các module dùng chung.
* 🔄 **Dữ liệu và Data Pipeline**
  * Tìm kiếm, kết nối và xử lý nguồn dữ liệu thể thao.
  * Xây dựng pipeline thu thập, chuyển đổi, kiểm tra, đồng bộ và lưu trữ dữ liệu.
  * Triển khai phân trang, cập nhật nền, retry và các cơ chế hỗ trợ đồng bộ dữ liệu.
* 🌐 **API và tích hợp dịch vụ**
  * Xây dựng API client, request/response models và mapping dữ liệu.
  * Tích hợp các endpoint liên quan đến trận đấu, đội bóng, giải đấu, ranking và odds.
* 🗄️ **Cơ sở dữ liệu Room**
  * Thiết kế entities, DAOs, quan hệ dữ liệu và repository triển khai.
  * Tích hợp Room, mapping domain, schema và database migrations.
* ⚙️ **Thuật toán**
  * Thiết kế, hiện thực, kiểm thử và tích hợp Linear Search, Binary Search, Quick Sort, Merge Sort, Descriptive Statistics, Moving Average, Form Score và Elo Rating System.
  * Hiện thực và tích hợp Weighted Scoring cho bài toán dự đoán kết quả trận đấu.
* 🔮 **Prediction Engine**
  * Xây dựng luồng dự đoán và tích hợp các tín hiệu Form, Elo, Goals, Odds, H2H và Rest Advantage.
  * Xử lý đầu vào, xác suất đầu ra, kết quả dự đoán và bằng chứng cho từng tín hiệu.
* 📱 **UI/UX và ứng dụng Android**
  * Phát triển giao diện bằng Jetpack Compose và Material 3.
  * Tích hợp navigation, ViewModel, trạng thái màn hình, biểu đồ và thành phần giao diện dùng chung.
  * Triển khai các luồng Home, Matches, Teams, Analytics, Prediction, Benchmark, H2H, Backtest và Settings.
* 🐛 **Debugging và kiểm thử**
  * Điều tra và xử lý lỗi trong các lớp dữ liệu, domain, thuật toán và giao diện.
  * Viết và chạy kiểm thử phù hợp cho thuật toán, use case, ViewModel và tích hợp.
* 📊 **Backtest và thực nghiệm**
  * Xây dựng luồng backtest và tính các chỉ số đánh giá mô hình.
  * Thực hiện benchmark thuật toán, thực nghiệm mô hình và phân tích kết quả.
* 🧰 **Tài liệu kỹ thuật**
  * Lập và cập nhật các kế hoạch kỹ thuật, issue, audit và tài liệu liên quan đến quá trình phát triển.

### 2.2. Hà Mạnh Long — Report

Phụ trách công việc báo cáo của đồ án:

* 📄 Tổng hợp nội dung dự án thành báo cáo đồ án.
* ✍️ Biên soạn và sắp xếp nội dung theo cấu trúc báo cáo thống nhất.
* 🧾 Rà soát hình thức, tính nhất quán và hoàn thiện bản báo cáo.

## 3. Phân công các thuật toán

| Phân nhóm | Thuật toán | Phụ trách phân tích và thiết kế | Phụ trách hiện thực và tích hợp |
| :--- | :--- | :--- | :--- |
| Searching | Linear Search | Bùi Anh Quốc | Bùi Anh Quốc |
| Searching | Binary Search | Bùi Anh Quốc | Bùi Anh Quốc |
| Sorting | Quick Sort | Bùi Anh Quốc | Bùi Anh Quốc |
| Sorting | Merge Sort | Bùi Anh Quốc | Bùi Anh Quốc |
| Statistics | Descriptive Statistics | Bùi Anh Quốc | Bùi Anh Quốc |
| Trend | Moving Average | Bùi Anh Quốc | Bùi Anh Quốc |
| Evaluation | Form Score | Bùi Anh Quốc | Bùi Anh Quốc |
| Rating | Elo Rating System | Bùi Anh Quốc | Bùi Anh Quốc |
| Prediction | Weighted Scoring | Bùi Anh Quốc | Bùi Anh Quốc |

## 4. Phối hợp giữa hai thành viên

* **Bùi Anh Quốc** triển khai phần kỹ thuật của ứng dụng và cung cấp thông tin về chức năng, kiến trúc, thuật toán, kiểm thử, backtest và kết quả thực nghiệm.
* **Hà Mạnh Long** sử dụng nội dung dự án để biên soạn, tổng hợp và hoàn thiện báo cáo đồ án.

---

<div align="center">
<b>TrueLab © 2026 — Đồ án Thuật toán ứng dụng</b><br/>
<b>Nhóm thực hiện: Bùi Anh Quốc & Hà Mạnh Long</b>
</div>
