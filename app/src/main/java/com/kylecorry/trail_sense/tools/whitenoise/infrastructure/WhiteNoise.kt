package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.andromeda.sound.SoundPlayer

class WhiteNoise : SoundPlayer(WhiteNoiseGenerator().getNoise(durationSeconds = 2.0))