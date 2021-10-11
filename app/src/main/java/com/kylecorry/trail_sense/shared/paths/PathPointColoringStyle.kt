package com.kylecorry.trail_sense.shared.paths

import com.kylecorry.trail_sense.shared.database.Identifiable

enum class PathPointColoringStyle(override val id: Long) : Identifiable {
    None(1),
    CellSignal(2),
    Altitude(3),
    Time(4)
}