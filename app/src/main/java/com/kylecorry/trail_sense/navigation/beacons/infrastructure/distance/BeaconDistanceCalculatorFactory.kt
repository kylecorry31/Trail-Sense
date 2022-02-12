package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon
import com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence.IBeaconService

class BeaconDistanceCalculatorFactory(private val service: IBeaconService) :
    IBeaconDistanceCalculatorFactory {
    override fun <T : IBeacon> getCalculator(beacon: T): IBeaconDistanceCalculator<T> {
        @Suppress("UNCHECKED_CAST")
        return if (beacon is Beacon) {
            BeaconDistanceCalculator()
        } else {
            BeaconGroupDistanceCalculator(service, this)
        } as IBeaconDistanceCalculator<T>
    }
}