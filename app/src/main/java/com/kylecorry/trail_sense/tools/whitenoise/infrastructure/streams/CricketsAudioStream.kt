package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.AudioStream
import com.kylecorry.andromeda.sound.stream.PrecomputedAudioStream
import com.kylecorry.andromeda.sound.stream.SineWaveAudioStream
import com.kylecorry.andromeda.sound.stream.SumAudioStream
import com.kylecorry.sol.math.SolMath
import kotlin.math.PI

class CricketsAudioStream(private val includeNearbyCricket: Boolean = true) : AudioStream {

    private val brownNoise = BrownNoiseAudioStream()
    private val whiteNoise1 =
        PrecomputedAudioStream(
            BandedNoiseAudioStream(1800.0, 2500.0, numOscillators = 50),
            totalChirpDuration,
            0.05
        )
    private val whiteNoise2 =
        PrecomputedAudioStream(
            BandedNoiseAudioStream(3800.0, 5800.0, numOscillators = 50),
            totalChirpDuration,
            0.05
        )
    private val cricketChirpSound = PrecomputedAudioStream(
        BandedNoiseAudioStream(
            4200.0,
            5000.0,
            numOscillators = 10,
            evenDistribution = true
        ) { frequency, _ ->
            val normFrequency = SolMath.norm(frequency, 4200.0, 5000.0)
            BandedNoiseAudioStream.Oscillator(
                normFrequency,
                frequency,
                (1 - normFrequency) * 2 * PI
            )
        }, impulseDuration,
        0.0
    )
    private val chirpFade = SineWaveAudioStream(0.5)

    private val backgroundNoise = SumAudioStream(
        0.7f to whiteNoise1,
        0.29f to whiteNoise2,
        0.01f to brownNoise
    )

    private var t = 0.0

    private suspend fun getChirp(
        t: Double,
        sampleRate: Int,
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
        var signal = cricketChirpSound.peek(t, sampleRate).toDouble()
        // Fade
        signal *= chirpFade.peek(newT / impulseDuration, sampleRate).toDouble()
        return signal
    }

    override suspend fun next(sampleRate: Int): Float {
        var amplitude = 0.0
        if (includeNearbyCricket) {
            for (cricket in crickets) {
                amplitude += cricket.volume * getChirp(
                    t,
                    sampleRate,
                    cricket.startOffset,
                    cricket.endOffset
                )

            }
        }
        amplitude += if (includeNearbyCricket) {
            backgroundVolume
        } else {
            1.0
        } * backgroundNoise.next(sampleRate)
        t += 1 / sampleRate.toDouble()
        return amplitude.toFloat()
    }

    override suspend fun reset() {
        backgroundNoise.reset()
        cricketChirpSound.reset()
        chirpFade.reset()
        t = 0.0
    }

    private class Cricket(val volume: Double, val startOffset: Double) {
        val endOffset = timeBetweenChirps - startOffset
    }

    companion object {
        private val impulseDuration = 0.04
        private val impulseGapDuration = 0.01
        private val numPulses = 3
        private val backgroundVolume = 0.8
        private val timeBetweenChirps = 0.4

        val totalChirpDuration =
            numPulses * (impulseDuration + impulseGapDuration) + timeBetweenChirps

        private val crickets = listOf<Cricket>(
            Cricket(0.2, 0.0)
        )
    }
}