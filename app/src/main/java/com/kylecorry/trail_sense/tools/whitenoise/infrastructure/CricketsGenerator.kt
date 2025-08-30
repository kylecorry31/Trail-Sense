package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator
import com.kylecorry.sol.math.SolMath.power
import kotlin.math.sin

// Inspired by https://sound.stackexchange.com/questions/5901/how-do-you-design-a-cricket-sound

class CricketsGenerator {
    private val soundGenerator = SoundGenerator()

    private fun getFrequencyWaveform(
        t: Double,
        frequency: Double,
        warble: Double = 0.0,
        numHarmonics: Int = 0
    ): Double {
        val factor = 2 * Math.PI * t
        var signal = 0.0
        var spreadFactor = 0.0
        if (warble > 0) {
            spreadFactor = sin(factor * warble)
        }
        for (i in 0..numHarmonics) {
            val multiplier = i + 1
            val divisor = power(2, i)
            signal += sin(factor * frequency * multiplier + spreadFactor) / divisor
        }
        return signal
    }

    private fun getBackgroundNoise(t: Double): Double {
        return getFrequencyWaveform(t, 4800.0, 100.0) + 0.5 * getFrequencyWaveform(
            t, 6500.0, 80.0
        ) * getFrequencyWaveform(t, 15.0)
    }

    private fun getChirp(
        t: Double,
        startGapDuration: Double = 0.0,
        endGapDuration: Double = 0.5,
        pitch: Double = 4850.0
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
        signal *= sin(Math.PI.toFloat() * newT / impulseDuration.toFloat())
        return signal
    }


    fun getNoise(sampleRate: Int = 44100, durationSeconds: Float = 1f): AudioTrack {
        return soundGenerator.getSound(sampleRate, durationSeconds) {
            val t = it / sampleRate.toDouble()
            var amplitude = 0.0
            for (cricket in crickets) {
                amplitude += cricket.volume * getChirp(
                    t,
                    cricket.startOffset,
                    cricket.endOffset
                )

            }
            amplitude += backgroundVolume * getBackgroundNoise(t)
            amplitude
        }
    }

    private class Cricket(val volume: Double, val startOffset: Double){
        val endOffset = timeBetweenChirps - startOffset
    }

    companion object {
        private val impulseDuration = 0.02
        private val impulseGapDuration = 0.005
        private val numPulses = 4
        private val backgroundVolume = 0.04
        private val timeBetweenChirps = 0.6

        val totalChirpDuration = numPulses * (impulseDuration + impulseGapDuration) + timeBetweenChirps

        private val crickets = listOf(
            Cricket(0.03, 0.1),
            Cricket(0.01, 0.0),
            Cricket(0.01, 0.4),
            Cricket(0.008, 0.7),
            Cricket(0.004, 0.2),
        )

    }

}