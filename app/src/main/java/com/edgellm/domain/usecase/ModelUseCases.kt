package com.edgellm.domain.usecase

import com.edgellm.core.error.Failure
import com.edgellm.core.error.Resource
import com.edgellm.domain.model.HFModel
import com.edgellm.domain.model.ModelFilter
import com.edgellm.domain.model.ModelFilterType
import com.edgellm.domain.model.SortOption
import com.edgellm.domain.repository.ModelRepository
import javax.inject.Inject

class SearchModelsUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    suspend operator fun invoke(filter: ModelFilter): Resource<List<HFModel>> {
        if (filter.query.isBlank() && filter.type == ModelFilterType.TRENDING) {
            return repository.getTrendingModels(filter.limit)
        }
        return repository.searchModels(filter)
    }
}

class GetTrendingModelsUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    suspend operator fun invoke(limit: Int = 20): Resource<List<HFModel>> {
        return repository.getTrendingModels(limit)
    }
}

class GetGGUFModelsUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    suspend operator fun invoke(limit: Int = 20, offset: Int = 0): Resource<List<HFModel>> {
        return repository.getGGUFModels(limit, offset)
    }
}

class GetLiteRTModelsUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    suspend operator fun invoke(limit: Int = 20, offset: Int = 0): Resource<List<HFModel>> {
        return repository.getLiteRTModels(limit, offset)
    }
}

class GetModelDetailsUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    suspend operator fun invoke(modelId: String): Resource<HFModel> {
        return repository.getModelDetails(modelId)
    }
}

class GetInstalledModelsUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    operator fun invoke(): List<HFModel> {
        return repository.getInstalledModels().map { installed ->
            HFModel(
                id = installed.modelId,
                author = null,
                lastModified = null,
                downloads = 0,
                likes = 0,
                pipelineTag = null,
                tags = emptyList(),
                files = installed.files.map { name ->
                    com.edgellm.domain.model.HFModelFile(
                        name = name,
                        size = 0,
                        isGGUF = name.endsWith(".gguf", ignoreCase = true),
                        isLiteRT = name.endsWith(".litertlm", ignoreCase = true)
                    )
                },
                modelType = installed.type,
                size = installed.totalSize,
                quantization = null
            )
        }
    }
}

class DeleteModelUseCase @Inject constructor(
    private val repository: ModelRepository
) {
    suspend operator fun invoke(modelId: String): Resource<Unit> {
        return repository.deleteModel(modelId)
    }
}