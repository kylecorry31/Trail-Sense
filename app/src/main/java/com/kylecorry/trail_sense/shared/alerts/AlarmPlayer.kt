package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.sound.SystemSoundPlayer
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.time.Duration

internal class AlarmPlayer(
    private val context: Context,
    private val notificationChannel: String,
    private val onComplete: () -> Unit = {}
) {
    private val systemPlayer = SystemSoundPlayer(context)
    private val files = DependencyRegistry.get<FileSubsystem>()

    fun play() {
        val player = getPlayer()
        if (player == null) {
            onComplete()
            return
        }
        val haptics = HapticSubsystem.getInstance(context)
        val duration = player.duration
        val timer = CoroutineTimer {
            try {
                player.stop()
            } finally {
                onComplete()
            }
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

    private fun getPlayer(): MediaPlayer? {
        val options = listOf(
            Pair(getUri(), SystemSoundPlayer.AudioChannel.Alarm),
            Pair(systemPlayer.getNotificationUri(), SystemSoundPlayer.AudioChannel.Alarm),
            Pair(getUri(), SystemSoundPlayer.AudioChannel.Notification),
            Pair(systemPlayer.getNotificationUri(), SystemSoundPlayer.AudioChannel.Notification),
        )

        for (option in options) {
            val player = tryGetPlayer(option.first ?: continue, option.second)
            if (player != null) {
                return player
            }
        }
        return null
    }

    private fun tryGetPlayer(uri: Uri, channel: SystemSoundPlayer.AudioChannel): MediaPlayer? {
        try {
            return systemPlayer.player(uri, channel)
        } catch (_: Exception) {
            // Do nothing
        }
        return null
    }

    private fun getUri(): Uri? {
        val channelUri = Notify.getSoundUri(context, notificationChannel)
        if (channelUri != null && files.canRead(channelUri)) {
            return channelUri
        }
        return systemPlayer.getNotificationUri()
    }
}
