package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.trail_sense.shared.andromeda_temp.nextGaussian
import kotlin.random.Random

class PinkNoiseGenerator {

    private val soundGenerator = SoundGenerator()
    private val blendDuration = 0.01

    // Uses a version of Voss-McCartney algorithm
    fun getNoise(sampleRate: Int = 44100, durationSeconds: Double = 1.0): AudioTrack {
        val rows = DoubleArray(16)
        var runningSum = 0.0
        var counter = 0L
        val random = Random(0)

        val noise = mutableListOf<Double>()
        val size = ((durationSeconds + blendDuration) * sampleRate).toInt()
        for (i in 0 until size) {
            counter++
            var lastBit = counter
            for (j in rows.indices) {
                if (lastBit and 1L != 0L) {
                    var newValue = random.nextGaussian()
                    newValue = newValue.coerceIn(-1.0, 1.0)
                    runningSum += newValue - rows[j]
                    rows[j] = newValue
                }
                lastBit = lastBit shr 1
            }
            val pink = runningSum / rows.size.toDouble()
            noise.add(pink)
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