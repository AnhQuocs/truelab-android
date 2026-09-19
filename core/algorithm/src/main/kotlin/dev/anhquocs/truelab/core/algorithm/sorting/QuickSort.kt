package dev.anhquocs.truelab.core.algorithm.sorting

/**
 * Triển khai thuật toán Sắp xếp Nhanh (Quick Sort).
 * 
 * - Time Complexity: Trung bình O(n log n), Xấu nhất O(n^2).
 * - Space Complexity: O(log n) (Đệ quy) + O(n) (Copy mảng).
 * - Tính ổn định (Stability): KHÔNG ỔN ĐỊNH (Unstable).
 */
class QuickSort<T> : SortAlgorithm<T> {

    override fun sort(dataset: List<T>, comparator: Comparator<T>): List<T> {
        if (dataset.size <= 1) return dataset

        // Tạo mảng mutable để thao tác in-place nội bộ, bảo toàn tính bất biến cho list gốc
        val array = dataset.toMutableList()
        quickSort(array, 0, array.size - 1, comparator)
        return array.toList()
    }

    private fun quickSort(array: MutableList<T>, low: Int, high: Int, comparator: Comparator<T>) {
        if (low < high) {
            val pivotIndex = partition(array, low, high, comparator)
            quickSort(array, low, pivotIndex - 1, comparator)
            quickSort(array, pivotIndex + 1, high, comparator)
        }
    }

    private fun partition(array: MutableList<T>, low: Int, high: Int, comparator: Comparator<T>): Int {
        // Chọn phần tử giữa làm pivot để tối ưu hóa trường hợp mảng đã sắp xếp sẵn
        val mid = low + (high - low) / 2
        swap(array, mid, high) // Đưa pivot về cuối
        val pivot = array[high]
        
        var i = low - 1

        for (j in low until high) {
            if (comparator.compare(array[j], pivot) <= 0) {
                i++
                swap(array, i, j)
            }
        }
        
        // Đưa pivot về đúng vị trí
        swap(array, i + 1, high)
        return i + 1
    }

    private fun swap(array: MutableList<T>, i: Int, j: Int) {
        val temp = array[i]
        array[i] = array[j]
        array[j] = temp
    }
}
