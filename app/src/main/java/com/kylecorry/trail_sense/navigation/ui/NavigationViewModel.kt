package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.domain.NavigationVector
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconRepo
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.Accuracy
import com.kylecorry.trail_sense.shared.formatHM
import com.kylecorry.trail_sense.shared.math.deltaAngle
import com.kylecorry.trail_sense.shared.sensors.DeviceOrientation
import com.kylecorry.trail_sense.shared.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.ICompass
import com.kylecorry.trail_sense.shared.sensors.IGPS
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class NavigationViewModel(
    private val compass: ICompass,
    private val gps: IGPS,
    private val altimeter: IAltimeter,
    private val orientation: DeviceOrientation,
    prefs: UserPreferences,
    beaconRepo: BeaconRepo
) {

    private val useTrueNorth = prefs.navigation.useTrueNorth
    private val distanceUnits = prefs.distanceUnits
    private val prefShowLinearCompass = prefs.navigation.showLinearCompass
    private val beacons = beaconRepo.get()
    private val showNearbyBeacons = prefs.navigation.showMultipleBeacons
    private val visibleBeacons = prefs.navigation.numberOfVisibleBeacons
    private val showSunAndMoon = prefs.astronomy.showOnCompass
    private val showSunAndMoonWhenDown = prefs.astronomy.showOnCompassWhenDown
    private val astronomyService = AstronomyService()
    private val navigationService = NavigationService()
    private var speed = 0f

    val rulerScale = prefs.navigation.rulerScale

    val azimuth: Float
        get() {
            compass.declination = if (useTrueNorth) {
                navigationService.getDeclination(gps.location, gps.altitude)
            } else {
                0f
            }
            return compass.bearing.value
        }

    val azimuthTxt: String
        get() = "${(azimuth.roundToInt() % 360).toString().padStart(3, ' ')}°"

    val azimuthDirection: String
        get() {
            compass.declination = if (useTrueNorth) {
                navigationService.getDeclination(gps.location, gps.altitude)
            } else {
                0f
            }
            return compass.bearing.direction.symbol
        }

    val location: String
        get() = gps.location.getFormattedString()

    val altitude: String
        get() {
            return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
                "${altimeter.altitude.roundToInt()} m"
            } else {
                "${LocationMath.convertToBaseUnit(altimeter.altitude, distanceUnits)
                    .roundToInt()} ft"
            }
        }

    val showLinearCompass: Boolean
        get() = prefShowLinearCompass && orientation.orientation == DeviceOrientation.Orientation.Portrait

    var beacon: Beacon? = null

    val hasComment: Boolean
        get() = !visibleBeacon?.comment.isNullOrBlank()

    val comment: String
        get() = visibleBeacon?.comment ?: ""

    val commentTitle: String
        get() = visibleBeacon?.name ?: ""

    private val destinationBearing: Float?
        get() {
            visibleBeacon?.apply {
                return navigationService.navigate(
                    gps.location,
                    this.coordinate,
                    gps.altitude,
                    useTrueNorth
                ).direction.value
            }
            return null
        }

    val showDestination: Boolean
        get() = beacon != null

    val showNavigationSheet: Boolean
        get() = visibleBeacon != null

    val shareableLocation: Coordinate
        get() = gps.location

    private fun isFacingBeacon(beacon: Beacon): Boolean {
        val direction = navigationService.navigate(
            gps.location,
            beacon.coordinate,
            gps.altitude,
            useTrueNorth
        ).direction.value
        return abs(deltaAngle(direction, azimuth)) < 20
    }

    val nearestBeacons: List<Float>
        get() {
            if (showDestination) {
                return listOf(sunBearing, moonBearing, destinationBearing ?: 0f)
            }

            if (!showNearbyBeacons) {
                return listOf(sunBearing, moonBearing)
            }

            val sunAndMoon = listOf(sunBearing, moonBearing)

            val beacons = _nearestVisibleBeacons
                .map {
                    it.second.direction.value
                }.toList()

            return sunAndMoon + beacons
        }

    private val _nearestVisibleBeacons: List<Pair<Beacon, NavigationVector>>
        get() {
            val navigationService = NavigationService()
            return beacons.asSequence()
                .filter { it.visible }
                .map {
                    Pair(
                        it,
                        navigationService.navigate(
                            gps.location,
                            it.coordinate,
                            gps.altitude,
                            useTrueNorth
                        )
                    )
                }
                .filter { it.second.distance >= MIN_BEACON_DISTANCE } // Don't look at really close beacons
                .sortedBy { it.second.distance }
                .take(visibleBeacons)
                .toList()
        }

    val beaconDistance: String
        get() {
            visibleBeacon?.apply {
                val vector = navigationService.navigate(
                    gps.location,
                    this.coordinate,
                    gps.altitude,
                    useTrueNorth
                )
                return LocationMath.distanceToReadableString(vector.distance, distanceUnits)
            }
            return ""
        }

    val beaconName: String
        get() = visibleBeacon?.name ?: ""

    val beaconDirection: String
        get() {
            visibleBeacon?.apply {
                val vector = navigationService.navigate(
                    gps.location,
                    this.coordinate,
                    gps.altitude,
                    useTrueNorth
                )
                val bearing = vector.direction.value
                return "${bearing.roundToInt()}°"
            }
            return ""
        }

    val beaconCardinalDirection: String
        get() {
            visibleBeacon?.apply {
                val vector = navigationService.navigate(
                    gps.location,
                    this.coordinate,
                    gps.altitude,
                    useTrueNorth
                )
                return vector.direction.direction.symbol
            }
            return ""
        }

    val showBeaconElevation: Boolean
        get() = visibleBeacon?.elevation != null

    val beaconElevation: String
        get() {
            visibleBeacon ?: return ""
            return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
                "${visibleBeacon?.elevation?.roundToInt() ?: 0} m"
            } else {
                "${LocationMath.convertToBaseUnit(visibleBeacon?.elevation ?: 0f, distanceUnits)
                    .roundToInt()} ft"
            }
        }

    val beaconElevationDiffColor: Int
        get() {
            val elevation = visibleBeacon?.elevation ?: 0f
            val diff = elevation - altimeter.altitude
            return when {
                diff >= 0 -> {
                    R.color.positive
                }
                else -> {
                    R.color.negative
                }
            }
        }

    val beaconElevationDiff: String
        get() {
            val elevation = visibleBeacon?.elevation ?: 0f
            val diff = elevation - altimeter.altitude
            return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
                "${if (diff.roundToInt() > 0) "+" else ""}${diff.roundToInt()} m"
            } else {
                "${if (LocationMath.convertToBaseUnit(diff, distanceUnits)
                        .roundToInt() > 0
                ) "+" else ""}${LocationMath.convertToBaseUnit(diff, distanceUnits)
                    .roundToInt()} ft"
            }
        }

    val beaconEta: String?
        get() {
            if (speed == 0f) {
                return null
            }

            visibleBeacon?.apply {
                val vector = navigationService.navigate(
                    gps.location,
                    this.coordinate,
                    gps.altitude,
                    useTrueNorth
                )
                val time = Duration.ofSeconds((vector.distance / speed).roundToLong())
                return time.formatHM()
            }

            return null
        }

    val currentSpeed: String
        get() {
            val s = gps.speed
            return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
                "${s.roundToInt()} m/s"
            } else {
                "${LocationMath.convertToBaseUnit(s, distanceUnits).roundToInt()} ft/s"
            }
        }

    fun onLocationUpdate() {
        if (gps.speed == 0f) {
            return
        }

        speed = if (speed == 0f) {
            gps.speed
        } else {
            speed * 0.4f + gps.speed * 0.6f
        }
    }

    fun updateVisibleBeacon() {
        if (beacon != null) {
            visibleBeacon = beacon
            return
        }

        if (!showNearbyBeacons) {
            visibleBeacon = null
            return
        }

        val vectors = _nearestVisibleBeacons

        val nearestBeacon = vectors.minBy {
            val direction = it.second.direction.value
            abs(deltaAngle(direction, azimuth))
        }

        nearestBeacon?.apply {
            if (isFacingBeacon(first)) {
                visibleBeacon = first
                return
            }
        }

        visibleBeacon = null
    }

    private var visibleBeacon: Beacon? = null

    val moonBeaconVisibility: Int
        get() {
            return if (showSunAndMoonWhenDown) {
                View.VISIBLE
            } else if (!showSunAndMoon || !astronomyService.isMoonUp(gps.location)) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

    val sunBeaconVisibility: Int
        get() {
            return if (showSunAndMoonWhenDown) {
                View.VISIBLE
            } else if (!showSunAndMoon || !astronomyService.isSunUp(gps.location)) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

    val sunBeaconOpacity: Float
        get() {
            return if (astronomyService.isSunUp(gps.location)) {
                1f
            } else {
                0.5f
            }
        }

    val moonBeaconOpacity: Float
        get() {
            return if (astronomyService.isMoonUp(gps.location)) {
                1f
            } else {
                0.5f
            }
        }

    val showCompassAccuracy: Boolean
        get() = compass.accuracy != Accuracy.Unknown

    val compassAccuracy: String
        get() = compass.accuracy.toString()

    val showGpsAccuracy: Boolean
        get() = gps.accuracy != Accuracy.Unknown

    val gpsAccuracy: String
        get() = gps.accuracy.toString()

    private val sunBearing: Float
        get() {
            val declination = if (!useTrueNorth) navigationService.getDeclination(
                gps.location,
                gps.altitude
            ) else 0f
            return astronomyService.getSunAzimuth(gps.location).withDeclination(-declination).value
        }

    private val moonBearing: Float
        get() {
            val declination = if (!useTrueNorth) navigationService.getDeclination(
                gps.location,
                gps.altitude
            ) else 0f
            return astronomyService.getMoonAzimuth(gps.location).withDeclination(-declination).value
        }

    companion object {
        const val MIN_BEACON_DISTANCE = 8f
    }

}