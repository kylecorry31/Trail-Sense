package com.kylecorry.trail_sense.navigation.beacons.domain

enum class BeaconOwner(val id: Int) {
    User(0),
    Path(1),
    CellSignal(2)
}