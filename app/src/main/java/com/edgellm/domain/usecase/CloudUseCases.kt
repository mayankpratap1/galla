package com.edgellm.domain.usecase

import com.edgellm.core.error.Resource
import com.edgellm.domain.model.*
import com.edgellm.domain.repository.CloudRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendCloudMessageUseCase @Inject constructor(
    private val repository: CloudRepository
) {
    suspend operator fun invoke(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 2048,
        imageUrl: String? = null
    ): Resource<ChatCompletionResponse> {
        return repository.sendMessage(provider, model, messages, temperature, maxTokens, imageUrl)
    }
}

class StreamCloudMessageUseCase @Inject constructor(
    private val repository: CloudRepository
) {
    suspend operator fun invoke(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 2048,
        imageUrl: String? = null
    ): Flow<Resource<String>> {
        return repository.streamMessage(provider, model, messages, temperature, maxTokens, imageUrl)
    }
}

class SaveApiKeyUseCase @Inject constructor(
    private val repository: CloudRepository
) {
    suspend operator fun invoke(provider: ProviderType, apiKey: String) {
        repository.saveApiKey(provider, apiKey)
    }
}

class GetApiKeyUseCase @Inject constructor(
    private val repository: CloudRepository
) {
    suspend operator fun invoke(provider: ProviderType): String? {
        return repository.getApiKey(provider)
    }
}

class GetAvailableCloudModelsUseCase @Inject constructor() {
    operator fun invoke(provider: ProviderType): List<CloudModel> {
        return CloudModels.getModelsForProvider(provider)
    }

    fun getAllProviders(): List<ProviderType> {
        return ProviderType.entries.filter { it != ProviderType.XAI }
    }
}