package com.kylecorry.trail_sense.shared.beacons

enum class BeaconOwner(val id: Int) {
    User(0),
    Path(1),
    CellSignal(2)
}