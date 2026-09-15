# 👥 PHÂN CÔNG CÔNG VIỆC CÁC THÀNH VIÊN TRONG ĐỀ TÀI TRUELAB

> **Đồ án môn học:** Thuật toán ứng dụng  
> **Tên đề tài:** *Ứng dụng thuật toán trong phân tích dữ liệu thể thao đa nguồn*  
> **Ứng dụng:** TrueLab (`dev.anhquocs.truelab`)

---

## 📋 1. Tổng quan phân công nhiệm vụ

Để đảm bảo tiến độ và chất lượng của đề tài, nhóm phân chia công việc theo hai hướng chuyên môn chính: **phát triển hệ thống và triển khai kỹ thuật**, **nghiên cứu thuật toán, kiểm thử và đánh giá**. Hai thành viên phối hợp xuyên suốt trong quá trình phân tích bài toán, xây dựng Dataset, triển khai thuật toán, thực nghiệm, đánh giá kết quả và hoàn thiện báo cáo.

| Thành viên | Vai trò chuyên môn | Trách nhiệm chính |
| :--- | :--- | :--- |
| **Bùi Anh Quốc** | **System Architect & Mobile Lead** | Phân tích yêu cầu và thiết kế kiến trúc hệ thống; thiết kế và triển khai Data Pipeline; tích hợp API và xây dựng cơ chế thu thập dữ liệu; thiết kế cơ sở dữ liệu Room; hiện thực và tích hợp các thuật toán **Linear Search, Binary Search, Quick Sort, Merge Sort, Descriptive Statistics, Moving Average, Form Score, Elo Rating System và Weighted Scoring**; phát triển giao diện Jetpack Compose; tích hợp các thành phần xử lý dữ liệu, thuật toán và giao diện; kiểm thử tích hợp và hoàn thiện sản phẩm. |
| **Hà Mạnh Long** | **Algorithm Analyst & QA Engineer** | Nghiên cứu, đặc tả và phân tích các thuật toán; phân tích độ phức tạp; nghiên cứu **Linear Search, Binary Search, Quick Sort, Merge Sort, Descriptive Statistics, Moving Average, Form Score, Elo Rating System và Weighted Scoring**; thiết kế Test Cases và Benchmark; thực nghiệm, thu thập và phân tích kết quả; đánh giá hiệu năng và độ chính xác của các phương pháp; nghiên cứu các phương pháp Machine Learning mở rộng nếu được triển khai; phối hợp kiểm thử và hoàn thiện báo cáo. |

---

## 🛠️ 2. Chi tiết công việc của từng thành viên

### 2.1. Bùi Anh Quốc – System Architect & Mobile Lead
Thành viên phụ trách chính phần **thiết kế, triển khai và tích hợp hệ thống**, bao gồm:

* 📐 **Kiến trúc & Hệ thống:** Phân tích yêu cầu chức năng và xác định kiến trúc tổng thể của ứng dụng (Clean Architecture + MVVM).
* 🔄 **Data Pipeline:** Thiết kế Data Pipeline phục vụ quá trình thu thập, chuẩn hóa và lưu trữ Dataset quy mô 50.000 – 75.000 matches.
* 🌐 **Tích hợp API:** Tích hợp các API cung cấp dữ liệu trận đấu, thông tin đội bóng, tỷ lệ và lịch sử biến động tỷ lệ.
* 🗄️ **Cơ sở dữ liệu Room:** Thiết kế cơ sở dữ liệu Room và mô hình dữ liệu (Entities, DAOs) phục vụ lưu trữ Dataset offline.
* ⚙️ **Đồng bộ dữ liệu:** Xây dựng cơ chế đồng bộ, phân trang, lưu trữ và xử lý dữ liệu cục bộ an toàn, có khả năng resume.
* 🔍 **Hiện thực Thuật toán Searching:**
  * Hiện thực và tích hợp **Linear Search** phục vụ tìm kiếm tuần tự trên Dataset.
  * Hiện thực và tích hợp **Binary Search** phục vụ tìm kiếm trên Dataset đã được sắp xếp.
