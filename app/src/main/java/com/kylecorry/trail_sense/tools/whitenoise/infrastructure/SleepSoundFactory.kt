package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.andromeda.sound.stream.StreamSoundPlayer
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.BrownNoiseAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.CricketsAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.FanNoiseAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.OceanWavesAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.PinkNoiseAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.WhiteNoiseAudioStream

class SleepSoundFactory {
    fun getSleepSound(sleepSound: SleepSound): ISoundPlayer {
        val stream = when (sleepSound) {
            SleepSound.WhiteNoise -> WhiteNoiseAudioStream()
            SleepSound.PinkNoise -> PinkNoiseAudioStream()
            SleepSound.BrownNoise -> BrownNoiseAudioStream()
            SleepSound.Crickets -> CricketsAudioStream()
            SleepSound.CricketsNoChirp -> CricketsAudioStream(false)
            SleepSound.OceanWaves -> OceanWavesAudioStream()
            SleepSound.Fan -> FanNoiseAudioStream()
        }

        return StreamSoundPlayer(stream)
    }
}