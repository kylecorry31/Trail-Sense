package com.kylecorry.trail_sense.shared

import android.content.Context
import android.text.format.DateUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trailsensecore.domain.Accuracy
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.units.Distance
import com.kylecorry.trailsensecore.domain.units.DistanceUnits
import com.kylecorry.trailsensecore.domain.units.PressureUnits
import com.kylecorry.trailsensecore.domain.units.TemperatureUnits
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FormatServiceV2(private val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    fun formatTime(time: LocalTime): String {
        val amPm = !prefs.use24HourTime
        return if (amPm) {
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

    fun formatDistance(distance: Distance, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(distance.distance.toDouble(), decimalPlaces)
        return when (distance.units) {
            DistanceUnits.Meters -> context.getString(R.string.precise_meters_format, formatted)
            DistanceUnits.Kilometers -> context.getString(
                R.string.precise_kilometers_format,
                formatted
            )
            DistanceUnits.Feet -> context.getString(R.string.precise_feet_format, formatted)
            DistanceUnits.Miles -> context.getString(R.string.precise_miles_format, formatted)
            DistanceUnits.NauticalMiles -> context.getString(
                R.string.precise_nautical_miles_format,
                formatted
            )
            DistanceUnits.Inches -> context.getString(R.string.precise_inches_format, formatted)
            DistanceUnits.Centimeters -> context.getString(
                R.string.precise_centimeters_format,
                formatted
            )
        }
    }

    fun formatPercentage(percent: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(percent, decimalPlaces)
        return context.getString(R.string.precise_percent_format, formatted)
    }

    fun formatTemperature(
        temperature: Float,
        units: TemperatureUnits,
        decimalPlaces: Int = 0
    ): String {
        val formatted = DecimalFormatter.format(temperature, decimalPlaces)
        return when (units) {
            TemperatureUnits.F -> context.getString(R.string.precise_temp_f_format, formatted)
            TemperatureUnits.C -> context.getString(R.string.precise_temp_c_format, formatted)
        }
    }

    fun formatDegrees(degrees: Float, decimalPlaces: Int = 0, replace360: Boolean = false): String {
        val formatted = DecimalFormatter.format(degrees.toDouble(), decimalPlaces)
        val finalFormatted = if (replace360) formatted.replace("360", "0") else formatted
        return context.getString(R.string.degree, finalFormatted)
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

    fun formatAccuracy(accuracy: Accuracy): String {
        return when (accuracy) {
            Accuracy.Low -> context.getString(R.string.accuracy_low)
            Accuracy.Medium -> context.getString(R.string.accuracy_medium)
            Accuracy.High -> context.getString(R.string.accuracy_high)
            else -> context.getString(R.string.accuracy_unknown)
        }
    }

    fun formatPressure(pressure: Float, units: PressureUnits, decimalPlaces: Int = 0): String {
        val symbol = getPressureUnitString(units)
        val amt = DecimalFormatter.format(pressure.toDouble(), decimalPlaces)
        return context.getString(R.string.pressure_format, amt, symbol)
    }

    // TODO: Rewrite this
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
        val formatted = when (prefs.navigation.coordinateFormat) {
            CoordinateFormat.DecimalDegrees -> location.toDecimalDegrees()
            CoordinateFormat.DegreesDecimalMinutes -> location.toDegreeDecimalMinutes()
            CoordinateFormat.DegreesMinutesSeconds -> location.toDegreeMinutesSeconds()
            CoordinateFormat.UTM -> location.toUTM()
            CoordinateFormat.MGRS -> location.toMGRS()
        }
        if (formatted == "?") {
            return location.toDecimalDegrees()
        }
        return formatted
    }

    private fun getPressureUnitString(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> context.getString(R.string.units_hpa)
            PressureUnits.Mbar -> context.getString(R.string.units_mbar)
            PressureUnits.Inhg -> context.getString(R.string.units_inhg_short)
            PressureUnits.Psi -> context.getString(R.string.units_psi)
        }
    }

}