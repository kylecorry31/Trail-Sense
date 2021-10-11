package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.sol.math.SolMath.clamp
import com.kylecorry.sol.math.SolMath.deltaAngle
import com.kylecorry.sol.science.geology.GeologyService
import com.kylecorry.sol.science.geology.NavigationVector
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.Position
import com.kylecorry.trail_sense.shared.beacons.Beacon
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import java.time.Duration
import kotlin.math.PI
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

    fun eta(from: Position, to: Beacon, nonLinear: Boolean = false): Duration {
        val speed =
            if (from.speed < 3) clamp(from.speed, 0.89408f, 1.78816f) else from.speed
        val elevationGain =
            max(if (to.elevation == null) 0f else (to.elevation - from.altitude), 0f)
        val distance =
            from.location.distanceTo(to.coordinate) * (if (nonLinear) PI.toFloat() / 2f else 1f)

        val baseTime = distance / speed
        val elevationSeconds = (elevationGain / 300f) * 30f * 60f

        return Duration.ofSeconds(baseTime.toLong()).plusSeconds(elevationSeconds.toLong())
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