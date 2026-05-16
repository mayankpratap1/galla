package com.edgellm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FIX: Register notification channel at app start — if service launches
        // before channel is created the notification silently fails on Android 8+
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(
                "edgellm_channel",
                "EdgeLLM Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "On-device AI inference and API server" }
        )
    }
}
