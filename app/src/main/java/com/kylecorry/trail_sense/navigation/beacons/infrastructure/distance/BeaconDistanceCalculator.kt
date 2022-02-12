package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon

class BeaconDistanceCalculator : IBeaconDistanceCalculator<Beacon> {
    override suspend fun calculate(from: Coordinate, to: Beacon): Float {
        return from.distanceTo(to.coordinate)
    }
}