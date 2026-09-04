package com.kidi.app

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class MonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        startForeground(KidiApp.NOTIFICATION_ID, createNotification())
        startMonitoring()
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, KidiApp.CHANNEL_ID)
            .setContentTitle("ChronoFamille")
            .setContentText("Surveillance active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()
    }
    
    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                checkSchoolMode()
                checkLockExpiry()
                delay(30000)
            }
        }
    }
    
    private fun checkSchoolMode() {
        val ctx = this
        val schedule = LocalDataStore.getSchoolSchedule(ctx)
        if (schedule.enabled && LocalDataStore.isSchoolTime(ctx)) {
            if (!LocalDataStore.isLocked(ctx)) {
                LocalDataStore.lockForMinutes(ctx, 240)
                sendBroadcast(Intent("com.kidi.app.UPDATE_UI"))
            }
        }
    }
    
    private fun checkLockExpiry() {
        val ctx = this
        if (LocalDataStore.isLocked(ctx)) {
            val remaining = LocalDataStore.getRemainingMinutes(ctx)
            if (remaining <= 0) {
                LocalDataStore.unlock(ctx)
                sendBroadcast(Intent("com.kidi.app.UPDATE_UI"))
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    companion object {
        fun start(ctx: Context) {
            val intent = Intent(ctx, MonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
        
        fun stop(ctx: Context) {
            val intent = Intent(ctx, MonitorService::class.java)
            ctx.stopService(intent)
        }
    }
}
