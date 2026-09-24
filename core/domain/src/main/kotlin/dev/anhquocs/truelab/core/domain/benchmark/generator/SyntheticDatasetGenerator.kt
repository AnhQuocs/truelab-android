package dev.anhquocs.truelab.core.domain.benchmark.generator

import kotlin.random.Random

/**
 * Bộ sinh dữ liệu thử nghiệm giả lập (Synthetic Dataset Generator) có tính tất định và tái lập được (Deterministic & Reproducible).
 * Sử dụng seed cố định để đảm bảo kết quả đo đạc benchmark luôn công bằng và đồng nhất qua các lần chạy.
 */
object SyntheticDatasetGenerator {

    const val DEFAULT_SEED: Long = 42L

    /**
     * Sinh danh sách số nguyên ngẫu nhiên theo seed xác định.
     *
     * @param size Kích thước danh sách cần sinh.
     * @param seed Hạt giống ngẫu nhiên (mặc định [DEFAULT_SEED]).
     * @return Danh sách các số nguyên ngẫu nhiên.
     */
    fun generateRandomIntList(size: Int, seed: Long = DEFAULT_SEED): List<Int> {
        if (size <= 0) return emptyList()
        val random = Random(seed)
        return List(size) { random.nextInt(0, 1_000_000) }
    }

    /**
     * Sinh danh sách số nguyên đã được sắp xếp tăng dần theo seed xác định,
     * phục vụ các thuật toán yêu cầu mảng có thứ tự như Binary Search.
     *
     * @param size Kích thước danh sách cần sinh.
     * @param seed Hạt giống ngẫu nhiên (mặc định [DEFAULT_SEED]).
     * @return Danh sách các số nguyên đã được sắp xếp tăng dần.
     */
    fun generateSortedIntList(size: Int, seed: Long = DEFAULT_SEED): List<Int> {
        if (size <= 0) return emptyList()
        return generateRandomIntList(size, seed).sorted()
    }
}
