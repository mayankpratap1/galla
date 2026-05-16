package com.edgellm.data.local

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edge_llm_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val KEY_ENGINE_TYPE = stringPreferencesKey("engine_type")
        private val KEY_CONTEXT_LENGTH = stringPreferencesKey("context_length")
        private val KEY_TEMPERATURE = stringPreferencesKey("temperature")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_API_KEY_OPENAI = stringPreferencesKey("api_key_openai")
        private val KEY_API_KEY_ANTHROPIC = stringPreferencesKey("api_key_anthropic")
        private val KEY_API_KEY_GEMINI = stringPreferencesKey("api_key_gemini")
    }

    suspend fun saveSelectedModel(modelId: String) {
        dataStore.edit { it[KEY_SELECTED_MODEL] = modelId }
    }

    suspend fun getSelectedModel(): String? {
        return dataStore.data.map { it[KEY_SELECTED_MODEL] }.first()
    }

    suspend fun saveEngineType(type: String) {
        dataStore.edit { it[KEY_ENGINE_TYPE] = type }
    }

    suspend fun getEngineType(): String? {
        return dataStore.data.map { it[KEY_ENGINE_TYPE] }.first()
    }

    suspend fun saveContextLength(length: Int) {
        dataStore.edit { it[KEY_CONTEXT_LENGTH] = length.toString() }
    }

    suspend fun getContextLength(): Int {
        return dataStore.data.map { it[KEY_CONTEXT_LENGTH]?.toIntOrNull() ?: 2048 }.first()
    }

    suspend fun saveTemperature(temp: Float) {
        dataStore.edit { it[KEY_TEMPERATURE] = temp.toString() }
    }

    suspend fun getTemperature(): Float {
        return dataStore.data.map { it[KEY_TEMPERATURE]?.toFloatOrNull() ?: 0.7f }.first()
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun getTheme(): String {
        return dataStore.data.map { it[KEY_THEME] ?: "system" }.first()
    }

    suspend fun saveApiKey(provider: ApiProvider, key: String) {
        val keyRef = when (provider) {
            ApiProvider.OPENAI -> KEY_API_KEY_OPENAI
            ApiProvider.ANTHROPIC -> KEY_API_KEY_ANTHROPIC
            ApiProvider.GEMINI -> KEY_API_KEY_GEMINI
        }
        dataStore.edit { it[keyRef] = key }
    }

    suspend fun getApiKey(provider: ApiProvider): String? {
        val keyRef = when (provider) {
            ApiProvider.OPENAI -> KEY_API_KEY_OPENAI
            ApiProvider.ANTHROPIC -> KEY_API_KEY_ANTHROPIC
            ApiProvider.GEMINI -> KEY_API_KEY_GEMINI
        }
        return dataStore.data.map { it[keyRef] }.first()
    }

    fun getModelsDirectory(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }
        } else {
            File(context.filesDir, "models").also { it.mkdirs() }
        }
    }

    fun getCacheDirectory(): File {
        return File(context.cacheDir, "model_cache").also { it.mkdirs() }
    }

    fun getAppVersion(): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }
}

enum class ApiProvider {
    OPENAI, ANTHROPIC, GEMINI
}