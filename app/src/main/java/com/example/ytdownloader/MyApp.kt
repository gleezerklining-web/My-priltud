package com.example.ytdownloader

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("MyApp", "init failed", e)
        }
    }
}
