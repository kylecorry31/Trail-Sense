package com.kylecorry.trail_sense.navigation.beacons.infrastructure.sort

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance.IBeaconDistanceCalculatorFactory

class NearestBeaconSort(
    private val distanceFactory: IBeaconDistanceCalculatorFactory,
    private val locationProvider: () -> Coordinate
) : IBeaconSort {
    override suspend fun sort(beacons: List<IBeacon>): List<IBeacon> {
        val location = locationProvider.invoke()
        return beacons
            .map { it to getDistance(location, it) }
            .sortedBy { it.second }
            .map { it.first }
    }

    private suspend fun getDistance(location: Coordinate, beacon: IBeacon): Float {
        return distanceFactory.getCalculator(beacon).calculate(location, beacon)
    }
}