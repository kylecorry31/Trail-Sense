package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.andromeda.sound.SoundPlayer

class BrownNoise : SoundPlayer(BrownNoiseGenerator().getNoise(durationSeconds = 2.0))