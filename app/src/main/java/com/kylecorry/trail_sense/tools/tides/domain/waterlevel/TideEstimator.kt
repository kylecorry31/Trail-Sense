package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class TideEstimator(override val id: Long): Identifiable {
    Clock(1),
    LunitidalInterval(2),
    Harmonic(3),
    TideModel(4)
}