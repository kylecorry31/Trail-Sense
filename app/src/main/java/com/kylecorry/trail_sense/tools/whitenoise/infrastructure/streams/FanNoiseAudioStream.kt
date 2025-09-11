package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.AudioStream
import com.kylecorry.andromeda.sound.stream.SineWaveAudioStream
import com.kylecorry.andromeda.sound.stream.SumAudioStream

class FanNoiseAudioStream(seed: Int = 0) : AudioStream {
    private val brownNoise = BrownNoiseAudioStream(seed)
    private val hum = SineWaveAudioStream(100.0, numHarmonics = 2)
    private val combined = SumAudioStream(
        0.85f to brownNoise,
        0.15f to hum
    )

    override suspend fun next(sampleRate: Int): Float {
        return combined.next(sampleRate)
    }

    override suspend fun reset() {
        return combined.reset()
    }
}