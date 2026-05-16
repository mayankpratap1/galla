package com.edgellm.domain.model

data class HFModel(
    val id: String,
    val author: String?,
    val lastModified: String?,
    val downloads: Int,
    val likes: Int,
    val pipelineTag: String?,
    val tags: List<String>,
    val files: List<HFModelFile>,
    val modelType: ModelType,
    val size: Long,
    val quantization: String?
)

data class HFModelFile(
    val name: String,
    val size: Long,
    val isGGUF: Boolean,
    val isLiteRT: Boolean
)

enum class ModelType {
    GGUF,
    LITERT,
    PYTORCH,
    ONNX,
    UNKNOWN
}

data class DownloadProgress(
    val modelId: String,
    val filename: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val state: DownloadState
)

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

data class InstalledModel(
    val modelId: String,
    val files: List<String>,
    val totalSize: Long,
    val type: ModelType,
    val installedAt: Long,
    val path: String
)

data class ModelFilter(
    val query: String = "",
    val type: ModelFilterType = ModelFilterType.TRENDING,
    val sortBy: SortOption = SortOption.DOWNLOADS,
    val limit: Int = 20,
    val offset: Int = 0
)

enum class ModelFilterType {
    TRENDING,
    GGUF,
    LITERT,
    CHAT,
    VISION
}

enum class SortOption {
    DOWNLOADS,
    LIKES,
    RECENT
}