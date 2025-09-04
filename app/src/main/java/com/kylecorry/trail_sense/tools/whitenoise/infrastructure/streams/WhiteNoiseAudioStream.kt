package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.trail_sense.shared.andromeda_temp.nextGaussian
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.andromeda.AudioStream
import kotlin.random.Random

class WhiteNoiseAudioStream(private val seed: Int = 0) : AudioStream {
    private var random = Random(seed)

    override suspend fun next(sampleRate: Int): Float {
        return random.nextGaussian().toFloat()
    }

    override suspend fun reset() {
        random = Random(seed)
    }
}