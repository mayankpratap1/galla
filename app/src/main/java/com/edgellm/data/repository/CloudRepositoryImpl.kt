package com.edgellm.data.repository

import com.edgellm.core.error.Resource
import com.edgellm.data.local.PreferencesManager
import com.edgellm.data.remote.CloudApiClient
import com.edgellm.domain.model.*
import com.edgellm.domain.repository.CloudRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepositoryImpl @Inject constructor(
    private val cloudApiClient: CloudApiClient,
    private val preferencesManager: PreferencesManager
) : CloudRepository {

    override suspend fun sendMessage(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        imageUrl: String?
    ): Resource<ChatCompletionResponse> {
        val apiKey = getApiKey(provider) ?: return Resource.error(
            com.edgellm.core.error.Failure.Validation(overrideMessage = "No API key for ${provider.name}")
        )

        val result = cloudApiClient.chatCompletion(
            provider = provider,
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            apiKey = apiKey,
            imageUrl = imageUrl
        )

        return result.fold(
            onSuccess = { Resource.success(it) },
            onFailure = { Resource.error(com.edgellm.core.error.Failure.Network(overrideMessage = it.message)) }
        )
    }

    override suspend fun streamMessage(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        imageUrl: String?
    ): Flow<Resource<String>> = flow {
        val apiKey = getApiKey(provider)
        if (apiKey == null) {
            emit(Resource.error(com.edgellm.core.error.Failure.Validation(overrideMessage = "No API key")))
            return@flow
        }

        cloudApiClient.streamChatCompletion(
            provider = provider,
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            apiKey = apiKey,
            imageUrl = imageUrl
        ).collect { result ->
            emit(result.fold(
                onSuccess = { Resource.success(it) },
                onFailure = { Resource.error(com.edgellm.core.error.Failure.Network(overrideMessage = it.message)) }
            ))
        }
    }

    override suspend fun saveApiKey(provider: ProviderType, apiKey: String) {
        when (provider) {
            ProviderType.OPENAI -> preferencesManager.saveApiKey(com.edgellm.data.local.ApiProvider.OPENAI, apiKey)
            ProviderType.ANTHROPIC -> preferencesManager.saveApiKey(com.edgellm.data.local.ApiProvider.ANTHROPIC, apiKey)
            ProviderType.GEMINI -> preferencesManager.saveApiKey(com.edgellm.data.local.ApiProvider.GEMINI, apiKey)
            ProviderType.XAI -> {}
        }
    }

    override suspend fun getApiKey(provider: ProviderType): String? {
        return when (provider) {
            ProviderType.OPENAI -> preferencesManager.getApiKey(com.edgellm.data.local.ApiProvider.OPENAI)
            ProviderType.ANTHROPIC -> preferencesManager.getApiKey(com.edgellm.data.local.ApiProvider.ANTHROPIC)
            ProviderType.GEMINI -> preferencesManager.getApiKey(com.edgellm.data.local.ApiProvider.GEMINI)
            ProviderType.XAI -> null
        }
    }

    override suspend fun clearApiKey(provider: ProviderType) {
        saveApiKey(provider, "")
    }

    override fun hasApiKey(provider: ProviderType): Boolean {
        // This is sync - in production you'd want to cache this
        return false // Will be checked via getApiKey in async context
    }
}