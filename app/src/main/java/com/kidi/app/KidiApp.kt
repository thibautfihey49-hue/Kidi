package com.kidi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class KidiApp : Application() {
    companion object {
        const val CHANNEL_ID = "kidi_service"
        const val NOTIFICATION_ID = 1001
        lateinit var instance: KidiApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ChronoFamille Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Surveillance du temps d'écran"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
