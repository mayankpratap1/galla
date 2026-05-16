package com.edgellm.data.remote.dto

import com.edgellm.domain.model.HFModel
import com.edgellm.domain.model.HFModelFile
import com.edgellm.domain.model.ModelType
import com.google.gson.annotations.SerializedName

data class HFSearchResponse(
    val total: Int,
    @SerializedName("next") val nextOffset: Int?,
    @SerializedName("previous") val prevOffset: Int?,
    val models: List<HFModelDto>
)

data class HFModelDto(
    val id: String,
    @SerializedName("author") val author: String?,
    @SerializedName("lastModified") val lastModified: String?,
    @SerializedName("private") val isPrivate: Boolean = false,
    val downloads: Int = 0,
    val likes: Int = 0,
    @SerializedName("tags") val tagDtos: List<HFModelTagDto>? = null,
    @SerializedName("pipeline_tag") val pipelineTag: String? = null,
    @SerializedName("siblings") val files: List<HFModelFileDto>? = null,
    @SerializedName("cardData") val metadata: Map<String, String>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    val sha: String? = null,
    val url: String? = null
) {
    fun toDomain(): HFModel {
        val ggufFiles = files?.filter { it.rfilename.endsWith(".gguf", ignoreCase = true) } ?: emptyList()
        val litertFiles = files?.filter { it.rfilename.endsWith(".litertlm", ignoreCase = true) } ?: emptyList()

        val modelType = when {
            ggufFiles.isNotEmpty() -> ModelType.GGUF
            litertFiles.isNotEmpty() -> ModelType.LITERT
            else -> ModelType.UNKNOWN
        }

        val totalSize = files?.sumOf { it.size } ?: 0L

        val quantization = tagDtos?.find { it.name.contains("q", ignoreCase = true) &&
            (it.name.contains("k", ignoreCase = true) || it.name.contains("bit", ignoreCase = true)) }?.name

        return HFModel(
            id = id,
            author = author,
            lastModified = lastModified,
            downloads = downloads,
            likes = likes,
            pipelineTag = pipelineTag,
            tags = tagDtos?.map { it.name } ?: emptyList(),
            files = files?.map { it.toDomain() } ?: emptyList(),
            modelType = modelType,
            size = totalSize,
            quantization = quantization
        )
    }
}

data class HFModelTagDto(
    val name: String,
    val type: String?
)

data class HFModelFileDto(
    @SerializedName("rfilename") val rfilename: String,
    val size: Long = 0,
    @SerializedName("blobId") val blobId: String? = null,
    @SerializedName("lfs") val lfsInfo: HFLfsInfoDto? = null
) {
    fun toDomain(): HFModelFile {
        return HFModelFile(
            name = rfilename,
            size = size,
            isGGUF = rfilename.endsWith(".gguf", ignoreCase = true),
            isLiteRT = rfilename.endsWith(".litertlm", ignoreCase = true)
        )
    }
}

data class HFLfsInfoDto(
    val size: Long? = null,
    @SerializedName("sha256") val sha256: String? = null
)