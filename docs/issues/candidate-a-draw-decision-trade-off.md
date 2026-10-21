# Trade-off trong quyết định Draw của Candidate A

- **Trạng thái:** Đã đánh giá Candidate B (Closed - Candidate B REJECTED, Candidate A giữ nguyên làm mặc định)
- **Phân loại:** Chất lượng thuật toán / decision policy
- **Loại:** Vấn đề cần nghiên cứu — không phải lỗi triển khai

## Vấn đề

Candidate A hiện tại dự đoán Draw khi:

```text
|P(H) - P(A)| < 0.04 AND P(D) >= 0.255
```

Trên cohort Odds lịch sử được audit, policy cải thiện khả năng phát hiện Draw nhưng đồng thời thay thế một số dự đoán Home/Away vốn đúng. Câu hỏi cần nghiên cứu là liệu một quy tắc quyết định khác có thể cải thiện sự đánh đổi này trên dữ liệu chưa được dùng để chọn tham số hay không.

## Bằng chứng

Trên 791 trận đã kết thúc, có Odds lịch sử hợp lệ được lưu sẵn trong Room:

- Raw argmax: Accuracy **52,5%**, Macro F1 **38,9%**, Draw F1 **0%**.
- Candidate A: Accuracy **49,7%**, Macro F1 **44,0%**, Draw F1 **19,6%**.
- Candidate A được kích hoạt **123/791 lần (15,5%)**.
- Nhóm được kích hoạt có **30 Draw true-positive** và **93 Draw false-positive**.
- **52** dự đoán Draw sai đã thay thế một dự đoán H/A raw argmax vốn đúng.
- Trên toàn cohort, có thêm 30 trận hòa được dự đoán đúng nhưng mất 52 dự đoán H/A đúng; Accuracy giảm **2,8 điểm phần trăm**.
- Trong 183 trận hòa thực tế, 142 trận không đạt điều kiện margin, 11 trận đạt margin nhưng không đạt ngưỡng P(D), và 30 trận đạt cả hai.

Odds được fetch theo yêu cầu; audit chỉ dùng snapshot hợp lệ đã có trong Room. Đây là mẫu được chọn lọc, không phải thước đo độ phủ Odds API.

Đầy đủ kết quả và ma trận nhầm lẫn: [`candidate-a-historical-odds-audit.md`](../audits/candidate-a-historical-odds-audit.md).

## Tác động

Trên cohort này, policy quan sát được làm tăng Draw F1 nhưng giảm Accuracy tổng thể và Home F1. Thay đổi decision rule có thể làm thay đổi các kết quả Home/Away được gán thành Draw; tác động lên các cohort khác chưa được xác định.

## Nguyên nhân gốc / giả thuyết

Candidate A kết hợp điều kiện margin Home/Away tuyệt đối với ngưỡng xác suất Draw tối thiểu. Trong mẫu này, điều kiện margin là cửa chặn lớn hơn đối với các trận hòa thực tế: 142/183 trận không đạt margin, so với 11 trận đạt margin nhưng không đạt ngưỡng P(D).

Giả thuyết cần kiểm chứng là so sánh trực tiếp xác suất Draw với xác suất lớn hơn giữa H/A có thể tạo ra ranh giới quyết định hữu ích hơn. Giả thuyết này chưa được xác minh; audit không chứng minh policy hiện tại là nguyên nhân duy nhất gây bất ổn dự đoán.

## Hướng đề xuất

Nghiên cứu **Candidate B — Relative Draw Gap**:

```text
drawDeficit = max(P(H), P(A)) - P(D)
```

Giữ nguyên dự đoán Draw của raw argmax. Nếu raw argmax không phải Draw thì dự đoán Draw khi `drawDeficit <= τ`; nếu không, giữ raw argmax. Chỉ chọn `τ` trên tập calibration chia theo thời gian và khóa tham số trước khi đánh giá holdout.

Candidate B mới là đề xuất, chưa được chứng minh tốt hơn. Issue này không cho phép triển khai lên production.

Kế hoạch triển khai: [`candidate-b-relative-draw-gap-plan.md`](../plans/candidate-b-relative-draw-gap-plan.md).

## Tiêu chí chấp nhận

- Đánh giá raw argmax, Candidate A và Candidate B trên cùng input đã cố định và tập temporal holdout không được dùng để chọn `τ`.
- Báo cáo Accuracy, Macro F1, Home/Draw/Away F1, Draw Precision/Recall/F1 và ma trận nhầm lẫn.
- Báo cáo số dự đoán Draw, Draw TP/FP và số dự đoán H/A đúng bị đổi thành Draw.
- Chỉ xem Candidate B tốt hơn nếu kết quả holdout chứng minh trade-off Draw so với H/A được cải thiện theo các giới hạn non-inferiority đã đăng ký trước.
- Nếu holdout quá nhỏ, khoảng tin cậy chưa kết luận được hoặc không đạt tiêu chí, ghi nhận kết quả là chưa kết luận được hoặc loại Candidate B; không tinh chỉnh dựa trên holdout.

## Audit liên quan

- [`candidate-a-historical-odds-audit.md`](../audits/candidate-a-historical-odds-audit.md)

## Kế hoạch triển khai liên quan

- [`candidate-b-relative-draw-gap-plan.md`](../plans/candidate-b-relative-draw-gap-plan.md)
