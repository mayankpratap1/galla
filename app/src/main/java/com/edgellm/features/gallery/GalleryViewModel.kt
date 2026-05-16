package com.edgellm.features.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.core.error.Resource
import com.edgellm.domain.model.DownloadProgress
import com.edgellm.domain.model.DownloadState
import com.edgellm.domain.model.HFModel
import com.edgellm.domain.model.ModelFilter
import com.edgellm.domain.model.ModelFilterType
import com.edgellm.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val isLoading: Boolean = false,
    val models: List<HFModel> = emptyList(),
    val installedModels: List<HFModel> = emptyList(),
    val searchQuery: String = "",
    val filterType: ModelFilterTypeUI = ModelFilterTypeUI.TRENDING,
    val error: String? = null,
    val selectedModel: HFModel? = null,
    val downloadProgress: Map<String, Float> = emptyMap(),
    val availableStorage: Long = 0L
)

enum class ModelFilterTypeUI(val displayName: String) {
    TRENDING("Trending"),
    GGUF("GGUF Models"),
    LITERT("LiteRT Models"),
    CHAT("Chat Models"),
    VISION("Vision Models")
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val searchModelsUseCase: SearchModelsUseCase,
    private val getInstalledModelsUseCase: GetInstalledModelsUseCase,
    private val deleteModelUseCase: DeleteModelUseCase,
    private val downloadModelUseCase: DownloadModelUseCase,
    private val observeDownloadsUseCase: ObserveDownloadsUseCase,
    private val getAvailableStorageUseCase: GetAvailableStorageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadModels()
        refreshInstalledModels()
        observeDownloads()
        updateStorageInfo()
    }

    fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = ModelFilter(
                type = _uiState.value.filterType.toDomain(),
                limit = 20
            )

            when (val result = searchModelsUseCase(filter)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, models = result.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.failure.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = ModelFilter(
                query = query,
                limit = 20
            )

            when (val result = searchModelsUseCase(filter)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, models = result.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.failure.message)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setFilter(filterType: ModelFilterTypeUI) {
        _uiState.update { it.copy(filterType = filterType, searchQuery = "") }
        loadModels()
    }

    fun selectModel(model: HFModel) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun clearSelectedModel() {
        _uiState.update { it.copy(selectedModel = null) }
    }

    fun downloadModel(model: HFModel, filename: String) {
        val file = model.files.find { it.name == filename } ?: return
        val downloadUrl = "https://huggingface.co/${model.id}/resolve/main/$filename"

        viewModelScope.launch {
            when (val result = downloadModelUseCase(model, filename, downloadUrl, file.size)) {
                is Resource.Success -> {
                    refreshInstalledModels()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = "Download failed: ${result.failure.message}") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            when (deleteModelUseCase(modelId)) {
                is Resource.Success -> refreshInstalledModels()
                is Resource.Error -> { /* Handle error */ }
                is Resource.Loading -> {}
            }
        }
    }

    fun refreshInstalledModels() {
        val models = getInstalledModelsUseCase()
        _uiState.update { it.copy(installedModels = models) }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            observeDownloadsUseCase().collect { downloads ->
                val progressMap = downloads.mapValues { it.value.progress }
                _uiState.update { it.copy(downloadProgress = progressMap) }
            }
        }
    }

    private fun updateStorageInfo() {
        _uiState.update { it.copy(availableStorage = getAvailableStorageUseCase()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun ModelFilterTypeUI.toDomain(): ModelFilterType {
        return when (this) {
            ModelFilterTypeUI.TRENDING -> ModelFilterType.TRENDING
            ModelFilterTypeUI.GGUF -> ModelFilterType.GGUF
            ModelFilterTypeUI.LITERT -> ModelFilterType.LITERT
            ModelFilterTypeUI.CHAT -> ModelFilterType.CHAT
            ModelFilterTypeUI.VISION -> ModelFilterType.VISION
        }
    }
}