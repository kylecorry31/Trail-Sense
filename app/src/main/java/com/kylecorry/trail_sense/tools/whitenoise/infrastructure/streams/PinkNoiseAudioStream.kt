package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.AudioStream
import kotlin.random.Random

class PinkNoiseAudioStream : AudioStream {
    private val whiteNoise = WhiteNoiseAudioStream()
    private var counter = 0L
    private val rows = DoubleArray(16)
    private var runningSum = 0.0


    override suspend fun next(sampleRate: Int): Float {
        counter++
        var lastBit = counter
        for (j in rows.indices) {
            if (lastBit and 1L != 0L) {
                var newValue = whiteNoise.next(sampleRate).toDouble()
                newValue = newValue.coerceIn(-1.0, 1.0)
                runningSum += newValue - rows[j]
                rows[j] = newValue
            }
            lastBit = lastBit shr 1
        }
        val pink = runningSum / rows.size.toDouble()
        return pink.toFloat()
    }

    override suspend fun reset() {
        whiteNoise.reset()
        counter = 0L
        rows.fill(0.0)
        runningSum = 0.0
    }
}