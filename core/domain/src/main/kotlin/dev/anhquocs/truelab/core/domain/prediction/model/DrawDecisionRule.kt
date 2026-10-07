package dev.anhquocs.truelab.core.domain.prediction.model

/**
 * Quy tắc ra quyết định phân loại kết quả hòa ở tầng Post-Mixture Decision (Final Draw Decision Rule).
 *
 * Tách biệt hoàn toàn với [DrawModelingStrategy] (vốn phụ trách mô hình hóa xác suất ở cấp Signal Transformer).
 *
 * @property RAW_ARGMAX Nhóm đối chứng (Control Group / Baseline): Chọn nhãn có xác suất cao nhất từ vector xác suất thô.
 * @property DECISION_MARGIN Candidate A: Luật ra quyết định phân ngưỡng chênh lệch (|P_H - P_A| < δ và P_D >= θ).
 * @property RELATIVE_DRAW_GAP Candidate B: Luật phân ranh khoảng cách hòa tương đối (drawDeficit = max(P_H, P_A) - P_D <= τ).
 */
enum class DrawDecisionRule {
    RAW_ARGMAX,
    DECISION_MARGIN,
    RELATIVE_DRAW_GAP
}
