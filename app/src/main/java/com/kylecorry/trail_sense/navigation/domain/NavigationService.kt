package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.math.MathUtils
import com.kylecorry.trail_sense.shared.math.deltaAngle
import java.time.Duration
import kotlin.math.*

class NavigationService {

    fun navigate(
        from: Coordinate,
        to: Coordinate,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): NavigationVector {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)

        val declinationAdjustment = if (usingTrueNorth) {
            0f
        } else {
            -declination
        }

        return NavigationVector(
            Bearing(results[1]).withDeclination(declinationAdjustment),
            results[0]
        )
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
            if (from.speed < 3) MathUtils.clamp(from.speed, 0.89408f, 1.78816f) else from.speed
        val elevationGain =
            max(if (to.elevation == null) 0f else (to.elevation - from.altitude), 0f)
        val distance = from.location.distanceTo(to.coordinate) * (if (nonLinear) PI.toFloat() / 2f else 1f)

        val baseTime = distance / speed
        val elevationMinutes = (elevationGain / 300f) * 30f * 60f

        return Duration.ofSeconds(baseTime.toLong()).plusSeconds(elevationMinutes.toLong())
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
        }.minBy { abs(deltaAngle(it.second.value, position.bearing.value)) }?.first
    }

}