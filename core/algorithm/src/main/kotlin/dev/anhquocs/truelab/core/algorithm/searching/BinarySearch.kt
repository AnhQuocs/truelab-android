package dev.anhquocs.truelab.core.algorithm.searching

/**
 * Triển khai thuật toán Tìm kiếm Nhị phân (Binary Search).
 * 
 * LƯU Ý QUAN TRỌNG: 
 * Dataset truyền vào BẮT BUỘC phải được sắp xếp (Sorted) theo cùng tiêu chí comparator/key.
 * Thuật toán này không hỗ trợ Partial Match trong Phase 1.
 * 
 * Độ phức tạp thời gian: O(log n).
 * Độ phức tạp không gian: O(1).
 */
class BinarySearch<T, K : Comparable<K>> : SearchAlgorithm<T, K> {

    /**
     * Tìm kiếm chính xác (Exact Match) bằng phương pháp chia để trị.
     */
    override fun search(dataset: List<T>, target: K, selector: (T) -> K): Int {
        if (dataset.isEmpty()) return -1

        var left = 0
        var right = dataset.size - 1

        while (left <= right) {
            val mid = left + (right - left) / 2
            val midVal = selector(dataset[mid])

            val comparison = midVal.compareTo(target)
            when {
                comparison == 0 -> return mid // Tìm thấy
                comparison < 0 -> left = mid + 1 // Target nằm bên phải
                else -> right = mid - 1 // Target nằm bên trái
            }
        }

        return -1
    }
}
