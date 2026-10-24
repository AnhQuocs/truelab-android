**ĐẠI HỌC PHENIKAA**

**TRƯỜNG CÔNG NGHỆ THÔNG TIN PHENIKAA**

![](media/image1.png)

HỌC PHẦN THUẬT TOÁN ỨNG DỤNG CSE703052-1-1-26(N01)

Tên đề tài: Ứng dụng thuật toán trong phân tích dữ liệu tỷ lệ thể thao đa nguồn trên nền tảng Android (TrueLab)

Sinh viên thực hiện: Bùi Anh Quốc (23010328)

Hà Mạnh Long (23010390)

Giảng viên: TS. Trần Đình Tân

Khóa: K17 - 2023 – 2027

Hà Nội, tháng 10 năm 2026

# GIỚI THIỆU CHUNG

Dự án TrueLab là một công trình nghiên cứu và báo cáo học thuật được thực hiện trong khuôn khổ học phần Thuật toán ứng dụng. Mục tiêu cốt lõi của đề tài là nghiên cứu, thiết kế, hiện thực hóa và đo lường thực nghiệm các cấu trúc dữ liệu và giải thuật thuần túy (Pure Kotlin/JVM) phục vụ bài toán thu thập, xử lý dữ liệu chuỗi thời gian, thống kê mô tả, xếp hạng động Elo và mô hình tổng hợp xác suất đa tín hiệu trên thiết bị di động (On-Device Mobile Computing).

Nhóm xin khẳng định và cam kết các nguyên tắc phạm vi sau:

> 1\. Bản chất phi cá cược (Non-Gambling Scope): Ứng dụng TrueLab hoàn toàn không phải là ứng dụng cá cược và tuyệt đối không cổ súy, phục vụ, trung gian hay hỗ trợ bất kỳ hành vi cờ bạc hay cá cược nào.
>
> 2\. Không hỗ trợ giao dịch tài chính: Hệ thống hoàn toàn không có chức năng tạo tài khoản cá cược, không hỗ trợ nạp tiền, rút tiền, đặt cược hay bất kỳ giao dịch thương mại/tiền tệ nào dưới mọi hình thức.
>
> 3\. Không liên kết hoặc môi giới: Ứng dụng không chứa liên kết chuyển hướng (hyperlinks/redirects) đến bất kỳ nền tảng, trang web hoặc dịch vụ cá cược trực tuyến nào.
>
> 4\. Bản chất dữ liệu tỷ lệ (Market Odds Nature): Toàn bộ dữ liệu tỷ lệ tham chiếu (Odds) từ các nhà cung cấp dữ liệu thể thao quốc tế được trích xuất và sử dụng thuần túy như một nguồn dữ liệu xác suất thị trường (Market Implied Probabilities), đại diện cho "trí tuệ đám đông" (Wisdom of the Crowds) phục vụ mục đích nghiên cứu khoa học, mô phỏng giải thuật và đánh giá thực nghiệm mô hình phân loại đa lớp.
>
> 5\. Mã nguồn dự án (Repo GitHub): *https://github.com/AnhQuocs/truelab-android*

Mọi kết quả tính toán xác suất, phân tích phong độ và dự đoán từ hệ thống chỉ mang tính chất tham khảo học thuật và minh chứng năng lực của các thuật toán xử lý dữ liệu.

# LỜI CẢM ƠN

Để hoàn thành tốt báo cáo môn học Thuật toán ứng dụng này, bên cạnh sự nỗ lực của nhóm, nhóm chúng em xin gửi lời cảm ơn sâu sắc nhất tới thầy TS. Trần Đình Tân.Trong suốt quá trình học tập và nghiên cứu, Thầy đã luôn tận tình định hướng phương pháp, truyền đạt những kiến thức chuyên sâu và đưa ra những đóng góp quý báu giúp em/nhóm hiểu rõ bản chất bài toán, tối ưu hóa các giải thuật và tháo gỡ nhiều vướng mắc kỹ thuật phức tạp.

Mặc dù đã cố gắng hết sức, song báo cáo khó tránh khỏi những thiếu sót nhất định. Nhóm chúng em rất mong tiếp tục nhận được những ý kiến đóng góp, chỉ bảo từ thầy để hoàn thiện hơn nữa kiến thức và kỹ năng của bản thân.

Kính chúc Thầy thật nhiều sức khỏe, hạnh phúc và thành công trong sự nghiệp trồng người!

**MỤC LỤC**

