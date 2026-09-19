package dev.anhquocs.truelab.core.algorithm.searching

/**
 * Triển khai thuật toán Tìm kiếm Tuần tự (Linear Search).
 * Độ phức tạp thời gian: O(n).
 * Độ phức tạp không gian: O(1).
 */
class LinearSearch<T, K> : SearchAlgorithm<T, K> {

    /**
     * Tìm kiếm chính xác (Exact Match) bằng cách duyệt từng phần tử.
     */
    override fun search(dataset: List<T>, target: K, selector: (T) -> K): Int {
        for (i in dataset.indices) {
            if (selector(dataset[i]) == target) {
                return i
            }
        }
        return -1
    }

    /**
     * Tìm kiếm cục bộ/tương đối (Partial Match) dựa trên một điều kiện linh hoạt (Predicate).
     * Hàm này được thiết kế Generic, không hard-code kiểu String.
     * 
     * @param dataset Danh sách các phần tử.
     * @param predicate Hàm điều kiện trả về true nếu phần tử thỏa mãn.
     * @return Index của phần tử ĐẦU TIÊN thỏa mãn điều kiện, ngược lại trả về -1.
     */
    fun searchPartial(dataset: List<T>, predicate: (T) -> Boolean): Int {
        for (i in dataset.indices) {
            if (predicate(dataset[i])) {
                return i
            }
        }
        return -1
    }

    /**
     * Tìm kiếm cục bộ/tương đối (Partial Match).
     * 
     * @param dataset Danh sách các phần tử.
     * @param predicate Hàm điều kiện trả về true nếu phần tử thỏa mãn.
     * @return Danh sách tất cả các Index thỏa mãn điều kiện (phục vụ bộ lọc).
     */
    fun searchAllPartial(dataset: List<T>, predicate: (T) -> Boolean): List<Int> {
        val resultIndices = mutableListOf<Int>()
        for (i in dataset.indices) {
            if (predicate(dataset[i])) {
                resultIndices.add(i)
            }
        }
        return resultIndices
    }
}
