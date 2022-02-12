package com.kylecorry.trail_sense.navigation.beacons.infrastructure.distance

import com.kylecorry.trail_sense.navigation.beacons.domain.IBeacon

interface IBeaconDistanceCalculatorFactory {
    fun <T : IBeacon> getCalculator(beacon: T): IBeaconDistanceCalculator<T>
}