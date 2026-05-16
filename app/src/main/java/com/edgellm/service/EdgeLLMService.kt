package com.edgellm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.edgellm.MainActivity
import com.edgellm.engine.EngineConfig
import com.edgellm.engine.EngineFactory
import com.edgellm.engine.InferenceEngine
import com.edgellm.server.ApiServer
import com.edgellm.skills.SkillManager
import kotlinx.coroutines.*

class EdgeLLMService : Service() {

    companion object {
        const val CHANNEL_ID   = "edgellm_channel"
        const val NOTIF_ID     = 1001
        const val ACTION_START = "com.edgellm.START"
        const val ACTION_STOP  = "com.edgellm.STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): EdgeLLMService = this@EdgeLLMService
    }

    private val binder = LocalBinder()

    var currentEngine: InferenceEngine? = null
        private set

    private var apiServer: ApiServer? = null
    lateinit var skillManager: SkillManager
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        skillManager = SkillManager(this)
        scope.launch { skillManager.loadAll() }
        createNotificationChannel()
    }

    // FIX: onStartCommand was missing entirely — service never handled START/STOP
    // intents and never returned START_STICKY, so system didn't restart it after kill.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("EdgeLLM Pro running"))
        acquireWakeLock()
        return START_STICKY // Restart if killed by system
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        apiServer?.stop()
        currentEngine?.unload()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    suspend fun loadModel(uriString: String, config: EngineConfig = EngineConfig()): Result<Unit> {
        // Unload existing engine BEFORE creating new one to prevent OOM
        currentEngine?.unload()
        currentEngine = null

        val finalUri = if (uriString.startsWith("content://") && uriString.contains(".litertlm")) {
            copyToInternalStorage(uriString) ?: return Result.failure(Exception("Failed to copy LiteRT model to internal storage"))
        } else {
            uriString
        }

        return try {
            val newEngine = EngineFactory.create(finalUri, contentResolver, this)
            val result = newEngine.load(finalUri, config)
            if (result.isSuccess) {
                currentEngine = newEngine
                updateNotification("Model loaded: ${newEngine.modelName}")
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyToInternalStorage(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val fileName = "loaded_model.litertlm"
            val destFile = java.io.File(filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) { null }
    }

    fun startServer(port: Int = 8080) {
        val engine = currentEngine ?: return
        apiServer?.stop()
        apiServer = ApiServer(engine).also { it.start(port) }
        updateNotification("API Server running on :$port")
    }

    fun stopServer() {
        apiServer?.stop()
        apiServer = null
        updateNotification("API Server stopped")
    }

    // FIX: WakeLock prevents CPU sleeping during long inference
    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EdgeLLM::InferenceLock")
            .also { it.acquire(60 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "EdgeLLM Service", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "On-device AI inference service" }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("EdgeLLM Pro")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(
            PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE)
        )
        .setOngoing(true)
        .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, buildNotification(text))
    }
}
