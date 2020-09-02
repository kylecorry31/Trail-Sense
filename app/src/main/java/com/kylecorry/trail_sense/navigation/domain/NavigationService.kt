package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.math.deltaAngle
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToLong

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

    fun eta(distance: Float, speed: Float): Duration? {
        if (speed == 0f) {
            return null
        }

        return Duration.ofSeconds((distance / speed).roundToLong())
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

    fun getFacingBeacon(position: Position, beacons: Collection<Beacon>, declination: Float, usingTrueNorth: Boolean = true): Beacon? {
        return beacons.map {
            val declinationAdjustment = if (usingTrueNorth) {
                0f
            } else {
                -declination
            }
            Pair(it, position.location.bearingTo(it.coordinate).withDeclination(declinationAdjustment))
        }.filter {
            isFacingBearing(position.bearing, it.second)
        }.minBy { it.second.value }?.first
    }

    fun getBaseUnit(prefUnits: UserPreferences.DistanceUnits): DistanceUnits {
        return if (prefUnits == UserPreferences.DistanceUnits.Feet){
            DistanceUnits.Feet
        } else {
            DistanceUnits.Meters
        }
    }

    fun toUnits(meters: Float, units: DistanceUnits): Float {
        return LocationMath.convert(meters, DistanceUnits.Meters, units)
    }

    fun getDistanceUnits(meters: Float, prefUnits: UserPreferences.DistanceUnits): DistanceUnits {
        if (prefUnits == UserPreferences.DistanceUnits.Feet) {
            val feetThreshold = 1000
            val feet = LocationMath.convert(meters, DistanceUnits.Meters, DistanceUnits.Feet)
            return if (feet >= feetThreshold) {
                DistanceUnits.Miles
            } else {
                DistanceUnits.Feet
            }
        } else {
            val meterThreshold = 999
            return if (meters >= meterThreshold) {
                DistanceUnits.Kilometers
            } else {
                DistanceUnits.Meters
            }
        }
    }

}