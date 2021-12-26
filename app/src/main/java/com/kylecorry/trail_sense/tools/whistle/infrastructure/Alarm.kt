package com.kylecorry.trail_sense.tools.whistle.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.andromeda.sound.SoundPlayer
import kotlin.math.absoluteValue
import kotlin.math.sin

class Alarm(baseFrequency: Int = 3150, alarmFrequency: Int = 10) :
    SoundPlayer(getTone(baseFrequency, alarmFrequency)) {


    companion object {
        private fun getTone(
            baseFrequency: Int,
            alarmFrequency: Int,
            sampleRate: Int = 64000,
            durationSeconds: Int = 1
        ): AudioTrack {
            val soundGenerator = SoundGenerator()
            return soundGenerator.getSound(sampleRate, durationSeconds) {
                sin(baseFrequency * 2 * Math.PI * it / sampleRate) * sin(alarmFrequency * Math.PI * it / sampleRate).absoluteValue
            }
        }
    }
}