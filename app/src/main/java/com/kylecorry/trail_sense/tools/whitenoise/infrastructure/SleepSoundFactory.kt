package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.andromeda.sound.SoundPlayer

class SleepSoundFactory {
    fun getSleepSound(sleepSound: SleepSound): ISoundPlayer {
        val track = when (sleepSound) {
            SleepSound.WhiteNoise -> WhiteNoiseGenerator().getNoise(durationSeconds = 2.0)
            SleepSound.PinkNoise -> PinkNoiseGenerator().getNoise(durationSeconds = 2.0)
            SleepSound.BrownNoise -> BrownNoiseGenerator().getNoise(durationSeconds = 2.0)
            SleepSound.Crickets -> CricketsGenerator().getNoise(durationSeconds = CricketsGenerator.totalChirpDuration.toFloat())
            SleepSound.CricketsNoChirp -> CricketsGenerator(includeNearbyCricket = false).getNoise(
                durationSeconds = CricketsGenerator.totalChirpDuration.toFloat()
            )
        }

        return SoundPlayer(track)
    }
}