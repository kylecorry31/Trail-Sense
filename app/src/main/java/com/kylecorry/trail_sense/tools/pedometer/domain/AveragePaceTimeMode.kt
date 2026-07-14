package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class AveragePaceTimeMode(override val id: Long) : Identifiable {
    Active(1),
    Elapsed(2)
}
