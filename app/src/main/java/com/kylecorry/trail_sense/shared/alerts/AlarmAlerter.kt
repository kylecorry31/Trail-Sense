package com.kylecorry.trail_sense.shared.alerts

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sound.SystemSoundPlayer
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import java.time.Duration

class AlarmAlerter(
    private val context: Context,
    private val isEnabled: Boolean,
    private val notificationChannel: String? = null
) :
    IAlerter {

    private val systemPlayer = SystemSoundPlayer(context)

    override fun alert() {
        if (!isEnabled) {
            return
        }

        val player = systemPlayer.player(
            getSoundUriForNotificationChannel() ?: systemPlayer.getNotificationUri(),
            SystemSoundPlayer.AudioChannel.Alarm
        )
        val haptics = HapticSubsystem.getInstance(context)

        val duration = player.duration

        val timer = CoroutineTimer {
            player.stop()
        }

        val end = if (duration == -1) {
            Duration.ofSeconds(5)
        } else {
            Duration.ofMillis(duration.toLong()).coerceAtMost(Duration.ofSeconds(5))
        }

        timer.once(end)

        player.start()
        haptics.alert()
        player.isLooping = false
    }

    private fun getSoundUriForNotificationChannel(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null
        }

        val channelId = notificationChannel ?: return null

        val notificationManager = context.getSystemService<NotificationManager>()
        val channel = notificationManager?.getNotificationChannel(channelId)
        return channel?.sound
    }
}