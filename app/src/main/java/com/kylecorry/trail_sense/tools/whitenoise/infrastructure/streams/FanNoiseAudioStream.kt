package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda.AudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda.SineWaveAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda.SumAudioStream

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