* 📊 **Hiện thực Thuật toán Sorting:**
  * Hiện thực và tích hợp **Quick Sort** phục vụ sắp xếp dữ liệu theo các tiêu chí khác nhau.
  * Hiện thực và tích hợp **Merge Sort** phục vụ sắp xếp dữ liệu theo phương pháp chia để trị.
* 📈 **Hiện thực Thống kê & Phân tích chuỗi:**
  * Hiện thực và tích hợp **Descriptive Statistics** để tính toán các đại lượng thống kê (Mean, Median, Variance, Standard Deviation).
  * Hiện thực và tích hợp **Moving Average** để làm mịn và phân tích xu hướng biến động Odds theo thời gian.
* ⚡ **Hiện thực Đánh giá & Xếp hạng:**
  * Hiện thực và tích hợp **Form Score** nhằm đánh giá phong độ của đội bóng dựa trên các trận đấu gần nhất.
  * Hiện thực và tích hợp **Elo Rating System** để cập nhật và đánh giá sức mạnh tương đối của các đội bóng theo trình tự thời gian.
* 🔮 **Hiện thực Dự đoán:**
  * Hiện thực và tích hợp mô hình **Weighted Scoring** để tổng hợp các đặc trưng và đưa ra kết quả dự đoán.
* 📱 **Giao diện người dùng (UI/UX):**
  * Phát triển giao diện người dùng hiện đại bằng **Jetpack Compose (Material 3)**.
  * Xây dựng 6 màn hình: Home, Matches, Teams, Analytics, Prediction, Benchmark.
* 🧪 **Tích hợp & Kiểm thử:** Thực hiện kiểm thử tích hợp, xử lý lỗi và tối ưu quá trình vận hành ứng dụng.
* 🤖 **Phần mở rộng:** Phối hợp triển khai Logistic Regression và Decision Tree nếu nhóm lựa chọn thực hiện phần mở rộng Machine Learning.

---

### 2.2. Hà Mạnh Long – Algorithm Analyst & QA Engineer
Thành viên phụ trách chính phần **nghiên cứu thuật toán, phân tích độ phức tạp, kiểm thử và đánh giá thực nghiệm**, bao gồm:

* 📚 **Nghiên cứu & Đặc tả:** Nghiên cứu cơ sở lý thuyết và đặc tả đầu vào, đầu ra của các thuật toán.
* ⏱️ **Phân tích Độ phức tạp:** Phân tích độ phức tạp về thời gian và không gian của từng thuật toán:
  * Nghiên cứu và đánh giá **Linear Search** với độ phức tạp lý thuyết $\mathcal{O}(n)$.
  * Nghiên cứu và đánh giá **Binary Search** với độ phức tạp lý thuyết $\mathcal{O}(\log n)$.
  * Nghiên cứu và đánh giá **Quick Sort** với độ phức tạp trung bình $\mathcal{O}(n \log n)$.
  * Nghiên cứu và đánh giá **Merge Sort** với độ phức tạp $\mathcal{O}(n \log n)$.
* 🧮 **Nghiên cứu Toán học:**
  * Nghiên cứu **Descriptive Statistics**, bao gồm Mean, Median, Variance và Standard Deviation.
  * Nghiên cứu **Moving Average** phục vụ phân tích xu hướng chuỗi dữ liệu Odds.
  * Nghiên cứu **Form Score** phục vụ đánh giá phong độ của đội bóng.
  * Phân tích cơ chế cập nhật và ý nghĩa toán học của **Elo Rating System**.
  * Nghiên cứu **Weighted Scoring** và phương pháp lựa chọn trọng số cho các đặc trưng dự đoán.
