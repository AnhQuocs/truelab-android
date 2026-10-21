# Báo Cáo Thực Nghiệm So Sánh Form-5 và Form-10 Trên Dữ Liệu Thực Tế TrueLab

> **Phương thức thực thi**: Full Production Kotlin Pipeline (100% production classes, Candidate A Decision Margin, Zero Temporal Data Leakage).
> **Nguồn dữ liệu**: Trích xuất trực tiếp từ SQLite Database thực tế trên thiết bị Android qua ADB (`truelab_database.db`, 75.590 matches).
> **Khung thời gian đánh giá**: 16/10/2026 00:00:00 -> 19/10/2026 23:59:59 (Giờ Việt Nam UTC+7).

---

## 1. Tổng Quan Cohort Đánh Giá & Dữ Liệu Thiết Bị

- **Thiết bị kết nối**: Android Device `13195704AS018155` (Package `dev.anhquocs.truelab`).
- **Tổng số trận ghi nhận trong khung giờ 16/10 -> 19/10 (VN)**: `846` trận.
- **Số trận FT hợp lệ đưa vào đánh giá**: `757` trận.
- **Số trận bị loại**: `89` trận.
  - Trạng thái `pending` (chưa đấu): 72 trận.
  - Trạng thái `postponed` (hoãn): 16 trận.
  - Trạng thái `live` (đang diễn ra): 1 trận.
- **Phân bổ theo ngày (Giờ Việt Nam)**:
  - 16/10/2026: 90 trận
  - 17/10/2026: 329 trận
  - 18/10/2026: 255 trận
  - 19/10/2026: 83 trận

---

## 2. Bảng So Sánh Hiệu Năng Form-5 vs Form-10 (Full Cohort N = 757)

| Chỉ số Đánh Giá | Form-5 (Production Default) | Form-10 (Candidate Window) | Chênh lệch (Form-10 - Form-5) |
| :--- | :---: | :---: | :---: |
| **Accuracy** | **49.67%** (376/757) | **48.75%** (369/757) | **-0.92 pp** |
| **Macro F1** | **0.4543** | **0.4460** | **-0.0083** |
| **Macro Precision** | 0.4551 | 0.4437 | -0.0114 |
| **Macro Recall** | 0.4658 | 0.4554 | -0.0104 |
| *Home Win Precision* | 58.98% | 59.52% | +0.53 pp |
| *Home Win Recall* | 58.81% | 58.81% | +0.00 pp |
| *Home Win F1* | 0.5889 | 0.5916 | +0.0027 |
| *Draw Precision* | 29.92% | 26.06% | -3.86 pp |
| *Draw Recall* | 19.90% | 19.37% | -0.52 pp |
| *Draw F1* | 0.2390 | 0.2222 | -0.0168 |
| *Away Win Precision* | 47.64% | 47.54% | -0.10 pp |
| *Away Win Recall* | 61.04% | 58.44% | -2.60 pp |
| *Away Win F1* | 0.5351 | 0.5243 | -0.0108 |

---

## 3. Ma Trận Nhầm Lẫn 3×3 (Confusion Matrix)

### Form-5
```text
                Predicted HW    Predicted D     Predicted AW    Total Actual
Actual HW:          197             57              81              335
Actual D:           79              38              74              191
Actual AW:          58              32              141             231
```

### Form-10
```text
                Predicted HW    Predicted D     Predicted AW    Total Actual
Actual HW:          197             63              75              335
Actual D:           80              37              74              191
Actual AW:          54              42              135             231
```

---

## 4. Phân Tích Độ Sâu Lịch Sử & Tính Nhất Quán (Discordance Analysis)

- **Số mẫu có đủ >= 5 trận lịch sử cho cả 2 đội**: `665` / `757` (87.8%)
- **Số mẫu có đủ >= 10 trận lịch sử cho cả 2 đội**: `596` / `757` (78.7%)
- **Hai mô hình đồng thuận (Cùng Đúng hoặc Cùng Sai)**: `710` trận (93.8%)
  - Cùng đoán đúng: `349` trận
  - Cùng đoán sai: `361` trận
- **Trận đấu phân kỳ (Discordant Matches)**: `47` trận (6.2%)
  - **Form-10 đúng, Form-5 sai**: **20 trận**
  - **Form-5 đúng, Form-10 sai**: **27 trận**
  - **Lợi thế ròng (Net Win)**: **-7 trận** nghiêng về Form-5

### Đánh giá riêng trên tập con có đủ >= 10 trận lịch sử (N = 596)
| Chỉ số | Form-5 | Form-10 | Chênh lệch |
| :--- | :---: | :---: | :---: |
| Accuracy | 50.17% (299/596) | 49.66% (296/596) | -0.50 pp |
| Macro F1 | 0.4555 | 0.4543 | -0.0012 |

---

## 5. Ước Lượng Thống Kê Bằng Paired Bootstrap (B = 2.000, Seed = 42)

- **Số lần lặp (Iterations)**: `2.000`
- **Random Seed**: `42`
- **Khoảng tin cậy 95% (95% Confidence Interval)**:
  - **Δ Accuracy (Form-10 - Form-5)**: Mean = **-0.93 pp**, 95% CI = `[-2.77 pp, +0.92 pp]`
  - **Δ Macro F1 (Form-10 - Form-5)**: Mean = **-0.0082**, 95% CI = `[-0.0281, +0.0107]`

