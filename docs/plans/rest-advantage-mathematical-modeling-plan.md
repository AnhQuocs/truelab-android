# Implementation Plan: Rest Advantage Mathematical Modeling

## 1. Mục tiêu

Xác định contract toán học hoàn chỉnh cho Rest Advantage trước khi triển khai production.

Rest Advantage sử dụng chênh lệch thời gian nghỉ giữa hai đội trước thời điểm kickoff:

```text
homeRestDays = (targetKickoff - homePreviousKickoff) / 86400
awayRestDays = (targetKickoff - awayPreviousKickoff) / 86400

deltaRest = homeRestDays - awayRestDays
```

Trong đó:

* `deltaRest > 0`: Home nghỉ lâu hơn.
* `deltaRest < 0`: Away nghỉ lâu hơn.
* `deltaRest ≈ 0`: Hai đội có thời gian nghỉ tương đương.

Mục tiêu của phase này là xác định:

```text
deltaRest → Signal3Way
```

một cách có cơ sở từ dữ liệu hiện tại.

---

## 2. Phạm vi

### In Scope

* Phân tích distribution thực tế của `deltaRest`.
* Xác định vùng cân bằng.
* Xác định sensitivity phù hợp.
* Đánh giá các hàm chuyển đổi:
  * Linear
  * Sigmoid
  * Tanh
* Xác định saturation/cap.
* Xác định baseline khi Rest Advantage không có lợi thế rõ ràng.
* Xác định fallback khi thiếu previous match.
* Xác định contract output `Signal3Way`.
* Kiểm tra tính hợp lệ:

```text
P(Home) + P(Draw) + P(Away) = 1
```

* Đánh giá numerical stability và boundary behavior.
* Xác định test cases cần thiết cho implementation phase.

### Out of Scope

* Không sửa production source code.
* Không tích hợp Rest vào Prediction pipeline.
* Không thay đổi 6 signal weights.
* Không thay đổi Draw logic.
* Không tuning model theo Accuracy/F1.
* Không chạy A/B backtest để chọn công thức.
* Không mở rộng dataset 30k → 50k.
* Không thêm DAO/API/database schema.
* Không thay đổi Odds/Elo/Form/H2H/Goals.
* Không kết luận Rest Advantage cải thiện prediction accuracy.

---

## 3. Nguyên tắc thiết kế

### 3.1 Temporal safety

Chỉ sử dụng trận đã kết thúc trước kickoff:

```text
previousMatch.isEnded == true
previousMatch.startTimeDate < targetKickoff
```

Không sử dụng dữ liệu tương lai.

### 3.2 Sử dụng kickoff timestamp

Dataset hiện tại không có full-time timestamp đáng tin cậy cho pipeline này.

Do đó:

```text
Rest = targetKickoff - previousMatchKickoff
```

được xem là modeling approximation.

Không được mô tả đây là thời gian hồi phục sinh lý thực tế.

### 3.3 Rest Advantage không được biến thành Home Advantage

Rest signal phải phụ thuộc vào:

```text
homeRestDays - awayRestDays
```

không được mặc định:

```text
Home > Away
```

chỉ vì Home là đội chủ nhà.

### 3.4 Baseline cân bằng

Khi hai đội có thời gian nghỉ tương đương, Rest signal phải trở về trạng thái neutral:

```text
[Home, Draw, Away] = [0.37, 0.26, 0.37]
```

Đây là baseline neutral hiện đang được sử dụng trong prediction pipeline.

### 3.5 Không thay đổi Draw

Trong phase này không được tìm cách làm Draw dễ thắng argmax hơn.

Rest chỉ thay thế signal Home Advantage.

---

## 4. Phân tích dữ liệu

Sử dụng dataset hiện tại để tính `deltaRest` cho các match có đủ previous match của cả hai đội.

Phân tích tối thiểu:

* count
* min
* max
* mean
* median
* standard deviation
* percentile:
  * P5
  * P10
  * P25
  * P50
  * P75
  * P90
  * P95

Phân nhóm:

```text
|ΔRest| < 0.25 day
0.25–0.5
0.5–1
1–2
2–3
3–5
5–7
> 7
```

Đồng thời phân tích riêng:

```text
deltaRest < 0
deltaRest ≈ 0
deltaRest > 0
```

Mục đích là hiểu distribution trước khi chọn threshold.

Không được mặc định rằng các threshold trong feasibility audit là đúng.

---

## 5. Candidate mathematical models

Đánh giá ít nhất ba candidate.

### 5.1 Linear

Một dạng bounded linear mapping:

