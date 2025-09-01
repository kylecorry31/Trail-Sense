package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.sol.math.SolMath

class OceanWavesGenerator {

    private val soundGenerator = SoundGenerator()

    fun getNoise(sampleRate: Int = 44100, durationSeconds: Double = 1.0): AudioTrack {
        val noise = SoundGenerators.brownNoise(sampleRate)
        val wave = SoundGenerators.sineWave(waveFrequency)

        return soundGenerator.getSound(sampleRate, durationSeconds.toFloat()) {
            val t = it / sampleRate.toDouble()
            val n = noise(t)
            val n2 = noise(t + 1)
            0.95 * n * SolMath.power(wave(t), 3) + 0.05 * n2
        }
    }

    companion object {
        private const val waveFrequency = 0.05
        const val totalDuration = 1 / waveFrequency
    }

}