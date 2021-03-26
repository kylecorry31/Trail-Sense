package com.kylecorry.trail_sense.shared.services

import android.app.Notification
import android.content.Intent

abstract class ForegroundService: CustomService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(foregroundNotificationId, getForegroundNotification())
        return onServiceStarted(intent, flags, startId)
    }

    fun stopService(removeNotification: Boolean = true){
        stopForeground(removeNotification)
        stopSelf()
    }

    abstract fun onServiceStarted(intent: Intent?, flags: Int, startId: Int): Int
    abstract fun getForegroundNotification(): Notification
    abstract val foregroundNotificationId: Int
}