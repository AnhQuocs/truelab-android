package dev.anhquocs.truelab.core.domain.prediction.model

/**
 * Chiến lược mô hình hóa và ra quyết định cho kết quả Hòa (Draw Modeling Strategy).
 *
 * @property BASELINE Nhóm đối chứng (Control Group): Sử dụng cơ chế Linear Mixture và Tie-breaking Argmax ban đầu.
 * @property DECISION_MARGIN Candidate A: Luật ra quyết định phân ngưỡng chênh lệch (|P_H - P_A| < δ và P_D >= θ).
 * @property DYNAMIC_DRAW_PRIOR Candidate B: Tiên nghiệm hòa động dạng Gauss đối xứng theo độ lệch sức mạnh Δ.
 */
enum class DrawModelingStrategy {
    BASELINE,
    DECISION_MARGIN,
    DYNAMIC_DRAW_PRIOR
}
