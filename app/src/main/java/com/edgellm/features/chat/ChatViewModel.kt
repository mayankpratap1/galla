package com.edgellm.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.core.error.Resource
import com.edgellm.domain.model.*
import com.edgellm.domain.repository.CloudRepository
import com.edgellm.engine.InferenceEngine
import com.edgellm.domain.usecase.SendCloudMessageUseCase
import com.edgellm.domain.usecase.StreamCloudMessageUseCase
import com.edgellm.skills.SkillManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedModel: ModelSelection = ModelSelection.Local,
    val availableModels: List<String> = emptyList(),
    val currentProvider: ProviderType = ProviderType.OPENAI,
    val currentCloudModel: String = "gpt-4o-mini"
)

sealed class ModelSelection {
    data object Local : ModelSelection()
    data class Cloud(val provider: ProviderType, val model: String) : ModelSelection()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendCloudMessageUseCase: SendCloudMessageUseCase,
    private val streamCloudMessageUseCase: StreamCloudMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var engineRef: InferenceEngine? = null
    var skillManager: SkillManager? = null
    var agentSkillsEnabled: Boolean = true

    private var streamingJob: Job? = null

    fun setModelLoaded(loaded: Boolean) {
        _uiState.update { it.copy(availableModels = if (loaded) listOfNotNull(engineRef?.modelName) else emptyList()) }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank()) return

        val userMessage = ChatMessage(role = MessageRole.USER, content = input)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        when (val selection = _uiState.value.selectedModel) {
            is ModelSelection.Local -> sendLocalMessage(input)
            is ModelSelection.Cloud -> sendCloudMessage(input, selection.provider, selection.model)
        }
    }

    private fun sendLocalMessage(input: String) {
        viewModelScope.launch {
            try {
                val engine = engineRef
                if (engine == null || !engine.isLoaded) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No model loaded. Please load a model first."
                        )
                    }
                    return@launch
                }

                val response = engine.generate(input)
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = response
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Generation failed"
                    )
                }
            }
        }
    }

    private fun sendCloudMessage(input: String, provider: ProviderType, model: String) {
        viewModelScope.launch {
            val messages = _uiState.value.messages + ChatMessage(role = MessageRole.USER, content = input)

            when (val result = sendCloudMessageUseCase(
                provider = provider,
                model = model,
                messages = messages,
                temperature = 0.7,
                maxTokens = 2048
            )) {
                is Resource.Success -> {
                    val response = result.data.choices.firstOrNull()?.message?.content ?: ""
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage(role = MessageRole.ASSISTANT, content = response),
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.failure.message
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun selectModel(modelName: String) {
        _uiState.update { it.copy(selectedModel = ModelSelection.Local) }
    }

    fun selectCloudModel(provider: ProviderType, model: String) {
        _uiState.update {
            it.copy(
                selectedModel = ModelSelection.Cloud(provider, model),
                currentProvider = provider,
                currentCloudModel = model
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}