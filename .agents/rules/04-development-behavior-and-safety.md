# Hành vi Agent & Quy chuẩn An toàn (Development Behavior & Safety)

## 1. Ràng buộc Hành vi Cốt lõi của AI Agent

Khi lập trình dự án TrueLab, AI Agent phải tuân thủ nghiêm ngặt các giới hạn sau:

1. **KHÔNG tự ý thay đổi kiến trúc**: Không thay đổi cấu trúc multi-module, không tạo module mới hoặc đổi hướng phụ thuộc nếu không có yêu cầu rõ ràng.
2. **KHÔNG tự ý thêm thư viện ngoài**: Không thêm plugin hay dependencies vào `build.gradle.kts` hoặc `libs.versions.toml` khi chưa được chỉ định.
3. **KHÔNG xóa logic đang hoạt động**: Không được xóa các file, logic nghiệp vụ hoặc test case đang chạy ổn định.
4. **KHÔNG over-engineer**: Không tạo các tầng trừu tượng không cần thiết hoặc viết code phức tạp hóa vấn đề.
5. **Luôn kiểm tra tính tái sử dụng trước khi viết mới**:
   > *"Dự án đã có component, extension, dimension, utility hoặc abstraction tương tự chưa?"*
   - Nếu ĐÃ CÓ: Tái sử dụng ngay.
   - Nếu CHƯA CÓ: Tạo thành phần dùng chung mới đúng chuẩn tại `:core:*`.

---

## 2. Quy tắc An toàn Bảo mật Tuyệt đối (Zero Credential Leak)

Agent phải đặt vấn đề bảo mật lên hàng đầu:

- **TUYỆT ĐỐI KHÔNG commit**:
  - API Keys (API-Football, TheOddsAPI, Gemini, RapidAPI,...).
  - Access tokens, Bearer tokens, Refresh tokens, Passwords.
  - Signing keystore (`.jks`), thông tin đăng ký mật.
  - Endpoints private hoặc staging nhạy cảm.
- **Kênh thông tin cấm chứa secret**:
  - Mã nguồn Kotlin/Gradle, Git commit, `README.md`, Logs (`Log.d`, `println`), Exception message, Ảnh chụp màn hình, Dữ liệu benchmark.
- **Quản lý cấu hình nhạy cảm**:
  - Đặt trong `local.properties` (file đã được đưa vào `.gitignore`).
  - Sử dụng biến môi trường (Environment Variables) cho CI/CD.
  - Private API documentation (như `DATA_API_INTERNAL.md`) phải luôn được ignore khỏi Git.

---

## 3. Format Báo cáo Phản hồi Tiêu chuẩn

Sau mỗi lần thực hiện task lớn hoặc sửa đổi mã nguồn, Agent bắt buộc phải tổng kết bằng cấu trúc chuẩn sau:

```markdown
## Changed
- <Tóm tắt những thay đổi đã hoàn thành>

## Architecture
- <Xác thực tính toàn vẹn của phân tầng kiến trúc, hướng phụ thuộc và ranh giới module>

## Implementation
- <Chi tiết kỹ thuật chính, Design Pattern áp dụng và lý do lựa chọn>

## Testing
- <Kết quả chạy Unit Test, Integration Test hoặc Benchmark>

## Security
- <Xác nhận không có API key hay thông tin nhạy cảm nào bị lộ lọt>

## Files Changed
- Created:
  - [file_path](file:///...)
- Modified:
  - [file_path](file:///...)

## Notes
- <Ghi chú quan trọng, giả định kỹ thuật hoặc bước tiếp theo cho team>
```
