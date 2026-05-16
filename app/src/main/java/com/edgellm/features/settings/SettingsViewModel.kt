package com.edgellm.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.domain.model.CloudModel
import com.edgellm.domain.model.CloudModels
import com.edgellm.domain.model.ProviderType
import com.edgellm.domain.usecase.GetApiKeyUseCase
import com.edgellm.domain.usecase.GetAvailableCloudModelsUseCase
import com.edgellm.domain.usecase.SaveApiKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val selectedEngine: EngineType = EngineType.AUTO,
    val contextLength: Int = 2048,
    val temperature: Float = 0.7f,
    val apiKeys: Map<ProviderType, String> = emptyMap(),
    val availableModels: Map<ProviderType, List<CloudModel>> = emptyMap(),
    val showApiKeyDialog: ProviderType? = null,
    val isSaving: Boolean = false
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class EngineType { GGUF, LITERT, AUTO }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val saveApiKeyUseCase: SaveApiKeyUseCase,
    private val getApiKeyUseCase: GetApiKeyUseCase,
    private val getAvailableCloudModelsUseCase: GetAvailableCloudModelsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val providers = getAvailableCloudModelsUseCase.getAllProviders()
            val modelsMap = providers.associateWith { getAvailableCloudModelsUseCase(it) }

            _uiState.update {
                it.copy(availableModels = modelsMap)
            }

            // Load API keys
            val keysMap = mutableMapOf<ProviderType, String>()
            providers.forEach { provider ->
                val key = getApiKeyUseCase(provider)
                if (!key.isNullOrBlank()) {
                    keysMap[provider] = key
                }
            }
            _uiState.update { it.copy(apiKeys = keysMap) }
        }
    }

    fun setTheme(theme: ThemeMode) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun setEngine(engine: EngineType) {
        _uiState.update { it.copy(selectedEngine = engine) }
    }

    fun setContextLength(length: Int) {
        _uiState.update { it.copy(contextLength = length) }
    }

    fun setTemperature(temp: Float) {
        _uiState.update { it.copy(temperature = temp) }
    }

    fun showApiKeyDialog(provider: ProviderType) {
        _uiState.update { it.copy(showApiKeyDialog = provider) }
    }

    fun hideApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = null) }
    }

    fun saveApiKey(provider: ProviderType, apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            saveApiKeyUseCase(provider, apiKey)
            _uiState.update {
                it.copy(
                    apiKeys = it.apiKeys + (provider to apiKey),
                    isSaving = false,
                    showApiKeyDialog = null
                )
            }
        }
    }

    fun hasApiKey(provider: ProviderType): Boolean {
        return _uiState.value.apiKeys.containsKey(provider)
    }
}