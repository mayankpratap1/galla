package com.edgellm.core.network

import com.edgellm.core.error.Failure
import com.edgellm.core.error.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkClient {
    suspend fun <T> execute(request: Request, mapper: (Response) -> T): Resource<T>
    fun buildRequest(url: String, block: Request.Builder.() -> Unit): Request
}

@Singleton
class OkHttpNetworkClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) : NetworkClient {

    override suspend fun <T> execute(request: Request, mapper: (Response) -> T): Resource<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Resource.success(mapper(response))
                } else {
                    Resource.error(Failure.Server(
                        statusCode = response.code,
                        overrideMessage = response.message
                    ))
                }
            } catch (e: IOException) {
                Resource.error(Failure.Network(overrideMessage = e.message))
            } catch (e: Exception) {
                Resource.error(Failure.Unknown(throwable = e))
            }
        }

    override fun buildRequest(url: String, block: Request.Builder.() -> Unit): Request {
        return Request.Builder()
            .url(url)
            .apply(block)
            .build()
    }
}

sealed class HttpMethod {
    data object GET : HttpMethod()
    data object POST : HttpMethod()
    data object PUT : HttpMethod()
    data object DELETE : HttpMethod()
}

suspend inline fun <T> safeApiCall(
    noinline request: () -> Request,
    noinline mapper: (Response) -> T,
    client: NetworkClient
): Resource<T> = client.execute(request(), mapper)