* 🧪 **Kiểm thử & Benchmark:**
  * Xây dựng bộ **Test Cases** nhằm kiểm tra tính đúng đắn của các thuật toán và chức năng liên quan.
  * Thiết kế các kịch bản **Benchmark** trên nhiều quy mô Dataset khác nhau ($N = 1.000, 10.000, 50.000, 75.000$).
  * Đo lường thời gian thực thi (ms) và mức sử dụng tài nguyên bộ nhớ (MB) của các thuật toán.
  * So sánh, đối chiếu kết quả thực nghiệm với độ phức tạp lý thuyết.
* 📊 **Đánh giá Dự đoán:**
  * Thu thập, tổng hợp và phân tích kết quả thực nghiệm dự đoán.
  * Đánh giá độ chính xác thông qua Ma trận nhầm lẫn (**Confusion Matrix**), **Accuracy**, **Precision**, **Recall**, **F1-Score**.
* 🤖 **Phần mở rộng:** Nghiên cứu Logistic Regression và Decision Tree nếu nhóm lựa chọn triển khai phần mở rộng Machine Learning.
* 📄 **Hồ sơ & Báo cáo:** Tổng hợp nội dung nghiên cứu thuật toán và kết quả thực nghiệm để hoàn thiện cuốn Báo cáo đồ án và Slide thuyết trình.

---

## 🧮 3. Danh sách thuật toán và phân công chuyên môn

Các thuật toán của đề tài được chia thành **nhóm thuật toán cốt lõi (Core)** và **nhóm Machine Learning mở rộng (Optional)**:

| Phân nhóm | Thuật toán | Vai trò nghiên cứu & đặc tả | Vai trò triển khai & tích hợp | Phạm vi |
| :--- | :--- | :---: | :---: | :---: |
| **Searching** | **Linear Search** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Searching** | **Binary Search** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Sorting** | **Quick Sort** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Sorting** | **Merge Sort** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Statistics** | **Descriptive Statistics** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Trend** | **Moving Average** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Evaluation** | **Form Score** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Rating** | **Elo Rating System** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **Prediction** | **Weighted Scoring** | Hà Mạnh Long | Bùi Anh Quốc | **Core** |
| **ML Extension**| **Logistic Regression** | Hà Mạnh Long | Phối hợp / Mở rộng | **Optional** |
| **ML Extension**| **Decision Tree** | Hà Mạnh Long | Phối hợp / Mở rộng | **Optional** |

> **Ghi chú phân công:** Việc phân công được hiểu theo trách nhiệm chuyên môn. Thành viên **Hà Mạnh Long (Algorithm Analyst & QA Engineer)** chịu trách nhiệm chính về *nghiên cứu, đặc tả, phân tích độ phức tạp, xây dựng Test Cases và đánh giá thực nghiệm*; thành viên **Bùi Anh Quốc (System Architect & Mobile Lead)** chịu trách nhiệm chính về *hiện thực hóa và tích hợp các thuật toán vào hệ thống Android*.

---

## 🔄 4. Chu trình phối hợp giữa hai thành viên

Quy trình phối hợp giữa hai thành viên được thực hiện theo chu trình khép kín:

```text
                     Phân tích bài toán
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
      [Nghiên cứu thuật toán]   [Thiết kế hệ thống]
        (Hà Mạnh Long)            (Bùi Anh Quốc)
               │                         │
               ▼                         ▼
      [Đặc tả & Complexity]     [Data Pipeline & Room]
               │                         │
               ▼                         ▼
          [Test Cases]               [Room Database]
               │                         │
               └────────────┬────────────┘
                            ▼
               [Algorithm Implementation]
                     (Bùi Anh Quốc)
                            │
                            ▼
               [Jetpack Compose Integration]
                            │
                            ▼
                [Benchmark & Thực nghiệm]
                    (Cả hai thành viên)
                            │
                            ▼
                     [Phân tích kết quả]
                       (Hà Mạnh Long)
                            │
                            ▼
                [Hoàn thiện Hệ thống & Báo cáo]
```

---

<div align="center">
<b>TrueLab © 2026 — Đồ án Thuật toán ứng dụng</b><br/>
<b>Nhóm thực hiện: Bùi Anh Quốc & Hà Mạnh Long</b>
</div>
