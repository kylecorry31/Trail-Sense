package com.kylecorry.trail_sense.shared

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.annotation.DrawableRes
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toDecimalDegrees
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toDegreeDecimalMinutes
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toDegreeMinutesSeconds
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toMGRS
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toOSGB
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toUSNG
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toUTM
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.time.Time.toEpochMillis
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.shared.domain.Probability
import com.kylecorry.trail_sense.shared.extensions.StringCache
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.weather.domain.forecasting.arrival.WeatherArrivalTime
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class FormatService(private val context: Context) {

    private val prefs by lazy { UserPreferences(context) }
    private val strings by lazy { StringCache(context.resources) }

    @DrawableRes
    fun getWeatherImage(condition: WeatherCondition?): Int {
        return when (condition) {
            WeatherCondition.Clear -> R.drawable.sunny
            WeatherCondition.Overcast -> R.drawable.cloudy
            WeatherCondition.Precipitation -> R.drawable.ic_precipitation
            WeatherCondition.Storm -> R.drawable.storm
            WeatherCondition.Wind -> R.drawable.wind
            WeatherCondition.Rain -> R.drawable.light_rain
            WeatherCondition.Snow -> R.drawable.ic_precipitation_snow
            WeatherCondition.Thunderstorm -> R.drawable.storm
            null -> R.drawable.steady
        }
    }

    fun formatWeather(condition: WeatherCondition?): String {
        return when (condition) {
            WeatherCondition.Clear -> strings.getString(R.string.weather_clear)
            WeatherCondition.Overcast -> strings.getString(R.string.weather_overcast)
            WeatherCondition.Precipitation -> strings.getString(R.string.weather_precipitation)
            WeatherCondition.Storm -> strings.getString(R.string.weather_storm)
            WeatherCondition.Wind -> strings.getString(R.string.weather_wind)
            WeatherCondition.Rain -> strings.getString(R.string.precipitation_rain)
            WeatherCondition.Snow -> strings.getString(R.string.precipitation_snow)
            WeatherCondition.Thunderstorm -> strings.getString(R.string.weather_thunderstorm)
            null -> strings.getString(R.string.weather_no_change)
        }
    }

    fun formatWeatherArrival(arrival: WeatherArrivalTime?): String {
        if (arrival?.isExact != true) {
            return when (arrival?.toRelative(Instant.now())) {
                RelativeArrivalTime.Now -> strings.getString(R.string.now)
                RelativeArrivalTime.VerySoon -> strings.getString(R.string.very_soon)
                RelativeArrivalTime.Soon -> strings.getString(R.string.soon)
                RelativeArrivalTime.Later -> strings.getString(R.string.later)
                null -> ""
            }.lowercase()
        }

        val time = arrival.time
        val duration = Duration.between(Instant.now(), time)

        if (duration < Duration.ofMinutes(1)) {
            return strings.getString(R.string.now).lowercase()
        }

        if (duration > Duration.ofHours(20)) {
            return strings.getString(R.string.later).lowercase()
        }

        val datetime = time.toZonedDateTime()

        return strings.getString(
            R.string.at_time,
            formatTime(datetime.toLocalTime(), includeSeconds = false)
        )
    }

    fun formatProbability(probability: Probability): String {
        return when (probability) {
            Probability.Never -> strings.getString(R.string.never)
            Probability.Low -> strings.getString(R.string.low)
            Probability.Moderate -> strings.getString(R.string.moderate)
            Probability.High -> strings.getString(R.string.high)
            Probability.Always -> strings.getString(R.string.always)
        }
    }

    fun formatRelativeDate(date: LocalDate, abbreviateMonth: Boolean = false): String {
        val now = LocalDate.now()

        return when (date) {
            now -> {
                strings.getString(R.string.today)
            }
            now.plusDays(1) -> {
                strings.getString(R.string.tomorrow)
            }
            now.minusDays(1) -> {
                strings.getString(R.string.yesterday)
            }
            else -> {
                DateUtils.formatDateTime(
                    context,
                    date.atStartOfDay().toEpochMillis(),
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_RELATIVE or (if (abbreviateMonth) DateUtils.FORMAT_ABBREV_MONTH else 0)
                )
            }
        }
    }

    fun formatTime(
        time: ZonedDateTime,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true
    ): String {
        return formatTime(time.toLocalTime(), includeSeconds, includeMinutes)
    }

    fun formatTime(
        time: Instant,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true
    ): String {
        return formatTime(time.toZonedDateTime(), includeSeconds, includeMinutes)
    }

    fun formatTime(
        time: LocalTime,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true
    ): String {
        val amPm = !prefs.use24HourTime
        val lZero = prefs.addLeadingZeroToTime
        return if (amPm) {
            time.format(DateTimeFormatter.ofPattern("${if (lZero) "h" else ""}h${if (includeMinutes) ":mm" else ""}${if (includeSeconds) ":ss" else ""} a"))
        } else {
            time.format(DateTimeFormatter.ofPattern("${if (lZero) "H" else ""}H${if (includeMinutes) ":mm" else ""}${if (includeSeconds) ":ss" else ""}"))
        }
    }

    fun formatMonth(
        month: Month,
        short: Boolean = false
    ): String {
        return month.getDisplayName(
            if (short) TextStyle.SHORT else TextStyle.FULL,
            Locale.getDefault()
        )
    }

    fun formatDate(
        date: ZonedDateTime,
        includeWeekDay: Boolean = true,
        abbreviateMonth: Boolean = false
    ): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_DATE or (if (includeWeekDay) DateUtils.FORMAT_SHOW_WEEKDAY else 0) or DateUtils.FORMAT_SHOW_YEAR or (if (abbreviateMonth) DateUtils.FORMAT_ABBREV_MONTH else 0)
        )
    }

    fun formatDateTime(
        dateTime: ZonedDateTime,
        relative: Boolean = false,
        abbreviateMonth: Boolean = false
    ): String {
        val date = if (relative) formatRelativeDate(
            dateTime.toLocalDate(),
            abbreviateMonth = abbreviateMonth
        ) else formatDate(
            dateTime,
            false,
            abbreviateMonth
        )
        val time = formatTime(dateTime.toLocalTime(), false)
        return "$date $time"
    }

    fun formatTimeSpan(
        start: ZonedDateTime,
        end: ZonedDateTime,
        relative: Boolean = false
    ): String {
        val startTime = start.toLocalTime()
        val endTime = end.toLocalTime()

        val dateFormatFn =
            if (relative) { date: ZonedDateTime ->
                formatRelativeDate(date.toLocalDate())
            } else { date: ZonedDateTime ->
                formatDate(date)
            }

        return if (start.toLocalDate() == end.toLocalDate()) {
            strings.getString(
                R.string.dash_separated_pair, "${dateFormatFn(start)} ${
                    formatTime(
                        startTime,
                        false
                    )
                }", formatTime(endTime, false)
            )
        } else {
            strings.getString(
                R.string.dash_separated_pair, "${dateFormatFn(start)} ${
                    formatTime(
                        startTime,
                        false
                    )
                }", "${dateFormatFn(end)} ${formatTime(endTime, false)}"
            )
        }

    }

    fun formatDistance(
        distance: Distance,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(distance.distance, decimalPlaces, strict)
        return when (distance.units) {
            DistanceUnits.Meters -> strings.getString(R.string.precise_meters_format, formatted)
            DistanceUnits.Kilometers -> strings.getString(
                R.string.precise_kilometers_format,
                formatted
            )
            DistanceUnits.Feet -> strings.getString(R.string.precise_feet_format, formatted)
            DistanceUnits.Miles -> strings.getString(R.string.precise_miles_format, formatted)
            DistanceUnits.NauticalMiles -> strings.getString(
                R.string.precise_nautical_miles_format,
                formatted
            )
            DistanceUnits.Inches -> strings.getString(R.string.precise_inches_format, formatted)
            DistanceUnits.Centimeters -> strings.getString(
                R.string.precise_centimeters_format,
                formatted
            )
            DistanceUnits.Yards -> strings.getString(R.string.yards_format, formatted)
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
            TimeUnits.Milliseconds -> strings.getString(R.string.milliseconds_format, formatted)
            TimeUnits.Seconds -> strings.getString(R.string.seconds_format, formatted)
            TimeUnits.Minutes -> strings.getString(R.string.minutes_format, formatted)
            TimeUnits.Hours -> strings.getString(R.string.hours_format, formatted)
            TimeUnits.Days -> strings.getString(R.string.days_format, formatted)
        }
    }

    fun formatVolume(volume: Volume, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(volume.volume, decimalPlaces, strict)
        return when (volume.units) {
            VolumeUnits.Liters -> strings.getString(R.string.liter_format, formatted)
            VolumeUnits.Milliliter -> strings.getString(R.string.milliliter_format, formatted)
            VolumeUnits.USCups -> strings.getString(R.string.cup_format, formatted)
            VolumeUnits.USPints -> strings.getString(R.string.pint_format, formatted)
            VolumeUnits.USQuarts -> strings.getString(R.string.quart_format, formatted)
            VolumeUnits.USOunces -> strings.getString(R.string.ounces_volume_format, formatted)
            VolumeUnits.USGallons -> strings.getString(R.string.gallon_format, formatted)
            VolumeUnits.ImperialCups -> strings.getString(R.string.cup_format, formatted)
            VolumeUnits.ImperialPints -> strings.getString(R.string.pint_format, formatted)
            VolumeUnits.ImperialQuarts -> strings.getString(R.string.quart_format, formatted)
            VolumeUnits.ImperialOunces -> strings.getString(
                R.string.ounces_volume_format,
                formatted
            )
            VolumeUnits.ImperialGallons -> strings.getString(R.string.gallon_format, formatted)
        }
    }

    fun formatWeight(weight: Weight, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(weight.weight, decimalPlaces, strict)
        return when (weight.units) {
            WeightUnits.Pounds -> strings.getString(R.string.pounds_format, formatted)
            WeightUnits.Ounces -> strings.getString(R.string.ounces_weight_format, formatted)
            WeightUnits.Kilograms -> strings.getString(R.string.kilograms_format, formatted)
            WeightUnits.Grams -> strings.getString(R.string.grams_format, formatted)
        }
    }

    fun formatDbm(dbm: Int): String {
        return strings.getString(R.string.dbm_format, dbm.toString())
    }

    fun formatPercentage(percent: Float, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(percent, decimalPlaces, strict)
        return strings.getString(R.string.precise_percent_format, formatted)
    }

    fun formatBatteryHealth(batteryHealth: BatteryHealth): String {
        return when (batteryHealth) {
            BatteryHealth.Cold -> strings.getString(R.string.battery_health_cold)
            BatteryHealth.Dead -> strings.getString(R.string.battery_health_dead)
            BatteryHealth.Good -> strings.getString(R.string.quality_good)
            BatteryHealth.Overheat -> strings.getString(R.string.battery_health_overheat)
            BatteryHealth.OverVoltage -> strings.getString(R.string.battery_health_over_voltage)
            BatteryHealth.Unknown -> strings.getString(R.string.unknown)
        }
    }

    fun formatTemperature(
        temperature: Temperature,
        decimalPlaces: Int = 0,
        strict: Boolean = true
    ): String {
        val formatted = DecimalFormatter.format(temperature.temperature, decimalPlaces, strict)
        return when (temperature.units) {
            TemperatureUnits.F -> strings.getString(R.string.precise_temp_f_format, formatted)
            TemperatureUnits.C -> strings.getString(R.string.precise_temp_c_format, formatted)
        }
    }

    fun formatDegrees(
        degrees: Float,
        decimalPlaces: Int = 0,
        replace360: Boolean = false
    ): String {
        val formatted = DecimalFormatter.format(degrees.toDouble(), decimalPlaces)
        val finalFormatted = if (replace360) formatted.replace("360", "0") else formatted
        return strings.getString(R.string.degree, finalFormatted)
    }

    fun formatDirection(direction: CompassDirection): String {
        return when (direction) {
            CompassDirection.North -> strings.getString(R.string.direction_north)
            CompassDirection.South -> strings.getString(R.string.direction_south)
            CompassDirection.East -> strings.getString(R.string.direction_east)
            CompassDirection.West -> strings.getString(R.string.direction_west)
            CompassDirection.NorthEast -> strings.getString(R.string.direction_north_east)
            CompassDirection.SouthEast -> strings.getString(R.string.direction_south_east)
            CompassDirection.NorthWest -> strings.getString(R.string.direction_north_west)
            CompassDirection.SouthWest -> strings.getString(R.string.direction_south_west)
        }
    }

    fun formatDays(days: Int): String {
        return context.resources.getQuantityString(R.plurals.number_days, days, days)
    }

    fun formatDuration(
        duration: Duration,
        short: Boolean = false,
        includeSeconds: Boolean = false
    ): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        val h = strings.getString(R.string.duration_hour_format, hours)
        val m = strings.getString(R.string.duration_minute_format, minutes)
        val s = strings.getString(R.string.duration_second_format, seconds)

        val strs = mutableListOf<String>()

        if (hours > 0) {
            strs.add(h)
        }

        if (minutes > 0 && (!short || hours == 0L)) {
            strs.add(m)
        }

        if (!(strs.size > 0 && seconds == 0L) && seconds >= 0 && includeSeconds && (!short || (hours == 0L && minutes == 0L))) {
            strs.add(s)
        }

        if (strs.isEmpty()) {
            strs.add(m)
        }

        return strs.joinToString(" ")
    }

    fun formatAcceleration(acceleration: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(acceleration.toDouble(), decimalPlaces)
        return strings.getString(R.string.acceleration_m_s2_format, formatted)
    }

    fun formatMagneticField(magneticField: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(magneticField.toDouble(), decimalPlaces)
        return strings.getString(R.string.magnetic_field_format_precise, formatted)
    }

    fun formatQuality(quality: Quality): String {
        return when (quality) {
            Quality.Poor -> strings.getString(R.string.quality_poor)
            Quality.Moderate -> strings.getString(R.string.moderate)
            Quality.Good -> strings.getString(R.string.quality_good)
            else -> strings.getString(R.string.unknown)
        }
    }

    fun formatPressure(pressure: Pressure, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val symbol = getPressureUnitString(pressure.units)
        val amt = DecimalFormatter.format(pressure.pressure, decimalPlaces, strict)
        return strings.getString(R.string.pressure_format, amt, symbol)
    }

    // TODO: Rewrite this
    fun formatSpeed(metersPerSecond: Float): String {
        val distanceUnits = prefs.distanceUnits
        val convertedSpeed = LocationMath.convertToBaseSpeed(metersPerSecond, distanceUnits)
        return if (distanceUnits == UserPreferences.DistanceUnits.Meters) {
            strings.getString(R.string.kilometers_per_hour_format, convertedSpeed)
        } else {
            strings.getString(R.string.miles_per_hour_format, convertedSpeed)
        }
    }

    fun formatLocation(
        location: Coordinate,
        format: CoordinateFormat? = null,
        fallbackToDD: Boolean = true
    ): String {
        val formatted = when (format ?: prefs.navigation.coordinateFormat) {
            CoordinateFormat.DecimalDegrees -> location.toDecimalDegrees()
            CoordinateFormat.DegreesDecimalMinutes -> location.toDegreeDecimalMinutes()
            CoordinateFormat.DegreesMinutesSeconds -> location.toDegreeMinutesSeconds()
            CoordinateFormat.UTM -> location.toUTM()
            CoordinateFormat.MGRS -> location.toMGRS()
            CoordinateFormat.USNG -> location.toUSNG()
            CoordinateFormat.OSGB -> location.toOSGB()
        }
        if (formatted == "?" && fallbackToDD) {
            return location.toDecimalDegrees()
        }
        return formatted
    }

    fun formatCoordinateType(type: CoordinateFormat): String {
        return when (type) {
            CoordinateFormat.DecimalDegrees -> strings.getString(R.string.coordinate_format_decimal_degrees)
            CoordinateFormat.DegreesDecimalMinutes -> strings.getString(R.string.coordinate_format_degrees_decimal_minutes)
            CoordinateFormat.DegreesMinutesSeconds -> strings.getString(R.string.coordinate_format_degrees_minutes_seconds)
            CoordinateFormat.UTM -> strings.getString(R.string.coordinate_format_utm)
            CoordinateFormat.MGRS -> strings.getString(R.string.coordinate_format_mgrs)
            CoordinateFormat.USNG -> strings.getString(R.string.coordinate_format_usng)
            CoordinateFormat.OSGB -> strings.getString(R.string.coordinate_format_osgb)
        }
    }

    fun formatElectricalCapacity(capacity: Float): String {
        return strings.getString(
            R.string.battery_capacity_format,
            DecimalFormatter.format(capacity, 0)
        )
    }

    fun formatCurrent(current: Float, decimalPlaces: Int = 0): String {
        return strings.getString(
            R.string.current_format,
            DecimalFormatter.format(current, decimalPlaces)
        )
    }

    fun formatLux(lux: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(lux.toDouble(), decimalPlaces)
        return strings.getString(R.string.lux_format, formatted)
    }

    fun formatCandela(candela: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(candela.toDouble(), decimalPlaces)
        return strings.getString(R.string.candela_format, formatted)
    }

    fun formatSolarEnergy(energy: Float, decimalPlaces: Int = 1, strict: Boolean = false): String {
        val formatted = DecimalFormatter.format(energy, decimalPlaces, strict)
        return strings.getString(R.string.kwh_per_meter_squared_format, formatted)
    }

    fun formatFileSize(bytes: Long, short: Boolean = true): String {
        return if (short) {
            Formatter.formatShortFileSize(context, bytes)
        } else {
            Formatter.formatFileSize(context, bytes)
        }
    }

    fun sortDistanceUnits(
        units: List<DistanceUnits>,
        metric: Boolean = prefs.baseDistanceUnits == DistanceUnits.Meters
    ): List<DistanceUnits> {
        // TODO: Secondary sort by size
        val metricUnits =
            listOf(DistanceUnits.Centimeters, DistanceUnits.Meters, DistanceUnits.Kilometers)
        return units.sortedBy {
            if (metric) {
                if (metricUnits.contains(it)) 0 else 1
            } else {
                if (metricUnits.contains(it)) 1 else 0
            }
        }
    }

    fun sortWeightUnits(
        units: List<WeightUnits>,
        metric: Boolean = prefs.weightUnits == WeightUnits.Kilograms
    ): List<WeightUnits> {
        // TODO: Secondary sort by size
        val metricUnits =
            listOf(WeightUnits.Grams, WeightUnits.Kilograms)
        return units.sortedBy {
            if (metric) {
                if (metricUnits.contains(it)) 0 else 1
            } else {
                if (metricUnits.contains(it)) 1 else 0
            }
        }
    }

    fun getWeightUnitName(unit: WeightUnits, short: Boolean = false): String {
        if (short) {
            return when (unit) {
                WeightUnits.Pounds -> strings.getString(R.string.pounds_format, "")
                WeightUnits.Ounces -> strings.getString(R.string.ounces_weight_format, "")
                WeightUnits.Kilograms -> strings.getString(R.string.kilograms_format, "")
                WeightUnits.Grams -> strings.getString(R.string.grams_format, "")
            }.replace(" ", "")
        }
        return when (unit) {
            WeightUnits.Pounds -> strings.getString(R.string.pounds)
            WeightUnits.Ounces -> strings.getString(R.string.ounces_weight)
            WeightUnits.Kilograms -> strings.getString(R.string.kilograms)
            WeightUnits.Grams -> strings.getString(R.string.grams)
        }
    }

    fun getTemperatureUnitName(unit: TemperatureUnits, short: Boolean = false): String {
        return if (short) {
            when (unit) {
                TemperatureUnits.F -> strings.getString(R.string.temp_f_short)
                TemperatureUnits.C -> strings.getString(R.string.temp_c_short)
            }
        } else {
            when (unit) {
                TemperatureUnits.F -> strings.getString(R.string.fahrenheit)
                TemperatureUnits.C -> strings.getString(R.string.celsius)
            }
        }
    }

    fun getDistanceUnitName(unit: DistanceUnits, short: Boolean = false): String {
        if (short) {
            return when (unit) {
                DistanceUnits.Meters -> strings.getString(R.string.precise_meters_format, "")
                DistanceUnits.Kilometers -> strings.getString(
                    R.string.precise_kilometers_format,
                    ""
                )
                DistanceUnits.Feet -> strings.getString(R.string.precise_feet_format, "")
                DistanceUnits.Miles -> strings.getString(R.string.precise_miles_format, "")
                DistanceUnits.NauticalMiles -> strings.getString(
                    R.string.precise_nautical_miles_format,
                    ""
                )
                DistanceUnits.Centimeters -> strings.getString(
                    R.string.precise_centimeters_format,
                    ""
                )
                DistanceUnits.Inches -> strings.getString(R.string.precise_inches_format, "")
                DistanceUnits.Yards -> strings.getString(R.string.yards_format, "")
            }.replace(" ", "")
        }
        return when (unit) {
            DistanceUnits.Meters -> strings.getString(R.string.unit_meters)
            DistanceUnits.Kilometers -> strings.getString(R.string.unit_kilometers)
            DistanceUnits.Feet -> strings.getString(R.string.unit_feet)
            DistanceUnits.Miles -> strings.getString(R.string.unit_miles)
            DistanceUnits.NauticalMiles -> strings.getString(R.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> strings.getString(R.string.unit_centimeters)
            DistanceUnits.Inches -> strings.getString(R.string.unit_inches)
            DistanceUnits.Yards -> strings.getString(R.string.unit_yards)
        }
    }

    fun formatCellNetwork(cellType: CellNetwork?): String {
        return when (cellType) {
            CellNetwork.Nr -> strings.getString(R.string.network_5g)
            CellNetwork.Lte -> strings.getString(R.string.network_4g)
            CellNetwork.Cdma, CellNetwork.Gsm -> strings.getString(R.string.network_2g)
            CellNetwork.Wcdma -> strings.getString(R.string.network_3g)
            else -> strings.getString(R.string.network_no_signal)
        }
    }

    fun formatMoonPhase(phase: MoonTruePhase): String {
        return strings.getString(
            when (phase) {
                MoonTruePhase.FirstQuarter -> R.string.first_quarter
                MoonTruePhase.Full -> R.string.full_moon
                MoonTruePhase.ThirdQuarter -> R.string.third_quarter
                MoonTruePhase.New -> R.string.new_moon
                MoonTruePhase.WaningCrescent -> R.string.waning_crescent
                MoonTruePhase.WaningGibbous -> R.string.waning_gibbous
                MoonTruePhase.WaxingCrescent -> R.string.waxing_crescent
                MoonTruePhase.WaxingGibbous -> R.string.waxing_gibbous
            }
        )
    }

    fun formatSeason(season: Season): String {
        return when (season) {
            Season.Winter -> strings.getString(R.string.season_winter)
            Season.Spring -> strings.getString(R.string.season_spring)
            Season.Summer -> strings.getString(R.string.season_summer)
            Season.Fall -> strings.getString(R.string.season_fall)
        }
    }


    private fun getPressureUnitString(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> strings.getString(R.string.units_hpa)
            PressureUnits.Mbar -> strings.getString(R.string.units_mbar)
            PressureUnits.Inhg -> strings.getString(R.string.units_inhg_short)
            PressureUnits.Psi -> strings.getString(R.string.units_psi)
            PressureUnits.MmHg -> strings.getString(R.string.units_mmhg_short)
        }
    }

    fun formatPrecipitation(precipitation: Precipitation): String {
        return when (precipitation) {
            Precipitation.Rain -> strings.getString(R.string.precipitation_rain)
            Precipitation.Drizzle -> strings.getString(R.string.precipitation_drizzle)
            Precipitation.Snow -> strings.getString(R.string.precipitation_snow)
            Precipitation.SnowPellets -> strings.getString(R.string.precipitation_snow_pellets)
            Precipitation.Hail -> strings.getString(R.string.precipitation_hail)
            Precipitation.SmallHail -> strings.getString(R.string.precipitation_small_hail)
            Precipitation.IcePellets -> strings.getString(R.string.precipitation_ice_pellets)
            Precipitation.SnowGrains -> strings.getString(R.string.precipitation_snow_grains)
            Precipitation.Lightning -> strings.getString(R.string.lightning)
        }
    }

    fun formatHikingDifficulty(difficulty: HikingDifficulty): String {
        return when (difficulty) {
            HikingDifficulty.Easiest -> strings.getString(R.string.easy)
            HikingDifficulty.Moderate, HikingDifficulty.ModeratelyStrenuous -> strings.getString(R.string.moderate)
            else -> strings.getString(R.string.hard)
        }
    }

    fun formatMapProjection(projection: MapProjectionType): String {
        return when (projection) {
            MapProjectionType.Mercator -> strings.getString(R.string.map_projection_mercator)
            MapProjectionType.CylindricalEquidistant -> strings.getString(R.string.map_projection_equidistant)
        }
    }

    companion object {
        private var instance: FormatService? = null

        @Synchronized
        fun getInstance(context: Context): FormatService {
            if (instance == null) {
                instance = FormatService(context.applicationContext)
            }
            return instance!!
        }
    }

}