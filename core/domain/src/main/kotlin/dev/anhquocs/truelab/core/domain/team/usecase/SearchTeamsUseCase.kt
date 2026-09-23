package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.searching.BinarySearch
import dev.anhquocs.truelab.core.algorithm.searching.LinearSearch
import dev.anhquocs.truelab.core.algorithm.searching.SearchAlgorithm
import dev.anhquocs.truelab.core.domain.match.model.TeamSummary

/**
 * UseCase tìm kiếm và tra cứu đội bóng dựa trên thuật toán Linear Search và Binary Search (Phase 1).
 *
 * UseCase này là pure computation, độc lập hoàn toàn với Repository và Data persistence.
 *
 * @param linearSearchByName Thuật toán tìm kiếm tuyến tính theo tên (mặc định [LinearSearch]).
 * @param linearSearchById Thuật toán tìm kiếm tuyến tính theo ID (mặc định [LinearSearch]).
 * @param binarySearchById Thuật toán tìm kiếm nhị phân theo ID (mặc định [BinarySearch]).
 */
class SearchTeamsUseCase(
    private val linearSearchByName: LinearSearch<TeamSummary, String> = LinearSearch(),
    private val linearSearchById: SearchAlgorithm<TeamSummary, Int> = LinearSearch(),
    private val binarySearchById: SearchAlgorithm<TeamSummary, Int> = BinarySearch()
) {

    /**
     * Tìm kiếm danh sách đội bóng có tên chứa [query] (không phân biệt hoa thường).
     *
     * @param teams Danh sách các đội bóng.
     * @param query Từ khóa tìm kiếm.
     * @return Danh sách các đội bóng thỏa mãn. Nếu [query] rỗng hoặc trắng, trả về [teams].
     */
    fun searchByName(
        teams: List<TeamSummary>,
        query: String
    ): List<TeamSummary> {
        if (query.isBlank() || teams.isEmpty()) {
            return teams
        }

        val trimmedQuery = query.trim()
        val matchingIndices = linearSearchByName.searchAllPartial(teams) { team ->
            team.name.contains(trimmedQuery, ignoreCase = true)
        }

        return matchingIndices.map { teams[it] }
    }

    /**
     * Tra cứu chính xác một đội bóng theo ID.
     *
     * @param teams Danh sách các đội bóng.
     * @param teamId ID đội bóng cần tra cứu.
     * @param isSortedById Nếu true, danh sách đã được sắp xếp tăng dần theo ID và UseCase
     *                     sẽ sử dụng BinarySearch O(log n). Nếu false, sử dụng LinearSearch O(n).
     * @return [TeamSummary] nếu tìm thấy, ngược lại trả về null.
     */
    fun findById(
        teams: List<TeamSummary>,
        teamId: Int,
        isSortedById: Boolean = false
    ): TeamSummary? {
        if (teams.isEmpty()) return null

        val index = if (isSortedById) {
            binarySearchById.search(teams, teamId) { it.id }
        } else {
            linearSearchById.search(teams, teamId) { it.id }
        }

        return if (index != -1) teams[index] else null
    }
}
