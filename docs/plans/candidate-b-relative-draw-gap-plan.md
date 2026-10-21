# Candidate B — Kế hoạch triển khai Relative Draw Gap

**Trạng thái:** Đã hoàn thành (Closed - REJECTED). Xem báo cáo nghiệm thu tại [`candidate-b-relative-draw-gap-final.md`](../reports/final/candidate-b-relative-draw-gap-final.md) và issue [`candidate-b-relative-draw-gap-evaluation.md`](../issues/candidate-b-relative-draw-gap-evaluation.md). Candidate A tiếp tục là mặc định production.

## Mục tiêu

Đánh giá một decision policy Draw thay thế, có khả năng cải thiện phát hiện Draw đồng thời hạn chế ảnh hưởng tiêu cực đến dự đoán Home/Away, mà không thay đổi sáu signal hoặc trọng số của chúng.

## Candidate A hiện tại

Candidate A áp dụng lên xác suất raw sau khi tính weighted score:

```text
|P(H) - P(A)| < 0.04 AND P(D) >= 0.255
    => dự đoán DRAW
ngược lại
    => dùng raw argmax
```

Giữ nguyên các ngưỡng này khi dùng Candidate A làm phương án đối chiếu.

## Candidate B — Relative Draw Gap

Định nghĩa:

```text
drawDeficit = max(P(H), P(A)) - P(D)
```

Quyết định:

```text
nếu raw argmax là DRAW:
    giữ DRAW
ngược lại nếu drawDeficit <= τ:
    dự đoán DRAW
ngược lại:
    giữ raw argmax
```

`τ` là mức dung sai theo đơn vị xác suất, thể hiện P(D) có thể thấp hơn xác suất H/A lớn nhất bao nhiêu mà vẫn được chọn là Draw. Giá trị `τ` **chưa được chọn**. Candidate B là giả thuyết, chưa phải giải pháp đã được chứng minh.

## Quy trình chọn tham số

- Chỉ chọn `τ` trên tập calibration được sắp theo thời điểm kickoff.
- Xác định ranh giới calibration/holdout trước khi xem metric holdout của Candidate B. Dùng các trận sớm hơn cho calibration và các trận muộn hơn cho holdout; không chia ngẫu nhiên.
- Giới hạn các giá trị `τ` ứng viên vào một lưới xác định được chỉ từ calibration (ví dụ: các giá trị `drawDeficit` không âm duy nhất, đã sắp xếp, cùng mức baseline không tạo thêm dự đoán Draw).
- Chọn trước mục tiêu calibration và ràng buộc non-inferiority cho Home/Away trước khi chạy quét tham số.
- Khóa `τ` được chọn, ngữ nghĩa quyết định và mã đánh giá trước khi chạy holdout.
- Không điều chỉnh lại `τ` dựa trên kết quả holdout. Nếu holdout có quá ít trận hòa hoặc độ bất định lớn, báo cáo **chưa đủ bằng chứng**.

## Kiến trúc và đường dẫn code production

### Luồng hiện tại

`PredictMatchOutcomeUseCase` tạo các signal Form, Elo, Goals, Odds, H2H và Rest Advantage, sau đó gọi `WeightedScorer.predictOutcome`. Vector xác suất raw được chuyển đến `DrawDecisionPolicy.resolvePredictedOutcome`; confidence được gán sau đó dựa trên xác suất của nhãn cuối cùng.

Các file liên quan hiện tại:

- [`DrawDecisionPolicy.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/policy/DrawDecisionPolicy.kt): `resolvePredictedOutcome` điều phối nhãn cuối; `applyDecisionMargin` hiện thực Candidate A.
- [`DrawStrategyConfig.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/DrawStrategyConfig.kt): chứa `DrawMarginConfig`, `DynamicDrawPriorConfig` và `DrawStrategyConfig`.
- [`DrawModelingStrategy.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/model/DrawModelingStrategy.kt): hiện có `BASELINE`, `DECISION_MARGIN` và `DYNAMIC_DRAW_PRIOR`.
- [`PredictMatchOutcomeUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/prediction/usecase/PredictMatchOutcomeUseCase.kt): gọi sáu transformer, weighted scorer rồi đến draw policy cuối.
- [`RunDailyBacktestUseCase.kt`](../../core/domain/src/main/kotlin/dev/anhquocs/truelab/core/domain/evaluation/usecase/RunDailyBacktestUseCase.kt): truyền draw strategy có thể cấu hình vào prediction use case production cho từng trận.

### Tách biệt cần thiết trong thiết kế

