package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.trail_sense.shared.database.Identifiable

enum class BeaconSortMethod(override val id: Long) : Identifiable {
    MostRecent(1),
    Closest(2),
    Name(3)
}