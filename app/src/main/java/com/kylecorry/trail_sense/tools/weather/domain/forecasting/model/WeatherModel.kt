package com.kylecorry.trail_sense.tools.weather.domain.forecasting.model

import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.RelativeArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.LocalClimate
import java.time.Duration
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

internal class WeatherModel(
    private val climate: LocalClimate,
    private val location: Coordinate,
    private val forecastSensitivity: ForecastSensitivity = ForecastSensitivity.Medium,
    private val stormSensitivity: ForecastSensitivity = ForecastSensitivity.Medium
) {

    private val network = WeatherNetwork()

    /**
     * @param pressures sea level pressures with the atmospheric tide removed, oldest first
     * @return null when the pressure history is too short
     */
    fun predict(pressures: List<Reading<Pressure>>): WeatherModelPrediction? {
        val probabilities = getProbabilities(pressures) ?: return null
        return WeatherModelPrediction(getConditions(probabilities), getLaterConditions(probabilities))
    }

    private fun getConditions(probabilities: Map<WeatherOutput, Float>): List<ForecastCondition> {
        val soonSky = getSky(probabilities, WeatherOutput.ClearNext3h, WeatherOutput.OvercastNext3h)
        val sky = soonSky?.let { RelativeArrivalTime.Soon to it }
            ?: getSky(probabilities, WeatherOutput.Clear3hTo12h, WeatherOutput.Overcast3hTo12h)
                ?.let { RelativeArrivalTime.Later to it }
        val skyCondition = sky?.let { (arrival, prediction) ->
            ForecastCondition(prediction.first, probabilities.getValue(prediction.second), arrival)
        }
        val weather = EVENTS.mapNotNull { (condition, soon, later) ->
            when {
                getMargin(probabilities, soon) >= 0 ->
                    ForecastCondition(condition, probabilities.getValue(soon), RelativeArrivalTime.Soon)

                getMargin(probabilities, later) >= 0 ->
                    ForecastCondition(condition, probabilities.getValue(later), RelativeArrivalTime.Later)

                else -> null
            }
        }
        val thunderstorm = ForecastCondition(
            WeatherCondition.Thunderstorm,
            probabilities.getValue(WeatherOutput.ThunderstormNext12h)
        ).takeIf { getMargin(probabilities, WeatherOutput.ThunderstormNext12h) >= 0 }
        return listOfNotNull(skyCondition) + weather + listOfNotNull(thunderstorm)
    }

    private fun getLaterConditions(probabilities: Map<WeatherOutput, Float>): List<WeatherCondition> {
        val sky = getSky(probabilities, WeatherOutput.Clear12hTo24h, WeatherOutput.Overcast12hTo24h)
        return listOfNotNull(
            sky?.first,
            WeatherCondition.Precipitation.takeIf { getMargin(probabilities, WeatherOutput.Precipitation12hTo24h) >= 0 }
        )
    }

    private fun getProbabilities(pressures: List<Reading<Pressure>>): Map<WeatherOutput, Float>? {
        val features = getFeatures(pressures) ?: return null
        return network.predict(features)
    }

    private fun getSky(
        probabilities: Map<WeatherOutput, Float>,
        clear: WeatherOutput,
        overcast: WeatherOutput
    ): Pair<WeatherCondition, WeatherOutput>? {
        val clearMargin = getMargin(probabilities, clear)
        val overcastMargin = getMargin(probabilities, overcast)
        return when {
            clearMargin >= 0 && clearMargin > overcastMargin -> WeatherCondition.Clear to clear
            overcastMargin >= 0 -> WeatherCondition.Overcast to overcast
            else -> null
        }
    }

    /**
     * @return the log odds of the output above its threshold, positive when the condition is predicted
     */
    private fun getMargin(probabilities: Map<WeatherOutput, Float>, output: WeatherOutput): Float {
        val sensitivity = if (output in STORM_OUTPUTS) stormSensitivity else forecastSensitivity
        val thresholdOdds = when (sensitivity) {
            ForecastSensitivity.Low -> logit(output.threshold) + SENSITIVITY_LOG_ODDS
            ForecastSensitivity.Medium -> logit(output.threshold)
            ForecastSensitivity.High -> logit(output.threshold) - SENSITIVITY_LOG_ODDS
        }
        return logit(probabilities.getValue(output)) - thresholdOdds
    }

    private fun logit(probability: Float): Float {
        val p = probability.coerceIn(MIN_PROBABILITY, 1 - MIN_PROBABILITY)
        return ln(p / (1 - p))
    }

    private fun getFeatures(pressures: List<Reading<Pressure>>): Map<String, Float>? {
        val current = pressures.lastOrNull() ?: return null
        val hasOldEnoughReading = pressures.any { Duration.between(it.time, current.time) >= MINIMUM_HISTORY }
        if (!hasOldEnoughReading) {
            return null
        }
        val recent = pressures.filter { Duration.between(it.time, current.time) <= HISTORY_WINDOW }

        val time = current.time.atZone(ZoneOffset.UTC)
        val monthPrecipitation = climate.monthlyPrecipitation[time.month]?.millimeters() ?: 0f
        val annualPrecipitation = climate.monthlyPrecipitation.values.sumOf { it.millimeters().toDouble() }.toFloat()
        val solarHour = ((time.hour + time.minute / 60f + location.longitude.toFloat() / 15f) % 24 + 24) % 24
        val seasonDay = time.dayOfYear + if (location.latitude < 0) SOUTHERN_SEASON_OFFSET_DAYS else 0f
        val season = 2 * PI.toFloat() * seasonDay / DAYS_PER_YEAR

        return mapOf(
            "pressure_from_standard" to current.value.hpa().value - STANDARD_PRESSURE_HPA,
            "solar_hour_sine" to sin(2 * PI.toFloat() * solarHour / 24),
            "solar_hour_cosine" to cos(2 * PI.toFloat() * solarHour / 24),
            "season_sine" to sin(season),
            "season_cosine" to cos(season),
            "absolute_latitude" to abs(location.latitude.toFloat()),
            "low_temperature" to climate.dailyTemperatures.start.celsius().value,
            "high_temperature" to climate.dailyTemperatures.end.celsius().value,
            "log_monthly_precipitation" to ln(1 + monthPrecipitation),
            "log_annual_precipitation" to ln(1 + annualPrecipitation)
        ) + DIFFERENCE_HOURS.associate { "pressure_difference_${it}h" to getPressureDifference(recent, it) }
    }

    private fun Distance.millimeters(): Float {
        return meters().value * 1000
    }

    companion object {
        private const val STANDARD_PRESSURE_HPA = 1013f
        private const val MIN_PROBABILITY = 1e-6f
        private val EVENTS = listOf(
            Triple(WeatherCondition.Precipitation, WeatherOutput.PrecipitationNext3h, WeatherOutput.Precipitation3hTo12h),
            Triple(WeatherCondition.Storm, WeatherOutput.StormNext3h, WeatherOutput.Storm3hTo12h),
            Triple(WeatherCondition.Wind, WeatherOutput.WindNext3h, WeatherOutput.Wind3hTo12h)
        )
        private val STORM_OUTPUTS = setOf(WeatherOutput.StormNext3h, WeatherOutput.Storm3hTo12h)
        private val SENSITIVITY_LOG_ODDS = ln(2f)
        private val HISTORY_WINDOW = Duration.ofHours(48)
        private val MINIMUM_HISTORY = Duration.ofHours(2)
        private const val SOUTHERN_SEASON_OFFSET_DAYS = 182.5f
        private const val DAYS_PER_YEAR = 365.25f
        private val DIFFERENCE_HOURS = listOf(1, 2, 3, 4, 6, 8, 10, 12, 15, 18, 21, 24, 30, 36, 42, 48)

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
