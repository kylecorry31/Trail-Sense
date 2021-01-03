package com.kylecorry.trail_sense.shared

import android.content.Context
import android.text.format.DateUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.Accuracy
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FormatService(private val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    fun formatTime(time: LocalTime): String {
        val amPm = !prefs.use24HourTime
        return if (amPm){
            time.format(DateTimeFormatter.ofPattern("h:mm:ss a"))
        } else {
            time.format(DateTimeFormatter.ofPattern("H:mm:ss"))
        }
    }

    fun formatDate(date: ZonedDateTime): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatDegrees(degrees: Float): String {
        val formatted = context.getString(R.string.degree_format, degrees)
        return formatted.replace("360", "0")
    }

    fun formatTemperature(degreesC: Float, units: TemperatureUnits): String {
        return if (units == TemperatureUnits.C) {
            context.getString(R.string.temp_c_format, degreesC)
        } else {
            val f = degreesC * 9 / 5f + 32
            context.getString(R.string.temp_f_format, f)
        }
    }

    fun formatHumidity(humidity: Float): String {
        return context.getString(R.string.humidity_format, humidity)
    }

    fun formatDirection(direction: CompassDirection): String {
        return when (direction) {
            CompassDirection.North -> context.getString(R.string.direction_north)
            CompassDirection.South -> context.getString(R.string.direction_south)
            CompassDirection.East -> context.getString(R.string.direction_east)
            CompassDirection.West -> context.getString(R.string.direction_west)
            CompassDirection.NorthEast -> context.getString(R.string.direction_north_east)
            CompassDirection.SouthEast -> context.getString(R.string.direction_south_east)
            CompassDirection.NorthWest -> context.getString(R.string.direction_north_west)
            CompassDirection.SouthWest -> context.getString(R.string.direction_south_west)
        }
    }

    fun formatDuration(duration: Duration, short: Boolean = false): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60

        return if (short) {
            when (hours) {
                0L -> context.getString(R.string.duration_minute_format, minutes)
                else -> context.getString(R.string.duration_hour_format, hours)
            }
        } else {
            when {
                hours == 0L -> context.getString(R.string.duration_minute_format, minutes)
                minutes == 0L -> context.getString(R.string.duration_hour_format, hours)
                else -> context.getString(R.string.duration_hour_minute_format, hours, minutes)
            }
        }
    }

    fun formatDistance(distance: Float, units: DistanceUnits): String {
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.meters_format, distance)
            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
            DistanceUnits.Feet -> context.getString(R.string.feet_format, distance)
            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
            DistanceUnits.NauticalMiles -> context.getString(R.string.nautical_miles_format, distance)
            DistanceUnits.Centimeters -> context.getString(R.string.centimeters_format, distance)
            DistanceUnits.Inches -> context.getString(R.string.inches_format, distance)
        }
    }

    fun formatFractionalDistance(distanceCentimeters: Float): String {
        val units = prefs.distanceUnits
        val multiplier = if (units == UserPreferences.DistanceUnits.Meters) 1f else 0.393701f
        val formatted = DecimalFormatter.format((distanceCentimeters * multiplier).toDouble())
        return when(units){
            UserPreferences.DistanceUnits.Meters -> context.getString(R.string.precise_centimeters_format, formatted)
            UserPreferences.DistanceUnits.Feet -> context.getString(R.string.precise_inches_format, formatted)
        }
    }

    fun formatDistancePrecise(distance: Float, units: DistanceUnits): String {
        val formatted = DecimalFormatter.format(distance.toDouble())
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.precise_meters_format, formatted)
            DistanceUnits.Kilometers -> context.getString(R.string.precise_kilometers_format, formatted)
            DistanceUnits.Feet -> context.getString(R.string.precise_feet_format, formatted)
            DistanceUnits.Miles -> context.getString(R.string.precise_miles_format, formatted)
            DistanceUnits.NauticalMiles -> context.getString(R.string.precise_nautical_miles_format, formatted)
            DistanceUnits.Inches -> context.getString(R.string.precise_inches_format, formatted)
            DistanceUnits.Centimeters -> context.getString(R.string.precise_centimeters_format, formatted)
        }
    }

    fun formatDepth(distance: Float, units: DistanceUnits): String {
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.depth_meters_format, distance)
            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
            DistanceUnits.Feet -> context.getString(R.string.depth_feet_format, distance)
            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
            DistanceUnits.NauticalMiles -> context.getString(R.string.nautical_miles_format, distance)
            DistanceUnits.Inches -> context.getString(R.string.inches_format, distance)
            DistanceUnits.Centimeters -> context.getString(R.string.centimeters_format, distance)
        }
    }

    fun formatSmallDistance(distanceMeters: Float): String {
        val base = getBaseUnit()
        return formatDistance(
            LocationMath.convert(distanceMeters, DistanceUnits.Meters, base),
            base
        )
    }

    fun formatLargeDistance(distanceMeters: Float): String {
        val units = getLargeDistanceUnits(distanceMeters)
        return formatDistance(
            LocationMath.convert(distanceMeters, DistanceUnits.Meters, units),
            units
        )
    }

    fun formatAccuracy(accuracy: Accuracy): String {
        return when (accuracy) {
            Accuracy.Low -> context.getString(R.string.accuracy_low)
            Accuracy.Medium -> context.getString(R.string.accuracy_medium)
            Accuracy.High -> context.getString(R.string.accuracy_high)
            else -> context.getString(R.string.accuracy_unknown)
        }
    }

    fun formatSpeed(metersPerSecond: Float): String {
        val distanceUnits = prefs.distanceUnits
        val convertedSpeed = LocationMath.convertToBaseSpeed(metersPerSecond, distanceUnits)
        return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
            context.getString(R.string.kilometers_per_hour_format, convertedSpeed)
        } else {
            context.getString(R.string.miles_per_hour_format, convertedSpeed)
        }
    }

    fun formatLocation(location: Coordinate): String {
        val formatter = prefs.navigation.locationFormatter
        val lat =
            formatter.formatLatitude(location).replace("N", formatDirection(CompassDirection.North))
                .replace("S", formatDirection(CompassDirection.South))
        val lng =
            formatter.formatLongitude(location).replace("E", formatDirection(CompassDirection.East))
                .replace("W", formatDirection(CompassDirection.West))
        return getFormattedLocation(lat, lng)
    }

    private fun getFormattedLocation(latitude: String, longitude: String): String {
        return context.getString(prefs.navigation.locationFormat, latitude, longitude)
    }

    private fun getBaseUnit(): DistanceUnits {
        val prefUnits = prefs.distanceUnits
        return if (prefUnits == UserPreferences.DistanceUnits.Feet) {
            DistanceUnits.Feet
        } else {
            DistanceUnits.Meters
        }
    }

    private fun getLargeDistanceUnits(meters: Float): DistanceUnits {
        val units = prefs.distanceUnits

        if (units == UserPreferences.DistanceUnits.Feet) {
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

    fun formatPressure(pressure: Float, unit: PressureUnits): String {
        val symbol = getPressureUnitString(unit)
        val format = PressureUnitUtils.getTendencyDecimalFormat(unit)
        val amt = format.format(pressure)
        return context.getString(R.string.pressure_format, amt, symbol)
    }

    private fun getPressureUnitString(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> context.getString(R.string.units_hpa)
            PressureUnits.Mbar -> context.getString(R.string.units_mbar)
            PressureUnits.Inhg -> context.getString(R.string.units_inhg_short)
            PressureUnits.Psi -> context.getString(R.string.units_psi)
        }
    }

    fun formatPercentage(percent: Int): String {
        return context.getString(R.string.percent_format, percent)
    }

    fun formatBatteryCapacity(capacity: Float): String {
        return context.getString(R.string.battery_capacity_format, capacity)
    }

    fun formatCurrent(current: Float): String {
        return context.getString(R.string.current_format, current)
    }

    fun formatMagneticField(magneticField: Float): String {
        return context.getString(R.string.magnetic_field_format, magneticField)
    }

}