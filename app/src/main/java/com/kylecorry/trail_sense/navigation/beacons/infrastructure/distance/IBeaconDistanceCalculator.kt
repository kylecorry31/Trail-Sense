package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

interface IBeaconDistanceCalculator<T> where T: IBeacon {
    suspend fun calculate(from: Coordinate, to: T): Float
}