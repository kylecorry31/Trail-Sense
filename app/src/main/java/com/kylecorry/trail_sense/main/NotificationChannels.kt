package com.kylecorry.trail_sense.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object NotificationChannels {

    // Legacy (intended for deletion)
    private const val CHANNEL_BACKGROUND_UPDATES = "background_updates"
    private const val CHANNEL_BACKGROUND_LAUNCHER = "background_launcher"

    fun createChannels(context: Context) {
        val tools = Tools.getTools(context)
        val channels = tools.flatMap { it.notificationChannels }

        channels.forEach {
            if (!it.isAlarm) {
                Notify.createChannel(
                    context,
                    it.id,
                    it.name,
                    it.description,
                    it.importance,
                    muteSound = it.muteSound,
                    showBadge = it.showBadge
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(it.id, it.name, it.importance).apply {
                    description = it.description
                    setShowBadge(it.showBadge)
                    setSound(
                        Settings.System.DEFAULT_NOTIFICATION_URI,
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_ALARM)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    enableVibration(true)
                }
                context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
            }
        }

        // CHANNEL CLEANUP SECTION
        Notify.deleteChannel(context, CHANNEL_BACKGROUND_UPDATES)
        Notify.deleteChannel(context, CHANNEL_BACKGROUND_LAUNCHER)
    }

}
