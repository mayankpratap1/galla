package com.edgellm.domain.repository

import com.edgellm.core.error.Resource
import com.edgellm.domain.model.DownloadProgress
import com.edgellm.domain.model.HFModel
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    suspend fun downloadModel(
        model: HFModel,
        filename: String,
        downloadUrl: String,
        totalSize: Long
    ): Resource<String>

    fun cancelDownload(modelId: String, filename: String)

    fun observeDownloads(): Flow<Map<String, DownloadProgress>>

    fun getDownloadProgress(modelId: String, filename: String): DownloadProgress?

    fun getModelPath(modelId: String, filename: String): String

    fun getAvailableStorage(): Long

    fun hasEnoughStorage(requiredBytes: Long): Boolean
}