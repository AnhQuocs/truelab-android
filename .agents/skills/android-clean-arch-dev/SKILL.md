---
name: android-clean-arch-dev
description: Hướng dẫn chuyên sâu phát triển tính năng Android đa module theo kiến trúc Clean Architecture & MVVM cho TrueLab.
---

# Kỹ năng Phát triển Clean Architecture Đa Module

Tài liệu này cung cấp công thức chuẩn từng bước để triển khai một tính năng mới trên toàn bộ các module của TrueLab.

## 1. Trình tự Triển khai Tính năng Mới

```text
[1] :core:domain (Entity & UseCase)
         ↓
[2] :core:data (API, Room DB, DTO, DAO & RepoImpl)
         ↓
[3] :core:algorithm (Thuật toán bổ trợ nếu có)
         ↓
[4] :app (ViewModel & UI State)
         ↓
[5] :core:ui / :app (Composable Screens & Components)
```

---

## 2. Công thức Code Chi tiết theo Tầng

### Bước 1: Khởi tạo Domain Model & UseCase (`:core:domain`)
```kotlin
// dev/anhquocs/truelab/core/domain/model/Match.kt
package dev.anhquocs.truelab.core.domain.model

data class Match(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val matchTimeMillis: Long,
    val homeScore: Int?,
    val awayScore: Int?
)

// dev/anhquocs/truelab/core/domain/usecase/GetMatchesUseCase.kt
package dev.anhquocs.truelab.core.domain.usecase

import dev.anhquocs.truelab.core.domain.model.Match
import dev.anhquocs.truelab.core.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow

class GetMatchesUseCase(
    private val repository: MatchRepository
) {
    operator fun invoke(): Flow<List<Match>> = repository.getMatches()
}
```

### Bước 2: Khởi tạo Data Layer (`:core:data`)
```kotlin
// dev/anhquocs/truelab/core/data/repository/MatchRepositoryImpl.kt
package dev.anhquocs.truelab.core.data.repository

import dev.anhquocs.truelab.core.domain.model.Match
import dev.anhquocs.truelab.core.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MatchRepositoryImpl(
    private val localDao: Any, // Room DAO
    private val remoteApi: Any // Retrofit API
) : MatchRepository {
    override fun getMatches(): Flow<List<Match>> {
        // Lấy từ Room DB và map sang Domain Model
        TODO("Triển khai đọc DB và sync API")
    }
}
```

### Bước 3: Khởi tạo ViewModel & UI State (`:app`)
```kotlin
data class MatchUiState(
    val isLoading: Boolean = false,
    val matches: List<Match> = emptyList(),
    val errorMessage: String? = null
)
```
- Sử dụng `StateFlow<MatchUiState>` với toán tử `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatchUiState())`.

---

## 3. Danh sách Kiểm tra trước khi Hoàn thành (Checklist)
- [ ] Không có import Android nào trong `:core:domain` hay `:core:algorithm`.
- [ ] UseCase chỉ có 1 nhiệm vụ và dùng `operator fun invoke`.
- [ ] DTO và Entity được chuyển đổi qua Mapper trước khi ra khỏi `:core:data`.
- [ ] Tất cả các file code đều dưới 400 dòng.
