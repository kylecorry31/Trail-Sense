package com.kylecorry.trail_sense.navigation.ui

import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.shared.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.DeviceOrientation
import com.kylecorry.trail_sense.shared.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.sensors.ICompass
import com.kylecorry.trail_sense.shared.sensors.IGPS
import kotlin.math.roundToInt

class NavigationViewModel(
    private val compass: ICompass,
    private val gps: IGPS,
    private val altimeter: IAltimeter,
    private val orientation: DeviceOrientation,
    private val prefs: UserPreferences
) {

    private val declinationCalculator = DeclinationCalculator()
    private val useTrueNorth = prefs.navigation.useTrueNorth
    private val distanceUnits = prefs.distanceUnits
    private val prefShowLinearCompass = prefs.navigation.showLinearCompass
    private val showRuler = prefs.navigation.showRuler

    val azimuth: Float
        get() {
            if (useTrueNorth) {
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                compass.declination = declination
            } else {
                compass.declination = 0f
            }
            return compass.bearing.value
        }

    val azimuthTxt: String
        get() = "${(azimuth.roundToInt() % 360).toString().padStart(3, ' ')}°"

    val azimuthDirection: String
        get() {
            if (useTrueNorth) {
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                compass.declination = declination
            } else {
                compass.declination = 0f
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
                "${LocationMath.convertToBaseUnit(altimeter.altitude, distanceUnits).roundToInt()} ft"
            }
        }

    val rulerVisible: Boolean
        get() = showRuler

    val showLinearCompass: Boolean
        get() = prefShowLinearCompass && orientation.orientation == DeviceOrientation.Orientation.Portrait

    var beacon: Beacon? = null

    val destination: String
        get() {
            beacon?.apply {
                val vector = NavigationService().navigate(gps.location, this.coordinate)
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                val bearing =
                    if (!useTrueNorth) vector.direction.withDeclination(-declination).value else vector.direction.value
                return "${this.name}    (${bearing.roundToInt()}°)\n${LocationMath.distanceToReadableString(
                    vector.distance,
                    distanceUnits
                )}"
            }
            return ""
        }

    val destinationBearing: Float?
        get() {
            beacon?.apply {
                val vector = NavigationService().navigate(gps.location, this.coordinate)
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                return if (!useTrueNorth) vector.direction.withDeclination(-declination).value else vector.direction.value
            }
            return null
        }

    val showDestination: Boolean
        get() = beacon != null

    val shareableLocation: Coordinate
        get() = gps.location

}