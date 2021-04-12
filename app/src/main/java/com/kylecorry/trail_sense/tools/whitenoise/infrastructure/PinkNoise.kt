package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.trailsensecore.infrastructure.audio.SoundPlayer

class PinkNoise : SoundPlayer(PinkNoiseGenerator().getNoise(durationSeconds = 5))