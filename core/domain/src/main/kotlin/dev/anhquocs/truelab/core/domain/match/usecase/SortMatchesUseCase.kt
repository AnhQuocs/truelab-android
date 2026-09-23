package dev.anhquocs.truelab.core.domain.match.usecase

import dev.anhquocs.truelab.core.algorithm.sorting.MergeSort
import dev.anhquocs.truelab.core.algorithm.sorting.SortAlgorithm
import dev.anhquocs.truelab.core.domain.match.model.Match
import dev.anhquocs.truelab.core.domain.match.model.MatchSortCriteria

/**
 * UseCase sắp xếp danh sách các trận đấu theo các tiêu chí nghiệp vụ.
 *
 * Mặc định sử dụng thuật toán [MergeSort] để đảm bảo tính ổn định (Stable Ordering).
 * UseCase này là pure computation, không có phụ thuộc vào Repository hay Data layer.
 *
 * @param sortAlgorithm Thuật toán sắp xếp (mặc định [MergeSort]).
 */
class SortMatchesUseCase(
    private val sortAlgorithm: SortAlgorithm<Match> = MergeSort()
) {

    /**
     * Sắp xếp danh sách [matches] dựa theo [criteria] cho trước.
     *
     * @param matches Danh sách trận đấu cần sắp xếp (không bị thay đổi/mutate).
     * @param criteria Tiêu chí sắp xếp (mặc định [MatchSortCriteria.START_TIME_ASC]).
     * @return Danh sách mới đã được sắp xếp, hoặc chính [matches] nếu danh sách có độ dài <= 1.
     */
    operator fun invoke(
        matches: List<Match>,
        criteria: MatchSortCriteria = MatchSortCriteria.START_TIME_ASC
    ): List<Match> {
        if (matches.size <= 1) return matches

        val comparator = when (criteria) {
            MatchSortCriteria.START_TIME_ASC -> Comparator<Match> { a, b ->
                a.startTimeDate.compareTo(b.startTimeDate)
            }
            MatchSortCriteria.START_TIME_DESC -> Comparator<Match> { a, b ->
                b.startTimeDate.compareTo(a.startTimeDate)
            }
            MatchSortCriteria.TOTAL_GOALS_DESC -> Comparator<Match> { a, b ->
                b.totalGoals.compareTo(a.totalGoals)
            }
            MatchSortCriteria.GOAL_DIFF_DESC -> Comparator<Match> { a, b ->
                // Hiệu số bàn thắng có dấu (+3 > +1 > 0 > -2), tuyệt đối không dùng abs()
                b.goalDifference.compareTo(a.goalDifference)
            }
            MatchSortCriteria.ID_ASC -> Comparator<Match> { a, b ->
                a.id.compareTo(b.id)
            }
        }

        return sortAlgorithm.sort(matches, comparator)
    }
}
