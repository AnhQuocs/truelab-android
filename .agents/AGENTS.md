# TrueLab Agent Guidelines & System Index

Chào mừng bạn đến với **TrueLab** — Ứng dụng Android phân tích và dự đoán bóng đá chuyên sâu, xây dựng trên nền tảng Jetpack Compose, Kiến trúc Đa Module Clean Architecture và Đánh giá Thuật toán thuần túy (Pure Kotlin/JVM).

Tài liệu này là **Single Source of Truth** quy định toàn bộ tiêu chuẩn kiến trúc, quy ước coding, design system, quy trình kiểm thử và ràng buộc hành vi cho AI Agent trong suốt quá trình phát triển dự án.

---

## 1. Bản đồ Kiến trúc Đa Module (Multi-Module Architecture)

```text
                    :app (Application, Navigation, Hilt, Presentation)
                  /  |  \
                 ↓   ↓   ↓
       :core:data   :core:ui (Material3 Design System, Reusable UI, Charts)
            ↓
       :core:domain (Pure Kotlin/JVM - Domain Models, Repositories, UseCases)
            ↓
     :core:algorithm (Pure Kotlin/JVM - Thuật toán thuần: Search, Sort, Stats, Elo, Prediction)
```

- **`:app`**: Entry point chính của ứng dụng, điều hướng (Navigation Compose), Dependency Injection (Hilt), và orchestration các màn hình.
- **`:core:ui`**: Android Library chứa Typography, Color Scheme, Dimensions, Shape, các Component tái sử dụng và biểu đồ trực quan (Charts).
- **`:core:data`**: Android Library phụ trách Retrofit API, Room Database, DTO, DAO, Repository Implementations và Crawler/Data Sync Engine.
- **`:core:domain`**: Pure Kotlin/JVM Module định nghĩa Domain Entities, Repository Interfaces và Use Cases (Không phụ thuộc Android SDK hay Data layer).
- **`:core:algorithm`**: Pure Kotlin/JVM Module chứa toàn bộ thuật toán cốt lõi và cấu trúc dữ liệu thuần túy (Tuyệt đối 0 phụ thuộc Android SDK).

---

## 2. Mục lục Quy chuẩn & Tài liệu (.agents)

### 📜 Quy tắc cốt lõi (`.agents/rules/`)
- [01-architecture-standards.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/rules/01-architecture-standards.md): Tiêu chuẩn Clean Architecture, MVVM, ranh giới các layer và bất biến phụ thuộc.
- [02-ui-design-system-conventions.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/rules/02-ui-design-system-conventions.md): Quy ước Material 3 Design System, Dimensions dùng chung, Theme, Typography, Shape và Đa ngôn ngữ (Triple-locale).
- [03-code-organization-and-modularity.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/rules/03-code-organization-and-modularity.md): Giới hạn 400 dòng/file, quy ước đặt tên, Mapper và tổ chức Extension function.
- [04-development-behavior-and-safety.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/rules/04-development-behavior-and-safety.md): Ràng buộc hành vi Agent, bảo mật tuyệt đối (không leak secret), kiểm tra tái sử dụng và format báo cáo.
- [05-testing-standards.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/rules/05-testing-standards.md): Kim tự tháp kiểm thử, ma trận test case thuật toán, test ViewModel/Repository và quy chuẩn benchmark.

### 🛠️ Kỹ năng chuyên sâu (`.agents/skills/`)
- [android-clean-arch-dev](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/skills/android-clean-arch-dev/SKILL.md): Hướng dẫn triển khai tính năng chuẩn Clean Architecture đa module.
- [android-unit-test-guide](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/skills/android-unit-test-guide/SKILL.md): Công thức viết Unit Test thuật toán, ViewModel Coroutines và Repository.
- [compose-design-system-guard](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/skills/compose-design-system-guard/SKILL.md): Rà soát token giao diện, chặn hardcode số đo/màu sắc và bảo vệ Reusable UI.

### 🔄 Quy trình làm việc (`.agents/workflows/`)
- [feature-implementation.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/workflows/feature-implementation.md): Quy trình chuẩn 12 bước phát triển tính năng từ phân tích đến xác thực build.
- [code-refactoring-cleanup.md](file:///d:/Android%20Studio/Jetpack%20Compose/TrueLab/.agents/workflows/code-refactoring-cleanup.md): Quy trình tái cấu trúc code, phân rã file vượt quá 400 dòng và gom nhóm tái sử dụng.

---

## 3. Bốn Nguyên tắc Bất biến (Core Behavioral Mandates)

1. **Hiểu rõ trước khi code**: Phân tích kỹ yêu cầu, kiểm tra code/component hiện có và tôn trọng tuyệt đối ranh giới kiến trúc.
2. **Tuyệt đối không lộ thông tin nhạy cảm**: Zero tolerance với việc commit API keys, tokens, mật khẩu vào git hoặc tài liệu công khai.
3. **Không tự ý gây Breaking Changes**: Không tự ý đổi cấu trúc module, thêm thư viện lạ hoặc xóa logic đang chạy ổn định.
4. **Bắt buộc xác thực Build/Test**: Luôn chạy `./gradlew assembleDebug` hoặc test tasks sau mỗi thay đổi mã nguồn quan trọng.
