package dev.anhquocs.truelab.core.domain.match.usecase

import dev.anhquocs.truelab.core.algorithm.searching.LinearSearch
import dev.anhquocs.truelab.core.domain.match.model.Match

/**
 * UseCase tìm kiếm các trận đấu dựa trên thuật toán Tìm kiếm Tuyến tính (Phase 1: Linear Search).
 *
 * UseCase này là pure computation, độc lập hoàn toàn với Repository và Data persistence.
 *
 * @param searchAlgorithm Thuật toán tìm kiếm tuyến tính (mặc định [LinearSearch]).
 */
class SearchMatchesUseCase(
    private val searchAlgorithm: LinearSearch<Match, String> = LinearSearch()
) {

    /**
     * Tìm kiếm các trận đấu có tên đội nhà hoặc đội khách chứa [query] (không phân biệt hoa thường).
     *
     * @param matches Danh sách trận đấu cần tìm.
     * @param query Từ khóa tìm kiếm.
     * @return Danh sách các trận đấu thỏa mãn. Nếu [query] rỗng hoặc chỉ toàn khoảng trắng, trả về toàn bộ [matches].
     */
    operator fun invoke(
        matches: List<Match>,
        query: String
    ): List<Match> {
        if (query.isBlank() || matches.isEmpty()) {
            return matches
        }

        val trimmedQuery = query.trim()
        val matchingIndices = searchAlgorithm.searchAllPartial(matches) { match ->
            match.homeTeam.name.contains(trimmedQuery, ignoreCase = true) ||
                match.awayTeam.name.contains(trimmedQuery, ignoreCase = true)
        }

        return matchingIndices.map { matches[it] }
    }
}
