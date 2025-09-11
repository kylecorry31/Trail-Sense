package com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams

import com.kylecorry.andromeda.sound.stream.AudioStream
import com.kylecorry.andromeda.sound.stream.SineWaveAudioStream
import com.kylecorry.sol.math.SolMath

class OceanWavesAudioStream : AudioStream {

    private val brownNoise1 = BrownNoiseAudioStream()
    private val brownNoise2 = BrownNoiseAudioStream(1)
    private val wave = SineWaveAudioStream(waveFrequency)

    override suspend fun next(sampleRate: Int): Float {
        val noise1 = brownNoise1.next(sampleRate)
        val noise2 = brownNoise2.next(sampleRate)
        return 0.95f * noise1 * SolMath.power(wave.next(sampleRate), 3) + 0.05f * noise2
    }

    override suspend fun reset() {
        brownNoise1.reset()
        brownNoise2.reset()
        wave.reset()
    }

    companion object {
        private const val waveFrequency = 0.05
    }

}