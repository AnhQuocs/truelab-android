# Báo Cáo Audit Độ Bao Phủ Dữ Liệu Odds & Runtime (Odds & Runtime Data Coverage Audit)

**Ngày thực hiện:** 2026-09-29
**Mục tiêu:** Kiểm toán cơ sở dữ liệu runtime để xác minh tính khả dụng của dữ liệu, phân phối tỷ lệ cược (Odds) và các tín hiệu dự đoán (Prediction signals), tránh các giả định thiếu căn cứ.

---

## 1. Ảnh Chụp Trạng Thái Runtime (Runtime Snapshot)
- **Git HEAD tại thời điểm audit**: `d285a14` (docs: finalize Presentation P3)
- **Trạng thái Database**: Database runtime `truelab.db` đã được trích xuất và truy vấn phân tích trực tiếp qua Python Pandas.

---

## 2. Độ Bao Phủ Trận Đấu (Match Coverage)
- **Tổng số trận đấu (Total Matches)**: 156
- **Trận đấu đã kết thúc (`status='ended'`)**: 97
- **Trận đấu chờ / trạng thái khác**: 59 (pending, live, determined, cancelled, postponed)

*Đánh giá:* Tập dữ liệu ban đầu rất nhỏ (156 trận), cách xa mục tiêu quy mô "50,000–75,000 trận". Đây là tập dữ liệu thử nghiệm bị giới hạn nghiêm ngặt.

---

## 3. Độ Bao Phủ Bản Ghi Tỷ Lệ Cược (Odds Record Coverage)
- **Tổng số bản ghi Odds**: 550,962
- **Số bản ghi trung bình mỗi trận**: ~4,142
- **Dấu thời gian sớm nhất (Earliest timestamp)**: 1785625961
- **Dấu thời gian mới nhất (Latest timestamp)**: 1790666187

*Đánh giá:* Mặc dù số lượng trận đấu ban đầu chỉ có 156, chiều sâu lịch sử biến động odds lại cực kỳ lớn (trung bình hơn 4,000 snapshot odds trên mỗi trận).

---

## 4. Độ Bao Phủ Nhà Cái / Nhà Cung Cấp (Provider Coverage)
- **Số nhà cái riêng biệt (Distinct Providers)**: 17 công ty/nhà cung cấp.
- **Top nhà cái theo khối lượng bản ghi**:
  1. Sbobet (63,330)
  2. Easybets (62,666)
  3. BET365 (60,942)
  4. Vcbet (55,216)
  5. Pinnacle (46,635)
- **Số nhà cái trung bình mỗi trận**: Median đạt 14 nhà cái cùng đưa tỷ lệ cho 1 trận đấu.

*Đánh giá:* Độ phủ đa nhà cái (Multi-provider) rất tốt đối với các trận đấu hiện có.

---

## 5. Độ Bao Phủ Kèo Châu Âu 1X2 (1X2 Odds Coverage)
- **Phân loại loại kèo (Odds Types)**:
  - `bs` (Tài/Xỉu - Over/Under): 210,450
  - `eu` (Kèo Châu Âu 1X2): 171,008
  - `asia` (Kèo Châu Á - Asian Handicap): 140,193
  - `cr` (Tỷ số chính xác - Correct Score): 29,311
- **Độ bao phủ khả dụng của kèo 1X2 (`eu`)**: 133 trận đấu riêng biệt sở hữu đầy đủ tỷ lệ 1X2 (Chủ nhà + Hòa + Đội khách).

*Đánh giá:* 133/156 (~85%) trận đấu trong DB có tỷ lệ 1X2 hợp lệ. Hàm `getLatestOddsForMatch()` sẽ trả về dữ liệu 1X2 non-null cho 133 trận này.

---

## 6. Độ Bao Phủ Lịch Sử Biến Động Odds (Odds History Coverage)
- **Số trận có từ 10 snapshots trở lên**: 133 trận.
- **Phân bổ giai đoạn thị trường (Market Phases)**: `rolling_ball` (Trực tiếp trong trận) chiếm ưu thế với 464,324 bản ghi, `instant` (Cập nhật trước trận) có 81,726 bản ghi, và `initial` (Kèo mở sớm) có 4,912 bản ghi.

*Đánh giá:* Phân tích xu hướng biến động odds (Trend, Volatility, Moving Average) hoàn toàn khả thi nhờ chiều sâu snapshot phong phú trên từng trận.

---

## 7. Độ Bao Phủ Phong Độ (Form Coverage)
- **Tổng số đội bóng (Total Teams)**: 309
- **Đội có 0 trận đã kết thúc**: 115
- **Đội có 1 trận đã kết thúc**: 194
- **Đội có từ 2 trận đã kết thúc trở lên**: 0

