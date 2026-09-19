package dev.anhquocs.truelab.core.algorithm.sorting

/**
 * Interface chung định nghĩa contract cho mọi thuật toán sắp xếp.
 *
 * @param T Kiểu dữ liệu của phần tử trong mảng.
 */
interface SortAlgorithm<T> {

    /**
     * Sắp xếp danh sách dựa trên tiêu chí so sánh cho trước.
     * Thuật toán không làm thay đổi trực tiếp (mutate) danh sách đầu vào,
     * mà sẽ trả về một bản sao MỚI đã được sắp xếp.
     *
     * @param dataset Danh sách đầu vào.
     * @param comparator Tiêu chí so sánh các phần tử.
     * @return Danh sách mới đã được sắp xếp.
     */
    fun sort(dataset: List<T>, comparator: Comparator<T>): List<T>
}
