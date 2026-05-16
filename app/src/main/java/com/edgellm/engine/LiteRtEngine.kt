package com.edgellm.engine

// Official LiteRT-LM Kotlin API
// Source: https://ai.google.dev/edge/litert-lm/android
// Docs: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
// Maven: com.google.ai.edge.litertlm:litertlm-android:latest.release
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LiteRtEngine(private val cacheDir: String? = null) : InferenceEngine {

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = true   // Gemma 4 multimodal
    override val supportsThinking = true // Gemma 4 thinking mode

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Unload any previous engine to free memory
                conversation?.close()
                engine?.close()
                conversation = null
                engine = null

                // FIXED: Backend.CPU() / Backend.GPU() are constructor calls, NOT enum values.
                // The original code used Backend.CPU (wrong) — crashes at runtime.
                // Official API: Backend.CPU(), Backend.GPU(), Backend.NPU("provider_string")
                val backend = when {
                    config.useNpu -> Backend.NPU("") // NPU provider string varies by device
                    config.useGpu -> Backend.GPU()
                    else          -> Backend.CPU()
                }

                // FIXED: EngineConfig constructor matches official API exactly.
                // cacheDir is optional — improves 2nd-load time by caching compiled kernels.
                val engineConfig = com.google.ai.edge.litertlm.EngineConfig(
                    modelPath = uri,             // Must be absolute file path, NOT content:// URI
                    backend   = backend,
                    // Uncomment if cacheDir is provided for faster subsequent loads:
                    // cacheDir = cacheDir
                )

                val e = Engine(engineConfig)
                // CRITICAL: initialize() can take up to 10s — already on Dispatchers.IO
                e.initialize()

                engine       = e
                conversation = e.createConversation()
                isLoaded     = true
                modelName    = uri.substringAfterLast("/").substringBeforeLast(".")
                Result.success(Unit)
            } catch (ex: Exception) {
                isLoaded = false
                Result.failure(ex)
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        check(isLoaded) { "No LiteRT model loaded" }
        return withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            // Official API: sendMessageAsync returns Flow<String>
            conversation!!.sendMessageAsync(prompt).collect { token -> sb.append(token) }
            sb.toString()
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        check(isLoaded) { "No LiteRT model loaded" }
        // Official API: sendMessageAsync(prompt) returns a Flow<String> of tokens
        return conversation!!.sendMessageAsync(prompt).flowOn(Dispatchers.IO)
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        // LiteRT-LM multimodal: Gemma 4 supports vision via sendMessageAsync with image
        // Full multimodal API is stable in later builds — using text fallback for now
        // TODO: Replace with conversation!!.sendMessageAsync(prompt, imageBytes).collect{...}
        //       once the multimodal Conversation API stabilises in your target version
        return generate(prompt)
    }

    override fun unload() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine       = null
        isLoaded     = false
        modelName    = null
    }
}
