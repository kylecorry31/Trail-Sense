package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda

import com.kylecorry.sol.math.SolMath.power
import kotlin.math.PI
import kotlin.math.sin

class SineWaveAudioStream(
    private val frequency: Double,
    private val phase: Double = 0.0,
    private val numHarmonics: Int = 0
) : SeekableAudioStream {

    private var t = 0.0

    override suspend fun next(sampleRate: Int): Float {
        val amplitude = peek(t, sampleRate)
        t += 1 / sampleRate.toDouble()
        return amplitude
    }

    override suspend fun reset() {
        t = 0.0
    }

    override suspend fun peek(time: Double, sampleRate: Int): Float {
        val factor = 2 * PI * time
        var signal = 0.0
        var totalAmplitude = 0.0
        for (i in 0..numHarmonics) {
            val multiplier = i + 1
            val divisor = power(2, i)
            signal += sin(factor * frequency * multiplier + phase) / divisor
            totalAmplitude += 1 / divisor.toDouble()
        }
        return (signal / totalAmplitude).toFloat()
    }

}
