package com.edgellm.data.repository

import com.edgellm.core.error.Failure
import com.edgellm.core.error.Resource
import com.edgellm.data.local.PreferencesManager
import com.edgellm.data.remote.HuggingFaceApi
import com.edgellm.domain.model.HFModel
import com.edgellm.domain.model.InstalledModel
import com.edgellm.domain.model.ModelFilter
import com.edgellm.domain.model.ModelType
import com.edgellm.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.inject.Inject

class ModelRepositoryImpl @Inject constructor(
    private val huggingFaceApi: HuggingFaceApi,
    private val preferencesManager: PreferencesManager
) : ModelRepository {

    private val modelsDir by lazy {
        File(preferencesManager.getModelsDirectory(), "models").also { it.mkdirs() }
    }

    private val _installedModelsFlow = MutableStateFlow<List<InstalledModel>>(emptyList())

    override suspend fun searchModels(filter: ModelFilter): Resource<List<HFModel>> {
        val result = huggingFaceApi.searchModels(
            query = filter.query,
            pipelineTag = getPipelineTag(filter.type),
            library = getLibrary(filter.type),
            sort = getSortString(filter.sortBy),
            limit = filter.limit,
            offset = filter.offset
        )

        return result.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getTrendingModels(limit: Int): Resource<List<HFModel>> {
        val result = huggingFaceApi.getTrendingModels(limit)
        return result.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getGGUFModels(limit: Int, offset: Int): Resource<List<HFModel>> {
        val result = huggingFaceApi.getGGUFModels(limit, offset)
        return result.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getLiteRTModels(limit: Int, offset: Int): Resource<List<HFModel>> {
        val result = huggingFaceApi.getLiteRTModels(limit, offset)
        return result.map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun getModelDetails(modelId: String): Resource<HFModel> {
        val result = huggingFaceApi.getModelInfo(modelId)
        return result.map { it.toDomain() }
    }

    override fun getInstalledModels(): List<InstalledModel> {
        val models = mutableListOf<InstalledModel>()

        if (!modelsDir.exists()) return models

        modelsDir.listFiles()?.forEach { modelDir ->
            if (modelDir.isDirectory) {
                val files = modelDir.listFiles()?.map { it.name } ?: emptyList()
                val totalSize = modelDir.listFiles()?.sumOf { it.length() } ?: 0L
                val modelId = modelDir.name.replace("_", "/")

                val hasGGUF = files.any { it.endsWith(".gguf", ignoreCase = true) }
                val hasLiteRT = files.any { it.endsWith(".litertlm", ignoreCase = true) }
                val type = when {
                    hasGGUF -> ModelType.GGUF
                    hasLiteRT -> ModelType.LITERT
                    else -> ModelType.UNKNOWN
                }

                models.add(InstalledModel(
                    modelId = modelId,
                    files = files,
                    totalSize = totalSize,
                    type = type,
                    installedAt = modelDir.lastModified(),
                    path = modelDir.absolutePath
                ))
            }
        }

        _installedModelsFlow.value = models
        return models
    }

    override suspend fun deleteModel(modelId: String): Resource<Unit> {
        return try {
            val sanitizedId = modelId.replace("/", "_")
            val modelDir = File(modelsDir, sanitizedId)
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
                _installedModelsFlow.value = getInstalledModels()
            }
            Resource.success(Unit)
        } catch (e: Exception) {
            Resource.error(Failure.Cache(overrideMessage = "Failed to delete model: ${e.message}"))
        }
    }

    override fun observeInstalledModels(): Flow<List<InstalledModel>> = _installedModelsFlow

    private fun getPipelineTag(type: com.edgellm.domain.model.ModelFilterType): String? {
        return when (type) {
            com.edgellm.domain.model.ModelFilterType.CHAT -> "conversational"
            com.edgellm.domain.model.ModelFilterType.VISION -> "image-text-to-text"
            else -> null
        }
    }

    private fun getLibrary(type: com.edgellm.domain.model.ModelFilterType): String? {
        return when (type) {
            com.edgellm.domain.model.ModelFilterType.GGUF -> "llama-cpp"
            com.edgellm.domain.model.ModelFilterType.LITERT -> "litert"
            else -> null
        }
    }

    private fun getSortString(sortBy: com.edgellm.domain.model.SortOption): String {
        return when (sortBy) {
            com.edgellm.domain.model.SortOption.DOWNLOADS -> "downloads"
            com.edgellm.domain.model.SortOption.LIKES -> "likes"
            com.edgellm.domain.model.SortOption.RECENT -> "lastModified"
        }
    }
}