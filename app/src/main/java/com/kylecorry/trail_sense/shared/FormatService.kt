package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.units.*
import java.time.Duration

class FormatService(private val context: Context) {

    private val v2 by lazy { FormatServiceV2(context) }
    private val prefs by lazy { UserPreferences(context) }

    fun formatDegrees(degrees: Float): String {
        return v2.formatDegrees(degrees, 0, true)
    }

    fun formatDirection(direction: CompassDirection): String {
        return v2.formatDirection(direction)
    }

    fun formatDuration(duration: Duration, short: Boolean = false): String {
        return v2.formatDuration(duration, short)
    }

    fun formatDistance(distance: Distance): String {
        return formatDistance(distance.distance, distance.units)
    }

    fun formatDistance(distance: Float, units: DistanceUnits): String {
        // 0: M, FT
        // 2: KM,MI,NM,CM,IN,YD

        val places = if (units == DistanceUnits.Meters || units == DistanceUnits.Feet){
            0
        } else {
            2
        }

        return v2.formatDistance(Distance(distance, units), places, false)


//        return when (units) {
//            DistanceUnits.Meters -> context.getString(R.string.meters_format, distance)
//            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
//            DistanceUnits.Feet -> context.getString(R.string.feet_format, distance)
//            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
//            DistanceUnits.NauticalMiles -> context.getString(
//                R.string.nautical_miles_format,
//                distance
//            )
//            DistanceUnits.Centimeters -> context.getString(R.string.centimeters_format, distance)
//            DistanceUnits.Inches -> context.getString(R.string.inches_format, distance)
//            DistanceUnits.Yards -> context.getString(R.string.yards_format, DecimalFormatter.format(distance, 2))
//        }
    }

    fun formatSmallDistance(distanceMeters: Float): String {
        val base = getBaseUnit()
        return formatDistance(Distance(distanceMeters, DistanceUnits.Meters).convertTo(base))
    }

    fun formatLargeDistance(distanceMeters: Float, prefUnits: UserPreferences.DistanceUnits? = null): String {
        val units = getLargeDistanceUnits(distanceMeters, prefUnits)
        return formatDistance(Distance(distanceMeters, DistanceUnits.Meters).convertTo(units))
    }

    fun formatQuality(quality: Quality): String {
        return v2.formatQuality(quality)
    }

    fun formatSpeed(metersPerSecond: Float): String {
        return v2.formatSpeed(metersPerSecond)
    }

    fun formatLocation(location: Coordinate, format: CoordinateFormat? = null): String {
        return v2.formatLocation(location, format)
    }

    private fun getBaseUnit(): DistanceUnits {
        return prefs.baseDistanceUnits
    }

    private fun getLargeDistanceUnits(meters: Float, prefUnits: UserPreferences.DistanceUnits? = null): DistanceUnits {
        val units = prefUnits ?: prefs.distanceUnits

        if (units == UserPreferences.DistanceUnits.Feet) {
            val feetThreshold = 1000
            val feet = Distance(meters, DistanceUnits.Meters).convertTo(DistanceUnits.Feet).distance
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

    fun formatPressure(pressure: Float, unit: PressureUnits): String {
        return v2.formatPressure(Pressure(pressure, unit), 1)
    }

    fun formatPercentage(percent: Int): String {
        return v2.formatPercentage(percent.toFloat())
    }

    fun coordinateFormatString(unit: CoordinateFormat): String {
        return v2.formatCoordinateType(unit)
    }

}