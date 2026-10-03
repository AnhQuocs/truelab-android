# Issue: Xây dựng mô hình toán học cho Rest Advantage

## 1. Bối cảnh

TrueLab hiện có 6 prediction signals:

1. Odds
2. Elo
3. Form
4. Goals
5. H2H
6. Home Advantage

Audit trước đó xác định Home Advantage có semantic mismatch vì dataset hiện tại không có metadata đủ để xác định một fixture có thực sự diễn ra trên sân nhà hay sân trung lập.

Rest Advantage được chọn để thay thế Home Advantage.

Feasibility audit đã xác nhận dữ liệu hiện tại đủ để tính Rest Advantage từ previous completed match của mỗi đội và không cần thay đổi Data Layer. 

---

## 2. Problem

Cần chuyển:

```text
homeRestDays
awayRestDays
```

thành:

```text
Signal3Way(Home, Draw, Away)
```

một cách nhất quán, bounded và temporal-safe.

Hiện chưa có mathematical contract chính thức.

Feasibility audit mới chỉ đưa ra các candidate như sigmoid/tanh, vùng cân bằng ±0.5 ngày và saturation khoảng ±3 ngày; các giá trị này chưa được xác thực trên distribution thực tế.

---

## 3. Proposed input

```text
targetKickoff
homePreviousKickoff
awayPreviousKickoff
```

Tính:

```text
homeRestDays =
    (targetKickoff - homePreviousKickoff) / 86400

awayRestDays =
    (targetKickoff - awayPreviousKickoff) / 86400

deltaRest =
    homeRestDays - awayRestDays
```

Interpretation:

```text
deltaRest > 0 → Home nghỉ lâu hơn
deltaRest < 0 → Away nghỉ lâu hơn
deltaRest ≈ 0 → cân bằng
```

---

## 4. Requirements

Model phải:

* temporal-safe
* đối xứng Home/Away
* monotonic theo `deltaRest`
* bounded
* normalized
* neutral tại `deltaRest = 0`
* không phụ thuộc fixture-side để tạo bias Home
* xử lý missing previous match rõ ràng
* không tạo probability bất hợp lệ.

---

## 5. Investigation

Phân tích dataset hiện tại để xác định:

* distribution của `deltaRest`
* typical rest difference
* extreme rest difference
* sensitivity phù hợp
* saturation cần thiết
* tỷ lệ missing previous match.

So sánh:

* Linear
* Sigmoid
* Tanh

Không sử dụng Accuracy/F1 làm objective để lựa chọn mathematical model.

---

## 6. Constraints

### Không thay đổi

* Odds transformer
* Elo transformer
* Form transformer
* Goals transformer
* H2H transformer
* Draw logic
* prediction weights
* Data Layer
* database schema.

### Không implement

Đây là modeling phase.

Chưa tạo production implementation.

---

## 7. Expected output

Issue phải kết thúc bằng một mathematical contract đủ cụ thể để phase tiếp theo có thể implement trực tiếp:

```text
RestAdvantageSignalTransformer
```

Contract cần bao gồm:

```text
Input
↓
Rest calculation
↓
deltaRest
↓
Transformation function
↓
Baseline / Draw handling
↓
Normalization
↓
Signal3Way
```

---

## 8. Acceptance Criteria

* [ ] Có distribution analysis thực tế.
* [ ] Có candidate comparison.
* [ ] Có một formula được chọn.
* [ ] Có sensitivity.
* [ ] Có saturation behavior.
* [ ] Có missing-data behavior.
* [ ] `deltaRest = 0` trả neutral.
* [ ] `+x` và `-x` có behavior đối xứng.
* [ ] Model monotonic.
* [ ] Tất cả probabilities nằm trong `[0,1]`.
* [ ] Tổng probabilities bằng `1`.
* [ ] Không có temporal leakage.
* [ ] Không sửa production source.
* [ ] Không thay đổi Draw logic.
* [ ] Không lựa chọn model dựa trên prediction accuracy.
