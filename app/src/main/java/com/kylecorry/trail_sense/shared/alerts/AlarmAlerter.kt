package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.andromeda.sound.SystemSoundPlayer
import java.time.Duration

class AlarmAlerter(context: Context) : IAlerter {

    private val systemPlayer = SystemSoundPlayer(context)

    override fun alert() {
        val player = systemPlayer.player(
            systemPlayer.getNotificationUri(),
            SystemSoundPlayer.AudioChannel.Alarm
        )

        player.isLooping = false

        val timer = CoroutineTimer {
            player.stop()
        }
        timer.once(Duration.ofSeconds(1))

        player.start()
    }

}