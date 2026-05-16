package com.edgellm.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloaded: Long, val total: Long) : DownloadState()
    data object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
    data object Paused : DownloadState()
}

data class DownloadTask(
    val modelId: String,
    val filename: String,
    val downloadUrl: String,
    val totalSize: Long,
    val destinationPath: String,
    val state: DownloadState = DownloadState.Idle
)

class ModelDownloadService(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val _downloads = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadTask>> = _downloads.asStateFlow()

    private val activeDownloads = ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val downloadedBytes = ConcurrentHashMap<String, Long>()

    private val modelsDir: File by lazy {
        File(context.filesDir, "models").also { it.mkdirs() }
    }

    fun getModelPath(modelId: String, filename: String): String {
        val sanitizedId = modelId.replace("/", "_")
        val modelDir = File(modelsDir, sanitizedId).also { it.mkdirs() }
        return File(modelDir, filename).absolutePath
    }

    suspend fun download(
        modelId: String,
        filename: String,
        downloadUrl: String,
        totalSize: Long = 0L
    ): Result<String> = withContext(Dispatchers.IO) {
        val taskId = "$modelId/$filename"
        val destinationPath = getModelPath(modelId, filename)

        val task = DownloadTask(
            modelId = modelId,
            filename = filename,
            downloadUrl = downloadUrl,
            totalSize = totalSize,
            destinationPath = destinationPath,
            state = DownloadState.Downloading(0f, 0L, totalSize)
        )

        _downloads.value = _downloads.value + (taskId to task)

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val error = "HTTP ${response.code}: ${response.message}"
                updateTaskState(taskId, DownloadState.Failed(error))
                return@withContext Result.failure(IOException(error))
            }

            val body = response.body ?: throw IOException("Empty response body")

            val contentLength = if (totalSize > 0) totalSize else body.contentLength()

            val file = File(destinationPath)
            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        downloadedBytes[taskId] = totalBytesRead
                        val progress = if (contentLength > 0) {
                            (totalBytesRead.toFloat() / contentLength.toFloat())
                        } else 0f

                        updateTaskState(
                            taskId,
                            DownloadState.Downloading(progress, totalBytesRead, contentLength)
                        )
                    }
                }
            }

            downloadedBytes.remove(taskId)
            updateTaskState(taskId, DownloadState.Completed)
            Result.success(destinationPath)

        } catch (e: Exception) {
            updateTaskState(taskId, DownloadState.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    fun cancelDownload(modelId: String, filename: String) {
        val taskId = "$modelId/$filename"
        activeDownloads[taskId]?.cancel()
        activeDownloads.remove(taskId)
        downloadedBytes.remove(taskId)
        _downloads.value = _downloads.value - taskId
    }

    fun getDownloadState(modelId: String, filename: String): DownloadState? {
        val taskId = "$modelId/$filename"
        return _downloads.value[taskId]?.state
    }

    fun deleteModel(modelId: String): Result<Unit> {
        return try {
            val sanitizedId = modelId.replace("/", "_")
            val modelDir = File(modelsDir, sanitizedId)
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInstalledModels(): List<ModelInfo> {
        val models = mutableListOf<ModelInfo>()

        if (!modelsDir.exists()) return models

        modelsDir.listFiles()?.forEach { modelDir ->
            if (modelDir.isDirectory) {
                val files = modelDir.listFiles()?.map { it.name } ?: emptyList()
                val totalSize = modelDir.listFiles()?.sumOf { it.length() } ?: 0L
                val modelId = modelDir.name.replace("_", "/")

                val hasGGUF = files.any { it.endsWith(".gguf", ignoreCase = true) }
                val hasLiteRT = files.any { it.endsWith(".litertlm", ignoreCase = true) }
                val type = when {
                    hasGGUF -> "GGUF"
                    hasLiteRT -> "LiteRT"
                    else -> "Unknown"
                }

                models.add(ModelInfo(
                    modelId = modelId,
                    files = files,
                    totalSize = totalSize,
                    type = type,
                    installedAt = modelDir.lastModified()
                ))
            }
        }

        return models
    }

    private fun updateTaskState(taskId: String, state: DownloadState) {
        val current = _downloads.value[taskId] ?: return
        _downloads.value = _downloads.value + (taskId to current.copy(state = state))
    }
}

data class ModelInfo(
    val modelId: String,
    val files: List<String>,
    val totalSize: Long,
    val type: String,
    val installedAt: Long
) {
    val formattedSize: String
        get() = formatFileSize(totalSize)

    val hasGGUF: Boolean
        get() = files.any { it.endsWith(".gguf", ignoreCase = true) }

    val hasLiteRT: Boolean
        get() = files.any { it.endsWith(".litertlm", ignoreCase = true) }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}