*Đánh giá:* **DỮ LIỆU BỊ PHÂN TÁN/THIẾU HỤT NGHIÊM TRỌNG (SEVERE DATA SPARSITY)**. Không có đội bóng nào trong tập dữ liệu cục bộ ban đầu thi đấu quá 1 trận. Việc tính toán điểm phong độ (Form Score - thường yêu cầu 5 trận gần nhất) là bất khả thi nếu không có lịch sử đấu.

---

## 8. Độ Bao Phủ Elo (Elo Coverage)
- **Đội bóng có điểm Elo = 1500**: 309 (100%)
- **Đội bóng có điểm Elo != 1500**: 0

*Đánh giá:* **KHOẢNG TRỐNG TRONG DOMAIN PIPELINE (DOMAIN PIPELINE GAP)**. Điểm Elo hoàn toàn tĩnh ở mức mặc định 1500 do chưa có cơ chế tính toán và cập nhật Elo lịch sử chạy qua tập dữ liệu này.

---

## 9. Độ Bao Phủ Đối Đầu (H2H Coverage)
- **Số cặp đội từng gặp nhau**: 97 cặp.
- **Phân phối**: Mỗi cặp chỉ có duy nhất 1 trận đấu (97 trận đã kết thúc). Không có cặp nào gặp nhau lần thứ 2.

*Đánh giá:* **DỮ LIỆU BỊ THIẾU HỤT (DATA SPARSITY)**. Công cụ so sánh đối đầu H2H chỉ có thể hiển thị tối đa 1 trận lịch sử cho mỗi cặp.

---

## 10. Độ Bao Phủ Bàn Thắng (Goals Coverage)
- **Số trận có tỷ số Chủ/Khách hợp lệ**: 97 (Toàn bộ các trận đã kết thúc).

*Đánh giá:* Tín hiệu phân tích bàn thắng (Goals signal) có thể tính toán được cho 97 trận đấu này.

---

## 11. Phân Bổ Tín Hiệu Dự Đoán (Prediction Signal Distribution)
- **Tổng số bản ghi dự đoán lưu trong DB**: 0.

*Đánh giá:* Bảng `predictions` hoàn toàn rỗng. Các kết quả backtest trước đó được chạy trực tiếp in-memory khi tương tác UI chứ không được persist vào database. Do Form=0 và Elo=1500 cho mọi đội, mô hình WeightedScoring chủ yếu dựa vào Xác suất ngầm định từ Odds (Implied Probability) + Lợi thế sân nhà (Home Advantage).

---

## 12. Điều Tra Hiển Thị Odds Trên UI Trận Đấu
*Đánh giá:* Việc UI hiển thị dấu "-" cho tỷ lệ cược trong khi DB có 133 trận chứa đầy đủ kèo 1X2 `eu` chỉ ra vấn đề **KẾT NỐI TÍCH HỢP UI (UI INTEGRATION GAP)** hoặc **LỖI MAPPING DỮ LIỆU**. `OddsDao.getLatestOddsForMatch()` hoạt động chuẩn, nhưng luồng xử lý từ `OddsRepository` -> `UseCase` -> `ViewModel` -> `UI` bị gián đoạn hoặc lọc nhầm dữ liệu.

---

## 13. Phân Loại Nguyên Nhân Gốc (Root Cause Classification)
- **DATA SPARSITY (Thiếu hụt dữ liệu)**: Nguyên nhân khiến Form, H2H, và Elo chưa phát huy được giá trị là do thiếu dữ liệu lịch sử (mỗi đội chỉ có tối đa 1 trận). Cần đồng bộ sâu nhiều mùa giải lịch sử.
- **DOMAIN PIPELINE GAP (Thiếu pipeline tính toán)**: Điểm Elo chưa có worker/engine tính toán cập nhật theo dòng thời gian.
- **UI INTEGRATION GAP (Đứt gãy hiển thị UI)**: Dữ liệu Odds trong DB rất phong phú nhưng chưa được render đầy đủ lên giao diện.

---

## 14. Đề Xuất Các Bước Tiếp Theo (Recommended Next Phase)
Dựa trên bằng chứng thực tế từ runtime:
1. **Khắc phục hiển thị Odds trên UI**: Đảm bảo dữ liệu Odds phong phú từ DB được chuyển tải chính xác lên UI.
2. **Triển khai đồng bộ dữ liệu lịch sử quy mô lớn (Deep Historical Sync)**: Mở rộng dataset lên 15,000 → 50,000+ trận đấu để phục vụ các thuật toán Form, Elo và H2H.
3. **Persist kết quả Elo / Dự đoán**: Thiết lập cơ chế tính toán và lưu trữ Elo/Predictions khi cần.

---

## 15. Hạn Chế & Ghi Chú (Limitations)
- Kiểm toán sử dụng Python `sqlite3` trên bản snapshot cơ sở dữ liệu `truelab.db`.
- Không thể phân tích lịch sử dự đoán từ DB do bảng `predictions` chưa thực hiện persist.