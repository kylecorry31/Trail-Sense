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
            isPredicted(probabilities.getValue(WeatherOutput.SnowNext12h), WeatherForecast.SnowNext12h),
            isPredicted(probabilities.getValue(WeatherOutput.Snow12hTo24h), WeatherForecast.Snow12hTo24h),
            isStormAlert(probabilities)
        )
    }

    private fun getConditions(probabilities: Map<WeatherOutput, Float>): List<ForecastCondition> {
        val sky = getSkyCondition(probabilities, WeatherOutput.ClearNext3h, WeatherOutput.OvercastNext3h)
            ?.copy(arrival = RelativeArrivalTime.Soon)
        val weather = EVENTS.mapNotNull { getEventCondition(probabilities, it) }
        val thunderstorm = getCondition(
            probabilities,
            WeatherCondition.Thunderstorm,
            WeatherOutput.ThunderstormNext12h,
            WeatherForecast.ThunderstormNext12h
        )
        return listOfNotNull(sky) + weather + listOfNotNull(thunderstorm)
    }

    private fun getLaterConditions(probabilities: Map<WeatherOutput, Float>): List<ForecastCondition> {
        val sky = getSkyCondition(probabilities, WeatherOutput.Clear12hTo24h, WeatherOutput.Overcast12hTo24h)
        val precipitation = getCondition(
            probabilities,
            WeatherCondition.Precipitation,
            WeatherOutput.Precipitation12hTo24h,
            WeatherForecast.Precipitation12hTo24h
        )
        return listOfNotNull(sky, precipitation)
    }

    /**
     * @return the event within 3 hours when its next 3 hours output reaches its threshold, otherwise in 3 to 12 hours
     * when that output does, with the chance it happens by the end of that window
     */
    private fun getEventCondition(probabilities: Map<WeatherOutput, Float>, event: Event): ForecastCondition? {
        val soonChance = probabilities.getValue(event.soon)
        val laterChance = probabilities.getValue(event.later)
        return when {
            isPredicted(soonChance, event.soonForecast) -> ForecastCondition(
                event.condition,
                soonChance,
                RelativeArrivalTime.Soon,
                soonChance / event.soonForecast.thresholds.medium
            )

            isPredicted(laterChance, event.laterForecast) -> ForecastCondition(
                event.condition,
                soonChance + laterChance,
                RelativeArrivalTime.Later,
                laterChance / event.laterForecast.thresholds.medium
            )

            else -> null
        }
    }

    private fun getCondition(
        probabilities: Map<WeatherOutput, Float>,
        condition: WeatherCondition,
        output: WeatherOutput,
        forecast: WeatherForecast
    ): ForecastCondition? {
        val chance = probabilities.getValue(output)
        if (!isPredicted(chance, forecast)) {
            return null
        }
        return ForecastCondition(condition, chance, relativeProbability = chance / forecast.thresholds.medium)
    }

    private fun getSkyCondition(
        probabilities: Map<WeatherOutput, Float>,
        clear: WeatherOutput,
        overcast: WeatherOutput
    ): ForecastCondition? {
        val clearChance = probabilities.getValue(clear)
        val overcastChance = probabilities.getValue(overcast)
        val neitherChance = 1 - clearChance - overcastChance
        return when {
            clearChance > overcastChance && clearChance > neitherChance ->
                ForecastCondition(WeatherCondition.Clear, clearChance)

            overcastChance >= clearChance && overcastChance > neitherChance ->
                ForecastCondition(WeatherCondition.Overcast, overcastChance)

            else -> null
        }
    }

    private fun isStormAlert(probabilities: Map<WeatherOutput, Float>): Boolean {
        val chance = probabilities.getValue(WeatherOutput.StormNext3h) +
                probabilities.getValue(WeatherOutput.Storm3hTo12h)
        return chance >= WeatherNetworkCoefficients.stormAlertThresholds.get(stormSensitivity)
    }

    private fun isPredicted(chance: Float, forecast: WeatherForecast): Boolean {
        return chance >= forecast.thresholds.get(forecastSensitivity)
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
        private val EVENTS = listOf(
            Event(
                WeatherCondition.Precipitation,
                WeatherOutput.PrecipitationNext3h,
                WeatherOutput.Precipitation3hTo12h,
                WeatherForecast.PrecipitationNext3h,
                WeatherForecast.Precipitation3hTo12h
            ),
            Event(
                WeatherCondition.Storm,
                WeatherOutput.StormNext3h,
                WeatherOutput.Storm3hTo12h,
                WeatherForecast.StormNext3h,
                WeatherForecast.Storm3hTo12h
            ),
            Event(
                WeatherCondition.Wind,
                WeatherOutput.WindNext3h,
                WeatherOutput.Wind3hTo12h,
                WeatherForecast.WindNext3h,
                WeatherForecast.Wind3hTo12h
            )
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

    private data class Event(
        val condition: WeatherCondition,
        val soon: WeatherOutput,
        val later: WeatherOutput,
        val soonForecast: WeatherForecast,
        val laterForecast: WeatherForecast
    )
}
