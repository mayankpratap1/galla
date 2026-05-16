package com.edgellm.huggingface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HuggingFaceApiClient(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private const val BASE_URL = "https://huggingface.co/api"
        private const val MODELS_ENDPOINT = "$BASE_URL/models"
        private const val MODEL_FILES_ENDPOINT = "$BASE_URL/api/models"
    }

    suspend fun searchModels(filter: HFModelFilter): Result<HFSearchResponse> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = StringBuilder(MODELS_ENDPOINT)
            urlBuilder.append("?")

            if (filter.searchQuery.isNotBlank()) {
                urlBuilder.append("search=${filter.searchQuery}&")
            }
            filter.task?.let {
                urlBuilder.append("pipeline_tag=${it.value}&")
            }
            filter.library?.let {
                urlBuilder.append("library=${it.value}&")
            }
            urlBuilder.append("sort=${filter.sortBy.value}&")
            urlBuilder.append("direction=${if (filter.sortDirection == SortDirection.DESCENDING) "-" else "+"}&")
            urlBuilder.append("limit=${filter.limit}&")
            urlBuilder.append("offset=${filter.offset}&")
            urlBuilder.append("full=true")

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val errorMessage = try {
                    json.decodeFromString<HFApiError>(errorBody ?: "").error
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                return@withContext Result.failure(IOException(errorMessage))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response body")
            )

            val searchResponse = json.decodeFromString<HFSearchResponse>(body)
            Result.success(searchResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModelInfo(modelId: String): Result<HFModel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$MODELS_ENDPOINT/$modelId?full=true")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response")
            )

            val model = json.decodeFromString<HFModel>(body)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getModelFiles(modelId: String): Result<List<HFModelFile>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$MODEL_FILES_ENDPOINT/$modelId/revision/main")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response")
            )

            val files = json.decodeFromString<List<HFModelFile>>(body)
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDownloadUrl(modelId: String, filename: String): Result<HFModelDownloadInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$MODEL_FILES_ENDPOINT/$modelId/main/$filename")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(
                IOException("Empty response")
            )

            @Suppress("UNCHECKED_CAST")
            val responseMap = json.decodeFromString<Map<String, Any>>(body)
            
            val downloadUrl = responseMap["url"] as? String 
                ?: "https://huggingface.co/$modelId/resolve/main/$filename"
            
            val size = (responseMap["size"] as? Number)?.toLong() ?: 0L

            Result.success(HFModelDownloadInfo(downloadUrl, filename, size))
        } catch (e: Exception) {
            // Fallback to direct URL
            Result.success(HFModelDownloadInfo(
                "https://huggingface.co/$modelId/resolve/main/$filename",
                filename,
                0L
            ))
        }
    }

    fun getDirectDownloadUrl(modelId: String, filename: String): String {
        return "https://huggingface.co/$modelId/resolve/main/$filename"
    }

    suspend fun getTrendingModels(limit: Int = 10): Result<List<HFModel>> {
        return searchModels(HFModelFilter(
            sortBy = HFSortBy.DOWNLOADS,
            limit = limit
        )).map { it.models }
    }

    suspend fun getGGUFModels(limit: Int = 20, offset: Int = 0): Result<List<HFModel>> {
        return searchModels(HFModelFilter(
            library = HFLibrary.LLAMA_CPP,
            sortBy = HFSortBy.DOWNLOADS,
            limit = limit,
            offset = offset
        )).map { it.models }
    }

    suspend fun getLiteRTModels(limit: Int = 20, offset: Int = 0): Result<List<HFModel>> {
        return searchModels(HFModelFilter(
            library = HFLibrary.LITERTLM,
            sortBy = HFSortBy.DOWNLOADS,
            limit = limit,
            offset = offset
        )).map { it.models }
    }
}