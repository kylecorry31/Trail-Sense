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
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toOSNG
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toUSNG
import com.kylecorry.andromeda.core.units.CoordinateExtensions.toUTM
import com.kylecorry.andromeda.core.units.CoordinateFormat
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.time.Time.toEpochMillis
import com.kylecorry.sol.units.*
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.LocationMath
import com.kylecorry.trail_sense.navigation.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.shared.domain.Probability
import com.kylecorry.trail_sense.tools.maps.domain.MapProjectionType
import com.kylecorry.trail_sense.weather.domain.HourlyArrivalTime
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class FormatService(private val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    @DrawableRes
    fun getWeatherImage(condition: WeatherCondition?): Int {
        return when (condition) {
            WeatherCondition.Clear -> R.drawable.sunny
            WeatherCondition.Overcast -> R.drawable.cloudy
            WeatherCondition.Precipitation -> R.drawable.light_rain // TODO: Change this
            WeatherCondition.Storm -> R.drawable.storm
            WeatherCondition.Wind -> R.drawable.wind
            WeatherCondition.Rain -> R.drawable.light_rain
            WeatherCondition.Snow -> R.drawable.ic_precipitation_snow
            null -> R.drawable.steady
        }
    }

    fun formatWeather(condition: WeatherCondition?): String {
        return when (condition) {
            WeatherCondition.Clear -> context.getString(R.string.weather_clear)
            WeatherCondition.Overcast -> context.getString(R.string.weather_overcast)
            WeatherCondition.Precipitation -> context.getString(R.string.weather_precipitation)
            WeatherCondition.Storm -> context.getString(R.string.weather_storm)
            WeatherCondition.Wind -> context.getString(R.string.weather_wind)
            WeatherCondition.Rain -> context.getString(R.string.precipitation_rain)
            WeatherCondition.Snow -> context.getString(R.string.precipitation_snow)
            null -> context.getString(R.string.weather_no_change)
        }
    }

    fun formatWeatherArrival(speed: HourlyArrivalTime?): String {
        return when (speed) {
            HourlyArrivalTime.Now -> context.getString(R.string.now)
            HourlyArrivalTime.VerySoon -> context.getString(R.string.very_soon)
            HourlyArrivalTime.Soon -> context.getString(R.string.soon)
            HourlyArrivalTime.Later -> context.getString(R.string.later)
            null -> ""
        }
    }

    fun formatProbability(probability: Probability): String {
        return when (probability) {
            Probability.Never -> context.getString(R.string.never)
            Probability.Low -> context.getString(R.string.low)
            Probability.Moderate -> context.getString(R.string.moderate)
            Probability.High -> context.getString(R.string.high)
            Probability.Always -> context.getString(R.string.always)
        }
    }

    fun formatRelativeDate(date: LocalDate, abbreviateMonth: Boolean = false): String {
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
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_RELATIVE or (if (abbreviateMonth) DateUtils.FORMAT_ABBREV_MONTH else 0)
                )
            }
        }
    }

    fun formatTime(
        time: LocalTime,
        includeSeconds: Boolean = true,
        includeMinutes: Boolean = true
    ): String {
        val amPm = !prefs.use24HourTime
        return if (amPm) {
            time.format(DateTimeFormatter.ofPattern("h${if (includeMinutes) ":mm" else ""}${if (includeSeconds) ":ss" else ""} a"))
        } else {
            time.format(DateTimeFormatter.ofPattern("H${if (includeMinutes) ":mm" else ""}${if (includeSeconds) ":ss" else ""}"))
        }
    }

    fun formatMonth(
        month: Month,
        short: Boolean = false
    ): String {
        return month.getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, Locale.getDefault())
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
            context.getString(
                R.string.dash_separated_pair, "${dateFormatFn(start)} ${
                    formatTime(
                        startTime,
                        false
                    )
                }", formatTime(endTime, false)
            )
        } else {
            context.getString(
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

    fun formatPercentage(percent: Float, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val formatted = DecimalFormatter.format(percent, decimalPlaces, strict)
        return context.getString(R.string.precise_percent_format, formatted)
    }

    fun formatBatteryHealth(batteryHealth: BatteryHealth): String {
        return when (batteryHealth) {
            BatteryHealth.Cold -> context.getString(R.string.battery_health_cold)
            BatteryHealth.Dead -> context.getString(R.string.battery_health_dead)
            BatteryHealth.Good -> context.getString(R.string.quality_good)
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

    fun formatDegrees(
        degrees: Float,
        decimalPlaces: Int = 0,
        replace360: Boolean = false
    ): String {
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

    fun formatDuration(
        duration: Duration,
        short: Boolean = false,
        includeSeconds: Boolean = false
    ): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        val h = context.getString(R.string.duration_hour_format, hours)
        val m = context.getString(R.string.duration_minute_format, minutes)
        val s = context.getString(R.string.duration_second_format, seconds)

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
        return context.getString(R.string.acceleration_m_s2_format, formatted)
    }

    fun formatMagneticField(magneticField: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(magneticField.toDouble(), decimalPlaces)
        return context.getString(R.string.magnetic_field_format_precise, formatted)
    }

    fun formatQuality(quality: Quality): String {
        return when (quality) {
            Quality.Poor -> context.getString(R.string.quality_poor)
            Quality.Moderate -> context.getString(R.string.moderate)
            Quality.Good -> context.getString(R.string.quality_good)
            else -> context.getString(R.string.unknown)
        }
    }

    fun formatPressure(pressure: Pressure, decimalPlaces: Int = 0, strict: Boolean = true): String {
        val symbol = getPressureUnitString(pressure.units)
        val amt = DecimalFormatter.format(pressure.pressure, decimalPlaces, strict)
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
            CoordinateFormat.OSNG_OSGB36 -> location.toOSNG()
        }
        if (formatted == "?" && fallbackToDD) {
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

    fun formatCurrent(current: Float, decimalPlaces: Int = 0): String {
        return context.getString(
            R.string.current_format,
            DecimalFormatter.format(current, decimalPlaces)
        )
    }

    fun formatLux(lux: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(lux.toDouble(), decimalPlaces)
        return context.getString(R.string.lux_format, formatted)
    }

    fun formatCandela(candela: Float, decimalPlaces: Int = 0): String {
        val formatted = DecimalFormatter.format(candela.toDouble(), decimalPlaces)
        return context.getString(R.string.candela_format, formatted)
    }

    fun formatSolarEnergy(energy: Float, decimalPlaces: Int = 1, strict: Boolean = false): String {
        val formatted = DecimalFormatter.format(energy, decimalPlaces, strict)
        return context.getString(R.string.kwh_per_meter_squared_format, formatted)
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
                WeightUnits.Pounds -> context.getString(R.string.pounds_format, "")
                WeightUnits.Ounces -> context.getString(R.string.ounces_weight_format, "")
                WeightUnits.Kilograms -> context.getString(R.string.kilograms_format, "")
                WeightUnits.Grams -> context.getString(R.string.grams_format, "")
            }.replace(" ", "")
        }
        return when (unit) {
            WeightUnits.Pounds -> context.getString(R.string.pounds)
            WeightUnits.Ounces -> context.getString(R.string.ounces_weight)
            WeightUnits.Kilograms -> context.getString(R.string.kilograms)
            WeightUnits.Grams -> context.getString(R.string.grams)
        }
    }

    fun getTemperatureUnitName(unit: TemperatureUnits, short: Boolean = false): String {
        return if (short) {
            when (unit) {
                TemperatureUnits.F -> context.getString(R.string.temp_f_short)
                TemperatureUnits.C -> context.getString(R.string.temp_c_short)
            }
        } else {
            when (unit) {
                TemperatureUnits.F -> context.getString(R.string.fahrenheit)
                TemperatureUnits.C -> context.getString(R.string.celsius)
            }
        }
    }

    fun getDistanceUnitName(unit: DistanceUnits, short: Boolean = false): String {
        if (short) {
            return when (unit) {
                DistanceUnits.Meters -> context.getString(R.string.precise_meters_format, "")
                DistanceUnits.Kilometers -> context.getString(
                    R.string.precise_kilometers_format,
                    ""
                )
                DistanceUnits.Feet -> context.getString(R.string.precise_feet_format, "")
                DistanceUnits.Miles -> context.getString(R.string.precise_miles_format, "")
                DistanceUnits.NauticalMiles -> context.getString(
                    R.string.precise_nautical_miles_format,
                    ""
                )
                DistanceUnits.Centimeters -> context.getString(
                    R.string.precise_centimeters_format,
                    ""
                )
                DistanceUnits.Inches -> context.getString(R.string.precise_inches_format, "")
                DistanceUnits.Yards -> context.getString(R.string.yards_format, "")
            }.replace(" ", "")
        }
        return when (unit) {
            DistanceUnits.Meters -> context.getString(R.string.unit_meters)
            DistanceUnits.Kilometers -> context.getString(R.string.unit_kilometers)
            DistanceUnits.Feet -> context.getString(R.string.unit_feet)
            DistanceUnits.Miles -> context.getString(R.string.unit_miles)
            DistanceUnits.NauticalMiles -> context.getString(R.string.unit_nautical_miles)
            DistanceUnits.Centimeters -> context.getString(R.string.unit_centimeters)
            DistanceUnits.Inches -> context.getString(R.string.unit_inches)
            DistanceUnits.Yards -> context.getString(R.string.unit_yards)
        }
    }

    fun formatCellNetwork(cellType: CellNetwork?): String {
        return when (cellType) {
            CellNetwork.Nr -> context.getString(R.string.network_5g)
            CellNetwork.Lte -> context.getString(R.string.network_4g)
            CellNetwork.Cdma, CellNetwork.Gsm -> context.getString(R.string.network_2g)
            CellNetwork.Wcdma -> context.getString(R.string.network_3g)
            else -> context.getString(R.string.network_no_signal)
        }
    }

    fun formatMoonPhase(phase: MoonTruePhase): String {
        return context.getString(
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
            Season.Winter -> context.getString(R.string.season_winter)
            Season.Spring -> context.getString(R.string.season_spring)
            Season.Summer -> context.getString(R.string.season_summer)
            Season.Fall -> context.getString(R.string.season_fall)
        }
    }


    private fun getPressureUnitString(unit: PressureUnits): String {
        return when (unit) {
            PressureUnits.Hpa -> context.getString(R.string.units_hpa)
            PressureUnits.Mbar -> context.getString(R.string.units_mbar)
            PressureUnits.Inhg -> context.getString(R.string.units_inhg_short)
            PressureUnits.Psi -> context.getString(R.string.units_psi)
            PressureUnits.MmHg -> context.getString(R.string.units_mmhg_short)
        }
    }

    fun formatPrecipitation(precipitation: Precipitation): String {
        return when (precipitation) {
            Precipitation.Rain -> context.getString(R.string.precipitation_rain)
            Precipitation.Drizzle -> context.getString(R.string.precipitation_drizzle)
            Precipitation.Snow -> context.getString(R.string.precipitation_snow)
            Precipitation.SnowPellets -> context.getString(R.string.precipitation_snow_pellets)
            Precipitation.Hail -> context.getString(R.string.precipitation_hail)
            Precipitation.SmallHail -> context.getString(R.string.precipitation_small_hail)
            Precipitation.IcePellets -> context.getString(R.string.precipitation_ice_pellets)
            Precipitation.SnowGrains -> context.getString(R.string.precipitation_snow_grains)
            Precipitation.Lightning -> context.getString(R.string.lightning)
        }
    }

    fun formatHikingDifficulty(difficulty: HikingDifficulty): String {
        return when (difficulty) {
            HikingDifficulty.Easiest -> context.getString(R.string.easy)
            HikingDifficulty.Moderate, HikingDifficulty.ModeratelyStrenuous -> context.getString(R.string.moderate)
            else -> context.getString(R.string.hard)
        }
    }

    fun formatMapProjection(projection: MapProjectionType): String {
        return when (projection) {
            MapProjectionType.Mercator -> context.getString(R.string.map_projection_mercator)
            MapProjectionType.CylindricalEquidistant -> context.getString(R.string.map_projection_equidistant)
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