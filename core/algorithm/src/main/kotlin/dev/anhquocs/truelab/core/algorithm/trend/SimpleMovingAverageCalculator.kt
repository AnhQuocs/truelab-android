package dev.anhquocs.truelab.core.algorithm.trend

/**
 * Triển khai thuật toán Đường trung bình động đơn giản (Simple Moving Average - SMA).
 *
 * Áp dụng kỹ thuật Cửa sổ trượt (Sliding Window / Rolling Sum) để đạt độ phức tạp thời gian O(n),
 * triệt tiêu thao tác tính lại tổng lặp lồng nhau O(n * k).
 *
 * Quản lý trạng thái các giá trị không hữu hạn (NaN, +Infinity, -Infinity) theo chuẩn IEEE-754
 * bằng các biến đếm O(1), đảm bảo:
 * - Phục hồi giá trị trung bình chính xác ngay khi các giá trị không hữu hạn trượt ra khỏi cửa sổ.
 * - Tránh triệt tiêu vô hạn không xác định (Infinity - Infinity = NaN).
 * - Đảm bảo độ phức tạp thời gian luôn là O(n) trong mọi trường hợp (kể cả khi dữ liệu dày đặc NaN).
 */
class SimpleMovingAverageCalculator : MovingAverageCalculator {

    override fun calculate(dataset: List<Double>, windowSize: Int): List<Double> {
        // Step 1: Kiểm tra tính hợp lệ của windowSize trước tiên
        require(windowSize > 0) {
            "windowSize must be greater than 0, but was $windowSize"
        }

        // Step 2: Nếu dataset rỗng và windowSize hợp lệ
        if (dataset.isEmpty()) {
            return emptyList()
        }

        val n = dataset.size

        // Step 3: Nếu windowSize vượt quá kích thước dataset
        if (windowSize > n) {
            return emptyList()
        }

        // Step 5: Nếu windowSize == 1, mỗi giá trị trung bình chính là phần tử đó
        if (windowSize == 1) {
            return ArrayList(dataset)
        }

        // Step 4 & Triển khai Rolling Sum: windowSize in 2..n
        val resultSize = n - windowSize + 1
        val result = ArrayList<Double>(resultSize)

        var finiteSum = 0.0
        var nanCount = 0
        var posInfCount = 0
        var negInfCount = 0

        fun addValue(v: Double) {
            when {
                v.isNaN() -> nanCount++
                v == Double.POSITIVE_INFINITY -> posInfCount++
                v == Double.NEGATIVE_INFINITY -> negInfCount++
                else -> finiteSum += v
            }
        }

        fun removeValue(v: Double) {
            when {
                v.isNaN() -> nanCount--
                v == Double.POSITIVE_INFINITY -> posInfCount--
                v == Double.NEGATIVE_INFINITY -> negInfCount--
                else -> finiteSum -= v
            }
        }

        fun currentAverage(): Double = when {
            nanCount > 0 -> Double.NaN
            posInfCount > 0 && negInfCount > 0 -> Double.NaN
            posInfCount > 0 -> Double.POSITIVE_INFINITY
            negInfCount > 0 -> Double.NEGATIVE_INFINITY
            else -> finiteSum / windowSize
        }

        // Khởi tạo cửa sổ đầu tiên [0 until windowSize]
        for (i in 0 until windowSize) {
            addValue(dataset[i])
        }
        result.add(currentAverage())

        // Trượt cửa sổ: Loại bỏ phần tử rời khỏi và thêm phần tử mới gia nhập (O(1) mỗi bước)
        for (i in windowSize until n) {
            removeValue(dataset[i - windowSize])
            addValue(dataset[i])
            result.add(currentAverage())
        }

        return result
    }
}
