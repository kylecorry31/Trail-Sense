package com.kylecorry.trail_sense.shared.alerts

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sound.SystemSoundPlayer
import java.time.Duration

class AlarmAlerter(private val context: Context, private val notificationChannel: String? = null) :
    IAlerter {

    private val systemPlayer = SystemSoundPlayer(context)

    override fun alert() {
        val player = systemPlayer.player(
            getSoundUriForNotificationChannel() ?: systemPlayer.getNotificationUri(),
            SystemSoundPlayer.AudioChannel.Alarm
        )

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