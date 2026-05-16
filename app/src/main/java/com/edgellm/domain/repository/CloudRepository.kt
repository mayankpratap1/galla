package com.edgellm.domain.repository

import com.edgellm.core.error.Resource
import com.edgellm.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CloudRepository {
    suspend fun sendMessage(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        imageUrl: String? = null
    ): Resource<ChatCompletionResponse>

    suspend fun streamMessage(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        imageUrl: String? = null
    ): Flow<Resource<String>>

    suspend fun saveApiKey(provider: ProviderType, apiKey: String)
    suspend fun getApiKey(provider: ProviderType): String?
    suspend fun clearApiKey(provider: ProviderType)

    fun hasApiKey(provider: ProviderType): Boolean
}