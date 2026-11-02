package com.kylecorry.trail_sense.tools.navigation.domain

import com.kylecorry.sol.math.arithmetic.Arithmetic.clamp
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.NavigationVector
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.declination.DeclinationUtils
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.NearbyBeaconFilter
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
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
        return Geography.navigate(from, to, declination, usingTrueNorth)
    }

    fun navigate(
        fromLocation: Coordinate,
        fromElevation: Float,
        to: Beacon,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): NavigationVector {
        val originalVector = navigate(fromLocation, to.coordinate, declination, usingTrueNorth)
        val altitudeChange = to.elevation?.let { it - fromElevation }
        return originalVector.copy(altitudeChange = altitudeChange)
    }

    fun eta(
        location: Coordinate,
        elevation: Float,
        speed: Float,
        to: Beacon
    ): Duration {
        val time = scarfsDistance(location, to.coordinate, elevation, to.elevation) / getHikingSpeed(speed)

        return Duration.ofSeconds(time.toLong())
    }

    fun eta(guidance: Guidance, speed: Speed): Duration? {
        val remainingDistance = guidance.remainingDistance ?: return null
        val hikingSpeed = Speed.from(
            getHikingSpeed(speed.convertTo(DistanceUnits.Meters, TimeUnits.Seconds).value),
            DistanceUnits.Meters,
            TimeUnits.Seconds
        )
        return HikingService.getHikingDuration(
            remainingDistance,
            guidance.remainingElevationGain ?: Distance.meters(0f),
            hikingSpeed
        )
    }

    fun getHikingSpeed(speed: Float): Float {
        return if (speed < 3) clamp(speed, 0.89408f, 1.78816f) else speed
    }

    private fun scarfsDistance(
        from: Coordinate,
        to: Coordinate,
        fromAltitude: Float? = null,
        toAltitude: Float? = null
    ): Float {
        val distance = from.distanceTo(to)
        val elevationGain =
            if (toAltitude == null || fromAltitude == null) 0f else max(toAltitude - fromAltitude, 0f)

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

    fun getVisibleBeacons(
        location: Coordinate,
        beacons: Collection<Beacon>,
        destination: Beacon?,
        showNearby: Boolean,
        numNearby: Int,
        maxDistance: Float
    ): List<Beacon> {
        if (!showNearby) {
            return listOfNotNull(destination)
        }
        val nearby = getNearbyBeacons(
            location,
            beacons,
            numNearby,
            MIN_NEARBY_BEACON_DISTANCE_METERS,
            maxDistance
        )
        return (nearby + listOfNotNull(destination)).distinctBy { it.id }
    }

    fun getSelectedBeacon(
        destination: Beacon?,
        location: Coordinate,
        bearing: Float,
        beacons: Collection<Beacon>,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): Beacon? {
        return destination ?: getFacingBeacon(location, bearing, beacons, declination, usingTrueNorth)
    }

    fun getBearing(
        from: Coordinate,
        to: Coordinate,
        declination: Float,
        usingTrueNorth: Boolean = true
    ): Float {
        return DeclinationUtils.fromTrueNorthBearing(
            from.bearingTo(to).value,
            declination,
            usingTrueNorth
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
            Pair(it, getBearing(location, it.coordinate, declination, usingTrueNorth))
        }.filter {
            isFacingBearing(bearing, it.second)
        }.minByOrNull { abs(deltaAngle(it.second, bearing)) }?.first
    }

    private companion object {
        const val MIN_NEARBY_BEACON_DISTANCE_METERS = 8f
    }

}
