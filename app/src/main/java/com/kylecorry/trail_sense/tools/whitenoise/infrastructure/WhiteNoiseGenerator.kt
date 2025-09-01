package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.trail_sense.shared.andromeda_temp.nextGaussian
import kotlin.random.Random

class WhiteNoiseGenerator {

    private val soundGenerator = SoundGenerator()

    // Uses a version of Voss-McCartney algorithm
    fun getNoise(sampleRate: Int = 44100, durationSeconds: Double = 1.0): AudioTrack {
        val random = Random(0)
        return soundGenerator.getSound(sampleRate, durationSeconds.toFloat()) {
            random.nextGaussian()
        }
    }

}