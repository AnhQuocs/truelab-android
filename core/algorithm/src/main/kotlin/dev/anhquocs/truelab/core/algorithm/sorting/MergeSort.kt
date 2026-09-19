package dev.anhquocs.truelab.core.algorithm.sorting

/**
 * Triển khai thuật toán Sắp xếp Trộn (Merge Sort).
 * 
 * - Time Complexity: Trung bình & Xấu nhất luôn là O(n log n).
 * - Space Complexity: O(n) (Do mảng phụ phục vụ trộn).
 * - Tính ổn định (Stability): ỔN ĐỊNH (Stable).
 */
class MergeSort<T> : SortAlgorithm<T> {

    override fun sort(dataset: List<T>, comparator: Comparator<T>): List<T> {
        if (dataset.size <= 1) return dataset

        val mid = dataset.size / 2
        val left = dataset.subList(0, mid)
        val right = dataset.subList(mid, dataset.size)

        // Gọi đệ quy chia đôi mảng
        val sortedLeft = sort(left, comparator)
        val sortedRight = sort(right, comparator)

        // Hợp nhất (Merge) hai mảng đã sắp xếp
        return merge(sortedLeft, sortedRight, comparator)
    }

    private fun merge(left: List<T>, right: List<T>, comparator: Comparator<T>): List<T> {
        val result = ArrayList<T>(left.size + right.size)
        var i = 0
        var j = 0

        while (i < left.size && j < right.size) {
            // Dấu <= đảm bảo tính Stable: Nếu bằng nhau, ưu tiên lấy ở mảng Left trước
            if (comparator.compare(left[i], right[j]) <= 0) {
                result.add(left[i])
                i++
            } else {
                result.add(right[j])
                j++
            }
        }

        // Bổ sung các phần tử còn thừa
        while (i < left.size) {
            result.add(left[i])
            i++
        }

        while (j < right.size) {
            result.add(right[j])
            j++
        }

        return result
    }
}
