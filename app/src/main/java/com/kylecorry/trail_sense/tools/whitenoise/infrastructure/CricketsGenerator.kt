package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.sol.math.SolMath.power
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class CricketsGenerator(private val includeNearbyCricket: Boolean = true) {
    private val soundGenerator = SoundGenerator()
    private val random = Random(100)

    private val whiteNoise1 = createLoopBlendedNoiseGenerator(
        0.05,
        totalChirpDuration,
        createBandedWhiteNoiseGenerator(1800.0, 2500.0)
    )
    private val whiteNoise2 = createLoopBlendedNoiseGenerator(
        0.05,
        totalChirpDuration,
        createBandedWhiteNoiseGenerator(3600.0, 5000.0)
    )

    private fun createLoopBlendedNoiseGenerator(
        blendDuration: Double,
        loopDuration: Double,
        producer: (t: Double) -> Double
    ): (Double) -> Double {
        return { t ->
            var amplitude = producer(t)
            if (t < blendDuration) {
                val norm = t / blendDuration
                amplitude *= norm
                amplitude += (1 - norm) * producer(loopDuration + t)
            }
            amplitude
        }
    }

    private fun createBandedWhiteNoiseGenerator(
        lowFreq: Double,
        highFreq: Double,
        numOscillators: Int = 100
    ): (Double) -> Double {
        val oscillators = List(numOscillators) {
            val freq = lowFreq + random.nextDouble() * (highFreq - lowFreq)
            val phase = random.nextDouble() * 2 * PI
            Oscillator(freq, phase)
        }

        return { t ->
            var amplitude = 0.0
            for (osc in oscillators) {
                amplitude += sin(2 * PI * osc.freq * t + osc.phase)
            }
            amplitude / oscillators.size
        }
    }

    private fun getFrequencyWaveform(
        t: Double,
        frequency: Double,
        phase: Double = 0.0,
        numHarmonics: Int = 0
    ): Double {
        val factor = 2 * PI * t
        var signal = 0.0
        var totalAmplitude = 0.0
        for (i in 0..numHarmonics) {
            val multiplier = i + 1
            val divisor = power(2, i)
            signal += sin(factor * frequency * multiplier + phase) / divisor
            totalAmplitude += 1 / divisor.toDouble()
        }
        return signal / totalAmplitude
    }

    private fun getBackgroundNoise(t: Double): Double {
        return 0.6 * whiteNoise1(t) + 0.4 * whiteNoise2(t)
    }

    private fun getChirp(
        t: Double,
        startGapDuration: Double = 0.0,
        endGapDuration: Double = 0.5,
        pitch: Double = 4500.0
    ): Double {
        // Calculate the t value for the current pulse
        var currentCycleT = t % (
                numPulses * (impulseDuration + impulseGapDuration)
                        + endGapDuration
                        + startGapDuration
                )
        if (currentCycleT < startGapDuration) {
            return 0.0
        }
        currentCycleT -= startGapDuration
        // Determine if it is in between pulses
        if (currentCycleT % (impulseDuration + impulseGapDuration) > impulseDuration) {
            return 0.0
        }
        // Determine if it is at the end
        if (currentCycleT > numPulses * (impulseDuration + impulseGapDuration)) {
            return 0.0
        }
        val newT = (currentCycleT % (impulseDuration + impulseGapDuration)).toFloat()
        var signal = getFrequencyWaveform(t, pitch, numHarmonics = 2)
        // Fade
        signal *= getFrequencyWaveform(newT / impulseDuration, 0.5)

        return signal
    }


    private fun getAmplitude(t: Double): Double {
        var amplitude = 0.0
        if (includeNearbyCricket) {
            for (cricket in crickets) {
                amplitude += cricket.volume * getChirp(
                    t,
                    cricket.startOffset,
                    cricket.endOffset
                )

            }
        }
        amplitude += if (includeNearbyCricket) {
            backgroundVolume
        } else {
            1.0
        } * getBackgroundNoise(t)
        return amplitude
    }

    fun getNoise(sampleRate: Int = 44100, durationSeconds: Float = 1f): AudioTrack {
        return soundGenerator.getSound(sampleRate, durationSeconds) {
            val t = it / sampleRate.toDouble()
            getAmplitude(t)
        }
    }

    private class Oscillator(val freq: Double, val phase: Double)

    private class Cricket(val volume: Double, val startOffset: Double) {
        val endOffset = timeBetweenChirps - startOffset
    }

    companion object {
        private val impulseDuration = 0.02
        private val impulseGapDuration = 0.005
        private val numPulses = 3
        private val backgroundVolume = 0.8
        private val timeBetweenChirps = 0.5

        val totalChirpDuration =
            numPulses * (impulseDuration + impulseGapDuration) + timeBetweenChirps

        private val crickets = listOf<Cricket>(
            Cricket(0.2, 0.0)
        )
    }
}
