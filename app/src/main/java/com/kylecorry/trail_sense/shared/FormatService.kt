package com.kylecorry.trail_sense.shared

import android.content.Context
import android.text.format.DateUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trail_sense.weather.domain.PressureUnitUtils
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.units.*
import com.kylecorry.trailsensecore.infrastructure.text.DecimalFormatter
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FormatService(private val context: Context) {

    private val v2 by lazy { FormatServiceV2(context) }
    private val prefs by lazy { UserPreferences(context) }

    fun formatTime(time: LocalTime, showSeconds: Boolean = true): String {
        val amPm = !prefs.use24HourTime
        return if (amPm) {
            time.format(DateTimeFormatter.ofPattern("h:mm${if (showSeconds) ":ss" else ""} a"))
        } else {
            time.format(DateTimeFormatter.ofPattern("H:mm${if (showSeconds) ":ss" else ""}"))
        }
    }

    fun formatDate(date: ZonedDateTime, includeWeekDay: Boolean = true): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            if (includeWeekDay) {
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR
            } else {
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
            }
        )
    }

    fun formatDayOfWeek(date: ZonedDateTime): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_WEEKDAY
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

    fun formatDistance(distance: Distance): String {
        return formatDistance(distance.distance, distance.units)
    }

    fun formatDistance(distance: Float, units: DistanceUnits): String {
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.meters_format, distance)
            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
            DistanceUnits.Feet -> context.getString(R.string.feet_format, distance)
            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
            DistanceUnits.NauticalMiles -> context.getString(
                R.string.nautical_miles_format,
                distance
            )
            DistanceUnits.Centimeters -> context.getString(R.string.centimeters_format, distance)
            DistanceUnits.Inches -> context.getString(R.string.inches_format, distance)
        }
    }

    fun formatFractionalDistance(distanceCentimeters: Float): String {
        val units = prefs.distanceUnits
        val smallDist = Distance(
            distanceCentimeters,
            DistanceUnits.Centimeters
        ).convertTo(if (units == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters else DistanceUnits.Inches)
        val formatted = DecimalFormatter.format(smallDist.distance, 4)
        return when (units) {
            UserPreferences.DistanceUnits.Meters -> context.getString(
                R.string.precise_centimeters_format,
                formatted
            )
            UserPreferences.DistanceUnits.Feet -> context.getString(
                R.string.precise_inches_format,
                formatted
            )
        }
    }

    fun formatDistancePrecise(distance: Float, units: DistanceUnits, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(distance, 4, strict)
        return when (units) {
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

    fun formatDepth(distance: Float, units: DistanceUnits): String {
        return when (units) {
            DistanceUnits.Meters -> context.getString(R.string.depth_meters_format, distance)
            DistanceUnits.Kilometers -> context.getString(R.string.kilometers_format, distance)
            DistanceUnits.Feet -> context.getString(R.string.depth_feet_format, distance)
            DistanceUnits.Miles -> context.getString(R.string.miles_format, distance)
            DistanceUnits.NauticalMiles -> context.getString(
                R.string.nautical_miles_format,
                distance
            )
            DistanceUnits.Inches -> context.getString(R.string.inches_format, distance)
            DistanceUnits.Centimeters -> context.getString(R.string.centimeters_format, distance)
        }
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
        val distanceUnits = prefs.distanceUnits
        val convertedSpeed = LocationMath.convertToBaseSpeed(metersPerSecond, distanceUnits)
        return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
            context.getString(R.string.kilometers_per_hour_format, convertedSpeed)
        } else {
            context.getString(R.string.miles_per_hour_format, convertedSpeed)
        }
    }

    fun formatLocation(location: Coordinate, format: CoordinateFormat? = null): String {
        val formatted = when (format ?: prefs.navigation.coordinateFormat) {
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

    private fun getBaseUnit(): DistanceUnits {
        val prefUnits = prefs.distanceUnits
        return if (prefUnits == UserPreferences.DistanceUnits.Feet) {
            DistanceUnits.Feet
        } else {
            DistanceUnits.Meters
        }
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

    fun formatMagneticField(magneticField: Float): String {
        return context.getString(R.string.magnetic_field_format, magneticField)
    }

    fun coordinateFormatString(unit: CoordinateFormat): String {
        return when(unit){
            CoordinateFormat.DecimalDegrees -> context.getString(R.string.coordinate_format_decimal_degrees)
            CoordinateFormat.DegreesDecimalMinutes -> context.getString(R.string.coordinate_format_degrees_decimal_minutes)
            CoordinateFormat.DegreesMinutesSeconds -> context.getString(R.string.coordinate_format_degrees_minutes_seconds)
            CoordinateFormat.UTM -> context.getString(R.string.coordinate_format_utm)
            CoordinateFormat.MGRS -> context.getString(R.string.coordinate_format_mgrs)
        }
    }

}