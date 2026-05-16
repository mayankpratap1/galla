package com.edgellm.domain.usecase

import com.edgellm.core.error.Resource
import com.edgellm.domain.model.DownloadProgress
import com.edgellm.domain.model.HFModel
import com.edgellm.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DownloadModelUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        model: HFModel,
        filename: String,
        downloadUrl: String,
        totalSize: Long
    ): Resource<String> {
        if (!repository.hasEnoughStorage(totalSize)) {
            return Resource.error(
                com.edgellm.core.error.Failure.Download(
                    overrideMessage = "Not enough storage space. Required: ${formatSize(totalSize)}"
                )
            )
        }
        return repository.downloadModel(model, filename, downloadUrl, totalSize)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000.0} GB"
            bytes >= 1_000_000 -> "${bytes / 1_000_000.0} MB"
            else -> "${bytes / 1_000.0} KB"
        }
    }
}

class CancelDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(modelId: String, filename: String) {
        repository.cancelDownload(modelId, filename)
    }
}

class ObserveDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<Map<String, DownloadProgress>> {
        return repository.observeDownloads()
    }
}

class GetDownloadProgressUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(modelId: String, filename: String): DownloadProgress? {
        return repository.getDownloadProgress(modelId, filename)
    }
}

class GetAvailableStorageUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Long {
        return repository.getAvailableStorage()
    }

    fun hasEnough(requiredBytes: Long): Boolean {
        return repository.hasEnoughStorage(requiredBytes)
    }
}