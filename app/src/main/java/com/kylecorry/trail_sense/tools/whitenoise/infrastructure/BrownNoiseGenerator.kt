package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator

class BrownNoiseGenerator {

    private val soundGenerator = SoundGenerator()

    fun getNoise(sampleRate: Int = 44100, durationSeconds: Double = 1.0): AudioTrack {
        val noise = SoundGenerators.brownNoise(sampleRate)

        return soundGenerator.getSound(sampleRate, durationSeconds.toFloat()) {
            val t = it / sampleRate.toDouble()
            noise(t)
        }
    }

}