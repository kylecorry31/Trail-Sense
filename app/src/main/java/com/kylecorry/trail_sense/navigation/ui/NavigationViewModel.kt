package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import com.kylecorry.trail_sense.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.domain.NavigationVector
import com.kylecorry.trail_sense.navigation.infrastructure.BeaconDB
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.math.deltaAngle
import com.kylecorry.trail_sense.shared.sensors.DeviceOrientation
import com.kylecorry.trail_sense.shared.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.ICompass
import com.kylecorry.trail_sense.shared.sensors.IGPS
import kotlin.math.abs
import kotlin.math.roundToInt

class NavigationViewModel(
    private val compass: ICompass,
    private val gps: IGPS,
    private val altimeter: IAltimeter,
    private val orientation: DeviceOrientation,
    prefs: UserPreferences,
    beaconDB: BeaconDB
) {

    private val useTrueNorth = prefs.navigation.useTrueNorth
    private val distanceUnits = prefs.distanceUnits
    private val prefShowLinearCompass = prefs.navigation.showLinearCompass
    private val beacons = beaconDB.beacons
    private val showNearbyBeacons = prefs.navigation.showMultipleBeacons
    private val visibleBeacons = prefs.navigation.numberOfVisibleBeacons
    private val showSunAndMoon = prefs.astronomy.showOnCompass
    private val showSunAndMoonWhenDown = prefs.astronomy.showOnCompassWhenDown
    private val astronomyService = AstronomyService()
    private val navigationService = NavigationService()

    val rulerScale = prefs.navigation.rulerScale

    val azimuth: Float
        get() {
            compass.declination = if (useTrueNorth){
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
            compass.declination = if (useTrueNorth){
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

    private val destination: String
        get() {
            beacon?.apply {
                val vector = navigationService.navigate(
                    gps.location,
                    this.coordinate,
                    gps.altitude,
                    useTrueNorth
                )
                val bearing = vector.direction.value
                return "${this.name}    (${bearing.roundToInt()}°)\n${LocationMath.distanceToReadableString(
                    vector.distance,
                    distanceUnits
                )}"
            }
            return ""
        }

    private val destinationBearing: Float?
        get() {
            beacon?.apply {
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

    val navigation: String
        get() {
            if (showDestination) {
                return destination
            }

            if (!showNearbyBeacons) {
                return ""
            }

            val vectors = _nearestVisibleBeacons

            val nearestBeacon = vectors.minBy {
                val direction = it.second.direction.value
                abs(deltaAngle(direction, azimuth))
            }
            nearestBeacon?.apply {
                if (!isFacingBeacon(this.first)) return ""
                val direction = this.second.direction.value
                return "${this.first.name}    (${direction.roundToInt() % 360}°)\n${LocationMath.distanceToReadableString(
                    this.second.distance,
                    distanceUnits
                )}"
            }
            return ""
        }

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

    val compassAccuracy: String
        get() {
            return compass.accuracy.toString()
        }

    val gpsAccuracy: String
        get() {
            return gps.accuracy.toString()
        }

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