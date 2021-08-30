package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.andromeda.sound.SoundPlayer

class PinkNoise : SoundPlayer(PinkNoiseGenerator().getNoise(durationSeconds = 5))