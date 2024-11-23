package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.math.SolMath.clamp
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.science.geology.NavigationVector
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.NearbyBeaconFilter
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max

class NavigationService {

    fun navigate(
        from: Coordinate,
        to: Coordinate,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): NavigationVector {
        return Geology.navigate(from, to, declination, usingTrueNorth)
    }

    fun navigate(
        fromLocation: Coordinate,
        fromElevation: Float,
        to: Beacon,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): NavigationVector {
        val originalVector = navigate(fromLocation, to.coordinate, declination, usingTrueNorth)
        val altitudeChange = if (to.elevation != null) to.elevation - fromElevation else null
        return originalVector.copy(altitudeChange = altitudeChange)
    }

    fun eta(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        to: Beacon
    ): Duration {
        val adjustedSpeed =
            if (speed < 3) clamp(speed, 0.89408f, 1.78816f) else speed

        val time = scarfsDistance(location, to.coordinate, elevation, to.elevation) / adjustedSpeed

        return Duration.ofSeconds(time.toLong())
    }

    private fun scarfsDistance(
        from: Coordinate,
        to: Coordinate,
        fromAltitude: Float? = null,
        toAltitude: Float? = null
    ): Float {
        val distance = from.distanceTo(to)
        val elevationGain =
            max(
                if (toAltitude == null || fromAltitude == null) 0f else (toAltitude - fromAltitude),
                0f
            )

        return distance + 7.92f * elevationGain
    }

    fun getNearbyBeacons(
        location: Coordinate,
        beacons: Collection<Beacon>,
        numNearby: Int,
        minDistance: Float = 0f,
        maxDistance: Float = Float.POSITIVE_INFINITY
    ): Collection<Beacon> {
        return NearbyBeaconFilter().filterNearbyBeacons(
            location,
            beacons,
            numNearby,
            minDistance,
            maxDistance
        )
    }

    private fun isFacingBearing(azimuth: Float, bearing: Float): Boolean {
        return abs(deltaAngle(bearing, azimuth)) < 20
    }

    fun getFacingBeacon(
        location: Coordinate,
        bearing: Float,
        beacons: Collection<Beacon>,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): Beacon? {
        return beacons.map {
            Pair(
                it,
                if (usingTrueNorth) {
                    location.bearingTo(it.coordinate)
                } else {
                    DeclinationUtils.fromTrueNorthBearing(
                        location.bearingTo(it.coordinate),
                        declination
                    )
                }
            )
        }.filter {
            isFacingBearing(bearing, it.second.value)
        }.minByOrNull { abs(deltaAngle(it.second.value, bearing)) }?.first
    }

}