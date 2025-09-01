package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.trail_sense.shared.andromeda_temp.nextGaussian
import kotlin.math.abs
import kotlin.random.Random

class BrownNoiseGenerator {

    private val soundGenerator = SoundGenerator()

    private val blendDuration = 0.01

    fun getNoise(sampleRate: Int = 44100, durationSeconds: Double = 1.0): AudioTrack {
        val random = Random(0)

        val noise = mutableListOf<Double>()
        val size = ((durationSeconds + blendDuration) * sampleRate).toInt()
        var brown = 0.0
        for (i in 0 until size) {
            var white = random.nextGaussian()
            if ((brown + white / 10) >= 1.0) {
                white = -abs(white)
            } else if ((brown + white / 10) <= -1.0) {
                white = abs(white)
            }

            brown += white / 10.0

            noise.add(brown)
        }

        val precomputed = SoundGenerators.precomputed(
            noise,
            durationSeconds + blendDuration
        )

        val blended = SoundGenerators.loopBlended(
            blendDuration,
            durationSeconds,
            precomputed
        )

        return soundGenerator.getSound(sampleRate, durationSeconds.toFloat()) {
            val t = it / sampleRate.toDouble()
            blended(t)
        }
    }

}