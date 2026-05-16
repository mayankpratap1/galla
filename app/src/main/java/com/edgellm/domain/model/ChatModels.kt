package com.edgellm.domain.model

import kotlinx.serialization.Serializable

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val imageUrl: String? = null
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val stream: Boolean = false,
    val imageUrl: String? = null
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<ChatChoice>,
    val usage: Usage
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String?
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class CloudProvider(
    val type: ProviderType,
    val apiKey: String,
    val baseUrl: String? = null
)

enum class ProviderType {
    OPENAI,
    ANTHROPIC,
    GEMINI,
    XAI
}

data class CloudModel(
    val id: String,
    val name: String,
    val provider: ProviderType,
    val supportsVision: Boolean = false,
    val supportsJson: Boolean = true,
    val maxTokens: Int = 4096,
    val contextWindow: Int = 128000
)

object CloudModels {
    val OPENAI_MODELS = listOf(
        CloudModel("gpt-4o", "GPT-4o", ProviderType.OPENAI, supportsVision = true, contextWindow = 128000),
        CloudModel("gpt-4o-mini", "GPT-4o Mini", ProviderType.OPENAI, supportsVision = true, contextWindow = 128000),
        CloudModel("gpt-4-turbo", "GPT-4 Turbo", ProviderType.OPENAI, supportsVision = true, contextWindow = 128000),
        CloudModel("gpt-3.5-turbo", "GPT-3.5 Turbo", ProviderType.OPENAI, contextWindow = 16385)
    )

    val ANTHROPIC_MODELS = listOf(
        CloudModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", ProviderType.ANTHROPIC, supportsVision = true, contextWindow = 200000),
        CloudModel("claude-3-opus-20240229", "Claude 3 Opus", ProviderType.ANTHROPIC, supportsVision = true, contextWindow = 200000),
        CloudModel("claude-3-haiku-20240307", "Claude 3 Haiku", ProviderType.ANTHROPIC, contextWindow = 200000)
    )

    val GEMINI_MODELS = listOf(
        CloudModel("gemini-2.0-flash-exp", "Gemini 2.0 Flash", ProviderType.GEMINI, supportsVision = true, contextWindow = 1000000),
        CloudModel("gemini-1.5-pro", "Gemini 1.5 Pro", ProviderType.GEMINI, supportsVision = true, contextWindow = 200000),
        CloudModel("gemini-1.5-flash", "Gemini 1.5 Flash", ProviderType.GEMINI, supportsVision = true, contextWindow = 1000000)
    )

    val ALL_MODELS = OPENAI_MODELS + ANTHROPIC_MODELS + GEMINI_MODELS

    fun getModelsForProvider(provider: ProviderType): List<CloudModel> {
        return when (provider) {
            ProviderType.OPENAI -> OPENAI_MODELS
            ProviderType.ANTHROPIC -> ANTHROPIC_MODELS
            ProviderType.GEMINI -> GEMINI_MODELS
            ProviderType.XAI -> emptyList()
        }
    }
}