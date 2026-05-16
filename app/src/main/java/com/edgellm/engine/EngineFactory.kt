package com.edgellm.engine

import android.content.ContentResolver
import android.content.Context

object EngineFactory {
    /**
     * Returns the correct engine based on file extension.
     * .gguf     → GgufEngine  (kotlinllamacpp, llama.cpp)
     * .litertlm → LiteRtEngine (Google LiteRT-LM, official SDK)
     *
     * IMPORTANT:
     * - GGUF:    pass content:// URI from file picker — uses FD mechanism internally
     * - LiteRT:  must be absolute file path (NOT content://) — copy to internal storage first
     */
    fun create(uri: String, contentResolver: ContentResolver, context: Context): InferenceEngine {
        return when {
            uri.endsWith(".gguf", ignoreCase = true) ||
            uri.contains(".gguf", ignoreCase = true) ->
                GgufEngine(contentResolver)

            uri.endsWith(".litertlm", ignoreCase = true) ||
            uri.contains(".litertlm", ignoreCase = true) ->
                // Pass cacheDir so LiteRT can cache compiled GPU kernels for faster reloads
                LiteRtEngine(cacheDir = context.cacheDir.absolutePath)

            else -> throw IllegalArgumentException(
                "Unknown model format for URI: $uri\n" +
                "Supported: .gguf (GGUF/llama.cpp) or .litertlm (LiteRT-LM/Google)"
            )
        }
    }
}
