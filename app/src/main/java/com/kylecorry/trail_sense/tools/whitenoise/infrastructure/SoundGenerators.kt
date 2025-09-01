package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.power
import com.kylecorry.trail_sense.shared.extensions.range
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

object SoundGenerators {

    fun precomputed(
        amplitudes: List<Double>,
        totalDuration: Double,
        normalize: Boolean = true
    ): (t: Double) -> Double {
        val amps = if (normalize){
            val range = amplitudes.range() ?: return { 0.0 }
            amplitudes.map {
                SolMath.map(it, range.start, range.end, -1.0, 1.0)
            }
        } else {
            amplitudes
        }
        val n = amps.size
        return { t ->
            val index = ((t / totalDuration) * n).toInt()
            if (index < 0 || index >= n) {
                0.0
            } else {
                amps[index]
            }
        }
    }

    fun normalized(
        minimum: Double,
        maximum: Double,
        producer: (Double) -> Double
    ): (Double) -> Double {
        return { t ->
            SolMath.map(producer(t), minimum, maximum, -1.0, 1.0)
        }
    }

    fun bandedNoise(
        lowFreq: Double,
        highFreq: Double,
        numOscillators: Int = 100,
        seed: Int = 0,
        evenDistribution: Boolean = false,
        amplitude: (frequency: Double) -> Double = { 1.0 }
    ): (Double) -> Double {
        val random = Random(seed)
        val oscillators = List(numOscillators) {
            val frequency = if (evenDistribution) {
                lowFreq * (highFreq / lowFreq).pow(it / (numOscillators - 1).toDouble())
            } else {
                lowFreq + random.nextDouble() * (highFreq - lowFreq)
            }
            val phase = random.nextDouble() * 2 * PI
            Oscillator(amplitude(frequency), frequency, phase)
        }

        return { t ->
            var amplitude = 0.0
            var totalAmplitude = 0.0
            for (osc in oscillators) {
                amplitude += osc.amplitude * sin(2 * PI * osc.frequency * t + osc.phase)
                totalAmplitude += osc.amplitude
            }
            amplitude / totalAmplitude
        }
    }

    fun fractalNoise(
        lowFreq: Double,
        highFreq: Double,
        alpha: Double = 1.0,
        numOscillators: Int = 500,
        seed: Int = 0,
        evenDistribution: Boolean = false,
    ): (Double) -> Double {
        return bandedNoise(lowFreq, highFreq, numOscillators, seed, evenDistribution) {
            1 / it.pow(
                alpha
            )
        }
    }

    fun loopBlended(
        blendDuration: Double,
        loopDuration: Double,
        producer: (t: Double) -> Double
    ): (Double) -> Double {
        return { t ->
            var amplitude = producer(t)
            if (t <= blendDuration) {
                val norm = t / blendDuration
                amplitude *= norm
                amplitude += (1 - norm) * producer(loopDuration + t)
            }
            amplitude
        }
    }

    fun sineWave(
        frequency: Double,
        phase: Double = 0.0,
        numHarmonics: Int = 0
    ): (Double) -> Double {
        return { t ->
            val factor = 2 * PI * t
            var signal = 0.0
            var totalAmplitude = 0.0
            for (i in 0..numHarmonics) {
                val multiplier = i + 1
                val divisor = power(2, i)
                signal += sin(factor * frequency * multiplier + phase) / divisor
                totalAmplitude += 1 / divisor.toDouble()
            }
            signal / totalAmplitude
        }
    }

    private class Oscillator(val amplitude: Double, val frequency: Double, val phase: Double)
}