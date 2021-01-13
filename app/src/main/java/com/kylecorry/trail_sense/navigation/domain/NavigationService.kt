package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trailsensecore.domain.geo.Bearing
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.shared.math.deltaAngle
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.INavigationService
import com.kylecorry.trailsensecore.domain.navigation.NavigationService
import com.kylecorry.trailsensecore.domain.navigation.NavigationVector
import com.kylecorry.trailsensecore.domain.navigation.Position
import java.time.Duration
import kotlin.math.abs

class NavigationService {

    private val newNavigationService: INavigationService = NavigationService()

    fun navigate(
        from: Coordinate,
        to: Coordinate,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): NavigationVector {
        return newNavigationService.navigate(from, to, declination, usingTrueNorth)
    }

    fun navigate(
        from: Position,
        to: Beacon,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): NavigationVector {
        return newNavigationService.navigate(from, to, declination, usingTrueNorth)
    }

    fun eta(from: Position, to: Beacon, nonLinear: Boolean = false): Duration {
        return newNavigationService.eta(from, to, nonLinear)
    }

    fun getNearbyBeacons(
        location: Coordinate,
        beacons: Collection<Beacon>,
        numNearby: Int,
        minDistance: Float = 0f,
        maxDistance: Float = Float.POSITIVE_INFINITY
    ): Collection<Beacon> {
        return beacons.asSequence()
            .filter { it.visible }
            .map { Pair(it, location.distanceTo(it.coordinate)) }
            .filter { it.second in minDistance..maxDistance }
            .sortedBy { it.second }
            .take(numNearby)
            .map { it.first }
            .toList()
    }

    fun isFacingBearing(azimuth: Bearing, bearing: Bearing): Boolean {
        return abs(deltaAngle(bearing.value, azimuth.value)) < 20
    }

    fun getFacingBeacon(
        position: Position,
        beacons: Collection<Beacon>,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): Beacon? {
        return beacons.map {
                val declinationAdjustment = if (usingTrueNorth) {
                    0f
                } else {
                    -declination
                }
                Pair(
                    it,
                    position.location.bearingTo(it.coordinate).withDeclination(declinationAdjustment)
                )
            }.filter {
                isFacingBearing(position.bearing, it.second)
            }.minByOrNull { abs(deltaAngle(it.second.value, position.bearing.value)) }?.first
    }

}