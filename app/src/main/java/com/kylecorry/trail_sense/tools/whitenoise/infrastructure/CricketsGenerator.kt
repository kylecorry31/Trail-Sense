package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.media.AudioTrack
import com.kylecorry.andromeda.sound.SoundGenerator

class CricketsGenerator(private val includeNearbyCricket: Boolean = true) {
    private val soundGenerator = SoundGenerator()

    private val whiteNoise1 = SoundGenerators.loopBlended(
        0.05,
        totalChirpDuration,
        SoundGenerators.bandedNoise(1800.0, 2500.0)
    )
    private val whiteNoise2 = SoundGenerators.loopBlended(
        0.05,
        totalChirpDuration,
        SoundGenerators.bandedNoise(3600.0, 5000.0)
    )

    private val cricketChirpSound = SoundGenerators.sineWave(
        4500.0,
        numHarmonics = 2
    )

    private val chirpFade = SoundGenerators.sineWave(0.5)

    private fun getBackgroundNoise(t: Double): Double {
        return 0.6 * whiteNoise1(t) + 0.4 * whiteNoise2(t)
    }

    private fun getChirp(
        t: Double,
        startGapDuration: Double = 0.0,
        endGapDuration: Double = 0.5
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
        var signal = cricketChirpSound(t)
        // Fade
        signal *= chirpFade(newT / impulseDuration)
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
