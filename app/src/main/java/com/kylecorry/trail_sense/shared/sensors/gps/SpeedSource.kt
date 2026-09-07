package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class SpeedSource(override val id: Long) : Identifiable {
    Unknown(0),
    Provider(1),
    PositionDerived(2)
}
