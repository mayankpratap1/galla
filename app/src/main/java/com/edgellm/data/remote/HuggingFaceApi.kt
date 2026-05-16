package com.edgellm.data.remote

import com.edgellm.core.error.Failure
import com.edgellm.core.error.Resource
import com.edgellm.core.network.NetworkClient
import com.edgellm.data.remote.dto.HFModelDto
import com.edgellm.data.remote.dto.HFSearchResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HuggingFaceApi @Inject constructor(
    private val networkClient: NetworkClient
) {
    private val gson = Gson()

    companion object {
        private const val BASE_URL = "https://huggingface.co"
        private const val MODELS_ENDPOINT = "$BASE_URL/api/models"
    }

    suspend fun searchModels(
        query: String = "",
        pipelineTag: String? = null,
        library: String? = null,
        sort: String = "downloads",
        direction: String = "-",
        limit: Int = 20,
        offset: Int = 0
    ): Resource<List<HFModelDto>> {
        val urlBuilder = StringBuilder(MODELS_ENDPOINT)
        urlBuilder.append("?sort=$sort&direction=$direction&limit=$limit&offset=$offset&full=true")

        if (query.isNotBlank()) {
            urlBuilder.append("&search=$query")
        }
        pipelineTag?.let { urlBuilder.append("&pipeline_tag=$it") }
        library?.let { urlBuilder.append("&library=$it") }

        val request = networkClient.buildRequest(urlBuilder.toString()) {
            get()
        }

        return networkClient.execute(request) { response ->
            val body = response.body?.string() ?: throw Exception("Empty response")
            val type = object : TypeToken<HFSearchResponse>() {}.type
            val result: HFSearchResponse = gson.fromJson(body, type)
            result.models
        }
    }

    suspend fun getModelInfo(modelId: String): Resource<HFModelDto> {
        val request = networkClient.buildRequest("$MODELS_ENDPOINT/$modelId?full=true") {
            get()
        }

        return networkClient.execute(request) { response ->
            val body = response.body?.string() ?: throw Exception("Empty response")
            gson.fromJson(body, HFModelDto::class.java)
        }
    }

    fun getDirectDownloadUrl(modelId: String, filename: String): String {
        return "$BASE_URL/$modelId/resolve/main/$filename"
    }

    suspend fun getGGUFModels(limit: Int, offset: Int): Resource<List<HFModelDto>> {
        return searchModels(
            query = "",
            library = "llama-cpp",
            limit = limit,
            offset = offset
        )
    }

    suspend fun getLiteRTModels(limit: Int, offset: Int): Resource<List<HFModelDto>> {
        return searchModels(
            query = "",
            library = "litert",
            limit = limit,
            offset = offset
        )
    }

    suspend fun getTrendingModels(limit: Int): Resource<List<HFModelDto>> {
        return searchModels(limit = limit)
    }
}