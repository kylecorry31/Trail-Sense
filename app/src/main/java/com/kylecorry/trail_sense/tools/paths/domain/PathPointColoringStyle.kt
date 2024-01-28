package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class PathPointColoringStyle(override val id: Long) : Identifiable {
    None(1),
    CellSignal(2),
    Altitude(3),
    Time(4),
    Slope(5)
}