```text
bias = clamp(deltaRest / sensitivity, -1, 1)
```

Sau đó chuyển bias thành Home/Away probability quanh neutral baseline.

Phải kiểm tra:

* continuity
* boundary
* saturation
* symmetry

### 5.2 Tanh

Candidate:

```text
bias = tanh(deltaRest / sensitivity)
```

Ưu điểm cần đánh giá:

* smooth
* symmetric
* naturally bounded
* diminishing returns với extreme rest difference

### 5.3 Sigmoid

Candidate symmetric:

```text
homeAwayShare = sigmoid(k * deltaRest)
```

Cần thiết kế lại để giữ draw probability và đảm bảo tổng probability bằng 1.

Không được chọn candidate chỉ vì “phổ biến” hoặc “nghe hợp lý”.

---

## 6. Baseline và normalization

Model phải đảm bảo:

```text
P(H) + P(D) + P(A) = 1
```

với mọi input hợp lệ.

Kiểm tra:

* `deltaRest = 0`
* deltaRest rất nhỏ
* deltaRest dương lớn
* deltaRest âm lớn
* floating-point precision
* extreme input

Kết quả phải giữ tính đối xứng:

```text
f(+x) → Home advantage
f(-x) → Away advantage
```

và:

```text
f(0) → neutral
```

---

## 7. Saturation / extreme rest

Phân tích dữ liệu để quyết định có cần cap hay saturation hay không.

Không mặc định:

```text
cap = 10 days
```

hoặc:

```text
cap = 14 days
```

Các giá trị này chỉ là candidate từ feasibility audit.

Nếu dùng tanh/sigmoid thì cần xác định sensitivity sao cho extreme rest không tạo probability phi thực tế.

---

## 8. Missing-data behavior

Xác định behavior khi một hoặc cả hai đội không có previous completed match.

Candidate:

### Option A — Neutral

```text
Signal = [0.37, 0.26, 0.37]
```

### Option B — Signal unavailable

```text
weight = 0
```

Phase này phải phân tích trade-off và chọn một contract duy nhất.

Không implement cả hai.

---

## 9. Contract đề xuất

Sau phase này phải chốt được contract tương đương:

```text
RestAdvantageSignalTransformer
    input:
        targetKickoff
        homePreviousKickoff?
        awayPreviousKickoff?

    output:
        Signal3Way
```

Contract phải xác định rõ:

* đơn vị thời gian
* null handling
* threshold/sensitivity
* mathematical function
* saturation
* baseline
* probability bounds
* normalization

---

## 10. Validation

Validation của R2 chỉ tập trung vào mathematical properties.

Không dùng prediction accuracy để chọn model.

Phải kiểm tra:

### Zero difference

```text
deltaRest = 0
```

→ neutral.

### Symmetry

```text
deltaRest = +x
deltaRest = -x
```

→ Home/Away được đối xứng.

### Monotonicity

Khi Home nghỉ lâu hơn:

```text
deltaRest ↑
```

thì Home probability không được giảm.

Khi Away nghỉ lâu hơn:

```text
deltaRest ↓
```

thì Away probability không được giảm.

### Boundedness

```text
0 <= P <= 1
```

### Normalization

```text
P(H) + P(D) + P(A) = 1
```

### Saturation

Extreme deltaRest không được tạo probability vượt ngoài contract.

### Temporal safety

Model không cần và không được sử dụng match sau kickoff.

---

## 11. Deliverables

R2 phải tạo:

1. Mathematical modeling report.
2. Distribution analysis của `deltaRest`.
3. Candidate model comparison.
4. Mathematical contract cuối cùng.
5. Quyết định về missing-data behavior.
6. Quyết định sensitivity/saturation.
7. Test specification cho implementation phase.

Không thay đổi production source code.

---

## 12. Exit Criteria

R2 chỉ hoàn thành khi:

* [ ] Distribution `deltaRest` đã được phân tích.
* [ ] Các candidate formula đã được so sánh.
* [ ] Một mathematical model được chọn.
* [ ] Sensitivity đã được xác định.
* [ ] Saturation behavior đã được xác định.
* [ ] Missing-data behavior đã được xác định.
* [ ] Neutral baseline đã được xác định.
* [ ] Symmetry đã được kiểm chứng.
* [ ] Monotonicity đã được kiểm chứng.
* [ ] Probability normalization đã được kiểm chứng.
* [ ] Không có production source change.
* [ ] Không thay đổi Draw logic.
* [ ] Không tuning theo prediction accuracy.
* [ ] Có đủ contract để viết implementation plan tiếp theo.
