package com.edgellm.engine

// kotlinllamacpp — Kotlin bindings for llama.cpp (GGUF models on Android)
// Source: https://github.com/ljcamargo/kotlinllamacpp
// Maven: io.github.ljcamargo:llamacpp-kotlin:0.4.0
// Key facts:
//   - Uses File Descriptor (FD) mechanism — bypasses Android scoped storage restrictions
//   - load() callback receives model ID (Long?), NOT an error object
//   - predict() is the trigger — events arrive via SharedFlow
//   - Multimodal: load with mmprojPath for LLaVA-style vision models
import android.content.ContentResolver
import android.net.Uri
import io.github.ljcamargo.llamacpp.LlamaHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GgufEngine(private val contentResolver: ContentResolver) : InferenceEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex() // llama.cpp is NOT thread-safe — serialize all inference

    private val _events = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var helper: LlamaHelper? = null
    var mmprojUri: String? = null  // Optional: set for LLaVA vision models

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = true   // LLaVA GGUF models with mmproj file
    override val supportsThinking = true // Any model emitting <think> tags

    // FIXED: load() now uses CompletableDeferred to wait for the async callback.
    // Original code returned Result.success before callback fired — race condition.
    //
    // FIXED: Callback receives model ID (Long?) on success, not an error object.
    // Library docs: llamaHelper.load(path = modelUri) { id -> /* id is Long? */ }
    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val loadDeferred = CompletableDeferred<Unit>()
                val h = LlamaHelper(contentResolver, scope, _events)

                // Load with optional mmproj for multimodal (LLaVA-style) models
                if (mmprojUri != null) {
                    h.load(
                        path         = uri,
                        contextLength = config.contextLength,
                        mmprojPath   = mmprojUri
                    ) { _ ->
                        // Callback: model ID received — load succeeded
                        helper    = h
                        isLoaded  = true
                        modelName = extractModelName(uri)
                        loadDeferred.complete(Unit)
                    }
                } else {
                    h.load(path = uri, contextLength = config.contextLength) { _ ->
                        helper    = h
                        isLoaded  = true
                        modelName = extractModelName(uri)
                        loadDeferred.complete(Unit)
                    }
                }

                // Block until callback fires (with 30s timeout for large models)
                withTimeout(30_000L) { loadDeferred.await() }
                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                isLoaded = false
                helper   = null
                Result.failure(Exception("Model load timed out after 30s — model may be too large"))
            } catch (e: Exception) {
                isLoaded = false
                helper   = null
                Result.failure(e)
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        check(isLoaded) { "No GGUF model loaded" }
        return mutex.withLock {
            val sb = StringBuilder()
            val job = scope.launch {
                _events.collect { e ->
                    when (e) {
                        is LlamaHelper.LLMEvent.Ongoing -> sb.append(e.word)
                        is LlamaHelper.LLMEvent.Done    -> return@collect
                        else -> {}
                    }
                }
            }
            helper!!.predict(prompt)
            job.join()
            sb.toString()
        }
    }

    // FIXED: Uses callbackFlow for proper stream termination.
    // Original MutableSharedFlow approach never emitted a completion signal —
    // downstream collectors ran forever.
    override fun generateStream(prompt: String): Flow<String> {
        check(isLoaded) { "No GGUF model loaded" }
        return callbackFlow<String> {
            val eventJob = scope.launch {
                _events.collect { e ->
                    when (e) {
                        is LlamaHelper.LLMEvent.Ongoing -> trySend(e.word)
                        is LlamaHelper.LLMEvent.Done    -> close() // Terminates the flow
                        is LlamaHelper.LLMEvent.Error   -> close(Exception(e.toString()))
                        else -> {}
                    }
                }
            }
            mutex.withLock { helper!!.predict(prompt) }
            awaitClose { eventJob.cancel() }
        }.flowOn(Dispatchers.IO)
    }

    // Vision: multimodal models (LLaVA) can analyze images when mmprojUri is set.
    // For models without mmproj, falls back to base64 text embedding.
    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        if (mmprojUri != null) {
            // True multimodal path — helper analyzes JPEG bytes with the vision projector
            check(isLoaded) { "No GGUF model loaded" }
            return mutex.withLock {
                val sb = StringBuilder()
                val job = scope.launch {
                    _events.collect { e ->
                        when (e) {
                            is LlamaHelper.LLMEvent.Ongoing -> sb.append(e.word)
                            is LlamaHelper.LLMEvent.Done    -> return@collect
                            else -> {}
                        }
                    }
                }
                // Pass image bytes alongside the text prompt for multimodal inference
                helper!!.predict(prompt, imageBytes)
                job.join()
                sb.toString()
            }
        } else {
            // Text-only fallback: base64-encode image in prompt
            val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            return generate("[Image data: $base64]\n$prompt")
        }
    }

    override fun unload() {
        helper    = null
        isLoaded  = false
        modelName = null
        mmprojUri = null
    }

    private fun extractModelName(uri: String): String =
        uri.substringAfterLast("/").substringBeforeLast(".")
}
