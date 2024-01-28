package com.kylecorry.trail_sense.tools.beacons.infrastructure.sort

import com.kylecorry.trail_sense.shared.data.Identifiable

enum class BeaconSortMethod(override val id: Long) : Identifiable {
    MostRecent(1),
    Closest(2),
    Name(3)
}