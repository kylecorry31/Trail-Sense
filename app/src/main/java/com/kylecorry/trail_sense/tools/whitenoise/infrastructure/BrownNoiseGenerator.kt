package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import kotlin.random.Random

class BrownNoiseGenerator {

    private val soundGenerator = SoundGenerator()

    // Uses a version of Voss-McCartney algorithm
    fun getNoise(sampleRate: Int = 44100, durationSeconds: Double = 1.0): AudioTrack {
        val random = Random(0)

        val noise = mutableListOf<Double>()
        val size = ((durationSeconds + 1) * sampleRate).toInt()
        var brown = 0.0
        for (i in 0 until size) {
            val white = random.nextDouble(-1.0, 1.0)
            brown += white / 10.0
            brown = brown.coerceIn(-1.0, 1.0)
            noise.add(brown)
        }

        val precomputed = SoundGenerators.precomputed(
            noise,
            durationSeconds + 1.0
        )

        val blended = SoundGenerators.loopBlended(
            0.02,
            durationSeconds,
            precomputed
        )

        return soundGenerator.getSound(sampleRate, durationSeconds.toFloat()) {
            val t = it / sampleRate.toDouble()
            blended(t)
        }
    }

}