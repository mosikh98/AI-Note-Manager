package com.ainote.manager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AiNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "cloud_sync",
                "Cloud backup",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows progress while notes are uploaded" }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
