package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService

class BeaconGroupDistanceCalculator(
    private val service: IBeaconService,
    private val factory: IBeaconDistanceCalculatorFactory
) :
    IBeaconDistanceCalculator<BeaconGroup> {
    override suspend fun calculate(from: Coordinate, to: BeaconGroup): Float {
        val beacons = service.getBeacons(to.id)

        return beacons
            .map { it to factory.getCalculator(it) }
            .map { it.second.calculate(from, it.first) }
            .minByOrNull { it } ?: Float.POSITIVE_INFINITY
    }
}