package com.edgellm.data.repository

import android.content.Context
import android.os.Environment
import com.edgellm.core.error.Failure
import com.edgellm.core.error.Resource
import com.edgellm.core.network.NetworkClient
import com.edgellm.domain.model.DownloadProgress
import com.edgellm.domain.model.DownloadState
import com.edgellm.domain.model.HFModel
import com.edgellm.domain.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val context: Context,
    private val networkClient: NetworkClient
) : DownloadRepository {

    private val _downloadsFlow = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    private val downloads = ConcurrentHashMap<String, DownloadProgress>()

    private val modelsDir: File by lazy {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }
        } else {
            File(context.filesDir, "models").also { it.mkdirs() }
        }
    }

    override suspend fun downloadModel(
        model: HFModel,
        filename: String,
        downloadUrl: String,
        totalSize: Long
    ): Resource<String> = withContext(Dispatchers.IO) {
        val taskId = "${model.id}/$filename"
        val destinationPath = getModelPath(model.id, filename)

        updateProgress(taskId, DownloadProgress(
            modelId = model.id,
            filename = filename,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = totalSize,
            state = DownloadState.DOWNLOADING
        ))

        try {
            val request = networkClient.buildRequest(downloadUrl) {
                get()
            }

            val response = networkClient.execute(request) { resp ->
                resp
            }

            if (response.isError) {
                updateProgress(taskId, DownloadProgress(
                    modelId = model.id,
                    filename = filename,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = totalSize,
                    state = DownloadState.FAILED
                ))
                return@withContext Resource.error(Failure.Download(overrideMessage = "Download failed"))
            }

            val httpResponse = (response as com.edgellm.core.error.Resource.Success).data

            if (!httpResponse.isSuccessful) {
                updateProgress(taskId, DownloadProgress(
                    modelId = model.id,
                    filename = filename,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = totalSize,
                    state = DownloadState.FAILED
                ))
                return@withContext Resource.error(Failure.Download(
                    overrideMessage = "HTTP ${httpResponse.code}"
                ))
            }

            val body = httpResponse.body ?: run {
                updateProgress(taskId, DownloadProgress(
                    modelId = model.id,
                    filename = filename,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = totalSize,
                    state = DownloadState.FAILED
                ))
                return@withContext Resource.error(Failure.Download(overrideMessage = "Empty response"))
            }

            val file = File(destinationPath)
            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    val contentLength = body.contentLength().takeIf { it > 0 } ?: totalSize

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val progress = (totalBytesRead.toFloat() / contentLength.toFloat())
                        updateProgress(taskId, DownloadProgress(
                            modelId = model.id,
                            filename = filename,
                            progress = progress,
                            downloadedBytes = totalBytesRead,
                            totalBytes = contentLength,
                            state = DownloadState.DOWNLOADING
                        ))
                    }
                }
            }

            updateProgress(taskId, DownloadProgress(
                modelId = model.id,
                filename = filename,
                progress = 1f,
                downloadedBytes = totalSize,
                totalBytes = totalSize,
                state = DownloadState.COMPLETED
            ))

            Resource.success(destinationPath)

        } catch (e: Exception) {
            updateProgress(taskId, DownloadProgress(
                modelId = model.id,
                filename = filename,
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = totalSize,
                state = DownloadState.FAILED
            ))
            Resource.error(Failure.Download(overrideMessage = e.message))
        }
    }

    override fun cancelDownload(modelId: String, filename: String) {
        val taskId = "$modelId/$filename"
        downloads.remove(taskId)
        _downloadsFlow.value = downloads.toMap()
    }

    override fun observeDownloads(): Flow<Map<String, DownloadProgress>> = _downloadsFlow

    override fun getDownloadProgress(modelId: String, filename: String): DownloadProgress? {
        val taskId = "$modelId/$filename"
        return downloads[taskId]
    }

    override fun getModelPath(modelId: String, filename: String): String {
        val sanitizedId = modelId.replace("/", "_")
        val modelDir = File(modelsDir, sanitizedId).also { it.mkdirs() }
        return File(modelDir, filename).absolutePath
    }

    override fun getAvailableStorage(): Long {
        val stat = android.os.StatFs(modelsDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    override fun hasEnoughStorage(requiredBytes: Long): Boolean {
        return getAvailableStorage() > requiredBytes + (100 * 1024 * 1024) // Keep 100MB free
    }

    private fun updateProgress(taskId: String, progress: DownloadProgress) {
        downloads[taskId] = progress
        _downloadsFlow.value = downloads.toMap()
    }
}