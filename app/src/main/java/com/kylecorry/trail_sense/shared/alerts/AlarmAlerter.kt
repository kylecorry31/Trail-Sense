package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.core.content.getSystemService
import com.kylecorry.andromeda.core.time.CoroutineTimer
import com.kylecorry.trail_sense.R
import java.time.Duration

class AlarmAlerter(private val context: Context) : IAlerter {

    override fun alert() {
        val player = player()

        player.isLooping = false

        val timer = CoroutineTimer {
            player.stop()
        }
        timer.once(Duration.ofSeconds(1))

        player.start()
    }

    // TODO: Extract to Andromeda
    fun player(): MediaPlayer {
        return MediaPlayer.create(
            context,
            R.raw.alarm,
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            context.getSystemService<AudioManager>()?.generateAudioSessionId() ?: 0
        )
    }

}