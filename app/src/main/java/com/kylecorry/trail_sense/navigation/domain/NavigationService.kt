package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.sol.math.SolMath.clamp
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.NavigationVector
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.Position
import com.kylecorry.trail_sense.navigation.beacons.domain.Beacon
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.navigation.paths.domain.PathPoint
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max

class NavigationService {

    private val newNavigationService = GeologyService()

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
        val originalVector = navigate(from.location, to.coordinate, declination, usingTrueNorth)
        val altitudeChange = if (to.elevation != null) to.elevation - from.altitude else null
        return originalVector.copy(altitudeChange = altitudeChange)
    }

    fun eta(from: Position, to: Beacon): Duration {
        val speed =
            if (from.speed < 3) clamp(from.speed, 0.89408f, 1.78816f) else from.speed

        val time = scarfsDistance(from.location, to.coordinate, from.altitude, to.elevation) / speed

        return Duration.ofSeconds(time.toLong())
    }

    fun pathDuration(path: List<PathPoint>, speed: Float = 0f): Duration {
        val realSpeed = if (speed < 3) clamp(speed, 0.89408f, 1.78816f) else speed
        var distance = 0f
        for (i in 1..path.lastIndex) {
            distance += scarfsDistance(
                path[i - 1].coordinate,
                path[i].coordinate,
                path[i - 1].elevation,
                path[i].elevation
            )
        }
        return Duration.ofSeconds((distance / realSpeed).toLong())
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
            Pair(
                it,
                if (usingTrueNorth) {
                    position.location.bearingTo(it.coordinate)
                } else {
                    DeclinationUtils.fromTrueNorthBearing(
                        position.location.bearingTo(it.coordinate),
                        declination
                    )
                }
            )
        }.filter {
            isFacingBearing(position.bearing, it.second)
        }.minByOrNull { abs(deltaAngle(it.second.value, position.bearing.value)) }?.first
    }

}