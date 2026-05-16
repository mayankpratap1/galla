package com.edgellm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EdgeLLMApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: EdgeLLMApplication
            private set
    }
}