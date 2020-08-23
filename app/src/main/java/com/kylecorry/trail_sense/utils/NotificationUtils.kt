package com.kylecorry.trail_sense.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationUtils {

    val CHANNEL_IMPORTANCE_HIGH =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_HIGH else 4
    val CHANNEL_IMPORTANCE_DEFAULT =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_DEFAULT else 3
    val CHANNEL_IMPORTANCE_LOW =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_LOW else 2

    fun isNotificationActive(context: Context, notificationId: Int): Boolean {
        val notificationManager = getNotificationManager(context)
        return notificationManager?.activeNotifications?.any { it.id == notificationId } ?: false
    }

    fun send(context: Context, notificationId: Int, notification: Notification) {
        val notificationManager = getNotificationManager(context)
        notificationManager?.notify(notificationId, notification)
    }

    fun cancel(context: Context, notificationId: Int) {
        val notificationManager = getNotificationManager(context)
        notificationManager?.cancel(notificationId)
    }

    fun createChannel(
        context: Context,
        id: String,
        name: String,
        description: String,
        importance: Int
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(id, name, importance).apply {
            this.description = description
        }
        getNotificationManager(context)?.createNotificationChannel(channel)
    }

    private fun getNotificationManager(context: Context): NotificationManager? {
        return context.getSystemService()
    }

}