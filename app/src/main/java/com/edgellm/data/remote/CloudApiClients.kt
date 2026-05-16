package com.edgellm.data.remote

import com.edgellm.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun chatCompletion(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        apiKey: String,
        baseUrl: String? = null,
        imageUrl: String? = null
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        try {
            val (url, requestBody) = when (provider) {
                ProviderType.OPENAI -> buildOpenAIRequest(model, messages, temperature, maxTokens, baseUrl)
                ProviderType.ANTHROPIC -> buildAnthropicRequest(model, messages, temperature, maxTokens, imageUrl)
                ProviderType.GEMINI -> buildGeminiRequest(model, messages, temperature, maxTokens, imageUrl)
                ProviderType.XAI -> buildXAIRequest(model, messages, temperature, maxTokens, baseUrl)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (provider == ProviderType.ANTHROPIC) {
                        addHeader("anthropic-version", "2023-06-01")
                    }
                }
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val completion = when (provider) {
                ProviderType.OPENAI, ProviderType.XAI -> gson.fromJson(body, OpenAIResponse::class.java).toDomain()
                ProviderType.ANTHROPIC -> gson.fromJson(body, AnthropicResponse::class.java).toDomain()
                ProviderType.GEMINI -> gson.fromJson(body, GeminiResponse::class.java).toDomain()
            }

            Result.success(completion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChatCompletion(
        provider: ProviderType,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        apiKey: String,
        baseUrl: String? = null,
        imageUrl: String? = null
    ): Flow<Result<String>> = flow {
        try {
            val (url, requestBody) = when (provider) {
                ProviderType.OPENAI -> buildOpenAIRequest(model, messages, temperature, maxTokens, baseUrl, stream = true)
                else -> buildOpenAIRequest(model, messages, temperature, maxTokens, baseUrl, stream = true)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body ?: throw Exception("Empty response body")

            body.byteStream().bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data != "[DONE]") {
                            val chunk = gson.fromJson(data, OpenAIChunk::class.java)
                            val content = chunk.choices?.firstOrNull()?.delta?.content
                            if (!content.isNullOrBlank()) {
                                emit(Result.success(content))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun buildOpenAIRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        baseUrl: String?,
        stream: Boolean = false
    ): Pair<String, String> {
        val url = baseUrl?.let { "$it/v1/chat/completions" } ?: "https://api.openai.com/v1/chat/completions"
        val body = gson.toJson(mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role.name.lowercase(), "content" to it.content) },
            "temperature" to temperature,
            "max_tokens" to maxTokens,
            "stream" to stream
        ))
        return url to body
    }

    private fun buildAnthropicRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        imageUrl: String?
    ): Pair<String, String> {
        val url = "https://api.anthropic.com/v1/messages"
        val systemMessage = messages.find { it.role == MessageRole.SYSTEM }?.content ?: ""
        val filteredMessages = messages.filter { it.role != MessageRole.SYSTEM }

        val content = mutableListOf<Map<String, Any>>()
        filteredMessages.forEach { msg ->
            content.add(mapOf(
                "type" to "text",
                "text" to msg.content
            ))
        }

        val body = gson.toJson(mapOf(
            "model" to model,
            "system" to systemMessage,
            "messages" to content,
            "max_tokens" to maxTokens,
            "temperature" to temperature
        ))
        return url to body
    }

    private fun buildGeminiRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        imageUrl: String?
    ): Pair<String, String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val contents = messages.map { msg ->
            mapOf(
                "role" to (if (msg.role == MessageRole.USER) "user" else "model"),
                "parts" to listOf(mapOf("text" to msg.content))
            )
        }
        val body = gson.toJson(mapOf(
            "contents" to contents,
            "generationConfig" to mapOf(
                "temperature" to temperature,
                "maxOutputTokens" to maxTokens
            )
        ))
        return url to body
    }

    private fun buildXAIRequest(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        maxTokens: Int,
        baseUrl: String?
    ): Pair<String, String> {
        val url = baseUrl?.let { "$it/v1/chat/completions" } ?: "https://api.x.ai/v1/chat/completions"
        val body = gson.toJson(mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role.name.lowercase(), "content" to it.content) },
            "temperature" to temperature,
            "max_tokens" to maxTokens
        ))
        return url to body
    }
}

data class OpenAIResponse(
    val id: String?,
    val choices: List<OpenAIChoice>?,
    val usage: OpenAIUsage?
) {
    fun toDomain(): ChatCompletionResponse {
        return ChatCompletionResponse(
            id = id ?: "unknown",
            choices = choices?.map { choice ->
                ChatChoice(
                    index = choice.index ?: 0,
                    message = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = choice.message?.content ?: ""
                    ),
                    finishReason = choice.finishReason
                )
            } ?: emptyList(),
            usage = Usage(
                promptTokens = usage?.promptTokens ?: 0,
                completionTokens = usage?.completionTokens ?: 0,
                totalTokens = usage?.totalTokens ?: 0
            )
        )
    }
}

data class OpenAIChoice(
    val index: Int?,
    val message: OpenAIMessage?,
    val finishReason: String?
)

data class OpenAIMessage(
    val role: String?,
    val content: String?
)

data class OpenAIUsage(
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)

data class OpenAIChunk(
    val choices: List<OpenAIChunkChoice>?
)

data class OpenAIChunkChoice(
    val delta: OpenAIDelta?,
    val finishReason: String?
)

data class OpenAIDelta(
    val content: String?
)

data class AnthropicResponse(
    val id: String?,
    val type: String?,
    val role: String?,
    val content: List<AnthropicContent>?,
    val model: String?,
    val stopReason: String?,
    val usage: AnthropicUsage?
) {
    fun toDomain(): ChatCompletionResponse {
        val textContent = content?.filterIsInstance<AnthropicContent>()?.firstOrNull()?.text ?: ""
        return ChatCompletionResponse(
            id = id ?: "unknown",
            choices = listOf(ChatChoice(
                index = 0,
                message = ChatMessage(role = MessageRole.ASSISTANT, content = textContent),
                finishReason = stopReason
            )),
            usage = Usage(
                promptTokens = usage?.inputTokens ?: 0,
                completionTokens = usage?.outputTokens ?: 0,
                totalTokens = (usage?.inputTokens ?: 0) + (usage?.outputTokens ?: 0)
            )
        )
    }
}

data class AnthropicContent(
    val type: String?,
    val text: String?
)

data class AnthropicUsage(
    val inputTokens: Int?,
    val outputTokens: Int?
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val usageMetadata: GeminiUsage?
) {
    fun toDomain(): ChatCompletionResponse {
        val content = candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        return ChatCompletionResponse(
            id = "gemini-${System.currentTimeMillis()}",
            choices = listOf(ChatChoice(
                index = 0,
                message = ChatMessage(role = MessageRole.ASSISTANT, content = content),
                finishReason = "stop"
            )),
            usage = Usage(
                promptTokens = usageMetadata?.promptTokenCount ?: 0,
                completionTokens = usageMetadata?.candidatesTokenCount ?: 0,
                totalTokens = usageMetadata?.totalTokenCount ?: 0
            )
        )
    }
}

data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String?
)

data class GeminiContent(
    val parts: List<GeminiPart>?
)

data class GeminiPart(
    val text: String?
)

data class GeminiUsage(
    val promptTokenCount: Int?,
    val candidatesTokenCount: Int?,
    val totalTokenCount: Int?
)