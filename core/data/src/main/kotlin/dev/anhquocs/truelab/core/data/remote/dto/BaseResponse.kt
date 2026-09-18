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
    @SerialName("total_count") val totalCount: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null
)
