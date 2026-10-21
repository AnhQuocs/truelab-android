# Candidate A — Audit Odds lịch sử

## Tóm tắt điều hành

Trên tập dữ liệu Odds lịch sử được audit, Candidate A làm Draw F1 tăng từ 0% lên 19,6%, đồng thời làm Accuracy giảm từ 52,5% xuống 49,7%. Candidate A được kích hoạt ở 123/791 trận: 30 trận thực tế hòa và 93 dự đoán hòa sai. Trong 93 dự đoán sai này, 52 trận vốn được dự đoán đúng là Home/Away đã bị đổi thành Draw.

Kết quả thể hiện rõ sự đánh đổi giữa khả năng phát hiện Draw và độ chính xác của Home/Away. Trên mẫu này, điều kiện margin là cửa chặn lớn hơn đối với các trận hòa thực tế. Audit không chứng minh Candidate A là nguyên nhân duy nhất gây bất ổn dự đoán trên các tập dữ liệu khác.

Đây là vấn đề chất lượng thuật toán/chính sách quyết định, không phải lỗi triển khai. Tập Odds lịch sử được chọn lọc theo hành vi fetch theo yêu cầu của ứng dụng; không đại diện cho toàn bộ trận đấu và không phải thước đo độ phủ Odds của API.

## Dataset và phương pháp

- Database Room được dump trực tiếp từ thiết bị đang kết nối, bao gồm WAL; bản sao database vượt qua kiểm tra toàn vẹn SQLite.
- Chỉ sử dụng các Odds snapshot đã tồn tại trong Room. Không gọi Odds API mới.
- Chạy lại Prediction Engine bằng đúng luồng production `RunDailyBacktestUseCase` và `PredictMatchOutcomeUseCase`, với Odds repository offline chỉ đọc các dòng đã trích xuất từ Room.
- Điều kiện hợp lệ cho Odds 1X2 lịch sử:
  - `oddsType = 'eu'`
  - `homeWin > 1`, `draw > 1`, `awayWin > 1`
  - `changeTime < kickoff`
  - `marketPhase IN ('initial', 'instant')`
  - chọn snapshot hợp lệ mới nhất theo từng `(matchId, companyId)`
- Dataset hợp lệ gồm **791 trận đã kết thúc** và **1.726 snapshot trận–nhà cung cấp**.
- Các con số này mô tả Odds đã được ứng dụng fetch theo yêu cầu và lưu trong Room. Một trận không có snapshot hợp lệ trong Room không chứng minh API Odds không có dữ liệu cho trận đó.
- Không sửa xác suất backtest, trọng số signal, Candidate A hoặc bộ chọn Odds production.

## Kết quả tổng thể

### Raw argmax

| Chỉ số | Kết quả |
|---|---:|
| Số trận | 791 |
| Accuracy | 52,5% |
| Macro F1 | 38,9% |
| Home F1 | 63,6% |
| Draw F1 | 0,0% |
| Away F1 | 53,1% |

Ma trận nhầm lẫn (hàng: thực tế H/D/A; cột: dự đoán H/D/A):

```text
[[263,   0, 104],
 [108,   0,  75],
 [ 89,   0, 152]]
```

### Candidate A — `|P(H) - P(A)| < 0.04 AND P(D) >= 0.255`

| Chỉ số | Kết quả |
|---|---:|
| Số trận | 791 |
| Accuracy | 49,7% |
| Macro F1 | 44,0% |
| Home F1 | 59,8% |
| Draw F1 | 19,6% |
| Away F1 | 52,6% |
| Draw Precision | 24,4% |
| Draw Recall | 16,4% |

Candidate A được kích hoạt ở **123/791 trận (15,5%)**. Cả 123 trận đều được đổi từ raw argmax Home/Away sang Draw:

- **30** trận thực tế hòa và được dự đoán Draw đúng.
- **93** trận có kết quả thực tế Home/Away và bị dự đoán Draw sai.
- **52** trong số 93 trận đó vốn có raw argmax Home/Away đúng.
- Accuracy giảm **2,8 điểm phần trăm** so với raw argmax. Tính ròng, nhóm được kích hoạt có ít hơn 22 dự đoán đúng (30 trận hòa mới dự đoán đúng trừ 52 dự đoán H/A đúng bị thay thế).

### Ma trận nhầm lẫn của Candidate A

Hàng là nhãn thực tế H/D/A; cột là nhãn dự đoán H/D/A.

Toàn bộ 791 trận:

