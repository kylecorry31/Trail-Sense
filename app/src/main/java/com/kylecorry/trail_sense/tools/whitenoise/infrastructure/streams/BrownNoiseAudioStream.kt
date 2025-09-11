package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.AudioStream

class BrownNoiseAudioStream(seed: Int = 0) : AudioStream {
    private val whiteNoise = WhiteNoiseAudioStream(seed)
    private var brown = 0.0f

    override suspend fun next(sampleRate: Int): Float {
        val white = whiteNoise.next(sampleRate)
        brown += white * 0.02f
        // Leaky integration
        brown /= 1.02f
        return brown
    }

    override suspend fun reset() {
        whiteNoise.reset()
        brown = 0.0f
    }
}