---

## 6. Danh Sách Các Trận Đấu Phân Kỳ Tiêu Biểu

### A. Trận Form-10 Đoán Đúng còn Form-5 Đoán Sai (Top 10 tiêu biểu)
| Ngày | Cặp đấu | Tỷ số FT | Kết quả | Form-5 đoán | Form-10 đoán | Lịch sử (Home / Away) |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: |
| 16/10/2026 | Singida Fountain Gate vs Pamba Jiji | 1-1 | DRAW | AWAY_WIN | **DRAW** | 76 / 64 |
| 17/10/2026 | Burnley vs Wolverhampton Wanderers | 1-2 | AWAY_WIN | DRAW | **AWAY_WIN** | 102 / 100 |
| 17/10/2026 | Ventura County FC vs MINNESOTA United B | 3-2 | HOME_WIN | AWAY_WIN | **HOME_WIN** | 55 / 71 |
| 17/10/2026 | Ventura County FC vs MINNESOTA United B | 3-2 | HOME_WIN | AWAY_WIN | **HOME_WIN** | 55 / 71 |
| 17/10/2026 | SV Werder Bremen vs SC Paderborn 07 | 2-2 | DRAW | HOME_WIN | **DRAW** | 76 / 72 |
| 17/10/2026 | SV 07 Elversberg vs FC Augsburg | 4-3 | HOME_WIN | DRAW | **HOME_WIN** | 71 / 76 |
| 17/10/2026 | Dynamo Makhachkala vs Akron Togliatti | 1-1 | DRAW | AWAY_WIN | **DRAW** | 68 / 69 |
| 17/10/2026 | Bristol City vs Sheffield United | 0-1 | AWAY_WIN | HOME_WIN | **AWAY_WIN** | 112 / 113 |
| 17/10/2026 | SC Austria Lustenau vs LASK Linz | 0-4 | AWAY_WIN | DRAW | **AWAY_WIN** | 13 / 93 |
| 17/10/2026 | Al-Riffa vs East Riffa | 4-0 | HOME_WIN | DRAW | **HOME_WIN** | 62 / 35 |

### B. Trận Form-5 Đoán Đúng còn Form-10 Đoán Sai (Top 10 tiêu biểu)
| Ngày | Cặp đấu | Tỷ số FT | Kết quả | Form-5 đoán | Form-10 đoán | Lịch sử (Home / Away) |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: |
| 16/10/2026 | Sumgayit FK vs Araz Nakhchivan | 2-1 | HOME_WIN | **HOME_WIN** | DRAW | 80 / 82 |
| 16/10/2026 | FK Sloga Doboj vs NK Siroki Brijeg | 0-1 | AWAY_WIN | **AWAY_WIN** | DRAW | 81 / 83 |
| 16/10/2026 | FK Sloga Doboj vs NK Siroki Brijeg | 0-1 | AWAY_WIN | **AWAY_WIN** | DRAW | 81 / 83 |
| 17/10/2026 | Guingamp vs Pau FC | 1-2 | AWAY_WIN | **AWAY_WIN** | DRAW | 88 / 83 |
| 17/10/2026 | CD Leganes vs Cordoba | 0-1 | AWAY_WIN | **AWAY_WIN** | DRAW | 100 / 91 |
| 17/10/2026 | Oita Trinita vs Albirex Niigata | 5-1 | HOME_WIN | **HOME_WIN** | DRAW | 76 / 90 |
| 17/10/2026 | Jiangsu Yinhao (W) vs Shanghai (W) | 0-2 | AWAY_WIN | **AWAY_WIN** | DRAW | 64 / 60 |
| 17/10/2026 | Balestier Khalsa FC vs Geylang United FC | 0-0 | DRAW | **DRAW** | HOME_WIN | 55 / 56 |
| 17/10/2026 | Norwich City vs Portsmouth | 0-1 | AWAY_WIN | **AWAY_WIN** | DRAW | 111 / 102 |
| 17/10/2026 | Bracknell Town vs Chichester City | 1-0 | HOME_WIN | **HOME_WIN** | DRAW | 47 / 95 |

---

## 7. Kết Luận & Khuyến Nghị

1. **Hiệu năng thực tế**: Form-10 làm giảm độ chính xác tổng thể **-0.92 pp** (48.75% so với 49.67%) trên 757 trận FT thực tế.
2. **Độ ổn định thống kê**: Khoảng tin cậy 95% của Bootstrap chứa cả khoảng âm và dương, cho thấy việc mở rộng cửa sổ từ 5 lên 10 trận tạo ra sự đánh đổi giữa việc nắm bắt xu hướng dài hạn vs độ nhạy với phong độ ngắn hạn gần nhất.
3. **Khuyến nghị kiến trúc**: Giữ nguyên Form-5 làm mặc định cho Production theo đúng nguyên tắc an toàn, đồng thời duy trì khả năng cấu hình `formWindowSize` đã triển khai để phục vụ các module nghiên cứu và tùy biến người dùng.
