package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda

import kotlin.math.cos
import kotlin.math.sin

class PrecomputedAudioStream(
    private val source: AudioStream,
    private val seconds: Double = 1.0,
    private val blendDuration: Double = seconds * 0.2
) : SeekableAudioStream {

    private val buffer = mutableListOf<Float>()
    private var i = 0
    private var totalActualSamples = 0

    override suspend fun next(sampleRate: Int): Float {
        populateBuffer(sampleRate)
        val sample = buffer[i]
        i = (i + 1) % totalActualSamples
        return sample
    }

    override suspend fun reset() {
        i = 0
    }

    override suspend fun peek(time: Double, sampleRate: Int): Float {
        populateBuffer(sampleRate)
        val index = ((time % seconds) / seconds * totalActualSamples).toInt() % totalActualSamples
        return buffer[index]
    }

    private suspend fun populateBuffer(sampleRate: Int) {
        if (buffer.isEmpty()) {
            totalActualSamples = (seconds * sampleRate).toInt()
            val totalSamples = ((seconds + blendDuration) * sampleRate).toInt()
            val blendSamples = (blendDuration * sampleRate).toInt()
            source.reset()
            for (i in 0 until totalSamples) {
                buffer.add(source.next(sampleRate))
            }

            for (i in 0 until blendSamples) {
                val norm = i / blendSamples.toFloat()
                val startVal = buffer[i]
                val endVal = buffer[totalSamples - blendSamples + i]
                val a = cos(norm * Math.PI / 2)
                val b = sin(norm * Math.PI / 2)
                buffer[i] = (a * endVal + b * startVal).toFloat()
            }

        }
    }
}