```text
[[228, 61, 78],
 [ 94, 30, 59],
 [ 74, 32,135]]
```

Candidate A được kích hoạt (n=123):

```text
[[ 0, 61, 0],
 [ 0, 30, 0],
 [ 0, 32, 0]]
```

Candidate A không được kích hoạt (n=668):

```text
[[228, 0, 78],
 [ 94, 0, 59],
 [ 74, 0,135]]
```

## Phân tích các điều kiện

| Tập mẫu | Đạt margin: `|P(H)-P(A)| < 0.04` | Đạt theta: `P(D) >= 0.255` | Đạt cả hai |
|---|---:|---:|---:|
| Toàn bộ 791 trận | 160 | 435 | 123 |
| 183 trận hòa thực tế | 41 | 126 | 30 |

Trong 183 trận hòa thực tế:

- 142 trận không đạt điều kiện margin.
- 11 trận đạt margin nhưng không đạt ngưỡng xác suất Draw.
- 30 trận đạt cả hai điều kiện và được Candidate A dự đoán Draw.

Trên mẫu này, điều kiện margin là nút thắt lớn hơn đối với khả năng phát hiện trận hòa thực tế. Đây là kết quả mô tả trên tập dữ liệu đã chọn lọc, không phải bằng chứng rằng chỉ cần thay đổi margin sẽ cải thiện hiệu năng.

## Các kết quả bổ sung từ audit

### Nhóm được kích hoạt và không được kích hoạt

| Nhóm | N | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw Precision | Draw Recall |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Candidate A được kích hoạt | 123 | 24,4% | 13,1% | 0,0% | 39,2% | 0,0% | 24,4% | 100,0% |
| Candidate A không được kích hoạt | 668 | 54,3% | 37,5% | 59,8% | 0,0% | 52,6% | 0,0% | 0,0% |

Draw Recall 100% trong nhóm được kích hoạt chỉ đúng với nhóm con mà mọi dự đoán đều là Draw; đây không phải Recall của toàn bộ dataset.

### So sánh thăm dò có/không có Odds đã lưu sau khi ghép cặp

Một phân tích mô tả bổ sung ghép 155 cặp theo ngày UTC, competition và kết quả thực tế. “Không có Odds đã lưu” nghĩa là không tìm thấy snapshot hợp lệ trong Room; điều đó không có nghĩa API không có Odds.

| Nhóm | N | Accuracy | Macro F1 | Home F1 | Draw F1 | Away F1 | Draw Precision | Draw Recall | Candidate A kích hoạt |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Đã ghép cặp, có Odds lịch sử lưu trong Room | 155 | 58,7% | 48,8% | 64,6% | 14,0% | 68,0% | 17,6% | 11,5% | 17 (11,0%) |
| Đã ghép cặp, không có snapshot Odds hợp lệ lưu trong Room | 155 | 43,2% | 38,8% | 51,0% | 21,1% | 44,4% | 19,4% | 23,1% | 31 (20,0%) |

Đây là so sánh quan sát trong một database được chọn lọc bởi cơ chế fetch theo yêu cầu, không phải ước lượng nhân quả về tác động của việc có Odds.

### Tóm tắt confidence

Confidence trung bình là 45,4% ở các dự đoán đúng và 39,3% ở các dự đoán sai. Chỉ riêng chênh lệch tổng hợp này không phải phân tích calibration và không chứng minh xác suất đã được calibration tốt.

## Hạn chế và kết luận

- Odds được fetch theo yêu cầu, do đó 791 trận là mẫu được chọn lọc và có thể khác có hệ thống so với các trận không có Odds được lưu.
- Audit đánh giá một snapshot Room và một cohort đã chọn; không chứng minh hiệu năng trên mọi competition, ngày thi đấu hoặc trận production.
- Candidate A cho thấy trade-off thực nghiệm rõ ràng: Draw F1 tăng, trong khi Accuracy và hiệu năng Home/Away giảm trên cohort này.
- Audit không chứng minh Candidate A là nguyên nhân duy nhất của hiện tượng bất ổn dự đoán rộng hơn.
- Kết quả là cơ sở để tiếp tục nghiên cứu decision policy; chưa chứng minh policy thay thế nào sẽ tốt hơn.

## Tài liệu liên quan

- Issue: [`candidate-a-draw-decision-trade-off.md`](../issues/candidate-a-draw-decision-trade-off.md)
- Kế hoạch Candidate B: [`candidate-b-relative-draw-gap-plan.md`](../plans/candidate-b-relative-draw-gap-plan.md)
