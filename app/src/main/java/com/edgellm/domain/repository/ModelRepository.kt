package com.edgellm.domain.repository

import com.edgellm.core.error.Failure
import com.edgellm.core.error.Resource
import com.edgellm.domain.model.HFModel
import com.edgellm.domain.model.InstalledModel
import com.edgellm.domain.model.ModelFilter
import kotlinx.coroutines.flow.Flow

interface ModelRepository {

    suspend fun searchModels(filter: ModelFilter): Resource<List<HFModel>>

    suspend fun getTrendingModels(limit: Int): Resource<List<HFModel>>

    suspend fun getGGUFModels(limit: Int, offset: Int): Resource<List<HFModel>>

    suspend fun getLiteRTModels(limit: Int, offset: Int): Resource<List<HFModel>>

    suspend fun getModelDetails(modelId: String): Resource<HFModel>

    fun getInstalledModels(): List<InstalledModel>

    suspend fun deleteModel(modelId: String): Resource<Unit>

    fun observeInstalledModels(): Flow<List<InstalledModel>>
}