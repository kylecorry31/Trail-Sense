package com.kylecorry.trail_sense.navigation.ui

import com.kylecorry.trail_sense.navigation.domain.Beacon
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.NavigationService
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
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

    val azimuth: Float
        get() {
            if (prefs.navigation.useTrueNorth) {
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
            if (prefs.navigation.useTrueNorth) {
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                compass.declination = declination
            } else {
                compass.declination = 0f
            }
            return compass.bearing.direction.symbol.toUpperCase()
        }

    val location: String
        get() = gps.location.getFormattedString()

    val altitude: String
        get() {
            val units = prefs.distanceUnits
            return if (units == UserPreferences.DistanceUnits.Meters) {
                "${altimeter.altitude.roundToInt()} m"
            } else {
                "${LocationMath.convertToBaseUnit(altimeter.altitude, units).roundToInt()} ft"
            }
        }

    val rulerVisible: Boolean
        get() = prefs.navigation.showRuler

    val showLinearCompass: Boolean
        get() = prefs.navigation.showLinearCompass && orientation.orientation == DeviceOrientation.Orientation.Portrait

    var beacon: Beacon? = null

    val destination: String
        get() {
            beacon?.apply {
                val vector = NavigationService().navigate(gps.location, this.coordinate)
                val units = prefs.distanceUnits
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                val bearing =
                    if (!prefs.navigation.useTrueNorth) vector.direction.withDeclination(-declination).value else vector.direction.value
                return "${this.name}    (${bearing.roundToInt()}°)\n${LocationMath.distanceToReadableString(
                    vector.distance,
                    units
                )}"
            }
            return ""
        }

    val destinationBearing: Float?
        get() {
            beacon?.apply {
                val vector = NavigationService().navigate(gps.location, this.coordinate)
                val declination = declinationCalculator.calculate(gps.location, gps.altitude)
                return if (!prefs.navigation.useTrueNorth) vector.direction.withDeclination(-declination).value else vector.direction.value
            }
            return null
        }

    val showDestination: Boolean
        get() = beacon != null

}