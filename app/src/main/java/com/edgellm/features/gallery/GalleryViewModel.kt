package com.edgellm.features.gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.edgellm.download.DownloadState
import com.edgellm.download.ModelDownloadService
import com.edgellm.download.ModelInfo
import com.edgellm.huggingface.HFFilterType
import com.edgellm.huggingface.HFModel
import com.edgellm.huggingface.HuggingFaceApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = false,
    val models: List<HFModel> = emptyList(),
    val installedModels: List<ModelInfo> = emptyList(),
    val searchQuery: String = "",
    val filterType: HFFilterType = HFFilterType.TRENDING,
    val error: String? = null,
    val selectedModel: HFModel? = null,
    val downloadProgress: Map<String, DownloadState> = emptyMap()
)

sealed class HFFilterType(val displayName: String) {
    data object Trending : HFFilterType("Trending")
    data object GGUF : HFFilterType("GGUF Models")
    data object LiteRT : HFFilterType("LiteRT Models")
    data object Chat : HFFilterType("Chat Models")
    data object Vision : HFFilterType("Vision Models")
}

class GalleryViewModel(
    private val apiClient: HuggingFaceApiClient,
    private val downloadService: ModelDownloadService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadModels()
        refreshInstalledModels()
        observeDownloads()
    }

    fun loadModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = when (_uiState.value.filterType) {
                HFFilterType.Trending -> apiClient.getTrendingModels(20)
                HFFilterType.GGUF -> apiClient.getGGUFModels(20)
                HFFilterType.LiteRT -> apiClient.getLiteRTModels(20)
                HFFilterType.Chat -> apiClient.searchModels(
                    com.edgellm.huggingface.HFModelFilter(
                        task = com.edgellm.huggingface.HFTask.CONVERSATIONAL,
                        limit = 20
                    )
                ).map { it.models }
                HFFilterType.Vision -> apiClient.searchModels(
                    com.edgellm.huggingface.HFModelFilter(
                        task = com.edgellm.huggingface.HFTask.IMAGE_TO_TEXT,
                        limit = 20
                    )
                ).map { it.models }
            }

            result.fold(
                onSuccess = { models ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        models = models
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load models"
                    )
                }
            )
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = apiClient.searchModels(
                com.edgellm.huggingface.HFModelFilter(
                    searchQuery = query,
                    limit = 20
                )
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        models = response.models
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun setFilter(filterType: HFFilterType) {
        _uiState.value = _uiState.value.copy(filterType = filterType, searchQuery = "")
        loadModels()
    }

    fun selectModel(model: HFModel) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun clearSelectedModel() {
        _uiState.value = _uiState.value.copy(selectedModel = null)
    }

    fun downloadModel(model: HFModel, filename: String) {
        val downloadUrl = apiClient.getDirectDownloadUrl(model.id, filename)
        
        viewModelScope.launch {
            val result = downloadService.download(
                modelId = model.id,
                filename = filename,
                downloadUrl = downloadUrl
            )

            result.fold(
                onSuccess = {
                    refreshInstalledModels()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Download failed: ${e.message}"
                    )
                }
            )
        }
    }

    fun deleteModel(modelId: String) {
        downloadService.deleteModel(modelId)
        refreshInstalledModels()
    }

    fun refreshInstalledModels() {
        val models = downloadService.getInstalledModels()
        _uiState.value = _uiState.value.copy(installedModels = models)
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadService.downloads.collect { downloads ->
                val progressMap = downloads.mapValues { it.value.state }
                _uiState.value = _uiState.value.copy(downloadProgress = progressMap)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(
                apiClient = HuggingFaceApiClient(),
                downloadService = ModelDownloadService(context)
            ) as T
        }
    }
}