Hiện tại `DrawModelingStrategy` quyết định hành vi ở cả Form/Elo transformers và final decision policy. Cụ thể, `DYNAMIC_DRAW_PRIOR` đã tồn tại và thay đổi cách tạo xác suất Draw trong `FormSignalTransformer` và `EloSignalTransformer`; không được tái sử dụng chiến lược này cho Relative Draw Gap.

Trước khi triển khai, cần tách quy tắc quyết định cuối khỏi mô hình tạo xác suất của signal. Ưu tiên một cấu hình decision policy nhỏ (ví dụ: quy tắc quyết định tường minh và `RelativeDrawGapConfig`), chỉ được `DrawDecisionPolicy` sử dụng; giữ cấu hình signal-model hiện tại cho các chiến lược hiện có. Candidate A tiếp tục là mặc định/phương án dự phòng; giữ nguyên hành vi `BASELINE` và `DYNAMIC_DRAW_PRIOR`. Không thay đổi hành vi Form/Elo transformer để định tuyến Candidate B.

Phạm vi triển khai dự kiến, chờ review API cuối:

1. `DrawDecisionPolicy.kt`: thêm hàm quyết định Relative Draw Gap thuần và định tuyến theo rule. Giữ nguyên xác suất raw.
2. `DrawStrategyConfig.kt` hoặc file cấu hình decision riêng: thêm lựa chọn decision rule tường minh và cấu hình `τ`, không trộn lẫn với dynamic signal prior. Giữ nguyên ngưỡng Candidate A.
3. `PredictMatchOutcomeUseCase.kt`: truyền decision rule được chọn vào policy sau khi scoring. Giữ nguyên việc tạo sáu signal, weights, xác suất raw và probability evidence. Duy trì cách tính confidence theo xác suất của nhãn được chọn cuối cùng.
4. `RunDailyBacktestUseCase.kt`: truyền tiếp decision config để chạy cả ba policy trên cùng input trận. Giữ nguyên cấu hình mặc định hiện tại.
5. Kiểm thử:
   - `DrawDecisionPolicyTest`: các trường hợp biên, giữ raw Draw, `drawDeficit == τ`, và giá trị ngay trên/dưới `τ`.
   - `DrawStrategyAbstractionTest`: kiểm tra định tuyến, tương thích với mặc định và xác nhận Candidate A / Dynamic Draw Prior không đổi.
   - `PredictMatchOutcomeUseCaseTest` và `RunDailyBacktestUseCaseTest`: xác nhận xác suất/evidence raw không đổi và policy được truyền đúng.
   - Các test transformer Form/Elo hiện có: kiểm thử hồi quy để xác nhận output không đổi khi chọn decision rule mới.

Dự kiến không cần thay đổi Data, Room, Odds API, schema hoặc `:core:algorithm`.

## Ngoài phạm vi và các bất biến

Candidate B không được thay đổi:

- Form
- Elo
- Goals
- Odds
- H2H
- Rest Advantage
- Trọng số prediction
- `WeightedScorer`
- Cách tính xác suất raw
- Lệnh gọi Odds API hoặc cách chọn Odds
- Room schema

Chỉ thay đổi quyết định nhãn Draw so với Home/Away cuối cùng. Không thay đổi ngưỡng Candidate A và không diễn giải việc thiếu Odds record là upstream API không có dữ liệu.

## Thực nghiệm

Chạy ba decision mode sau trên cùng cohort Odds lịch sử đã cố định và cùng xác suất raw do production tạo:

1. **Raw argmax** — đối chứng/tham chiếu.
2. **Candidate A** — rule hiện tại đã đóng băng (`δ = 0.04`, `θ = 0.255`).
3. **Candidate B** — Relative Draw Gap, với `τ` được chọn trên calibration và khóa trước holdout.

Dùng 791 trận trong [`candidate-a-historical-odds-audit.md`](../audits/candidate-a-historical-odds-audit.md), chỉ tái sử dụng Odds snapshot và record lịch sử đã trích xuất cho audit đó. Không fetch Odds lần nữa. Giữ nguyên nhãn thực tế, signal context và xác suất raw của từng trận giữa các mode. Chia theo thứ tự kickoff; không chia ngẫu nhiên.

Báo cáo riêng calibration và holdout. Cohort 791 trận có Odds được fetch theo yêu cầu là mẫu chọn lọc, không đại diện cho toàn bộ trận trong Room. Đồng thời báo cáo kích thước từng split, số lượng H/D/A thực tế, mức độ tập trung theo competition, cùng các dòng bị loại hoặc không hợp lệ.

## Các metric bắt buộc

Với raw argmax, Candidate A và Candidate B, báo cáo riêng cho calibration và holdout:

- Accuracy
- Macro F1
- Home F1
- Draw F1
- Away F1
- Draw Precision
- Draw Recall
- Ma trận nhầm lẫn (hàng nhãn thực tế H/D/A, cột nhãn dự đoán H/D/A)
- Số lượng và tỷ lệ dự đoán Draw
- Draw TP và FP
- Số dự đoán raw argmax H/A vốn đúng bị đổi thành Draw
- Số lần Candidate A được kích hoạt hoặc số quyết định Candidate B phát sinh do `τ`

Bổ sung khoảng bất định theo cặp (resampling theo competition nếu khả thi) và nêu rõ nếu cỡ mẫu chưa đủ. Không suy diễn quan hệ nhân quả từ nhóm quan sát có Odds/không có Odds.

## Tiêu chí chấp nhận và loại

Chỉ xem Candidate B có thể tốt hơn nếu **temporal holdout đã đóng băng** chứng minh trade-off tốt hơn Candidate A theo các tiêu chí đăng ký trước khi đánh giá holdout:

- Draw F1 tăng mà không làm giảm số Draw TP.
- Draw FP không tăng; nếu một operating point buộc phải đánh đổi FP lấy TP, mức đánh đổi phải nằm trong giới hạn đã định trước và được review rõ ràng.
- Accuracy nằm trong biên non-inferiority đã định trước.
- Home F1 và Away F1 nằm trong các biên non-inferiority đã định trước.
- Phân tích bất định theo cặp hỗ trợ kết quả; nếu không, ghi nhận là chưa kết luận được.

Phải được phê duyệt trước khi quét tham số các biên non-inferiority chính xác và mục tiêu calibration. Nếu holdout không đạt tiêu chí hoặc bằng chứng chưa đủ, loại Candidate B đối với dataset này. Không gọi Candidate B là cải thiện chỉ dựa trên kết quả calibration.

## Rủi ro

- **Overfit ngưỡng:** quá nhiều giá trị `τ` ứng viên có thể dẫn đến overfit calibration.
- **Chọn lọc do fetch theo yêu cầu:** Odds đã lưu phản ánh hành vi sử dụng/fetch của ứng dụng, không phải mẫu ngẫu nhiên của mọi trận.
- **Biến động theo thời gian:** calibration sớm và holdout muộn có thể khác nhau về competition, đội bóng hoặc mùa giải.
- **Tập trung theo competition:** các trận trong cùng competition có thể tương quan và cohort có thể bị chi phối bởi một số competition.
- **Số lượng Draw / mất cân bằng lớp:** quá ít trận hòa thực tế trong mỗi split có thể khiến metric Draw thiếu ổn định.
- **Liên kết giữa các policy:** `DrawModelingStrategy` hiện điều khiển cả việc tạo xác suất signal lẫn quyết định cuối; nếu không tách hai trách nhiệm, có thể vô tình đổi Form/Elo hoặc hành vi Candidate B hiện có.

## Rollback và tương thích

- Candidate A tiếp tục là mặc định production và phương án dự phòng tường minh trong suốt thực nghiệm.
- Candidate B phải được chọn tường minh để đánh giá; không được đặt làm mặc định trong phạm vi kế hoạch này.
- Hành vi và test hiện tại của `BASELINE`, `DECISION_MARGIN` và `DYNAMIC_DRAW_PRIOR` phải được giữ nguyên.
- Có thể tắt thực nghiệm bằng cách chọn cấu hình Candidate A hiện tại, không cần migration dữ liệu hoặc thay đổi Room.

## Các bước triển khai

1. Review và phê duyệt cách tách decision-policy/config cùng quy trình calibration `τ`.
2. Cố định snapshot input 791 trận, các dòng xác suất raw, nhãn thực tế và metadata competition/thời gian.
3. Đăng ký trước ranh giới temporal, mục tiêu calibration, lưới ngưỡng Candidate B và biên non-inferiority.
4. Chỉ sau khi được phê duyệt mới triển khai thay đổi policy/config độc lập và các kiểm thử hồi quy tập trung.
5. Xác minh output của cả sáu signal, weights và xác suất raw giống hệt giữa các lần chạy Candidate A và Candidate B.
6. Chạy đối chứng raw argmax và Candidate A; chọn `τ` trên split sớm, khóa lại, rồi chỉ đánh giá một lần trên holdout muộn.
7. Công bố metrics, ma trận nhầm lẫn, độ bất định và giới hạn; không đổi mặc định production nếu chưa có quyết định riêng.

Tài liệu này chỉ là kế hoạch. Candidate B chưa được triển khai hoặc xác thực bằng thực nghiệm.
