package com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.LocalClimate
import java.time.Duration
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal class LearnedWeatherModel(
    private val climate: LocalClimate,
    private val location: Coordinate,
    private val forecastSensitivity: ForecastSensitivity = ForecastSensitivity.Medium,
    private val stormSensitivity: ForecastSensitivity = ForecastSensitivity.Medium
) {

    private val network = LearnedWeatherNetwork()

    /**
     * @param pressures sea level pressures with the atmospheric tide removed, oldest first
     * @return the predicted conditions, each with its probability from 0 to 1
     */
    fun getConditions(pressures: List<Reading<Pressure>>): Map<WeatherCondition, Float> {
        val probabilities = getProbabilities(pressures) ?: return emptyMap()
        val sky = getSky(probabilities, LearnedOutput.Clear, LearnedOutput.Overcast)
        val weather = listOf(
            WeatherCondition.Precipitation to LearnedOutput.Precipitation,
            WeatherCondition.Storm to LearnedOutput.Storm,
            WeatherCondition.Wind to LearnedOutput.Wind,
            WeatherCondition.Thunderstorm to LearnedOutput.Thunderstorm
        ).filter { (_, output) -> getMargin(probabilities, output) >= 0 }
        return (listOfNotNull(sky) + weather).associate { (condition, output) ->
            condition to probabilities.getValue(output)
        }
    }

    /**
     * @param pressures sea level pressures with the atmospheric tide removed, oldest first
     * @return the conditions expected 12 to 24 hours from now
     */
    fun getLaterConditions(pressures: List<Reading<Pressure>>): List<WeatherCondition> {
        val probabilities = getProbabilities(pressures) ?: return emptyList()
        val sky = getSky(probabilities, LearnedOutput.LaterClear, LearnedOutput.LaterOvercast)
        return listOfNotNull(
            sky?.first,
            WeatherCondition.Precipitation.takeIf { getMargin(probabilities, LearnedOutput.LaterPrecipitation) >= 0 }
        )
    }

    /**
     * @param temperatures the expected temperature range while it is precipitating
     */
    fun addTemperatureConditions(
        conditions: List<WeatherCondition>,
        temperatures: Range<Temperature>
    ): List<WeatherCondition> {
        if (WeatherCondition.Precipitation !in conditions) {
            return conditions
        }
        val mean = (temperatures.start.celsius().value + temperatures.end.celsius().value) / 2
        val type = if (mean <= SNOW_MEAN_TEMPERATURE_C) WeatherCondition.Snow else WeatherCondition.Rain
        return (conditions.filterNot { it == WeatherCondition.Rain || it == WeatherCondition.Snow } + type).distinct()
    }

    private fun getProbabilities(pressures: List<Reading<Pressure>>): Map<LearnedOutput, Float>? {
        val features = getFeatures(pressures) ?: return null
        return network.predict(features)
    }

    private fun getSky(
        probabilities: Map<LearnedOutput, Float>,
        clear: LearnedOutput,
        overcast: LearnedOutput
    ): Pair<WeatherCondition, LearnedOutput>? {
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
    private fun getMargin(probabilities: Map<LearnedOutput, Float>, output: LearnedOutput): Float {
        val sensitivity = if (output == LearnedOutput.Storm) stormSensitivity else forecastSensitivity
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
        val pressureFromStandard = current.value.hpa().value - STANDARD_PRESSURE_HPA
        val change3h = getPressureChange(pressures, SHORT_WINDOW) ?: return null
        val change12h = getPressureChange(pressures, LONG_WINDOW) ?: return null
        val change24h = getPressureChange(pressures, DAY_WINDOW) ?: return null
        val recent = pressures
            .filter { Duration.between(it.time, current.time) <= RANGE_WINDOW }
            .map { it.value.hpa().value }

        val time = current.time.atZone(ZoneOffset.UTC)
        val low = climate.dailyTemperatures.start.celsius().value
        val high = climate.dailyTemperatures.end.celsius().value
        val monthPrecipitation = climate.monthlyPrecipitation[time.month]?.millimeters() ?: 0f
        val annualPrecipitation = climate.monthlyPrecipitation.values.sumOf { it.millimeters().toDouble() }.toFloat()
        val solarHour = ((time.hour + time.minute / 60f + location.longitude.toFloat() / 15f) % 24 + 24) % 24

        return mapOf(
            "pressure_from_standard" to pressureFromStandard,
            "change_3h" to change3h,
            "change_12h" to change12h,
            "change_24h" to change24h,
            "range_48h" to recent.max() - recent.min(),
            "pressure_from_standard_squared" to pressureFromStandard * pressureFromStandard,
            "change_3h_squared" to change3h * change3h,
            "change_12h_squared" to change12h * change12h,
            "change_24h_squared" to change24h * change24h,
            "pressure_from_standard_x_change_12h" to pressureFromStandard * change12h,
            "pressure_from_standard_x_change_3h" to pressureFromStandard * change3h,
            "change_3h_x_change_12h" to change3h * change12h,
            "fall_3h" to min(change3h, 0f),
            "fall_12h" to min(change12h, 0f),
            "mean_temperature" to (low + high) / 2,
            "high_temperature" to high,
            "temperature_range" to high - low,
            "log_monthly_precipitation" to ln(1 + monthPrecipitation),
            "monthly_precipitation_share" to monthPrecipitation * 12 / max(annualPrecipitation, 1f),
            "log_annual_precipitation" to ln(1 + annualPrecipitation),
            "solar_hour_sine" to sin(2 * PI.toFloat() * solarHour / 24),
            "solar_hour_cosine" to cos(2 * PI.toFloat() * solarHour / 24)
        )
    }

    private fun Distance.millimeters(): Float {
        return meters().value * 1000
    }

    companion object {
        private const val STANDARD_PRESSURE_HPA = 1013f
        private const val SNOW_MEAN_TEMPERATURE_C = 0f
        private const val MIN_PROBABILITY = 1e-6f
        private val SENSITIVITY_LOG_ODDS = ln(2f)
        private val SHORT_WINDOW = Duration.ofHours(3)
        private val LONG_WINDOW = Duration.ofHours(12)
        private val DAY_WINDOW = Duration.ofHours(24)
        private val RANGE_WINDOW = Duration.ofHours(48)
        private val MINIMUM_CHANGE_WINDOW = Duration.ofHours(2)

        // WHY: The model was trained on changes scaled to the full window, so a short history reads as if it covered it
        private fun getPressureChange(pressures: List<Reading<Pressure>>, window: Duration): Float? {
            val current = pressures.last()
            val past = pressures
                .filter { Duration.between(it.time, current.time) >= MINIMUM_CHANGE_WINDOW }
                .minByOrNull { abs(Duration.between(it.time, current.time).seconds - window.seconds) }
                ?: return null
            val span = Duration.between(past.time, current.time).seconds.toFloat()
            return (current.value.hpa().value - past.value.hpa().value) * window.seconds / span
        }
    }
}
