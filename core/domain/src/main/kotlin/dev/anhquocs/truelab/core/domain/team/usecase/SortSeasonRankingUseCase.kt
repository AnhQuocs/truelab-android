package dev.anhquocs.truelab.core.domain.team.usecase

import dev.anhquocs.truelab.core.algorithm.sorting.MergeSort
import dev.anhquocs.truelab.core.algorithm.sorting.SortAlgorithm
import dev.anhquocs.truelab.core.domain.team.model.SeasonRanking
import dev.anhquocs.truelab.core.domain.team.model.StandingsSortCriteria

/**
 * UseCase sắp xếp bảng xếp hạng mùa giải (Season Ranking) theo các tiêu chí nghiệp vụ.
 *
 * Mặc định sử dụng thuật toán [MergeSort] để đảm bảo tính ổn định (Stable Ordering).
 * Riêng tiêu chí [StandingsSortCriteria.POINTS_DESC] sử dụng Composite Comparator theo chuẩn bóng đá:
 * Điểm (Points DESC) -> Hiệu số (GoalDiff DESC) -> Số trận thắng (Wins DESC) -> Thứ hạng chính thức (Position ASC).
 *
 * UseCase này là pure computation, không phụ thuộc vào Repository hay Data layer.
 *
 * @param sortAlgorithm Thuật toán sắp xếp (mặc định [MergeSort]).
 */
class SortSeasonRankingUseCase(
    private val sortAlgorithm: SortAlgorithm<SeasonRanking> = MergeSort()
) {

    /**
     * Sắp xếp danh sách [rankings] theo [criteria] cho trước.
     *
     * @param rankings Danh sách xếp hạng các đội (không bị mutate).
     * @param criteria Tiêu chí sắp xếp (mặc định [StandingsSortCriteria.POSITION_ASC]).
     * @return Danh sách mới đã được sắp xếp, hoặc chính [rankings] nếu danh sách có độ dài <= 1.
     */
    operator fun invoke(
        rankings: List<SeasonRanking>,
        criteria: StandingsSortCriteria = StandingsSortCriteria.POSITION_ASC
    ): List<SeasonRanking> {
        if (rankings.size <= 1) return rankings

        val comparator = when (criteria) {
            StandingsSortCriteria.POSITION_ASC -> Comparator<SeasonRanking> { a, b ->
                a.position.compareTo(b.position)
            }
            StandingsSortCriteria.POINTS_DESC -> Comparator<SeasonRanking> { a, b ->
                // Composite Tie-breaker: Points DESC -> GoalDiff DESC -> Wins DESC -> Position ASC
                val ptsComp = b.totalPoints.compareTo(a.totalPoints)
                if (ptsComp != 0) return@Comparator ptsComp

                val gdComp = b.goalDiff.compareTo(a.goalDiff)
                if (gdComp != 0) return@Comparator gdComp

                val wonComp = b.won.compareTo(a.won)
                if (wonComp != 0) return@Comparator wonComp

                a.position.compareTo(b.position)
            }
            StandingsSortCriteria.GOAL_DIFF_DESC -> Comparator<SeasonRanking> { a, b ->
                b.goalDiff.compareTo(a.goalDiff)
            }
            StandingsSortCriteria.WINS_DESC -> Comparator<SeasonRanking> { a, b ->
                b.won.compareTo(a.won)
            }
            StandingsSortCriteria.LOSSES_ASC -> Comparator<SeasonRanking> { a, b ->
                a.loss.compareTo(b.loss)
            }
        }

        return sortAlgorithm.sort(rankings, comparator)
    }
}
