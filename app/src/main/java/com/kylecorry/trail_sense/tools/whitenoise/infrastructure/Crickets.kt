package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.andromeda.sound.SoundPlayer

class Crickets :
    SoundPlayer(CricketsGenerator().getNoise(durationSeconds = CricketsGenerator.totalChirpDuration.toFloat()))