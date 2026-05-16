package com.edgellm.huggingface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HFModel(
    val id: String,
    val author: String? = null,
    val lastModified: String? = null,
    val private: Boolean = false,
    val downloads: Int = 0,
    @SerialName("likes") val upvotes: Int = 0,
    @SerialName("tags") val modelTags: List<HFModelTag>? = null,
    val pipelineTag: String? = null,
    @SerialName("siblings") val files: List<HFModelFile>? = null,
    @SerialName("cardData") val metadata: Map<String, String>? = null,
    val createdAt: String? = null,
    val sha: String? = null,
    val url: String? = null,
    val authorData: HFAuthor? = null
)

@Serializable
data class HFModelTag(
    val name: String,
    val type: String? = null
)

@Serializable
data class HFModelFile(
    val rfilename: String,
    val size: Long = 0,
    @SerialName("blobId") val blobId: String? = null,
    @SerialName("lfs") val lfsInfo: HFLfsInfo? = null
)

@Serializable
data class HFLfsInfo(
    val size: Long? = null,
    @SerialName("sha256") val sha: String? = null
)

@Serializable
data class HFAuthor(
    val name: String,
    val fullname: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class HFSearchResponse(
    val total: Int,
    val next: Int? = null,
    val previous: Int? = null,
    val models: List<HFModel>
)

@Serializable
data class HFApiError(
    val error: String
)

@Serializable
data class HFModelDownloadInfo(
    @SerialName("url") val downloadUrl: String,
    val filename: String,
    val size: Long
)

data class HFModelFilter(
    val searchQuery: String = "",
    val task: HFTask? = null,
    val library: HFLibrary? = null,
    val sortBy: HFSortBy = HFSortBy.DOWNLOADS,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val limit: Int = 20,
    val offset: Int = 0
)

enum class HFTask(val value: String, val displayName: String) {
    TEXT_GENERATION("text-generation", "Text Generation"),
    TEXT_TO_IMAGE("text-to-image", "Text to Image"),
    IMAGE_TO_TEXT("image-to-text", "Image to Text"),
    SUMMARIZATION("summarization", "Summarization"),
    TRANSLATION("translation", "Translation"),
    CONVERSATIONAL("conversational", "Chat"),
    FILL_MASK("fill-mask", "Fill Mask"),
    QUESTION_ANSWERING("question-answering", "Question Answering"),
    TOKEN_CLASSIFICATION("token-classification", "Token Classification"),
    TEXT_CLASSIFICATION("text-classification", "Text Classification"),
    OBJECT_DETECTION("object-detection", "Object Detection"),
    IMAGE_CLASSIFICATION("image-classification", "Image Classification")
}

enum class HFLibrary(val value: String, val displayName: String) {
    LLAMA_CPP("llama-cpp", "llama.cpp"),
    GGML("ggml", "GGML"),
    PYTORCH("pytorch", "PyTorch"),
    TENSORFLOW("tensorflow", "TensorFlow"),
    ONNX("onnx", "ONNX"),
    KERAS("keras", "Keras"),
    SAFETENSORS("safetensors", "SafeTensors"),
    MLX("mlx", "MLX"),
    LITERT("litert", "LiteRT"),
    LITERTLM("litertlm", "LiteRT-LM")
}

enum class HFSortBy(val value: String) {
    DOWNLOADS("downloads"),
    LIKES("likes"),
    LAST_MODIFIED("lastModified"),
    CREATED_AT("createdAt")
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

fun HFModel.getQuantization(): String? {
    return modelTags?.find { it.name.contains("q", ignoreCase = true) && 
        (it.name.contains("k", ignoreCase = true) || it.name.contains("bit", ignoreCase = true)) }?.name
}

fun HFModel.getFileSize(): String {
    val ggufFile = files?.find { it.rfilename.endsWith(".gguf", ignoreCase = true) }
        ?: files?.firstOrNull()
    return formatFileSize(ggufFile?.size ?: 0)
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

fun HFModel.hasGGUF(): Boolean {
    return files?.any { it.rfilename.endsWith(".gguf", ignoreCase = true) } == true
}

fun HFModel.hasLitertlm(): Boolean {
    return files?.any { it.rfilename.endsWith(".litertlm", ignoreCase = true) } == true
}