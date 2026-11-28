package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.LocalClimate
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sin

internal class WeatherModel(
    private val climate: LocalClimate,
    private val location: Coordinate,
    private val forecastSensitivity: ForecastSensitivity = ForecastSensitivity.Medium,
    private val stormSensitivity: ForecastSensitivity = ForecastSensitivity.Medium
) {

    /**
     * @param pressures sea level pressures with the atmospheric tide removed, oldest first
     * @return null when the pressure history is too short and no sky is logged
     */
    fun predict(
        pressures: List<Reading<Pressure>>,
        sky: LoggedSky = LoggedSky(null, null),
        now: Instant = pressures.lastOrNull()?.time ?: Instant.now()
    ): WeatherModelPrediction? {
        val pressureFeatures = getPressureFeatures(pressures)
        if (pressureFeatures == null && !sky.isLogged) {
            return null
        }
        val time = if (pressureFeatures != null) pressures.last().time else now
        val features = getClimateFeatures(time) + getSkyFeatures(sky) + (pressureFeatures ?: NO_PRESSURE_FEATURES)
        val probabilities = network.predict(features)
        return WeatherModelPrediction(
            getConditions(probabilities),
            getLaterConditions(probabilities),
            probabilities.getValue(WeatherOutput.SnowNext12h),
            probabilities.getValue(WeatherOutput.Snow12hTo24h),
            isPredicted(probabilities, WeatherOutput.SnowNext12h),
            isPredicted(probabilities, WeatherOutput.Snow12hTo24h),
            isStormAlert(probabilities)
        )
    }

    private fun getConditions(probabilities: Map<WeatherOutput, Float>): List<ForecastCondition> {
        val sky = getSkyCondition(
            probabilities,
            WeatherOutput.ClearNext3h,
            WeatherOutput.OvercastNext3h
        ) ?: getSkyCondition(
            probabilities,
            WeatherOutput.Clear3hTo12h,
            WeatherOutput.Overcast3hTo12h,
            soonClear = WeatherOutput.ClearNext3h,
            soonOvercast = WeatherOutput.OvercastNext3h
        )
        val weather = EVENTS.mapNotNull { (condition, soon, later) ->
            getCondition(probabilities, condition, soon, RelativeArrivalTime.Soon)
                ?: getCondition(probabilities, condition, later, RelativeArrivalTime.Later, soon)
        }
        val thunderstorm = getCondition(
            probabilities,
            WeatherCondition.Thunderstorm,
            WeatherOutput.ThunderstormNext12h,
            arrival = null
        )
        return listOfNotNull(sky) + weather + listOfNotNull(thunderstorm)
    }

    private fun getLaterConditions(probabilities: Map<WeatherOutput, Float>): List<ForecastCondition> {
        val sky = getSkyCondition(
            probabilities,
            WeatherOutput.Clear12hTo24h,
            WeatherOutput.Overcast12hTo24h
        )?.copy(arrival = null)
        val precipitation = getCondition(
            probabilities,
            WeatherCondition.Precipitation,
            WeatherOutput.Precipitation12hTo24h,
            arrival = null
        )
        return listOfNotNull(sky, precipitation)
    }

    /**
     * @param soon the next 3 hours output when [output] is a 3 to 12 hour output. The 3 to 12 hour output is the
     * chance the condition starts then, so the two add up to the chance of it within 12 hours.
     */
    private fun getCondition(
        probabilities: Map<WeatherOutput, Float>,
        condition: WeatherCondition,
        output: WeatherOutput,
        arrival: RelativeArrivalTime?,
        soon: WeatherOutput? = null
    ): ForecastCondition? {
        if (!isPredicted(probabilities, output)) {
            return null
        }
        val soonProbability = soon?.let { probabilities.getValue(it) } ?: 0f
        return ForecastCondition(
            condition,
            (probabilities.getValue(output) + soonProbability).coerceAtMost(1f),
            arrival,
            getRelativeProbability(probabilities, output)
        )
    }

    private fun getSkyCondition(
        probabilities: Map<WeatherOutput, Float>,
        clear: WeatherOutput,
        overcast: WeatherOutput,
        soonClear: WeatherOutput? = null,
        soonOvercast: WeatherOutput? = null
    ): ForecastCondition? {
        val clearMargin = getMargin(probabilities, clear)
        val overcastMargin = getMargin(probabilities, overcast)
        return when {
            clearMargin >= 0 && clearMargin > overcastMargin -> getCondition(
                probabilities,
                WeatherCondition.Clear,
                clear,
                getArrival(soonClear),
                soonClear
            )

            overcastMargin >= 0 -> getCondition(
                probabilities,
                WeatherCondition.Overcast,
                overcast,
                getArrival(soonOvercast),
                soonOvercast
            )

            else -> null
        }
    }

    private fun getArrival(soon: WeatherOutput?): RelativeArrivalTime {
        return if (soon == null) RelativeArrivalTime.Soon else RelativeArrivalTime.Later
    }

    private fun isStormAlert(probabilities: Map<WeatherOutput, Float>): Boolean {
        val chance = probabilities.getValue(WeatherOutput.StormNext3h) +
                probabilities.getValue(WeatherOutput.Storm3hTo12h)
        return chance >= WeatherNetworkCoefficients.stormAlertThresholds.get(stormSensitivity)
    }

    private fun isPredicted(probabilities: Map<WeatherOutput, Float>, output: WeatherOutput): Boolean {
        return getMargin(probabilities, output) >= 0
    }

    /**
     * @return the log odds of the output above its threshold, positive when the condition is predicted
     */
    private fun getMargin(probabilities: Map<WeatherOutput, Float>, output: WeatherOutput): Float {
        return logit(probabilities.getValue(output)) - logit(output.thresholds.get(forecastSensitivity))
    }

    private fun getRelativeProbability(probabilities: Map<WeatherOutput, Float>, output: WeatherOutput): Float {
        return probabilities.getValue(output) / output.thresholds.medium
    }

    private fun logit(probability: Float): Float {
        val p = probability.coerceIn(MIN_PROBABILITY, 1 - MIN_PROBABILITY)
        return ln(p / (1 - p))
    }

    private fun getPressureFeatures(pressures: List<Reading<Pressure>>): Map<String, Float>? {
        val current = pressures.lastOrNull() ?: return null
        val hasOldEnoughReading = pressures.any { Duration.between(it.time, current.time) >= MINIMUM_HISTORY }
        if (!hasOldEnoughReading) {
            return null
        }
        val recent = pressures.filter { Duration.between(it.time, current.time) <= HISTORY_WINDOW }
        return mapOf(
            "pressure_from_standard" to current.value.hpa().value - STANDARD_PRESSURE_HPA,
            "has_pressure" to 1f
        ) +
                DIFFERENCE_HOURS.associate { "pressure_difference_${it}h" to getPressureDifference(recent, it) }
    }

    private fun getClimateFeatures(now: Instant): Map<String, Float> {
        val time = now.atZone(ZoneOffset.UTC)
        val monthPrecipitation = climate.monthlyPrecipitation[time.month]?.millimeters() ?: 0f
        val annualPrecipitation = climate.monthlyPrecipitation.values.sumOf { it.millimeters().toDouble() }.toFloat()
        val solarHour = ((time.hour + time.minute / 60f + location.longitude.toFloat() / 15f) % 24 + 24) % 24
        val seasonDay = time.dayOfYear + if (location.latitude < 0) SOUTHERN_SEASON_OFFSET_DAYS else 0f
        val season = 2 * PI.toFloat() * seasonDay / DAYS_PER_YEAR

        return mapOf(
            "solar_hour_sine" to sin(2 * PI.toFloat() * solarHour / 24),
            "solar_hour_cosine" to cos(2 * PI.toFloat() * solarHour / 24),
            "season_sine" to sin(season),
            "season_cosine" to cos(season),
            "absolute_latitude" to abs(location.latitude.toFloat()),
            "low_temperature" to climate.dailyTemperatures.start.celsius().value,
            "high_temperature" to climate.dailyTemperatures.end.celsius().value,
            "log_monthly_precipitation" to ln(1 + monthPrecipitation),
            "log_annual_precipitation" to ln(1 + annualPrecipitation)
        )
    }

    private fun getSkyFeatures(sky: LoggedSky): Map<String, Float> {
        return getSkyFeatures("recent", sky.recent) + getSkyFeatures("earlier", sky.earlier)
    }

    private fun getSkyFeatures(window: String, log: SkyLog?): Map<String, Float> {
        val covers = SKY_COVER_NAMES.entries.associate { (cover, name) ->
            "${window}_${name}_sky" to if (log?.cover == cover) 1f else 0f
        }
        val clouds = CloudGenus.entries.associate {
            "${window}_${it.name.lowercase()}" to if (log?.clouds?.contains(it) == true) 1f else 0f
        }
        return covers + clouds
    }

    private fun Distance.millimeters(): Float {
        return meters().value * 1000
    }

    companion object {
        private val network by lazy { WeatherNetwork() }
        private val SKY_COVER_NAMES = mapOf(
            SkyCover.Clear to "clear",
            SkyCover.PartlyCloudy to "partly_cloudy",
            SkyCover.MostlyCloudy to "mostly_cloudy"
        )
        private const val TENDENCY_HOURS = 3
        private const val TENDENCY_CHANGE_HPA = 1.5f
        private const val TENDENCY_FAST_CHANGE_HPA = 3.5f
        private const val STANDARD_PRESSURE_HPA = 1013f
        private const val MIN_PROBABILITY = 1e-6f
        private val EVENTS = listOf(
            Triple(WeatherCondition.Precipitation, WeatherOutput.PrecipitationNext3h, WeatherOutput.Precipitation3hTo12h),
            Triple(WeatherCondition.Storm, WeatherOutput.StormNext3h, WeatherOutput.Storm3hTo12h),
            Triple(WeatherCondition.Wind, WeatherOutput.WindNext3h, WeatherOutput.Wind3hTo12h)
        )
        private val HISTORY_WINDOW = Duration.ofHours(48)
        private val MINIMUM_HISTORY = Duration.ofHours(2)
        private const val SOUTHERN_SEASON_OFFSET_DAYS = 182.5f
        private const val DAYS_PER_YEAR = 365.25f
        private const val MILLIS_PER_HOUR = 3_600_000f
        private val DIFFERENCE_HOURS = listOf(1, 2, 3, 4, 6, 8, 10, 12, 15, 18, 21, 24, 30, 36, 42, 48)
        private val NO_PRESSURE_FEATURES = mapOf("pressure_from_standard" to 0f, "has_pressure" to 0f) +
                DIFFERENCE_HOURS.associate { "pressure_difference_${it}h" to 0f }

        /**
         * @param pressures sea level pressures with the atmospheric tide removed, oldest first
         * @return the change over the past 3 hours, measured as the network measures it, or the rate of change over
         * a shorter history scaled to 3 hours
         */
        fun getTendency(pressures: List<Reading<Pressure>>): PressureTendency {
            val current = pressures.lastOrNull() ?: return PressureTendency.zero
            val hours = min(
                TENDENCY_HOURS.toFloat(),
                Duration.between(pressures.first().time, current.time).toMillis() / MILLIS_PER_HOUR
            )
            if (hours <= 0f) {
                return PressureTendency.zero
            }
            val change = getPressureDifference(pressures, TENDENCY_HOURS) * TENDENCY_HOURS / hours
            val characteristic = when {
                change <= -TENDENCY_FAST_CHANGE_HPA -> PressureCharacteristic.FallingFast
                change <= -TENDENCY_CHANGE_HPA -> PressureCharacteristic.Falling
                change >= TENDENCY_FAST_CHANGE_HPA -> PressureCharacteristic.RisingFast
                change >= TENDENCY_CHANGE_HPA -> PressureCharacteristic.Rising
                else -> PressureCharacteristic.Steady
            }
            return PressureTendency(characteristic, change)
        }

        /**
         * @param pressures readings oldest first
         * @return the current pressure minus the pressure [hours] ago, interpolated between readings, or from the
         * oldest reading when the history is shorter
         */
        private fun getPressureDifference(pressures: List<Reading<Pressure>>, hours: Int): Float {
            val current = pressures.last()
            val time = current.time.minus(Duration.ofHours(hours.toLong()))
            val nextIndex = pressures.indexOfFirst { !it.time.isBefore(time) }
            val past = if (nextIndex <= 0) {
                pressures.first().value.hpa().value
            } else {
                val before = pressures[nextIndex - 1]
                val after = pressures[nextIndex]
                val span = Duration.between(before.time, after.time).toMillis().toFloat()
                val fraction = Duration.between(before.time, time).toMillis() / span
                before.value.hpa().value + (after.value.hpa().value - before.value.hpa().value) * fraction
            }
            return current.value.hpa().value - past
        }
    }
}
