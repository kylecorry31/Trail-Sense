package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class SleepSound(override val id: Long) : Identifiable {
    WhiteNoise(1),
    PinkNoise(2),
    BrownNoise(3),
    Crickets(4),
    CricketsNoChirp(5),
    OceanWaves(6),
    Fan(7)
}