package com.kylecorry.trail_sense.tools.weather.domain.forecasting

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.ForecastCondition
import com.kylecorry.trail_sense.tools.weather.domain.ForecastSensitivity
import com.kylecorry.trail_sense.tools.weather.domain.PressureTide
import com.kylecorry.trail_sense.tools.weather.domain.WeatherObservation
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.alerts.WeatherAlertGenerator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTime
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival.WeatherArrivalTimeCalculator
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.learned.LearnedWeatherModel
import com.kylecorry.trail_sense.tools.weather.domain.forecasting.temperatures.ITemperatureService
import com.kylecorry.trail_sense.tools.weather.domain.get3hTendency
import com.kylecorry.trail_sense.tools.weather.domain.getLastCloud
import com.kylecorry.trail_sense.tools.weather.domain.getTemperaturePrediction
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

internal class WeatherForecaster(
    private val temperatureService: ITemperatureService,
    prefs: IWeatherPreferences,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val climate: LocalClimate? = null
) : IWeatherForecaster {

    private val forecastSensitivity = prefs.forecastSensitivity
    private val stormSensitivity = prefs.stormSensitivity
    private val arrivalCalculator = WeatherArrivalTimeCalculator(clock)
    private val alertGenerator = WeatherAlertGenerator()

    override suspend fun forecast(
        observations: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): CurrentWeather {
        val (arrival, forecast, probabilities) = getForecast(observations, clouds, location)
        val last = observations.lastOrNull()
        val lastCloud = clouds.getLastCloud(Duration.ofHours(4))

        val tendency = getTendency(forecast)

        val weather = CurrentWeather(
            WeatherPrediction(
                forecast.first().conditions.map { ForecastCondition(it, probabilities[it]) },
                forecast.last().conditions.map { ForecastCondition(it) },
                forecast.first().front,
                arrival,
                temperatureService.getTemperaturePrediction(ZonedDateTime.now(clock)),
                emptyList()
            ),
            tendency,
            last,
            lastCloud
        )

        val alerts = alertGenerator.getAlerts(weather)

        return weather.copy(prediction = weather.prediction.copy(alerts = alerts))
    }

    private suspend fun getForecast(
        readings: List<WeatherObservation>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): Forecast {
        val pressures = if (!hasEnoughReadings(readings)) {
            emptyList()
        } else {
            readings.map { PressureTide.remove(it.pressureReading(), it.location) }
        }

        val model = climate?.let { LearnedWeatherModel(it, location, forecastSensitivity, stormSensitivity) }
        val (forecast, probabilities) = getLearnedForecast(model, pressures, clouds, location)
        val arrival = arrivalCalculator.getArrivalTime(forecast, clouds)

        // WHY: Temperatures only matter for precipitation, so skip fetching them otherwise
        if (forecast.none { it.conditions.contains(WeatherCondition.Precipitation) }) {
            return Forecast(arrival, forecast, probabilities)
        }

        val arrivesIn = arrival?.let { Duration.between(Instant.now(clock), it.time) } ?: Duration.ZERO
        val temperatures = getHighLowTemperature(ZonedDateTime.now(clock), arrivesIn, Duration.ofHours(6))
        val withTemperatures = forecast.map { it.copy(conditions = addPrecipitationType(it.conditions, temperatures)) }
        val precipitationProbability = probabilities[WeatherCondition.Precipitation]
            ?: return Forecast(arrival, withTemperatures, probabilities)
        val precipitationTypes = withTemperatures.first().conditions.filter { it in PRECIPITATION_TYPES }
        return Forecast(arrival, withTemperatures, probabilities + precipitationTypes.associateWith { precipitationProbability })
    }

    private fun getLearnedForecast(
        model: LearnedWeatherModel?,
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): Pair<List<WeatherForecast>, Map<WeatherCondition, Float>> {
        val pressureForecast = getSolForecast(pressures, clouds, location)
        val cloudForecast = if (clouds.isEmpty()) null else getSolForecast(emptyList(), clouds, location)
        val cloudConditions = cloudForecast?.first()?.conditions.orEmpty()
        val modelConditions = model?.getConditions(pressures).orEmpty()
        val conditions = combine(modelConditions.keys.toList(), cloudConditions)
        val laterConditions = combine(
            model?.getLaterConditions(pressures).orEmpty(),
            cloudForecast?.last()?.conditions.orEmpty()
        )
        val current = pressureForecast.first().copy(
            conditions = conditions,
            time = getConditionTime(conditions, cloudForecast?.first())
        )
        val isSkyFromClouds = cloudConditions.any { it in SKY_CONDITIONS }
        val probabilities = modelConditions.filterKeys {
            it in conditions && !(isSkyFromClouds && it in SKY_CONDITIONS)
        }
        return listOf(current, pressureForecast.last().copy(conditions = laterConditions)) to probabilities
    }

    /**
     * @param temperatures the expected temperature range while it is precipitating
     */
    private fun addPrecipitationType(
        conditions: List<WeatherCondition>,
        temperatures: Range<Temperature>
    ): List<WeatherCondition> {
        if (WeatherCondition.Precipitation !in conditions) {
            return conditions
        }
        val mean = (temperatures.start.celsius().value + temperatures.end.celsius().value) / 2
        val type = if (mean <= SNOW_MEAN_TEMPERATURE_C) WeatherCondition.Snow else WeatherCondition.Rain
        return (conditions.filterNot { it in PRECIPITATION_TYPES } + type).distinct()
    }

    private fun combine(
        pressureConditions: List<WeatherCondition>,
        cloudConditions: List<WeatherCondition>
    ): List<WeatherCondition> {
        val cloudSky = cloudConditions.filter { it in SKY_CONDITIONS }
        val sky = cloudSky.ifEmpty { pressureConditions.filter { it in SKY_CONDITIONS } }
        val weather = (pressureConditions + cloudConditions).filter { it in WEATHER_CONDITIONS }.distinct()
        return if (WeatherCondition.Precipitation in weather) {
            weather + WeatherCondition.Overcast
        } else {
            sky + weather
        }
    }

    private fun getConditionTime(
        conditions: List<WeatherCondition>,
        cloudForecast: WeatherForecast?
    ): Instant? {
        if (WeatherCondition.Precipitation in conditions) {
            return cloudForecast?.time?.takeIf { WeatherCondition.Precipitation in cloudForecast.conditions }
        }
        return Instant.now(clock).takeIf { conditions.isNotEmpty() && conditions.none { it == WeatherCondition.Storm } }
    }

    private fun hasEnoughReadings(readings: List<WeatherObservation>): Boolean {
        val times = readings.map { it.time }
        val start = times.minOrNull() ?: return false
        val end = times.maxOrNull() ?: return false
        return Duration.between(start, end) >= minDuration
    }

    // WHY: Sol's forecast still provides the pressure tendency, front and cloud conditions, and its thresholds set when the tendency counts as changing
    private fun getSolForecast(
        pressures: List<Reading<Pressure>>,
        clouds: List<Reading<CloudGenus?>>,
        location: Coordinate
    ): List<WeatherForecast> {
        return Meteorology.forecast(
            pressures,
            clouds,
            null,
            TENDENCY_CHANGE_HPA_PER_HOUR,
            TENDENCY_STORM_HPA_PER_HOUR,
            Instant.now(clock),
            location
        )
    }

    private fun getTendency(forecast: List<WeatherForecast>): PressureTendency {
        return forecast.firstOrNull()?.get3hTendency() ?: PressureTendency.zero
    }

    private suspend fun getHighLowTemperature(
        startTime: ZonedDateTime,
        start: Duration,
        duration: Duration
    ): Range<Temperature> {
        return temperatureService.getTemperatureRange(
            startTime.plus(start),
            startTime.plus(start).plus(duration)
        )
    }

    private data class Forecast(
        val arrival: WeatherArrivalTime?,
        val forecasts: List<WeatherForecast>,
        val probabilities: Map<WeatherCondition, Float> = emptyMap()
    )

    companion object {
        private val minDuration = Duration.ofMinutes(10)
        private const val TENDENCY_CHANGE_HPA_PER_HOUR = 0.5f
        private const val TENDENCY_STORM_HPA_PER_HOUR = 1.5f
        private const val SNOW_MEAN_TEMPERATURE_C = 0f
        private val SKY_CONDITIONS = setOf(WeatherCondition.Clear, WeatherCondition.Overcast)
        private val PRECIPITATION_TYPES = setOf(WeatherCondition.Rain, WeatherCondition.Snow)
        private val WEATHER_CONDITIONS = setOf(
            WeatherCondition.Precipitation,
            WeatherCondition.Storm,
            WeatherCondition.Wind,
            WeatherCondition.Thunderstorm
        )
    }
}
