package dev.anhquocs.truelab.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: T
)

@Serializable
data class MetaResponse(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("total_page") val totalPage: Int? = null,
    @SerialName("last_page") val lastPage: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    @SerialName("total") val total: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null,
    @SerialName("per_page") val perPage: Int? = null
) {
    val effectiveLastPage: Int
        get() = lastPage ?: totalPage ?: 1

    val effectiveTotal: Int
        get() = total ?: totalCount ?: 0

    val effectivePageSize: Int
        get() = perPage ?: pageSize ?: 50
}