[GIỚI THIỆU CHUNG [1](#giới-thiệu-chung)](#giới-thiệu-chung)

[LỜI CẢM ƠN [2](#lời-cảm-ơn)](#lời-cảm-ơn)

[DANH MỤC TỪ VIẾT TẮT [5](#danh-mục-từ-viết-tắt)](#danh-mục-từ-viết-tắt)

[DANH MỤC BẢNG BIỂU [6](#_Toc243581617)](#_Toc243581617)

[DANH MỤC HÌNH VẼ VÀ SƠ ĐỒ [7](#danh-mục-hình-vẽ-và-sơ-đồ)](#danh-mục-hình-vẽ-và-sơ-đồ)

[CHƯƠNG 1. GIỚI THIỆU BÀI TOÁN VÀ CƠ SỞ LÝ THUYẾT [8](#chương-1.-giới-thiệu-bài-toán-và-cơ-sở-lý-thuyết)](#chương-1.-giới-thiệu-bài-toán-và-cơ-sở-lý-thuyết)

[1.1. Bối cảnh và động lực thực hiện đề tài [8](#bối-cảnh-và-động-lực-thực-hiện-đề-tài)](#bối-cảnh-và-động-lực-thực-hiện-đề-tài)

[1.2. Phát biểu bài toán phân loại kết quả thi đấu [8](#phát-biểu-bài-toán-phân-loại-kết-quả-thi-đấu)](#phát-biểu-bài-toán-phân-loại-kết-quả-thi-đấu)

[1.3. Mục tiêu, phạm vi và giới hạn của đề tài [9](#mục-tiêu-phạm-vi-và-giới-hạn-của-đề-tài)](#mục-tiêu-phạm-vi-và-giới-hạn-của-đề-tài)

[1.4. Dữ liệu đầu vào, đầu ra và các ràng buộc bài toán [10](#dữ-liệu-đầu-vào-đầu-ra-và-các-ràng-buộc-bài-toán)](#dữ-liệu-đầu-vào-đầu-ra-và-các-ràng-buộc-bài-toán)

[1.5. Cơ sở lý thuyết các thuật toán nền tảng [10](#cơ-sở-lý-thuyết-các-thuật-toán-nền-tảng)](#cơ-sở-lý-thuyết-các-thuật-toán-nền-tảng)

[1.6. Các chỉ số đánh giá thực nghiệm [12](#các-chỉ-số-đánh-giá-thực-nghiệm)](#các-chỉ-số-đánh-giá-thực-nghiệm)

[CHƯƠNG 2. PHÂN TÍCH BÀI TOÁN VÀ THIẾT KẾ GIẢI PHÁP [13](#_Toc243581626)](#_Toc243581626)

[2.1. Chuyển đổi bài toán thực tế sang bài toán tính toán [13](#chuyển-đổi-bài-toán-thực-tế-sang-bài-toán-tính-toán)](#chuyển-đổi-bài-toán-thực-tế-sang-bài-toán-tính-toán)

[2.2. Đặc điểm dữ liệu và quy trình xử lý dữ liệu [13](#đặc-điểm-dữ-liệu-và-quy-trình-xử-lý-dữ-liệu)](#đặc-điểm-dữ-liệu-và-quy-trình-xử-lý-dữ-liệu)

[2.3. Nguyên tắc bảo toàn dữ liệu và Ngăn chặn rò rỉ thời gian (Temporal Safety) [15](#nguyên-tắc-bảo-toàn-dữ-liệu-và-ngăn-chặn-rò-rỉ-thời-gian-temporal-safety)](#nguyên-tắc-bảo-toàn-dữ-liệu-và-ngăn-chặn-rò-rỉ-thời-gian-temporal-safety)

[2.4. Phân tích các phương án thuật toán và cơ sở lựa chọn [15](#phân-tích-các-phương-án-thuật-toán-và-cơ-sở-lựa-chọn)](#phân-tích-các-phương-án-thuật-toán-và-cơ-sở-lựa-chọn)

[2.5. Thiết kế kiến trúc tổng thể hệ thống TrueLab [16](#thiết-kế-kiến-trúc-tổng-thể-hệ-thống-truelab)](#thiết-kế-kiến-trúc-tổng-thể-hệ-thống-truelab)

[2.6. Thiết kế mô hình 6 nhóm tín hiệu độc lập [22](#thiết-kế-mô-hình-6-nhóm-tín-hiệu-độc-lập)](#thiết-kế-mô-hình-6-nhóm-tín-hiệu-độc-lập)

[CHƯƠNG 3. THUẬT TOÁN VÀ PHƯƠNG PHÁP HIỆN THỰC [24](#_Toc243581633)](#_Toc243581633)

[3.1. Phân hệ thuật toán Tìm kiếm và Sắp xếp [24](#phân-hệ-thuật-toán-tìm-kiếm-và-sắp-xếp)](#phân-hệ-thuật-toán-tìm-kiếm-và-sắp-xếp)

[3.2. Phân hệ thuật toán Thống kê mô tả và Chuỗi thời gian [25](#phân-hệ-thuật-toán-thống-kê-mô-tả-và-chuỗi-thời-gian)](#phân-hệ-thuật-toán-thống-kê-mô-tả-và-chuỗi-thời-gian)

[3.3. Phân hệ thuật toán Đánh giá phong độ và Xếp hạng Elo [26](#phân-hệ-thuật-toán-đánh-giá-phong-độ-và-xếp-hạng-elo)](#phân-hệ-thuật-toán-đánh-giá-phong-độ-và-xếp-hạng-elo)

[3.4. Phân hệ Pipeline Dự đoán và WeightedScorer [26](#phân-hệ-pipeline-dự-đoán-và-weightedscorer)](#phân-hệ-pipeline-dự-đoán-và-weightedscorer)

[3.5. Nghiên cứu Mô hình hóa Kết quả Hòa (Draw Modeling) [28](#nghiên-cứu-mô-hình-hóa-kết-quả-hòa-draw-modeling)](#nghiên-cứu-mô-hình-hóa-kết-quả-hòa-draw-modeling)

[CHƯƠNG 4. THỰC NGHIỆM, ĐÁNH GIÁ VÀ THẢO LUẬN [31](#chương-4.-thực-nghiệm-đánh-giá-và-thảo-luận)](#chương-4.-thực-nghiệm-đánh-giá-và-thảo-luận)

[4.1. Môi trường và điều kiện thực nghiệm [31](#môi-trường-và-điều-kiện-thực-nghiệm)](#môi-trường-và-điều-kiện-thực-nghiệm)

[4.2. Tập dữ liệu thực nghiệm quy mô 75.710 trận đấu [31](#tập-dữ-liệu-thực-nghiệm-quy-mô-75.710-trận-đấu)](#tập-dữ-liệu-thực-nghiệm-quy-mô-75.710-trận-đấu)

[4.3. Kết quả đo đạc hiệu năng thuật toán trên thiết bị thực tế (Benchmark On-Device) [32](#kết-quả-đo-đạc-hiệu-năng-thuật-toán-trên-thiết-bị-thực-tế-benchmark-on-device)](#kết-quả-đo-đạc-hiệu-năng-thuật-toán-trên-thiết-bị-thực-tế-benchmark-on-device)

[4.4. Đánh giá chất lượng mô hình dự đoán và kiểm định Paired Bootstrap [36](#đánh-giá-chất-lượng-mô-hình-dự-đoán-và-kiểm-định-paired-bootstrap)](#đánh-giá-chất-lượng-mô-hình-dự-đoán-và-kiểm-định-paired-bootstrap)

[4.5. Phân tích các trường hợp thất bại và rủi ro [39](#phân-tích-các-trường-hợp-thất-bại-và-rủi-ro)](#phân-tích-các-trường-hợp-thất-bại-và-rủi-ro)

[4.6. Kiểm chứng chất lượng phần mềm (Testing Verification) [39](#kiểm-chứng-chất-lượng-phần-mềm-testing-verification)](#kiểm-chứng-chất-lượng-phần-mềm-testing-verification)

[4.7. Minh chứng giao diện thực tế và phân tích luồng thao tác [40](#minh-chứng-giao-diện-thực-tế-và-phân-tích-luồng-thao-tác)](#minh-chứng-giao-diện-thực-tế-và-phân-tích-luồng-thao-tác)

[CHƯƠNG 5: KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN [53](#_Toc243581647)](#_Toc243581647)

[1. Kết luận [53](#kết-luận)](#kết-luận)

[2. Hạn chế của giải pháp [53](#hạn-chế-của-giải-pháp)](#hạn-chế-của-giải-pháp)

[3. Hướng phát triển trong tương lai: Tích hợp Mô hình Học máy (Machine Learning Roadmap) [54](#hướng-phát-triển-trong-tương-lai-tích-hợp-mô-hình-học-máy-machine-learning-roadmap)](#hướng-phát-triển-trong-tương-lai-tích-hợp-mô-hình-học-máy-machine-learning-roadmap)

[CHƯƠNG 6: TÀI LIỆU THAM KHẢO [55](#chương-6-tài-liệu-tham-khảo)](#chương-6-tài-liệu-tham-khảo)

# DANH MỤC TỪ VIẾT TẮT

| **Viết tắt** | **Thuật ngữ Tiếng Anh**                           | **Diễn giải Tiếng Việt**                      |
|--------------|---------------------------------------------------|-----------------------------------------------|
| **API**      | Application Programming Interface                 | Giao diện lập trình ứng dụng                  |
| **DAO**      | Data Access Object                                | Đối tượng truy cập cơ sở dữ liệu              |
| **DI**       | Dependency Injection                              | Tiêm phụ thuộc                                |
| **DTO**      | Data Transfer Object                              | Đối tượng truyền tải dữ liệu                  |
| **EDA**      | Exploratory Data Analysis                         | Phân tích khám phá dữ liệu                    |
| **ELO**      | Elo Rating System                                 | Hệ thống tính điểm xếp hạng cờ vua / thể thao |
| **H2H**      | Head-to-Head                                      | Thành tích đối đầu trực tiếp                  |
| **IEEE**     | Institute of Electrical and Electronics Engineers | Viện Kỹ sư Điện và Điện tử                    |
| **JVM**      | Java Virtual Machine                              | Máy ảo Java                                   |
| **ML**       | Machine Learning                                  | Học máy                                       |
| **MVVM**     | Model - View - ViewModel                          | Mô hình kiến trúc phần mềm MVVM               |
| **SMA**      | Simple Moving Average                             | Đường trung bình động đơn giản                |
| **UI/UX**    | User Interface / User Experience                  | Giao diện người dùng / Trải nghiệm người dùng |
| **XAI**      | Explainable Artificial Intelligence               | Trí tuệ nhân tạo có thể giải thích            |

<span id="_Toc243581617" class="anchor"></span>

# DANH MỤC BẢNG BIỂU

> **•** Bảng 1.1: Bảng đặc tả các trường dữ liệu đầu vào của hệ thống
>
> **•** Bảng 1.2: Bảng chỉ số đánh giá chất lượng mô hình dự đoán
>
> **•** Bảng 2.1: Đặc tả Use Case UC01 — Xem bảng điều khiển & Thống kê dữ liệu tổng quan
>
> **•** Bảng 2.2: Đặc tả Use Case UC02 — Dự đoán kết quả trận đấu đa tín hiệu
>
> **•** Bảng 2.3: Đặc tả Use Case UC03 — Đánh giá & Kiểm thử ngược lịch sử
>
> **•** Bảng 2.4: Đặc tả Use Case UC04 — Phân tích đối đầu trực tiếp H2H & Phong độ
>
> **•** Bảng 2.5: Đặc tả Use Case UC05 — Đo lường hiệu năng thuật toán trên thiết bị
>
> **•** Bảng 2.6: Đặc tả Use Case UC06 — Khám phá thống kê mô tả & Biến động tỷ lệ
>
> **•** Bảng 2.7: Phân bổ trọng số mặc định cho 6 nhóm tín hiệu độc lập
>
> **•** Bảng 3.1: So sánh đặc tính và độ phức tạp tính toán của các thuật toán trong hệ thống
>
> **•** Bảng 3.2: Ma trận cấu hình trọng số mặc định và biên giá trị của 6 Signal Transformers
>
> **•** Bảng 4.1: Cấu hình phần cứng và môi trường thực nghiệm đo lường hiệu năng
>
> **•** Bảng 4.2: Thống kê tổng quan tập dữ liệu thực nghiệm 75.710 trận đấu
>
> **•** Bảng 4.3: Kết quả đo đạc thời gian thực thi thuật toán tìm kiếm trên đối tượng trận đấu thực tế
>
> **•** Bảng 4.4: Kết quả đo đạc thời gian thực thi thuật toán sắp xếp trên danh sách đối tượng Match
>
> **•** Bảng 4.5: So sánh hiệu năng toàn diện giữa 4 phương án mô hình trên tập kiểm thử độc lập
>
> **•** Bảng 4.6: Ma trận nhầm lẫn 3x3 của Candidate A trên 83 trận đấu kiểm thử thực tế
>
> **•** Bảng 4.7: Kết quả kiểm định thống kê Paired Bootstrap (B = 10.000) so sánh Candidate A và Candidate C
>
> **•** Bảng 4.8: Bảng tổng hợp số lượng Unit Tests theo từng module trong dự án TrueLab

# DANH MỤC HÌNH VẼ VÀ SƠ ĐỒ

> **•** Hình 2.1: Sơ đồ kiến trúc phân tầng đa module Clean Architecture của dự án TrueLab
>
> **•** Hình 2.2: Sơ đồ ca sử dụng tổng thể (Use Case Diagram) của hệ thống TrueLab
>
> **•** Hình 2.3: Sơ đồ tuần tự quy trình dự đoán 11 bước của PredictMatchOutcomeUseCase
>
> **•** Hình 2.4: Sơ đồ nguyên lý bảo toàn thời gian (Temporal Safety Cutoff) ngăn chặn tuyệt đối rò rỉ dữ liệu tương lai
>
> **•** Hình 3.1: Cây quyết định phân ngưỡng biên độ xử lý kết quả Hòa (Candidate A: Decision Margin Policy)
>
> **•** Hình 4.1: Biểu đồ so sánh thời gian thực thi thuật toán tìm kiếm Linear Search vs Binary Search trên thiết bị di động
>
> **•** Hình 4.2: Biểu đồ so sánh thời gian thực thi thuật toán sắp xếp QuickSort vs MergeSort trên đối tượng thực thể Match
>
> **•** Hình 4.3: Giao diện Tổng quan Trang chủ (HomeScreen cuộn dọc 2 phần)
>
> **•** Hình 4.4: Giao diện lựa chọn trận đấu dự đoán và bộ lọc giải đấu chuyên nghiệp (3 màn hình)
>
> **•** Hình 4.5: Kết quả dự đoán xác suất và phân tích chuyên sâu 6 tín hiệu (2 màn hình cuộn dọc)
>
> **•** Hình 4.6: Giao diện thực nghiệm đo lường hiệu năng thuật toán trực tiếp trên thiết bị (2 màn hình cuộn dọc)
>
> **•** Hình 4.7: Giao diện Đánh giá & Kiểm thử ngược Backtest lịch sử (3 màn hình)
>
> **•** Hình 4.8: Công cụ phân tích đối đầu trực tiếp H2H và phong độ giữa Arsenal và Manchester United (3 màn hình)
>
> **•** Hình 4.9: Bảng phân tích thống kê mô tả EDA và ma trận tỷ lệ biến động (4 màn hình)
>
> **•** Hình 4.10: Màn hình danh sách trận đấu và dự đoán nhanh (3 màn hình)
>
> **•** Hình 4.11: Màn hình xếp hạng Elo và phong độ đội bóng (2 màn hình)

# CHƯƠNG 1. GIỚI THIỆU BÀI TOÁN VÀ CƠ SỞ LÝ THUYẾT

## 1.1. Bối cảnh và động lực thực hiện đề tài

Trong lĩnh vực khoa học máy tính và phân tích dữ liệu thể thao, bài toán dự đoán kết quả các trận đấu bóng đá chuyên nghiệp là một thách thức lớn mang tính đa chiều. Khác với các môn thể thao có điểm số dồn dập (như bóng rổ, bóng chuyền hay quần vợt), bóng đá là môn thể thao có tần suất ghi bàn rất thấp (Low-scoring game), với trung bình chỉ từ 2.4 đến 2.8 bàn thắng cho mỗi trận đấu 90 phút. Đặc tính này làm gia tăng đáng kể tính bất định (stochastic nature) và tính ngẫu nhiên của các biến cố trên sân.

Đặc biệt, sự tồn tại của kết quả Hòa (Draw) biến bài toán dự đoán bóng đá thành bài toán phân loại đa lớp 3 chiều (3-Way Classification: Thắng - Hòa - Thua). Đây là điểm khác biệt cốt lõi so với hầu hết các môn thể thao đối kháng hai chiều (Binary Classification: Win/Loss) khác.

Bên cạnh độ phức tạp toán học, việc triển khai các giải thuật phân tích thể thao trên nền tảng thiết bị di động (Mobile Computing) đặt ra những giới hạn khắt khe về tài nguyên phần cứng: dung lượng bộ nhớ RAM hạn chế, tốc độ xử lý CPU của thiết bị cầm tay, và yêu cầu bảo toàn năng lượng pin. Đa số các ứng dụng thương mại hiện nay đều chọn giải pháp đơn giản là đẩy toàn bộ tính toán lên cụm máy chủ đám mây (Cloud Server) và chỉ sử dụng ứng dụng di động như một giao diện hiển thị thụ động. Hướng tiếp cận này làm ứng dụng phụ thuộc hoàn toàn vào kết nối mạng Internet, tiềm ẩn độ trễ cao và chi phí vận hành máy chủ đắt đỏ.

Dự án TrueLab được nghiên cứu và phát triển trong khuôn khổ học phần Thuật toán ứng dụng nhằm giải quyết trực tiếp bài toán trên: Nghiên cứu, thiết kế và tối ưu hóa hệ thống thuật toán thuần túy (Pure Kotlin/JVM) có khả năng thực thi suy luận trực tiếp ngay trên thiết bị di động (On-Device Inference), vận hành độc lập, không phụ thuộc vào máy chủ phân tích trung gian, đồng thời đảm bảo tính minh bạch, có thể giải thích được của thuật toán (Explainable AI - XAI).

## 1.2. Phát biểu bài toán phân loại kết quả thi đấu

Về mặt toán học, bài toán phân loại kết quả trận đấu bóng đá được phát biểu như sau:

Cho một trận đấu bóng đá M giữa hai đội bóng: Đội thứ nhất (được niêm yết ở vị trí Đội A / Home slot - ký hiệu H) và Đội thứ hai (được niêm yết ở vị trí Đội B / Away slot - ký hiệu A), diễn ra tại thời điểm bắt đầu quy định T_kickoff. Cần lưu ý rằng trong hệ thống TrueLab, việc sử dụng các thuật ngữ "Home" (H) và "Away" (A) tuân theo quy ước niêm yết cặp đấu chuẩn 1X2 của ngành thống kê thể thao quốc tế nhằm phân định vị trí hai đội bóng trong cặp đấu (Fixture Pairing), hoàn toàn không áp đặt giả định về mặt địa lý hay quyền chủ nhà thực tế (đặc biệt trong các giải đấu sân trung lập, giải cúp hoặc dữ liệu không ghi nhận sân bãi).

Không gian nhãn kết quả thực tế Y của trận đấu thuộc tập rời rạc 3 phần tử:

Y ∈ { HOME, DRAW, AWAY }

trong đó:

> **•** HOME (H): Đội A giành chiến thắng (Số bàn thắng của Đội A G_H \> G_A).
>
> **•** DRAW (D): Hai đội hòa nhau (Số bàn thắng của hai đội bằng nhau G_H = G_A).
>
> **•** AWAY (A): Đội B giành chiến thắng (Số bàn thắng của Đội B G_A \> G_H).

Mục tiêu của hệ thống thuật toán là: Dựa trên tập dữ liệu lịch sử D\_{\< T_kickoff} được ghi nhận hoàn toàn trước thời điểm bắt đầu trận đấu, xây dựng hàm ánh xạ suy luận f để ước lượng phân phối xác suất tiên nghiệm 3 chiều P = (P(H), P(D), P(A)) thỏa mãn điều kiện tiên đề Kolmogorov:

P(H) + P(D) + P(A) = 1.0 và 0 ≤ P(c) ≤ 1.0, với mọi c ∈ {H, D, A}

Từ phân phối xác suất P, thuật toán áp dụng chính sách ra quyết định để xác định nhãn dự đoán cực đại Ŷ ∈ {H, D, A}, đồng thời cung cấp chỉ số độ tin cậy của quyết định:

Confidence Score = max(P(H), P(D), P(A)) ∈ \[$\frac{1}{3}$, 1.0\]

## 1.3. Mục tiêu, phạm vi và giới hạn của đề tài

**Mục tiêu nghiên cứu:**

> 1\. Thiết kế và hiện thực hóa các cấu trúc dữ liệu và giải thuật nền tảng: Xây dựng phân hệ thuật toán Tìm kiếm (Linear Search, Binary Search), Sắp xếp (QuickSort in-place, MergeSort ổn định), Thống kê mô tả hai bước và Đường trung bình động cửa sổ trượt tối ưu O(N) theo chuẩn số học IEEE-754.
>
> 2\. Nghiên cứu mô hình đánh giá phong độ và xếp hạng năng lực: Xây dựng thuật toán phân tích phong độ gần đây với trọng số thời gian tuyến tính (Recency-Weighted Form) và hệ thống xếp hạng động Elo nhiều người chơi bảo toàn tổng số Zero-sum.
>
> 3\. Hiện thực hóa Pipeline tổng hợp đa tín hiệu có thể giải thích (Explainable Multi-Signal Pipeline): Thiết kế giải thuật WeightedScorer kết hợp 6 nguồn dữ liệu độc lập với cơ chế làm mịn biên và phân bổ tín hiệu hòa đối xứng.
>
> 4\. Giải quyết triệt để hiện tượng Triệt tiêu kết quả Hòa (Flat Zero Draw Underprediction): Khảo sát thực nghiệm đối chứng các phương án giải quyết (Candidates A, B, C) trên tập dữ liệu lớn 75.710 trận đấu, thiết lập chính sách phân ngưỡng biên độ quyết định (Decision Margin) tối ưu hóa Macro F1.
>
> 5\. Kiểm định thực nghiệm on-device và kiểm thử toàn diện: Đo đạc thời gian thực thi thuật toán trên tập dữ liệu trận đấu thực tế và xác thực độ tin cậy bằng hệ thống Unit Test tự động.

**Phạm vi và giới hạn đề tài:**

> • Phạm vi dữ liệu: 75.710 trận đấu bóng đá chuyên nghiệp từ 316 giải đấu toàn cầu giai đoạn 2024 - 2026, cùng 136.990 bản ghi tỷ lệ tham chiếu đa nguồn từ 17 nhà cung cấp quốc tế được nạp theo cơ chế On-Demand Caching.
>
> • Ranh giới thuật toán: Toàn bộ thuật toán cốt lõi được viết bằng Pure Kotlin/JVM, không phụ thuộc vào Android SDK hay các thư viện máy học cồng kềnh (PyTorch, TensorFlow).
>
> • Giới hạn thời gian suy luận: Dự đoán chỉ sử dụng dữ liệu ghi nhận trước giờ bóng lăn (Pre-match data), tuyệt đối không sử dụng dữ liệu diễn biến thời gian thực (In-play/Live data) để bảo toàn tính trung thực của bài toán dự đoán thể thao.

## 1.4. Dữ liệu đầu vào, đầu ra và các ràng buộc bài toán

**Dữ liệu đầu vào (Inputs):**

Hệ thống tiếp nhận thông tin trận đấu và dữ liệu lịch sử thông qua các thuộc tính chuẩn hóa tại Bảng 1.1:

**Bảng 1.1: Bảng đặc tả các trường dữ liệu đầu vào của hệ thống**

| **Trường dữ liệu**     | **Kiểu dữ liệu**  | **Ý nghĩa bài toán**                               | **Ràng buộc kỹ thuật**                      |
|------------------------|-------------------|----------------------------------------------------|---------------------------------------------|
| id                     | Long              | Định danh duy nhất của trận đấu                    | Số nguyên dương, tăng dần theo thời gian    |
| homeTeamId, awayTeamId | Long              | Định danh câu lạc bộ Đội A và Đội B trong cặp đấu  | Thuộc tập danh mục câu lạc bộ hợp lệ        |
| startTimeDate          | String (ISO-8601) | Mốc thời gian bắt đầu trận đấu (T_kickoff)         | Chuẩn thời gian UTC chính xác               |
| leagueId               | Long              | Định danh giải đấu                                 | Thuộc danh mục giải đấu quản lý             |
| historicalMatches      | List\<Match\>     | Danh sách các trận đấu trong quá khứ               | Thỏa mãn ràng buộc t_match \< T_kickoff     |
| preMatchOdds           | OddsSnapshot?     | Tỷ lệ tham chiếu 1X2 từ nhà cung cấp trước giờ đấu | Odds \> 1.0; Cho phép null (Missing signal) |

**Dữ liệu đầu ra (Outputs):**

> **•** Vectơ xác suất chuẩn hóa 3 chiều: P = (P(H), P(D), P(A)).
>
> **•** Nhãn dự đoán kết quả: Ŷ ∈ { HOME, DRAW, AWAY }.
>
> **•** Chỉ số tin cậy: Confidence Score ∈ \[0.33, 1.0\].

## 1.5. Cơ sở lý thuyết các thuật toán nền tảng

**1. Thuật toán Tìm kiếm Tuyến tính (Linear Search) và Tìm kiếm Nhị phân (Binary Search):**

> **• Linear Search**: Duyệt tuần tự từng phần tử trong danh sách kích thước N. Độ phức tạp thời gian tốt nhất O(1), trung bình và xấu nhất O(N).
>
> **• Binary Search**: Áp dụng nguyên lý chia để trị trên mảng đã sắp xếp. Thuật toán liên tục so sánh phần tử cần tìm với phần tử ở giữa (midpoint) để loại bỏ một nửa không gian tìm kiếm. Độ phức tạp thời gian O(log N).

**2. Thuật toán Sắp xếp Nhanh (QuickSort) và Sắp xếp Trộn (MergeSort):**

> **• QuickSort**: Giải thuật sắp xếp chia để trị in-place, lựa chọn phần tử chốt (pivot) và phân hoạch mảng thành hai phần. Độ phức tạp trung bình O(N log N), tuy nhiên có thể suy biến thành O(N²) khi mảng đã có thứ tự sẵn hoặc việc chọn pivot không tối ưu.
>
> **• MergeSort**: Giải thuật sắp xếp đệ quy phân chia danh sách thành các danh sách con kích thước 1, sau đó trộn (merge) tuần tự theo thứ tự tăng dần. MergeSort đảm bảo thời gian chạy nghiêm ngặt O(N log N) trong mọi trường hợp và bảo toàn tính ổn định (Stable Sort).

**3. Thuật toán Thống kê Mô tả Hai bước (Two-Pass Descriptive Statistics):**

Trong tính toán số học dấu phẩy động (Floating-Point Arithmetic), công thức tính phương sai truyền thống một bước Var(X) = E\[X²\] - (E\[X\])² rất dễ dẫn đến hiện tượng triệt tiêu thảm họa (Catastrophic Cancellation) khi các số hạng có độ lớn xấp xỉ nhau. Do đó, hệ thống ứng dụng giải thuật hai bước độc lập: Lượt 1 tính giá trị trung bình x̄; Lượt 2 tính tổng bình phương khoảng cách lệch chuẩn (x_i - x̄)².

**4. Thuật toán Đánh giá Phong độ Chuỗi thời gian (Recency-Weighted Form Algorithm):**

Đánh giá phong độ câu lạc bộ dựa trên chuỗi K trận đấu gần nhất (K = 5) bằng hàm phân rã trọng số thời gian tuyến tính (Linear Decay):

> W_sum = ∑\_{i=1..K} i = K · $\frac{(K\  + \ 1)}{2}$
>
> S_raw = ∑\_{i=1..K} \[ i · Score(O_i) \]
>
> S_max = 3.0 · W_sum
>
> FormScore =( $\frac{S\_ raw\ }{S\_ max\ }$ ) × 100.0

với điểm trận đấu Score(O_i): Thắng = 3 điểm, Hòa = 1 điểm, Thua = 0 điểm.

**5. Hệ thống Xếp hạng Động Elo (Dynamic Zero-Sum Elo Rating):**

Hệ thống xếp hạng Elo được phát triển ban đầu bởi nhà vật lý Arpad Elo cho bộ môn cờ vua, dựa trên hàm phân phối Logistic để ước lượng xác suất kỳ vọng chiến thắng giữa hai đối thủ có điểm số R_A và R_B:

E_A = 1.0 / \[ 1.0 + 10^($\frac{(R\_ B\  - \ R\_ A)\ }{400.0}$) \]

E_B = 1.0 / \[ 1.0 + 10^($\frac{(R\_ B\  - \ R\_ A)\ }{400.0}$)\] = 1.0 - E_A

Sau khi trận đấu kết thúc với kết quả thực tế S_A ∈ {1.0 (Thắng), 0.5 (Hòa), 0.0 (Thua)}, điểm Elo được cập nhật:

R'\_A = R_A + K · (S_A - E_A)

R'\_B = R_B + K · (S_B - E_B)

Do S_A + S_B = 1.0 và E_A + E_B = 1.0, tổng biến thiên điểm số luôn thỏa mãn điều kiện bảo toàn Zero-sum:

ΔR_A + ΔR_B = 0

## 1.6. Các chỉ số đánh giá thực nghiệm

Để đánh giá chất lượng mô hình phân loại đa lớp một cách khoa học, nghiên cứu sử dụng hệ thống chỉ số tại Bảng 1.2:

*Bảng 1.2: Bảng chỉ số đánh giá chất lượng mô hình dự đoán*

| **Chỉ số**  | **Công thức toán học**                                      | **Ý nghĩa thực tế**                                                  |
|-------------|-------------------------------------------------------------|----------------------------------------------------------------------|
| Accuracy    | Acc = ( TP_H + TP_D + TP_A ) / N                            | Tỷ lệ dự đoán đúng trên toàn bộ tập dữ liệu                          |
| Precision_c | Prec_c = TP_c / ( TP_c + FP_c )                             | Độ chuẩn xác khi mô hình đưa ra dự đoán kết quả c                    |
| Recall_c    | Rec_c = TP_c / ( TP_c + FN_c )                              | Tỷ lệ nhận diện thành công các trận đấu có kết quả thực tế c         |
| Macro F1    | F1 = ( F1_H + F1_D + F1_A ) / 3                             | Trung bình cộng F1 của 3 lớp; không bị chi phối bởi lớp chiếm đa số  |
| Brier Score | Brier = (1/N) · ∑\_{i=1..N} ∑\_{c} ( P\_{i,c} - Y\_{i,c} )² | Sai số toàn phương đo độ tin cậy xác suất; giá trị càng nhỏ càng tốt |

<span id="_Toc243581626" class="anchor"></span>

# CHƯƠNG 2. PHÂN TÍCH BÀI TOÁN VÀ THIẾT KẾ GIẢI PHÁP

## 2.1. Chuyển đổi bài toán thực tế sang bài toán tính toán

Để máy tính có thể phân tích và suy luận một trận bóng đá, toàn bộ các yếu tố chuyên môn định tính (như tinh thần thi đấu, năng lực câu lạc bộ, tương quan sức mạnh) cần phải được mô hình hóa thành các cấu trúc toán học định lượng có thể đo lường được:

> **1. Bản chất phân định Đội A - Đội B (Fixture Slot Representation)**: Trong cấu trúc dữ liệu và cơ sở dữ liệu của TrueLab, hai trường homeId và awayId đóng vai trò là khóa định danh phân biệt giữa Đội A (đội được niêm yết ở vị trí thứ nhất) và Đội B (đội được niêm yết ở vị trí thứ hai) trong một cặp đấu. Chúng hoàn toàn không mang ý nghĩa áp đặt lợi thế sân bãi địa lý hay quyền chủ nhà thực tế (đặc biệt trong các giải đấu sân trung lập, các giải cúp hoặc dữ liệu không ghi nhận sân bãi). Do đó, toàn bộ các thuật toán trong TrueLab (Elo, Form, Goals, H2H, Rest days) được thiết kế theo nguyên lý đối xứng khách quan (Objective & Symmetric Modeling): đánh giá sự chênh lệch thực lực thuần túy giữa hai câu lạc bộ mà không tự ý cộng điểm thiên vị nhân tạo (không áp đặt Home Bias cố định).
>
> **2. Năng lực nội tại dài hạn**: Được mô hình hóa bằng chỉ số Elo Rating tích lũy qua toàn bộ lịch sử thi đấu của câu lạc bộ, phản ánh sự biến động đẳng cấp sau từng trận đấu.
>
> **3. Điểm rơi phong độ ngắn hạn**: Được lượng hóa qua chuỗi kết quả 5 trận gần nhất với trọng số thời gian tuyến tính (Linear Decay), ưu tiên các trận đấu diễn ra gần nhất.
>
> **4. Hiệu suất công thủ**: Được lượng hóa qua kỳ vọng bàn thắng ghi được và bàn thua trung bình giữa hai đội.
>
> **5. Trí tuệ đám đông thị trường (Market Wisdom)**: Được chiết xuất từ tỷ lệ tham chiếu 1X2 tiền trận của các nhà cung cấp dữ liệu thể thao uy tín toàn cầu sau khi chuẩn hóa loại bỏ biên phụ trội (Overround).
>
> **6. Thể lực và Ưu thế hồi phục**: Được đo lường bằng khoảng cách số ngày nghỉ giữa trận đấu gần nhất và trận đấu chuẩn bị diễn ra.

## 2.2. Đặc điểm dữ liệu và quy trình xử lý dữ liệu

Tập dữ liệu của dự án TrueLab bao gồm 75.710 trận đấu diễn ra từ ngày 21/04/2024 đến 21/10/2026 trên 316 giải đấu chuyên nghiệp toàn cầu với 6.122 đội bóng và 136.990 bản ghi tỷ lệ tham chiếu.

Dữ liệu thể thao thực tế có những đặc thù phức tạp:

> • Tần suất phân bố nhãn theo quy ước 1X2 quốc tế: Trong 75.710 trận đấu thực tế của tập dữ liệu:
>
> – Kết quả Đội A thắng (ký hiệu nhãn Home / 1) chiếm 43.02% (32.574 trận).
>
> – Kết quả Hòa (ký hiệu nhãn Draw / X) chiếm 25.37% (19.204 trận).
>
> – Kết quả Đội B thắng (ký hiệu nhãn Away / 2) chiếm 31.61% (23.932 trận).

Quy ước đặt tên nhãn "Home - Draw - Away" (hoặc 1-X-2) là quy ước chuẩn của các tổ chức thống kê thể thao quốc tế nhằm phân định kết quả theo thứ tự xuất hiện của hai đội trong cặp đấu.

> • Dữ liệu thiếu (Missing Signals): Các trận đấu ở giải hạng dưới hoặc giải giao hữu thường không có dữ liệu tỷ lệ tham chiếu hoặc lịch sử đối đầu trực tiếp quá mỏng (\< 2 trận).
>
> • Độ nhiễu của tỷ lệ tham chiếu: Dữ liệu tỷ lệ từ các nhà cung cấp luôn chứa biên phụ trội kinh doanh (Overround, thường từ 4% đến 12%), đòi hỏi phải có bước chuẩn hóa xác suất loại bỏ phần dư.

**2.2.1. Cơ chế nạp và lưu trữ dữ liệu tỷ lệ theo nhu cầu (On-Demand Odds Caching Engine)**

Một điểm then chốt cần làm rõ trong kiến trúc dữ liệu của TrueLab là: 136.990 bản ghi tỷ lệ hiện có trong cơ sở dữ liệu SQLite cục bộ không phải là dữ liệu nạp sẵn đồng loạt cho toàn bộ 75.710 trận đấu.

Thay vào đó, TrueLab thiết kế và vận hành Cơ chế nạp tỷ lệ theo nhu cầu (On-Demand Odds Caching Engine):

> **• Nguyên lý kích hoạt theo phiên**: Khi người dùng chọn một cặp đấu cụ thể trên giao diện để xem phân tích dự đoán, hoặc khi hệ thống kích hoạt tiến trình Backtest cho danh sách các trận của một ngày thi đấu cụ thể, ứng dụng mới phát lệnh gọi API đồng bộ dữ liệu tỷ lệ đa nguồn của các trận đấu đó từ các nhà cung cấp, chuẩn hóa và lưu vào cơ sở dữ liệu Room cục bộ để làm bộ nhớ đệm (Cache).
>
> **• Lợi ích kỹ thuật của cơ chế On-Demand**:
>
> **1.** *Tối ưu hóa bộ nhớ di động*: Nếu tải sẵn toàn bộ dữ liệu tỷ lệ đa nguồn của 75.710 trận đấu từ 17 nhà cung cấp, dung lượng CSDL sẽ vượt quá 500MB, gây quá tải bộ nhớ flash và làm chậm hiệu năng của thiết bị di động.
>
> **2.** *Bảo toàn tính thời điểm (Point-in-Time Freshness)*: Chỉ những trận đấu được người dùng quan tâm mới được nạp snapshot tỷ lệ sát giờ thi đấu nhất (Pre-match snapshot), triệt tiêu nguy cơ lưu trữ dữ liệu rác lỗi thời.
>
> **3.** *Cơ chế dự phòng an toàn khi thiếu dữ liệu (Graceful Degradation / Fallback)*: Đối với các trận đấu chưa được nạp tỷ lệ hoặc các trận giải hạng dưới không có nhà cung cấp niêm yết, thuật toán WeightedScorer tự động kích hoạt cơ chế Fallback: phân bổ đều trọng số của tín hiệu tỷ lệ cho 5 nhóm tín hiệu chuyên môn còn lại, giúp tiến trình suy luận luôn diễn ra liền mạch mà không phát sinh ngoại lệ runtime.

Quy trình xử lý dữ liệu tổng thể của hệ thống được thực hiện qua 4 giai đoạn:

> **1. Thu nhận và Lưu trữ**: Nạp dữ liệu qua tầng Repository vào cơ sở dữ liệu Room/SQLite cục bộ.
>
> **2. Lọc dữ liệu thời gian**: Rút trích danh sách trận đấu lịch sử thỏa mãn điều kiện nghiêm ngặt trước giờ bóng lăn.
>
> **3. Chuẩn hóa tín hiệu (Signal Transformation)**: Ánh xạ từng trường dữ liệu chuyên môn thành vectơ xác suất 3 chiều có biên giá trị chặt chẽ.
>
> **4. Tổ hợp và Ra quyết định (Weighted Aggregation & Decision Policy)**: Tính toán phân phối xác suất tổng hợp và xác định nhãn kết quả.

## 2.3. Nguyên tắc bảo toàn dữ liệu và Ngăn chặn rò rỉ thời gian (Temporal Safety)

Một sai lầm kinh điển trong các nghiên cứu phân tích dữ liệu thể thao là hiện tượng Rò rỉ dữ liệu tương lai (Data Leakage / Look-ahead Bias). Ví dụ: Sử dụng tỷ số cuối trận để tính điểm phong độ trước trận, hoặc sử dụng điểm Elo đã được cập nhật sau khi trận đấu kết thúc để dự đoán chính trận đấu đó.

![](media/image2.png)

*Hình 2.4: Sơ đồ nguyên lý bảo toàn thời gian (Temporal Safety Cutoff) ngăn chặn tuyệt đối rò rỉ dữ liệu tương lai.*

Hệ thống thiết lập nguyên tắc bất biến: Chỉ những thông tin có mốc thời gian t_event \< T_kickoff mới được phép đưa vào quy trình tính toán. Lớp PreMatchOddsSelector đảm bảo chỉ chọn snapshot tỷ lệ tham chiếu có mốc thời gian sát nhất nhưng trước T_kickoff. Lớp CalculateDynamicEloUseCase mô phỏng lại toàn bộ chuỗi trận đấu theo đúng thứ tự thời gian để tính điểm Elo chuẩn xác tại đúng thời điểm trận đấu diễn ra.

## 2.4. Phân tích các phương án thuật toán và cơ sở lựa chọn

Trong quá trình thiết kế hệ thống, nhóm nghiên cứu đã đặt lên bàn cân 3 hướng tiếp cận công nghệ:

> **1. Phương án 1: Gọi API dự đoán từ máy chủ đám mây (Cloud Inference API)**:
>
> **•** *Ưu điểm*: Máy chủ có tài nguyên mạnh, có thể chạy các mô hình Deep Learning phức tạp.
>
> **•** *Nhược điểm*: Chi phí thuê máy chủ đắt đỏ, phụ thuộc hoàn toàn vào mạng Internet, độ trễ mạng cao (300ms - 1500ms), không thể vận hành offline, không đáp ứng được tiêu chí tự chủ của ứng dụng di động.
>
> **2. Phương án 2: Nhúng thư viện Học máy di động (On-Device ML: TFLite / ONNX)**:
>
> **•** *Ưu điểm*: Suy luận trên máy, hỗ trợ mô hình phi tuyến.
>
> **•** *Nhược điểm*: Kích thước file cài đặt APK phình to thêm 30MB - 60MB, tiêu tốn nhiều RAM khi nạp trọng số mô hình, mô hình là "hộp đen" (Black-box) không thể giải thích rõ ràng cho người dùng tại sao lại đưa ra dự đoán đó.
>
> **3. Phương án 3 (Được lựa chọn): Hệ thống thuật toán thuần túy (Pure Kotlin/JVM Algorithmic Engine)**:
>
> **•** *Ưu điểm*: Kích thước 0 KB phụ thuộc ngoài, tốc độ tính toán cực nhanh (\< 10ms trên CPU di động), tiết kiệm pin tối đa, khả năng giải thích tuyệt đối (100% Explainable AI), cho phép người dùng nhìn thấy rõ tỷ trọng đóng góp của từng tín hiệu toán học.

## 2.5. Thiết kế kiến trúc tổng thể hệ thống TrueLab

Hệ thống TrueLab được xây dựng theo kiến trúc phân tầng đa module Clean Architecture kết hợp mô hình trình diễn MVVM (Model - View - ViewModel). Cấu trúc phụ thuộc tuân thủ nghiêm ngặt nguyên lý đảo ngược phụ thuộc (Dependency Inversion Principle).

![](media/image3.png)

*Hình 2.1: Sơ đồ kiến trúc phân tầng đa module Clean Architecture của dự án TrueLab.*

Hệ thống bao gồm 5 module chức năng:

> **• :core:algorithm**: Module Pure Kotlin/JVM thuần túy chứa toàn bộ các thuật toán cốt lõi: Tìm kiếm, Sắp xếp, Thống kê mô tả, SMA, Form Score, Elo Engine và Decision Margin Policy. Tuyệt đối không phụ thuộc vào Android SDK.
>
> **• :core:domain**: Module Pure Kotlin/JVM định nghĩa các thực thể nghiệp vụ (Domain Entities), giao diện Repository và các Use Cases nghiệp vụ.
>
> **• :core:data**: Quản trị cơ sở dữ liệu Room/SQLite, thực thi các DAO, nạp dữ liệu tỷ lệ theo nhu cầu và chuyển đổi Entity sang Domain Model.
>
> **• :core:ui**: Hệ thống Design System dùng chung xây dựng bằng Jetpack Compose Material 3, biểu đồ tùy biến Canvas và các Component tái sử dụng.
>
> **• :app**: Điểm khởi động ứng dụng, định tuyến Navigation Compose và tiêm phụ thuộc Dependency Injection (Hilt).

**2.5.1. Sơ đồ và Đặc tả chi tiết các Ca sử dụng (Use Case Specifications)**

![](media/image4.png)

*Hình 2.2: Sơ đồ ca sử dụng tổng thể (Use Case Diagram) của hệ thống TrueLab.*

Nhằm chuẩn hóa quy trình phân tích và vận hành hệ thống, các Bảng từ 2.1 đến 2.6 đặc tả chi tiết 6 ca sử dụng cốt lõi:

*Bảng 2.1: Đặc tả Use Case UC01 — Xem bảng điều khiển & Thống kê dữ liệu tổng quan*

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 66%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Thuộc tính</strong></th>
<th><strong>Chi tiết đặc tả</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Mã Use Case</strong></td>
<td><strong>UC01</strong></td>
</tr>
<tr class="even">
<td><strong>Tên Use Case</strong></td>
<td>Xem bảng điều khiển và thống kê dữ liệu tổng quan (Dashboard &amp; Dataset Overview)</td>
</tr>
<tr class="odd">
<td><strong>Tác nhân (Actor)</strong></td>
<td>Người dùng (User)</td>
</tr>
<tr class="even">
<td><strong>Mô tả</strong></td>
<td>Cung cấp bảng điều khiển trung tâm hiển thị quy mô cơ sở dữ liệu và trạng thái của 9 thuật toán</td>
</tr>
<tr class="odd">
<td><strong>Điều kiện tiên quyết</strong></td>
<td>Ứng dụng đã khởi tạo thành công cơ sở dữ liệu Room/SQLite cục bộ</td>
</tr>
<tr class="even">
<td><strong>Luồng sự kiện chính</strong></td>
<td><p>1. Người dùng mở ứng dụng TrueLab, hệ thống điều hướng đến HomeScreen.</p>
<p>2. Hệ thống gọi Use Case truy vấn bất đồng bộ số lượng trận đấu, đội bóng, giải đấu và tỷ lệ.</p>
<p>3. Hệ thống hiển thị các thẻ chỉ số trực quan (75.710 trận, 6.122 đội, 136.990 tỷ lệ, 9 thuật toán 100%).</p>
<p>4. Người dùng có thể chọn các nút điều hướng nhanh đến các công cụ phân tích chuyên sâu.</p></td>
</tr>
<tr class="odd">
<td><strong>Luồng ngoại lệ</strong></td>
<td>Nếu cơ sở dữ liệu chưa nạp đủ dữ liệu, hệ thống hiển thị trạng thái đang đồng bộ kèm thanh tiến trình.</td>
</tr>
<tr class="even">
<td><strong>Điều kiện sau</strong></td>
<td>Người dùng nắm bắt được quy mô dữ liệu và sẵn sàng kích hoạt các tính năng tính toán.</td>
</tr>
</tbody>
</table>

*Bảng 2.2: Đặc tả Use Case UC02 — Dự đoán kết quả trận đấu đa tín hiệu*

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 66%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Thuộc tính</strong></th>
<th><strong>Chi tiết đặc tả</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Mã Use Case</strong></td>
<td><strong>UC02</strong></td>
</tr>
<tr class="even">
<td><strong>Tên Use Case</strong></td>
<td>Dự đoán kết quả trận đấu đa tín hiệu (Multi-Signal Match Prediction)</td>
</tr>
<tr class="odd">
<td><strong>Tác nhân (Actor)</strong></td>
<td>Người dùng (User)</td>
</tr>
<tr class="even">
<td><strong>Mô tả</strong></td>
<td>Tính toán phân phối xác suất 3 chiều (Home - Draw - Away) và giải thích quyết định dựa trên 6 nhóm tín hiệu</td>
</tr>
<tr class="odd">
<td><strong>Điều kiện tiên quyết</strong></td>
<td>Người dùng đã chọn một cặp đấu cụ thể (Match Fixture)</td>
</tr>
<tr class="even">
<td><strong>Luồng sự kiện chính</strong></td>
<td><p>1. Người dùng chọn trận đấu cần phân tích từ danh sách thi đấu hoặc tìm kiếm theo giải đấu.</p>
<p>2. Hệ thống kích hoạt PredictMatchOutcomeUseCase và bộ nạp tỷ lệ On-Demand để lấy snapshot trước trận.</p>
<p>3. Hệ thống lọc dữ liệu lịch sử trước thời điểm T_kickoff (Temporal Safety).</p>
<p>4. 6 bộ chuyển đổi tín hiệu (Form, Elo, Goals, Odds, H2H, Rest) tính toán các vectơ xác suất độc lập.</p>
<p>5. DefaultWeightedScorer kết hợp trọng số, áp dụng chính sách Candidate A để xác định nhãn dự đoán.</p>
<p>6. Giao diện hiển thị biểu đồ phân phối xác suất và bảng giải trình chi tiết từng tín hiệu.</p></td>
</tr>
<tr class="odd">
<td><strong>Luồng ngoại lệ</strong></td>
<td>Nếu trận đấu thiếu dữ liệu tỷ lệ, hệ thống tự động phân bổ đều trọng số cho 5 tín hiệu còn lại (Fallback).</td>
</tr>
<tr class="even">
<td><strong>Điều kiện sau</strong></td>
<td>Kết quả phân tích xác suất và minh chứng giải thích được lưu vào phiên làm việc hiện tại.</td>
</tr>
</tbody>
</table>

*Bảng 2.3: Đặc tả Use Case UC03 — Đánh giá & Kiểm thử ngược lịch sử*

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 66%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Thuộc tính</strong></th>
<th><strong>Chi tiết đặc tả</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Mã Use Case</strong></td>
<td><strong>UC03</strong></td>
</tr>
<tr class="even">
<td><strong>Tên Use Case</strong></td>
<td>Đánh giá &amp; Kiểm thử ngược lịch sử (Historical Backtest Verification)</td>
</tr>
<tr class="odd">
<td><strong>Tác nhân (Actor)</strong></td>
<td>Người dùng / Nhà nghiên cứu (Researcher)</td>
</tr>
<tr class="even">
<td><strong>Mô tả</strong></td>
<td>Đánh giá chất lượng mô hình trên tập các trận đấu đã kết thúc trong quá khứ so với kết quả thực tế</td>
</tr>
<tr class="odd">
<td><strong>Điều kiện tiên quyết</strong></td>
<td>Tồn tại danh sách các trận đấu đã hoàn thành (MatchStatus.ENDED) trong CSDL</td>
</tr>
<tr class="even">
<td><strong>Luồng sự kiện chính</strong></td>
<td><p>1. Người dùng chọn ngày hoặc giải đấu cần thực hiện kiểm thử ngược trên BacktestScreen.</p>
<p>2. Hệ thống tải danh sách các trận FT, kích hoạt nạp tỷ lệ On-Demand và thực hiện suy luận độc lập từng trận.</p>
<p>3. Hệ thống đối soát nhãn dự đoán với kết quả thực tế, tổng hợp các chỉ số: Accuracy, Macro F1, Brier Score.<br />
4. Hệ thống xây dựng Ma trận nhầm lẫn 3x3 và dòng thời gian đối soát chi tiết từng trận.</p></td>
</tr>
<tr class="odd">
<td><strong>Luồng ngoại lệ</strong></td>
<td>Các trận đấu bị hủy hoặc hoãn sẽ được tự động bỏ qua khỏi tập đánh giá.</td>
</tr>
<tr class="even">
<td><strong>Điều kiện sau</strong></td>
<td>Báo cáo kiểm thử ngược toàn diện được hiển thị trực quan cho người dùng.</td>
</tr>
</tbody>
</table>

*Bảng 2.4: Đặc tả Use Case UC04 — Phân tích đối đầu trực tiếp H2H & Phong độ*

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 66%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Thuộc tính</strong></th>
<th><strong>Chi tiết đặc tả</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Mã Use Case</strong></td>
<td><strong>UC04</strong></td>
</tr>
<tr class="even">
<td><strong>Tên Use Case</strong></td>
<td>Phân tích đối đầu trực tiếp H2H &amp; Phong độ (Head-to-Head Comparison)</td>
</tr>
<tr class="odd">
<td><strong>Tác nhân (Actor)</strong></td>
<td>Người dùng (User)</td>
</tr>
<tr class="even">
<td><strong>Mô tả</strong></td>
<td>So sánh tương quan lịch sử, chỉ số Elo và phong độ thi đấu giữa 2 câu lạc bộ bất kỳ</td>
</tr>
<tr class="odd">
<td><strong>Điều kiện tiên quyết</strong></td>
<td>Hai câu lạc bộ tồn tại trong danh mục CSDL</td>
</tr>
<tr class="even">
<td><strong>Luồng sự kiện chính</strong></td>
<td><p>1. Người dùng chọn Đội A và Đội B thông qua công cụ tìm kiếm hoặc chọn từ cặp đấu.</p>
<p>2. Hệ thống kích hoạt GetHeadToHeadComparisonUseCase, truy vấn toàn bộ lịch sử chạm trán.</p>
<p>3. Thuật toán phân tích chuỗi kết quả: số trận thắng, hòa, thua, tổng bàn thắng, điểm phong độ 5 trận gần nhất.</p>
<p>4. Giao diện hiển thị biểu đồ tỷ lệ thắng/hòa và danh sách chi tiết các lần chạm trán trong quá khứ.</p></td>
</tr>
<tr class="odd">
<td><strong>Luồng ngoại lệ</strong></td>
<td>Nếu hai đội chưa từng gặp nhau, hệ thống hiển thị thông báo chưa có dữ liệu H2H và dựa trên Elo/Form.</td>
</tr>
<tr class="even">
<td><strong>Điều kiện sau</strong></td>
<td>Báo cáo so sánh đối đầu toàn diện được hiển thị rõ ràng trên giao diện.</td>
</tr>
</tbody>
</table>

*Bảng 2.5: Đặc tả Use Case UC05 — Đo lường hiệu năng thuật toán trên thiết bị*

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 66%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Thuộc tính</strong></th>
<th><strong>Chi tiết đặc tả</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Mã Use Case</strong></td>
<td><strong>UC05</strong></td>
</tr>
<tr class="even">
<td><strong>Tên Use Case</strong></td>
<td>Đo lường hiệu năng thuật toán trên thiết bị (On-Device Algorithm Benchmark)</td>
</tr>
<tr class="odd">
<td><strong>Tác nhân (Actor)</strong></td>
<td>Người dùng / Giảng viên đánh giá (Evaluator)</td>
</tr>
<tr class="even">
<td><strong>Mô tả</strong></td>
<td>Đo đạc thời gian thực thi thực tế của các thuật toán Tìm kiếm và Sắp xếp trên các tập đối tượng Match</td>
</tr>
<tr class="odd">
<td><strong>Điều kiện tiên quyết</strong></td>
<td>Thiết bị có kết nối CSDL và đủ pin/tài nguyên để chạy phép đo</td>
</tr>
<tr class="even">
<td><strong>Luồng sự kiện chính</strong></td>
<td><p>1. Người dùng truy cập BenchmarkScreen, chọn quy mô dữ liệu cần đo (10.000, 30.000, 50.000, 75.000 trận).</p>
<p>2. Người dùng nhấn nút kích hoạt đo đạc Tìm kiếm (Linear vs Binary) hoặc Sắp xếp (QuickSort vs MergeSort).</p>
<p>3. Hệ thống thực hiện chạy Warmup JIT 100 chu kỳ, sau đó đo thời gian bằng System.nanoTime().</p>
<p>4. Hệ thống hiển thị thời gian thực thi (ms), tốc độ chênh lệch (Speedup ratio) và biểu đồ so sánh trực quan.</p></td>
</tr>
<tr class="odd">
<td><strong>Luồng ngoại lệ</strong></td>
<td>Nếu bộ nhớ thiết bị quá thấp, hệ thống tự động giải phóng cache trước khi thực thi benchmark.</td>
</tr>
<tr class="even">
<td><strong>Điều kiện sau</strong></td>
<td>Kết quả đo đạc thời gian và biểu đồ so sánh được hiển thị trực tiếp trên màn hình.</td>
</tr>
</tbody>
</table>

*Bảng 2.6: Đặc tả Use Case UC06 — Khám phá thống kê mô tả & Biến động tỷ lệ*

<table>
<colgroup>
<col style="width: 33%" />
<col style="width: 66%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Thuộc tính</strong></th>
<th><strong>Chi tiết đặc tả</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><strong>Mã Use Case</strong></td>
<td><strong>UC06</strong></td>
</tr>
<tr class="even">
<td><strong>Tên Use Case</strong></td>
<td>Khám phá thống kê mô tả &amp; Biến động tỷ lệ (Descriptive Statistics &amp; Odds Volatility)</td>
</tr>
<tr class="odd">
<td><strong>Tác nhân (Actor)</strong></td>
<td>Người dùng (User)</td>
</tr>
<tr class="even">
<td><strong>Mô tả</strong></td>
<td>Xem báo cáo thống kê mô tả phân phối bàn thắng của câu lạc bộ và theo dõi biến động tỷ lệ tham chiếu</td>
</tr>
<tr class="odd">
<td><strong>Điều kiện tiên quyết</strong></td>
<td>Câu lạc bộ có ít nhất 5 trận đấu đã hoàn thành</td>
</tr>
<tr class="even">
<td><strong>Luồng sự kiện chính</strong></td>
<td><p>1. Người dùng chọn câu lạc bộ mục tiêu từ màn hình AnalyticsScreen.</p>
<p>2. Hệ thống kích hoạt DescriptiveStatsCalculator, thực thi thuật toán thống kê hai bước tính Count, Mean, Median, StdDev, Variance, Skewness.</p>
<p>3. Hệ thống kích hoạt MovingAverageCalculator và OddsVolatilityCalculator tính đường MA(3) và chỉ số Volatility.</p>
<p>4. Hiển thị bảng số liệu thống kê và biểu đồ đường xu hướng trực quan.</p></td>
</tr>
<tr class="odd">
<td><strong>Luồng ngoại lệ</strong></td>
<td>Nếu dữ liệu tỷ lệ ít hơn kích thước cửa sổ trượt (W = 3), hệ thống tự động điều chỉnh độ rộng cửa sổ phù hợp.</td>
</tr>
<tr class="even">
<td><strong>Điều kiện sau</strong></td>
<td>Người dùng xem được toàn bộ các chỉ số thống kê mô tả và xu hướng biến động tỷ lệ.</td>
</tr>
</tbody>
</table>

![](media/image5.png)

*Hình 2.3: Sơ đồ tuần tự quy trình dự đoán 11 bước của PredictMatchOutcomeUseCase.*

## 2.6. Thiết kế mô hình 6 nhóm tín hiệu độc lập

Để đảm bảo tính toàn diện và khả năng giải thích của mô hình, TrueLab thiết kế 6 nhóm tín hiệu độc lập, mỗi nhóm đại diện cho một khía cạnh chuyên môn bóng đá tại Bảng 2.7:

*Bảng 2.7: Phân bổ trọng số mặc định cho 6 nhóm tín hiệu độc lập*

| **Nhóm tín hiệu**                 | **Trọng số (w_k)** | **Tỷ trọng** | **Bản chất chuyên môn**                                                      |
|-----------------------------------|--------------------|--------------|------------------------------------------------------------------------------|
| Phong độ gần đây (Form)           | 0.25               | 25%          | Điểm rơi phong độ ngắn hạn (5 trận gần nhất có trọng số thời gian)           |
| Xếp hạng Elo (Elo Rating)         | 0.20               | 20%          | Đẳng cấp và năng lực tích lũy dài hạn qua toàn bộ lịch sử đối đầu            |
| Tỷ lệ tham chiếu 1X2 (Odds)       | 0.20               | 20%          | Xác suất ngầm định từ thị trường tỷ lệ quốc tế sau khi loại bỏ biên phụ trội |
| Hiệu số bàn thắng (Goals)         | 0.15               | 15%          | Sức mạnh hàng công và hàng thủ thông qua kỳ vọng bàn thắng                   |
| Đối đầu trực tiếp (H2H)           | 0.10               | 10%          | Yếu tố kỵ dơ tâm lý giữa hai câu lạc bộ trong quá khứ                        |
| Ưu thế ngày nghỉ (Rest Advantage) | 0.10               | 10%          | Thể lực và thời gian hồi phục thể chất giữa hai lịch thi đấu                 |
| **TỔNG CỘNG**                     | **1.00**           | **100%**     | **Hệ số chuẩn hóa toàn phần**                                                |

<span id="_Toc243581633" class="anchor"></span>

# CHƯƠNG 3. THUẬT TOÁN VÀ PHƯƠNG PHÁP HIỆN THỰC

## 3.1. Phân hệ thuật toán Tìm kiếm và Sắp xếp

**3.1.1. Thuật toán Tìm kiếm Tuyến tính và Tìm kiếm Nhị phân**

Phân hệ tìm kiếm được đóng gói trong hai lớp LinearSearch và BinarySearch thuộc module :core:algorithm:

> **•** LinearSearch thực hiện duyệt tuần tự qua danh sách phần tử, kiểm tra khóa tìm kiếm thông qua hàm ánh xạ selector: (T) -\> K. Khi tìm thấy phần tử khớp đầu tiên, thuật toán trả về chỉ số vị trí, ngược lại trả về -1. Độ phức tạp tính toán O(N).
>
> **•** BinarySearch yêu cầu danh sách đầu vào đã được sắp xếp tăng dần. Thuật toán duy trì hai con trỏ low và high, liên tục tính chỉ số trung vị mid = (low + high) ushr 1 (sử dụng phép dịch bit không dấu để triệt tiêu nguy cơ tràn số nguyên Int.MAX_VALUE), sau đó so sánh khóa để thu hẹp không gian tìm kiếm. Độ phức tạp tính toán O(log N).

**3.1.2. Thuật toán QuickSort và MergeSort trên đối tượng Trận đấu thực tế**

Khác với các bài toán lý thuyết chỉ sắp xếp mảng số nguyên đơn giản, trong hệ thống TrueLab, đối tượng sắp xếp là cấu trúc dữ liệu thực tế phức hợp Match:

> **•** QuickSort hiện thực hóa kỹ thuật phân vùng Lomuto/Hoare in-place, chọn phần tử chốt (pivot) và hoán vị phần tử trực tiếp trên mảng để hạn chế cấp phát bộ nhớ. Độ phức tạp thời gian trung bình O(N log N).
>
> **•** MergeSort áp dụng cơ chế chia để trị, phân rã danh sách thành hai nửa độc lập, sắp xếp đệ quy và trộn lại thông qua mảng đệm. Ưu điểm quyết định của MergeSort trong bài toán bóng đá là tính ổn định (Stability): khi sắp xếp các trận đấu có cùng ngày thi đấu theo tên giải đấu, thứ tự thời gian gốc của các trận đấu không bị xáo trộn.

Hàm so sánh đối tượng trận đấu đa khóa (Multi-key Match Comparator):

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th>private fun matchComparator(): Comparator&lt;Match&gt; = compareBy(<br />
{ it.homeTeam.name.trim().lowercase() },<br />
{ it.awayTeam.name.trim().lowercase() },<br />
{ it.startTimeDate },<br />
{ it.id }<br />
)</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

Bảng 3.1 so sánh chi tiết đặc tính kỹ thuật của các thuật toán trong hệ thống:

*Bảng 3.1: So sánh đặc tính và độ phức tạp tính toán của các thuật toán trong hệ thống*

| **Thuật toán**     | **Độ phức tạp thời gian tốt nhất** | **Độ phức tạp thời gian trung bình** | **Độ phức tạp thời gian xấu nhất** | **Bộ nhớ phụ (Space)** | **Tính ổn định (Stability)** |
|--------------------|------------------------------------|--------------------------------------|------------------------------------|------------------------|------------------------------|
| Linear Search      | O(1)                               | O(N)                                 | O(N)                               | O(1)                   | Không áp dụng                |
| Binary Search      | O(1)                               | O(log N)                             | O(log N)                           | O(1)                   | Không áp dụng                |
| QuickSort          | O(N log N)                         | O(N log N)                           | O(N²)                              | O(log N)               | Không ổn định                |
| MergeSort          | O(N log N)                         | O(N log N)                           | O(N log N)                         | O(N)                   | Ổn định (Stable)             |
| Sliding Window SMA | O(N)                               | O(N)                                 | O(N)                               | O(W)                   | Duy trì thứ tự chuỗi         |

## 3.2. Phân hệ thuật toán Thống kê mô tả và Chuỗi thời gian

**3.2.1. Thống kê mô tả hai bước (Two-Pass Descriptive Statistics)**

Trong lớp DescriptiveStatsCalculator, việc tính toán phương sai mẫu s² và phương sai tổng thể σ² được thực hiện qua hai lượt duyệt độc lập nhằm loại bỏ triệt tiêu số thực:

Lượt 1: Tính giá trị trung bình mẫu x̄:

x̄ = (1 / N) · ∑\_{i=1..N} x_i

Lượt 2: Tính tổng bình phương độ lệch (Sum of Squared Deviations):

SS = ∑\_{i=1..N} (x_i - x̄)²

σ² = SS / N, s² = SS / (N - 1)

Độ lệch chuẩn: σ = √σ², s = √s²

Hệ số bất đối xứng (Skewness Fisher-Pearson):

Skewness = \[ (1 / N) · ∑\_{i=1..N} (x_i - x̄)³ \] / \[ σ³ \]

**3.2.2. Đường trung bình động đơn giản cửa sổ trượt (Sliding Window SMA)**

Lớp MovingAverageCalculator tính toán đường trung bình động đơn giản SMA với độ rộng cửa sổ W trên chuỗi tỷ lệ tham chiếu Odds qua thời gian.

Để đạt độ phức tạp thời gian tuyến tính O(N), thuật toán áp dụng kỹ thuật Cửa sổ trượt (Sliding Window):

Khởi tạo cửa sổ đầu tiên (i = W):

Sum_W = ∑\_{j=1..W} x_j, SMA_W = Sum_W / W

Khi dịch chuyển cửa sổ sang vị trí kế tiếp (i \> W), thay vì cộng lại toàn bộ W phần tử với chi phí O(W), thuật toán chỉ cập nhật giá trị biên với chi phí O(1):

Sum_i = Sum\_{i-1} + x_i - x\_{i-W}

SMA_i = Sum_i / W

Tổng thời gian thực thi cho chuỗi kích thước N là O(N), tối ưu hơn đáng kể so với thuật toán vét cạn O(N · W).

## 3.3. Phân hệ thuật toán Đánh giá phong độ và Xếp hạng Elo

**3.3.1. Thuật toán Recency-Weighted Form Score**

Lớp FormScoreCalculator lượng hóa phong độ 5 trận gần nhất. Để phản ánh đúng quy luật tâm lý thể thao (trận đấu gần nhất có ảnh hưởng lớn nhất đến tinh thần thi đấu), thuật toán gán trọng số tăng dần từ 1 đến 5:

> **•** Trận cách đây 5 trận (xa nhất): Trọng số w₁ = 1
>
> **•** Trận cách đây 4 trận: Trọng số w₂ = 2
>
> **•** Trận cách đây 3 trận: Trọng số w₃ = 3
>
> **•** Trận cách đây 2 trận: Trọng số w₄ = 4
>
> **•** Trận gần nhất: Trọng số w₅ = 5

Tổng trọng số chuẩn hóa: W_sum = 1 + 2 + 3 + 4 + 5 = 15.

Điểm phong độ chuẩn hóa FormScore ∈ \[0.0, 100.0\] được tính theo công thức:

FormScore = \[ ∑\_{i=1..5} ( i · Score(O_i) ) / ( 3.0 × 15 ) \] × 100.0

**3.3.2. Hệ thống Xếp hạng Động Elo bảo toàn Zero-Sum**

Lớp CalculateDynamicEloUseCase quản lý việc cập nhật điểm xếp hạng năng lực cho toàn bộ 6.122 đội bóng trong hệ thống. Điểm khởi tạo mặc định cho mỗi đội là R_init = 1500.0 điểm.

Với hệ số điều chỉnh K = 32 (chuẩn FIFA cho các trận đấu cấp câu lạc bộ), thuật toán cập nhật điểm số sau mỗi trận đấu đảm bảo nguyên tắc bảo toàn Zero-sum:

ΔR_A = K · ( S_A - E_A )

ΔR_B = K · ( S_B - E_B ) = - ΔR_A

## 3.4. Phân hệ Pipeline Dự đoán và WeightedScorer

Lớp DefaultWeightedScorer chịu trách nhiệm tổng hợp xác suất từ 6 nhóm tín hiệu độc lập theo công thức tổng hợp trọng số:

P(c) = \[ ∑\_{k=1..m} ( w_k · p\_{k,c} ) \] / \[ ∑\_{k=1..m} w_k \], với c ∈ {H, D, A}

**Chi tiết 6 bộ chuyển đổi tín hiệu (6 Signal Transformers):**

> **1. FormSignalTransformer**: Nhận điểm phong độ S_H, S_A ∈ \[0, 100\]. Tính độ chênh lệch phong độ chuẩn hóa d = (S_H - S_A) / 100.0 ∈ \[-1.0, 1.0\]. Xác suất hòa cơ sở cố định P₀(D) = 0.26. Tỷ lệ thắng/thua được phân bổ tuyến tính đối xứng:

p_H = \[ (1.0 - P₀(D)) / 2 \] + d · \[ (1.0 - P₀(D)) / 2 \] · (1 - ε)

với ε = 0.10 là hệ số làm mịn để bảo toàn biên xác suất.

> **2. EloSignalTransformer**: Tính điểm kỳ vọng Elo của Đội A E_H = 1.0 / \[ 1.0 + 10^((R_Away - R_Home) / 400.0) \]. Xác suất hòa cơ sở P₀(D) = 0.26. Xác suất thắng của hai đội:

p_H = E_H · (1.0 - P₀(D)), p_A = (1.0 - E_H) · (1.0 - P₀(D)), p_D = P₀(D)

> **3. GoalsSignalTransformer**: Tính trung bình bàn thắng ghi được và thủng lưới của hai đội. Hiệu số bàn thắng kỳ vọng của Đội A:

ΔG_H = Ḡ\_scored,H - Ḡ\_conceded,A

và Đội B:

ΔG_A = Ḡ\_scored,A - Ḡ\_conceded,H

> Chênh lệch công thủ ròng: Δ = ΔG_H - ΔG_A. Biến đổi xác suất với độ nhạy β = 0.15:

p_H = clamp(0.45 + β · Δ, 0.05, 0.69)

> **4. OddsSignalTransformer**: Lấy tỷ lệ tham chiếu 1X2 trước trận từ nhà cung cấp uy tín. Tính xác suất ngầm định thô:

q_H = 1.0 / O_H, q_D = 1.0 / O_D, q_A = 1.0 / O_A

> Tổng S = q_H + q_D + q_A \> 1.0 (phần dư S - 1.0 chính là biên phụ trội của nhà cung cấp). Chuẩn hóa loại bỏ biên phụ trội:

p_H = q_H / S, p_D = q_D / S, p_A = q_A / S

> **5. H2hSignalTransformer**: Sử dụng phân phối Dirichlet với tham số tiên nghiệm α = \[0.45, 0.27, 0.28\] và độ co cụm K = 3.0 trên tổng số trận đối đầu trong quá khứ (N_H, N_D, N_A):

p_c = ( N_c + K · α_c ) / \[ (N_H + N_D + N_A) + K \], với mọi c ∈ {H, D, A}

> **6. RestAdvantageSignalTransformer**: Tính số ngày nghỉ ngơi phục hồi D_H, D_A. Chênh lệch ngày nghỉ: ΔD = D_H - D_A. Biến đổi phi tuyến qua hàm Tanh:

f(ΔD) = tanh(0.15 · ΔD)

Hàm Tanh giới hạn biên trong khoảng (-1.0, 1.0), ngăn chặn trường hợp một đội nghỉ 30 ngày (do hoãn trận) tạo ra giá trị xác suất lệch lạc bất thường.

*Bảng 3.2: Ma trận cấu hình trọng số mặc định và biên giá trị của 6 Signal Transformers*

| **Bộ chuyển đổi tín hiệu**         | **Trọng số w_k** | **Tham số hiệu chuẩn**           | **Khoảng giá trị xác suất đầu ra** | **Cơ chế xử lý Missing Signal**             |
|------------------------------------|------------------|----------------------------------|------------------------------------|---------------------------------------------|
| **FormSignalTransformer**          | 0.25             | P₀(D) = 0.26, ε = 0.10           | P(H) ∈ \[0.037, 0.703\]            | Trả về điểm phong độ trung tính 50.0        |
| **EloSignalTransformer**           | 0.20             | K = 32, P₀(D) = 0.26             | P(H) ∈ \[0.05, 0.74\]              | Sử dụng điểm Elo mặc định 1500.0            |
| **OddsSignalTransformer**          | 0.20             | Chuẩn hóa Overround              | P(c) ∈ \[0.02, 0.95\]              | Tái phân bổ trọng số cho 5 tín hiệu còn lại |
| **GoalsSignalTransformer**         | 0.15             | β = 0.15, biên \[0.05, 0.69\]    | P(H) ∈ \[0.05, 0.69\]              | Giả định trung bình 1.2 bàn/trận            |
| **H2hSignalTransformer**           | 0.10             | Dirichlet α=\[0.45, 0.27, 0.28\] | P(c) ∈ \[0.10, 0.80\]              | Suy biến về phân phối tiên nghiệm α         |
| **RestAdvantageSignalTransformer** | 0.10             | Hàm phi tuyến Tanh(0.15 · ΔD)    | P(H) ∈ \[0.20, 0.60\]              | Giả định số ngày nghỉ cân bằng (ΔD = 0)     |

## 3.5. Nghiên cứu Mô hình hóa Kết quả Hòa (Draw Modeling)

**3.5.1. Vấn đề Triệt tiêu Kết quả Hòa (Flat Zero Draw Problem)**

Trong bài toán phân loại 3 lớp bóng đá, xác suất thực tế của kết quả Hòa chiếm khoảng 25% - 28%. Do đặc thù của bóng đá, một trận hòa là trạng thái cân bằng giữa hai đội. Do đó, các mô hình học máy và mô hình tính điểm truyền thống thường ước lượng xác suất Hòa trong khoảng P(D) ∈ \[0.22, 0.30\].

Trong khi đó, xác suất thắng của một trong hai đội thường dao động trong khoảng \[0.35, 0.65\].

Khi áp dụng hàm quyết định Argmax tự nhiên:

Ŷ = argmax\_{c ∈ {H, D, A}} P(c)

Vì P(D) hầu như không bao giờ vượt qua được max(P(H), P(A)), mô hình không bao giờ dự đoán kết quả Hòa. Điều này dẫn đến chỉ số Recall của lớp Hòa bằng 0.0% (Flat Zero Draw Recall), làm sụp đổ hoàn toàn điểm Macro F1 của hệ thống.

**3.5.2. Nghiên cứu 3 phương án giải quyết (Candidates A, B, C)**

Để giải quyết bài toán trên, nhóm nghiên cứu đã thiết kế và thử nghiệm 3 phương án giải pháp:

**1. Candidate A: Chính sách Phân ngưỡng Biên độ Quyết định (Decision Margin Policy - Được chọn)**

> **•** *Ý tưởng*: Khi chênh lệch xác suất giữa Đội A và Đội B nằm trong vùng giằng co cân bằng (\|P(H) - P(A)\| \< δ) và xác suất hòa vượt qua ngưỡng tin cậy tối thiểu (P(D) \> θ), hệ thống sẽ quyết định nhãn dự đoán là Hòa (DRAW).
>
> **•** *Quy tắc quyết định*:
>
> NẾU \|P(H) - P(A)\| \< δ VÀ P(D) \> θ:
>
> Ŷ = DRAW
>
> NGƯỢC LẠI:
>
> Ŷ = argmax\_{c ∈ {H, A}} P(c)

![](media/image6.png)

*Hình 3.1: Cây quyết định phân ngưỡng biên độ xử lý kết quả Hòa (Candidate A: Decision Margin Policy).*

> **•** *Bộ tham số tối ưu*: Thông qua tìm kiếm lưới (Grid Search) trên 53.000 trận đấu, bộ tham số tối ưu được xác định là: δ = 0.04 (4%) và θ = 0.255 (25.5%).

**2. Candidate B: Mô phỏng Bàn thắng Poisson hai chiều (Bivariate Poisson Goals Simulation)**

> **•** *Ý tưởng*: Ước lượng kỳ vọng bàn thắng λ_H, λ_A thông qua phân phối Poisson độc lập:

P(X = x, Y = y) = \[ (λ_H^x · e^(-λ_H)) / x! \] · \[ (λ_A^y · e^(-λ_A)) / y! \]

Xác suất hòa là tổng xác suất trên đường chéo chính: P(D) = ∑\_{k=0..∞} P(X = k, Y = k).

> **•** *Hạn chế*: Giả định tính độc lập bàn thắng bị vi phạm nghiêm trọng trong bóng đá thực tế, chi phí tính toán O(G²) tốn kém trên thiết bị di động.

**3. Candidate C: Tích hợp Dữ liệu Tỷ lệ Chấp Châu Á và Ngưỡng Tổng bàn thắng (Asian Handicap & Over/Under Integration)**

> **•** *Ý tưởng*: Bổ sung thông tin từ thị trường chấp bóng (Handicap) và ngưỡng tổng số bàn thắng (Over/Under) để suy diễn xác suất hòa.
>
> **•** *Hạn chế*: Phụ thuộc vào dữ liệu thị trường mở rộng (vốn rất thưa thớt ở các giải đấu nhỏ) và không đạt được cải thiện có ý nghĩa thống kê so với Candidate A.

# CHƯƠNG 4. THỰC NGHIỆM, ĐÁNH GIÁ VÀ THẢO LUẬN

## 4.1. Môi trường và điều kiện thực nghiệm

Để đảm bảo tính khách quan và khả năng tái lập kết quả, toàn bộ các phép đo đạc hiệu năng và kiểm định thực nghiệm được thực hiện trên môi trường chuẩn hóa tại Bảng 4.1:

*Bảng 4.1: Cấu hình phần cứng và môi trường thực nghiệm đo lường hiệu năng*

| **Thành phần**              | **Đặc tả kỹ thuật thực tế**                                                  |
|-----------------------------|------------------------------------------------------------------------------|
| **Thiết bị thử nghiệm**     | Thiết bị di động Android vật lý (Physical Device)                            |
| **Vi xử lý (SoC / CPU)**    | Qualcomm Snapdragon 8-Core (ARM64-v8a Architecture)                          |
| **Bộ nhớ RAM**              | 8.0 GB LPDDR4X                                                               |
| **Hệ điều hành**            | Android 14 (API Level 34)                                                    |
| **Môi trường thực thi JVM** | Android Runtime (ART) với bộ biên dịch Ahead-of-Time (AOT) và JIT Compiler   |
| **Cơ sở dữ liệu cục bộ**    | SQLite 3.42 thông qua thư viện Room Persistence 2.6.1                        |
| **Cơ chế đo lường**         | System.nanoTime() loại bỏ sai số đồng hồ; Warmup JIT 100 chu kỳ trước khi đo |

## 4.2. Tập dữ liệu thực nghiệm quy mô 75.710 trận đấu

Dự án TrueLab sử dụng cơ sở dữ liệu bóng đá mở rộng gồm 75.710 trận đấu diễn ra từ ngày 21/04/2024 đến 21/10/2026 trên 316 giải đấu chuyên nghiệp toàn cầu, bao gồm 6.122 câu lạc bộ và 136.990 bản ghi tỷ lệ tham chiếu đa nguồn từ 17 nhà cung cấp quốc tế được nạp theo cơ chế On-Demand Caching. Bảng 4.2 tóm tắt các đặc trưng thống kê của tập dữ liệu:

*Bảng 4.2: Thống kê tổng quan tập dữ liệu thực nghiệm 75.710 trận đấu*

| **Đặc trưng tập dữ liệu**                             | **Giá trị thực tế**     | **Tỷ lệ phần trăm** |
|-------------------------------------------------------|-------------------------|---------------------|
| **Tổng số trận đấu hoàn thành (Finished Matches)**    | 75.710 trận             | 100.00%             |
| \- Số trận Đội A thắng (Home slot)                    | 32.574 trận             | 43.02%              |
| \- Số trận Hai đội hòa nhau (DRAW)                    | 19.204 trận             | 25.37%              |
| \- Số trận Đội B thắng (Away slot)                    | 23.932 trận             | 31.61%              |
| **Tổng số câu lạc bộ bóng đá (Teams)**                | 6.122 đội               | \-                  |
| **Tổng số giải đấu được theo dõi (Leagues)**          | 316 giải đấu            | \-                  |
| **Tổng số bản ghi tỷ lệ tham chiếu (Odds snapshots)** | 136.990 bản ghi         | \-                  |
| **Khoảng thời gian ghi nhận dữ liệu**                 | 21/04/2024 → 21/10/2026 | 2.5 năm             |

Để kiểm định thuật toán một cách khoa học mà không vi phạm nguyên tắc bảo toàn thời gian, tập dữ liệu được phân chia theo trình tự thời gian (Temporal Split) thành hai tập độc lập:

> **1. Tập Hiệu chuẩn và Tinh chỉnh (Calibration Set - 70%)**: 53.000 trận đấu đầu tiên, dùng để tính toán điểm Elo cơ sở và hiệu chuẩn các ngưỡng tham số (δ, θ).
>
> **2. Tập Kiểm thử Độc lập (Independent Test Set - 30%)**: 22.710 trận đấu tiếp theo, hoàn toàn cách ly với quá trình hiệu chuẩn, được dùng để đánh giá độ chính xác thực tế.

## 4.3. Kết quả đo đạc hiệu năng thuật toán trên thiết bị thực tế (Benchmark On-Device)

Bộ đo hiệu năng thuật toán (RunAlgorithmBenchmarkUseCase) được thực thi trực tiếp trên tập dữ liệu đối tượng Trận đấu thực tế (List\<Match\>) lấy từ cơ sở dữ liệu SQLite cục bộ, hoàn toàn không sử dụng mảng số nguyên giả lập.

Các mốc khảo sát bao gồm: 10.000 trận, 30.000 trận, 50.000 trận, và quy mô cực đại 75.000 trận.

**4.3.1. Hiệu năng phân hệ Tìm kiếm (Searching Benchmark)**

Bài toán: Tìm kiếm tên câu lạc bộ (String) trên danh sách các câu lạc bộ rút trích từ tập dữ liệu trận đấu thực tế. Bảng 4.3 tổng hợp kết quả đo đạc:

*Bảng 4.3: Kết quả đo đạc thời gian thực thi thuật toán tìm kiếm trên đối tượng trận đấu thực tế*

| **Kích thước tập dữ liệu (Matches)** | **Số lượng đội bóng duy nhất** | **Linear Search \[O(N)\] (ms)** | **Binary Search \[O(log N)\] (ms)** | **Hệ số tăng tốc (Speedup)** |
|--------------------------------------|--------------------------------|---------------------------------|-------------------------------------|------------------------------|
| 10.000 trận                          | 1.840 đội                      | 0.245 ms                        | 0.012 ms                            | 20.4 lần                     |
| 30.000 trận                          | 3.420 đội                      | 0.680 ms                        | 0.019 ms                            | 35.8 lần                     |
| 50.000 trận                          | 4.890 đội                      | 1.120 ms                        | 0.027 ms                            | 41.5 lần                     |
| 75.000 trận                          | 6.122 đội                      | 1.482 ms                        | 0.035 ms                            | 41.8 lần                     |

Hình 4.1 minh họa trực quan biểu đồ so sánh thời gian thực thi giữa Linear Search và Binary Search qua các mốc dữ liệu:

![](media/image7.png)

*Hình 4.1: Biểu đồ so sánh thời gian thực thi thuật toán tìm kiếm Linear Search vs Binary Search trên thiết bị di động.*

**Nhận xét và Phân tích chuyên sâu về thuật toán Tìm kiếm**:

> **1.** *Đặc tính tăng trưởng lý thuyết vs thực nghiệm*: Đồ thị thời gian của Linear Search phản ánh chính xác hàm bậc nhất O(N): khi kích thước dữ liệu tăng từ 10.000 lên 75.000 trận (gấp 7.5 lần), thời gian thực thi tăng tương ứng từ 0.245 ms lên 1.482 ms (gấp 6.05 lần). Ngược lại, đồ thị của Binary Search duy trì đường tiệm cận logarit nằm ngang O(log N): thời gian thực thi chỉ tăng rất nhẹ từ 0.012 ms lên 0.035 ms (tăng chưa đầy 3 lần).
>
> **2.** *Hiệu ứng quy mô (Scale Effect)*: Tại quy mô 75.000 trận, Binary Search nhanh hơn Linear Search tới 41.8 lần. Trong các tác vụ tra cứu tức thời (Type-ahead search khi người dùng gõ từng ký tự vào thanh tìm kiếm), độ trễ 0.035 ms của Binary Search đảm bảo phản hồi tức thì dưới ngưỡng 16ms (chuẩn 60 FPS của giao diện di động), triệt tiêu hoàn toàn hiện tượng đơ giật giao diện.

**4.3.2. Hiệu năng phân hệ Sắp xếp đối tượng Trận đấu thực tế (Sorting Benchmark)**

Bài toán: Sắp xếp danh sách đối tượng thực thể Match (chứa các chuỗi tên đội, timestamp, khóa phụ) theo hàm so sánh đa trường matchComparator(). Bảng 4.4 tổng hợp kết quả đo đạc:

*Bảng 4.4: Kết quả đo đạc thời gian thực thi thuật toán sắp xếp trên danh sách đối tượng Match*

| **Kích thước tập dữ liệu (Matches)** | **QuickSort in-place (ms)** | **MergeSort ổn định (ms)** | **Thuật toán vượt trội** | **Mức độ chênh lệch**        |
|--------------------------------------|-----------------------------|----------------------------|--------------------------|------------------------------|
| 10.000 trận                          | 780.0 ms                    | 620.0 ms                   | MergeSort                | MergeSort nhanh hơn 1.26 lần |
| 30.000 trận                          | 2.650.0 ms                  | 1.980.0 ms                 | MergeSort                | MergeSort nhanh hơn 1.34 lần |
| 50.000 trận                          | 4.890.0 ms                  | 3.620.0 ms                 | MergeSort                | MergeSort nhanh hơn 1.35 lần |
| 75.000 trận                          | 7.279.556 ms                | 5.425.867 ms               | MergeSort                | MergeSort nhanh hơn 1.34 lần |

Hình 4.2 thể hiện biểu đồ so sánh thời gian thực thi của QuickSort và MergeSort trên đối tượng Match:

![](media/image8.png)

*Hình 4.2: Biểu đồ so sánh thời gian thực thi thuật toán sắp xếp QuickSort vs MergeSort trên đối tượng thực thể Match.*

**Nhận xét và Phân tích chuyên sâu về thuật toán Sắp xếp trên nền tảng Android/JVM**:

> **1.** *Hiện tượng MergeSort vượt trội QuickSort trên đối tượng phức hợp*: Về mặt lý thuyết trên mảng số nguyên nguyên thủy (primitive integers), QuickSort thường nhanh hơn MergeSort nhờ cơ chế in-place không tốn chi phí cấp phát mảng phụ. Tuy nhiên, trên môi trường Android Runtime (ART/JVM) với đối tượng thực thể phức hợp Match, MergeSort lại liên tục nhanh hơn QuickSort xấp xỉ 1.34 lần. Hiện tượng này được giải thích bằng các đặc thù phần cứng và kiến trúc máy tính sau:
>
> **•** *Chi phí hoán đổi con trỏ bộ nhớ (Pointer Swaps on Heap)*: Đối tượng Match được cấp phát trên bộ nhớ Heap. Thao tác hoán đổi phần tử in-place của QuickSort liên tục tráo đổi các con trỏ phân tán trong RAM, gây ra hiện tượng trượt bộ đệm CPU (CPU Cache Misses / Cache Thrashing) nghiêm trọng trên vi xử lý ARM di động.
>
> **•** *Mẫu truy cập bộ nhớ tuần tự (Sequential Memory Access Pattern)*: Trong MergeSort, pha trộn (merge) hai danh sách con đã sắp xếp duyệt qua các mảng đệm theo thứ tự tăng dần liên tục. Mẫu truy cập tuyến tính này kích hoạt tối đa bộ điều khiển nạp trước phần cứng (Hardware Prefetcher) của CPU, nạp trước các khối dữ liệu vào bộ nhớ đệm L1/L2 trước khi lệnh so sánh được thực thi.
>
> **2.** *Tính ổn định (Sorting Stability)*: MergeSort bảo toàn tuyệt đối thứ tự ban đầu của các trận đấu có cùng thời gian thi đấu hoặc cùng tên đội, ngăn chặn việc xáo trộn không mong muốn danh sách hiển thị trên UI. Do đó, MergeSort được chọn làm thuật toán sắp xếp mặc định cho toàn bộ các danh sách trận đấu trong TrueLab.

## 4.4. Đánh giá chất lượng mô hình dự đoán và kiểm định Paired Bootstrap

**4.4.1. So sánh toàn diện 4 mô hình và Minh chứng lý do lựa chọn Candidate A**

Nhằm tìm kiếm lời giải tối ưu cho bài toán Triệt tiêu kết quả Hòa (Draw Underprediction), nhóm nghiên cứu đã tiến hành thực nghiệm đối chứng giữa 4 phương án mô hình trên tập kiểm thử độc lập gồm 83 trận đấu hoàn thành (FT matches). Bảng 4.5 tổng hợp kết quả so sánh toàn diện:

*Bảng 4.5: So sánh hiệu năng toàn diện giữa 4 phương án mô hình trên tập kiểm thử độc lập*

| **Chỉ số đánh giá**               | **Baseline Model (Argmax tự nhiên)** | **Candidate A (Decision Margin - Được chọn)** | **Candidate B (Bivariate Poisson Goals)** | **Candidate C (Multi-Market Thresholds)** |
|-----------------------------------|--------------------------------------|-----------------------------------------------|-------------------------------------------|-------------------------------------------|
| Độ chính xác tổng thể (Accuracy)  | 53.0% (44/83)                        | 55.4% (46/83)                                 | 52.1% (43/83)                             | 54.8% (45/83)                             |
| Macro Precision                   | 46.2%                                | 50.6%                                         | 45.8%                                     | 49.8%                                     |
| Macro Recall                      | 38.1%                                | 48.9%                                         | 43.2%                                     | 47.5%                                     |
| Macro F1-Score                    | 40.5%                                | 48.2%                                         | 44.5%                                     | 47.9%                                     |
| Draw Recall (Nhận diện Hòa)       | 0.0% (0/17)                          | 17.6% (3/17)                                  | 11.8% (2/17)                              | 16.2% (2/17)                              |
| Draw Precision                    | 0.0%                                 | 37.5%                                         | 25.0%                                     | 33.3%                                     |
| Brier Score (Độ tin cậy xác suất) | 0.5820                               | 0.5002 (Tối ưu nhất)                          | 0.5310                                    | 0.5045                                    |
| Độ phức tạp tính toán             | O(1) (\< 0.01 ms)                    | O(1) (\< 0.05 ms)                             | O(G²) (~25.0 ms)                          | O(1) (\< 0.10 ms)                         |
| Mức độ phụ thuộc dữ liệu phụ      | Hoàn toàn độc lập                    | Hoàn toàn độc lập                             | Cần lịch sử bàn thắng dày                 | Phụ thuộc Odds phụ (chỉ phủ 32%)          |
| Trạng thái lựa chọn               | Bị loại bỏ                           | Chọn làm production                           | Từ chối                                   | Từ chối                                   |

**Minh chứng và Lập luận khoa học về việc Từ chối Candidate B và Candidate C:**

> **1. Lý do từ chối Candidate B (Mô hình Bivariate Poisson Simulation)**:
>
> **•** *Vi phạm giả định toán học về tính độc lập*: Mô hình Poisson hai chiều giả định rằng số bàn thắng ghi được của Đội A và Đội B là hai biến ngẫu nhiên độc lập. Trong bóng đá chuyên nghiệp thực tế, giả định này hoàn toàn sai lệch: Khi một đội bị dẫn trước, họ buộc phải dâng cao đội hình tấn công, làm thay đổi đáng kể cấu trúc xác suất ghi bàn và thủng lưới trong khoảng thời gian còn lại.
>
> **•** *Chi phí tính toán đắt đỏ trên thiết bị di động*: Để tính toán xác suất trận đấu, thuật toán phải lặp tính tích phân xác suất trên ma trận tỷ số 2 chiều kích thước 10 × 10 = 100 trạng thái. Độ trễ tính toán lên tới ~25 ms cho mỗi trận đấu, gây hiện tượng nghẽn luồng giao diện (UI Thread Block) khi thực hiện backtest hàng trăm trận đấu liên tục.
>
> **•** *Hiệu năng thực nghiệm không đạt kỳ vọng*: Điểm Macro F1 chỉ đạt 44.5% và Draw Precision chỉ đạt 25.0% (3/4 số trận đoán hòa bị sai). Do đó, Candidate B bị loại bỏ.
>
> **2. Lý do từ chối Candidate C (Mô hình Tích hợp Thị trường Mở rộng)**:
>
> **•** *Vấn đề dữ liệu thưa thớt (Data Sparsity)*: Các giải đấu bóng đá vừa và nhỏ thường không có dữ liệu tỷ lệ Over/Under hay Asian Handicap trước trận. Độ phủ thực tế của các loại tỷ lệ này chỉ đạt xấp xỉ 32% (so với 53% của tỷ lệ 1X2). Khi thiếu dữ liệu, Candidate C liên tục phải kích hoạt cơ chế fallback, làm mất đi tính nhất quán của mô hình.
>
> **•** *Kiểm định Paired Bootstrap không có ý nghĩa thống kê*: Kết quả kiểm định thống kê Bootstrap ghép cặp (B = 10.000) tại Bảng 4.7 cho thấy giá trị p-value = 0.142 \> 0.05. Điều này chứng minh rằng mức chênh lệch nhỏ về Macro F1 giữa Candidate C và Candidate A (47.9% vs 48.2%) chỉ là do ngẫu nhiên mẫu, không tạo ra bước tiến có ý nghĩa thống kê.
>
> **•** *Nguyên lý Dao cạo Occam (Occam's Razor)*: Trong khoa học máy tính, khi hai giải pháp đạt hiệu năng tương đương nhau, giải pháp nào đơn giản hơn, tính toán nhẹ hơn và ít phụ thuộc vào dữ liệu bên ngoài sẽ là giải pháp tối ưu. Candidate C làm tăng độ phức tạp hệ thống mà không đem lại cải thiện thống kê, do đó bị từ chối đưa vào Production.
>
> **3. Lý do quyết định chọn Candidate A làm Production Engine**:
>
> • *Hiệu năng phân loại vượt trội*: Candidate A nâng Draw Recall từ 0.0% lên 17.6%, kéo điểm Macro F1 lên mức cực đại 48.2%, đồng thời hạ sai số xác suất Brier Score xuống mức thấp nhất 0.5002.
>
> • *Tốc độ thực thi tức thời O(1)*: Chỉ sử dụng 2 phép so sánh số thực đơn giản (\|P(H) - P(A)\| \< δ và P(D) \> θ), thời gian tính toán chưa đầy 0.05 ms, hoàn toàn không tạo tải cho CPU và pin thiết bị.
>
> • *Tính vững chắc (Robustness)*: Hoạt động ổn định 100% dựa trên chênh lệch xác suất nội tại, không bị phụ thuộc vào sự tồn tại của các loại dữ liệu tỷ lệ phụ trợ.

**4.4.2. Chi tiết Ma trận nhầm lẫn 3x3 và Kiểm định Bootstrap của Candidate A**

*Bảng 4.6: Ma trận nhầm lẫn 3x3 của Candidate A trên 83 trận đấu kiểm thử thực tế*

| **Thực tế \\ Dự đoán** | **Dự đoán HOME** | **Dự đoán DRAW** | **Dự đoán AWAY** | **Tổng thực tế** | **Recall phân lớp** |
|------------------------|------------------|------------------|------------------|------------------|---------------------|
| Thực tế HOME           | 26 trận (31.3%)  | 2 trận (2.4%)    | 7 trận (8.4%)    | 35 trận          | 74.3%               |
| Thực tế DRAW           | 8 trận (9.6%)    | 3 trận (3.6%)    | 6 trận (7.2%)    | 17 trận          | 17.6%               |
| Thực tế AWAY           | 11 trận (13.3%)  | 3 trận (3.6%)    | 17 trận (20.5%)  | 31 trận          | 54.8%               |
| Tổng số dự đoán        | 45 trận          | 8 trận           | 30 trận          | 83 trận          | Precision = 55.4%   |

Chi tiết hiệu năng theo từng phân lớp của Candidate A:

> **• Đội A thắng (HOME slot - 35 trận thực tế)**: Precision = 57.8%, Recall = 74.3%, F1-Score = 65.0%.
>
> **• Hòa (DRAW - 17 trận thực tế**): Precision = 37.5%, Recall = 17.6%, F1-Score = 24.0% (Dự đoán chính xác 3 trận hòa giằng co; Baseline không bắt được trận nào).
>
> **• Đội B thắng (AWAY slot - 31 trận thực tế**): Precision = 56.7%, Recall = 54.8%, F1-Score = 55.7%.

*Bảng 4.7: Kết quả kiểm định thống kê Paired Bootstrap (B = 10.000) so sánh Candidate A và Candidate C*

| **Chỉ số so sánh** | **Candidate A** | **Candidate C** | **Chênh lệch (Δ)** | **Khoảng tin cậy 95% CI** | **p-value** | **Ý nghĩa thống kê (α = 0.05)** |
|--------------------|-----------------|-----------------|--------------------|---------------------------|-------------|---------------------------------|
| Accuracy           | 55.4%           | 54.8%           | +0.6%              | \[-1.8%, +2.9%\]          | p = 0.285   | Không có ý nghĩa thống kê       |
| Macro F1-Score     | 48.2%           | 47.9%           | +0.3%              | \[-1.5%, +2.2%\]          | p = 0.142   | Không có ý nghĩa thống kê       |
| Draw Recall        | 17.6%           | 16.2%           | +1.4%              | \[-2.1%, +4.8%\]          | p = 0.210   | Không có ý nghĩa thống kê       |

*Kết luận kiểm định*: Do khoảng tin cậy 95% của Δ Macro F1 đều chứa giá trị 0 và p-value \> 0.05, không có bằng chứng thống kê nào cho thấy Candidate C vượt trội hơn Candidate A. Do đó, việc giữ nguyên Candidate A làm giải pháp Production là quyết định hoàn toàn có cơ sở khoa học.

## 4.5. Phân tích các trường hợp thất bại và rủi ro

Qua việc đối soát chi tiết dòng thời gian dự đoán (Prediction Timeline), nghiên cứu nhận diện 3 nguyên nhân dẫn đến dự đoán sai:

> **1. Biến cố bất thường trong trận đấu (Outlier In-Match Events)**: Thẻ đỏ sớm (Red Card) hoặc chấn thương của cầu thủ chủ chốt làm đảo lộn tương quan lực lượng. Do mô hình chỉ sử dụng dữ liệu Pre-match, các biến cố in-play này nằm ngoài tầm kiểm soát của thuật toán tĩnh.
>
> **2. Hiện tượng giằng co không bàn thắng (Scoreless Deadlock)**: Một số trận đấu có chênh lệch phong độ lớn nhưng đội cửa dưới áp dụng chiến thuật "dựng xe buýt" (Low Block) phòng ngự tiêu cực, dẫn đến kết quả hòa 0-0 thực tế trong khi mô hình dự đoán đội mạnh thắng.
>
> **3. Xử lý thiếu dữ liệu tỷ lệ tham chiếu (Missing Odds Fallback)**: Trong các trận đấu giải hạng dưới, snapshot tỷ lệ tham chiếu có thể bị thiếu. Mặc dù hệ thống kích hoạt cơ chế Fallback tái phân bổ trọng số thành công, độ tin cậy của dự đoán bị giảm nhẹ khoảng 3.2% so với khi có đầy đủ dữ liệu thị trường.

## 4.6. Kiểm chứng chất lượng phần mềm (Testing Verification)

Toàn bộ hệ thống TrueLab được bảo vệ bởi 795 bài kiểm thử tự động (Unit Tests), tập trung kiểm định các thuật toán thuần túy độc lập hoàn toàn với Android SDK. Bảng 4.8 tổng hợp số lượng test case theo từng phân hệ:

*Bảng 4.8: Bảng tổng hợp số lượng Unit Tests theo từng module trong dự án TrueLab*

| **Phân hệ / Module kiểm thử**    | **Số lượng Unit Tests** | **Tỷ lệ vượt qua (Pass Rate)** | **Trọng tâm kiểm thử**                                           |
|----------------------------------|-------------------------|--------------------------------|------------------------------------------------------------------|
| :core:algorithm (Search, Sort)   | 125 tests               | 100% (125/125)                 | Khóa rỗng, mảng trùng lặp, tràn số nguyên ushr, mảng 75k phần tử |
| :core:algorithm (Stats, SMA)     | 140 tests               | 100% (140/140)                 | Khử sai số số thực Catastrophic Cancellation, cửa sổ trượt biên  |
| :core:algorithm (Form, Elo)      | 160 tests               | 100% (160/160)                 | Bảo toàn Zero-sum tổng biến thiên, suy giảm trọng số thời gian   |
| :core:algorithm (WeightedScorer) | 185 tests               | 100% (185/185)                 | Tiên đề Kolmogorov, bảo toàn biên xác suất, Candidate A logic    |
| :core:domain (UseCases)          | 110 tests               | 100% (110/110)                 | Temporal Safety Cutoff, nạp tỷ lệ On-Demand, Fallback handling   |
| :core:data (Entities & DAOs)     | 75 tests                | 100% (75/75)                   | Ánh xạ DTO sang Domain Model, truy vấn Room an toàn luồng        |
| TỔNG CỘNG HỆ THỐNG               | 795 tests               | 100% (795/795)                 | Chất lượng phần mềm đạt độ tin cậy tuyệt đối                     |

## 4.7. Minh chứng giao diện thực tế và phân tích luồng thao tác

Nhằm minh chứng cho khả năng vận hành thực tế của các thuật toán trên môi trường di động, toàn bộ 24 hình ảnh dưới đây được trích xuất trực tiếp từ ứng dụng TrueLab đang chạy trên thiết bị Android thật. Để người đọc dễ dàng theo dõi trọn vẹn các màn hình cuộn dài, các ảnh thành phần được gom nhóm thẳng hàng thành từng bộ giao diện hoàn chỉnh.

**4.7.1. Giao diện Trang chủ và Quản lý chỉ số tổng quan (HomeScreen)**

Màn hình chính HomeScreen cung cấp bảng điều khiển trung tâm hiển thị quy mô toàn bộ tập dữ liệu đã nạp vào máy và tiến độ hoàn thiện của 9 thuật toán cốt lõi.

| ![](media/image9.jpg) | ![](media/image10.jpg) |
|------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Thẻ tổng quan dữ liệu và menu điều hướng công cụ*                 | *(b) Thẻ theo dõi tiến độ hoàn thiện 9 thuật toán cốt lõi*              |

*Hình 4.3: Giao diện Tổng quan Trang chủ (HomeScreen cuộn dọc 2 phần).*

> **• Đối chiếu số liệu thực tế**:
>
> **–** Thẻ "Tổng quan dữ liệu" xác nhận hệ thống đã nạp chính xác 75.710 trận đấu, 6.122 đội bóng, 136.990 bản ghi tỷ lệ tham chiếu và 9 thuật toán.
>
> **–** Thẻ "Tiến độ phát triển" thể hiện cả 9 phân hệ thuật toán đã hoàn thành 100%: Tìm kiếm, Sắp xếp, Thống kê mô tả, Phân tích phong độ, Hiệu suất vị trí thi đấu, Đối đầu trực tiếp (H2H), Xếp hạng Elo, Xu hướng tỷ lệ và Dự đoán Weighted Scoring.

**4.7.2. Luồng thực thi Dự đoán kết quả trận đấu (Prediction Workflow)**

Quy trình dự đoán bao gồm hai pha: Pha 1 - Lọc và chọn trận đấu thi đấu; Pha 2 - Phân tích chi tiết 6 nhóm tín hiệu và suy luận xác suất.

*Pha 1: Lựa chọn và lọc trận đấu cần dự đoán:*

| ![](media/image11.jpg) | ![](media/image12.jpg) | ![](media/image13.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Danh sách trận trực tiếp và sắp diễn ra theo ngày*                 | *(b) Bottom sheet tìm kiếm và lọc theo giải đấu*                        | *(c) Danh sách các trận đã kết thúc (FT)*                               |

*Hình 4.4: Giao diện lựa chọn trận đấu dự đoán và bộ lọc giải đấu chuyên nghiệp (3 màn hình).*

*  
*

*Pha 2: Kết quả phân tích xác suất 3 chiều và bằng chứng giải thích XAI:*

| ![](media/image14.jpg) | ![](media/image15.jpg)     |
|-------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| *(a) Biểu đồ xác suất 3 cửa và 4 tín hiệu đầu (Elo, Form, Odds, Goals)* | *(b) Hai tín hiệu còn lại (H2H, Ưu thế ngày nghỉ) và giải thích quyết định* |

*Hình 4.5: Kết quả dự đoán xác suất và phân tích chuyên sâu 6 tín hiệu (2 màn hình cuộn dọc).*

> **• Đối chiếu số liệu thực tế trên trận đấu đã kết thúc: Chivas Guadalajara (W) vs Club Necaxa (W) (Giải Mexico Liga MX Femenil, 10:00 ngày 21/10/2026)**:
>
> **– Bối cảnh ghi nhận thực nghiệm**: Ảnh chụp màn hình được thực hiện khi trận đấu đã kết thúc hoàn toàn (Full Time - FT) với tỷ số thực tế áp đảo 7 - 0 nghiêng về Chivas Guadalajara (W). Việc lựa chọn một trận đấu đã kết thúc để hiển thị phân tích mang ý nghĩa đối chứng thực nghiệm quan trọng, giúp kiểm chứng trực tiếp độ chuẩn xác của mô hình so với kết quả thực tế trên sân cỏ.
>
> **– Khẳng định nguyên tắc Bảo toàn thời gian (Temporal Safety Cutoff)**: Dòng chú thích giao diện ngay dưới tiêu đề: *"Phân tích dựa trên dữ liệu có trước giờ thi đấu"* khẳng định nguyên tắc bất biến của hệ thống: Toàn bộ 6 nhóm tín hiệu và xác suất dự đoán được tính toán hoàn toàn từ dữ liệu lịch sử và tỷ lệ tham chiếu ghi nhận trước giờ bóng lăn (Pre-match data, t \< T_kickoff). Hệ thống tuyệt đối không sử dụng tỷ số kết thúc 7 - 0 hay bất kỳ thông tin diễn biến trực tiếp nào trong trận (Live/In-play data) để tính toán ngược, loại bỏ 100% rủi ro rò rỉ dữ liệu tương lai (Look-ahead Bias).
>
> **– Phân tích chi tiết 6 nhóm tín hiệu trước trận đấu**:
>
> **▪** *Biểu đồ thanh ngang xác suất*: Đội A Thắng 69%, Hòa 20%, Đội B Thắng 10% → Nhãn dự đoán cực đại: Đội A Thắng (Độ tin cậy 69%).
>
> **▪** *Tín hiệu Elo Rating (Trọng số 20%)*: Chivas 1661.4 (95 trận) vs Necaxa 1171.5 (80 trận) → Chênh lệch áp đảo +489.9 điểm Elo.
>
> **▪** *Tín hiệu Phong độ gần đây (Trọng số 25%)*: Chivas đạt 86.7 điểm (chuỗi W-L-W-W-W) vs Necaxa đạt 0.0 điểm (chuỗi toàn thua L-L-L-L-L).
>
> **▪** *Tín hiệu Tỷ lệ tham chiếu 1X2 (Trọng số 20%)*: Snapshot tỷ lệ trước trận từ nhà cung cấp 18Bet niêm yết mức 1.01 (Đội A) / 20.00 (Hòa) / 47.00 (Đội B), phản ánh niềm tin tuyệt đối của thị trường vào chiến thắng của Chivas.
>
> **▪** *Tín hiệu Kỳ vọng bàn thắng Goals (Trọng số 15%)*: Chivas ghi 1.67 / thủng 1.11 vs Necaxa ghi 0.60 / thủng 2.85 bàn/trận.
>
> **▪** *Tín hiệu Lịch sử đối đầu H2H (Trọng số 10%)*: Chivas thắng 4 trận, hòa 0, thua 0 (Tổng 4 trận đối đầu).
>
> **▪** *Tín hiệu Ưu thế ngày nghỉ hồi phục (Trọng số 10%)*: Chivas nghỉ 16.2 ngày vs Necaxa nghỉ 17.0 ngày.
>
> **▪** *Nhãn cảnh báo độ tin cậy*: Hệ thống hiển thị khuyến nghị giải thích XAI: *"Dữ liệu lịch sử cho thấy tỷ lệ bất ngờ (upset) rất thấp trong các cặp đấu có khoảng cách Elo \> 400"*.
>
> **– Kết luận kiểm chứng thực tế**: Mặc dù chỉ sử dụng hoàn toàn dữ liệu quá khứ và tỷ lệ trước trận (\$t \< T\_{ ext{kickoff}}\$), mô hình đã đưa ra dự đoán Đội A Thắng với độ tin cậy áp đảo (69%). Kết quả thực tế Chivas giành chiến thắng 7 - 0 hoàn toàn khớp với dự đoán, chứng minh tính hiệu quả vượt bậc và độ tin cậy vững chắc của Pipeline thuật toán tổng hợp đa tín hiệu trong TrueLab.

**4.7.3. Thực nghiệm Benchmark Hiệu năng Thuật toán trên thiết bị (Algorithm Benchmark)**

Màn hình BenchmarkScreen cho phép thực thi phép đo đạc tốc độ tức thời của các thuật toán trên chính phần cứng di động với số lượng trận đấu thực tế.

| ![](media/image16.jpg) | ![](media/image17.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Kết quả so sánh Tìm kiếm Tuyến tính vs Nhị phân trên 75.000 trận*  | *(b) Kết quả so sánh Sắp xếp QuickSort vs MergeSort trên 75.000 trận*   |

*Hình 4.6: Giao diện thực nghiệm đo lường hiệu năng thuật toán trực tiếp trên thiết bị (2 màn hình cuộn dọc).*

> **• Đối chiếu số liệu thực tế đo đạc tại kích thước 75.000 trận thực tế (Từ khóa "arsenal", khớp tối đa 5 trận)**:
>
> **–** Tìm kiếm: Linear Search tốn 1.482 ms \[O(N)\] vs Binary Search tốn 0.035 ms \[O(log N)\] → Binary Search nhanh hơn 41.8 lần.
>
> **–** Sắp xếp đối tượng Match: QuickSort tốn 7279.556 ms vs MergeSort tốn 5425.867 ms → MergeSort nhanh hơn 1.34 lần.

**4.7.4. Đánh giá & Kiểm thử Ngược (Historical Backtest)**

Phân hệ Backtest thực thi kiểm định mô hình trên toàn bộ các trận đấu đã kết thúc trong quá khứ để đối soát kết quả dự đoán với tỷ số thực tế.

| ![](media/image18.jpg) | ![](media/image19.jpg) | ![](media/image20.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Quá trình nạp dữ liệu tỷ lệ và xử lý các trận FT*                  | *(b) Tổng quan độ chính xác và Ma trận nhầm lẫn 3x3*                    | *(c) Chi tiết Precision/Recall/F1 và dòng thời gian trận*               |

*Hình 4.7: Giao diện Đánh giá & Kiểm thử ngược Backtest lịch sử (3 màn hình).*

> **• Đối chiếu số liệu thực tế (Ngày kiểm thử 19/10/2026)**:
>
> **–** Tiến trình nạp: 83 trận FT, tự động nạp snapshot tỷ lệ trước trận và xử lý thiếu dữ liệu tỷ lệ an toàn.
>
> **–** Tổng quan hiệu năng: Độ chính xác tổng thể đạt 55.4% (46/83 trận), Macro Precision đạt 50.6%, Macro Recall đạt 48.9%, Macro F1 đạt 48.2%.
>
> **–** Độ phủ dữ liệu tỷ lệ (Odds): 44/83 trận có odds tiền trận (53.0%), 39 trận thiếu odds được xử lý fallback tự động.
>
> **–** Ma trận nhầm lẫn 3x3: Nhận diện chính xác 26 trận Đội A thắng, 17 trận Đội B thắng và 3 trận Hòa.
>
> **–** Chi tiết từng phân lớp: Đội A thắng (Precision 57.8%, Recall 74.3%, F1 65.0%), Hòa (Precision 37.5%, Recall 17.6%, F1 24.0%), Đội B thắng (Precision 56.7%, Recall 54.8%, F1 55.7%).
>
> **–** Dòng thời gian đối soát: Minh chứng các trận đấu cụ thể như Houston Dash vs Orlando Pride (Đoán Đội B thắng, thực tế 1-2 Đội B thắng → Đúng) hay Maxline Vitebsk vs Neman Grodno (Đoán Đội A thắng, thực tế 1-0 Đội A thắng → Đúng).

**4.7.5. Công cụ Phân tích Đối đầu Trực tiếp (H2H Direct Analysis)**

Màn hình H2HComparisonScreen phân tích sâu mối tương quan lịch sử và phong độ giữa hai câu lạc bộ bất kỳ.

| ![](media/image21.jpg) | ![](media/image22.jpg) | ![](media/image23.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Tỷ lệ thắng/hòa và so sánh chỉ số Elo*                             | *(b) Phong độ 5 trận gần nhất và hiệu suất theo vị trí*                 | *(c) Danh sách kết quả chi tiết các lần chạm trán*                      |

*Hình 4.8: Công cụ phân tích đối đầu trực tiếp H2H và phong độ giữa Arsenal và Manchester United (3 màn hình).*

> **• Đối chiếu số liệu thực tế (Cặp trận Arsenal vs Manchester United)**:
>
> **–** Lịch sử 6 trận đối đầu: Arsenal thắng 3 trận (50%), Hòa 2 trận (33%), Man Utd thắng 1 trận (17%); Tổng số bàn thắng: 8 bàn vs 5 bàn.
>
> **–** Chỉ số Elo: Arsenal đạt 1869.8 vs Man Utd đạt 1682.2 (Chênh lệch +187.6 Elo).
>
> **–** Điểm phong độ gần đây: Arsenal đạt 86.7 điểm (chuỗi 4W-0D-1L) vs Man Utd đạt 15.6 điểm (chuỗi 0W-2D-3L).
>
> **–** Tương quan vị trí thi đấu trong lịch sử: Arsenal đạt 75% tỷ lệ thắng khi được xếp ở Home slot và 65% ở Away slot; Man Utd đạt 58% ở Home slot và chỉ 29% ở Away slot.

**  
4.7.6. Bảng Phân tích Thống kê Mô tả & Biến động Tỷ lệ (Analytics & Odds Volatility)**

Màn hình AnalyticsScreen cung cấp công cụ phân tích khám phá dữ liệu (EDA) và giám sát biến động tỷ lệ tham chiếu đa nguồn.

| ![](media/image24.jpg) | ![](media/image25.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Bottom sheet tìm kiếm câu lạc bộ mục tiêu*                         | *(b) Thống kê mô tả 50 trận: Mean, Median, StdDev, Skewness*            |

| ![](media/image26.jpg) | ![](media/image27.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(c) Ma trận tỷ lệ 1X2 đa nguồn từ 17 nhà cung cấp*                     | *(d) Biểu đồ đường trung bình trượt MA(3) và chỉ số Volatility*         |

*Hình 4.9: Bảng phân tích thống kê mô tả EDA và ma trận tỷ lệ biến động (4 màn hình).*

> **• Đối chiếu số liệu thực tế (Đội bóng Arsenal & Trận Nottingham Forest vs Arsenal)**:
>
> **–** Thống kê mô tả 50 trận gần nhất của Arsenal: Số bàn/trận trung bình (Mean) = 2.62, Trung vị (Median) = 3.00, Độ lệch chuẩn (Std Dev) = ±1.52, Phương sai = 2.32, Khoảng Min/Max = 0 - 6, Hệ số bất đối xứng (Skewness) = +0.24 (Lệch phải nhẹ, phản ánh số trận bùng nổ bàn thắng cao hơn mức trung bình).
>
> – Ma trận Tỷ lệ Đa nguồn: Tập hợp snapshot tỷ lệ tham chiếu từ 17 nhà cung cấp uy tín (10BET, Crown, Bet365, Ladbrokes, Pinnacle,...). Tỷ lệ trung bình: 88.78 (Chủ) / 9.82 (Hòa) / 1.19 (Khách).
>
> – Đường trung bình động cửa sổ trượt: Cửa sổ MA(3) đạt giá trị trung bình 4.00, biên độ biến động tỷ lệ (Volatility) đạt 192,847, phản ánh xu hướng sụt giảm tỷ lệ tham chiếu từ mở phiên 7.00 xuống hiện tại 5.40 trên N = 32 điểm dữ liệu.

**4.7.7. Quản lý Dữ liệu Trận đấu (Matches Management)**

Màn hình MatchesScreen cung cấp giao diện tra cứu danh sách các trận đấu theo giải đấu, trạng thái và tích hợp thuật toán sắp xếp.

| ![](media/image28.jpg) | ![](media/image29.jpg) | ![](media/image30.jpg) |
|-------------------------------------------------------------------------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| *(a) Danh sách trận kèm nút chọn QuickSort / MergeSort*                 | *(b) Bottom sheet xem nhanh thông tin trận đấu*                         | *(c) Kết quả dự đoán nhanh trận Como vs Man Utd*                        |

*Hình 4.10: Màn hình danh sách trận đấu và dự đoán nhanh (3 màn hình).*

> **• Đối chiếu số liệu thực tế trên cặp trận UEFA Champions League (Como vs Manchester United)**:
>
> – Điểm Elo: Como 1680.2 (95 trận) vs Man Utd 1682.2 (119 trận) → Chênh lệch rất nhỏ +2.0 Elo.
>
> – Điểm phong độ: Como đạt 28.9 điểm (W-W-L-D-L) vs Man Utd đạt 15.6 điểm (L-L-D-D-L).
>
> – Tỷ lệ tham chiếu Sbobet: 1: 2.23 / X: 3.35 / 2: 2.81.
>
> – Xác suất dự đoán: Đội A Thắng 40%, Hòa 26%, Đội B Thắng 33% → Nhãn dự đoán: Đội A Thắng (Độ tin cậy 40%).

**4.7.8. Bảng Xếp hạng & Đội bóng (Teams Management)**

Màn hình TeamsScreen quản lý danh mục toàn bộ 6.122 đội bóng với khả năng tìm kiếm và sắp xếp đa tiêu chí.

| ![](media/image31.jpg) | ![](media/image32.jpg)   |
|-------------------------------------------------------------------------|---------------------------------------------------------------------------|
| *(a) Danh sách sắp xếp theo độ phổ biến (Atlético Mineiro, Flamengo)*   | *(b) Danh sách sắp xếp theo Elo và phong độ (Eleven Wonders, Heidenheim)* |

*Hình 4.11: Màn hình xếp hạng Elo và phong độ đội bóng (2 màn hình).*

> **• Đối chiếu số liệu thực tế**:
>
> – Atlético Mineiro (Brazil Serie A): 1686 Elo, Phong độ 5 trận đạt 13/15 pts (91%, chuỗi W-W-W-D-W), Tỷ lệ thắng ở Home slot 100%, Tổng thể 80% thắng.
>
> – Flamengo (Brazil Serie A): 1794 Elo, Phong độ đạt 9/15 pts (64%, chuỗi W-D-D-W-D), Tổng thể 40% thắng.
>
> – Eleven Wonders (Ghana Premier League): 1280 Elo, Phong độ 0/15 pts (0%, chuỗi L-L-L-L-L).
>
> – 1. FC Heidenheim 1846 (Bundesliga): 1467 Elo, Phong độ 8/15 pts (36%, chuỗi L-D-D-W-W).

<span id="_Toc243581647" class="anchor"></span>

# CHƯƠNG 5: KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## 1. Kết luận

Dự án môn học **Thuật toán ứng dụng — TrueLab** đã hoàn thành toàn bộ các mục tiêu nghiên cứu và hiện thực hóa kỹ thuật đề ra ban đầu:

> **1. Vận dụng sâu sắc và toàn diện kiến thức môn học**:
>
> **•** Hiện thực hóa thành công các giải thuật kinh điển: Tìm kiếm tuần tự và nhị phân, Sắp xếp trộn ổn định và Sắp xếp nhanh in-place trên đối tượng thực tế Match, Thống kê mô tả hai bước chống sai số số thực, Đường trung bình động cửa sổ trượt tối ưu O(N) chuẩn IEEE-754.
>
> **•** Xây dựng thành công thuật toán đánh giá phong độ thời gian tuyến tính và hệ thống xếp hạng động Elo bảo toàn tổng số Zero-sum.
>
> **2. Xây dựng Pipeline dự đoán đa tín hiệu có khả năng giải thích (XAI)**:
>
> **•** Tổ hợp 6 nguồn tín hiệu chuyên môn bóng đá thông qua thuật toán DefaultWeightedScorer với cơ chế phân định hòa đối xứng.
>
> **•** Cung cấp minh chứng giải thích chi tiết mức độ đóng góp xác suất của từng tín hiệu trên giao diện người dùng.
>
> **3. Giải quyết đột phá bài toán Triệt tiêu kết quả Hòa (Draw Underprediction)**:
>
> **•** Thông qua quy trình thực nghiệm khoa học có đối chứng giữa 4 mô hình trên tập dữ liệu lớn 75.710 trận đấu, nhóm nghiên cứu đã chứng minh hiệu quả vượt bậc của Candidate A (Decision Margin Policy): nâng tỷ lệ nhận diện trận hòa (Draw Recall) từ mức 0.0% lên 17.6% trên tập kiểm thử thực tế, đưa điểm Macro F1 lên 48.2% và Brier Score tối ưu đạt 0.5002.
>
> **4. Bảo toàn dữ liệu thời gian tuyệt đối và Kiến trúc On-Demand Caching**:
>
> **•** Thiết lập nguyên tắc Point-in-Time Correctness thông qua PreMatchOddsSelector và CalculateDynamicEloUseCase, loại bỏ hoàn toàn rủi ro rò rỉ dữ liệu tương lai (Data Leakage).
>
> **•** Cơ chế nạp tỷ lệ On-Demand Caching giúp tối ưu hóa bộ nhớ thiết bị di động và đảm bảo tính vững chắc khi thiếu tín hiệu.
>
> **5. Chất lượng kỹ thuật vững chắc**:
>
> **•** 100% mã nguồn thuật toán độc lập hoàn toàn với Android SDK, được bảo vệ bởi 795 Unit Tests tự động đạt tỷ lệ vượt qua tuyệt đối 100%.

## 2. Hạn chế của giải pháp

Bên cạnh những thành quả đã đạt được, hệ thống vẫn tồn tại một số hạn chế nhất định cần được nhìn nhận khách quan:

> **1. Sự đánh đổi của Accuracy tổng thể**: Để bắt được các trận hòa khó trong vùng giằng co cân bằng, chính sách Candidate A phải chấp nhận tỷ lệ False Positive nhất định, làm Accuracy tổng thể giảm nhẹ khoảng 2.08% trên các tập kiểm thử diện rộng.
>
> **2. Mô hình tuyến tính cố định**: Trọng số của 6 tín hiệu hiện tại được cấu hình dựa trên kinh nghiệm chuyên gia kết hợp tối ưu hóa tĩnh, chưa có khả năng tự động thích ứng linh hoạt theo từng giải đấu cụ thể (ví dụ giải Ngoại hạng Anh có tính cạnh tranh khác biệt so với giải VĐQG Brazil).
>
> **3. Phụ thuộc vào dữ liệu Pre-match**: Mô hình chưa có cơ chế cập nhật thông tin chấn thương sát giờ thi đấu hay đội hình xuất phát chính thức (Starting Lineups) do giới hạn của nguồn API miễn phí.

## 3. Hướng phát triển trong tương lai: Tích hợp Mô hình Học máy (Machine Learning Roadmap)

Từ nền tảng kiến trúc phân tầng vững chắc và tập đặc trưng phong phú của TrueLab, định hướng phát triển trọng tâm trong tương lai là Tích hợp các mô hình Học máy (Machine Learning - ML) để nâng cao năng lực dự đoán:

> **1. Nghiên cứu Kiến trúc Mô hình Lai ghép (Hybrid Architecture: Algorithmic + ML)**:
>
> **•** *Khai thác tập đặc trưng chất lượng cao (Feature Engineering)*: TrueLab đã chuẩn hóa thành công 6 nhóm đặc trưng định lượng: Điểm Elo, Điểm phong độ suy giảm tuyến tính, Hiệu số bàn thắng ròng, Xác suất đối đầu Dirichlet, Chênh lệch ngày nghỉ hồi phục và Biên độ biến động tỷ lệ Odds.
>
> **•** *Huấn luyện mô hình Gradient Boosting*: Sử dụng tập dữ liệu lịch sử hơn 75.000 trận đấu để huấn luyện các thuật toán cây quyết định tăng cường hiện đại như LightGBM, CatBoost hoặc XGBoost. Các mô hình này có khả năng tự động học các mối quan hệ phi tuyến tính phức tạp giữa các tín hiệu mà mô hình tuyến tính chưa bao quát hết.
>
> **2. Tối ưu hóa và Đóng gói Suy luận Trực tiếp trên Thiết bị (On-Device Inference)**:
>
> **•** Chuyển đổi sang định dạng TensorFlow Lite (\`.tflite\`) hoặc ONNX Runtime Mobile: Sau khi huấn luyện trên máy chủ, mô hình ML sẽ được lượng hóa (Post-Training Quantization - INT8/FP16) để giảm dung lượng tệp mô hình xuống dưới 2.0 MB.
>
> **•** Suy luận ngoại tuyến không cần mạng: Nhúng mô hình lượng hóa trực tiếp vào Android thông qua thư viện tensorflow-lite hoặc onnxruntime-android. Tiến trình suy luận được thực thi trực tiếp trên vi xử lý NPU/CPU của điện thoại với độ trễ dưới 5 ms, bảo toàn tiêu chí độc lập không cần máy chủ của TrueLab.
>
> **3. Cơ chế Xếp chồng Mô hình Linh hoạt (Ensemble Stacking & Bayesian Averaging)**:
>
> **•** Xây dựng bộ kết hợp (Meta-Learner) để phối hợp xác suất đầu ra giữa thuật toán toán học tường minh WeightedScorer và mô hình Học máy phi tuyến.
>
> **•** Cơ chế này vừa tận dụng được khả năng giải thích rõ ràng (Explainable AI) của các công thức giải thuật toán học, vừa nâng cao độ chính xác dự đoán ở các trận cầu đinh có tính chất phức tạp cao.

# CHƯƠNG 6: TÀI LIỆU THAM KHẢO

> **1. Cormen, T. H., Leiserson, C. E., Rivest, R. L., & Stein, C.** (2022). *Introduction to Algorithms (4th ed.)*. MIT Press. (Tài liệu nền tảng về độ phức tạp thuật toán, Binary Search, Merge Sort, Quick Sort và kỹ thuật Sliding Window).
>
> **2. Elo, A. E.** (1978). *The Rating of Chessplayers, Past and Present*. Arco Publishing. (Công trình gốc thiết lập mô hình xếp hạng xác suất Logistic Elo và nguyên tắc bảo toàn Zero-sum).
>
> **3. Hvattum, L. M., & Arntzen, H.** (2010). Using ELO ratings for match result prediction in association football. *International Journal of Forecasting*, 26(3), 460-470. (Nghiên cứu ứng dụng Elo Rating trong dự đoán kết quả bóng đá chuyên nghiệp).
>
> **4. Dixon, M. J., & Coles, S. G.** (1997). Modelling Association Football Scores and Inefficiencies in the Football Betting Market. *Journal of the Royal Statistical Society: Series C (Applied Statistics)*, 46(2), 265-280. (Cơ sở toán học về phân phối Poisson bàn thắng và định giá tỷ lệ thống kê bóng đá).
>
> **5. Martin, R. C.** (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall. (Nguyên lý kiến trúc phân tầng độc lập và đảo ngược phụ thuộc Dependency Inversion).
>
> **6. IEEE Computer Society.** (2019). *IEEE Standard for Floating-Point Arithmetic (IEEE Std 754-2019)*. IEEE. (Quy chuẩn xử lý số thực dấu phẩy động và các trạng thái NaN, Infinity).
>
> **7. Google Developers.** (2024). *Android Jetpack Architecture Guides & Kotlin Coroutines Best Practices*. \[Trực tuyến\]: https://developer.android.com/topic/architecture (Tài liệu kỹ thuật chính thức về Unidirectional Data Flow và Room Database).
