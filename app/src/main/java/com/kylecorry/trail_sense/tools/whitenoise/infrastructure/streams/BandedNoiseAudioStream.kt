package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.SeekableAudioStream
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class BandedNoiseAudioStream(
    private val lowFreq: Double,
    private val highFreq: Double,
    private val numOscillators: Int = 100,
    seed: Int = 0,
    private val evenDistribution: Boolean = false,
    private val createOscillator: (frequency: Double, phase: Double) -> Oscillator = { frequency, phase ->
        Oscillator(
            1.0,
            frequency,
            phase
        )
    }
) : SeekableAudioStream {

    private val random = Random(seed)
    private val oscillators = List(numOscillators) {
        val frequency = if (evenDistribution) {
            lowFreq * (highFreq / lowFreq).pow(it / (numOscillators - 1).toDouble())
        } else {
            lowFreq + random.nextDouble() * (highFreq - lowFreq)
        }
        val phase = random.nextDouble() * 2 * PI
        createOscillator(frequency, phase)
    }
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
        var amplitude = 0.0
        var totalAmplitude = 0.0
        for (osc in oscillators) {
            amplitude += osc.amplitude * sin(2 * PI * osc.frequency * time + osc.phase)
            totalAmplitude += osc.amplitude
        }
        return (amplitude / totalAmplitude).toFloat()
    }

    class Oscillator(val amplitude: Double, val frequency: Double, val phase: Double)
}