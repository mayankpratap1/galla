package com.edgellm.core.di

import android.content.Context
import com.edgellm.core.network.NetworkClient
import com.edgellm.core.network.OkHttpNetworkClient
import com.edgellm.data.local.PreferencesManager
import com.edgellm.data.remote.CloudApiClient
import com.edgellm.data.remote.HuggingFaceApi
import com.edgellm.data.repository.CloudRepositoryImpl
import com.edgellm.data.repository.DownloadRepositoryImpl
import com.edgellm.data.repository.ModelRepositoryImpl
import com.edgellm.domain.repository.CloudRepository
import com.edgellm.domain.repository.DownloadRepository
import com.edgellm.domain.repository.ModelRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // Longer for streaming
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideNetworkClient(okHttpClient: OkHttpClient): NetworkClient {
        return OkHttpNetworkClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideHuggingFaceApi(networkClient: NetworkClient): HuggingFaceApi {
        return HuggingFaceApi(networkClient)
    }

    @Provides
    @Singleton
    fun provideCloudApiClient(okHttpClient: OkHttpClient): CloudApiClient {
        return CloudApiClient(okHttpClient)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideModelRepository(
        huggingFaceApi: HuggingFaceApi,
        preferencesManager: PreferencesManager
    ): ModelRepository {
        return ModelRepositoryImpl(huggingFaceApi, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideDownloadRepository(
        @ApplicationContext context: Context,
        networkClient: NetworkClient
    ): DownloadRepository {
        return DownloadRepositoryImpl(context, networkClient)
    }

    @Provides
    @Singleton
    fun provideCloudRepository(
        cloudApiClient: CloudApiClient,
        preferencesManager: PreferencesManager
    ): CloudRepository {
        return CloudRepositoryImpl(cloudApiClient, preferencesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindCloudRepository(impl: CloudRepositoryImpl): CloudRepository
}