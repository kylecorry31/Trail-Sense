package com.kylecorry.trail_sense.shared

import android.content.Context
import android.text.format.DateUtils
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trailsensecore.domain.geo.CompassDirection
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import com.kylecorry.trailsensecore.domain.geo.CoordinateFormat
import com.kylecorry.trailsensecore.domain.units.*
import com.kylecorry.trailsensecore.domain.weather.Weather
import com.kylecorry.trailsensecore.infrastructure.sensors.battery.BatteryHealth
import com.kylecorry.trailsensecore.infrastructure.text.DecimalFormatter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FormatServiceV2(private val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    fun formatRelativeDate(date: LocalDate): String {
        val now = LocalDate.now()

        return when (date) {
            now -> {
                context.getString(R.string.today)
            }
            now.plusDays(1) -> {
                context.getString(R.string.tomorrow)
            }
            now.minusDays(1) -> {
                context.getString(R.string.yesterday)
            }
            else -> {
                DateUtils.formatDateTime(
                    context,
                    date.atStartOfDay().toEpochMillis(),
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_RELATIVE
                )
            }
        }
    }

    fun formatTime(time: LocalTime, includeSeconds: Boolean = true): String {
        val amPm = !prefs.use24HourTime
        return if (amPm) {
            time.format(DateTimeFormatter.ofPattern("h:mm${if (includeSeconds) ":ss" else ""} a"))
        } else {
            time.format(DateTimeFormatter.ofPattern("H:mm${if (includeSeconds) ":ss" else ""}"))
        }
    }

    fun formatDate(date: ZonedDateTime, includeWeekDay: Boolean = true): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_DATE or (if (includeWeekDay) DateUtils.FORMAT_SHOW_WEEKDAY else 0) or DateUtils.FORMAT_SHOW_YEAR
        )
    }

    fun formatDateTime(dateTime: ZonedDateTime): String {
        val date = formatDate(dateTime, false)
        val time = formatTime(dateTime.toLocalTime(), false)
        return "$date $time"
    }

    fun formatDistance(distance: Distance, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(distance.distance, decimalPlaces, strict)
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
            DistanceUnits.Yards -> context.getString(R.string.yards_format, formatted)
        }
    }

    fun formatTime(
        amount: Float,
        units: TimeUnits,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(amount, decimalPlaces, strict)
        return when (units) {
            TimeUnits.Milliseconds -> context.getString(R.string.milliseconds_format, formatted)
            TimeUnits.Seconds -> context.getString(R.string.seconds_format, formatted)
            TimeUnits.Minutes -> context.getString(R.string.minutes_format, formatted)
            TimeUnits.Hours -> context.getString(R.string.hours_format, formatted)
            TimeUnits.Days -> context.getString(R.string.days_format, formatted)
        }
    }

    fun formatVolume(volume: Volume, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(volume.volume, decimalPlaces, strict)
        return when (volume.units) {
            VolumeUnits.Liters -> context.getString(R.string.liter_format, formatted)
            VolumeUnits.Milliliter -> context.getString(R.string.milliliter_format, formatted)
            VolumeUnits.USCups -> context.getString(R.string.cup_format, formatted)
            VolumeUnits.USPints -> context.getString(R.string.pint_format, formatted)
            VolumeUnits.USQuarts -> context.getString(R.string.quart_format, formatted)
            VolumeUnits.USOunces -> context.getString(R.string.ounces_volume_format, formatted)
            VolumeUnits.USGallons -> context.getString(R.string.gallon_format, formatted)
            VolumeUnits.ImperialCups -> context.getString(R.string.cup_format, formatted)
            VolumeUnits.ImperialPints -> context.getString(R.string.pint_format, formatted)
            VolumeUnits.ImperialQuarts -> context.getString(R.string.quart_format, formatted)
            VolumeUnits.ImperialOunces -> context.getString(
                R.string.ounces_volume_format,
                formatted
            )
            VolumeUnits.ImperialGallons -> context.getString(R.string.gallon_format, formatted)
        }
    }

    fun formatWeight(weight: Weight, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(weight.weight, decimalPlaces, strict)
        return when (weight.units) {
            WeightUnits.Pounds -> context.getString(R.string.pounds_format, formatted)
            WeightUnits.Ounces -> context.getString(R.string.ounces_weight_format, formatted)
            WeightUnits.Kilograms -> context.getString(R.string.kilograms_format, formatted)
            WeightUnits.Grams -> context.getString(R.string.grams_format, formatted)
        }
    }

    fun formatDbm(dbm: Int): String {
        return context.getString(R.string.dbm_format, dbm.toString())
    }

    fun formatPercentage(percent: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(percent, decimalPlaces)
        return context.getString(R.string.precise_percent_format, formatted)
    }

    fun formatBatteryHealth(batteryHealth: BatteryHealth): String {
        return when (batteryHealth) {
            BatteryHealth.Cold -> context.getString(R.string.battery_health_cold)
            BatteryHealth.Dead -> context.getString(R.string.battery_health_dead)
            BatteryHealth.Good -> context.getString(R.string.battery_health_good)
            BatteryHealth.Overheat -> context.getString(R.string.battery_health_overheat)
            BatteryHealth.OverVoltage -> context.getString(R.string.battery_health_over_voltage)
            BatteryHealth.Unknown -> context.getString(R.string.unknown)
        }
    }

    fun formatTemperature(
        temperature: Temperature,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(temperature.temperature, decimalPlaces, strict)
        return when (temperature.units) {
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

    fun formatDays(days: Int): String {
        return context.resources.getQuantityString(R.plurals.number_days, days, days)
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

    fun formatAcceleration(acceleration: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(acceleration.toDouble(), decimalPlaces)
        return context.getString(R.string.acceleration_m_s2_format, formatted)
    }

    fun formatMagneticField(magneticField: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(magneticField.toDouble(), decimalPlaces)
        return context.getString(R.string.magnetic_field_format_precise, formatted)
    }

    fun formatQuality(quality: Quality): String {
        return when (quality) {
            Quality.Poor -> context.getString(R.string.quality_poor)
            Quality.Moderate -> context.getString(R.string.quality_moderate)
            Quality.Good -> context.getString(R.string.quality_good)
            else -> context.getString(R.string.unknown)
        }
    }

    fun formatPressure(pressure: Pressure, decimalPlaces: Int = 0): String {
        val symbol = getPressureUnitString(pressure.units)
        val amt = DecimalFormatter.format(pressure.pressure.toDouble(), decimalPlaces)
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

    fun formatLocation(location: Coordinate, format: CoordinateFormat? = null): String {
        val formatted = when (format ?: prefs.navigation.coordinateFormat) {
            CoordinateFormat.DecimalDegrees -> location.toDecimalDegrees()
            CoordinateFormat.DegreesDecimalMinutes -> location.toDegreeDecimalMinutes()
            CoordinateFormat.DegreesMinutesSeconds -> location.toDegreeMinutesSeconds()
            CoordinateFormat.UTM -> location.toUTM()
            CoordinateFormat.MGRS -> location.toMGRS()
            CoordinateFormat.USNG -> location.toUSNG()
            CoordinateFormat.OSNG_OSGB36 -> location.toOSNG()
        }
        if (formatted == "?") {
            return location.toDecimalDegrees()
        }
        return formatted
    }

    fun formatCoordinateType(type: CoordinateFormat): String {
        return when (type) {
            CoordinateFormat.DecimalDegrees -> context.getString(R.string.coordinate_format_decimal_degrees)
            CoordinateFormat.DegreesDecimalMinutes -> context.getString(R.string.coordinate_format_degrees_decimal_minutes)
            CoordinateFormat.DegreesMinutesSeconds -> context.getString(R.string.coordinate_format_degrees_minutes_seconds)
            CoordinateFormat.UTM -> context.getString(R.string.coordinate_format_utm)
            CoordinateFormat.MGRS -> context.getString(R.string.coordinate_format_mgrs)
            CoordinateFormat.USNG -> context.getString(R.string.coordinate_format_usng)
            CoordinateFormat.OSNG_OSGB36 -> context.getString(R.string.coordinate_format_osng)
        }
    }

    fun formatElectricalCapacity(capacity: Float): String {
        return context.getString(
            R.string.battery_capacity_format,
            DecimalFormatter.format(capacity, 0)
        )
    }

    fun formatCurrent(current: Float): String {
        // TODO: Use decimal formatter
        return context.getString(R.string.current_format, current)
    }

    fun formatLux(lux: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(lux.toDouble(), decimalPlaces)
        return context.getString(R.string.lux_format, formatted)
    }

    fun formatCandela(candela: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(candela.toDouble(), decimalPlaces)
        return context.getString(R.string.candela_format, formatted)
    }

    fun formatShortTermWeather(weather: Weather, relative: Boolean): String {
        return if (relative) {
            when (weather) {
                Weather.ImprovingFast -> context.getString(R.string.weather_improving_fast)
                Weather.ImprovingSlow -> context.getString(R.string.weather_improving_slow)
                Weather.WorseningSlow -> context.getString(R.string.weather_worsening_slow)
                Weather.WorseningFast -> context.getString(R.string.weather_worsening_fast)
                Weather.Storm -> context.getString(R.string.weather_storm_incoming)
                else -> context.getString(R.string.weather_not_changing)
            }
        } else {
            when (weather) {
                Weather.ImprovingFast -> context.getString(R.string.pressure_rising_fast)
                Weather.ImprovingSlow -> context.getString(R.string.pressure_rising)
                Weather.WorseningSlow -> context.getString(R.string.pressure_falling)
                Weather.WorseningFast -> context.getString(R.string.pressure_falling_fast)
                Weather.Storm -> context.getString(R.string.weather_storm_incoming)
                else -> context.getString(R.string.pressure_no_change)
            }
        }
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