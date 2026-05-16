package com.edgellm.core.error

sealed class Failure {
    abstract val message: String
    abstract val code: String?

    data class Network(val overrideMessage: String? = null) : Failure() {
        override val message: String = overrideMessage ?: "Network error occurred"
        override val code: String? = "NETWORK_ERROR"
    }

    data class Server(val overrideMessage: String? = null, val statusCode: Int? = null) : Failure() {
        override val message: String = overrideMessage ?: "Server error occurred"
        override val code: String? = statusCode?.toString() ?: "SERVER_ERROR"
    }

    data class Cache(val overrideMessage: String? = null) : Failure() {
        override val message: String = overrideMessage ?: "Cache error occurred"
        override val code: String? = "CACHE_ERROR"
    }

    data class Unknown(val overrideMessage: String? = null, val throwable: Throwable? = null) : Failure() {
        override val message: String = overrideMessage ?: "Unknown error occurred"
        override val code: String? = "UNKNOWN_ERROR"
        val exception: Throwable? = throwable
    }

    data class Validation(val overrideMessage: String? = null) : Failure() {
        override val message: String = overrideMessage ?: "Validation failed"
        override val code: String? = "VALIDATION_ERROR"
    }

    data class Model(val overrideMessage: String? = null) : Failure() {
        override val message: String = overrideMessage ?: "Model loading failed"
        override val code: String? = "MODEL_ERROR"
    }

    data class Download(val overrideMessage: String? = null) : Failure() {
        override val message: String = overrideMessage ?: "Download failed"
        override val code: String? = "DOWNLOAD_ERROR"
    }
}

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val failure: Failure) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(failure)
        is Loading -> Loading
    }

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun error(failure: Failure): Resource<Nothing> = Error(failure)
        fun loading(): Resource<Nothing> = Loading
    }
}

inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) action(data)
    return this
}

inline fun <T> Resource<T>.onError(action: (Failure) -> Unit): Resource<T> {
    if (this is Resource.Error) action(failure)
    return this
}

inline fun <T> Resource<T>.onLoading(action: () -> Unit): Resource<T> {
    if (this is Resource.Loading) action()
    return this
}