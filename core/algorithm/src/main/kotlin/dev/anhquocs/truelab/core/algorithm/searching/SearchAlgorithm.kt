package dev.anhquocs.truelab.core.algorithm.searching

/**
 * Interface chung định nghĩa contract cho mọi thuật toán tìm kiếm.
 * 
 * @param T Kiểu dữ liệu của phần tử trong mảng (Dataset).
 * @param K Kiểu dữ liệu của khóa tìm kiếm (Target Key).
 */
interface SearchAlgorithm<T, K> {

    /**
     * Tìm kiếm một phần tử trong danh sách dựa trên khóa tìm kiếm (Exact Match).
     * 
     * @param dataset Danh sách các phần tử.
     * @param target Khóa cần tìm.
     * @param selector Hàm trích xuất khóa từ phần tử kiểu T.
     * @return Index của phần tử nếu tìm thấy, ngược lại trả về -1.
     */
    fun search(dataset: List<T>, target: K, selector: (T) -> K): Int
}
